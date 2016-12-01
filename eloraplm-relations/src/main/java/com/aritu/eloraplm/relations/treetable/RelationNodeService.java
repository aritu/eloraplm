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
package com.aritu.eloraplm.relations.treetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class RelationNodeService implements NodeManager {

    public static final String TREE_DIRECTION_COMPOSITION = "Composition";

    public static final String TREE_DIRECTION_WHERE_USED = "WhereUsed";

    protected static final String RELATION_TYPE_CAD = "CadRelation";

    protected static final String RELATION_TYPE_BOM = "BomRelation";

    protected static final boolean NODES_EXPANDED_BY_DEFAULT = false;

    protected CoreSession session;

    protected RelationManager relationManager = Framework.getLocalService(
            RelationManager.class);

    protected Map<String, Boolean> iconOnlyPredicates;

    protected Map<String, Boolean> predicates;

    protected Map<String, List<String>> iconOnlyRelationDocs;

    protected DocumentModel currentDoc;

    protected String relationType;

    protected String treeDirection;

    protected int nodeId;

    protected boolean specialInLevel0;

    protected boolean showUniqueVersionsPerDocument;

    public RelationNodeService(CoreSession session) {

        this.session = session;
        iconOnlyPredicates = new LinkedHashMap<String, Boolean>();
        predicates = new LinkedHashMap<String, Boolean>();
        treeDirection = "";
        relationType = "";
        nodeId = 0;
        showUniqueVersionsPerDocument = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aritu.eloraplm.treetable.NodeService#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {

        int level = 0;

        // TODO proxy bat bada fallau,... txekeuak
        DocumentModel parentDoc = (DocumentModel) parentObject;

        if (parentDoc.isProxy()) {
            throw new EloraException("The root document is a proxy.");
        }

        // If the root document is a working copy, we get the tree of its base
        // document
        if (parentDoc.isVersion() || parentDoc.isCheckedOut()) {
            currentDoc = parentDoc;
        } else {
            currentDoc = EloraDocumentHelper.getLatestVersion(parentDoc,
                    session);
        }

        RelationNodeData nodeData = saveRelationNodeData(String.valueOf(nodeId),
                level, currentDoc.getId(), currentDoc, null, null, null, 0,
                null, false, 0, false);

        nodeId++;

        // Get icon only documents
        iconOnlyRelationDocs = new HashMap<String, List<String>>();
        if (treeDirection.equals(TREE_DIRECTION_COMPOSITION)) {
            iconOnlyRelationDocs = getIconOnlyRelationDocs();
        }

        TreeNode root = processTreeNode(null, null, nodeData, level);

        return root;
    }

    /**
     * @param parentNode
     * @param nodeData
     * @return
     * @throws EloraException
     */
    private TreeNode processTreeNode(TreeNode rootNode, TreeNode parentNode,
            RelationNodeData nodeData, int level) throws EloraException {

        TreeNode node = new DefaultTreeNode(nodeData, parentNode);
        // Set nodes initial expanded state
        node.setExpanded(NODES_EXPANDED_BY_DEFAULT);

        if (rootNode == null) {
            rootNode = node;
        }

        List<RelationNodeData> childNodeList;
        level++;
        // Check if it is special in level 0 (now 1)
        if (level == 1 && treeDirection.equals(TREE_DIRECTION_COMPOSITION)
                && nodeData.getData().getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING)) {

            specialInLevel0 = true;
            childNodeList = getSpecialInLevel0ChildrenNodeData(nodeData);

        } else {

            // Get child nodes
            childNodeList = getChildrenNodeData(nodeData, level);
        }

        // For each child, processTreeNode
        if (!childNodeList.isEmpty()) {
            for (RelationNodeData childNode : childNodeList) {
                rootNode = processTreeNode(rootNode, node, childNode, level);
            }
        }

        // Return root TreeNode
        return rootNode;
    }

    /**
     * Gets the UIDs of the documents that have an iconOnly relation with the
     * current document
     *
     * @param nodeData
     * @return
     * @throws EloraException
     */
    private Map<String, List<String>> getIconOnlyRelationDocs()
            throws EloraException {
        Map<String, List<String>> iconOnlyRelationDocs = new HashMap<String, List<String>>();

        for (Map.Entry<String, Boolean> predicateEntry : iconOnlyPredicates.entrySet()) {
            String predicateUri = predicateEntry.getKey();
            Boolean isSpecial = predicateEntry.getValue();

            Resource predicateResource = new ResourceImpl(predicateUri);

            DocumentModelList relatedDocs;
            if (isSpecial) {
                relatedDocs = RelationHelper.getSubjectDocuments(
                        predicateResource, currentDoc);
            } else {
                relatedDocs = RelationHelper.getObjectDocuments(currentDoc,
                        predicateResource);
            }

            if (!relatedDocs.isEmpty()) {
                for (DocumentModel doc : relatedDocs) {
                    String docId = doc.getId();
                    if (iconOnlyRelationDocs.containsKey(docId)) {
                        iconOnlyRelationDocs.get(docId).add(predicateUri);
                    } else {
                        List<String> relations = new ArrayList<String>();
                        relations.add(predicateUri);
                        iconOnlyRelationDocs.put(doc.getId(), relations);
                    }
                }
            }
        }

        return iconOnlyRelationDocs;
    }

    /**
     * @param nodeData
     * @return
     * @throws EloraException
     */
    private List<RelationNodeData> getSpecialInLevel0ChildrenNodeData(
            RelationNodeData nodeData) throws EloraException {
        List<RelationNodeData> childNodeList = new ArrayList<RelationNodeData>();
        DocumentModel parentDoc = nodeData.getData();

        String predicateUri = EloraRelationConstants.CAD_DRAWING_OF;
        Resource predicateResource = new ResourceImpl(predicateUri);
        boolean isSpecial = false;
        boolean inverse = false;
        List<Statement> relatedStmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                predicateResource);

        childNodeList.addAll(treatRelatedDocs(relatedStmts, predicateUri,
                parentDoc.isVersion(), inverse, 1, isSpecial));

        return childNodeList;
    }

    /**
     * @param nodeData
     * @param level
     * @return
     * @throws EloraException
     */
    private List<RelationNodeData> getChildrenNodeData(
            RelationNodeData nodeData, int level) throws EloraException {
        List<RelationNodeData> childNodeList = new ArrayList<RelationNodeData>();
        DocumentModel parentDoc = nodeData.getData();

        for (Map.Entry<String, Boolean> predicateEntry : predicates.entrySet()) {
            String predicateUri = predicateEntry.getKey();
            Boolean isSpecial = predicateEntry.getValue();

            Resource predicateResource = new ResourceImpl(predicateUri);

            Map<String, List<DocumentModel>> docList = new HashMap<String, List<DocumentModel>>();
            Map<String, Statement> stmtList = new HashMap<String, Statement>();

            boolean inverse = false;
            List<Statement> relatedStmts;
            if (treeDirection.equals(TREE_DIRECTION_COMPOSITION)) {
                if (isSpecial) {
                    // Don't show special relations of the main document in
                    // composition
                    if (level == 1) {
                        continue;
                    }
                    relatedStmts = EloraRelationHelper.getSubjectStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                            predicateResource);

                    inverse = true;
                } else {
                    relatedStmts = RelationHelper.getStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                            predicateResource);
                }
            } else if (treeDirection.equals(TREE_DIRECTION_WHERE_USED)) {
                if (isSpecial) {
                    relatedStmts = RelationHelper.getStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                            predicateResource);
                } else {
                    relatedStmts = EloraRelationHelper.getSubjectStatements(
                            EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                            predicateResource);
                    inverse = true;
                }
            } else {
                throw new EloraException(
                        "Unsupported tree direction provided.");
            }

            for (Statement stmt : relatedStmts) {
                DocumentModel relatedDoc = null;
                if (inverse) {
                    relatedDoc = RelationHelper.getDocumentModel(
                            stmt.getSubject(), session);
                } else {
                    relatedDoc = RelationHelper.getDocumentModel(
                            stmt.getObject(), session);
                }

                // TODO: Poner control de permisos.
                // We consider that when relatedDoc is null user doesn't have
                // any permission on the document

                // TODO: se puede mirar si todo esto se podría meter en la
                // estructura de una clase y luego no tener que andar
                // consultando datos en el bucle
                String versionSeriesId = session.getVersionSeriesId(
                        relatedDoc.getRef());
                if (docList.containsKey(versionSeriesId)) {
                    docList.get(versionSeriesId).add(relatedDoc);
                } else {
                    List<DocumentModel> relatedDocList = new ArrayList<DocumentModel>();
                    relatedDocList.add(relatedDoc);
                    docList.put(versionSeriesId, relatedDocList);
                }
                stmtList.put(relatedDoc.getId(), stmt);

            }

            childNodeList.addAll(
                    treatRelatedDocs(docList, stmtList, predicateUri,
                            parentDoc.isVersion(), inverse, level, isSpecial));
        }

        return childNodeList;
    }

    /**
     * @param relatedDocs
     * @param level
     * @param currentDocType
     * @param predicateUri
     * @param parentIsVersion
     * @return
     * @throws EloraException
     */
    private List<RelationNodeData> treatRelatedDocs(
            List<Statement> relatedStmts, String predicateUri,
            boolean parentIsVersion, boolean inverse, int level,
            boolean isSpecial) throws EloraException {
        List<RelationNodeData> childNodeList = new ArrayList<RelationNodeData>();
        if (relatedStmts != null && !relatedStmts.isEmpty()) {

            Map<String, List<String>> processedRels = new HashMap<String, List<String>>();

            for (Statement stmt : relatedStmts) {

                DocumentModel childDoc;
                if (inverse) {
                    childDoc = RelationHelper.getDocumentModel(
                            stmt.getSubject(), session);
                } else {
                    childDoc = RelationHelper.getDocumentModel(stmt.getObject(),
                            session);
                }

                if (childDoc != null) {
                    // TODO HAU OBJECT->SUBJECT norantzan bakarrik da???
                    // parentIsVersion hori atara. ZE PASETAN DA SPECIALEKIN???
                    // DRAWING ADB???

                    // If the user creates a relation from WC to a specific
                    // archived version (instead of to another WC), we have to
                    // hide this relation when we display the tree of an AV
                    // document, or the user will see the same document twice
                    // (one AV and the WC).
                    // If parent element is an AV and child is WC, ignore it
                    if (parentIsVersion && !childDoc.isVersion()) {
                        continue;
                    }

                    String versionSeriesId = childDoc.getVersionSeriesId();

                    // Check if there is a special in level 0, and avoid to
                    // repeat it (even when it is the same document in a
                    // different version)
                    if (level == 2 && specialInLevel0 && isSpecial) {

                        if (versionSeriesId.equals(
                                currentDoc.getVersionSeriesId())) {
                            continue;
                        }
                    }

                    // If showUniqueVersionsPerDocument is checked, discard
                    // duplicate docs with different versions
                    if (showUniqueVersionsPerDocument) {
                        if (processedRels.containsKey(versionSeriesId)) {
                            if (processedRels.get(versionSeriesId).contains(
                                    predicateUri)) {
                                // Ignore if this relation has been processed
                                // before to avoid painting it twice
                                continue;
                            } else {
                                // It has multiple different relations to the
                                // same document. If this is possible continue
                                // painting node
                                processedRels.get(versionSeriesId).add(
                                        predicateUri);
                            }
                        } else {
                            List<String> predicateList = new ArrayList<String>();
                            predicateList.add(predicateUri);
                            processedRels.put(versionSeriesId, predicateList);
                        }

                        // If composition & special or where used, get the
                        // correct version
                        if (childDoc.isVersion() && (treeDirection.equals(
                                TREE_DIRECTION_WHERE_USED) || isSpecial)) {
                            childDoc = EloraRelationHelper.getLatestRelatedReleasedVersion(
                                    childDoc, stmt, session);
                        }
                    }

                    String childUid = childDoc.getId();

                    EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                            stmt);
                    int quantity = stmtInfo.getQuantity();
                    String comment = stmtInfo.getComment();
                    boolean isObjectWc = stmtInfo.getIsObjectWc();
                    int ordering = stmtInfo.getOrdering();

                    DocumentModel wcDoc = null;
                    if (childDoc.isImmutable()) {
                        wcDoc = session.getWorkingCopy(childDoc.getRef());
                    } else {
                        wcDoc = childDoc;
                    }

                    RelationNodeData node = saveRelationNodeData(
                            String.valueOf(nodeId), level, childUid, childDoc,
                            wcDoc, stmt, predicateUri, quantity, comment,
                            isObjectWc, ordering, isSpecial);

                    // Check if it has iconOnlyRelationDocs
                    if (iconOnlyRelationDocs.containsKey(childUid)) {
                        List<String> iconOnlyRelations = iconOnlyRelationDocs.get(
                                childUid);
                        node.setIconOnlyRelations(iconOnlyRelations);
                    }

                    childNodeList.add(node);

                    nodeId++;
                }
            }
        }

        return childNodeList;
    }

    private List<RelationNodeData> treatRelatedDocs(
            Map<String, List<DocumentModel>> docList,
            Map<String, Statement> stmtList, String predicateUri,
            boolean parentIsVersion, boolean inverse, int level,
            boolean isSpecial) throws EloraException {

        List<RelationNodeData> childNodeList = new ArrayList<RelationNodeData>();
        if (docList != null && !docList.isEmpty()) {
            for (Map.Entry<String, List<DocumentModel>> entry : docList.entrySet()) {
                String versionSeriesId = entry.getKey();
                List<DocumentModel> docs = entry.getValue();

                RelationNodeData node = null;

                if (showUniqueVersionsPerDocument) {
                    node = createNodeData(docs.get(0), docs, versionSeriesId,
                            docs.size(), predicateUri, stmtList, isSpecial,
                            inverse, parentIsVersion, level);
                    if (node != null) {
                        childNodeList.add(node);
                    }
                } else {
                    for (DocumentModel doc : docs) {
                        node = createNodeData(doc, docs, versionSeriesId, 0,
                                predicateUri, stmtList, isSpecial, inverse,
                                parentIsVersion, level);
                        if (node != null) {
                            childNodeList.add(node);
                        }
                    }
                }
            }
        }

        return childNodeList;
    }

    protected RelationNodeData createNodeData(DocumentModel childDoc,
            List<DocumentModel> docs, String versionSeriesId, int docNum,
            String predicateUri, Map<String, Statement> stmtList,
            boolean isSpecial, boolean inverse, boolean parentIsVersion,
            int level) throws EloraException {

        if (childDoc != null) {
            // TODO HAU OBJECT->SUBJECT norantzan bakarrik da???
            // parentIsVersion hori atara. ZE PASETAN DA SPECIALEKIN???
            // DRAWING ADB???

            // If the user creates a relation from WC to a specific
            // archived version (instead of to another WC), we have to
            // hide this relation when we display the tree of an AV
            // document, or the user will see the same document twice
            // (one AV and the WC).
            // If parent element is an AV and child is WC, ignore it
            if (parentIsVersion && !childDoc.isVersion()) {
                return null;
            }
            // Check if there is a special in level 0, and avoid to
            // repeat it (even when it is the same document in a
            // different version)
            if (level == 2 && specialInLevel0 && isSpecial) {
                if (versionSeriesId.equals(currentDoc.getVersionSeriesId())) {
                    return null;
                }
            }

            // If showUniqueVersionsPerDocument is checked, discard
            // duplicate docs with different versions
            if (docNum > 1) {
                // If composition & special or where used, get the
                // correct version
                if (childDoc.isVersion()
                        && (treeDirection.equals(TREE_DIRECTION_WHERE_USED)
                                || isSpecial)) {
                    List<String> uidList = new ArrayList<String>();

                    // TODO: se puede evitar esto sacandolo antes?¿?¿
                    for (DocumentModel doc : docs) {
                        uidList.add(doc.getId());
                    }
                    childDoc = EloraRelationHelper.getLatestRelatedReleasedVersion(
                            versionSeriesId, uidList, session);
                }
            }

            String childUid = childDoc.getId();

            Statement stmt = stmtList.get(childUid);
            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
            int quantity = stmtInfo.getQuantity();
            String comment = stmtInfo.getComment();
            boolean isObjectWc = stmtInfo.getIsObjectWc();
            int ordering = stmtInfo.getOrdering();

            DocumentModel wcDoc = null;
            if (childDoc.isImmutable()) {
                wcDoc = session.getWorkingCopy(childDoc.getRef());
            } else {
                wcDoc = childDoc;
            }

            RelationNodeData node = saveRelationNodeData(String.valueOf(nodeId),
                    level, childUid, childDoc, wcDoc, stmt, predicateUri,
                    quantity, comment, isObjectWc, ordering, isSpecial);

            nodeId++;

            // Check if it has iconOnlyRelationDocs
            if (iconOnlyRelationDocs.containsKey(childUid)) {
                List<String> iconOnlyRelations = iconOnlyRelationDocs.get(
                        childUid);
                node.setIconOnlyRelations(iconOnlyRelations);
            }
            return node;
        }
        return null;
    }

    /**
     * This method is overridden in the classes that extend the service.
     *
     * @param id
     * @param level
     * @param docId
     * @param data
     * @param wcDoc
     * @param predicateUri
     * @param quantity
     * @param comment
     * @param isObjectWc
     * @param isSpecial
     * @return
     */
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, int quantity, String comment,
            boolean isObjectWc, int ordering, boolean isSpecial) {
        // Do nothing
        return null;
    }

}
