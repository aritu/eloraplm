package com.aritu.eloraplm.treetable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.primefaces.PrimeFaces;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.core.DocumentContextBoundActions;

public abstract class CoreTree extends DocumentContextBoundActions {

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

    private static final TreeNode[] EMPTY_TREENODE_ARRAY = {};

    private TreeNode filteredValue;

    protected int firstLevelChildrenCount;

    protected int childrenCount;

    private boolean hasUnreadableNodes;

    private boolean isInvalid;

    public CoreTree() {
    }

    protected abstract void createRoot();

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
        firstLevelChildrenCount = root.getChildCount();
        childrenCount = countChildren(root, 0);
    }

    protected int countChildren(TreeNode node, int count) {
        count += node.getChildCount();
        for (TreeNode child : node.getChildren()) {
            count = countChildren(child, count);
        }
        return count;
    }

    public int getFirstLevelChildrenCount() {
        return firstLevelChildrenCount;
    }

    public int getChildrenCount() {
        return childrenCount;
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

    public void setFilteredValue(TreeNode filteredValue) {
        this.filteredValue = filteredValue;
    }

    public TreeNode getFilteredValue() {
        return filteredValue;
    }

    public boolean getIsFiltered(TreeTable treeTable) {
        if (treeTable != null) {
            return treeTable.isFilteringCurrentlyActive();
        }
        return false;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public void collapseAll(TreeTable treeTable) {
        collapseOrExpandAll(root, false);
        syncFilters(treeTable);
    }

    public void expandAll(TreeTable treeTable) {
        collapseOrExpandAll(root, true);
        syncFilters(treeTable);
    }

    public void reloadTree(TreeTable treeTable) {
        createRoot();
        clearFilters(treeTable);
        clearSelection();
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
        // clearFilters();
        // clearSelection();
    }

    public void clearSelection() {
        selectedNode = null;
        selectedNodes = EMPTY_TREENODE_ARRAY;

    }

    public void clearFilters(TreeTable treeTable) {
        if (treeTable != null && treeTable.isFilteringEnabled()) {
            treeTable.reset();
        }
    }

    /*
     * PF10 TreeTable does not sync filtered nodes with root nodes,
     * so we have to do it manually on some actions
     */
    public void syncFilters(TreeTable treeTable) {
        if (treeTable != null && treeTable.isFilteringEnabled()) {
            PrimeFaces.current().executeScript(
                    "PF('" + treeTable.getWidgetVar() + "').filter()");
        }
    }

    public boolean getIsDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public boolean getHasUnreadableNodes() {
        return hasUnreadableNodes;
    }

    public void setHasUnreadableNodes(boolean hasUnreadableNodes) {
        this.hasUnreadableNodes = hasUnreadableNodes;
    }

    public boolean getIsInvalid() {
        return isInvalid;
    }

    public void setIsInvalid(boolean isInvalid) {
        this.isInvalid = isInvalid;
    }

    public List<NodeData> getTreeAsList() {
        List<NodeData> nodeList = new ArrayList<NodeData>();
        nodeList = processNodeAndChildren(nodeList, root);
        return nodeList;
    }

    private List<NodeData> processNodeAndChildren(List<NodeData> nodeList, TreeNode node) {
        for (TreeNode child : node.getChildren()) {
            nodeList.add((NodeData) child.getData());
            nodeList = processNodeAndChildren(nodeList, child);
        }
        return nodeList;
    }

    public String getTreeBeanName() {
        try {
            return (String) this.getClass().getField("NAME").get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
