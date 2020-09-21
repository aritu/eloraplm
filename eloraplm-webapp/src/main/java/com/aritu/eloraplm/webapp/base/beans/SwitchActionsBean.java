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

import static org.jboss.seam.ScopeType.PAGE;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGroupConstants;
import com.aritu.eloraplm.constants.EloraPageProvidersConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */

@Name("switchActions")
@Scope(PAGE)
public class SwitchActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SwitchActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String originEloraRootFolderUid;

    protected String targetEloraRootFolderUid;

    protected boolean isValidTargetEloraRootFolderUid = false;

    protected boolean displayCalculateDocumentListToBeSwitched = false;

    protected boolean calculatedDocumentListToBeSwitched = false;

    protected boolean recursiveSwitch = false;

    protected ArrayList<String> availableDocumentListToBeSwitched = new ArrayList<String>();

    protected ArrayList<String> selectedDocumentListToBeSwitched = new ArrayList<String>();

    protected boolean isDisabledSwitchButton = true;

    // -------------------- getters and setters ---- START ----
    public String getOriginEloraRootFolderUid() {
        String logInitMsg = "[getOriginEloraRootFolderUid] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (originEloraRootFolderUid == null
                || originEloraRootFolderUid.length() == 0) {
            try {
                DocumentModel currentDocument = navigationContext.getCurrentDocument();
                originEloraRootFolderUid = EloraStructureHelper.getEloraRootFolderUid(
                        currentDocument, documentManager);
            } catch (EloraException e) {
                log.error(logInitMsg + " Exception message = |" + e.getMessage()
                        + "|", e);
            }
        }

        return originEloraRootFolderUid;
    }

    public void setOriginEloraRootFolderUid(String originEloraRootFolderUid) {
        this.originEloraRootFolderUid = originEloraRootFolderUid;
    }

    public String getTargetEloraRootFolderUid() {
        return targetEloraRootFolderUid;
    }

    public void setTargetEloraRootFolderUid(String targetEloraRootFolderUid) {

        if (targetEloraRootFolderUid != null
                && !targetEloraRootFolderUid.equals(
                        this.targetEloraRootFolderUid)) {
            // reset other fields
            displayCalculateDocumentListToBeSwitched = false;

            calculatedDocumentListToBeSwitched = false;

            recursiveSwitch = false;

            availableDocumentListToBeSwitched = new ArrayList<String>();

            selectedDocumentListToBeSwitched = new ArrayList<String>();

        }

        this.targetEloraRootFolderUid = targetEloraRootFolderUid;

    }

    public boolean getIsValidTargetEloraRootFolderUid() {
        if (targetEloraRootFolderUid != null
                && targetEloraRootFolderUid.length() > 0
                && targetEloraRootFolderUid.equals(
                        getOriginEloraRootFolderUid())) {

            isValidTargetEloraRootFolderUid = false;
        } else {
            isValidTargetEloraRootFolderUid = true;
        }

        return isValidTargetEloraRootFolderUid;
    }

    public boolean getDisplayCalculateDocumentListToBeSwitched() {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (currentDocument.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                || currentDocument.hasFacet(
                        EloraFacetConstants.FACET_CAD_DOCUMENT)) {

            displayCalculateDocumentListToBeSwitched = true;
        } else {

            displayCalculateDocumentListToBeSwitched = false;
        }

        return displayCalculateDocumentListToBeSwitched;
    }

    public boolean getCalculatedDocumentListToBeSwitched() {
        return calculatedDocumentListToBeSwitched;
    }

    public boolean getRecursiveSwitch() {
        return recursiveSwitch;
    }

    public void setRecursiveSwitch(boolean recursiveSwitch) {
        this.recursiveSwitch = recursiveSwitch;
    }

    public ArrayList<String> getAvailableDocumentListToBeSwitched() {
        return availableDocumentListToBeSwitched;
    }

    public ArrayList<String> getSelectedDocumentListToBeSwitched() {
        return selectedDocumentListToBeSwitched;
    }

    public void setSelectedDocumentListToBeSwitched(
            ArrayList<String> selectedDocumentListToBeSwitched) {
        this.selectedDocumentListToBeSwitched = selectedDocumentListToBeSwitched;
    }

    public boolean getIsDisabledSwitchButton() {

        if (targetEloraRootFolderUid != null
                && targetEloraRootFolderUid.length() > 0
                && isValidTargetEloraRootFolderUid) {
            if (displayCalculateDocumentListToBeSwitched) {
                if (calculatedDocumentListToBeSwitched) {
                    isDisabledSwitchButton = false;
                } else {
                    isDisabledSwitchButton = true;
                }
            } else {
                isDisabledSwitchButton = false;
            }
        } else {
            isDisabledSwitchButton = true;
        }

        return isDisabledSwitchButton;
    }

    // -------------------- getters and setters ---- END ----

    public DocumentModel getOriginEloraRootFolder() {
        return getDocumentEloraRootFolder(getOriginEloraRootFolderUid());
    }

    public DocumentModel getDocumentEloraRootFolder(String docUid) {
        String logInitMsg = "[getDocumentEloraRootFolder] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel documentEloraRootFolder = null;
        try {

            DocumentModel document = documentManager.getDocument(
                    new IdRef(docUid));

            String documentEloraRootFolderUid = EloraStructureHelper.getEloraRootFolderUid(
                    document, documentManager);

            documentEloraRootFolder = documentManager.getDocument(
                    new IdRef(documentEloraRootFolderUid));

        } catch (EloraException e) {
            log.error(logInitMsg + " Exception message = |" + e.getMessage()
                    + "|", e);
        }

        return documentEloraRootFolder;
    }

    public Boolean getCanSwitchCurrentDocEloraRootFolder() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanSwitchDocEloraRootFolder(currentDocument);
    }

    /**
     *
     * @param document
     * @return
     */
    public Boolean getCanSwitchDocEloraRootFolder(DocumentModel document) {
        String logInitMsg = "[getCanSwitchDocEloraRootFolder] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        boolean canSwitch = false;

        if (document == null) {
            log.warn(logInitMsg
                    + "Can't evaluate switch action : currentDocument is null");
        } else {
            if (!document.isImmutable() && !document.isProxy()
                    && (document.hasFacet(
                            EloraFacetConstants.FACET_BOM_DOCUMENT)
                            || document.hasFacet(
                                    EloraFacetConstants.FACET_CAD_DOCUMENT)
                            || document.hasFacet(
                                    EloraFacetConstants.FACET_BASIC_DOCUMENT)
                            || document.hasFacet(
                                    EloraFacetConstants.FACET_ELORA_WORKSPACE))) {

                NuxeoPrincipal currentUser = (NuxeoPrincipal) documentManager.getPrincipal();

                if (currentUser.isAdministrator() || currentUser.isMemberOf(
                        EloraGroupConstants.POWER_USERS_GROUP)) {
                    canSwitch = true;
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT with canSwitch = |" + canSwitch
                + "|---");
        return canSwitch;
    }

    public String getPageProviderNameForSwitchingCurrentDocument() {
        String logInitMsg = "[getPageProviderNameForSwitchingDocument] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String pageProviderName = getPageProviderNameForSwitchingDocument(
                currentDocument);

        log.trace(logInitMsg + "--- EXIT ---");

        return pageProviderName;
    }

    public String getPageProviderNameForSwitchingDocument(
            DocumentModel document) {

        String logInitMsg = "[getPageProviderNameForSwitchingDocument] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        String pageProviderName = "";

        if (!document.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)
                && (document.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                        || document.hasFacet(
                                EloraFacetConstants.FACET_CAD_DOCUMENT)
                        || document.hasFacet(
                                EloraFacetConstants.FACET_BASIC_DOCUMENT))) {
            pageProviderName = EloraPageProvidersConstants.STRUCTURE_COLLABORATION_LIBRARY_ROOT_WC_DOC_SUGG;
        } else if (document.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                || document.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                || document.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                || document.hasFacet(
                        EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            pageProviderName = EloraPageProvidersConstants.STRUCTURE_COLLABORATION_ROOT_WC_DOC_SUGG;
        }

        log.trace(logInitMsg + "--- EXIT with pageProviderName = |"
                + pageProviderName + "| ---");

        return pageProviderName;
    }

    public void calculateDocumentsToBeSwitchedForCurrentDocument() {
        String logInitMsg = "[calculateDocumentsToBeSwitchedForCurrentDocument] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        calculateDocumentsToBeSwitched(currentDocument);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void calculateDocumentsToBeSwitched(DocumentModel document) {
        String logInitMsg = "[calculateDocumentsToBeSwitched] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {

            // First, reset available and selected document list
            availableDocumentListToBeSwitched = new ArrayList<String>();
            selectedDocumentListToBeSwitched = new ArrayList<String>();

            // Calculate the list of documents that has to be switched, in
            // function of the facet
            DocumentModelList docListToBeSwitched = new DocumentModelListImpl();

            DocumentRef targetEloraRootFolderRef = new IdRef(
                    targetEloraRootFolderUid);

            if (document.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                docListToBeSwitched = EloraStructureHelper.retrieveDocListToBeSwitchedForBomDoc(
                        document, targetEloraRootFolderRef, recursiveSwitch);
            } else if (document.hasFacet(
                    EloraFacetConstants.FACET_CAD_DOCUMENT)) {
                docListToBeSwitched = EloraStructureHelper.retrieveDocListToBeSwitchedForCadDoc(
                        document, targetEloraRootFolderRef, recursiveSwitch);
            }

            if (docListToBeSwitched != null && docListToBeSwitched.size() > 0) {
                for (DocumentModel doc : docListToBeSwitched) {
                    availableDocumentListToBeSwitched.add(doc.getId());
                }
            }

            // By default, all of them are selected. So, add them to the
            // selectedDocumentListToBeSwitched also
            for (int i = 0; i < availableDocumentListToBeSwitched.size(); i++) {
                selectedDocumentListToBeSwitched.add(i,
                        availableDocumentListToBeSwitched.get(i));
            }

            calculatedDocumentListToBeSwitched = true;

        } catch (EloraException e) {

            log.error(logInitMsg + "Exception: " + e.getClass().getName() + ". "
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.calculateDocumentsToBeSwitched"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void switchCurrentDocumentEloraRootFolder() {
        String logInitMsg = "[switchCurrentDocumentEloraRootFolder] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        switchDocumentEloraRootFolder(currentDocument);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void switchDocumentEloraRootFolder(DocumentModel document) {
        String logInitMsg = "[switchDocumentEloraRootFolder] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        log.trace(logInitMsg + "document to be switched = |" + document.getId()
                + "|");

        if (targetEloraRootFolderUid == null
                || targetEloraRootFolderUid.length() == 0) {

            log.error(logInitMsg
                    + "Cannot switch document. Specified targetEloraRootFolderUid is empty or null. targetEloraRootFolderUid = |"
                    + targetEloraRootFolderUid + "|");

            // display error message, targetEloraRootFolderUid cannot be null
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.switchEloraRootFolder.emptytargetStructureRootUid"));
        } else {
            try {

                // check, if selected targetEloraRootFolderUid is not the same
                // as current Elora root folder
                String originEloraRootFolderUid = EloraStructureHelper.getEloraRootFolderUid(
                        document, documentManager);

                log.trace(logInitMsg + "originEloraRootFolderUid = |"
                        + originEloraRootFolderUid
                        + "|, targetEloraRootFolderUid = |"
                        + targetEloraRootFolderUid + "|");

                if (originEloraRootFolderUid.equals(targetEloraRootFolderUid)) {

                    log.error(logInitMsg
                            + "Cannot switch document. Specified targetEloraRootFolderUid is the same as the origin one. targetEloraRootFolderUid = |"
                            + targetEloraRootFolderUid + "|");

                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.switchEloraRootFolder.targetStructureRootUidSameAsOrigin"));
                } else {
                    DocumentRef targetEloraRootFolderRef = new IdRef(
                            targetEloraRootFolderUid);
                    DocumentRef switchOriginDocRef = document.getRef();

                    if (document.hasFacet(
                            EloraFacetConstants.FACET_BOM_DOCUMENT)
                            || document.hasFacet(
                                    EloraFacetConstants.FACET_CAD_DOCUMENT)) {

                        DocumentModelList documentListToBeSwitched = new DocumentModelListImpl();

                        // Add the document itself
                        documentListToBeSwitched.add(document);

                        // Add related selected documents
                        if (selectedDocumentListToBeSwitched != null
                                && selectedDocumentListToBeSwitched.size() > 0) {
                            for (Iterator<String> iterator = selectedDocumentListToBeSwitched.iterator(); iterator.hasNext();) {
                                String docUid = iterator.next();
                                documentListToBeSwitched.add(
                                        documentManager.getDocument(
                                                new IdRef(docUid)));
                            }
                        }
                        document = EloraStructureHelper.switchDocumentListEloraRootFolder(
                                documentListToBeSwitched,
                                targetEloraRootFolderRef, switchOriginDocRef,
                                documentManager);

                    } else {
                        document = EloraStructureHelper.switchSingleDocumentEloraRootFolder(
                                document, targetEloraRootFolderRef,
                                switchOriginDocRef);
                    }

                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.switchEloraRootFolder"));

                    navigationContext.navigateToDocument(document);

                    log.trace(logInitMsg + "document |" + document.getId()
                            + "| successfully switched.");
                }
            } catch (EloraException e) {

                log.error(logInitMsg + "Exception: " + e.getClass().getName()
                        + ". " + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.switchEloraRootFolder"));
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void cancelswitchCurrentDocumentEloraRootFolder() {
        String logInitMsg = "[cancelswitchCurrentDocumentEloraRootFolder] ";
        log.trace(logInitMsg + "--- ENTER ---");

        originEloraRootFolderUid = null;

        targetEloraRootFolderUid = null;

        isValidTargetEloraRootFolderUid = false;

        displayCalculateDocumentListToBeSwitched = false;

        calculatedDocumentListToBeSwitched = false;

        recursiveSwitch = false;

        availableDocumentListToBeSwitched = new ArrayList<String>();

        selectedDocumentListToBeSwitched = new ArrayList<String>();

        isDisabledSwitchButton = true;

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
