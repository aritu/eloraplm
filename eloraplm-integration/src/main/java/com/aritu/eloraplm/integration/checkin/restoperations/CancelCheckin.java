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

package com.aritu.eloraplm.integration.checkin.restoperations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.api.DraftManager;
import com.aritu.eloraplm.integration.checkin.restoperations.util.CancelCheckinRequestFolder;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author aritu
 *
 */
@Operation(id = CancelCheckin.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Cancel Checkin", description = "Cancel the checkin process of documents from the Elora Plm Connector and unlock docs.")
public class CancelCheckin {

    public static final String ID = "Elora.PlmConnector.CancelCheckin";

    private static final Log log = LogFactory.getLog(CancelCheckin.class);

    @Context
    private CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = false)
    private ArrayList<JsonNode> documents;

    @Param(name = "folders", required = false)
    private ArrayList<JsonNode> folders;

    private List<CancelCheckinRequestFolder> requestFolders;

    private List<DocumentRef> requestDocs;

    private EloraGeneralResponse cancelCheckinResponse;

    private DraftManager draftManager;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        draftManager = Framework.getService(DraftManager.class);
        cancelCheckinResponse = new EloraGeneralResponse();
        requestFolders = new ArrayList<CancelCheckinRequestFolder>();
        requestDocs = new ArrayList<DocumentRef>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (folders != null) {
                loadRequestFolders();
            }
            if (documents != null) {
                loadRequestDocs();
            }

            // Process the documents
            if (!requestDocs.isEmpty()) {
                for (DocumentRef wcRef : requestDocs) {
                    draftManager.removeDocumentDraft(session, wcRef);
                }
            }
            // Process the folders
            if (!requestFolders.isEmpty()) {
                for (CancelCheckinRequestFolder requestFolder : requestFolders) {

                    // TODO Orain requestFolder.getStructureRootRealRef sobran
                    // dau, begiratzen da dokumentuan gurasoa TempFolder motakoa
                    // izatia
                    draftManager.removeDocumentDraft(session,
                            requestFolder.getWcRef());
                }
            }

            session.save();

            log.info(logInitMsg
                    + "Checkin of documents and/or folders successfuly cancelled.");
            cancelCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            cancelCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            cancelCheckinResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            cancelCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            cancelCheckinResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            cancelCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            cancelCheckinResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = cancelCheckinResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    /**
     * Loads the documents to cancel
     *
     * @throws EloraException
     */
    protected void loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + documents.size() + " documents found.");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);
            String wcUid = EloraJsonHelper.getJsonFieldAsString(docItem,
                    "wcUid", true);
            requestDocs.add(new IdRef(wcUid));
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Loads the folders to cancel
     *
     * @throws EloraException
     */
    protected void loadRequestFolders() throws EloraException {
        String logInitMsg = "[loadRequestFolders] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + folders.size() + " folders found.");

        for (int i = 0; i < folders.size(); ++i) {
            JsonNode folderItem = folders.get(i);
            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "wcUid", true);
            DocumentRef structureRootRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "structureRootRealUid", true);

            CancelCheckinRequestFolder requestFolder = new CancelCheckinRequestFolder(
                    wcRef, structureRootRealRef);
            requestFolders.add(requestFolder);
        }

        log.trace(logInitMsg + requestFolders.size() + " folders loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    //
    // /**
    // * Removes the draft, and the real document if it is new
    // *
    // * @param wcUid
    // * @throws EloraException
    // */
    // protected void processSingleDocument(DocumentRef wcRef)
    // throws EloraException {
    // String logInitMsg = "[processSingleDocument] ["
    // + session.getPrincipal().getName() + "] ";
    //
    //
    //
    // }
    //
    // /**
    // * Removes the draft, and the folder if it is new
    // *
    // * @param wcUid
    // * @throws EloraException
    // */
    // protected void processSingleFolder(CancelCheckinRequestFolder
    // requestFolder)
    // throws EloraException {
    // String logInitMsg = "[processSingleFolder] ["
    // + session.getPrincipal().getName() + "] ";
    // log.trace(logInitMsg + "--- ENTER --- ");
    //
    // DocumentRef wcRef = requestFolder.getWcRef();
    // if (session.exists(wcRef)) {
    // DocumentModel wcFolder = session.getDocument(wcRef);
    // if (!wcFolder.isFolder()) {
    // throw new EloraException("Document is not a folder.");
    // }
    //
    // // Remove the draft
    // DocumentModel draftFolder = EloraDraftHelper.getDraftForDocument(
    // session, wcFolder, session.getPrincipal().getName(), true);
    // session.removeDocument(draftFolder.getRef());
    //
    // // If it is in Temp, remove the folder (twice to remove it
    // // completely, instead of sending it to the Trash folder
    // String tempFolderPath = EloraStructureHelper.getTempFolderPath(
    // requestFolder.getStructureRootRealRef(), session);
    //
    // if (session.getParentDocument(wcRef).getPathAsString().equals(
    // tempFolderPath)) {
    // session.removeDocument(wcRef);
    // }
    // }
    //
    // log.trace(logInitMsg + "--- EXIT --- ");
    // }

}
