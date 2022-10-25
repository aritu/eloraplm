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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.lifecycles.api.LifecycleConfigService;

/**
 *
 * This class listens to all state changes of a document.
 *
 * @author aritu
 *
 */
public class DocFinalStateListener implements EventListener {

    private static final Log log = LogFactory.getLog(
            DocFinalStateListener.class);

    @Override
    public void handleEvent(Event event) {

        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docEventCtx = (DocumentEventContext) event.getContext();

            // Check that we are handling the right event
            if (isEventHandled(event)) {

                // Check if we have to skip the listener
                if (docEventCtx.hasProperty("default/"
                        + EloraGeneralConstants.CONTEXT_SKIP_STATE_LOG_LISTENER)) {
                    return;
                }

                // Get the document model from the event context
                DocumentModel doc = docEventCtx.getSourceDocument();
                CoreSession session = docEventCtx.getCoreSession();

                // Process the event
                processEvent(docEventCtx, doc, session);

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
        return Arrays.asList(TRANSITION_EVENT);
    }

    protected void processEvent(DocumentEventContext docEventCtx,
            DocumentModel doc, CoreSession session) {

        String logInitMsg = "[processEvent] ["
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

        // if stateTo is a final State => remove the document from favorites
        LifecycleConfigService lcs = Framework.getService(
                LifecycleConfigService.class);
        boolean isFinalState = lcs.isFinalState(stateTo);
        log.trace(logInitMsg + "isFinalState = |" + isFinalState + "|");

        if (isFinalState) {
            final FavoritesManager favoritesManager = Framework.getLocalService(
                    FavoritesManager.class);

            if (favoritesManager.isFavorite(doc, session)) {
                favoritesManager.removeFromFavorites(doc, session);

                log.trace(logInitMsg + "Document docId = |" + doc.getId()
                        + "| removed from favorites.");
            }
        }
    }

}
