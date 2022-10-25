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

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.batchProcessing.util.CmModifiedItemsBatchProcessingHelper;
import com.aritu.eloraplm.cm.treetable.BomModifiedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.CMItemsNodeData;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("cmModifiedItemsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmModifiedItemsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsBatchProcessingBean.class);

    protected VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient BomModifiedItemsTreeBean cmBomModifiedItemsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public CmModifiedItemsBatchProcessingBean() {
    }

    public void promote() {
        String logInitMsg = "[promote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomModifiedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomModifiedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                int totalDocuments = CmModifiedItemsBatchProcessingHelper.checkAndcountTreeDocumentsForPromote(
                        root, documentManager);
                if (totalDocuments == 0) {
                    log.trace(logInitMsg + "Nothing to be promoted.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingToBePromoted"));
                } else {

                    cmProcessDoc = navigationContext.getCurrentDocument();
                    if (cmProcessDoc == null) {
                        throw new EloraException("cmProcessDoc is null");
                    }

                    String currentLifeCycleState = cmProcessDoc.getCurrentLifeCycleState();

                    String resultMsg = CmBatchProcessingHelper.checkCurrentLifeCycleInProgress(
                            currentLifeCycleState);
                    if (!resultMsg.isEmpty()) {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                resultMsg);
                        throw new EloraException(
                                "There is already a process running");
                    }

                    // Log Nuxeo event
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_PROMOTE_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.PROMOTE,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager);

                    // calculate what is the transition to come back to the
                    // current state
                    transitionToComeBackToPreviousState = LifecycleHelper.getTransitionToDestinationState(
                            cmProcessDoc, currentLifeCycleState);

                    if (transitionToComeBackToPreviousState == null) {
                        throw new EloraException(
                                "transitionToComeBackToPreviousState is null");
                    }

                    // Raise the asynchronous event
                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.PROMOTE_MODIFIED_ITEMS,
                            cmProcessDoc, root, eloraDocumentRelationManager, // ?????????
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.PROMOTE);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, processingAction,
                            totalDocuments);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.IN_PROGRESS
                            + "| event fired.");
                }

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingPromote"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingPromote"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void toggleLockAll(boolean lock) {
        String logInitMsg = "[toggleLockAll] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomModifiedItemsTreeBean.getIsDirty()) {

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                TreeNode root = cmBomModifiedItemsTreeBean.getRoot();
                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.toggleLockDoc(modifiedItemNode,
                            lock, facesMessages, messages, documentManager);
                }

                // Log Nuxeo event
                if (lock) {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_LOCK_EVENT,
                            navigationContext.getCurrentDocument());
                } else {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_UNLOCK_EVENT,
                            navigationContext.getCurrentDocument());
                }

                cmBomModifiedItemsTreeBean.createRoot();

                String message = lock
                        ? "eloraplm.message.success.cm.batch.lockAllItems"
                        : "eloraplm.message.success.cm.batch.unlockAllItems";
                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get(message));

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }

        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            String message = lock
                    ? "eloraplm.message.error.cm.batch.lockAllItems"
                    : "eloraplm.message.error.cm.batch.unlockAllItems";
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(message));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            String message = lock
                    ? "eloraplm.message.error.cm.batch.lockAllItems"
                    : "eloraplm.message.error.cm.batch.unlockAllItems";
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(message));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void checkout() {
        String logInitMsg = "[checkout] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomModifiedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomModifiedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                int totalDocuments = checkAndcountTreeDocumentsForCheckout(
                        root);
                if (totalDocuments == 0) {
                    log.trace(logInitMsg + "Nothing to be checked out.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingToBeCheckedOut"));
                } else {

                    cmProcessDoc = navigationContext.getCurrentDocument();
                    if (cmProcessDoc == null) {
                        throw new EloraException("cmProcessDoc is null");
                    }

                    String currentLifeCycleState = cmProcessDoc.getCurrentLifeCycleState();

                    String resultMsg = CmBatchProcessingHelper.checkCurrentLifeCycleInProgress(
                            currentLifeCycleState);
                    if (!resultMsg.isEmpty()) {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                resultMsg);
                        throw new EloraException(
                                "There is already a process running");
                    }

                    // Log Nuxeo event
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_CHECKOUT_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.CHECKOUT,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager);

                    // calculate what is the transition to come back to the
                    // current state
                    transitionToComeBackToPreviousState = LifecycleHelper.getTransitionToDestinationState(
                            cmProcessDoc, currentLifeCycleState);

                    if (transitionToComeBackToPreviousState == null) {
                        throw new EloraException(
                                "transitionToComeBackToPreviousState is null");
                    }

                    // Raise the asynchronous event
                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.CHECKOUT_MODIFIED_ITEMS,
                            cmProcessDoc, root,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKOUT_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.CHECKOUT);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, processingAction,
                            totalDocuments);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.IN_PROGRESS
                            + "| event fired.");

                }

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }

        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingExecuteActions"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingExecuteActions"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private int checkAndcountTreeDocumentsForCheckout(TreeNode root)
            throws EloraException {
        int totalDocs = 0;

        String logInitMsg = "[checkAndcountTreeDocumentsForCheckout] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {

            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isManaged(childNodeData)
                    && !CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isRemoved(childNodeData)) {

                DocumentModel destItem = childNodeData.getDestinationItem();

                checkDestinationItemLocked(destItem);

                totalDocs++;
            }
        }
        log.trace(logInitMsg + "|" + totalDocs + "| documents for checkout.");

        return totalDocs;
    }

    private int checkAndcountTreeDocumentsForCheckin(TreeNode root)
            throws EloraException {
        int totalDocs = 0;

        String logInitMsg = "[checkAndcountTreeDocumentsForCheckin] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {

            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isRemoved(childNodeData)
                    && !CmBatchProcessingHelper.isManaged(childNodeData)
                    && CmBatchProcessingHelper.isCheckedOut(childNodeData)) {

                DocumentModel destItem = childNodeData.getDestinationItem();

                checkDestinationItemLocked(destItem);

                totalDocs++;
            }
        }
        log.trace(logInitMsg + "|" + totalDocs + "| documents for checkin.");

        return totalDocs;
    }

    private void checkDestinationItemLocked(DocumentModel destItem)
            throws EloraException {
        if (!EloraDocumentHelper.isLockedByUserOrAdmin(destItem,
                documentManager)) {

            String reference = destItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.itemNotLocked"),
                    reference, destItem.getTitle());
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.cm.batch.allItemsWithActionMustBeLocked"));

            throw new EloraException("Item with id |" + destItem.getId()
                    + "| and reference |" + reference + "| is not locked.");
        }
    }

    public void checkin() {
        String logInitMsg = "[checkin] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomModifiedItemsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomModifiedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                int totalDocuments = checkAndcountTreeDocumentsForCheckin(root);
                if (totalDocuments == 0) {
                    log.trace(logInitMsg + "Nothing to be checked in.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingToBeCheckedIn"));
                } else {
                    cmProcessDoc = navigationContext.getCurrentDocument();
                    if (cmProcessDoc == null) {
                        throw new EloraException("cmProcessDoc is null");
                    }

                    String currentLifeCycleState = cmProcessDoc.getCurrentLifeCycleState();

                    String resultMsg = CmBatchProcessingHelper.checkCurrentLifeCycleInProgress(
                            currentLifeCycleState);
                    if (!resultMsg.isEmpty()) {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                resultMsg);
                        throw new EloraException(
                                "There is already a process running");
                    }

                    // Log Nuxeo event
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_CHECKIN_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.CHECKIN,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager);

                    // calculate what is the transition to come back to the
                    // current state
                    transitionToComeBackToPreviousState = LifecycleHelper.getTransitionToDestinationState(
                            cmProcessDoc, currentLifeCycleState);
                    if (transitionToComeBackToPreviousState == null) {
                        throw new EloraException(
                                "transitionToComeBackToPreviousState is null");
                    }

                    // Raise the asynchronous event
                    CheckinManager checkinManager = Framework.getService(
                            CheckinManager.class);

                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.CHECKIN_MODIFIED_ITEMS,
                            cmProcessDoc, root, checkinManager,
                            eloraDocumentRelationManager, // ???????
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKIN_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.CHECKIN);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, processingAction,
                            totalDocuments);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.IN_PROGRESS
                            + "| event fired.");
                }
            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }

        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingCheckin"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingCheckin"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void undoCheckout() {
        String logInitMsg = "[undoCheckout] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomModifiedItemsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomModifiedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                List<DocumentModel> checkedOutDocs = getCheckedOutDocuments(
                        root);

                if (checkedOutDocs == null || checkedOutDocs.size() == 0) {
                    log.trace(logInitMsg
                            + "There is not any document checked out. Nothing to do Undo Checkout.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingCheckedOut"));
                } else {

                    cmProcessDoc = navigationContext.getCurrentDocument();
                    if (cmProcessDoc == null) {
                        throw new EloraException("cmProcessDoc is null");
                    }

                    String currentLifeCycleState = cmProcessDoc.getCurrentLifeCycleState();

                    String resultMsg = CmBatchProcessingHelper.checkCurrentLifeCycleInProgress(
                            currentLifeCycleState);
                    if (!resultMsg.isEmpty()) {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                resultMsg);
                        throw new EloraException(
                                "There is already a process running");
                    }

                    // Log Nuxeo event
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_ITEMS_BATCH_UNDO_CHECKOUT_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc,
                            CMBatchProcessingConstants.UNDO_CHECKOUT,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager);

                    // calculate what is the transition to come back to the
                    // current state
                    transitionToComeBackToPreviousState = LifecycleHelper.getTransitionToDestinationState(
                            cmProcessDoc, currentLifeCycleState);
                    if (transitionToComeBackToPreviousState == null) {
                        throw new EloraException(
                                "transitionToComeBackToPreviousState is null");
                    }

                    // Raise the asynchronous event
                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.UNDO_CHECKOUT_MODIFIED_ITEMS,
                            cmProcessDoc, checkedOutDocs,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.UNDO_CHECKOUT_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.UNDO_CHECKOUT);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, processingAction,
                            checkedOutDocs.size());

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.IN_PROGRESS
                            + "| event fired.");
                }

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }

        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingUndoCheckout"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingUndoCheckout"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<DocumentModel> getCheckedOutDocuments(TreeNode root) {
        List<DocumentModel> checkedOutDocs = new ArrayList<DocumentModel>();

        for (TreeNode childNode : root.getChildren()) {

            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isManaged(childNodeData)) {

                DocumentModel destItem = childNodeData.getDestinationItem();

                if (destItem.isCheckedOut()) {
                    if (EloraDocumentHelper.isLockedByUserOrAdmin(destItem,
                            documentManager)) {
                        checkedOutDocs.add(destItem);
                    }
                }
            }

        }

        return checkedOutDocs;
    }

}
