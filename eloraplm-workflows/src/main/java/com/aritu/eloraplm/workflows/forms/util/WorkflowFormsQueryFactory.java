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
package com.aritu.eloraplm.workflows.forms.util;

import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.WorkflowDoctypeConstants;
import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class WorkflowFormsQueryFactory {

    // WORKFLOWS

    public static String getWorkflowsDefinedByForms() throws EloraException {

        String query = String.format(
                "SELECT * FROM " + WorkflowDoctypeConstants.WF_DOCUMENT_ROUTE
                        + " WHERE " + NXQL.ECM_PATH + " STARTSWITH '%s'"
                        + " AND " + NXQL.ECM_MIXINTYPE + " = '%s'",
                "/" + DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_ID,
                EloraFacetConstants.FACET_DEFINED_BY_WORKFLOW_FORMS);

        return query;
    }

    // MODEL QUESTIONS

    public static String getWorkflowModelQuestions() throws EloraException {

        String query = String.format("SELECT * FROM "
                + WorkflowDoctypeConstants.WF_FORMS_QUESTION + " WHERE "
                + WorkflowMetadataConstants.WFQ_IS_MODEL + " = 1 " + "ORDER BY "
                + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + ", "
                + WorkflowMetadataConstants.WFQ_NODE_ID + ", "
                + WorkflowMetadataConstants.WFQ_ORDER);

        return query;
    }

    public static String getWorkflowQuestionsForDoc(String wfId, String docId)
            throws EloraException {

        String query = String.format("SELECT * FROM "
                + WorkflowDoctypeConstants.WF_FORMS_QUESTION + " WHERE "
                + WorkflowMetadataConstants.WFQ_IS_MODEL + " = 0 AND "
                + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + " = '%s' AND "
                + WorkflowMetadataConstants.WFQ_PARENT_DOC + " = '%s' "
                + "ORDER BY " + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + ","
                + WorkflowMetadataConstants.WFQ_NODE_ID + ","
                + WorkflowMetadataConstants.WFQ_ORDER, wfId, docId);

        return query;
    }

    public static String getDistinctAnsweredWorkflowIdsForDoc(String docId)
            throws EloraException {

        String pfx = WorkflowMetadataConstants.WFANS_ANSWERS + "/*1/";

        String query = String.format(
                "SELECT DISTINCT " + pfx
                        + WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID
                        + " FROM Document WHERE " + NXQL.ECM_UUID + " = '%s'",
                docId);

        return query;
    }

    public static String getDistinctAnsweredTaskIdsForDoc(String docId,
            String wfId) throws EloraException {

        String pfx = WorkflowMetadataConstants.WFANS_ANSWERS + "/*1/";

        String query = String.format("SELECT DISTINCT " + pfx
                + WorkflowMetadataConstants.WFANS_ANSWERS_TASK_ID + ", " + pfx
                + WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED
                + " FROM Document WHERE " + NXQL.ECM_UUID + " = '%s'" + " AND "
                + pfx + WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID
                + " = '%s' ORDER BY " + pfx
                + WorkflowMetadataConstants.WFANS_ANSWERS_TASK_EXECUTED, docId,
                wfId);

        return query;
    }

    public static String getModelQuestionsForWorkflow(String wfId)
            throws EloraException {

        String query = String.format("SELECT * FROM "
                + WorkflowDoctypeConstants.WF_FORMS_QUESTION + " WHERE "
                + WorkflowMetadataConstants.WFQ_IS_MODEL + " = 1 AND "
                + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + " = '%s' AND "
                + WorkflowMetadataConstants.WFQ_OBSOLETE + " = 0 " + "ORDER BY "
                + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + ","
                + WorkflowMetadataConstants.WFQ_NODE_ID + ","
                + WorkflowMetadataConstants.WFQ_ORDER, wfId);

        return query;
    }

    // DOC QUESTIONS

    public static String getNodeQuestionsForDoc(String wfId, String nodeId,
            String docId) throws EloraException {

        String query = String.format("SELECT * FROM "
                + WorkflowDoctypeConstants.WF_FORMS_QUESTION + " WHERE "
                + WorkflowMetadataConstants.WFQ_IS_MODEL + " = 0 AND "
                + WorkflowMetadataConstants.WFQ_WORKFLOW_ID + " = '%s' AND "
                + WorkflowMetadataConstants.WFQ_NODE_ID + " = '%s' AND "
                + WorkflowMetadataConstants.WFQ_PARENT_DOC + " = '%s' "
                + "ORDER BY " + WorkflowMetadataConstants.WFQ_ORDER, wfId,
                nodeId, docId);

        return query;
    }

}
