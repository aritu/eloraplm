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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
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
public class NodeTaskQuestionsWriterListener implements EventListener {

    private static Log log = LogFactory.getLog(
            NodeTaskQuestionsWriterListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                CoreSession session = docEventContext.getCoreSession();

                try {

                    DocumentModel targetDoc = docEventContext.getSourceDocument();

                    String action = (String) eventContext.getProperty(
                            "workflowTaskCompletionAction");
                    Task task = (Task) eventContext.getProperty("taskInstance");
                    String wfInstDocUid = task.getVariable(
                            "routeInstanceDocId");

                    if (wfInstDocUid != null) {
                        DocumentModel wfInstance = session.getDocument(
                                new IdRef(wfInstDocUid));

                        if (wfInstance.hasFacet(
                                EloraFacetConstants.FACET_DEFINED_BY_WORKFLOW_FORMS)) {

                            String nodeId = task.getVariable("nodeId");

                            log.trace(logInitMsg
                                    + "Writing node task questions answers for doc |"
                                    + targetDoc.getId() + "| and node |"
                                    + nodeId + "|...");

                            GraphRoute route = wfInstance.getAdapter(
                                    GraphRoute.class);
                            GraphNode node = route.getNode(nodeId);

                            WorkflowFormsService wfs = Framework.getService(
                                    WorkflowFormsService.class);

                            wfs.writeNodeTaskAnswersForDocument(session, route,
                                    node, task, targetDoc, action);

                            log.trace(logInitMsg + "Answers for doc |"
                                    + targetDoc.getId() + "| and node |"
                                    + nodeId + "| written.");
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

    private boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getHandledEventsName() {
        return Arrays.asList(WorkflowEventNames.WF_WORKFLOW_TASK_COMPLETED);
    }

}
