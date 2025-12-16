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

package com.aritu.eloraplm.bom.linked.characteristics.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsQueryFactory;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Listens to the overwrite event and find all documents that have linked
 * characteristics of the overwritten document. For each found document:
 *
 * - Check that all linked characteristics are still valid. If a characteristic
 * has been removed, remove also the linked characteristic entry.
 *
 * - If the document's viewer shows linked characteristics of the overwritten
 * document, update the viewer file.
 *
 * @author aritu
 *
 */
public class UpdateLinkedCharacteristicsOnOverwriteListener
        implements PostCommitEventListener {

    private static Log log = LogFactory.getLog(
            UpdateLinkedCharacteristicsOnOverwriteListener.class);

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

                        // Only listen documents that can have BOM
                        // Characteristics

                        if (EloraDocumentHelper.checkFilter(doc,
                                BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
                            UnrestrictedLinkedCharacUpdater ulcvu = new UnrestrictedLinkedCharacUpdater(
                                    session, baseVersion);
                            ulcvu.runUnrestricted();
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

    private class UnrestrictedLinkedCharacUpdater
            extends UnrestrictedSessionRunner {

        private DocumentModel baseVersion;

        private UnrestrictedLinkedCharacUpdater(CoreSession session,
                DocumentModel baseVersion) {
            super(session);
            this.baseVersion = baseVersion;
        }

        @Override
        public void run() {
            try {

                processDocumentsWithLinkedCharacs(session, baseVersion);

            } catch (Exception e) {
                log.error(
                        "[UnrestrictedLinkedCharacUpdater] Uncontrolled exception: "
                                + e.getClass().getName() + ". "
                                + e.getMessage(),
                        e);
            }
        }

        private void processDocumentsWithLinkedCharacs(CoreSession session,
                DocumentModel overwrittenDocBaseVersion) throws EloraException {

            String logInitMsg = "[processLinkedCharacDocs] ["
                    + session.getPrincipal().getName() + "] ";
            log.trace(logInitMsg + "Processing document |"
                    + overwrittenDocBaseVersion.getId() + "|.");

            String query = BomCharacteristicsQueryFactory.getDocsHavingLinkedCharacByLinkedDocQuery(
                    overwrittenDocBaseVersion.getId());

            DocumentModelList docsWithLinkedCharac = session.query(query);

            if (docsWithLinkedCharac != null
                    && !docsWithLinkedCharac.isEmpty()) {

                log.trace(logInitMsg + "|" + docsWithLinkedCharac.size()
                        + "| documents found with linked characteristics of ovewritten document |"
                        + overwrittenDocBaseVersion.getId() + "|.");

                Map<String, DocumentModel> affectedWcDocs = new HashMap<String, DocumentModel>();

                Set<String> overwrittenDocBomCharacsIds = new HashSet<String>();

                @SuppressWarnings("unchecked")
                ArrayList<HashMap<String, Object>> overwrittenDocCharacteristics = (ArrayList<HashMap<String, Object>>) overwrittenDocBaseVersion.getPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);

                if (overwrittenDocCharacteristics.size() > 0) {
                    for (int i = 0; i < overwrittenDocCharacteristics.size(); i++) {
                        HashMap<String, Object> overwrittenDocCharac = overwrittenDocCharacteristics.get(
                                i);
                        String bomCharacteristicId = (String) overwrittenDocCharac.get(
                                "bomCharacteristicId");
                        overwrittenDocBomCharacsIds.add(bomCharacteristicId);
                    }
                }

                for (DocumentModel docWithLinkedCharac : docsWithLinkedCharac) {

                    // If the related doc is a WC, the base AV will also have
                    // same linked characteristics.
                    // We have to update the AV, and then just restore the WC at
                    // the end.
                    if (docWithLinkedCharac.isImmutable()) {

                        // update document's linked characteristics, if required
                        docWithLinkedCharac = updateLinkedCharacteristics(
                                session, docWithLinkedCharac,
                                overwrittenDocBaseVersion,
                                overwrittenDocBomCharacsIds);

                        // update viewer file
                        updateViewerFile(session, docWithLinkedCharac,
                                overwrittenDocBaseVersion);

                        // Add WC to affectedDocs
                        DocumentModel docWithLinkedCharacWc = session.getWorkingCopy(
                                docWithLinkedCharac.getRef());
                        String docWithLinkedCharacWcId = docWithLinkedCharacWc.getId();
                        if (!affectedWcDocs.containsKey(
                                docWithLinkedCharacWcId)) {
                            affectedWcDocs.put(docWithLinkedCharacWcId,
                                    docWithLinkedCharacWc);
                        }
                    }
                }

                // Restore WCs
                restoreAffectedWcs(session, affectedWcDocs);
            }
        }

        private void restoreAffectedWcs(CoreSession session,
                Map<String, DocumentModel> affectedWcs) {

            String logInitMsg = "[restoreAffectedWcs] ["
                    + session.getPrincipal().getName() + "] ";

            log.trace(logInitMsg + "|" + affectedWcs.size()
                    + "| WC documents to be restored.");

            for (DocumentModel affectedWc : affectedWcs.values()) {
                DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                        affectedWc);
                if (!affectedWc.isCheckedOut() && baseVersion != null) {
                    // The only thing that has changed is the viewer file, so we
                    // need to call to a simple restore (no relations restored)
                    EloraDocumentHelper.restoreToVersion(affectedWc.getRef(),
                            baseVersion.getRef(), true, true, session);

                    log.trace(logInitMsg + "|" + affectedWc.getId()
                            + "| WC restored.");
                }
            }
        }

        @SuppressWarnings("unchecked")
        private DocumentModel updateLinkedCharacteristics(CoreSession session,
                DocumentModel docToBeUpdated,
                DocumentModel overwrittenDocBaseVersion,
                Set<String> overwrittenDocBomCharacsIds) throws EloraException {

            boolean linkedCharacteristicsUpdated = false;

            // Begiratu dokumentuak dituen linkedCharacteristics-ak banan-banan.
            ArrayList<HashMap<String, Object>> linkedCharacteristics = new ArrayList<HashMap<String, Object>>();
            ArrayList<HashMap<String, Object>> updatedLinkedCharacteristics = new ArrayList<HashMap<String, Object>>();

            if (docToBeUpdated.getPropertyValue(
                    BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST) != null) {
                linkedCharacteristics = (ArrayList<HashMap<String, Object>>) docToBeUpdated.getPropertyValue(
                        BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST);
            }

            if (linkedCharacteristics.size() > 0) {

                for (int i = 0; i < linkedCharacteristics.size(); i++) {

                    HashMap<String, Object> linkedCharac = linkedCharacteristics.get(
                            i);

                    String linkedDocumentUid = (String) linkedCharac.get(
                            BomCharacteristicsConstants.LINKED_DOCUMENT);

                    if (linkedDocumentUid != null && linkedDocumentUid.equals(
                            overwrittenDocBaseVersion.getId())) {

                        String linkedDocumentCharacteristicId = (String) linkedCharac.get(
                                BomCharacteristicsConstants.LINKED_DOCUMENT_CHARAC_ID);

                        // overwrittenDocBomCharacsIds zerrendan ez badaude,
                        // ezabatu.
                        if (overwrittenDocBomCharacsIds.contains(
                                linkedDocumentCharacteristicId)) {
                            updatedLinkedCharacteristics.add(linkedCharac);
                        } else {
                            linkedCharacteristicsUpdated = true;
                        }
                    } else {
                        updatedLinkedCharacteristics.add(linkedCharac);
                    }
                }
            }

            if (linkedCharacteristicsUpdated) {
                docToBeUpdated.setPropertyValue(
                        BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST,
                        updatedLinkedCharacteristics);

                EloraDocumentHelper.disableVersioningDocument(docToBeUpdated);
                docToBeUpdated = session.saveDocument(docToBeUpdated);
            }

            return docToBeUpdated;
        }

        private DocumentModel updateViewerFile(CoreSession session,
                DocumentModel docToBeUpdated,
                DocumentModel docCausingViewerUpdate) throws EloraException {

            String logInitMsg = "[updateViewerFile] ["
                    + session.getPrincipal().getName() + "] ";

            log.trace(logInitMsg + "Update viewer file for doc |"
                    + docToBeUpdated.getId() + "| due to change in doc |"
                    + docCausingViewerUpdate.getId() + "|.");

            // Fire PdmViewerDocOverwrittenEvent Nuxeo Event (viewer creation
            // listens this event)

            // Construct event comment
            Serializable ref = docCausingViewerUpdate.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            String docReference = ref != null ? ref.toString() : null;
            String comment = docToBeUpdated.getVersionLabel();
            comment += " Due to Linked Characteristics of overwritten document: ";
            if (docReference != null) {
                comment += docReference;
            }
            comment += " " + docCausingViewerUpdate.getTitle() + " ("
                    + docCausingViewerUpdate.getVersionLabel() + ")";

            // Fire the event
            EloraEventHelper.fireEvent(
                    PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT,
                    docToBeUpdated, comment);

            log.trace(logInitMsg + "|"
                    + PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT
                    + "| event fired on document |" + docToBeUpdated.getId()
                    + "| with comment |" + comment + "|.");

            EloraDocumentHelper.disableVersioningDocument(docToBeUpdated);
            docToBeUpdated = session.saveDocument(docToBeUpdated);

            return docToBeUpdated;
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
