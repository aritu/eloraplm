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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.checkin.api.CheckinManager;
import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.treetable.BomImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.RelationEventNames;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.overwrite.version.helper.OverwriteVersionHelper;
import com.aritu.eloraplm.promote.constants.util.PromoteHelper;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;
import com.aritu.eloraplm.viewer.ViewerPdfUpdater;

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

    private Map<DocumentModel, List<String>> actionTreeDocMap;

    private List<String> bomHierarchicalAndDirectRelations;

    private DAG dag;

    private Map<String, DocumentModel> helperDocMap;

    private Map<String, String> helperCommentMap;

    private Boolean overwrite;

    public CmItemsBatchProcessingBean() {
    }

    public void promoteDocs() {
        String logInitMsg = "[promoteDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
            repeatedTreeDocMap = new HashMap<>();
            checkModifiedItemsAreManagedAndReleased(root);

            List<String> sortedIds = sortTreeDocumentsForPromote(logInitMsg,
                    root);
            promoteDoc(sortedIds, root);

            log.trace(logInitMsg + "Finished promoting all docs");

            // Raise an event for refreshing the impact matrix
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX);

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_ITEMS_BATCH_PROMOTE_EVENT,
                    navigationContext.getCurrentDocument());

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<String> sortTreeDocumentsForPromote(String logInitMsg,
            TreeNode root) throws EloraException, CycleDetectedException {
        dag = new DAG();
        helperDocMap = new HashMap<>();
        childrenVersionSeriesMap = new HashMap<>();

        log.trace(logInitMsg + "Proceeding to build promote DAG");
        for (TreeNode modifiedItemNode : root.getChildren()) {
            DocumentModel modifiedItemDoc = getArchivedDestinationDoc(
                    (ImpactedItemsNodeData) modifiedItemNode.getData());
            if (!EloraDocumentHelper.isReleased(modifiedItemDoc)) {
                checkRepeatedDocs(modifiedItemDoc);
            }
            buildPromoteDAG(modifiedItemNode, modifiedItemDoc);
        }
        @SuppressWarnings("unchecked")
        List<String> sortedIds = TopologicalSorter.sort(dag);
        log.trace(logInitMsg + "Sorted promote DAG: |" + sortedIds.toString()
                + "|");
        return sortedIds;
    }

    private void promoteDoc(List<String> sortedIds, TreeNode root)
            throws EloraException {
        String logInitMsg = "[promoteDoc] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");
        bomHierarchicalAndDirectRelations = new ArrayList<>(
                RelationsConfig.bomHierarchicalRelationsList);
        bomHierarchicalAndDirectRelations.addAll(
                RelationsConfig.bomDirectRelationsList);
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
                    // TODO: cambiar este 'check' por un 'is' para que se
                    // entienda
                    // mejor. También convendría moverlo de promotehelper...
                    if (PromoteHelper.checkReleasedAndObsoleteInMajor(doc,
                            documentManager)) {
                        log.trace(logInitMsg
                                + "Finished checking release or obsolete in major of document |"
                                + docId + "|");
                        if (areRelatedCadDocsReleased(doc)) {
                            checkDocumentChildrenStates(doc);
                            // executePromote(doc);
                            if (doc.getAllowedStateTransitions().contains(
                                    EloraLifeCycleConstants.TRANS_APPROVE)) {

                                doc.followTransition(
                                        EloraLifeCycleConstants.TRANS_APPROVE);
                                updateViewer(doc);

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

                                log.trace(logInitMsg
                                        + "Finished promoting document |"
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
                                            "eloraplm.message.error.cm.batch.relatedCadNotReleased"),
                                    doc.getPropertyValue(
                                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                    doc.getTitle());
                            throw new EloraException(
                                    "Document with related cad not released");
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
            cmBomImpactedItemsTreeBean.getNodeService().setAsManaged(
                    navigationContext.getCurrentDocument(), root,
                    destinationWcUidList);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void updateViewer(DocumentModel doc) throws EloraException {
        Blob viewerBlob = ViewerPdfUpdater.createViewer(doc,
                ViewerActionConstants.ACTION_PROMOTE);
        if (viewerBlob != null) {
            EloraDocumentHelper.disableVersioningDocument(doc);
            documentManager.saveDocument(doc);
        }
    }

    private void checkDocumentChildrenStates(DocumentModel doc)
            throws EloraException {
        String logInitMsg = "[checkDocumentChildrenStates] ["
                + documentManager.getPrincipal().getName() + "] ";
        List<Statement> bomStmts = getBomHierarchicalStmts(doc);
        log.trace(logInitMsg + "Retrieved |" + bomStmts.size()
                + "| bom hierarchical statements of document |" + doc.getId()
                + "|");
        @SuppressWarnings("unchecked")
        List<String> childrenIdList = dag.getChildLabels(doc.getId());
        for (Statement bomStmt : bomStmts) {
            DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                    bomStmt.getObject(), documentManager);
            log.trace(logInitMsg + "Retrieved object |" + objectBomDoc.getId()
                    + "|");
            List<String> childrenInTreeVersionSeriesIdList = childrenVersionSeriesMap.get(
                    doc.getId());
            if (childrenInTreeVersionSeriesIdList.contains(
                    objectBomDoc.getVersionSeriesId())) {
                log.trace(logInitMsg + "A version of object |"
                        + objectBomDoc.getId() + "| is in tree");
                if (!childrenIdList.contains(objectBomDoc.getId())) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
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
                log.trace(logInitMsg + "Object |" + objectBomDoc.getId()
                        + "| is NOT in tree");
                if (EloraDocumentHelper.isReleased(objectBomDoc)) {
                    log.trace(logInitMsg + "Object |" + objectBomDoc.getId()
                            + "| is released");
                    DocumentModel latestReleased = EloraDocumentHelper.getLatestReleasedVersion(
                            objectBomDoc);
                    log.trace(logInitMsg + "Latest released of object |"
                            + objectBomDoc.getId() + "| is |"
                            + latestReleased.getId() + "|");
                    if (!latestReleased.getId().equals(objectBomDoc.getId())) {
                        facesMessages.add(StatusMessage.Severity.WARN,
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
                    facesMessages.add(StatusMessage.Severity.ERROR,
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
    }

    private void buildPromoteDAG(TreeNode node, DocumentModel structureChildDoc)
            throws EloraException, CycleDetectedException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();

            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                DocumentModel structureParentDoc = getArchivedDestinationDoc(
                        nodeData);
                structureParentDoc.refresh();
                if (!EloraDocumentHelper.isReleased(structureParentDoc)) {
                    checkRepeatedDocs(structureParentDoc);
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
            doc = EloraDocumentHelper.getLatestVersion(doc);
        }
        return doc;
    }

    private boolean areRelatedCadDocsReleased(DocumentModel doc) {
        try {
            checkRelatedCadDocsReleased(doc);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private void checkRelatedCadDocsReleased(DocumentModel doc)
            throws EloraException {
        String logInitMsg = "[checkRelatedCadDocsReleased] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "Start getting related cad documents of document |"
                        + doc.getId() + "|");
        List<Statement> cadStmts = getRelatedCadDocumentStmts(doc);
        log.trace(logInitMsg
                + "Finished getting related cad documents of document |"
                + doc.getId() + "|");
        for (Statement cadStmt : cadStmts) {
            DocumentModel cadDoc = RelationHelper.getDocumentModel(
                    cadStmt.getObject(), documentManager);
            if (!EloraDocumentHelper.isReleased(cadDoc)) {
                throw new EloraException("Related doc is not released");
            }
        }
    }

    private List<Statement> getRelatedCadDocumentStmts(DocumentModel doc) {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        List<Statement> cadStmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, doc,
                predicateResource);
        return cadStmts;
    }

    private List<Statement> getBomHierarchicalStmts(DocumentModel doc) {
        List<Resource> predicates = new ArrayList<>();
        for (String predicateUri : bomHierarchicalAndDirectRelations) {
            predicates.add(new ResourceImpl(predicateUri));
        }
        return EloraRelationHelper.getStatements(doc, predicates);
    }

    private void switchRelationWithLatestReleasedOrLatestVersion(
            DocumentModel doc, Statement bomStmt, DocumentModel objectBomDoc)
            throws EloraException {
        log.trace("Get latest released or latest version of item |"
                + objectBomDoc.getId() + "|");
        DocumentModel releasedBomDoc = EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                objectBomDoc);
        log.trace("Item |" + releasedBomDoc.getId() + "| retrieved");
        if (!releasedBomDoc.getId().equals(objectBomDoc.getId())) {

            switchRelation(doc, bomStmt.getPredicate().getUri(), releasedBomDoc,
                    objectBomDoc);

        }
    }

    private void switchRelation(DocumentModel subjectDoc, String predicateUri,
            DocumentModel objectDoc, DocumentModel oldObjectDoc) {
        // EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(oldStmt);
        log.trace("Switch relation");
        eloraDocumentRelationManager.updateRelation(documentManager, subjectDoc,
                predicateUri, oldObjectDoc, objectDoc);

        log.trace("Switched relation with subject |" + subjectDoc.getId()
                + "| Old object: |" + oldObjectDoc.getId() + "| New object: |"
                + objectDoc.getId() + "|");
    }

    public void toggleLockAllDocs(boolean lock) {
        String logInitMsg = "[toggleLockAllDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
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
                        CMEventNames.CM_ITEMS_BATCH_LOCK_EVENT,
                        navigationContext.getCurrentDocument());
            } else {
                EloraEventHelper.fireEvent(
                        CMEventNames.CM_ITEMS_BATCH_UNLOCK_EVENT,
                        navigationContext.getCurrentDocument());
            }

            cmBomImpactedItemsTreeBean.createRoot();
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

    public void executeActions() {
        String logInitMsg = "[processTreeActions] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            bomHierarchicalAndDirectRelations = new ArrayList<>(
                    RelationsConfig.bomHierarchicalRelationsList);
            repeatedTreeDocMap = new HashMap<>();
            TreeNode root = cmBomImpactedItemsTreeBean.getRoot();

            checkModifiedItemsAreManagedAndMajorReleased(root);
            checkTreeDocuments(root);

            actionTreeDocMap = new HashMap<>();
            log.trace(logInitMsg + "Start processing impacted items");
            for (TreeNode modifiedItemNode : root.getChildren()) {
                processImpactedDocs(modifiedItemNode);
            }
            log.trace(logInitMsg + "Finished processing impacted items");
            log.trace(logInitMsg
                    + "Start fixing relations to documents missing in tree");
            fixRelationsToDocumentsMissingInTree();
            log.trace(logInitMsg
                    + "Finished fixing relations to documents missing in tree");

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_ITEMS_BATCH_EXECUTE_ACTIONS_EVENT,
                    navigationContext.getCurrentDocument());

            navigationContext.invalidateCurrentDocument();
            cmBomImpactedItemsTreeBean.createRoot();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.processActions"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

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
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.batch.modifiedNotReleasedOrAnotherReleased"));
                        throw new EloraException(
                                "All modified items must be released or they can't have another version released in the same major");
                    }
                }
            }
        }
        log.trace(logInitMsg
                + "All modified items are managed and there is not another version released in their major");
    }

    private void fixRelationsToDocumentsMissingInTree() throws EloraException {
        String logInitMsg = "[checkModifiedItemsAreManagedAndMajorReleased] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (Map.Entry<DocumentModel, List<String>> entry : actionTreeDocMap.entrySet()) {
            DocumentModel subject = entry.getKey();
            List<String> relatedTreeObjectList = entry.getValue();
            log.trace("Get bom hierarchical statements of item |"
                    + subject.getId() + "|");
            List<Statement> bomStmts = getBomHierarchicalStmts(subject);
            log.trace("Get bom hierarchical statements of item |"
                    + subject.getId() + "|");
            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                for (Statement bomStmt : bomStmts) {
                    DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                            bomStmt.getObject(), documentManager);
                    if (!relatedTreeObjectList.contains(objectBomDoc.getId())) {
                        log.trace("Item |" + objectBomDoc.getId()
                                + "| not in tree");
                        switchRelationWithLatestReleasedOrLatestVersion(subject,
                                bomStmt, objectBomDoc);
                    }
                }
            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage(), e);
                TransactionHelper.setTransactionRollbackOnly();
            } catch (Exception e) {
                log.error(logInitMsg + "Uncontrolled exception: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);
                TransactionHelper.setTransactionRollbackOnly();
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.processActions"));
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
    }

    private void checkTreeDocuments(TreeNode root) throws EloraException {
        String logInitMsg = "[checkTreeDocuments] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();
            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                DocumentModel destItem = nodeData.getDestinationItem();
                if (!CmBatchProcessingHelper.isManaged(nodeData)) {
                    checkDestinationItemLocked(destItem);
                    checkRepeatedDocs(destItem);
                }
                // else {
                // checkRepeatedDocs(destItem);
                // }
                checkTreeDocuments(modifiedItemNode);
            }
        }
        log.trace(logInitMsg
                + "There are no conflicts in tree with versions or locks");
    }

    private void checkDestinationItemLocked(DocumentModel destItem)
            throws EloraException {
        if (!EloraDocumentHelper.isLockedByUserOrAdmin(destItem,
                documentManager)) {

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.cm.batch.documentNotLocked"),
                    destItem.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    destItem.getTitle());
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warn.cm.batch.allDocumentsWithActionMustBeLocked"));
            throw new EloraException(
                    "eloraplm.message.error.cm.documentsNotLocked");
        }
    }

    private void checkRepeatedDocs(DocumentModel doc) throws EloraException {
        String treeDocUid = repeatedTreeDocMap.get(doc.getVersionSeriesId());
        if (treeDocUid != null && !doc.getId().equals(treeDocUid)) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.batch.repeatedDocWithDiffVersion"));
            throw new EloraException(
                    "Document repeated with different version");
        } else {
            repeatedTreeDocMap.put(doc.getVersionSeriesId(), doc.getId());
        }
    }

    // private boolean isManaged(ImpactedItemsNodeData nodeData) {
    // return nodeData.getIsManaged();
    // }

    private void processImpactedDocs(TreeNode node) throws EloraException {
        String logInitMsg = "[processImpactedDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                if (!CmBatchProcessingHelper.isManaged(nodeData)) {
                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                        executeAction(childNode, nodeData);
                    } catch (EloraException e) {
                        log.error(logInitMsg + e.getMessage(), e);
                        TransactionHelper.setTransactionRollbackOnly();
                    } catch (Exception e) {
                        log.error(logInitMsg + "Uncontrolled exception: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(), e);
                        TransactionHelper.setTransactionRollbackOnly();
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.batch.processActions"));
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                    }
                }
                processImpactedDocs(childNode);
            }
        }
    }

    private void executeAction(TreeNode node, ImpactedItemsNodeData nodeData)
            throws EloraException {
        TreeNode parentNode = node.getParent();
        ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) parentNode.getData();

        String parentAction = parentNodeData.getAction();
        switch (parentAction) {
        case CMConstants.ACTION_CHANGE:
            log.trace("Execute change action");
            executeChangeAction(nodeData, parentNodeData);
            break;
        case CMConstants.ACTION_REMOVE:
            log.trace("Execute remove action");
            executeRemoveAction(nodeData, parentNodeData);
            break;
        case CMConstants.ACTION_REPLACE:
            log.trace("Execute replace action");
            executeReplaceAction(nodeData, parentNodeData);
            break;
        default:
            throw new EloraException("Action not defined in tree");
        }
    }

    private void executeChangeAction(ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData) throws EloraException {

        DocumentModel parentDestDoc = parentNodeData.getDestinationItem();
        parentDestDoc.refresh();
        DocumentModel parentOrigDoc = parentNodeData.getOriginItem();
        DocumentModel destinationDoc = nodeData.getDestinationItem();
        destinationDoc.refresh();

        if (!nodeData.getIsAnarchic()) {
            log.trace("About to manage relation for document |"
                    + destinationDoc.getId() + "|");
            manageRelations(destinationDoc, parentDestDoc, parentOrigDoc,
                    nodeData.getPredicate(), false);
            log.trace("Relation managed");
        }

        // Nuxeo Event
        String ecoReference = navigationContext.getCurrentDocument().getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String childReference = parentOrigDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String comment = childReference + " (" + parentOrigDoc.getVersionLabel()
                + ") => " + childReference + " ("
                + parentDestDoc.getVersionLabel() + ") @" + ecoReference;
        EloraEventHelper.fireEvent(
                RelationEventNames.CHECKED_OUT_AND_RELATION_CHANGED_EVENT,
                destinationDoc, comment);

        marcActionDocumentsInTree(destinationDoc, parentDestDoc);
        EloraDocumentHelper.checkOutDocument(destinationDoc);

        log.trace("Change action executed in document |"
                + destinationDoc.getId() + "|");
    }

    private void marcActionDocumentsInTree(DocumentModel subject,
            DocumentModel object) {
        List<String> objectList = actionTreeDocMap.get(subject);
        if (objectList == null) {
            objectList = new ArrayList<>();
        }
        if (object != null) {
            objectList.add(object.getId());
        }
        actionTreeDocMap.put(subject, objectList);
    }

    private void manageRelations(DocumentModel subject, DocumentModel object,
            DocumentModel originObject, String predicate, boolean replace)
            throws EloraException {
        if (subject.isCheckedOut()) {
            log.trace("Document is checked out");
            manageCheckedOutDocRelations(subject, object, originObject,
                    predicate, replace);
        } else {
            log.trace("Document is checked in");
            manageCheckedInDocRelations(subject, object, originObject,
                    predicate, replace);
        }
    }

    private void manageCheckedOutDocRelations(DocumentModel subject,
            DocumentModel object, DocumentModel originObject, String predicate,
            boolean replace) throws EloraException {

        log.trace("Getting relations from document to any version of |"
                + object.getId() + "|");
        // TODO: Mirar si se puede mejorar esta consulta. Aquí vendría bien
        // tener una consulta de un subject a un versionseriresid de un object!
        // Para ver si existe y hacer switch si es necesario
        DocumentModelList objectDocList = getRelationsFromSubjectToAnyObjectVersion(
                subject, object, predicate);
        log.trace("All relations retrieved");
        if (objectDocList.size() > 1) {
            throw new EloraException("Subject |" + subject.getId()
                    + "| with the same relation to different object |"
                    + object.getId() + "| versions");
        }

        if (objectDocList.size() == 1) {
            DocumentModel relatedObject = objectDocList.get(0);
            log.trace("Relation to document |" + relatedObject.getId()
                    + "| exists");
            if (!relatedObject.getId().equals(object.getId())) {
                log.trace(
                        "Relation is not the same as the one in impact tree document |"
                                + object.getId() + "|");
                // TODO: Mira si al crear el stmt así las propiedades de la
                // relacion se mantienen con lo que habia antes. Quantity,
                // comment, etc.
                Statement stmt = new StatementImpl(
                        RelationHelper.getDocumentResource(subject),
                        new ResourceImpl(predicate),
                        RelationHelper.getDocumentResource(relatedObject));
                switchRelation(subject, stmt.getPredicate().getUri(), object,
                        relatedObject);
                if (replace) {
                    // ¿Borramos la relacion a originObject? ¿Si no existe da
                    // error?
                }
            } else if (replace) {
                // ¿Borramos la relacion a originObject? ¿Si no existe da error?
            }
        } else {
            if (replace) {
                log.trace(
                        "No existing relations. Proceed to replace relation from origin object |"
                                + originObject.getId()
                                + "| to destination object |" + object.getId()
                                + "|");
                switchRelation(subject, predicate, object, originObject);
            } else {
                log.trace(
                        "No existing relations. Proceed to add relation from |"
                                + subject.getId() + "| to |" + object.getId()
                                + "|");
                eloraDocumentRelationManager.addRelation(documentManager,
                        subject, RelationHelper.getDocumentResource(object),
                        predicate, null, "1");
                log.trace("Relation added");
            }
        }
    }

    private DocumentModelList getRelationsFromSubjectToAnyObjectVersion(
            DocumentModel subject, DocumentModel object, String predicate) {
        return EloraRelationHelper.getAllVersionsOfRelatedObject(subject,
                object, predicate, documentManager);
    }

    private void manageCheckedInDocRelations(DocumentModel subject,
            DocumentModel object, DocumentModel originObject, String predicate,
            boolean replace) {
        if (object.isCheckedOut()) {
            if (!EloraRelationHelper.existsRelation(subject, object, predicate,
                    documentManager)) {
                if (replace) {
                    log.trace(
                            "No existing relations. Proceed to replace relation from origin object |"
                                    + originObject.getId()
                                    + "| to destination object |"
                                    + object.getId() + "|");
                    switchRelation(subject, predicate, object, originObject);
                } else {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                }
            } else if (replace) {
                // ¿Borramos la relacion a originObject? ¿Si no existe da error?
            }
        } else {
            DocumentModel objectWc = documentManager.getWorkingCopy(
                    object.getRef());

            if (!EloraRelationHelper.existsRelation(subject, objectWc,
                    predicate, documentManager)) {
                if (replace) {
                    log.trace(
                            "No existing relations. Proceed to replace relation from origin object |"
                                    + originObject.getId()
                                    + "| to destination object |"
                                    + object.getId() + "|");
                    switchRelation(subject, predicate, object, originObject);
                } else {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                }
            } else {
                switchRelation(subject, predicate, object, objectWc);
                if (replace) {
                    // ¿Borramos la relacion a originObject? ¿Si no existe da
                    // error?
                }
            }

        }
    }

    private void executeRemoveAction(ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData) {

        DocumentModel parentItem = nodeData.getParentItem();
        DocumentModel parentItemWc = documentManager.getWorkingCopy(
                parentItem.getRef());
        DocumentModel destinationDoc = nodeData.getDestinationItem();

        // TODO: Mirar si se puede mejorar esta consulta. Aquí vendría bien
        // tener una consulta de un subject a un versionseriresid de un object!
        // Para ver si existe y hacer switch si es necesario
        if (EloraRelationHelper.existsRelation(destinationDoc, parentItemWc,
                nodeData.getPredicate(), documentManager)) {
            // Resource predicateResource = new ResourceImpl(
            // nodeData.getPredicate());
            log.trace("Remove relation from |" + destinationDoc.getId()
                    + "| to |" + parentItemWc.getId() + "| with predicate |"
                    + nodeData.getPredicate() + "|");
            // RelationHelper.removeRelation(destinationDoc, predicateResource,
            // parentItemWc);
            eloraDocumentRelationManager.softDeleteRelation(documentManager,
                    destinationDoc, nodeData.getPredicate(), parentItemWc);
            log.trace("Relation removed");

            // Nuxeo Event
            String ecoReference = navigationContext.getCurrentDocument().getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            String removedReference = parentItemWc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            String comment = removedReference + " ("
                    + parentItemWc.getVersionLabel() + ") @" + ecoReference;
            EloraEventHelper.fireEvent(
                    RelationEventNames.CHECKED_OUT_AND_RELATION_REMOVED_EVENT,
                    destinationDoc, comment);

            marcActionDocumentsInTree(destinationDoc, null);
            EloraDocumentHelper.checkOutDocument(destinationDoc);
        }
    }

    private void executeReplaceAction(ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData) throws EloraException {

        DocumentModel parentDestDoc = parentNodeData.getDestinationItem();
        parentDestDoc.refresh();
        DocumentModel parentOrigDocWc = documentManager.getWorkingCopy(
                parentNodeData.getOriginItem().getRef());
        DocumentModel destinationDoc = nodeData.getDestinationItem();
        destinationDoc.refresh();

        if (!nodeData.getIsAnarchic()) {
            log.trace("About to manage relation for document |"
                    + destinationDoc.getId() + "|");
            manageRelations(destinationDoc, parentDestDoc, parentOrigDocWc,
                    nodeData.getPredicate(), true);
        }

        // Nuxeo Event
        String ecoReference = navigationContext.getCurrentDocument().getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String originReference = parentOrigDocWc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String destReference = parentDestDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String comment = originReference + " ("
                + parentOrigDocWc.getVersionLabel() + ") => " + destReference
                + " (" + parentDestDoc.getVersionLabel() + ") @" + ecoReference;
        EloraEventHelper.fireEvent(
                RelationEventNames.CHECKED_OUT_AND_RELATION_REPLACED_EVENT,
                destinationDoc, comment);

        marcActionDocumentsInTree(destinationDoc, parentDestDoc);
        EloraDocumentHelper.checkOutDocument(destinationDoc);

        log.trace("Replace action executed in document |"
                + destinationDoc.getId() + "|");

        // executeRemoveAction(nodeData, parentNodeData);
        // executeChangeAction(nodeData, parentNodeData);
    }

    public void overwriteDocs() {
        String logInitMsg = "[overwriteDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            overwrite = true;
            processCheckins(logInitMsg, null);

            // Raise an event for refreshing the impact matrix
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX);

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_ITEMS_BATCH_OVERWRITE_EVENT,
                    navigationContext.getCurrentDocument());

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void checkinDocs() {
        String logInitMsg = "[checkinDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        CheckinManager checkinManager = Framework.getService(
                CheckinManager.class);
        try {
            overwrite = false;
            processCheckins(logInitMsg, checkinManager);

            // Raise an event for refreshing the impact matrix
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX);

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_ITEMS_BATCH_CHECK_IN_EVENT,
                    navigationContext.getCurrentDocument());

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processCheckins(String logInitMsg,
            CheckinManager checkinManager) throws EloraException,
            CycleDetectedException, DocumentNotCheckedOutException {
        List<String> destinationWcUidList = new ArrayList<String>();
        TreeNode root = cmBomImpactedItemsTreeBean.getRoot();
        checkModifiedItemsAreManagedAndMajorReleased(root);
        List<String> sortedIds = sortTreeDocumentsForCheckin(logInitMsg, root);
        Boolean stopProcessing = false;
        for (String docId : sortedIds) {
            DocumentModel doc = helperDocMap.get(docId);
            if (doc != null) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    String ecoReference = navigationContext.getCurrentDocument().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    if (!overwrite) {
                        log.trace(
                                logInitMsg + "Proceeding to checkin document |"
                                        + docId + "|");
                        doc = checkinManager.checkinDocument(doc,
                                helperCommentMap.get(docId), false);

                        // Nuxeo Event
                        String comment = doc.getVersionLabel() + " @"
                                + ecoReference + " "
                                + helperCommentMap.get(docId);
                        EloraEventHelper.fireEvent(
                                PdmEventNames.PDM_CHECKED_IN_EVENT, doc,
                                comment);

                    } else {
                        doc = overwriteDoc(doc);

                        // Nuxeo Event
                        String comment = doc.getVersionLabel() + " @"
                                + ecoReference;
                        EloraEventHelper.fireEvent(
                                PdmEventNames.PDM_OVERWRITTEN_EVENT, doc,
                                comment);
                    }
                    destinationWcUidList.add(docId);
                } catch (CheckinNotAllowedException e) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.cm.batch.checkinNotAllowed"),
                            e.getCheckinDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getCheckinDocument().getTitle(),
                            e.getErrorDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getErrorDocument().getTitle());
                    TransactionHelper.setTransactionRollbackOnly();
                    stopProcessing = true;
                } catch (BomCharacteristicsValidatorException e) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.pdm.characteristicsRequired"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                    TransactionHelper.setTransactionRollbackOnly();
                    stopProcessing = true;
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
            cmBomImpactedItemsTreeBean.getNodeService().setAsManaged(
                    navigationContext.getCurrentDocument(), root,
                    destinationWcUidList);
        }
    }

    private List<String> sortTreeDocumentsForCheckin(String logInitMsg,
            TreeNode root) throws CycleDetectedException, EloraException {
        log.trace(logInitMsg + "Proceeding to build checkin DAG");
        dag = new DAG();
        helperDocMap = new HashMap<String, DocumentModel>();
        helperCommentMap = new HashMap<String, String>();
        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();
            DocumentModel parentDoc = null;
            if (!nodeData.getAction().equals(CMConstants.ACTION_REMOVE)) {
                parentDoc = nodeData.getDestinationItem();
            }
            buildCheckinDAG(modifiedItemNode, parentDoc, false);
        }
        @SuppressWarnings("unchecked")
        List<String> sortedIds = TopologicalSorter.sort(dag);
        log.trace(logInitMsg + "Sorted checkin DAG: |" + sortedIds.toString()
                + "|");
        return sortedIds;
    }

    private DocumentModel overwriteDoc(DocumentModel doc)
            throws CheckinNotAllowedException, EloraException,
            BomCharacteristicsValidatorException {
        String logInitMsg = "[overwriteDoc] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
        DocumentValidationService validator = Framework.getService(
                DocumentValidationService.class);

        log.trace(logInitMsg + "Start overwriting document |" + doc.getId()
                + "|");
        OverwriteVersionHelper.overwriteDocument(doc, baseDoc,
                eloraDocumentRelationManager, validator, documentManager);
        log.trace(logInitMsg + "Finished overwriting document |" + doc.getId()
                + "|");

        log.trace(logInitMsg + "Start restoring to version document |"
                + doc.getId() + "|");
        EloraDocumentHelper.restoreToVersion(doc.getRef(), baseDoc.getRef(),
                true, true, documentManager);
        log.trace(logInitMsg + "Finished restoring to version document |"
                + doc.getId() + "|");

        if (doc.isLocked()) {
            doc.removeLock();
        }

        doc.refresh();
        return doc;
    }

    private void checkModifiedItemsAreManagedAndReleased(TreeNode root)
            throws EloraException {
        String logInitMsg = "[checkModifiedItemsAreManagedAndReleased] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (TreeNode modifiedItemNode : root.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) modifiedItemNode.getData();
            if (!CmBatchProcessingHelper.isManaged(nodeData)) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedItemNotManaged"));
                throw new EloraException("All modified items must be managed");
            } else if (!EloraDocumentHelper.isReleased(
                    nodeData.getDestinationItem())) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.batch.modifiedNotReleased"));
                throw new EloraException("All modified items must be released");
            }
        }
        log.trace(logInitMsg + "Modified items are managed and released");
    }

    private void buildCheckinDAG(TreeNode node, DocumentModel parentDoc,
            boolean parentCheckedOut)
            throws CycleDetectedException, EloraException {
        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                DocumentModel destinationDoc = nodeData.getDestinationItem();
                destinationDoc.refresh();
                if (!nodeData.getIsManaged()) {
                    if (destinationDoc.isCheckedOut()) {
                        if (EloraDocumentHelper.isLockedByUserOrAdmin(
                                destinationDoc, documentManager)) {
                            if (parentDoc != null) {
                                // Modified is not removed
                                if (nodeData.getIsAnarchic()
                                        || EloraRelationHelper.existsRelation(
                                                destinationDoc, parentDoc,
                                                nodeData.getPredicate(),
                                                documentManager)) {
                                    // TODO: Igual hay que mirar si el vertice
                                    // existe de
                                    // antes
                                    dag.addVertex(destinationDoc.getId());
                                    helperDocMap.put(destinationDoc.getId(),
                                            destinationDoc);
                                    helperCommentMap.put(destinationDoc.getId(),
                                            nodeData.getComment());
                                    if (parentCheckedOut) {
                                        // Igual hay que mirar si el edge existe
                                        // de
                                        // antes
                                        dag.addEdge(destinationDoc.getId(),
                                                parentDoc.getId());
                                    } else {
                                        dag.addEdge(destinationDoc.getId(),
                                                null);
                                    }
                                } else {
                                    facesMessages.add(
                                            StatusMessage.Severity.ERROR,
                                            messages.get(
                                                    "eloraplm.message.error.cm.batch.relationNoExists"),
                                            destinationDoc.getPropertyValue(
                                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                            destinationDoc.getTitle());
                                    throw new EloraException(
                                            "Relation in impacted tree does not exist in checked out document");
                                }
                            }
                        } else {
                            facesMessages.add(StatusMessage.Severity.ERROR,
                                    messages.get(
                                            "eloraplm.message.error.cm.batch.documentNotLocked"),
                                    destinationDoc.getPropertyValue(
                                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                    destinationDoc.getTitle());
                            throw new EloraException(
                                    "Checked out document must be locked by user");
                        }
                    }
                }
                buildCheckinDAG(childNode, destinationDoc,
                        destinationDoc.isCheckedOut());
            }
        }
    }
}