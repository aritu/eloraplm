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
package com.aritu.eloraplm.cm.batchProcessing.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.treetable.CMItemsNodeData;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMBatchProcessingMetadataConstants;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Change Management Batch Processing.
 *
 * @author aritu
 *
 */
public class CmBatchProcessingHelper {

    private static final Log log = LogFactory.getLog(
            CmBatchProcessingHelper.class);

    public static void toggleLockProcessableDocs(TreeNode node, boolean lock,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {

        String logInitMsg = "[toogleLockProcessableDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (TreeNode childNode : node.getChildren()) {

            toggleLockDoc(childNode, lock, facesMessages, messages, session);

            toggleLockProcessableDocs(childNode, lock, facesMessages, messages,
                    session);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static void toggleLockDoc(TreeNode node, boolean lock,
            FacesMessages facesMessages, Map<String, String> messages,
            CoreSession session) throws EloraException {

        String logInitMsg = "[toggleLockDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        CMItemsNodeData nodeData = (CMItemsNodeData) node.getData();
        if (!isIgnored(nodeData) && !isRemoved(nodeData)) {
            if (lock) {
                try {
                    lockDestinationDocument(nodeData, session);
                } catch (DocumentAlreadyLockedException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.locked"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                } catch (DocumentInUnlockableStateException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.not.lockable"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                } catch (DocumentLockRightsException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.lock.rights"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                }
            } else {
                unlockDestinationDocument(nodeData, session);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static boolean isIgnored(CMItemsNodeData nodeData) {
        String action = nodeData.getAction();
        return action.equals(CMConstants.ACTION_IGNORE) ? true : false;
    }

    public static boolean isRemoved(CMItemsNodeData nodeData) {
        String action = nodeData.getAction();
        return action.equals(CMConstants.ACTION_REMOVE) ? true : false;
    }

    public static boolean isManaged(CMItemsNodeData nodeData) {
        return nodeData.getIsManaged();
    }

    public static boolean isCheckedOut(CMItemsNodeData nodeData) {
        return nodeData.getDestinationItem().isCheckedOut();
    }

    private static void lockDestinationDocument(CMItemsNodeData nodeData,
            CoreSession session)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {
        DocumentModel destinationItem = nodeData.getDestinationItem();
        EloraDocumentHelper.lockDocument(destinationItem);
    }

    private static void unlockDestinationDocument(CMItemsNodeData nodeData,
            CoreSession session) {
        DocumentModel destinationItem = nodeData.getDestinationItem();
        unlockDocument(destinationItem, session);

    }

    private static void unlockDocument(DocumentModel destinationItem,
            CoreSession session) {
        DocumentModel destinationWcDoc = session.getWorkingCopy(
                destinationItem.getRef());
        if (!destinationWcDoc.isCheckedOut()
                && EloraDocumentHelper.isLockedByUserOrAdmin(destinationWcDoc,
                        session)) {
            destinationWcDoc.removeLock();
        }
    }

    public static boolean areBomRelatedDocsReleased(DocumentModel doc,
            CoreSession documentManager) throws EloraException {
        try {
            checkBomRelatedDocsReleased(doc, documentManager);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private static void checkBomRelatedDocsReleased(DocumentModel doc,
            CoreSession documentManager) throws EloraException {
        String logInitMsg = "[checkBomRelatedCadDocsReleased] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "Start getting related cad documents of document |"
                        + doc.getId() + "|");
        List<Statement> cadStmts = getBomRelatedDocumentStmts(doc);
        log.trace(logInitMsg
                + "Finished getting related cad documents of document |"
                + doc.getId() + "|");
        for (Statement cadStmt : cadStmts) {
            DocumentModel cadDoc = RelationHelper.getDocumentModel(
                    cadStmt.getObject(), documentManager);
            if (cadDoc == null) {
                log.trace(logInitMsg
                        + "Error getting document from statement. cadStmt = |"
                        + cadStmt.toString() + "|. Document is null.");
                throw new EloraException(
                        "Error getting document from statement |"
                                + cadStmt.toString() + "|");
            }
            if (!EloraDocumentHelper.isReleased(cadDoc)) {
                throw new EloraException("Related doc is not released");
            }
        }
    }

    private static List<Statement> getBomRelatedDocumentStmts(
            DocumentModel doc) {
        List<Resource> predicateResources = new ArrayList<Resource>();
        predicateResources.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));
        predicateResources.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));

        List<Statement> cadStmts = EloraRelationHelper.getStatements(doc,
                predicateResources);

        return cadStmts;
    }

    public static boolean areBomRelatedBomDocsReleased(DocumentModel doc,
            List<String> bomHierarchicalAndDirectRelations,
            CoreSession documentManager) throws EloraException {
        try {
            checkBomRelatedBomDocsReleased(doc,
                    bomHierarchicalAndDirectRelations, documentManager);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private static void checkBomRelatedBomDocsReleased(DocumentModel doc,
            List<String> bomHierarchicalAndDirectRelations,
            CoreSession documentManager) throws EloraException {
        String logInitMsg = "[checkBomRelatedBomDocsReleased] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "Start getting related bom documents of document |"
                        + doc.getId() + "|");
        List<Statement> bomStmts = getBomHierarchicalStmts(doc,
                bomHierarchicalAndDirectRelations);
        log.trace(logInitMsg
                + "Finished getting related bom documents of document |"
                + doc.getId() + "|");
        for (Statement bomStmt : bomStmts) {
            DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                    bomStmt.getObject(), documentManager);
            if (objectBomDoc == null) {
                log.trace(logInitMsg
                        + "Error getting document from statement. bomStmt = |"
                        + bomStmt.toString() + "|. Document is null.");
                throw new EloraException(
                        "Error getting document from statement |"
                                + bomStmt.toString() + "|");
            }
            log.trace(logInitMsg + "Retrieved object |" + objectBomDoc.getId()
                    + "|");
            if (!EloraDocumentHelper.isReleased(objectBomDoc)) {
                throw new EloraException("Related doc is not released");
            }
            DocumentModel latestReleased = EloraDocumentHelper.getLatestReleasedVersion(
                    objectBomDoc);
            log.trace(logInitMsg + "Latest released of object |"
                    + objectBomDoc.getId() + "| is |" + latestReleased.getId()
                    + "|");
            if (!latestReleased.getId().equals(objectBomDoc.getId())) {
                String exceptionMsg = "Document has documents in its composition that are not latest released";
                log.error(logInitMsg + exceptionMsg + "DOC reference =|"
                        + doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + "| title=|" + doc.getTitle()
                        + "|  objetctBomDoc reference =|"
                        + objectBomDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                        + "| title=|" + objectBomDoc.getTitle() + "|");

                throw new EloraException(exceptionMsg);
            }
        }
    }

    public static List<Statement> getBomHierarchicalStmts(DocumentModel doc,
            List<String> bomHierarchicalAndDirectRelations) {
        List<Resource> predicates = new ArrayList<>();
        for (String predicateUri : bomHierarchicalAndDirectRelations) {
            predicates.add(new ResourceImpl(predicateUri));
        }
        return EloraRelationHelper.getStatements(doc, predicates);
    }

    public static void processUndoCheckoutOnCheckedoutItems(
            DocumentModel cmProcessDoc, List<DocumentModel> checkedOutDocs,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            List<ResultType> successList, List<ResultType> errorsList,
            CoreSession documentManager) throws EloraException,
            CycleDetectedException, DocumentNotCheckedOutException {

        String logInitMsg = "[processUndoCheckoutOnCheckedoutItems] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        for (DocumentModel checkedOutDoc : checkedOutDocs) {
            String checkedOutDocId = checkedOutDoc.getId();
            String checkedOutDocReference = checkedOutDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                log.trace(
                        logInitMsg + "Proceeding to undo checkout on document |"
                                + checkedOutDocId + "|");

                VersionModel version = new VersionModelImpl();
                DocumentModel latestVersion = EloraDocumentHelper.getLatestVersion(
                        checkedOutDoc);
                if (latestVersion == null) {
                    throw new EloraException("Document |"
                            + checkedOutDoc.getId()
                            + "| has no latest version or is unreadable.");
                }
                version.setId(latestVersion.getId());

                checkedOutDoc = EloraDocumentHelper.restoreWorkingCopyToVersion(
                        checkedOutDoc, version, eloraDocumentRelationManager,
                        documentManager);

                if (checkedOutDoc.isLocked()) {
                    checkedOutDoc.removeLock();
                }

                // Seam event
                Events.instance().raiseEvent(
                        PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, checkedOutDoc);

                // Log Nuxeo Event
                String comment = checkedOutDoc.getVersionLabel() + " @"
                        + cmProcessReference;
                EloraEventHelper.fireEvent(
                        PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, checkedOutDoc,
                        comment);

                ResultType success = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle());
                successList.add(success);

            } catch (EloraException e) {
                log.error(logInitMsg
                        + "Exception processing undo checkout on documentId =|"
                        + checkedOutDocId + "|, reference = |"
                        + checkedOutDocReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.undoCheckout");

                ResultType error = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();

            } catch (Exception e) {
                log.error(logInitMsg
                        + "Exception processing undo checkout on documentId =|"
                        + checkedOutDocId + "|, reference = |"
                        + checkedOutDocReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.undoCheckout");

                ResultType error = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();

            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

            // Increase processed counter
            Events.instance().raiseEvent(
                    CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                    cmProcessDoc.getId());
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static DocumentModel getArchivedDestinationDoc(
            CMItemsNodeData nodeData) throws EloraException {
        DocumentModel destinationDoc = getDocumentArchivedVersion(
                nodeData.getDestinationItem());
        return destinationDoc;
    }

    public static DocumentModel getDocumentArchivedVersion(DocumentModel doc)
            throws EloraException {
        if (!doc.isVersion()) {
            DocumentModel latestDoc = EloraDocumentHelper.getLatestVersion(doc);
            if (latestDoc == null) {
                throw new EloraException("Document |" + doc.getId()
                        + "| has no latest version or it is unreadable.");
            }
            doc = latestDoc;
        }
        return doc;
    }

    public static void addChildVersionSeriesId(String docId,
            String childVersionSeriesId,
            Map<String, List<String>> childrenVersionSeriesMap) {
        List<String> versionSeriesList = childrenVersionSeriesMap.get(docId);
        if (versionSeriesList == null) {
            versionSeriesList = new ArrayList<>();
        }
        versionSeriesList.add(childVersionSeriesId);
        childrenVersionSeriesMap.put(docId, versionSeriesList);
    }

    // --------------------------------------------------------
    // Methods related with the asynchronous processing
    // --------------------------------------------------------
    public static boolean prepareAsynchronousProcess(DocumentModel cmProcessDoc,
            String batchProcessName, String itemType, String itemClass,
            CoreSession documentManager) throws EloraException {

        boolean lifecycleStateChanged = false;

        String logInitMsg = "[prepareAsynchronousProcess] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            // Change the state of the cmProcess to 'asyncProcessing'
            cmProcessDoc.followTransition(
                    EloraLifeCycleConstants.TRANS_START_ASYNC_PROCESS);

            // Store batch process name, executor and started date and time.
            // Clear other metadatas.
            String lastBatchProcessMetadata = "";
            String lastBatchProcessExecutorMetadata = "";
            String lastBatchProcessResultMsgMetadata = "";
            String lastBatchProcesSuccessResultListMetadata = "";
            String lastBatchProcessErrorResultListMetadata = "";
            String lastBatchProcessStartedMetadata = "";
            String lastBatchProcessFinishedMetadata = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                    lastBatchProcessMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS;
                    lastBatchProcessExecutorMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_EXECUTOR;
                    lastBatchProcessResultMsgMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_RESULT_MSG;
                    lastBatchProcesSuccessResultListMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_SUCCESS_RESULT_LIST;
                    lastBatchProcessErrorResultListMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_ERROR_RESULT_LIST;
                    lastBatchProcessStartedMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_STARTED;
                    lastBatchProcessFinishedMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_FINISHED;
                } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                    lastBatchProcessMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS;
                    lastBatchProcessExecutorMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_EXECUTOR;
                    lastBatchProcessResultMsgMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_RESULT_MSG;
                    lastBatchProcesSuccessResultListMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_SUCCESS_RESULT_LIST;
                    lastBatchProcessErrorResultListMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_ERROR_RESULT_LIST;
                    lastBatchProcessStartedMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_STARTED;
                    lastBatchProcessFinishedMetadata = CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_FINISHED;
                } else {
                    log.error(logInitMsg + "Unknown item type. itemType =|"
                            + itemType + "|");
                    throw new EloraException(
                            "Unknown item type: |" + itemType + "|");
                }
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                    lastBatchProcessMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS;
                    lastBatchProcessExecutorMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_EXECUTOR;
                    lastBatchProcessResultMsgMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_RESULT_MSG;
                    lastBatchProcesSuccessResultListMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_SUCCESS_RESULT_LIST;
                    lastBatchProcessErrorResultListMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_ERROR_RESULT_LIST;
                    lastBatchProcessStartedMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_STARTED;
                    lastBatchProcessFinishedMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_FINISHED;
                } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                    lastBatchProcessMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS;
                    lastBatchProcessExecutorMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_EXECUTOR;
                    lastBatchProcessResultMsgMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_RESULT_MSG;
                    lastBatchProcesSuccessResultListMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_SUCCESS_RESULT_LIST;
                    lastBatchProcessErrorResultListMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_ERROR_RESULT_LIST;
                    lastBatchProcessStartedMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_STARTED;
                    lastBatchProcessFinishedMetadata = CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_FINISHED;
                } else {
                    log.error(logInitMsg + "Unknown item type. itemType =|"
                            + itemType + "|");
                    throw new EloraException(
                            "Unknown item type: |" + itemType + "|");
                }
            } else {
                log.error(logInitMsg + "Unknown item class. itemClass =|"
                        + itemClass + "|");
                throw new EloraException(
                        "Unknown item class: |" + itemClass + "|");
            }

            cmProcessDoc.setPropertyValue(lastBatchProcessMetadata,
                    batchProcessName);

            cmProcessDoc.setPropertyValue(lastBatchProcessExecutorMetadata,
                    documentManager.getPrincipal().getName());

            cmProcessDoc.setPropertyValue(lastBatchProcessResultMsgMetadata,
                    "");

            cmProcessDoc.setPropertyValue(
                    lastBatchProcesSuccessResultListMetadata,
                    new ArrayList<HashMap<String, Object>>());

            cmProcessDoc.setPropertyValue(
                    lastBatchProcessErrorResultListMetadata,
                    new ArrayList<HashMap<String, Object>>());

            cmProcessDoc.setPropertyValue(lastBatchProcessStartedMetadata,
                    Calendar.getInstance());

            cmProcessDoc.setPropertyValue(lastBatchProcessFinishedMetadata, "");

            cmProcessDoc.setPropertyValue(
                    CMBatchProcessingMetadataConstants.NEED_TO_BE_REFRESHED,
                    true);

            documentManager.saveDocument(cmProcessDoc);

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            lifecycleStateChanged = true;

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return lifecycleStateChanged;
    }

    public static String checkCurrentLifeCycleInProgress(
            String currentLifeCycleState) throws EloraException {

        String checkResultMsg = "";

        if (currentLifeCycleState == null || currentLifeCycleState.equals(
                EloraLifeCycleConstants.ASYNC_PROCESSING)) {
            log.error("The current lifecycle state is |" + currentLifeCycleState
                    + "| and it should not be.");

            checkResultMsg = "eloraplm.message.error.cm.batch.processIsAlreadyInProgress";

        }

        return checkResultMsg;
    }

    public static void handleExceptionInAsyncProcess(DocumentModel cmProcessDoc,
            boolean lifecycleStateChanged,
            String transitionToComeBackToPreviousState,
            CoreSession documentManager) {

        if (lifecycleStateChanged) {
            // change to previous state
            cmProcessDoc.followTransition(transitionToComeBackToPreviousState);
            documentManager.saveDocument(cmProcessDoc);
            log.trace("----------document saved, state changed to |"
                    + transitionToComeBackToPreviousState + "|");
        }

    }

    public static boolean initBatchProcessExecution(DocumentModel cmProcessDoc,
            LoginStack loginStack, CoreSession documentManager)
            throws EloraException {

        String logInitMsg = "[initBatchProcessExecution] ["
                + cmProcessDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Check life cycle state of the document
        checkCurrentLifeCycleState(cmProcessDoc.getCurrentLifeCycleState());

        boolean txStarted = TransactionHelper.isTransactionActive();

        if (txStarted) {
            if (!TransactionHelper.isTransactionTimedOut()) {
                TransactionHelper.commitOrRollbackTransaction();
                txStarted = TransactionHelper.startTransaction();

            } else {
                TransactionHelper.suspendTransaction();
                txStarted = TransactionHelper.startTransaction();

            }
        } else {
            txStarted = TransactionHelper.startTransaction();

        }

        // Push principal in LoginSTack
        pushPrincipalInLoginStack(loginStack, documentManager);

        log.trace(logInitMsg + "--- EXIT ---");

        return txStarted;
    }

    private static void checkCurrentLifeCycleState(String lifeCycleState)
            throws EloraException {

        // Check that the lifecycle state of the document is 'asyncProcessing'
        if (lifeCycleState == null || !lifeCycleState.equals(
                EloraLifeCycleConstants.ASYNC_PROCESSING)) {
            throw new EloraException(
                    "Invalid lifecycleState: |" + lifeCycleState + "|");
        }
    }

    private static void pushPrincipalInLoginStack(LoginStack loginStack,
            CoreSession documentManager) {
        NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
        loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
    }

    private static void popPrincipalFromLoginStack(LoginStack loginStack) {
        if (loginStack != null) {
            loginStack.pop();
        }
    }

    public static void finalizeBatchProcessExecution(DocumentModel cmProcessDoc,
            String itemType, String itemClass,
            String transitionToComeBackToPreviousState, boolean txStarted,
            List<ResultType> successList, List<ResultType> errorsList,
            String exceptionErrorMsg, LoginStack loginStack,
            CoreSession documentManager) {

        String logInitMsg = "[finalizeBatchProcessExecution] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (txStarted && cmProcessDoc != null) {

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            String resultMsg = "";

            // if the process has finished due to an exception
            if (exceptionErrorMsg != null && exceptionErrorMsg.length() > 0) {
                Object[] messageParams = { exceptionErrorMsg,
                        successList.size(), errorsList.size() };
                String messageKey = (itemType.equals(CMConstants.ITEM_TYPE_BOM)
                        ? "eloraplm.message.info.cm.batch.item.processFinishedWithProblem"
                        : "eloraplm.message.info.cm.batch.doc.processFinishedWithProblem");
                resultMsg = EloraMessageHelper.getTranslatedMessage(
                        documentManager, messageKey, messageParams);
            }
            // if the process has finished with errors
            else if (errorsList.size() > 0) {
                Object[] messageParams = { successList.size(),
                        errorsList.size() };
                String messageKey = (itemType.equals(CMConstants.ITEM_TYPE_BOM)
                        ? "eloraplm.message.info.cm.batch.item.processFinishedWithErrors"
                        : "eloraplm.message.info.cm.batch.doc.processFinishedWithErrors");
                resultMsg = EloraMessageHelper.getTranslatedMessage(
                        documentManager, messageKey, messageParams);

            }
            // else, the process has been completed
            else {
                Object[] messageParams = { successList.size() };
                String messageKey = (itemType.equals(CMConstants.ITEM_TYPE_BOM)
                        ? "eloraplm.message.info.cm.batch.item.processSuccessfullyFinished"
                        : "eloraplm.message.info.cm.batch.doc.processSuccessfullyFinished");
                resultMsg = EloraMessageHelper.getTranslatedMessage(
                        documentManager, messageKey, messageParams);
            }

            String lastBatchProcessResultMsgMetadata = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                lastBatchProcessResultMsgMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_RESULT_MSG
                                : CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_RESULT_MSG);

            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                lastBatchProcessResultMsgMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_RESULT_MSG
                                : CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_RESULT_MSG);
            }
            cmProcessDoc.setPropertyValue(lastBatchProcessResultMsgMetadata,
                    resultMsg);

            ArrayList<HashMap<String, Object>> lastBatchProcessSuccessList = new ArrayList<HashMap<String, Object>>();
            for (int i = 0; i < successList.size(); i++) {
                ResultType successResult = successList.get(i);
                HashMap<String, Object> successResultType = CmBatchProcessingHelper.createResultType(
                        successResult);
                lastBatchProcessSuccessList.add(successResultType);
            }
            String lastBatchProcesSuccessResultListMetadata = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                lastBatchProcesSuccessResultListMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_SUCCESS_RESULT_LIST
                                : CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_SUCCESS_RESULT_LIST);
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                lastBatchProcesSuccessResultListMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_SUCCESS_RESULT_LIST
                                : CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_SUCCESS_RESULT_LIST);
            }
            cmProcessDoc.setPropertyValue(
                    lastBatchProcesSuccessResultListMetadata,
                    lastBatchProcessSuccessList);

            ArrayList<HashMap<String, Object>> lastBatchProcessErrorList = new ArrayList<HashMap<String, Object>>();
            for (int i = 0; i < errorsList.size(); i++) {
                ResultType errorResult = errorsList.get(i);
                HashMap<String, Object> errorType = CmBatchProcessingHelper.createResultType(
                        errorResult);
                lastBatchProcessErrorList.add(errorType);

            }
            String lastBatchProcessErrorResultListMetadata = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                lastBatchProcessErrorResultListMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_ERROR_RESULT_LIST
                                : CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_ERROR_RESULT_LIST);
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                lastBatchProcessErrorResultListMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_ERROR_RESULT_LIST
                                : CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_ERROR_RESULT_LIST);
            }
            cmProcessDoc.setPropertyValue(
                    lastBatchProcessErrorResultListMetadata,
                    lastBatchProcessErrorList);

            String lastBatchProcessFinishedMetadata = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                lastBatchProcessFinishedMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_IMPACTED_ITEMS_BATCH_PROCESS_FINISHED
                                : CMBatchProcessingMetadataConstants.LAST_IMPACTED_DOCS_BATCH_PROCESS_FINISHED);
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                lastBatchProcessFinishedMetadata = (itemType.equals(
                        CMConstants.ITEM_TYPE_BOM)
                                ? CMBatchProcessingMetadataConstants.LAST_MODIFIED_ITEMS_BATCH_PROCESS_FINISHED
                                : CMBatchProcessingMetadataConstants.LAST_MODIFIED_DOCS_BATCH_PROCESS_FINISHED);
            }
            cmProcessDoc.setPropertyValue(lastBatchProcessFinishedMetadata,
                    Calendar.getInstance());

            // change to previous state
            cmProcessDoc.followTransition(transitionToComeBackToPreviousState);
            log.trace(logInitMsg
                    + "Document state changed to the previous state with transition = |"
                    + transitionToComeBackToPreviousState + "|");

            documentManager.saveDocument(cmProcessDoc);
            documentManager.save();
            log.trace(logInitMsg + "Document saved");

            TransactionHelper.commitOrRollbackTransaction();

        } else {
            log.trace(logInitMsg + "Nothing done. txStarted = |" + txStarted
                    + "|");
        }

        // Launch event for finalizing batch process
        Events.instance().raiseEvent(CMBatchProcessingEventNames.FINISHED,
                cmProcessDoc.getId());

        popPrincipalFromLoginStack(loginStack);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public static HashMap<String, Object> createResultType(
            ResultType errorResult) {

        HashMap<String, Object> errorType = new HashMap<>();

        errorType.put("document", errorResult.getDocumentId());

        errorType.put("reference", errorResult.getReference());

        errorType.put("title", errorResult.getTitle());

        errorType.put("message", errorResult.getMessage());

        return errorType;
    }

}
