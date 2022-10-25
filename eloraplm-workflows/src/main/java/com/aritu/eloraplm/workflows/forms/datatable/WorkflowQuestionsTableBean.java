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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.PrimeFaces;

import com.aritu.eloraplm.datatable.EditableAdminTableBean;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.webapp.util.ListChoice;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;
import com.aritu.eloraplm.webapp.util.LocalizedLabelHelper;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 *
 * @author aritu
 *
 */
@Name("workflowQuestionsTableBean")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
public class WorkflowQuestionsTableBean extends EditableAdminTableBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            WorkflowQuestionsTableBean.class);

    private WorkflowFormsService wfs;

    /* Add/Edit fields */

    private String workflowId;

    private String nodeId;

    private String questionId;

    private String type;

    private List<LocalizedLabel> labels = new ArrayList<LocalizedLabel>();

    private Integer numberMaxIntegerPlaces;

    private Integer numberMaxDecimalPlaces;

    private Integer stringMaxLength;

    private List<ListChoice> listChoices;

    private String defaultValue;

    private boolean required;

    private int order;

    private boolean obsolete;

    /* ---------------- */

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        nodeId = null;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<LocalizedLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalizedLabel> labels) {
        this.labels = labels;
    }

    public Integer getNumberMaxIntegerPlaces() {
        return numberMaxIntegerPlaces;
    }

    public void setNumberMaxIntegerPlaces(Integer numberMaxIntegerPlaces) {
        this.numberMaxIntegerPlaces = numberMaxIntegerPlaces;
    }

    public Integer getNumberMaxDecimalPlaces() {
        return numberMaxDecimalPlaces;
    }

    public void setNumberMaxDecimalPlaces(Integer numberMaxDecimalPlaces) {
        this.numberMaxDecimalPlaces = numberMaxDecimalPlaces;
    }

    public Integer getStringMaxLength() {
        return stringMaxLength;
    }

    public void setStringMaxLength(Integer stringMaxLength) {
        this.stringMaxLength = stringMaxLength;
    }

    public List<ListChoice> getListChoices() {
        return listChoices;
    }

    public void setListChoices(List<ListChoice> listChoices) {
        this.listChoices = listChoices;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean getObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public WorkflowQuestionsTableBean() {
        tableService = new WorkflowQuestionsTableServiceImpl();
        wfs = Framework.getService(WorkflowFormsService.class);
        // workflowId = wfFormActions.getSelectedWorkflow();
        // nodeId = wfFormActions.getSelectedNode();
    }

    @Create
    @Override
    public void init() {
        labels = LocalizedLabelHelper.initializeLabelList(labels);
        listChoices = new ArrayList<ListChoice>();
        super.init();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(documentManager));
            setIsDirty(false);
            log.trace(logInitMsg + "Table created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    @Factory(value = "workflowQuestionsData", scope = ScopeType.EVENT)
    public List<RowData> getDataFromFactory() {
        return getData();
    }

    @Override
    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding row to workflow questions datatable.");

        try {
            WorkflowQuestionsTableServiceImpl ts = (WorkflowQuestionsTableServiceImpl) tableService;

            // After we save the row, it will change to document's uid
            String rowId = workflowId + "_" + nodeId + "_" + questionId;

            RowData row = ts.createRowData(rowId, workflowId, nodeId,
                    questionId, type, numberMaxIntegerPlaces,
                    numberMaxDecimalPlaces, stringMaxLength, listChoices,
                    defaultValue, labels, required, order, obsolete, true,
                    false, false);

            getData().add(row);
            if (getDataTable().isFilteringEnabled()) {
                PrimeFaces.current().executeScript(
                        "PF('" + getDataTable().getWidgetVar() + "').filter()");
            }

            log.trace(logInitMsg + "Row |" + rowId + "| added.");

            resetCreateFormValues();
            setIsDirty(true);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.datatable.row.add"));
        } catch (

        Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.datatable.row.add"));
        }
    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (getIsDirty()) {
            try {
                for (RowData r : getData()) {

                    WorkflowQuestionRowData row = (WorkflowQuestionRowData) r;

                    if (row.getIsNew()) {
                        DocumentModel question = wfs.createQuestion(
                                documentManager, row.getWorkflowId(),
                                row.getNodeId(), row.getQuestionId(), true,
                                null, null, row.getType(),
                                row.getNumberMaxIntegerPlaces(),
                                row.getNumberMaxDecimalPlaces(),
                                row.getStringMaxLength(), row.getListChoices(),
                                row.getDefaultValue(), row.getLabels(),
                                row.getRequired(), row.getOrder(),
                                row.getObsolete());

                    } else if (row.getIsModified()) {
                        DocumentModel question = wfs.updateQuestion(
                                documentManager, new IdRef(row.getId()),
                                row.getWorkflowId(), row.getNodeId(),
                                row.getQuestionId(), row.getType(),
                                row.getNumberMaxIntegerPlaces(),
                                row.getNumberMaxDecimalPlaces(),
                                row.getStringMaxLength(), row.getListChoices(),
                                row.getDefaultValue(), row.getLabels(),
                                row.getRequired(), row.getOrder(),
                                row.getObsolete());

                    } else if (row.getIsRemoved()) {
                        wfs.removeQuestion(documentManager,
                                new IdRef(row.getId()));
                    }
                }

                documentManager.save();
                createData();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.workflows.forms.save"));
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.workflows.forms.save"));
            }
        }
    }

    @Override
    protected void clearFilters() {
        workflowId = null;
        nodeId = null;
        super.clearFilters();
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();

        labels = LocalizedLabelHelper.initializeLabelList(labels);

        questionId = null;
        type = null;
        required = false;
        order = 0;
        obsolete = false;
    }

    public String getWorkflowFilter() {
        Map<String, Object> filters = getDataTable().getFilters();
        if (!filters.isEmpty()) {
            return (String) filters.values().toArray()[0];
        }
        return null;
    }

    public void onTypeChanged() {
        numberMaxIntegerPlaces = null;
        numberMaxDecimalPlaces = null;
        stringMaxLength = null;
        listChoices.clear();
        defaultValue = null;
    }

    public void onTypeChangedEditing(WorkflowQuestionRowData rowData) {
        rowData.setNumberMaxIntegerPlaces(null);
        rowData.setNumberMaxDecimalPlaces(null);
        rowData.setStringMaxLength(null);
        rowData.getListChoices().clear();
        rowData.setDefaultValue(null);
    }
}
