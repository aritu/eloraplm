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
package com.aritu.eloraplm.pdm.makeobsolete.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;
import com.aritu.eloraplm.pdm.makeobsolete.util.MakeObsoleteHelper;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;

/**
 * Service implementation for make obsolete function.
 *
 * @author aritu
 *
 */
public class MakeObsoleteServiceImpl implements MakeObsoleteService {

    private static final Log log = LogFactory.getLog(
            MakeObsoleteServiceImpl.class);

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService#canMakeObsoleteDocument(org.nuxeo.ecm.core.api.DocumentModel)
     */
    @Override
    public CanMakeObsoleteResult canMakeObsoleteDocument(CoreSession session,
            DocumentModel doc) throws EloraException {
        String logInitMsg = "[canMakeObsoleteDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");

        CanMakeObsoleteResult result = new CanMakeObsoleteResult();

        try {
            if (MakeObsoleteHelper.impliesMakingObsoleteAllVersions(session,
                    doc)) {
                result = canMakeObsoleteAllVersionsOfADocument(session, doc);
            } else {
                if (doc.getCurrentLifeCycleState().equals(
                        EloraLifeCycleConstants.OBSOLETE)) {
                    log.trace(logInitMsg
                            + "Cannot makeObsolete the document since the it is already in obsolete state.");
                    result.setCannotMakeObsoleteReasonMsg(
                            "eloraplm.message.error.makeobsolete.already.obsolete");
                } else {
                    result = canMakeObsoleteSingleDocument(session, doc);
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- with canMakeObsolete = |"
                + result.getCanMakeObsolete() + "|");

        return result;
    }

    protected CanMakeObsoleteResult canMakeObsoleteAllVersionsOfADocument(
            CoreSession session, DocumentModel doc) throws EloraException {
        String logInitMsg = "[canMakeObsoleteAllVersionsOfADocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");

        CanMakeObsoleteResult allVersionsResult = new CanMakeObsoleteResult(
                true);

        // get all AV versions of the document
        List<DocumentModel> docVersions = doc.getCoreSession().getVersions(
                doc.getRef());

        // Check if each version can be made obsolete. If at least one cannot be
        // made obsolete, exit with canMakeObsolete false.
        for (DocumentModel version : docVersions) {

            DocumentModel wcVersion = doc.getCoreSession().getWorkingCopy(
                    version.getRef());
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                    wcVersion);

            CanMakeObsoleteResult versionResult = new CanMakeObsoleteResult();

            if (version.getId().equals(doc.getId())
                    || version.getId().equals(baseDoc.getId())) {
                if (version.getCurrentLifeCycleState().equals(
                        EloraLifeCycleConstants.OBSOLETE)) {
                    log.trace(logInitMsg
                            + "Cannot makeObsolete the document since the it is already in obsolete state.");
                    versionResult.setCannotMakeObsoleteReasonMsg(
                            "eloraplm.message.error.makeobsolete.already.obsolete");
                    versionResult.setCannotMakeObsoleteReasonMsgParam(
                            version.getId());
                } else {
                    versionResult = canMakeObsoleteSingleDocument(session,
                            version);
                }
            } else {
                // Ignore versions that are already in obsolete state
                if (version.getCurrentLifeCycleState().equals(
                        EloraLifeCycleConstants.OBSOLETE)) {
                    versionResult.setCanMakeObsolete(true);
                } else {
                    versionResult = canMakeObsoleteSingleDocument(session,
                            version);
                }
            }

            log.trace(logInitMsg + "canMakeObsolete = |"
                    + versionResult.getCanMakeObsolete()
                    + "| for doc version with id = |" + version.getId() + "|");

            // Complete all versions result message
            if (!versionResult.getCanMakeObsolete()) {
                if (versionResult.getIncompatibleRelatedDocIds().size() > 0) {
                    List<String> partialIncompatibleRelatedDocIds = allVersionsResult.getIncompatibleRelatedDocIds();
                    partialIncompatibleRelatedDocIds.addAll(
                            versionResult.getIncompatibleRelatedDocIds());
                    allVersionsResult.setCanMakeObsolete(false);
                    allVersionsResult.setIncompatibleRelatedDocIds(
                            partialIncompatibleRelatedDocIds);
                } else {

                    // If it is the initiator document, set its error message
                    if (version.getId().equals(doc.getId())
                            || version.getId().equals(baseDoc.getId())) {
                        allVersionsResult.setCanMakeObsolete(false);
                        allVersionsResult.setCannotMakeObsoleteReasonMsg(
                                versionResult.getCannotMakeObsoleteReasonMsg());
                        allVersionsResult.setCannotMakeObsoleteReasonMsgParam(
                                versionResult.getCannotMakeObsoleteReasonMsgParam());
                    } else {
                        // otherwise, the first error message
                        if (allVersionsResult.getCannotMakeObsoleteReasonMsg() == null
                                || allVersionsResult.getCannotMakeObsoleteReasonMsg().length() == 0) {
                            allVersionsResult.setCanMakeObsolete(false);
                            allVersionsResult.setCannotMakeObsoleteReasonMsg(
                                    versionResult.getCannotMakeObsoleteReasonMsg());
                            allVersionsResult.setCannotMakeObsoleteReasonMsgParam(
                                    versionResult.getCannotMakeObsoleteReasonMsgParam());
                        }
                    }
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- with canMakeObsolete = |"
                + allVersionsResult.getCanMakeObsolete() + "|");

        return allVersionsResult;
    }

    protected CanMakeObsoleteResult canMakeObsoleteSingleDocument(
            CoreSession session, DocumentModel doc) throws EloraException {
        String logInitMsg = "[canMakeObsoleteSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");

        CanMakeObsoleteResult result = new CanMakeObsoleteResult();

        // Check that user has permission to change the specified object
        if (session.hasPermission(doc.getRef(), SecurityConstants.WRITE)) {

            DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
            // Check that the WC is not checked out
            if (!wcDoc.isCheckedOut()) {

                // Check that the document is not locked
                if (PromoteHelper.checkDocumentLock(wcDoc)) {

                    // Check if makeObsolete transition is allowed for this
                    // document
                    if (doc.getAllowedStateTransitions().contains(
                            EloraLifeCycleConstants.TRANS_OBSOLETE)) {

                        List<DocumentModel> incompatibleRelatedDocs = MakeObsoleteHelper.checkRelatedDocumentsCompatibility(
                                session, doc);
                        if (incompatibleRelatedDocs == null
                                || incompatibleRelatedDocs.size() == 0) {

                            result.setCanMakeObsolete(true);
                        } else {
                            List<String> incompatibleRelatedDocIds = new ArrayList<String>();

                            for (DocumentModel incompatibleRelatedDoc : incompatibleRelatedDocs) {
                                String incompatibleRelatedDocId = incompatibleRelatedDoc.getId();
                                if (!incompatibleRelatedDocIds.contains(
                                        incompatibleRelatedDocId)) {
                                    incompatibleRelatedDocIds.add(
                                            incompatibleRelatedDocId);
                                }
                            }
                            result.setIncompatibleRelatedDocIds(
                                    incompatibleRelatedDocIds);
                        }

                    } else {
                        log.trace(logInitMsg
                                + "Cannot makeObsolete the document since "
                                + EloraLifeCycleConstants.TRANS_OBSOLETE
                                + " transition is not allowed for this document. Document current state is = |"
                                + doc.getCurrentLifeCycleState() + "|.");
                        result.setCannotMakeObsoleteReasonMsg(
                                "eloraplm.message.error.makeobsolete.transition.not.allowed");
                    }
                } else {
                    log.trace(logInitMsg
                            + "Cannot makeObsolete the document since the document is locked by |"
                            + wcDoc.getLockInfo().getOwner() + "|.");
                    result.setCannotMakeObsoleteReasonMsg(
                            "eloraplm.message.error.makeobsolete.document.locked.by");
                    result.setCannotMakeObsoleteReasonMsgParam(
                            wcDoc.getLockInfo().getOwner());
                }
            } else {
                log.trace(logInitMsg
                        + "Cannot makeObsolete the document since the document is checked out.");
                result.setCannotMakeObsoleteReasonMsg(
                        "eloraplm.message.error.makeobsolete.document.checked.out");
            }
        } else {
            log.trace(logInitMsg
                    + "Cannot makeObsolete the document since the user has not writePermission on the document.");

            result.setCannotMakeObsoleteReasonMsg(
                    "eloraplm.message.error.makeobsolete.no.write.permisssion");
        }

        log.trace(logInitMsg + "--- EXIT --- with canMakeObsolete = |"
                + result.getCanMakeObsolete() + "|");

        return result;
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService#makeObsoleteDocument(org.nuxeo.ecm.core.api.DocumentModel)
     *
     */
    // /////////////////////////////////////////////////////////////////////
    // TODO: metodoa eginda dago, baina kontextu erreal batetan testeatzea
    // falta da.
    // /////////////////////////////////////////////////////////////////////
    @Override
    public CanMakeObsoleteResult makeObsoleteDocument(CoreSession session,
            DocumentModel doc) throws EloraException {
        String logInitMsg = "[makeObsolete] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");
        CanMakeObsoleteResult canMakeObsoletResult = new CanMakeObsoleteResult();

        // /////////////////////////////////////////////////////////////////////
        // TODO: Ondorengo kodea eginda dago, baina testeatu gabe kasu erreal
        // batetan.
        // /////////////////////////////////////////////////////////////////////
        try {
            canMakeObsoletResult = canMakeObsoleteDocument(session, doc);
            if (canMakeObsoletResult.getCanMakeObsolete()) {
                makeObsolete(session, doc);
            } else {
                log.trace(logInitMsg
                        + "Cannot make obsolete document with id = |"
                        + doc.getId() + "|. Reason: |"
                        + canMakeObsoletResult.getCannotMakeObsoleteReasonMsg()
                        + "|, param: |"
                        + canMakeObsoletResult.getCannotMakeObsoleteReasonMsgParam());
            }
        } catch (Exception e) {
            log.error(logInitMsg + "Error making obsolete document with id = |"
                    + doc.getId() + "|. Exception message is: "
                    + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return canMakeObsoletResult;
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService#makeObsoleteDocumentList(java.util.List)
     */
    @Override
    public Map<String, CanMakeObsoleteResult> makeObsoleteDocumentList(
            CoreSession session, List<DocumentModel> docs)
            throws EloraException {
        String logInitMsg = "[makeObsoleteDocumentList] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, CanMakeObsoleteResult> canMakeObsoletWholeListResult = new HashMap<String, CanMakeObsoleteResult>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // Go through the entire list and makeObsolete each entry.
            if (docs != null && docs.size() > 0) {

                canMakeObsoletWholeListResult = checkIfWholeDocumentListCanBeMakeObsole(
                        session, docs);
                if (canMakeObsoletWholeListResult.isEmpty()) {
                    for (DocumentModel doc : docs) {
                        makeObsolete(session, doc);
                    }
                } else {
                    log.trace(logInitMsg
                            + "Cannot make obsolete specified documents list.");
                    Iterator<Entry<String, CanMakeObsoleteResult>> iterator = canMakeObsoletWholeListResult.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, CanMakeObsoleteResult> docResult = iterator.next();
                        log.trace(logInitMsg + "doc id = |" + docResult.getKey()
                                + "| cannot be make obsolete. Reason: |"
                                + docResult.getValue().getCannotMakeObsoleteReasonMsg()
                                + "|, param: |"
                                + docResult.getValue().getCannotMakeObsoleteReasonMsgParam()
                                + "|");
                    }
                }
            } else {
                log.trace(logInitMsg + "Nothing to do. docs is null or empty.");
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();

            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return canMakeObsoletWholeListResult;
    }

    protected void makeObsolete(CoreSession session, DocumentModel doc)
            throws EloraException {
        String logInitMsg = "[makeObsolete] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");
        try {

            // follow transition to become obsolete
            doc.followTransition(EloraLifeCycleConstants.TRANS_OBSOLETE);

            // Update dc:lastContributor, dc:contributors and dc:modified
            doc = EloraDocumentHelper.updateContributorAndModified(doc, false);

            // Fire Nuxeo Event
            String comment = doc.getVersionLabel();
            EloraEventHelper.fireEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc,
                    comment);

            EloraDocumentHelper.disableVersioningDocument(doc);
            doc = session.saveDocument(doc);

            // If the document is locked, we ensure it gets unlocked, as
            // obsolete state cannot be locked
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
                // checks the document out always. This is the only way we know
                // to change the state without checkin the document out.
                EloraDocumentHelper.restoreToVersion(wcDoc.getRef(),
                        new IdRef(version.getId()), true, true, session);
            }

        } catch (Exception e) {
            log.error(logInitMsg + "Error making obsolete document with id = |"
                    + doc.getId() + "|. Exception message is: "
                    + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected Map<String, CanMakeObsoleteResult> checkIfWholeDocumentListCanBeMakeObsole(
            CoreSession session, List<DocumentModel> docs)
            throws EloraException {
        String logInitMsg = "[checkIfWholeDocumentListCanBeMakeObsole] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, CanMakeObsoleteResult> cannotMakeObsoleteDocsResults = new HashMap<String, CanMakeObsoleteResult>();

        try {
            // Keep a list of received docIds
            List<String> docIds = new ArrayList<String>();
            for (DocumentModel doc : docs) {
                String docId = doc.getId();
                if (!docIds.contains(docId)) {
                    docIds.add(docId);
                }
            }

            for (DocumentModel doc : docs) {
                if (MakeObsoleteHelper.impliesMakingObsoleteAllVersions(session,
                        doc)) {
                    // check if all of the versions are contained in the list
                    // get all AV versions of the document
                    List<DocumentModel> docVersions = doc.getCoreSession().getVersions(
                            doc.getRef());
                    for (DocumentModel docVersion : docVersions) {
                        if (!docVersion.getCurrentLifeCycleState().equals(
                                EloraLifeCycleConstants.OBSOLETE)
                                && !docs.contains(docVersion)) {

                            CanMakeObsoleteResult versionResult = new CanMakeObsoleteResult(
                                    false,
                                    "eloraplm.message.error.makeobsolete.documentFromList.version.missing.since.implies.making.obsolete.all.versions",
                                    docVersion.getId());
                            cannotMakeObsoleteDocsResults.put(doc.getId(),
                                    versionResult);
                            // exit from loop
                            break;
                        }
                    }
                    if (!cannotMakeObsoleteDocsResults.isEmpty()) {
                        // exit from loop
                        break;
                    }
                }
            }

            if (cannotMakeObsoleteDocsResults.isEmpty()) {
                for (DocumentModel doc : docs) {
                    if (doc.getCurrentLifeCycleState().equals(
                            EloraLifeCycleConstants.OBSOLETE)) {
                        log.trace(logInitMsg
                                + "Cannot makeObsolete the document since the it is already in obsolete state.");
                        CanMakeObsoleteResult docResult = new CanMakeObsoleteResult(
                                false,
                                "eloraplm.message.error.makeobsolete.documentFromList.already.obsolete");
                        cannotMakeObsoleteDocsResults.put(doc.getId(),
                                docResult);
                        // exit from loop
                        break;
                    } else {
                        CanMakeObsoleteResult docResult = canMakeObsoleteSingleDocument(
                                session, doc);
                        if (!docResult.getCanMakeObsolete()) {
                            if (docResult.getIncompatibleRelatedDocIds() != null
                                    && docResult.getIncompatibleRelatedDocIds().size() > 0) {
                                // check if incompatible docs are included
                                boolean allIncompatibleRealtedDocsAreIncluded = true;
                                for (String incompatibleRelatedDocId : docResult.getIncompatibleRelatedDocIds()) {
                                    if (!docIds.contains(
                                            incompatibleRelatedDocId)) {
                                        allIncompatibleRealtedDocsAreIncluded = false;
                                        // exit from loop
                                        break;
                                    }
                                }
                                if (!allIncompatibleRealtedDocsAreIncluded) {
                                    log.trace(logInitMsg
                                            + "Cannot makeObsolete the document since it has incompatible related docs.");

                                    docResult.setCannotMakeObsoleteReasonMsg(
                                            "eloraplm.message.error.makeobsolete.documentFromList.incompatible.related.docs");
                                    cannotMakeObsoleteDocsResults.put(
                                            doc.getId(), docResult);
                                    // exit from loop
                                    break;
                                }
                            } else {
                                log.trace(logInitMsg
                                        + "Cannot makeObsolete the document since. Reason: |"
                                        + docResult.getCannotMakeObsoleteReasonMsg()
                                        + "|, param: |"
                                        + docResult.getCannotMakeObsoleteReasonMsgParam());
                                cannotMakeObsoleteDocsResults.put(doc.getId(),
                                        docResult);
                                // exit from loop
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- with canMakeObsolete = |"
                + cannotMakeObsoleteDocsResults.isEmpty() + "|");

        return cannotMakeObsoleteDocsResults;
    }

}
