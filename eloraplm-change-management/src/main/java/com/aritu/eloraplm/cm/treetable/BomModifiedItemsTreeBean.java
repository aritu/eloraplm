package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.UnlockCheckedOutDocumentException;
import com.aritu.eloraplm.pdm.promote.treetable.PromoteTreeBean;
import com.aritu.eloraplm.pdm.webapp.beans.PdmActionBean;
import com.aritu.eloraplm.webapp.base.beans.EloraWebActionsBean;

@Name("cmBomModifiedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomModifiedItemsTreeBean extends ModifiedItemsTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomModifiedItemsTreeBean.class);

    private boolean reloadBomModifiedItems = false;

    @In(create = true)
    protected transient PdmActionBean pdmAction;

    @In(create = true)
    protected transient PromoteTreeBean promoteTreeBean;

    @In(create = true)
    protected EloraWebActionsBean eloraWebActions;

    public BomModifiedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_BOM);
    }

    @Override
    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            super.saveTree();
            reloadBomModifiedItems = true;
            Events.instance().raiseEvent(
                    CMEventNames.CM_BOM_MODIFIED_ITEMS_SAVED);

            // Nuxeo event
            EloraEventHelper.fireEvent(CMEventNames.CM_BOM_MODIFIED_ITEMS_SAVED,
                    navigationContext.getCurrentDocument());

        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    // If this method is changed, we will have to change derived methods too
    // (<Client>BomModifiedItemsActionBean)
    @Override
    public void calculateImpactMatrix() {
        String logInitMsg = "[calculateImpactMatrix] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // if there is any unsaved change in the tree, don't do anything
        if (!getIsDirty()) {

            super.calculateImpactMatrix();

            Events.instance().raiseEvent(
                    CMEventNames.CM_BOM_IMPACT_MATRIX_CALCULATED);

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_BOM_IMPACT_MATRIX_CALCULATED,
                    navigationContext.getCurrentDocument());

        } else {
            log.trace(logInitMsg + "Unsaved changes.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.unsavedChanges"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    public TreeNode getRoot() {
        if (reloadBomModifiedItems) {
            createRoot();
            reloadBomModifiedItems = false;
        }
        return super.getRoot();
    }

    @Observer(value = { CMEventNames.CM_DOC_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_MODIFIED_ITEMS_DERIVED_FROM_IMPACTED_DOCS_ADDED,
            CMEventNames.CM_REFRESH_MODIFIED_ITEMS })
    @BypassInterceptors
    public void markTreeToBeReloaded() {
        reloadBomModifiedItems = true;
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
                                    getItemType(),
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
                    pdmAction.checkOut(selectedDoc, true);
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

    private boolean isCheckinAllowed(ModifiedItemsNodeData selectedNodeData) {
        if (selectedNodeData.getDestinationItem().isCheckedOut()) {
            return isNodeStateAllowed(selectedNodeData);
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.cm.isNotCheckedOut"));
        }
        return false;
    }

    private boolean isNodeStateAllowed(ModifiedItemsNodeData selectedNodeData) {
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
                                getItemType(), selectedNodeData.getNodeId());
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

    public void addModifiedItemsDerivedFromImpactedDocs() {
        String logInitMsg = "[addModifiedItemsDerivedFromImpactedDocs] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // This functionality is enabled by configuration
        if (eloraWebActions.isAddModifiedItemsDerivedFromImpactedDocsEnabled()) {

            try {
                DocumentModel currentDoc = navigationContext.getCurrentDocument();

                // Retrieve current Documents Impact Matrix
                ArrayList<HashMap<String, Object>> currentDocImpactMatrix = new ArrayList<HashMap<String, Object>>();
                if (currentDoc.getPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                    currentDocImpactMatrix = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
                }
                if (currentDocImpactMatrix.size() > 0) {

                    // Add modified items from impacted documents
                    boolean derivedModifiedItemsAdded = CMHelper.addModifiedItemsDerivedFromImpactedDocs(
                            documentManager, currentDoc);

                    // Check if something has been added
                    if (derivedModifiedItemsAdded) {
                        // BomModifiedItems should be reloaded
                        reloadBomModifiedItems = true;
                        Events.instance().raiseEvent(
                                CMEventNames.CM_MODIFIED_ITEMS_DERIVED_FROM_IMPACTED_DOCS_ADDED);

                        // Nuxeo event
                        EloraEventHelper.fireEvent(
                                CMEventNames.CM_MODIFIED_ITEMS_DERIVED_FROM_IMPACTED_DOCS_ADDED,
                                navigationContext.getCurrentDocument());

                        facesMessages.add(StatusMessage.Severity.INFO,
                                messages.get(
                                        "eloraplm.message.success.cm.addModifiedItemsDerivedFromImpactedDocs"));
                    } else {
                        facesMessages.add(StatusMessage.Severity.WARN,
                                messages.get(
                                        "eloraplm.message.success.cm.addModifiedItemsDerivedFromImpactedDocs.nothingToBeAdded"));
                    }
                } else {
                    log.trace(logInitMsg
                            + "Current Documents Impac Matrix is empty. Nothing to add as derived.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.warning.cm.addModifiedItemsDerivedFromImpacted.emptyDocImpactMatrix"));
                }
            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cm.addModifiedItemsDerivedFromImpactedDocs"));
            }
        } else {
            log.trace(logInitMsg
                    + "'Add modified items derived from impacted cads' functionality is not enabled.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.cm.addModifiedItemsDerivedFromImpactedDocs.notEnabled"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    @Factory(value = "bomModifiedItemsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}
