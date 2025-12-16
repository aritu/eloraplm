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

package com.aritu.eloraplm.securitypolicy.listener;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.securitypolicy.api.SecurityConfigService;

/**
 * Listens to the event triggered after a document is created and sets the
 * default security level configured in the extension point
 *
 * @author aritu
 *
 */
public class DocumentDefaultSecurityLevelListener implements EventListener {

    private static Log log = LogFactory.getLog(
            DocumentDefaultSecurityLevelListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                String logInitMsg = "[handleEvent] ["
                        + event.getContext().getPrincipal().getName() + "] ";
                log.trace(logInitMsg + "--- ENTER --- ");

                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                try {

                    // Check if it comes from a template
                    if (doc.hasFacet(EloraFacetConstants.FACET_TEMPLATABLE)) {
                        String sourceDocUid = (String) doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_TEMPL_SOURCE_DOC_UID);
                        if (sourceDocUid != null) {
                            // We don't modify the security level (it inherits
                            // from the template)
                            return;
                        }
                    }

                    SecurityConfigService scs = Framework.getService(
                            SecurityConfigService.class);

                    scs.configureDefaultSecurityLevel(doc);

                } catch (Exception e) {
                    log.error(logInitMsg + e.getMessage(), e);
                    FacesMessages facesMessages = (FacesMessages) Contexts.getConversationContext().get(
                            FacesMessages.class);
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            Messages.instance().get(
                                    "eloraplm.message.error.viewer.createViewer"));
                }

                log.trace(logInitMsg + "--- EXIT --- ");
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
}
