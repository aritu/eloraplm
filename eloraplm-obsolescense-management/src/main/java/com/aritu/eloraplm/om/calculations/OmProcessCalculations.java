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
package com.aritu.eloraplm.om.calculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.om.util.OmHelper;
import com.aritu.eloraplm.om.util.ObsoleteProcessResult;
import com.aritu.eloraplm.om.util.OmRowData;
import com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;

/**
 * // Calculates the impact of the document that is going be obsoleted
 *
 * @author aritu
 *
 */
public class OmProcessCalculations {

    private CoreSession session;

    private DocumentModel omProcess;

    private DocumentModel sourceDoc;

    private List<Resource> impactPredicates;

    private Map<String, OmRowData> impactedDocMap;

    private List<String> processedUids;

    private DAG dag;

    private Map<String, DAG> anarchicDags;

    private MakeObsoleteService makeObsoleteService;

    private static final Log log = LogFactory.getLog(
            OmProcessCalculations.class);

    public OmProcessCalculations(CoreSession session, DocumentModel omProcess,
            DocumentModel sourceDoc) throws EloraException {
        this.session = session;
        this.omProcess = omProcess;
        this.sourceDoc = sourceDoc;

        impactPredicates = getImpactPredicates();

        impactedDocMap = new HashMap<String, OmRowData>();
        anarchicDags = new HashMap<String, DAG>();
        dag = new DAG();

        makeObsoleteService = Framework.getService(MakeObsoleteService.class);

        processedUids = OmHelper.getProcessedDocUids(session,
                omProcess.getId());
    }

    private List<Resource> getImpactPredicates() {
        List<String> impactPredicateList = new ArrayList<String>();
        impactPredicateList.addAll(RelationsConfig.cadRelationsList);
        impactPredicateList.addAll(RelationsConfig.docRelationsList);
        impactPredicateList.addAll(RelationsConfig.bomRelationsList);
        return EloraRelationHelper.getPredicateResourceList(
                impactPredicateList);
    }

    public void calculateAndSaveImpact() throws EloraException,
            CycleDetectedException, DocumentUnreadableException {

        calculateImpact();

        OmHelper.saveOmProcessDocs(session, omProcess, impactedDocMap,
                OmMetadataConstants.OM_IMPACTED_DOC_LIST);
    }

    private void calculateImpact() throws EloraException,
            CycleDetectedException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpact] ["
                + session.getPrincipal().getName() + "] ";

        // First we calculate the normal DAG and impact map
        boolean comesFromAnAnarchic = false;
        calculateDocImpact(sourceDoc, sourceDoc.getId(), dag,
                comesFromAnAnarchic);

        // The we calculate individual DAGs for all the anarchics
        comesFromAnAnarchic = true;
        for (Map.Entry<String, DAG> entry : anarchicDags.entrySet()) {

            DocumentModel anarchicDoc = session.getDocument(
                    new IdRef(entry.getKey()));

            if (anarchicDoc == null) {
                log.trace(logInitMsg
                        + "Throw DocumentUnreadableException since anarchicDoc is null. Anarchic uid = |"
                        + entry.getKey() + "|");
                throw new DocumentUnreadableException(
                        "Error getting anarchic document with uid |"
                                + entry.getKey() + "|");
            }

            calculateDocImpact(anarchicDoc, anarchicDoc.getId(),
                    entry.getValue(), comesFromAnAnarchic);
        }
    }

    private void calculateDocImpact(DocumentModel doc, String initialDocId,
            DAG contextDag, boolean comesFromAnAnarchic) throws EloraException,
            CycleDetectedException, DocumentUnreadableException {

        String logInitMsg = "[calculateDocImpact] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Processing doc |" + doc.getId()
                + "| - comesFromAnAnarchic: |" + comesFromAnAnarchic + "|...");

        contextDag.addVertex(doc.getId());

        processPreviousVersionsIfNeeded(doc, initialDocId, contextDag,
                comesFromAnAnarchic);

        List<Statement> impactStmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                doc, impactPredicates);

        boolean hasNormalStatements = false;
        for (Statement stmt : impactStmts) {

            boolean isAnarchic = RelationsConfig.bomAnarchicRelationsList.contains(
                    stmt.getPredicate().getUri()) ? true : false;

            hasNormalStatements = hasNormalStatements || !isAnarchic;

            DocumentModel impactedDoc = RelationHelper.getDocumentModel(
                    stmt.getSubject(), session);

            if (impactedDoc == null) {
                log.trace(logInitMsg
                        + "Throw DocumentUnreadableException since impactedDoc is null. stmt = |"
                        + stmt.toString() + "|");
                throw new DocumentUnreadableException(
                        "Error getting document from statement |"
                                + stmt.toString() + "|");
            }

            // We exclude deleted documents
            if (session.getWorkingCopy(
                    impactedDoc.getRef()).getCurrentLifeCycleState().equals(
                            EloraLifeCycleConstants.NX_DELETED)) {
                continue;
            }

            log.trace(logInitMsg + "Processing doc |" + doc.getId()
                    + "| statement - Impacted doc: |" + impactedDoc.getId()
                    + "| isAnarchic: |" + isAnarchic + "|...");

            String impactedDocId = impactedDoc.getId();

            boolean isInProcessedList = processedUids.contains(impactedDocId);
            boolean isJustToGetRelations = impactedDoc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.OBSOLETE) && isInProcessedList;
            boolean breakLoop = impactedDoc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.OBSOLETE) && !isInProcessedList;
            boolean existsInDag = contextDag.getVertex(impactedDocId) != null;

            if (breakLoop) {
                continue;
            }

            treatAnarchics(impactedDocId, isAnarchic, comesFromAnAnarchic);

            treatDagForImpactedDoc(contextDag, doc, impactedDoc, isAnarchic);

            CanMakeObsoleteResult result = treatImpactMapAndStopCalculationIfKo(
                    impactedDoc, stmt, isAnarchic, comesFromAnAnarchic,
                    isJustToGetRelations, existsInDag);

            if (hasToContinueCalculating(contextDag, impactedDoc, isAnarchic,
                    comesFromAnAnarchic, isJustToGetRelations, existsInDag,
                    result)) {
                calculateDocImpact(impactedDoc, initialDocId, contextDag,
                        comesFromAnAnarchic);
            }
        }

        if (!hasNormalStatements) {
            treatTopDocs(contextDag, doc, initialDocId, comesFromAnAnarchic);
        }

        log.trace(logInitMsg + "Doc |" + doc.getId() + "| processed.");

    }

    private void treatAnarchics(String impactedDocId, boolean isAnarchic,
            boolean comesFromAnAnarchic) {
        if (!isAnarchic) {
            return;
        }

        if (!comesFromAnAnarchic && !anarchicDags.containsKey(impactedDocId)) {
            anarchicDags.put(impactedDocId, new DAG());
        }
    }

    private void treatDagForImpactedDoc(DAG contextDag, DocumentModel doc,
            DocumentModel impactedDoc, boolean isAnarchic)
            throws CycleDetectedException {
        if (!isAnarchic) {
            contextDag.addEdge(doc.getId(), impactedDoc.getId());
        }
    }

    private CanMakeObsoleteResult treatImpactMapAndStopCalculationIfKo(
            DocumentModel impactedDoc, Statement stmt, boolean isAnarchic,
            boolean comesFromAnAnarchic, boolean isJustToGetRelations,
            boolean existsInDag) throws EloraException {

        if (existsInDag || comesFromAnAnarchic || isJustToGetRelations) {
            return null;
        }

        if (impactedDocMap.containsKey(impactedDoc.getId())) {
            if (isAnarchic) {
                return null;
            }
            anarchicDags.remove(impactedDoc.getId());
        }

        return addDocToImpactMap(impactedDoc, stmt);
    }

    private CanMakeObsoleteResult addDocToImpactMap(DocumentModel impactedDoc,
            Statement stmt) throws EloraException {
        return updateImpactMap(impactedDoc, stmt);
    }

    private boolean hasToContinueCalculating(DAG contextDag,
            DocumentModel impactedDoc, boolean isAnarchic,
            boolean comesFromAnAnarchic, boolean isJustToGetRelations,
            boolean existsInDag, CanMakeObsoleteResult result) {

        if (existsInDag || isAnarchic) {
            return false;
        }

        if (result != null && !result.getCanMakeObsolete()) {
            return false;
        }

        return true;
    }

    private void treatTopDocs(DAG contextDag, DocumentModel doc,
            String initialDocId, boolean comesFromAnAnarchic) {
        if (!comesFromAnAnarchic || doc.getId().equals(initialDocId)) {
            return;
        }

        if (impactedDocMap.containsKey(initialDocId)) {
            impactedDocMap.get(initialDocId).addAnarchicTopDoc(doc.getId());
        }
    }

    private void processPreviousVersionsIfNeeded(DocumentModel doc,
            String initialDocId, DAG contextDag, boolean comesFromAnAnarchic)
            throws EloraException, CycleDetectedException,
            DocumentUnreadableException {

        // Treat base docs
        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                session.getWorkingCopy(doc.getRef()));

        if (doc.getId().equals(baseDoc.getId())) {
            processPreviousVersions(doc, initialDocId, contextDag,
                    comesFromAnAnarchic);
        }
        // Treat if it's the initial doc and marked to process previous versions
        else {

            Boolean includePreviousVersions = (Boolean) omProcess.getPropertyValue(
                    OmMetadataConstants.OM_INCLUDE_PREVIOUS_VERSIONS);
            if (includePreviousVersions != null
                    && includePreviousVersions.booleanValue() == true) {
                if (initialDocId.equals(doc.getId())) {
                    processPreviousVersions(doc, initialDocId, contextDag,
                            comesFromAnAnarchic);
                }
            }
        }
    }

    private void processPreviousVersions(DocumentModel doc, String initialDocId,
            DAG contextDag, boolean comesFromAnAnarchic) throws EloraException,
            CycleDetectedException, DocumentUnreadableException {

        List<DocumentModel> previousVersions = EloraDocumentHelper.getPreviousVersions(
                session, doc);

        for (DocumentModel docVersion : previousVersions) {

            // TODO obsolete badago ere aurrera jarraitu bilaketarako, bestela
            // bere anarkikoak galtzen dira 2. bueltarako

            if (contextDag.getVertex(docVersion.getId()) != null
                    || docVersion.getCurrentLifeCycleState().equals(
                            EloraLifeCycleConstants.OBSOLETE)
                    || docVersion.getId().equals(doc.getId())) {
                continue;
            }

            contextDag.addEdge(doc.getId(), docVersion.getId());

            if (!comesFromAnAnarchic) {
                if (impactedDocMap.containsKey(docVersion.getId())) {
                    continue;
                }

                CanMakeObsoleteResult versionResult = updateImpactMap(
                        docVersion, null);

                if (!versionResult.getCanMakeObsolete()) {
                    continue;
                }
            }

            calculateDocImpact(docVersion, initialDocId, contextDag,
                    comesFromAnAnarchic);
        }
    }

    private CanMakeObsoleteResult updateImpactMap(DocumentModel doc,
            Statement stmt) throws EloraException {

        // TODO: En las comprobaciones que se hacen dentro de esta
        // funcion se mira que el wc no esté checked out ni bloqueado,
        // Cuando estoy obsoleteando AVs ¿tiene sentido? Si, si voy a
        // obsoletear un base version!!! Si lo que obsoleteo es una version
        // igual no tendría sentido...
        CanMakeObsoleteResult result = makeObsoleteService.canMakeObsoleteDocumentWithoutRelationChecks(
                session, doc);

        OmRowData docData = OmHelper.createOmProcessDoc(session, doc, stmt,
                result);

        impactedDocMap.put(doc.getId(), docData);

        return result;
    }

    public ObsoleteProcessResult processImpactedDocs(
            List<String> selectedAnarchics) throws EloraException,
            CycleDetectedException, DocumentUnreadableException {

        calculateImpact();

        ObsoleteProcessResult processResult = checkImpactDiffs(
                selectedAnarchics);

        if (!(processResult.getNewImpactedDocList().size() > 0)
                && !(processResult.getMissingImpactedDocList().size() > 0)) {

            processResult = obsoleteImpactedDocs(dag, processResult, false);

            for (String selectedAnarchic : selectedAnarchics) {
                processResult = obsoleteImpactedDocs(
                        anarchicDags.get(selectedAnarchic), processResult,
                        true);
            }

        } else {
            OmHelper.saveOmProcessDocs(session, omProcess, impactedDocMap,
                    OmMetadataConstants.OM_IMPACTED_DOC_LIST);
        }
        return processResult;
    }

    private ObsoleteProcessResult checkImpactDiffs(
            List<String> selectedAnarchics) throws EloraException {

        // Check diffs in main dag
        ObsoleteProcessResult processResult = new ObsoleteProcessResult(false);

        List<String> oldImpactUids = OmHelper.getImpactedDocUids(session,
                omProcess.getId());
        List<String> updatedImpactUids = new ArrayList<>(
                impactedDocMap.keySet());

        // Check main DAG diff
        processResult = updateResultWithDiff(processResult, oldImpactUids,
                updatedImpactUids);

        // Check selected anarchic top docs diff
        Map<String, List<String>> anarchicTopDocs = OmHelper.getAnarchicTopDocUids(
                session, omProcess);
        for (String selectedUid : selectedAnarchics) {
            List<String> oldTopDocs = anarchicTopDocs.containsKey(selectedUid)
                    ? anarchicTopDocs.get(selectedUid)
                    : new ArrayList<String>();
            List<String> updatedTopDocs = impactedDocMap.get(
                    selectedUid).getAnarchicTopDocs();

            processResult = updateResultWithDiff(processResult, oldTopDocs,
                    updatedTopDocs);
        }

        return processResult;
    }

    private ObsoleteProcessResult updateResultWithDiff(
            ObsoleteProcessResult result, List<String> oldUids,
            List<String> newUids) {

        List<String> addedDocs = new ArrayList<>(newUids);
        addedDocs.removeAll(oldUids);
        result.addToNewImpactedDocList(addedDocs);

        List<String> missingDocs = new ArrayList<>(oldUids);
        missingDocs.removeAll(newUids);

        for (String missingDocId : missingDocs) {
            if (processedUids.contains(missingDocId)) {
                missingDocs.remove(missingDocId);
            }
        }
        result.addToMissingImpactedDocList(missingDocs);

        return result;
    }

    private ObsoleteProcessResult obsoleteImpactedDocs(DAG contextDag,
            ObsoleteProcessResult processResult, boolean comesFromAnAnarchic)
            throws EloraException {

        // TODO: Lo mejor en estos procesos es marcar los documentos para que
        // nadie pueda usarlos mientras se está ejecutando. Queda pendiente.

        Map<String, OmRowData> obsoletedDocMap = OmHelper.getOmProcessDocMap(
                session, omProcess, OmMetadataConstants.OM_PROCESSED_DOC_LIST);

        List<String> sortedIds = sortDAG(contextDag);
        for (String idDoc : sortedIds) {
            DocumentModel doc = session.getDocument(new IdRef(idDoc));

            // We exclude obsoleted docs, because they are already processed,
            // just in the DAG to be able to get to other relations
            if (doc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.STATUS_OBSOLETE)) {
                continue;
            }

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                /* TODO Bigarren fase batean, StateLogera doa iruzkina??
                 * String comment = (String) omProcess.getPropertyValue(
                        NuxeoMetadataConstants.NX_DC_DESCRIPTION);*/
                String processReference = (String) omProcess.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE);
                CanMakeObsoleteResult result = makeObsoleteService.makeObsoleteDocumentInProcess(
                        session, doc, processReference);

                if (!result.getCanMakeObsolete()) {
                    processResult.setObsoleteResult(result);
                    OmRowData failedDoc = impactedDocMap.get(doc.getId());
                    if (failedDoc != null) {
                        failedDoc.setIsOk(false);
                        failedDoc.setErrorMsg(
                                result.getCannotMakeObsoleteReasonMsg());
                        failedDoc.setErrorMsgParams(
                                result.getCannotMakeObsoleteReasonMsgParam());

                        OmHelper.saveOmProcessDocs(session, omProcess,
                                impactedDocMap,
                                OmMetadataConstants.OM_IMPACTED_DOC_LIST);
                    }
                    break;
                }

                updateImpactedAndObsoleteLists(obsoletedDocMap, idDoc,
                        comesFromAnAnarchic);

            } catch (EloraException e) {
                TransactionHelper.setTransactionRollbackOnly();
                throw new EloraException("Error making obsolete document |"
                        + doc.getId() + "| ");
            } catch (Exception e) {
                TransactionHelper.setTransactionRollbackOnly();
                throw new EloraException("Error making obsolete document |"
                        + doc.getId() + "| ");
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        return processResult;
    }

    private void updateImpactedAndObsoleteLists(
            Map<String, OmRowData> obsoletedDocMap, String docId,
            boolean comesFromAnAnarchic) throws EloraException {

        OmRowData obsoletedDoc = impactedDocMap.get(docId);
        // This if checks if idDoc is the source doc of the process. We never
        // save source doc in the map. But if it comes from an anarchic we have
        // to create a row in the processed list

        if (obsoletedDoc != null) {
            impactedDocMap.remove(docId);
            obsoletedDoc.setIsProcessed(true);
            obsoletedDocMap.put(docId, obsoletedDoc);

            OmHelper.saveOmProcessDocs(session, omProcess, impactedDocMap,
                    OmMetadataConstants.OM_IMPACTED_DOC_LIST);
            OmHelper.saveOmProcessDocs(session, omProcess, obsoletedDocMap,
                    OmMetadataConstants.OM_PROCESSED_DOC_LIST);
        }

        if (obsoletedDoc == null && comesFromAnAnarchic) {
            DocumentModel doc = session.getDocument(new IdRef(docId));
            obsoletedDoc = OmHelper.createOmProcessDoc(session, doc, null,
                    new CanMakeObsoleteResult(true, null, null));
            // We do not know what was the origin state, but at least we leave
            // it empty
            obsoletedDoc.setOriginState(null);
            obsoletedDoc.setIsProcessed(true);
            obsoletedDocMap.put(docId, obsoletedDoc);

            OmHelper.saveOmProcessDocs(session, omProcess, obsoletedDocMap,
                    OmMetadataConstants.OM_PROCESSED_DOC_LIST);
        }

    }

    private List<String> sortDAG(DAG dag) {
        @SuppressWarnings("unchecked")
        List<String> sortedIds = TopologicalSorter.sort(dag);
        List<String> newSortedIds = new LinkedList<String>();

        for (String sortedId : sortedIds) {
            if (sortedId != null) {
                newSortedIds.add(sortedId);
            }
        }
        return newSortedIds;
    }

}
