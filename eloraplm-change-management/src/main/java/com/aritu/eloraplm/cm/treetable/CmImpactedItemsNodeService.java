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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDocTypeConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class CmImpactedItemsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            CmImpactedItemsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected String itemType;

    EloraConfigTable releasedStatesConfig;

    public CmImpactedItemsNodeService(CoreSession session, String itemType)
            throws EloraException {
        this.session = session;
        this.itemType = itemType;
        nodeId = 0;
        releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
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

        if (!(currentDoc.getType().equals(CMDocTypeConstants.CM_ECR)
                || currentDoc.getType().equals(CMDocTypeConstants.CM_ECO))) {
            throw new EloraException(
                    "First level document of the tree must be an ECR or an ECO.");
        }

        int level = 0;
        CmImpactedItemsNodeData nodeData = new CmImpactedItemsNodeData(
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
                            currentDoc.getId());
                    it = session.queryAndFetch(query, NXQL.NXQL);

                    if (it.size() > 0) {
                        for (Map<String, Serializable> map : it) {

                            String originItemUid = (String) map.get(
                                    CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                            + "/*1/originItem");

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

                                    long rowNumber = (long) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/rowNumber");
                                    /*String originItemUid = (String) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/originItem");*/
                                    String originItemWcUid = (String) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/originItemWc");
                                    String action = (String) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/action");
                                    String destinationItemUid = (String) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/destinationItem");
                                    boolean isManaged = (boolean) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/isManaged");
                                    String type = (String) map.get(
                                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                                    + "/*1/type");

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

                                    CmImpactedItemsNodeData nodeData = new CmImpactedItemsNodeData(
                                            String.valueOf(nodeId), level,
                                            rowNumber, true, originItem,
                                            originItem, originItem,
                                            originItemWc, action, true,
                                            destinationItem, isManaged, true,
                                            false, type, type, "");

                                    nodeId++;

                                    TreeNode node = new DefaultTreeNode(
                                            nodeData, root);
                                    node.setExpanded(true);

                                    level++;
                                    root = createImpactedItemsChildNodesTree(
                                            currentDoc, itemType, originItemUid,
                                            originItemUid, root, node, level);
                                }
                            }
                            countMIIt.close();
                        }
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

    // TODO: HAU ALDATU
    private TreeNode createImpactedItemsChildNodesTree(DocumentModel currentDoc,
            String itemType, String modifiedItemUid, String parentItemUid,
            TreeNode root, TreeNode parentNode, int level)
            throws EloraException {

        String logInitMsg = "[createImpactedItemsChildNodesTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getImpactedItemsByParentQuery(
                    currentDoc.getId(), itemType, modifiedItemUid,
                    parentItemUid);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = "";
                if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                    pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
                } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                    pfx = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
                }

                for (Map<String, Serializable> map : it) {
                    long rowNumber = (long) map.get(pfx + "/*1/rowNumber");
                    // TODO:: modifiedItemUid eta parentItemUid rekuperatzia
                    // soberan dago
                    /*modifiedItemUid = (String) map.get(
                            pfx + "/*1/modifiedItem");
                    parentItemUid = (String) map.get(pfx + "/*1/parentItem");*/
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");
                    String originItemWcUid = (String) map.get(
                            pfx + "/*1/originItemWc");
                    String action = (String) map.get(pfx + "/*1/action");
                    String destinationItemUid = (String) map.get(
                            pfx + "/*1/destinationItem");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    boolean isManual = (boolean) map.get(pfx + "/*1/isManual");
                    String type = (String) map.get(pfx + "/*1/type");
                    String messageType = (String) map.get(
                            pfx + "/*1/messageType");
                    String messageData = (String) map.get(
                            pfx + "/*1/messageData");

                    DocumentModel modifiedItem = null;
                    if (modifiedItemUid != null) {
                        modifiedItem = session.getDocument(
                                new IdRef(modifiedItemUid));
                    }
                    DocumentModel parentItem = null;
                    if (parentItemUid != null) {
                        parentItem = session.getDocument(
                                new IdRef(parentItemUid));
                    }
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

                    // Calculate if editable fields are editable or not in
                    // function of the item values
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, destinationItem, releasedStatesConfig);

                    boolean actionIsReadOnly = CMTreeBeanHelper.calculatedImpactedItemActionIsReadOnlyValue(
                            action, parentNode);

                    CmImpactedItemsNodeData nodeData = new CmImpactedItemsNodeData(
                            String.valueOf(nodeId), level, rowNumber, false,
                            modifiedItem, parentItem, originItem, originItemWc,
                            action, actionIsReadOnly, destinationItem,
                            isManaged, isManagedIsReadOnly, isManual, type,
                            messageType, messageData);

                    nodeId++;

                    TreeNode node = new DefaultTreeNode(nodeData, parentNode);
                    node.setExpanded(true);

                    level++;
                    root = createImpactedItemsChildNodesTree(currentDoc,
                            itemType, modifiedItemUid, originItemUid, root,
                            node, level);
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

        return root;
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root)
            throws EloraException {

        // TODO::: gehitu try/catch

        String logInitMsg = "[saveTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ImpactedItem> impactedItems = getImpactedItemsListFromTree(root);

        CMHelper.saveImpactedItemListInCMProcess(session, currentDoc, itemType,
                impactedItems);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    // TODO:: TO BE MODIFIED
    private List<ImpactedItem> getImpactedItemsListFromTree(TreeNode node) {

        List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

        for (TreeNode childNode : node.getChildren()) {
            CmImpactedItemsNodeData nodeData = (CmImpactedItemsNodeData) childNode.getData();

            // Do not take into account modified items. They are only displayed
            // in the tree for information.
            if (!nodeData.getIsModifiedItem()) {

                String modifiedItemUid = null;
                if (nodeData.getModifiedItem() != null) {
                    modifiedItemUid = nodeData.getModifiedItem().getId();
                }

                String parentItemUid = null;
                if (nodeData.getParentItem() != null) {
                    parentItemUid = nodeData.getParentItem().getId();
                }

                String originItemUid = null;
                if (nodeData.getOriginItem() != null) {
                    originItemUid = nodeData.getOriginItem().getId();
                }

                String originItemWcUid = null;
                if (nodeData.getOriginItemWc() != null) {
                    originItemWcUid = nodeData.getOriginItemWc().getId();
                }

                String destinationItemUid = null;
                if (nodeData.getDestinationItem() != null) {
                    destinationItemUid = nodeData.getDestinationItem().getId();
                }

                ImpactedItem impactedItem = new ImpactedItem(
                        nodeData.getRowNumber(), modifiedItemUid, parentItemUid,
                        originItemUid, originItemWcUid, nodeData.getAction(),
                        destinationItemUid, nodeData.getIsManaged(),
                        nodeData.getIsManual(), nodeData.getType(),
                        nodeData.getMessageType(), nodeData.getMessageData());

                impactedItems.add(impactedItem);
            }

            impactedItems.addAll(getImpactedItemsListFromTree(childNode));

        }

        return impactedItems;
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {

        String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        CmImpactedItemsNodeData nodeData = (CmImpactedItemsNodeData) node.getData();
        nodeData.setIsModified(true);

        // log.trace(logInitMsg + "triggeredField = |" + triggeredField + "|");

        if (triggeredField.equals(CMConstants.TRIGGER_FIELD_ACTION)) {
            String action = nodeData.getAction();
            log.trace(logInitMsg + "action = |" + action + "|");

            // action cannot be null. If it is null, we will change to the
            // default action (CHANGE)
            if (action == null) {
                action = CMConstants.ACTION_CHANGE;
                nodeData.setAction(action);
                log.trace(logInitMsg + "action changed to = |" + action + "|");
            }

            // TODO:: komentarioak jarri
            if (action.equals(CMConstants.ACTION_IGNORE)) {
                nodeData.setDestinationItem(null);
                nodeData.setIsManaged(true);
                nodeData.setIsManagedIsReadOnly(true);

                refreshChildNodes(node, action);
            }
            if (action.equals(CMConstants.ACTION_CHANGE)) {
                nodeData.setDestinationItem(nodeData.getOriginItemWc());
                nodeData.setIsManaged(false);
                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                        action, nodeData.getOriginItemWc(),
                        releasedStatesConfig);
                nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);

                refreshChildNodes(node, action);
            }
        } else if (triggeredField.equals(
                CMConstants.TRIGGER_FIELD_IS_MANAGED)) {

            CMTreeBeanHelper.processRefreshNodeTriggeredByIsManaged(nodeData,
                    session);

        }

        // log.trace(logInitMsg + "--- EXIT ---");
    }

    private void refreshChildNodes(TreeNode node, String action) {

        if (action.equals(CMConstants.ACTION_IGNORE)) {

            // Set the following for each children:
            // -- action = IGNORE and readOnly
            // -- destinationItem = null
            // -- isManaged = true and readOnly
            for (TreeNode childNode : node.getChildren()) {
                CmImpactedItemsNodeData childNodeData = (CmImpactedItemsNodeData) childNode.getData();
                childNodeData.setIsModified(true);
                childNodeData.setAction(CMConstants.ACTION_IGNORE);
                childNodeData.setActionIsReadOnly(true);
                childNodeData.setDestinationItem(null);
                childNodeData.setIsManaged(true);
                childNodeData.setIsManagedIsReadOnly(true);
                refreshChildNodes(childNode, action);
            }
        }

        if (action.equals(CMConstants.ACTION_CHANGE)) {

            // Set the following for each children:
            // -- action = CHANGE and NOT readOnly
            // -- destinationItem = WC
            // -- isManaged = false and NOT readOnly
            for (TreeNode childNode : node.getChildren()) {
                CmImpactedItemsNodeData childNodeData = (CmImpactedItemsNodeData) childNode.getData();
                childNodeData.setIsModified(true);
                childNodeData.setAction(CMConstants.ACTION_CHANGE);
                childNodeData.setActionIsReadOnly(false);
                childNodeData.setDestinationItem(
                        childNodeData.getOriginItemWc());
                childNodeData.setIsManaged(false);
                boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                        action, childNodeData.getOriginItemWc(),
                        releasedStatesConfig);
                childNodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
                refreshChildNodes(childNode, action);
            }
        }
    }

}
