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
import com.aritu.eloraplm.cm.treetable.DocModifiedItemsTreeBean;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("cmModifiedDocsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmModifiedDocsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmModifiedDocsBatchProcessingBean.class);

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
    protected transient DocModifiedItemsTreeBean cmDocModifiedItemsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public CmModifiedDocsBatchProcessingBean() {
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
            if (!cmDocModifiedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmDocModifiedItemsTreeBean.getRoot();
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
                            CMEventNames.CM_MODIF_DOCS_BATCH_PROMOTE_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.PROMOTE,
                            CMConstants.ITEM_TYPE_DOC,
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
                            CMBatchProcessingEventNames.PROMOTE_MODIFIED_DOCS,
                            cmProcessDoc, root, eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_MODIFIED_DOCS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.PROMOTE);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_DOC,
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
            if (!cmDocModifiedItemsTreeBean.getIsDirty()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                TreeNode root = cmDocModifiedItemsTreeBean.getRoot();

                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.toggleLockDoc(modifiedItemNode,
                            lock, facesMessages, messages, documentManager);
                }

                // Log Nuxeo event
                if (lock) {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_DOCS_BATCH_LOCK_EVENT,
                            navigationContext.getCurrentDocument());
                } else {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_MODIF_DOCS_BATCH_UNLOCK_EVENT,
                            navigationContext.getCurrentDocument());
                }

                cmDocModifiedItemsTreeBean.createRoot();

                String message = lock
                        ? "eloraplm.message.success.cm.batch.lockAllDocs"
                        : "eloraplm.message.success.cm.batch.unlockAllDocs";
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
                    ? "eloraplm.message.error.cm.batch.lockAllDocs"
                    : "eloraplm.message.error.cm.batch.unlockAllDocs";
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(message));

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            String message = lock
                    ? "eloraplm.message.error.cm.batch.lockAllDocs"
                    : "eloraplm.message.error.cm.batch.unlockAllDocs";
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(message));

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
