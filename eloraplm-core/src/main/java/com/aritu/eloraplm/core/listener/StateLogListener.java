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
package com.aritu.eloraplm.core.listener;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static com.aritu.eloraplm.constants.PdmEventNames.PDM_CHECKED_IN_EVENT;
import static com.aritu.eloraplm.constants.PdmEventNames.PDM_OVERWRITTEN_EVENT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.StateLogHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * This class logs all state changes of a document.
 *
 * @author aritu
 *
 */
public class StateLogListener implements EventListener {

    private static final Log log = LogFactory.getLog(StateLogListener.class);

    @Override
    public void handleEvent(Event event) {

        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docEventCtx = (DocumentEventContext) event.getContext();

            // Check that we are handling the right event
            if (isEventHandled(event)) {

                // Get the document model from the event context
                DocumentModel doc = docEventCtx.getSourceDocument();
                // Process the event
                CoreSession session = docEventCtx.getCoreSession();

                // Check document's facet in order to know if it is storing
                // States Log.
                if (doc.hasFacet(EloraFacetConstants.FACET_STORE_STATES_LOG)) {

                    // Ignore document templates, that is, documents that are
                    // located under TemplateRoot directory
                    if (EloraDocumentHelper.isDocumentUnderTemplateRoot(doc,
                            session)) {
                        log.trace(logInitMsg
                                + "--- return [template document]---");
                        return;
                    }

                    String eventName = event.getName();

                    // Filter the event
                    if (eventName.equals(DOCUMENT_CREATED)
                            || eventName.equals(DOCUMENT_CREATED_BY_COPY)) {
                        // don't handle proxies and versions.
                        if (doc.isProxy() || doc.isVersion()) {
                            return;
                        }
                    }

                    log.trace(logInitMsg + "eventName = |" + eventName + "|");

                    if (eventName.equals(TRANSITION_EVENT)) {
                        processTransitionEvent(docEventCtx, doc, session);
                    } else if (eventName.equals(DOCUMENT_CREATED)
                            || eventName.equals(DOCUMENT_CREATED_BY_COPY)) {
                        processDocumentCreationEvent(doc, session);
                    } else if (eventName.equals(PDM_CHECKED_IN_EVENT)) {
                        processCheckedInEvent(doc, session);
                    } else if (eventName.equals(PDM_OVERWRITTEN_EVENT)) {
                        processOverwrittenEvent(doc, session);
                    }
                }
            } else {
                log.trace(logInitMsg + "--- return [invalid event name]---");
            }
        } else {
            log.trace(logInitMsg + "--- return [invalid event context]---");
        }
    }

    protected boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledEventsName() {
        return Arrays.asList(TRANSITION_EVENT, DOCUMENT_CREATED,
                DOCUMENT_CREATED_BY_COPY, PDM_CHECKED_IN_EVENT,
                PDM_OVERWRITTEN_EVENT);
    }

    protected void processTransitionEvent(DocumentEventContext docEventCtx,
            DocumentModel doc, CoreSession session) {

        String logInitMsg = "[processTransitionEvent] ["
                + session.getPrincipal().getName() + "] ";

        // Get the original state, final state and transition
        String stateFrom = (String) docEventCtx.getProperty(
                LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM);
        String stateTo = (String) docEventCtx.getProperty(
                LifeCycleConstants.TRANSTION_EVENT_OPTION_TO);
        String transition = (String) docEventCtx.getProperty(
                LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);

        log.trace(logInitMsg + "stateFrom = |" + stateFrom + "| stateTo = |"
                + stateTo + "|, transition = |" + transition + "|");

        String versionDocId = doc.getId();

        StateLogHelper.addStateLogProperties(doc,
                session.getPrincipal().toString(), stateFrom, stateTo,
                transition, versionDocId);

        log.trace(logInitMsg + "stateLogProperties added");

        saveProcessedDocument(doc, session);

        if (doc.isVersion()) {
            DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
            restoreWcFromVersionDoc(wcDoc, doc, session);
        }
    }

    protected void processDocumentCreationEvent(DocumentModel doc,
            CoreSession session) {

        String logInitMsg = "[processDocumentCreationEvent] ["
                + session.getPrincipal().getName() + "] ";

        String stateTo = null;

        Serializable initialStateCtxt = doc.getContextData(
                LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME);
        if (initialStateCtxt == null) {
            LifeCycleService lcService = NXCore.getLifeCycleService();
            stateTo = lcService.getLifeCycleByName(
                    lcService.getLifeCycleNameFor(
                            doc.getType())).getDefaultInitialStateName();
        } else {
            stateTo = initialStateCtxt.toString();
        }
        log.trace(logInitMsg + "stateTo = |" + stateTo + "|");

        // Ignore PRECREATED state
        if (EloraLifeCycleConstants.PRECREATED.equals(stateTo)) {
            log.trace(logInitMsg + "Ignore |" + stateTo
                    + "| state in state logs registry.");
            return;
        }

        String versionDocId = doc.getId();
        StateLogHelper.addStateLogProperties(doc,
                session.getPrincipal().toString(), null, stateTo, null,
                versionDocId);

        log.trace(logInitMsg + "stateLogProperties added");

        saveProcessedDocument(doc, session);
    }

    protected void processCheckedInEvent(DocumentModel doc,
            CoreSession session) {

        String logInitMsg = "[processCheckedInEvent] ["
                + session.getPrincipal().getName() + "] ";

        if (doc.isVersion()) {
            log.error(logInitMsg
                    + "It is not possible to checkin a versioned document.");
            return;
        }

        DocumentModel versionDoc = EloraDocumentHelper.getBaseVersion(doc);

        boolean updated = StateLogHelper.updateVersionDocIdInStateLogIfRequired(
                versionDoc, session);

        if (updated) {
            log.trace(logInitMsg + "versionDocId updated");
            saveProcessedDocument(versionDoc, session);
            restoreWcFromVersionDoc(doc, versionDoc, session);
        }
    }

    protected void processOverwrittenEvent(DocumentModel doc,
            CoreSession session) {

        String logInitMsg = "[processOverwrittenEvent] ["
                + session.getPrincipal().getName() + "] ";

        if (doc.isVersion()) {
            log.error(logInitMsg
                    + "It is not possible to overwrite a versioned document.");
            return;
        }

        DocumentModel versionDoc = EloraDocumentHelper.getBaseVersion(doc);

        boolean updated = StateLogHelper.removeLastStateInStateLogIfRequired(
                versionDoc, session);

        if (updated) {
            log.trace(logInitMsg + "last StateLog removed.");
            saveProcessedDocument(versionDoc, session);
            restoreWcFromVersionDoc(doc, versionDoc, session);
        }
    }

    protected void saveProcessedDocument(DocumentModel doc,
            CoreSession session) {

        String logInitMsg = "[saveProcessedDocument] ["
                + session.getPrincipal().getName() + "] ";

        try {
            EloraDocumentHelper.disableVersioningDocument(doc);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage());
            return;
        }
        session.saveDocument(doc);
    }

    protected void restoreWcFromVersionDoc(DocumentModel wcDoc,
            DocumentModel versionDoc, CoreSession session) {

        String logInitMsg = "[restoreWcFromVersinDoc] ["
                + session.getPrincipal().getName() + "] ";

        EloraDocumentHelper.restoreToVersion(wcDoc.getRef(),
                versionDoc.getRef(), true, true, session);

        log.trace(logInitMsg + "wc restored from versionDoc id = "
                + versionDoc.getId() + "|");
    }
}
