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
package com.aritu.eloraplm.integration.restoperations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.restoperations.util.EloraLockRequestDoc;
import com.aritu.eloraplm.integration.restoperations.util.EloraLockUnlockResponse;
import com.aritu.eloraplm.integration.restoperations.util.EloraLockUnlockResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
@Operation(id = EloraLock.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - EloraLock", description = "Lock document from the Elora Plm Connector.")
public class EloraLock {

    public static final String ID = "Elora.PlmConnector.EloraLock";

    private static final Log log = LogFactory.getLog(EloraLock.class);

    protected static final String RESULT_OK = "OK";

    protected static final String RESULT_KO = "KO";

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = true)
    protected ArrayList<JsonNode> documents;

    @Context
    protected CoreSession session;

    protected Map<String, EloraLockRequestDoc> requestDocs;

    protected EloraLockUnlockResponse lockResponse;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        lockResponse = new EloraLockUnlockResponse();
        requestDocs = new LinkedHashMap<String, EloraLockRequestDoc>();

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
                for (EloraLockRequestDoc requestDoc : requestDocs.values()) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            session.save();

            log.info(logInitMsg + "Document successfuly saved.");
            lockResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            lockResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            lockResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            lockResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            lockResponse.setErrorMessage(e.getMessage());
            lockResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            lockResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            lockResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            lockResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = lockResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
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

            EloraLockRequestDoc requestDoc = new EloraLockRequestDoc(wcRef,
                    realRef);

            if (wcRef != null) {
                requestDocs.put(wcRef.toString(), requestDoc);
            } else if (realRef != null) {
                requestDocs.put(realRef.toString(), requestDoc);
            } else {
                throw new EloraException("Null wcUid and realUid");
            }
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected void processSingleDocument(EloraLockRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel wcDoc;
        if (requestDoc.getWcRef() == null && requestDoc.getRealRef() == null) {
            throw new EloraException("Null wcUid and realUid");
        } else if (requestDoc.getWcRef() != null) {
            wcDoc = session.getDocument(requestDoc.getWcRef());
        } else {
            wcDoc = session.getWorkingCopy(requestDoc.getRealRef());
        }

        EloraLockUnlockResponseDoc responseDoc = new EloraLockUnlockResponseDoc(
                requestDoc.getWcRef() != null ? requestDoc.getWcRef().toString()
                        : null,
                requestDoc.getRealRef() != null
                        ? requestDoc.getRealRef().toString() : null);

        try {
            wcDoc = EloraDocumentHelper.lockDocument(wcDoc);
            responseDoc.setResult(RESULT_OK);
        } catch (DocumentAlreadyLockedException e) {
            responseDoc.setResult(RESULT_KO);
            responseDoc.setError("Locked by " + e.getOwner());
        } catch (DocumentInUnlockableStateException e) {
            responseDoc.setResult(RESULT_KO);
            responseDoc.setError("Document is in an unlockable state: "
                    + e.getCurrentLifeCycleState());
        } catch (DocumentLockRightsException e) {
            responseDoc.setResult(RESULT_KO);
            responseDoc.setError("User has no rights to lock the document");
        } catch (EloraException e) {
            responseDoc.setResult(RESULT_KO);
            responseDoc.setError(e.getMessage());
        }

        EloraLockInfo lockInfo = EloraDocumentHelper.getLockInfo(wcDoc);
        responseDoc.setEloraLockInfo(lockInfo);

        lockResponse.addDocument(responseDoc);
    }
}
