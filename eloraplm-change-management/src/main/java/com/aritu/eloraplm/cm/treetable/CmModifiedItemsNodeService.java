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
import java.util.HashMap;
import java.util.LinkedList;
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

import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
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
public class CmModifiedItemsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    EloraConfigTable releasedStatesConfig;

    public CmModifiedItemsNodeService(CoreSession session,
            EloraConfigTable releasedStatesConfig) throws EloraException {
        this.session = session;
        nodeId = 0;
        this.releasedStatesConfig = releasedStatesConfig;
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
        CmModifiedItemsNodeData nodeData = new CmModifiedItemsNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createModifiedItemsRootTree(currentDoc, root, level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createModifiedItemsRootTree(DocumentModel currentDoc,
            TreeNode root, int level) throws EloraException {

        String logInitMsg = "[createModifiedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getModifiedItemsQuery(
                    currentDoc.getId());
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {
                for (Map<String, Serializable> map : it) {
                    String originItemUid = (String) map.get(
                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                    + "/*1/originItem");
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
                    // Calculate if editable fields are editable or not in
                    // function of the item values
                    boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                            action, destinationItem, releasedStatesConfig);

                    CmModifiedItemsNodeData nodeData = new CmModifiedItemsNodeData(
                            String.valueOf(nodeId), level, false, false, false,
                            originItem, originItemWc, action, destinationItem,
                            isManaged, isManagedIsReadOnly, type);

                    nodeId++;

                    TreeNode node = new DefaultTreeNode(nodeData, root);
                    node.setExpanded(true);
                }

            }
        } catch (

        NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (

        Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            it.close();
        }
        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public TreeNode addNewData(TreeNode root, DocumentModel originItem,
            DocumentModel originItemWc, String action,
            DocumentModel destinationItem, boolean isManaged,
            String originItemType) {

        String logInitMsg = "[addNewData] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Calculate if editable fields are editable or not in
        // function of the item values
        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                action, destinationItem, releasedStatesConfig);

        CmModifiedItemsNodeData newNodeData = new CmModifiedItemsNodeData(
                String.valueOf(nodeId), 1, true, false, false, originItem,
                originItemWc, action, destinationItem, isManaged,
                isManagedIsReadOnly, originItemType);

        nodeId++;

        TreeNode node = new DefaultTreeNode(newNodeData, root);
        node.setExpanded(true);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public String validateModificationOriginValue(TreeNode root,
            String modifiedItemRealUid) {

        String logInitMsg = "[validateModificationOriginValue] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = "";

        for (TreeNode childNode : root.getChildren()) {
            CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) childNode.getData();

            String nodeDataOriginItemUid = nodeData.getOriginItem().getId();

            if (nodeDataOriginItemUid.equals(modifiedItemRealUid)) {
                validationResultMessage = "eloraplm.message.error.cm.modifiedItemAlreadyExist";
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return validationResultMessage;
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root)
            throws EloraException {

        String logInitMsg = "[saveTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO::: gehitu behar direnak egiaztatu ondo daudela (????)

        // TODO:: begiratu ze motatakoa sortu, ordena mantentzeko
        List<ModifiedItem> modifiedItemsToBeAdded = new LinkedList<ModifiedItem>();
        // only to be sure that same modifiedItem is not added more than once
        List<String> originItemUidsToBeAdded = new LinkedList<String>();

        // TODO:: begiratu ze motatakoa sortu, ordena mantentzeko
        List<String> originItemUidsToBeRemoved = new LinkedList<String>();

        HashMap<String, ModifiedItem> changedModifiedItems = new HashMap<String, ModifiedItem>();

        for (TreeNode childNode : root.getChildren()) {
            CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) childNode.getData();

            String nodeDataOriginItemUid = nodeData.getOriginItem().getId();

            if (nodeData.getIsNew()) {
                if (!originItemUidsToBeAdded.contains(nodeDataOriginItemUid)) {
                    originItemUidsToBeAdded.add(nodeDataOriginItemUid);
                    ModifiedItem newModifiedItem = getModifiedItemFromNodeData(
                            nodeData);
                    modifiedItemsToBeAdded.add(newModifiedItem);
                }
            } else if (nodeData.getIsRemoved()) {
                originItemUidsToBeRemoved.add(nodeDataOriginItemUid);
            } else if (nodeData.getIsModified()) {
                ModifiedItem changedModifiedItem = getModifiedItemFromNodeData(
                        nodeData);
                changedModifiedItems.put(nodeDataOriginItemUid,
                        changedModifiedItem);
            }
        }

        CMHelper.saveModifiedItemChangesInCMProcess(session, currentDoc,
                originItemUidsToBeRemoved, modifiedItemsToBeAdded,
                changedModifiedItems);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private ModifiedItem getModifiedItemFromNodeData(
            CmModifiedItemsNodeData nodeData) {

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

        ModifiedItem modifiedItem = new ModifiedItem(originItemUid,
                originItemWcUid, nodeData.getAction(), destinationItemUid,
                nodeData.getIsManaged(), nodeData.getType());

        return modifiedItem;
    }

    /* private List<ModifiedItem> getModifiedItemsListFromTree(TreeNode node) {
    
    List<ModifiedItem> modifiedItems = new ArrayList<ModifiedItem>();
    
    for (TreeNode childNode : node.getChildren()) {
        CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) childNode.getData();
    
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
    
        ModifiedItem modifiedItem = new ModifiedItem(
                nodeData.getRowNumber(), originItemUid, originItemWcUid,
                nodeData.getAction(), destinationItemUid,
                nodeData.getIsManaged(), nodeData.getType());
    
        modifiedItems.add(modifiedItem);
    
        modifiedItems.addAll(getModifiedItemsListFromTree(childNode));
    
    }
    
    return modifiedItems;
    }*/

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {

        /*String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";*/
        // log.trace(logInitMsg + "--- ENTER --- ");

        CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) node.getData();
        nodeData.setIsModified(true);

        // log.trace(logInitMsg + "triggeredField = |" + triggeredField + "|");

        if (triggeredField.equals(CMConstants.TRIGGER_FIELD_IS_MANAGED)) {

            CMTreeBeanHelper.processRefreshNodeTriggeredByIsManaged(nodeData,
                    session);
        }

        // log.trace(logInitMsg + "--- EXIT ---");
    }

    /*public void refreshNode(TreeNode node, String triggeredField)
        throws EloraException {
    String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
            + "] ";
    log.trace(logInitMsg + "--- ENTER --- ");
    
    CmModifiedItemsNodeData nodeData = (CmModifiedItemsNodeData) node.getData();
    
    log.trace(logInitMsg + "********** rowNumber = |"
            + nodeData.getRowNumber() + "|"); // TODO:: to be removed
    
    log.trace(logInitMsg + "triggeredField = |" + triggeredField + "|");
    
    if (triggeredField.equals(CMConstants.TRIGGER_FIELD_ACTION)) {
        String action = nodeData.getAction();
        log.trace(logInitMsg + "action = |" + action + "|");
    
        // action cannot be null. If it is null, we will change it to the
        // default action (CHANGE)
        if (action == null) {
            action = CMConstants.ACTION_CHANGE;
            nodeData.setAction(action);
            log.trace(logInitMsg + "action changed to = |" + action + "|");
        }
    
        // TODO:: komentarioak jarri
        if (action.equals(CMConstants.ACTION_REMOVE)) {
            nodeData.setDestinationItem(null);
            nodeData.setIsManaged(true);
            nodeData.setIsManagedIsReadOnly(true);
    
            // ---- Clear the properties for managing "Replace destination
            // item"
            nodeData.clearReplaceSelectionElements();
    
        } else if (action.equals(CMConstants.ACTION_CHANGE)) {
            nodeData.setDestinationItem(nodeData.getOriginItemWc());
            nodeData.setIsManaged(false);
    
            boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                    action, nodeData.getOriginItemWc(),
                    releasedStatesConfig);
    
            nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
    
            // ---- Clear the properties for managing "Replace destination
            // item"
            nodeData.clearReplaceSelectionElements();
    
        } else if (action.equals(CMConstants.ACTION_REPLACE)) {
            nodeData.setIsManaged(false);
            nodeData.setDestinationItem(null);
        }
    
    } else if (triggeredField.equals(
            CMConstants.TRIGGER_FIELD_DEST_ITEM_REAL_UID)) {
        String action = nodeData.getAction();
    
        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                action, nodeData.getDestinationItem(),
                releasedStatesConfig);
        nodeData.setIsManagedIsReadOnly(isManagedIsReadOnly);
    }
    
    log.trace(logInitMsg + "--- EXIT ---");
    }*/

}
