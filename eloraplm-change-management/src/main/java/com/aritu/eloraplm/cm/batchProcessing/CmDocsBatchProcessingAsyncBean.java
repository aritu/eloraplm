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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.DAG;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.batchProcessing.util.CmBatchProcessingHelper;
import com.aritu.eloraplm.cm.batchProcessing.util.ResultType;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMBatchProcessingEventNames;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

@AutoCreate
@Name("cmDocsBatchProcessingAsync")
public class CmDocsBatchProcessingAsyncBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmDocsBatchProcessingAsyncBean.class);

    private List<ResultType> successList = new ArrayList<ResultType>();

    private List<ResultType> errorsList = new ArrayList<ResultType>();

    private String exceptionErrorMsg = null;

    private boolean txStarted = false;

    private LoginStack loginStack = null;

    private String itemType = CMConstants.ITEM_TYPE_DOC;

    @Observer(CMBatchProcessingEventNames.PROMOTE_DOCS)
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
            List<String> destinationWcUidList = processPromoteDocs(cmProcessDoc,
                    root, sortedIds, dag, childrenVersionSeriesMap,
                    eloraDocumentRelationManager, session);
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

    private List<String> processPromoteDocs(DocumentModel cmProcessDoc,
            TreeNode root, List<String> sortedIds, DAG dag,
            Map<String, List<String>> childrenVersionSeriesMap,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession documentManager) throws EloraException {
        String logInitMsg = "[processPromoteDocs] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        List<String> destinationWcUidList = new ArrayList<String>();

        List<String> cadHierarchicalAndSpecialAndDirectRelations = new ArrayList<>(
                RelationsConfig.cadHierarchicalRelationsList);
        cadHierarchicalAndSpecialAndDirectRelations.addAll(
                RelationsConfig.cadSpecialRelationsList);
        cadHierarchicalAndSpecialAndDirectRelations.addAll(
                RelationsConfig.cadDirectRelationsList);

        String cmProcessReference = cmProcessDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        boolean stopProcessing = false;
        for (String docId : sortedIds) {
            log.trace(logInitMsg + "Start promoting document |" + docId + "|");

            DocumentModel doc = documentManager.getDocument(new IdRef(docId));
            String reference = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
            String title = doc.getTitle();

            String warningMsg = "";

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
                    List<Statement> bomStmts = getCadHierarchicalStmts(doc,
                            cadHierarchicalAndSpecialAndDirectRelations);
                    log.trace(logInitMsg
                            + "Finished getting hierarchical children of document |"
                            + docId + "|");
                    List<String> childrenIdList = dag.getChildLabels(docId);

                    for (Statement bomStmt : bomStmts) {
                        DocumentModel objectBomDoc = RelationHelper.getDocumentModel(
                                bomStmt.getObject(), documentManager);
                        List<String> childrenInTreeVersionSeriesIdList = childrenVersionSeriesMap.get(
                                docId);
                        if (childrenInTreeVersionSeriesIdList != null
                                && childrenInTreeVersionSeriesIdList.contains(
                                        objectBomDoc.getVersionSeriesId())) {
                            if (!childrenIdList.contains(
                                    objectBomDoc.getId())) {
                                log.error(logInitMsg
                                        + "Document to be promoted has different document version in composition and tree. docId = |"
                                        + docId + "|, reference =|" + reference
                                        + "|, title=|" + title + "|");

                                Object[] messageParams = { reference, title,
                                        objectBomDoc.getPropertyValue(
                                                EloraMetadataConstants.ELORA_ELO_REFERENCE),
                                        objectBomDoc.getTitle() };
                                String message = EloraMessageHelper.getTranslatedMessage(
                                        documentManager,
                                        "eloraplm.message.error.cm.batch.docCompositionWithDiffDocVersion",
                                        messageParams);
                                ResultType error = new ResultType(doc.getId(),
                                        reference, title, message);
                                errorsList.add(error);

                                TransactionHelper.setTransactionRollbackOnly();
                                stopProcessing = true;
                                break; // from bomStmts loop
                            }
                        } else {
                            if (EloraDocumentHelper.isReleased(objectBomDoc)) {
                                DocumentModel latestReleased = EloraDocumentHelper.getLatestReleasedVersion(
                                        objectBomDoc);
                                if (latestReleased != null) {
                                    if (!latestReleased.getId().equals(
                                            objectBomDoc.getId())) {

                                        String childReference = objectBomDoc.getPropertyValue(
                                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                                        String childTitle = objectBomDoc.getTitle();

                                        log.warn(logInitMsg
                                                + "This document has child  released but it is not the latest released. docId = |"
                                                + docId + "|, reference =|"
                                                + reference + "|, title=|"
                                                + title + "| childDocId = |"
                                                + objectBomDoc.getId()
                                                + "|, child reference = |"
                                                + childReference
                                                + "|, child title = |"
                                                + childTitle + "|");

                                        Object[] messageParams = {
                                                childReference, childTitle };
                                        warningMsg = EloraMessageHelper.getTranslatedMessage(
                                                documentManager,
                                                "eloraplm.message.error.cm.batch.docWithNoLastReleasedChild",
                                                messageParams);
                                    }
                                } else {
                                    log.error(logInitMsg
                                            + "Could not get latest released version of document |"
                                            + objectBomDoc.getId() + "|");
                                }
                            } else {
                                String childReference = objectBomDoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
                                String childTitle = objectBomDoc.getTitle();

                                log.error(logInitMsg
                                        + "Document has no released documents in its composition. docId = |"
                                        + docId + "|, reference =|" + reference
                                        + "|, title=|" + title
                                        + "|, childDocId = |"
                                        + objectBomDoc.getId()
                                        + "|, child reference = |"
                                        + childReference + "|, child title = |"
                                        + childTitle + "|");

                                Object[] messageParams = { childReference,
                                        childTitle };
                                String message = EloraMessageHelper.getTranslatedMessage(
                                        documentManager,
                                        "eloraplm.message.error.cm.batch.docWithNoReleasedChild",
                                        messageParams);
                                ResultType error = new ResultType(doc.getId(),
                                        reference, title, message);
                                errorsList.add(error);

                                TransactionHelper.setTransactionRollbackOnly();
                                stopProcessing = true;
                                break; // from bomStmts loop
                            }
                        }
                    }
                    // executePromote(doc);
                    if (!stopProcessing) {
                        if (doc.getAllowedStateTransitions().contains(
                                EloraLifeCycleConstants.TRANS_APPROVE)) {
                            doc.followTransition(
                                    EloraLifeCycleConstants.TRANS_APPROVE);

                            // Nuxeo Event
                            // doc.refresh();
                            String comment = doc.getVersionLabel() + " @"
                                    + cmProcessReference;
                            EloraEventHelper.fireEvent(
                                    PdmEventNames.PDM_PROMOTED_EVENT, doc,
                                    comment);

                            EloraDocumentHelper.disableVersioningDocument(doc);
                            doc = documentManager.saveDocument(doc);

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
                                EloraDocumentHelper.restoreToVersion(
                                        wcDoc.getRef(), doc.getRef(), true,
                                        true, documentManager);
                            }
                            documentManager.removeLock(wcDoc.getRef());
                            destinationWcUidList.add(wcDoc.getId());

                            ResultType success = new ResultType(docId,
                                    reference, title, warningMsg);
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
                                    "eloraplm.message.error.cm.batch.docNotAllowedTransition");
                            ResultType error = new ResultType(doc.getId(),
                                    reference, title, message);
                            errorsList.add(error);

                            TransactionHelper.setTransactionRollbackOnly();
                            stopProcessing = true;
                        }
                    }

                } else {
                    log.error(logInitMsg + "Document  with docId = |" + docId
                            + "|, reference =|" + reference + "|, title=|"
                            + title
                            + "| has another released or obsolete document in the same major");

                    String message = EloraMessageHelper.getTranslatedMessage(
                            documentManager,
                            "eloraplm.message.error.cm.batch.docWithReleasedOnMajor");
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

    private List<Statement> getCadHierarchicalStmts(DocumentModel doc,
            List<String> cadHierarchicalAndSpecialAndDirectRelations) {
        List<Resource> predicates = new ArrayList<>();
        for (String predicateUri : cadHierarchicalAndSpecialAndDirectRelations) {
            predicates.add(new ResourceImpl(predicateUri));
        }
        return EloraRelationHelper.getStatements(doc, predicates);
    }

}
