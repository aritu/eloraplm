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
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;

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
                CoreSession session = docEventContext.getCoreSession();

                // The first time a user enters in Nuxeo, automatically a new
                // workspace is created under UserWorkspacesRoot.
                // Do not execute initial actions in this new workspace.
                if (doc.getParentRef() != null) {
                    DocumentModel parentDoc = session.getDocument(
                            doc.getParentRef());
                    if (parentDoc != null && parentDoc.getType().equals(
                            NuxeoDoctypeConstants.USER_WORKSPACE_ROOT)) {
                        return;
                    }
                }

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

                executeInitialActions(docEventContext, session, doc);

                session.save();

                log.trace(logInitMsg + "Initial actions executed for doc |"
                        + doc.getId() + "|");
            }
        }
    }

    protected void executeInitialActions(DocumentEventContext docEventCtx,
            CoreSession session, DocumentModel doc) {

        boolean skipLock = false;

        if (docEventCtx.hasProperty(EloraGeneralConstants.CONTEXT_SKIP_LOCK)) {
            skipLock = (boolean) docEventCtx.getProperty(
                    EloraGeneralConstants.CONTEXT_SKIP_LOCK);
        }

        DocCreationHelper.executeInitialActions(session, doc, skipLock);
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
