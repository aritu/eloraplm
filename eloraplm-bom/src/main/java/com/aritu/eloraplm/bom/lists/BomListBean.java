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
package com.aritu.eloraplm.bom.lists;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.bom.lists.service.BomListService;
import com.aritu.eloraplm.config.util.BomListsConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.DocumentContextBoundActions;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Name("bomList")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomListBean extends DocumentContextBoundActions {
    private static final long serialVersionUID = 1L;

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
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    private static final Log log = LogFactory.getLog(BomListBean.class);

    private EloraConfigTable bomListsTable;

    private String id;

    private String label;

    private String description;

    private String type;

    private boolean isListCreated;

    private boolean reloadBomListData = true;

    private DocumentModel currentBomList;

    public EloraConfigTable getBomListsTable() {
        return bomListsTable;
    }

    public void setBomListsTable(EloraConfigTable bomListsTable) {
        this.bomListsTable = bomListsTable;
    }

    public String getId() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getIsListCreated() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return isListCreated;
    }

    public void setIsListCreated(boolean isListCreated) {
        this.isListCreated = isListCreated;
    }

    public DocumentModel getCurrentBomList() {
        if (reloadBomListData) {
            loadBomListData();
        }
        return currentBomList;
    }

    public void setCurrentBomList(DocumentModel currentBomList) {
        this.currentBomList = currentBomList;
    }

    public boolean getIsSubTabVisible(String docRef, String bomListId,
            String treeDirection) {
        BomListService nodeService = Framework.getService(BomListService.class);

        return nodeService.hasBomList(docRef, bomListId, treeDirection);
    }

    @Observer(value = {
            WebActions.CURRENT_TAB_CHANGED_EVENT
                    + "_TAB_BOM_COMPOSITION_sub_tab",
            WebActions.CURRENT_TAB_CHANGED_EVENT
                    + "_TAB_BOM_WHERE_USED_sub_tab" })
    @BypassInterceptors
    public void reloadBomListSubtab() {
        reloadBomListData = true;
    }

    public void loadBomListData() {
        String logInitMsg = "[loadBomListData] ["
                + documentManager.getPrincipal().getName() + "] ";

        reloadBomListData = false;

        try {
            bomListsTable = BomListsConfigHelper.getBomLists(false);

            id = webActions.getCurrentSubTabId().replace(
                    webActions.getCurrentTabId().concat("_"), "");

            if (!id.isEmpty() && !bomListsTable.isEmpty()
                    && bomListsTable.containsKey(id)) {
                EloraConfigRow configRow = bomListsTable.getRow(id);
                label = (String) configRow.getProperty("label");
                description = (String) configRow.getProperty("description");
                type = (String) configRow.getProperty("type");

                Events.instance().raiseEvent(
                        EloraEventNames.ELORA_BOM_LIST_SUBTAB_CHANGED_EVENT,
                        id);
            }

            // Check if the BOM list is created or not (for Composition)
            isListCreated = false;

            DocumentModel currentDocument = getCurrentDocument();
            if (currentDocument != null) {

                if (currentDocument.isProxy()
                        || currentDocument.isImmutable()) {
                    currentDocument = documentManager.getWorkingCopy(
                            currentDocument.getRef());
                }

                DocumentModelList bomLists = BomListHelper.getBomListForDocument(
                        currentDocument, id, false, documentManager);
                if (bomLists != null && !bomLists.isEmpty()) {
                    if (bomLists.size() == 1) {
                        isListCreated = true;
                        currentBomList = bomLists.get(0);
                    } else {
                        throw new EloraException(
                                "The current document has more than one BomList documents of the same list id.");
                    }
                }
            }

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.bom.loadBomListData"));
        }

    }

    public void createList() {
        String logInitMsg = "[createList] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDocument = getCurrentDocument();

            if (currentDocument.isProxy() || currentDocument.isImmutable()) {
                currentDocument = documentManager.getWorkingCopy(
                        currentDocument.getRef());
            }

            PathSegmentService pss = Framework.getService(
                    PathSegmentService.class);
            String docPathSegment = null;
            String reference = (String) currentDocument.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (reference != null) {
                docPathSegment = reference;
            } else {
                docPathSegment = currentDocument.getTitle();
            }
            String nameForPath = pss.generatePathSegment(
                    "BomList_" + docPathSegment + "_" + id);
            DocumentModel bomList = documentManager.createDocumentModel(
                    getBomListsFolderPath(), nameForPath,
                    EloraDoctypeConstants.BOM_LIST);
            bomList.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                    getLabel() + " - " + docPathSegment);
            bomList.setPropertyValue(
                    EloraMetadataConstants.ELORA_BOMLIST_BOMLIST, id);
            bomList = documentManager.createDocument(bomList);

            eloraDocumentRelationManager.addRelation(documentManager,
                    currentDocument, bomList,
                    EloraRelationConstants.BOM_HAS_LIST, null, "1");
            bomList = documentManager.saveDocument(bomList);

            documentManager.save();

            log.trace(logInitMsg + "Created BOM list |" + getLabel()
                    + "| for document |" + currentDocument.getId() + "|.");

            isListCreated = true;
            currentBomList = bomList;

            log.trace(logInitMsg + "--- EXIT --- ");
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.bom.createList"));

        }
    }

    private String getBomListsFolderPath() {

        String logInitMsg = "[getBomListsFolderPath] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel bomListsFolder = null;
        String bomListsFolderPath = null;

        // get parent folder
        DocumentModelList res = documentManager.query(String.format(
                "SELECT * from %s", EloraDoctypeConstants.BOM_LISTS_FOLDER));

        if (res == null || res.isEmpty()) {

            // if it doesn't exist, create it
            bomListsFolder = documentManager.createDocumentModel("/",
                    "bom-lists-folder", EloraDoctypeConstants.BOM_LISTS_FOLDER);
            bomListsFolder.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                    "BOM Lists");
            bomListsFolder = documentManager.createDocument(bomListsFolder);
            ACP acp = documentManager.getACP(bomListsFolder.getRef());
            ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
            acl.add(new ACE(SecurityConstants.EVERYONE, "ReadWriteAndRemove", true));
            documentManager.setACP(bomListsFolder.getRef(), acp, true);

        } else {
            if (res.size() > 1) {
                log.error("More than one BomListsFolder found:");
                for (DocumentModel model : res) {
                    log.warn(" - " + model.getName() + ", "
                            + model.getPathAsString());
                }
            }
            bomListsFolder = res.get(0);
        }

        bomListsFolderPath = bomListsFolder.getPathAsString();

        log.trace(logInitMsg + "bomListsFolderPath = |" + bomListsFolderPath
                + "|");
        log.trace(logInitMsg + "--- EXIT ---");

        return bomListsFolderPath;

    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        id = null;
        label = null;
        description = null;
        type = null;
        isListCreated = false;
        currentBomList = null;
        loadBomListData();
    }

}
