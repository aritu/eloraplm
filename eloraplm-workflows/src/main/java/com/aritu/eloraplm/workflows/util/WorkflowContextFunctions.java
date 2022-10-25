package com.aritu.eloraplm.workflows.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.template.api.context.DocumentWrapper;

import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;
import com.aritu.eloraplm.workflows.forms.datatable.WorkflowAnswerRowData;
import com.aritu.eloraplm.workflows.forms.datatable.WorkflowAnswersTableServiceImpl;
import com.aritu.eloraplm.workflows.forms.util.WorkflowFormsHelper;
import com.aritu.eloraplm.workflows.forms.util.WorkflowFormsQueryFactory;

public class WorkflowContextFunctions {

    private static final Log log = LogFactory.getLog(
            WorkflowContextFunctions.class);

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    protected UserNameResolverHelper unr;

    public WorkflowContextFunctions(DocumentModel doc,
            DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
    }

    public List<TaskInfo> getWorkflowTasks(String wfId) {
        List<TaskInfo> tasks = new ArrayList<TaskInfo>();

        IterableQueryResult it = null;
        try {
            CoreSession session = doc.getCoreSession();
            String q = WorkflowFormsQueryFactory.getDistinctAnsweredTaskIdsForDoc(
                    doc.getId(), wfId);
            it = session.queryAndFetch(q, NXQL.NXQL);
            if (it.size() > 0) {
                String pfx = WorkflowMetadataConstants.WFANS_ANSWERS + "/*1/";

                for (Map<String, Serializable> map : it) {

                    String taskId = (String) map.get(pfx
                            + WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID);
                    GregorianCalendar executed = (GregorianCalendar) map.get(pfx
                            + WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED);

                    if (taskId != null) {
                        DocumentModel taskDoc = session.getDocument(
                                new IdRef(taskId));
                        Task task = taskDoc.getAdapter(Task.class);
                        String title = (String) taskDoc.getPropertyValue(
                                "nt:name");

                        tasks.add(new TaskInfo(taskId, title, executed,
                                task.getActors(), task.getDelegatedActors()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Uncontrolled exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
        } finally {
            if (it != null) {
                it.close();
            }
        }

        return tasks;
    }

    public List<RowData> getTaskAnswers(String wfId, String taskId) {

        // TODO Query bidez??

        List<RowData> taskAnswers = new ArrayList<RowData>();

        try {
            WorkflowAnswersTableServiceImpl wats = new WorkflowAnswersTableServiceImpl(
                    wfId);
            List<RowData> allAnswers = wats.getData(doc);
            for (RowData row : allAnswers) {
                WorkflowAnswerRowData answer = (WorkflowAnswerRowData) row;
                if (answer.getTaskId().equals(taskId)) {
                    taskAnswers.add(answer);
                }
            }

            return taskAnswers;

        } catch (EloraException e) {
            return null;
        }
    }

    public String getEuLabel(WorkflowAnswerRowData answer) {
        return WorkflowFormsHelper.getDisplayLabel("eu_ES",
                answer.getQuestionId(), answer.getLabels());
    }

    public String getEsLabel(WorkflowAnswerRowData answer) {
        return WorkflowFormsHelper.getDisplayLabel("es_ES",
                answer.getQuestionId(), answer.getLabels());
    }

    public String getAnswerEuDisplayValue(WorkflowAnswerRowData answer) {
        return getAnswerDisplayValue(answer, "eu_ES");
    }

    public String getAnswerEsDisplayValue(WorkflowAnswerRowData answer) {
        return getAnswerDisplayValue(answer, "es_ES");
    }

    public String getAnswerEnDisplayValue(WorkflowAnswerRowData answer) {
        return getAnswerDisplayValue(answer, "en_US");
    }

    public String getAnswerDisplayValue(WorkflowAnswerRowData answer,
            String locale) {
        String value = "";

        if (answer.getQuestion() == null) {
            String questionId = answer.getQuestionId();

            // WFA
            if (questionId.startsWith(
                    WorkflowFormsService.NODE_VARS_WFANS_PREFIX)) {
                value = answer.getValue();
            }
            // Action
            else if (questionId.equals(
                    WorkflowMetadataConstants.FIXED_QUESTION_ID_ACTION)) {
                value = TaskStatusHelper.convertStatus(locale,
                        answer.getValue(), false);
            }
            // Comment
            else {
                value = answer.getValue();
            }
        } else {

            value = treatWfqAnswer(answer, locale);

        }

        // Treat forms questions

        return value == null ? "" : value;
    }

    private String treatWfqAnswer(WorkflowAnswerRowData answer, String locale) {
        String value = "";

        if (answer.getValue() != null) {
            String twoDigitLocale = locale.substring(0, 2);

            DocumentModel wfq = answer.getQuestion();
            String type = (String) wfq.getPropertyValue(
                    WorkflowMetadataConstants.WFQ_TYPE);
            switch (type) {

            // TODO date? list labelekin, number? (dezimalak...)

            case "boolean":
                value = EloraMessageHelper.getTranslatedMessage(locale, "label."
                        + (answer.getValue().equals("true") ? "yes" : "no"));
                break;

            case "text":
            default:
                value = answer.getValue();
                break;
            }
        }

        return value;
    }

}
