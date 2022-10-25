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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 *
 * @author aritu
 *
 */
public class NodeTaskQuestionsInitializerListener implements EventListener {

    private static Log log = LogFactory.getLog(
            NodeTaskQuestionsInitializerListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                CoreSession session = docEventContext.getCoreSession();

                try {

                    DocumentModel taskDoc = docEventContext.getSourceDocument();
                    Task task = taskDoc.getAdapter(Task.class);

                    String wfInstanceDocUid = null;
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> taskVars = (ArrayList<Map<String, String>>) taskDoc.getPropertyValue(
                            "nt:task_variables");
                    for (Map<String, String> map : taskVars) {
                        if (map.get("key").equals("routeInstanceDocId")) {
                            wfInstanceDocUid = map.get("value");
                        }
                    }

                    if (wfInstanceDocUid != null) {

                        DocumentModel wfInstance = session.getDocument(
                                new IdRef(wfInstanceDocUid));
                        GraphRoute route = wfInstance.getAdapter(
                                GraphRoute.class);

                        if (wfInstance.hasFacet(
                                EloraFacetConstants.FACET_DEFINED_BY_WORKFLOW_FORMS)) {

                            log.trace(logInitMsg
                                    + "Initializing node task questions for WF instance |"
                                    + wfInstanceDocUid + "|...");

                            WorkflowFormsService wfs = Framework.getService(
                                    WorkflowFormsService.class);

                            wfs.initializeNodeTaskAnswersForDocument(session,
                                    route, task);

                            log.trace(logInitMsg
                                    + "Questions initialized for WF instance |"
                                    + wfInstanceDocUid + "|.");

                        }
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
        return Arrays.asList(WorkflowEventNames.WF_AFTER_WORKFLOW_TASK_CREATED);
    }

}
