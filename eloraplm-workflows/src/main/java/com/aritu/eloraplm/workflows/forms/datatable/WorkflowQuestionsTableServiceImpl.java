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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.datatable.EditableTableService;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.webapp.util.ListChoice;
import com.aritu.eloraplm.webapp.util.ListChoiceHelper;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;
import com.aritu.eloraplm.webapp.util.LocalizedLabelHelper;
import com.aritu.eloraplm.workflows.forms.util.WorkflowFormsQueryFactory;

/**
 *
 * @author aritu
 *
 */
public class WorkflowQuestionsTableServiceImpl implements EditableTableService {

    @Override
    public List<RowData> getData(Object dm) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();

        CoreSession session = (CoreSession) dm;
        String query = WorkflowFormsQueryFactory.getWorkflowModelQuestions();
        DocumentModelList questionList = session.query(query);
        for (DocumentModel question : questionList) {

            // Unsaved questions' rowId is workflowId + "_" + nodeId + "_" +
            // questionId, but saved questions use the UID
            String rowId = question.getId();

            String workflowId = (String) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_WORKFLOW_ID);
            String nodeId = (String) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_NODE_ID);
            String questionId = (String) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_QUESTION_ID);

            String type = (String) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_TYPE);

            Integer numberMaxIntegerPlaces = (question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_NUMBER_MAX_INTEGER_PLACES) == null
                            ? null
                            : (int) (long) question.getPropertyValue(
                                    WorkflowMetadataConstants.WFQ_NUMBER_MAX_INTEGER_PLACES));
            Integer numberMaxDecimalPlaces = (question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_NUMBER_MAX_DECIMAL_PLACES) == null
                            ? null
                            : (int) (long) question.getPropertyValue(
                                    WorkflowMetadataConstants.WFQ_NUMBER_MAX_DECIMAL_PLACES));
            Integer stringMaxLength = (question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_STRING_MAX_LENGTH) == null
                            ? null
                            : (int) (long) question.getPropertyValue(
                                    WorkflowMetadataConstants.WFQ_STRING_MAX_LENGTH));
            @SuppressWarnings("unchecked")
            List<ListChoice> listChoices = ListChoiceHelper.convertMapListToObjectList(
                    (List<Map<String, Object>>) question.getPropertyValue(
                            WorkflowMetadataConstants.WFQ_LIST_CHOICES));
            String defaultValue = (String) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_DEFAULT_VALUE);
            @SuppressWarnings("unchecked")
            List<LocalizedLabel> labels = LocalizedLabelHelper.convertMapListToObjectList(
                    (List<Map<String, String>>) question.getPropertyValue(
                            WorkflowMetadataConstants.WFQ_LABELS));

            boolean required = (boolean) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_REQUIRED);
            int order = (question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_ORDER) == null ? 0
                            : (int) (long) question.getPropertyValue(
                                    WorkflowMetadataConstants.WFQ_ORDER));
            boolean obsolete = (boolean) question.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_OBSOLETE);

            data.add(createRowData(rowId, workflowId, nodeId, questionId, type,
                    numberMaxIntegerPlaces, numberMaxDecimalPlaces,
                    stringMaxLength, listChoices, defaultValue, labels,
                    required, order, obsolete, false, false, false));
        }

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, false, false, false);
    }

    @Override
    public RowData createRowData(String rowId, boolean isNew,
            boolean isModified, boolean isRemoved) {
        return createRowData(rowId, null, null, null, null, null, null, null,
                null, null, null, false, 0, false, isNew, isModified,
                isRemoved);
    }

    public RowData createRowData(String rowId, String workflowId, String nodeId,
            String questionId, String type, Integer numberMaxIntegerPlaces,
            Integer numberMaxDecimalPlaces, Integer stringMaxLength,
            List<ListChoice> listChoices, String defaultValue,
            List<LocalizedLabel> labels, boolean required, int order,
            boolean obsolete, boolean isNew, boolean isModified,
            boolean isRemoved) {

        RowData row = new WorkflowQuestionRowData(rowId, workflowId, nodeId,
                questionId, type, numberMaxIntegerPlaces,
                numberMaxDecimalPlaces, stringMaxLength, listChoices,
                defaultValue, labels, required, order, obsolete, isNew,
                isModified, isRemoved);
        return row;
    }

}
