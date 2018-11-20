package com.aritu.eloraplm.treetable;

import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;

public abstract class CoreTreeBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private boolean isDirty = false;

    private TreeNode root;

    private TreeNode selectedNode;

    private TreeNode[] selectedNodes;

    public CoreTreeBean() {
    }

    protected abstract void createRoot();

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public void collapseAll() {
        collapseOrExpandAll(root, false);
    }

    public void expandAll() {
        collapseOrExpandAll(root, true);
    }

    public void reloadTree() {
        createRoot();
    }

    public void collapseOrExpandAll(TreeNode node, boolean expanded) {
        if (node.getChildren().size() > 0) {
            for (TreeNode childNode : node.getChildren()) {
                collapseOrExpandAll(childNode, expanded);
            }
            node.setExpanded(expanded);
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        createRoot();
    }

    public boolean getIsDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    protected abstract TreeNode getRootFromFactory();

}