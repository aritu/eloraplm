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

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.webapp.util.ListChoice;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;

/**
 *
 * @author aritu
 *
 */
public interface WorkflowFormsService {

    public static final String NODE_VARS_WFANS_PREFIX = "wfa_";

    public static final String FIELD_TYPE_TEXT = "text";

    public static final String FIELD_TYPE_LINK = "link";

    public static final String FIELD_TYPE_NUMBER = "number";

    public static final String FIELD_TYPE_DATE = "date";

    public static final String FIELD_TYPE_BOOLEAN = "boolean";

    public static final String FIELD_TYPE_LIST = "list";

    DocumentModelList getWorkflowList(CoreSession session)
            throws EloraException;

    Map<String, String> getWorkflowsMap(CoreSession session)
            throws EloraException;

    Map<String, String> getNodesMap(CoreSession session, String workflowId)
            throws EloraException;

    List<String> getFieldTypes();

    void reloadWorkflowsData(CoreSession session) throws EloraException;

    DocumentModelList getQuestionsForWorkflow(CoreSession session, String wfId)
            throws EloraException;

    DocumentModelList getNodeQuestionsForDoc(CoreSession session, String wfId,
            String nodeId, String docId) throws EloraException;

    DocumentModel createQuestion(CoreSession session, String wfId,
            String nodeId, String questionId, boolean isModel, String model,
            String parentDoc, String type, Integer numberMaxIntegerPlaces,
            Integer numberMaxDecimalPlaces, Integer stringMaxLength,
            List<ListChoice> listChoices, String defaultValue,
            List<LocalizedLabel> labels, boolean required, int order,
            boolean obsolete);

    DocumentModel updateQuestion(CoreSession session, DocumentRef docRef,
            String wfId, String nodeId, String questionId, String type,
            Integer numberMaxIntegerPlaces, Integer numberMaxDecimalPlaces,
            Integer stringMaxLength, List<ListChoice> listChoices,
            String defaultValue, List<LocalizedLabel> labels, boolean required,
            int order, boolean obsolete);

    void removeQuestion(CoreSession session, DocumentRef docRef);

    void initializeWorkflowQuestionsForDocument(CoreSession session,
            GraphRoute route, DocumentModel doc) throws EloraException;

    void initializeNodeTaskAnswersForDocument(CoreSession session,
            GraphRoute route, Task task);

    void writeNodeTaskAnswersForDocument(CoreSession session, GraphRoute route,
            GraphNode node, Task task, DocumentModel targetDoc, String action);

    Map<String, DocumentModelList> copyQuestionsForDocument(CoreSession session,
            String wfId, DocumentModel doc) throws EloraException;

}
