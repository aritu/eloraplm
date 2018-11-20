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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.batchProcessing.util.EloraCMBatchProcessingConstants;
import com.aritu.eloraplm.cm.treetable.DocImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.constants.util.PromoteHelper;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;
import com.aritu.eloraplm.viewer.ViewerPdfUpdater;

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

    private Map<String, DocumentModel> helperDocMap;

    public CmDocsBatchProcessingBean() {
    }

    public void promoteDocs() {
        String logInitMsg = "[promoteDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TreeNode root = cmDocImpactedItemsTreeBean.getRoot();

            repeatedTreeDocMap = new HashMap<>();

            checkModifiedItemsAreManagedAndReleased(root);
            List<String> sortedIds = sortTreeDocumentsForPromote(root);
            promoteDoc(sortedIds, root);

            // Raise an event for refreshing the impact matrix
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_DOCS_IMPACT_MATRIX);

            // Nuxeo event
            EloraEventHelper.fireEvent(CMEventNames.CM_DOCS_BATCH_PROMOTE_EVENT,
                    navigationContext.getCurrentDocument());

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            // facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
            // "eloraplm.message.error.cm.batch.promoteItems"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<String> sortTreeDocumentsForPromote(TreeNode root)
            throws EloraException, CycleDetectedException {
        dag = new DAG();
        helperDocMap = new HashMap<>();
        childrenVersionSeriesMap = new HashMap<>();

        for (TreeNode modifiedItemNode : root.getChildren()) {
            DocumentModel modifiedItemDoc = ((ImpactedItemsNodeData) modifiedItemNode.getData()).getDestinationItem();
            if (!EloraDocumentHelper.isReleased(modifiedItemDoc)) {
                checkRepeatedDocs(modifiedItemDoc);
            }
            buildPromoteDAG(modifiedItemNode, modifiedItemDoc);
        }
        List<String> sortedIds = TopologicalSorter.sort(dag);
        return sortedIds;
    }

    private void checkModifiedItemsAreManagedAndReleased(TreeNode root)
            throws EloraException {
        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();
            if (!CmBatchProcessingHelper.isManaged(nodeData)) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedDocNotManaged"));
                throw new EloraException("All modified items must be managed");
            } else if (!EloraDocumentHelper.isReleased(
                    nodeData.getDestinationItem())) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedNotReleased"));
                throw new EloraException("All modified items must be released");
            }
        }
    }

    // TODO: ¿Hay que tener en cuenta si los documentos estan ignored para
    // comprobar si estan repetidos?
    private void checkRepeatedDocs(DocumentModel doc) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.repeatedDocWithDiffVersion"),
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
                    helperDocMap.put(structureParentDoc.getId(),
                            structureParentDoc);
                    if (nodeData.getIsAnarchic()) {
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

    private void promoteDoc(List<String> sortedIds, TreeNode root)
            throws EloraException {
        String logInitMsg = "[promoteDoc] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");
        cadHierarchicalAndSpecialAndDirectRelations = new ArrayList<>(
                RelationsConfig.cadHierarchicalRelationsList);
        cadHierarchicalAndSpecialAndDirectRelations.addAll(
                RelationsConfig.cadSpecialRelationsList);
        cadHierarchicalAndSpecialAndDirectRelations.addAll(
                RelationsConfig.cadDirectRelationsList);
        List<String> destinationWcUidList = new ArrayList<String>();
        boolean stopProcessing = false;
        for (String docId : sortedIds) {
            DocumentModel doc = helperDocMap.get(docId);
            if (doc != null) { // Modified are not in helperDocMap
                log.trace(logInitMsg + "Start promoting document |" + docId
                        + "|");
                try {
                    // TODO: se puede poner mejor la transaccion ya que no haría
                    // falta para los chequeos previos al promote. Ahora si
                    // falla un chequeo se hace rollback y en realidad no se ha
                    // cambiado nada!
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                    log.trace(logInitMsg
                            + "Start checking release or obsolete in major of document |"
                            + docId + "|");
                    if (PromoteHelper.checkReleasedAndObsoleteInMajor(doc,
                            documentManager)) {
                        log.trace(logInitMsg
                                + "Finished checking release or obsolete in major of document |"
                                + docId + "|");
                        log.trace(logInitMsg
                                + "Start getting hierarchical children of document |"
                                + docId + "|");
                        List<Statement> bomStmts = getCadHierarchicalStmts(doc);
                        log.trace(logInitMsg
                                + "Finished getting hierarchical children of document |"
                                + docId + "|");
                        List<String> childrenIdList = dag.getChildLabels(docId);
                        for (Statement bomStmt : bomStmts) {
                            DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                                    bomStmt.getObject(), documentManager);
                            List<String> childrenInTreeVersionSeriesIdList = childrenVersionSeriesMap.get(
                                    docId);
                            if (childrenInTreeVersionSeriesIdList.contains(
                                    objectBomDoc.getVersionSeriesId())) {
                                if (!childrenIdList.contains(
                                        objectBomDoc.getId())) {
                                    facesMessages.add(
                                            StatusMessage.Severity.ERROR,
                                            messages.get(
                                                    "eloraplm.message.error.cm.batch.compositionWithDiffDocVersion"),
                                            doc.getPropertyValue(
                                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                            doc.getTitle(),
                                            objectBomDoc.getPropertyValue(
                                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                            objectBomDoc.getTitle());
                                    throw new EloraException(
                                            "Document to be promoted has different document version in composition and tree");
                                }
                            } else {
                                if (EloraDocumentHelper.isReleased(
                                        objectBomDoc)) {
                                    DocumentModel latestReleased = EloraDocumentHelper.getLatestReleasedVersion(
                                            objectBomDoc);
                                    if (!latestReleased.getId().equals(
                                            objectBomDoc.getId())) {
                                        facesMessages.add(
                                                StatusMessage.Severity.WARN,
                                                messages.get(
                                                        "eloraplm.message.error.cm.batch.documentWithNoLastReleasedChild"),
                                                doc.getPropertyValue(
                                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                                doc.getTitle(),
                                                objectBomDoc.getPropertyValue(
                                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                                objectBomDoc.getTitle());
                                    }
                                } else {
                                    facesMessages.add(
                                            StatusMessage.Severity.ERROR,
                                            messages.get(
                                                    "eloraplm.message.error.cm.batch.documentWithNoReleasedChild"),
                                            doc.getPropertyValue(
                                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                            doc.getTitle(),
                                            objectBomDoc.getPropertyValue(
                                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                            objectBomDoc.getTitle());
                                    throw new EloraException(
                                            "Document has no released documents in its composition");
                                }
                            }
                        }
                        // executePromote(doc);
                        if (doc.getAllowedStateTransitions().contains(
                                EloraLifeCycleConstants.TRANS_APPROVE)) {
                            doc.followTransition(
                                    EloraLifeCycleConstants.TRANS_APPROVE);

                            if (doc.hasSchema(
                                    EloraSchemaConstants.ELORA_VIEWER)) {
                                Blob viewerBlob = ViewerPdfUpdater.createViewer(
                                        doc,
                                        ViewerActionConstants.ACTION_PROMOTE);
                                if (viewerBlob != null) {
                                    EloraDocumentHelper.disableVersioningDocument(
                                            doc);
                                    documentManager.saveDocument(doc);
                                }
                            }

                            DocumentModel wcDoc = documentManager.getWorkingCopy(
                                    doc.getRef());
                            if (!EloraDocumentHelper.getBaseVersion(
                                    wcDoc).getId().equals(doc.getId())) {
                                VersionModel version = new VersionModelImpl();
                                version.setId(doc.getId());
                                EloraDocumentHelper.restoreWorkingCopyToVersion(
                                        wcDoc, version,
                                        eloraDocumentRelationManager,
                                        documentManager);
                            } else {
                                EloraDocumentHelper.restoreToVersion(
                                        wcDoc.getRef(), doc.getRef(), true,
                                        true, documentManager);
                            }
                            documentManager.removeLock(wcDoc.getRef());
                            destinationWcUidList.add(wcDoc.getId());

                            // Nuxeo Event
                            doc.refresh();
                            String ecoReference = navigationContext.getCurrentDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                            String comment = doc.getVersionLabel() + " @"
                                    + ecoReference;
                            EloraEventHelper.fireEvent(
                                    PdmEventNames.PDM_PROMOTED_EVENT, doc,
                                    comment);

                            log.trace(
                                    logInitMsg + "Finished promoting document |"
                                            + docId + "|");
                        } else {
                            facesMessages.add(StatusMessage.Severity.ERROR,
                                    messages.get(
                                            "eloraplm.message.error.cm.batch.notAllowedTransition"),
                                    doc.getPropertyValue(
                                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                    doc.getTitle());
                            throw new EloraException(
                                    "Document state does not support actual transition");
                        }
                    } else {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.batch.documentWithReleasedOnMajor"),
                                doc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                doc.getTitle());
                        throw new EloraException("Document |" + doc.getId()
                                + "| has another released or obsolete document in the same major");
                    }
                } catch (EloraException e) {
                    log.error(logInitMsg + e.getMessage(), e);
                    TransactionHelper.setTransactionRollbackOnly();
                    stopProcessing = true;
                } catch (Exception e) {
                    log.error(logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                            e);
                    TransactionHelper.setTransactionRollbackOnly();
                    stopProcessing = true;
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }
                if (stopProcessing) {
                    break;
                }
            }
        }
        if (destinationWcUidList.size() > 0) {
            cmDocImpactedItemsTreeBean.getNodeService().setAsManaged(
                    navigationContext.getCurrentDocument(), root,
                    destinationWcUidList);
        }
    }

    private List<Statement> getCadHierarchicalStmts(DocumentModel doc) {
        List<Resource> predicates = new ArrayList<>();
        for (String predicateUri : cadHierarchicalAndSpecialAndDirectRelations) {
            predicates.add(new ResourceImpl(predicateUri));
        }
        return EloraRelationHelper.getStatements(doc, predicates);
    }

    // TODO: En un futuro hay que utilizar PromoteExecuterService para que todos
    // los promote se lancen desde el mismo sitio
    private void executePromote(DocumentModel doc) throws EloraException {
        if (doc.getAllowedStateTransitions().contains(
                EloraLifeCycleConstants.TRANS_APPROVE)) {
            doc.followTransition(EloraLifeCycleConstants.TRANS_APPROVE);

            if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
                Blob viewerBlob = ViewerPdfUpdater.createViewer(doc,
                        ViewerActionConstants.ACTION_PROMOTE);
                if (viewerBlob != null) {
                    EloraDocumentHelper.disableVersioningDocument(doc);
                    documentManager.saveDocument(doc);
                }
            }

            DocumentModel wcDoc = documentManager.getWorkingCopy(doc.getRef());
            if (!EloraDocumentHelper.getBaseVersion(wcDoc).getId().equals(
                    doc.getId())) {
                VersionModel version = new VersionModelImpl();
                version.setId(doc.getId());
                EloraDocumentHelper.restoreWorkingCopyToVersion(wcDoc, version,
                        eloraDocumentRelationManager, documentManager);
            } else {
                wcDoc.followTransition(EloraLifeCycleConstants.TRANS_APPROVE);
            }

            documentManager.removeLock(wcDoc.getRef());
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.notAllowedTransition"),
                    doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    doc.getTitle());
            throw new EloraException(
                    "Document state does not support actual transition");
        }
    }

    private DocumentModel getDocumentArchivedVersion(DocumentModel doc)
            throws EloraException {
        if (!doc.isVersion()) {
            doc = EloraDocumentHelper.getLatestVersion(doc);
        }
        return doc;
    }

    public void toggleLockAllDocs(boolean lock) {
        String logInitMsg = "[toggleLockAllDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            TreeNode root = cmDocImpactedItemsTreeBean.getRoot();
            // Map<String, List<DocumentModel>> errMap = new HashMap<String,
            // List<DocumentModel>>();
            for (TreeNode modifiedItemNode : root.getChildren()) {
                CmBatchProcessingHelper.toggleLockProcessableDocs(
                        modifiedItemNode, lock, facesMessages, messages,
                        documentManager);
            }
            // printWarnings(errMap);

            // Nuxeo event
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
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.lockAllItems"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void printWarnings(Map<String, List<DocumentModel>> errMap) {
        for (Map.Entry<String, List<DocumentModel>> entry : errMap.entrySet()) {
            String errType = entry.getKey();
            List<DocumentModel> docList = entry.getValue();
            String errorMsg = null;
            switch (errType) {
            case EloraCMBatchProcessingConstants.ERR_ALREADY_LOCKED:
                errorMsg = "eloraplm.message.error.locked";
                break;
            case EloraCMBatchProcessingConstants.ERR_UNLOCKABLE_DOC:
                errorMsg = "eloraplm.message.error.not.lockable";
                break;
            case EloraCMBatchProcessingConstants.ERR_MISSING_LOCK_RIGHTS:
                errorMsg = "eloraplm.message.error.lock.rights";
                break;
            }
            for (DocumentModel doc : docList) {
                facesMessages.add(StatusMessage.Severity.WARN,
                        messages.get(errorMsg),
                        doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                        doc.getTitle());
            }
        }
    }
}
