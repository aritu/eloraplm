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

package com.aritu.eloraplm.viewer.listener;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.constants.ViewerEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.OverwriteOriginalViewerException;
import com.aritu.eloraplm.viewer.api.ViewerFileService;

/**
 * Listens to all the events that trigger viewer file creation, and tries to
 * create the viewer file and update document's property.
 *
 * @author aritu
 *
 */
public class ViewerFileCreationListener implements EventListener {

    private static Log log = LogFactory.getLog(
            ViewerFileCreationListener.class);

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

                // Templates do not have viewer files
                if (EloraDocumentHelper.isTemplate(doc)) {
                    return;
                }

                // Check event context to see if viewer file creation should be
                // skipped
                if (docEventContext.hasProperty(
                        EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION)) {
                    boolean skipViewerFileCreation = (boolean) docEventContext.getProperty(
                            EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION);
                    log.trace(logInitMsg + "skipViewerFileCreation = |"
                            + skipViewerFileCreation + "|");
                    if (skipViewerFileCreation) {
                        return;
                    }
                }

                /* RelatedViewerUpdateOnOverwriteListener fires the event in unrestricted mode, so the principal is system
                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }
                */

                if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {

                    try {
                        String action = "";
                        switch (event.getName()) {
                        case PdmEventNames.PDM_CHECKED_IN_EVENT:
                            action = ViewerConstants.ACTION_CHECK_IN;
                            break;
                        case PdmEventNames.PDM_ABOUT_TO_OVERWRITE_EVENT:
                            action = ViewerConstants.ACTION_OVERWRITE;
                            break;
                        case PdmEventNames.PDM_DEMOTED_EVENT:
                            action = ViewerConstants.ACTION_DEMOTE;
                            break;
                        case PdmEventNames.PDM_PROMOTED_EVENT:
                            action = ViewerConstants.ACTION_PROMOTE;
                            break;
                        case PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT:
                            action = ViewerConstants.ACTION_OVERWRITE_AV;
                            break;
                        }

                        ViewerFileService vfs = Framework.getService(
                                ViewerFileService.class);

                        vfs.createViewer(doc, action);

                        // Launch event indicating viewer file has been created
                        Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
                        ctxProperties.put(
                                EloraGeneralConstants.CONTEXT_KEY_ACTION,
                                action);
                        EloraEventHelper.fireEvent(
                                ViewerEventNames.VIEWER_FILE_CREATED_EVENT, doc,
                                ctxProperties);

                    } catch (OverwriteOriginalViewerException e) {
                        log.error(logInitMsg + e.getMessage(), e);
                        FacesMessages facesMessages = (FacesMessages) Contexts.getConversationContext().get(
                                FacesMessages.class);
                        facesMessages.add(StatusMessage.Severity.WARN,
                                Messages.instance().get(
                                        "eloraplm.message.error.viewer.overwriteOriginalViewerException"));
                    } catch (Exception e) {
                        log.error(logInitMsg + e.getMessage(), e);
                        FacesMessages facesMessages = (FacesMessages) Contexts.getConversationContext().get(
                                FacesMessages.class);
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                Messages.instance().get(
                                        "eloraplm.message.error.viewer.createViewer"));
                    }
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
        return Arrays.asList(PdmEventNames.PDM_CHECKED_IN_EVENT,
                PdmEventNames.PDM_ABOUT_TO_OVERWRITE_EVENT,
                PdmEventNames.PDM_DEMOTED_EVENT,
                PdmEventNames.PDM_PROMOTED_EVENT,
                PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT);
    }
}
