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
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.lifecycles.util.LifecycleHelper;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("cmDocsBatchProcessing")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CmDocsBatchProcessingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmDocsBatchProcessingBean.class);

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
    protected transient DocImpactedItemsTreeBean cmDocImpactedItemsTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    private Map<String, List<String>> childrenVersionSeriesMap;

    private Map<String, String> repeatedTreeDocMap;

    private List<String> cadHierarchicalAndSpecialAndDirectRelations;

    private DAG dag;

    public CmDocsBatchProcessingBean() {
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
                repeatedTreeDocMap = new HashMap<>();

                List<String> modifiedDocsArchivedDestinationDocIds = checkModifiedDocsAreManagedAndReleased(
                        root);

                List<String> sortedIds = sortTreeDocumentsForPromote(root,
                        modifiedDocsArchivedDestinationDocIds);

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
                            cmProcessDoc, CMBatchProcessingConstants.PROMOTE,
                            CMConstants.ITEM_TYPE_DOC, documentManager);

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
                            transitionToComeBackToPreviousState);

                    log.trace(logInitMsg + "|"
                            + CMBatchProcessingEventNames.PROMOTE_DOCS
                            + "| Asynchronous Event fired.");

                    String processingAction = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                                    + CMBatchProcessingConstants.PROMOTE);

                    Events.instance().raiseEvent(
                            CMBatchProcessingEventNames.IN_PROGRESS,
                            cmProcessDoc.getId(), CMConstants.ITEM_TYPE_DOC,
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
            List<String> modifiedDocsArchivedDestinationDocIds)
            throws EloraException, CycleDetectedException {
        String logInitMsg = "[sortTreeDocumentsForPromote] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        dag = new DAG();

        childrenVersionSeriesMap = new HashMap<>();

        log.trace(logInitMsg + "Proceeding to build promote DAG");
        for (TreeNode modifiedDocNode : root.getChildren()) {
            // As modified items must be managed to promote impacted items,
            // destination item will be AV
            DocumentModel modifiedItemArchivedDestinationDoc = ((ImpactedItemsNodeData) modifiedDocNode.getData()).getDestinationItem();
            /* if (!EloraDocumentHelper.isReleased(modifiedItemDoc)) {
                checkRepeatedDocs(modifiedItemDoc);
            }*/
            buildPromoteDAG(modifiedDocNode,
                    modifiedItemArchivedDestinationDoc);
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
            DocumentModel modifiedDocDestinationDoc = nodeData.getDestinationItem();

            // if the modified document is managed, the destination must be AV.
            // We check it here, to be sure that everything is ok.
            if (!CmBatchProcessingHelper.isManaged(nodeData)
                    || !modifiedDocDestinationDoc.isVersion()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedDocNotManaged"));
                throw new EloraException(
                        "All modified documents must be managed");
            } else if (!EloraDocumentHelper.isReleased(
                    modifiedDocDestinationDoc)) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedDocNotReleased"));
                throw new EloraException(
                        "All modified documents must be released");
            }
            /*DocumentModel modifiedItemArchivedDestinationDoc = getArchivedDestinationDoc(
                    nodeData);*/
            modifiedDocsArchivedDestinationDocIds.add(
                    modifiedDocDestinationDoc.getId());
        }
        log.trace(logInitMsg + "Modified items are managed and released");

        return modifiedDocsArchivedDestinationDocIds;

    }

    // TODO: ¿Hay que tener en cuenta si los documentos estan ignored para
    // comprobar si estan repetidos?
    private void checkRepeatedDocs(DocumentModel doc) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.docRepeatedWithDiffVersion"),
                    doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    doc.getTitle());
            throw new EloraException(
                    "Document repeated with different version");
        } else {
            repeatedTreeDocMap.put(doc.getVersionSeriesId(), doc.getId());
        }
    }

    private void buildPromoteDAG(TreeNode node, DocumentModel structureChildDoc)
            throws EloraException, CycleDetectedException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                DocumentModel structureParentDoc = getArchivedDestinationDoc(
                        nodeData);
                structureParentDoc.refresh();
                checkRepeatedDocs(structureParentDoc);
                if (!EloraDocumentHelper.isReleased(structureParentDoc)) {
                    dag.addVertex(structureParentDoc.getId());
                    if (nodeData.getIsAnarchic()
                            || EloraDocumentHelper.isReleased(
                                    structureChildDoc)) {
                        dag.addEdge(structureParentDoc.getId(), null);
                    } else {
                        dag.addEdge(structureParentDoc.getId(),
                                structureChildDoc.getId());
                        addChildVersionSeriesId(structureParentDoc.getId(),
                                structureChildDoc.getVersionSeriesId());
                    }
                }
                buildPromoteDAG(childNode, structureParentDoc);
            }
        }
    }

    private DocumentModel getArchivedDestinationDoc(
            ImpactedItemsNodeData nodeData) throws EloraException {
        DocumentModel destinationDoc = getDocumentArchivedVersion(
                nodeData.getDestinationItem());
        return destinationDoc;
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

}
