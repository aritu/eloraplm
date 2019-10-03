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
package com.aritu.eloraplm.webapp.clipboard.beans;

import static org.jboss.seam.ScopeType.SESSION;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraWebappClipboardConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
@Name("clipboardActions")
@Scope(SESSION)
@Install(precedence = Install.DEPLOYMENT)
public class EloraClipboardActionsBean extends ClipboardActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraClipboardActionsBean.class);

    /*
     * --------------------------------------------------------------------
     * --------------------------------------------------------------------
     * METHODS AND VARIABLES COPIED/OVERRIDED FROM ClipboardActionsBean
     * in order to manage correctly the lists:
     * (current list, previous list, etc.)
     * --------------------------------------------------------------------
     * ---------------- BEGIN ----------------
     */
    private String currentSelectedList;

    private String previouslySelectedList;

    private transient List<String> availableLists;

    private transient List<DocumentsListDescriptor> descriptorsForAvailableLists;

    private Boolean canEditSelectedDocs;

    private transient Map<String, List<Action>> actionCache;

    @Override
    public void setCurrentSelectedList(String listId) {
        if (listId != null && !listId.equals(currentSelectedList)) {
            currentSelectedList = listId;
            canEditSelectedDocs = null;
        }
    }

    private void returnToPreviouslySelectedList() {
        setCurrentSelectedList(previouslySelectedList);
    }

    @Override
    public String getCurrentSelectedListName() {
        if (currentSelectedList == null) {
            if (!getAvailableLists().isEmpty()) {
                setCurrentSelectedList(availableLists.get(0));
            }
        }
        return currentSelectedList;
    }

    private void autoSelectCurrentList(String listName) {
        previouslySelectedList = getCurrentSelectedListName();
        setCurrentSelectedList(listName);
    }

    @Override
    public List<String> getAvailableLists() {
        if (availableLists == null) {
            availableLists = documentsListsManager.getWorkingListNamesForCategory(
                    "CLIPBOARD");
        }
        return availableLists;
    }

    @Override
    public List<DocumentsListDescriptor> getDescriptorsForAvailableLists() {
        if (descriptorsForAvailableLists == null) {
            List<String> availableLists = getAvailableLists();
            descriptorsForAvailableLists = new ArrayList<DocumentsListDescriptor>();
            for (String lName : availableLists) {
                descriptorsForAvailableLists.add(
                        documentsListsManager.getWorkingListDescriptor(lName));
            }
        }
        return descriptorsForAvailableLists;
    }

    @Override
    public void putSelectionInWorkList(Boolean forceAppend) {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            putSelectionInWorkList(
                    documentsListsManager.getWorkingList(
                            DocumentsListsManager.CURRENT_DOCUMENT_SELECTION),
                    forceAppend);
            autoSelectCurrentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
        } else {
            log.debug(
                    "No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    public void putSelectionInDefaultWorkList() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_copied_docs"), params);
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.DEFAULT_WORKING_LIST, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.DEFAULT_WORKING_LIST);

        } else {
            log.debug(
                    "No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    public void putSelectionInClipboard() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_copied_docs"), params);

            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CLIPBOARD, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);

        } else {
            log.debug(
                    "No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    public void putSelectionInWorkList(List<DocumentModel> docsList,
            Boolean forceAppend) {
        canEditSelectedDocs = null;
        if (null != docsList) {
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_added_to_worklist_docs"), params);

            // Add to the default working list
            documentsListsManager.addToWorkingList(getCurrentSelectedListName(),
                    docsList, forceAppend);
            log.debug("Elements copied to clipboard...");

        } else {
            log.debug("No copiedDocs to process copy on...");
        }

        log.debug("add to worklist processed...");
    }

    @Override
    public boolean getCanEditSelectedDocs() {
        if (canEditSelectedDocs == null) {
            if (getCurrentSelectedList().isEmpty()) {
                canEditSelectedDocs = false;
            } else {
                final List<DocumentModel> selectedDocs = getCurrentSelectedList();

                // check selected docs
                canEditSelectedDocs = checkWritePerm(selectedDocs);
            }
        }
        return canEditSelectedDocs;
    }

    private boolean checkWritePerm(List<DocumentModel> selectedDocs) {
        for (DocumentModel documentModel : selectedDocs) {
            boolean canWrite = documentManager.hasPermission(
                    documentModel.getRef(), SecurityConstants.WRITE_PROPERTIES);
            if (!canWrite) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Action> getActionsForCurrentList() {
        String lstName = getCurrentSelectedListName();
        if (isWorkListEmpty()) {
            // we use cache here since this is a very common case ...
            if (actionCache == null) {
                actionCache = new HashMap<String, List<Action>>();
            }
            if (!actionCache.containsKey(lstName)) {
                actionCache.put(lstName,
                        webActions.getActionsList(lstName + "_LIST"));
            }
            return actionCache.get(lstName);
        } else {
            return webActions.getActionsList(lstName + "_LIST");
        }
    }

    // TODO::: putInClipboard agian aldatuko degu, edo filtroa behintzat,
    // karpetak etabar ez kopiatu ahal izateko.
    @Override
    @WebRemote
    public void putInClipboard(String docId) {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        documentsListsManager.addToWorkingList(DocumentsListsManager.CLIPBOARD,
                doc);
        Object[] params = { 1 };
        facesMessages.add(StatusMessage.Severity.INFO,
                "#0 " + messages.get("n_copied_docs"), params);

        autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);
    }
    /*
     * ---------------- END ----------------
     * --------------------------------------------------------------------
     */

    /*
     * --------------------------------------------------------------------
     * --------------------------------------------------------------------
     * ELORA IMPLEMENTATION STARTS HERE:
     * --------------------------------------------------------------------
     * --------------------------------------------------------------------
     */

    ///////////////////////////////////////////////////////////////////////
    // METHODS CALLED BY FILTERS
    ///////////////////////////////////////////////////////////////////////
    public boolean getCanPasteWorkListAsProxy() {
        /* String logInitMsg = "[getCanPasteWorkListAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");*/

        String currentSelectedListName = getCurrentSelectedListName();

        /* log.trace(logInitMsg + "currentSelectedListName = |"
                + currentSelectedListName + "|");*/

        boolean canPasteWorkListAsProxy = getCanPasteProxy(
                currentSelectedListName);

        /*  log.trace(logInitMsg + "--- EXIT --- canPasteWorkListAsProxy = |"
                + canPasteWorkListAsProxy + "|");
        */
        return canPasteWorkListAsProxy;
    }

    protected boolean getCanPasteProxy(String listName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (documentsListsManager.isWorkingListEmpty(listName)
                || currentDocument == null) {
            return false;
        }

        DocumentModel pasteTarget = getParent(
                navigationContext.getCurrentDocument());
        if (pasteTarget == null) {
            // parent may be unreachable (right inheritance blocked)
            return false;
        }
        if (!documentManager.hasPermission(pasteTarget.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        }
        // Elora filters: it is only possible to paste the workList content as
        // Proxy list if the target is folderish (in the actual action we filter
        // it more)

        if (pasteTarget.isFolder()) {

            /*if (pasteTarget.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)
                || pasteTarget.getType().equals(NuxeoDoctypeConstants.FOLDER)) {*/

            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(
                    listName);
            for (String pasteTypeName : pasteTypesName) {
                if (typeManager.isAllowedSubType(pasteTypeName,
                        pasteTarget.getType(),
                        navigationContext.getCurrentDocument())) {
                    return true;
                }
            }
            return false;
        } else {

            return false;
        }
    }

    public boolean getCanPasteFromClipboardAsProxy() {
        /*String logInitMsg = "[getCanPasteFromClipboardAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");*/

        boolean canPasteFromClipboardAsProxy = getCanPasteProxy(
                DocumentsListsManager.CLIPBOARD);

        /*  log.trace(logInitMsg + "--- EXIT --- canPasteFromClipboardAsProxy = |"
                + canPasteFromClipboardAsProxy + "|");*/

        return canPasteFromClipboardAsProxy;
    }

    public boolean getCanPasteFromClipboardInsideAsProxy(
            DocumentModel document) {
        /* String logInitMsg = "[getCanPasteFromClipboardInsideAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        */
        boolean canPasteFromClipboardInsideAsProxy = getCanPasteInsideAsProxy(
                DocumentsListsManager.CLIPBOARD, document);

        /*  log.trace(logInitMsg
                + "--- EXIT --- canPasteFromClipboardInsideAsProxy = |"
                + canPasteFromClipboardInsideAsProxy + "|");*/

        return canPasteFromClipboardInsideAsProxy;
    }

    protected boolean getCanPasteInsideAsProxy(String listName,
            DocumentModel document) {
        if (documentsListsManager.isWorkingListEmpty(listName)
                || document == null) {
            return false;
        }

        if (!documentManager.hasPermission(document.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        }
        // Elora filters: it is only possible to paste the workList content as
        // Proxy list if the target is a document with EloraWorkspace facet or
        // it is a folder
        if (document.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)
                || document.getType().equals(NuxeoDoctypeConstants.FOLDER)) {
            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(
                    listName);
            for (String pasteTypeName : pasteTypesName) {
                if (typeManager.isAllowedSubType(pasteTypeName,
                        document.getType(),
                        navigationContext.getCurrentDocument())) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // METHODS CALLED BY ACTIONS
    ///////////////////////////////////////////////////////////////////////
    public String pasteWorkingListAsProxy() {
        String logInitMsg = "[pasteWorkingListAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            pasteDocumentListAsProxy(getCurrentSelectedList());

        } catch (NuxeoException e) {
            log.error(logInitMsg + "pasteWorkingListAsProxy failed"
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("invalid_operation"));
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }

    protected String pasteDocumentListAsProxy(List<DocumentModel> docPaste) {
        String logInitMsg = "[pasteDocumentListAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        // TODO Add filters to check if currentDocument is inside a
        // WorkspaceRoot

        if (null != docPaste) {

            if (EloraStructureHelper.isDocUnderWorkspaceRoot(currentDocument)) {
                log.trace(logInitMsg + "docPaste.size = |" + docPaste.size()
                        + "|");

                List<DocumentModel> newDocs = recreateDocumentsAsProxyWithNewParent(
                        getParent(currentDocument), docPaste);

                Object[] params = { newDocs.size() };
                facesMessages.add(StatusMessage.Severity.INFO,
                        "#0 " + messages.get("n_pasted_docs"), params);

                EventManager.raiseEventsOnDocumentSelected(currentDocument);
                Events.instance().raiseEvent(
                        EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);

                log.debug(logInitMsg
                        + "Elements pasted as proxy and created into the backend.");
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.pasteAsProxy"));
                log.debug(logInitMsg
                        + "Current document is not under a WorkspaceRoot.");
            }
        } else {
            log.debug(logInitMsg + "No docPaste to process paste as proxy on.");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }

    protected List<DocumentModel> recreateDocumentsAsProxyWithNewParent(
            DocumentModel parent, List<DocumentModel> documents) {
        String logInitMsg = "[recreateDocumentsAsProxyWithNewParent] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        if (null == parent || null == documents) {
            log.error(logInitMsg + "Null params received, returning.");
            return newDocuments;
        }

        log.trace(logInitMsg + "parent = |" + parent.getId()
                + "|, documents.size = |" + documents.size() + "|");

        List<DocumentModel> documentsToPast = new LinkedList<DocumentModel>();

        if (EloraStructureHelper.isDocUnderWorkspaceRoot(parent)) {

            // filter list on content type
            for (DocumentModel doc : documents) {

                // if the copied document is a proxy, retrieve the document
                // pointed
                // by the proxy
                if (doc.isProxy()) {
                    log.trace(logInitMsg + "doc = |" + doc.getId()
                            + "| is a proxy. Retrieve it's source.");
                    doc = documentManager.getDocument(
                            new IdRef(doc.getSourceId()));
                }

                // Elora filter: only documents without facet FOLDERISH
                // (folders,
                // eloraWorkspace, ...) can be pasted as Proxy
                if (!doc.hasFacet(FacetNames.FOLDERISH)) {

                    // Nuxeo filter
                    if (typeManager.isAllowedSubType(doc.getType(),
                            parent.getType(),
                            navigationContext.getCurrentDocument())) {

                        // Elora filter: check if there is already a proxy to
                        // that
                        // document
                        DocumentModelList proxies = documentManager.getProxies(
                                doc.getRef(), parent.getRef());
                        if (proxies != null && proxies.size() > 0) {
                            log.trace(logInitMsg + "doc = |" + doc.getId()
                                    + "| cannot be pasted as proxy, since there is already a proxy to that document.");
                        } else {
                            documentsToPast.add(doc);
                        }
                    } else {
                        log.trace(logInitMsg + "doc = |" + doc.getId()
                                + "| cannot be pasted as proxy, since it is not an allowed subtype.");
                    }
                } else {
                    log.trace(logInitMsg + "doc = |" + doc.getId()
                            + "| cannot be pasted as proxy, since it has FOLDERISH facet.");
                }

            }

            boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(
                    parent.getCurrentLifeCycleState());

            StringBuilder sb = new StringBuilder();

            // paste documents as Proxy
            for (DocumentModel doc : documentsToPast) {
                if (destinationIsDeleted && !checkDeletedState(doc)) {
                    addWarnMessage(sb, doc);
                } else {
                    // create the proxy
                    newDocuments.add(documentManager.createProxy(doc.getRef(),
                            parent.getRef()));
                    log.trace("proxy created for doc = |" + doc.getId()
                            + "|, in target = |" + parent.getId() + "|");
                }
            }

            if (destinationIsDeleted) {
                for (DocumentModel d : newDocuments) {
                    setDeleteState(d);
                }
            }
            documentManager.save();
            if (sb.length() > 0) {
                facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.pasteAsProxy"));
            log.debug(logInitMsg
                    + "Current document is not under a WorkspaceRoot.");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return newDocuments;
    }

    public String pasteClipboardAsProxy() {
        String logInitMsg = "[pasteClipboardAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            pasteDocumentListAsProxy(DocumentsListsManager.CLIPBOARD);
            returnToPreviouslySelectedList();
        } catch (NuxeoException e) {
            log.error(logInitMsg + "pasteClipboardAsProxy failed"
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("invalid_operation"));
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return null;
    }

    protected String pasteDocumentListAsProxy(String listName) {
        return pasteDocumentListAsProxy(
                documentsListsManager.getWorkingList(listName));
    }

    @WebRemote
    public String pasteClipboardInsideAsProxy(String docId) {
        String logInitMsg = "[pasteClipboardInsideAsProxy] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        pasteDocumentListInsideAsProxy(DocumentsListsManager.CLIPBOARD, docId);

        log.trace(logInitMsg + "--- EXIT --- ");

        return null;
    }

    protected String pasteDocumentListInsideAsProxy(String listName,
            String docId) {
        return pasteDocumentListInsideAsProxy(
                documentsListsManager.getWorkingList(listName), docId);
    }

    protected String pasteDocumentListInsideAsProxy(
            List<DocumentModel> docPaste, String docId) {
        DocumentModel targetDoc = documentManager.getDocument(new IdRef(docId));
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsAsProxyWithNewParent(
                    targetDoc, docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_pasted_docs"), params);

            EventManager.raiseEventsOnDocumentSelected(targetDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    targetDoc);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return null;
    }

    // ----------------------------------------------------------------

    public String pasteWorkingListAsDuplicate() {
        String logInitMsg = "[pasteWorkingListAsDuplicate] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            pasteDocumentListAsDuplicate(getCurrentSelectedList());

        } catch (NuxeoException e) {
            log.error(logInitMsg + "pasteWorkingListAsDuplicate failed"
                    + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("invalid_operation"));
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }

    protected String pasteDocumentListAsDuplicate(
            List<DocumentModel> docPaste) {
        String logInitMsg = "[pasteDocumentListAsDuplicate] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (null != docPaste) {

            log.trace(logInitMsg + "docPaste.size = |" + docPaste.size() + "|");

            List<DocumentModel> newDocs = recreateDocumentsAsDuplicateWithNewParent(
                    getParent(currentDocument), docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_pasted_docs"), params);

            EventManager.raiseEventsOnDocumentSelected(currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);

            log.debug(logInitMsg
                    + "Elements pasted as duplicate and created into the backend.");
        } else {
            log.debug(logInitMsg
                    + "No docPaste to process paste as duplicate on.");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return null;
    }

    protected List<DocumentModel> recreateDocumentsAsDuplicateWithNewParent(
            DocumentModel parent, List<DocumentModel> documents) {
        String logInitMsg = "[recreateDocumentsAsDuplicateWithNewParent] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        if (null == parent || null == documents) {
            log.error(logInitMsg + "Null params received, returning.");
            return newDocuments;
        }

        log.trace(logInitMsg + "parent = |" + parent.getId()
                + "|, documents.size = |" + documents.size() + "|");

        List<DocumentModel> documentsToPast = new LinkedList<DocumentModel>();

        // filter list on content type
        for (DocumentModel doc : documents) {

            // if the copied document is a proxy, retrieve the document pointed
            // by the proxy
            if (doc.isProxy()) {
                log.trace(logInitMsg + "doc = |" + doc.getId()
                        + "| is a proxy. Retrieve it's source.");
                doc = documentManager.getDocument(new IdRef(doc.getSourceId()));
            }

            // Elora filter: only documents without facet FOLDERISH (folders,
            // eloraWorkspace, ...) can be pasted as duplicate
            if (!doc.hasFacet(FacetNames.FOLDERISH)) {

                // Nuxeo filter
                if (typeManager.isAllowedSubType(doc.getType(),
                        parent.getType(),
                        navigationContext.getCurrentDocument())) {
                    documentsToPast.add(doc);
                } else {
                    log.trace(logInitMsg + "doc = |" + doc.getId()
                            + "| cannot be pasted as duplicate, since it is not an allowed subtype.");
                }
            } else {
                log.trace(logInitMsg + "doc = |" + doc.getId()
                        + "| cannot be pasted as duplicate, since it has FOLDERISH facet.");
            }
        }

        boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(
                parent.getCurrentLifeCycleState());

        StringBuilder sb = new StringBuilder();

        // paste documents as Duplicate
        for (DocumentModel doc : documentsToPast) {
            if (destinationIsDeleted && !checkDeletedState(doc)) {
                addWarnMessage(sb, doc);
            } else {
                // create the duplicate
                DocumentModel newDoc = documentManager.copy(doc.getRef(),
                        parent.getRef(), doc.getName(),
                        CopyOption.RESET_LIFE_CYCLE);

                // modify the reference and title of the duplicate by adding
                // "COPY OF " before
                String currentReference = "";
                String currentTitle = "";
                if (doc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE) != null) {
                    currentReference = (String) doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE);
                }
                if (doc.getPropertyValue(
                        NuxeoMetadataConstants.NX_DC_TITLE) != null) {
                    currentTitle = (String) doc.getPropertyValue(
                            NuxeoMetadataConstants.NX_DC_TITLE);
                }
                String newReference = EloraWebappClipboardConstants.COPY_OF
                        + currentReference;
                String newTitle = EloraWebappClipboardConstants.COPY_OF
                        + currentTitle;
                newDoc.setPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE,
                        newReference);
                newDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                        newTitle);
                documentManager.saveDocument(newDoc);

                newDocuments.add(newDoc);

                log.trace("duplicate created for doc = |" + doc.getId()
                        + "|, in target = |" + parent.getId() + "|");
            }
        }

        if (destinationIsDeleted) {
            for (DocumentModel d : newDocuments) {
                setDeleteState(d);
            }
        }
        documentManager.save();
        if (sb.length() > 0) {
            facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return newDocuments;
    }

    public String pasteClipboardAsDuplicate() {
        String logInitMsg = "[pasteClipboardAsDuplicate] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            pasteDocumentListAsDuplicate(DocumentsListsManager.CLIPBOARD);
            returnToPreviouslySelectedList();
        } catch (NuxeoException e) {
            log.error(logInitMsg + "pasteClipboard failed" + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("invalid_operation"));

        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return null;
    }

    public String pasteDocumentListAsDuplicate(String listName) {
        return pasteDocumentListAsDuplicate(
                documentsListsManager.getWorkingList(listName));
    }

    @WebRemote
    public String pasteClipboardInsideAsDuplicate(String docId) {
        String logInitMsg = "[pasteClipboardInsideAsDuplicate] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        pasteDocumentListInsideAsDuplicate(DocumentsListsManager.CLIPBOARD,
                docId);

        log.trace(logInitMsg + "--- EXIT --- ");

        return null;
    }

    public String pasteDocumentListInsideAsDuplicate(String listName,
            String docId) {
        return pasteDocumentListInsideAsDuplicate(
                documentsListsManager.getWorkingList(listName), docId);
    }

    public String pasteDocumentListInsideAsDuplicate(
            List<DocumentModel> docPaste, String docId) {
        DocumentModel targetDoc = documentManager.getDocument(new IdRef(docId));
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsAsDuplicateWithNewParent(
                    targetDoc, docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO,
                    "#0 " + messages.get("n_pasted_docs"), params);

            EventManager.raiseEventsOnDocumentSelected(targetDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    targetDoc);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return null;
    }

}
