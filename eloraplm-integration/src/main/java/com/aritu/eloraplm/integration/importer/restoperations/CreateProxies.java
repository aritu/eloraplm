///*
// * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// */
//
package com.aritu.eloraplm.integration.importer.restoperations;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.importer.restoperations.util.CreateProxiesRequestDoc;
import com.aritu.eloraplm.integration.importer.restoperations.util.CreateProxiesResponse;
import com.aritu.eloraplm.integration.importer.restoperations.util.CreateProxiesResponseError;

@Operation(id = CreateProxies.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_IMPORTER, label = "Importer - Create Proxies", description = "Create proxies of documents in selected destination folders.")
public class CreateProxies {
    public static final String ID = "Elora.Importer.CreateProxies";

    private static final Log log = LogFactory.getLog(CreateProxies.class);

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    @Param(name = "documents", required = true)
    private ArrayList<JsonNode> documents;

    private List<CreateProxiesRequestDoc> requestDocs;

    private CreateProxiesResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new CreateProxiesResponse();
        requestDocs = new ArrayList<CreateProxiesRequestDoc>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (documents != null) {
                loadRequestDocs();
            }

            if (!requestDocs.isEmpty()) {
                for (CreateProxiesRequestDoc requestDoc : requestDocs) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");

                session.save();
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            response.emptyErrorList();
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return response.convertToJson();
    }

    private void loadRequestDocs() throws EloraException {

        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + documents.size() + " documents found.");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            DocumentRef docRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "docUid", true);

            DocumentRef destinationRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "destinationUid", true);

            CreateProxiesRequestDoc requestDoc = new CreateProxiesRequestDoc(
                    docRef, destinationRef);
            requestDocs.add(requestDoc);
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void processSingleDocument(CreateProxiesRequestDoc requestDoc) {
        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            if (!session.exists(requestDoc.getDocRef())) {
                throw new EloraException("Provided docUid |"
                        + requestDoc.getDocRef() + "| does not exist.");
            }
            if (!session.exists(requestDoc.getDestinationRef())) {
                throw new EloraException("Provided destinationUid |"
                        + requestDoc.getDestinationRef() + "| does not exist.");
            }

            // Only create if it does not exist
            DocumentModelList proxies = session.getProxies(
                    requestDoc.getDocRef(), requestDoc.getDestinationRef());
            if (proxies.isEmpty()) {
                session.createProxy(requestDoc.getDocRef(),
                        requestDoc.getDestinationRef());
            }

        } catch (Exception e) {
            CreateProxiesResponseError error = new CreateProxiesResponseError(
                    requestDoc.getDocRef().toString(),
                    requestDoc.getDestinationRef().toString(), e.getMessage());
            response.addError(error);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }
}
