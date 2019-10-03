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

package com.aritu.eloraplm.bom.lists.listener;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import com.aritu.eloraplm.bom.lists.BomListHelper;
import com.aritu.eloraplm.constants.EloraFacetConstants;

/**
 * @author aritu
 *
 */
public class BomListsRemovalListener implements EventListener {

    protected static Log log = LogFactory.getLog(BomListsRemovalListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                // TODO: Hau hobeto ulertu.
                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                // Execute only if the document is a BomDocument
                if (!doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                    return;
                }

                CoreSession session = docEventContext.getCoreSession();

                // Get all the child BOM lists, and remove them
                List<String> allBomListUids = BomListHelper.getAllBomListUidsForDocument(
                        doc, false, session);

                if (!allBomListUids.isEmpty()) {
                    for (String bomListUid : allBomListUids) {
                        try {
                            session.removeDocument(new IdRef(bomListUid));
                        } catch (Exception e) {
                            log.error(
                                    "Error while removing BOM document's BOM lists.");
                        }
                    }
                }
                session.save();

            }
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
        return Arrays.asList(DocumentEventTypes.ABOUT_TO_REMOVE);
    }
}