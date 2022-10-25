/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package com.aritu.eloraplm.doctypes;

import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.documenttemplates.DocumentTemplatesActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * Implementation for the documentTemplatesBean component available on the
 * session.
 */
@Name("eloraDocumentTemplatesActions")
@Scope(ScopeType.CONVERSATION)
public class EloraDocumentTemplatesActionsBean extends InputController
        implements DocumentTemplatesActions, Serializable {

    private static final Log log = LogFactory.getLog(
            EloraDocumentTemplatesActionsBean.class);

    private static final long serialVersionUID = -4031259222075515590L;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(required = false)
    private transient DocumentActions documentActions;

    @In(required = false)
    private TypesTool typesTool;

    @In(required = false)
    protected DocumentModel changeableDocument;

    @In(required = false, create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    // cached list of templates
    private DocumentModelList templates;

    private String selectedTemplateId;

    private String targetType;

    @Override
    @Factory(value = "eloraAvailableTemplates", scope = ScopeType.EVENT)
    public DocumentModelList templatesListFactory() {
        templates = getTemplates();
        return templates;
    }

    @Override
    public DocumentModelList getTemplates(String targetTypeName) {
        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }

        // Don't allow creating a template based on another template
        DocumentModelList path = navigationContext.getCurrentPath();
        for (DocumentModel parent : path) {
            if (parent.getType().equals(NuxeoDoctypeConstants.TEMPLATE_ROOT)) {
                return null;
            }
        }

        templates = DocCreationFromTemplateHelper.getDocumentTemplates(
                documentManager, targetTypeName);

        return templates;
    }

    private void setDefaultTemplate() {
        // If it is defined in Elora Properties to select the first available
        // template, and there is at least one available template, select the
        // first one by default
        boolean selectFirstAvailableTemplate = false;
        if (targetType.equals(NuxeoDoctypeConstants.FILE)) {
            selectFirstAvailableTemplate = Boolean.valueOf(
                    Framework.getProperty(
                            EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_SELECT_FIRST_AVAILABLE_FILE_TEMPLATE,
                            Boolean.toString(false)));
        } else {
            selectFirstAvailableTemplate = Boolean.valueOf(
                    Framework.getProperty(
                            EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_SELECT_FIRST_AVAILABLE_TEMPLATE,
                            Boolean.toString(false)));
        }

        if (selectFirstAvailableTemplate) {
            if (templates != null && templates.size() > 0) {
                setSelectedTemplateId(templates.get(0).getId());
            } else {
                setSelectedTemplateId(null);
            }
        } else {
            setSelectedTemplateId(null);
        }
    }

    @Override
    public DocumentModelList getTemplates() {
        if (targetType == null || targetType.equals("")) {
            targetType = typesTool.getSelectedType().getId();
        }
        return getTemplates(targetType);
    }

    @Override
    public String createDocumentFromTemplate(DocumentModel doc,
            String templateId) {
        selectedTemplateId = templateId;
        return createDocumentFromTemplate(doc);
    }

    @Override
    public String createDocumentFromTemplate(DocumentModel doc) {
        String logInitMsg = "[createDocumentFromTemplate] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (documentManager == null) {
            log.error("Unable to access documentManager");
            return null;
        }

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // If no template is selected, create the document as usual
            if (selectedTemplateId == null || selectedTemplateId.equals("")) {
                String defaultDoc = createDefaultDocument(doc);
                log.trace(logInitMsg + "--- EXIT --- ");
                return defaultDoc;
            } else { // Otherwise, create the document based on the selected
                     // template

                // Check that selected template is of the correct doctype
                DocumentModel templateDoc = documentManager.getDocument(
                        new IdRef(selectedTemplateId));
                String templateType = templateDoc.getType();
                if (!templateType.equals(doc.getType())) {
                    throw new EloraException(
                            "Trying to create a document from a template of different doctype.");
                }

                DocumentModel created = createDocumentFromSelectedTemplate(doc);

                log.trace(logInitMsg + "--- EXIT --- ");
                return navigationContext.navigateToDocument(created,
                        "after-create");
            }
        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);

            Serializable errorKey = doc.getContextData(
                    EloraGeneralConstants.CONTEXT_ERROR_KEY);
            if (errorKey != null) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(errorKey.toString()));
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.createDocument.error"));
            }

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }

        return null;

    }

    private DocumentModel createDocumentFromSelectedTemplate(DocumentModel doc)
            throws EloraException {
        DocumentRef currentDocRef = navigationContext.getCurrentDocument().getRef();
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String name = pss.generatePathSegment(doc);

        VersionLabelService versionLabelService = Framework.getService(
                VersionLabelService.class);

        DocumentModel created = DocCreationFromTemplateHelper.createDocumentFromTemplate(
                documentManager, currentDocRef, selectedTemplateId, name, doc,
                eloraDocumentRelationManager, versionLabelService, false);

        templates = null;
        selectedTemplateId = null;
        logDocumentWithTitle("Created the document: ", created);
        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_saved"),
                messages.get(created.getType()));

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                navigationContext.getCurrentDocument());
        return created;
    }

    private String createDefaultDocument(DocumentModel doc) {
        if (documentActions != null) {
            return documentActions.saveDocument(doc);
        } else {
            log.error("Unable to find documentActions");
            return null;
        }
    }

    @Override
    public String createDocumentFromTemplate() {
        return createDocumentFromTemplate(changeableDocument);
    }

    @Override
    public String getSelectedTemplateId() {
        return selectedTemplateId;
    }

    @Override
    public void setSelectedTemplateId(String requestedId) {
        selectedTemplateId = requestedId;
    }

    public void setTemplateProperties() {
        setTemplatePropertiesToCurrentDoc(selectedTemplateId);
    }

    private void setTemplatePropertiesToCurrentDoc(String templateId) {
        if (changeableDocument != null && templateId != null) {
            DocumentModel templDoc = documentManager.getDocument(
                    new IdRef(templateId));
            if (templDoc != null) {
                Map<String, List<String>> excludedProperties = new HashMap<String, List<String>>();

                List<String> eloList = new ArrayList<String>();
                eloList.add(EloraMetadataConstants.ELORA_ELO_REFERENCE);
                excludedProperties.put(EloraSchemaConstants.ELORA_OBJECT,
                        eloList);

                List<String> templList = new ArrayList<String>();
                templList.add(EloraMetadataConstants.ELORA_TEMPL_IS_TEMPLATE);
                excludedProperties.put(EloraSchemaConstants.TEMPLATE_INFO,
                        templList);

                List<String> dcList = new ArrayList<String>();
                dcList.add(NuxeoMetadataConstants.NX_DC_CREATED);
                dcList.add(NuxeoMetadataConstants.NX_DC_CREATOR);
                dcList.add(NuxeoMetadataConstants.NX_DC_CONTRIBUTORS);
                dcList.add(NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
                dcList.add(NuxeoMetadataConstants.NX_DC_MODIFIED);
                excludedProperties.put(NuxeoSchemaConstants.DUBLINCORE, dcList);

                EloraDocumentHelper.copyProperties(templDoc, changeableDocument,
                        excludedProperties);
            }
        } else if (changeableDocument != null) {
            DocumentModel newChangeableDocument = documentManager.createDocumentModel(
                    changeableDocument.getType());

            navigationContext.setChangeableDocument(newChangeableDocument);
        }
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(String targetType) {
        this.targetType = targetType;
        getTemplates(this.targetType);
        setDefaultTemplate();
        setTemplatePropertiesToCurrentDoc(selectedTemplateId);
    }

    @Override
    @Observer(value = { DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChildrenChanged() {
        if (templates != null) {
            templates.clear();
        }
    }

    @Override
    @Observer(value = { DOMAIN_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void domainChanged() {
        if (templates != null) {
            templates.clear();
        }
    }

    public boolean isCurrentDocumentUnderTemplateRoot() {
        // TODO: Esto se podría juntar con
        // EloraDocumentHelper.isDocumentUnderTemplateRoot
        DocumentModelList path = navigationContext.getCurrentPath();
        for (DocumentModel parent : path) {
            if (parent.getType().equals(NuxeoDoctypeConstants.TEMPLATE_ROOT)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTemplate() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return isTemplate(currentDocument);
    }

    public boolean isTemplate(DocumentModel doc) {
        return doc != null ? EloraDocumentHelper.isTemplate(doc) : false;
    }

}
