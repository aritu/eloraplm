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
package com.aritu.eloraplm.workflows.forms.listener;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 *
 * @author aritu
 *
 */
public class WorkflowQuestionsInitializerListener implements EventListener {

    private static Log log = LogFactory.getLog(
            WorkflowQuestionsInitializerListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                CoreSession session = docEventContext.getCoreSession();

                try {

                    DocumentModel workflow = docEventContext.getSourceDocument();

                    if (workflow.hasFacet(
                            EloraFacetConstants.FACET_DEFINED_BY_WORKFLOW_FORMS)) {

                        GraphRoute route = workflow.getAdapter(
                                GraphRoute.class);
                        DocumentModelList targetDocs = route.getAttachedDocumentModels();
                        if (targetDocs.size() != 1) {
                            throw new EloraException(
                                    "No target docs or more than one target doc.");
                        }
                        DocumentModel targetDoc = targetDocs.get(0);

                        log.trace(logInitMsg
                                + "Initializing workflow questions for WF |"
                                + route.getModelName() + "| and doc |"
                                + targetDoc.getId() + "|...");

                        WorkflowFormsService wfs = Framework.getService(
                                WorkflowFormsService.class);
                        wfs.initializeWorkflowQuestionsForDocument(session,
                                route, targetDoc);

                        log.trace(logInitMsg + "Workflow questions for WF |"
                                + route.getModelName() + "| and doc |"
                                + targetDoc.getId() + "| intitialized.");
                    }

                } catch (Exception e) {
                    log.error("[handleEvent] Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                            e);
                }

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
        return Arrays.asList(WorkflowEventNames.WF_BEFORE_ROUTE_START);
    }

}
