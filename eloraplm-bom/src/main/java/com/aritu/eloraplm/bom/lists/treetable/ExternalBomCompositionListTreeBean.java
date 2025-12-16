/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.bom.lists.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.treetable.NodeData;

/**
 * @author aritu
 *
 */

@Name(ExternalBomCompositionListTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ExternalBomCompositionListTreeBean extends CoreTree {

    public static final String NAME = "extBomCompositionListTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ExternalBomCompositionListTreeBean.class);

    @In(create = true)
    protected transient WebActions webActions;

    private Map<String, TreeNode> roots;

    private String bomListId;

    private String getBomListId() {
        if (bomListId == null) {
            bomListId = webActions.getCurrentSubTabId().replace(
                    webActions.getCurrentTabId().concat("_"), "");
        }
        return bomListId;
    }

    @Observer(WebActions.CURRENT_TAB_CHANGED_EVENT
            + "_TAB_BOM_COMPOSITION_sub_tab")
    @BypassInterceptors
    public void resetBomListId() {
        bomListId = null;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        roots = new HashMap<>();

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");

            ExternalBomListNodeService nodeService = Framework.getService(
                    ExternalBomListNodeService.class);

            nodeService.init(documentManager, 0,
                    EloraRelationConstants.DIRECTION_COMPOSITION,
                    getBomListId());

            setRoot(nodeService.getRoot(currentDoc));
            setIsDirty(false);
            setHasUnreadableNodes(false);
            setIsInvalid(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (DocumentSecurityException e) {
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
    }

    @Override
    public TreeNode getRoot() {
        if (roots.isEmpty() || !roots.containsKey(getBomListId())) {
            createRoot();
        }
        return roots.get(getBomListId());
    }

    @Override
    public void setRoot(TreeNode root) {
        if (roots != null) {
            roots.put(getBomListId(), root);
        }
        firstLevelChildrenCount = root.getChildCount();
        childrenCount = countChildren(root, 0);
    }

    @Override
    public void collapseAll(TreeTable treeTable) {
        if (roots != null && roots.containsKey(getBomListId())) {
            collapseOrExpandAll(roots.get(getBomListId()), false);
            clearFilters(treeTable);
        }
    }

    @Override
    public void expandAll(TreeTable treeTable) {
        if (roots != null && roots.containsKey(getBomListId())) {
            collapseOrExpandAll(roots.get(getBomListId()), true);
            clearFilters(treeTable);
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        // Empty all the roots, so we don't get a tree of another document in
        // all the subtabs except in the first loaded
        roots = null;
        createRoot();
    }

    @Override
    public List<NodeData> getTreeAsList() {
        List<NodeData> nodeList = new ArrayList<NodeData>();
        if (!roots.isEmpty() && roots.containsKey(getBomListId())) {
            nodeList = processNodeAndChildren(nodeList, roots.get(getBomListId()));
        }
        return nodeList;
    }

    private List<NodeData> processNodeAndChildren(List<NodeData> nodeList, TreeNode node) {
        for (TreeNode child : node.getChildren()) {
            nodeList.add((NodeData) child.getData());
            nodeList = processNodeAndChildren(nodeList, child);
        }
        return nodeList;
    }

}
