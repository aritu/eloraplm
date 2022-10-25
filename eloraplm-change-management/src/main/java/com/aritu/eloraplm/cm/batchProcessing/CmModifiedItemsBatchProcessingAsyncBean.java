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
package com.aritu.eloraplm.cm.batchProcessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.batchProcessing.util.ResultType;
import com.aritu.eloraplm.cm.treetable.CMItemsNodeData;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.pdm.overwrite.helper.OverwriteVersionHelper;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

@AutoCreate
@Name("cmModifiedItemsBatchProcessingAsync")
public class CmModifiedItemsBatchProcessingAsyncBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsBatchProcessingAsyncBean.class);

    private List<ResultType> successList = new ArrayList<ResultType>();

    private List<ResultType> errorsList = new ArrayList<ResultType>();

    private String exceptionErrorMsg = null;

    private boolean txStarted = false;

    private LoginStack loginStack = null;

    private String itemType = CMConstants.ITEM_TYPE_BOM;

    private String itemClass = CMConstants.ITEM_CLASS_MODIFIED;

    @Observer(CMBatchProcessingEventNames.CHECKOUT_MODIFIED_ITEMS)
    public void checkoutAsync(DocumentModel cmProcessDoc, TreeNode root,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[checkoutAsync] ["
                + cmProcessDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        successList = new ArrayList<ResultType>();
        errorsList = new ArrayList<ResultType>();
        exceptionErrorMsg = "";
        txStarted = false;
        loginStack = null;

        CoreSession receivedSession = cmProcessDoc.getCoreSession();
        CoreSession session = null;

        try {
            // Open a new session for asynchronous processing. In this way, even
            // if the user log outs, the process can continue.
            session = CoreInstance.openCoreSession(
                    receivedSession.getRepositoryName(),
                    receivedSession.getPrincipal());

            txStarted = CmBatchProcessingHelper.initBatchProcessExecution(
                    cmProcessDoc, loginStack, session);

            cmProcessDoc = session.getDocument(new IdRef(cmProcessDoc.getId()));

            log.trace(logInitMsg + "Start processing modified items");

            processCheckoutItems(cmProcessDoc, root, session);

            log.trace(logInitMsg + "|" + successList.size()
                    + "| documents processed successfully");

        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing checkout: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception processing checkout: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private void processCheckoutItems(DocumentModel cmProcessDoc, TreeNode node,
            CoreSession documentManager) {
        String logInitMsg = "[processCheckoutItems] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : node.getChildren()) {
            CMItemsNodeData nodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isManaged(nodeData)
                    && !CmBatchProcessingHelper.isIgnored(nodeData)
                    && !CmBatchProcessingHelper.isRemoved(nodeData)) {
                DocumentModel destinationItem = nodeData.getDestinationItem();
                String documentId = destinationItem.getId();
                String reference = destinationItem.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                String title = destinationItem.getTitle();

                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    EloraDocumentHelper.checkOutDocument(destinationItem);

                    ResultType success = new ResultType(documentId, reference,
                            title);
                    successList.add(success);

                } catch (Exception e) {
                    log.error(logInitMsg
                            + "Exception processing checkout  on documentId =|"
                            + documentId + "|, reference = |" + reference
                            + "|. Exception details: " + e.getClass().getName()
                            + ". " + e.getMessage(), e);

                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.checkout");

                    ResultType error = new ResultType(documentId, reference,
                            title, message);
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
        }
    }

    @Observer(CMBatchProcessingEventNames.CHECKIN_MODIFIED_ITEMS)
    public void checkinAsync(DocumentModel cmProcessDoc, TreeNode root,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[checkinAsync] ["
                + cmProcessDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        successList = new ArrayList<ResultType>();
        errorsList = new ArrayList<ResultType>();
        exceptionErrorMsg = "";
        txStarted = false;
        loginStack = null;

        CoreSession receivedSession = cmProcessDoc.getCoreSession();
        CoreSession session = null;

        try {
            // Open a new session for asynchronous processing. In this way, even
            // if the user log outs, the process can continue.
            session = CoreInstance.openCoreSession(
                    receivedSession.getRepositoryName(),
                    receivedSession.getPrincipal());

            txStarted = CmBatchProcessingHelper.initBatchProcessExecution(
                    cmProcessDoc, loginStack, session);

            cmProcessDoc = session.getDocument(new IdRef(cmProcessDoc.getId()));

            log.trace(logInitMsg + "Start processing modified items");

            boolean overwrite = false;
            List<String> destinationWcUidList = processCheckinItems(logInitMsg,
                    cmProcessDoc, root, overwrite, checkinManager,
                    eloraDocumentRelationManager, session);

            log.trace(logInitMsg + "|" + successList.size()
                    + "| documents processed successfully");

            // Set as Managed successfully processed items
            if (destinationWcUidList != null
                    && destinationWcUidList.size() > 0) {
                try {
                    CMHelper.setAsManagedItemsByDestinationWcUidList(session,
                            cmProcessDoc, itemType, itemClass,
                            destinationWcUidList);
                } catch (EloraException e) {
                    log.error(logInitMsg
                            + "An error occurred when setting as managed processed modified items. Error message is: "
                            + e.getMessage(), e);
                }
            }

        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing Checkin: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception processing Checkin: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }

        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private List<String> processCheckinItems(String logInitMsg,
            DocumentModel cmProcessDoc, TreeNode root, boolean overwrite,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException,
            CycleDetectedException, DocumentNotCheckedOutException {

        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> destinationWcUidList = new ArrayList<String>();

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        for (TreeNode childNode : root.getChildren()) {

            CMItemsNodeData nodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(nodeData)
                    && !CmBatchProcessingHelper.isRemoved(nodeData)
                    && !CmBatchProcessingHelper.isManaged(nodeData)
                    && CmBatchProcessingHelper.isCheckedOut(nodeData)) {

                DocumentModel destinationItem = nodeData.getDestinationItem();
                String documentId = destinationItem.getId();
                String reference = destinationItem.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                String title = destinationItem.getTitle();

                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    if (!overwrite) {
                        log.trace(
                                logInitMsg + "Proceeding to checkin document |"
                                        + documentId + "|");

                        String checkinComment = nodeData.getComment();

                        destinationItem = checkinManager.checkinDocument(
                                destinationItem, checkinComment, null,
                                cmProcessReference, false);

                    } else {
                        log.trace(logInitMsg
                                + "Proceeding to overwrite document |"
                                + documentId + "|");

                        overwriteDoc(destinationItem,
                                eloraDocumentRelationManager, documentManager,
                                cmProcessReference);
                    }

                    destinationWcUidList.add(documentId);

                    ResultType success = new ResultType(documentId, reference,
                            title);
                    successList.add(success);

                } catch (CheckinNotAllowedException e) {
                    log.error(logInitMsg
                            + "Exception processing check in  on documentId =|"
                            + documentId + "|, reference = |" + reference
                            + "|. Exception details: " + e.getClass().getName()
                            + ". " + e.getMessage(), e);

                    DocumentModel errorCausedDocument = e.getErrorDocument();
                    String errorCausedDocumentReference = errorCausedDocument.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

                    Object[] messageParams = { errorCausedDocumentReference,
                            errorCausedDocument.getTitle() };
                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.itemCheckinNotAllowed",
                            messageParams);

                    ResultType error = new ResultType(documentId, reference,
                            title, message);
                    errorsList.add(error);

                    TransactionHelper.setTransactionRollbackOnly();

                } catch (BomCharacteristicsValidatorException e) {

                    log.error(logInitMsg
                            + "Exception processing check in  on documentId =|"
                            + documentId + "|, reference = |" + reference
                            + "|. Exception details: " + e.getClass().getName()
                            + ". " + e.getMessage(), e);

                    Object[] messageParams = {
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle() };
                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.pdm.characteristicsRequired",
                            messageParams);

                    ResultType error = new ResultType(documentId, reference,
                            title, message);
                    errorsList.add(error);

                    TransactionHelper.setTransactionRollbackOnly();

                } catch (Exception e) {

                    log.error(logInitMsg
                            + "Exception processing check in  on documentId =|"
                            + documentId + "|, reference = |" + reference
                            + "|. Exception details: " + e.getClass().getName()
                            + ". " + e.getMessage(), e);

                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.checkin");

                    ResultType error = new ResultType(documentId, reference,
                            title, message);
                    errorsList.add(error);

                    TransactionHelper.setTransactionRollbackOnly();

                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }

            }

            // Increase processed counter
            Events.instance().raiseEvent(
                    CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                    cmProcessDoc.getId());
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return destinationWcUidList;
    }

    private void overwriteDoc(DocumentModel doc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager, String cmProcessReference)
            throws CheckinNotAllowedException, EloraException,
            BomCharacteristicsValidatorException {
        String logInitMsg = "[overwriteDoc] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
        if (baseDoc == null) {
            throw new EloraException("Document |" + doc.getId()
                    + "| has no base version. Probably because it has no AVs.");
        }

        DocumentValidationService validator = Framework.getService(
                DocumentValidationService.class);

        log.trace(logInitMsg + "Start overwriting document |" + doc.getId()
                + "|");
        OverwriteVersionHelper.overwriteDocument(doc, baseDoc,
                eloraDocumentRelationManager, validator, documentManager, null,
                null, cmProcessReference);
        log.trace(logInitMsg + "Finished overwriting document |" + doc.getId()
                + "|");

        if (doc.isLocked()) {
            doc.removeLock();
        }
    }

    @Observer(CMBatchProcessingEventNames.PROMOTE_MODIFIED_ITEMS)
    public void promoteAsync(DocumentModel cmProcessDoc, TreeNode root,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[promoteAsync] ["
                + cmProcessDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        successList = new ArrayList<ResultType>();
        errorsList = new ArrayList<ResultType>();
        exceptionErrorMsg = "";
        txStarted = false;
        loginStack = null;

        CoreSession receivedSession = cmProcessDoc.getCoreSession();
        CoreSession session = null;

        try {
            // Open a new session for asynchronous processing. In this way, even
            // if the user log outs, the process can continue.
            session = CoreInstance.openCoreSession(
                    receivedSession.getRepositoryName(),
                    receivedSession.getPrincipal());

            txStarted = CmBatchProcessingHelper.initBatchProcessExecution(
                    cmProcessDoc, loginStack, session);

            cmProcessDoc = session.getDocument(new IdRef(cmProcessDoc.getId()));

            // Process promotes
            List<String> destinationWcUidList = processPromoteItems(
                    cmProcessDoc, root, eloraDocumentRelationManager, session);
            log.trace(logInitMsg + "Finished promoting all items");

            // Set as Managed successfully processed items
            if (destinationWcUidList != null
                    && destinationWcUidList.size() > 0) {
                try {
                    CMHelper.setAsManagedItemsByDestinationWcUidList(session,
                            cmProcessDoc, itemType, itemClass,
                            destinationWcUidList);
                } catch (EloraException e) {
                    log.error(logInitMsg
                            + "An error occurred when setting as managed processed impacted items. Error message is: "
                            + e.getMessage(), e);
                }
            }
        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing Promote: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception processing Promote: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);
            // close opened session
            session.close();
        }
        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private List<String> processPromoteItems(DocumentModel cmProcessDoc,
            TreeNode root,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {
        String logInitMsg = "[processPromoteItems] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        List<String> destinationWcUidList = new ArrayList<String>();

        List<String> bomHierarchicalAndDirectRelations = new ArrayList<>(
                RelationsConfig.bomHierarchicalRelationsList);
        bomHierarchicalAndDirectRelations.addAll(
                RelationsConfig.bomDirectRelationsList);

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        for (TreeNode childNode : root.getChildren()) {
            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isRemoved(childNodeData)) {

                DocumentModel doc = CmBatchProcessingHelper.getArchivedDestinationDoc(
                        childNodeData);

                if (!EloraDocumentHelper.isReleased(doc)) {

                    String docId = doc.getId();

                    log.trace(logInitMsg + "Start promoting document |" + docId
                            + "|");
                    String reference = doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    String title = doc.getTitle();

                    try {
                        // TODO: se puede poner mejor la transaccion ya que no
                        // haría falta para los chequeos previos al promote.
                        // Ahora si falla un chequeo se hace rollback y en
                        // realidad no se ha cambiado nada!
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        log.trace(logInitMsg
                                + "Start checking release or obsolete in major of document |"
                                + docId + "|");
                        // TODO: cambiar este 'check' por un 'is' para que se
                        // entienda
                        // mejor. También convendría moverlo de promotehelper...
                        if (PromoteHelper.checkReleasedInMajor(doc,
                                documentManager)) {
                            log.trace(logInitMsg
                                    + "Finished checking release or obsolete in major of document |"
                                    + docId + "|");
                            if (CmBatchProcessingHelper.areBomRelatedDocsReleased(
                                    doc, documentManager)) {
                                CmBatchProcessingHelper.areBomRelatedBomDocsReleased(
                                        doc, bomHierarchicalAndDirectRelations,
                                        documentManager);

                                if (doc.getAllowedStateTransitions().contains(
                                        EloraLifeCycleConstants.TRANS_APPROVE)) {

                                    doc.followTransition(
                                            EloraLifeCycleConstants.TRANS_APPROVE);

                                    // Log Nuxeo Event
                                    String comment = doc.getVersionLabel()
                                            + " @" + cmProcessReference;
                                    EloraEventHelper.fireEvent(
                                            PdmEventNames.PDM_PROMOTED_EVENT,
                                            doc, comment);

                                    EloraDocumentHelper.disableVersioningDocument(
                                            doc);
                                    documentManager.saveDocument(doc);

                                    DocumentModel wcDoc = documentManager.getWorkingCopy(
                                            doc.getRef());
                                    DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                                            wcDoc);
                                    if (baseDoc == null) {
                                        throw new EloraException("Document |"
                                                + wcDoc.getId()
                                                + "| has no base version. Probably because it has no AVs.");
                                    }
                                    if (!baseDoc.getId().equals(doc.getId())) {
                                        VersionModel version = new VersionModelImpl();
                                        version.setId(doc.getId());
                                        EloraDocumentHelper.restoreWorkingCopyToVersion(
                                                wcDoc, version,
                                                eloraDocumentRelationManager,
                                                documentManager);
                                    } else {
                                        EloraDocumentHelper.restoreToVersion(
                                                wcDoc.getRef(), doc.getRef(),
                                                true, true, documentManager);
                                    }
                                    documentManager.removeLock(wcDoc.getRef());
                                    // Fire Approved event
                                    EloraEventHelper.fireEvent(
                                            PdmEventNames.PDM_APPROVED_EVENT,
                                            doc);

                                    destinationWcUidList.add(wcDoc.getId());

                                    ResultType success = new ResultType(docId,
                                            reference, title);
                                    successList.add(success);

                                    log.trace(logInitMsg
                                            + "Finished promoting document |"
                                            + docId + "|");

                                } else {
                                    log.error(logInitMsg
                                            + "Document state does not support actual transition. docId = |"
                                            + docId + "|, reference =|"
                                            + reference + "|, title=|" + title
                                            + "|");

                                    String message = EloraMessageHelper.getTranslatedMessage(
                                            documentManager,
                                            "eloraplm.message.error.cm.batch.itemNotAllowedTransition");
                                    ResultType error = new ResultType(
                                            doc.getId(), reference, title,
                                            message);
                                    errorsList.add(error);

                                    TransactionHelper.setTransactionRollbackOnly();
                                }
                            } else {
                                log.error(logInitMsg
                                        + "Document with related cad not released. docId = |"
                                        + docId + "|, reference =|" + reference
                                        + "|, title=|" + title + "|");

                                String message = EloraMessageHelper.getTranslatedMessage(
                                        documentManager,
                                        "eloraplm.message.error.cm.batch.itemRelatedCadNotReleased");
                                ResultType error = new ResultType(doc.getId(),
                                        reference, title, message);
                                errorsList.add(error);

                                TransactionHelper.setTransactionRollbackOnly();
                            }
                        } else {
                            log.error(logInitMsg
                                    + "Document has another released document in the same major. docId = |"
                                    + docId + "|, reference =|" + reference
                                    + "|, title=|" + title + "|");

                            String message = EloraMessageHelper.getTranslatedMessage(
                                    documentManager,
                                    "eloraplm.message.error.cm.batch.itemWithReleasedOnMajor");
                            ResultType error = new ResultType(doc.getId(),
                                    reference, title, message);
                            errorsList.add(error);

                            TransactionHelper.setTransactionRollbackOnly();
                        }
                    } catch (Exception e) {
                        log.error(logInitMsg
                                + "Exception processing Promote on documentId =|"
                                + doc.getId() + "|, reference = |" + reference
                                + "|. Exception details: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(), e);

                        String message = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.promote");
                        ResultType error = new ResultType(doc.getId(),
                                reference, title, message);
                        errorsList.add(error);

                        TransactionHelper.setTransactionRollbackOnly();

                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                    }
                }

            }

            // Increase processed counter
            Events.instance().raiseEvent(
                    CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                    cmProcessDoc.getId());
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return destinationWcUidList;
    }

    @Observer(CMBatchProcessingEventNames.UNDO_CHECKOUT_MODIFIED_ITEMS)
    public void undoCheckoutAsync(DocumentModel cmProcessDoc,
            List<DocumentModel> checkedOutDocs,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[checundoCheckoutAsynckinAsync] ["
                + cmProcessDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        successList = new ArrayList<ResultType>();
        errorsList = new ArrayList<ResultType>();
        exceptionErrorMsg = "";
        txStarted = false;
        loginStack = null;

        CoreSession receivedSession = cmProcessDoc.getCoreSession();
        CoreSession session = null;

        try {
            // Open a new session for asynchronous processing. In this way, even
            // if the user log outs, the process can continue.
            session = CoreInstance.openCoreSession(
                    receivedSession.getRepositoryName(),
                    receivedSession.getPrincipal());

            txStarted = CmBatchProcessingHelper.initBatchProcessExecution(
                    cmProcessDoc, loginStack, session);

            cmProcessDoc = session.getDocument(new IdRef(cmProcessDoc.getId()));

            // Process Undo Checkouts
            CmBatchProcessingHelper.processUndoCheckoutOnCheckedoutItems(
                    cmProcessDoc, checkedOutDocs, eloraDocumentRelationManager,
                    successList, errorsList, session);

            log.trace(logInitMsg + "|" + successList.size()
                    + "| documents processed successfully");

        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing Undo Checkout: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(logInitMsg
                    + "Uncontrolled exception processing Undo Checkout: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }

        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

}
