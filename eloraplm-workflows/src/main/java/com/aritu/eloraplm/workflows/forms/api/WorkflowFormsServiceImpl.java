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
package com.aritu.eloraplm.workflows.forms.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.WorkflowConstants;
import com.aritu.eloraplm.constants.WorkflowDoctypeConstants;
import com.aritu.eloraplm.constants.WorkflowEventNames;
import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
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
public class WorkflowFormsServiceImpl extends DefaultComponent
        implements WorkflowFormsService {

    private static Log log = LogFactory.getLog(WorkflowFormsServiceImpl.class);

    private DocumentModelList workflows;

    private Map<String, String> workflowsMap;

    private Map<String, Map<String, String>> nodesMap;

    private List<String> fieldTypes;

    @Override
    public void activate(ComponentContext context) {
        // Inizializazioa
        // modifiers = new HashMap<String, ModifierDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        // Amaiera
        // modifiers = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        // XPen erregistroa

    }

    /*
     * Workflows & Nodes
     *
     */

    @Override
    public DocumentModelList getWorkflowList(CoreSession session)
            throws EloraException {

        if (workflows == null) {
            String query = WorkflowFormsQueryFactory.getWorkflowsDefinedByForms();
            workflows = session.query(query);
        }
        return workflows;
    }

    @Override
    public Map<String, String> getWorkflowsMap(CoreSession session)
            throws EloraException {
        if (workflowsMap == null) {
            loadWorkflowsData(session);
        }
        return workflowsMap;
    }

    @Override
    public Map<String, String> getNodesMap(CoreSession session,
            String workflowId) throws EloraException {
        if (workflowId == null) {
            // TODO Throw exception
        }
        if (nodesMap == null) {
            loadWorkflowsData(session);
        }
        if (workflowsMap.containsKey(workflowId)) {
            return nodesMap.get(workflowId);
        }
        return new HashMap<String, String>();
    }

    @Override
    public void reloadWorkflowsData(CoreSession session) throws EloraException {
        loadWorkflowsData(session);
    }

    private void loadWorkflowsData(CoreSession session) throws EloraException {
        workflowsMap = new HashMap<String, String>();
        nodesMap = new HashMap<String, Map<String, String>>();

        for (DocumentModel wf : getWorkflowList(session)) {
            workflowsMap.put(wf.getName(), wf.getTitle());
            Map<String, String> wfNodes = new HashMap<String, String>();

            DocumentModelList nodeList = session.getChildren(wf.getRef());
            for (DocumentModel node : nodeList) {
                // Workflow Forms are only available in nodes with task
                boolean hasTask = node.getPropertyValue(
                        NuxeoMetadataConstants.NX_RNODE_HAS_TASK) == null
                                ? false
                                : (boolean) node.getPropertyValue(
                                        NuxeoMetadataConstants.NX_RNODE_HAS_TASK);
                if (hasTask) {
                    // nodeId != node name
                    String nodeId = (String) node.getPropertyValue(
                            "rnode:nodeId");
                    wfNodes.put(nodeId, node.getTitle());
                }
            }

            nodesMap.put(wf.getName(), wfNodes);
        }
    }

    @Override
    public List<String> getFieldTypes() {
        if (fieldTypes == null) {
            fieldTypes = Arrays.asList(new String[] { FIELD_TYPE_TEXT,
                    FIELD_TYPE_LINK, FIELD_TYPE_NUMBER, FIELD_TYPE_DATE,
                    FIELD_TYPE_BOOLEAN, FIELD_TYPE_LIST });

        }
        return fieldTypes;
    }

    /*
     * Questions
     *
     */

    @Override
    public DocumentModelList getQuestionsForWorkflow(CoreSession session,
            String wfId) throws EloraException {

        DocumentModelList questions = new DocumentModelListImpl();

        if (wfId != null) {
            String q = WorkflowFormsQueryFactory.getModelQuestionsForWorkflow(
                    wfId);
            questions = session.query(q);
        }

        return questions;
    }

    @Override
    public DocumentModelList getNodeQuestionsForDoc(CoreSession session,
            String wfId, String nodeId, String docId) throws EloraException {

        DocumentModelList questions = new DocumentModelListImpl();

        if (wfId != null && nodeId != null && docId != null) {
            String q = WorkflowFormsQueryFactory.getNodeQuestionsForDoc(wfId,
                    nodeId, docId);
            questions = session.query(q);
        }

        return questions;
    }

    public DocumentModelList getWorkflowQuestionsForDoc(CoreSession session,
            String wfId, String docId) throws EloraException {
        DocumentModelList questions = new DocumentModelListImpl();

        if (wfId != null && docId != null) {
            String q = WorkflowFormsQueryFactory.getWorkflowQuestionsForDoc(
                    wfId, docId);
            questions = session.query(q);
        }

        return questions;
    }

    @Override
    public DocumentModel createQuestion(CoreSession session, String wfId,
            String nodeId, String questionId, boolean isModel, String model,
            String parentDoc, String type, Integer numberMaxIntegerPlaces,
            Integer numberMaxDecimalPlaces, Integer stringMaxLength,
            List<ListChoice> listChoices, String defaultValue,
            List<LocalizedLabel> labels, boolean required, int order,
            boolean obsolete) {

        String logInitMsg = "[createQuestion] ["
                + session.getPrincipal().getName() + "] ";

        return new UnrestrictedSessionRunner("default") {

            DocumentModel question;

            @Override
            public void run() {

                createQuestionsRootIfDoesNotExist(session);

                PathSegmentService pss = Framework.getService(
                        PathSegmentService.class);
                String name = pss.generatePathSegment(
                        wfId + "_" + nodeId + "_" + questionId);

                question = session.createDocumentModel(
                        WorkflowConstants.WORKFLOW_QUESTIONS_ROOT_PATH, name,
                        WorkflowDoctypeConstants.WF_FORMS_QUESTION);

                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_WORKFLOW_ID, wfId);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_NODE_ID,
                        nodeId);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_QUESTION_ID, questionId);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_IS_MODEL, isModel);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_MODEL,
                        model);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_PARENT_DOC, parentDoc);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_TYPE,
                        type);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_NUMBER_MAX_INTEGER_PLACES,
                        numberMaxIntegerPlaces);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_NUMBER_MAX_DECIMAL_PLACES,
                        numberMaxDecimalPlaces);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_STRING_MAX_LENGTH,
                        stringMaxLength);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_LIST_CHOICES,
                        (Serializable) ListChoiceHelper.convertObjectListToMapList(
                                listChoices));
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_DEFAULT_VALUE,
                        defaultValue);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_LABELS,
                        (Serializable) LocalizedLabelHelper.convertObjectListToMapList(
                                labels));
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_REQUIRED, required);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_ORDER,
                        order);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_OBSOLETE, obsolete);

                question = session.createDocument(question);

                log.trace(logInitMsg + "Workflow question |" + question.getId()
                        + "| with name |" + name + "| created.");
            }

            private void createQuestionsRootIfDoesNotExist(
                    CoreSession session) {
                try {

                    if (!session.exists(new PathRef(
                            WorkflowConstants.WORKFLOW_QUESTIONS_ROOT_PATH))) {

                        DocumentModel root = session.createDocumentModel("/",
                                "workflow-questions",
                                NuxeoDoctypeConstants.FOLDER);
                        root.setPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE,
                                "Workflow Form Questions");
                        root = session.createDocument(root);

                        // Set ACP (Permissions)
                        ACP acp = new ACPImpl();
                        ACE readForEveryone = new ACE("members",
                                SecurityConstants.READ, true);
                        ACE everythingForAdmins = new ACE("Administrator",
                                SecurityConstants.EVERYTHING, true);
                        ACE everythingForPowerUsers = new ACE("powerusers",
                                SecurityConstants.EVERYTHING, true);

                        ACL acl = new ACLImpl();
                        acl.setACEs(new ACE[] { readForEveryone,
                                everythingForAdmins, everythingForPowerUsers });
                        acp.addACL(acl);
                        root.setACP(acp, true);
                    }
                } catch (Exception e) {
                    log.error(
                            "[unrestrictedRootCreation] Uncontrolled exception: "
                                    + e.getClass().getName() + ". "
                                    + e.getMessage(),
                            e);
                }

            }

            public DocumentModel runAndReturnQuestion() {
                runUnrestricted();
                return question;
            }

        }.runAndReturnQuestion();
    }

    @Override
    public DocumentModel updateQuestion(CoreSession session, DocumentRef docRef,
            String wfId, String nodeId, String questionId, String type,
            Integer numberMaxIntegerPlaces, Integer numberMaxDecimalPlaces,
            Integer stringMaxLength, List<ListChoice> listChoices,
            String defaultValue, List<LocalizedLabel> labels, boolean required,
            int order, boolean obsolete) {

        String logInitMsg = "[updateQuestion] ["
                + session.getPrincipal().getName() + "] ";

        return new UnrestrictedSessionRunner("default") {

            DocumentModel question;

            @Override
            public void run() {

                question = session.getDocument(docRef);

                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_WORKFLOW_ID, wfId);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_NODE_ID,
                        nodeId);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_QUESTION_ID, questionId);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_TYPE,
                        type);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_NUMBER_MAX_INTEGER_PLACES,
                        numberMaxIntegerPlaces);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_NUMBER_MAX_DECIMAL_PLACES,
                        numberMaxDecimalPlaces);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_STRING_MAX_LENGTH,
                        stringMaxLength);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_LIST_CHOICES,
                        (Serializable) ListChoiceHelper.convertObjectListToMapList(
                                listChoices));
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_DEFAULT_VALUE,
                        defaultValue);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_LABELS,
                        (Serializable) LocalizedLabelHelper.convertObjectListToMapList(
                                labels));
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_REQUIRED, required);
                question.setPropertyValue(WorkflowMetadataConstants.WFQ_ORDER,
                        order);
                question.setPropertyValue(
                        WorkflowMetadataConstants.WFQ_OBSOLETE, obsolete);

                question = session.saveDocument(question);

                log.trace(logInitMsg + "Workflow question |" + question.getId()
                        + "| with name |" + question.getName() + "| updated.");
            }

            public DocumentModel runAndReturnQuestion() {
                runUnrestricted();
                return question;
            }

        }.runAndReturnQuestion();
    }

    @Override
    public void removeQuestion(CoreSession session, DocumentRef docRef) {

        String logInitMsg = "[removeQuestion] ["
                + session.getPrincipal().getName() + "] ";

        new UnrestrictedSessionRunner("default") {

            @Override
            public void run() {

                session.removeDocument(docRef);

                log.trace(logInitMsg + "Workflow question |" + docRef.toString()
                        + "| removed.");

            }

        }.runUnrestricted();
    }

    @Override
    public void initializeWorkflowQuestionsForDocument(CoreSession session,
            GraphRoute route, DocumentModel doc) throws EloraException {

        String wfId = route.getModelName();

        // Remove old questions and answers
        removeWorkflowAnswers(session, doc, wfId);

        removeWorkflowQuestionsForDocument(session, wfId, doc.getId());

        // Copy all model questions for workflow
        copyQuestionsForDocument(session, wfId, doc);

        session.save();

        Events.instance().raiseEvent(
                WorkflowEventNames.WF_FORMS_ANSWERS_UPDATED);
    }

    private void removeWorkflowAnswers(CoreSession session, DocumentModel doc,
            String wfId) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) doc.getPropertyValue(
                WorkflowMetadataConstants.WFANS_ANSWERS);

        Iterator<Map<String, Object>> it = answers.iterator();
        while (it.hasNext()) {

            Map<String, Object> answer = it.next();

            if (answer.get(
                    WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID).equals(
                            wfId)) {
                it.remove();
            }
        }

        doc.setPropertyValue(WorkflowMetadataConstants.WFANS_ANSWERS,
                (Serializable) answers);

        session.saveDocument(doc);
    }

    private void removeWorkflowQuestionsForDocument(CoreSession session,
            String wfId, String docId) throws EloraException {

        DocumentModelList questions = getWorkflowQuestionsForDoc(session, wfId,
                docId);

        for (DocumentModel question : questions) {
            session.removeDocument(question.getRef());
        }

        session.save();

    }

    @Override
    public Map<String, DocumentModelList> copyQuestionsForDocument(
            CoreSession session, String wfId, DocumentModel doc)
            throws EloraException {

        Map<String, DocumentModelList> questionsByNode = new HashMap<String, DocumentModelList>();

        new UnrestrictedSessionRunner("default") {

            @Override
            public void run() {
                try {

                    DocumentModelList questions = getQuestionsForWorkflow(
                            session, wfId);
                    for (DocumentModel question : questions) {

                        if (!(boolean) question.getPropertyValue(
                                WorkflowMetadataConstants.WFQ_OBSOLETE)) {

                            DocumentModel docQuestion = session.copy(
                                    question.getRef(), question.getParentRef(),
                                    question.getName() + "." + doc.getId(),
                                    CopyOption.RESET_CREATOR,
                                    CopyOption.RESET_LIFE_CYCLE);

                            // Update metadata so it isn't a model
                            docQuestion.setPropertyValue(
                                    WorkflowMetadataConstants.WFQ_IS_MODEL,
                                    false);
                            docQuestion.setPropertyValue(
                                    WorkflowMetadataConstants.WFQ_MODEL,
                                    question.getId());
                            docQuestion.setPropertyValue(
                                    WorkflowMetadataConstants.WFQ_PARENT_DOC,
                                    doc.getId());

                            docQuestion = session.saveDocument(docQuestion);

                            String nodeId = docQuestion.getPropertyValue(
                                    WorkflowMetadataConstants.WFQ_NODE_ID).toString();
                            if (questionsByNode.containsKey(nodeId)) {
                                questionsByNode.get(nodeId).add(docQuestion);
                            } else {
                                DocumentModelList dml = new DocumentModelListImpl();
                                dml.add(docQuestion);
                                questionsByNode.put(nodeId, dml);
                            }
                        }

                    }

                } catch (Exception e) {
                    log.error(
                            "[copyQuestionsForDocument] Uncontrolled exception: "
                                    + e.getClass().getName() + ". "
                                    + e.getMessage(),
                            e);
                }
            }
        }.runUnrestricted();

        return questionsByNode;
    }

    @Override
    public void initializeNodeTaskAnswersForDocument(CoreSession session,
            GraphRoute route, Task task) {

        new UnrestrictedSessionRunner("default") {

            @Override
            public void run() {

                try {

                    Map<String, Serializable> wfVars = route.getVariables();

                    String wfId = route.getModelName();
                    String nodeId = task.getType();
                    String taskId = task.getId();

                    List<String> targets = task.getTargetDocumentsIds();
                    if (targets.size() != 1) {
                        throw new EloraException(
                                "No target docs or more than one target doc.");
                    }
                    String targetDocId = targets.get(0);
                    DocumentModel targetDoc = session.getDocument(
                            new IdRef(targetDocId));
                    if (!targetDoc.hasFacet(
                            EloraFacetConstants.FACET_HAS_WORKFLOW_FORMS)) {
                        throw new EloraException(
                                "Target doc has no HasWorkflowForms facet.");
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> taskAnswers = new ArrayList<Map<String, Object>>();

                    // Get the questions
                    String q = WorkflowFormsQueryFactory.getNodeQuestionsForDoc(
                            wfId, nodeId, targetDocId);
                    DocumentModelList questions = session.query(q);
                    for (DocumentModel question : questions) {

                        Map<String, Object> answer = new HashMap<String, Object>();
                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID,
                                wfId);
                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_NODE_ID,
                                nodeId);
                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID,
                                taskId);

                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION_ID,
                                question.getPropertyValue(
                                        WorkflowMetadataConstants.WFQ_QUESTION_ID));

                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION,
                                question.getId());

                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_VALUE,
                                question.getPropertyValue(
                                        WorkflowMetadataConstants.WFQ_DEFAULT_VALUE));

                        answer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_ORDER,
                                question.getPropertyValue(
                                        WorkflowMetadataConstants.WFQ_ORDER));

                        taskAnswers.add(answer);

                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> answers = (List<Map<String, Object>>) wfVars.get(
                            "answers");
                    answers.addAll(taskAnswers);
                    wfVars.put("answers", (Serializable) answers);
                    route.setVariables(wfVars);
                    route.save(session);
                } catch (Exception e) {
                    log.error(
                            "[initializeNodeTaskAnswersForDocument] Uncontrolled exception: "
                                    + e.getClass().getName() + ". "
                                    + e.getMessage(),
                            e);
                }
            }
        }.runUnrestricted();

    }

    @Override
    public void writeNodeTaskAnswersForDocument(CoreSession session,
            GraphRoute route, GraphNode node, Task task,
            DocumentModel targetDoc, String action) {

        new UnrestrictedSessionRunner("default") {

            @Override
            public void run() {

                try {
                    Map<String, Serializable> wfVars = route.getVariables();

                    if (!targetDoc.hasFacet(
                            EloraFacetConstants.FACET_HAS_WORKFLOW_FORMS)) {
                        throw new EloraException(
                                "Target document |" + targetDoc.getId()
                                        + "| has no HasWorkflowForms facet.");
                    }

                    // Get previous answers
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> docAnswers = (List<Map<String, Object>>) targetDoc.getPropertyValue(
                            WorkflowMetadataConstants.WFANS_ANSWERS);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> wfAnswers = (List<Map<String, Object>>) wfVars.get(
                            "answers");

                    GregorianCalendar taskExecuted = new GregorianCalendar();
                    taskExecuted.setTime(new Date());

                    for (Map<String, Object> wfAnswer : wfAnswers) {
                        if (wfAnswer.get("taskId").equals(task.getId())) {
                            wfAnswer.put("taskExecuted", taskExecuted);
                            docAnswers.add(wfAnswer);
                        }
                    }

                    // Add node vars answers
                    List<Map<String, Object>> nodeVarAnswers = createNodeVarAnswers(
                            route, node, task, taskExecuted);
                    docAnswers.addAll(nodeVarAnswers);

                    // Add comment answer
                    Map<String, Object> commentAnswer = createCommentAnswer(
                            route, node, task, taskExecuted);
                    docAnswers.add(commentAnswer);

                    // Add selected action
                    Map<String, Object> actionAnswer = createActionAnswer(
                            action, route, node, task, taskExecuted);
                    docAnswers.add(actionAnswer);

                    // We save workflow variables to add executed datetimes
                    route.setVariables(wfVars);
                    route.save(session);

                    // We save the document with the answers
                    targetDoc.setPropertyValue(
                            WorkflowMetadataConstants.WFANS_ANSWERS,
                            (Serializable) docAnswers);
                    session.saveDocument(targetDoc);

                    Events.instance().raiseEvent(
                            WorkflowEventNames.WF_FORMS_ANSWERS_UPDATED);

                    session.save();

                } catch (Exception e) {
                    log.error(
                            "[writeNodeTaskAnswersForDocument] Uncontrolled exception: "
                                    + e.getClass().getName() + ". "
                                    + e.getMessage(),
                            e);
                }
            }

            private List<Map<String, Object>> createNodeVarAnswers(
                    GraphRoute route, GraphNode node, Task task,
                    GregorianCalendar taskExecuted) {

                List<Map<String, Object>> nodeVarAnswers = new ArrayList<Map<String, Object>>();
                Map<String, Serializable> nodeVars = node.getVariables();

                // Sort map by key
                SortedSet<String> keys = new TreeSet<>(nodeVars.keySet());
                int i = 9000;
                for (String key : keys) {
                    // We exclude the comment, action and variables that do not
                    // begin with "wfa_"
                    if (key.equals(
                            WorkflowMetadataConstants.FIXED_QUESTION_ID_COMMENT)
                            || key.equals(
                                    WorkflowMetadataConstants.FIXED_QUESTION_ID_ACTION)
                            || !key.startsWith(NODE_VARS_WFANS_PREFIX)) {
                        continue;
                    }

                    Map<String, Object> nodeVarAnswer = new HashMap<String, Object>();
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID,
                            route.getModelName());
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_NODE_ID,
                            node.getId());
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION_ID,
                            key);
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID,
                            task.getId());
                    Serializable value = nodeVars.get(key);
                    if (value != null) {
                        nodeVarAnswer.put(
                                WorkflowMetadataConstants.WFANS_ANSWERS_VALUE,
                                stringifyNodeVarAnswer(value));
                    }
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED,
                            taskExecuted);
                    nodeVarAnswer.put(
                            WorkflowMetadataConstants.WFANS_ANSWERS_ORDER, i);

                    nodeVarAnswers.add(nodeVarAnswer);

                    i++;
                }

                return nodeVarAnswers;
            }

            private String stringifyNodeVarAnswer(Serializable value) {
                String strValue = "";
                if (value instanceof Serializable[]) {
                    Serializable[] arValue = (Serializable[]) value;
                    for (int i = 0; i < arValue.length; i++) {
                        strValue += arValue[i].toString();
                        if ((i + 1) < arValue.length) {
                            strValue += ", ";
                        }
                    }
                } else {
                    strValue = value.toString();
                }

                return strValue;
            }

            private Map<String, Object> createCommentAnswer(GraphRoute route,
                    GraphNode node, Task task, GregorianCalendar taskExecuted) {

                String comment = getComment(node, task);

                Map<String, Object> commentAnswer = new HashMap<String, Object>();
                commentAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID,
                        route.getModelName());
                commentAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_NODE_ID,
                        node.getId());
                commentAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION_ID,
                        WorkflowMetadataConstants.FIXED_QUESTION_ID_COMMENT);
                commentAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID,
                        task.getId());
                commentAnswer.put(WorkflowMetadataConstants.WFANS_ANSWERS_VALUE,
                        comment);
                commentAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED,
                        taskExecuted);
                commentAnswer.put(WorkflowMetadataConstants.WFANS_ANSWERS_ORDER,
                        "9998");

                return commentAnswer;

            }

            private String getComment(GraphNode node, Task task) {

                Map<String, Serializable> nodeVars = node.getVariables();
                String comment = (String) nodeVars.get(
                        WorkflowMetadataConstants.FIXED_QUESTION_ID_COMMENT);
                if (comment == null) {
                    comment = "";
                    List<TaskComment> taskComments = task.getComments();
                    for (TaskComment tc : taskComments) {
                        if (tc.getText() != null && !tc.getText().isEmpty()) {
                            comment += tc.getText() + " ("
                                    + getDisplayNameFromUsername(tc.getAuthor())
                                    + ") / ";
                        }
                    }
                    if (!comment.isEmpty()) {
                        comment = comment.substring(0, comment.length() - 3);
                    }
                }

                return comment;
            }

            private String getDisplayNameFromUsername(String username) {
                UserNameResolverHelper unr = new UserNameResolverHelper();
                String displayName = username;
                NuxeoPrincipal principal = new NuxeoPrincipalImpl(username);
                if (principal != null) {
                    if (principal.getFirstName() != null
                            && principal.getLastName() != null) {
                        displayName = principal.getFirstName() + " "
                                + principal.getLastName();
                    } else {
                        displayName = unr.getUserFullName(username);
                    }
                }
                return displayName;
            }

            private Map<String, Object> createActionAnswer(String action,
                    GraphRoute route, GraphNode node, Task task,
                    GregorianCalendar taskExecuted) {

                Map<String, Object> actionAnswer = new HashMap<String, Object>();
                actionAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID,
                        route.getModelName());
                actionAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_NODE_ID,
                        node.getId());
                actionAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_QUESTION_ID,
                        WorkflowMetadataConstants.FIXED_QUESTION_ID_ACTION);
                actionAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID,
                        task.getId());
                actionAnswer.put(WorkflowMetadataConstants.WFANS_ANSWERS_VALUE,
                        action);
                actionAnswer.put(
                        WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED,
                        taskExecuted);
                actionAnswer.put(WorkflowMetadataConstants.WFANS_ANSWERS_ORDER,
                        "9999");

                return actionAnswer;

            }

        }.runUnrestricted();

    }

}
