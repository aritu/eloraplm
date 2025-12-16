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
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.RelationEventNames;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CMMissingOriginRelationException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.pdm.overwrite.helper.OverwriteVersionHelper;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;
import com.aritu.eloraplm.queries.EloraQueryFactory;

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

    private String itemClass = CMConstants.ITEM_CLASS_IMPACTED;

    @Observer(CMBatchProcessingEventNames.EXECUTE_ACTIONS_ITEMS)
    public void executeActionsAsync(DocumentModel cmProcessDoc, TreeNode root,
            List<String> bomHierarchicalAndDirectRelations,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState,
            CheckoutService checkoutService) {
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
                        actionTreeDocMap, eloraDocumentRelationManager, session,
                        checkoutService);
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
            fixRelationsToDocumentsMissingInTree(cmProcessDoc,
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
            log.error(logInitMsg
                    + "Uncontrolled exception processing ExecuteActions: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

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
            CoreSession documentManager, CheckoutService checkoutService) {
        String logInitMsg = "[processExecuteActionsOnItems] ["
                + documentManager.getPrincipal().getName() + "] ";

        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (nodeData.getIsModifiedItem()) {
                processExecuteActionsOnItems(cmProcessDoc, childNode,
                        actionTreeDocMap, eloraDocumentRelationManager,
                        documentManager, checkoutService);
                continue;
            }
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
                        && (parentNodeData.getAction().equals(
                                CMConstants.ACTION_CHANGE)
                                || parentNodeData.getAction().equals(
                                        CMConstants.ACTION_OVERWRITE))) {

                    DocumentModel destinationDoc = parentNodeData.getDestinationItem();
                    try {
                        checkoutService.checkOutFromVersionInCmProcess(
                                destinationDoc,
                                parentNodeData.getDestinationItemSourceVersion());
                        marcActionDocumentsInTree(destinationDoc, null,
                                actionTreeDocMap);
                    } catch (EloraException | DocumentAlreadyLockedException
                            | DocumentInUnlockableStateException
                            | DocumentLockRightsException e) {
                        log.error(logInitMsg
                                + "Exception processing ExecuteAction on documentId =|"
                                + destinationDoc.getId()
                                + "|. Exception details: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(), e);
                    }
                }
            } else {
                parentNodeData = (ImpactedItemsNodeData) node.getData();
                action = parentNodeData.getAction();
            }

            if (!CmBatchProcessingHelper.isIgnored(nodeData)
                    && !CmBatchProcessingHelper.isManaged(nodeData)
                    && (!isDirect || !CmBatchProcessingHelper.isIgnored(
                            parentNodeData))) {

                DocumentModel destinationItem = nodeData.getDestinationItem();

                if (nodeData.getIsDerived()) {
                    try {
                        VersionModel version = new VersionModelImpl();
                        version.setId(
                                nodeData.getDestinationItemSourceVersion().getId());
                        checkoutService.checkOutFromVersion(destinationItem,
                                version);
                        // Since Derived items don't have any relations in the
                        // tree, their relations must not be fixed.
                        // For that reason, they are not added to
                        // actionTreeDocMap.
                    } catch (EloraException | DocumentAlreadyLockedException
                            | DocumentInUnlockableStateException
                            | DocumentLockRightsException e) {
                        log.error(logInitMsg
                                + "Exception processing ExecuteAction on documentId =|"
                                + destinationItem.getId()
                                + "|. Exception details: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(), e);
                    }

                } else {
                    String documentId = destinationItem.getId();
                    String reference = destinationItem.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                    String title = destinationItem.getTitle();

                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        executeAction(cmProcessDoc, nodeData, parentNodeData,
                                action, isDirect, actionTreeDocMap,
                                eloraDocumentRelationManager, documentManager,
                                checkoutService);

                        ResultType success = new ResultType(documentId,
                                reference, title);
                        successList.add(success);
                    } catch (CMMissingOriginRelationException e) {
                        String message = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.missingOriginRelation");

                        ResultType error = new ResultType(documentId, reference,
                                title, message);
                        errorsList.add(error);

                        TransactionHelper.setTransactionRollbackOnly();

                        continue;
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

                        // Increase processed counter
                        Events.instance().raiseEvent(
                                CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                                cmProcessDoc.getId());
                    }
                }
            }
            processExecuteActionsOnItems(cmProcessDoc, childNode,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager, checkoutService);
        }
    }

    private void executeAction(DocumentModel cmProcessDoc,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData, String action,
            boolean isDirect, Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager, CheckoutService checkoutService)
            throws EloraException, CMMissingOriginRelationException,
            DocumentAlreadyLockedException, DocumentInUnlockableStateException,
            DocumentLockRightsException {

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        switch (action) {
        case CMConstants.ACTION_CHANGE:
        case CMConstants.ACTION_OVERWRITE:
            log.trace("Execute change/overwrite action");
            executeChangeAction(cmProcessReference, nodeData, parentNodeData,
                    isDirect, actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager, checkoutService);
            break;
        case CMConstants.ACTION_REMOVE:
            log.trace("Execute remove action");
            executeRemoveAction(cmProcessReference, nodeData, parentNodeData,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager, checkoutService);
            break;
        case CMConstants.ACTION_REPLACE:
            log.trace("Execute replace action");
            executeReplaceAction(cmProcessReference, nodeData, parentNodeData,
                    actionTreeDocMap, eloraDocumentRelationManager,
                    documentManager, checkoutService);
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
            CoreSession documentManager, CheckoutService checkoutService)
            throws EloraException, CMMissingOriginRelationException,
            DocumentAlreadyLockedException, DocumentInUnlockableStateException,
            DocumentLockRightsException {

        DocumentModel parentDestDoc = null;
        if (isDirectObject && parentNodeData.getAction().equals(
                CMConstants.ACTION_IGNORE)) {
            // Si direct object está ignore no tiene destination y sacamos el
            // origen wc.
            parentDestDoc = parentNodeData.getOriginItemWc();
        } else {
            parentDestDoc = parentNodeData.getDestinationItem();
        }
        parentDestDoc.refresh();

        DocumentModel parentOrigDoc = parentNodeData.getOriginItem();
        DocumentModel originDoc = nodeData.getOriginItem();
        DocumentModel destinationDoc = nodeData.getDestinationItem();
        destinationDoc.refresh();
        boolean isFromMbom = nodeData.getIsFromMbom();

        if (isDirectObject || !nodeData.getIsAnarchic()) {
            log.trace("About to manage relation for document |"
                    + destinationDoc.getId() + "|");
            String predicate = null;
            if (isDirectObject) {
                predicate = parentNodeData.getPredicate();
            } else {
                predicate = nodeData.getPredicate();
            }
            manageRelations(destinationDoc, originDoc, parentDestDoc,
                    parentOrigDoc, isFromMbom, predicate, false,
                    eloraDocumentRelationManager, documentManager);
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

        checkoutService.checkOutFromVersionInCmProcess(destinationDoc,
                nodeData.getDestinationItemSourceVersion());

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

    private void manageRelations(DocumentModel subject,
            DocumentModel originSubject, DocumentModel object,
            DocumentModel originObject, boolean isFromMbom, String predicate,
            boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager)
            throws EloraException, CMMissingOriginRelationException {
        if (subject.isCheckedOut()) {
            log.trace("Document is checked out");
            manageCheckedOutDocRelations(subject, originSubject, object,
                    originObject, isFromMbom, predicate, replace,
                    eloraDocumentRelationManager, documentManager);
        } else {
            log.trace("Document is checked in");
            manageCheckedInDocRelations(subject, originSubject, object,
                    originObject, isFromMbom, predicate, replace,
                    eloraDocumentRelationManager, documentManager);
        }
    }

    private void manageCheckedOutDocRelations(DocumentModel subject,
            DocumentModel originSubject, DocumentModel object,
            DocumentModel originObject, boolean isFromMbom, String predicate,
            boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager)
            throws EloraException, CMMissingOriginRelationException {

        Statement origStmt = EloraRelationHelper.getStatement(
                EloraRelationConstants.ELORA_GRAPH_NAME, originSubject,
                new ResourceImpl(predicate), originObject);

        // If it is mbom we continue executing actions
        if (origStmt == null && !isFromMbom) {
            throw new CMMissingOriginRelationException(originObject);
        }

        log.trace("Getting relations from document to any version of |"
                + object.getId() + "|");
        // TODO: Mirar si se puede mejorar esta consulta. Aquí vendría bien
        // tener una consulta de un subject a un versionseriesid de un
        // object!
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

                // Aqui entra en replace si le hemos cambiado la relación
                // desde fuera a otra AV por ejemplo.
                // Se tiene que hacer el switch de la relación del wc de
                // forma normal.

                if (origStmt == null) {
                    origStmt = EloraRelationHelper.getStatement(
                            EloraRelationConstants.ELORA_GRAPH_NAME, subject,
                            new ResourceImpl(predicate), object);
                }

                Statement stmt = new StatementImpl(
                        RelationHelper.getDocumentResource(subject),
                        new ResourceImpl(predicate),
                        RelationHelper.getDocumentResource(relatedObject));

                switchRelation(subject, stmt.getPredicate().getUri(), object,
                        relatedObject, origStmt, eloraDocumentRelationManager,
                        documentManager);

            }
        } else if (!isFromMbom) {
            if (replace) {
                // TODO: Este bloque se utiliza igual en
                // manageCheckedInDoCRelations y se puede sacar una función
                log.trace(
                        "No existing relations. Proceed to replace relation from origin object |"
                                + originObject.getId()
                                + "| to destination object |" + object.getId()
                                + "|");

                DocumentModel originObjectWc = documentManager.getWorkingCopy(
                        originObject.getRef());

                if (!EloraRelationHelper.existsRelation(subject, originObjectWc,
                        predicate, documentManager)) {
                    // No existe relación ni al origen ni al nuevo destino.
                    // Se ha borrado desde fuera. Hay que añadir relación al
                    // nuevo destino
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                } else {
                    // No existe relación al destino nuevo pero si existe la
                    // relación al origen. O estaba checked out antes de
                    // calcular el impacto o se ha borrado la relación desde
                    // fuera. Hay que modificar la relación que apunta al
                    // origen para que apunte al destino.
                    switchRelation(subject, predicate, object, originObjectWc,
                            origStmt, eloraDocumentRelationManager,
                            documentManager);
                }
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
            DocumentModel originSubject, DocumentModel object,
            DocumentModel originObject, boolean isFromMbom, String predicate,
            boolean replace,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager)
            throws CMMissingOriginRelationException, EloraException {

        Statement origStmt = EloraRelationHelper.getStatement(
                EloraRelationConstants.ELORA_GRAPH_NAME, originSubject,
                new ResourceImpl(predicate), originObject);

        // If it is mbom we continue executing actions
        if (origStmt == null && !isFromMbom) {
            throw new CMMissingOriginRelationException(originObject);
        }

        DocumentModel originObjectWc = documentManager.getWorkingCopy(
                originObject.getRef());

        if (object.isCheckedOut()) {
            if (!EloraRelationHelper.existsRelation(subject, object, predicate,
                    documentManager) && !isFromMbom) {
                if (replace) {
                    // Puede llegar aquí si hacemos check out del object desde
                    // fuera y luego ejecutamos por primera vez la ECO. En ese
                    // caso se tiene que hacer switch si existe la relación con
                    // el subject origen y add si no existe.
                    log.trace(
                            "No existing relations. Proceed to replace relation from origin object |"
                                    + originObject.getId()
                                    + "| to destination object |"
                                    + object.getId() + "|");

                    if (!EloraRelationHelper.existsRelation(subject,
                            originObjectWc, predicate, documentManager)) {
                        // No existe relación ni al origen ni al nuevo destino.
                        // Se ha borrado desde fuera. Hay que añadir relación al
                        // nuevo destino
                        eloraDocumentRelationManager.addRelation(
                                documentManager, subject,
                                RelationHelper.getDocumentResource(object),
                                predicate, null, "1");
                    } else {
                        // No existe relación al destino nuevo pero si
                        // existe la relación al origen. O estaba checked out
                        // antes de calcular el impacto o se ha borrado la
                        // relación desde fuera. Hay que modificar la relación
                        // que apunta al origen para que apunte al destino.
                        switchRelation(subject, predicate, object,
                                originObjectWc, origStmt,
                                eloraDocumentRelationManager, documentManager);
                    }
                } else {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                }
            }
        } else {
            DocumentModel objectWc = documentManager.getWorkingCopy(
                    object.getRef());

            if (EloraRelationHelper.existsRelation(subject, objectWc, predicate,
                    documentManager)) {
                if (replace) {
                    // Update replacer item relation:
                    // - new quantity is the SUM of replacer and replaced item
                    // relations quantities
                    // - new ordering is the MINIMUM between replacer and
                    // replaced item relations ordering
                    EloraStatementInfo origStmtInfo = new EloraStatementInfoImpl(
                            EloraRelationHelper.getStatement(
                                    EloraRelationConstants.ELORA_GRAPH_NAME,
                                    subject, new ResourceImpl(predicate),
                                    originObjectWc));
                    int originStmtQty = (origStmtInfo.getQuantity() == null) ? 0
                            : Integer.valueOf(origStmtInfo.getQuantity());

                    Statement destStmt = EloraRelationHelper.getStatement(
                            EloraRelationConstants.ELORA_GRAPH_NAME, subject,
                            new ResourceImpl(predicate), objectWc);
                    EloraStatementInfo destStmtInfo = new EloraStatementInfoImpl(
                            destStmt);
                    int destStmtQty = (destStmtInfo.getQuantity() == null) ? 0
                            : Integer.valueOf(destStmtInfo.getQuantity());

                    int newQuantity = originStmtQty + destStmtQty;
                    Integer newOrdering = Math.min(origStmtInfo.getOrdering(),
                            destStmtInfo.getOrdering());

                    eloraDocumentRelationManager.updateQuantityAndOrderingInRelation(
                            documentManager, subject, predicate, objectWc,
                            String.valueOf(newQuantity), newOrdering);

                    // Remove replaced item relation
                    eloraDocumentRelationManager.softDeleteRelation(
                            documentManager, subject, predicate,
                            originObjectWc);
                } else {
                    if (origStmt == null) {
                        origStmt = EloraRelationHelper.getStatement(
                                EloraRelationConstants.ELORA_GRAPH_NAME,
                                subject, new ResourceImpl(predicate), objectWc);
                    }
                    switchRelation(subject, predicate, object, objectWc,
                            origStmt, eloraDocumentRelationManager,
                            documentManager);
                }
            } else if (!isFromMbom) {
                if (replace) {
                    log.trace(
                            "No existing relations. Proceed to replace relation from origin object |"
                                    + originObject.getId()
                                    + "| to destination object |"
                                    + object.getId() + "|");

                    switchRelation(subject, predicate, object, originObjectWc,
                            origStmt, eloraDocumentRelationManager,
                            documentManager);
                } else {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subject, RelationHelper.getDocumentResource(object),
                            predicate, null, "1");
                }
            }
        }
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
                    objectBomDoc, bomStmt, eloraDocumentRelationManager,
                    documentManager);

        }
    }

    private void switchRelation(DocumentModel subjectDoc, String predicateUri,
            DocumentModel objectDoc, DocumentModel oldObjectDoc,
            Statement oldStmt,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {

        log.trace("Switch relation");

        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(oldStmt);

        eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                documentManager, subjectDoc, predicateUri, oldObjectDoc,
                objectDoc, stmtInfo.getQuantity(), stmtInfo.getOrdering(),
                stmtInfo.getDirectorOrdering(), stmtInfo.getViewerOrdering(),
                stmtInfo.getInverseViewerOrdering(), stmtInfo.getIsManual(),
                stmtInfo.getDesignators(), stmtInfo.getIsExcludedFromMapping());

        log.trace("Switched relation with subject |" + subjectDoc.getId()
                + "| Old object: |" + oldObjectDoc.getId() + "| New object: |"
                + objectDoc.getId() + "|");
    }

    private void executeRemoveAction(String cmProcessReference,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager, CheckoutService checkoutService)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

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
            checkoutService.checkOutFromVersionInCmProcess(destinationDoc,
                    nodeData.getDestinationItemSourceVersion());
        }
    }

    private void executeReplaceAction(String cmProcessReference,
            ImpactedItemsNodeData nodeData,
            ImpactedItemsNodeData parentNodeData,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager, CheckoutService checkoutService)
            throws EloraException, CMMissingOriginRelationException,
            DocumentAlreadyLockedException, DocumentInUnlockableStateException,
            DocumentLockRightsException {

        DocumentModel parentDestDoc = parentNodeData.getDestinationItem();
        parentDestDoc.refresh();

        // CHANGE: Antes se utilizaba parentOrigDocWC pero no se el motivo. He
        // puesto parentOrigDoc para poder sacar el stmt original. Con el wc no
        // puedo sacar el stmt orignal porque no existe la relación entre el
        // impactado AV y el modificado WC (antes no se calculaba el stmt
        // original). Creo que antes daba igual qué cogíamos porque al final, en
        // el update de las relaciones, se cambia el object original del
        // impactado, que es este parentOrigDocWc. Como se cambiaba por el
        // object nuevo daba igual lo que llegaba como object.

        // DocumentModel parentOrigDocWc =
        // documentManager.getWorkingCopy(parentNodeData.getOriginItem().getRef());
        DocumentModel parentOrigDoc = parentNodeData.getOriginItem();
        DocumentModel destinationDoc = nodeData.getDestinationItem();
        DocumentModel originDoc = nodeData.getOriginItem();
        destinationDoc.refresh();
        boolean isFromMbom = nodeData.getIsFromMbom();

        // Debugear esto con el direct. Mirar si alguna vez llega aquí por si le
        // molesta el cambio de wc a av que hemos hecho con el parentOrigDoc

        if (!nodeData.getIsAnarchic()) {
            log.trace("About to manage relation for document |"
                    + destinationDoc.getId() + "|");

            manageRelations(destinationDoc, originDoc, parentDestDoc,
                    parentOrigDoc, isFromMbom, nodeData.getPredicate(), true,
                    eloraDocumentRelationManager, documentManager);
        }

        // Nuxeo Event
        String originReference = parentOrigDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String destReference = parentDestDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String comment = originReference + " ("
                + parentOrigDoc.getVersionLabel() + ") => " + destReference
                + " (" + parentDestDoc.getVersionLabel() + ") @"
                + cmProcessReference;
        EloraEventHelper.fireEvent(
                RelationEventNames.CHECKED_OUT_AND_RELATION_REPLACED_EVENT,
                destinationDoc, comment);

        marcActionDocumentsInTree(destinationDoc, parentDestDoc,
                actionTreeDocMap);
        checkoutService.checkOutFromVersionInCmProcess(destinationDoc,
                nodeData.getDestinationItemSourceVersion());

        log.trace("Replace action executed in document |"
                + destinationDoc.getId() + "|");
    }

    private void fixRelationsToDocumentsMissingInTree(
            DocumentModel cmProcessDoc,
            List<String> bomHierarchicalAndDirectRelations,
            Map<DocumentModel, List<String>> actionTreeDocMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) {
        String logInitMsg = "[fixRelationsToDocumentsMissingInTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        for (Map.Entry<DocumentModel, List<String>> entry : actionTreeDocMap.entrySet()) {
            DocumentModel subject = entry.getKey();

            try {

                List<String> relatedTreeObjectList = entry.getValue();

                boolean subjectDocCreatedFromNonCorrelativeVersion = (boolean) subject.getPropertyValue(
                        EloraMetadataConstants.ELORA_VINF_NON_CORRELATIVE_VERSION);

                if (subjectDocCreatedFromNonCorrelativeVersion) {

                    // If subject document has been created from a non
                    // correlative version, following actions will be done:
                    // - Find the subject document source version
                    // - Restore relations from source version. During this
                    // restore, documents that are included in
                    // relatedTreeObjectList list should not be modified.

                    VersionModel sourceVersion = null;
                    String sourceVersionDocId = "";
                    if (subject.getPropertyValue(
                            EloraMetadataConstants.ELORA_VINF_SOURCE_VERSION) != null) {
                        sourceVersionDocId = (String) subject.getPropertyValue(
                                EloraMetadataConstants.ELORA_VINF_SOURCE_VERSION);
                        if (sourceVersionDocId.length() > 0) {
                            sourceVersion = new VersionModelImpl();
                            sourceVersion.setId(sourceVersionDocId);
                        }
                    }
                    if (sourceVersion != null) {
                        // create a list with the versionSeriesId of the
                        // documents to be excluded from being restored
                        List<String> excludedDocsVersionSeriesIds = new ArrayList<>();
                        for (String relatedTreeObject : relatedTreeObjectList) {
                            DocumentModel excludedDoc = documentManager.getDocument(
                                    new IdRef(relatedTreeObject));
                            excludedDocsVersionSeriesIds.add(
                                    excludedDoc.getVersionSeriesId());
                        }

                        EloraRelationHelper.restoreCMRelationsAfterCheckoutFromVersion(
                                subject, sourceVersion,
                                eloraDocumentRelationManager,
                                excludedDocsVersionSeriesIds, documentManager);

                    } else {
                        log.trace(logInitMsg
                                + "Not able to found sourceVersion for subject id = |"
                                + subject.getId() + "|");
                        throw new EloraException(
                                "Not able to found sourceVersion for subject id = |"
                                        + subject.getId() + "|");
                    }
                } else {

                    log.trace("Get bom hierarchical statements of item |"
                            + subject.getId() + "|");

                    // ----------------------------------------------------------------------------------------------
                    // By default, relations will be switched to latest released
                    // or latest version. But, if property
                    // com.aritu.eloraplm.cm.switch.relations.to.astored.when.fixing.relations.in.items.impact.matrix
                    // is set to true, relations will be switched to the AV
                    // version. Relation will be switched only if the relation
                    // is currently pointing to a otherwise it will not be
                    // switched.
                    // ----------------------------------------------------------------------------------------------

                    boolean switchRelationsToAsStored = Boolean.valueOf(
                            Framework.getProperty(
                                    EloraPropertiesConstants.PROP_CM_SWITCH_RELATIONS_TO_ASSTORED_WHEN_FIXING_RELATIONS_IN_ITEMS_IMPACT_MATRIX,
                                    Boolean.toString(false)));
                    List<Statement> bomStmts = null;
                    if (!switchRelationsToAsStored) {
                        bomStmts = CmBatchProcessingHelper.getBomHierarchicalStmts(
                                subject, bomHierarchicalAndDirectRelations);
                    } else {
                        DocumentModel subjectAv = EloraDocumentHelper.getBaseVersion(
                                subject);
                        bomStmts = CmBatchProcessingHelper.getBomHierarchicalStmts(
                                subjectAv, bomHierarchicalAndDirectRelations);
                    }

                    log.trace("Get bom hierarchical statements of item |"
                            + subject.getId() + "|");

                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    for (Statement bomStmt : bomStmts) {
                        DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                                bomStmt.getObject(), documentManager);
                        if (objectBomDoc == null) {
                            log.trace(logInitMsg
                                    + "Throw DocumentUnreadableException  since objectBomDoc is null. bomStmt = |"
                                    + bomStmt.toString() + "|");
                            throw new DocumentUnreadableException(
                                    "Error getting document from statement |"
                                            + bomStmt.toString() + "|");
                        }

                        DocumentModel objectBomDocWc = null;
                        boolean hasToSwitch = false;

                        if (switchRelationsToAsStored) {
                            objectBomDocWc = documentManager.getWorkingCopy(
                                    objectBomDoc.getRef());
                            hasToSwitch = !relatedTreeObjectList.contains(
                                    objectBomDoc.getId())
                                    && !relatedTreeObjectList.contains(
                                            objectBomDocWc.getId());
                            log.trace("Item |" + objectBomDoc.getId()
                                    + "| and Item |" + objectBomDocWc.getId()
                                    + "| not in tree");
                        } else {
                            hasToSwitch = !relatedTreeObjectList.contains(
                                    objectBomDoc.getId());
                            log.trace("Item |" + objectBomDoc.getId()
                                    + "| not in tree");
                        }

                        if (hasToSwitch) {
                            if (!switchRelationsToAsStored) {
                                switchRelationWithLatestReleasedOrLatestVersion(
                                        subject, bomStmt, objectBomDoc,
                                        eloraDocumentRelationManager,
                                        documentManager);
                            } else {
                                // Before switching the relation, check if it
                                // exist.
                                boolean relationExist = EloraRelationHelper.existsRelation(
                                        subject, objectBomDocWc,
                                        bomStmt.getPredicate().getUri(),
                                        documentManager);
                                if (relationExist) {
                                    switchRelation(subject,
                                            bomStmt.getPredicate().getUri(),
                                            objectBomDoc, objectBomDocWc,
                                            bomStmt,
                                            eloraDocumentRelationManager,
                                            documentManager);
                                }
                            }
                        }
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

                // Increase processed counter
                Events.instance().raiseEvent(
                        CMBatchProcessingEventNames.INCREASE_PROCESSED_COUNTER,
                        cmProcessDoc.getId());
            }
        }
    }

    @Observer(CMBatchProcessingEventNames.CHECKIN_ITEMS)
    public void checkinAsync(DocumentModel cmProcessDoc, List<String> sortedIds,
            Map<String, ImpactedItemsNodeData> nodeMap,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            String transitionToComeBackToPreviousState, boolean keepLock) {
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

            List<String> destinationWcUidList = processCheckinItems(logInitMsg,
                    cmProcessDoc, sortedIds, nodeMap, checkinManager,
                    eloraDocumentRelationManager, keepLock, session);

            log.trace(logInitMsg + "|" + successList.size()
                    + "| documents processed successfully");

            setAsManagedProcessedItems(cmProcessDoc, logInitMsg, session,
                    destinationWcUidList);

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
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

    private void setAsManagedProcessedItems(DocumentModel cmProcessDoc,
            String logInitMsg, CoreSession session,
            List<String> destinationWcUidList) {
        if (destinationWcUidList != null && destinationWcUidList.size() > 0) {
            try {
                CMHelper.setAsManagedItemsByDestinationWcUidList(session,
                        cmProcessDoc, itemType, itemClass,
                        destinationWcUidList);
            } catch (EloraException e) {
                log.error(logInitMsg
                        + "An error occurred when setting as managed processed impacted items. Error message is: "
                        + e.getMessage(), e);
            }
        }
    }

    private List<String> processCheckinItems(String logInitMsg,
            DocumentModel cmProcessDoc, List<String> sortedIds,
            Map<String, ImpactedItemsNodeData> nodeMap,
            CheckinManager checkinManager,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            boolean keepLock, CoreSession documentManager)
            throws EloraException, CycleDetectedException,
            DocumentNotCheckedOutException {

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

                boolean overwrite = nodeMap.get(docId).getAction().equals(
                        CMConstants.ACTION_OVERWRITE);

                if (!overwrite) {
                    log.trace(logInitMsg + "Proceeding to checkin document |"
                            + docId + "|");

                    doc = checkinManager.checkinDocument(doc,
                            nodeMap.get(docId).getComment(), null,
                            cmProcessReference, !keepLock);

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
            String transitionToComeBackToPreviousState,
            boolean forcePromotion) {

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
                    forcePromotion, session);
            log.trace(logInitMsg + "Finished promoting all docs");

            setAsManagedProcessedItems(cmProcessDoc, logInitMsg, session,
                    destinationWcUidList);
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
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);
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
            boolean forcePromotion, CoreSession documentManager)
            throws EloraException {
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

                DocumentModel wcDoc = documentManager.getWorkingCopy(
                        doc.getRef());
                
                boolean hasAtLeastOneApprovedVersion = false;
                Long previousApprovedVersions = EloraQueryFactory.countApprovedVersionsOfDocumentQuery(
                        documentManager, wcDoc.getId(), doc.getType());
                if (previousApprovedVersions > 0) {
                    hasAtLeastOneApprovedVersion = true;
                }

                if (hasAtLeastOneApprovedVersion || forcePromotion) {
                    
                    // TODO: cambiar este 'check' por un 'is' para que se
                    // entienda
                    // mejor. También convendría moverlo de promotehelper...
                    if (PromoteHelper.checkReleasedInMajor(doc,
                            documentManager)) {
                        log.trace(logInitMsg
                                + "Finished checking release or obsolete in major of document |"
                                + docId + "|");
                        if (CmBatchProcessingHelper.areBomRelatedDocsReleased(
                                doc, documentManager)) {
                            checkDocumentChildrenStates(doc, dag,
                                    childrenVersionSeriesMap,
                                    bomHierarchicalAndDirectRelations,
                                    documentManager);

                            if (doc.getAllowedStateTransitions().contains(
                                    EloraLifeCycleConstants.TRANS_APPROVE)) {

                                processPromote(eloraDocumentRelationManager,
                                        documentManager, logInitMsg,
                                        destinationWcUidList,
                                        cmProcessReference, docId, doc, wcDoc,
                                        reference, title);

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
                                + "Document has another released document in the same major. docId = |"
                                + docId + "|, reference =|" + reference
                                + "|, title=|" + title + "|");

                        String message = EloraMessageHelper.getTranslatedMessage(
                                documentManager,
                                "eloraplm.message.error.cm.batch.itemWithReleasedOnMajor");
                        ResultType error = new ResultType(doc.getId(),
                                reference, title, message);
                        errorsList.add(error);

                        TransactionHelper.setTransactionRollbackOnly();
                        stopProcessing = true;
                    }
                } else {
                    log.trace(
                            "Document is not approved because previous version is not "
                                    + "approved and action is not forced. docId = |"
                                    + docId + "|, reference =|" + reference
                                    + "|, title=|" + title);
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
                        message + ": " + e.getMessage());
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

    private void processPromote(
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager, String logInitMsg,
            List<String> destinationWcUidList, String cmProcessReference,
            String docId, DocumentModel doc, DocumentModel wcDoc,
            String reference, String title) throws EloraException {

        String previousState = doc.getCurrentLifeCycleState();
        doc.followTransition(EloraLifeCycleConstants.TRANS_APPROVE);

        // Log Nuxeo Event
        String comment = doc.getVersionLabel() + " @" + cmProcessReference;
        Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
        ctxProperties.put("comment", comment);
        ctxProperties.put("previousState", previousState);
        EloraEventHelper.fireEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc,
                ctxProperties);

        EloraDocumentHelper.disableVersioningDocument(doc);
        documentManager.saveDocument(doc);

        // DocumentModel wcDoc = documentManager.getWorkingCopy(doc.getRef());
        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
        if (baseDoc == null) {
            throw new EloraException("Document |" + wcDoc.getId()
                    + "| has no base version. Probably because it has no AVs.");
        }
        if (!baseDoc.getId().equals(doc.getId())) {
            VersionModel version = new VersionModelImpl();
            version.setId(doc.getId());
            EloraDocumentHelper.restoreWorkingCopyToVersion(wcDoc, version,
                    eloraDocumentRelationManager, documentManager);
        } else {
            EloraDocumentHelper.restoreToVersion(wcDoc.getRef(), doc.getRef(),
                    true, true, documentManager);
        }
        documentManager.removeLock(wcDoc.getRef());
        // Fire Approved event
        EloraEventHelper.fireEvent(PdmEventNames.PDM_APPROVED_EVENT, doc);

        destinationWcUidList.add(wcDoc.getId());
        /*
         * managedNodeIdList.add(
         * helperNodeIdMap.get(docId));
         */

        ResultType success = new ResultType(docId, reference, title);
        successList.add(success);

        log.trace(logInitMsg + "Finished promoting document |" + docId + "|");
    }

    private void checkDocumentChildrenStates(DocumentModel doc, DAG dag,
            Map<String, List<String>> childrenVersionSeriesMap,
            List<String> bomHierarchicalAndDirectRelations,
            CoreSession documentManager)
            throws EloraException, DocumentUnreadableException {
        String logInitMsg = "[checkDocumentChildrenStates] ["
                + documentManager.getPrincipal().getName() + "] ";
        List<Statement> bomStmts = CmBatchProcessingHelper.getBomHierarchicalStmts(
                doc, bomHierarchicalAndDirectRelations);
        log.trace(logInitMsg + "Retrieved |" + bomStmts.size()
                + "| bom hierarchical statements of document |" + doc.getId()
                + "|");
        @SuppressWarnings("unchecked")
        List<String> childrenIdList = dag.getChildLabels(doc.getId());
        for (Statement bomStmt : bomStmts) {
            DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                    bomStmt.getObject(), documentManager);
            if (objectBomDoc == null) {
                log.trace(logInitMsg
                        + "Throw DocumentUnreadableException  since objectBomDoc is null. bomStmt = |"
                        + bomStmt.toString() + "|");
                throw new DocumentUnreadableException(
                        "Error getting document from statement |"
                                + bomStmt.toString() + "|");
            }
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
                    log.error(logInitMsg
                            + "Document to be promoted has different document version in composition and tree. DOC reference =|"
                            + reference + "| title=|" + title
                            + "|  objetctBomDoc reference =|"
                            + objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE)
                            + "| title=|" + objectBomDoc.getTitle() + "|");

                    throw new EloraException(message);
                }
            } else {
                log.trace(logInitMsg + "Object |" + objectBomDoc.getId()
                        + "| is NOT in tree");
                if (EloraDocumentHelper.isReleased(objectBomDoc)) {
                    log.trace(logInitMsg + "Object |" + objectBomDoc.getId()
                            + "| is released");
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
                    log.error(logInitMsg
                            + "Document has no released documents in its composition. DOC reference =|"
                            + reference + "| title=|" + title
                            + "|  objetctBomDoc reference =|"
                            + objectBomDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE)
                            + "| title=|" + objectBomDoc.getTitle() + "|");

                    throw new EloraException(message);
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
            CmBatchProcessingHelper.processUndoCheckoutOnCheckedoutItems(
                    cmProcessDoc, checkedOutDocs, eloraDocumentRelationManager,
                    successList, errorsList, session);

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
            log.error(logInitMsg
                    + "Uncontrolled exception processing Undo Checkout: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            exceptionErrorMsg = (e.getMessage() != null) ? e.getMessage()
                    : (e.getCause() != null) ? e.getCause().toString()
                            : e.getClass().getName();
            if (txStarted) {
                TransactionHelper.setTransactionRollbackOnly();
            }

        } finally {
            CmBatchProcessingHelper.finalizeBatchProcessExecution(cmProcessDoc,
                    itemType, itemClass, transitionToComeBackToPreviousState,
                    txStarted, successList, errorsList, exceptionErrorMsg,
                    loginStack, session);

            // close opened session
            session.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with successList size = |"
                + successList.size() + "| and errorsList size = |"
                + errorsList.size() + "|");
    }

}
