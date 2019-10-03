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
import com.aritu.eloraplm.cm.treetable.BomImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("cmItemsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmItemsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmItemsBatchProcessingBean.class);

    protected EloraVersionLabelService eloraVersionLabelService = Framework.getService(
            EloraVersionLabelService.class);

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

    private Map<String, String> repeatedTreeDocMap;

    private List<String> bomHierarchicalAndDirectRelations;

    private DAG dag;

    private Map<String, String> helperCommentMap;

    public CmItemsBatchProcessingBean() {
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
                repeatedTreeDocMap = new HashMap<>();

                List<String> modifiedItemsArchivedDestinationDocIds = checkModifiedItemsAreManagedAndReleased(
                        root);

                List<String> sortedIds = sortTreeDocumentsForPromote(root,
                        modifiedItemsArchivedDestinationDocIds);

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
                            cmProcessDoc, CMBatchProcessingConstants.PROMOTE,
                            CMConstants.ITEM_TYPE_BOM, documentManager);

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
                            cmProcessDoc, root, sortedIds, dag,
                            childrenVersionSeriesMap,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.PROMOTE);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            processingAction, sortedIds.size());

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

    private List<String> sortTreeDocumentsForPromote(TreeNode root,
            List<String> modifiedItemsArchivedDestinationDocIds)
            throws EloraException, CycleDetectedException {
        String logInitMsg = "[sortTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        dag = new DAG();

        childrenVersionSeriesMap = new HashMap<>();

        log.trace(logInitMsg + "Proceeding to build promote DAG");
        for (TreeNode modifiedItemNode : root.getChildren()) {
            buildPromoteDAG(modifiedItemNode);
        }
        @SuppressWarnings("unchecked")
        List<String> sortedIds = TopologicalSorter.sort(dag);

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

        log.trace(logInitMsg + "Size of Sorted IDs for Promote DAG: |"
                + newSortedIds.size() + "|");

        log.trace(logInitMsg + "--- EXIT --- ");
        return newSortedIds;
    }

    private void buildPromoteDAG(TreeNode node)
            throws EloraException, CycleDetectedException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) node.getData();
            if (nodeData.getIsDirectObject()) {
                createPromoteVertexAndEdges(childNode, parentNodeData, nodeData,
                        true);
            } else {
                createPromoteVertexAndEdges(childNode, nodeData, parentNodeData,
                        false);
            }
        }
    }

    private void createPromoteVertexAndEdges(TreeNode childNode,
            ImpactedItemsNodeData subjectNodeData,
            ImpactedItemsNodeData objectNodeData, boolean isDirectObject)
            throws EloraException, CycleDetectedException {

        DocumentModel object = null;
        if (isDirectObject) {
            if (!CmBatchProcessingHelper.isIgnored(objectNodeData)) {
                object = getArchivedDestinationDoc(objectNodeData);
                if (!EloraDocumentHelper.isReleased(object)) {
                    dag.addVertex(object.getId());
                }
            } else {
                object = objectNodeData.getOriginItemWc();
            }
        } else {
            object = getArchivedDestinationDoc(objectNodeData);
        }

        if (!CmBatchProcessingHelper.isIgnored(subjectNodeData)) {
            DocumentModel subject = getArchivedDestinationDoc(subjectNodeData);
            subject.refresh();
            if (!EloraDocumentHelper.isReleased(subject)) {
                checkRepeatedDocs(subject);
                dag.addVertex(subject.getId());
                // DIRECT: hay que mirar la relacion inversa
                if ((!isDirectObject && subjectNodeData.getIsAnarchic())
                        || EloraDocumentHelper.isReleased(object)) {
                    dag.addEdge(subject.getId(), null);
                } else {
                    dag.addEdge(subject.getId(), object.getId());
                    addChildVersionSeriesId(subject.getId(),
                            object.getVersionSeriesId());
                }
            }
            buildPromoteDAG(childNode);
        }
    }

    private void addChildVersionSeriesId(String docId,
            String childVersionSeriesId) {
        List<String> versionSeriesList = childrenVersionSeriesMap.get(docId);
        if (versionSeriesList == null) {
            versionSeriesList = new ArrayList<>();
        }
        versionSeriesList.add(childVersionSeriesId);
        childrenVersionSeriesMap.put(docId, versionSeriesList);
    }

    private DocumentModel getArchivedDestinationDoc(
            ImpactedItemsNodeData nodeData) throws EloraException {
        DocumentModel destinationDoc = getDocumentArchivedVersion(
                nodeData.getDestinationItem());
        return destinationDoc;
    }

    private DocumentModel getDocumentArchivedVersion(DocumentModel doc)
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
            // if there is any unsaved change in the tree, don't do anything
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {

                // First do all required checks SYNCHRONOUSLY
                bomHierarchicalAndDirectRelations = new ArrayList<>(
                        RelationsConfig.bomHierarchicalRelationsList);
                repeatedTreeDocMap = new HashMap<>();
                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                checkModifiedItemsAreManagedAndMajorReleased(root);
                int totalDocuments = checkAndcountTreeDocumentsForExecuteActions(
                        root);

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
                            CMConstants.ITEM_TYPE_BOM, documentManager);

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
                            CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS,
                            cmProcessDoc, root,
                            bomHierarchicalAndDirectRelations,
                            eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.EXECUTE_ACTIONS);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            processingAction, totalDocuments);

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
            if (!CmBatchProcessingHelper.isManaged(nodeData)) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedItemNotManaged"));
                throw new EloraException("All modified items must be managed");
            } else {
                String action = nodeData.getAction();
                if (action != null
                        && !action.equals(CMConstants.ACTION_REMOVE)) {

                    DocumentModel destinationDoc = nodeData.getDestinationItem();
                    DocumentModel releasedDoc = EloraDocumentHelper.getMajorReleasedVersion(
                            destinationDoc);
                    if (releasedDoc != null && !releasedDoc.getId().equals(
                            destinationDoc.getId())) {
                        log.error(
                                "All modified items must be released or they can't have another version released in the same major");
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.batch.modifiedItemNotReleasedOrAnotherReleased"));
                        throw new EloraException(
                                "All modified items must be released or they can't have another version released in the same major");
                    }
                }
            }
        }
        log.trace(logInitMsg
                + "All modified items are managed and there is not another version released in their major");

    }

    private int checkAndcountTreeDocumentsForExecuteActions(TreeNode root)
            throws EloraException {
        int totalDocs = 0;

        String logInitMsg = "[checkTreeDocuments] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : root.getChildren()) {

            ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

            // modified items
            if (childNodeData.getIsModifiedItem()) {

                if (!CmBatchProcessingHelper.isIgnored(childNodeData)) {
                    totalDocs += checkAndcountTreeDocumentsForExecuteActions(
                            childNode);
                }

            } else {
                if (!CmBatchProcessingHelper.isIgnored(childNodeData)
                        && !CmBatchProcessingHelper.isManaged(childNodeData)) {

                    DocumentModel destItem = childNodeData.getDestinationItem();

                    checkDestinationItemLocked(destItem);
                    checkRepeatedDocs(destItem);

                    totalDocs++;
                }

                totalDocs += checkAndcountTreeDocumentsForExecuteActions(
                        childNode);
            }

        }
        log.trace(logInitMsg
                + "There are no conflicts in tree with versions or locks");

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

    private void checkRepeatedDocs(DocumentModel doc) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.itemRepeatedWithDiffVersion"));
            throw new EloraException(
                    "Document repeated with different version");
        } else {
            repeatedTreeDocMap.put(doc.getVersionSeriesId(), doc.getId());
        }
    }

    public void overwrite() {
        String logInitMsg = "[overwrite] ["
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

                List<String> sortedIds = sortTreeDocumentsForCheckin(logInitMsg,
                        root);

                if (sortedIds == null || sortedIds.size() == 0) {
                    log.trace(logInitMsg + "Nothing to be overwritten.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.cm.batch.nothingToBeOverwritten"));
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
                            CMEventNames.CM_ITEMS_BATCH_OVERWRITE_EVENT,
                            navigationContext.getCurrentDocument());

                    // Start ASYNCHRONOUS processing
                    lifecycleStateChanged = CmBatchProcessingHelper.prepareAsynchronousProcess(
                            cmProcessDoc, CMBatchProcessingConstants.OVERWRITE,
                            CMConstants.ITEM_TYPE_BOM, documentManager);

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
                            CMBatchProcessingEventNames.OVERWRITE_ITEMS,
                            cmProcessDoc, root, sortedIds, helperCommentMap,
                            checkinManager, eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.OVERWRITE_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.OVERWRITE);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            processingAction, sortedIds.size());

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
                    "eloraplm.message.error.cm.batch.processingOverwrite"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            CmBatchProcessingHelper.handleExceptionInAsyncProcess(cmProcessDoc,
                    lifecycleStateChanged, transitionToComeBackToPreviousState,
                    documentManager);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processingOverwrite"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
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
            if (!cmBomImpactedItemsTreeBean.getIsDirty()) {
                // First do all required checks SYNCHRONOUSLY
                TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
                if (root == null) {
                    throw new EloraException("root is null");
                }

                checkModifiedItemsAreManagedAndMajorReleased(root);

                List<String> sortedIds = sortTreeDocumentsForCheckin(logInitMsg,
                        root);

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
                            CMConstants.ITEM_TYPE_BOM, documentManager);

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
                            cmProcessDoc, root, sortedIds, helperCommentMap,
                            checkinManager, eloraDocumentRelationManager,
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.CHECKIN_ITEMS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.CHECKIN);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            processingAction, sortedIds.size());

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

    private List<String> sortTreeDocumentsForCheckin(String logInitMsg,
            TreeNode root) throws CycleDetectedException, EloraException {
        log.trace(logInitMsg + "Proceeding to build checkin DAG");
        dag = new DAG();
        helperCommentMap = new HashMap<String, String>();
        for (TreeNode modifiedItemNode : root.getChildren()) {
            buildCheckinDAG(modifiedItemNode);
        }
        @SuppressWarnings("unchecked")
        List<String> sortedIds = TopologicalSorter.sort(dag);

        List<String> newSortedIds = new LinkedList<String>();

        // Exclude items having null sortedId from the sortedIds list
        for (Iterator<String> iterator = sortedIds.iterator(); iterator.hasNext();) {
            String sortedId = iterator.next();
            if (sortedId != null) {
                newSortedIds.add(sortedId);
            }
        }

        log.trace(logInitMsg + "Size of Sorted IDs for Checkin DAG: |"
                + newSortedIds.size() + "|");

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
            DocumentModel modifiedItemDestinationDoc = nodeData.getDestinationItem();

            // if the modified item is managed, the destination must be AV. We
            // check here, to be sure that everything is ok.
            if (!CmBatchProcessingHelper.isManaged(nodeData)
                    || !modifiedItemDestinationDoc.isVersion()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedItemNotManaged"));
                throw new EloraException("All modified items must be managed");
            } else {
                if (!EloraDocumentHelper.isReleased(
                        modifiedItemDestinationDoc)) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.cm.batch.modifiedItemNotReleased"));
                    throw new EloraException(
                            "All modified items must be released");
                }
            }
            /*
             * DocumentModel modifiedItemArchivedDestinationDoc =
             * getArchivedDestinationDoc( nodeData);
             */
            modifiedItemsArchivedDestinationDocIds.add(
                    modifiedItemDestinationDoc.getId());
        }
        log.trace(logInitMsg + "Modified items are managed and released");

        return modifiedItemsArchivedDestinationDocIds;
    }

    private void buildCheckinDAG(TreeNode node)
            throws CycleDetectedException, EloraException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) node.getData();
            if (nodeData.getIsDirectObject()) {
                createCheckinVertexAndEdges(childNode, parentNodeData, nodeData,
                        true);
            } else {
                createCheckinVertexAndEdges(childNode, nodeData, parentNodeData,
                        false);
            }
        }
    }

    private void createCheckinVertexAndEdges(TreeNode childNode,
            ImpactedItemsNodeData subjectNodeData,
            ImpactedItemsNodeData objectNodeData, boolean isDirectObject)
            throws CycleDetectedException, EloraException {

        DocumentModel object = null;
        if (isDirectObject) {
            if (!CmBatchProcessingHelper.isIgnored(objectNodeData)) {
                object = objectNodeData.getDestinationItem();
                if (object.isCheckedOut()) {
                    dag.addVertex(object.getId());
                    dag.addEdge(object.getId(), null);
                    helperCommentMap.put(object.getId(),
                            objectNodeData.getComment());
                }
            } else {
                object = objectNodeData.getOriginItemWc();
            }
        } else {
            object = objectNodeData.getDestinationItem();
        }

        if (!CmBatchProcessingHelper.isIgnored(subjectNodeData)) {
            DocumentModel subject = subjectNodeData.getDestinationItem();
            subject.refresh();
            if (!subjectNodeData.getIsManaged()) {
                if (subject.isCheckedOut()) {
                    if (EloraDocumentHelper.isLockedByUserOrAdmin(subject,
                            documentManager)) {
                        if (object != null) {
                            // Modified is not removed. When direct never
                            // happens object == null
                            String predicate = null;
                            if (isDirectObject) {
                                predicate = objectNodeData.getPredicate();
                            } else {
                                predicate = subjectNodeData.getPredicate();
                            }
                            if ((!isDirectObject
                                    && subjectNodeData.getIsAnarchic())
                                    || EloraRelationHelper.existsRelation(
                                            subject, object, predicate,
                                            documentManager)) {
                                // TODO: Igual hay que mirar si el vertice
                                // existe de antes
                                dag.addVertex(subject.getId());
                                helperCommentMap.put(subject.getId(),
                                        subjectNodeData.getComment());
                                if (object.isCheckedOut()) {
                                    // Igual hay que mirar si el edge existe
                                    // de antes
                                    dag.addEdge(subject.getId(),
                                            object.getId());
                                } else {
                                    dag.addEdge(subject.getId(), null);
                                }
                            } else {
                                facesMessages.add(StatusMessage.Severity.ERROR,
                                        messages.get(
                                                "eloraplm.message.error.cm.batch.itemRelationNoExists"),
                                        subject.getPropertyValue(
                                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                        subject.getTitle());
                                throw new EloraException(
                                        "Relation in impacted tree does not exist in checked out document");
                            }
                        }
                    } else {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.batch.itemNotLocked"),
                                subject.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                subject.getTitle());
                        throw new EloraException(
                                "Checked out document must be locked by user");
                    }
                }
            }
            buildCheckinDAG(childNode);
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
                            CMConstants.ITEM_TYPE_BOM, documentManager);

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

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.UNDO_CHECKOUT);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_BOM,
                            processingAction, checkedOutDocs.size());

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

}
