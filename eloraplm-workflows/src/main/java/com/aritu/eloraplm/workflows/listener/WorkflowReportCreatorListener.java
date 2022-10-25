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
package com.aritu.eloraplm.workflows.listener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
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

import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.templating.api.TemplatingService;

/**
 *
 * @author aritu
 *
 */
public class WorkflowReportCreatorListener implements EventListener {

    private static Log log = LogFactory.getLog(
            WorkflowReportCreatorListener.class);

    @Override
    public void handleEvent(Event event) {
        // String logInitMsg = "[handleEvent] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                CoreSession session = docEventContext.getCoreSession();

                try {

                    Task task = (Task) eventContext.getProperty("taskInstance");
                    String wfInstDocUid = task.getVariable(
                            "routeInstanceDocId");

                    if (wfInstDocUid != null) {
                        DocumentModel wfInstance = session.getDocument(
                                new IdRef(wfInstDocUid));
                        GraphRoute route = wfInstance.getAdapter(
                                GraphRoute.class);

                        if (!route.isDone()) {
                            // We only want to execute it after the workflow is
                            // finished.
                            // We should use afterRouteFinish or
                            // afterWorkflowFinish
                            // events, but those are executed before the last
                            // workflowTaskCompleted event, so the last
                            // answers data is not saved.
                            return;
                        }

                        TemplatingService ts = Framework.getService(
                                TemplatingService.class);
                        String templateId = route.getModelName();
                        String wfTitle = EloraMessageHelper.getTranslatedEnMessage(
                                route.getTitle());
                        if (ts.existsTemplate(templateId)) {

                            Map<String, Object> params = new HashMap<String, Object>();
                            params.put("title",
                                    "Workflow Report - " + wfTitle + " at "
                                            + new SimpleDateFormat(
                                                    "yyyy-MM-dd HH:mm").format(
                                                            new Date()));
                            params.put("templateId", templateId);

                            AutomationService automationService = Framework.getService(
                                    AutomationService.class);

                            OperationContext ctx = new OperationContext(
                                    session);
                            ctx.setInput(route.getAttachedDocumentModels());
                            automationService.run(ctx,
                                    "Elora.Plm.RenderPdfWithTemplateAndAddAsChildren",
                                    params);

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
        return Arrays.asList(WorkflowEventNames.WF_WORKFLOW_TASK_COMPLETED);
    }

}
