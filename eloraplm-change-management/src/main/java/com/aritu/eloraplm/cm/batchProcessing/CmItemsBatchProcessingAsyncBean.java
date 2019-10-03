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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.batchProcessing.util.ResultType;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMBatchProcessingConstants;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.RelationEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
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
@Name("cmItemsBatchProcessingAsync")
public class CmItemsBatchProcessingAsyncBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmItemsBatchProcessingAsyncBean.class);

    private List<ResultType> successList = new ArrayList<ResultType>();

    private List<ResultType> errorsList = new ArrayList<ResultType>();

    private String exceptionErrorMsg = null;

    private boolean txStarted = false;

    private LoginStack loginStack = null;

    private String itemType = CMConstants.ITEM_TYPE_BOM;

    @Observer(CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS)
    public void executeActionsAsync(DocumentModel cmProcessDoc, TreeNode root,
            List<String> bomHierarchicalAndDirectRelations,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[executeActionsAsync] ["
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

            Map<DocumentModel, List<String>> actionTreeDocMap = new HashMap<>();

            // Process Impacted Items
            log.trace(logInitMsg + "Start processing impacted items");
            for (TreeNode modifiedItemNode : root.getChildren()) {
                processExecuteActionsOnItems(cmProcessDoc, modifiedItemNode,
                        actionTreeDocMap, eloraDocumentRelationManager,
                        session);
            }
            log.trace(logInitMsg + "Finished processing impacted items");

            // Reset processed counter and change processing action
            String processingAction = EloraMessageHelper.getTranslatedMessage(
                    session,
                    "eloraplm.message.warning.cm.batch.batchProcessInProgress."
                            + CMBatchProcessingConstants.FIX_RELATIONS);

            Events.instance().raiseEvent(
                    CMBatchProcessingEventNames.RESET_PROCESSED_COUNTER,
                    cmProcessDoc.getId(), processingAction);

            // Fix relations
            log.trace(logInitMsg
                    + "Start fixing relations to documents missing in tree");
            fixRelationsToDocumentsMissingInTree(
                    bomHierarchicalAndDirectRelations, actionTreeDocMap,
                    eloraDocumentRelationManager, session);
            log.trace(logInitMsg
                    + "Finished fixing relations to documents missing in tree");

        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing ExecuteActions: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg
                            + "Uncontrolled exception processing ExecuteActions: "
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
                    itemType, transitionToComeBackToPreviousState, txStarted,
                    successList, errorsList, exceptionErrorMsg, loginStack,
                    session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private void processExecuteActionsOnItems(DocumentModel cmProcessDoc,
            TreeNode node, Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {
        String logInitMsg = "[processExecuteActionsOnItems] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            ImpactedItemsNodeData parentNodeData = null;
            String action = null;
            boolean isDirect = false;

            if (nodeData.getIsDirectObject()) {
                nodeData = (ImpactedItemsNodeData) node.getData();
                parentNodeData = (ImpactedItemsNodeData) childNode.getData();
                action = nodeData.getAction();
                isDirect = true;
                if (!CmBatchProcessingHelper.isIgnored(parentNodeData)
                        && !CmBatchProcessingHelper.isManaged(parentNodeData)
                        && parentNodeData.getAction().equals(
                                CMConstants.ACTION_CHANGE)) {
                    DocumentModel destinationDoc = parentNodeData.getDestinationItem();
                    EloraDocumentHelper.checkOutDocument(destinationDoc);

                    marcActionDocumentsInTree(destinationDoc, null,
                            actionTreeDocMap);
                }
            } else {
                parentNodeData = (ImpactedItemsNodeData) node.getData();
                action = parentNodeData.getAction();
            }

            if (!CmBatchProcessingHelper.isIgnored(nodeData)) {
                if (!CmBatchProcessingHelper.isManaged(nodeData)) {

                    DocumentModel destinationItem = nodeData.getDestinationItem();
                    String documentId = destinationItem.getId();
                    String reference = destinationItem.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    String title = destinationItem.getTitle();

                    try {

                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        executeAction(cmProcessDoc, nodeData, parentNodeData,
                                action, isDirect, actionTreeDocMap,
                                eloraDocumentRelationManager, documentManager);

                        ResultType success = new ResultType(documentId,
                                reference, title);
                        successList.add(success);

                    } catch (Exception e) {
                        log.error(logInitMsg
                                + "Exception processing ExecuteAction on documentId =|"
                                + documentId + "|, reference = |" + reference
                                + "|. Exception details: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(), e);

                        String message = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.executeAction");

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
            processExecuteActionsOnItems(cmProcessDoc, childNode,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager);
        }
    }

    private void executeAction(DocumentModel cmProcessDoc,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData, String action,
            boolean isDirect, Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        switch (action) {
        case CMConstants.ACTION_CHANGE:
            log.trace("Execute change action");
            executeChangeAction(cmProcessReference, nodeData, parentNodeData,
                    isDirect, actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager);
            break;
        case CMConstants.ACTION_REMOVE:
            log.trace("Execute remove action");
            executeRemoveAction(cmProcessReference, nodeData, parentNodeData,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager);
            break;
        case CMConstants.ACTION_REPLACE:
            log.trace("Execute replace action");
            executeReplaceAction(cmProcessReference, nodeData, parentNodeData,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager);
            break;
        default:
            throw new EloraException("Action not defined in tree");
        }
    }

    private void executeChangeAction(String cmProcessReference,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData, boolean isDirectObject,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {

        DocumentModel parentDestDoc = null;
        if (isDirectObject && parentNodeData.getAction().equals(
                CMConstants.ACTION_IGNORE)) {
            // Si direct object está ignore no tiene destination y sacamos el
            // origen wc. ¿Si estuviera managed ataríamos con el AV?
            parentDestDoc = parentNodeData.getOriginItemWc();
        } else {
            parentDestDoc = parentNodeData.getDestinationItem();
        }
        parentDestDoc.refresh();
        // parentOrigDoc se utiliza cuando hay que hacer replace. Para los
        // direct no se utiliza ya que no llegan aquí los direct de los
        // modificados (en el unico sitio donde puede haber un replace)
        DocumentModel parentOrigDoc = parentNodeData.getOriginItem();
        DocumentModel destinationDoc = nodeData.getDestinationItem();
        destinationDoc.refresh();

        if (isDirectObject || !nodeData.getIsAnarchic()) {
            log.trace("About to manage relation for document |"
                    + destinationDoc.getId() + "|");
            String predicate = null;
            if (isDirectObject) {
                predicate = parentNodeData.getPredicate();
            } else {
                predicate = nodeData.getPredicate();
            }
            manageRelations(destinationDoc, parentDestDoc, parentOrigDoc,
                    predicate, false, eloraDocumentRelationManager,
                    documentManager);
            log.trace("Relation managed");
        }

        // Nuxeo Event
        String childReference = parentOrigDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String comment = childReference + " (" + parentOrigDoc.getVersionLabel()
                + ") => " + childReference + " ("
                + parentDestDoc.getVersionLabel() + ") @" + cmProcessReference;
        EloraEventHelper.fireEvent(
                RelationEventNames.CHECKED_OUT_AND_RELATION_CHANGED_EVENT,
                destinationDoc, comment);

        marcActionDocumentsInTree(destinationDoc, parentDestDoc,
                actionTreeDocMap);

        EloraDocumentHelper.checkOutDocument(destinationDoc);

        log.trace("Change action executed in document |"
                + destinationDoc.getId() + "|");
    }

    private void marcActionDocumentsInTree(DocumentModel subject,
            DocumentModel object,
            Map<DocumentModel, List<String>> actionTreeDocMap) {
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
            DocumentModel originObject, String predicate, boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {
        if (subject.isCheckedOut()) {
            log.trace("Document is checked out");
            manageCheckedOutDocRelations(subject, object, originObject,
                    predicate, replace, eloraDocumentRelationManager,
                    documentManager);
        } else {
            log.trace("Document is checked in");
            manageCheckedInDocRelations(subject, object, originObject,
                    predicate, replace, eloraDocumentRelationManager,
                    documentManager);
        }
    }

    private void manageCheckedOutDocRelations(DocumentModel subject,
            DocumentModel object, DocumentModel originObject, String predicate,
            boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {

        log.trace("Getting relations from document to any version of |"
                + object.getId() + "|");
        // TODO: Mirar si se puede mejorar esta consulta. Aquí vendría bien
        // tener una consulta de un subject a un versionseriresid de un object!
        // Para ver si existe y hacer switch si es necesario
        DocumentModelList objectDocList = getRelationsFromSubjectToAnyObjectVersion(
                subject, object, predicate, documentManager);
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
                        relatedObject, eloraDocumentRelationManager,
                        documentManager);
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
                switchRelation(subject, predicate, object, originObject,
                        eloraDocumentRelationManager, documentManager);
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
            DocumentModel subject, DocumentModel object, String predicate,
            CoreSession documentManager) {
        return EloraRelationHelper.getAllVersionsOfRelatedObject(subject,
                object, predicate, documentManager);
    }

    private void manageCheckedInDocRelations(DocumentModel subject,
            DocumentModel object, DocumentModel originObject, String predicate,
            boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {
        if (object.isCheckedOut()) {
            if (!EloraRelationHelper.existsRelation(subject, object, predicate,
                    documentManager)) {
                if (replace) {
                    log.trace(
                            "No existing relations. Proceed to replace relation from origin object |"
                                    + originObject.getId()
                                    + "| to destination object |"
                                    + object.getId() + "|");
                    switchRelation(subject, predicate, object, originObject,
                            eloraDocumentRelationManager, documentManager);
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
                    switchRelation(subject, predicate, object, originObject,
                            eloraDocumentRelationManager, documentManager);
                } else {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                }
            } else {
                switchRelation(subject, predicate, object, objectWc,
                        eloraDocumentRelationManager, documentManager);
                if (replace) {
                    // ¿Borramos la relacion a originObject? ¿Si no existe da
                    // error?
                }
            }

        }
    }

    private List<Statement> getBomHierarchicalStmts(DocumentModel doc,
            List<String> bomHierarchicalAndDirectRelations) {
        List<Resource> predicates = new ArrayList<>();
        for (String predicateUri : bomHierarchicalAndDirectRelations) {
            predicates.add(new ResourceImpl(predicateUri));
        }
        return EloraRelationHelper.getStatements(doc, predicates);
    }

    private void switchRelationWithLatestReleasedOrLatestVersion(
            DocumentModel doc, Statement bomStmt, DocumentModel objectBomDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {
        log.trace("Get latest released or latest version of item |"
                + objectBomDoc.getId() + "|");
        DocumentModel releasedBomDoc = EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                objectBomDoc);
        if (releasedBomDoc == null) {
            throw new EloraException("Document |" + objectBomDoc.getId()
                    + "| has no latest version or it is unreadable.");
        }
        log.trace("Item |" + releasedBomDoc.getId() + "| retrieved");
        if (!releasedBomDoc.getId().equals(objectBomDoc.getId())) {

            switchRelation(doc, bomStmt.getPredicate().getUri(), releasedBomDoc,
                    objectBomDoc, eloraDocumentRelationManager,
                    documentManager);

        }
    }

    private void switchRelation(DocumentModel subjectDoc, String predicateUri,
            DocumentModel objectDoc, DocumentModel oldObjectDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {
        // EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(oldStmt);
        log.trace("Switch relation");
        eloraDocumentRelationManager.updateRelation(documentManager, subjectDoc,
                predicateUri, oldObjectDoc, objectDoc);

        log.trace("Switched relation with subject |" + subjectDoc.getId()
                + "| Old object: |" + oldObjectDoc.getId() + "| New object: |"
                + objectDoc.getId() + "|");
    }

    private void executeRemoveAction(String cmProcessReference,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {

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
            String removedReference = parentItemWc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            String comment = removedReference + " ("
                    + parentItemWc.getVersionLabel() + ") @"
                    + cmProcessReference;
            EloraEventHelper.fireEvent(
                    RelationEventNames.CHECKED_OUT_AND_RELATION_REMOVED_EVENT,
                    destinationDoc, comment);

            marcActionDocumentsInTree(destinationDoc, null, actionTreeDocMap);
            EloraDocumentHelper.checkOutDocument(destinationDoc);
        }
    }

    private void executeReplaceAction(String cmProcessReference,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {

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
                    nodeData.getPredicate(), true, eloraDocumentRelationManager,
                    documentManager);
        }

        // Nuxeo Event
        String originReference = parentOrigDocWc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String destReference = parentDestDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String comment = originReference + " ("
                + parentOrigDocWc.getVersionLabel() + ") => " + destReference
                + " (" + parentDestDoc.getVersionLabel() + ") @"
                + cmProcessReference;
        EloraEventHelper.fireEvent(
                RelationEventNames.CHECKED_OUT_AND_RELATION_REPLACED_EVENT,
                destinationDoc, comment);

        marcActionDocumentsInTree(destinationDoc, parentDestDoc,
                actionTreeDocMap);
        EloraDocumentHelper.checkOutDocument(destinationDoc);

        log.trace("Replace action executed in document |"
                + destinationDoc.getId() + "|");
    }

    private void fixRelationsToDocumentsMissingInTree(
            List<String> bomHierarchicalAndDirectRelations,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {
        String logInitMsg = "[fixRelationsToDocumentsMissingInTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (Map.Entry<DocumentModel, List<String>> entry : actionTreeDocMap.entrySet()) {
            DocumentModel subject = entry.getKey();
            List<String> relatedTreeObjectList = entry.getValue();
            log.trace("Get bom hierarchical statements of item |"
                    + subject.getId() + "|");
            List<Statement> bomStmts = getBomHierarchicalStmts(subject,
                    bomHierarchicalAndDirectRelations);
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
                                bomStmt, objectBomDoc,
                                eloraDocumentRelationManager, documentManager);
                    }
                }
            } catch (Exception e) {
                String documentId = subject.getId();
                String reference = subject.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                String title = subject.getTitle();

                log.error(logInitMsg
                        + "Exception fixing relations missing in tree. documentId =|"
                        + documentId + "|, reference = |" + reference
                        + "|. Exception details: " + e.getClass().getName()
                        + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.fixRelationsMissingInTree");
                ResultType error = new ResultType(documentId, reference, title,
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();

            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
    }

    @Observer(CMBatchProcessingEventNames.CHECKIN_ITEMS)
    public void checkinAsync(DocumentModel cmProcessDoc, TreeNode root,
            List<String> sortedIds, Map<String, String> helperCommentMap,
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

            // Process checkins
            boolean overwrite = false;
            List<String> destinationWcUidList = processCheckinItems(logInitMsg,
                    cmProcessDoc, root, sortedIds, helperCommentMap, overwrite,
                    checkinManager, eloraDocumentRelationManager, session);

            log.trace(logInitMsg + "|" + successList.size()
                    + "| documents processed successfully");

            // Set as Managed successfully processed items
            if (destinationWcUidList != null
                    && destinationWcUidList.size() > 0) {
                try {
                    CMHelper.setAsManagedImpactedItemsByDestinationWcUidList(
                            session, cmProcessDoc, itemType,
                            destinationWcUidList);
                } catch (EloraException e) {
                    log.error(logInitMsg
                            + "An error occurred when setting as managed processed impacted items. Error message is: "
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
                    itemType, transitionToComeBackToPreviousState, txStarted,
                    successList, errorsList, exceptionErrorMsg, loginStack,
                    session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    @Observer(CMBatchProcessingEventNames.OVERWRITE_ITEMS)
    public void overwriteAsync(DocumentModel cmProcessDoc, TreeNode root,
            List<String> sortedIds, Map<String, String> helperCommentMap,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState) {
        String logInitMsg = "[overwriteAsync] ["
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

            // Process checkins
            boolean overwrite = true;
            List<String> destinationWcUidList = processCheckinItems(logInitMsg,
                    cmProcessDoc, root, sortedIds, helperCommentMap, overwrite,
                    checkinManager, eloraDocumentRelationManager, session);

            // Set as Managed successfully processed items
            if (destinationWcUidList != null
                    && destinationWcUidList.size() > 0) {
                try {
                    CMHelper.setAsManagedImpactedItemsByDestinationWcUidList(
                            session, cmProcessDoc, itemType,
                            destinationWcUidList);
                } catch (EloraException e) {
                    log.error(logInitMsg
                            + "An error occurred when setting as managed processed impacted items. Error message is: "
                            + e.getMessage(), e);
                }
            }

        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Exception processing Overwrite: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            exceptionErrorMsg = e.getMessage();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Exception processing Overwrite: "
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
                    itemType, transitionToComeBackToPreviousState, txStarted,
                    successList, errorsList, exceptionErrorMsg, loginStack,
                    session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private List<String> processCheckinItems(String logInitMsg,
            DocumentModel cmProcessDoc, TreeNode root, List<String> sortedIds,
            Map<String, String> helperCommentMap, boolean overwrite,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException,
            CycleDetectedException, DocumentNotCheckedOutException {

        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> destinationWcUidList = new ArrayList<String>();

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        Boolean stopProcessing = false;
        for (String docId : sortedIds) {
            try {
                DocumentModel doc = documentManager.getDocument(
                        new IdRef(docId));

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                if (!overwrite) {
                    log.trace(logInitMsg + "Proceeding to checkin document |"
                            + docId + "|");

                    doc = checkinManager.checkinDocument(doc,
                            helperCommentMap.get(docId), null,
                            cmProcessReference, false);

                } else {
                    log.trace(logInitMsg + "Proceeding to overwrite document |"
                            + docId + "|");

                    overwriteDoc(doc, eloraDocumentRelationManager,
                            documentManager, cmProcessReference);
                }

                destinationWcUidList.add(docId);

                String reference = doc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                ResultType success = new ResultType(docId, reference,
                        doc.getTitle());
                successList.add(success);

            } catch (CheckinNotAllowedException e) {
                DocumentModel checkinDocument = e.getCheckinDocument();
                String checkinDocumentId = checkinDocument.getId();
                String checkinDocumentReference = checkinDocument.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

                log.error(logInitMsg
                        + "Exception processing check in  on documentId =|"
                        + checkinDocumentId + "|, reference = |"
                        + checkinDocumentReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                DocumentModel errorCausedDocument = e.getErrorDocument();
                String errorCausedDocumentReference = errorCausedDocument.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

                Object[] messageParams = { errorCausedDocumentReference,
                        errorCausedDocument.getTitle() };
                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.itemCheckinNotAllowed",
                        messageParams);

                ResultType error = new ResultType(checkinDocumentId,
                        checkinDocumentReference, checkinDocument.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();
                stopProcessing = true;

            } catch (BomCharacteristicsValidatorException e) {
                DocumentModel checkinDocument = e.getDocument();
                String checkinDocumentId = checkinDocument.getId();
                String checkinDocumentReference = checkinDocument.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

                log.error(logInitMsg
                        + "Exception processing check in  on documentId =|"
                        + checkinDocumentId + "|, reference = |"
                        + checkinDocumentReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                Object[] messageParams = {
                        e.getDocument().getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                        e.getDocument().getTitle() };
                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.pdm.characteristicsRequired",
                        messageParams);

                ResultType error = new ResultType(checkinDocumentId,
                        checkinDocumentReference, checkinDocument.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();
                stopProcessing = true;

            } catch (Exception e) {
                DocumentModel checkinDocument = documentManager.getDocument(
                        new IdRef(docId));
                String checkinDocumentId = checkinDocument.getId();
                String checkinDocumentReference = checkinDocument.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

                log.error(logInitMsg
                        + "Exception processing check in  on documentId =|"
                        + checkinDocumentId + "|, reference = |"
                        + checkinDocumentReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.checkin");

                ResultType error = new ResultType(checkinDocumentId,
                        checkinDocumentReference, checkinDocument.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();
                stopProcessing = true;

            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            if (stopProcessing) {
                break;
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

    @Observer(CMBatchProcessingEventNames.PROMOTE_ITEMS)
    public void promoteAsync(DocumentModel cmProcessDoc, TreeNode root,
            List<String> sortedIds, DAG dag,
            Map<String, List<String>> childrenVersionSeriesMap,
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
                    cmProcessDoc, root, sortedIds, dag,
                    childrenVersionSeriesMap, eloraDocumentRelationManager,
                    session);
            log.trace(logInitMsg + "Finished promoting all docs");

            // Set as Managed successfully processed items
            if (destinationWcUidList != null
                    && destinationWcUidList.size() > 0) {
                try {
                    CMHelper.setAsManagedImpactedItemsByDestinationWcUidList(
                            session, cmProcessDoc, itemType,
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
                    itemType, transitionToComeBackToPreviousState, txStarted,
                    successList, errorsList, exceptionErrorMsg, loginStack,
                    session);
            // close opened session
            session.close();
        }
        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private List<String> processPromoteItems(DocumentModel cmProcessDoc,
            TreeNode root, List<String> sortedIds, DAG dag,
            Map<String, List<String>> childrenVersionSeriesMap,
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

        boolean stopProcessing = false;
        for (String docId : sortedIds) {
            log.trace(logInitMsg + "Start promoting document |" + docId + "|");

            DocumentModel doc = documentManager.getDocument(new IdRef(docId));
            String reference = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            String title = doc.getTitle();

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
                    if (areRelatedCadDocsReleased(doc, documentManager)) {
                        checkDocumentChildrenStates(doc, dag,
                                childrenVersionSeriesMap,
                                bomHierarchicalAndDirectRelations,
                                documentManager);

                        if (doc.getAllowedStateTransitions().contains(
                                EloraLifeCycleConstants.TRANS_APPROVE)) {

                            doc.followTransition(
                                    EloraLifeCycleConstants.TRANS_APPROVE);

                            // Log Nuxeo Event
                            String comment = doc.getVersionLabel() + " @"
                                    + cmProcessReference;
                            EloraEventHelper.fireEvent(
                                    PdmEventNames.PDM_PROMOTED_EVENT, doc,
                                    comment);

                            EloraDocumentHelper.disableVersioningDocument(doc);
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
                                documentManager.restoreToVersion(wcDoc.getRef(),
                                        doc.getRef(), true, true);
                            }
                            documentManager.removeLock(wcDoc.getRef());
                            destinationWcUidList.add(wcDoc.getId());
                            /*
                             * managedNodeIdList.add(
                             * helperNodeIdMap.get(docId));
                             */

                            ResultType success = new ResultType(docId,
                                    reference, title);
                            successList.add(success);

                            log.trace(
                                    logInitMsg + "Finished promoting document |"
                                            + docId + "|");

                        } else {
                            log.error(logInitMsg
                                    + "Document state does not support actual transition. docId = |"
                                    + docId + "|, reference =|" + reference
                                    + "|, title=|" + title + "|");

                            String message = EloraMessageHelper.getTranslatedMessage(
                                    documentManager,
                                    "eloraplm.message.error.cm.batch.itemNotAllowedTransition");
                            ResultType error = new ResultType(doc.getId(),
                                    reference, title, message);
                            errorsList.add(error);

                            TransactionHelper.setTransactionRollbackOnly();
                            stopProcessing = true;
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
                        stopProcessing = true;
                    }
                } else {
                    log.error(logInitMsg
                            + "Document has another released or obsolete document in the same major. docId = |"
                            + docId + "|, reference =|" + reference
                            + "|, title=|" + title + "|");

                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.itemWithReleasedOnMajor");
                    ResultType error = new ResultType(doc.getId(), reference,
                            title, message);
                    errorsList.add(error);

                    TransactionHelper.setTransactionRollbackOnly();
                    stopProcessing = true;
                }
            } catch (Exception e) {

                log.error(logInitMsg
                        + "Exception processing Promote on documentId =|"
                        + doc.getId() + "|, reference = |" + reference
                        + "|. Exception details: " + e.getClass().getName()
                        + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.promote");
                ResultType error = new ResultType(doc.getId(), reference, title,
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();
                stopProcessing = true;

            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            if (stopProcessing) {
                break;
            }

            // Increase processed counter
            Events.instance().raiseEvent(
                    CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                    cmProcessDoc.getId());
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return destinationWcUidList;
    }

    private boolean areRelatedCadDocsReleased(DocumentModel doc,
            CoreSession documentManager) {
        try {
            checkRelatedCadDocsReleased(doc, documentManager);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private void checkRelatedCadDocsReleased(DocumentModel doc,
            CoreSession documentManager) throws EloraException {
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

    private void checkDocumentChildrenStates(DocumentModel doc, DAG dag,
            Map<String, List<String>> childrenVersionSeriesMap,
            List<String> bomHierarchicalAndDirectRelations,
            CoreSession documentManager) throws EloraException {
        String logInitMsg = "[checkDocumentChildrenStates] ["
                + documentManager.getPrincipal().getName() + "] ";
        List<Statement> bomStmts = getBomHierarchicalStmts(doc,
                bomHierarchicalAndDirectRelations);
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
            if (childrenInTreeVersionSeriesIdList != null
                    && childrenInTreeVersionSeriesIdList.contains(
                            objectBomDoc.getVersionSeriesId())) {
                log.trace(logInitMsg + "A version of object |"
                        + objectBomDoc.getId() + "| is in tree");
                if (!childrenIdList.contains(objectBomDoc.getId())) {

                    String reference = doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    String title = doc.getTitle();

                    Object[] messageParams = { reference, title,
                            objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            objectBomDoc.getTitle() };
                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.itemCompositionWithDiffDocVersion",
                            messageParams);
                    ResultType error = new ResultType(doc.getId(), reference,
                            title, message);
                    errorsList.add(error);

                    String exceptionMsg = "Document to be promoted has different document version in composition and tree";
                    log.error(logInitMsg + exceptionMsg + "DOC reference =|"
                            + reference + "| title=|" + title
                            + "|  objetctBomDoc reference =|"
                            + objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE)
                            + "| title=|" + objectBomDoc.getTitle() + "|");

                    throw new EloraException(exceptionMsg);
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

                        String reference = doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                        String title = doc.getTitle();

                        Object[] messageParams = { reference, title,
                                objectBomDoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                objectBomDoc.getTitle() };
                        String message = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.documentWithNoLastReleasedChil",
                                messageParams);
                        ResultType error = new ResultType(doc.getId(),
                                reference, title, message);
                        errorsList.add(error);

                        String exceptionMsg = "Document has documents in its composition that are not latest released";
                        log.error(logInitMsg + exceptionMsg + "DOC reference =|"
                                + reference + "| title=|" + title
                                + "|  objetctBomDoc reference =|"
                                + objectBomDoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                + "| title=|" + objectBomDoc.getTitle() + "|");

                        throw new EloraException(exceptionMsg);
                    }
                } else {
                    String reference = doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    String title = doc.getTitle();

                    Object[] messageParams = {
                            objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            objectBomDoc.getTitle() };
                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.itemWithNoReleasedChild",
                            messageParams);

                    ResultType error = new ResultType(doc.getId(), reference,
                            title, message);
                    errorsList.add(error);

                    String exceptionMsg = "Document has no released documents in its composition";
                    log.error(logInitMsg + exceptionMsg + "DOC reference =|"
                            + reference + "| title=|" + title
                            + "|  objetctBomDoc reference =|"
                            + objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE)
                            + "| title=|" + objectBomDoc.getTitle() + "|");

                    throw new EloraException(exceptionMsg);

                }
            }
        }
    }

    @Observer(CMBatchProcessingEventNames.UNDO_CHECKOUT_ITEMS)
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
            processUndoCheckoutOnCheckedoutItems(cmProcessDoc, checkedOutDocs,
                    eloraDocumentRelationManager, session);

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
            log.error(
                    logInitMsg
                            + "Uncontrolled exception processing Undo Checkout: "
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
                    itemType, transitionToComeBackToPreviousState, txStarted,
                    successList, errorsList, exceptionErrorMsg, loginStack,
                    session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private void processUndoCheckoutOnCheckedoutItems(
            DocumentModel cmProcessDoc, List<DocumentModel> checkedOutDocs,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException,
            CycleDetectedException, DocumentNotCheckedOutException {

        String logInitMsg = "[processUndoCheckoutOnCheckedoutItems] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        for (DocumentModel checkedOutDoc : checkedOutDocs) {
            String checkedOutDocId = checkedOutDoc.getId();
            String checkedOutDocReference = checkedOutDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                log.trace(
                        logInitMsg + "Proceeding to undo checkout on document |"
                                + checkedOutDocId + "|");

                VersionModel version = new VersionModelImpl();
                DocumentModel latestVersion = EloraDocumentHelper.getLatestVersion(
                        checkedOutDoc);
                if (latestVersion == null) {
                    throw new EloraException("Document |"
                            + checkedOutDoc.getId()
                            + "| has no latest version or is unreadable.");
                }
                version.setId(latestVersion.getId());

                checkedOutDoc = EloraDocumentHelper.restoreWorkingCopyToVersion(
                        checkedOutDoc, version, eloraDocumentRelationManager,
                        documentManager);

                if (checkedOutDoc.isLocked()) {
                    checkedOutDoc.removeLock();
                }

                // Seam event
                Events.instance().raiseEvent(
                        PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, checkedOutDoc);

                // Log Nuxeo Event
                String comment = checkedOutDoc.getVersionLabel() + " @"
                        + cmProcessReference;
                EloraEventHelper.fireEvent(
                        PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, checkedOutDoc,
                        comment);

                ResultType success = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle());
                successList.add(success);

            } catch (EloraException e) {
                log.error(logInitMsg
                        + "Exception processing undo checkout on documentId =|"
                        + checkedOutDocId + "|, reference = |"
                        + checkedOutDocReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.undoCheckout");

                ResultType error = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle(),
                        message);
                errorsList.add(error);

                TransactionHelper.setTransactionRollbackOnly();

            } catch (Exception e) {
                log.error(logInitMsg
                        + "Exception processing undo checkout on documentId =|"
                        + checkedOutDocId + "|, reference = |"
                        + checkedOutDocReference + "|. Exception details: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                String message = EloraMessageHelper.getTranslatedMessage(
                        documentManager,
                        "eloraplm.message.error.cm.batch.undoCheckout");

                ResultType error = new ResultType(checkedOutDocId,
                        checkedOutDocReference, checkedOutDoc.getTitle(),
                        message);
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

        log.trace(logInitMsg + "--- EXIT --- ");
    }

}
