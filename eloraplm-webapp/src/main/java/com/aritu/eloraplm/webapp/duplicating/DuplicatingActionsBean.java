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
package com.aritu.eloraplm.webapp.duplicating;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import com.aritu.eloraplm.core.duplicating.api.DuplicatingService;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
@Name("duplicatingActions")
@Scope(ScopeType.CONVERSATION)
public class DuplicatingActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            DuplicatingActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected Map<String, String> messages;

    @In(required = false)
    protected DocumentModel changeableDocument;

    private DuplicatingService duplService;

    private DocumentModel sourceDoc;

    public DuplicatingActionsBean() {
        duplService = Framework.getService(DuplicatingService.class);
        init();
    }

    public void init() {
        sourceDoc = null;
    }

    public boolean canDocumentBeDuplicated(DocumentModel sourceDoc) {
        try {
            if (sourceDoc == null) {
                throw new EloraException("source doc must not be null");
            }
            return duplService.canDocBeDuplicated(documentManager, sourceDoc);

        } catch (Exception e) {
            return false;
        }
    }

    public String prepareChangeableDocumentAsDuplicateAndRedirect(
            DocumentModel sourceDoc) {

        try {

            prepareChangeableDocumentAsDuplicate(sourceDoc);

            return navigationContext.navigateToDocumentWithView(sourceDoc,
                    "duplicate");

        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.duplicate"));
        }

        return null;
    }

    public void prepareChangeableDocumentAsDuplicate(DocumentModel sourceDoc)
            throws EloraException {

        init();

        if (sourceDoc == null) {
            throw new EloraException("source doc must not be null");
        }

        this.sourceDoc = sourceDoc;

        DocumentModel duplicatedDoc = duplService.prepareDuplicatedDocument(
                documentManager, sourceDoc);
        navigationContext.setChangeableDocument(duplicatedDoc);
    }

    public String createDuplicatedDocument() {
        return createDuplicatedDocument(changeableDocument);
    }

    public String createDuplicatedDocument(DocumentModel duplicatedDoc) {

        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }

        DocumentModel docToRedirect = sourceDoc;

        try {

            docToRedirect = duplService.completeDuplication(documentManager,
                    sourceDoc, duplicatedDoc);

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    documentManager.getParentDocument(sourceDoc.getRef()));

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.duplicate"));

            return navigationContext.navigateToDocument(docToRedirect,
                    "after-create");

        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "label.schema.constraint.violation.documentValidation"),
                    e.getMessage());
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.duplicate"));
        }

        return null;

    }

}
