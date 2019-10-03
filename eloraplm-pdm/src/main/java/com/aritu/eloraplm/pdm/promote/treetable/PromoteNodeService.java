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
package com.aritu.eloraplm.pdm.promote.treetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.promote.checker.PromoteCheckerManager;
import com.aritu.eloraplm.pdm.promote.constants.PromoteConstants;
import com.aritu.eloraplm.pdm.promote.executer.PromoteExecuterManager;
import com.aritu.eloraplm.pdm.promote.util.PromoteHelper;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class PromoteNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(PromoteNodeService.class);

    protected CoreSession session;

    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    protected PromoteCheckerManager promoteCheckerManager;

    protected PromoteExecuterManager promoteExecuterManager;

    protected Map<String, String> messages;

    protected String promoteTransition;

    protected String finalState;

    protected String relationOption;

    protected RelationManager relationManager = Framework.getLocalService(
            RelationManager.class);

    protected Map<String, List<String>> iconOnlyRelationDocs;

    protected int nodeId;

    protected boolean firstLoad;

    protected Graph graph;

    // protected boolean topLevelOK;

    protected boolean rootIsSpecial;

    // protected EloraConfigTable relationDescendingPropagationConfig;

    // TODO: Es posible que se pueda mejorar el rendimiento del árbol. Ahora se
    // vuelve a recorrer todo el árbol para ver si el root tiene que ser OK o
    // KO. Al cambiar una versión o clickar en un checkbox se vuelven a calcular
    // todos los results otra vez. Mirar si hay alguna forma de ahorrarnos toda
    // esa vuelta
    public PromoteNodeService(String finalState, String promoteTransition,
            String relationOption, CoreSession session,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            PromoteCheckerManager promoteCheckerManager,
            PromoteExecuterManager promoteExecuterManager, boolean firstLoad,
            boolean rootIsSpecial, Map<String, String> messages) {

        this.session = session;
        this.eloraDocumentRelationManager = eloraDocumentRelationManager;
        this.messages = messages;
        this.finalState = finalState;
        this.relationOption = relationOption;
        this.firstLoad = firstLoad;
        nodeId = 0;
        this.rootIsSpecial = rootIsSpecial;
        this.promoteTransition = promoteTransition;
        this.promoteCheckerManager = promoteCheckerManager;
        this.promoteExecuterManager = promoteExecuterManager;
    }

    public PromoteNodeService() {
        session = null;
        eloraDocumentRelationManager = null;
        messages = null;
        finalState = null;
        relationOption = null;
        firstLoad = true;
        nodeId = 0;
        rootIsSpecial = false;
        promoteTransition = null;
        promoteCheckerManager = null;
        promoteExecuterManager = null;
    }

    public TreeNode getEmptyRoot() {
        return createEmptyRoot();
    }

    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {
        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        if (firstLoad) {
            return createEmptyRoot();
        }

        // TODO proxy bat bada fallau,... txekeuak
        DocumentModel doc = (DocumentModel) parentObject;
        NodeDynamicInfo nodeInfo = createInitialNodeInfo();

        // DocumentModel parentDoc =
        // session.getLastDocumentVersion(doc.getRef());
        DocumentModel parentDoc = null;

        if (!doc.isImmutable()) {
            parentDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (parentDoc == null) {
                throw new EloraException("Document |" + doc.getId()
                        + "| has no base version. Probably because it has no AVs.");
            }
        } else {
            parentDoc = doc;
        }

        DocumentModel wcDoc = session.getWorkingCopy(parentDoc.getRef());
        String wcVersion = wcDoc.getVersionLabel();

        if (!firstLoad) {
            promoteCheckerManager.resetValues();
            // loadConfigurations();
        }

        int level = 0;

        // TODO: Poner configurable que se vean o no los desplegables de las
        // versiones
        // LinkedHashMap<String, String> versionMap = getVersionMap(parentDoc,
        // null, false, level);
        PromoteNodeData nodeData = new PromoteNodeData(String.valueOf(nodeId),
                parentDoc, nodeInfo, level, parentDoc.getId(), null, null, null,
                wcVersion, false, false, nodeInfo.getAlreadyPromoted());
        nodeId++;

        iconOnlyRelationDocs = promoteCheckerManager.getIconOnlyRelationDocs(
                parentDoc);

        TreeNode root = processTree(null, null, nodeData, -1, level);
        log.trace(logInitMsg + "--- EXIT --- ");
        return root;
    }

    private TreeNode createEmptyRoot() {
        NodeDynamicInfo nodeInfo = createInitialNodeInfo();
        PromoteNodeData nodeData = new PromoteNodeData(String.valueOf(nodeId),
                null, nodeInfo, 0, null, null, null, null, null, false, false,
                false);

        return new DefaultTreeNode(nodeData, null);
    }

    private NodeDynamicInfo createInitialNodeInfo() {
        NodeDynamicInfo nodeInfo = new NodeDynamicInfo();
        nodeInfo.setEditableVersion(true);
        nodeInfo.setFinalState(finalState);
        nodeInfo.setIsEnforced(true);
        nodeInfo.setIsPropagated(true);
        nodeInfo.setResult(PromoteConstants.RESULT_OK);
        nodeInfo.setAlreadyPromoted(false);
        return nodeInfo;
    }

    private TreeNode processTree(TreeNode rootNode, TreeNode parentNode,
            PromoteNodeData nodeData, int nodeIndex, int level)
            throws EloraException {

        String logInitMsg = "[processTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            // topLevelOK = true;
            if (level == 0) {
                level++;
                rootNode = new DefaultTreeNode(nodeData, parentNode);
                if (firstLoad) {
                    return rootNode;
                }

                promoteCheckerManager.processNodeInfo(nodeData.getData(), null,
                        nodeData.getNodeInfo(), nodeData.getLevel(),
                        nodeData.getIsSpecial(), finalState, nodeData.getStmt(),
                        promoteTransition, finalState, messages);

                Map<String, String> versionMap = promoteExecuterManager.getVersionMap(
                        nodeData.getData(), null, nodeData.getIsSpecial(),
                        level);
                nodeData = new PromoteNodeData(String.valueOf(nodeId),
                        nodeData.getData(), nodeData.getNodeInfo(), level,
                        nodeData.getDocId(), null, null, versionMap,
                        nodeData.getWcVersion(), false, false, false);

                nodeId++;
                processTreeNode(rootNode, nodeData, nodeIndex, level);
            } else {
                processTreeNode(parentNode, nodeData, nodeIndex, level);
            }

            // if (!PromoteHelper.isTransitionAllowsConfig(promoteTransition)
            // && !topLevelOK) {

            // if (!topLevelOK) {
            // TreeNode firstNode = rootNode.getChildren().get(0);
            // PromoteNodeData firstNodeData = (PromoteNodeData)
            // firstNode.getData();
            // firstNodeData.setResult(PromoteConstants.RESULT_KO);
            // }

            // processAllResults(rootNode);

            log.trace(logInitMsg + "--- EXIT --- ");
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
        // log.trace(logInitMsg + "--- ENTER --- ");
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

            // if (topLevelOK
            // && nodeData.getResult().equals(PromoteConstants.RESULT_KO)) {
            // topLevelOK = false;
            // }
            if (!nodeData.getIsDirect()) {
                level++;
                List<PromoteNodeData> childNodeList = getChildrenNodeData(
                        nodeData, level);
                if (!childNodeList.isEmpty()) {
                    for (PromoteNodeData childNode : childNodeList) {
                        processTreeNode(node, childNode, -1, level);
                    }
                }
            }
            // log.trace(logInitMsg + "--- EXIT --- ");
        } catch (Exception e) {
            log.trace(logInitMsg + "Error with doc |" + nodeData.getDocId()
                    + "|");
            throw e;
        }
    }

    private List<PromoteNodeData> getChildrenNodeData(PromoteNodeData nodeData,
            int level) throws EloraException {
        List<PromoteNodeData> childList = new ArrayList<PromoteNodeData>();
        childList = getSpecialChildrenNodeList(nodeData, level,
                promoteCheckerManager.getSpecialPredicates());
        childList.addAll(getChildrenNodeList(nodeData, level,
                promoteCheckerManager.getHierarchicalAndDirectPredicates()));

        return childList;
    }

    protected List<PromoteNodeData> getChildrenNodeList(
            PromoteNodeData nodeData, int level, List<Resource> predicateList)
            throws EloraException {
        String logInitMsg = "[getChildrenNodeList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc = nodeData.getData();
        List<PromoteNodeData> childrenNodeList = new ArrayList<PromoteNodeData>();
        List<Statement> objectStmts = EloraRelationHelper.getStatements(doc,
                predicateList);

        log.trace(logInitMsg + "Document |" + doc.getId() + "| Found relations:"
                + "\n" + objectStmts.size() + " normal children");

        if (!objectStmts.isEmpty()) {
            treatDocs(nodeData, level, logInitMsg, doc, childrenNodeList,
                    objectStmts);
        }
        return childrenNodeList;
    }

    protected List<PromoteNodeData> getSpecialChildrenNodeList(
            PromoteNodeData nodeData, int level, List<Resource> predicateList)
            throws EloraException {
        String logInitMsg = "[getSpecialChildrenNodeList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc = nodeData.getData();
        List<PromoteNodeData> childrenNodeList = new ArrayList<PromoteNodeData>();

        List<Statement> subjectStmts = EloraRelationHelper.getSubjectStatementsByPredicateList(
                doc, predicateList);

        List<Statement> specialObjectStmts = new ArrayList<Statement>();
        if (level == 2) {
            specialObjectStmts = EloraRelationHelper.getStatements(doc,
                    predicateList);
        }
        log.trace(logInitMsg + "Document |" + doc.getId() + "| Found relations:"
                + "\n" + subjectStmts.size() + " special children" + "\n"
                + specialObjectStmts.size()
                + " special children in first level.");

        if (!subjectStmts.isEmpty()) {
            treatSpecialDocs(nodeData, level, doc, childrenNodeList,
                    subjectStmts);
        }
        if (!specialObjectStmts.isEmpty()) {
            treatDocs(nodeData, level, logInitMsg, doc, childrenNodeList,
                    specialObjectStmts);
        }
        return childrenNodeList;
    }

    // TODO: Esta función se puede juntar con treatDocs pasandole el parametro
    // isSpecial
    private void treatSpecialDocs(PromoteNodeData nodeData, int level,
            DocumentModel doc, List<PromoteNodeData> childrenNodeList,
            List<Statement> subjectStmts) throws EloraException {
        Map<String, List<DocumentModel>> relatedDocs = new HashMap<String, List<DocumentModel>>();
        Map<String, Statement> stmtMap = new HashMap<String, Statement>();
        for (Statement subjectStmt : subjectStmts) {
            DocumentModel subject = RelationHelper.getDocumentModel(
                    subjectStmt.getSubject(), session);
            if (relatedDocs.containsKey(subject.getVersionSeriesId())) {
                List<DocumentModel> subjectList = relatedDocs.get(
                        subject.getVersionSeriesId());
                if (!subjectList.contains(subject)) {
                    relatedDocs.get(subject.getVersionSeriesId()).add(subject);
                    // TODO: No se tienen en cuenta diferentes predicateUri al
                    // mismo documento!
                    stmtMap.put(subject.getId(), subjectStmt);
                }
            } else {
                List<DocumentModel> subjectList = new ArrayList<DocumentModel>();
                subjectList.add(subject);
                relatedDocs.put(subject.getVersionSeriesId(), subjectList);
                // TODO: No se tienen en cuenta diferentes predicateUri al mismo
                // documento!
                stmtMap.put(subject.getId(), subjectStmt);
            }
        }
        childrenNodeList.addAll(treatRelatedDocs(relatedDocs, stmtMap, doc,
                nodeData, level, true));
    }

    private void treatDocs(PromoteNodeData nodeData, int level,
            String logInitMsg, DocumentModel doc,
            List<PromoteNodeData> childrenNodeList,
            List<Statement> specialObjectStmts) throws EloraException {
        Map<String, List<DocumentModel>> relatedDocs = new HashMap<String, List<DocumentModel>>();
        Map<String, Statement> stmtMap = new HashMap<String, Statement>();
        for (Statement specialObjectStmt : specialObjectStmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    specialObjectStmt.getObject(), session);
            if (relatedDocs.containsKey(object.getVersionSeriesId())) {
                List<DocumentModel> objectList = relatedDocs.get(
                        object.getVersionSeriesId());
                if (!objectList.contains(object)) {
                    // Otra version del mismo documento como hijo. Ahora
                    // consideramos que está mal aunque tenga diferente
                    // statement.
                    log.trace(logInitMsg + "Multiple versions related of doc: "
                            + object.getId());
                    throw new EloraException(
                            "Multiple versions related of docs: "
                                    + object.getId());
                }
            } else {
                List<DocumentModel> objectList = new ArrayList<DocumentModel>();
                objectList.add(object);
                relatedDocs.put(object.getVersionSeriesId(), objectList);
                // TODO: No se tienen en cuenta diferentes predicateUri al
                // mismo documento! Se utiliza el primero que llega.
                stmtMap.put(object.getId(), specialObjectStmt);
            }
        }
        childrenNodeList.addAll(treatRelatedDocs(relatedDocs, stmtMap, doc,
                nodeData, level, false));
    }

    private List<PromoteNodeData> treatRelatedDocs(
            Map<String, List<DocumentModel>> relatedDocs,
            Map<String, Statement> stmtMap, DocumentModel parentDoc,
            PromoteNodeData parentNodeData, int level, boolean isSpecial)
            throws EloraException {

        String logInitMsg = "[treatRelatedDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String parentState = getParentState(parentDoc,
                parentNodeData.getAlreadyPromoted(), parentNodeData.getResult(),
                parentNodeData.getIsPropagated());

        DocumentModel doc;
        List<PromoteNodeData> childNodeList = new ArrayList<PromoteNodeData>();
        for (Map.Entry<String, List<DocumentModel>> entry : relatedDocs.entrySet()) {
            try {
                List<DocumentModel> docList = entry.getValue();
                // Comprobar si coincide el drawing padre y el drawing hijo.
                if (isSpecial && level == 3) {
                    Statement parentStmt = parentNodeData.getStmt();
                    DocumentModel grandParentDoc = RelationHelper.getDocumentModel(
                            parentStmt.getSubject(), session);
                    String grandParentVersionSeriesId = grandParentDoc.getVersionSeriesId();
                    if (grandParentVersionSeriesId.equals(entry.getKey())) {
                        for (DocumentModel childDoc : docList) {
                            if (!grandParentDoc.getId().equals(
                                    childDoc.getId())) {
                                checkErrorsInMissingDocumentVersion(
                                        stmtMap.get(childDoc.getId()),
                                        parentNodeData, childDoc);
                            }
                        }
                        continue;
                    }
                }

                if (isSpecial && docList.size() > 1) {
                    // TODO: Ahora no se tiene en cuenta el predicateUri. Por si
                    // vienen dos relaciones diferentes al mismo documento. Se
                    // supone que para el drawing no va a ocurrir. Habría que
                    // seperarlo al crear el map, si tiene diferentes predicates
                    // meter en diferentes grupos

                    DocumentModel docWc = session.getDocument(
                            new IdRef(entry.getKey()));
                    if (docList.contains(docWc)) {
                        // is related with wc
                        doc = docWc;
                    } else {
                        DocumentModel subjectBv = EloraDocumentHelper.getBaseVersion(
                                docWc);
                        if (subjectBv == null) {
                            throw new EloraException("Document |"
                                    + docWc.getId()
                                    + "| has no base version. Probably because it has no AVs.");
                        }
                        if (docList.contains(subjectBv)) {
                            // is related with wc base version
                            doc = subjectBv;
                        } else {
                            // get latest related
                            List<String> uidList = EloraDocumentHelper.getUidListFromDocList(
                                    docList);
                            Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                                    docList);
                            String type = docList.get(0).getType();

                            doc = EloraRelationHelper.getLatestRelatedVersion(
                                    session, majorVersion, uidList, type);
                        }
                    }

                    for (DocumentModel childDoc : docList) {
                        if (!doc.getId().equals(childDoc.getId())) {
                            checkErrorsInMissingDocumentVersion(
                                    stmtMap.get(childDoc.getId()),
                                    parentNodeData, childDoc);
                        }
                    }
                } else if (docList.size() == 1) {
                    doc = docList.get(0);
                } else {
                    // documento no especial con varias versiones
                    log.trace(logInitMsg + "Multiple versions related of docs: "
                            + docList.toString());
                    throw new EloraException(
                            "Multiple versions related of docs: "
                                    + docList.toString());
                }

                if (doc != null) {
                    log.trace(logInitMsg + "Processing doc [" + doc.getId()
                            + "]");
                    // If the user creates a relation from WC to a
                    // specific archived version (instead of to another
                    // WC), we have to hide this relation when we
                    // display the tree of an AV document, or the user
                    // will see the same document twice (one AV and the
                    // WC). If parent element is an AV and child is WC,
                    // ignore it
                    if (parentDoc.isVersion() && !doc.isVersion()) {
                        continue;
                    }

                    PromoteNodeData nodeData = getNodeData(doc, parentDoc,
                            stmtMap.get(doc.getId()), parentNodeData, level,
                            parentState, isSpecial);
                    processIconOnlyRelations(doc, nodeData);
                    childNodeList.add(nodeData);
                    nodeId++;
                }
            } catch (Exception e) {
                if (isSpecial) {
                    log.trace(logInitMsg
                            + "Error with special doc versionSeriesId |"
                            + entry.getKey() + "|");
                } else {
                    log.trace(logInitMsg + "Error with doc versionSeriesId |"
                            + entry.getKey() + "|");
                }
                throw e;
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
        return childNodeList;
    }

    private void checkErrorsInMissingDocumentVersion(Statement stmt,
            PromoteNodeData parentNodeData, DocumentModel childDoc)
            throws EloraException {

        // Check status even if special document is not showing in tree
        NodeDynamicInfo nodeInfo = new NodeDynamicInfo();
        nodeInfo.setEditableVersion(false);
        nodeInfo.setIsPropagated(false);
        nodeInfo.setIsEnforced(false);
        nodeInfo.setSwitchableVersion(false);

        // Esto se tiene que mirar siempre aunque la pieza no vaya en la
        // operacion. El resultado de la compatibilidad de los estados
        // se guarda en una variable
        // para ver si en un futuro, si marcamos que la pieza va en la
        // operación, va a dar problemas por un documento especial que
        // no aparece en el árbol. Así,
        // siempre miraremos esa variable y si está a KO sabremos que
        // tiene que dar KO aunque lo que hay en el árbol sea correcto!

        // Simulacro de si el nodo padre se va a promocionar en algun
        // momento que pasaría con los drawings que no se ven en el
        // arbol! Por eso en el estado del padre pongo finalState

        String resultMsg = PromoteHelper.checkSupportedStates(childDoc,
                nodeInfo.getIsPropagated(), parentNodeData.getDocId(), true,
                finalState, promoteTransition,
                promoteCheckerManager.getHierarchicalAndDirectPredicates(),
                childDoc.getCurrentLifeCycleState(), stmt, messages);

        if (!resultMsg.equals("")) {
            NodeDynamicInfo parentNodeInfo = parentNodeData.getNodeInfo();
            if (parentNodeData.getIsPropagated()) {
                parentNodeInfo.setResult(PromoteConstants.RESULT_KO);
                parentNodeData.setResult(PromoteConstants.RESULT_KO);
                parentNodeInfo.setResultMsg(
                        "Special document is missing in tree and it has a not compatible status");
                parentNodeData.setResultMsg(
                        "Special document is missing in tree and it has a not compatible status");
                parentNodeData.setNodeInfo(parentNodeInfo);
            }
            parentNodeInfo.setHiddenResult(PromoteConstants.RESULT_KO);
            parentNodeInfo.setHiddenResultMsg(
                    "Special document is missing in tree and it has a not compatible status");
        }
    }

    private PromoteNodeData getNodeData(DocumentModel childDoc,
            DocumentModel parentDoc, Statement stmt,
            PromoteNodeData parentNodeData, int level, String parentState,
            boolean isSpecial) throws EloraException {

        // get info checking configuration
        NodeDynamicInfo nodeInfo = calculateConfigNodeInfo(childDoc, stmt,
                parentNodeData.getIsPropagated(),
                parentNodeData.getAlreadyPromoted(), parentNodeData.getResult(),
                level);

        promoteCheckerManager.processNodeInfo(childDoc, parentNodeData,
                nodeInfo, level, isSpecial, parentState, stmt,
                promoteTransition, finalState, messages);

        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
        DocumentModel wcDoc = session.getWorkingCopy(childDoc.getRef());
        String wcVersion = wcDoc.getVersionLabel();

        Map<String, String> versionMap = null;
        // if (level == 2) {
        // versionMap = getVersionMap(childDoc, stmt, isSpecial, level);
        // }

        boolean isDirect = isSpecial
                && isDirectPredicate(stmt.getPredicate().getUri());

        return new PromoteNodeData(String.valueOf(nodeId), childDoc, nodeInfo,
                level, childDoc.getId(), stmt, stmtInfo.getQuantity(),
                versionMap, wcVersion, isDirect, isSpecial,
                nodeInfo.getAlreadyPromoted());
    }

    private boolean isDirectPredicate(String predicateUri) {
        if (promoteCheckerManager.getDirectPredicates().contains(
                predicateUri)) {
            return true;
        } else {
            return false;
        }
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

        // User selected document version
        DocumentRef docRef = new IdRef(nodeData.getDocId());
        DocumentModel doc = session.getDocument(docRef);

        boolean isSpecial = nodeData.getIsSpecial();
        NodeDynamicInfo nodeInfo = calculateConfigNodeInfo(doc, stmt,
                parentIsPropagated, parentAlreadyPromoted, parentResult, level);

        // Recalculate things that could change. Configuration changes are not
        // considered, we load nodes the same way as tree was loaded the first
        // time
        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        String wcVersion = wcDoc.getVersionLabel();

        LinkedHashMap<String, String> versionMap = null;
        if (level == 1) {
            versionMap = promoteExecuterManager.getVersionMap(wcDoc, stmt,
                    isSpecial, level);
        }

        // Parent final state!
        String parentState = getParentState(doc, parentAlreadyPromoted,
                parentResult, parentIsPropagated);

        promoteCheckerManager.processNodeInfo(doc, parentNodeData, nodeInfo,
                level, isSpecial, parentState, stmt, promoteTransition,
                finalState, messages);

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

    private void processPropagation(TreeNode treeNode, PromoteNodeData nodeData,
            TreeNode previousTreeNode, String previousNodePosition,
            boolean check) throws EloraException {

        // int level = nodeData.getLevel();

        if (!nodeData.getNodeInfo().getAlreadyPromoted()) {

            Map<String, List<EloraConfigRow>> propagationMap = promoteExecuterManager.getRelationPropagationMap();

            for (EloraConfigRow configRow : propagationMap.get(
                    nodeData.getStmt().getPredicate().getUri())) {
                boolean propagate = (long) configRow.getProperty(
                        PromoteConstants.PROPAGATE) == 1;
                // boolean enforce = (long) configRow.getProperty(
                // PromoteConstants.ENFORCE) == 1;
                long direction = (long) configRow.getProperty(
                        PromoteConstants.DIRECTION);

                // if (rootIsSpecial && level == 2) {
                // direction = invertDirection(direction);
                // }

                if (propagate
                        && direction == PromoteConstants.DIRECTION_ASCENDING) {
                    if (nodeData.getLevel() > 2 && (previousNodePosition.equals(
                            PromoteConstants.FIRST_NODE)
                            || previousNodePosition.equals(
                                    PromoteConstants.PREVIOUS_NODE_POSITION_BELOW))) {
                        TreeNode parent = treeNode.getParent();
                        PromoteNodeData parentNodeData = (PromoteNodeData) parent.getData();
                        // updateNodeCheck(check, enforce, parentNodeData);

                        processPropagation(parent, parentNodeData, treeNode,
                                PromoteConstants.PREVIOUS_NODE_POSITION_BELOW,
                                check);
                    }
                } else {
                    if (propagate && previousNodePosition.equals(
                            PromoteConstants.PREVIOUS_NODE_POSITION_ABOVE)) {
                        // updateNodeCheck(check, enforce, nodeData);
                    }

                    if (propagate
                            || previousNodePosition.equals(
                                    PromoteConstants.FIRST_NODE)
                            || previousNodePosition.equals(
                                    PromoteConstants.PREVIOUS_NODE_POSITION_BELOW)) {
                        List<TreeNode> children = treeNode.getChildren();
                        for (TreeNode child : children) {
                            if (previousNodePosition.equals(
                                    PromoteConstants.FIRST_NODE)
                                    || !child.equals(previousTreeNode)) {
                                processPropagation(child,
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
    }

    // TODO: Por ahora se quita para no liarnos con las propagaciones al
    // clickar. En un futuro puede que se ponga el check cuando es enforce y no
    // hacer caso cuando es default

    // private void updateNodeCheck(boolean check, boolean enforce,
    // PromoteNodeData parentNodeData) {

    // parentNodeData.setIsPropagated(check);
    // parentNodeData.getNodeInfo().setIsPropagated(check);

    // if (check) {
    // parentNodeData.setIsEnforced(enforce);
    // parentNodeData.getNodeInfo().setIsEnforced(enforce);
    // } else {
    // parentNodeData.setIsEnforced(false);
    // parentNodeData.getNodeInfo().setIsEnforced(false);
    // }
    // }

    private void processAllResults(TreeNode rootNode) throws EloraException {
        promoteCheckerManager.resetValues();
        TreeNode firstNode = rootNode.getChildren().get(0);
        // PromoteNodeData firstNodeData = (PromoteNodeData)
        // firstNode.getData();
        promoteCheckerManager.processTreeResult(firstNode, 1, promoteTransition,
                finalState, messages);

        // if (!PromoteHelper.isTransitionAllowsConfig(promoteTransition)
        // && !topLevelOK) {
        // firstNodeData.setResult(PromoteConstants.RESULT_KO);
        // }
        // if (topLevelOK) {
        // firstNodeData.setResult(PromoteConstants.RESULT_OK);
        // } else {
        // firstNodeData.setResult(PromoteConstants.RESULT_KO);
        // }
    }

    private void processIconOnlyRelations(DocumentModel childDoc,
            PromoteNodeData nodeData) {
        if (iconOnlyRelationDocs.containsKey(childDoc.getId())) {
            List<String> iconOnlyRelations = iconOnlyRelationDocs.get(
                    childDoc.getId());
            nodeData.setIconOnlyRelations(iconOnlyRelations);
        }
    }

    private NodeDynamicInfo calculateConfigNodeInfo(DocumentModel doc,
            Statement stmt, boolean parentIsPropagated,
            boolean parentAlreadyPromoted, String parentResult, int level)
            throws EloraException {

        NodeDynamicInfo nodeInfo = new NodeDynamicInfo();
        if (level == 1) {
            nodeInfo.setEditableVersion(true);
            nodeInfo.setIsPropagated(true);
            nodeInfo.setIsEnforced(true);
            nodeInfo.setSwitchableVersion(true);
        } else {
            // TODO: Ahora se ponen fijo que no se puede editar las versiones
            // if (level == 2) {
            // nodeInfo.setEditableVersion(true);
            // } else {
            // nodeInfo.setEditableVersion(false);
            // }

            nodeInfo.setEditableVersion(false);

            // nodeInfo.setEditableVersion(!parentAlreadyPromoted
            // && !parentResult.equals(PromoteConstants.RESULT_KO));

            if (PromoteHelper.isAlreadyPromoted(doc, finalState,
                    promoteCheckerManager.getLifeCycleStatesConfig())) {
                nodeInfo.setIsPropagated(false);
                nodeInfo.setIsEnforced(true);
                nodeInfo.setSwitchableVersion(false);
            } else if (((parentIsPropagated && isResultOK(parentResult))
                    || (parentAlreadyPromoted))) {
                EloraConfigRow propagationConfig = promoteExecuterManager.getRelationDescendingPropagationConfig().getRow(
                        stmt.getPredicate().getUri());
                if (propagationConfig != null) {
                    // if (rootIsSpecial && level == 2) {
                    // boolean isPropagated = !((long)
                    // propagationConfig.getProperty(
                    // "propagate") == 1);
                    // if (isPropagated) {
                    // nodeInfo.setIsEnforced(
                    // (long) propagationConfig.getProperty(
                    // "enforce") == 1);
                    // }
                    // } else {
                    nodeInfo.setIsPropagated(
                            (long) propagationConfig.getProperty(
                                    "propagate") == 1);
                    nodeInfo.setIsEnforced((long) propagationConfig.getProperty(
                            "enforce") == 1);
                    // }
                } else {
                    nodeInfo.setIsPropagated(false);
                    nodeInfo.setIsEnforced(false);
                }
                nodeInfo.setSwitchableVersion(true);
            } else {
                nodeInfo.setIsPropagated(false);
                nodeInfo.setIsEnforced(false);
                nodeInfo.setSwitchableVersion(false);
            }
        }
        return nodeInfo;
    }

    private boolean isResultOK(String parentResult) {
        return parentResult.equals(PromoteConstants.RESULT_OK)
                || parentResult.equals("");
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
}
