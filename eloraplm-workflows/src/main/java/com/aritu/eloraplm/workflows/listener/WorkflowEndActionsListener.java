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

import java.io.Serializable;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.transaction.TransactionHelper;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.constants.WorkflowMetadataConstants;

/**
 *
 * @author aritu
 *
 */
public class WorkflowEndActionsListener implements EventListener {

    private static Log log = LogFactory.getLog(
            WorkflowEndActionsListener.class);

    @Override
    public void handleEvent(Event event) {

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                CoreSession session = docEventContext.getCoreSession();

                try {

                    GraphRoute route = (GraphRoute) eventContext.getProperty(
                            "documentElementEventContextKey");
                    if (route != null) {
                        if (!route.isDone()) {
                            // This should not happen
                            return;
                        }

                        DocumentModelList attachedDocs = route.getAttachedDocumentModels();
                        for (DocumentModel doc : attachedDocs) {
                            if (doc.hasFacet(
                                    EloraFacetConstants.FACET_HAS_WORKFLOW_FORMS)) {
                                updateWorkflowInfo(session, doc, route);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("[handleEvent] Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                            e);

                    // Tell the event the exception must be bubbled
                    event.markBubbleException();

                    // Rollback the transaction
                    TransactionHelper.setTransactionRollbackOnly();
                    event.markRollBack();

                    // Throw NuxeoException to be handled in UI layer
                    throw new NuxeoException(
                            "Error updating workflow info. Error message:"
                                    + e.getClass().getName() + "."
                                    + e.getMessage());
                }

            }

        }

    }

    private void updateWorkflowInfo(CoreSession session, DocumentModel doc,
            GraphRoute route) {
        String workflowId = route.getModelName();
        String workflowInstanceId = route.getDocument().getId();

        GraphNode endNode = findEndNode(route);
        if (endNode != null) {
            String endNodeId = endNode.getId();
            String endNodeLabel = endNode.getDocument().getTitle();
            GregorianCalendar endDate = (GregorianCalendar) endNode.getDocument().getPropertyValue(
                    "rnode:endDate");

            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> wfInfoInstances = (List<Map<String, Serializable>>) doc.getPropertyValue(
                    WorkflowMetadataConstants.WFINFO_INSTANCES);
            Map<String, Serializable> instanceInfo = new HashMap<String, Serializable>();
            instanceInfo.put(
                    WorkflowMetadataConstants.WFINFO_INSTANCES_WORKFLOW_ID,
                    workflowId);
            instanceInfo.put(
                    WorkflowMetadataConstants.WFINFO_INSTANCES_WORKFLOW_INSTANCE_ID,
                    workflowInstanceId);
            instanceInfo.put(
                    WorkflowMetadataConstants.WFINFO_INSTANCES_END_NODE,
                    endNodeId);
            instanceInfo.put(
                    WorkflowMetadataConstants.WFINFO_INSTANCES_END_NODE_LABEL,
                    endNodeLabel);
            instanceInfo.put(
                    WorkflowMetadataConstants.WFINFO_INSTANCES_END_DATE,
                    endDate);
            wfInfoInstances.add(instanceInfo);
            doc.setPropertyValue(WorkflowMetadataConstants.WFINFO_INSTANCES,
                    (Serializable) wfInfoInstances);
            session.saveDocument(doc);
            session.save();
        }
    }

    private GraphNode findEndNode(GraphRoute route) {
        for (GraphNode node : route.getNodes()) {
            if (node.isStop()) {
                if (node.getDocument().getPropertyValue(
                        GraphNode.PROP_NODE_END_DATE) != null) {
                    return node;
                }
            }
        }
        return null;
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
        return Arrays.asList(WorkflowEventNames.WF_AFTER_WORKFLOW_FINISH);
    }

}
