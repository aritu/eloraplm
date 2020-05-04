package com.aritu.eloraplm.cm.treetable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.event.ActionEvent;
import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraPageProvidersConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class ModifiedItemsTreeBean extends CoreTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ModifiedItemsTreeBean.class);

    private String itemType;

    private ModifiedItemsNodeService nodeService;

    // ---- Properties for managing "Add Modified Item" window ----
    // -- Modified Item Origin
    private String originItemWcUid;

    private String originItemRealUid;

    private Map<String, String> originItemVersionList;

    private DocumentModel originItem;

    private DocumentModel originItemWc;

    private String originItemType;

    // -- Action
    private String action;

    // -- Modified Item Destination
    private String destinationItemWcUid;

    private String destinationItemRealUid;

    /*private Map<String, String> destinationItemVersionList;*/

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

    private String editOriginItemRealUid;

    private DocumentModel editOriginItem;

    private DocumentModel editOriginItemWc;

    private String editOriginItemType;

    // -- Action
    private String editAction;

    // -- Modified Item Destination
    private String editDestinationItemWcUid;

    private String editDestinationItemRealUid;

    /*private Map<String, String> editDestinationItemVersionList;*/

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

    // ------------------------------------------------------------

    public ModifiedItemsTreeBean(String itemType) {
        this.itemType = itemType;

        resetAddModifiedItemFormValues();

        resetEditModifiedItemFormValues(true);
    }

    /**
     * @return the itemType
     */
    public String getItemType() {
        return itemType;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();
            nodeService = new ModifiedItemsNodeService(documentManager,
                    itemType);
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

        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();

            nodeService.saveTree(currentDoc, getRoot());

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.saveRoot"));

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.saveRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void calculateImpactMatrix() {
        String logInitMsg = "[calculateImpactMatrix] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();

            CMHelper.calculateImpactMatrix(documentManager, currentDoc,
                    itemType);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.calculateImpactMatrix"));

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.calculateImpactMatrix"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void refreshNode(TreeTable table, TreeNode node,
            String triggeredField, boolean updateChildren)
            throws EloraException {
        refreshNode(node, triggeredField);
        EloraAjax.updateTreeTableRow(table, node.getRowKey(), updateChildren);
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {

            nodeService.refreshNode(node, triggeredField);

            if (!triggeredField.equals(
                    CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)) {
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
            node.getChildren().clear();
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
                getRoot(), originItemRealUid);

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

        // The origin of a replace action cannot be the destination of a replace
        // action.
        if (action.equals(CMConstants.ACTION_REMOVE)) {
            validationResultMessage = nodeService.validateRemoveOriginValue(
                    getRoot(), originItemWcUid);
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    public void cancelAddModifiedItemActionListener(ActionEvent event) {
        String logInitMsg = "[cancelAddModifiedItemActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        resetAddModifiedItemFormValues();

        resetAddModifiedItemFormInputValues(event.getComponent(), true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void editModifiedItem() {
        String logInitMsg = "[editModifiedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) getSelectedNode().getData();

            /*// if editAction or editDestinationItem changes, it means that
            // impacted items should be recalculated.
            if (selectedNodeData.getAction() != editAction
                    || selectedNodeData.getDestinationItem() != editDestinationItem) {
                selectedNodeData.setActionChanged(true);
            }*/

            selectedNodeData.setAction(editAction);
            selectedNodeData.setDestinationItem(editDestinationItem);
            selectedNodeData.setDestinationItemWc(editDestinationItemWc);
            /*selectedNodeData.setDestinationItemVersionList(
                    editDestinationItemVersionList);*/
            selectedNodeData.setIsManaged(editIsManaged);

            ////////////// todo... hau kanpora atera ??????????????????

            Map<String, String> editDestinationItemVersionList = new HashMap<String, String>();
            // if the destinationItem is the WC, initialize the version list
            // with
            // the WC
            if (editDestinationItem != null && editDestinationItemWc != null
                    && editDestinationItem.getId().equals(
                            editDestinationItemWc.getId())) {
                // initialize the version list with the WC

                editDestinationItemVersionList.put(editDestinationItem.getId(),
                        editDestinationItem.getVersionLabel() + " (WC)");
            } else {
                editDestinationItemVersionList = null;
            }
            selectedNodeData.setDestinationItemVersionList(
                    editDestinationItemVersionList);

            /////////////////////////////

            // Calculate if editable fields are editable or not in
            // function of the item values
            boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper.calculateDestinationItemVersionListIsReadOnlyValue(
                    editAction, editIsManaged);

            selectedNodeData.setDestinationItemVersionIsReadOnly(
                    destinationItemVersionIsReadOnly);

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

        // The origin of a replace action cannot be the destination of a replace
        // action.
        if (editAction.equals(CMConstants.ACTION_REMOVE)) {
            validationResultMessage = nodeService.validateRemoveOriginValue(
                    getRoot(), editOriginItemWcUid);
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    public void cancelEditModifiedItemActionListener(ActionEvent event) {
        String logInitMsg = "[cancelEditModifiedItemActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // resetEditModifiedItemFormValues(true);

        resetEditModifiedItemFormInputValues(event.getComponent(), true); // ?????????????

        log.trace(logInitMsg + "--- EXIT ---");
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
            switch (originItemDocType) {
            case (EloraDoctypeConstants.BOM_PART):
                pageProviderName = EloraPageProvidersConstants.BOM_PART_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_MANUFACTURER_PART):
                pageProviderName = EloraPageProvidersConstants.BOM_MANUFACTURER_PART_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_TOOL):
                pageProviderName = EloraPageProvidersConstants.BOM_TOOL_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_PACKAGING):
                pageProviderName = EloraPageProvidersConstants.BOM_PACKAGING_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_SPECIFICATION):
                pageProviderName = EloraPageProvidersConstants.BOM_SPECIFICATION_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_PRODUCT):
                pageProviderName = EloraPageProvidersConstants.BOM_PRODUCT_WC_DOC_SUGG;
                break;
            case (EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT):
                pageProviderName = EloraPageProvidersConstants.BOM_CUSTOMER_PRODUCT_WC_DOC_SUGG;
                break;
            default:
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

        setEditOriginItemRealUid(selectedNodeData.getOriginItem().getId());

        setEditOriginItemWc(selectedNodeData.getOriginItemWc());

        setEditOriginItem(selectedNodeData.getOriginItem());

        setEditAction(selectedNodeData.getAction());

        setEditDestinationItem(selectedNodeData.getDestinationItem());
        if (selectedNodeData.getDestinationItem() != null) {
            setEditDestinationItemRealUid(
                    selectedNodeData.getDestinationItem().getId());
        } else {
            setEditDestinationItemRealUid(null);
        }

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
                // if the origin is a BOM or CAD, retrieve only RELEASED
                // versions of the document, otherwise all the versions

                if (getOriginItemWc().hasFacet(
                        EloraFacetConstants.FACET_CAD_DOCUMENT)
                        || getOriginItemWc().hasFacet(
                                EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                    onlyReleasedVersion = true;
                }*/
                // *******************************************************

                // Calculate the version list for the selected origin item
                // and the default selected value
                Map<String, String> versionList = CMHelper.calculateOriginVersionList(
                        documentManager, originItemWcUid, onlyReleasedVersion);
                setOriginItemVersionList(versionList);

                // By default, select the last element of the list
                if (versionList != null && versionList.size() > 0) {
                    setOriginItemRealUid(
                            CMHelper.getLastElementValueFromVersionList(
                                    versionList));
                }
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    public String getOriginItemRealUid() {
        return originItemRealUid;
    }

    public void setOriginItemRealUid(String originItemRealUid) {
        String logInitMsg = "[setOriginItemRealUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.originItemRealUid = originItemRealUid;

            if (originItemRealUid != null) {

                String validationResultMessage = validateModificationOriginValue();

                if (validationResultMessage != null
                        && validationResultMessage.length() > 0) {
                    setOriginItemMsg(messages.get(validationResultMessage));
                    setOriginItem(null);
                    setDestinationItemWcUid(null);
                    setDestinationItemRealUid(null);
                    setIsManaged(false);
                    setComment(null);
                    setIsValid(false);

                } else {
                    setOriginItemMsg(null);
                    // retrieve the originItem from its id
                    setOriginItem(documentManager.getDocument(
                            new IdRef(originItemRealUid)));
                    // initialize action to default value
                    setAction(null);
                }

            } else {
                setOriginItemMsg(null);
                setOriginItem(null);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    public Map<String, String> getOriginItemVersionList() {
        return originItemVersionList;
    }

    public void setOriginItemVersionList(
            Map<String, String> originItemVersionList) {
        this.originItemVersionList = originItemVersionList;
    }

    public DocumentModel getOriginItem() {
        return originItem;
    }

    public void setOriginItem(DocumentModel originItem) {
        this.originItem = originItem;
        if (originItem != null) {
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
                setDestinationItemRealUid(null);
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
                    setDestinationItemWcUid(null);
                    setDestinationItemRealUid(null);
                    setIsManaged(false);
                    setIsValid(false);
                }

                setIsImpactable(CMHelper.getIsImpactable(originItem.getType(),
                        action, destinationItemRealUid));
                setIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                originItem.getType(), action,
                                destinationItemRealUid));
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
        destinationItemRealUid = null;
        destinationItemMsg = null;
        /*   destinationItemVersionList = new HashMap<String, String>();*/

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

                    setDestinationItemWc(documentManager.getDocument(
                            new IdRef(destinationItemWcUid)));

                    setDestinationItemRealUid(destinationItemWcUid);

                    setIsImpactable(
                            CMHelper.getIsImpactable(originItem.getType(),
                                    action, destinationItemWcUid));
                    setIncludeInImpactMatrix(
                            CMHelper.getIncludeInImpactMatrixDefaultValue(
                                    originItem.getType(), action,
                                    destinationItemWcUid));

                    setIsValid(true);

                    // TODO::????? EZ, destination itemak ez du orain
                    // bertsiorik...
                    /*// Calculate the version list for the selected destination
                    // item
                    Map<String, String> versionList = CMHelper.calculateModifiableItemVersionList(
                            documentManager, destinationItemWcUid);
                    setDestinationItemVersionList(versionList);
                    
                    // By default, select the last element of the list
                    if (versionList != null && versionList.size() > 0) {
                        setDestinationItemRealUid(
                                CMHelper.getLastElementValueFromVersionList(
                                        versionList));
                    }*/
                }

            } /*else {
                setDestinationItemVersionList(new HashMap<String, String>());
              }*/
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public String getDestinationItemRealUid() {
        return destinationItemRealUid;
    }

    public void setDestinationItemRealUid(String destinationItemRealUid) {
        String logInitMsg = "[setDestinationItemRealUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.destinationItemRealUid = destinationItemRealUid;

            if (destinationItemRealUid != null) {
                // retrieve the destinationItem from its id
                setDestinationItem(documentManager.getDocument(
                        new IdRef(destinationItemRealUid)));

                /*if (destinationItem.isVersion()) {
                    setIsManaged(true);
                } else {
                    setIsManaged(false);
                }
                
                setComment(CMTreeBeanHelper.calculateModifiedItemComment(action,
                        getCurrentDocument(), originItem, destinationItem));
                
                setIsImpactable(CMHelper.getIsImpactable(originItem.getType(),
                        action, destinationItemRealUid));
                setIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                originItem.getType(), action,
                                destinationItemRealUid));*/

            } else {
                destinationItem = null;
                setIsManaged(false);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    /*public Map<String, String> getDestinationItemVersionList() {
        return destinationItemVersionList;
    }
    
    public void setDestinationItemVersionList(
            Map<String, String> destinationItemVersionList) {
        this.destinationItemVersionList = destinationItemVersionList;
    }*/

    public DocumentModel getDestinationItem() {
        return destinationItem;
    }

    public void setDestinationItem(DocumentModel destinationItem) {
        this.destinationItem = destinationItem;
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

        originItemRealUid = null;

        originItemVersionList = new HashMap<String, String>();

        originItem = null;

        originItemWc = null;

        originItemType = null;

        action = null;

        destinationItemWcUid = null;

        destinationItemRealUid = null;

        /*destinationItemVersionList = new HashMap<String, String>();*/

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

    private void resetAddModifiedItemFormInputValues(UIComponent component,
            boolean includeOriginItem) {

        if (includeOriginItem) {
            // reset originItem component
            UIInput originSuggestInputComponent = (UIInput) component.findComponent(
                    "nxw_eloraSingleDocumentSuggestion_select2");
            if (originSuggestInputComponent != null) {
                EditableValueHolder originSuggestInput = originSuggestInputComponent;
                if (originSuggestInput != null) {
                    originSuggestInput.resetValue();
                    // originSuggestInput.setValue(null);
                }
            }

            // reset originItemRealUid component
            UIInput originItemRealUidInputComponent = (UIInput) component.findComponent(
                    "originItemRealUid");
            if (originItemRealUidInputComponent != null) {
                EditableValueHolder originItemRealUidInput = originItemRealUidInputComponent;
                if (originItemRealUidInput != null) {
                    originItemRealUidInput.resetValue();
                }
            }
        }

        // reset action component
        UIInput actionInputComponent = (UIInput) component.findComponent(
                "nxw_selectOneDirectory_2");
        if (actionInputComponent != null) {
            EditableValueHolder actionInput = actionInputComponent;
            if (actionInput != null) {
                actionInput.resetValue();
            }
        }

        // reset destinationItemSuggestInputComponent
        UIInput destinationItemSuggestInputComponent = (UIInput) component.findComponent(
                "nxw_eloraSingleDocumentSuggestion_1_select2");
        if (destinationItemSuggestInputComponent != null) {
            EditableValueHolder destinationItemSuggestInput = destinationItemSuggestInputComponent;
            if (destinationItemSuggestInput != null) {
                destinationItemSuggestInput.resetValue();
            }
        }

        // reset destinationItemRealUid component
        UIInput destinationItemRealUidInputComponent = (UIInput) component.findComponent(
                "destinationItemRealUid");
        if (destinationItemRealUidInputComponent != null) {
            EditableValueHolder destinationItemRealUidInput = destinationItemRealUidInputComponent;
            if (destinationItemRealUidInput != null) {
                destinationItemRealUidInput.resetValue();
            }
        }

        // reset isManaged component
        UIInput isManagedInputComponent = (UIInput) component.findComponent(
                "nxw_checkbox_1");
        if (isManagedInputComponent != null) {
            EditableValueHolder isManagedInput = isManagedInputComponent;
            if (isManagedInput != null) {
                isManagedInput.resetValue();
            }
        }

        // TODO:: FALTA DIRA reset Comment eta includeInImpactMatrix
        // !!!!!!!!!!!!
    }

    // ---------------------------------------------------------
    // ---- Methods for managing "Edit Modified Item" window ----
    public String getEditOriginItemWcUid() {
        return editOriginItemWcUid;
    }

    public void setEditOriginItemWcUid(String editOriginItemWcUid) {
        this.editOriginItemWcUid = editOriginItemWcUid;
    }

    public String getEditOriginItemRealUid() {
        return editOriginItemRealUid;
    }

    public void setEditOriginItemRealUid(String editOriginItemRealUid) {
        this.editOriginItemRealUid = editOriginItemRealUid;
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
                setEditDestinationItemRealUid(null);
                setEditIsManaged(false);
                setEditComment(null);
                setEditIsValid(false);

            } else {
                setEditActionMsg(null);

                setEditComment(CMTreeBeanHelper.calculateComment(
                        getCurrentDocument(), editAction));

                if (editAction.equals(CMConstants.ACTION_REMOVE)
                        || editAction.equals(CMConstants.ACTION_IGNORE)) {
                    setEditDestinationItemRealUid(null);
                    setEditDestinationItemWcUid(null);
                    setEditIsManaged(true);
                    setEditIsValid(true);

                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceEditDestinationSelectionElements();

                } else if (editAction.equals(CMConstants.ACTION_CHANGE)) {
                    setEditDestinationItemRealUid(editOriginItemWcUid);
                    setEditDestinationItemWcUid(editOriginItemWcUid);
                    setEditIsManaged(false);
                    setEditIsValid(true);
                    // Clear the properties for managing "Replace destination
                    // item"
                    clearReplaceEditDestinationSelectionElements();

                } else if (editAction.equals(CMConstants.ACTION_REPLACE)) {
                    setEditDestinationItemWcUid(null);
                    setEditDestinationItemRealUid(null);
                    setEditIsManaged(false);
                    setEditIsValid(false);
                }

                setEditIsImpactable(
                        CMHelper.getIsImpactable(editOriginItem.getType(),
                                editAction, editDestinationItemRealUid));
                setEditIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                editOriginItem.getType(), editAction,
                                editDestinationItemRealUid));
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
        editDestinationItemRealUid = null;
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

                    setEditDestinationItemWc(documentManager.getDocument(
                            new IdRef(editDestinationItemWcUid)));

                    setEditDestinationItemRealUid(editDestinationItemWcUid);

                    setEditIsImpactable(
                            CMHelper.getIsImpactable(editOriginItem.getType(),
                                    editAction, editDestinationItemRealUid));
                    setEditIncludeInImpactMatrix(
                            CMHelper.getIncludeInImpactMatrixDefaultValue(
                                    editOriginItem.getType(), editAction,
                                    editDestinationItemRealUid));

                    setEditIsValid(true);
                }
            }

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    public String getEditDestinationItemRealUid() {
        return editDestinationItemRealUid;
    }

    public void setEditDestinationItemRealUid(
            String editDestinationItemRealUid) {
        String logInitMsg = "[setEditDestinationItemRealUid] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            this.editDestinationItemRealUid = editDestinationItemRealUid;

            if (editDestinationItemRealUid != null) {
                // retrieve the destinationItem from its id
                setEditDestinationItem(documentManager.getDocument(
                        new IdRef(editDestinationItemRealUid)));

                /*if (editDestinationItem.isVersion()) {
                    setEditIsManaged(true); // TODO:: ?????????????????????????
                } else {
                    setEditIsManaged(false);
                }

                setEditComment(CMTreeBeanHelper.calculateModifiedItemComment(
                        editAction, getCurrentDocument(), editOriginItem,
                        editDestinationItem));

                setEditIsImpactable(
                        CMHelper.getIsImpactable(editOriginItem.getType(),
                                editAction, editDestinationItemRealUid));
                setEditIncludeInImpactMatrix(
                        CMHelper.getIncludeInImpactMatrixDefaultValue(
                                editOriginItem.getType(), editAction,
                                editDestinationItemRealUid));*/

            } else {
                editDestinationItem = null;
                setEditIsManaged(false); // TODO:: ?????????????????????????
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.setDestinationItem"));
        }
    }

    /* public Map<String, String> getEditDestinationItemVersionList() {
        return editDestinationItemVersionList;
    }

    public void setEditDestinationItemVersionList(
            Map<String, String> editDestinationItemVersionList) {
        this.editDestinationItemVersionList = editDestinationItemVersionList;
    }*/

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

    private void resetEditModifiedItemFormValues(boolean includeOriginItem) {

        if (includeOriginItem) {
            editOriginItemWcUid = null;

            editOriginItemRealUid = null;

            editOriginItem = null;

            editOriginItemWc = null;

            editOriginItemType = null;
        }

        editAction = null;

        editDestinationItemWcUid = null;

        editDestinationItemRealUid = null;

        /*editDestinationItemVersionList = new HashMap<String, String>();*/

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

    // TODO::: hau agian ez da beharrezkoa????????
    private void resetEditModifiedItemFormInputValues(UIComponent component,
            boolean includeOriginItem) {

        if (includeOriginItem) {
            // reset originItem component
            UIInput originSuggestInputComponent = (UIInput) component.findComponent(
                    "nxw_eloraSingleDocumentSuggestion_select2");
            if (originSuggestInputComponent != null) {
                EditableValueHolder originSuggestInput = originSuggestInputComponent;
                if (originSuggestInput != null) {
                    originSuggestInput.resetValue();
                    // originSuggestInput.setValue(null);
                }
            }

            // reset editOriginItemRealUid component
            UIInput originItemRealUidInputComponent = (UIInput) component.findComponent(
                    "editOriginItemRealUid");
            if (originItemRealUidInputComponent != null) {
                EditableValueHolder originItemRealUidInput = originItemRealUidInputComponent;
                if (originItemRealUidInput != null) {
                    originItemRealUidInput.resetValue();
                }
            }
        }

        // reset action component
        UIInput actionInputComponent = (UIInput) component.findComponent(
                "nxw_selectOneDirectory_2");
        if (actionInputComponent != null) {
            EditableValueHolder actionInput = actionInputComponent;
            if (actionInput != null) {
                actionInput.resetValue();
            }
        }

        // reset destinationItemSuggestInputComponent
        UIInput destinationItemSuggestInputComponent = (UIInput) component.findComponent(
                "nxw_eloraSingleDocumentSuggestion_1_select2");
        if (destinationItemSuggestInputComponent != null) {
            EditableValueHolder destinationItemSuggestInput = destinationItemSuggestInputComponent;
            if (destinationItemSuggestInput != null) {
                destinationItemSuggestInput.resetValue();
            }
        }

        // reset editDestinationItemRealUid component
        UIInput destinationItemRealUidInputComponent = (UIInput) component.findComponent(
                "editDestinationItemRealUid");
        if (destinationItemRealUidInputComponent != null) {
            EditableValueHolder destinationItemRealUidInput = destinationItemRealUidInputComponent;
            if (destinationItemRealUidInput != null) {
                destinationItemRealUidInput.resetValue();
            }
        }

        // reset isManaged component
        UIInput isManagedInputComponent = (UIInput) component.findComponent(
                "nxw_checkbox_1");
        if (isManagedInputComponent != null) {
            EditableValueHolder isManagedInput = isManagedInputComponent;
            if (isManagedInput != null) {
                isManagedInput.resetValue();
            }
        }

        // TODO:: FALTA DIRA reset Comment eta
        // includeInImpactMatrix!!!!!!!!!!!!!!!!!!
    }

    // ---------------------------------------------------------
}
