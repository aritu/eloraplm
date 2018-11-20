/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package com.aritu.eloraplm.integration.get.restoperations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileInfoResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileInfoResponseDoc;
import com.aritu.eloraplm.integration.get.restoperations.util.UidRequestDoc;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.ItemInfo;

/**
 * @author aritu
 *
 */
@Operation(id = GetFileInfo.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get File Info", description = "Get information of the document.")
public class GetFileInfo {
    public static final String ID = "Elora.PlmConnector.GetFileInfo";

    private static final Log log = LogFactory.getLog(GetFileInfo.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = true)
    protected ArrayList<JsonNode> documents;

    @Param(name = "getItemsInfo", required = true)
    protected boolean getItemsInfo;

    protected Map<String, UidRequestDoc> requestDocs;

    protected GetFileInfoResponse fileInfoResponse;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        fileInfoResponse = new GetFileInfoResponse();
        requestDocs = new HashMap<String, UidRequestDoc>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (documents != null) {
                loadRequestDocs();
            }

            // Process the documents
            if (!requestDocs.isEmpty()) {
                for (UidRequestDoc requestDoc : requestDocs.values()) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            fileInfoResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            fileInfoResponse.setErrorMessage(e.getMessage());
            fileInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            fileInfoResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            fileInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = fileInfoResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    protected void processSingleDocument(UidRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc;
        DocumentModel realDoc;
        DocumentModel wcDoc;
        DocumentModel parentDoc = null;
        String proxyUid = null;

        // TODO: Mirar como solucionar el problema de que cuando se hace un save
        // se quieren sacar los datos del wc y si no se quieren sacar los datos
        // de la version. Hay problemas si se hace un save en el integrador y
        // luego se cambia algo (el mismo usuario) desde elora. ¿Qué se tiene
        // que mostrar en el integrador?
        if (requestDoc.getRealRef() != null) {
            doc = EloraIntegrationHelper.getRealDoc(requestDoc.getRealRef(),
                    session);
            realDoc = doc;
            wcDoc = session.getWorkingCopy(doc.getRef());
        } else {
            doc = EloraIntegrationHelper.getWcDoc(requestDoc.getWcRef(),
                    session);
            wcDoc = doc;
            realDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
        }

        if (requestDoc.getParentRealRef() != null) {
            parentDoc = EloraIntegrationHelper.getWcDoc(
                    requestDoc.getParentRealRef(), session);

            // Get documents proxy in parentDoc from document version (if
            // integration sends me realRef)
            DocumentModelList proxyList = session.getProxies(doc.getRef(),
                    parentDoc.getRef());
            if (proxyList.size() > 0) {
                proxyUid = proxyList.get(0).getId();
            } else {
                // Get documents proxy in parentDoc from wc. In case it has not
                // a proxy pointing to an archived version we get proxy pointing
                // to wc
                proxyList = session.getProxies(wcDoc.getRef(),
                        parentDoc.getRef());
                if (proxyList.size() > 0) {
                    proxyUid = proxyList.get(0).getId();
                }
            }
        }

        String reference = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String title = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String lastContributor = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR) == null ? ""
                        : doc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR).toString();

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(wcDoc);

        Blob contentBlob = (Blob) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        String filename = null;
        String hash = null;
        if (contentBlob != null) {
            filename = contentBlob.getFilename();
            hash = contentBlob.getDigest();
        }

        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            lastModified = lastModifiedGc.getTime();
        }

        String latestVersionLabel = wcDoc.getVersionLabel();

        String path = doc.getPathAsString();
        String realUid = realDoc.getId();
        String wcUid = wcDoc.getId();

        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String summaryUrl = EloraUrlHelper.getDocumentSummaryInPrintModeUrl(
                request, doc);

        VersionInfo currentVersionInfo = EloraIntegrationHelper.createVersionInfo(
                doc, wcDoc);

        GetFileInfoResponseDoc responseDoc = new GetFileInfoResponseDoc(realUid,
                wcUid, proxyUid, doc.getType(), reference, title, filename,
                hash, description, currentVersionInfo,
                doc.getCurrentLifeCycleState(), latestVersionLabel, path,
                eloraLockInfo, lastContributor, lastModified, summaryUrl);

        if (getItemsInfo) {
            List<ItemInfo> itemsInfo = EloraIntegrationHelper.getItemsInfo(
                    session, doc, true);
            responseDoc.setItemsInfo(itemsInfo);
        }

        fileInfoResponse.addDocument(responseDoc);
    }

    protected void loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);
            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "wcUid", false);
            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", false);
            DocumentRef parentRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "parentRealUid", false);

            UidRequestDoc requestDoc = new UidRequestDoc(wcRef, realRef,
                    parentRealRef);
            if (wcRef != null || realRef != null) {
                // We want to avoid duplicated documents (but an AV of a
                // document and its WC are different documents)
                String indexUid = realRef == null ? wcRef.toString()
                        : realRef.toString();
                if (!requestDocs.containsKey(indexUid)) {
                    requestDocs.put(indexUid, requestDoc);
                }
            } else {
                throw new EloraException("Null wcUid and realUid");
            }
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

}
