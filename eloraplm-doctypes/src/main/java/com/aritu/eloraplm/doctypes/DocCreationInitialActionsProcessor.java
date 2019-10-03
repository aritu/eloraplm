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

package com.aritu.eloraplm.doctypes;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;

/**
 * @author aritu
 *
 */
public class DocCreationInitialActionsProcessor implements EventListener {

    protected static Log log = LogFactory.getLog(
            DocCreationInitialActionsProcessor.class);

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

                // Only execute for WC of certain facets
                // CAUTION! At the moment, we have no way to limit BasicDocument
                // to REAL basic documents, so CAD and BOM docs also have this
                // facet
                if (doc.isVersion() || doc.isProxy() || !(doc.hasFacet(
                        EloraFacetConstants.FACET_CAD_DOCUMENT)
                        || doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                        || doc.hasFacet(
                                EloraFacetConstants.FACET_ELORA_WORKSPACE)
                        || doc.hasFacet(
                                EloraFacetConstants.FACET_BASIC_DOCUMENT))) {
                    return;
                }

                String logInitMsg = "[handleEvent] ["
                        + event.getContext().getPrincipal().getName() + "] ";

                CoreSession session = docEventContext.getCoreSession();

                executeInitialActions(session, doc);

                session.save();

                log.trace(logInitMsg + "Initial actions executed for doc |"
                        + doc.getId() + "|");

            }
        }
    }

    private void executeInitialActions(CoreSession session, DocumentModel doc) {

        // Lock document
        if (!doc.isLocked()) {
            doc.setLock();
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
        return Arrays.asList(DocumentEventTypes.DOCUMENT_CREATED);
    }
}
