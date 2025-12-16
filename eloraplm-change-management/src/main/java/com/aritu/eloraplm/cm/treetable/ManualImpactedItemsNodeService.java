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
package com.aritu.eloraplm.cm.treetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.ManualItem;
import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class ManualImpactedItemsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            ManualImpactedItemsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected String itemType;

    protected Map<String, String> messages;

    private Map<String, TreeNode> modifiedNodeMap = new HashMap<String, TreeNode>();

    public ManualImpactedItemsNodeService(CoreSession session,
            Map<String, String> messages) throws EloraException {
        this.session = session;
        this.messages = messages;
        nodeId = 0;
    }

    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {

        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel currentDoc = (DocumentModel) parentObject;

        if (!(currentDoc.getType().equals(CMDoctypeConstants.CM_ECR)
                || currentDoc.getType().equals(CMDoctypeConstants.CM_ECO))) {
            throw new EloraException(
                    "First level document of the tree must be an "
                            + CMDoctypeConstants.CM_ECR + " or an "
                            + CMDoctypeConstants.CM_ECO + ".");
        }

        int level = 0;
        ManualImpactedItemsNodeData nodeData = new ManualImpactedItemsNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createManualImpactedItemsRootTree(currentDoc, root);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createManualImpactedItemsRootTree(DocumentModel currentDoc,
            TreeNode root) throws EloraException {

        String logInitMsg = "[createManualImpactedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            ArrayList<HashMap<String, Object>> modifiedItems = new ArrayList<HashMap<String, Object>>();
            if (currentDoc.getPropertyValue(
                    CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
                modifiedItems = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                        CMMetadataConstants.MODIFIED_ITEM_LIST);
            }

            ArrayList<HashMap<String, Object>> manualImpactedItems = new ArrayList<HashMap<String, Object>>();
            if (currentDoc.getPropertyValue(
                    CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST) != null) {
                manualImpactedItems = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                        CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST);
            }

            Map<String, List<ManualImpactedItemsNodeData>> modifiedIndexManualImpactedList = convertManualImpactedItemsToModifiedIndexList(
                    manualImpactedItems, 1);

            for (Map<String, Object> modifiedItem : modifiedItems) {

                ManualImpactedItemsNodeData nodeData = createManualImpactedNodeData(
                        modifiedItem, 0);

                if (!(boolean) modifiedItem.get("includeInImpactMatrix")
                        || nodeData.getOriginItem().getType().equals(
                                EloraDoctypeConstants.CAD_DRAWING)) {
                    continue;
                }

                String modifiedItemUid = (String) modifiedItem.get(
                        "originItem");

                TreeNode node = new DefaultTreeNode(nodeData, root);
                node.setExpanded(true);

                modifiedNodeMap.put(modifiedItemUid, node);

                if (modifiedIndexManualImpactedList.containsKey(
                        modifiedItemUid)) {
                    List<ManualImpactedItemsNodeData> manualImpactedList = modifiedIndexManualImpactedList.get(
                            modifiedItemUid);
                    for (ManualImpactedItemsNodeData manualImpactedItem : manualImpactedList) {
                        TreeNode childNode = new DefaultTreeNode(
                                manualImpactedItem, node);
                        childNode.setExpanded(true);
                    }
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private Map<String, List<ManualImpactedItemsNodeData>> convertManualImpactedItemsToModifiedIndexList(
            ArrayList<HashMap<String, Object>> manualImpactedItems, int level) {

        HashMap<String, List<ManualImpactedItemsNodeData>> manualImpactedItemListWithModifiedIndex = new HashMap<String, List<ManualImpactedItemsNodeData>>();

        for (HashMap<String, Object> manualImpactedItemHM : manualImpactedItems) {
            String modifiedItem = (String) manualImpactedItemHM.get(
                    "modifiedItem");

            ManualImpactedItemsNodeData nodeData = createManualImpactedItemsNodeData(
                    manualImpactedItemHM, level);

            if (manualImpactedItemListWithModifiedIndex.containsKey(
                    modifiedItem)) {
                List<ManualImpactedItemsNodeData> modifiedIndexList = manualImpactedItemListWithModifiedIndex.get(
                        modifiedItem);
                modifiedIndexList.add(nodeData);
            } else {
                List<ManualImpactedItemsNodeData> modifiedIndexList = new ArrayList<ManualImpactedItemsNodeData>();
                modifiedIndexList.add(nodeData);
                manualImpactedItemListWithModifiedIndex.put(modifiedItem,
                        modifiedIndexList);
            }
        }

        return manualImpactedItemListWithModifiedIndex;
    }

    private ManualImpactedItemsNodeData createManualImpactedNodeData(
            Map<String, Object> map, int level) {

        Long rowNumber = (Long) map.get("rowNumber");
        String currentNodeId = (String) map.get("nodeId");
        String originItemUid = (String) map.get("originItem");

        String action = (String) map.get("action");
        String type = (String) map.get("type");
        String comment = (String) map.get("comment");

        DocumentModel originItem = null;
        DocumentModel originItemWc = null;
        if (originItemUid != null) {
            originItem = session.getDocument(new IdRef(originItemUid));
            originItemWc = session.getWorkingCopy(originItem.getRef());
        }

        // Modified item in impacted tree
        ManualImpactedItemsNodeData nodeData = new ManualImpactedItemsNodeData(
                String.valueOf(nodeId), level, false, false, false, rowNumber,
                currentNodeId, originItemUid, true, originItem, originItemWc,
                action, true, type, comment);

        return nodeData;
    }

    private ManualImpactedItemsNodeData createManualImpactedItemsNodeData(
            Map<String, Object> manualImpactedItems, int level) {

        Long rowNumber = (Long) manualImpactedItems.get("rowNumber");
        String nodeId = (String) manualImpactedItems.get("nodeId");
        String modifiedItem = (String) manualImpactedItems.get("modifiedItem");
        String originItemUid = (String) manualImpactedItems.get("originItem");
        String action = (String) manualImpactedItems.get("action");
        String type = (String) manualImpactedItems.get("type");
        String comment = (String) manualImpactedItems.get("comment");

        DocumentModel originItem = null;
        DocumentModel originItemWc = null;
        if (originItemUid != null) {
            originItem = session.getDocument(new IdRef(originItemUid));
            originItemWc = session.getWorkingCopy(originItem.getRef());
        }

        // Modified item in impacted tree
        ManualImpactedItemsNodeData nodeData = new ManualImpactedItemsNodeData(
                String.valueOf(nodeId), level, false, false, false, rowNumber,
                nodeId, modifiedItem, false, originItem, originItemWc, action,
                false, type, comment);

        return nodeData;
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root,
            ChangeManagementService cmService) throws EloraException {

        String logInitMsg = "[saveTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ManualItem> manualItemsToBeAdded = new LinkedList<ManualItem>();

        // only to be sure that same modifiedItem is not added more than once

        List<String> originItemUidsToBeRemoved = new LinkedList<String>();

        HashMap<String, ManualItem> changedModifiedItems = new HashMap<String, ManualItem>();

        for (TreeNode modifiedItemNode : root.getChildren()) {
            for (TreeNode manualImpactedNode : modifiedItemNode.getChildren()) {
                ManualImpactedItemsNodeData nodeData = (ManualImpactedItemsNodeData) manualImpactedNode.getData();

                processNodeChanges(nodeData, manualItemsToBeAdded,
                        originItemUidsToBeRemoved, changedModifiedItems);
            }
        }

        cmService.saveManualItemChangesInCMProcess(session, currentDoc,
                originItemUidsToBeRemoved, manualItemsToBeAdded,
                changedModifiedItems);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processNodeChanges(ManualImpactedItemsNodeData nodeData,
            List<ManualItem> manualItemsToBeAdded,
            List<String> originItemUidsToBeRemoved,
            HashMap<String, ManualItem> changedManualItems) {

        String originItemUid = nodeData.getOriginItem().getId();

        List<String> originItemUidsToBeAdded = new ArrayList<String>();

        if (nodeData.getIsNew()) {
            if (!originItemUidsToBeAdded.contains(originItemUid)) {
                originItemUidsToBeAdded.add(originItemUid);
                ManualItem newManualItem = getManualItemFromNodeData(nodeData);
                manualItemsToBeAdded.add(newManualItem);
            }
        } else if (nodeData.getIsRemoved()) {
            originItemUidsToBeRemoved.add(originItemUid);
        } else if (nodeData.getIsModified()) {
            ManualItem changedManualItem = getManualItemFromNodeData(nodeData);
            changedManualItems.put(originItemUid, changedManualItem);
        }
    }

    private ManualItem getManualItemFromNodeData(
            ManualImpactedItemsNodeData nodeData) {

        String originItemUid = null;
        if (nodeData.getOriginItem() != null) {
            originItemUid = nodeData.getOriginItem().getId();
        }

        ManualItem manualItem = new ManualItem(nodeData.getRowNumber(),
                nodeData.getNodeId(), nodeData.getModifiedItemId(),
                originItemUid, nodeData.getAction(), nodeData.getType(),
                nodeData.getComment());

        return manualItem;
    }

    private List<ImpactedItem> getImpactedItemsListFromTree(TreeNode node) {
        List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

        for (TreeNode childNode : node.getChildren()) {
            ManualImpactedItemsNodeData nodeData = (ManualImpactedItemsNodeData) childNode.getData();
            // Do not take into account modified items. They are only displayed
            // in the tree for information.
            if (nodeData.getIsModifiedItem()) {
                impactedItems.addAll(getImpactedItemsListFromTree(childNode));
            } else {
                // ImpactedItem impactedItem = nodeData.convertToImpactedItem();
                // impactedItems.add(impactedItem);
            }
        }
        return impactedItems;
    }

    public void refreshNode(DocumentModel cmProcess, TreeNode node,
            String trigger, List<TreeNode> actionChangeTreeNodeList)
            throws EloraException {

        String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- trigger = |" + trigger + "|");

        ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) node.getData();

        if (trigger.equals(
                CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)) {

            CMTreeBeanHelper.loadDestinationVersions(nodeData, session,
                    messages);

        } else {
            nodeData.setIsModified(true);
            nodeData.setIsUpdated(true);

            if (trigger.equals(CMConstants.TRIGGER_FIELD_ACTION)) {
                String action = nodeData.getAction();
                log.trace(logInitMsg + "action = |" + action + "|");

                // action cannot be null. If it is null, we will change to the
                // default action (CHANGE)
                if (action == null) {
                    action = CMConstants.ACTION_CHANGE;
                    nodeData.setAction(action);
                    log.trace(logInitMsg + "action changed to = |" + action
                            + "|");
                }

                // If action is ignore, destination will be empty and the
                // element is marked as managed.
                if (action.equals(CMConstants.ACTION_IGNORE)) {
                    nodeData.setDestinationItem(null);
                    nodeData.setDestinationItemVersionList(null);
                    nodeData.setDestinationItemWc(null);
                    nodeData.setDestinationItemSourceVersion(null);
                    nodeData.setDestinationItemSourceVersionList(null);
                    nodeData.setDestinationItemVersionIsReadOnly(true);
                    nodeData.setDestinationItemSourceVersionIsReadOnly(true);
                    nodeData.setIsManaged(true);
                    nodeData.setIsManagedIsReadOnly(true);
                }
                if (action.equals(CMConstants.ACTION_CHANGE)
                        || action.equals(CMConstants.ACTION_OVERWRITE)) {
                    // set as destinationItem and destinationItemWc the originWC
                    DocumentModel destinationItem = nodeData.getOriginItemWc();
                    nodeData.setDestinationItem(destinationItem);
                    nodeData.setDestinationItemWc(destinationItem);
                    List<SelectItem> destinationItemVersionList = new ArrayList<SelectItem>();
                    destinationItemVersionList.add(new SelectItem(
                            destinationItem,
                            destinationItem.getVersionLabel() + " (WC)"));
                    nodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    CMHelper.setDestinationItemSourceVersionInformation(
                            nodeData, messages);
                    nodeData.setDestinationItemVersionIsReadOnly(false);
                    boolean isManaged = false;
                    boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                            .calculateDestinationItemSourceVersionIsReadOnlyValue(
                                    destinationItem, isManaged);
                    nodeData.setDestinationItemSourceVersionIsReadOnly(
                            destinationItemSourceVersionIsReadOnly);
                    nodeData.setIsManaged(isManaged);
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, nodeData.getOriginItemWc());
                    nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                }

                actionChangeTreeNodeList.add(node);

                String comment = CMTreeBeanHelper.calculateComment(cmProcess,
                        action);
                nodeData.setComment(comment);
                refreshChildNodes(node, action, comment,
                        actionChangeTreeNodeList);

            } else if (trigger.equals(CMConstants.TRIGGER_FIELD_IS_MANAGED)) {

                CMTreeBeanHelper.processRefreshNodeTriggeredByIsManaged(
                        nodeData, session, messages);

            } else if (trigger.equals(
                    CMConstants.TRIGGER_FIELD_DESTINATION_ITEM)) {

                CMTreeBeanHelper.processRefreshNodeTriggeredByDestinationItem(
                        nodeData, session);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void refreshChildNodes(TreeNode node, String action, String comment,
            List<TreeNode> actionChangeTreeNodeList) throws EloraException {

        if (action.equals(CMConstants.ACTION_IGNORE)) {

            // Set the following for each child:
            // -- action = IGNORE and readOnly
            // -- destinationItem = null
            // -- isManaged = true and readOnly
            // -- comment = ignored since ancestor's action is ignore
            for (TreeNode childNode : node.getChildren()) {
                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                if (childNodeData.getIsFromMbom()
                        && childNodeData.getOriginItem() == null) {
                    continue;
                }

                if (EloraDocumentHelper.isBasicDocument(
                        childNodeData.getOriginItem())
                        && !action.equals(CMConstants.ACTION_IGNORE)) {
                    continue;
                }

                // If childNode is already MANAGED and its action is NOT IGNORE,
                // do not change it.
                if (childNodeData.getIsManaged()
                        && !childNodeData.getAction().equals(
                                CMConstants.ACTION_IGNORE)) {
                    // nothing to do
                } else {
                    actionChangeTreeNodeList.add(node);

                    boolean actionIsReadOnly = true;
                    // EXCEPTION: if this child node is a CadDrawing,
                    // action should always be editable
                    if (childNodeData.getOriginItem() != null
                            && childNodeData.getOriginItem().getType().equals(
                                    EloraDoctypeConstants.CAD_DRAWING)) {
                        actionIsReadOnly = false;
                    }

                    childNodeData.setIsModified(true);
                    childNodeData.setIsUpdated(true);
                    childNodeData.setAction(CMConstants.ACTION_IGNORE);
                    childNodeData.setActionIsReadOnly(actionIsReadOnly);
                    childNodeData.setDestinationItem(null);
                    childNodeData.setDestinationItemVersionList(null);
                    childNodeData.setDestinationItemWc(null);
                    childNodeData.setDestinationItemSourceVersion(null);
                    childNodeData.setDestinationItemSourceVersionList(null);
                    childNodeData.setDestinationItemVersionIsReadOnly(true);
                    childNodeData.setDestinationItemSourceVersionIsReadOnly(
                            true);
                    childNodeData.setIsManaged(true);
                    childNodeData.setIsManagedIsReadOnly(true);
                    childNodeData.setComment(
                            CMConstants.COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE);
                }

                refreshChildNodes(childNode, action, comment,
                        actionChangeTreeNodeList);
            }
        } else if (action.equals(CMConstants.ACTION_CHANGE)
                || action.equals(CMConstants.ACTION_OVERWRITE)) {
            // -- action = CHANGE and NOT readOnly
            // -- destinationItem = WC
            // -- isManaged = false and NOT readOnly
            for (TreeNode childNode : node.getChildren()) {
                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                if (childNodeData.getIsFromMbom()
                        && childNodeData.getOriginItem() == null) {
                    continue;
                }

                if (EloraDocumentHelper.isBasicDocument(
                        childNodeData.getOriginItem())) {
                    continue;
                }

                // If childNode is already MANAGED and its action is NOT IGNORE,
                // do not change it.
                if (childNodeData.getIsManaged()
                        && !childNodeData.getAction().equals(
                                CMConstants.ACTION_IGNORE)) {
                    // nothing to do
                } else {
                    actionChangeTreeNodeList.add(childNode);

                    childNodeData.setIsModified(true);
                    childNodeData.setIsUpdated(true);
                    childNodeData.setAction(action);
                    childNodeData.setActionIsReadOnly(false);
                    DocumentModel destinationItem = childNodeData.getOriginItemWc();
                    childNodeData.setDestinationItem(destinationItem);
                    childNodeData.setDestinationItemWc(destinationItem);
                    List<SelectItem> destinationItemVersionList = new ArrayList<SelectItem>();
                    destinationItemVersionList.add(new SelectItem(
                            destinationItem,
                            destinationItem.getVersionLabel() + " (WC)"));
                    childNodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    CMHelper.setDestinationItemSourceVersionInformation(
                            childNodeData, messages);
                    childNodeData.setDestinationItemVersionIsReadOnly(false);
                    boolean isManaged = false;
                    boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                            .calculateDestinationItemSourceVersionIsReadOnlyValue(
                                    destinationItem, isManaged);
                    childNodeData.setDestinationItemSourceVersionIsReadOnly(
                            destinationItemSourceVersionIsReadOnly);
                    childNodeData.setIsManaged(isManaged);
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, childNodeData.getOriginItemWc());
                    childNodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                    childNodeData.setComment(comment);
                }

                refreshChildNodes(childNode, action, comment,
                        actionChangeTreeNodeList);
            }
        }
    }

    public boolean checkModifiedIsInComposition(String modifiedItemUid,
            DocumentModel impactedItemDoc, ArrayList<String> impactRelations) {

        for (String relation : impactRelations) {
            Resource predicateResource = new ResourceImpl(relation);
            List<Statement> stmts = RelationHelper.getStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, impactedItemDoc,
                    predicateResource);
            for (Statement stmt : stmts) {
                DocumentModel objectDoc = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);
                if (objectDoc.getId().equals(modifiedItemUid)) {
                    return true;
                }
                if (checkModifiedIsInComposition(modifiedItemUid, objectDoc,
                        impactRelations)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TreeNode addManualImpactedItemNode(TreeNode root,
            String modifiedItemUid, DocumentModel impactedItemDoc,
            DocumentModel impactedItemDocWc, String impactItemAction,
            boolean lockManualImpacted, String comment) {

        String logInitMsg = "[addManualImpactedItemNode] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String currentNodeId = CMHelper.generateNodeId(impactedItemDoc.getId());

        String type = CMHelper.getItemType(impactedItemDoc);

        ManualImpactedItemsNodeData newNodeData = new ManualImpactedItemsNodeData(
                String.valueOf(nodeId), 2, true, false, false, null,
                currentNodeId, modifiedItemUid, false, impactedItemDoc,
                impactedItemDocWc, impactItemAction, false, type, comment);

        nodeId++;

        TreeNode modifiedNode = modifiedNodeMap.get(modifiedItemUid);

        TreeNode node = new DefaultTreeNode(newNodeData, modifiedNode);
        node.setExpanded(true);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public String validateManualImpactedItem(TreeNode root,
            String modifiedItemId, String newManualImpactedItemId) {

        String logInitMsg = "[validateManualImpactedItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = "";

        for (TreeNode modifiedItemNode : root.getChildren()) {
            for (TreeNode manualImpactedNode : modifiedItemNode.getChildren()) {
                ManualImpactedItemsNodeData nodeData = (ManualImpactedItemsNodeData) manualImpactedNode.getData();

                String nodeDataOriginItemId = nodeData.getOriginItem().getId();
                String nodeDataModifiedItemId = nodeData.getModifiedItemId();

                if (nodeDataOriginItemId.equals(newManualImpactedItemId)
                        && modifiedItemId.equals(nodeDataModifiedItemId)) {
                    validationResultMessage = "eloraplm.message.error.cm.manualItemAlreadyExist";
                    return validationResultMessage;
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return validationResultMessage;
    }

    public void refreshNode(TreeNode node, String trigger)
            throws EloraException {

        String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- trigger = |" + trigger + "|");

        ManualImpactedItemsNodeData nodeData = (ManualImpactedItemsNodeData) node.getData();

        nodeData.setIsModified(true);
        // nodeData.setIsUpdated(true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

}
