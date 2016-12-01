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

public class CoreTreeBean extends EloraDocContextBoundActionBean
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

    private TreeNode root;

    public CoreTreeBean() {
    }

    public void createRoot() {
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
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
}