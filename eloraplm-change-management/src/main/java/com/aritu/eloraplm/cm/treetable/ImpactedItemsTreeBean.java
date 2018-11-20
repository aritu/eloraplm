package com.aritu.eloraplm.cm.treetable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.event.ActionEvent;

import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class ImpactedItemsTreeBean extends CoreTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ImpactedItemsTreeBean.class);

    private String itemType;

    private ImpactedItemsNodeService nodeService;

    // ------------------------------------------------------------
    // ---- Properties for managing "Edit Impacted Items" window ----
    // -- Attribute
    private String editAttribute;

    // -- Action
    private String editAction;

    // -- IsManaged
    private boolean editIsManaged;

    // -- Comment
    private String editComment;

    // ------------------------------------------------------------

    public ImpactedItemsTreeBean(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createRoot() {

        String logInitMsg = "[" + itemType + "] [createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService = new ImpactedItemsNodeService(documentManager,
                    itemType);
            setRoot(nodeService.getRoot(currentDoc));

            setIsDirty(false);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void saveTree() {
        String logInitMsg = "[" + itemType + "] [saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

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

    public void refreshNode(TreeTable table, TreeNode node,
            String triggeredField, boolean updateChildren)
            throws EloraException {
        refreshNode(node, triggeredField);
        EloraAjax.updateTreeTableRow(table, node.getRowKey(), updateChildren);
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {
        String logInitMsg = "[" + itemType + "] [refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {

            DocumentModel currentDoc = getCurrentDocument();

            nodeService.refreshNode(currentDoc, node, triggeredField);

            if (!triggeredField.equals(
                    CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)) {
                setIsDirty(true);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        // log.trace(logInitMsg + "--- EXIT --- ");
    }

    public ImpactedItemsNodeService getNodeService() {
        return nodeService;
    }

    // -------- editImpactedITems
    public void editImpactedItems() {
        String logInitMsg = "[editImpactedItems] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- editAttribute = |" + editAttribute
                + "|, editAction = |" + editAction + "|, editIsManaged = |"
                + editIsManaged + "|, editComment = |" + editComment + "|");

        try {

            if (editAttribute != null && editAttribute.length() > 0) {

                TreeNode[] nodes = getSelectedNodes();
                if (nodes != null && nodes.length > 0) {

                    switch (editAttribute) {
                    case CMConstants.MODIFIABLE_ATTRIBUTE_ACTION:
                        processActionChangeInSelectedNodes(editAction, nodes);
                        break;

                    case CMConstants.MODIFIABLE_ATTRIBUTE_IS_MANAGED:
                        processIsManagedChangeInSelectedNodes(editIsManaged,
                                nodes);
                        break;

                    case CMConstants.MODIFIABLE_ATTRIBUTE_COMMENT:
                        processCommentChangeInSelectedNodes(editComment, nodes);
                        break;
                    }
                }
            }

            resetEditImpactedItemsFormValues();

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.editImpactedItems"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processActionChangeInSelectedNodes(String action,
            TreeNode[] nodes) throws EloraException {
        String logInitMsg = "[processActionChangeInSelectedNodes] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- action = |" + action + "|");

        try {

            // Sort the selected nodes, since the action change has to be done
            // in a given order
            Comparator<TreeNode> treeNodeComparator = new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    Integer o1Data = Integer.valueOf(o1.getData().toString());
                    Integer o2Data = Integer.valueOf(o2.getData().toString());
                    return o1Data.compareTo(o2Data);
                }
            };
            Arrays.sort(nodes, treeNodeComparator);

            // Process selected nodes
            List<String> processedNodes = new ArrayList<String>();

            for (TreeNode treeNode : nodes) {

                ImpactedItemsNodeData selectedNodeData = (ImpactedItemsNodeData) treeNode.getData();

                if (!processedNodes.contains(selectedNodeData.getNodeId())) {

                    log.trace(logInitMsg + "Process node |" + selectedNodeData
                            + "|");

                    if (!selectedNodeData.getIsModifiedItem()
                            && !selectedNodeData.getActionIsReadOnly()) {

                        // if the selected node action is not the same as the
                        // new one, then, change it.
                        if (!selectedNodeData.getAction().equals(action)) {
                            selectedNodeData.setAction(action);

                            selectedNodeData.setIsModified(true);
                            selectedNodeData.setIsUpdated(true);

                            processedNodes.add(selectedNodeData.getNodeId());

                            // If action is ignore, destination will be empty
                            // and
                            // the element is marked as managed.
                            if (action.equals(CMConstants.ACTION_IGNORE)) {
                                selectedNodeData.setDestinationItem(null);
                                selectedNodeData.setDestinationItemUid(null);
                                selectedNodeData.setDestinationItemVersionList(
                                        null);
                                selectedNodeData.setDestinationItemWc(null);
                                selectedNodeData.setDestinationItemVersionIsReadOnly(
                                        true);
                                selectedNodeData.setIsManaged(true);
                                selectedNodeData.setIsManagedIsReadOnly(true);
                            }
                            if (action.equals(CMConstants.ACTION_CHANGE)) {
                                // set as destinationItem and destinationItemWc
                                // the
                                // originWC
                                DocumentModel destinationItem = selectedNodeData.getOriginItemWc();
                                selectedNodeData.setDestinationItem(
                                        destinationItem);
                                selectedNodeData.setDestinationItemWc(
                                        destinationItem);
                                String destinationItemUid = destinationItem.getId();
                                selectedNodeData.setDestinationItemUid(
                                        destinationItemUid);
                                Map<String, String> destinationItemVersionList = new HashMap<String, String>();
                                destinationItemVersionList.put(
                                        destinationItemUid,
                                        destinationItem.getVersionLabel()
                                                + " (WC)");
                                selectedNodeData.setDestinationItemVersionList(
                                        destinationItemVersionList);
                                selectedNodeData.setDestinationItemVersionIsReadOnly(
                                        false);
                                selectedNodeData.setIsManaged(false);
                                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                                        action,
                                        selectedNodeData.getOriginItemWc());
                                selectedNodeData.setIsManagedIsReadOnly(
                                        isManagedIsReadOnly);
                            }

                            String comment = CMTreeBeanHelper.calculateComment(
                                    getCurrentDocument(), action);
                            selectedNodeData.setComment(comment);
                            refreshEditedImpactedItemChildNodes(processedNodes,
                                    treeNode, action, comment);

                            setIsDirty(true);
                        }
                    }
                } else {
                    log.trace(logInitMsg + "Node already processed |"
                            + selectedNodeData + "|");
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);

            throw new EloraException(e.getMessage());
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processIsManagedChangeInSelectedNodes(boolean isManaged,
            TreeNode[] nodes) throws EloraException {
        String logInitMsg = "[processIsManagedChangeInSelectedNodes] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- isManaged = |" + isManaged + "|");

        // Process selected nodes
        for (TreeNode treeNode : nodes) {

            ImpactedItemsNodeData selectedNodeData = (ImpactedItemsNodeData) treeNode.getData();

            log.trace(logInitMsg + "Process node |" + selectedNodeData + "|");

            // if the selected node isManagedIsReadOnly attribute is true, it
            // cannot be modified
            if (!selectedNodeData.getIsModifiedItem()
                    && !selectedNodeData.getIsManagedIsReadOnly()) {

                // if the selected node isManaged value is not the same as the
                // new one, then, change it.
                if (selectedNodeData.getIsManaged() != isManaged) {
                    selectedNodeData.setIsManaged(isManaged);

                    selectedNodeData.setIsModified(true);
                    selectedNodeData.setIsUpdated(true);

                    CMTreeBeanHelper.processRefreshNodeTriggeredByIsManaged(
                            selectedNodeData, documentManager);

                    setIsDirty(true);
                }

            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processCommentChangeInSelectedNodes(String comment,
            TreeNode[] nodes) throws EloraException {
        String logInitMsg = "[processCommentChangeInSelectedNodes] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- comment = |" + comment + "|");

        // Process selected nodes
        for (TreeNode treeNode : nodes) {
            ImpactedItemsNodeData selectedNodeData = (ImpactedItemsNodeData) treeNode.getData();

            log.trace(logInitMsg + "Process node |" + selectedNodeData + "|");

            // if the selected node is a impacted item
            if (!selectedNodeData.getIsModifiedItem()) {
                selectedNodeData.setComment(comment);

                selectedNodeData.setIsModified(true);
                selectedNodeData.setIsUpdated(true);

                setIsDirty(true);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void refreshEditedImpactedItemChildNodes(
            List<String> processedNodes, TreeNode node, String action,
            String comment) throws EloraException {

        if (action.equals(CMConstants.ACTION_IGNORE)) {

            // Set the following for each child:
            // -- action = IGNORE and readOnly
            // -- destinationItem = null
            // -- isManaged = true and readOnly
            // -- comment = ignored since ancestor's action is ignore
            for (TreeNode childNode : node.getChildren()) {

                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                // if the child node action is not the same as the
                // new one, then, change it.
                if (!childNodeData.getAction().equals(action)) {

                    // EXCEPTION: if this child node is a CadDrawing, don't
                    // propagate the IGNORE action
                    if (childNodeData.getOriginItem() != null
                            && !childNodeData.getOriginItem().getType().equals(
                                    EloraDoctypeConstants.CAD_DRAWING)) {

                        childNodeData.setIsModified(true);
                        childNodeData.setIsUpdated(true);
                        childNodeData.setAction(CMConstants.ACTION_IGNORE);
                        childNodeData.setActionIsReadOnly(true);
                        childNodeData.setDestinationItem(null);
                        childNodeData.setDestinationItemUid(null);
                        childNodeData.setDestinationItemVersionList(null);
                        childNodeData.setDestinationItemWc(null);
                        childNodeData.setDestinationItemVersionIsReadOnly(true);
                        childNodeData.setIsManaged(true);
                        childNodeData.setIsManagedIsReadOnly(true);
                        childNodeData.setComment(
                                CMConstants.COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE);

                        processedNodes.add(childNodeData.getNodeId());

                        refreshEditedImpactedItemChildNodes(processedNodes,
                                childNode, action, comment);
                    }
                }
            }
        }

        if (action.equals(CMConstants.ACTION_CHANGE)) {

            // -- action = CHANGE and NOT readOnly
            // -- destinationItem = WC
            // -- isManaged = false and NOT readOnly
            for (TreeNode childNode : node.getChildren()) {

                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                // if the child node action is not the same as the
                // new one, then, change it.
                if (!childNodeData.getAction().equals(action)) {
                    childNodeData.setIsModified(true);
                    childNodeData.setIsUpdated(true);
                    childNodeData.setAction(CMConstants.ACTION_CHANGE);
                    childNodeData.setActionIsReadOnly(false);
                    DocumentModel destinationItem = childNodeData.getOriginItemWc();
                    childNodeData.setDestinationItem(destinationItem);
                    childNodeData.setDestinationItemWc(destinationItem);
                    String destinationItemUid = destinationItem.getId();
                    childNodeData.setDestinationItemUid(destinationItemUid);
                    Map<String, String> destinationItemVersionList = new HashMap<String, String>();
                    destinationItemVersionList.put(destinationItemUid,
                            destinationItem.getVersionLabel() + " (WC)");
                    childNodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    childNodeData.setDestinationItemVersionIsReadOnly(false);
                    childNodeData.setIsManaged(false);
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, childNodeData.getOriginItemWc());
                    childNodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                    childNodeData.setComment(comment);

                    processedNodes.add(childNodeData.getNodeId());

                    refreshEditedImpactedItemChildNodes(processedNodes,
                            childNode, action, comment);
                }
            }
        }
    }

    public void cancelEditImpactedItemsActionListener(ActionEvent event) {
        String logInitMsg = "[cancelEditImpactedItemsActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        resetEditImpactedItemsFormValues();

        resetEditImpactedItemsFormInputValues(event.getComponent(), true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetEditImpactedItemsFormValues();

        createRoot();
    }

    // ---------------------------------------------------------
    // ---- Methods for managing "Edit Impacted Items" window ----

    public String getEditAttribute() {
        return editAttribute;
    }

    public void setEditAttribute(String editAttribute) {
        if (editAttribute == null) {
            setEditAction(null);
            setEditIsManaged(false);
            setEditComment(null);
        }

        this.editAttribute = editAttribute;
    }

    public String getEditAction() {
        return editAction;
    }

    public void setEditAction(String editAction) {
        /*if (editAction == null) {
            editAction = CMConstants.ACTION_CHANGE;
        }*/
        this.editAction = editAction;
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

    private void resetEditImpactedItemsFormValues() {
        editAttribute = null;
        editAction = null;
        editIsManaged = false;
        editComment = null;
    }

    // TODO::: hau agian ez da beharrezkoa????????
    private void resetEditImpactedItemsFormInputValues(UIComponent component,
            boolean includeOriginItem) {

        // TODO: reset attribute, editIsManaged and editComment components

        // reset action component
        UIInput actionInputComponent = (UIInput) component.findComponent(
                "nxw_selectOneDirectory_2");
        if (actionInputComponent != null) {
            EditableValueHolder actionInput = actionInputComponent;
            if (actionInput != null) {
                actionInput.resetValue();
            }
        }

    }

    // ---------------------------------------------------------

}