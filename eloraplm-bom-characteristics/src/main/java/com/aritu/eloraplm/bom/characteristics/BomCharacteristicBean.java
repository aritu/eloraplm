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
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import com.aritu.eloraplm.constants.EloraBomCharacteristicsConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
@Name("bomCharacteristic")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
public class BomCharacteristicBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicBean.class);

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

    protected DocumentModel changeableDocument;

    protected String changeableDocumentAction;

    protected String parentFolderPath;

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

        if (parentFolderPath == null || parentFolderPath.isEmpty()) {

            parentFolderPath = getParentFolderPath(documentManager);
        }

        String currentAction = adminViews.getCurrentSubViewId();

        String bomCharacDocType = getBomCharacteristicDocTypeForAction(
                currentAction);

        DocumentModel changeableDocument = documentManager.createDocumentModel(
                parentFolderPath, bomCharacDocType, bomCharacDocType);

        setChangeableDocument(changeableDocument);

        setChangeableDocumentAction(currentAction);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    protected String getParentFolderPath(CoreSession session) {

        String logInitMsg = "[getParentFolderPath] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel bomCharacFolder = null;

        // get parent folder
        DocumentModelList res = session.query(String.format("SELECT * from %s",
                EloraBomCharacteristicsConstants.BOM_CHARAC_FOLDER_DOCUMENT_TYPE));

        if (res == null || res.isEmpty()) {

            // if it doesn't exist, create it
            bomCharacFolder = session.createDocumentModel("/",
                    EloraBomCharacteristicsConstants.BOM_CHARAC_FOLDER_ID,
                    EloraBomCharacteristicsConstants.BOM_CHARAC_FOLDER_DOCUMENT_TYPE);
            bomCharacFolder.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                    EloraBomCharacteristicsConstants.BOM_CHARAC_FOLDER_DOCUMENT_TYPE);
            bomCharacFolder = session.createDocument(bomCharacFolder);
            ACP acp = session.getACP(bomCharacFolder.getRef());
            ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
            acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ,
                    true));
            session.setACP(bomCharacFolder.getRef(), acp, true);

        } else {
            if (res.size() > 1) {
                if (log.isWarnEnabled()) {
                    // TODO::: hemen zerbait?????????????
                    log.warn("More han one "
                            + EloraBomCharacteristicsConstants.BOM_CHARAC_FOLDER_DOCUMENT_TYPE
                            + " found:");
                    for (DocumentModel model : res) {
                        log.warn(" - " + model.getName() + ", "
                                + model.getPathAsString());
                    }
                }
            }
            bomCharacFolder = res.get(0);
        }

        String bomCharacFolderPath = bomCharacFolder.getPathAsString();

        log.trace(logInitMsg + "bomCharacFolderPath = |" + bomCharacFolderPath
                + "|");

        log.trace(logInitMsg + "--- EXIT ---");

        return bomCharacFolderPath;

    }

    public void saveBomCharacteristic() {
        String logInitMsg = "[saveBomCharacteristic] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel changeableDocument = getChangeableDocument();

        if (changeableDocument.getId() != null) {
            log.debug("Document " + changeableDocument.getName()
                    + " already created");
            /* return navigationContext.navigateToDocument(newDocument,
                    "after-create");*/
        }
        try {
            changeableDocument = documentManager.createDocument(
                    changeableDocument);

            setChangeableDocument(changeableDocument);

        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "label.schema.constraint.violation.documentValidation"),
                    e.getMessage());
            // return null;
        }
        documentManager.save();

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_saved"),
                messages.get(changeableDocument.getType()));

        resetChangeableDocument();

        // refresh the content view (in function of the type)
        String currentAction = adminViews.getCurrentSubViewId();
        String bomCharacContentView = getBomCharacteristicContentViewForAction(
                currentAction);
        contentViewActions.refresh(bomCharacContentView);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public void resetTypeRelatedFields(DocumentModel editedDoc) {
        String logInitMsg = "[resetTypeRelatedFields] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_LENGTH,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_MAX_DECIMAL_PLACES,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_NUMBER_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_STRING_MAX_LENGTH,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_STRING_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_DATE_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_BOOLEAN_DEFAULT_VALUE,
                "");
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_CONTENT,
                new ArrayList<HashMap<String, Object>>());
        editedDoc.setPropertyValue(
                EloraBomCharacteristicsConstants.BOM_CHARAC_LIST_DEFAULT_VALUE,
                "");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public String updateBomCharacteristic() {
        String logInitMsg = "[updateBomCharacteristic] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        try {
            currentDocument = documentManager.saveDocument(currentDocument);
        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
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
        String bomCharacContentView = getBomCharacteristicContentViewForAction(
                currentAction);
        contentViewActions.refresh(bomCharacContentView);

        log.trace(logInitMsg + "--- EXIT --- ");

        // redirect to ADMIN VIEW
        return AdminViewManager.VIEW_ADMIN;
    }

    private String getBomCharacteristicDocTypeForAction(String action) {

        String logInitMsg = "[getBomCharacteristicDocTypeForAction] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- action = |" + action + "|");

        String bomCharacteristicDocType = "";

        switch (action) {
        case EloraBomCharacteristicsConstants.BOM_PART_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicDocType = EloraBomCharacteristicsConstants.BOM_PART_CHARAC_DOCUMENT_TYPE;
            break;
        case EloraBomCharacteristicsConstants.BOM_PRODUCT_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicDocType = EloraBomCharacteristicsConstants.BOM_PRODUCT_CHARAC_DOCUMENT_TYPE;
            break;
        case EloraBomCharacteristicsConstants.BOM_TOOL_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicDocType = EloraBomCharacteristicsConstants.BOM_TOOL_CHARAC_DOCUMENT_TYPE;
            break;
        case EloraBomCharacteristicsConstants.BOM_PACKAGING_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicDocType = EloraBomCharacteristicsConstants.BOM_PACKAGING_CHARAC_DOCUMENT_TYPE;
            break;
        case EloraBomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicDocType = EloraBomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_DOCUMENT_TYPE;
            break;
        }

        log.trace(logInitMsg + "--- EXIT --- with bomCharacteristicDocType = |"
                + bomCharacteristicDocType + "|");

        return bomCharacteristicDocType;
    }

    private String getBomCharacteristicContentViewForAction(String action) {

        String logInitMsg = "[getBomCharacteristicContentViewForAction] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "--- ENTER --- currentAction = |" + action + "|");

        String bomCharacteristicContentView = "";

        switch (action) {
        case EloraBomCharacteristicsConstants.BOM_PART_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicContentView = EloraBomCharacteristicsConstants.BOM_PART_CHARAC_CONTENT_VIEW;
            break;
        case EloraBomCharacteristicsConstants.BOM_PRODUCT_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicContentView = EloraBomCharacteristicsConstants.BOM_PRODUCT_CHARAC_CONTENT_VIEW;
            break;
        case EloraBomCharacteristicsConstants.BOM_TOOL_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicContentView = EloraBomCharacteristicsConstants.BOM_TOOL_CHARAC_CONTENT_VIEW;
            break;
        case EloraBomCharacteristicsConstants.BOM_PACKAGING_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicContentView = EloraBomCharacteristicsConstants.BOM_PACKAGING_CHARAC_CONTENT_VIEW;
            break;
        case EloraBomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_ADMIN_ACTION_ID:
            bomCharacteristicContentView = EloraBomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_CONTENT_VIEW;
            break;
        }

        log.trace(logInitMsg
                + "--- EXIT --- with bomCharacteristicContentView = |"
                + bomCharacteristicContentView + "|");

        return bomCharacteristicContentView;
    }

}
