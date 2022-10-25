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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * Listens to the overwrite event, finds all the parent docs related to the
 * document that has been overwritten, and updates their viewer files. It is
 * designed for CAD and Basic document overwrites.
 *
 * @author aritu
 *
 */
public class RelatedViewerUpdateOnOverwriteListener
        implements PostCommitEventListener {

    private static Log log = LogFactory.getLog(
            RelatedViewerUpdateOnOverwriteListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        for (Event each : events) {
            onEvent(each);
        }
    }

    private void onEvent(Event event) {
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

                try {
                    CoreSession session = doc.getCoreSession();
                    DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                            doc);
                    if (baseVersion != null) {
                        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                                || doc.hasFacet(
                                        EloraFacetConstants.FACET_CAD_DOCUMENT)
                                || doc.hasFacet(
                                        EloraFacetConstants.FACET_BASIC_DOCUMENT)) {

                            log.info(logInitMsg
                                    + "Checking related documents that use the document |"
                                    + baseVersion.getId()
                                    + "| as viewer to update...");

                            UnrestrictedViewerUpdater uvu = new UnrestrictedViewerUpdater(
                                    session, baseVersion);
                            uvu.runUnrestricted();

                            log.info(logInitMsg + "Related documents checked.");

                        }
                    }
                } catch (Exception e) {
                    log.error(logInitMsg
                            + "There was an error while updating related docs' viewer files. Error message: "
                            + e.getMessage());
                }
            }
        }
    }

    private class UnrestrictedViewerUpdater extends UnrestrictedSessionRunner {

        private DocumentModel baseVersion;

        private List<String> processedUids = new ArrayList<String>();

        private UnrestrictedViewerUpdater(CoreSession session,
                DocumentModel baseVersion) {
            super(session);
            this.baseVersion = baseVersion;
        }

        @Override
        public void run() {
            try {
                processRelatedDocs(session, baseVersion);
            } catch (Exception e) {
                log.error("[UnrestrictedViewerUpdater] Uncontrolled exception: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);
            }
        }

        private void processRelatedDocs(CoreSession session,
                DocumentModel baseVersion) throws EloraException {

            processedUids.add(baseVersion.getId());

            String logInitMsg = "[processRelatedDocs] ["
                    + session.getPrincipal().getName() + "] ";
            log.trace(
                    logInitMsg + "Processing |" + baseVersion.getId() + "|...");

            String query = EloraQueryFactory.getRelationsWithViewerUse(
                    baseVersion.getId());
            DocumentModelList relations = session.query(query);
            if (relations != null && !relations.isEmpty()) {
                Map<String, DocumentModel> affectedWcs = new HashMap<String, DocumentModel>();

                for (DocumentModel relation : relations) {
                    Serializable inv = relation.getPropertyValue(
                            EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING);
                    String docId;
                    if (inv == null || ((long) inv) == 0L) {
                        Serializable subj = relation.getPropertyValue(
                                NuxeoMetadataConstants.NX_RELATION_SOURCE);
                        docId = (String) subj;
                    } else {
                        Serializable obj = relation.getPropertyValue(
                                NuxeoMetadataConstants.NX_RELATION_TARGET);
                        docId = (String) obj;
                    }

                    DocumentModel relatedDoc = session.getDocument(
                            new IdRef(docId));

                    // If the related doc is a WC, the base AV will also have a
                    // relation to the document. We have to update the viewer
                    // file of the AV, and then just restore the WC at the end.
                    if (relatedDoc.isImmutable()) {
                        DocumentModel affectedWc = session.getWorkingCopy(
                                relatedDoc.getRef());
                        // We also exclude the docs that are checked out, or we
                        // will end up with corrupted relations
                        if (!affectedWc.isCheckedOut()) {
                            log.info(logInitMsg + "Updating viewer file for |"
                                    + relatedDoc.getId() + "|...");
                            updateViewerFile(session, relatedDoc, baseVersion);
                            String affectedWcId = affectedWc.getId();
                            if (!affectedWcs.containsKey(affectedWcId)) {
                                affectedWcs.put(affectedWcId, affectedWc);
                            }
                        }
                    }
                }

                log.trace(logInitMsg + "Restoring working copies...");
                restoreAffectedWcs(session, affectedWcs);
            }
        }

        private void restoreAffectedWcs(CoreSession session,
                Map<String, DocumentModel> affectedWcs) {

            for (DocumentModel affectedWc : affectedWcs.values()) {
                DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                        affectedWc);
                if (baseVersion != null) {
                    // The only thing that has changed is the viewer file, so we
                    // need to call to a simple restore (no relations restored)
                    EloraDocumentHelper.restoreToVersion(affectedWc.getRef(),
                            baseVersion.getRef(), true, true, session);
                }
            }
        }

        private void updateViewerFile(CoreSession session,
                DocumentModel affectedDoc, DocumentModel baseVersion)
                throws EloraException {

            // Nuxeo Event (viewer creation listens this event)
            Serializable ref = baseVersion.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            String docReference = ref != null ? ref.toString() : null;
            String comment = affectedDoc.getVersionLabel();
            comment += " Caused by: ";
            if (docReference != null) {
                comment += docReference;
            }
            comment += " " + baseVersion.getTitle() + " ("
                    + baseVersion.getVersionLabel() + ")";

            EloraEventHelper.fireEvent(
                    PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT, affectedDoc,
                    comment);

            if (affectedDoc.isImmutable()) {
                EloraDocumentHelper.disableVersioningDocument(affectedDoc);
            }

            affectedDoc = session.saveDocument(affectedDoc);

            // We call again processRelatedDocs with the affectedDoc to update
            // all the levels. Only if not processed, to cut loops.
            if (!processedUids.contains(affectedDoc.getId())) {
                processRelatedDocs(session, affectedDoc);
            }
        }
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
