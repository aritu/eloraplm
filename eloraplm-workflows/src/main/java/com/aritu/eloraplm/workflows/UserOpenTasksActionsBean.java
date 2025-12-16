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

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.core.DocumentContextBoundActions;

/**
 *
 * @author aritu
 *
 */
@Name("userOpenTasksActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class UserOpenTasksActionsBean extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    private Map<String, String> tasks;

    private Map<String, String> workflows;

    public Map<String, String> getTasks(String selectedWorkflow) {

        tasks = loadTasksFromWorkflow(selectedWorkflow);

        return tasks;
    }

    public Map<String, String> getWorkflows() {

        if (workflows == null) {
            loadWorkflows();
        }

        return workflows;
    }

    private Map<String, DocumentModel> loadTasks() {

        String query = "Select * from TaskDoc where ecm:mixinType IN ('RoutingTask') AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState = 'opened'";
        DocumentModelList docs = documentManager.query(query);
        Map<String, DocumentModel> taskList = new HashMap<String, DocumentModel>();

        for (DocumentModel doc : docs) {
            if (!taskList.containsKey(doc.getSourceId())) {
                taskList.put(doc.getSourceId(), doc);
            }
        }

        return taskList;
    }

    private Map<String, String> loadWorkflows() {
        Map<String, DocumentModel> taskList = loadTasks();
        workflows = new HashMap<String, String>();
        for (Map.Entry<String, DocumentModel> task : taskList.entrySet()) {
            DocumentModel val = task.getValue();

            String workflowId = (String) val.getPropertyValue("nt:processId");
            if (workflowId != null) {
                DocumentModel workflowDoc = documentManager.getDocument(
                        new IdRef(workflowId));
                if (workflowDoc != null) {
                    if (!workflows.containsKey(workflowDoc.getSourceId())) {
                        workflows.put(workflowDoc.getSourceId(),
                                workflowDoc.getTitle());
                    }
                }
            }
        }

        return workflows;
    }

    private Map<String, String> loadTasksFromWorkflow(String selectedWorkflow) {

        Map<String, DocumentModel> tasksDocs = loadTasks();
        Map<String, String> tasksFromWorkflow = new HashMap<String, String>();
        for (Map.Entry<String, DocumentModel> taskDoc : tasksDocs.entrySet()) {
            DocumentModel val = taskDoc.getValue();
            String workflowId = (String) val.getPropertyValue("nt:processId");
            if (workflowId != null) {
                if (workflowId.equals(selectedWorkflow)) {
                    tasksFromWorkflow.put((String) val.getPropertyValue("nt:name"),
                            (String) val.getPropertyValue("nt:name"));
                }
            }
        }

        return tasksFromWorkflow;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        tasks = null;
        workflows = null;
    }

    public void setDefaultFilterValues(ContentView contentView) {
        if (contentView != null) {
            DocumentModel doc = contentView.getSearchDocumentModel();
            doc.setPropertyValue("elotaskscv:initiator", documentManager.getPrincipal().getName());
            contentView.setSearchDocumentModel(doc);
            contentView.refreshPageProvider();
        }
    }

}
