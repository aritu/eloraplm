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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class ImpactedItemsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            ImpactedItemsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected String itemType;

    public ImpactedItemsNodeService(CoreSession session, String itemType)
            throws EloraException {
        this.session = session;
        this.itemType = itemType;
        nodeId = 0;
    }

    /* (non-Javadoc)
     * @see com.aritu.eloraplm.treetable.NodeService#getRoot(java.lang.Object)
     */
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
        ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createImpactedItemsRootTree(currentDoc, itemType, root, level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createImpactedItemsRootTree(DocumentModel currentDoc,
            String itemType, TreeNode root, int level) throws EloraException {

        String logInitMsg = "[createImpactedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult countIt = null;
        IterableQueryResult it = null;
        IterableQueryResult countMIIt = null;
        try {

            // First, check if this process has any impacted items. If yes,
            // search modified items and then their impacted ones for creating
            // the tree.
            String query = CMQueryFactory.getCountImpactedItemsQuery(
                    currentDoc.getId(), itemType);
            countIt = session.queryAndFetch(query, NXQL.NXQL);

            if (countIt.iterator().hasNext()) {
                Map<String, Serializable> cuntMap = countIt.iterator().next();
                String resultCountStr = cuntMap.get(
                        "COUNT(" + NXQL.ECM_UUID + ")").toString();
                int resultCount = Integer.valueOf(resultCountStr);

                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");

                if (resultCount > 0) {
                    query = CMQueryFactory.getModifiedItemsQuery(
                            currentDoc.getId(), itemType);
                    it = session.queryAndFetch(query, NXQL.NXQL);

                    if (it.size() > 0) {

                        String pfx = CMHelper.getModifiedItemListMetadaName(
                                itemType);

                        for (Map<String, Serializable> map : it) {

                            String originItemUid = (String) map.get(
                                    pfx + "/*1/originItem");

                            // Verify that this modified item has any impacted
                            // item
                            query = CMQueryFactory.getCountImpactedItemsByModifiedItemQuery(
                                    currentDoc.getId(), itemType,
                                    originItemUid);
                            countMIIt = session.queryAndFetch(query, NXQL.NXQL);

                            if (countMIIt.iterator().hasNext()) {
                                Map<String, Serializable> countMIMap = countMIIt.iterator().next();
                                String resultcountMIStr = countMIMap.get(
                                        "COUNT(" + NXQL.ECM_UUID
                                                + ")").toString();
                                int resultcountMI = Integer.valueOf(
                                        resultcountMIStr);

                                log.trace(logInitMsg
                                        + "DB query resultcountMI = |"
                                        + resultcountMI + "|");

                                if (resultcountMI > 0) {
                                    Long rowNumber = (Long) map.get(
                                            pfx + "/*1/rowNumber");
                                    String currentNodeId = (String) map.get(
                                            pfx + "/*1/nodeId");
                                    String parentNodeId = (String) map.get(
                                            pfx + "/*1/parentNodeId");
                                    String originItemWcUid = (String) map.get(
                                            pfx + "/*1/originItemWc");
                                    String action = (String) map.get(
                                            pfx + "/*1/action");
                                    String destinationItemUid = (String) map.get(
                                            pfx + "/*1/destinationItem");
                                    String destinationItemWcUid = (String) map.get(
                                            pfx + "/*1/destinationItemWc");
                                    boolean isManaged = (boolean) map.get(
                                            pfx + "/*1/isManaged");
                                    String type = (String) map.get(
                                            pfx + "/*1/type");
                                    String comment = (String) map.get(
                                            pfx + "/*1/comment");
                                    boolean isUpdated = (boolean) map.get(
                                            pfx + "/*1/isUpdated");

                                    DocumentModel originItem = null;
                                    if (originItemUid != null) {
                                        originItem = session.getDocument(
                                                new IdRef(originItemUid));
                                    }
                                    DocumentModel originItemWc = null;
                                    if (originItemWcUid != null) {
                                        originItemWc = session.getDocument(
                                                new IdRef(originItemWcUid));
                                    }
                                    DocumentModel destinationItem = null;
                                    if (destinationItemUid != null) {
                                        destinationItem = session.getDocument(
                                                new IdRef(destinationItemUid));
                                    }
                                    DocumentModel destinationItemWc = null;
                                    if (destinationItemWcUid != null) {
                                        destinationItemWc = session.getDocument(
                                                new IdRef(
                                                        destinationItemWcUid));
                                    }

                                    ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                                            String.valueOf(nodeId), level,
                                            rowNumber, currentNodeId,
                                            parentNodeId, true, originItem,
                                            null, originItem, originItemWc, "",
                                            "", false, false, action, true,
                                            destinationItem, destinationItemWc,
                                            true, isManaged, true, false, type,
                                            comment, true, isUpdated);

                                    nodeId++;

                                    TreeNode node = new DefaultTreeNode(
                                            nodeData, root);
                                    node.setExpanded(true);

                                    level++;
                                    root = createImpactedItemsChildNodesTree(
                                            currentDoc, itemType, originItemUid,
                                            action, destinationItemWcUid,
                                            currentNodeId, root, node, level);
                                }
                            }
                            countMIIt.close();
                        }
                    }
                }
            }
        } catch (DocumentNotFoundException | DocumentSecurityException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw (e);
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (countMIIt != null) {
                countMIIt.close();
            }
            if (countIt != null) {
                countIt.close();
            }
            if (it != null) {
                it.close();
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createImpactedItemsChildNodesTree(DocumentModel currentDoc,
            String itemType, String modifiedItemUid, String modifiedItemAction,
            String modifiedItemDestinationWcUid, String modifiedItemNodeId,
            TreeNode root, TreeNode parentNode, int level)
            throws EloraException {

        String logInitMsg = "[createImpactedItemsChildNodesTree] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // First, retrieve the impacted items of this modified item and put all
        // them in a HashMap for then creating the tree.
        // The HashMap contains: for each parentNodeId, its related
        // impactedItems ordered by rowNumber
        HashMap<String, ArrayList<ImpactedItem>> impactedItems = retrieveImpactedItemsByModifiedItem(
                currentDoc.getId(), itemType, modifiedItemUid);

        // Create the tree in function of the retrieved structure.
        // First parent is the modified item itself.
        log.trace(logInitMsg + "START filling the tree");
        fillTreeWithImpactedItems(root, parentNode, level, impactedItems,
                modifiedItemNodeId, modifiedItemAction, modifiedItemAction,
                modifiedItemDestinationWcUid);
        log.trace(logInitMsg + "END filling the tree");

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private HashMap<String, ArrayList<ImpactedItem>> retrieveImpactedItemsByModifiedItem(
            String cmProcessUid, String itemType, String modifiedItemUid)
            throws EloraException {

        String logInitMsg = "[retrieveImpactedItemsByModifiedItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        HashMap<String, ArrayList<ImpactedItem>> impactedItems = new HashMap<String, ArrayList<ImpactedItem>>();

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getImpactedItemsByModifiedQuery(
                    cmProcessUid, itemType, modifiedItemUid);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = CMHelper.getImpactedItemListMetadaName(itemType);

                for (Map<String, Serializable> map : it) {
                    Long rowNumber = (Long) map.get(pfx + "/*1/rowNumber");
                    String currentNodeId = (String) map.get(pfx + "/*1/nodeId");
                    String parentNodeId = (String) map.get(
                            pfx + "/*1/parentNodeId");
                    String parentItemUid = (String) map.get(
                            pfx + "/*1/parentItem");
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");
                    String originItemWcUid = (String) map.get(
                            pfx + "/*1/originItemWc");
                    String predicate = (String) map.get(pfx + "/*1/predicate");
                    String quantity = (String) map.get(pfx + "/*1/quantity");
                    boolean isAnarchic = (boolean) map.get(
                            pfx + "/*1/isAnarchic");
                    boolean isDirectObject = (boolean) map.get(
                            pfx + "/*1/isDirectObject");
                    String action = (String) map.get(pfx + "/*1/action");
                    String destinationItemUid = (String) map.get(
                            pfx + "/*1/destinationItem");
                    String destinationItemWcUid = (String) map.get(
                            pfx + "/*1/destinationItemWc");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    boolean isManual = (boolean) map.get(pfx + "/*1/isManual");
                    String type = (String) map.get(pfx + "/*1/type");
                    String comment = (String) map.get(pfx + "/*1/comment");
                    boolean isUpdated = (boolean) map.get(
                            pfx + "/*1/isUpdated");

                    ImpactedItem impactedItem = new ImpactedItem(rowNumber,
                            currentNodeId, parentNodeId, modifiedItemUid,
                            parentItemUid, originItemUid, originItemWcUid,
                            predicate, quantity, isAnarchic, isDirectObject,
                            action, destinationItemUid, destinationItemWcUid,
                            isManaged, isManual, type, comment, isUpdated);

                    if (impactedItems.containsKey(parentNodeId)) {
                        impactedItems.get(parentNodeId).add(impactedItem);
                    } else {
                        ArrayList<ImpactedItem> impactedItemListByParentNodeId = new ArrayList<ImpactedItem>();
                        impactedItemListByParentNodeId.add(impactedItem);
                        impactedItems.put(parentNodeId,
                                impactedItemListByParentNodeId);
                    }
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            it.close();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return impactedItems;
    }

    private TreeNode fillTreeWithImpactedItems(TreeNode root,
            TreeNode parentNode, int level,
            HashMap<String, ArrayList<ImpactedItem>> impactedItems,
            String parentNodeId, String parentItemAction,
            String modifiedItemAction, String modifiedItemDestinationWcUid) {

        String logInitMsg = "[fillTreeWithImpactedItems] ["
                + session.getPrincipal().getName() + "] ";

        if (impactedItems.containsKey(parentNodeId)) {

            ArrayList<ImpactedItem> impactedItemListByParentNodeId = impactedItems.get(
                    parentNodeId);

            for (ImpactedItem impactedItem : impactedItemListByParentNodeId) {

                DocumentModel modifiedItem = null;
                if (impactedItem.getModifiedItem() != null) {
                    modifiedItem = session.getDocument(
                            new IdRef(impactedItem.getModifiedItem()));
                }
                DocumentModel parentItem = null;
                if (impactedItem.getParentItem() != null) {
                    parentItem = session.getDocument(
                            new IdRef(impactedItem.getParentItem()));
                }
                DocumentModel originItem = null;
                String originItemType = null;
                if (impactedItem.getOriginItem() != null) {
                    originItem = session.getDocument(
                            new IdRef(impactedItem.getOriginItem()));
                    if (originItem != null) {
                        originItemType = originItem.getType();
                    }
                }
                DocumentModel originItemWc = null;
                if (impactedItem.getOriginItemWc() != null) {
                    originItemWc = session.getDocument(
                            new IdRef(impactedItem.getOriginItemWc()));
                }
                DocumentModel destinationItem = null;
                if (impactedItem.getDestinationItem() != null) {
                    destinationItem = session.getDocument(
                            new IdRef(impactedItem.getDestinationItem()));
                }

                DocumentModel destinationItemWc = null;
                if (impactedItem.getDestinationItemWc() != null) {
                    destinationItemWc = session.getDocument(
                            new IdRef(impactedItem.getDestinationItemWc()));
                }

                // Calculate if editable fields are editable or not in
                // function of the item values
                boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper.calculateDestinationItemVersionListIsReadOnlyValue(
                        impactedItem.getAction(), impactedItem.isManaged());

                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                        impactedItem.getAction(), destinationItem);

                boolean actionIsReadOnly = CMTreeBeanHelper.calculatedImpactedItemActionIsReadOnlyValue(
                        impactedItem.getAction(),
                        impactedItem.getOriginItemWc(), originItemType,
                        modifiedItemAction, modifiedItemDestinationWcUid,
                        parentItemAction);

                ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                        String.valueOf(nodeId), level,
                        impactedItem.getRowNumber(), impactedItem.getNodeId(),
                        impactedItem.getParentNodeId(), false, modifiedItem,
                        parentItem, originItem, originItemWc,
                        impactedItem.getPredicate(), impactedItem.getQuantity(),
                        impactedItem.isAnarchic(),
                        impactedItem.isDirectObject(), impactedItem.getAction(),
                        actionIsReadOnly, destinationItem, destinationItemWc,
                        destinationItemVersionIsReadOnly,
                        impactedItem.isManaged(), isManagedIsReadOnly,
                        impactedItem.isManual(), impactedItem.getType(),
                        impactedItem.getComment(), false,
                        impactedItem.isUpdated());

                nodeId++;

                TreeNode node = new DefaultTreeNode(nodeData, parentNode);
                node.setExpanded(true);

                level++;

                // recursive call
                root = fillTreeWithImpactedItems(root, node, level,
                        impactedItems, impactedItem.getNodeId(),
                        impactedItem.getAction(), modifiedItemAction,
                        modifiedItemDestinationWcUid);
            }
        }
        return root;
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root)
            throws EloraException {

        String logInitMsg = "[saveTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ImpactedItem> impactedItems = getImpactedItemsListFromTree(root);

        CMHelper.saveImpactedItemListInCMProcess(session, currentDoc, itemType,
                impactedItems);

    }

    private List<ImpactedItem> getImpactedItemsListFromTree(TreeNode node) {

        List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

        for (TreeNode childNode : node.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();

            // Do not take into account modified items. They are only displayed
            // in the tree for information.
            if (!nodeData.getIsModifiedItem()) {

                ImpactedItem impactedItem = nodeData.convertToImpactedItem();

                impactedItems.add(impactedItem);
            }

            impactedItems.addAll(getImpactedItemsListFromTree(childNode));
        }

        return impactedItems;
    }

    public void refreshNode(DocumentModel cmProcess, TreeNode node,
            String trigger) throws EloraException {

        String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- trigger = |" + trigger + "|");

        ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) node.getData();

        if (trigger.equals(
                CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)) {

            CMTreeBeanHelper.loadDestinationVersions(nodeData, session);

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
                    nodeData.setDestinationItemUid(null);
                    nodeData.setDestinationItemVersionList(null);
                    nodeData.setDestinationItemWc(null);
                    nodeData.setDestinationItemVersionIsReadOnly(true);
                    nodeData.setIsManaged(true);
                    nodeData.setIsManagedIsReadOnly(true);
                }
                if (action.equals(CMConstants.ACTION_CHANGE)) {
                    // set as destinationItem and destinationItemWc the originWC
                    DocumentModel destinationItem = nodeData.getOriginItemWc();
                    nodeData.setDestinationItem(destinationItem);
                    nodeData.setDestinationItemWc(destinationItem);
                    String destinationItemUid = destinationItem.getId();
                    nodeData.setDestinationItemUid(destinationItemUid);
                    Map<String, String> destinationItemVersionList = new HashMap<String, String>();
                    destinationItemVersionList.put(destinationItemUid,
                            destinationItem.getVersionLabel() + " (WC)");
                    nodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    nodeData.setDestinationItemVersionIsReadOnly(false);
                    nodeData.setIsManaged(false);
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, nodeData.getOriginItemWc());
                    nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                }

                String comment = CMTreeBeanHelper.calculateComment(cmProcess,
                        action);
                nodeData.setComment(comment);
                refreshChildNodes(node, action, comment);

            } else if (trigger.equals(CMConstants.TRIGGER_FIELD_IS_MANAGED)) {

                CMTreeBeanHelper.processRefreshNodeTriggeredByIsManaged(
                        nodeData, session);

            } else if (trigger.equals(
                    CMConstants.TRIGGER_FIELD_DESTINATION_ITEM_UID)) {

                CMTreeBeanHelper.processRefreshNodeTriggeredByDestinationItemUid(
                        nodeData, session);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void refreshChildNodes(TreeNode node, String action, String comment)
            throws EloraException {

        if (action.equals(CMConstants.ACTION_IGNORE)) {

            // Set the following for each child:
            // -- action = IGNORE and readOnly
            // -- destinationItem = null
            // -- isManaged = true and readOnly
            // -- comment = ignored since ancestor's action is ignore
            for (TreeNode childNode : node.getChildren()) {
                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                // If childNode is already MANAGED and its action is NOT IGNORE,
                // do not change it.
                if (childNodeData.getIsManaged()
                        && !childNodeData.getAction().equals(
                                CMConstants.ACTION_IGNORE)) {
                    // nothing to do
                } else {
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
                    childNodeData.setDestinationItemUid(null);
                    childNodeData.setDestinationItemVersionList(null);
                    childNodeData.setDestinationItemWc(null);
                    childNodeData.setDestinationItemVersionIsReadOnly(true);
                    childNodeData.setIsManaged(true);
                    childNodeData.setIsManagedIsReadOnly(true);
                    childNodeData.setComment(
                            CMConstants.COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE);
                }

                refreshChildNodes(childNode, action, comment);
            }
        }

        if (action.equals(CMConstants.ACTION_CHANGE)) {
            // -- action = CHANGE and NOT readOnly
            // -- destinationItem = WC
            // -- isManaged = false and NOT readOnly
            for (TreeNode childNode : node.getChildren()) {
                ImpactedItemsNodeData childNodeData = (ImpactedItemsNodeData) childNode.getData();

                // If childNode is already MANAGED and its action is NOT IGNORE,
                // do not change it.
                if (childNodeData.getIsManaged()
                        && !childNodeData.getAction().equals(
                                CMConstants.ACTION_IGNORE)) {
                    // nothing to do
                } else {
                    childNodeData.setIsModified(true);
                    childNodeData.setIsUpdated(true);
                    childNodeData.setAction(CMConstants.ACTION_CHANGE);
                    childNodeData.setActionIsReadOnly(false);
                    DocumentModel destinationItem = childNodeData.getOriginItemWc();
                    childNodeData.setDestinationItem(destinationItem);
                    childNodeData.setDestinationItemWc(destinationItem);
                    String destinationItemUid = destinationItem.getId();
                    childNodeData.setDestinationItemUid(destinationItemUid);
                    Map<String, String> destinationItemVersionList = new HashMap<String, String>();
                    destinationItemVersionList.put(destinationItemUid,
                            destinationItem.getVersionLabel() + " (WC)");
                    childNodeData.setDestinationItemVersionList(
                            destinationItemVersionList);
                    childNodeData.setDestinationItemVersionIsReadOnly(false);
                    childNodeData.setIsManaged(false);
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, childNodeData.getOriginItemWc());
                    childNodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                    childNodeData.setComment(comment);
                }

                refreshChildNodes(childNode, action, comment);
            }
        }
    }

}
