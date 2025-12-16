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

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class RelationNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(RelationNodeService.class);

    protected static final String RELATION_TYPE_CAD = "CadRelation";

    protected static final String RELATION_TYPE_BOM = "BomRelation";

    protected static final String RELATION_TYPE_CONTAINER = "ContainerRelation";

    protected static final String RELATION_TYPE_DOC = "DocRelation";

    protected static final boolean NODES_EXPANDED_BY_DEFAULT = false;

    protected CoreSession session;

    protected RelationManager relationManager = Framework.getLocalService(
            RelationManager.class);

    protected List<Resource> predicates;

    protected List<Resource> cadSpecialPredicates;

    protected List<String> directRelationsList;

    protected Map<String, List<String>> iconOnlyRelationDocs;

    Map<String, RelatedDocInfo> relatedDocInfoList;

    protected DocumentModel currentDoc;

    protected String relationType;

    protected String treeDirection;

    protected int nodeId;

    protected boolean specialInLevel0;

    protected boolean showUniqueVersionsPerDocument;

    protected boolean showObsoleteStateDocuments;

    public RelationNodeService(CoreSession session) {

        this.session = session;
        predicates = new ArrayList<>();
        cadSpecialPredicates = new ArrayList<>();
        directRelationsList = new ArrayList<>();
        iconOnlyRelationDocs = new HashMap<>();
        treeDirection = "";
        relationType = "";
        nodeId = 0;
        showUniqueVersionsPerDocument = true;
        showObsoleteStateDocuments = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.aritu.eloraplm.treetable.NodeManager#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object parentObject)
            throws EloraException, DocumentUnreadableException {
        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        int level = 0;

        // TODO proxy bat bada fallau,... txekeuak
        DocumentModel parentDoc = (DocumentModel) parentObject;
        currentDoc = getTreeRootDocument(parentDoc);

        RelationNodeData nodeData = saveRelationNodeData(String.valueOf(nodeId),
                level, currentDoc.getId(), currentDoc, null, null, null, null,
                null, null, null, null, null, null, null, null, false, false);

        nodeId++;

        // Get icon only documents (are only shown the ones refering the root
        // document)
        if (relationType.equals(RELATION_TYPE_CAD) && treeDirection.equals(
                EloraRelationConstants.DIRECTION_COMPOSITION)) {
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

        if (!nodeData.getIsDirect()) {
            List<RelationNodeData> childNodeList;
            level++;
            // Check if it is special in level 0 (now 1)
            if (level == 1
                    && treeDirection.equals(
                            EloraRelationConstants.DIRECTION_COMPOSITION)
                    && relationType.equals(RELATION_TYPE_CAD)
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
                    rootNode = processTreeNode(rootNode, node, childNode,
                            level);
                }
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
        Map<String, List<String>> iconOnlyRelationDocs = new HashMap<>();

        for (String predicateUri : RelationsConfig.cadIconOnlyRelationsList) {
            Resource predicateResource = new ResourceImpl(predicateUri);

            DocumentModelList relatedDocs;
            relatedDocs = RelationHelper.getObjectDocuments(currentDoc,
                    predicateResource);

            if (!relatedDocs.isEmpty()) {
                for (DocumentModel doc : relatedDocs) {
                    String docId = doc.getId();
                    if (iconOnlyRelationDocs.containsKey(docId)) {
                        iconOnlyRelationDocs.get(docId).add(predicateUri);
                    } else {
                        List<String> relations = new ArrayList<>();
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
     * @throws DocumentUnreadableException
     */
    private List<RelationNodeData> getSpecialInLevel0ChildrenNodeData(
            RelationNodeData nodeData)
            throws EloraException, DocumentUnreadableException {

        relatedDocInfoList = new LinkedHashMap<String, RelatedDocInfo>();

        List<RelationNodeData> childNodeList = new ArrayList<>();
        DocumentModel parentDoc = nodeData.getData();

        String predicateUri = EloraRelationConstants.CAD_DRAWING_OF;
        Resource predicateResource = new ResourceImpl(predicateUri);
        boolean isSpecial = false;
        boolean inverse = false;
        List<Statement> relatedStmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, parentDoc,
                predicateResource);

        if (!relatedStmts.isEmpty()) {
            processStatementsAndPopulateLists(relatedStmts, false);
            childNodeList.addAll(treatRelatedDocs(relatedDocInfoList,
                    parentDoc.isVersion(), inverse, 1, isSpecial));
        }

        return childNodeList;
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

        if (relationType.equals(RELATION_TYPE_CAD)) {
            childNodeList.addAll(getSpecialChildrenNodeData(level, parentDoc));
        }

        childNodeList.addAll(getNormalChildrenNodeData(level, parentDoc));

        return childNodeList;
    }

    private List<RelationNodeData> getSpecialChildrenNodeData(int level,
            DocumentModel parentDoc)
            throws EloraException, DocumentUnreadableException {

        List<RelationNodeData> specialChildren = new ArrayList<>();

        switch (treeDirection) {
        case EloraRelationConstants.DIRECTION_COMPOSITION:

            // Do not show special relations of root in composition
            if (level > 1) {
                specialChildren = getChildNodes(parentDoc, level, true, true);
            }
            break;

        case EloraRelationConstants.DIRECTION_WHERE_USED:
            specialChildren = getChildNodes(parentDoc, level, false, true);
            break;

        default:
            throw new EloraException("Unsupported tree direction provided.");
        }

        return specialChildren;

    }

    private List<RelationNodeData> getNormalChildrenNodeData(int level,
            DocumentModel parentDoc)
            throws EloraException, DocumentUnreadableException {

        List<RelationNodeData> normalChildren = new ArrayList<>();

        switch (treeDirection) {
        case EloraRelationConstants.DIRECTION_COMPOSITION:
            normalChildren = getChildNodes(parentDoc, level, false, false);
            break;

        case EloraRelationConstants.DIRECTION_WHERE_USED:
            normalChildren = getChildNodes(parentDoc, level, true, false);
            break;

        default:
            throw new EloraException("Unsupported tree direction provided.");
        }

        return normalChildren;

    }

    private List<RelationNodeData> getChildNodes(DocumentModel parentDoc,
            int level, boolean inverse, boolean isSpecial)
            throws EloraException, DocumentUnreadableException {

        // Empty list
        relatedDocInfoList = new LinkedHashMap<String, RelatedDocInfo>();

        List<RelationNodeData> childNodes = new ArrayList<>();

        List<Resource> predicatesList = isSpecial ? cadSpecialPredicates
                : predicates;

        List<Statement> relatedStmts;
        if (inverse) {
            relatedStmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                    parentDoc, predicatesList);
        } else {
            relatedStmts = EloraRelationHelper.getStatements(parentDoc,
                    predicatesList);
        }

        if (!relatedStmts.isEmpty()) {
            processStatementsAndPopulateLists(relatedStmts, inverse);
            childNodes = treatRelatedDocs(relatedDocInfoList,
                    parentDoc.isVersion(), inverse, level, isSpecial);
        }

        return childNodes;
    }

    private void processStatementsAndPopulateLists(List<Statement> stmts,
            boolean inverse)
            throws EloraException, DocumentUnreadableException {
        String logInitMsg = "[processStatementsAndPopulateLists] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        for (Statement stmt : stmts) {
            DocumentModel relatedDoc = null;
            if (inverse) {
                log.trace(logInitMsg
                        + "Trying to get doc from subject using statement |"
                        + stmt.toString() + "|");
                relatedDoc = RelationHelper.getDocumentModel(stmt.getSubject(),
                        session);
            } else {
                log.trace(logInitMsg
                        + "Trying to get doc from object using statement |"
                        + stmt.toString() + "|");
                relatedDoc = RelationHelper.getDocumentModel(stmt.getObject(),
                        session);
            }

            if (relatedDoc == null) {
                log.trace(logInitMsg
                        + "Throw DocumentUnreadableException since relatedDoc is null. stmt = |"
                        + stmt.toString() + "|");
                throw new DocumentUnreadableException(
                        "Error getting document from statement |"
                                + stmt.toString() + "|");
            }

            log.trace(logInitMsg + "Related doc |" + relatedDoc.getId()
                    + "| retrieved");

            // TODO: Poner control de permisos.
            // We consider that when relatedDoc is null user doesn't have
            // any permission on the document
            if (showObsoleteStateDocuments
                    || !LifecyclesConfig.obsoleteStatesList.contains(
                            relatedDoc.getCurrentLifeCycleState())) {
                // TODO: se puede mirar si todo esto se podría meter en la
                // estructura de una clase y luego no tener que andar
                // consultando datos en el bucle
                String versionSeriesId = session.getVersionSeriesId(
                        relatedDoc.getRef());

                String predicateUri = stmt.getPredicate().getUri();
                String mapKey = versionSeriesId + "___" + predicateUri;
                RelatedDocInfo rdi;
                if (relatedDocInfoList.containsKey(mapKey)) {
                    rdi = relatedDocInfoList.get(mapKey);
                } else {
                    rdi = new RelatedDocInfo(versionSeriesId, predicateUri);
                }
                rdi.addDocVersionAndStatement(relatedDoc, stmt);
                relatedDocInfoList.put(mapKey, rdi);

                log.trace("Related doc added to list");
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<RelationNodeData> treatRelatedDocs(
            Map<String, RelatedDocInfo> relatedDocInfoList,
            boolean parentIsVersion, boolean inverse, int level,
            boolean isSpecial) throws EloraException {

        List<RelationNodeData> childNodeList = new ArrayList<>();
        if (relatedDocInfoList != null && !relatedDocInfoList.isEmpty()) {
            for (Map.Entry<String, RelatedDocInfo> entry : relatedDocInfoList.entrySet()) {
                RelatedDocInfo rdi = entry.getValue();
                String versionSeriesId = rdi.getVersionSeriesId();
                List<DocumentModel> docs = rdi.getDocVersions();

                RelationNodeData node = null;

                if (showUniqueVersionsPerDocument && docs.size() > 1) {

                    DocumentModel doc = rdi.getUniqueVersion();
                    Statement stmt = rdi.getStatementForDocVersion(doc.getId());
                    node = createNodeData(doc, versionSeriesId, stmt, isSpecial,
                            inverse, parentIsVersion, level);
                    if (node != null) {
                        childNodeList.add(node);
                    }
                } else {
                    for (DocumentModel doc : docs) {
                        Statement stmt = rdi.getStatementForDocVersion(
                                doc.getId());
                        node = createNodeData(doc, versionSeriesId, stmt,
                                isSpecial, inverse, parentIsVersion, level);
                        if (node != null) {
                            childNodeList.add(node);
                        }
                    }
                }
            }
        }
        return childNodeList;
    }

    protected DocumentModel getTreeRootDocument(DocumentModel doc)
            throws EloraException {
        if (doc.isProxy()) {
            throw new EloraException("The root document is a proxy.");
        }
        DocumentModel rootDoc = null;
        // If the root document is a working copy, we get the tree of its base
        // document
        if (doc.isVersion() || doc.isCheckedOut()) {
            rootDoc = doc;
        } else {
            rootDoc = EloraDocumentHelper.getLatestVersion(doc);
            if (rootDoc == null) {
                rootDoc = doc;
            }
        }
        return rootDoc;
    }

    protected RelationNodeData createNodeData(DocumentModel childDoc,
            String versionSeriesId, Statement stmt, boolean isSpecial,
            boolean inverse, boolean parentIsVersion, int level)
            throws EloraException {

        String logInitMsg = "[createNodeData] ["
                + session.getPrincipal().getName() + "] ";

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
            if (level == 2 && specialInLevel0 && isSpecial) {
                if (versionSeriesId.equals(currentDoc.getVersionSeriesId())) {
                    return null;
                }
            }

            String childUid = childDoc.getId();
            RelationNodeData node = saveNodeData(childDoc, stmt, level,
                    childUid);

            // Check if it is in iconOnlyRelationDocs
            if (relationType.equals(RELATION_TYPE_CAD)
                    && iconOnlyRelationDocs.containsKey(childUid)) {
                List<String> iconOnlyRelations = iconOnlyRelationDocs.get(
                        childUid);
                node.setIconOnlyRelations(iconOnlyRelations);
            }
            return node;
        }
        return null;
    }

    private RelationNodeData saveNodeData(DocumentModel childDoc,
            Statement stmt, int level, String childUid) {

        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
        String predicateUri = stmtInfo.getPredicate().getUri();
        String quantity = stmtInfo.getQuantity();
        String comment = stmtInfo.getComment();
        // Some values should not be taken into account if the node is level 2+,
        // of filters won't work correctly
        Integer directorOrdering = level == 1 ? stmtInfo.getDirectorOrdering()
                : null;
        Integer viewerOrdering = level == 1 ? stmtInfo.getViewerOrdering()
                : null;
        Integer inverseViewerOrdering = level == 1
                ? stmtInfo.getInverseViewerOrdering()
                : null;
        Boolean isManual = level == 1 ? stmtInfo.getIsManual() : null;
        Boolean isExcludedFromMapping = level == 1
                ? stmtInfo.getIsExcludedFromMapping()
                : null;
        // Ordering is an exception because in some cases we have to export the
        // value
        Integer ordering = stmtInfo.getOrdering();
        // ---
        String designators = stmtInfo.getDesignators();
        boolean isSpecial = RelationsConfig.cadSpecialRelationsList.contains(
                predicateUri);
        boolean isDirect = directRelationsList.contains(predicateUri);

        DocumentModel wcDoc = null;
        if (childDoc.isImmutable()) {
            wcDoc = session.getWorkingCopy(childDoc.getRef());
        } else {
            wcDoc = childDoc;
        }

        RelationNodeData node = saveRelationNodeData(String.valueOf(nodeId),
                level, childUid, childDoc, wcDoc, stmt, predicateUri, quantity,
                comment, ordering, directorOrdering, viewerOrdering,
                inverseViewerOrdering, isManual, designators,
                isExcludedFromMapping, isSpecial, isDirect);

        nodeId++;
        return node;
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
     * @param ordering
     * @param directorOrdering
     * @param viewerOrdering
     * @param inverseViewerOrdering
     * @param isManual
     * @param designator
     * @param isExcludedFromMapping
     * @param isSpecial
     * @param isDirect
     * @return
     */
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, String quantity,
            String comment, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual, String designators, Boolean isExcludedFromMapping,
            boolean isSpecial, boolean isDirect) {
        // Do nothing
        return null;
    }

}
