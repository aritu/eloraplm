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
package com.aritu.eloraplm.constants;

/**
 * // This class contains the constants related to the Metadata of Elora
 * Obsolete Management
 *
 * @author aritu
 *
 */
public class WorkflowMetadataConstants {

    /* Nuxeo Workflows */

    public static final String WF_DOCRI_MODEL_ID = "docri:modelId";

    public static final String WF_RNODE_NODE_ID = "rnode:nodeId";

    public static final String WF_RNODE_HAS_TASK = "rnode:hasTask";

    /* Workflow Question */

    public static final String WFQ_WORKFLOW_ID = "wfq:workflowId";

    public static final String WFQ_NODE_ID = "wfq:nodeId";

    public static final String WFQ_QUESTION_ID = "wfq:questionId";

    public static final String WFQ_IS_MODEL = "wfq:isModel";

    public static final String WFQ_MODEL = "wfq:model";

    public static final String WFQ_PARENT_DOC = "wfq:parentDoc";

    public static final String WFQ_TYPE = "wfq:type";

    public static final String WFQ_NUMBER_MAX_INTEGER_PLACES = "wfq:numberMaxIntegerPlaces";

    public static final String WFQ_NUMBER_MAX_DECIMAL_PLACES = "wfq:numberMaxDecimalPlaces";

    public static final String WFQ_STRING_MAX_LENGTH = "wfq:stringMaxLength";

    public static final String WFQ_LIST_CHOICES = "wfq:listChoices";

    public static final String WFQ_DEFAULT_VALUE = "wfq:defaultValue";

    public static final String WFQ_LABELS = "wfq:labels";

    public static final String WFQ_REQUIRED = "wfq:required";

    public static final String WFQ_ORDER = "wfq:order";

    public static final String WFQ_OBSOLETE = "wfq:obsolete";

    /* Workflow Answer */

    public static final String WFANS_ANSWERS = "wfans:answers";

    public static final String WFANS_ANSWERS_WORKFLOW_ID = "workflowId";

    public static final String WFANS_ANSWERS_NODE_ID = "nodeId";

    public static final String WFANS_ANSWERS_QUESTION_ID = "questionId";

    public static final String WFANS_ANSWERS_TASK_ID = "taskId";

    public static final String WFANS_ANSWERS_TASK_EXECUTED = "taskExecuted";

    public static final String WFANS_ANSWERS_QUESTION = "question";

    public static final String WFANS_ANSWERS_VALUE = "value";

    public static final String WFANS_ANSWERS_ORDER = "order";

    /* Schema prefixes */

    public static final String PREFIX_WFQ = "wfq:";

    /* Special questions */

    public static final String FIXED_QUESTION_ID_COMMENT = "comment";

    public static final String FIXED_QUESTION_ID_ACTION = "action";

}
