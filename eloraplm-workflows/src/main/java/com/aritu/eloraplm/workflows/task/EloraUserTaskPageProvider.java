/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package com.aritu.eloraplm.workflows.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.platform.task.providers.UserTaskPageProvider;
import org.nuxeo.runtime.api.Framework;

public class EloraUserTaskPageProvider extends UserTaskPageProvider {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraUserTaskPageProvider.class);

    /**
     * Override method in order to handle un-handled exceptions in
     * getTargetDocumentModel method (e.g. DocumentSecurityException).
     *
     */
    @Override
    protected void getAllTasks() {

        error = null;
        errorMessage = null;
        userTasks = new ArrayList<DashBoardItem>();
        CoreSession coreSession = getCoreSession();
        boolean filterTrashDocs = getFilterDocumentsInTrash();
        NuxeoPrincipal pal = (NuxeoPrincipal) coreSession.getPrincipal();

        String logInitMsg = "[getAllTasks] [" + pal.getName() + "] ";

        TaskService taskService = Framework.getService(TaskService.class);
        List<Task> tasks = taskService.getAllCurrentTaskInstances(coreSession,
                getSortInfos());
        if (tasks != null) {
            for (Task task : tasks) {
                if (task.hasEnded() || task.isCancelled()) {
                    continue;
                }
                DocumentModel doc = null;
                try {
                    doc = taskService.getTargetDocumentModel(task, coreSession);
                } catch (Exception e) {
                    log.error(logInitMsg
                            + "Exception getting document related to task |"
                            + task.getName() + "|: " + e.getClass().getName()
                            + ". " + e.getMessage());
                }
                if (doc != null) {
                    if (filterTrashDocs
                            && LifeCycleConstants.DELETED_STATE.equals(
                                    doc.getCurrentLifeCycleState())) {
                        continue;
                    } else {
                        userTasks.add(
                                new DashBoardItemImpl(task, doc, getLocale()));
                    }
                } else {
                    log.warn(String.format(
                            "User '%s' has a task of type '%s' on a "
                                    + "missing, deleted or unreadable document",
                            pal.getName(), task.getName()));
                }
            }
        }
    }

}
