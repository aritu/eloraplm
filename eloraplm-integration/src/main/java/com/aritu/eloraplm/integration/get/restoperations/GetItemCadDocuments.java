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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.FileInfoBuilder;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileInfoResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.UidRequestDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author aritu
 *
 */
@Operation(id = GetItemCadDocuments.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get Item CAD Documents", description = "Get information of the CAD documents related to the provided items.")
public class GetItemCadDocuments {
    public static final String ID = "Elora.PlmConnector.GetItemCadDocuments";

    private static final Log log = LogFactory.getLog(GetItemCadDocuments.class);

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "uid", required = true)
    private DocumentRef docRef;

    @Param(name = "getItemsInfo", required = true)
    private boolean getItemsInfo;

    private Map<String, UidRequestDoc> requestDocs;

    private GetFileInfoResponse fileInfoResponse;

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

            processDocument();

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

    protected void processDocument() throws IOException, EloraException {

        String logInitMsg = "[processDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc;

        if (!session.exists(docRef)) {
            throw new EloraException("Provided document does not exist.");
        }

        doc = EloraIntegrationHelper.getRealOrWcDoc(docRef, session);
        if (doc == null) {
            throw new EloraException("Document |" + docRef.toString()
                    + "| does not exist or is unreadable.");
        }

        if (!doc.isImmutable()) {
            doc = EloraDocumentHelper.getBaseVersion(doc);
            if (doc == null) {
                throw new EloraException("Document |" + docRef.toString()
                        + "| has no base version. Probably because it has no AVs.");
            }
        }

        requestDocs = getRelatedCadDocuments(doc);
        FileInfoBuilder fib = new FileInfoBuilder(session, ctx);
        fileInfoResponse = fib.processDocuments(requestDocs, getItemsInfo);
    }

    private Map<String, UidRequestDoc> getRelatedCadDocuments(DocumentModel doc)
            throws EloraException {

        String logInitMsg = "[getRelatedCadDocuments] ["
                + session.getPrincipal().getName() + "] ";

        Map<String, UidRequestDoc> requestDocs = new HashMap<String, UidRequestDoc>();

        log.trace(logInitMsg + "Getting CAD documents for item |" + doc.getId()
                + "|");

        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModelList objectDocuments = RelationHelper.getObjectDocuments(
                doc, predicate);

        for (DocumentModel objectDoc : objectDocuments) {
            UidRequestDoc requestDoc = new UidRequestDoc(null,
                    objectDoc.getRef(), null);
            requestDocs.put(objectDoc.getId(), requestDoc);
        }

        return requestDocs;
    }

}
