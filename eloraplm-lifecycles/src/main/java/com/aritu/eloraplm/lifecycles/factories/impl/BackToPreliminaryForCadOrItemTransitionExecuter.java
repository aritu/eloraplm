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
package com.aritu.eloraplm.lifecycles.factories.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.EloraExceptionWithStoredMessage;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;

/**
 * @author aritu
 *
 */
public class BackToPreliminaryForCadOrItemTransitionExecuter
        implements TransitionExecuter {

    private static final String transition = EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY;

    private static final String targetState = EloraLifeCycleConstants.PRELIMINARY;

    private List<String> errorList;

    private boolean canBeExecuted;

    private List<Resource> predicatesToCheck;

    @Override
    public String getPreviousScreen() {
        return "/incl/action/demote_promote_previous_screen_with_error_messages.xhtml";
    }

    @Override
    public void init(DocumentModel doc) {
        errorList = new ArrayList<String>();
        canBeExecuted = true;

        List<String> relationsToCheck = new ArrayList<String>();
        relationsToCheck.addAll(RelationsConfig.cadRelationsList);
        relationsToCheck.addAll(RelationsConfig.bomHierarchicalRelationsList);
        relationsToCheck.addAll(RelationsConfig.bomDirectRelationsList);
        relationsToCheck.addAll(RelationsConfig.docRelationsList);
        predicatesToCheck = getPredicateResourceList(relationsToCheck);

        processTransitionChecks(doc);
    }

    private void processTransitionChecks(DocumentModel doc) {

        try {
            DocumentModel wcDoc;
            CoreSession session = doc.getCoreSession();
            if (doc.isImmutable()) {
                wcDoc = session.getWorkingCopy(doc.getRef());
            } else {
                wcDoc = doc;
            }

            if (wcDoc.isCheckedOut()) {
                throw new EloraExceptionWithStoredMessage(
                        "The document to demote (backToPreliminary), with id |"
                                + doc.getId() + "| is checked out.",
                        "eloraplm.label.error.promoteDemote.docCheckedOut");
            }

            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
            if (baseDoc == null || (doc.isImmutable()
                    && !doc.getId().equals(baseDoc.getId()))) {
                throw new EloraExceptionWithStoredMessage(
                        "The document to demote (" + transition + "), with id |"
                                + doc.getId()
                                + "| is not a working copy or its base. Only working copies and base AVs can be demoted with this transition.",
                        "eloraplm.label.error.promoteDemote.docNotBaseOrWc");
            }

            if (!doc.getAllowedStateTransitions().contains(transition)) {
                throw new EloraExceptionWithStoredMessage(
                        "The document to demote (" + transition + "), with id |"
                                + doc.getId() + "| has no " + transition
                                + " transition.",
                        "eloraplm.label.error.promoteDemote.transitionNotAllowed");
            }

            if (!relatedDocumentsAllowTransition(baseDoc)) {
                throw new EloraExceptionWithStoredMessage(
                        "The document to demote (" + transition + "), with id |"
                                + doc.getId()
                                + "| is related to a document that does not allow the target state of the transition.",
                        "eloraplm.label.error.promoteDemote.transitionNotAllowedByRelations");
            }

        } catch (EloraExceptionWithStoredMessage e) {
            canBeExecuted = false;
            errorList.add(e.getStoredMessage());
        } catch (Exception e) {
            canBeExecuted = false;
            errorList.add(
                    "eloraplm.label.error.promoteDemote.unknownException");
        }
    }

    private boolean relatedDocumentsAllowTransition(DocumentModel doc)
            throws EloraException {
        return relatedParentsAllowTransition(doc)
                && relatedChildrenAllowTransition(doc);
    }

    private boolean relatedParentsAllowTransition(DocumentModel doc)
            throws EloraException {
        CoreSession session = doc.getCoreSession();

        if (!LifecyclesConfig.allowedByAllStatesTransitionsList.contains(
                transition)) {

            List<Statement> stmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                    doc, predicatesToCheck);
            for (Statement stmt : stmts) {
                DocumentModel subject = RelationHelper.getDocumentModel(
                        stmt.getSubject(), session);
                if (!LifecyclesConfig.isSupported(
                        subject.getCurrentLifeCycleState(), targetState)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean relatedChildrenAllowTransition(DocumentModel doc)
            throws EloraException {
        CoreSession session = doc.getCoreSession();

        if (!LifecyclesConfig.allowsAllStatesTransitionsList.contains(
                transition)) {

            List<Statement> stmts = EloraRelationHelper.getStatements(doc,
                    predicatesToCheck);
            for (Statement stmt : stmts) {
                DocumentModel object = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);
                if (!LifecyclesConfig.isSupported(targetState,
                        object.getCurrentLifeCycleState())) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<Resource> getPredicateResourceList(
            List<String> predicateList) {
        List<Resource> predicates = new ArrayList<Resource>();
        for (String predicateUri : predicateList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            predicates.add(predicateResource);
        }
        return predicates;
    }

    @Override
    public boolean canBeExecuted() {
        return canBeExecuted;
    }

    @Override
    public List<String> getErrorList() {
        return errorList;
    }

    @Override
    public void execute(DocumentModel doc) throws EloraException {
        CoreSession session = doc.getCoreSession();

        DocumentModel wcDoc;
        DocumentModel baseDoc;
        if (!doc.isImmutable()) {
            wcDoc = doc;
            baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc == null) {
                throw new EloraException("The document |" + doc.getId()
                        + "| has no base version.");
            }
        } else {
            wcDoc = session.getWorkingCopy(doc.getRef());
            baseDoc = doc;
        }

        baseDoc.followTransition(transition);

        // Update dc:lastContributor, dc:contributors and dc:modified
        doc = EloraDocumentHelper.updateContributorAndModified(baseDoc, false);

        // Nuxeo Event (we cannot fire default event, the document has to be
        // baseDoc)
        String comment = baseDoc.getVersionLabel();
        EloraEventHelper.fireEvent(PdmEventNames.PDM_DEMOTED_EVENT, baseDoc,
                comment);

        EloraDocumentHelper.disableVersioningDocument(baseDoc);
        baseDoc = session.saveDocument(baseDoc);

        // We get the real last version (as Nuxeo does)
        DocumentRef lastVersionRef = session.getLastDocumentVersionRef(
                wcDoc.getRef());

        // We cannot follow transition instead of restoring, because it
        // checks the document out always. This is the only way we know to
        // change the state without checkin the document out.
        EloraDocumentHelper.restoreToVersion(wcDoc.getRef(), lastVersionRef,
                true, true, session);
    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return false;
    }

}
