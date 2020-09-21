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
package com.aritu.eloraplm.pdm.makeobsolete.treetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.makeobsolete.api.MakeObsoleteService;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;
import com.aritu.eloraplm.pdm.makeobsolete.util.MakeObsoleteHelper;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * Service class for managing the content of a Make Obsolete Tree.
 *
 * @author aritu
 *
 */
public class MakeObsoleteNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            MakeObsoleteNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected List<String> nodeDocIds;

    protected MakeObsoleteService makeObsoleteService;

    /**
     * @throws EloraException
     *
     */
    public MakeObsoleteNodeService(CoreSession session,
            MakeObsoleteService makeObsoleteService) throws EloraException {
        super();
        this.session = session;
        nodeId = 0;
        nodeDocIds = new ArrayList<String>();
        this.makeObsoleteService = makeObsoleteService;
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.treetable.NodeManager#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object currentDocumentObject)
            throws EloraException, DocumentUnreadableException {

        DocumentModel currentDoc = (DocumentModel) currentDocumentObject;

        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (!currentDoc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)
                && !currentDoc.hasFacet(
                        EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            throw new EloraException(
                    "The document should be a BOM or a CAD document");
        }

        int level = 0;
        MakeObsoleteNodeData nodeData = new MakeObsoleteNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createMakeObsoleteRootTree(currentDoc, root, level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    protected TreeNode createMakeObsoleteRootTree(DocumentModel currentDoc,
            TreeNode root, int level) throws EloraException {

        String logInitMsg = "[createModifiedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (MakeObsoleteHelper.impliesMakingObsoleteAllVersions(session,
                currentDoc)) {

            // Retrieve all the versions of the document, and add each of them
            List<DocumentModel> versionsDocs = currentDoc.getCoreSession().getVersions(
                    currentDoc.getRef());
            for (DocumentModel versionDoc : versionsDocs) {
                boolean isVersionInitiatorDocument = isVersionInitiatorDocument(
                        currentDoc, versionDoc);
                CanMakeObsoleteResult canMakeObsoleteResult = makeObsoleteService.canMakeObsoleteDocument(
                        session, versionDoc);
                TreeNode newNodeData = addNodeData(versionDoc, root, root,
                        level, isVersionInitiatorDocument,
                        canMakeObsoleteResult, true);
                root = addChildrenNodeData(versionDoc, root, newNodeData,
                        level);
            }
        } else {
            CanMakeObsoleteResult canMakeObsoleteResult = makeObsoleteService.canMakeObsoleteDocument(
                    session, currentDoc);
            TreeNode newNodeData = addNodeData(currentDoc, root, root, level,
                    true, canMakeObsoleteResult, true);
            root = addChildrenNodeData(currentDoc, root, newNodeData, level);
        }

        // Once the tree is finished, check all the content of the tree and see
        // if for those that cannot be made obsolete since it has incompatible
        // related docs, and those related docs go in the tree, if we can put
        // then available to make obsolete
        checkNodesWithIncompatibleRelatedDocsInWholeTree(root, root,
                currentDoc.getCoreSession());

        List<String> allNodeDocIds = retrieveAllNodeDocIdFromTree(root);
        checkNodesThatImpliesMakingObsoleteAllVersions(allNodeDocIds, root,
                currentDoc.getCoreSession());

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    /**
     * Checks if the specified document version is the initiator document.
     *
     * @param initiatorDocument
     * @param versionDocument
     * @return
     */
    protected boolean isVersionInitiatorDocument(
            DocumentModel initiatorDocument, DocumentModel versionDocument) {

        boolean isInitiatorDocument = false;

        DocumentModel initiatorDocumentWcDoc = initiatorDocument.getCoreSession().getWorkingCopy(
                initiatorDocument.getRef());
        DocumentModel initiatorDocumentBaseDoc = EloraDocumentHelper.getBaseVersion(
                initiatorDocumentWcDoc);

        if (versionDocument.getId().equals(initiatorDocument.getId())
                || versionDocument.getId().equals(
                        initiatorDocumentBaseDoc.getId())) {
            isInitiatorDocument = true;

        }
        return isInitiatorDocument;
    }

    /**
     * @param nodeDoc
     * @param root
     * @param parentNode
     * @param level
     * @param mandatoryToMakeObsolete
     * @return added node element
     * @throws EloraException
     */
    protected TreeNode addNodeData(DocumentModel nodeDoc, TreeNode root,
            TreeNode parentNode, int level, boolean isInitiatorDocument,
            CanMakeObsoleteResult canMakeObsoleteResult,
            boolean mandatoryToMakeObsolete) throws EloraException {

        DocumentModel wcDoc = nodeDoc.getCoreSession().getWorkingCopy(
                nodeDoc.getRef());

        boolean selectedToMakeObsolete = canMakeObsoleteResult.getCanMakeObsolete();
        // Process only current document
        MakeObsoleteNodeData nodeData = new MakeObsoleteNodeData(
                String.valueOf(nodeId), level, nodeDoc, wcDoc,
                isInitiatorDocument, canMakeObsoleteResult,
                selectedToMakeObsolete, mandatoryToMakeObsolete);

        nodeId++;

        if (!nodeDocIds.contains(nodeDoc.getId())) {
            nodeDocIds.add(nodeDoc.getId());
        }

        TreeNode node = new DefaultTreeNode(nodeData, parentNode);
        node.setExpanded(true);

        return node;
    }

    protected TreeNode addChildrenNodeData(DocumentModel parentDoc,
            TreeNode root, TreeNode parentNode, int level)
            throws EloraException {

        // For the instance only CAD Documents will have to display children
        if (parentDoc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {

            // Treat special subjects (cannot be unselected from tree)
            DocumentModelList relatedSpecialSubjectDocs = new DocumentModelListImpl();
            relatedSpecialSubjectDocs.addAll(RelationHelper.getSubjectDocuments(
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF),
                    parentDoc));
            if (!relatedSpecialSubjectDocs.isEmpty()) {

                level++;

                for (DocumentModel relatedSpecialSubject : relatedSpecialSubjectDocs) {
                    CanMakeObsoleteResult canMakeObsoleteResult = calculateCanMakeObsoleteResultForRelatedDoc(
                            relatedSpecialSubject, parentDoc);
                    addNodeData(relatedSpecialSubject, root, parentNode, level,
                            false, canMakeObsoleteResult, true);
                }
            }

            // Treat special objects (can be unselected)
            DocumentModelList relatedSpecialObjectDocs = new DocumentModelListImpl();
            relatedSpecialObjectDocs.addAll(RelationHelper.getObjectDocuments(
                    parentDoc,
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF)));
            if (!relatedSpecialObjectDocs.isEmpty()) {

                level++;

                for (DocumentModel relatedSpecialObject : relatedSpecialObjectDocs) {
                    CanMakeObsoleteResult canMakeObsoleteResult = calculateCanMakeObsoleteResultForRelatedDoc(
                            relatedSpecialObject, parentDoc);
                    addNodeData(relatedSpecialObject, root, parentNode, level,
                            false, canMakeObsoleteResult, false);
                }
            }
        }

        return root;
    }

    protected CanMakeObsoleteResult calculateCanMakeObsoleteResultForRelatedDoc(
            DocumentModel relatedDoc, DocumentModel parentDoc)
            throws EloraException {

        CanMakeObsoleteResult canMakeObsoleteResult = makeObsoleteService.canMakeObsoleteDocument(
                session, relatedDoc);

        if (!canMakeObsoleteResult.getCanMakeObsolete()
                && canMakeObsoleteResult.getIncompatibleRelatedDocIds().size() > 0) {
            // Remove parentDoc from incompatible related docs
            List<String> incompatibleRelatedDocIds = canMakeObsoleteResult.getIncompatibleRelatedDocIds();
            if (incompatibleRelatedDocIds.contains(parentDoc.getId())) {
                incompatibleRelatedDocIds.remove(parentDoc.getId());
            }
            if (incompatibleRelatedDocIds.size() == 0) {
                canMakeObsoleteResult.setCanMakeObsolete(true);
                canMakeObsoleteResult.setIncompatibleRelatedDocIds(
                        incompatibleRelatedDocIds);
            }
        }

        return canMakeObsoleteResult;
    }

    // TODO:: honi agian izena aldatu, parametroak ulertzeko
    protected void checkNodesWithIncompatibleRelatedDocsInWholeTree(
            TreeNode root, TreeNode currentNode, CoreSession session)
            throws EloraException {

        String logInitMsg = "[checkNodesWithIncompatibleRelatedDocsInWholeTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (TreeNode childNode : currentNode.getChildren()) {
            MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();

            // If it cannot be make obsolete and it has incompatibleRelatedDocs,
            // check if those incompatible related docs are in the tree and
            // their node attributes.
            CanMakeObsoleteResult childNodeCanMakeObsoleteResult = childNodeData.getCanMakeObsoleteResult();

            if (!childNodeCanMakeObsoleteResult.getCanMakeObsolete()
                    && childNodeCanMakeObsoleteResult.getIncompatibleRelatedDocIds().size() > 0) {

                // Remove from the incompatibleRelatdDocIds list those that are
                // also contained in the tree.
                // For that, create a new list without the docIds that are
                // already contained in the tree.
                // If after filtering those doc identifiers list is empty, it
                // means that this node can be made obsolete.
                List<String> filteredIncompatibleRelatedDocIds = new ArrayList<String>();
                for (String incompatibleRelatedDocId : childNodeCanMakeObsoleteResult.getIncompatibleRelatedDocIds()) {
                    filteredIncompatibleRelatedDocIds.add(
                            incompatibleRelatedDocId);
                }

                for (String incompatibleRelatedDocId : childNodeCanMakeObsoleteResult.getIncompatibleRelatedDocIds()) {
                    boolean changeCanBeObsoleteValue = true;

                    // if the incompatibleRelatedDoc in a node of the tree,
                    // retrieve its node data and check the status
                    if (nodeDocIds.contains(incompatibleRelatedDocId)) {
                        MakeObsoleteNodeData incompatibleReatedDocNodeData = getMakeObsoleteNodeDataByNodeDocId(
                                root, incompatibleRelatedDocId);

                        if (incompatibleReatedDocNodeData == null) {
                            changeCanBeObsoleteValue = false;
                        } else {
                            CanMakeObsoleteResult incompatibleRelateDocMakeObsoleteResult = incompatibleReatedDocNodeData.getCanMakeObsoleteResult();

                            if (!incompatibleRelateDocMakeObsoleteResult.getCanMakeObsolete()
                                    && incompatibleReatedDocNodeData.getMandatoryToMakeObsolete()) {
                                changeCanBeObsoleteValue = false;
                            }
                        }
                    } else {
                        changeCanBeObsoleteValue = false;
                    }
                    if (changeCanBeObsoleteValue) {
                        filteredIncompatibleRelatedDocIds.remove(
                                incompatibleRelatedDocId);
                    }
                }

                log.trace(logInitMsg
                        + "filteredIncompatibleRelatedDocIds size = |"
                        + filteredIncompatibleRelatedDocIds.size() + "|");

                if (filteredIncompatibleRelatedDocIds.size() == 0) {

                    boolean impliesMakingObsoleteAllVersions = MakeObsoleteHelper.impliesMakingObsoleteAllVersions(
                            session, childNodeData.getData());

                    if (!impliesMakingObsoleteAllVersions
                            || (impliesMakingObsoleteAllVersions
                                    && (childNodeData.getCanMakeObsoleteResult().getCannotMakeObsoleteReasonMsg() == null
                                            || childNodeData.getCanMakeObsoleteResult().getCannotMakeObsoleteReasonMsg().length() == 0))) {

                        log.trace(logInitMsg + "Document with id = |"
                                + childNodeData.getData().getId()
                                + "| can be made obsolete since its related incompatible docs are contained in the tree.");
                        childNodeData.setCanMakeObsoleteResult(
                                new CanMakeObsoleteResult(true));
                        childNodeData.setSelectedToMakeObsolete(true);
                    }

                } else {
                    childNodeCanMakeObsoleteResult.setCannotMakeObsoleteReasonMsg(
                            "eloraplm.message.error.makeobsolete.transition.not.allowed.since.related.docs");
                    childNodeCanMakeObsoleteResult.setIncompatibleRelatedDocIds(
                            filteredIncompatibleRelatedDocIds);
                }
            }
            checkNodesWithIncompatibleRelatedDocsInWholeTree(root, childNode,
                    session);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void checkNodesThatImpliesMakingObsoleteAllVersions(
            List<String> allNodeDocIds, TreeNode currentNode,
            CoreSession session) throws EloraException {

        String logInitMsg = "[checkNodesThatImpliesMakingObsoleteAllVersions] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (TreeNode childNode : currentNode.getChildren()) {
            MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();

            // if it is the initiator document, all versions are already
            // included.
            // we should test cases that are not initiator document
            if (!childNodeData.isInitiatorDocument) {

                DocumentModel childNodeDoc = childNodeData.getData();
                if (childNodeDoc == null) {
                    log.error(logInitMsg
                            + "Error parsing the tree. Node document is null.");
                    throw new EloraException("Error parsing the tree");
                }

                if (MakeObsoleteHelper.impliesMakingObsoleteAllVersions(session,
                        childNodeDoc)) {

                    // check if all of the versions are contained in the list
                    // get all AV versions of the document
                    List<DocumentModel> docVersions = childNodeDoc.getCoreSession().getVersions(
                            childNodeDoc.getRef());
                    for (DocumentModel docVersion : docVersions) {
                        if (!docVersion.getCurrentLifeCycleState().equals(
                                EloraLifeCycleConstants.OBSOLETE)
                                && !allNodeDocIds.contains(
                                        docVersion.getId())) {

                            CanMakeObsoleteResult childNodeCanMakeObsoleteResult = childNodeData.getCanMakeObsoleteResult();
                            childNodeCanMakeObsoleteResult.setCanMakeObsolete(
                                    false);
                            childNodeCanMakeObsoleteResult.setCannotMakeObsoleteReasonMsg(
                                    "eloraplm.message.error.makeobsolete.version.missing.since.implies.making.obsolete.all.versions");
                            childNodeCanMakeObsoleteResult.setCannotMakeObsoleteReasonMsgParam(
                                    docVersion.getId());
                            // exit form versions loop
                            break;
                        }
                    }
                }
            }

            checkNodesThatImpliesMakingObsoleteAllVersions(allNodeDocIds,
                    childNode, session);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public MakeObsoleteNodeData getMakeObsoleteNodeDataByNodeDocId(
            TreeNode root, String nodeDocId) {

        MakeObsoleteNodeData nodeData = null;

        // Returns the first nodeData with this id
        for (TreeNode childNode : root.getChildren()) {
            MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();
            if (childNodeData.getData().getId().equals(nodeDocId)) {
                nodeData = childNodeData;
                // exit from loop
                break;
            }
            nodeData = getMakeObsoleteNodeDataByNodeDocId(childNode, nodeDocId);
            if (nodeData != null) {
                // exit from loop
                break;
            }
        }
        return nodeData;
    }

    public List<String> retrieveAllNodeDocIdFromTree(TreeNode root) {
        List<String> nodeDocIdList = new ArrayList<String>();
        for (TreeNode childNode : root.getChildren()) {
            MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();
            if (childNodeData.getData() != null) {
                nodeDocIdList.add(childNodeData.getData().getId());

                List<String> childNodeDocIdList = retrieveAllNodeDocIdFromTree(
                        childNode);
                if (childNodeDocIdList != null
                        && childNodeDocIdList.size() > 0) {
                    nodeDocIdList.addAll(childNodeDocIdList);
                }
            }
        }
        return nodeDocIdList;
    }

    public boolean canBeExecuted(TreeNode node) {

        String logInitMsg = "[canBeExecuted] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean canBeExecuted = true;

        try {

            for (TreeNode childNode : node.getChildren()) {
                MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();

                // if it is not the initiator document, and it is already in a
                // obsolete state, canBeExecuted true
                if (!childNodeData.isInitiatorDocument
                        && LifecyclesConfig.obsoleteStatesList.contains(
                                childNodeData.getData().getCurrentLifeCycleState())) {
                    canBeExecuted = true;
                } else {
                    if (childNodeData.getMandatoryToMakeObsolete()
                            && !childNodeData.getCanMakeObsoleteResult().getCanMakeObsolete()) {
                        return false;
                    }
                }

                boolean childNodeCanBeExecuted = canBeExecuted(childNode);

                if (!childNodeCanBeExecuted) {
                    return false;
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage());
            canBeExecuted = false;

        }

        log.trace(logInitMsg + "--- EXIT ---");

        return canBeExecuted;
    }

    public Map<String, CanMakeObsoleteResult> makeObsolete(TreeNode node)
            throws EloraException {
        String logInitMsg = "[makeObsolete] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, CanMakeObsoleteResult> makeObsoleteResultList = new HashMap<String, CanMakeObsoleteResult>();

        List<DocumentModel> selectedDocumentsToMakeObsolete = new ArrayList<DocumentModel>();
        fillSelectedDocumentsListFromTree(node,
                selectedDocumentsToMakeObsolete);

        if (selectedDocumentsToMakeObsolete.size() > 0) {

            makeObsoleteResultList = makeObsoleteService.makeObsoleteDocumentList(
                    session, selectedDocumentsToMakeObsolete);
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return makeObsoleteResultList;
    }

    protected void fillSelectedDocumentsListFromTree(TreeNode node,
            List<DocumentModel> selectedDocuments) {

        for (TreeNode childNode : node.getChildren()) {
            MakeObsoleteNodeData childNodeData = (MakeObsoleteNodeData) childNode.getData();

            if (childNodeData.getSelectedToMakeObsolete()) {
                DocumentModel childNodeDocument = childNodeData.getData();
                if (!selectedDocuments.contains(childNodeDocument)) {
                    selectedDocuments.add(childNodeDocument);
                }
            }
            fillSelectedDocumentsListFromTree(childNode, selectedDocuments);

        }
    }

    public void toggleNodeAsSelected(TreeNode node) throws EloraException {

        String logInitMsg = "[toggleNodeAsSelected] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        MakeObsoleteNodeData nodeData = (MakeObsoleteNodeData) node.getData();

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
