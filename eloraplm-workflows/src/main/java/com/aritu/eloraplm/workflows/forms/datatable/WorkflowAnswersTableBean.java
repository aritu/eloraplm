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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.event.TabChangeEvent;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.datatable.DocBasedTable;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.workflows.forms.util.WorkflowFormsHelper;

/**
 *
 * @author aritu
 *
 */
@Name(WorkflowAnswersTableBean.NAME)
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class WorkflowAnswersTableBean extends DocBasedTable {

    public static final String NAME = "workflowAnswersTableBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            WorkflowAnswersTableBean.class);

    private boolean reloadData = false;

    private DocumentRoutingService docRoutingService;

    private List<String> workflowTabs;

    private Map<String, List<String>> instanceTabs;

    private String selectedWorkflowTab;

    private String selectedInstanceTab;

    private DocumentModel selectedInstance;

    private Integer selectedWorkflowTabIndex;

    private Integer selectedInstanceTabIndex;

    private Map<String, String> instanceNames;

    private SortedMap<String, List<String>> activeWfInstances;

    private List<DocumentRoute> allRelatedRoutes;

    public List<String> getWorkflowTabs() {
        return workflowTabs;
    }

    public void setWorkflowTabs(List<String> workflowTabs) {
        this.workflowTabs = workflowTabs;
    }

    public List<String> getInstanceTabs(String workflowId) {
        if (instanceTabs.containsKey(workflowId)) {
            List<String> it = instanceTabs.get(workflowId);
            if (it != null) {
                return it;
            }
        }
        return new ArrayList<String>();
    }

    public void setInstanceTabs(Map<String, List<String>> instanceTabs) {
        this.instanceTabs = instanceTabs;
    }

    public String getSelectedWorkflowTab() {
        return selectedWorkflowTab;
    }

    public void setSelectedWorkflowTab(String selectedWorkflowTab) {
        this.selectedWorkflowTab = selectedWorkflowTab;
        resetSelectedInstanceTab();
        calculateSelectedWorkflowTabIndex();
    }

    public String getSelectedInstanceTab() {
        return selectedInstanceTab;
    }

    public void setSelectedInstanceTab(String selectedInstanceTab) {
        this.selectedInstanceTab = selectedInstanceTab;
        calculateSelectedInstanceTabIndex();
        if (selectedInstanceTab != null) {
            setSelectedInstance(documentManager.getDocument(
                    new IdRef(selectedInstanceTab)));
        } else {
            setSelectedInstance(null);
        }
    }

    private void resetSelectedInstanceTab() {
        setSelectedInstanceTab(null);
        if (getSelectedWorkflowTab() != null) {
            if (instanceTabs.containsKey(getSelectedWorkflowTab())) {
                setSelectedInstanceTab(
                        instanceTabs.get(getSelectedWorkflowTab()).get(0));
            }
        }
    }

    public DocumentModel getSelectedInstance() {
        return selectedInstance;
    }

    public void setSelectedInstance(DocumentModel selectedInstance) {
        this.selectedInstance = selectedInstance;
    }

    public Integer getSelectedWorkflowTabIndex() {
        if (selectedWorkflowTabIndex == null) {
            calculateSelectedWorkflowTabIndex();
        }

        return selectedWorkflowTabIndex;
    }

    public void setSelectedWorkflowTabIndex(Integer selectedWorkflowTabIndex) {
        this.selectedWorkflowTabIndex = selectedWorkflowTabIndex;
    }

    public Integer getSelectedInstanceTabIndex() {
        if (selectedInstanceTabIndex == null) {
            calculateSelectedInstanceTabIndex();
        }

        return selectedInstanceTabIndex;
    }

    public void setSelectedInstanceTabIndex(Integer selectedInstanceTabIndex) {
        this.selectedInstanceTabIndex = selectedInstanceTabIndex;
    }

    public String getInstanceName(String instanceId) {
        if (instanceNames.containsKey(instanceId)) {
            return instanceNames.get(instanceId);
        } else {
            return instanceId;
        }
    }

    public boolean isActiveWorkflow(String workflowId) {
        return getActiveWfInstances().containsKey(workflowId);
    }

    public boolean isActiveInstance(String workflowId, String instanceId) {
        if (getActiveWfInstances().containsKey(workflowId)) {
            return getActiveWfInstances().get(workflowId).contains(instanceId);
        }
        return false;
    }

    public SortedMap<String, List<String>> getActiveWfInstances() {
        if (activeWfInstances == null) {
            calculateActiveWorkflowsAndInstances();
        }
        return activeWfInstances;
    }

    public void setActiveWfInstances(
            SortedMap<String, List<String>> activeWfInstances) {
        this.activeWfInstances = activeWfInstances;
    }

    private List<DocumentRoute> getAllRelatedRoutes() {
        if (allRelatedRoutes == null) {
            List<DocumentRouteElement.ElementLifeCycleState> states = new ArrayList<DocumentRouteElement.ElementLifeCycleState>();
            states.add(DocumentRouteElement.ElementLifeCycleState.ready);
            states.add(DocumentRouteElement.ElementLifeCycleState.running);
            states.add(DocumentRouteElement.ElementLifeCycleState.canceled);
            states.add(DocumentRouteElement.ElementLifeCycleState.done);
            states.add(DocumentRouteElement.ElementLifeCycleState.validated);
            allRelatedRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                    documentManager, getCurrentDocument().getId(), states);
        }
        return allRelatedRoutes;
    }

    public WorkflowAnswersTableBean() {
        workflowTabs = new ArrayList<String>();
        instanceTabs = new TreeMap<String, List<String>>();
        instanceNames = new HashMap<String, String>();
        allRelatedRoutes = null;
    }

    private void calculateSelectedWorkflowTabIndex() {
        selectedWorkflowTabIndex = 0;

        if (selectedWorkflowTab != null) {
            if (workflowTabs.contains(selectedWorkflowTab)) {
                selectedWorkflowTabIndex = workflowTabs.indexOf(
                        selectedWorkflowTab);
            }

        }
    }

    private void calculateSelectedInstanceTabIndex() {
        selectedInstanceTabIndex = 0;

        if (selectedWorkflowTab != null && selectedInstanceTab != null) {
            if (instanceTabs.containsKey(selectedWorkflowTab)
                    && instanceTabs.get(selectedWorkflowTab).contains(
                            selectedInstanceTab)) {
                selectedInstanceTabIndex = instanceTabs.get(
                        selectedWorkflowTab).indexOf(selectedInstanceTab);
            }

        }
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table for workflow id |"
                    + selectedWorkflowTab + "| and instance id |"
                    + selectedInstanceTab + "|...");
            tableService = new WorkflowAnswersTableServiceImpl(
                    selectedWorkflowTab, selectedInstanceTab);
            setData(tableService.getData(getCurrentDocument()));
            log.trace(logInitMsg + "Table created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty data attribute and set isInvalid attribute to true
            setData(new ArrayList<RowData>());
            setIsInvalid(true);
            setHasUnreadableRows(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Observer(value = { WorkflowEventNames.WF_FORMS_ANSWERS_UPDATED,
            TaskEventNames.WORKFLOW_CANCELED })
    @BypassInterceptors
    public void markToBeReloaded() {
        allRelatedRoutes = null;
        setActiveWfInstances(null);
        reloadData = true;
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        if (reloadData) {
            if (doc != null) {
                doc.refresh();
            }
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: workflow answers updated.");
        }

        return doc;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        allRelatedRoutes = null;
        setActiveWfInstances(null);
        loadSubTabs();
        super.resetBeanCache(newCurrentDocumentModel);
        reloadData = false;
    }

    private void loadSubTabs() {
        String logInitMsg = "[loadSubTabs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Loading subtabs...");

        Map<String, List<String>> answeredAndActiveInstances = WorkflowFormsHelper
                .getDistinctAnsweredWorkflowAndInstanceIdsForDoc(
                        documentManager, getCurrentDocument());

        // Add active ones without answers
        for (Map.Entry<String, List<String>> entry : getActiveWfInstances().entrySet()) {
            String wfId = entry.getKey();
            if (!answeredAndActiveInstances.containsKey(wfId)) {
                answeredAndActiveInstances.put(wfId, new ArrayList<String>());
            }
            for (String instanceId : entry.getValue()) {
                if (!answeredAndActiveInstances.get(wfId).contains(
                        instanceId)) {
                    answeredAndActiveInstances.get(wfId).add(instanceId);
                }
            }
        }

        // Ordered routes
        List<DocumentRoute> orderedRoutes = getAllRelatedRoutes();
        Collections.reverse(orderedRoutes);

        // Ordered instances and workflows
        List<String> orderedWorkflows = new ArrayList<String>();
        Map<String, List<String>> orderedInstances = new HashMap<String, List<String>>();
        instanceNames = new HashMap<String, String>();
        for (DocumentRoute route : orderedRoutes) {
            String wfId = route.getModelName();
            DocumentModel routeDoc = route.getDocument();
            String instId = routeDoc.getId();
            String instName = null;
            if (routeDoc.hasFacet(
                    EloraFacetConstants.FACET_ASYNCHRONOUS_SUBWORKFLOW)) {
                String instanceTitle = (String) routeDoc.getPropertyValue(
                        WorkflowMetadataConstants.SUBWF_INSTANCE_TITLE);
                if (instanceTitle != null && !instanceTitle.isEmpty()) {
                    instName = instanceTitle;
                }
            }
            // Default to creation time as instance name (tab title)
            if (instName == null) {
                GregorianCalendar instDate = (GregorianCalendar) route.getDocument().getPropertyValue(
                        NuxeoMetadataConstants.NX_DC_CREATED);
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                fmt.setCalendar(instDate);
                instName = fmt.format(instDate.getTime());
            }
            if (answeredAndActiveInstances.containsKey(wfId)) {
                if (answeredAndActiveInstances.get(wfId).contains(instId)) {
                    if (!orderedWorkflows.contains(wfId)) {
                        orderedWorkflows.add(wfId);
                    }
                    if (!orderedInstances.containsKey(wfId)) {
                        orderedInstances.put(wfId, new ArrayList<String>());
                    }
                    if (!orderedInstances.get(wfId).contains(instId)) {
                        orderedInstances.get(wfId).add(instId);
                    }
                    if (!instanceNames.containsKey(instId)) {
                        instanceNames.put(instId, instName);
                    }
                }
                // TODO WF IBAI ELSE gero gehitu?
            }
            // TODO WF IBAI ELSE gero gehitu?
        }

        instanceTabs = orderedInstances;
        workflowTabs = orderedWorkflows;

        log.trace(logInitMsg + "Answered + active workflows = |"
                + String.join(" , ", instanceTabs.keySet()) + "|");

        if (!workflowTabs.isEmpty() && !instanceTabs.isEmpty()) {
            setSelectedWorkflowTab(workflowTabs.get(0));
            log.trace(logInitMsg + "Selected tab: " + selectedWorkflowTab
                    + " -> " + selectedInstanceTab);
        } else {
            setSelectedWorkflowTab(null);
            log.trace(logInitMsg + "No available workflows.");
        }
        calculateSelectedWorkflowTabIndex();
    }

    public void onWorkflowTabChange(TabChangeEvent<String> event) {
        String tabId = event.getData();
        setSelectedWorkflowTab(tabId);

        // We want to resetBeanCache without reloading subtabs.
        super.resetBeanCache(getCurrentDocument());
        reloadData = false;
    }

    public void onInstanceTabChange(TabChangeEvent<String> event) {
        String tabId = event.getData();
        setSelectedInstanceTab(tabId);

        // We want to resetBeanCache without reloading subtabs.
        super.resetBeanCache(getCurrentDocument());
        reloadData = false;
    }

    private void calculateActiveWorkflowsAndInstances() {
        setActiveWfInstances(new TreeMap<String, List<String>>());
        List<DocumentRoute> relatedRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                documentManager, getCurrentDocument().getId());
        if (relatedRoutes.size() > 0) {
            for (DocumentRoute route : relatedRoutes) {
                String wfId = route.getModelName();
                String instanceId = route.getDocument().getId();
                if (!activeWfInstances.containsKey(wfId)) {
                    activeWfInstances.put(wfId, new ArrayList<String>());
                }
                if (!activeWfInstances.get(wfId).contains(instanceId)) {
                    activeWfInstances.get(wfId).add(instanceId);
                }
            }
        }
    }

    private DocumentRoutingService getDocumentRoutingService() {
        if (docRoutingService == null) {
            docRoutingService = Framework.getService(
                    DocumentRoutingService.class);
        }
        return docRoutingService;
    }

    // TODO Etorkizunean klase bat sortu eta dena itzuli, baina oraingoz
    // endNodeLabel bakarrik interesatzen zaigu
    public String getEndNodeLabel() {
        String wfInstanceId = getSelectedInstanceTab();
        if (wfInstanceId != null) {

            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> wfInfoInstances = (List<Map<String, Serializable>>) getCurrentDocument()
                    .getPropertyValue(
                            WorkflowMetadataConstants.WFINFO_INSTANCES);
            if (wfInfoInstances != null && !wfInfoInstances.isEmpty()) {
                Map<String, Serializable> instanceInfo = wfInfoInstances.stream().filter(
                        i -> i.get(
                                WorkflowMetadataConstants.WFINFO_INSTANCES_WORKFLOW_INSTANCE_ID).equals(
                                        wfInstanceId))
                        .findFirst().orElse(null);
                if (instanceInfo != null) {
                    return (String) instanceInfo.get(
                            WorkflowMetadataConstants.WFINFO_INSTANCES_END_NODE_LABEL);
                }
            }
        }

        return null;

    }

}
