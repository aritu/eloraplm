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

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author aritu
 *
 */
@Name("relatedWorkflowActions")
@Scope(ScopeType.EVENT)
public class RelatedWorkflowActionsBean {

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @org.jboss.seam.annotations.Factory(value = "relatedWorkflows")
    public List<DocumentModel> findRelatedWorkflows() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc != null) {
            // Treat proxies
            String currentDocId;
            if (currentDoc.isProxy()) {
                currentDocId = currentDoc.getSourceId();
            } else {
                currentDocId = currentDoc.getId();
            }

            return findRelatedWorkflows(currentDocId);
        }
        return new ArrayList<DocumentModel>();
    }

    public List<DocumentModel> findRelatedWorkflows(String documentId) {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        if (documentId == null || "".equals(documentId)) {
            return docs;
        }
        List<DocumentRoute> relatedRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                documentManager, documentId);
        for (DocumentRoute documentRoute : relatedRoutes) {
            docs.add(documentRoute.getDocument());
        }
        return docs;
    }

    public boolean hasRelatedWorkflow(String documentId) {
        return !findRelatedWorkflows(documentId).isEmpty();
    }

    public boolean hasRelatedWorkflow() {
        return !findRelatedWorkflows().isEmpty();
    }

    public DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }

    public DocumentRoute getRelatedWorkflowById(String workflowId) {
        if (workflowId != null) {
            DocumentModel wfDoc = documentManager.getDocument(
                    new IdRef(workflowId));
            return wfDoc.getAdapter(DocumentRoute.class);
        }
        return null;
    }

}
