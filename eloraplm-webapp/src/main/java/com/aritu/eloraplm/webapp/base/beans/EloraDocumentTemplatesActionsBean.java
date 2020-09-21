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

package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.documenttemplates.DocumentTemplatesActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.aritu.eloraplm.codecreation.util.CodeCreationHelper;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Implementation for the documentTemplatesBean component available on the
 * session.
 */
@Name("eloraDocumentTemplatesActions")
@Scope(CONVERSATION)
public class EloraDocumentTemplatesActionsBean extends InputController
        implements DocumentTemplatesActions, Serializable {

    private static final Log log = LogFactory.getLog(
            EloraDocumentTemplatesActionsBean.class);

    private static final long serialVersionUID = -4031259222075515590L;

    private static final String RESET_CREATOR_PROPERTY = "nuxeo.template.reset-creator-on-creation-from-template";

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

    // cached list of templates
    private DocumentModelList templates;

    private String selectedTemplateId;

    private String targetType;
    // private String targetType = "Workspace";

    @Override
    @Factory(value = "eloraAvailableTemplates", scope = EVENT)
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

        String query = "SELECT * FROM Document where ecm:primaryType = '%s' AND ecm:path STARTSWITH %s";
        DocumentModelList tl = documentManager.query(String.format(query,
                NuxeoDoctypeConstants.TEMPLATE_ROOT,
                NXQL.escapeString(navigationContext.getCurrentDomainPath())));

        if (tl.isEmpty()) {
            templates = tl;
        } else {

            // Get all document templates ordered by ascending title
            Map<String, String> order = new HashMap<String, String>();
            order.put("title", "asc");
            DocumentModelComparator dmc = new DocumentModelComparator(
                    "dublincore", order);
            templates = documentManager.getChildren(tl.get(0).getRef(),
                    targetTypeName, null, dmc);

            List<DocumentModel> deleted = new ArrayList<DocumentModel>();
            for (Iterator<DocumentModel> it = templates.iterator(); it.hasNext();) {
                DocumentModel current = it.next();
                if (LifeCycleConstants.DELETED_STATE.equals(
                        current.getCurrentLifeCycleState())) {
                    deleted.add(current);
                }
            }
            templates.removeAll(deleted);
        }

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
            if ((selectedTemplateId == null || selectedTemplateId.equals(""))
                    && templates.size() > 0) {
                setSelectedTemplateId(templates.get(0).getId());
            }
        }

        return templates;
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

        // If no template is selected, create the document as usual
        if (selectedTemplateId == null || selectedTemplateId.equals("")) {
            if (documentActions != null) {
                return documentActions.saveDocument(doc);
            } else {
                log.error("Unable to find documentActions");
                return null;
            }
        }

        // Otherwise, create the document based on the selected template
        DocumentRef currentDocRef = navigationContext.getCurrentDocument().getRef();

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String name = pss.generatePathSegment(doc);
        CopyOption opt = null;
        if (Framework.getService(
                ConfigurationService.class).isBooleanPropertyTrue(
                        RESET_CREATOR_PROPERTY)) {
            opt = CopyOption.RESET_CREATOR;
        }
        DocumentModel created = documentManager.copy(
                new IdRef(selectedTemplateId), currentDocRef, name, opt,
                CopyOption.RESET_LIFE_CYCLE);

        // Update from user input.
        EloraDocumentHelper.copyProperties(doc, created, true);

        // ----------------- AUTOMATIC CODE CREATION
        // If the document itself has automatic code creation facet and it has
        // not any reference yet, create and store the reference value
        String reference = generateAutomaticCodeIfRequired(created);
        if (reference != null && reference.length() > 0) {
            created.setPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE,
                    reference);
        }

        // Iterate over its children in order to set automatic reference if
        // required
        updateChildrenProperties(created);
        // --------------------------------------------------

        // If required, update the main content file name
        updateMainContentFileNameIfRequired(created);

        created = documentManager.saveDocument(created);
        documentManager.save();

        selectedTemplateId = "";

        logDocumentWithTitle("Created the document: ", created);
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("document_saved"),
                resourcesAccessor.getMessages().get(created.getType()));
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);

        // ----------------- APPEND REFERENCE TO CHILDREN TITLE
        // If it is defined in Elora Properties to append the document reference
        // to children title, and reference is set, do it
        if (Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_APPEND_REFERENCE_TO_CHILDREN_TITLE,
                Boolean.toString(false)))) {

            String currentReference = getDocumenReference(created);
            if (currentReference != null && currentReference.length() > 0) {
                appendReferenceToChildrenTitle(created, currentReference);
            }
        }

        // If required, update children's main content file name
        updateMainContentFileNameIfRequiredToChildren(created);

        log.trace(logInitMsg + "--- EXIT --- ");

        return navigationContext.navigateToDocument(created, "after-create");
    }

    private String generateAutomaticCodeIfRequired(DocumentModel doc) {
        String logInitMsg = "[generateAutomaticCodeIfRequired] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        String generatedReference = null;

        // Check if automatic code must be generated for this document
        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
            String currentReference = null;
            Serializable docReferencePropertyValue = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (docReferencePropertyValue != null) {
                currentReference = docReferencePropertyValue.toString();
            }

            log.trace(logInitMsg + "Current reference: currentReference = |"
                    + currentReference + "|");

            boolean generateAutomaticCode = CodeCreationHelper.isGenerateAutomaticCode(
                    doc, currentReference);

            log.trace(logInitMsg + "generateAutomaticCode = |"
                    + generateAutomaticCode + "|");

            // If automatic code must be generated, generate it
            if (generateAutomaticCode) {
                try {
                    generatedReference = CodeCreationHelper.createCode(doc,
                            documentManager.getPrincipal().getName());

                    log.trace(logInitMsg
                            + "Generated reference: generatedReference = |"
                            + generatedReference + "|");

                } catch (EloraException e) {
                    log.error(logInitMsg
                            + "Exception when generating automatic document reference: "
                            + e.getMessage(), e);
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- with ");

        return generatedReference;
    }

    /**
     * This method iterates over the document children and set their properties
     * if required. For each children: if it has automatic code, it generates
     * its code. Mark not modified documents as dirty, in order to force to
     * modify its creator and creation date.
     *
     * @param parentDocument
     */
    private void updateChildrenProperties(DocumentModel parentDocument) {
        String logInitMsg = "[updateChildrenProperties] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Iterate over its children
        DocumentModelIterator it = documentManager.getChildrenIterator(
                parentDocument.getRef());
        for (DocumentModel childDoc : it) {

            // Create its automatic code if required
            String reference = generateAutomaticCodeIfRequired(childDoc);
            if (reference != null && reference.length() > 0) {
                childDoc.setPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE, reference);
            }

            // In order to modify creationDate and creator, we should mark the
            // document as dirty, if it is not already be modified before.
            if (!childDoc.isDirty()) {
                EloraDocumentHelper.setDocumentDirty(childDoc);
            }

            // Save the document
            documentManager.saveDocument(childDoc);

            // Check if it has to be moved to its position in Elora Structure.
            // If yes, move it to the Elora Structure and create a Proxy
            // pointing to the moved document.
            if (EloraStructureHelper.hasToBeMovedToEloraStructure(childDoc,
                    parentDocument)) {
                EloraStructureHelper.moveToEloraStructureAndCreateProxy(
                        childDoc);
            }

            // Recursively call its children
            updateChildrenProperties(childDoc);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private String getDocumenReference(DocumentModel doc) {
        String reference = null;
        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
            Serializable referencePropertyValue = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (referencePropertyValue != null) {
                reference = referencePropertyValue.toString();
            }
        }
        return reference;
    }

    private String getDocumentTitle(DocumentModel doc) {
        String title = null;
        if (doc.hasSchema(NuxeoSchemaConstants.DUBLINCORE)) {
            Serializable titlePropertyValue = doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_TITLE);
            if (titlePropertyValue != null) {
                title = titlePropertyValue.toString();
            }
        }
        return title;
    }

    private Blob getDocumentMainFile(DocumentModel doc) {
        Blob blob = null;
        if (doc.hasSchema(NuxeoSchemaConstants.MAIN_BLOB_SCHEMA)) {
            Serializable blobPropertyValue = doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_FILE_CONTENT);
            if (blobPropertyValue != null) {
                blob = (Blob) blobPropertyValue;
            }
        }
        return blob;
    }

    private void appendReferenceToChildrenTitle(DocumentModel parentDocument,
            String reference) {
        String logInitMsg = "[appendReferenceToChildrenTitle] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        if (reference != null && reference.length() > 0) {

            // Iterate over its children
            DocumentModelIterator it = documentManager.getChildrenIterator(
                    parentDocument.getRef());

            // For the instance reference is added only to Base Document types.
            // This cannot be retrieved directly using BasicDocument face, so we
            // check directly the types we are interested on.
            for (DocumentModel childrenDoc : it) {
                if ((childrenDoc.getType().equals(NuxeoDoctypeConstants.FILE)
                        || childrenDoc.getType().equals(
                                NuxeoDoctypeConstants.NOTE)
                        || childrenDoc.getType().equals(
                                NuxeoDoctypeConstants.AUDIO)
                        || childrenDoc.getType().equals(
                                NuxeoDoctypeConstants.PICTURE)
                        || childrenDoc.getType().equals(
                                NuxeoDoctypeConstants.VIDEO))
                        && childrenDoc.hasSchema(
                                NuxeoSchemaConstants.DUBLINCORE)) {
                    String title = getDocumentTitle(childrenDoc);
                    if (title != null) {
                        if (title.length() > 0) {
                            title += " ";
                        }
                        title += reference;
                    }
                    childrenDoc.setPropertyValue(
                            NuxeoMetadataConstants.NX_DC_TITLE, title);
                    documentManager.saveDocument(childrenDoc);
                }

                // Recursively call its children
                appendReferenceToChildrenTitle(childrenDoc, reference);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");

    }

    private void updateMainContentFileNameIfRequired(DocumentModel document) {
        String logInitMsg = "[updateMainContentFileNameIfRequired] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        if (Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_RENAME_MAIN_FILE,
                Boolean.toString(false)))) {

            // check if it has a main file attached
            Blob mainFileBlob = getDocumentMainFile(document);

            if (mainFileBlob != null) {
                String newFilename = "";
                String currentReference = getDocumenReference(document);
                String currentTitle = getDocumentTitle(document);
                if (currentReference != null) {
                    newFilename = currentReference + " " + newFilename;
                }
                if (currentTitle != null) {
                    newFilename = newFilename + " " + currentTitle;
                }
                newFilename += "." + FilenameUtils.getExtension(
                        mainFileBlob.getFilename());

                mainFileBlob.setFilename(newFilename);
                document.setPropertyValue(
                        NuxeoMetadataConstants.NX_FILE_CONTENT,
                        (Serializable) mainFileBlob);
                log.trace(logInitMsg + "filename updated. newFilename = |"
                        + newFilename + "|");

                documentManager.saveDocument(document);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void updateMainContentFileNameIfRequiredToChildren(
            DocumentModel parentDocument) {
        String logInitMsg = "[updateMainContentFileNameIfRequiredToChildren] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Iterate over its children
        DocumentModelIterator it = documentManager.getChildrenIterator(
                parentDocument.getRef());

        //
        for (DocumentModel childrenDoc : it) {

            updateMainContentFileNameIfRequired(childrenDoc);

            updateMainContentFileNameIfRequiredToChildren(childrenDoc);
        }

        log.trace(logInitMsg + "--- EXIT --- ");

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

    @Override
    public String getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(String targetType) {

        if (!targetType.equals(this.targetType)) {
            setSelectedTemplateId(null);
        }

        this.targetType = targetType;

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

    public boolean isDocumentTemplateRootOrUnderTemplateRoot(
            DocumentModel doc) {
        String logInitMsg = "[isDocumentTemplateRootOrUnderTemplateRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        boolean result = false;

        if (doc.getType().equals(NuxeoDoctypeConstants.TEMPLATE_ROOT)) {
            result = true;
        } else {
            result = EloraDocumentHelper.isDocumentUnderTemplateRoot(doc,
                    documentManager);
        }

        log.trace(logInitMsg + "--- EXIT with result = |" + result + "|---");

        return result;
    }

}
