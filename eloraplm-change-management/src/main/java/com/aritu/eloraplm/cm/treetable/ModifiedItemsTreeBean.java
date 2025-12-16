package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

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
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.IMOptions;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraPageProvidersConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.UnlockCheckedOutDocumentException;
import com.aritu.eloraplm.pdm.promote.treetable.PromoteTreeBean;
import com.aritu.eloraplm.pdm.webapp.beans.PdmActionBean;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.webapp.base.beans.EloraWebActionsBean;
import com.aritu.eloraplm.webapp.util.EloraAjax;

@Name(ModifiedItemsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ModifiedItemsTreeBean extends CoreTree {

    public static final String NAME = "cmModifiedsTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ModifiedItemsTreeBean.class);

    private boolean reloadModifiedItems = false;

    private ModifiedItemsNodeService nodeService;

    private boolean modifiedsIMManaged;

    // private boolean IMAlreadyExists;

    protected IMOptions imOptions;

    // ---- Properties for managing "Add Modified Item" window ----
    // -- Modified Item Origin
    private String originItemWcUid;

    private List<SelectItem> originItemVersionList;

    private DocumentModel originItem;

    private DocumentModel originItemWc;

    private String originItemType;

    // -- Action
    private String action;

    // -- Modified Item Destination
    private String destinationItemWcUid;

    private DocumentModel destinationItem;

    private DocumentModel destinationItemWc;

    // -- IsManaged
    private boolean isManaged;

    // -- Comment
    private String comment;

    // -- IsImpactable and includeInImpactMatrix fields
    private boolean isImpactable;

    private boolean includeInImpactMatrix;

    // validity related fields
    private String originItemMsg;

    private String actionMsg;

    private String destinationItemMsg;

    private boolean isValid;

    // ------------------------------------------------------------
    // ---- Properties for managing "Edit Modified Item" window ----
    // -- Modified Item Origin
    private String editOriginItemWcUid;

    private DocumentModel editOriginItem;

    private DocumentModel editOriginItemWc;

    private String editOriginItemType;

    // -- Action
    private String editAction;

    // -- Modified Item Destination
    private String editDestinationItemWcUid;

    private DocumentModel editDestinationItem;

    private DocumentModel editDestinationItemWc;

    // -- IsManaged
    private boolean editIsManaged;

    // -- Comment
    private String editComment;

    // -- IsImpactable and includeInImpactMatrix fields
    private boolean editIsImpactable;

    private boolean editIncludeInImpactMatrix;

    // validity related fields
    private String editActionMsg;

    private String editDestinationItemMsg;

    private boolean editIsValid;

    @In(create = true)
    protected transient PdmActionBean pdmAction;

    @In(create = true)
    protected transient PromoteTreeBean promoteTreeBean;

    @In(create = true)
    protected EloraWebActionsBean eloraWebActions;

    public ModifiedItemsTreeBean() {
        resetAddModifiedItemFormValues();
        resetEditModifiedItemFormValues(true);
    }

    public void reloadTree(TreeNode selectedNode) {
        createRoot(selectedNode);
    }

    @Override
    public void createRoot() {
        createRoot(null);
    }

    public void createRoot(TreeNode selectedNode) {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();
            nodeService = new ModifiedItemsNodeService(documentManager,
                    selectedNode, messages);
            setRoot(nodeService.getRoot(currentDoc));
            setIsDirty(false);
            setHasUnreadableNodes(false);
            setIsInvalid(false);
        } catch (DocumentNotFoundException | DocumentSecurityException e) {
            log.error(logInitMsg + e.getMessage());
            // empty root attribute and set hasUnreadableNodes attribute to true
            setRoot(new DefaultTreeNode());
            setHasUnreadableNodes(true);
            setIsInvalid(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty root attribute and set isInvalid attribute to true
            setRoot(new DefaultTreeNode());
            setIsInvalid(true);
            setHasUnreadableNodes(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            saveCurrentTree();
            reloadModifiedItems = true;
            Events.instance().raiseEvent(CMEventNames.CM_MODIFIED_ITEMS_SAVED);
            // Nuxeo event
            EloraEventHelper.fireEvent(CMEventNames.CM_MODIFIED_ITEMS_SAVED,
                    navigationContext.getCurrentDocument());
        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void saveCurrentTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();

            ChangeManagementService cmService = Framework.getService(
                    ChangeManagementService.class);

            nodeService.saveTree(currentDoc, getRoot(), cmService);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.saveRoot"));

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.saveRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void refreshNode(TreeTable table, TreeNode node,
            String triggeredField, boolean updateChildren) {
        refreshNode(node, triggeredField);
        EloraAjax.updateTreeTableRow(table, node.getRowKey(), updateChildren);
    }

    public void refreshNode(TreeNode node, String triggeredField) {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {
            nodeService.refreshNode(node, triggeredField);

            if (!triggeredField.equals(
                    CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)
                    && !triggeredField.equals(
                            CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_SOURCE_VERSIONS)) {
                setIsDirty(true);
            }

            if (node.isSelected()) {
                setSelectedNodeAttributes(node);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        // log.trace(logInitMsg + "--- EXIT --- ");
    }

    public void markModifiedNodeAsRemoved(TreeTable table, TreeNode node) {
        markModifiedNodeAsRemoved(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void markModifiedNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[markModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleModifiedNodeAsRemoved(true, node);

        setIsDirty(true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void unmarkModifiedNodeAsRemoved(TreeTable table, TreeNode node) {
        unmarkModifiedNodeAsRemoved(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    public void unmarkModifiedNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[unmarkModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleModifiedNodeAsRemoved(false, node);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void toggleModifiedNodeAsRemoved(boolean isRemoved, TreeNode node) {
        String logInitMsg = "[toggleModifiedNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) node.getData();
        // If it is a new node, remove it completely
        if (nodeData.getIsNew()) {
            getRoot().getChildren().remove(node);
        } else {
            nodeData.setIsRemoved(isRemoved);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void addModifiedItem() {
        String logInitMsg = "[addModifiedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            setRoot(nodeService.addNewModifiedItem(getRoot(), originItem,
                    originItemWc, action, destinationItem, destinationItemWc,
                    isManaged, originItemType, comment, isImpactable,
                    includeInImpactMatrix));

            resetAddModifiedItemFormValues();

            setIsDirty(true);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.addModifiedItem"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private String validateModificationOriginValue() {

        String logInitMsg = "[validateModificationOriginValue] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = nodeService.validateModificationOriginValue(
                getRoot(), originItem.getId());

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    private String validateModificationDestinationValue() {

        String logInitMsg = "[validateModificationDestinationValue] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = null;

        // If action is replace, destination item cannot be the same as the
        // origin item and it cannot be the origin of a remove modification.
        if (action.equals(CMConstants.ACTION_REPLACE)) {
            if (destinationItemWcUid.equals(originItemWcUid)) {
                validationResultMessage = "eloraplm.message.error.cm.destinationItemSameAsOriginItem";
            } else {
                validationResultMessage = nodeService.validateReplaceDestinationValue(
                        getRoot(), destinationItemWcUid);
            }
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    private String validateModificationActionValue() {

        String logInitMsg = "[validateModificationActionValue] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = null;

        // The origin of a remove action cannot be the destination of a replace
        // action.
        if (action.equals(CMConstants.ACTION_REMOVE)) {
            validationResultMessage = nodeService.validateRemoveOriginValue(
                    getRoot(), originItemWcUid);
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    public void editModifiedItem() {
        String logInitMsg = "[editModifiedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) getSelectedNode().getData();

            /*
             * // if editAction or editDestinationItem changes, it means that
             * // impacted items should be recalculated.
             * if (selectedNodeData.getAction() != editAction
             * || selectedNodeData.getDestinationItem() != editDestinationItem) {
             * selectedNodeData.setActionChanged(true);
             * }
             */

            selectedNodeData.setAction(editAction);
            selectedNodeData.setDestinationItem(editDestinationItem);
            selectedNodeData.setDestinationItemWc(editDestinationItemWc);
            selectedNodeData.setIsManaged(editIsManaged);

            List<SelectItem> editDestinationItemVersionList;

            // if the destinationItemWc is not null, initialize the version list with the WC
            if (editDestinationItemWc != null) {
                editDestinationItemVersionList = new ArrayList<SelectItem>();
                editDestinationItemVersionList.add(
                        new SelectItem(editDestinationItemWc,
                                editDestinationItemWc.getVersionLabel()
                                        + CMConstants.VERSION_LABEL_WC));
            } else {
                editDestinationItemVersionList = null;
            }
            selectedNodeData.setDestinationItemVersionList(
                    editDestinationItemVersionList);

            DocumentModel defaultDestinationItemSourceVersion = CMHelper.calculateDefaultDestinationItemSourceVersion(
                    editDestinationItemWc, editOriginItem, editAction);
            selectedNodeData.setDestinationItemSourceVersion(
                    defaultDestinationItemSourceVersion);

            CMTreeBeanHelper.loadDestinationSourceVersions(selectedNodeData,
                    documentManager, messages);

            // Calculate if editable fields are editable or not in
            // function of the item values
            boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper
                    .calculateDestinationItemVersionListIsReadOnlyValue(
                            editAction, editIsManaged);
            selectedNodeData.setDestinationItemVersionIsReadOnly(
                    destinationItemVersionIsReadOnly);

            boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                    .calculateDestinationItemSourceVersionIsReadOnlyValue(
                            editDestinationItem, editIsManaged);
            selectedNodeData.setDestinationItemSourceVersionIsReadOnly(
                    destinationItemSourceVersionIsReadOnly);

            boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                    editAction, editDestinationItem);
            selectedNodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
            selectedNodeData.setComment(editComment);
            selectedNodeData.setIsImpactable(editIsImpactable);
            selectedNodeData.setIncludeInImpactMatrix(
                    editIncludeInImpactMatrix);

            selectedNodeData.setIsModified(true);

            setIsDirty(true);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.editModifiedItem"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void checkIn(DocumentModel doc) {
        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != null) {
                ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) selectedNode.getData();
                if (isCheckinAllowed(selectedNodeData)) {

                    String checkinComment = selectedNodeData.getComment();
                    pdmAction.setCheckinComment(checkinComment);
                    pdmAction.checkIn(doc);

                    // Set as managed
                    if (!selectedNodeData.getIsManaged()) {
                        try {
                            CMHelper.setAsManagedModifiedItemByNodeId(
                                    documentManager,
                                    navigationContext.getCurrentDocument(),
                                    selectedNodeData.getNodeId());
                        } catch (EloraException e) {
                            facesMessages.add(StatusMessage.Severity.ERROR,
                                    messages.get(
                                            "eloraplm.message.error.cm.failToSetAsManagedModifiedItem"));
                        }
                    }
                    reloadTree(selectedNode);
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.emptyItemSelection"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
    }

    public void checkOut() {
        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != null) {
                ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) selectedNode.getData();
                if (isNodeStateAllowed(selectedNodeData)) {
                    DocumentModel selectedDoc = selectedNodeData.getDestinationItem();
                    if (selectedNodeData.getDestinationItemSourceVersion() != null) {
                        VersionModel versionFrom = new VersionModelImpl();
                        versionFrom.setId(
                                selectedNodeData.getDestinationItemSourceVersion().getId());
                        pdmAction.checkOutFromVersion(selectedDoc, versionFrom);

                    } else {
                        pdmAction.checkOutFromVersion(selectedDoc, null);
                    }
                    reloadTree(selectedNode);
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.emptyItemSelection"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
    }

    public void runPromote() {
        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != null) {
                promoteTreeBean.runPromoteAction();

                // Set as managed
                ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) selectedNode.getData();
                if (!selectedNodeData.getIsManaged()) {
                    try {
                        CMHelper.setAsManagedModifiedItemByNodeId(
                                documentManager,
                                navigationContext.getCurrentDocument(),
                                selectedNodeData.getNodeId());
                    } catch (EloraException e) {
                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.cm.failToSetAsManagedModifiedItem"));
                    }
                }
                reloadTree(selectedNode);
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
    }

    private String validateEditModificationDestinationValue() {
        String logInitMsg = "[validateEditModificationDestinationValue] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = null;

        // If action is replace, destination item cannot be the same as the
        // origin item and it cannot be the origin of a remove modification.
        if (editAction.equals(CMConstants.ACTION_REPLACE)) {
            if (editDestinationItemWcUid.equals(editOriginItemWcUid)) {
                validationResultMessage = "eloraplm.message.error.cm.destinationItemSameAsOriginItem";
            } else {
                validationResultMessage = nodeService.validateReplaceDestinationValue(
                        getRoot(), editDestinationItemWcUid);
            }
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    private String validateEditModificationActionValue() {
        String logInitMsg = "[validateEditModificationActionValue] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = null;

        // The origin of a remove action cannot be the destination of a replace
        // action.
        if (editAction.equals(CMConstants.ACTION_REMOVE)) {
            validationResultMessage = nodeService.validateRemoveOriginValue(
                    getRoot(), editOriginItemWcUid);
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    public void lock() {
        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != null) {
                ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) selectedNode.getData();
                DocumentModel selectedDoc = (selectedNodeData.getDestinationItem() != null
                        ? selectedNodeData.getDestinationItem()
                        : selectedNodeData.getOriginItem());
                try {
                    EloraDocumentHelper.lockDocument(selectedDoc);
                    reloadTree(selectedNode);
                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get("eloraplm.message.success.lock"));
                } catch (DocumentAlreadyLockedException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.locked"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                } catch (DocumentInUnlockableStateException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.not.lockable"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                } catch (DocumentLockRightsException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.lock.rights"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                } catch (EloraException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.error.cm.lock"));
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.emptyItemSelection"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
    }

    public void unlock() {
        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != null) {
                ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) selectedNode.getData();
                DocumentModel selectedDoc = (selectedNodeData.getDestinationItem() != null
                        ? selectedNodeData.getDestinationItem()
                        : selectedNodeData.getOriginItem());
                try {
                    EloraDocumentHelper.unlockDocument(selectedDoc);
                    reloadTree(selectedNode);
                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get("eloraplm.message.success.unlock"));
                } catch (DocumentLockRightsException e) {
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.unlock.permissions"));
                } catch (UnlockCheckedOutDocumentException e) {
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.unlock.checkedOut"));
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.emptyItemSelection"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }
    }

    protected boolean isCheckinAllowed(ModifiedItemsNodeData selectedNodeData) {
        if (selectedNodeData.getDestinationItem().isCheckedOut()) {
            return isNodeStateAllowed(selectedNodeData);
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.isNotCheckedOut"));
        }
        return false;
    }

    protected boolean isNodeStateAllowed(
            ModifiedItemsNodeData selectedNodeData) {
        if (!selectedNodeData.getIsManaged()) {
            if (!selectedNodeData.getAction().equals(CMConstants.ACTION_IGNORE)
                    && !selectedNodeData.getAction().equals(
                            CMConstants.ACTION_REMOVE)) {
                return true;
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.incorrectAction"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.isManaged"));
        }
        return false;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetAddModifiedItemFormValues();

        resetEditModifiedItemFormValues(true);

        createRoot();
    }

    public String getPageProviderNameForAddModifiedItemReplaceDestinationItem() {
        return getPageProviderNameForReplaceDestinationItem(originItemType,
                originItem.getType());
    }

    public String getPageProviderNameForEditModifiedItemReplaceDestinationItem() {
        return getPageProviderNameForReplaceDestinationItem(editOriginItemType,
                editOriginItem.getType());
    }

    private String getPageProviderNameForReplaceDestinationItem(
            String originItemType, String originItemDocType) {
        String pageProviderName = "";

        switch (originItemType) {
            case CMConstants.ITEM_TYPE_DOC:
                switch (originItemDocType) {
                    case (EloraDoctypeConstants.CAD_ASSEMBLY):
                        pageProviderName = EloraPageProvidersConstants.CAD_ASSEMBLY_OR_CAD_PART_WC_DOC_SUGG;
                        break;
                    case (EloraDoctypeConstants.CAD_PART):
                        pageProviderName = EloraPageProvidersConstants.CAD_ASSEMBLY_OR_CAD_PART_WC_DOC_SUGG;
                        break;
                    case (EloraDoctypeConstants.CAD_DRAWING):
                        pageProviderName = EloraPageProvidersConstants.CAD_DRAWING_WC_DOC_SUGG;
                        break;
                    case (EloraDoctypeConstants.CAD_DESIGN_TABLE):
                        pageProviderName = EloraPageProvidersConstants.CAD_DESIGN_TABLE_WC_DOC_SUGG;
                        break;
                    default:
                        pageProviderName = EloraPageProvidersConstants.CAD_OTHER_WC_DOC_SUGG;
                }
                break;
            case CMConstants.ITEM_TYPE_BOM:
                if (EloraDocumentTypesHelper.getDocumentType(
                        EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                EloraDocumentTypesHelper.getDocumentType(
                                        originItemDocType))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_PART_WC_DOC_SUGG;
                } else if (originItemDocType.equals(
                        (EloraDoctypeConstants.BOM_TOOL))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_TOOL_WC_DOC_SUGG;
                } else if (originItemDocType.equals(
                        (EloraDoctypeConstants.BOM_PACKAGING))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_PACKAGING_WC_DOC_SUGG;
                } else if (originItemDocType.equals(
                        (EloraDoctypeConstants.BOM_SPECIFICATION))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_SPECIFICATION_WC_DOC_SUGG;
                } else if (originItemDocType.equals(
                        (EloraDoctypeConstants.BOM_PRODUCT))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_PRODUCT_WC_DOC_SUGG;
                } else if (originItemDocType.equals(
                        (EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT))) {
                    pageProviderName = EloraPageProvidersConstants.BOM_CUSTOMER_PRODUCT_WC_DOC_SUGG;
                } else {
                    pageProviderName = EloraPageProvidersConstants.BOM_WC_DOC_SUGG;
                }
                break;
            default:
                pageProviderName = EloraPageProvidersConstants.CAD_BOM_WC_DOC_SUGG;
        }

        return pageProviderName;
    }

    @Override
    public void setSelectedNode(TreeNode selectedNode) {
        super.setSelectedNode(selectedNode);

        if (selectedNode != null) {
            setSelectedNodeAttributes(selectedNode);
        }
    }

    private void setSelectedNodeAttributes(TreeNode node) {
        ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) node.getData();

        setEditOriginItemWcUid(selectedNodeData.getOriginItemWc().getId());

        setEditOriginItemWc(selectedNodeData.getOriginItemWc());

        setEditOriginItem(selectedNodeData.getOriginItem());

        setEditDestinationItem(selectedNodeData.getDestinationItem());

        setEditAction(selectedNodeData.getAction());

        setEditDestinationItemWc(selectedNodeData.getDestinationItemWc());
        if (selectedNodeData.getDestinationItemWc() != null) {
            setEditDestinationItemWcUid(
                    selectedNodeData.getDestinationItemWc().getId());
        } else {
            setEditDestinationItemWcUid(null);
        }

        setEditIsManaged(selectedNodeData.getIsManaged());

        setEditComment(selectedNodeData.getComment());

        setEditIsImpactable(selectedNodeData.getIsImpactable());

        setEditIncludeInImpactMatrix(
                selectedNodeData.getIncludeInImpactMatrix());
    }

    // ---- Methods for managing "Add Modified Item" window ----

    public String getOriginItemWcUid() {
        return originItemWcUid;
    }

    public void setOriginItemWcUid(String originItemWcUid) {
        String logInitMsg = "[setOriginItemWcUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            resetAddModifiedItemFormValues();

            this.originItemWcUid = originItemWcUid;

            if (originItemWcUid != null) {

                setOriginItemWc(documentManager.getDocument(
                        new IdRef(originItemWcUid)));

                // *******************************************************
                // LEIRE: this change is related to JIRA: ELO-491
                // Allow selecting any version, don't restrict to released
                // versions
                boolean onlyReleasedVersion = false;
                /*
                 * // if the origin is a BOM or CAD, retrieve only RELEASED
                 * // versions of the document, otherwise all the versions
                 * 
                 * if (getOriginItemWc().hasFacet(
                 * EloraFacetConstants.FACET_CAD_DOCUMENT)
                 * || getOriginItemWc().hasFacet(
                 * EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                 * onlyReleasedVersion = true;
                 * }
                 */
                // *******************************************************

                // Calculate the version list for the selected origin item
                // and the default selected value
                List<SelectItem> versionList = CMHelper.calculateOriginVersionList(
                        documentManager, originItemWcUid, onlyReleasedVersion,
                        messages);
                setOriginItemVersionList(versionList);

                // By default, select the first element of the list
                if (versionList != null && versionList.size() > 0) {
                    DocumentModel originItem = CMHelper.getFirstDocumentFromVersionList(versionList);
                    setOriginItem(originItem);
                }
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    public List<SelectItem> getOriginItemVersionList() {
        return originItemVersionList;
    }

    public void setOriginItemVersionList(
            List<SelectItem> originItemVersionList) {
        this.originItemVersionList = originItemVersionList;
    }

    public DocumentModel getOriginItem() {
        return originItem;
    }

    public void setOriginItem(DocumentModel originItem) {
        String logInitMsg = "[setOriginItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        this.originItem = originItem;
        if (originItem != null) {
            try {

                if (originItem.getId() != null) {

                    String validationResultMessage = validateModificationOriginValue();

                    if (validationResultMessage != null
                            && validationResultMessage.length() > 0) {
                        setOriginItemMsg(messages.get(validationResultMessage));
                        setDestinationItemWcUid(null);
                        setDestinationItem(null);
                        setIsManaged(false);
                        setComment(null);
                        setIsValid(false);

                    } else {
                        setOriginItemMsg(null);
                        // initialize action to default value
                        setAction(null);
                    }

                } else {
                    setOriginItemMsg(null);
                }

            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.setOriginItem"));
            }

            setOriginItemType(CMHelper.getItemType(originItem));
        }
    }

    public DocumentModel getOriginItemWc() {
        return originItemWc;
    }

    public void setOriginItemWc(DocumentModel originItemWc) {
        this.originItemWc = originItemWc;
    }

    public String getOriginItemType() {
        return originItemType;
    }

    public void setOriginItemType(String originItemType) {
        this.originItemType = originItemType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        String logInitMsg = "[setAction] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            if (action == null) {
                action = CMConstants.ACTION_CHANGE;
            }
            this.action = action;

            String validationResultMessage = validateModificationActionValue();

            if (validationResultMessage != null
                    && validationResultMessage.length() > 0) {
                setActionMsg(messages.get(validationResultMessage));
                setDestinationItemWcUid(null);
                setDestinationItem(null);
                setIsManaged(false);
                setComment(null);
                setIsValid(false);
            } else {
                setActionMsg(null);
                setComment(CMTreeBeanHelper.calculateComment(
                        getCurrentDocument(), action));

                if (action.equals(CMConstants.ACTION_REMOVE)
                        || action.equals(CMConstants.ACTION_IGNORE)) {
                    setDestinationItem(null);
                    setDestinationItemWc(null);
                    setIsManaged(true);
                    setIsValid(true);
                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceDestinationSelectionElements();

                } else if (action.equals(CMConstants.ACTION_CHANGE)) {
                    setDestinationItem(originItemWc);
                    setDestinationItemWc(originItemWc);
                    setIsManaged(false);
                    setIsValid(true);
                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceDestinationSelectionElements();

                } else if (action.equals(CMConstants.ACTION_REPLACE)) {
                    setDestinationItem(null);
                    setDestinationItemWcUid(null);
                    setIsManaged(false);
                    setIsValid(false);
                }

                setIsImpactable(CMHelper.getIsImpactable(originItem.getType(),
                        action, destinationItem));
                setIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                originItem.getType(), action,
                                destinationItem));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setAction"));
        }
    }

    // TODO::: begiratu ea hau beharrezkoa dan...
    private void clearReplaceDestinationSelectionElements() {
        destinationItemWcUid = null;
        destinationItemMsg = null;

        // TODO::: hemen gethitu comment eta includeInImpactMatrix !!!
    }

    public String getDestinationItemWcUid() {
        return destinationItemWcUid;
    }

    public void setDestinationItemWcUid(String destinationItemWcUid) {
        String logInitMsg = "[setDestinationItemWcUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.destinationItemWcUid = destinationItemWcUid;

            if (destinationItemWcUid != null) {

                String validationResultMessage = validateModificationDestinationValue();

                if (validationResultMessage != null
                        && validationResultMessage.length() > 0) {
                    setDestinationItemMsg(
                            messages.get(validationResultMessage));

                } else {
                    setDestinationItemMsg(null);

                    DocumentModel destinationItemWcDoc = documentManager.getDocument(
                            new IdRef(destinationItemWcUid));
                    setDestinationItemWc(destinationItemWcDoc);

                    if (action.equals(CMConstants.ACTION_REPLACE)) {
                        setDestinationItem(EloraDocumentHelper.getBaseVersion(
                                destinationItemWc));
                        setIsManaged(true);
                    } else {
                        setDestinationItem(destinationItemWcDoc);
                    }

                    setIsImpactable(
                            CMHelper.getIsImpactable(originItem.getType(),
                                    action, destinationItem));
                    setIncludeInImpactMatrix(
                            CMHelper.getIncludeInImpactMatrixDefaultValue(
                                    originItem.getType(), action,
                                    destinationItem));

                    setIsValid(true);
                }
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public DocumentModel getDestinationItem() {
        return destinationItem;
    }

    public void setDestinationItem(DocumentModel destinationItem) {
        String logInitMsg = "[setDestinationItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.destinationItem = destinationItem;

            if (destinationItem == null) {
                setIsManaged(false);
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public DocumentModel getDestinationItemWc() {
        return destinationItemWc;
    }

    public void setDestinationItemWc(DocumentModel destinationItemWc) {
        this.destinationItemWc = destinationItemWc;
    }

    public boolean getIsManaged() {
        return isManaged;
    }

    public void setIsManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean getIsImpactable() {
        return isImpactable;
    }

    public void setIsImpactable(boolean isImpactable) {
        this.isImpactable = isImpactable;
    }

    public boolean getIncludeInImpactMatrix() {
        return includeInImpactMatrix;
    }

    public void setIncludeInImpactMatrix(boolean includeInImpactMatrix) {
        this.includeInImpactMatrix = includeInImpactMatrix;
    }

    public String getOriginItemMsg() {
        return originItemMsg;
    }

    public void setOriginItemMsg(String originItemMsg) {
        this.originItemMsg = originItemMsg;
    }

    public String getActionMsg() {
        return actionMsg;
    }

    public void setActionMsg(String actionMsg) {
        this.actionMsg = actionMsg;
    }

    public String getDestinationItemMsg() {
        return destinationItemMsg;
    }

    public void setDestinationItemMsg(String destinationItemMsg) {
        this.destinationItemMsg = destinationItemMsg;
    }

    public boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    private void resetAddModifiedItemFormValues() {

        originItemWcUid = null;

        originItemVersionList = new ArrayList<SelectItem>();

        originItem = null;

        originItemWc = null;

        originItemType = null;

        action = null;

        destinationItemWcUid = null;

        destinationItem = null;

        destinationItemWc = null;

        isManaged = false;

        comment = null;

        isImpactable = false;

        includeInImpactMatrix = false;

        originItemMsg = null;

        actionMsg = null;

        destinationItemMsg = null;

        isValid = false;
    }

    // ---------------------------------------------------------
    // ---- Methods for managing "Edit Modified Item" window ----
    public String getEditOriginItemWcUid() {
        return editOriginItemWcUid;
    }

    public void setEditOriginItemWcUid(String editOriginItemWcUid) {
        this.editOriginItemWcUid = editOriginItemWcUid;
    }

    public DocumentModel getEditOriginItem() {
        return editOriginItem;
    }

    public void setEditOriginItem(DocumentModel editOriginItem) {
        this.editOriginItem = editOriginItem;
        if (editOriginItem != null) {
            setEditOriginItemType(CMHelper.getItemType(editOriginItem));
        }
    }

    public DocumentModel getEditOriginItemWc() {
        return editOriginItemWc;
    }

    public void setEditOriginItemWc(DocumentModel editOriginItemWc) {
        this.editOriginItemWc = editOriginItemWc;
    }

    public String getEditOriginItemType() {
        return editOriginItemType;
    }

    public void setEditOriginItemType(String editOriginItemType) {
        this.editOriginItemType = editOriginItemType;
    }

    public String getEditAction() {
        return editAction;
    }

    public void setEditAction(String editAction) {
        String logInitMsg = "[editAction] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            if (editAction == null) {
                editAction = CMConstants.ACTION_CHANGE;
            }
            this.editAction = editAction;

            String validationResultMessage = validateEditModificationActionValue();

            if (validationResultMessage != null
                    && validationResultMessage.length() > 0) {
                setEditActionMsg(messages.get(validationResultMessage));
                setEditDestinationItemWcUid(null);
                setEditDestinationItem(null);
                setEditIsManaged(false);
                setEditComment(null);
                setEditIsValid(false);

            } else {
                setEditActionMsg(null);

                setEditComment(CMTreeBeanHelper.calculateComment(
                        getCurrentDocument(), editAction));

                if (editAction.equals(CMConstants.ACTION_REMOVE)
                        || editAction.equals(CMConstants.ACTION_IGNORE)) {
                    setEditDestinationItemWc(null);
                    setEditDestinationItem(null);
                    setEditIsManaged(true);
                    setEditIsValid(true);

                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceEditDestinationSelectionElements();

                } else if (editAction.equals(CMConstants.ACTION_CHANGE)) {
                    setEditDestinationItemWc(editOriginItemWc);
                    setEditDestinationItem(editOriginItemWc);
                    setEditIsManaged(false);
                    setEditIsValid(true);
                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceEditDestinationSelectionElements();

                } else if (editAction.equals(CMConstants.ACTION_REPLACE)) {
                    setEditDestinationItemWcUid(null);
                    setEditDestinationItemWc(null);
                    setEditDestinationItem(null);
                    setEditIsManaged(false);
                    setEditIsValid(false);
                }

                setEditIsImpactable(
                        CMHelper.getIsImpactable(editOriginItem.getType(),
                                editAction, editDestinationItem));
                setEditIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                editOriginItem.getType(), editAction,
                                editDestinationItem));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setAction"));
        }
    }

    // TODO:: ??? ezabatu
    private void clearReplaceEditDestinationSelectionElements() {
        editDestinationItemWcUid = null;
        editDestinationItemMsg = null;
    }

    public String getEditDestinationItemWcUid() {
        return editDestinationItemWcUid;
    }

    public void setEditDestinationItemWcUid(String editDestinationItemWcUid) {
        String logInitMsg = "[setEditDestinationItemWcUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.editDestinationItemWcUid = editDestinationItemWcUid;

            if (editDestinationItemWcUid != null) {

                String validationResultMessage = validateEditModificationDestinationValue();

                if (validationResultMessage != null
                        && validationResultMessage.length() > 0) {
                    setEditDestinationItemMsg(
                            messages.get(validationResultMessage));

                } else {
                    setEditDestinationItemMsg(null);

                    DocumentModel editDestinationItemWcDoc = documentManager.getDocument(
                            new IdRef(editDestinationItemWcUid));
                    setEditDestinationItemWc(editDestinationItemWcDoc);

                    if (editAction.equals(CMConstants.ACTION_REPLACE)) {
                        setEditDestinationItem(EloraDocumentHelper.getBaseVersion(
                                editDestinationItemWcDoc));
                        setEditIsManaged(true);
                    } else {
                        setEditDestinationItem(editDestinationItemWcDoc);
                    }

                    setEditIsImpactable(
                            CMHelper.getIsImpactable(editOriginItem.getType(),
                                    editAction, editDestinationItem));
                    setEditIncludeInImpactMatrix(
                            CMHelper.getIncludeInImpactMatrixDefaultValue(
                                    editOriginItem.getType(), editAction,
                                    editDestinationItem));

                    setEditIsValid(true);
                }
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public DocumentModel getEditDestinationItem() {
        return editDestinationItem;
    }

    public void setEditDestinationItem(DocumentModel editDestinationItem) {
        this.editDestinationItem = editDestinationItem;
    }

    public DocumentModel getEditDestinationItemWc() {
        return editDestinationItemWc;
    }

    public void setEditDestinationItemWc(DocumentModel editDestinationItemWc) {
        this.editDestinationItemWc = editDestinationItemWc;
    }

    public boolean getEditIsManaged() {
        return editIsManaged;
    }

    public void setEditIsManaged(boolean editIsManaged) {
        this.editIsManaged = editIsManaged;
    }

    public String getEditComment() {
        return editComment;
    }

    public void setEditComment(String editComment) {
        this.editComment = editComment;
    }

    public boolean getEditIsImpactable() {
        return editIsImpactable;
    }

    public void setEditIsImpactable(boolean editIsImpactable) {
        this.editIsImpactable = editIsImpactable;
    }

    public boolean getEditIncludeInImpactMatrix() {
        return editIncludeInImpactMatrix;
    }

    public void setEditIncludeInImpactMatrix(
            boolean editIncludeInImpactMatrix) {
        this.editIncludeInImpactMatrix = editIncludeInImpactMatrix;
    }

    public String getEditActionMsg() {
        return editActionMsg;
    }

    public void setEditActionMsg(String editActionMsg) {
        this.editActionMsg = editActionMsg;
    }

    public String getEditDestinationItemMsg() {
        return editDestinationItemMsg;
    }

    public void setEditDestinationItemMsg(String editDestinationItemMsg) {
        this.editDestinationItemMsg = editDestinationItemMsg;
    }

    public boolean getEditIsValid() {
        return editIsValid;
    }

    public void setEditIsValid(boolean editIsValid) {
        this.editIsValid = editIsValid;
    }

    public boolean getModifiedsIMManaged() {
        modifiedsIMManaged = CMHelper.checkModifiedsWithIMAreManaged(
                navigationContext.getCurrentDocument());
        return modifiedsIMManaged;
    }

    public void setModifiedsIMManaged(Boolean modifiedsIMManaged) {
        this.modifiedsIMManaged = modifiedsIMManaged;
    }

    public void setImOptions(IMOptions imOptions) {
        this.imOptions = imOptions;
    }

    public IMOptions getImOptions() {
        if (imOptions == null) {
            imOptions = CMHelper.loadIMOptions(
                    navigationContext.getCurrentDocument());
        }
        return imOptions;
    }

    public void resetEditModifiedItemFormValues(boolean includeOriginItem) {

        if (includeOriginItem) {
            editOriginItemWcUid = null;

            editOriginItem = null;

            editOriginItemWc = null;

            editOriginItemType = null;
        }

        editAction = null;

        editDestinationItemWcUid = null;

        editDestinationItem = null;

        editDestinationItemWc = null;

        editIsManaged = false;

        editComment = null;

        editIsImpactable = false;

        editIncludeInImpactMatrix = false;

        editAction = null;

        editDestinationItemMsg = null;

        editIsValid = false;
    }

    @Override
    public TreeNode getRoot() {
        if (reloadModifiedItems) {
            createRoot();
            reloadModifiedItems = false;
        }
        return super.getRoot();
    }

    @Observer(value = { CMEventNames.CM_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_MODIFIED_ITEMS_DERIVED_FROM_IMPACTED_DOCS_ADDED,
            CMEventNames.CM_REFRESH_MODIFIED_ITEMS })
    @BypassInterceptors
    public void markTreeToBeReloaded() {
        reloadModifiedItems = true;
    }

}
