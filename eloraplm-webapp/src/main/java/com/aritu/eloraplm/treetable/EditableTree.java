package com.aritu.eloraplm.treetable;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class EditableTree<T extends NodeData> extends CoreTree {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EditableTree.class);

    @In(create = true)
    protected transient WebActions webActions;

    private Date currentLastModified;

    private boolean isDirty = false;

    private T newNode;

    private T editingNode;

    @Override
    public boolean getIsDirty() {
        return isDirty;
    }

    @Override
    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public T getNewNode() {
        return newNode;
    }

    public void setNewNode(T newNode) {
        this.newNode = newNode;
    }

    public T getEditingNode() {
        return editingNode;
    }

    public void setEditingNode(T editingNode) {
        this.editingNode = editingNode;
    }

    public EditableTree() {
    }

    public void addNode(DocumentModel currentDoc, boolean showMessages) {
        String logInitMsg = "[addNode] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Adding node to tree of document |"
                + currentDoc.getId() + "|.");

        try {

            TreeNode node = createAndAddTreeNode();

            if (node != null) {

                // Set node's initial expanded state
                node.setExpanded(false);

                setIsDirty(true);

                executePostCreateActions();
            }

            resetCreateFormValues();

            log.trace(logInitMsg + "Node |" + node.getRowKey() + "| added.");

            // When we reset tabs, the current tab/subtab is lost,
            // so we have to get it before, and reset it after
            String currentTabId = webActions.getCurrentTabId();
            String currentSubTabId = webActions.getCurrentSubTabId();
            webActions.resetTabList();
            webActions.setCurrentTabId(currentTabId);
            webActions.setCurrentSubTabId(currentSubTabId);

            if (showMessages) {
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.treetable.node.add"));
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            if (showMessages) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.treetable.node.add"));
            }

        }

        resetCreateFormValues();

    }

    protected abstract TreeNode createAndAddTreeNode();

    protected void executePostCreateActions() {
    }

    public void markNodeAsModified(TreeTable table, TreeNode node) {
        markNodeAsModified(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void markNodeAsModified(TreeNode node) {
        @SuppressWarnings("unchecked")
        T nodeData = (T) node.getData();
        nodeData.setIsModified(true);
        setIsDirty(true);
    }

    public void markNodeAsRemoved(TreeTable table, TreeNode node,
            DocumentModel currentDoc) {
        markNodeAsRemoved(node, currentDoc);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());

    }

    private void markNodeAsRemoved(TreeNode node, DocumentModel currentDoc) {
        toggleNodeAsRemoved(true, node, currentDoc);
    }

    public void unmarkNodeAsRemoved(TreeTable table, TreeNode node,
            DocumentModel currentDoc) {
        unmarkNodeAsRemoved(node, currentDoc);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());

    }

    private void unmarkNodeAsRemoved(TreeNode node, DocumentModel currentDoc) {
        toggleNodeAsRemoved(false, node, currentDoc);
    }

    protected void toggleNodeAsRemoved(boolean isRemoved, TreeNode node,
            DocumentModel currentDoc) {

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }

        @SuppressWarnings("unchecked")
        T nodeData = (T) node.getData();

        if (nodeData.getIsNew()) {
            getRoot().getChildren().remove(node);

        } else {
            nodeData.setIsRemoved(isRemoved);
        }

        setIsDirty(true);
    }

    public void refreshNode(TreeTable table, TreeNode node) {
        refreshNode(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    protected abstract void refreshNode(TreeNode node);

    protected abstract void resetCreateFormValues();

    public String save(DocumentModel subjectDoc) throws EloraException {

        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "Saving relation changes to document |"
        // + currentDoc.getId() + "|");

        if (getIsDirty()) {

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                DocumentModel currentDoc = getCurrentDocument();
                if (currentDoc.isProxy()) {
                    currentDoc = documentManager.getWorkingCopy(
                            currentDoc.getRef());
                }
                if (subjectDoc.isProxy()) {
                    subjectDoc = documentManager.getWorkingCopy(
                            subjectDoc.getRef());
                }

                // initialChecks()
                //
                // TODO: Hay que sacar estos chequeos de aqui. Si cambia algo
                // termina afectando a cosas que no deberia. Por ejemplo, al
                // pasar el parametro isAnarchic para el chequeo, me ha hecho
                // cambiar plantillas y clases que les debería dar igual este
                // cambio
                // TODO Begiratu hau. Checkoutakin aldatu egin da
                // if (areRelationsEditable(currentDoc, isAnarchic)) {
                //
                List<TreeNode> firstLevelNodes = getRoot().getChildren();
                if (firstLevelNodes != null && !firstLevelNodes.isEmpty()) {
                    for (TreeNode childNode : firstLevelNodes) {

                        @SuppressWarnings("unchecked")
                        T nodeData = (T) childNode.getData();

                        if (nodeData.getIsNew()) {
                            doCreate(nodeData, subjectDoc);
                        } else if (nodeData.getIsRemoved()) {
                            doRemove(nodeData, subjectDoc);
                        } else if (nodeData.getIsModified()) {
                            doUpdate(nodeData, subjectDoc);
                        }
                    }
                }

                // executePostSaveActions()

                createRoot();

                // } else if (!currentDoc.isLocked()) {
                // facesMessages.add(StatusMessage.Severity.ERROR,
                // messages.get("label.relation.documentNotLocked"));
                // } else {
                // facesMessages.add(StatusMessage.Severity.ERROR,
                // messages.get(
                // "label.relation.documentLockedByOther"));
                // }

            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(e.getMessage()));
                TransactionHelper.setTransactionRollbackOnly();
            } catch (Exception e) {
                log.error(logInitMsg + "Uncontrolled exception: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(e.getMessage()));
                TransactionHelper.setTransactionRollbackOnly();
                navigationContext.invalidateCurrentDocument();
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        return null;
    }

    protected abstract void doCreate(T nodeData, DocumentModel subjectDoc)
            throws EloraException;

    protected abstract void doRemove(T nodeData, DocumentModel subjectDoc)
            throws EloraException;

    protected abstract void doUpdate(T nodeData, DocumentModel subjectDoc)
            throws EloraException;

    protected void executePostSaveActions() {

    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        boolean invalidate = false;
        Date newLastModified = null;
        if (doc != null) {
            GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_MODIFIED);
            if (lastModifiedGc != null) {
                // Taken from Nuxeo code:
                // -------------------------------------------------
                // remove milliseconds as they are not stored in some
                // databases, which could make the comparison fail just
                // after a document creation (see NXP-8783)
                // -------------------------------------------------
                lastModifiedGc.set(Calendar.MILLISECOND, 0);
                newLastModified = lastModifiedGc.getTime();
            }
        }

        if (currentLastModified == null || newLastModified == null) {
            if (!(currentLastModified == null && newLastModified == null)) {
                invalidate = true;
            }
        } else {
            if (currentLastModified.compareTo(newLastModified) != 0) {
                invalidate = true;
            }
        }

        if (invalidate) {
            currentLastModified = newLastModified;
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different modification date. Current: |"
                    + currentLastModified + "| New: |" + newLastModified + "|");
        }

        return doc;
    }

}
