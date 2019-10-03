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
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class ModifiedItemsNodeService implements NodeManager {

    private static final Log log = LogFactory.getLog(
            ModifiedItemsNodeService.class);

    protected CoreSession session;

    protected int nodeId;

    protected String itemType;

    public ModifiedItemsNodeService(CoreSession session, String itemType)
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
        ModifiedItemsNodeData nodeData = new ModifiedItemsNodeData(
                String.valueOf(nodeId), level);
        nodeId++;

        TreeNode root = new DefaultTreeNode(nodeData, null);
        root.setExpanded(true);

        level++;

        root = createModifiedItemsRootTree(currentDoc, itemType, root, level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createModifiedItemsRootTree(DocumentModel currentDoc,
            String itemType, TreeNode root, int level) throws EloraException {

        String logInitMsg = "[createModifiedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getModifiedItemsQuery(
                    currentDoc.getId(), itemType);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = CMHelper.getModifiedItemListMetadaName(itemType);

                for (Map<String, Serializable> map : it) {
                    Long rowNumber = (Long) map.get(pfx + "/*1/rowNumber");
                    String currentNodeId = (String) map.get(pfx + "/*1/nodeId");
                    String parentNodeId = (String) map.get(
                            pfx + "/*1/parentNodeId");
                    String derivedFromUid = (String) map.get(
                            pfx + "/*1/derivedFrom");
                    String parentItemUid = (String) map.get(
                            pfx + "/*1/parentItem");
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");
                    String originItemWcUid = (String) map.get(
                            pfx + "/*1/originItemWc");
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
                    boolean includeInImpactMatrix = (boolean) map.get(
                            pfx + "/*1/includeInImpactMatrix");

                    try {
                        DocumentModel derivedFrom = null;
                        if (derivedFromUid != null) {
                            derivedFrom = session.getDocument(
                                    new IdRef(derivedFromUid));
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
                        DocumentModel destinationItemWc = null;
                        if (destinationItemWcUid != null) {
                            destinationItemWc = session.getDocument(
                                    new IdRef(destinationItemWcUid));
                        }
                        // Calculate if editable fields are editable or not in
                        // function of the item values
                        boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper.calculateDestinationItemVersionListIsReadOnlyValue(
                                action, isManaged);

                        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                                action, destinationItem);

                        boolean isImpactable = CMHelper.getIsImpactable(
                                originItem.getType(), action,
                                destinationItemUid);

                        ModifiedItemsNodeData nodeData = new ModifiedItemsNodeData(
                                String.valueOf(nodeId), level, rowNumber,
                                currentNodeId, parentNodeId, derivedFrom,
                                parentItem, originItem, originItemWc, null,
                                null, false, false, action, true,
                                destinationItem, destinationItemWc,
                                destinationItemVersionIsReadOnly, isManaged,
                                isManagedIsReadOnly, isManual, type, comment,
                                false, isUpdated, isImpactable,
                                includeInImpactMatrix);

                        nodeId++;

                        TreeNode node = new DefaultTreeNode(nodeData, root);
                        node.setExpanded(true);
                    } catch (DocumentNotFoundException e) {
                        log.error(logInitMsg + "Exception thrown: "
                                + e.getClass() + ": " + e.getMessage());
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

        return root;
    }

    public TreeNode addNewModifiedItem(TreeNode root, DocumentModel originItem,
            DocumentModel originItemWc, String action,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean isManaged, String originItemType, String comment,
            boolean isImpactable, boolean includeInImpactMatrix) {

        String logInitMsg = "[addNewModifiedItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String currentNodeId = CMHelper.generateNodeId(originItem.getId());

        // Calculate if editable fields are editable or not in
        // function of the item values
        boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper.calculateDestinationItemVersionListIsReadOnlyValue(
                action, isManaged);

        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                action, destinationItem);

        ModifiedItemsNodeData newNodeData = new ModifiedItemsNodeData(
                String.valueOf(nodeId), 1, true, false, false, null,
                currentNodeId, null, null, null, originItem, originItemWc, null,
                null, false, false, action, true, destinationItem,
                destinationItemWc, destinationItemVersionIsReadOnly, isManaged,
                isManagedIsReadOnly, true, originItemType, comment, false,
                false, isImpactable, includeInImpactMatrix);

        nodeId++;

        TreeNode node = new DefaultTreeNode(newNodeData, root);
        node.setExpanded(true);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    public String validateModificationOriginValue(TreeNode root,
            String modifiedOriginItemRealUid) {

        String logInitMsg = "[validateModificationOriginValue] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = "";

        for (TreeNode childNode : root.getChildren()) {
            ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) childNode.getData();

            String nodeDataOriginItemRealUid = nodeData.getOriginItem().getId();

            if (nodeDataOriginItemRealUid.equals(modifiedOriginItemRealUid)) {
                validationResultMessage = "eloraplm.message.error.cm.modifiedItemAlreadyExist";
                return validationResultMessage;
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return validationResultMessage;
    }

    public String validateReplaceDestinationValue(TreeNode root,
            String modifiedDestinationItemWcUid) {

        String logInitMsg = "[validateReplaceDestinationValue] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = "";

        for (TreeNode childNode : root.getChildren()) {
            ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) childNode.getData();

            if (nodeData.getAction().equals(CMConstants.ACTION_REMOVE)) {
                String nodeDataOriginItemWcUid = nodeData.getOriginItemWc().getId();
                if (nodeDataOriginItemWcUid.equals(
                        modifiedDestinationItemWcUid)) {
                    validationResultMessage = "eloraplm.message.error.cm.replaceDestinationSameAsRemoveOrigen";
                    return validationResultMessage;
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");

        return validationResultMessage;
    }

    public String validateRemoveOriginValue(TreeNode root,
            String modifiedOriginItemWcUid) {

        String logInitMsg = "[validateRemoveOriginValue] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String validationResultMessage = "";

        for (TreeNode childNode : root.getChildren()) {
            ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) childNode.getData();

            if (nodeData.getAction().equals(CMConstants.ACTION_REPLACE)) {
                String nodeDataDestinationItemWcUid = nodeData.getDestinationItemWc().getId();
                if (nodeDataDestinationItemWcUid.equals(
                        modifiedOriginItemWcUid)) {
                    validationResultMessage = "eloraplm.message.error.cm.removeOriginItemSameAsReplaceDestination";
                    return validationResultMessage;
                }
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

        List<ModifiedItem> modifiedItemsToBeAdded = new LinkedList<ModifiedItem>();

        // only to be sure that same modifiedItem is not added more than once
        List<String> originItemUidsToBeAdded = new LinkedList<String>();

        List<String> originItemUidsToBeRemoved = new LinkedList<String>();

        HashMap<String, ModifiedItem> changedModifiedItems = new HashMap<String, ModifiedItem>();

        for (TreeNode childNode : root.getChildren()) {
            ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) childNode.getData();

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

                // TODO::: TO BE REMOVED
                /*// If Action has changed, it means that derived Modified Items
                // and impacted items should be recalculated. This is achieved
                // by removing existing instance and adding it again as new.
                if (nodeData.getActionChanged()) {
                    // First remove old
                    originItemUidsToBeRemoved.add(nodeDataOriginItemUid);
                    // Then add as new modified one
                    ModifiedItem newModifiedItem = getModifiedItemFromNodeData(
                            nodeData);
                    modifiedItemsToBeAdded.add(newModifiedItem);
                } else {
                    ModifiedItem changedModifiedItem = getModifiedItemFromNodeData(
                            nodeData);
                    changedModifiedItems.put(nodeDataOriginItemUid,
                            changedModifiedItem);
                }*/
            }
        }

        CMHelper.saveModifiedItemChangesInCMProcess(session, currentDoc,
                itemType, originItemUidsToBeRemoved, modifiedItemsToBeAdded,
                changedModifiedItems);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private ModifiedItem getModifiedItemFromNodeData(
            ModifiedItemsNodeData nodeData) {

        String derivedFromUid = null;
        if (nodeData.getDerivedFrom() != null) {
            derivedFromUid = nodeData.getDerivedFrom().getId();
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

        String destinationItemWcUid = null;
        if (nodeData.getDestinationItemWc() != null) {
            destinationItemWcUid = nodeData.getDestinationItemWc().getId();
        }

        ModifiedItem modifiedItem = new ModifiedItem(nodeData.getRowNumber(),
                nodeData.getNodeId(), nodeData.getParentNodeId(),
                derivedFromUid, parentItemUid, originItemUid, originItemWcUid,
                nodeData.getPredicate(), nodeData.getQuantity(),
                nodeData.getIsAnarchic(), nodeData.getIsDirectObject(),
                nodeData.getAction(), destinationItemUid, destinationItemWcUid,
                nodeData.getIsManaged(), nodeData.getIsManual(),
                nodeData.getType(), nodeData.getComment(),
                nodeData.getIsUpdated(), nodeData.getIncludeInImpactMatrix());

        return modifiedItem;
    }

    public void refreshNode(TreeNode node, String trigger)
            throws EloraException {

        String logInitMsg = "[refreshNode] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- trigger = |" + trigger + "|");

        ModifiedItemsNodeData nodeData = (ModifiedItemsNodeData) node.getData();

        if (trigger.equals(
                CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS)) {

            CMTreeBeanHelper.loadDestinationVersions(nodeData, session);

        } else {
            nodeData.setIsModified(true);
            nodeData.setIsUpdated(true);

            if (trigger.equals(CMConstants.TRIGGER_FIELD_IS_MANAGED)) {

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

}
