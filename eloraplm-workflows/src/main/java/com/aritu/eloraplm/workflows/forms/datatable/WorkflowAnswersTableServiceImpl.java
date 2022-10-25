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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.datatable.TableService;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class WorkflowAnswersTableServiceImpl implements TableService {

    private String filterWfId;

    public WorkflowAnswersTableServiceImpl(String filterWfId) {
        this.filterWfId = filterWfId;
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
            String value = (String) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_VALUE);
            int order = (int) (long) answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_ORDER);

            String rowId = String.join(".", workflowId, nodeId, questionId,
                    taskId);

            if (filterWfId == null || filterWfId.equals(workflowId)) {
                data.add(createRowData(rowId, workflowId, nodeId, questionId,
                        taskId, taskExecuted, question, value, order));
            }
        }

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId);
    }

    public RowData createRowData(String rowId, String workflowId, String nodeId,
            String questionId, String taskId, GregorianCalendar taskExecuted,
            DocumentModel question, String value, int order) {

        RowData row = new WorkflowAnswerRowData(rowId, workflowId, nodeId,
                questionId, taskId, taskExecuted, question, value, order);

        return row;
    }
}
