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

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.datatable.BaseRowData;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;
import com.aritu.eloraplm.webapp.util.LocalizedLabelHelper;

/**
 *
 * @author aritu
 *
 */
public class WorkflowAnswerRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private String workflowId;

    private String nodeId;

    private String questionId;

    private String taskId;

    private GregorianCalendar taskExecuted;

    private String rowGroupId;

    private List<LocalizedLabel> labels;

    private DocumentModel question;

    private String value;

    private int order;

    public WorkflowAnswerRowData(String rowId, String workflowId, String nodeId,
            String questionId, String taskId, GregorianCalendar taskExecuted,
            DocumentModel question, String value, int order) {

        super(rowId);

        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.questionId = questionId;
        this.taskId = taskId;
        this.taskExecuted = taskExecuted;
        rowGroupId = taskExecuted.getTimeInMillis() + "__" + taskId;
        setQuestion(question);
        this.value = value;
        this.order = order;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public GregorianCalendar getTaskExecuted() {
        return taskExecuted;
    }

    public void setTaskExecuted(GregorianCalendar taskExecuted) {
        this.taskExecuted = taskExecuted;
    }

    public String getRowGroupId() {
        return rowGroupId;
    }

    public DocumentModel getQuestion() {
        return question;
    }

    public void setQuestion(DocumentModel question) {
        this.question = question;
        if (question != null) {
            @SuppressWarnings("unchecked")
            List<LocalizedLabel> labels = LocalizedLabelHelper.convertMapListToObjectList(
                    (List<Map<String, String>>) question.getPropertyValue(
                            WorkflowMetadataConstants.WFQ_LABELS));
            setLabels(labels);
        }
    }

    public List<LocalizedLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalizedLabel> labels) {
        this.labels = labels;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
