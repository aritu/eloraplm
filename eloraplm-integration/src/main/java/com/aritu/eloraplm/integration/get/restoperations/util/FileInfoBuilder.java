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

package com.aritu.eloraplm.integration.get.restoperations.util;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
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
public class FileInfoBuilder {

    private static final Log log = LogFactory.getLog(FileInfoBuilder.class);

    private GetFileInfoResponse fileInfoResponse;

    private CoreSession session;

    private OperationContext ctx;

    public FileInfoBuilder(CoreSession session, OperationContext ctx) {
        this.session = session;
        this.ctx = ctx;
        fileInfoResponse = new GetFileInfoResponse();
    }

    public GetFileInfoResponse processDocuments(
            Map<String, UidRequestDoc> requestDocs, boolean getItemsInfo)
            throws IOException, EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";

        if (!requestDocs.isEmpty()) {
            for (UidRequestDoc requestDoc : requestDocs.values()) {
                processSingleDocument(requestDoc, getItemsInfo);
            }
            log.info(logInitMsg + requestDocs.size() + " documents processed.");
        } else {
            log.info(logInitMsg + "No document to process.");
        }

        return fileInfoResponse;
    }

    private void processSingleDocument(UidRequestDoc requestDoc,
            boolean getItemsInfo) throws IOException, EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";

        DocumentModel doc;
        DocumentModel realDoc;
        DocumentModel wcDoc;
        DocumentModel parentDoc = null;
        String proxyUid = null;

        // TODO: Mirar como solucionar el problema de que cuando se hace un save
        // se quieren sacar los datos del wc y, si no, se quieren sacar los
        // datos
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
            if (realDoc == null) {
                throw new EloraException("Document |" + wcDoc.getId()
                        + "| has no base version. Probably because it has no AVs.");
            }
        }

        if (!doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            throw new EloraException(
                    "Provided documents must be of CAD type. Document |"
                            + doc.getId() + "| is not.");
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
        String authoringTool = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL) == null ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL).toString();
        String authoringToolVersion = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION) == null
                        ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL_VERSION).toString();

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
                eloraLockInfo, lastContributor, lastModified, summaryUrl,
                authoringTool, authoringToolVersion);

        if (getItemsInfo) {
            List<ItemInfo> itemsInfo = EloraIntegrationHelper.getItemsInfo(
                    session, doc, true);
            responseDoc.setItemsInfo(itemsInfo);
        }

        fileInfoResponse.addDocument(responseDoc);

        log.trace(logInitMsg + "Document " + doc.getId() + " processed.");
    }

}
