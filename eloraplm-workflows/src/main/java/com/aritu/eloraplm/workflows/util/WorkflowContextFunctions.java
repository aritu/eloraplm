package com.aritu.eloraplm.workflows.util;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.template.api.context.DocumentWrapper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.WorkflowConstants;
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

    protected CoreSession session;

    protected DocumentModel wfInstance;

    public WorkflowContextFunctions(DocumentModel doc,
            DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
        session = doc.getCoreSession();
    }

    protected void setWfInstanceId(String wfInstanceId) {
        wfInstance = session.getDocument(new IdRef(wfInstanceId));
    }

    protected DocumentModel getWfInstance(String wfInstanceId) {
        if (wfInstance == null || !wfInstance.getId().equals(wfInstanceId)) {
            setWfInstanceId(wfInstanceId);
        }

        return wfInstance;
    }

    public String getEuWorkflowTitle(String wfInstanceId) {
        return getWorkflowTitle(wfInstanceId, "eu_ES");
    }

    public String getEsWorkflowTitle(String wfInstanceId) {
        return getWorkflowTitle(wfInstanceId, "es_ES");
    }

    public String getEnWorkflowTitle(String wfInstanceId) {
        return getWorkflowTitle(wfInstanceId, "en_US");
    }

    public String getWorkflowTitle(String wfInstanceId, String locale) {
        DocumentModel wfInstance = getWfInstance(wfInstanceId);
        GraphRoute route = wfInstance.getAdapter(GraphRoute.class);
        String label = route.getTitle();
        return EloraMessageHelper.getTranslatedMessage(locale, label);
    }

    public List<TaskInfo> getWorkflowTasks(String wfInstanceId) {
        List<TaskInfo> tasks = new ArrayList<TaskInfo>();

        DocumentModel wfInstance = getWfInstance(wfInstanceId);
        GraphRoute route = wfInstance.getAdapter(GraphRoute.class);
        String wfId = route.getModelName();

        IterableQueryResult it = null;
        try {
            String q = WorkflowFormsQueryFactory.getDistinctAnsweredTaskIdsForDoc(
                    doc.getId(), wfId, wfInstanceId);
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

    public List<RowData> getTaskAnswers(String wfInstanceId, String taskId) {

        DocumentModel wfInstance = getWfInstance(wfInstanceId);
        GraphRoute route = wfInstance.getAdapter(GraphRoute.class);
        String wfId = route.getModelName();

        List<RowData> taskAnswers = new ArrayList<RowData>();
        try {
            WorkflowAnswersTableServiceImpl wats = new WorkflowAnswersTableServiceImpl(
                    wfId, wfInstance.getId());
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

    public String getEnLabel(WorkflowAnswerRowData answer) {
        return WorkflowFormsHelper.getDisplayLabel("en_US",
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
            Object objValue = answer.getValue();

            // WFA
            if (questionId.startsWith(
                    WorkflowFormsService.NODE_VARS_WFANS_PREFIX)) {
                String type = answer.getType();

                switch (type) {

                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_DATETIME:
                    DateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    value = dtf.format((Date) objValue);
                    break;
                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_DATE:
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    value = df.format((Date) objValue);
                    break;
                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_BOOLEAN:
                    value = EloraMessageHelper.getTranslatedMessage(locale,
                            "label." + (((boolean) objValue) ? "yes" : "no"));
                    break;
                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_TEXT:
                    value = (String) objValue;
                    break;
                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_DOCUMENT:
                    DocumentModel doc = session.getDocument(
                            new IdRef((String) objValue));
                    if (doc != null) {
                        String reference = null;
                        value = "";
                        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
                            reference = (String) doc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
                            value += reference + " ";
                        }
                        value += doc.getTitle();
                    }
                    break;
                case WorkflowConstants.WORKFLOW_ANSWER_TYPE_DOCUMENT_LIST:
                    value = "";
                    List<String> docValues = new ArrayList<String>();
                    @SuppressWarnings("unchecked")
                    List<String> docUids = (List<String>) objValue;
                    for (String docUid : docUids) {
                        DocumentModel listdoc = session.getDocument(
                                new IdRef(docUid));
                        if (listdoc != null) {
                            String reference = null;
                            String listValue = "";
                            if (listdoc.hasSchema(
                                    EloraSchemaConstants.ELORA_OBJECT)) {
                                reference = (String) listdoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE);
                                listValue += reference + " ";
                            }
                            listValue += listdoc.getTitle();
                            docValues.add(listValue);
                        }
                    }
                    value = String.join(", ", docValues);
                    break;
                default:
                    value = "-";
                    break;
                }
            }
            // Action
            else if (questionId.equals(
                    WorkflowMetadataConstants.FIXED_QUESTION_ID_ACTION)) {
                value = TaskStatusHelper.convertStatus(locale,
                        (String) objValue, false);
            }
            // Comment
            else {
                value = (String) objValue;
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
            // TODO WF IBAI Zetako da?
            // String twoDigitLocale = locale.substring(0, 2);

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
                value = (String) answer.getValue();
                break;
            }
        }

        return value;
    }

}
