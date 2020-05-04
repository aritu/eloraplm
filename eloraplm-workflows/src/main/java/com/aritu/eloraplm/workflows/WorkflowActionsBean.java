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
package com.aritu.eloraplm.workflows;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement.ElementLifeCycleState;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.event.SelectEvent;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * @author aritu
 *
 */
@Name("workflowActions")
@Scope(CONVERSATION)
public class WorkflowActionsBean extends InputController
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkflowActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected Map<String, String> messages;

    private String storedDocId;

    private List<DocumentModel> closedRelatedRoutes;

    private DocumentModel selectedWorkflow;

    public DocumentModel getSelectedWorkflow() {
        if (selectedWorkflow == null) {
            String docId = storedDocId != null ? storedDocId
                    : navigationContext.getCurrentDocument().getId();
            if (hasClosedRelatedRoutes(docId)) {
                selectedWorkflow = findClosedRelatedRoutes(docId).get(0);
            }
        }
        return selectedWorkflow;
    }

    public String getTaskStatus(String taskDocId) {

        String status = "";
        if (taskDocId != null) {
            String query = EloraQueryFactory.getTaskStatusByTaskDocId(
                    taskDocId);

            List<String> results = EloraQueryHelper.executeQueryAndGetResultStringList(
                    NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                            + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_STATUS,
                    query, documentManager);

            if (!results.isEmpty()) {
                if (results.size() > 1) {
                    log.error(
                            "More than one route node found while getting task status for taskDocId |"
                                    + taskDocId
                                    + "|. Status could not be obtained.");
                }

                status = results.get(0);
            }
        }

        return status;
    }

    public List<DocumentModel> findClosedRelatedRoutes(String documentId) {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (storedDocId == null || currentDoc.getId() != storedDocId) {
            List<DocumentModel> docs = new ArrayList<DocumentModel>();
            if (currentDoc == null || "".equals(documentId)) {
                return docs;
            }
            List<DocumentRouteElement.ElementLifeCycleState> closedStates = new ArrayList<DocumentRouteElement.ElementLifeCycleState>();
            closedStates.add(ElementLifeCycleState.canceled);
            closedStates.add(ElementLifeCycleState.done);

            List<DocumentRoute> documentRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                    documentManager, documentId, closedStates);
            for (DocumentRoute documentRoute : documentRoutes) {
                docs.add(0, documentRoute.getDocument());
            }
            closedRelatedRoutes = docs;

        }
        return closedRelatedRoutes;

    }

    public boolean hasClosedRelatedRoutes(String documentId) {
        return !findClosedRelatedRoutes(documentId).isEmpty();
    }

    public DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }

    public void onRowSelect(SelectEvent event) {
        selectedWorkflow = (DocumentModel) event.getObject();
    }

}
