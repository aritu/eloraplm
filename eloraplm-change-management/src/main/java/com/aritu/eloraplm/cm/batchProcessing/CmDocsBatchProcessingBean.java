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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
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
import com.aritu.eloraplm.cm.treetable.DocImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("cmDocsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmDocsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmDocsBatchProcessingBean.class);

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
    protected transient DocImpactedItemsTreeBean cmDocImpactedItemsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    private Map<String, List<String>> childrenVersionSeriesMap;

    private DAG dag;

    private boolean forcePromotion;

    public CmDocsBatchProcessingBean() {
        forcePromotion = false;
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
            if (!cmDocImpactedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmDocImpactedItemsTreeBean.getRoot();
                List<String> modifiedDocsArchivedDestinationDocIds = checkModifiedDocsAreManagedAndReleased(
                        root);
                Map<String, String> repeatedTreeDocMap = new HashMap<>();
                List<String> sortedIds = sortTreeDocumentsForPromote(root,
                        modifiedDocsArchivedDestinationDocIds,
                        repeatedTreeDocMap);

                if (sortedIds == null || sortedIds.size() == 0) {
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
                            CMEventNames.CM_DOCS_BATCH_PROMOTE_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.APPROVE,
                            CMConstants.ITEM_TYPE_DOC,
                            CMConstants.ITEM_CLASS_IMPACTED, documentManager,
                            sortedIds.size());

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
                            CMBatchProcessingEventNames.PROMOTE_DOCS,
                            cmProcessDoc, root, sortedIds, dag,
                            childrenVersionSeriesMap,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState,
                            forcePromotion);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_DOCS
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

    public void checkoutBasicDocs() {
        String logInitMsg = "[checkoutBasicDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmDocImpactedItemsTreeBean.getIsDirty()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                CheckoutService checkoutService = Framework.getService(
                        CheckoutService.class);

                TreeNode root = cmDocImpactedItemsTreeBean.getRoot();

                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.checkoutBasicDocs(modifiedItemNode,
                            facesMessages, messages, checkoutService);
                }

                EloraEventHelper.fireEvent(
                        CMEventNames.CM_DOCS_BATCH_BASIC_DOC_CHECKOUT_EVENT,
                        navigationContext.getCurrentDocument());

                cmDocImpactedItemsTreeBean.createRoot();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.cm.batch.checkoutBasicDocs"));

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.checkoutBasicDocs"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.checkoutBasicDocs"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void checkinBasicDocs() {
        String logInitMsg = "[checkinBasicDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmDocImpactedItemsTreeBean.getIsDirty()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                CheckinManager checkinManager = Framework.getService(
                        CheckinManager.class);

                TreeNode root = cmDocImpactedItemsTreeBean.getRoot();

                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.checkinBasicDocs(modifiedItemNode,
                            checkinManager);
                }

                EloraEventHelper.fireEvent(
                        CMEventNames.CM_DOCS_BATCH_BASIC_DOC_CHECKIN_EVENT,
                        navigationContext.getCurrentDocument());

                cmDocImpactedItemsTreeBean.createRoot();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.cm.batch.checkinBasicDocs"));

            } else {
                log.trace(logInitMsg + "Unsaved changes.");
                facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                        "eloraplm.message.warning.treetable.unsavedChanges"));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.checkinBasicDocs"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.checkinBasicDocs"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<String> sortTreeDocumentsForPromote(TreeNode root,
            List<String> modifiedDocsArchivedDestinationDocIds,
            Map<String, String> repeatedTreeDocMap)
            throws EloraException, CycleDetectedException {
        String logInitMsg = "[sortTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        dag = new DAG();

        childrenVersionSeriesMap = new HashMap<>();

        log.trace(logInitMsg + "Proceeding to build promote DAG");
        for (TreeNode modifiedDocNode : root.getChildren()) {

            DocumentModel modifiedItemArchivedDoc = null;
            // As modified items must be managed to promote impacted items,
            // destination item will be AV
            ImpactedItemsNodeData modifiedDocNodeData = (ImpactedItemsNodeData) modifiedDocNode.getData();
            String modifiedDocNodeDataAction = modifiedDocNodeData.getAction();
            if (modifiedDocNodeDataAction.equals(CMConstants.ACTION_REMOVE)) {
                modifiedItemArchivedDoc = ((ImpactedItemsNodeData) modifiedDocNode.getData()).getOriginItem();
            } else {
                modifiedItemArchivedDoc = ((ImpactedItemsNodeData) modifiedDocNode.getData()).getDestinationItem();
            }

            buildPromoteDAG(modifiedDocNode, modifiedItemArchivedDoc,
                    repeatedTreeDocMap);
        }
        List<String> sortedIds = TopologicalSorter.sort(dag);

        List<String> newSortedIds = new LinkedList<String>();

        // Exclude modified documents and documents having null sortedId from
        // the sortedIds list
        for (Iterator<String> iterator = sortedIds.iterator(); iterator.hasNext();) {
            String sortedId = iterator.next();
            if (sortedId != null
                    && !modifiedDocsArchivedDestinationDocIds.contains(
                            sortedId)) {
                newSortedIds.add(sortedId);
            }
        }

        log.trace(logInitMsg + "Size of Sorted IDs for Promote DAG: |"
                + newSortedIds.size() + "|");

        log.trace(logInitMsg + "--- EXIT --- ");

        return newSortedIds;
    }

    /**
     * Checks if modified documents are managed and released. If not, it throws
     * an exception. If all modified documents are managed and released, it
     * returns a list containing the destinationItem docId of modified
     * documents.
     *
     * @param root
     * @return
     * @throws EloraException
     */
    private List<String> checkModifiedDocsAreManagedAndReleased(TreeNode root)
            throws EloraException {

        List<String> modifiedDocsArchivedDestinationDocIds = new ArrayList<String>();

        String logInitMsg = "[checkModifiedDocsAreManagedAndReleased] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode modifiedDocNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedDocNode.getData();

            if (nodeData.getType().equals(CMConstants.ITEM_TYPE_DOC)) {
                checkModifiedAndReleased(modifiedDocsArchivedDestinationDocIds,
                        nodeData);
            } else {
                for (TreeNode modifiedDerivedNode : modifiedDocNode.getChildren()) {
                    ImpactedItemsNodeData modifiedDerivedNodeData = (ImpactedItemsNodeData) modifiedDerivedNode.getData();
                    checkModifiedAndReleased(
                            modifiedDocsArchivedDestinationDocIds,
                            modifiedDerivedNodeData);
                }
            }
            log.trace(logInitMsg + "Modified items are managed and released");
        }
        return modifiedDocsArchivedDestinationDocIds;
    }

    private void checkModifiedAndReleased(
            List<String> modifiedDocsArchivedDestinationDocIds,
            ImpactedItemsNodeData nodeData) throws EloraException {
        // if the modified document is managed, the destination must be AV,
        // excepting if the action is remove or ignore, since in these cases
        // destination is empty.
        // We check it here, to be sure that everything is ok.
        if (CmBatchProcessingHelper.isRemoved(nodeData)
                || CmBatchProcessingHelper.isIgnored(nodeData)) {
            // If action is remove or ignore, add the originId to filter the
            // documents that should not be processed
            DocumentModel modifiedDocOriginDoc = nodeData.getOriginItem();
            modifiedDocsArchivedDestinationDocIds.add(
                    modifiedDocOriginDoc.getId());
        } else {
            DocumentModel modifiedDocDestinationDoc = nodeData.getDestinationItem();
            if (!CmBatchProcessingHelper.isManaged(nodeData)
                    || !modifiedDocDestinationDoc.isVersion()) {
                log.error("All modified documents must be managed");
                throw new EloraException(messages.get(
                        "eloraplm.message.error.cm.batch.modifiedDocNotManaged"));
            } else if (!EloraDocumentHelper.isReleased(
                    modifiedDocDestinationDoc)) {
                log.error("All modified documents must be released");
                throw new EloraException(messages.get(
                        "eloraplm.message.error.cm.batch.modifiedDocNotReleased"));
            } else {
                modifiedDocsArchivedDestinationDocIds.add(
                        modifiedDocDestinationDoc.getId());
            }
        }
    }

    // TODO: ¿Hay que tener en cuenta si los documentos estan ignored para
    // comprobar si estan repetidos?
    private void checkRepeatedDocs(DocumentModel doc,
            Map<String, String> repeatedTreeDocMap) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            log.error("Document repeated with different version");
            Object[] messageParams = {
                    doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    doc.getTitle() };
            String errorMsg = EloraMessageHelper.getTranslatedMessage(
                    documentManager,
                    "eloraplm.message.error.cm.batch.docRepeatedWithDiffVersion",
                    messageParams);
            throw new EloraException(errorMsg);
        } else {
            repeatedTreeDocMap.put(doc.getVersionSeriesId(), doc.getId());
        }
    }

    private void buildPromoteDAG(TreeNode node, DocumentModel structureChildDoc,
            Map<String, String> repeatedTreeDocMap)
            throws EloraException, CycleDetectedException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (nodeData.getIsModifiedItem()) {
                DocumentModel structureParentDoc = CmBatchProcessingHelper.getArchivedDestinationDoc(
                        nodeData);
                buildPromoteDAG(childNode, structureParentDoc,
                        repeatedTreeDocMap);
            } else {
                if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                    DocumentModel structureParentDoc = CmBatchProcessingHelper.getArchivedDestinationDoc(
                            nodeData);
                    structureParentDoc.refresh();
                    checkRepeatedDocs(structureParentDoc, repeatedTreeDocMap);
                    if (!EloraDocumentHelper.isReleased(structureParentDoc)) {
                        dag.addVertex(structureParentDoc.getId());
                        if (nodeData.getIsAnarchic()
                                || EloraDocumentHelper.isReleased(
                                        structureChildDoc)) {
                            dag.addEdge(structureParentDoc.getId(), null);
                        } else {
                            String structureParentDocId;
                            String structureChildDocId;
                            String structureChildDocVersionSeriesId;
                            if (nodeData.getIsDirectObject()) {
                                structureParentDocId = structureChildDoc.getId();
                                structureChildDocId = structureParentDoc.getId();
                                structureChildDocVersionSeriesId = structureParentDoc.getVersionSeriesId();
                            } else {
                                structureParentDocId = structureParentDoc.getId();
                                structureChildDocId = structureChildDoc.getId();
                                structureChildDocVersionSeriesId = structureChildDoc.getVersionSeriesId();
                            }
                            dag.addEdge(structureParentDocId,
                                    structureChildDocId);
                            CmBatchProcessingHelper.addChildVersionSeriesId(
                                    structureParentDocId,
                                    structureChildDocVersionSeriesId,
                                    childrenVersionSeriesMap);
                        }
                    }
                    buildPromoteDAG(childNode, structureParentDoc,
                            repeatedTreeDocMap);
                }
            }
        }
    }

    public void toggleLockAll(boolean lock) {
        String logInitMsg = "[toggleLockAll] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmDocImpactedItemsTreeBean.getIsDirty()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                TreeNode root = cmDocImpactedItemsTreeBean.getRoot();

                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.toggleLockProcessableDocs(
                            modifiedItemNode, lock, facesMessages, messages,
                            documentManager);
                }

                // Log Nuxeo event
                if (lock) {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_DOCS_BATCH_LOCK_EVENT,
                            navigationContext.getCurrentDocument());
                } else {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_DOCS_BATCH_UNLOCK_EVENT,
                            navigationContext.getCurrentDocument());
                }

                cmDocImpactedItemsTreeBean.createRoot();

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

    public boolean getForcePromotion() {
        return forcePromotion;
    }

    public void setForcePromotion(boolean forcePromotion) {
        this.forcePromotion = forcePromotion;
    }

}
