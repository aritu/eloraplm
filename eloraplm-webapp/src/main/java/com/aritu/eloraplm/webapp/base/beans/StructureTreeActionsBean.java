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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.DocumentContextBoundActions;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
@Name("structureTreeActions")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class StructureTreeActionsBean extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            StructureTreeActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected Map<String, String> messages;

    private TreeNode structureTree;

    private Map<String, TreeNode> nodes;

    private String currentPath;

    public TreeNode getStructureTree() {
        return structureTree;
    }

    public void reloadStructureTree() {
        buildStructureTree();
    }

    private void buildStructureTree() {
        String logInitMsg = "[buildStructureTree] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            String query = EloraQueryFactory.getEloraStructDocuments();
            DocumentModelList docs = documentManager.query(query);
            nodes = new HashMap<String, TreeNode>();
            // Root node
            structureTree = new DefaultTreeNode(null, null);
            for (DocumentModel doc : docs) {
                nodes.put(doc.getId(), new DefaultTreeNode(doc, null));
            }
            for (DocumentModel doc : docs) {
                if (doc.getType().equals(NuxeoDoctypeConstants.DOMAIN)) {
                    TreeNode domainNode = nodes.get(doc.getId());
                    domainNode.setExpanded(true);
                    structureTree.getChildren().add(domainNode);
                } else {
                    String parentId = doc.getParentRef() != null
                            ? doc.getParentRef().toString()
                            : null;
                    if (parentId != null) {
                        if (nodes.containsKey(parentId)) {
                            nodes.get(parentId).getChildren().add(
                                    nodes.get(doc.getId()));
                        }
                    }
                }
            }

            // Expand nodes
            expandTreeNodes();

        } catch (Exception e) {
            log.error(logInitMsg + "Uncontrolled error: "
                    + e.getClass().getName() + " - " + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.structureTree"));
        }

    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        String newPath = newCurrentDocumentModel.getPathAsString();
        if (currentPath == null || !newPath.equals(currentPath)) {
            currentPath = newPath;
            if (structureTree == null) {
                buildStructureTree();
            } else {
                expandTreeNodes();
            }
        }
    }

    private void expandTreeNodes() {
        processNodeAndChildren(structureTree);
    }

    private void processNodeAndChildren(TreeNode node) {
        // Check node for expansion
        DocumentModel data = (DocumentModel) node.getData();
        if (data != null) {
            if (currentPath.startsWith(data.getPathAsString())) {
                node.setExpanded(true);
            } else {
                node.setExpanded(false);
            }
        }

        if (!node.isLeaf()) {
            for (TreeNode child : node.getChildren()) {
                processNodeAndChildren(child);
            }
        }
    }

}
