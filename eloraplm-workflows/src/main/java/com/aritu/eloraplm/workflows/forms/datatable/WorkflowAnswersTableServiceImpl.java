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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.constants.WorkflowConstants;
import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.datatable.TableService;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 *
 * @author aritu
 *
 */
public class WorkflowAnswersTableServiceImpl implements TableService {

    private String filterWfId;

    private String filterInstanceId;

    public WorkflowAnswersTableServiceImpl(String filterWfId,
            String filterInstanceId) {
        this.filterWfId = filterWfId;
        this.filterInstanceId = filterInstanceId;
    }

    @Override
    public List<RowData> getData(Object parentObject) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;
        CoreSession session = doc.getCoreSession();

        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> answers = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                WorkflowMetadataConstants.WFANS_ANSWERS);

        for (HashMap<String, Object> answer : answers) {
            String workflowId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID);
            String wfInstanceId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_INSTANCE_ID);
            String nodeId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_NODE_ID);
            String questionId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION_ID);
            String taskId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID);
            GregorianCalendar taskExecuted = (GregorianCalendar) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED);
            String questionDocId = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION);
            DocumentModel question = null;
            if (questionDocId != null
                    && session.exists(new IdRef(questionDocId))) {
                question = session.getDocument(new IdRef(questionDocId));
            }

            String type = null;
            String strValue = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_VALUE);
            Object value = null;

            if (question != null) {
                value = strValue;
                type = (String) question.getPropertyValue(
                        WorkflowMetadataConstants.WFQ_TYPE);
            } else {
                if (strValue != null) {
                    // If name starts with wfa_ we have to treat it before
                    if (questionId.startsWith(
                            WorkflowFormsService.NODE_VARS_WFANS_PREFIX)) {

                        // Check if it is boolean
                        if (strValue.equals("true")
                                || strValue.equals("false")) {
                            type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_BOOLEAN;
                            value = Boolean.valueOf(strValue);
                        }

                        // Check if it is a date
                        Date dateValue = null;
                        try {
                            // Thu Mar 14 00:00:00 CET 2024
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(
                                    "EEE MMM dd HH:mm:ss zzz yyyy");
                            ZonedDateTime zdt = ZonedDateTime.parse(strValue,
                                    dtf);
                            dateValue = Date.from(zdt.toInstant());
                        } catch (DateTimeParseException e) {
                            // It is not a date
                        }

                        if (dateValue != null) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(dateValue);
                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            int min = calendar.get(Calendar.MINUTE);
                            int sec = calendar.get(Calendar.SECOND);
                            if (hour == 0 && min == 0 && sec == 0) {
                                type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_DATE;
                            } else {
                                type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_DATETIME;
                            }
                            value = dateValue;
                        }

                        // Check if it is an UUID
                        if (strValue.matches(
                                "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$")) {
                            if (session.exists(new IdRef(strValue))) {
                                value = strValue;
                                type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_DOCUMENT;
                            }
                        }

                        // TODO This should be something global, for lists of
                        // values of any type.
                        // Check if it is a list of UUIDs
                        if (strValue.matches(
                                "^([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}, )+([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$")) {
                            String[] docValues = strValue.split(", ");
                            List<String> listValue = new ArrayList<String>();
                            for (String docValue : docValues) {
                                if (session.exists(new IdRef(docValue))) {
                                    listValue.add(docValue);
                                }
                            }
                            if (!listValue.isEmpty()) {
                                value = listValue;
                                type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_DOCUMENT_LIST;
                            }
                        }

                    }
                }
            }

            // If value has not been filled, we take the string format
            if (value == null) {
                value = strValue;
                type = WorkflowConstants.WORKFLOW_ANSWER_TYPE_TEXT;
            }

            int order = (int) (long) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_ORDER);

            String rowId = String.join(".", wfInstanceId, nodeId, questionId,
                    taskId);

            if (filterWfId == null || filterWfId.equals(workflowId)) {
                if (filterInstanceId == null
                        || filterInstanceId.equals(wfInstanceId)) {
                    data.add(createRowData(rowId, wfInstanceId, workflowId,
                            nodeId, questionId, taskId, taskExecuted, question,
                            type, value, order));
                }
            }
        }

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId);
    }

    public RowData createRowData(String rowId, String wfInstanceId,
            String workflowId, String nodeId, String questionId, String taskId,
            GregorianCalendar taskExecuted, DocumentModel question, String type,
            Object value, int order) {

        RowData row = new WorkflowAnswerRowData(rowId, wfInstanceId, workflowId,
                nodeId, questionId, taskId, taskExecuted, question, type, value,
                order);

        return row;
    }
}
