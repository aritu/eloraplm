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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.annotations.Install.DEPLOYMENT;
import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActionsBean;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;

/**
 * @author aritu
 *
 *         Copied and overwritten from DocumentActionsBean
 *
 */
@Name("documentActions")
@Scope(CONVERSATION)
@Install(precedence = DEPLOYMENT)
public class EloraDocumentActionsBean extends DocumentActionsBean
        implements DocumentActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraDocumentActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @Override
    public String saveDocument() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return saveDocument(changeableDocument);
    }

    @RequestParameter
    protected String parentDocumentPath;

    @Override
    public String saveDocument(DocumentModel newDocument) {
        // Document has already been created if it has an id.
        // This will avoid creation of many documents if user hit create button
        // too many times.
        if (newDocument.getId() != null) {
            log.debug("Document " + newDocument.getName() + " already created");
            return navigationContext.navigateToDocument(newDocument,
                    "after-create");
        }
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentModel parentDoc = null;
        if (parentDocumentPath == null) {
            if (currentDocument == null) {
                // creating item at the root
                parentDoc = documentManager.getRootDocument();
            } else {
                parentDoc = currentDocument;
            }
            parentDocumentPath = parentDoc.getPathAsString();
        } else {
            parentDoc = documentManager.getDocument(
                    new PathRef(parentDocumentPath));
        }

        newDocument.setPathInfo(parentDocumentPath,
                pss.generatePathSegment(newDocument));

        DocumentModel returnDoc;
        try {

            // Copy base viewer file to the viewer file
            if (newDocument.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
                Serializable baseFile = newDocument.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
                newDocument.setPropertyValue(
                        EloraMetadataConstants.ELORA_ELOVWR_FILE, baseFile);
            }

            newDocument = documentManager.createDocument(newDocument);
            returnDoc = newDocument;

            EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                    newDocument, parentDoc);

        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "label.schema.constraint.violation.documentValidation"),
                    e.getMessage());
            return null;
        }
        documentManager.save();

        logDocumentWithTitle("Created the document: ", newDocument);
        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_saved"),
                messages.get(newDocument.getType()));

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
        return navigationContext.navigateToDocument(returnDoc, "after-create");
    }

    @Override
    public String updateDocument(DocumentModel doc,
            Boolean restoreCurrentTabs) {

        // If viewer base file changes, we must apply the change to the viewer
        // file too.
        if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER) && doc.getProperty(
                EloraMetadataConstants.ELORA_ELOVWR_BASEFILE).isDirty()) {
            Serializable baseFile = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
            doc.setPropertyValue(EloraMetadataConstants.ELORA_ELOVWR_FILE,
                    baseFile);
        }

        return super.updateDocument(doc, restoreCurrentTabs);
    }

}
