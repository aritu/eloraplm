package com.aritu.eloraplm.bom.characteristics.treetable;

import java.io.Serializable;

import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.util.BomHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

public abstract class BomCharacteristicsTreeBean extends CoreTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsTreeBean.class);

    private String bomType;

    private BomCharacteristicsNodeService nodeService;

    private Serializable currentClassificationValue;

    public BomCharacteristicsTreeBean(String bomType) {
        this.bomType = bomType;
    }

    @Override
    public void createRoot() {

        String logInitMsg = "[" + bomType + "] [createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService = new BomCharacteristicsNodeService(documentManager,
                    bomType, messages);
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
        String logInitMsg = "[" + bomType + "] [saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            try {
                DocumentModel currentDoc = getCurrentDocument();

                boolean areRequiredFieldsFilled = nodeService.verifyRequiredFields(
                        getRoot());

                if (areRequiredFieldsFilled) {
                    nodeService.saveTree(currentDoc, getRoot());
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.treetable.saveRoot"));

                    // invalidate current document in order to refresh its
                    // content
                    // in the navigation context
                    navigationContext.invalidateCurrentDocument();

                    // refresh current treebean
                    resetBeanCache(null);

                } else {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            "Fill required fields, please"); // TODO:: itzulpena
                                                             // gehitu honi
                }

            } catch (Exception e) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        "Fill required fields, please");
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.treetable.saveRoot"));
            }
        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    // TODO:: TO BE REMOVED. Checkin egiten ez daukatu treebean-ik...
    /*public boolean verifyRequiredFields() {
        String logInitMsg = "[verifyRequiredFields] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
    
        boolean areRequiredFieldsFilled = nodeService.verifyRequiredFields(
                getRoot());
    
        log.trace(logInitMsg + "--- EXIT --- with areRequiredFieldsFilled = |"
                + areRequiredFieldsFilled + "|");
        return areRequiredFieldsFilled;
    }*/

    public void markNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[markNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleNodeAsRemoved(true, node);

        setIsDirty(true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void unmarkNodeAsRemoved(TreeNode node) {
        String logInitMsg = "[unmarkNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        toggleNodeAsRemoved(false, node);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void toggleNodeAsRemoved(boolean isRemoved, TreeNode node) {
        String logInitMsg = "[toggleNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        BomCharacteristicsNodeData nodeData = (BomCharacteristicsNodeData) node.getData();

        // TODO:: check if it is required or not. It cannot be removed if it is
        // required
        if (nodeData.getBomCharacteristic().getRequired()) {
            // TODO:: THROW EXCEPTION
        }
        // If it is a new node, remove it completely
        if (nodeData.getIsNew()) {
            node.getParent().getChildren().remove(node);
        } else {
            nodeData.setIsRemoved(isRemoved);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void loadCharacteristicMasters() {
        String logInitMsg = "[" + bomType + "] [loadCharacteristicMasters] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            setRoot(nodeService.loadCharacteristicMasters(currentDoc,
                    getRoot()));

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.loadCharacteristicMasters"));

            // invalidate current document in order to refresh its content
            // in the navigation context
            navigationContext.invalidateCurrentDocument();

            // refresh current treebean
            resetBeanCache(null);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.loadCharacteristicMasters"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            nodeService.refreshNode(node, triggeredField);

            setIsDirty(true);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        // if a check-out or check-in action has been done in the document
        if (!(doc.isCheckedOut() == getCurrentDocument().isCheckedOut())) {
            resetAll(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different checked out status.");
        }
        // check if classification has changed
        String classification = BomHelper.getBomClassificationValue(doc);
        if (currentClassificationValue != classification) {
            resetAll(doc);
        }

        return doc;
    }

    protected void resetAll(DocumentModel doc) {
        setCurrentDocument(doc);
        if (doc != null) {
            currentClassificationValue = BomHelper.getBomClassificationValue(
                    doc);
        } else {
            currentClassificationValue = null;
        }
        resetBeanCache(doc);
    }

}