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
package com.aritu.eloraplm.workflows.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 *
 * @author aritu
 *
 */
@Name("eloraTaskActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class EloraTaskActionsBean extends DocumentContextBoundActionBean {

    private static final long serialVersionUID = 1L;

    /**
     * @since 7.2
     */
    public static final String ELORA_TASKS_CACHE_RESET = "eloraTasksCacheReset";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected transient TaskService taskService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected List<Task> tasks;

    protected List<DashBoardItem> items;

    protected String comment;

    @Factory(value = "currentWfSingleTasks", scope = ScopeType.EVENT)
    public List<Task> getCurrentDocumentTasks() {
        if (tasks == null) {
            tasks = new ArrayList<Task>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                DocumentModel currentNonProxyDoc = getNonProxyDocument(
                        currentDocument);
                NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
                List<String> actors = new ArrayList<String>();
                actors.addAll(TaskActorsHelper.getTaskActors(principal));
                tasks = taskService.getTaskInstances(currentNonProxyDoc, actors,
                        true, documentManager);
            }
        }
        return tasks;
    }

    @Factory(value = "currentWfDashBoardItems", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItems() {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                DocumentModel currentNonProxyDoc = getNonProxyDocument(
                        currentDocument);

                for (Task task : getCurrentDocumentTasks()) {
                    DashBoardItem item = new DashBoardItemImpl(task,
                            currentNonProxyDoc, localeSelector.getLocale());
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Factory(value = "currentWfDashBoardItemsExceptPublishingTasks", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItemsExceptPublishingTasks() {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                DocumentModel currentNonProxyDoc = getNonProxyDocument(
                        currentDocument);
                for (Task task : getCurrentDocumentTasks()) {
                    String taskType = task.getVariable(
                            Task.TaskVariableName.taskType.name());
                    if (!"publish_moderate".equals(taskType)) {
                        DashBoardItem item = new DashBoardItemImpl(task,
                                currentNonProxyDoc, localeSelector.getLocale());
                        items.add(item);
                    }
                }
            }
        }
        return items;
    }

    private DocumentModel getNonProxyDocument(DocumentModel doc) {
        if (doc.isProxy()) {
            return documentManager.getSourceDocument(doc.getRef());
        } else {
            return doc;
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void acceptTask(Task task) {
        acceptTask(task, getComment());
        setComment(null);
    }

    public void acceptTask(Task task, String comment) {
        String seamEventName = taskService.acceptTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    public void rejectTask(Task task) {
        String userComment = getComment();
        if (userComment != null && !"".equals(userComment)) {
            rejectTask(task, userComment);
            setComment(null);
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.review.task.enterComment"));
        }

    }

    public void rejectTask(Task task, String comment) {
        String seamEventName = taskService.rejectTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    @Override
    public void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCache();
    }

    @Observer(value = { TaskEventNames.WORKFLOW_ENDED,
            TaskEventNames.WORKFLOW_NEW_STARTED,
            TaskEventNames.WORKFLOW_TASK_START,
            TaskEventNames.WORKFLOW_TASK_STOP,
            TaskEventNames.WORKFLOW_TASK_REJECTED,
            TaskEventNames.WORKFLOW_TASK_REASSIGNED,
            TaskEventNames.WORKFLOW_TASK_DELEGATED,
            TaskEventNames.WORKFLOW_TASK_COMPLETED,
            TaskEventNames.WORKFLOW_TASK_REMOVED,
            TaskEventNames.WORK_ITEMS_LIST_LOADED,
            TaskEventNames.WORKFLOW_TASKS_COMPUTED,
            TaskEventNames.WORKFLOW_ABANDONED, TaskEventNames.WORKFLOW_CANCELED,
            EventNames.DOMAIN_SELECTION_CHANGED, "documentPublicationRejected",
            "documentPublished" }, create = false)
    @BypassInterceptors
    public void resetCache() {
        tasks = null;
        items = null;
        Events.instance().raiseEvent(ELORA_TASKS_CACHE_RESET);
    }

    /**
     * @since 7.2
     */
    @Observer({ ELORA_TASKS_CACHE_RESET })
    public void resetTasksCache() {
        contentViewActions.refreshOnSeamEvent(ELORA_TASKS_CACHE_RESET);
        contentViewActions.resetPageProviderOnSeamEvent(
                ELORA_TASKS_CACHE_RESET);
    }

}
