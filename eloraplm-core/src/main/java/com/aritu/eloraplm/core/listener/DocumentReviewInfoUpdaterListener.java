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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.ReviewInfoHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * This class will listen to the document's state change event in order to
 * manage document Review Info metadata.
 *
 * @author aritu
 *
 */
public class DocumentReviewInfoUpdaterListener implements EventListener {

    private static final Log log = LogFactory.getLog(
            DocumentReviewInfoUpdaterListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docEventCtx = (DocumentEventContext) event.getContext();

            // Check that we are handling the right event
            if (isEventHandled(event)) {
                // Check event context to see if we have to skip review info
                if (docEventCtx.hasProperty("default/"
                        + EloraGeneralConstants.CONTEXT_SKIP_REVIEW_INFO)) {
                    return;
                }

                // Get the document model from the event context
                DocumentModel doc = docEventCtx.getSourceDocument();

                // Check document's facet in order to know if it is storing
                // States Log.
                if (doc.hasFacet(EloraFacetConstants.FACET_STORE_REVIEW_INFO)) {

                    CoreSession session = docEventCtx.getCoreSession();

                    // Get the final state and it's status
                    String stateTo = (String) docEventCtx.getProperty(
                            LifeCycleConstants.TRANSTION_EVENT_OPTION_TO);

                    ReviewInfoHelper.setLastReviewInfoPropertiesByState(doc,
                            stateTo, session);

                    // Save the document
                    try {
                        EloraDocumentHelper.disableVersioningDocument(doc);
                    } catch (EloraException e) {
                        log.error(logInitMsg + e.getMessage());
                        return;
                    }
                    session.saveDocument(doc);
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
        return Arrays.asList(LifeCycleConstants.TRANSITION_EVENT);
    }
}
