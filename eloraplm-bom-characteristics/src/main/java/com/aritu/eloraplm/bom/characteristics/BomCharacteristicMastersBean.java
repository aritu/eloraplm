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
package com.aritu.eloraplm.bom.characteristics;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.admin.AdminViewManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.action.DeleteActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicMastersHelper;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicMastersQueryFactory;
import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * BOM Characteristic Masters bean
 *
 * @author aritu
 *
 */
@Name("bomCharacteristicMasters")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
public class BomCharacteristicMastersBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicMastersBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    @In(create = true)
    protected AdminViewManager adminViews;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient DeleteActions deleteActions;

    protected DocumentModel changeableDocument;

    protected String changeableDocumentAction;

    protected DocumentModel parentFolder;

    protected String parentFolderPathErrorMsg;

    public DocumentModel getChangeableDocument() {
        String currentAction = adminViews.getCurrentSubViewId();

        if (changeableDocumentAction != null
                && changeableDocumentAction.equalsIgnoreCase(currentAction)) {

            return changeableDocument;

        } else {
            createChangeableDocument();
        }

        return changeableDocument;
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        this.changeableDocument = changeableDocument;
    }

    public void resetChangeableDocument() {
        changeableDocument = null;
        changeableDocumentAction = null;
    }

    public String getChangeableDocumentAction() {
        return changeableDocumentAction;
    }

    public void setChangeableDocumentAction(String changeableDocumentAction) {
        this.changeableDocumentAction = changeableDocumentAction;
    }

    protected void createChangeableDocument() {

        String logInitMsg = "[createChangeableDocument] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- currentSubViewId =|"
                + adminViews.getCurrentSubViewId() + "|");

        try {
            String parentFolderPath = getParentFolderPath();

            if (parentFolderPath != null && !parentFolderPath.isEmpty()) {

                String currentAction = adminViews.getCurrentSubViewId();

                String bomCharacDocType = BomCharacteristicMastersHelper.getBomCharacteristicMasterDocTypeForAction(
                        currentAction);

                DocumentModel changeableDocument = documentManager.createDocumentModel(
                        parentFolderPath, bomCharacDocType, bomCharacDocType);

                setChangeableDocument(changeableDocument);

                setChangeableDocumentAction(currentAction);

            } else {
                log.error(logInitMsg + "parentFolder should have a value");
                facesMessages.add(StatusMessage.Severity.ERROR,
                        "message.error.bomcharac.missingParentFolder");
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public String getParentFolderPath() {

        String logInitMsg = "[getParentFolderPath] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (parentFolder == null) {

            try {

                retrieveParentFolder(documentManager);

            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage());
                setParentFolderPathErrorMsg(e.getMessage());
                return null;
            }
        }

        return parentFolder.getPathAsString();
    }

    public String getParentFolderPathErrorMsg() {
        return parentFolderPathErrorMsg;
    }

    public void setParentFolderPathErrorMsg(String parentFolderPathErrorMsg) {
        this.parentFolderPathErrorMsg = parentFolderPathErrorMsg;
    }

    protected void retrieveParentFolder(CoreSession session)
            throws EloraException {

        String logInitMsg = "[retrieveParentFolder] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            String query = BomCharacteristicMastersQueryFactory.getBomCharacteristicMastersFolderQuery();
            DocumentModelList res = session.query(query);

            if (res == null || res.isEmpty()) {
                String errorMsg = BomCharacteristicsConstants.BOM_CHARAC_MASTER_FOLDER_DOCUMENT_TYPE
                        + " is missing in the system.";
                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);

            } else if (res.size() > 1) {
                String errorMsg = "More han one "
                        + BomCharacteristicsConstants.BOM_CHARAC_MASTER_FOLDER_DOCUMENT_TYPE
                        + " found:";
                for (DocumentModel model : res) {
                    errorMsg += " - " + model.getName() + ", "
                            + model.getPathAsString();
                }

                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);

            } else {
                parentFolder = res.get(0);

                if (parentFolder != null) {

                    log.trace(logInitMsg + "parentFolder = |"
                            + parentFolder.getId() + "|");

                } else {
                    String errorMsg = BomCharacteristicsConstants.BOM_CHARAC_MASTER_FOLDER_DOCUMENT_TYPE
                            + " is null";
                    log.error(logInitMsg + errorMsg);
                    throw new EloraException(errorMsg);
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        log.trace(logInitMsg + "--- EXIT ---");

    }

    public void saveBomCharacteristicMaster() {
        String logInitMsg = "[saveBomCharacteristicMaster] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            DocumentModel changeableDocument = getChangeableDocument();

            if (changeableDocument.getId() != null) {
                log.debug("Document " + changeableDocument.getName()
                        + " already created");
            }

            changeableDocument = documentManager.createDocument(
                    changeableDocument);

            setChangeableDocument(changeableDocument);

            documentManager.save();

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("document_saved"),
                    messages.get(changeableDocument.getType()));

            resetChangeableDocument();

            // refresh the content view (in function of the type)
            String currentAction = adminViews.getCurrentSubViewId();
            String bomCharacContentView = BomCharacteristicMastersHelper.getBomCharacteristicMasterContentViewForAction(
                    currentAction);
            contentViewActions.refresh(bomCharacContentView);

        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "label.schema.constraint.violation.documentValidation"),
                    e.getMessage());
            log.error(logInitMsg + " ****************** ERROR: "
                    + e.getMessage());
            // return null;
        } catch (NuxeoException e2) {

            facesMessages.add(StatusMessage.Severity.ERROR,
                    "Nuxeo error: " + e2.getMessage());

            log.error(logInitMsg + " ****************** ERROR: "
                    + e2.getMessage());
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public void resetTypeRelatedFields(DocumentModel editedDoc) {
        String logInitMsg = "[resetTypeRelatedFields] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_MAX_INTEGER_PLACES,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_MAX_DECIMAL_PLACES,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_STRING_MAX_LENGTH,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_STRING_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_DATE_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_BOOLEAN_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_LIST_CONTENT,
                new ArrayList<HashMap<String, Object>>());
        editedDoc.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_LIST_DEFAULT_VALUE,
                "");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public String updateBomCharacteristicMaster() {
        String logInitMsg = "[updateBomCharacteristicMaster] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        try {
            currentDocument = documentManager.saveDocument(currentDocument);
        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "label.schema.constraint.violation.documentValidation"),
                    e.getMessage());
            return null;
        }

        documentManager.save();

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_modified"),
                messages.get(currentDocument.getType()));

        // refresh the content view (in function of the type)
        String currentAction = adminViews.getCurrentSubViewId();
        String bomCharacContentView = BomCharacteristicMastersHelper.getBomCharacteristicMasterContentViewForAction(
                currentAction);
        contentViewActions.refresh(bomCharacContentView);

        log.trace(logInitMsg + "--- EXIT --- ");

        // redirect to ADMIN VIEW
        return AdminViewManager.VIEW_ADMIN;
    }

    public String getBomCharacteristicMasterClassificationLabel(
            String bomCharacteristicMasterDocType, String classification) {

        String bomType = BomCharacteristicMastersHelper.getBomTypeForBomCharacteristicMasterDocType(
                bomCharacteristicMasterDocType);

        return BomCharacteristicsHelper.getBomClassificationLabel(bomType,
                classification);
    }

    // ---------------------------------------
    // DUPLICATE MANAGEMENT FUNCTIONS
    // ---------------------------------------
    public boolean getCanDuplicateBomPartCharacteristicMaster() {
        return getCanDuplicateBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PART_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDuplicateBomProductCharacteristicMaster() {
        return getCanDuplicateBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDuplicateBomToolCharacteristicMaster() {
        return getCanDuplicateBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDuplicateBomPackagingCharacteristicMaster() {
        return getCanDuplicateBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDuplicateBomSpecificationCharacteristicMaster() {
        return getCanDuplicateBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    private boolean getCanDuplicateBomCharacteristicMaster(String listName) {
        if (navigationContext.getCurrentDocument() == null) {
            return false;
        }
        return !documentsListsManager.isWorkingListEmpty(listName);
    }

    public String duplicateBomPartCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[duplicateBomPartCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = duplicateBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PART_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String duplicateBomToolCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[duplicateBomToolCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = duplicateBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String duplicateBomProductCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[duplicateBomProductCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = duplicateBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String duplicateBomPackagingCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[duplicateBomPackagingCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = duplicateBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String duplicateBomSpecificationCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[duplicateBomSpecificationCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = duplicateBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    private String duplicateBomCharacteristicMasterCurrentSelection(
            String listName) {
        String logInitMsg = "[duplicateBomCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (!documentsListsManager.isWorkingListEmpty(listName)) {
            log.trace(logInitMsg + "|"
                    + documentsListsManager.getWorkingList(listName).size()
                    + "| selected documents will be duplicated");

            List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(
                    listName);

            if (selectedDocuments != null && selectedDocuments.size() > 0) {
                try {
                    for (Iterator<DocumentModel> iterator = selectedDocuments.iterator(); iterator.hasNext();) {
                        DocumentModel selectedDM = iterator.next();
                        String parentFolderPath = getParentFolderPath();

                        if (parentFolderPath != null
                                && !parentFolderPath.isEmpty()) {

                            String currentAction = adminViews.getCurrentSubViewId();
                            String bomCharacDocType = BomCharacteristicMastersHelper.getBomCharacteristicMasterDocTypeForAction(
                                    currentAction);

                            DocumentModel duplicatedDocM = new DocumentModelImpl(
                                    parentFolderPath, bomCharacDocType,
                                    bomCharacDocType);

                            duplicatedDocM.copyContent(selectedDM);
                            duplicatedDocM = documentManager.createDocument(
                                    duplicatedDocM);

                            // Add Copy of mark to new document's title
                            String originalTitle = (String) selectedDM.getPropertyValue(
                                    NuxeoMetadataConstants.NX_DC_TITLE);
                            duplicatedDocM.setPropertyValue(
                                    NuxeoMetadataConstants.NX_DC_TITLE,
                                    messages.get(
                                            "title.duplicate.bomcharac.copyOf")
                                            + " " + originalTitle);

                            documentManager.saveDocument(duplicatedDocM);
                        } else {
                            log.error(logInitMsg
                                    + "parentFolder should have a value");
                            facesMessages.add(StatusMessage.Severity.ERROR,
                                    "message.error.bomcharac.missingParentFolder");
                        }
                    }

                    documentManager.save();

                    // empty processed working list
                    documentsListsManager.resetWorkingList(listName);

                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get(
                                    "message.info.bomcharac.n_duplicated_docs"),
                            selectedDocuments.size());

                } catch (NuxeoException e) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            "Nuxeo error: " + e.getMessage());

                    log.error(logInitMsg + " ****************** ERROR: "
                            + e.getMessage());
                }
            }

            // refresh the content view (in function of the type)
            String currentAction = adminViews.getCurrentSubViewId();
            String bomCharacContentView = BomCharacteristicMastersHelper.getBomCharacteristicMasterContentViewForAction(
                    currentAction);
            contentViewActions.refresh(bomCharacContentView);

        } else {
            log.debug(logInitMsg
                    + "No documents selection in context to process duplicate on...");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }

    // ---------------------------------------
    // DELETE MANAGEMENT FUNCTIONS
    // ---------------------------------------
    public boolean getCanDeleteBomPartCharacteristicMaster() {
        return getCanDeleteBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PART_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDeleteBomToolCharacteristicMaster() {
        return getCanDeleteBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDeleteBomProductCharacteristicMaster() {
        return getCanDeleteBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDeleteBomPackagingCharacteristicMaster() {
        return getCanDeleteBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    public boolean getCanDeleteBomSpecificationCharacteristicMaster() {
        return getCanDeleteBomCharacteristicMaster(
                BomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENTS_SELECTION);
    }

    private boolean getCanDeleteBomCharacteristicMaster(String listName) {
        return deleteActions.getCanDelete(listName);
    }

    public String deleteBomPartCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[deleteBomPartCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = deleteBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PART_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String deleteBomToolCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[deleteBomToolCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = deleteBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String deleteBomProductCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[deleteBomProductCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = deleteBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String deleteBomPackagingCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[deleteBomPackagingCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = deleteBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    public String deleteBomSpecificationCharacteristicMasterCurrentSelection() {
        String logInitMsg = "[deleteBomSpecificationCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String result = deleteBomCharacteristicMasterCurrentSelection(
                BomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENTS_SELECTION);

        log.trace(logInitMsg + "--- EXIT --- ");
        return result;
    }

    private String deleteBomCharacteristicMasterCurrentSelection(
            String listName) {

        String logInitMsg = "[deleteBomCharacteristicMasterCurrentSelection] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (!documentsListsManager.isWorkingListEmpty(listName)) {

            log.trace(logInitMsg + "|"
                    + documentsListsManager.getWorkingList(listName).size()
                    + "| selected documents will be deleted");

            try {
                String result = deleteActions.deleteSelection(
                        documentsListsManager.getWorkingList(listName));

                log.trace(logInitMsg + "--- EXIT --- ");
                return result;

            } catch (NuxeoException e) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        "Nuxeo error: " + e.getMessage());

                log.error(logInitMsg + " ****************** ERROR: "
                        + e.getMessage());
            }

        } else {
            log.debug(logInitMsg
                    + "No documents selection in context to process delete on...");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }
}
