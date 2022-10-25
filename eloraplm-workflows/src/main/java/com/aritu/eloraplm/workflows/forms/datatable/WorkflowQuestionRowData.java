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

import java.util.List;
import com.aritu.eloraplm.datatable.BaseRowData;
import com.aritu.eloraplm.webapp.util.ListChoice;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;

/**
 *
 * @author aritu
 *
 */
public class WorkflowQuestionRowData extends BaseRowData {

    private static final long serialVersionUID = 1L;

    private String workflowId;

    private String nodeId;

    private String questionId;

    private String type;

    private Integer numberMaxIntegerPlaces;

    private Integer numberMaxDecimalPlaces;

    private Integer stringMaxLength;

    private List<ListChoice> listChoices;

    private String defaultValue;

    private List<LocalizedLabel> labels;

    private boolean required;

    private int order;

    private boolean obsolete;

    public WorkflowQuestionRowData(String rowId, String workflowId,
            String nodeId, String questionId, String type,
            Integer numberMaxIntegerPlaces, Integer numberMaxDecimalPlaces,
            Integer stringMaxLength, List<ListChoice> listChoices,
            String defaultValue, List<LocalizedLabel> labels, boolean required,
            int order, boolean obsolete, boolean isNew, boolean isModified,
            boolean isRemoved) {

        super(rowId, isNew, isModified, isRemoved);

        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.questionId = questionId;
        this.type = type;
        this.numberMaxIntegerPlaces = numberMaxIntegerPlaces;
        this.numberMaxDecimalPlaces = numberMaxDecimalPlaces;
        this.stringMaxLength = stringMaxLength;
        this.listChoices = listChoices;
        this.defaultValue = defaultValue;
        this.labels = labels;
        this.required = required;
        this.order = order;
        this.obsolete = obsolete;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<LocalizedLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalizedLabel> labels) {
        this.labels = labels;
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

}
