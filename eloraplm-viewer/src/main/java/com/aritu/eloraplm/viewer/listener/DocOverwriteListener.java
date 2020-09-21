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

package com.aritu.eloraplm.viewer.listener;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Listens to the overwrite event, finds all the parent docs related to the
 * document that has been overwritten, and updates their viewer files. It is
 * designed for CAD and Basic document overwrites.
 *
 * @author aritu
 *
 */
public class DocOverwriteListener implements EventListener {

    private static Log log = LogFactory.getLog(DocOverwriteListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                if (doc.isImmutable()) {
                    // For now, we only listen when it's overwritten from the WC
                    return;
                }

                String logInitMsg = "[handleEvent] ["
                        + event.getContext().getPrincipal().getName() + "] ";
                log.trace(logInitMsg + "--- ENTER --- ");

                try {
                    CoreSession session = doc.getCoreSession();
                    DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                            doc);
                    if (baseVersion != null) {
                        if (doc.hasFacet(
                                EloraFacetConstants.FACET_CAD_DOCUMENT)) {
                            processRelatedDocs(session, baseVersion,
                                    EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
                        }
                        // CAUTION! At the moment, we have no way to limit
                        // BasicDocument
                        // to REAL basic documents, so CAD and BOM docs also
                        // have this
                        // facet
                        else if (doc.hasFacet(
                                EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
                            processRelatedDocs(session, baseVersion,
                                    EloraRelationConstants.BOM_HAS_DOCUMENT);
                            processRelatedDocs(session, baseVersion,
                                    EloraRelationConstants.CAD_HAS_DOCUMENT);
                        }
                    }
                } catch (Exception e) {
                    log.trace(logInitMsg
                            + "There was an error while updating item's viewer file because one of its related documents has been overwritten. Error message: "
                            + e.getMessage());
                }

                log.trace(logInitMsg + "--- EXIT --- ");
            }
        }
    }

    private void processRelatedDocs(CoreSession session, DocumentModel doc,
            String predicateUri) throws EloraException {
        Map<String, DocumentModel> wcSubjects = new HashMap<String, DocumentModel>();
        Resource predicate = new ResourceImpl(predicateUri);
        List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, doc, predicate);
        for (Statement stmt : stmts) {
            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
            Integer order = stmtInfo.getViewerOrdering();
            if (order != null && order != 0) {
                DocumentModel subject = RelationHelper.getDocumentModel(
                        stmtInfo.getSubject(), session);
                // If the subject is a WC, the base AV will also have a relation
                // to
                // the document. We only have to restore the WC at the end.
                if (subject.isImmutable()) {
                    DocumentModel wcSubject = session.getWorkingCopy(
                            subject.getRef());
                    // We also exclude the subjects that are checked out, or we
                    // will end up with corrupted relations
                    if (!wcSubject.isCheckedOut()) {
                        updateViewerFile(session, subject, doc);
                        String wcId = wcSubject.getId();
                        if (!wcSubjects.containsKey(wcId)) {
                            wcSubjects.put(wcId, wcSubject);
                        }
                    }
                }
            }
        }
        for (DocumentModel wcSubject : wcSubjects.values()) {
            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                    wcSubject);
            if (wcSubject != null) {
                // The only thing that has changed is the viewer file, so we
                // need to call to a simple restore (no relations restored)
                EloraDocumentHelper.restoreToVersion(wcSubject.getRef(),
                        baseVersion.getRef(), true, true, session);
            }
        }
    }

    private void updateViewerFile(CoreSession session, DocumentModel subject,
            DocumentModel doc) throws EloraException {

        // Nuxeo Event (viewer creation listens this event)
        Serializable ref = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);
        String docReference = ref != null ? ref.toString() : null;
        String comment = subject.getVersionLabel();
        comment += " Caused by: ";
        if (docReference != null) {
            comment += docReference;
        }
        comment += doc.getTitle() + " (" + doc.getVersionLabel() + ")";
        // if (justification != null) {
        // comment += " " + justification;
        // }
        EloraEventHelper.fireEvent(PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT,
                subject, comment);

        if (subject.isImmutable()) {
            EloraDocumentHelper.disableVersioningDocument(subject);
        }

        session.saveDocument(subject);
    }

    private boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getHandledEventsName() {
        return Arrays.asList(PdmEventNames.PDM_OVERWRITTEN_EVENT);
    }
}
