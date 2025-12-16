package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.webapp.util.EloraAjax;

@Name(ManualImpactedItemsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ManualImpactedItemsTreeBean extends CoreTree {

    public static final String NAME = "cmManualImpactedItemsTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ManualImpactedItemsTreeBean.class);

    private ManualImpactedItemsNodeService nodeService;

    private DocumentModel modifiedItem;

    private List<SelectItem> modifiedItemList;

    private String impactedItemWcUid;

    private String impactedItemRealUid;

    private DocumentModel impactedItem;

    private String impactItemAction;

    private List<SelectItem> impactItemVersionList;

    private String comment;

    private boolean lockManualImpacted;

    public ManualImpactedItemsTreeBean() {
        setLockManualImpacted(true);
    }

    @Override
    public void createRoot() {

        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();
            modifiedItemList = getModifiedItems(currentDoc);

            nodeService = new ManualImpactedItemsNodeService(documentManager,
                    messages);
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

    @Observer(value = { CMEventNames.CM_MODIFIED_ITEMS_SAVED })
    @BypassInterceptors
    public void markTreeToBeReloaded() {
        forceReset();
    }

    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (getIsDirty()) {
            saveCurrentTree();
            createRoot();
            /*
             * Events.instance().raiseEvent(CMEventNames.CM_MANUAL_IMPACTED_ITEMS_SAVED);
             * // Nuxeo event
             * EloraEventHelper.fireEvent(CMEventNames.CM_MANUAL_IMPACTED_ITEMS_SAVED,
             * navigationContext.getCurrentDocument());
             */
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

    public void addManualImpactedItem() {

        String logInitMsg = "[addManualImpactedItem] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (impactedExistsAsModified(impactedItemRealUid)) {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.error.treetable.impactedItemAlreadyAsModified"));
            return;
        }
        DocumentModel impactedItemDocWc = documentManager.getDocument(
                new IdRef(impactedItemWcUid));

        DocumentModel impactedItemDocAv = documentManager.getDocument(
                new IdRef(impactedItemRealUid));

        String validationResultMessage = nodeService.validateManualImpactedItem(
                getRoot(), modifiedItem.getId(), impactedItemRealUid);

        if (validationResultMessage == null
                || validationResultMessage.length() == 0) {

            String impactType = CMHelper.getItemType(impactedItemDocWc);

            ArrayList<String> impactRelations = CMHelper.getManualImpactRelations(
                    impactType);

            if (!nodeService.checkModifiedIsInComposition(modifiedItem.getId(),
                    impactedItemDocAv, impactRelations)) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.notInComposition"));
                return;
            }

            if (lockManualImpacted) {
                String currentUser = documentManager.getPrincipal().getName();
                if (impactedItemDocWc.isLocked()
                        && !impactedItemDocWc.getLockInfo().getOwner().equals(
                                currentUser)) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.treetable.lockedByAnotherUser"));
                    return;
                } else if (!impactedItemDocWc.isLocked()) {
                    try {
                        EloraDocumentHelper.lockDocument(impactedItemDocWc);
                    } catch (DocumentAlreadyLockedException
                            | DocumentInUnlockableStateException
                            | EloraException | DocumentLockRightsException e) {

                        facesMessages.add(StatusMessage.Severity.ERROR,
                                messages.get(
                                        "eloraplm.message.error.treetable.lockError"));
                        return;
                    }
                }
            }

            setRoot(nodeService.addManualImpactedItemNode(getRoot(),
                    modifiedItem.getId(), impactedItemDocAv, impactedItemDocWc,
                    impactItemAction, lockManualImpacted, comment));

            setIsDirty(true);

        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(validationResultMessage));
        }

        resetAddManualImpactedFormValues();
    }

    public void refreshNode(TreeTable table, TreeNode node,
            String triggeredField, boolean updateChildren) {
        refreshNode(node, triggeredField);
        EloraAjax.updateTreeTableRow(table, node.getRowKey(), updateChildren);
    }

    public void refreshNode(TreeNode node, String triggeredField) {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {

            nodeService.refreshNode(node, triggeredField);

            setIsDirty(true);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        // log.trace(logInitMsg + "--- EXIT --- ");
    }

    public void markManualNodeAsRemoved(TreeTable table, TreeNode node) {
        markManualNodeAsRemoved(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void markManualNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[markManualNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleManualNodeAsRemoved(true, node);

        setIsDirty(true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void unmarkManualNodeAsRemoved(TreeTable table, TreeNode node) {
        unmarkManualNodeAsRemoved(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    public void unmarkManualNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[unmarkManualNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleManualNodeAsRemoved(false, node);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void toggleManualNodeAsRemoved(boolean isRemoved, TreeNode node) {
        String logInitMsg = "[toggleManualNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        ManualImpactedItemsNodeData nodeData = (ManualImpactedItemsNodeData) node.getData();
        // If it is a new node, remove it completely
        if (nodeData.getIsNew()) {
            getRoot().getChildren().remove(node);
        } else {
            nodeData.setIsRemoved(isRemoved);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void cancelEditManualImpactedItemsActionListener(ActionEvent event) {
        String logInitMsg = "[cancelManualEditImpactedItemsActionListener] ";
        log.trace(logInitMsg + "--- ENTER ---");

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<SelectItem> getModifiedItems(DocumentModel currentDoc) {

        List<SelectItem> modifiedList = new ArrayList<SelectItem>();
        ArrayList<HashMap<String, Object>> modifiedItems = new ArrayList<HashMap<String, Object>>();

        if (currentDoc.getPropertyValue(
                CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
            modifiedItems = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                    CMMetadataConstants.MODIFIED_ITEM_LIST);
        }

        for (Map<String, Object> modifiedItem : modifiedItems) {
            String originItemUid = (String) modifiedItem.get("originItem");

            DocumentModel modifiedItemDoc = documentManager.getDocument(
                    new IdRef(originItemUid));

            if (!(boolean) modifiedItem.get("includeInImpactMatrix")
                    || modifiedItemDoc.getType().equals(
                            EloraDoctypeConstants.CAD_DRAWING)) {
                continue;
            }

            String selectedLabel = "";
            String reference = (String) modifiedItemDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (reference != null) {
                selectedLabel += reference + " ";
            }
            selectedLabel += modifiedItemDoc.getTitle();
            modifiedList.add(new SelectItem(modifiedItemDoc, selectedLabel));
        }

        return modifiedList;
    }

    public DocumentModel getModifiedItem() {
        return modifiedItem;
    }

    public void setModifiedItem(DocumentModel modifiedItem) {
        this.modifiedItem = modifiedItem;
    }

    public String getImpactItemAction() {
        return impactItemAction;
    }

    public void setImpactItemAction(String impactItemAction) {
        this.impactItemAction = impactItemAction;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<SelectItem> getModifiedItemList() {
        return modifiedItemList;
    }

    public void setModifiedItemList(List<SelectItem> modifiedItemList) {
        this.modifiedItemList = modifiedItemList;
    }

    public boolean getLockManualImpacted() {
        return lockManualImpacted;
    }

    public void setLockManualImpacted(boolean lockManualImpacted) {
        this.lockManualImpacted = lockManualImpacted;
    }

    public String getImpactedItemWcUid() {
        return impactedItemWcUid;
    }

    public void setImpactedItemWcUid(String impactedItemWcUid) {
        this.impactedItemWcUid = impactedItemWcUid;

        if (impactedItemWcUid != null) {
            calculateImpactedItemVersionList(impactedItemWcUid);
        }
    }

    public String getImpactedItemRealUid() {
        return impactedItemRealUid;
    }

    public void setImpactedItemRealUid(String impactedItemRealUid) {
        this.impactedItemRealUid = impactedItemRealUid;
    }

    public List<SelectItem> getImpactItemVersionList() {
        return impactItemVersionList;
    }

    public void setImpactItemVersionList(
            List<SelectItem> impactItemVersionList) {
        this.impactItemVersionList = impactItemVersionList;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetAddManualImpactedFormValues();
        super.resetBeanCache(newCurrentDocumentModel);
    }

    private void resetAddManualImpactedFormValues() {
        setImpactItemAction(null);
        setImpactedItemWcUid(null);
        setImpactedItem(null);
        setComment(null);
    }

    private void calculateImpactedItemVersionList(String impactedItemWcUid) {
        String logInitMsg = "[calculateImpactedItemVersionList] ["
                + documentManager.getPrincipal().getName() + "] ";

        boolean onlyReleasedVersion = false;

        try {
            List<SelectItem> versionList = CMHelper.calculateOriginVersionList(
                    documentManager, impactedItemWcUid, onlyReleasedVersion,
                    messages);

            setImpactItemVersionList(versionList);

            // By default, select the first element of the list
            if (versionList != null && versionList.size() > 0) {
                DocumentModel impactedItem = CMHelper.getFirstDocumentFromVersionList(versionList);
                setImpactedItem(impactedItem);
            }

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.setOriginItem"));
        }
    }

    private boolean impactedExistsAsModified(String impactedUid) {
        for (SelectItem item : modifiedItemList) {
            if (item.getValue().equals(impactedUid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the impactedItem
     */
    public DocumentModel getImpactedItem() {
        return impactedItem;
    }

    /**
     * @param impactedItem the impactedItem to set
     */
    public void setImpactedItem(DocumentModel impactedItem) {
        this.impactedItem = impactedItem;
        if (impactedItem != null) {
            setImpactedItemRealUid(impactedItem.getId());
        } else {
            setImpactedItemRealUid(null);
        }

    }

}
