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
package com.aritu.eloraplm.workflows;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement.ElementLifeCycleState;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.event.SelectEvent;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * @author aritu
 *
 */
@Name("workflowActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class WorkflowActionsBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkflowActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private String storedDocId;

    private List<DocumentModel> closedRelatedRoutes;

    private DocumentModel selectedWorkflow;

    private boolean isTasksPaneExpanded;

    public DocumentModel getSelectedWorkflow() {
        if (selectedWorkflow == null) {
            String docId = storedDocId != null ? storedDocId
                    : getCurrentDocument().getId();
            if (hasClosedRelatedRoutes(docId)) {
                selectedWorkflow = findClosedRelatedRoutes(docId).get(0);
            }
        }
        return selectedWorkflow;
    }

    public void setSelectedWorkflow(DocumentModel selectedWorkflow) {
        this.selectedWorkflow = selectedWorkflow;
    }

    public boolean getIsTasksPaneExpanded() {
        return isTasksPaneExpanded;
    }

    public void toggleIsTasksPaneExpanded() {
        isTasksPaneExpanded = !isTasksPaneExpanded;
    }

    public String getTaskStatus(String taskDocId) {

        String status = "";
        if (taskDocId != null) {
            String query = EloraQueryFactory.getTaskStatusByTaskDocId(
                    taskDocId);

            List<String> results = EloraQueryHelper.executeQueryAndGetResultStringList(
                    NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                            + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_STATUS,
                    query, documentManager);

            if (!results.isEmpty()) {
                if (results.size() > 1) {
                    log.error(
                            "More than one route node found while getting task status for taskDocId |"
                                    + taskDocId
                                    + "|. Status could not be obtained.");
                }

                status = results.get(0);
            }
        }

        return status;
    }

    public List<DocumentModel> findClosedRelatedRoutes() {
        DocumentModel currentDoc = getCurrentDocument();
        String currentDocId = currentDoc.getId();
        if (currentDoc.isProxy()) {
            currentDocId = currentDoc.getSourceId();
        }
        return findClosedRelatedRoutes(currentDocId);
    }

    public List<DocumentModel> findClosedRelatedRoutes(String documentId) {
        if (storedDocId == null || documentId != storedDocId) {
            List<DocumentModel> docs = new ArrayList<DocumentModel>();

            if ("".equals(documentId)
                    || !documentManager.exists(new IdRef(documentId))) {
                return docs;
            }
            List<DocumentRouteElement.ElementLifeCycleState> closedStates = new ArrayList<DocumentRouteElement.ElementLifeCycleState>();
            closedStates.add(ElementLifeCycleState.canceled);
            closedStates.add(ElementLifeCycleState.done);

            List<DocumentRoute> documentRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                    documentManager, documentId, closedStates);
            for (DocumentRoute documentRoute : documentRoutes) {
                docs.add(0, documentRoute.getDocument());
            }
            closedRelatedRoutes = docs;

        }
        return closedRelatedRoutes;

    }

    public boolean hasClosedRelatedRoutes() {
        DocumentModel currentDoc = getCurrentDocument();
        String currentDocId = currentDoc.getId();
        if (currentDoc.isProxy()) {
            currentDocId = currentDoc.getSourceId();
        }
        return hasClosedRelatedRoutes(currentDocId);
    }

    public boolean hasClosedRelatedRoutes(String documentId) {
        return !findClosedRelatedRoutes(documentId).isEmpty();
    }

    public DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }

    public void onRowSelect(SelectEvent event) {
        selectedWorkflow = (DocumentModel) event.getObject();
    }

    public String reassignTaskToMe(String taskId) {
        try {
            String comment = "";
            String actor = documentManager.getPrincipal().getName();

            new TaskReassigner(documentManager, taskId, actor,
                    comment).runUnrestricted();

            Events.instance().raiseEvent(
                    TaskEventNames.WORKFLOW_TASK_REASSIGNED);
        } catch (DocumentRouteException e) {
            log.error(e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("workflow.feedback.error.taskEnded"));
        }
        return null;
    }

    public boolean hasTaskMultipleActors(List<String> actors) {
        if (actors != null && actors.size() > 1) {
            return true;
        } else {
            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            for (String actor : actors) {
                if (actor.startsWith(NuxeoGroup.PREFIX)) {
                    actor = actor.replace(NuxeoGroup.PREFIX, "");
                }

                NuxeoGroup g = userManager.getGroup(actor);
                if (g != null) {
                    if (g.getMemberUsers().size() > 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public class TaskReassigner extends UnrestrictedSessionRunner {

        String taskId;

        List<String> actors;

        String comment;

        DocumentRoutingService drs;

        public TaskReassigner(CoreSession session, String taskId, String actor,
                String comment) {
            super(session);
            actors = new ArrayList<String>();
            actors.add(actor);
            this.taskId = taskId;
            this.comment = comment;

            drs = Framework.getLocalService(DocumentRoutingService.class);
        }

        @Override
        public void run() {
            DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
            Task task = taskDoc.getAdapter(Task.class);
            if (task == null) {
                throw new DocumentRouteException("Invalid taskId: " + taskId);
            }
            if (!task.isOpened()) {
                throw new DocumentRouteException("Task  " + taskId
                        + " is not opened, can not reassign it");
            }
            String routeId = task.getProcessId();
            if (routeId != null) {
                DocumentModel routeDoc = session.getDocument(
                        new IdRef(routeId));
                GraphRoute routeInstance = routeDoc.getAdapter(
                        GraphRoute.class);
                if (routeInstance == null) {
                    throw new DocumentRouteException("Invalid routeInstanceId: "
                            + routeId + " referenced by the task " + taskId);
                }
                GraphNode node = routeInstance.getNode(task.getType());
                if (node == null) {
                    throw new DocumentRouteException("Invalid node " + routeId
                            + " referenced by the task " + taskId);
                }
                if (!node.allowTaskReassignment()
                        && !isReassigningToSelfInsideOldActors(actors,
                                task.getActors())) {

                    throw new DocumentRouteException("Task " + taskId
                            + " can not be reassigned. Node " + node.getId()
                            + " doesn't allow reassignment.");
                }
                DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                // remove permissions on the document following the
                // workflow for the current assignees
                drs.removePermissionFromTaskAssignees(session, docs, task);
                Framework.getLocalService(TaskService.class).reassignTask(
                        session, taskId, actors, comment);
                // refresh task
                task.getDocument().refresh();
                // grant permission to the new assignees
                drs.grantPermissionToTaskAssignees(session,
                        node.getTaskAssigneesPermission(), docs, task);

                // Audit task reassignment
                Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
                eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                        DocumentRoutingConstants.ROUTING_CATEGORY);
                eventProperties.put("taskName", task.getName());
                eventProperties.put("actors", (Serializable) actors);
                eventProperties.put("modelId", routeInstance.getModelId());
                eventProperties.put("modelName", routeInstance.getModelName());
                eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR,
                        routeInstance.getInitiator());
                eventProperties.put(RoutingAuditHelper.TASK_ACTOR,
                        ((NuxeoPrincipal) session.getPrincipal()).getActingUser());
                eventProperties.put("comment", comment);
                // compute duration since workflow started
                long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(
                        task.getProcessId());
                if (timeSinceWfStarted >= 0) {
                    eventProperties.put(
                            RoutingAuditHelper.TIME_SINCE_WF_STARTED,
                            timeSinceWfStarted);
                }
                // compute duration since task started
                long timeSinceTaskStarted = RoutingAuditHelper.computeDurationSinceTaskStarted(
                        task.getId());
                if (timeSinceWfStarted >= 0) {
                    eventProperties.put(
                            RoutingAuditHelper.TIME_SINCE_TASK_STARTED,
                            timeSinceTaskStarted);
                }
                DocumentEventContext envContext = new DocumentEventContext(
                        session, session.getPrincipal(), task.getDocument());
                envContext.setProperties(eventProperties);
                EventProducer eventProducer = Framework.getLocalService(
                        EventProducer.class);
                eventProducer.fireEvent(envContext.newEvent(
                        DocumentRoutingConstants.Events.afterWorkflowTaskReassigned.name()));
            }

        }

        /**
         * (For now) we only expect one new actor (must be current principal),
         * and to be inside the group of old actors, else return false
         *
         * @param currentPrincipal
         * @param newActors
         * @param oldActors
         * @param oldDelegateActors
         * @return
         */
        private boolean isReassigningToSelfInsideOldActors(
                List<String> newActors, List<String> oldActors) {

            if (newActors == null || newActors.size() != 1) {
                return false;
            }

            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            NuxeoPrincipal newActor = userManager.getPrincipal(
                    newActors.get(0));

            if (newActor != null) {

                for (String oldActor : oldActors) {
                    if (oldActor.startsWith(NuxeoGroup.PREFIX)) {
                        oldActor = oldActor.replace(NuxeoGroup.PREFIX, "");
                    }
                    NuxeoGroup g = userManager.getGroup(oldActor);
                    if (g != null) {
                        if (newActor.isMemberOf(oldActor)) {
                            return true;
                        }
                    } else {
                        if (oldActor.startsWith(NuxeoPrincipal.PREFIX)) {
                            oldActor = oldActor.replace(NuxeoPrincipal.PREFIX,
                                    "");
                        }
                        if (newActor.getName().equals(oldActor)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        isTasksPaneExpanded = false;
    }

}
