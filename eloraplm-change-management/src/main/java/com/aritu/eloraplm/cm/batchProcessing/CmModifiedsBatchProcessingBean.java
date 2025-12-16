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
import com.aritu.eloraplm.cm.treetable.CMItemsNodeData;
import com.aritu.eloraplm.cm.treetable.ModifiedItemsTreeBean;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("cmModifiedsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmModifiedsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmModifiedsBatchProcessingBean.class);

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
    protected transient ModifiedItemsTreeBean cmModifiedsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public CmModifiedsBatchProcessingBean() {
    }

    public void promote() {
        String logInitMsg = "[promote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            if (!cmModifiedsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmModifiedsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                List<CMItemsNodeData> nodeDataListToProcess = CmModifiedItemsBatchProcessingHelper.checkAndAddTreeDocumentsForPromote(
                        root, documentManager);
                if (nodeDataListToProcess.size() == 0) {
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
                            cmProcessDoc, CMBatchProcessingConstants.APPROVE,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager,
                            nodeDataListToProcess.size());

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
                            cmProcessDoc, nodeDataListToProcess,
                            eloraDocumentRelationManager, // ?????????
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");
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

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.processingPromote")
                            + ": " + e.getMessage());
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
            if (!cmModifiedsTreeBean.getIsDirty()) {

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                TreeNode root = cmModifiedsTreeBean.getRoot();
                toggleLockOnModifiedItems(lock, root);

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

                cmModifiedsTreeBean.createRoot();

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

    private void toggleLockOnModifiedItems(boolean lock, TreeNode root)
            throws EloraException {
        for (TreeNode modifiedItemNode : root.getChildren()) {
            CMItemsNodeData nodeData = (CMItemsNodeData) modifiedItemNode.getData();
            CmBatchProcessingHelper.toggleLockDoc(nodeData, lock, facesMessages,
                    messages, documentManager);

            for (TreeNode derivedItemNode : modifiedItemNode.getChildren()) {
                CMItemsNodeData derivedNodeData = (CMItemsNodeData) derivedItemNode.getData();
                CmBatchProcessingHelper.toggleLockDoc(derivedNodeData, lock,
                        facesMessages, messages, documentManager);
            }
        }
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
            if (!cmModifiedsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmModifiedsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                List<CMItemsNodeData> nodeDataListToProcess = checkAndAddTreeDocumentsForCheckout(
                        root);
                if (nodeDataListToProcess.size() == 0) {
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
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager,
                            nodeDataListToProcess.size());

                    // calculate what is the transition to come back to the
                    // current state
                    transitionToComeBackToPreviousState = LifecycleHelper.getTransitionToDestinationState(
                            cmProcessDoc, currentLifeCycleState);

                    if (transitionToComeBackToPreviousState == null) {
                        throw new EloraException(
                                "transitionToComeBackToPreviousState is null");
                    }

                    // Raise the asynchronous event
                    CheckoutService checkoutService = Framework.getService(
                            CheckoutService.class);

                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.CHECKOUT_MODIFIED_ITEMS,
                            cmProcessDoc, nodeDataListToProcess,
                            transitionToComeBackToPreviousState,
                            checkoutService);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKOUT_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");
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
                    "eloraplm.message.error.cm.batch.processingExecuteActions")
                    + ": " + e.getMessage());
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

    private List<CMItemsNodeData> checkAndAddTreeDocumentsForCheckout(
            TreeNode root) throws EloraException {

        List<CMItemsNodeData> nodeDataListToProcess = new ArrayList<CMItemsNodeData>();

        String logInitMsg = "[checkAndAddTreeDocumentsForCheckout] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {
            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();
            if (canNodeDataBeAddedToCheckout(childNodeData)) {
                nodeDataListToProcess.add(childNodeData);
            }
            for (TreeNode derivedNode : childNode.getChildren()) {
                CMItemsNodeData derivedNodeData = (CMItemsNodeData) derivedNode.getData();
                if (canNodeDataBeAddedToCheckout(derivedNodeData)) {
                    nodeDataListToProcess.add(derivedNodeData);
                }
            }
        }
        log.trace(logInitMsg + "|" + nodeDataListToProcess.size()
                + "| documents for checkout.");

        return nodeDataListToProcess;
    }

    private boolean canNodeDataBeAddedToCheckout(CMItemsNodeData nodeData)
            throws EloraException {

        if (!CmBatchProcessingHelper.isManaged(nodeData)
                && !CmBatchProcessingHelper.isIgnored(nodeData)
                && !CmBatchProcessingHelper.isRemoved(nodeData)
                && !EloraDocumentHelper.isCadDocument(
                        nodeData.getDestinationItem())) {

            DocumentModel destItem = nodeData.getDestinationItem();

            return checkDestinationItemLocked(destItem);
        }
        return false;
    }

    private boolean checkDestinationItemLocked(DocumentModel destItem)
            throws EloraException {
        try {
            EloraDocumentHelper.lockDocument(destItem);
            return true;
        } catch (DocumentAlreadyLockedException
                | DocumentInUnlockableStateException | EloraException
                | DocumentLockRightsException e) {

            String reference = destItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get(
                            "eloraplm.message.error.cm.batch.itemNotLocked"),
                    reference, destItem.getTitle());

            return false;
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
            if (!cmModifiedsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmModifiedsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                List<CMItemsNodeData> nodeDataListToProcess = checkAndAddTreeDocumentsForCheckin(
                        root);

                if (nodeDataListToProcess.size() == 0) {
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
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager,
                            nodeDataListToProcess.size());

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
                            cmProcessDoc, nodeDataListToProcess, checkinManager,
                            eloraDocumentRelationManager, // ???????
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKIN_MODIFIED_ITEMS
                            + "| Asynchronous Event fired.");
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

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.processingCheckin")
                            + ": " + e.getMessage());
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

    private List<CMItemsNodeData> checkAndAddTreeDocumentsForCheckin(
            TreeNode root) throws EloraException {

        List<CMItemsNodeData> nodeDataListToProcess = new ArrayList<>();

        String logInitMsg = "[checkAndAddTreeDocumentsForCheckin] ["
                + documentManager.getPrincipal().getName() + "] ";

        // Add first the derived nodes to do the checkin in this order
        // afterwards
        for (TreeNode childNode : root.getChildren()) {
            for (TreeNode derivedNode : childNode.getChildren()) {
                CMItemsNodeData derivedNodeData = (CMItemsNodeData) derivedNode.getData();
                if (canNodeDataBeAddedToCheckin(derivedNodeData)) {
                    nodeDataListToProcess.add(derivedNodeData);
                }
            }
            CMItemsNodeData childNodeData = (CMItemsNodeData) childNode.getData();
            if (canNodeDataBeAddedToCheckin(childNodeData)) {
                nodeDataListToProcess.add(childNodeData);
            }
        }

        log.trace(logInitMsg + "|" + nodeDataListToProcess.size()
                + "| documents for checkin.");

        return nodeDataListToProcess;
    }

    private boolean canNodeDataBeAddedToCheckin(CMItemsNodeData nodeData)
            throws EloraException {

        if (!CmBatchProcessingHelper.isIgnored(nodeData)
                && !CmBatchProcessingHelper.isRemoved(nodeData)
                && !CmBatchProcessingHelper.isManaged(nodeData)
                && CmBatchProcessingHelper.isCheckedOut(nodeData)
                && !EloraDocumentHelper.isCadDocument(
                        nodeData.getDestinationItem())) {
            return true;
        }
        return false;
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
            if (!cmModifiedsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmModifiedsTreeBean.getRoot();
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
                            CMConstants.ITEM_CLASS_MODIFIED, documentManager,
                            checkedOutDocs.size());

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
                    "eloraplm.message.error.cm.batch.processingUndoCheckout")
                    + ": " + e.getMessage());
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
            DocumentModel docToCheckout = getDocToBeCheckedOut(childNodeData);
            if (docToCheckout != null) {
                checkedOutDocs.add(docToCheckout);
            }

            for (TreeNode dreivedNode : childNode.getChildren()) {
                CMItemsNodeData derivedNodeData = (CMItemsNodeData) dreivedNode.getData();
                DocumentModel derivedDocToCheckout = getDocToBeCheckedOut(
                        derivedNodeData);
                if (derivedDocToCheckout != null) {
                    checkedOutDocs.add(derivedDocToCheckout);
                }
            }
        }
        return checkedOutDocs;
    }

    private DocumentModel getDocToBeCheckedOut(CMItemsNodeData nodeData) {

        if (!CmBatchProcessingHelper.isIgnored(nodeData)
                && !CmBatchProcessingHelper.isManaged(nodeData)
                && !EloraDocumentHelper.isCadDocument(
                        nodeData.getDestinationItem())) {

            DocumentModel destItem = nodeData.getDestinationItem();

            if (destItem.isCheckedOut()) {
                if (EloraDocumentHelper.isLockedByUserOrAdmin(destItem,
                        documentManager)) {
                    return destItem;
                }
            }
        }
        return null;
    }

}
