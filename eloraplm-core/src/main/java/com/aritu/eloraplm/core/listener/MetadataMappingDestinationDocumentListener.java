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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.metadatamapping.api.MetadataMappingService;

/**
 * This class handles metadata mapping configuration for destination document
 * changes.
 *
 * @author aritu
 *
 */
public class MetadataMappingDestinationDocumentListener
        implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {

                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                MetadataMappingService metMapService = Framework.getService(
                        MetadataMappingService.class);
                if (metMapService.isDestinationDocument(doc)) {
                    metMapService.applyMappingToDestinationDocument(doc);
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
        return Arrays.asList(PdmEventNames.PDM_ABOUT_TO_CHECK_IN_EVENT,
                PdmEventNames.PDM_ABOUT_TO_OVERWRITE_EVENT);
    }
}
