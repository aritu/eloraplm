package com.aritu.eloraplm.cm.treetable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.core.operations.notification.MailTemplateHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.DocumentNotificationInfo;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class AbstractImpactedItemsTree extends CoreTree {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            AbstractImpactedItemsTree.class);

    private String itemType;

    private ImpactedItemsNodeService nodeService;

    private Map<String, Boolean> isManagedYesNoFilterMap;

    // ------------------------------------------------------------
    // ---- Properties for managing "Edit Impacted Items" window ----
    // -- Attribute
    private String editAttribute;

    // -- Action
    private String editAction;

    // -- IsManaged
    private Boolean editIsManaged;

    // -- Comment
    private String editComment;

    private List<TreeNode> actionChangeTreeNodeList;

    // ------------------------------------------------------------

    public AbstractImpactedItemsTree(String itemType) {
        this.itemType = itemType;
        actionChangeTreeNodeList = new ArrayList<TreeNode>();
    }

    @Override
    public void createRoot() {

        String logInitMsg = "[" + itemType + "] [createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();
            nodeService = new ImpactedItemsNodeService(documentManager,
                    itemType, messages);
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
        String logInitMsg = "[" + itemType + "] [saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            ChangeManagementService cmService = Framework.getService(
                    ChangeManagementService.class);

            nodeService.saveTree(currentDoc, getRoot(),
                    actionChangeTreeNodeList, cmService);

            actionChangeTreeNodeList.clear();

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
        String logInitMsg = "[" + itemType + "] [refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService.refreshNode(currentDoc, node, triggeredField,
                    actionChangeTreeNodeList);

            if (!triggeredField.equals(
                    CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)
                    && !triggeredField.equals(
                            CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_SOURCE_VERSIONS)) {
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

    public void editImpactedItems() {
        String logInitMsg = "[editImpactedItems] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- action = |" + editAction + "|");

        try {

            TreeNode[] nodes = getSelectedNodes();

            if (editAction != null) {
                sortNodes(nodes);
            }

            // Process selected nodes
            List<String> processedNodes = new ArrayList<String>();

            for (TreeNode treeNode : nodes) {
                ImpactedItemsNodeData selectedNodeData = (ImpactedItemsNodeData) treeNode.getData();

                if (selectedNodeData.getIsFromMbom()
                        && selectedNodeData.getOriginItem() == null) {
                    continue;
                }

                if (editAction != null) {
                    processActionInSelectedNodes(editAction, processedNodes,
                            selectedNodeData, treeNode);
                }

                if (editIsManaged != null) {
                    processIsManagedInSelectedNodes(editIsManaged,
                            selectedNodeData);
                }

                if (editComment != null) {
                    processCommentInSelectedNodes(editComment,
                            selectedNodeData);
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.editImpactedItems"));
        } finally {
            setEditAttribute(null);
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processActionInSelectedNodes(String action,
            List<String> processedNodes, ImpactedItemsNodeData selectedNodeData,
            TreeNode treeNode) throws EloraException {

        if (!processedNodes.contains(selectedNodeData.getNodeId())) {

            if (!selectedNodeData.getIsModifiedItem()
                    && !selectedNodeData.getActionIsReadOnly()) {

                // if the selected node action the same as the new one
                // OR it is already MANAGED and it is not IGNORE,
                // do not change it.
                if (selectedNodeData.getAction().equals(action)
                        || (selectedNodeData.getIsManaged()
                                && !selectedNodeData.getAction().equals(
                                        CMConstants.ACTION_IGNORE))) {
                    // TODO: add a warning message
                } else {
                    selectedNodeData.setAction(action);

                    selectedNodeData.setIsModified(true);
                    selectedNodeData.setIsUpdated(true);

                    processedNodes.add(selectedNodeData.getNodeId());

                    // If action is ignore, destination will be empty
                    // and the element is marked as managed.
                    if (action.equals(CMConstants.ACTION_IGNORE)) {
                        selectedNodeData.setDestinationItem(null);
                        selectedNodeData.setDestinationItemVersionList(null);
                        selectedNodeData.setDestinationItemWc(null);
                        selectedNodeData.setDestinationItemSourceVersion(null);
                        selectedNodeData.setDestinationItemSourceVersionList(
                                null);
                        selectedNodeData.setDestinationItemVersionIsReadOnly(
                                true);
                        selectedNodeData.setDestinationItemSourceVersionIsReadOnly(
                                true);
                        selectedNodeData.setIsManaged(true);
                        selectedNodeData.setIsManagedIsReadOnly(true);
                    }
                    if (action.equals(CMConstants.ACTION_CHANGE)
                            || action.equals(CMConstants.ACTION_OVERWRITE)) {
                        // set as destinationItem and destinationItemWc
                        // the originWC
                        DocumentModel destinationItem = selectedNodeData.getOriginItemWc();
                        selectedNodeData.setDestinationItem(destinationItem);
                        selectedNodeData.setDestinationItemWc(destinationItem);
                        List<SelectItem> destinationItemVersionList = new ArrayList<SelectItem>();
                        destinationItemVersionList.add(new SelectItem(
                                destinationItem,
                                destinationItem.getVersionLabel()
                                        + CMConstants.VERSION_LABEL_WC));
                        selectedNodeData.setDestinationItemVersionList(
                                destinationItemVersionList);
                        CMHelper.setDestinationItemSourceVersionInformation(
                                selectedNodeData, messages);
                        selectedNodeData.setDestinationItemVersionIsReadOnly(
                                false);
                        boolean isManaged = false;
                        boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                                .calculateDestinationItemSourceVersionIsReadOnlyValue(
                                        destinationItem, isManaged);
                        selectedNodeData.setDestinationItemSourceVersionIsReadOnly(
                                destinationItemSourceVersionIsReadOnly);
                        selectedNodeData.setIsManaged(isManaged);
                        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                                action, selectedNodeData.getOriginItemWc());
                        selectedNodeData.setIsManagedIsReadOnly(
                                isManagedIsReadOnly);
                    }

                    actionChangeTreeNodeList.add(treeNode);

                    String comment = CMTreeBeanHelper.calculateComment(
                            getCurrentDocument(), action);
                    selectedNodeData.setComment(comment);
                    refreshEditedImpactedItemChildNodes(processedNodes,
                            treeNode, action, comment);

                    setIsDirty(true);
                }
            }
        }
    }

    private void processIsManagedInSelectedNodes(boolean isManaged,
            ImpactedItemsNodeData selectedNodeData) throws EloraException {

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
                        selectedNodeData, documentManager, messages);

                setIsDirty(true);
            }

        } else {
            // TODO: add a warning message
        }
    }

    private void processCommentInSelectedNodes(String comment,
            ImpactedItemsNodeData selectedNodeData) throws EloraException {
        // if the selected node is a impacted item
        if (!selectedNodeData.getIsModifiedItem()) {
            selectedNodeData.setComment(comment);

            selectedNodeData.setIsModified(true);
            selectedNodeData.setIsUpdated(true);

            setIsDirty(true);
        }
    }

    private void sortNodes(TreeNode[] nodes) {
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

                // if the child node action the same as the new one
                // OR it is already MANAGED and it is not IGNORE,
                // do not change it.
                if (childNodeData.getAction().equals(action)
                        || (childNodeData.getIsManaged()
                                && !childNodeData.getAction().equals(
                                        CMConstants.ACTION_IGNORE))) {
                    // TODO: add a warning message
                } else {
                    boolean actionIsReadOnly = true;
                    // EXCEPTION: if this child node is a CadDrawing,
                    // action should always be editable
                    if (childNodeData.getOriginItem() != null
                            && childNodeData.getOriginItem().getType().equals(
                                    EloraDoctypeConstants.CAD_DRAWING)) {
                        actionIsReadOnly = false;
                    }

                    childNodeData.setIsModified(true);
                    childNodeData.setIsUpdated(true);
                    childNodeData.setAction(CMConstants.ACTION_IGNORE);
                    childNodeData.setActionIsReadOnly(actionIsReadOnly);
                    childNodeData.setDestinationItem(null);
                    childNodeData.setDestinationItemVersionList(null);
                    childNodeData.setDestinationItemWc(null);
                    childNodeData.setDestinationItemSourceVersion(null);
                    childNodeData.setDestinationItemSourceVersionList(null);
                    childNodeData.setDestinationItemVersionIsReadOnly(true);
                    childNodeData.setDestinationItemSourceVersionIsReadOnly(
                            true);
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

        if (action.equals(CMConstants.ACTION_CHANGE)
                || action.equals(CMConstants.ACTION_OVERWRITE)) {

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
                    childNodeData.setAction(action);
                    childNodeData.setActionIsReadOnly(false);
                    DocumentModel destinationItem = childNodeData.getOriginItemWc();
                    childNodeData.setDestinationItem(destinationItem);
                    childNodeData.setDestinationItemWc(destinationItem);
                    List<SelectItem> destinationItemVersionList = new ArrayList<SelectItem>();
                    destinationItemVersionList.add(new SelectItem(
                            destinationItem, destinationItem.getVersionLabel()
                                    + CMConstants.VERSION_LABEL_WC));
                    childNodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    CMHelper.setDestinationItemSourceVersionInformation(
                            childNodeData, messages);
                    childNodeData.setDestinationItemVersionIsReadOnly(false);
                    boolean isManaged = false;
                    boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                            .calculateDestinationItemSourceVersionIsReadOnlyValue(
                                    destinationItem, isManaged);
                    childNodeData.setDestinationItemSourceVersionIsReadOnly(
                            destinationItemSourceVersionIsReadOnly);
                    childNodeData.setIsManaged(isManaged);
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

    public void sendLockConflictNotificationEmail() {
        String logInitMsg = "[sendLockConflictNotificationEmail] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();

            String username;
            if (principal.getFirstName() == null
                    || principal.getLastName() == null) {
                username = principal.getName();
            } else {
                username = principal.getFirstName() + " "
                        + principal.getLastName();
            }

            String template = "lockConflictNotification";
            String subjectPrefix = "[EloraPLM] ";
            String subject = subjectPrefix;

            subject += "Desblokeatzeko jakinarazpena / Notificación de desbloqueo / Unlocking notification";

            Map<String, Object> mail = new HashMap<String, Object>();
            mail.put("subject", subject);
            mail.put("template", template);
            mail.put("user", username);

            DocumentModel currentDoc = getCurrentDocument();
            String ecoUrl = MailTemplateHelper.getDocumentUrl(
                    getCurrentDocument(), null);
            String ecoName = "["
                    + currentDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE)
                    + "] " + currentDoc.getTitle();
            mail.put("ecoUrl", ecoUrl);
            mail.put("ecoName", ecoName);

            EmailHelper emailHelper = new EmailHelper();

            Map<String, List<DocumentModel>> docsByOwner = getDocsByOwner(
                    nodeService.getLockConflicts());

            for (Map.Entry<String, List<DocumentModel>> entry : docsByOwner.entrySet()) {
                String owner = entry.getKey();
                List<DocumentModel> documents = entry.getValue();

                List<DocumentNotificationInfo> documentsNotificationInfo = new ArrayList<DocumentNotificationInfo>();
                for (DocumentModel documentModel : documents) {
                    String docUrl = MailTemplateHelper.getDocumentUrl(
                            documentModel, null);
                    DocumentNotificationInfo docNotifInfo = new DocumentNotificationInfo(
                            documentModel, docUrl);
                    documentsNotificationInfo.add(docNotifInfo);
                }

                UserManager userManager = Framework.getLocalService(
                        UserManager.class);
                NuxeoPrincipal lockPrincipal = userManager.getPrincipal(owner);

                mail.put("mail.to", lockPrincipal.getEmail());
                mail.put("documentsNotificationInfo",
                        documentsNotificationInfo);
                emailHelper.sendmail(mail);
            }

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.notification.sent"));

        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.lockConflicts.sendEmail"));
            log.error(logInitMsg + e.getMessage(), e);
        }
    }

    private Map<String, List<DocumentModel>> getDocsByOwner(
            List<DocumentModel> lockedDocs) {

        Map<String, List<DocumentModel>> docsByOwner = new HashMap<String, List<DocumentModel>>();

        for (DocumentModel lockedDoc : lockedDocs) {
            String owner = lockedDoc.getLockInfo().getOwner();

            if (docsByOwner.containsKey(owner)) {
                docsByOwner.get(owner).add(lockedDoc);
            } else {
                List<DocumentModel> docs = new ArrayList<DocumentModel>();
                docs.add(lockedDoc);
                docsByOwner.put(owner, docs);
            }
        }
        return docsByOwner;
    }

    public void cancelEditImpactedItemsActionListener(ActionEvent event) {
        String logInitMsg = "[cancelEditImpactedItemsActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        resetEditImpactedItemsFormValues();

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void fillFilterValues() {
        isManagedYesNoFilterMap = new HashMap<String, Boolean>();
        isManagedYesNoFilterMap.put("label.yes", true);
        isManagedYesNoFilterMap.put("label.no", false);
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
            setEditIsManaged(null);
            setEditComment(null);
        }

        this.editAttribute = editAttribute;
    }

    public String getEditAction() {
        return editAction;
    }

    public void setEditAction(String editAction) {
        if (editAction != null) {
            if (editAction.equals(CMConstants.ACTION_CHANGE)
                    || editAction.equals(CMConstants.ACTION_OVERWRITE)) {
                setEditIsManaged(false);
            } else if (editAction.equals(CMConstants.ACTION_IGNORE)) {
                setEditIsManaged(true);
            }
        }
        this.editAction = editAction;
    }

    public Boolean getEditIsManaged() {
        return editIsManaged;
    }

    public void setEditIsManaged(Boolean editIsManaged) {
        this.editIsManaged = editIsManaged;
    }

    public String getEditComment() {
        return editComment;
    }

    public void setEditComment(String editComment) {
        this.editComment = editComment;
    }

    public List<DocumentModel> getLockConflicts() {
        return nodeService.getLockConflicts();
    }

    public boolean getPromotePendingItems() {
        return nodeService.getPromotePendingItems();
    }

    public boolean getPassedItems() {
        return nodeService.getPassedItems();
    }

    public boolean getUpdatePendingCharacterizedItems() {
        return nodeService.getUpdatePendingCharacterizedItems();
    }

    public boolean getExistsCharacterizedItems() {
        return nodeService.getExistsCharacterizedItems();
    }

    public Map<String, Boolean> getIsManagedYesNoFilterMap() {
        return isManagedYesNoFilterMap;
    }

    public void resetEditImpactedItemsFormValues() {
        editAttribute = null;
        editAction = null;
        editIsManaged = null;
        editComment = null;
    }

    // ---------------------------------------------------------

    public String getItemType() {
        return itemType;
    }

}
