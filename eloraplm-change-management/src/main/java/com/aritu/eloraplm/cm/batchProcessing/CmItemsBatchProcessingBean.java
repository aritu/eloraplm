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
import com.aritu.eloraplm.cm.batchProcessing.util.DAGManager;
import com.aritu.eloraplm.cm.treetable.BomImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("cmItemsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmItemsBatchProcessingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmItemsBatchProcessingBean.class);

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
    protected transient BomImpactedItemsTreeBean cmBomImpactedItemsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    private Map<String, List<String>> childrenVersionSeriesMap;

    private List<String> bomHierarchicalAndDirectRelations;

    private boolean keepLock;

    private boolean forcePromotion;

    // private DAG dag;

    // private Map<String, String> helperCommentMap;

    public CmItemsBatchProcessingBean() {
        keepLock = false;
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
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                List<String> modifiedItemsArchivedDestinationDocIds = checkModifiedItemsAreManagedAndReleased(
                        root);
                DAGManager dagManager = new DAGManager();
                Map<String, String> repeatedTreeDocMap = new HashMap<>();
                List<String> sortedIds = sortTreeDocumentsForPromote(root,
                        dagManager, modifiedItemsArchivedDestinationDocIds,
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
                            CMEventNames.CM_ITEMS_BATCH_PROMOTE_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.APPROVE,
                            CMConstants.ITEM_TYPE_BOM,
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
                            CMBatchProcessingEventNames.PROMOTE_ITEMS,
                            cmProcessDoc, root, sortedIds, dagManager.getDag(),
                            childrenVersionSeriesMap,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState,
                            forcePromotion);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_ITEMS
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

    private List<String> sortTreeDocumentsForPromote(TreeNode root,
            DAGManager dagManager,
            List<String> modifiedItemsArchivedDestinationDocIds,
            Map<String, String> repeatedTreeDocMap)
            throws EloraException, CycleDetectedException {
        String logInitMsg = "[sortTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        childrenVersionSeriesMap = new HashMap<>();

        log.trace(logInitMsg + "Proceeding to build promote DAG");
        for (TreeNode modifiedItemNode : root.getChildren()) {
            buildPromoteDAG(modifiedItemNode, dagManager, repeatedTreeDocMap);
        }
        @SuppressWarnings("unchecked")
        List<String> sortedIds = dagManager.topologicalSort();

        List<String> newSortedIds = excludeNullIdItemsAndModifieds(
                modifiedItemsArchivedDestinationDocIds, sortedIds);

        log.trace(logInitMsg + "Size of Sorted IDs for Promote DAG: |"
                + newSortedIds.size() + "|");

        log.trace(logInitMsg + "--- EXIT --- ");
        return newSortedIds;
    }

    private List<String> excludeNullIdItemsAndModifieds(
            List<String> modifiedItemsArchivedDestinationDocIds,
            List<String> sortedIds) {
        List<String> newSortedIds = new LinkedList<String>();

        // Exclude modified items and items having null sortedId from the
        // sortedIds list
        for (Iterator<String> iterator = sortedIds.iterator(); iterator.hasNext();) {
            String sortedId = iterator.next();
            if (sortedId != null
                    && !modifiedItemsArchivedDestinationDocIds.contains(
                            sortedId)) {
                newSortedIds.add(sortedId);
            }
        }
        return newSortedIds;
    }

    private void buildPromoteDAG(TreeNode node, DAGManager dagManager,
            Map<String, String> repeatedTreeDocMap)
            throws EloraException, CycleDetectedException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (nodeData.getIsModifiedItem()) {
                buildPromoteDAG(childNode, dagManager, repeatedTreeDocMap);
            } else {
                ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) node.getData();
                if (nodeData.getIsDirectObject()) {
                    createPromoteVertexAndEdges(childNode, dagManager,
                            parentNodeData, nodeData, true, repeatedTreeDocMap);
                } else {
                    createPromoteVertexAndEdges(childNode, dagManager, nodeData,
                            parentNodeData, false, repeatedTreeDocMap);
                }
            }
        }
    }

    private void createPromoteVertexAndEdges(TreeNode childNode,
            DAGManager dagManager, ImpactedItemsNodeData subjectNodeData,
            ImpactedItemsNodeData objectNodeData, boolean isDirectObject,
            Map<String, String> repeatedTreeDocMap)
            throws EloraException, CycleDetectedException {

        DocumentModel object = null;
        if (isDirectObject) {
            if (!CmBatchProcessingHelper.isIgnored(objectNodeData)) {
                object = CmBatchProcessingHelper.getArchivedDestinationDoc(
                        objectNodeData);
                if (!EloraDocumentHelper.isReleased(object)) {
                    dagManager.addVertex(object.getId(), objectNodeData);
                }
            } else {
                object = objectNodeData.getOriginItemWc();
            }
        } else {
            if (CmBatchProcessingHelper.isRemoved(objectNodeData)) {
                object = objectNodeData.getOriginItemWc();
            } else {
                object = objectNodeData.getOriginItem();
                // If it is null is the modified node of group of deriveds
                if (object != null) {
                    object = CmBatchProcessingHelper.getArchivedDestinationDoc(
                            objectNodeData);
                }
            }
        }

        if ((isDirectObject && !CmBatchProcessingHelper.isIgnored(objectNodeData))
                || (!isDirectObject && !CmBatchProcessingHelper.isIgnored(subjectNodeData))) {
            DocumentModel subject = null;
            // Subject node can have remove action, only if isDirectObject ==
            // true
            if (CmBatchProcessingHelper.isRemoved(subjectNodeData)) {
                subject = subjectNodeData.getOriginItem();
            } else {
                subject = CmBatchProcessingHelper.getArchivedDestinationDoc(
                        subjectNodeData);

            }
            subject.refresh();
            if (!EloraDocumentHelper.isReleased(subject)
                    || CmBatchProcessingHelper.isRemoved(subjectNodeData)) {
                checkRepeatedDocs(subject, repeatedTreeDocMap);
                dagManager.addVertex(subject.getId(), subjectNodeData);
                // DIRECT: hay que mirar la relacion inversa
                if ((!isDirectObject && subjectNodeData.getIsAnarchic())
                        || (object != null
                                && EloraDocumentHelper.isReleased(object))
                        || CmBatchProcessingHelper.isRemoved(objectNodeData)
                        || subjectNodeData.getIsDerived()) {
                    dagManager.addEdge(subject.getId(), null);
                } else {
                    dagManager.addEdge(subject.getId(), object.getId());
                    CmBatchProcessingHelper.addChildVersionSeriesId(
                            subject.getId(), object.getVersionSeriesId(),
                            childrenVersionSeriesMap);
                }
            }
            buildPromoteDAG(childNode, dagManager, repeatedTreeDocMap);
        }
    }

    public void toggleLockAll(boolean lock) {
        String logInitMsg = "[toggleLockAll] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                for (TreeNode modifiedItemNode : root.getChildren()) {
                    CmBatchProcessingHelper.toggleLockProcessableDocs(
                            modifiedItemNode, lock, facesMessages, messages,
                            documentManager);
                }

                // Log Nuxeo event
                if (lock) {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_ITEMS_BATCH_LOCK_EVENT,
                            navigationContext.getCurrentDocument());
                } else {
                    EloraEventHelper.fireEvent(
                            CMEventNames.CM_ITEMS_BATCH_UNLOCK_EVENT,
                            navigationContext.getCurrentDocument());
                }

                cmBomImpactedItemsTreeBean.createRoot();

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

    public void executeActions() {
        String logInitMsg = "[executeActions] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // TODO: Añadir el chequeo de si existen items caracterizados y
            // mostrar pantalla con el aviso de que hay que cambiarlos a mano!!

            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                bomHierarchicalAndDirectRelations = new ArrayList<>(
                        RelationsConfig.bomHierarchicalRelationsList);

                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                checkModifiedItemsAreManagedAndMajorReleased(root);

                Map<String, String> destItemsSourceVersions = new HashMap<>();
                int totalDocuments = checkAndcountTreeDocumentsForExecuteActions(
                        root, destItemsSourceVersions);

                if (totalDocuments == 0) {
                    log.trace(logInitMsg + "Nothing to be executed.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingToBeExecuted"));
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
                            CMEventNames.CM_ITEMS_BATCH_EXECUTE_ACTIONS_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc,
                            CMBatchProcessingConstants.EXECUTE_ACTIONS,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_IMPACTED, documentManager,
                            totalDocuments);

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
                            CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS,
                            cmProcessDoc, root,
                            bomHierarchicalAndDirectRelations,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState,
                            checkoutService);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS
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

    /**
     * Checks if modified items are managed and major released. If not, it
     * throws an exception. If all modified items are managed and major
     * released, it returns a list containing the destinationItem docId of
     * modified items.
     *
     * @param root
     * @throws EloraException
     */
    // TODO: Aqui no se comprueba nada de obsolete. Se supone que se controlara
    // en el checkin y no podra quedar una pieza mal
    private void checkModifiedItemsAreManagedAndMajorReleased(TreeNode root)
            throws EloraException {

        String logInitMsg = "[checkModifiedItemsAreManagedAndMajorReleased] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();

            if (nodeData.getType().equals(CMConstants.ITEM_TYPE_BOM)
                    && nodeData.getOriginItem() == null
                    && nodeData.getDestinationItem() == null) {
                // Deriveds group modified
                continue;
            }

            if (nodeData.getType().equals(CMConstants.ITEM_TYPE_BOM)
                    && !nodeData.getAction().equals(
                            CMConstants.ACTION_IGNORE)) {
                checkManagedAndMajorReleased(nodeData);
            } else {
                for (TreeNode modifiedDerivedNode : modifiedItemNode.getChildren()) {
                    ImpactedItemsNodeData modifiedDerivedNodeData = (ImpactedItemsNodeData) modifiedDerivedNode
                            .getData();
                    if (modifiedDerivedNodeData.getType().equals(
                            CMConstants.ITEM_TYPE_BOM)
                            && modifiedDerivedNodeData.getOriginItem() == null
                            && modifiedDerivedNodeData.getDestinationItem() == null) {
                        // Deriveds group modified
                        continue;
                    }

                    if (modifiedDerivedNodeData.getType().equals(
                            CMConstants.ITEM_TYPE_BOM)
                            && !modifiedDerivedNodeData.getAction().equals(
                                    CMConstants.ACTION_IGNORE)) {
                        checkManagedAndMajorReleased(modifiedDerivedNodeData);
                    }
                }
            }
        }
        log.trace(logInitMsg
                + "All modified items are managed and there is not another version released in their major");
    }

    private void checkManagedAndMajorReleased(ImpactedItemsNodeData nodeData)
            throws EloraException {
        if (!CmBatchProcessingHelper.isManaged(nodeData)) {
            log.error("All modified items must be managed");
            throw new EloraException(messages.get(
                    "eloraplm.message.error.cm.batch.modifiedItemNotManaged"));
        } else {
            String action = nodeData.getAction();
            if (action != null && !action.equals(CMConstants.ACTION_REMOVE)) {

                DocumentModel destinationDoc = nodeData.getDestinationItem();
                DocumentModel releasedDoc = EloraDocumentHelper.getMajorReleasedVersion(
                        destinationDoc);
                if (releasedDoc != null && !releasedDoc.getId().equals(
                        destinationDoc.getId())) {
                    log.error(
                            "All modified items must be released or they can't have another version released in the same major");
                    throw new EloraException(messages.get(
                            "eloraplm.message.error.cm.batch.modifiedItemNotReleasedOrAnotherReleased"));
                }
            }
        }
    }

    private int checkAndcountTreeDocumentsForExecuteActions(TreeNode root,
            Map<String, String> destItemsSourceVersions) throws EloraException {
        int totalDocs = 0;

        String logInitMsg = "[checkAndcountTreeDocumentsForExecuteActions] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {
            ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();
            totalDocs = countTreeDocuments(totalDocs, childNode, childNodeData,
                    destItemsSourceVersions);
        }
        log.trace(logInitMsg + "There are no conflicts in tree with.");

        return totalDocs;
    }

    private int countTreeDocuments(int totalDocs, TreeNode childNode,
            ImpactedItemsNodeData childNodeData,
            Map<String, String> destItemsSourceVersions) throws EloraException {
        // modified items
        if (childNodeData.getIsModifiedItem()) {
            if (!CmBatchProcessingHelper.isIgnored(childNodeData)) {
                totalDocs += checkAndcountTreeDocumentsForExecuteActions(
                        childNode, destItemsSourceVersions);
            }
        } else {
            if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                    && !CmBatchProcessingHelper.isManaged(childNodeData)) {

                DocumentModel destItem = childNodeData.getDestinationItem();
                checkDestinationItemLocked(destItem);
                checkDestItemsSourceVersions(destItem,
                        childNodeData.getDestinationItemSourceVersion(),
                        destItemsSourceVersions);
                totalDocs++;
            }
            totalDocs += checkAndcountTreeDocumentsForExecuteActions(childNode,
                    destItemsSourceVersions);
        }
        return totalDocs;
    }

    private void checkDestinationItemLocked(DocumentModel destItem)
            throws EloraException {
        if (!EloraDocumentHelper.isLockedByUserOrAdmin(destItem,
                documentManager)) {

            String reference = destItem.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            log.error("Item with id |" + destItem.getId()
            + "| and reference |" + reference + "| is not locked.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.cm.batch.allItemsWithActionMustBeLocked"));
            Object[] messageParams = { reference, destItem.getTitle() };
            String errorMsg = EloraMessageHelper.getTranslatedMessage(
                    documentManager,
                    "eloraplm.message.error.cm.batch.itemNotLocked",
                    messageParams);
            throw new EloraException(errorMsg);
        }
    }

    private void checkRepeatedDocs(DocumentModel doc,
            Map<String, String> repeatedTreeDocMap) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            log.error("Document repeated with different version");
            throw new EloraException(messages.get(
                    "eloraplm.message.error.cm.batch.itemRepeatedWithDiffVersion"));
        } else {
            repeatedTreeDocMap.put(doc.getVersionSeriesId(), doc.getId());
        }
    }

    /**
     * Checks if there are different destination source versions for the same
     * destination item.
     *
     * @param destItem destination item of the impacted item
     * @param sourceVersion destination source version of the impacted item
     * @param destItemsSourceVersions list storing different destination items
     *            and theirs source version
     * @throws EloraException
     */
    private void checkDestItemsSourceVersions(DocumentModel destItem,
            DocumentModel sourceVersion,
            Map<String, String> destItemsSourceVersions) throws EloraException {

        if (sourceVersion == null) {
            sourceVersion = destItem;
        }
        if (destItemsSourceVersions.containsKey(destItem.getId())) {
            String sourceVersionId = destItemsSourceVersions.get(
                    destItem.getId());
            if (!sourceVersionId.equals(sourceVersion.getId())) {
                String reference = destItem.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                log.error("Item with id |" + destItem.getId()
                + "| and reference |" + reference + "| is not locked.");
                Object[] messageParams = { reference, destItem.getTitle() };
                String errorMsg = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.destinationItemsWithDifferentSourceVersion",
                        messageParams);
                throw new EloraException(errorMsg);
            }
        } else {
            destItemsSourceVersions.put(destItem.getId(),
                    sourceVersion.getId());
        }
    }

    // TODO: Comprobar que los errores de los chequeos se muestran en pantalla!
    public void checkin() {
        String logInitMsg = "[checkin] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean lifecycleStateChanged = false;
        String transitionToComeBackToPreviousState = null;
        DocumentModel cmProcessDoc = null;

        try {
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                checkModifiedItemsAreManagedAndMajorReleased(root);
                DAGManager dagManager = new DAGManager();

                List<String> sortedIds = sortTreeDocumentsForCheckin(logInitMsg,
                        root, dagManager);

                if (sortedIds == null || sortedIds.size() == 0) {
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
                            CMEventNames.CM_ITEMS_BATCH_CHECK_IN_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.CHECKIN,
                            CMConstants.ITEM_TYPE_BOM,
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
                    CheckinManager checkinManager = Framework.getService(
                            CheckinManager.class);

                    Events.instance().raiseAsynchronousEvent(
                            CMBatchProcessingEventNames.CHECKIN_ITEMS,
                            cmProcessDoc, sortedIds, dagManager.getNodeMap(),
                            checkinManager, eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState, keepLock);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKIN_ITEMS
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

    private List<String> sortTreeDocumentsForCheckin(String logInitMsg,
            TreeNode root, DAGManager dagManager)
            throws CycleDetectedException, EloraException {
        log.trace(logInitMsg + "Proceeding to build checkin DAG");
        // dag = new DAG();

        // helperCommentMap = new HashMap<String, String>();
        for (TreeNode modifiedItemNode : root.getChildren()) {
            buildCheckinDAG(modifiedItemNode, dagManager);
        }

        List<String> sortedIds = dagManager.topologicalSort();

        // @SuppressWarnings("unchecked")
        // List<String> sortedIds = TopologicalSorter.sort(dag);

        List<String> newSortedIds = excludeNullIds(sortedIds);

        log.trace(logInitMsg + "Size of Sorted IDs for Checkin DAG: |"
                + newSortedIds.size() + "|");

        return newSortedIds;
    }

    private List<String> excludeNullIds(List<String> sortedIds) {
        List<String> newSortedIds = new LinkedList<String>();
        for (Iterator<String> iterator = sortedIds.iterator(); iterator.hasNext();) {
            String sortedId = iterator.next();
            if (sortedId != null) {
                newSortedIds.add(sortedId);
            }
        }
        return newSortedIds;
    }

    /**
     * Checks if modified items are managed and released. If not, it throws an
     * exception. If all modified items are managed and released, it returns a
     * list containing the destinationItem docId of modified items.
     *
     * @param root
     * @return
     * @throws EloraException
     */
    private List<String> checkModifiedItemsAreManagedAndReleased(TreeNode root)
            throws EloraException {

        List<String> modifiedItemsArchivedDestinationDocIds = new ArrayList<String>();

        String logInitMsg = "[checkModifiedItemsAreManagedAndReleased] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();

            if (nodeData.getType().equals(CMConstants.ITEM_TYPE_BOM)
                    && nodeData.getOriginItem() == null
                    && nodeData.getDestinationItem() == null) {
                // Deriveds group modified
                continue;
            }

            if (nodeData.getType().equals(CMConstants.ITEM_TYPE_BOM)) {
                checkManagedAndReleased(modifiedItemsArchivedDestinationDocIds,
                        nodeData);
            } else {
                for (TreeNode modifiedDerivedNode : modifiedItemNode.getChildren()) {
                    ImpactedItemsNodeData modifiedDerivedNodeData = (ImpactedItemsNodeData) modifiedDerivedNode
                            .getData();

                    if (modifiedDerivedNodeData.getType().equals(
                            CMConstants.ITEM_TYPE_BOM)
                            && modifiedDerivedNodeData.getOriginItem() == null
                            && modifiedDerivedNodeData.getDestinationItem() == null) {
                        // Deriveds group modified
                        continue;
                    }

                    checkManagedAndReleased(
                            modifiedItemsArchivedDestinationDocIds,
                            modifiedDerivedNodeData);
                }
            }
        }
        log.trace(logInitMsg + "Modified items are managed and released");
        return modifiedItemsArchivedDestinationDocIds;

    }

    private void checkManagedAndReleased(
            List<String> modifiedItemsArchivedDestinationDocIds,
            ImpactedItemsNodeData nodeData) throws EloraException {
        // if the modified item is managed, the destination must be AV,
        // excepting if the action is remove or ignore, since in these cases
        // destination is empty.
        // We check here, to be sure that everything is ok.
        if (CmBatchProcessingHelper.isRemoved(nodeData)
                || CmBatchProcessingHelper.isIgnored(nodeData)) {
            // If action is remove or ignore, add the originId to filter the
            // items that should not be processed
            DocumentModel modifiedItemOriginDoc = nodeData.getOriginItem();
            modifiedItemsArchivedDestinationDocIds.add(
                    modifiedItemOriginDoc.getId());
        } else {
            DocumentModel modifiedItemDestinationDoc = nodeData.getDestinationItem();
            if (!CmBatchProcessingHelper.isManaged(nodeData)
                    || !modifiedItemDestinationDoc.isVersion()) {
                log.error("All modified items must be managed");
                throw new EloraException(messages.get(
                        "eloraplm.message.error.cm.batch.modifiedItemNotManaged"));
            } else {
                if (!EloraDocumentHelper.isReleased(
                        modifiedItemDestinationDoc)) {
                    log.error("All modified items must be released");
                    throw new EloraException(messages.get(
                            "eloraplm.message.error.cm.batch.modifiedItemNotReleased"));
                }
            }
            modifiedItemsArchivedDestinationDocIds.add(
                    modifiedItemDestinationDoc.getId());
        }
    }

    private void buildCheckinDAG(TreeNode node, DAGManager dagManager)
            throws CycleDetectedException, EloraException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (nodeData.getIsModifiedItem()) {
                buildCheckinDAG(childNode, dagManager);
                continue;
            }
            ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) node.getData();
            if (nodeData.getIsDirectObject()) {
                createCheckinVertexAndEdges(childNode, dagManager,
                        parentNodeData, nodeData, true);
            } else {
                createCheckinVertexAndEdges(childNode, dagManager, nodeData,
                        parentNodeData, false);
            }
        }
    }

    private void createCheckinVertexAndEdges(TreeNode childNode,
            DAGManager dagManager, ImpactedItemsNodeData subjectNodeData,
            ImpactedItemsNodeData objectNodeData, boolean isDirectObject)
            throws CycleDetectedException, EloraException {

        DocumentModel object = null;
        DocumentModel objectOrigin = null;
        if (isDirectObject) {
            if (!CmBatchProcessingHelper.isIgnored(objectNodeData)) {
                object = objectNodeData.getDestinationItem();
                objectOrigin = objectNodeData.getOriginItem();
                if (object.isCheckedOut()) {
                    dagManager.addVertex(object.getId(), objectNodeData);
                    dagManager.addEdge(object.getId(), null);

                    // dag.addVertex(object.getId());
                    // dag.addEdge(object.getId(), null);
                    // helperCommentMap.put(object.getId(),
                    // objectNodeData.getComment());
                }
            } else {
                object = objectNodeData.getOriginItemWc();
            }
        } else {
            if (CmBatchProcessingHelper.isRemoved(objectNodeData)) {
                object = objectNodeData.getOriginItemWc();
            } else {
                object = objectNodeData.getDestinationItem();
            }
        }

        if (!CmBatchProcessingHelper.isIgnored(subjectNodeData)) {
            DocumentModel subject = null;
            // Subject node can have remove action, only if isDirectObject ==
            // true
            if (CmBatchProcessingHelper.isRemoved(subjectNodeData)) {
                subject = subjectNodeData.getOriginItemWc();
            } else {
                subject = subjectNodeData.getDestinationItem();
            }
            subject.refresh();
            if (!subjectNodeData.getIsManaged()) {
                if (subject.isCheckedOut()) {
                    if (EloraDocumentHelper.isLockedByUserOrAdmin(subject,
                            documentManager)) {
                        if (object != null || objectOrigin == null) {
                            // objectOrigin == null is when the modified node is
                            // the group of deriveds. We need to add vertex in
                            // this case.
                            // object != null is that Modified is not removed.
                            // When direct never happens object == null
                            String predicate = null;
                            if (isDirectObject) {
                                predicate = objectNodeData.getPredicate();
                            } else {
                                predicate = subjectNodeData.getPredicate();
                            }
                            if ((!isDirectObject
                                    && subjectNodeData.getIsAnarchic())
                                    || CmBatchProcessingHelper.isRemoved(
                                            objectNodeData)
                                    || subjectNodeData.getIsDerived()
                                    || EloraRelationHelper.existsRelation(
                                            subject, object, predicate,
                                            documentManager)
                                    || subjectNodeData.getIsFromMbom()) {
                                // TODO: Igual hay que mirar si el vertice
                                // existe de antes
                                dagManager.addVertex(subject.getId(),
                                        subjectNodeData);
                                if (!subjectNodeData.getIsDerived()
                                        && !subjectNodeData.getIsFromMbom()
                                        && object.isCheckedOut()) {
                                    // Igual hay que mirar si el edge existe
                                    // de antes
                                    dagManager.addEdge(subject.getId(),
                                            object.getId());
                                } else {
                                    dagManager.addEdge(subject.getId(), null);
                                }
                            } else {
                                log.error(
                                        "Relation in impacted tree does not exist in checked out document |"
                                                + subject.getPropertyValue(
                                                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                                + "| id = |" + subject.getId()
                                                + "|");
                                Object[] messageParams = {
                                        subject.getPropertyValue(
                                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                        subject.getTitle(),
                                        object.getPropertyValue(
                                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                        object.getTitle() };
                                String errorMsg = EloraMessageHelper.getTranslatedMessage(
                                        documentManager,
                                        "eloraplm.message.error.cm.batch.itemRelationNoExists",
                                        messageParams);
                                throw new EloraException(errorMsg);
                            }
                        }
                    } else {
                        log.error("Checked out document must be locked by user");
                        Object[] messageParams = { subject.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                subject.getTitle() };
                        String errorMsg = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.itemNotLocked",
                                messageParams);
                        throw new EloraException(errorMsg);
                    }
                }
            }
            buildCheckinDAG(childNode, dagManager);
        }
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
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
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
                            CMEventNames.CM_ITEMS_BATCH_UNDO_CHECKOUT_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc,
                            CMBatchProcessingConstants.UNDO_CHECKOUT,
                            CMConstants.ITEM_TYPE_BOM,
                            CMConstants.ITEM_CLASS_IMPACTED, documentManager,
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
                            CMBatchProcessingEventNames.UNDO_CHECKOUT_ITEMS,
                            cmProcessDoc, checkedOutDocs,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.UNDO_CHECKOUT_ITEMS
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

            ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

            // Don't take into account modified items
            if (!childNodeData.getIsModifiedItem()) {

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

            checkedOutDocs.addAll(getCheckedOutDocuments(childNode));
        }

        return checkedOutDocs;
    }

    public boolean getKeepLock() {
        return keepLock;
    }

    public void setKeepLock(boolean keepLock) {
        this.keepLock = keepLock;
    }

    public boolean getForcePromotion() {
        return forcePromotion;
    }

    public void setForcePromotion(boolean forcePromotion) {
        this.forcePromotion = forcePromotion;
    }

}
