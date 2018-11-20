package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

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

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.UnlockCheckedOutDocumentException;
import com.aritu.eloraplm.promote.treetable.PromoteTreeBean;

@Name("cmDocModifiedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class DocModifiedItemsTreeBean extends ModifiedItemsTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            DocModifiedItemsTreeBean.class);

    private boolean reloadDocModifiedItems = false;

    @In(create = true)
    protected transient PromoteTreeBean promoteTreeBean;

    public DocModifiedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_DOC);
    }

    @Override
    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            super.saveTree();
            reloadDocModifiedItems = true;
            Events.instance().raiseEvent(
                    CMEventNames.CM_DOC_MODIFIED_ITEMS_SAVED);

            // Nuxeo event
            EloraEventHelper.fireEvent(CMEventNames.CM_DOC_MODIFIED_ITEMS_SAVED,
                    navigationContext.getCurrentDocument());

        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    public void calculateImpactMatrix() {
        String logInitMsg = "[calculateImpactMatrix] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        super.calculateImpactMatrix();

        Events.instance().raiseEvent(
                CMEventNames.CM_DOC_IMPACT_MATRIX_CALCULATED);

        // Nuxeo event
        EloraEventHelper.fireEvent(CMEventNames.CM_DOC_IMPACT_MATRIX_CALCULATED,
                navigationContext.getCurrentDocument());

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    public TreeNode getRoot() {
        if (reloadDocModifiedItems) {
            createRoot();
            reloadDocModifiedItems = false;
        }
        return super.getRoot();
    }

    @Observer(CMEventNames.CM_BOM_MODIFIED_ITEMS_SAVED)
    @BypassInterceptors
    public void markTreeToBeRealoded() {
        reloadDocModifiedItems = true;
    }

    public void lock() throws EloraException {
        if (getSelectedNode() != null) {
            ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) getSelectedNode().getData();
            if (isNodeStateAllowed(selectedNodeData)) {
                DocumentModel selectedDoc = selectedNodeData.getDestinationItem();
                try {
                    EloraDocumentHelper.lockDocument(selectedDoc);
                    reloadTree();
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
                }
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.emptyItemSelection"));
        }
    }

    public void unlock() throws EloraException {
        if (getSelectedNode() != null) {
            ModifiedItemsNodeData selectedNodeData = (ModifiedItemsNodeData) getSelectedNode().getData();
            if (isNodeStateAllowed(selectedNodeData)) {
                DocumentModel selectedDoc = selectedNodeData.getDestinationItem();

                try {
                    EloraDocumentHelper.unlockDocument(selectedDoc);
                    reloadTree();
                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get("eloraplm.message.success.unlock"));
                } catch (DocumentLockRightsException e) {
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.unlock.permissions"));
                } catch (UnlockCheckedOutDocumentException e) {
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.unlock.checkedOut"));
                }
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.cm.emptyItemSelection"));
        }
    }

    private boolean isNodeStateAllowed(ModifiedItemsNodeData selectedNodeData) {
        if (!selectedNodeData.getIsManaged()) {
            if (!selectedNodeData.getAction().equals(CMConstants.ACTION_IGNORE)
                    && !selectedNodeData.getAction().equals(
                            CMConstants.ACTION_REMOVE)) {
                if (!selectedNodeData.getIsDirty()) {
                    return true;
                } else {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.cm.nodeIsDirty"));
                }
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

    public void runPromote() throws EloraException {
        promoteTreeBean.runPromoteAction();
        reloadTree();
    }

    @Override
    @Factory(value = "docModifiedItemsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }
}