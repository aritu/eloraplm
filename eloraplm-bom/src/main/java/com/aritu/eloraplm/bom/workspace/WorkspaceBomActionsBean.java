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
package com.aritu.eloraplm.bom.workspace;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;

/**
 * @author aritu
 *
 */

@Name("wsBomActions")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
public class WorkspaceBomActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public static final String TARGET_DOC = "DOC";

    public static final String TARGET_BOM = "BOM";

    public void createRelatedBomProxies() {

        List<DocumentModel> docsList = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        DocumentModelList relatedDocs = new DocumentModelListImpl();

        for (DocumentModel doc : docsList) {
            if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                    || doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
                relatedDocs.addAll(processRelatedDoc(doc, TARGET_BOM));
            }
        }

        if (!relatedDocs.isEmpty()) {
            createProxies(relatedDocs);
        }

        facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                "eloraplm.message.success.bom.createRelatedBomProxies"));

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                navigationContext.getCurrentDocument());
    }

    public void createRelatedDocProxies() {

        List<DocumentModel> docsList = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        DocumentModelList relatedDocs = new DocumentModelListImpl();

        for (DocumentModel doc : docsList) {
            if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                relatedDocs.addAll(processRelatedDoc(doc, TARGET_DOC));
            }
        }

        if (!relatedDocs.isEmpty()) {
            createProxies(relatedDocs);
        }

        facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                "eloraplm.message.success.bom.createRelatedDocProxies"));

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                navigationContext.getCurrentDocument());
    }

    private DocumentModelList processRelatedDoc(DocumentModel doc,
            String targetType) {

        // If it is a proxy, we need to get the source before checking the
        // relations
        if (doc.isProxy()) {
            doc = documentManager.getSourceDocument(doc.getRef());
        }

        List<Resource> predicates = new ArrayList<Resource>();
        predicates.add(new QNameResourceImpl(
                EloraRelationConstants.BOM_HAS_DOCUMENT, ""));
        predicates.add(new QNameResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT, ""));

        DocumentModelList relatedDocuments = new DocumentModelListImpl();

        for (Resource predicate : predicates) {
            if (targetType.equals(TARGET_DOC)) {
                relatedDocuments.addAll(
                        RelationHelper.getObjectDocuments(doc, predicate));
            } else {
                relatedDocuments.addAll(
                        RelationHelper.getSubjectDocuments(predicate, doc));
            }
        }

        return relatedDocuments;
    }

    private void createProxies(DocumentModelList relatedDocs) {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        for (DocumentModel doc : relatedDocs) {
            boolean createProxy = true;
            DocumentModelList proxies = documentManager.getProxies(doc.getRef(),
                    currentDoc.getRef());
            if (!proxies.isEmpty()) {
                for (DocumentModel proxy : proxies) {
                    if (proxy.getSourceId() == doc.getId()) {
                        createProxy = false;
                        break;
                    }
                }
            }

            if (createProxy) {
                documentManager.createProxy(doc.getRef(),
                        navigationContext.getCurrentDocument().getRef());
            }
        }

    }

}
