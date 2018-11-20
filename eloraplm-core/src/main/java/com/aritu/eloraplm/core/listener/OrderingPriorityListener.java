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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.core.util.EloraOrderingPriorityHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class OrderingPriorityListener implements EventListener {

    /*private static final Log log = LogFactory.getLog(
            OrderingPriorityListener.class);*/

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.core.event.EventListener#handleEvent(org.nuxeo.ecm.core.event.Event)
     */
    @Override
    public void handleEvent(Event event) {

        /*String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";*/
        // log.trace(logInitMsg + "--- ENTER --- ");

        // Check that we are handling the right event
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            // event is not tied to a document, we should not be here
            // log.trace(logInitMsg + "--- return [invalid event context]---");
            return;
        }

        String eventId = event.getName();
        // log.trace(logInitMsg + "eventId = |" + eventId + "|");

        // Take into account only document creation
        if (!eventId.equals(DOCUMENT_CREATED)) {
            // log.trace(logInitMsg + "--- return [invalid event name]---");
            return;
        }

        // get the document model from the event context
        DocumentModel doc = docCtx.getSourceDocument();

        // if the document has ELORA_OBJECT schema
        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {

            // set the ordering priority
            int orderingPriority = EloraOrderingPriorityHelper.getOrderingPriority(
                    doc.getType());

            doc.setPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_ORDERING_PRIORITY,
                    orderingPriority);
        }

        // log.trace(logInitMsg + "--- EXIT ---");
    }

}
