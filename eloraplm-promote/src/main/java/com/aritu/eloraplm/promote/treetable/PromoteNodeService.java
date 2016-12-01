/*
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
package com.aritu.eloraplm.promote.treetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.ViewerPdfUpdater;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.util.PromoteConstants;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class PromoteNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(PromoteNodeService.class);

    protected CoreSession session;

    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    protected PromoteNodeManager promoteNodeManager;

    protected Map<String, String> messages;

    protected String promoteTransition;

    protected String finalState;

    protected String relationOption;

    protected RelationManager relationManager = Framework.getLocalService(RelationManager.class);

    protected Map<String, List<String>> iconOnlyRelationDocs;

    protected int nodeId;

    protected boolean firstLoad;

    protected Graph graph;

    protected boolean topLevelOK;

    // TODO: Es posible que se pueda mejorar el rendimiento del árbol. Ahora se
    // vuelve a recorrer todo el árbol para ver si el root tiene que ser OK o
    // KO. Al cambiar una versión o clickar en un checkbox se vuelven a calcular
    // todos los results otra vez. Mirar si hay alguna forma de ahorrarnos toda
    // esa vuelta
    public PromoteNodeService(String promoteOption, String relationOption,
            CoreSession session,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            boolean firstLoad, Map<String, String> messages) {

        this.session = session;
        this.eloraDocumentRelationManager = eloraDocumentRelationManager;
        this.messages = messages;
        finalState = promoteOption;
        this.relationOption = relationOption;
        this.firstLoad = firstLoad;
        nodeId = 0;

        // TODO: Sacar la transicion de otra forma (mirar clases de nuxeo
        // LifeCycle...). Puede que haya mas de una transicion hacia el mismo
        // estado??
        if (finalState != null) {
            switch (finalState) {
            case EloraLifeCycleConstants.CAD_APPROVED:
                promoteTransition = EloraLifeCycleConstants.CAD_TRANS_APPROVE;
                break;
            }
        }
    }

    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {
        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO proxy bat bada fallau,... txekeuak
        DocumentModel doc = (DocumentModel) parentObject;

        promoteNodeManager = getPromoteNodeManager(doc);

        NodeDynamicInfo nodeInfo = new NodeDynamicInfo();
        // TODO: coger la version que se quiere hacer promote.
        DocumentModel parentDoc = session.getLastDocumentVersion(doc.getRef());
        nodeInfo.setEditableVersion(true);
        nodeInfo.setFinalState(finalState);
        nodeInfo.setIsEnforced(true);
        nodeInfo.setIsPropagated(true);
        nodeInfo.setResult(PromoteConstants.RESULT_OK);
        // nodeInfo.setResultMsg(resultMsg);

        DocumentModel wcDoc = session.getWorkingCopy(parentDoc.getRef());
        String wcVersion = wcDoc.getVersionLabel();

        // TODO: no calculo el alreadyPromoted del nodo que no se ve. Hay que
        // poner un control antes de poder lanzar el promote para que si esta
        // promocionado no deje ejecutarlo
        nodeInfo.setAlreadyPromoted(false);
        if (!firstLoad) {
            // First time finalState is set to null. We just want to load root
            // node (the one that is not visible)
            loadConfigurations();
            // alreadyPromoted =
            // promoteNodeManager.calculateAlreadyPromoted(parentDoc);
        }

        int level = 0;
        LinkedHashMap<String, String> versionMap = getVersionMap(parentDoc,
                null, false, level);
        PromoteNodeData nodeData = new PromoteNodeData(String.valueOf(nodeId),
                parentDoc, nodeInfo, level, parentDoc.getId(), null, 0, false,
                versionMap, wcVersion, false, false,
                nodeInfo.getAlreadyPromoted());
        nodeId++;

        // Get icon only documents
        iconOnlyRelationDocs = new HashMap<String, List<String>>();
        iconOnlyRelationDocs = promoteNodeManager.getIconOnlyRelationDocs(parentDoc);

        TreeNode root = processTree(null, null, nodeData, -1, level);

        log.trace(logInitMsg + "--- EXIT --- ");

        return root;
    }

    public void processPartialTreeNode(TreeNode rootNode, TreeNode partialNode)
            throws EloraException {

        // Get parent data
        // TODO: Empezar a coger cosas de nodeInfo cuando lo meta en
        // promoteNodeData
        TreeNode parentNode = partialNode.getParent();
        PromoteNodeData parentNodeData = (PromoteNodeData) parentNode.getData();
        String parentResult = parentNodeData.getResult();
        boolean parentIsPropagated = parentNodeData.getIsPropagated();
        boolean parentAlreadyPromoted = parentNodeData.getAlreadyPromoted();

        PromoteNodeData nodeData = (PromoteNodeData) partialNode.getData();
        int level = nodeData.getLevel();
        Statement stmt = nodeData.getStmt();

        boolean isSpecial = nodeData.getIsSpecial();
        NodeDynamicInfo nodeInfo = calculateConfigNodeInfo(stmt,
                parentIsPropagated, parentAlreadyPromoted, parentResult, level);

        // User selected document version
        DocumentRef docRef = new IdRef(nodeData.getDocId());
        DocumentModel doc = session.getDocument(docRef);

        // Recalculate things that could change. Configuration changes are not
        // considered, we load nodes the same way as tree was loaded the first
        // time
        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        String wcVersion = wcDoc.getVersionLabel();

        LinkedHashMap<String, String> versionMap = getVersionMap(wcDoc, stmt,
                isSpecial, level);

        // Parent final state!
        String parentState = getParentState(doc, parentAlreadyPromoted,
                parentResult, parentIsPropagated);

        promoteNodeManager.processNodeInfo(doc, nodeInfo, level, isSpecial,
                parentState, stmt, promoteTransition);

        // Remove old node
        int nodeIndex = parentNode.getChildren().indexOf(partialNode);
        parentNode.getChildren().remove(partialNode);

        nodeData.setDocId(doc.getId());
        nodeData.setData(doc);
        nodeData.setVersionMap(versionMap);
        nodeData.setWcVersion(wcVersion);
        // TODO: Poner un setNodeInfo!!!!!
        nodeData.setResult(nodeInfo.getResult());
        nodeData.setResultMsg(nodeInfo.getResultMsg());
        nodeData.setFinalState(nodeInfo.getFinalState());

        nodeData.setNodeInfo(nodeInfo);

        if (level != 1) {
            nodeData.setLevel(level);
            nodeData.setStmt(stmt);
            nodeData.setAlreadyPromoted(nodeInfo.getAlreadyPromoted());
            // TODO: Poner un setNodeInfo!!!!!
            nodeData.setSwitchableVersion(nodeInfo.getSwitchableVersion());

            nodeData.setIsPropagated(nodeInfo.getIsPropagated());
            nodeData.setIsEnforced(nodeInfo.getIsEnforced());
        }
        processTree(rootNode, parentNode, nodeData, nodeIndex, level);
    }

    public void processPartialTreeNodePropagation(TreeNode rootNode,
            TreeNode partialNode) throws EloraException {
        PromoteNodeData nodeData = (PromoteNodeData) partialNode.getData();
        processPropagation(partialNode, nodeData, partialNode,
                PromoteConstants.FIRST_NODE, nodeData.getIsPropagated());

        processAllResults(rootNode);

    }

    private void processPropagation(TreeNode treeNode,
            PromoteNodeData nodeData, TreeNode previousTreeNode,
            String previousNodePosition, boolean check) throws EloraException {
        for (EloraConfigRow configRow : getStatementPropagationConfig(nodeData.getStmt())) {
            boolean propagate = (long) configRow.getProperty(PromoteConstants.PROPAGATE) == 1;
            boolean enforce = (long) configRow.getProperty(PromoteConstants.ENFORCE) == 1;
            long direction = (long) configRow.getProperty(PromoteConstants.DIRECTION);

            if (propagate && direction == PromoteConstants.DIRECTION_ASCENDING) {
                if (nodeData.getLevel() > 2
                        && (previousNodePosition.equals(PromoteConstants.FIRST_NODE) || previousNodePosition.equals(PromoteConstants.PREVIOUS_NODE_POSITION_BELOW))) {
                    TreeNode parent = treeNode.getParent();
                    nodeData = (PromoteNodeData) parent.getData();
                    nodeData.setIsPropagated(check);
                    nodeData.setIsEnforced(enforce);
                    processPropagation(parent, nodeData, treeNode,
                            PromoteConstants.PREVIOUS_NODE_POSITION_BELOW,
                            check);
                }
            } else {
                if (propagate
                        && previousNodePosition.equals(PromoteConstants.PREVIOUS_NODE_POSITION_ABOVE)) {
                    nodeData.setIsPropagated(check);
                    nodeData.setIsEnforced(enforce);
                }

                if (propagate
                        || previousNodePosition.equals(PromoteConstants.FIRST_NODE)
                        || previousNodePosition.equals(PromoteConstants.PREVIOUS_NODE_POSITION_BELOW)) {
                    List<TreeNode> children = treeNode.getChildren();
                    for (TreeNode child : children) {
                        if (previousNodePosition.equals(PromoteConstants.FIRST_NODE)
                                || !child.equals(previousTreeNode)) {
                            processPropagation(
                                    child,
                                    (PromoteNodeData) child.getData(),
                                    treeNode,
                                    PromoteConstants.PREVIOUS_NODE_POSITION_ABOVE,
                                    check);
                        }
                    }
                }
            }
        }
    }

    private Collection<EloraConfigRow> getStatementPropagationConfig(
            Statement stmt) throws EloraException {
        // TODO: Puede cargarse al principio toda la configuracion de
        // relationPropagationConfig y luego aquí con un bucle rellenar un map
        // con los valores que coinciden con el uri. No se cual da mejor
        // rendimiento
        EloraConfigTable relationPropagationConfig = EloraConfigHelper.getApprovePropagationConfig(stmt.getPredicate().getUri());
        return relationPropagationConfig.getValues();
    }

    public void runPromote(TreeNode rootNode) throws EloraException {
        String logInitMsg = "[runPromote] [" + session.getPrincipal().getName()
                + "] ";
        log.info(logInitMsg + "--- ENTER --- ");

        // Get first shown node in tree
        TreeNode topNode = rootNode.getChildren().get(0);
        processTreeResult(topNode, 1);
        if (topLevelOK) {
            processPromote(topNode);
        } else {
            throw new EloraException(
                    messages.get("eloraplm.message.error.ko.nodes.in.tree"));
        }
    }

    /**
     * @param parentNode
     * @param nodeData
     * @return
     * @throws EloraException
     */
    private TreeNode processTree(TreeNode rootNode, TreeNode parentNode,
            PromoteNodeData nodeData, int nodeIndex, int level)
            throws EloraException {

        String logInitMsg = "[processTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            topLevelOK = true;
            if (level == 0) {
                level++;
                rootNode = new DefaultTreeNode(nodeData, parentNode);
                if (firstLoad) {
                    return rootNode;
                }

                promoteNodeManager.processNodeInfo(nodeData.getData(),
                        nodeData.getNodeInfo(), nodeData.getLevel(),
                        nodeData.getIsSpecial(),
                        nodeData.getData().getCurrentLifeCycleState(),
                        nodeData.getStmt(), promoteTransition);

                // Map<String, Object> resultMap = processResult(
                // nodeData.getData(), true, true,
                // nodeData.getIsSpecial(), finalState,
                // nodeData.getData().getCurrentLifeCycleState(),
                // nodeData.getAlreadyPromoted(), nodeData.getStmt());

                nodeData = new PromoteNodeData(String.valueOf(nodeId),
                        nodeData.getData(), nodeData.getNodeInfo(), level,
                        nodeData.getDocId(), null, 0, false,
                        nodeData.getVersionMap(), nodeData.getWcVersion(),
                        false, false, false);

                // nodeData = new PromoteNodeData(String.valueOf(nodeId), level,
                // nodeData.getDocId(), nodeData.getData(), null, 0,
                // false, nodeData.getVersionMap(),
                // nodeData.getWcVersion(), true, false, false,
                // finalState, true, nodeData.getAlreadyPromoted(),
                // nodeData.getEditableVersion(), true, resultMap.get(
                // "resultMsg").toString(),
                // resultMap.get("result").toString());

                nodeId++;
                processTreeNode(rootNode, nodeData, nodeIndex, level);
            } else {
                processTreeNode(parentNode, nodeData, nodeIndex, level);
            }

            processAllResults(rootNode);

            log.trace(logInitMsg + "--- EXIT --- ");
            // Return root TreeNode
            return rootNode;
        } catch (Exception e) {
            log.trace(logInitMsg + "Error with doc |" + nodeData.getDocId()
                    + "|");
            throw e;
        }
    }

    private void processTreeNode(TreeNode parentNode, PromoteNodeData nodeData,
            int nodeIndex, int level) throws EloraException {
        String logInitMsg = "[processTreeNode] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            TreeNode node;
            if (nodeIndex >= 0 && level > 0) {
                parentNode.getChildren().add(nodeIndex,
                        new DefaultTreeNode(nodeData));
                node = parentNode.getChildren().get(nodeIndex);
            } else {
                node = new DefaultTreeNode(nodeData, parentNode);
            }

            node.setExpanded(true);

            if (topLevelOK
                    && nodeData.getResult().equals(PromoteConstants.RESULT_KO)) {
                topLevelOK = false;
            }
            if (!nodeData.getIsDirect()) {
                // Get child nodes
                level++;
                List<PromoteNodeData> childNodeList = getChildrenNodeData(
                        nodeData, level);
                // For each child, processTreeNode
                if (!childNodeList.isEmpty()) {
                    for (PromoteNodeData childNode : childNodeList) {
                        processTreeNode(node, childNode, -1, level);
                    }
                }
            }
            log.trace(logInitMsg + "--- EXIT --- ");
        } catch (Exception e) {
            log.trace(logInitMsg + "Error with doc |" + nodeData.getDocId()
                    + "|");
            throw e;
        }
    }

    public List<PromoteNodeData> getChildrenNodeData(PromoteNodeData nodeData,
            int level) throws EloraException {

        List<PromoteNodeData> childList = new ArrayList<PromoteNodeData>();
        // Get direct CAD nodeData list
        childList = getChildNodeList(nodeData, level, true,
                promoteNodeManager.getDirectRelationsConfig());
        // Get hierarchical CAD nodeData list
        childList.addAll(getChildNodeList(nodeData, level, false,
                promoteNodeManager.getHierarchicalRelationsConfig()));
        return childList;
    }

    protected List<PromoteNodeData> getChildNodeList(PromoteNodeData nodeData,
            int level, boolean isDirect, EloraConfigTable relationsConfig)
            throws EloraException {
        String logInitMsg = "[getChildrenNodeList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc = nodeData.getData();
        List<PromoteNodeData> childNodeList = new ArrayList<PromoteNodeData>();
        QNameResource docResource = EloraDocumentHelper.getDocumentResource(
                relationManager, doc);
        String parentState = getParentState(doc, nodeData.getAlreadyPromoted(),
                nodeData.getResult(), nodeData.getIsPropagated());
        String parentType = doc.getType();

        // Get relations
        List<Statement> objects = new ArrayList<Statement>();
        List<Statement> subjects = new ArrayList<Statement>();
        List<Statement> specialObjects = new ArrayList<Statement>();
        for (EloraConfigRow relationConfig : relationsConfig.getValues()) {
            boolean isSpecial = ((long) relationConfig.getProperty("isSpecial") == 1);
            String predicateUri = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicateUri);
            if (isSpecial) {
                subjects.addAll(graph.getStatements(null, predicateResource,
                        docResource));
                // Get the specials in first level
                if (level == 2) {
                    specialObjects.addAll(graph.getStatements(docResource,
                            predicateResource, null));
                }
            } else {
                objects.addAll(graph.getStatements(docResource,
                        predicateResource, null));
            }
        }

        log.trace(logInitMsg + "Document |" + doc.getId()
                + "| Found relations:" + "\n" + objects.size()
                + " normal children" + "\n" + subjects.size()
                + " special children" + "\n" + specialObjects.size()
                + " special children in first level.");

        // Treat subjects (special children)
        if (!subjects.isEmpty()) {
            childNodeList.addAll(treatRelatedDocs(subjects, doc.isVersion(),
                    parentState, parentType, nodeData.getStmt(), level,
                    isDirect, true, nodeData.getIsPropagated(),
                    nodeData.getAlreadyPromoted(), nodeData.getResult()));
        }
        // Treat objects (normal children)
        if (!objects.isEmpty()) {
            childNodeList.addAll(treatRelatedDocs(objects, doc.isVersion(),
                    parentState, parentType, nodeData.getStmt(), level,
                    isDirect, false, nodeData.getIsPropagated(),
                    nodeData.getAlreadyPromoted(), nodeData.getResult()));
        }
        // Treat special objects (special children in first level)
        if (!specialObjects.isEmpty()) {
            childNodeList.addAll(treatRelatedDocs(specialObjects,
                    doc.isVersion(), parentState, parentType,
                    nodeData.getStmt(), level, false, false,
                    nodeData.getIsPropagated(), nodeData.getAlreadyPromoted(),
                    nodeData.getResult()));
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
    // TODO: Esta función es demasiado grande. Sería conveniente partirla
    private List<PromoteNodeData> treatRelatedDocs(
            List<Statement> relatedStmts, boolean parentIsVersion,
            String parentState, String parentType, Statement parentStmt,
            int level, boolean isDirect, boolean isSpecial,
            boolean parentIsPropagated, boolean parentAlreadyPromoted,
            String parentResult) throws EloraException {

        String logInitMsg = "[treatRelatedDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<PromoteNodeData> childNodeList = new ArrayList<PromoteNodeData>();
        if (relatedStmts != null && !relatedStmts.isEmpty()) {

            Map<String, List<String>> processedSpecialRels = new HashMap<String, List<String>>();

            for (Statement stmt : relatedStmts) {
                try {
                    if ((stmt.getObject() instanceof QNameResource)
                            && (stmt.getSubject() instanceof QNameResource)) {

                        DocumentModel childDoc = getChildDoc(stmt, parentStmt,
                                processedSpecialRels, isSpecial, level);

                        if (childDoc == null) {
                            continue;
                        }

                        // get info checking configuration
                        NodeDynamicInfo nodeInfo = calculateConfigNodeInfo(
                                stmt, parentIsPropagated,
                                parentAlreadyPromoted, parentResult, level);
                        // Update document depending on chosen option
                        childDoc = getUpdatedDoc(childDoc, stmt, parentResult,
                                isSpecial, parentAlreadyPromoted,
                                parentIsPropagated);
                        if (childDoc != null) {
                            log.trace(logInitMsg + "Processing doc ["
                                    + childDoc.getId() + "]");
                            // If the user creates a relation from WC to a
                            // specific archived version (instead of to another
                            // WC), we have to hide this relation when we
                            // display the tree of an AV document, or the user
                            // will see the same document twice (one AV and the
                            // WC). If parent element is an AV and child is WC,
                            // ignore it
                            if (parentIsVersion && !childDoc.isVersion()) {
                                continue;
                            }

                            // boolean alreadyPromoted =
                            // PromoteHelper.calculateAlreadyPromoted(
                            // childDoc, finalStateOrdering, lifecyc);
                            promoteNodeManager.processNodeInfo(childDoc,
                                    nodeInfo, level, isSpecial, parentState,
                                    stmt, promoteTransition);
                            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                                    stmt);
                            DocumentModel wcDoc = session.getWorkingCopy(childDoc.getRef());
                            String wcVersion = wcDoc.getVersionLabel();

                            Map<String, String> versionMap = getVersionMap(
                                    childDoc, stmt, isSpecial, level);

                            PromoteNodeData nodeData = new PromoteNodeData(
                                    String.valueOf(nodeId), childDoc, nodeInfo,
                                    level, childDoc.getId(), stmt,
                                    stmtInfo.getQuantity(),
                                    stmtInfo.getIsObjectWc(), versionMap,
                                    wcVersion, isDirect, isSpecial,
                                    nodeInfo.getAlreadyPromoted());

                            // Check if it has iconOnlyRelationDocs
                            if (iconOnlyRelationDocs.containsKey(childDoc.getId())) {
                                List<String> iconOnlyRelations = iconOnlyRelationDocs.get(childDoc.getId());
                                nodeData.setIconOnlyRelations(iconOnlyRelations);
                            }

                            childNodeList.add(nodeData);
                            nodeId++;
                        }
                    }
                } catch (Exception e) {
                    if (isSpecial) {
                        log.trace(logInitMsg
                                + "Error with doc special statement |"
                                + RelationHelper.getDocumentModel(
                                        stmt.getSubject(), session).getId()
                                + "|");
                    } else {
                        log.trace(logInitMsg
                                + "Error with doc statement |"
                                + RelationHelper.getDocumentModel(
                                        stmt.getObject(), session).getId()
                                + "|");
                    }
                    throw e;
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return childNodeList;
    }

    private DocumentModel getChildDoc(Statement stmt, Statement parentStmt,
            Map<String, List<String>> processedSpecialRels, boolean isSpecial,
            int level) {
        DocumentModel childDoc;
        if (isSpecial) {
            childDoc = EloraDocumentHelper.getDocumentModel(relationManager,
                    session, stmt.getSubject());
            String versionSeriesId = childDoc.getVersionSeriesId();

            if (level == 3) {
                // Do not repeat specials in 3rd level. If we
                // start from a drawing do not show the same
                // drawing again as it's children's child
                String parentVersionSeriesId = EloraDocumentHelper.getDocumentModel(
                        relationManager, session, parentStmt.getSubject()).getVersionSeriesId();
                if (parentVersionSeriesId.equals(versionSeriesId)) {
                    return null;
                }
            }

            String predicateUri = stmt.getPredicate().getUri();
            if (processedSpecialRels.containsKey(versionSeriesId)) {
                if (processedSpecialRels.get(versionSeriesId).contains(
                        predicateUri)) {
                    // Ignore if this special has been processed
                    // before to avoid painting it twice
                    return null;
                } else {
                    // It has multiple different special
                    // relations
                    // to the same document. If this is possible
                    // continue painting node
                    processedSpecialRels.get(versionSeriesId).add(predicateUri);
                }
            } else {
                List<String> predicateList = new ArrayList<String>();
                predicateList.add(predicateUri);
                processedSpecialRels.put(versionSeriesId, predicateList);
            }

        } else {
            childDoc = EloraDocumentHelper.getDocumentModel(relationManager,
                    session, stmt.getObject());
        }
        return childDoc;
    }

    private NodeDynamicInfo calculateConfigNodeInfo(Statement stmt,
            boolean parentIsPropagated, boolean parentAlreadyPromoted,
            String parentResult, int level) throws EloraException {

        NodeDynamicInfo nodeInfo = new NodeDynamicInfo();
        if (level == 1) {
            nodeInfo.setEditableVersion(true);
            nodeInfo.setIsPropagated(true);
            nodeInfo.setIsEnforced(true);
            nodeInfo.setSwitchableVersion(true);
        } else {
            nodeInfo.setEditableVersion(!parentAlreadyPromoted
                    && !parentResult.equals(PromoteConstants.RESULT_KO));
            if ((parentIsPropagated && (parentResult.equals(PromoteConstants.RESULT_OK) || parentResult.equals("")))
                    || (parentAlreadyPromoted)) {
                // If parent is propagated and OK or parent is in a
                // superior or equal state than final state
                EloraConfigRow propagationConfig = promoteNodeManager.getRelationDescendingPropagationConfig().getRow(
                        stmt.getPredicate().getUri());
                if (propagationConfig != null) {
                    nodeInfo.setIsPropagated((long) propagationConfig.getProperty("propagate") == 1);
                    nodeInfo.setIsEnforced((long) propagationConfig.getProperty("enforce") == 1);
                } else {
                    nodeInfo.setIsPropagated(false);
                    nodeInfo.setIsEnforced(false);
                }
                nodeInfo.setSwitchableVersion(true);
            } else {
                nodeInfo.setIsPropagated(false);
                nodeInfo.setIsEnforced(true);
                nodeInfo.setSwitchableVersion(false);
            }
        }
        return nodeInfo;
    }

    private DocumentModel getUpdatedDoc(DocumentModel childDoc, Statement stmt,
            String parentResult, boolean isSpecial,
            boolean parentAlreadyPromoted, boolean parentIsPropagated)
            throws EloraException {
        if ((!parentAlreadyPromoted && parentIsPropagated && (parentResult.equals(PromoteConstants.RESULT_OK) || parentResult.equals("")))
                || isSpecial) {
            // If parent is propagated and OK or parent is special load as
            // selected configuration
            // For special relations AS_STORED is not the
            // default childDoc. There could be multiple
            // versions of childDoc related and we must take
            // latest related released and in case it doesn't
            // exist latest version among all related childDocs
            childDoc = updateDoc(childDoc, relationOption, stmt, isSpecial);
        }
        return childDoc;
    }

    // TODO: Hay que poner esto para que getLatestReleasedVersion funcione como
    // en RelationNodeService.java
    private DocumentModel updateDoc(DocumentModel childDoc, String relationOpt,
            Statement stmt, boolean isSpecial) throws EloraException {
        if (isSpecial) {
            // Get last related released version. There could be multiple
            // versions of the special document pointing to the same doc
            // version. We just want to show the latest one
            childDoc = EloraRelationHelper.getLatestRelatedReleasedVersion(
                    childDoc, stmt, session);
        } else {
            switch (relationOpt) {
            case PromoteConstants.LATEST_RELEASED:
                // Get latest released doc. If it does not exist
                // returns latest version. For us, latest version will be
                // working copy base version if it is not checked out. If it is
                // checked out we return latest version or if it exists a
                // released version in this major return the released one
                childDoc = EloraDocumentHelper.getLatestReleasedVersion(
                        childDoc, session);
                break;
            case PromoteConstants.LATEST_VERSION:
                // Get the released version of latest major or if it doesn't
                // exist return latest version
                childDoc = EloraDocumentHelper.getLatestVersion(childDoc,
                        session);
                break;
            }
        }
        return childDoc;
    }

    private void processAllResults(TreeNode rootNode) throws EloraException {
        topLevelOK = true;
        TreeNode firstNode = rootNode.getChildren().get(0);
        PromoteNodeData firstNodeData = (PromoteNodeData) firstNode.getData();
        processTreeResult(firstNode, 1);
        if (topLevelOK) {
            firstNodeData.setResult(PromoteConstants.RESULT_OK);
        } else {
            firstNodeData.setResult(PromoteConstants.RESULT_KO);
        }
    }

    private void processTreeResult(TreeNode node, int level)
            throws EloraException {

        PromoteNodeData nodeData = (PromoteNodeData) node.getData();
        String parentState = null;
        if (level != 1) {
            PromoteNodeData parentNodeData = (PromoteNodeData) node.getParent().getData();
            parentState = parentNodeData.getFinalState();
        } else {
            parentState = finalState;
        }
        boolean isSpecial = nodeData.getIsSpecial();
        Statement stmt = nodeData.getStmt();

        promoteNodeManager.processNodeInfo(nodeData.getData(),
                nodeData.getNodeInfo(), level, isSpecial, parentState, stmt,
                promoteTransition);

        // TODO: Todos estos set no son necesarios si de los templates accedo a
        // nodeInfo!!
        if (level != 1) {
            nodeData.setFinalState(nodeData.getNodeInfo().getFinalState());
            nodeData.setIsPropagated(nodeData.getNodeInfo().getIsPropagated());
            nodeData.setIsEnforced(nodeData.getNodeInfo().getIsEnforced());
        }
        nodeData.setResult(nodeData.getNodeInfo().getResult());
        nodeData.setResultMsg(nodeData.getNodeInfo().getResultMsg());
        if (topLevelOK
                && nodeData.getResult().equals(PromoteConstants.RESULT_KO)) {
            topLevelOK = false;
        }
        for (TreeNode child : node.getChildren()) {
            processTreeResult(child, -1);
        }
    }

    private void processPromote(TreeNode node) throws EloraException {
        // Go to the deepest nodes
        for (TreeNode child : node.getChildren()) {
            processPromote(child);
        }
        PromoteNodeData nodeData = (PromoteNodeData) node.getData();
        boolean alreadyPromoted = nodeData.getNodeInfo().getAlreadyPromoted();
        if (!alreadyPromoted) {
            lockDocument(nodeData.getData());
            doPromote(nodeData);
            unlockDocument(nodeData.getData());
        } else {
            // TODO: Habrá casos en los que no se tendrá que cambiar la
            // estructura nunca. Por ejemplo, en los que el parent ya
            // está promoted. Hay que controlar para ser mas eficientes
            rebuildRelations(nodeData.getData(), nodeData);
        }
    }

    private void doPromote(PromoteNodeData nodeData) throws EloraException {
        String logInitMsg = "[doPromote] [" + session.getPrincipal().getName()
                + "] ";
        DocumentModel doc = nodeData.getData();

        doc.followTransition(promoteTransition);
        rebuildRelations(doc, nodeData);

        VersionModel version = new VersionModelImpl();
        version.setId(doc.getId());

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        wcDoc = EloraDocumentHelper.restoreToVersion(wcDoc, version,
                eloraDocumentRelationManager, session);
        updateViewer(wcDoc);
        updateViewer(doc);

        // TODO: Hemos decidido no actualizar las relaciones de todos los
        // subjects que apuntan a cualquier version de este major de este
        // documento(todos
        // los statement que tienen como object cualquier version dentro del
        // major del documento). Por ahora, no queremos cambiar cosas de
        // otros documentos sin que otro usuario que este utilizando ese
        // documento se de cuenta

        log.trace(logInitMsg + "Document promoted: |" + doc.getId() + "|");
    }

    private void rebuildRelations(DocumentModel doc, PromoteNodeData nodeData) {
        // For special relations we don't change structure because we
        // only paint related docs.
        if (!nodeData.getIsSpecial()) {
            // Check if original relation with parent doc has changed (if user
            // selects a different version)
            Statement stmt = nodeData.getStmt();
            if (stmt != null) {
                // root has stmt = null so you don't have to check relation with
                // parents
                DocumentModel asStoredDoc = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);
                if (!asStoredDoc.getId().equals(doc.getId())) {
                    // If document version has changed we create a new relation
                    // and remove previous one
                    EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(
                            stmt);
                    DocumentModel subject = RelationHelper.getDocumentModel(
                            stmt.getSubject(), session);
                    // Remove previous relation
                    RelationHelper.removeRelation(subject, stmt.getPredicate(),
                            asStoredDoc);

                    // TODO: Se está cambiando las relaciones del padre desde el
                    // hijo. No se hace ningún control sobre el padre y no se si
                    // esto está bien... Por ahora no vemos ningún caso en el
                    // que nos rompa algo
                    eloraDocumentRelationManager.addRelation(session, subject,
                            doc, stmt.getPredicate().getUri(),
                            eloraStmtInfo.getComment(),
                            eloraStmtInfo.getQuantity(),
                            eloraStmtInfo.getIsObjectWc(), 0);
                }
            }
        }
    }

    private void updateViewer(DocumentModel doc) throws EloraException {
        try {
            // Update viewer after promote
            EloraDocumentHelper.disableVersioningDocument(doc);
            Blob viewerBlob = ViewerPdfUpdater.createViewer(doc);
            if (viewerBlob != null) {
                EloraDocumentHelper.addViewerBlob(doc, viewerBlob);
            }
            session.saveDocument(doc);
        } catch (Exception e) {
            throw new EloraException(e.getMessage());
        }
    }

    /**
     * @param doc
     * @param stmt must have a value when isSpecial == true
     * @param isSpecial
     * @return a list of versions that can participate in promote operation
     * @throws EloraException
     */
    private LinkedHashMap<String, String> getVersionMap(DocumentModel doc,
            Statement stmt, boolean isSpecial, int level) throws EloraException {

        if (!doc.isVersion()) {
            // In versionMap there should never be a wc uid
            doc = EloraDocumentHelper.getLatestVersion(doc, session);
        }

        LinkedHashMap<String, String> versionMap = new LinkedHashMap<String, String>();
        if (level <= 1) {
            // Get only documents from major version
            // TODO: Si major version esta released no tendria que sacar nada.
            // Ahora sigue sacando todos
            DocumentModelList majorDocList = EloraDocumentHelper.getMajorVersionDocList(
                    doc.getRef(), session);
            for (DocumentModel majorDoc : majorDocList) {
                String versionRealUid = majorDoc.getId();
                String versionLabel = majorDoc.getVersionLabel();
                versionMap.put(versionRealUid, versionLabel);
            }
        } else {
            DocumentModelList promoteDocList = EloraDocumentHelper.getPromotableDocList(
                    doc.getRef(), stmt, isSpecial, session);
            for (DocumentModel promoteDoc : promoteDocList) {
                String versionRealUid = promoteDoc.getId();
                String versionLabel = promoteDoc.getVersionLabel();
                versionMap.put(versionRealUid, versionLabel);
            }
            // TODO: En la opción asStored puede que un conjunto tenga una pieza
            // en
            // una versión que no se puede promocionar. Aún así, lo mostramos
            // para
            // que se vea cuál es la estructura real. Esto dará un KO. Lo
            // añadimos
            // al final, no se ordena en la lista ya que puede afectar al
            // rendimiento andar ordenando. Mirar si hay otra forma más
            // eficiente de
            // sacar ordenado la lista de versiones
            if (!versionMap.containsKey(doc.getId())) {
                versionMap.put(doc.getId(), doc.getVersionLabel());
            }
        }
        return versionMap;
    }

    protected void lockDocument(DocumentModel doc) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        boolean isLockable = EloraDocumentHelper.getIsLockable(wcDoc);
        if (isLockable) {
            NuxeoPrincipal user = (NuxeoPrincipal) session.getPrincipal();
            if (!wcDoc.isLocked()) {
                try {
                    session.setLock(wcDoc.getRef());
                } catch (DocumentSecurityException e) {
                    throw new EloraException(e.getMessage());
                }
            } else if (!user.getName().equals(wcDoc.getLockInfo().getOwner())
                    && !user.isAdministrator()) {
                // Locked by someone else
                throw new EloraException("eloraplm.message.error.locked");
            }
        } else {
            throw new EloraException("eloraplm.message.error.not.lockable");
        }
    }

    protected void unlockDocument(DocumentModel doc) {
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        // boolean isUnlockable = true;
        // If document is checked out it is not possible to unlock
        // if (wcDoc.isCheckedOut()) {
        // isUnlockable = false;
        // }
        session.removeLock(wcDoc.getRef());
    }

    protected String getParentState(DocumentModel doc,
            boolean parentAlreadyPromoted, String parentResult,
            boolean isPropagated) {
        String parentState;
        if (parentAlreadyPromoted
                || parentResult.equals(PromoteConstants.RESULT_KO)
                || !isPropagated) {
            parentState = doc.getCurrentLifeCycleState();
        } else {
            parentState = finalState;
        }
        return parentState;
    }

    private PromoteNodeManager getPromoteNodeManager(DocumentModel doc)
            throws EloraException {
        if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            return new PromoteCadNodeService(finalState, messages, session);
        } else if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            return new PromoteBomNodeService(finalState, messages, session);
        } else {
            return null;
        }
    }

    protected void loadConfigurations() throws EloraException {
        String logInitMsg = "[loadConfigurations] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        graph = relationManager.getGraphByName(EloraRelationConstants.ELORA_GRAPH_NAME);
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
