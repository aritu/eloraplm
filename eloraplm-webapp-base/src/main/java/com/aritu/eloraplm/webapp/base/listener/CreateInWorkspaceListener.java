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
package com.aritu.eloraplm.webapp.base.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CreateInWorkspaceListener implements EventListener {

    protected static Log log = LogFactory.getLog(
            CreateInWorkspaceListener.class);

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

                if (!isDocHandled(doc)) {
                    return;
                }

                try {
                    moveToStructureAndCreateProxy(doc);
                } catch (EloraException e) {
                    return;
                }

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
        return Arrays.asList(DocumentEventTypes.DOCUMENT_CREATED);
    }

    private boolean isDocHandled(DocumentModel doc) {

        return !doc.isProxy() && !doc.isVersion()
                && (doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                        || doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                        || doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT))
                && EloraStructureHelper.isDocUnderWorkspaceRoot(doc);
    }

    private void moveToStructureAndCreateProxy(DocumentModel doc)
            throws EloraException {
        CoreSession session = doc.getCoreSession();

        PathRef targetDocPath = obtainTargetDocPath(doc, session);
        if (targetDocPath != null && session.exists(targetDocPath)) {
            DocumentRef initialParentRef = doc.getParentRef();
            session.move(doc.getRef(), targetDocPath, doc.getName());
            session.createProxy(doc.getRef(), initialParentRef);
        }
    }

    private PathRef obtainTargetDocPath(DocumentModel doc, CoreSession session)
            throws EloraException {
        String docPath = null;

        DocumentModel structureRoot;
        structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(doc,
                session);

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            docPath = EloraStructureHelper.getBomPathByType(
                    structureRoot.getRef(), doc.getType(), session);
        } else {
            docPath = EloraStructureHelper.getCadPathByType(
                    structureRoot.getRef(), doc.getType(), session);
        }

        return new PathRef(docPath);
    }

}