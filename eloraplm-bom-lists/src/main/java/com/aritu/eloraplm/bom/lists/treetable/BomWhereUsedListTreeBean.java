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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.BomListBean;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

/**
 * @author aritu
 *
 */

@Name("bomWhereUsedListTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomWhereUsedListTreeBean extends CoreTreeBean implements
        Serializable {

    private static final long serialVersionUID = 1L;

    @In
    protected transient BomListBean bomList;

    private Map<String, TreeNode> roots;

    @Override
    public void createRoot() {
        roots = new HashMap<String, TreeNode>();

        DocumentModel currentDoc = getCurrentDocument();
        try {
            BomWhereUsedListNodeService nodeService = new BomWhereUsedListNodeService(
                    documentManager, bomList.getId());
            setRoot(nodeService.getRoot(currentDoc));
        } catch (EloraException e) {
            // TODO Logetan idatzi

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.createRoot"));
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
}
