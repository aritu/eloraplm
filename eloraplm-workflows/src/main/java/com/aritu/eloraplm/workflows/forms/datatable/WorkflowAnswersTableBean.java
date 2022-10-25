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
package com.aritu.eloraplm.workflows.forms.datatable;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;

import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.datatable.DocBasedTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.workflows.forms.util.WorkflowFormsHelper;

/**
 *
 * @author aritu
 *
 */
@Name("workflowAnswersTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class WorkflowAnswersTableBean extends DocBasedTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            WorkflowAnswersTableBean.class);

    private boolean reloadData = false;

    public List<String> answeredWorkflows;

    public String activeTabWorkflow;

    public Integer activeTabIndex;

    public List<String> getAnsweredWorkflows() {
        return answeredWorkflows;
    }

    public void setAnsweredWorkflows(List<String> answeredWorkflows) {
        this.answeredWorkflows = answeredWorkflows;
    }

    public WorkflowAnswersTableBean() {
        answeredWorkflows = new ArrayList<String>();
    }

    public String getActiveTabWorkflow() {
        return activeTabWorkflow;
    }

    public void setActiveTabWorkflow(String activeTabWorkflow) {
        this.activeTabWorkflow = activeTabWorkflow;
    }

    public Integer getActiveTabIndex() {
        if (activeTabIndex == null) {
            calculateActiveTabIndex();
        }

        return activeTabIndex;
    }

    public void setActiveTabIndex(Integer activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    private void calculateActiveTabIndex() {
        activeTabIndex = 0;

        if (activeTabWorkflow != null) {
            if (answeredWorkflows.contains(activeTabWorkflow)) {
                activeTabIndex = answeredWorkflows.indexOf(activeTabWorkflow);
            }

        }
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table for workflow id |"
                    + activeTabWorkflow + "|...");
            tableService = new WorkflowAnswersTableServiceImpl(
                    activeTabWorkflow);
            setData(tableService.getData(getCurrentDocument()));
            log.trace(logInitMsg + "Table created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    @Factory(value = "workflowAnswersData", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Observer(value = { WorkflowEventNames.WF_FORMS_ANSWERS_UPDATED })
    @BypassInterceptors
    public void markToBeReloaded() {
        reloadData = true;
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        if (reloadData) {
            doc.refresh();
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: workflow answers updated.");
        }

        return doc;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        loadSubTabs();
        super.resetBeanCache(newCurrentDocumentModel);
        reloadData = false;
    }

    private void loadSubTabs() {
        String logInitMsg = "[loadSubTabs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Loading subtabs...");

        answeredWorkflows = WorkflowFormsHelper.getDistinctAnsweredWorkflowIdsForDoc(
                documentManager, getCurrentDocument());

        log.trace(logInitMsg + "Answered workflows=|"
                + String.join(" , ", answeredWorkflows) + "|");

        if (!answeredWorkflows.isEmpty()) {
            activeTabWorkflow = answeredWorkflows.get(0);
            log.trace(logInitMsg + "Selected tab: " + activeTabWorkflow);
        } else {
            activeTabWorkflow = null;
            log.trace(logInitMsg + "No available workflows.");
        }
        calculateActiveTabIndex();
    }

    public void onTabChange(TabChangeEvent event) {
        Tab activeTab = event.getTab();
        activeTabWorkflow = activeTab.getId();
        calculateActiveTabIndex();

        // We want to resetBeanCache without reloading subtabs.
        super.resetBeanCache(getCurrentDocument());
        reloadData = false;
    }

}
