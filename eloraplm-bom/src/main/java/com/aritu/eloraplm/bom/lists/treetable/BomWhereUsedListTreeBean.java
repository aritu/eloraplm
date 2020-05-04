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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.BomListBean;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

/**
 * @author aritu
 *
 */

@Name("bomWhereUsedListTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomWhereUsedListTreeBean extends CoreTreeBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomWhereUsedListTreeBean.class);

    @In
    protected transient BomListBean bomList;

    private Map<String, TreeNode> roots;

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        roots = new HashMap<String, TreeNode>();

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            BomWhereUsedListNodeService nodeService = new BomWhereUsedListNodeService(
                    documentManager, bomList.getId());
            setRoot(nodeService.getRoot(currentDoc));
            setHasUnreadableNodes(false);
            setIsInvalid(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (DocumentUnreadableException e) {
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
        String bomListId = bomList.getId();
        if (roots.isEmpty() || !roots.containsKey(bomListId)) {
            createRoot();
        }
        return roots.get(bomListId);
    }

    @Override
    public void setRoot(TreeNode root) {
        String bomListId = bomList.getId();
        if (roots != null) {
            roots.put(bomListId, root);
        }
    }

    @Override
    public void collapseAll() {
        String bomListId = bomList.getId();
        if (roots != null && roots.containsKey(bomListId)) {
            collapseOrExpandAll(roots.get(bomListId), false);
        }
    }

    @Override
    public void expandAll() {
        String bomListId = bomList.getId();
        if (roots != null && roots.containsKey(bomListId)) {
            collapseOrExpandAll(roots.get(bomListId), true);
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
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        if (!(doc.isCheckedOut() == getCurrentDocument().isCheckedOut())) {
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different checked out status.");
        }

        return doc;
    }

    @Override
    @Factory(value = "bomWhereUsedListRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }
}
