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

import javax.faces.model.SelectItem;

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

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.bom.lists.util.EloraBomListExternalData;
import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.service.ChangeManagementService;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;
import com.aritu.eloraplm.relations.treetable.BomListExternalData;
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

    protected Map<String, String> messages;

    protected List<DocumentModel> lockConflicts;

    protected boolean promotePendingItems;

    protected boolean passedItems;

    protected boolean updatePendingCharacterizedItems;

    protected boolean existsCharacterizedItems;

    public ImpactedItemsNodeService(CoreSession session, String itemType,
            Map<String, String> messages) throws EloraException {
        this.session = session;
        this.itemType = itemType;
        this.messages = messages;
        nodeId = 0;
        lockConflicts = new ArrayList<DocumentModel>();
        promotePendingItems = false;
        passedItems = false;
        updatePendingCharacterizedItems = false;
        existsCharacterizedItems = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aritu.eloraplm.treetable.NodeService#getRoot(java.lang.Object)
     */
    @Override
    public TreeNode getRoot(Object parentObject) throws EloraException {

        String logInitMsg = "[getRoot] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        passedItems = false;
        updatePendingCharacterizedItems = false;
        existsCharacterizedItems = false;

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

        root = createImpactedItemsRootTree(currentDoc, root, level);

        log.trace(logInitMsg + "--- EXIT ---");

        return root;
    }

    private TreeNode createImpactedItemsRootTree(DocumentModel currentDoc,
            TreeNode root, int level) throws EloraException {

        String logInitMsg = "[createImpactedItemsRootTree] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        IterableQueryResult countIt = null;
        IterableQueryResult it = null;
        try {

            // First, check if this process has any impacted items. If yes,
            // search modified items and then their impacted ones for creating
            // the tree.
            String query = CMQueryFactory.getCountImpactedItemsQuery(
                    currentDoc.getId());
            countIt = session.queryAndFetch(query, NXQL.NXQL);

            if (countIt.iterator().hasNext()) {
                int resultCount = calculateCount(countIt);
                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");
                if (resultCount > 0) {
                    query = CMQueryFactory.getModifiedItemsByDerivedFromQuery(
                            currentDoc.getId(), null);

                    it = session.queryAndFetch(query, NXQL.NXQL);
                    if (it.size() > 0) {
                        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

                        for (Map<String, Serializable> map : it) {
                            String originItemUid = (String) map.get(
                                    pfx + "/*1/originItem");

                            ImpactedItemsNodeData modifiedNodeData = createModifiedNodeData(
                                    level, pfx, map);

                            nodeId++;

                            TreeNode node = new DefaultTreeNode(
                                    modifiedNodeData, root);
                            node.setExpanded(true);

                            if (!itemType.equals(modifiedNodeData.getType())) {

                                root = processDerivedModifiedItems(currentDoc,
                                        root,
                                        CMMetadataConstants.MODIFIED_ITEM_LIST,
                                        level, originItemUid, node);

                            } else {

                                String destinationItemWcUid = null;
                                if (modifiedNodeData.getDestinationItemWc() != null) {
                                    destinationItemWcUid = modifiedNodeData.getDestinationItemWc().getId();
                                }

                                root = createImpactedItemsChildNodesTree(
                                        currentDoc, originItemUid,
                                        modifiedNodeData.getAction(),
                                        destinationItemWcUid,
                                        modifiedNodeData.getNodeId(), root,
                                        node, level, false);
                            }

                            root = processDerivedItemsFromCad(currentDoc, root,
                                    level, originItemUid, node);
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

    private TreeNode processDerivedItemsFromCad(DocumentModel currentDoc,
            TreeNode root, int level, String originItemUid, TreeNode node)
            throws EloraException {
        if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            // We need an extra node to paint the
            // derived items as its children

            String query = CMQueryFactory.getCountDerivedImpactedItemsQuery(
                    currentDoc.getId(), originItemUid, itemType);

            long countResult = EloraQueryHelper.executeCountQuery(query,
                    NXQL.ECM_UUID, session);

            if (countResult > 0) {

                ImpactedItemsNodeData derivedsGroupNodeData = createDerivedsGroupNodeData(
                        (ImpactedItemsNodeData) node.getData(), level);

                TreeNode derivedsMainNode = new DefaultTreeNode(
                        derivedsGroupNodeData, node);
                derivedsMainNode.setExpanded(true);

                String derivedsMainNodeId = ((ImpactedItemsNodeData) derivedsMainNode.getData()).getNodeId();

                root = createImpactedItemsChildNodesTree(currentDoc,
                        originItemUid, CMConstants.ACTION_CHANGE, null,
                        derivedsMainNodeId, root, derivedsMainNode, level,
                        true);
            }
        }
        return root;
    }

    private TreeNode processDerivedModifiedItems(DocumentModel currentDoc,
            TreeNode root, String pfx, int level, String originItemUid,
            TreeNode parentNode) throws EloraException {

        String query = CMQueryFactory.getModifiedItemsByDerivedFromQuery(
                currentDoc.getId(), originItemUid);

        IterableQueryResult derivedIt = null;
        derivedIt = session.queryAndFetch(query, NXQL.NXQL);

        if (derivedIt.size() > 0) {
            level++;
            for (Map<String, Serializable> derivedMap : derivedIt) {

                String derivedType = (String) derivedMap.get(pfx + "/*1/type");
                if (!itemType.equals(derivedType)) {
                    continue;
                }

                if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                    String originItemWcUid = (String) derivedMap.get(
                            pfx + "/*1/originItem");
                    DocumentModel originItem = session.getDocument(
                            new IdRef(originItemWcUid));
                    if (originItem.getType().equals(
                            EloraDoctypeConstants.CAD_DRAWING)
                            || EloraDocumentHelper.isBasicDocument(
                                    originItem)) {
                        continue;
                    }
                }

                ImpactedItemsNodeData derivedModifiedNodeData = createModifiedNodeData(
                        level, pfx, derivedMap);

                nodeId++;

                TreeNode node = new DefaultTreeNode(derivedModifiedNodeData,
                        parentNode);
                node.setExpanded(true);

                String destinationItemWcUid = null;
                if (derivedModifiedNodeData.getDestinationItemWc() != null) {
                    destinationItemWcUid = derivedModifiedNodeData.getDestinationItemWc().getId();
                }

                root = createImpactedItemsChildNodesTree(currentDoc,
                        originItemUid, derivedModifiedNodeData.getAction(),
                        destinationItemWcUid,
                        derivedModifiedNodeData.getNodeId(), root, node, level,
                        false);
            }
        }
        derivedIt.close();

        return root;
    }

    private ImpactedItemsNodeData createModifiedNodeData(int level, String pfx,
            Map<String, Serializable> map) {

        Long rowNumber = (Long) map.get(pfx + "/*1/rowNumber");
        String currentNodeId = (String) map.get(pfx + "/*1/nodeId");
        String parentNodeId = (String) map.get(pfx + "/*1/parentNodeId");
        String originItemUid = (String) map.get(pfx + "/*1/originItem");
        String originItemWcUid = (String) map.get(pfx + "/*1/originItemWc");
        String action = (String) map.get(pfx + "/*1/action");
        String destinationItemUid = (String) map.get(
                pfx + "/*1/destinationItem");
        String destinationItemWcUid = (String) map.get(
                pfx + "/*1/destinationItemWc");
        String destinationItemSourceVersionUid = (String) map.get(
                pfx + "/*1/destinationItemSourceVersion");
        boolean isManaged = (boolean) map.get(pfx + "/*1/isManaged");
        String type = (String) map.get(pfx + "/*1/type");
        String comment = (String) map.get(pfx + "/*1/comment");
        boolean isUpdated = (boolean) map.get(pfx + "/*1/isUpdated");

        DocumentModel originItem = null;
        if (originItemUid != null) {
            originItem = session.getDocument(new IdRef(originItemUid));
        }
        DocumentModel originItemWc = null;
        if (originItemWcUid != null) {
            originItemWc = session.getDocument(new IdRef(originItemWcUid));
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

        String defaultDestinationItemSourceVersionUid = null;
        DocumentModel defaultDestinationItemSourceVersion = CMHelper.calculateDefaultDestinationItemSourceVersion(
                destinationItemWc, originItem, action);
        if (defaultDestinationItemSourceVersion != null) {
            defaultDestinationItemSourceVersionUid = defaultDestinationItemSourceVersion.getId();
        }

        DocumentModel destinationItemSourceVersion = null;
        if (destinationItemSourceVersionUid != null) {
            destinationItemSourceVersion = session.getDocument(
                    new IdRef(destinationItemSourceVersionUid));
        }

        // Modified item in impacted tree
        ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                String.valueOf(nodeId), level, rowNumber, currentNodeId,
                parentNodeId, null, true, originItem, null, originItem,
                originItemWc, "", "", false, false, action, true,
                destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, true, true, isManaged, true,
                false, type, comment, true, isUpdated, false, null, messages);
        return nodeData;
    }

    private ImpactedItemsNodeData createDerivedsGroupNodeData(
            ImpactedItemsNodeData modifiedNodeData, int level) {

        Long rowNumber = 1L;
        String currentNodeId = "000000";
        String parentNodeId = modifiedNodeData.getNodeId();
        String action = CMConstants.ACTION_CHANGE;
        boolean isManaged = true;
        String type = CMConstants.ITEM_TYPE_BOM;
        String comment = null;
        boolean isUpdated = false;
        DocumentModel originItem = null;
        DocumentModel originItemWc = null;
        DocumentModel destinationItem = null;
        DocumentModel destinationItemWc = null;
        String defaultDestinationItemSourceVersionUid = null;
        DocumentModel destinationItemSourceVersion = null;

        // Modified item in impacted tree
        ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                String.valueOf(nodeId), level++, rowNumber, currentNodeId,
                parentNodeId, null, true, originItem, null, originItem,
                originItemWc, "", "", false, false, action, true,
                destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, true, true, isManaged, true,
                false, type, comment, true, isUpdated, false, null, messages);
        return nodeData;
    }

    private int calculateCount(IterableQueryResult countIt) {
        Map<String, Serializable> countMap = countIt.iterator().next();
        String resultCountStr = countMap.get(
                "COUNT(" + NXQL.ECM_UUID + ")").toString();
        int resultCount = Integer.valueOf(resultCountStr);
        return resultCount;
    }

    private TreeNode createImpactedItemsChildNodesTree(DocumentModel currentDoc,
            String modifiedItemUid, String modifiedItemAction,
            String modifiedItemDestinationWcUid, String modifiedItemNodeId,
            TreeNode root, TreeNode parentNode, int level,
            boolean processDeriveds) throws EloraException {

        String logInitMsg = "[createImpactedItemsChildNodesTree] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // First, retrieve the impacted items of this modified item and put all
        // them in a HashMap for then creating the tree.
        // The HashMap contains: for each parentNodeId, its related
        // impactedItems ordered by rowNumber
        HashMap<String, ArrayList<ImpactedItem>> impactedItems = retrieveImpactedItemsByModifiedItem(
                currentDoc.getId(), modifiedItemUid, modifiedItemNodeId,
                processDeriveds);

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
            String cmProcessUid, String modifiedItemUid,
            String modifiedItemNodeId, boolean processDeriveds)
            throws EloraException {

        String logInitMsg = "[retrieveImpactedItemsByModifiedItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        HashMap<String, ArrayList<ImpactedItem>> impactedItems = new HashMap<String, ArrayList<ImpactedItem>>();

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getImpactedItemsByModifiedQuery(
                    cmProcessUid, modifiedItemUid, itemType, processDeriveds);

            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

                for (Map<String, Serializable> map : it) {
                    Long rowNumber = (Long) map.get(pfx + "/*1/rowNumber");
                    String currentNodeId = (String) map.get(pfx + "/*1/nodeId");
                    String derivedFromUid = (String) map.get(
                            pfx + "/*1/derivedFrom");
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
                    String destinationItemSourceVersionUid = (String) map.get(
                            pfx + "/*1/destinationItemSourceVersion");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    boolean isManual = (boolean) map.get(pfx + "/*1/isManual");
                    String type = (String) map.get(pfx + "/*1/type");
                    String comment = (String) map.get(pfx + "/*1/comment");
                    boolean isUpdated = (boolean) map.get(
                            pfx + "/*1/isUpdated");
                    boolean isDerived = (boolean) map.get(
                            pfx + "/*1/isDerived");
                    String refNodeId = (String) map.get(pfx + "/*1/refNodeId");

                    boolean isFromMbom = (boolean) map.get(
                            pfx + "/*1/isFromMbom");

                    String externalReference = (String) map.get(
                            pfx + "/*1/externalReference");
                    String externalName = (String) map.get(
                            pfx + "/*1/externalName");

                    String parentNodeId;
                    String parentItemUid;

                    if (isDerived) {
                        parentNodeId = modifiedItemNodeId;
                        parentItemUid = modifiedItemUid;
                    } else {
                        parentNodeId = (String) map.get(
                                pfx + "/*1/parentNodeId");
                        parentItemUid = (String) map.get(
                                pfx + "/*1/parentItem");
                    }

                    ImpactedItem impactedItem = new ImpactedItem(rowNumber,
                            currentNodeId, parentNodeId, derivedFromUid,
                            modifiedItemUid, parentItemUid, originItemUid,
                            originItemWcUid, predicate, quantity, isAnarchic,
                            isDirectObject, action, destinationItemUid,
                            destinationItemWcUid,
                            destinationItemSourceVersionUid, isManaged,
                            isManual, type, comment, isUpdated, isDerived,
                            refNodeId, isFromMbom, externalReference,
                            externalName);

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

        if (impactedItems.containsKey(parentNodeId)) {

            ArrayList<ImpactedItem> impactedItemListByParentNodeId = impactedItems.get(
                    parentNodeId);

            level++;

            for (ImpactedItem impactedItem : impactedItemListByParentNodeId) {

                ImpactedItemsNodeData nodeData = createImpactedItemsNodeData(
                        level, parentItemAction, modifiedItemAction,
                        modifiedItemDestinationWcUid, impactedItem);

                nodeId++;

                TreeNode node = new DefaultTreeNode(nodeData, parentNode);
                node.setExpanded(true);

                // recursive call
                root = fillTreeWithImpactedItems(root, node, level,
                        impactedItems, impactedItem.getNodeId(),
                        impactedItem.getAction(), modifiedItemAction,
                        modifiedItemDestinationWcUid);
            }
        }
        return root;
    }

    private ImpactedItemsNodeData createImpactedItemsNodeData(int level,
            String parentItemAction, String modifiedItemAction,
            String modifiedItemDestinationWcUid, ImpactedItem impactedItem) {

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

        String defaultDestinationItemSourceVersionUid = null;
        DocumentModel defaultDestinationItemSourceVersion = CMHelper.calculateDefaultDestinationItemSourceVersion(
                destinationItemWc, originItem, impactedItem.getAction());
        if (defaultDestinationItemSourceVersion != null) {
            defaultDestinationItemSourceVersionUid = defaultDestinationItemSourceVersion.getId();
        }

        DocumentModel destinationItemSourceVersion = null;
        if (impactedItem.getDestinationItemSourceVersion() != null) {
            destinationItemSourceVersion = session.getDocument(
                    new IdRef(impactedItem.getDestinationItemSourceVersion()));
        }

        DocumentModel derivedFrom = null;
        if (impactedItem.getDerivedFrom() != null) {
            derivedFrom = session.getDocument(
                    new IdRef(impactedItem.getDerivedFrom()));
        }

        // Calculate if editable fields are editable or not in
        // function of the item values
        boolean destinationItemVersionIsReadOnly = CMTreeBeanHelper.calculateDestinationItemVersionListIsReadOnlyValue(
                impactedItem.getAction(), impactedItem.isManaged());

        boolean isManagedIsReadOnly = CMTreeBeanHelper.calculateIsManagedIsReadOnlyValue(
                impactedItem.getAction(), destinationItem);

        boolean actionIsReadOnly = CMTreeBeanHelper.calculatedImpactedItemActionIsReadOnlyValue(
                impactedItem.getAction(), impactedItem.getOriginItemWc(),
                impactedItem.getIsFromMbom(), originItemType,
                modifiedItemAction, modifiedItemDestinationWcUid,
                parentItemAction);

        checkWarnings(originItem, destinationItem, destinationItemWc,
                impactedItem.isManaged());

        boolean isCharacterized = destinationItem == null ? false
                : BomCharacteristicsHelper.isCharacterizedItem(destinationItem);

        // TODO: Mirar cómo separar el tema de Mbom de Elora y Copreci

        BomListExternalData bomListExternalData = new EloraBomListExternalData(
                impactedItem.getExternalReference(),
                impactedItem.getExternalName(), null, true);

        boolean destinationItemSourceVersionIsReadOnly = CMTreeBeanHelper
                .calculateDestinationItemSourceVersionIsReadOnlyValue(
                        destinationItem, impactedItem.isManaged());

        ImpactedItemsNodeData nodeData = new ImpactedItemsNodeData(
                String.valueOf(nodeId), level, impactedItem.getRowNumber(),
                impactedItem.getNodeId(), impactedItem.getParentNodeId(),
                derivedFrom, false, modifiedItem, parentItem, originItem,
                originItemWc, impactedItem.getPredicate(),
                impactedItem.getQuantity(), impactedItem.isAnarchic(),
                impactedItem.isDirectObject(), impactedItem.getAction(),
                actionIsReadOnly, destinationItem, destinationItemWc,
                defaultDestinationItemSourceVersionUid,
                destinationItemSourceVersion, destinationItemVersionIsReadOnly,
                destinationItemSourceVersionIsReadOnly,
                impactedItem.isManaged(), isManagedIsReadOnly,
                impactedItem.isManual(), impactedItem.getType(),
                impactedItem.getComment(), false, impactedItem.isUpdated(),
                impactedItem.isDerived(), impactedItem.getRefNodeId(),
                impactedItem.getIsFromMbom(), isCharacterized,
                bomListExternalData, messages);

        return nodeData;
    }

    private void checkWarnings(DocumentModel originItem,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean managed) {

        if (destinationItem == null) {
            return;
        }

        checkLockConflicts(destinationItem, managed);

        if (!passedItems || !promotePendingItems
                || !updatePendingCharacterizedItems) {
            checkItemProblems(originItem, destinationItem, destinationItemWc,
                    managed);
        }
    }

    private void checkItemProblems(DocumentModel originItem,
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean managed) {

        if (!passedItems || !promotePendingItems) {
            checkPromotePendingAndPassedItems(destinationItem,
                    destinationItemWc, managed);
        }

        if (destinationItem.hasSchema(EloraSchemaConstants.BOM_ITEM)
                && !updatePendingCharacterizedItems) {
            checkPendingCharacterizedItems(destinationItem, managed);
        }
    }

    private void checkPendingCharacterizedItems(DocumentModel destinationItem,
            boolean managed) {
        boolean isCharacterizedItem = BomCharacteristicsHelper.isCharacterizedItem(
                destinationItem);

        if (isCharacterizedItem) {
            existsCharacterizedItems = true;
        }

        if (isCharacterizedItem && (destinationItem.isCheckedOut()
                || (!destinationItem.isCheckedOut() && !managed))) {
            updatePendingCharacterizedItems = true;
        }
    }

    private void checkPromotePendingAndPassedItems(
            DocumentModel destinationItem, DocumentModel destinationItemWc,
            boolean managed) {

        boolean destIsPreliminary = EloraLifeCycleConstants.PRELIMINARY.equals(
                destinationItem.getCurrentLifeCycleState());

        boolean hasAtLeastOneApprovedVersion = false;
        Long previousApprovedVersions = EloraQueryFactory.countApprovedVersionsOfDocumentQuery(
                session, destinationItemWc.getId(),
                destinationItemWc.getType());
        if (previousApprovedVersions > 0) {
            hasAtLeastOneApprovedVersion = true;
        }

        if (managed && destIsPreliminary && hasAtLeastOneApprovedVersion) {
            String baseVersionId = EloraDocumentHelper.getBaseVersion(
                    destinationItemWc).getId();
            if (!baseVersionId.equals(destinationItem.getId())) {
                passedItems = true;
            } else {
                promotePendingItems = true;
            }
        }
    }

    private void checkLockConflicts(DocumentModel destinationItem,
            boolean managed) {
        String currentUser = session.getPrincipal().getName();
        if (destinationItem.isLocked() && !managed
                && !destinationItem.getLockInfo().getOwner().equals(
                        currentUser)) {
            if (!lockConflicts.contains(destinationItem)) {
                lockConflicts.add(destinationItem);
            }
        }
    }

    public void saveTree(DocumentModel currentDoc, TreeNode root,
            List<TreeNode> actionChangeTreeNodeList,
            ChangeManagementService cmService)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[saveTree] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ImpactedItem> impactedItems = getImpactedItemsListFromTree(root);

        cmService.saveImpactedItemListInCMProcess(session, currentDoc, itemType,
                impactedItems, actionChangeTreeNodeList);

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

        } else if (trigger.equals(
                CMConstants.TRIGGER_ACTION_LOAD_DESTINATION_SOURCE_VERSIONS)) {

            CMTreeBeanHelper.loadDestinationSourceVersions(nodeData, session,
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
                            destinationItem, destinationItem.getVersionLabel()
                                    + CMConstants.VERSION_LABEL_WC));
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

            } else if (trigger.equals(
                    CMConstants.TRIGGER_FIELD_DESTINATION_ITEM_SOURCE_VERSION)) {

                CMTreeBeanHelper.processRefreshNodeTriggeredByDestinationItemSourceVersion(
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
                            destinationItem, destinationItem.getVersionLabel()
                                    + CMConstants.VERSION_LABEL_WC));
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

    public List<DocumentModel> getLockConflicts() {
        return lockConflicts;
    }

    public boolean getPromotePendingItems() {
        return promotePendingItems;
    }

    public boolean getPassedItems() {
        return passedItems;
    }

    public boolean getUpdatePendingCharacterizedItems() {
        return updatePendingCharacterizedItems;
    }

    public boolean getExistsCharacterizedItems() {
        return existsCharacterizedItems;
    }
}
