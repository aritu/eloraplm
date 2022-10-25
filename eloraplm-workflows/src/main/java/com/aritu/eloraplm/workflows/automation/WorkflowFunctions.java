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
package com.aritu.eloraplm.workflows.automation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.PlatformFunctions;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.TaskInfo;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class WorkflowFunctions extends CoreFunctions {

    public StringList getPreviousActorEmails(Map<String, Serializable> ctx) {
        Set<String> emails = new HashSet<String>();

        PlatformFunctions pf = new PlatformFunctions();

        try (CoreSession session = CoreInstance.openCoreSession("default")) {

            String wfInstanceId = (String) ctx.get("workflowInstanceId");
            DocumentModel wfInstanceDoc = session.getDocument(
                    new IdRef(wfInstanceId));
            GraphRoute route = wfInstanceDoc.getAdapter(GraphRoute.class);

            for (GraphNode node : route.getNodes()) {
                for (TaskInfo taskInfo : node.getEndedTasksInfo()) {
                    String actor = taskInfo.getActor();
                    emails.add(pf.getEmail(actor));
                }
            }

        }

        return new StringList(emails);
    }

    public Object getTaskInfoValue(TaskInfo taskInfo, String property) {

        if (taskInfo != null) {
            switch (property) {
            case "status":
                return taskInfo.getStatus();
            case "actor":
                return taskInfo.getActor();
            case "comment":
                return taskInfo.getComment();
            case "taskDocId":
                return taskInfo.getTaskDocId();
            case "node":
                return taskInfo.getNode();
            }
        }

        return null;
    }

    public String concatenate(String... values) {

        String text = "";

        for (String value : values) {
            if (value == null) {
                continue;
            }

            text += value;
        }

        return text;
    }

    /* -------------------------------
     * WORKFLOW FORMS
     * -------------------------------
     */

    public String getAnswerValueOnNode(Map<String, Serializable> ctx,
            String nodeId, String questionId) throws EloraException {
        List<String> values = getAllAnswerValues(ctx, nodeId, questionId,
                false);
        if (!values.isEmpty()) {
            if (values.size() > 1) {
                throw new EloraException(
                        "More than one value for the question in specified node. Probably it is a multitask node or question id is duplicated.");
            }
            return values.get(0);
        } else {
            return null;
        }
    }

    public String getAnswerValueOnCurrentTask(Map<String, Serializable> ctx,
            String questionId) throws EloraException {
        List<String> values = getAllAnswerValues(ctx, null, questionId, true);
        if (!values.isEmpty()) {
            if (values.size() > 1) {
                throw new EloraException(
                        "More than one value for the question in current task. Probably it is a multitask node or question id is duplicated.");
            }
            return values.get(0);
        } else {
            return null;
        }
    }

    public List<String> getAnswerValuesOnCurrentMultiTask(
            Map<String, Serializable> ctx, String questionId)
            throws EloraException {
        return getAllAnswerValues(ctx, null, questionId, true);
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllAnswerValues(Map<String, Serializable> ctx,
            String nodeId, String questionId, boolean onlyCurrent)
            throws EloraException {

        if (ctx == null) {
            throw new IllegalArgumentException("no context");
        }

        if (questionId == null) {
            throw new IllegalArgumentException(
                    "questionId parameter must not be null");
        }

        Map<String, Serializable> wfVars = (Map<String, Serializable>) ctx.get(
                "WorkflowVariables");

        List<Map<String, String>> answers = (List<Map<String, String>>) wfVars.get(
                "answers");

        // If no nodeId specified, use current node
        if (nodeId == null) {
            nodeId = (String) ctx.get("nodeId");
        }

        List<String> values = new ArrayList<String>();

        for (Map<String, String> answer : answers) {
            if (answer.get("nodeId").equals(nodeId)
                    && answer.get("questionId").equals(questionId)) {
                if (!onlyCurrent || answer.get("taskExecuted") == null) {
                    values.add(answer.get("value"));
                }
            }
        }

        return values;
    }

}
