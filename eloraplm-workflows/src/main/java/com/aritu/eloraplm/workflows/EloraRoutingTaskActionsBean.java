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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.web.RoutingTaskActionsBean;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 *
 * Copy of RoutingTaskActionsBean to control errors
 *
 * @author aritu
 *
 */
@Scope(CONVERSATION)
@Name("eloraRoutingTaskActions")
public class EloraRoutingTaskActionsBean extends RoutingTaskActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraRoutingTaskActionsBean.class);

    @Override
    public String endTask(Task task) {

        // collect form data
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Serializable> formVariables = getFormVariables(task);
        if (getFormVariables(task) != null) {
            data.put("WorkflowVariables", getFormVariables(task));
            data.put("NodeVariables", getFormVariables(task));
            // if there is a comment on the submitted form, pass it to be
            // logged
            // by audit
            if (formVariables.containsKey(GraphNode.NODE_VARIABLE_COMMENT)) {
                data.put(GraphNode.NODE_VARIABLE_COMMENT,
                        formVariables.get(GraphNode.NODE_VARIABLE_COMMENT));
            }
        }
        // add the button name that was clicked
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentRoutingService routing = Framework.getLocalService(
                    DocumentRoutingService.class);
            routing.endTask(documentManager, task, data, button);
            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("workflow.feedback.info.taskEnded"));
        } catch (DocumentRouteException e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("workflow.feedback.error.taskEnded"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.workflow.task.end"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
        clear(task.getId());

        if (navigationContext.getCurrentDocument() != null
                && documentManager.hasPermission(
                        navigationContext.getCurrentDocument().getRef(),
                        SecurityConstants.READ)) {
            return null;
        }

        // if the user only had temporary permissions on the current doc given
        // by the workflow
        navigationContext.setCurrentDocument(null);
        return navigationContext.goHome();
    }

    private void clear(String taskId) {
        button = null;
        if (tasksInfoCache.containsKey(taskId)) {
            tasksInfoCache.remove(taskId);
        }
    }

}
