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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.BomListHelper;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;

/**
 * @author aritu
 *
 */
public class BomListNodeServiceImpl implements BomListNodeService {

    private static final Log log = LogFactory.getLog(
            BomListNodeServiceImpl.class);

    protected CoreSession session;

    protected RelationManager relationManager = Framework.getLocalService(
            RelationManager.class);

    protected DocumentModel currentDoc;

    protected String treeDirection;

    protected int nodeId;

    protected String bomListId;

    protected List<String> parentUids;

    @Override
    public void init(CoreSession session, int nodeId, String treeDirection,
            String bomListId) {
        this.session = session;
        this.nodeId = nodeId;
        this.treeDirection = treeDirection;
        this.bomListId = bomListId;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aritu.eloraplm.treetable.NodeService#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object parentObject)
            throws EloraException, DocumentUnreadableException {

        // TODO Hemen WC dala bakarrik gabitz konsideratzen!!!!!

        int level = 0;

        parentUids = new ArrayList<>();

        DocumentModel currentDoc = (DocumentModel) parentObject;

        if (currentDoc.isProxy()) {
            throw new EloraException("The root document is a proxy.");
        }

        RelationNodeData nodeData = saveRelationNodeData(String.valueOf(nodeId),
                level, currentDoc.getId(), currentDoc, null, null, null, null,
                null, null);

        nodeId++;

        TreeNode root = processTreeNode(null, null, nodeData, level);

        return root;
    }

    /**
     * @param parentNode
     * @param nodeData
     * @return
     * @throws EloraException
     * @throws DocumentUnreadableException
     */
    private TreeNode processTreeNode(TreeNode rootNode, TreeNode parentNode,
            RelationNodeData nodeData, int level)
            throws EloraException, DocumentUnreadableException {

        TreeNode node = new DefaultTreeNode(nodeData, parentNode);
        // Set nodes initial expanded state
        node.setExpanded(NODES_EXPANDED_BY_DEFAULT);

        if (rootNode == null) {
            rootNode = node;
        }

        // Store parent nodes, to avoid a loop
        parentUids.add(nodeData.getDocId());

        List<RelationNodeData> childNodeList;
        level++;

        // Get child nodes
        childNodeList = getChildrenNodeData(nodeData, level);

        // For each child, processTreeNode
        if (!childNodeList.isEmpty()) {
            for (RelationNodeData childNode : childNodeList) {

                // Only process it if it isn't in the parents
                if (!parentUids.contains(childNode.getDocId())) {
                    rootNode = processTreeNode(rootNode, node, childNode,
                            level);
                }
            }
        }

        // Remove the last parent, as the level is processed
        parentUids.remove(parentUids.size() - 1);

        // Return root TreeNode
        return rootNode;
    }

    /**
     * @param nodeData
     * @param level
     * @return
     * @throws EloraException
     * @throws DocumentUnreadableException
     */
    private List<RelationNodeData> getChildrenNodeData(
            RelationNodeData nodeData, int level)
            throws EloraException, DocumentUnreadableException {
        List<RelationNodeData> childNodeList = new ArrayList<>();
        DocumentModel parentDoc = nodeData.getData();

        DocumentModelList bomListDocs = BomListHelper.getBomListForDocument(
                parentDoc, bomListId,
                treeDirection.equals(TREE_DIRECTION_WHERE_USED), session);

        if (bomListDocs != null) {

            String predicateUri = null;
            if (treeDirection.equals(TREE_DIRECTION_COMPOSITION)) {
                predicateUri = EloraRelationConstants.BOM_LIST_HAS_ENTRY;
            } else {
                predicateUri = EloraRelationConstants.BOM_HAS_LIST;
            }
            Resource relatedBomPredicateResource = new ResourceImpl(
                    predicateUri);

            // 2. Get the related documents for each BomList: list entries in
            // Composition, BOM docs in WhereUsed
            for (DocumentModel bomListDoc : bomListDocs) {
                List<Statement> relatedBomStmts = null;
                if (treeDirection.equals(TREE_DIRECTION_COMPOSITION)) {
                    relatedBomStmts = RelationHelper.getStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, bomListDoc,
                            relatedBomPredicateResource);
                } else {
                    relatedBomStmts = EloraRelationHelper.getSubjectStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, bomListDoc,
                            relatedBomPredicateResource);
                }

                if (relatedBomStmts != null && !relatedBomStmts.isEmpty()) {
                    for (Statement stmt : relatedBomStmts) {
                        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                                stmt);

                        DocumentModel childDoc = null;
                        if (treeDirection.equals(TREE_DIRECTION_COMPOSITION)) {
                            childDoc = EloraDocumentHelper.getDocumentModel(
                                    relationManager, session, stmt.getObject());
                        } else {
                            childDoc = EloraDocumentHelper.getDocumentModel(
                                    relationManager, session,
                                    stmt.getSubject());
                        }

                        if (childDoc == null) {
                            log.trace(
                                    "Throw DocumentUnreadableException since childDoc is null. stmt = |"
                                            + stmt.toString() + "|");
                            throw new DocumentUnreadableException(
                                    "Error getting document from statement |"
                                            + stmt.toString() + "|");
                        }

                        String quantity = stmtInfo.getQuantity();
                        String comment = stmtInfo.getComment();
                        Integer ordering = stmtInfo.getOrdering();

                        DocumentModel wcDoc = null;
                        if (childDoc.isImmutable()) {
                            wcDoc = session.getWorkingCopy(childDoc.getRef());
                        } else {
                            wcDoc = childDoc;
                        }

                        RelationNodeData node = saveRelationNodeData(
                                String.valueOf(nodeId), level, childDoc.getId(),
                                childDoc, wcDoc, stmt,
                                EloraRelationConstants.BOM_LIST_HAS_ENTRY,
                                quantity, comment, ordering);

                        childNodeList.add(node);

                        nodeId++;
                    }
                }
            }
        }

        return childNodeList;
    }

    /**
     *
     * @param id
     * @param level
     * @param docId
     * @param data
     * @param predicateUri
     * @param quantity
     * @param comment
     * @param isSpecial
     * @return
     */
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, String quantity,
            String comment, Integer ordering) {

        RelationNodeData nodeData = new BaseRelationNodeData(id, level, docId,
                data, wcDoc, stmt, predicateUri, quantity, comment, ordering,
                null, null, false, false);

        return nodeData;
    }

}
