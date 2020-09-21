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

package com.aritu.eloraplm.integration.admin.restoperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.admin.restoperations.util.MakeItemsObsoleteResponse;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
@Operation(id = MakeItemsObsolete.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_ADMIN, label = "Admin - Make Items Obsolete", description = "Make a list of items obsolete, with or without their CAD documents.")
public class MakeItemsObsolete {
    public static final String ID = "Elora.Admin.MakeItemsObsolete";

    private static final Log log = LogFactory.getLog(MakeItemsObsolete.class);

    @Param(name = "references", required = true)
    private List<String> references;

    @Param(name = "includeRelatedCadDocuments", required = true)
    private boolean includeRelatedCadDocuments;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private MakeItemsObsoleteResponse response;

    private List<String> processedUids;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new MakeItemsObsoleteResponse();

        try {

            if (!session.getPrincipal().getName().equals("Administrator")) {
                throw new EloraException(
                        "Admin operations can only be executed by administrators.");
            }

            // Process the references
            if (!references.isEmpty()) {
                for (String reference : references) {
                    log.info(logInitMsg + "Processing reference |" + reference
                            + "|...");

                    DocumentModelList itemList = getItems(reference);
                    for (DocumentModel item : itemList) {
                        processItem(reference, item);
                    }

                    log.info(logInitMsg + "Reference |" + reference
                            + "| processed.");
                }

                int refSize = references.size();
                int skippedRefSize = response.getErrorListSize();
                log.info(logInitMsg + refSize + " references processed. "
                        + (refSize - skippedRefSize) + " made obsolete. "
                        + skippedRefSize + " skipped.");
            } else {
                log.info(logInitMsg + "No reference to process.");
            }

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

        }

        log.trace(logInitMsg + "--- EXIT ---");

        return response.convertToJson();
    }

    private DocumentModelList getItems(String reference) throws EloraException {

        List<String> facets = new ArrayList<String>();
        facets.add(EloraFacetConstants.FACET_BOM_DOCUMENT);
        String q = EloraQueryFactory.getWcDocsByFacetListReference(reference,
                facets);

        DocumentModelList dml = session.query(q);
        if (dml.isEmpty()) {
            String error = "No item found with reference |" + reference + "|";
            log.error(error);
            response.addError(reference, error);
        }
        return dml;
    }

    /**
     * @param requestDoc
     * @throws EloraException
     */
    private void processItem(String reference, DocumentModel wcDoc)
            throws EloraException {
        String logInitMsg = "[processItem] [" + session.getPrincipal().getName()
                + "] ";
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            log.trace(logInitMsg + "Processing item |" + wcDoc.getId()
                    + "| with reference |" + reference + "| ...");

            processedUids = new ArrayList<String>();

            if (wcDoc.isCheckedOut()) {
                throw new EloraException("Item with reference |" + reference
                        + "| is checked out.");
            }

            List<DocumentRef> versions = session.getVersionsRefs(
                    wcDoc.getRef());
            Map<String, List<DocumentModel>> relatedCads = new HashMap<String, List<DocumentModel>>();
            for (DocumentRef version : versions) {
                processRef(reference, version);
                relatedCads = getRelatedCadDocuments(reference, version);
            }
            session.save();
            if (includeRelatedCadDocuments) {
                processRelatedCadDocuments(reference, wcDoc, relatedCads);
            }

            response.addProcessedDocs(reference, processedUids);

            log.trace(logInitMsg + "Processed item |" + wcDoc.getId()
                    + "| with reference |" + reference + "|.");

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.addError(reference, e.getMessage());
            response.removeProcessedReference(reference);
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.addError(reference, e.getMessage());
            response.removeProcessedReference(reference);
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private void processRef(String reference, DocumentRef docRef)
            throws EloraException {
        String logInitMsg = "[processRef] [" + session.getPrincipal().getName()
                + "] ";

        log.trace(logInitMsg + "Processing uid |" + docRef.toString()
                + "| for reference |" + reference + "| ...");

        if (!response.isProcessedDoc(reference, docRef.toString())) {

            DocumentModel doc = session.getDocument(docRef);
            if (!doc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.OBSOLETE)) {

                validateMakeObsolete(reference, doc);
                makeObsolete(reference, doc);

            } else {
                log.trace("Document |" + doc.getId() + "| with reference |"
                        + reference + "| is already in obsolete state.");
            }
        } else {
            log.trace("Document |" + docRef.toString() + "| with reference |"
                    + reference + "| already processed.");
        }

        log.trace(logInitMsg + "Uid |" + docRef.toString() + "| for reference |"
                + reference + "| processed.");
    }

    private void validateMakeObsolete(String reference, DocumentModel doc)
            throws EloraException {

        if (!doc.getAllowedStateTransitions().contains(
                EloraLifeCycleConstants.TRANS_OBSOLETE)) {
            throw new EloraException(
                    "Document |" + doc.getId() + "| with reference |"
                            + reference + "| has no makeObsolete transition.");
        }

        // Check subject document's state
        List<Resource> predicates = new ArrayList<Resource>();
        predicates.addAll(
                loadPredicateResources(RelationsConfig.cadRelationsList));
        predicates.addAll(
                loadPredicateResources(RelationsConfig.bomDirectRelationsList));
        predicates.addAll(loadPredicateResources(
                RelationsConfig.bomHierarchicalRelationsList));
        predicates.addAll(
                loadPredicateResources(RelationsConfig.docRelationsList));

        /* TODO Zerk eman duen errorea jakiteko hobeto orain dagoen moduan? Begiratu:
        if (!PromoteHelper.parentsAllowTransition(doc, EloraLifeCycleConstants.TRANS_OBSOLETE,
                predicates)) {
        }
        */

        List<Statement> stmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                doc, predicates);
        for (Statement stmt : stmts) {
            DocumentModel subjectDoc = RelationHelper.getDocumentModel(
                    stmt.getSubject(), session);

            if (!subjectDoc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.OBSOLETE)) {
                throw new EloraException("Document |" + doc.getId()
                        + "| with reference |" + reference + "| has subject |"
                        + subjectDoc.getId() + "| that is not obsolete.");
            }

        }
    }

    private List<Resource> loadPredicateResources(List<String> predicatesList) {
        List<Resource> resourceList = new ArrayList<Resource>();
        for (String predicateUri : predicatesList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            resourceList.add(predicateResource);
        }
        return resourceList;
    }

    private void makeObsolete(String reference, DocumentModel doc)
            throws EloraException {

        doc.followTransition(EloraLifeCycleConstants.TRANS_OBSOLETE);

        // Update dc:lastContributor, dc:contributors and dc:modified
        doc = EloraDocumentHelper.updateContributorAndModified(doc, false);

        // Nuxeo Event
        String comment = doc.getVersionLabel() + " #Admin";
        EloraEventHelper.fireEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc,
                comment);

        EloraDocumentHelper.disableVersioningDocument(doc);
        doc = session.saveDocument(doc);

        // If the document is locked, we ensure it gets unlocked, as obsolete
        // state cannot be locked
        if (doc.isLocked()) {
            session.removeLock(doc.getRef());
        }

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        DocumentRef wcBaseRef = session.getBaseVersion(wcDoc.getRef());
        DocumentModel baseDoc = session.getDocument(wcBaseRef);

        VersionModel version = new VersionModelImpl();
        version.setId(doc.getId());
        if (doc.getId().equals(baseDoc.getId())) {
            // We cannot follow transition instead of restoring, because it
            // checks the document out always. This is the only way we know to
            // change the state without checkin the document out.
            EloraDocumentHelper.restoreToVersion(wcDoc.getRef(),
                    new IdRef(version.getId()), true, true, session);
        }

        // We don't want to change which is the base version
        // else if (isMajorVersionEqual(doc, wcDoc)) {
        // EloraDocumentHelper.restoreWorkingCopyToVersion(wcDoc, version,
        // eloraDocumentRelationManager, session);
        // }

        // Logak

        processedUids.add(doc.getId());
    }

    // private boolean isMajorVersionEqual(DocumentModel doc,
    // DocumentModel wcDoc) {
    // return doc.getPropertyValue(
    // VersioningService.MAJOR_VERSION_PROP).equals(
    // wcDoc.getPropertyValue(
    // VersioningService.MAJOR_VERSION_PROP));
    // }

    private Map<String, List<DocumentModel>> getRelatedCadDocuments(
            String reference, DocumentRef docRef) {
        List<DocumentModel> drwList = new ArrayList<DocumentModel>();
        List<DocumentModel> asmOrPrtList = new ArrayList<DocumentModel>();
        Map<String, List<DocumentModel>> cadMap = new HashMap<String, List<DocumentModel>>();

        DocumentModel doc = session.getDocument(docRef);

        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        DocumentModelList cadList = RelationHelper.getObjectDocuments(doc,
                predicate);
        for (DocumentModel cad : cadList) {
            if (cad.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
                drwList.add(cad);
            } else {
                asmOrPrtList.add(cad);
            }
        }
        cadMap.put("drw", drwList);
        cadMap.put("asmOrPrt", asmOrPrtList);

        return cadMap;
    }

    /**
     * We have to process first the drawings (top level in CAD documents), then
     * assemblies and parts.
     *
     * @param reference
     * @param wcDoc
     * @param relatedCads
     * @throws EloraException
     */
    private void processRelatedCadDocuments(String reference,
            DocumentModel wcDoc, Map<String, List<DocumentModel>> relatedCads)
            throws EloraException {
        log.trace("Processing related CAD documents for reference |" + reference
                + "| with uid |" + wcDoc.getId() + "|...");

        for (DocumentModel drw : relatedCads.get("drw")) {
            processSingleCadDocument(reference, wcDoc, drw);
        }
        for (DocumentModel asmOrPrt : relatedCads.get("asmOrPrt")) {
            processSingleCadDocument(reference, wcDoc, asmOrPrt);
        }

        log.trace("CAD documents processed.");
    }

    /**
     * If the document about to process is the base, we must process all the
     * versions.
     *
     * @param reference
     * @param wcDoc
     * @param cad
     * @throws EloraException
     */
    private void processSingleCadDocument(String reference, DocumentModel wcDoc,
            DocumentModel cad) throws EloraException {
        if (!cad.isImmutable()) {
            throw new EloraException(
                    "Document |" + wcDoc.getId() + "| with reference |"
                            + reference + "| has related CAD document |"
                            + cad.getId() + "| which is a working copy.");
        }

        DocumentModel baseObj = EloraDocumentHelper.getBaseVersion(
                session.getWorkingCopy(cad.getRef()));
        if (baseObj == null) {
            throw new EloraException("Document |" + wcDoc.getId()
                    + "| with reference |" + reference
                    + "| has related CAD document |" + cad.getId()
                    + "| without base. Probably it has no versions.");
        }
        if (cad.getId().equals(baseObj.getId())) {
            for (DocumentModel objVersion : session.getVersions(cad.getRef())) {
                processSingleRelatedCadRef(objVersion);
            }
        } else {
            processSingleRelatedCadRef(cad);
        }
    }

    private void processSingleRelatedCadRef(DocumentModel cadDoc)
            throws EloraException {
        String cadRef = cadDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        processRef(cadRef, cadDoc.getRef());
    }
}
