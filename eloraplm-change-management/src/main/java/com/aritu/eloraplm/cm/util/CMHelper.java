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
package com.aritu.eloraplm.cm.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.cm.CMItem;
import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.treetable.CMTreeBeanHelper;
import com.aritu.eloraplm.config.util.CMConfig;
import com.aritu.eloraplm.config.util.CMImpactableConfig;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.QueriesConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * Helper class for Elora Change Management.
 *
 * @author aritu
 *
 */
public class CMHelper {

    private static final Log log = LogFactory.getLog(CMHelper.class);

    // -------------------------------------------------------------------------
    // Methods related with the creation of CM Items
    // -------------------------------------------------------------------------
    private static HashMap<String, Object> createCMItemType(CMItem cmItem)
            throws EloraException {

        if (cmItem.getRowNumber() == null) {
            log.error("[createCMItemType] rowNumber is null.");
            throw new EloraException("rowNumber is null.");
        }

        // items
        HashMap<String, Object> cmItemType = new HashMap<>();

        // --- rowNumber
        cmItemType.put("rowNumber", cmItem.getRowNumber());

        // --- nodeId
        cmItemType.put("nodeId", cmItem.getNodeId());

        // --- parentNodeId
        cmItemType.put("parentNodeId", cmItem.getParentNodeId());

        // --- parentItem
        cmItemType.put("parentItem", cmItem.getParentItem());

        // --- originItem
        cmItemType.put("originItem", cmItem.getOriginItem());

        // --- originItemWc
        cmItemType.put("originItemWc", cmItem.getOriginItemWc());

        // --- predicate
        cmItemType.put("predicate", cmItem.getPredicate());

        // --- quantity
        cmItemType.put("quantity", cmItem.getQuantity());

        // --- isAnarchich
        cmItemType.put("isAnarchic", cmItem.isAnarchic());

        // --- isDirectObject
        cmItemType.put("isDirectObject", cmItem.isDirectObject());

        // --- action
        cmItemType.put("action", cmItem.getAction());

        // --- destinationItem
        cmItemType.put("destinationItem", cmItem.getDestinationItem());

        // --- destinationItemWc
        cmItemType.put("destinationItemWc", cmItem.getDestinationItemWc());

        // --- isManaged
        cmItemType.put("isManaged", cmItem.isManaged());

        // --- isManual
        cmItemType.put("isManual", cmItem.isManual());

        // --- type
        cmItemType.put("type", cmItem.getType());

        // --- comment
        cmItemType.put("comment", cmItem.getComment());

        // --- isUpdated
        cmItemType.put("isUpdated", cmItem.isUpdated());

        return cmItemType;
    }

    public static HashMap<String, Object> createModifiedItemType(
            ModifiedItem modifiedItem) throws EloraException {

        // Create Modified Item with common fields
        HashMap<String, Object> modifiedItemType = createCMItemType(
                modifiedItem);

        // Add its specific attributes
        // --- derivedFrom
        modifiedItemType.put("derivedFrom", modifiedItem.getDerivedFrom());

        // --- isDerivedFromImpactMatrix
        modifiedItemType.put("isDerivedFromImpactMatrix",
                modifiedItem.getIsDerivedFromImpactMatrix());

        // --- includeInImpactMatrix
        modifiedItemType.put("includeInImpactMatrix",
                modifiedItem.getIncludeInImpactMatrix());

        return modifiedItemType;
    }

    private static void updateModifiedItemType(
            HashMap<String, Object> modifiedItemType,
            ModifiedItem modifiedItem) {

        // --- action
        modifiedItemType.put("action", modifiedItem.getAction());

        // --- destinationItem
        modifiedItemType.put("destinationItem",
                modifiedItem.getDestinationItem());

        // --- destinationItemWc
        modifiedItemType.put("destinationItemWc",
                modifiedItem.getDestinationItemWc());

        // --- isManaged
        modifiedItemType.put("isManaged", modifiedItem.isManaged());

        // --- comment
        modifiedItemType.put("comment", modifiedItem.getComment());

        // --- includeInImpactMatrix
        modifiedItemType.put("includeInImpactMatrix",
                modifiedItem.getIncludeInImpactMatrix());
    }

    public static HashMap<String, Object> createImpactedItemType(
            ImpactedItem impactedItem) throws EloraException {

        // Create Impacted Item with common fields
        HashMap<String, Object> impactedItemType = createCMItemType(
                impactedItem);

        // Add its specific attributes
        // --- modifiedItem
        impactedItemType.put("modifiedItem", impactedItem.getModifiedItem());

        return impactedItemType;
    }

    public static ImpactedItem createImpactedItem(
            HashMap<String, Object> impactedItem) throws EloraException {

        Long rowNumber = (Long) impactedItem.get("rowNumber");

        String modifiedItem = (String) impactedItem.get("modifiedItem");

        String nodeId = (String) impactedItem.get("nodeId");

        String parentNodeId = (String) impactedItem.get("parentNodeId");

        String parentItem = (String) impactedItem.get("parentItem");

        String originItem = (String) impactedItem.get("originItem");

        String originItemWc = (String) impactedItem.get("originItemWc");

        String predicate = (String) impactedItem.get("predicate");

        String quantity = (String) impactedItem.get("quantity"); // TYPE???

        boolean isAnarchic = (boolean) impactedItem.get("isAnarchic");

        boolean isDirectObject = (boolean) impactedItem.get("isDirectObject");

        String action = (String) impactedItem.get("action");

        String destinationItem = (String) impactedItem.get("destinationItem");

        String destinationItemWc = (String) impactedItem.get(
                "destinationItemWc");

        boolean isManaged = (boolean) impactedItem.get("isManaged");

        boolean isManual = (boolean) impactedItem.get("isManual");

        String type = (String) impactedItem.get("type");

        String comment = (String) impactedItem.get("comment");

        boolean isUpdated = (boolean) impactedItem.get("isUpdated");

        ImpactedItem impactedItemTyped = new ImpactedItem(rowNumber, nodeId,
                parentNodeId, modifiedItem, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, destinationItem, destinationItemWc, isManaged, isManual,
                type, comment, isUpdated);

        return impactedItemTyped;
    }

    private static ModifiedItem createDerivedModifiedItem(CoreSession session,
            DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem,
            String modifiedItemAction, String modifiedItemComment,
            DocumentModel derivedModifiedItemOriginItem,
            String derivedModifiedItemPredicate,
            boolean derivedModifiedItemIsSubject,
            String derivedModifiedItemQuantity,
            boolean derivedModifiedItemIsAnarchic,
            boolean derivedModifiedItemIsDirectObject,
            String derivedModifiedItemType, boolean isDerivedFromImpactMatrix)
            throws EloraException, DocumentUnreadableException {

        String modifiedItemOriginUid = modifiedItemOriginItem.getId();

        String derivedModifiedItemOriginItemUid = derivedModifiedItemOriginItem.getId();
        DocumentModel derivedModifiedItemOriginItemWc = getWcDocForOriginItem(
                session, derivedModifiedItemOriginItem);
        String derivedModifiedItemOriginItemWcUid = derivedModifiedItemOriginItemWc.getId();

        String derivedModifedItemDestinationItemUid = null;
        String derivedModifiedItemDestinationItemWcUid = null;
        boolean derivedModifiedItemIsManaged = false;
        String derivedModifiedItemComment = null;
        boolean derivedModifiedItemIncludeInImpactMatrix = false;

        // Calculate the action for the derived item
        String derivedModifiedItemAction = calculateDerivedModifiedItemAction(
                modifiedItemOriginItem, modifiedItemAction,
                derivedModifiedItemOriginItem);

        derivedModifiedItemComment = modifiedItemComment;

        if (derivedModifiedItemAction.equals(CMConstants.ACTION_CHANGE)) {
            derivedModifedItemDestinationItemUid = derivedModifiedItemOriginItemWcUid;
            derivedModifiedItemDestinationItemWcUid = derivedModifiedItemOriginItemWcUid;

        } else if (derivedModifiedItemAction.equals(CMConstants.ACTION_REMOVE)
                || derivedModifiedItemAction.equals(
                        CMConstants.ACTION_IGNORE)) {
            derivedModifiedItemIsManaged = true;

        } else if (derivedModifiedItemAction.equals(
                CMConstants.ACTION_REPLACE)) {
            DocumentModel derivedModifiedItemDestination = calculateDerivedDestinationItem(
                    session, modifiedItemDestinationItem,
                    derivedModifiedItemOriginItem, derivedModifiedItemPredicate,
                    derivedModifiedItemIsSubject);

            if (derivedModifiedItemDestination != null) {
                derivedModifedItemDestinationItemUid = derivedModifiedItemDestination.getId();

                if (derivedModifiedItemDestination.isVersion()) {
                    DocumentModel derivedModifiedItemDestinationItemWc = getWcDocForOriginItem(
                            session, derivedModifiedItemDestination);
                    derivedModifiedItemDestinationItemWcUid = derivedModifiedItemDestinationItemWc.getId();
                    derivedModifiedItemIsManaged = true;
                } else {
                    derivedModifiedItemDestinationItemWcUid = derivedModifedItemDestinationItemUid;
                }
            } else {
                // If derived item action is Replace, but derived item
                // destination is null, change the derived action to Remove.
                derivedModifiedItemAction = CMConstants.ACTION_REMOVE;
            }
        }
        derivedModifiedItemIncludeInImpactMatrix = CMHelper.getIncludeInImpactMatrixDefaultValue(
                derivedModifiedItemOriginItem.getType(), modifiedItemAction,
                derivedModifedItemDestinationItemUid);

        String nodeId = generateNodeId(derivedModifiedItemOriginItem.getId());
        String parentNodeId = null;

        ModifiedItem derivedModifiedItem = new ModifiedItem(null, nodeId,
                parentNodeId, modifiedItemOriginUid, isDerivedFromImpactMatrix,
                null, derivedModifiedItemOriginItemUid,
                derivedModifiedItemOriginItemWcUid,
                derivedModifiedItemPredicate, derivedModifiedItemQuantity,
                derivedModifiedItemIsAnarchic,
                derivedModifiedItemIsDirectObject, derivedModifiedItemAction,
                derivedModifedItemDestinationItemUid,
                derivedModifiedItemDestinationItemWcUid,
                derivedModifiedItemIsManaged, false, derivedModifiedItemType,
                derivedModifiedItemComment, false,
                derivedModifiedItemIncludeInImpactMatrix);

        return derivedModifiedItem;
    }

    private static String calculateDerivedModifiedItemAction(
            DocumentModel modifiedItemOriginItem, String modifiedItemAction,
            DocumentModel derivedModifiedItemOriginItem) {

        String derivedModifiedItemAction = null;

        // if modifiedItemOriginItem is a CAD document or a BOM document
        if (modifiedItemOriginItem.hasFacet(
                EloraFacetConstants.FACET_CAD_DOCUMENT)
                || modifiedItemOriginItem.hasFacet(
                        EloraFacetConstants.FACET_BOM_DOCUMENT)) {

            // check if the derived modified item is impactable or not for the
            // modified item action
            boolean isImpactable = CMHelper.getIsImpactable(
                    derivedModifiedItemOriginItem.getType(),
                    modifiedItemAction);

            // If is impactable
            if (isImpactable) {
                // Set the same action
                derivedModifiedItemAction = modifiedItemAction;
            }
            // If is NOT impactable
            else {
                // if the modified item action is CHANGE, set the same action to
                // the derived item
                if (modifiedItemAction.equals(CMConstants.ACTION_CHANGE)) {
                    derivedModifiedItemAction = modifiedItemAction;

                }
                // else set derived item action to IGNORE.
                else {
                    derivedModifiedItemAction = CMConstants.ACTION_IGNORE;
                }
            }
        }
        // if modifiedItemOriginItem is NOT a CAD and is not a BOM)
        else {
            // if the modified item action is IGNORE, set the same action to
            // the derived item
            if (modifiedItemAction.equals(CMConstants.ACTION_IGNORE)) {
                derivedModifiedItemAction = modifiedItemAction;
            }
            // else set derived item action to CHANGE.
            else {
                derivedModifiedItemAction = CMConstants.ACTION_CHANGE;
            }
        }

        return derivedModifiedItemAction;
    }

    private static boolean checkIfImpactMatrixShouldBeRemoved(
            HashMap<String, Object> modifiedItemType,
            ModifiedItem changedModifiedItem) {

        boolean needToRemoveIM = false;

        boolean includeInImpactMatrix = (boolean) modifiedItemType.get(
                "includeInImpactMatrix");

        if (includeInImpactMatrix) {
            String newAction = changedModifiedItem.getAction();
            String currentAction = (String) modifiedItemType.get("action");
            // if the action has changed the IM should be removed
            if ((newAction != null && currentAction != null
                    && !newAction.equals(currentAction))
                    || (newAction == null && currentAction != null)
                    || (newAction != null && currentAction == null)) {
                needToRemoveIM = true;
            } else if (newAction != null
                    && newAction.equals(CMConstants.ACTION_REPLACE)) {
                // otherwise, if the action is replace and the destination item
                // has changed (check the WC, since the AV changes when setting
                // managed)
                String newDestinationItemWc = changedModifiedItem.getDestinationItemWc();
                String currentDestinationItemWc = (String) modifiedItemType.get(
                        "destinationItemWc");
                if (newDestinationItemWc != null
                        && currentDestinationItemWc != null
                        && !newDestinationItemWc.equals(
                                currentDestinationItemWc)) {
                    needToRemoveIM = true;
                }
            }
        }

        return needToRemoveIM;
    }

    /**
     * This method checks the impacted element regarding the modification
     * element.
     *
     * @param session
     * @param modifiedItemAction
     * @param modifiedItemDestinationWcUid
     * @param originItem
     * @return
     * @throws EloraException
     */
    private static String checkImpactedOriginWithModifiedDestination(
            CoreSession session, String modifiedItemAction,
            String modifiedItemDestinationWcUid, DocumentModel originItem)
            throws EloraException {

        String resultMsg = "";

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);
        String originItemWcUid = originItemWc.getId();

        // if the modified action is replace and impactedOrigin is the same
        // as the modification destination, impactedItem should not be
        // inserted in the matrix
        if (modifiedItemAction.equals(CMConstants.ACTION_REPLACE)
                && originItemWcUid.equals(modifiedItemDestinationWcUid)) {
            return resultMsg = CMConstants.COMMENT_IGNORE_SINCE_ORIGIN_SAME_AS_REPLACE_DESTINATION;
        }

        return resultMsg;

    }

    /**
     * This method checks the impacted element regarding its parent element.
     *
     * @param session
     * @param parentItem
     * @param originItem
     * @param predicate
     * @return
     * @throws EloraException
     */
    private static String checkImpactedOriginWithParent(CoreSession session,
            DocumentModel parentItem, DocumentModel originItem,
            String predicate) throws EloraException {

        String logInitMsg = "[checkImpactedOriginWithParent] ["
                + session.getPrincipal().getName() + "] ";

        String resultMsg = "";

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);

        // Check if the originItemWc is based on the originItem.
        DocumentModel originItemWcBasedOnDoc = EloraDocumentHelper.getBaseVersion(
                originItemWc);
        if (originItemWcBasedOnDoc == null) {
            String errorMsg = "Origin document |" + originItemWc.getId()
                    + "| has no base version.";
            log.error(logInitMsg + errorMsg);
            throw new EloraException(errorMsg);
        }

        if (!originItem.getId().equals(originItemWcBasedOnDoc.getId())) {

            // If originItemWc is not based on the originItem, check if the
            // parentItem has a relation with the document where the
            // originItemWc is based on
            // If not, exclude it from the matrix
            Graph graph = RelationHelper.getRelationManager().getGraphByName(
                    EloraRelationConstants.ELORA_GRAPH_NAME);

            Resource predicateResource = new ResourceImpl(predicate);
            QNameResource parentItemResource = RelationHelper.getDocumentResource(
                    parentItem);
            QNameResource originItemWcBasedOnDocResource = RelationHelper.getDocumentResource(
                    originItemWcBasedOnDoc);
            Statement statement = new StatementImpl(
                    originItemWcBasedOnDocResource, predicateResource,
                    parentItemResource);

            if (!graph.hasStatement(statement)) {
                return resultMsg = CMConstants.COMMENT_IGNORE_SINCE_NO_CHANGES_IN_DESTINATION;
            }
        }

        return resultMsg;
    }

    /**
     * This method checks the DOC impacted element regarding the actions of its
     * related BOM elements in the BOM impact matrix of this CM process.
     *
     * @param session
     * @param originItem
     * @return
     * @throws EloraException
     */
    private static String checkImpactedDocOriginWithRelatedBomActions(
            CoreSession session, String cmProcessUid, DocumentModel originItem)
            throws EloraException {

        String logInitMsg = "[checkImpactedDocOriginWithRelatedBomActions] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- cmProcessUid = |" + cmProcessUid
                + "|, originItemUid = |" + originItem.getId() + "|");

        String resultMsg = "";

        List<DocumentModel> relatedBoms = RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT),
                originItem);

        if (relatedBoms != null && relatedBoms.size() > 0) {
            log.trace(logInitMsg + " relatedBoms.size = |" + relatedBoms.size()
                    + "|");

            List<String> relatedBomsUids = new ArrayList<String>();
            for (DocumentModel relatedBom : relatedBoms) {
                relatedBomsUids.add(relatedBom.getId());
            }

            List<String> distinctActions = CMQueryResultFactory.getDistinctImpacteItemsActionsByOriginList(
                    session, cmProcessUid, CMConstants.ITEM_TYPE_BOM,
                    relatedBomsUids);

            log.trace(logInitMsg + "|" + distinctActions.size()
                    + "| distinct actions found.");

            if (distinctActions != null && distinctActions.size() == 1
                    && distinctActions.contains(CMConstants.ACTION_IGNORE)) {
                return resultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_IGNORED;
            }
        } else {
            log.trace(logInitMsg + "NO relatedBoms found.");
        }

        log.trace(logInitMsg + "--- EXIT --- with resultMsg = |" + resultMsg
                + "|");

        return resultMsg;
    }

    private static ImpactedItem createIgnoredImpactedItem(CoreSession session,
            DocumentModel cmProcess, Long rowNumber, String nodeId,
            String parentNodeId, DocumentModel modifiedItem,
            DocumentModel parentItem, String parentItemAction,
            DocumentModel originItem, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String originItemType,
            boolean isManual, boolean isUpdated, String comment)
            throws EloraException {

        String modifiedItemUid = modifiedItem.getId();

        String parentItemUid = parentItem.getId();

        String originItemUid = originItem.getId();

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);

        String originItemWcUid = originItemWc.getId();

        ImpactedItem impactedItem = new ImpactedItem(rowNumber, nodeId,
                parentNodeId, modifiedItemUid, parentItemUid, originItemUid,
                originItemWcUid, predicate, quantity, isAnarchic,
                isDirectObject, CMConstants.ACTION_IGNORE, null, null, true,
                isManual, originItemType, comment, isUpdated);

        return impactedItem;
    }

    private static ImpactedItem createImpactedItem(CoreSession session,
            DocumentModel cmProcess, Long rowNumber, String nodeId,
            String parentNodeId, DocumentModel modifiedItem,
            DocumentModel parentItem, String parentItemAction,
            DocumentModel originItem, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String originItemType,
            boolean isManual, boolean isUpdated) throws EloraException {

        String modifiedItemUid = modifiedItem.getId();

        String parentItemUid = parentItem.getId();

        String originItemUid = originItem.getId();

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);

        String originItemWcUid = originItemWc.getId();

        String action = null;
        boolean isManaged = false;
        String destinationItemUid = null;
        String destinationItemWcUid = null;
        String comment = null;

        // If parent item's action is Ignore, current item action should
        // also be IGNORE
        if (parentItemAction != null
                && parentItemAction.equals(CMConstants.ACTION_IGNORE)) {
            action = CMConstants.ACTION_IGNORE;
            isManaged = true;
            comment = CMConstants.COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE;
        } else {
            // By default, impacted items action should be CHANGE
            action = CMConstants.ACTION_CHANGE;
            destinationItemUid = originItemWcUid;
            destinationItemWcUid = originItemWcUid;
            comment = CMTreeBeanHelper.calculateComment(cmProcess, action);
        }

        ImpactedItem impactedItem = new ImpactedItem(rowNumber, nodeId,
                parentNodeId, modifiedItemUid, parentItemUid, originItemUid,
                originItemWcUid, predicate, quantity, isAnarchic,
                isDirectObject, action, destinationItemUid,
                destinationItemWcUid, isManaged, isManual, originItemType,
                comment, isUpdated);

        return impactedItem;
    }

    /**
     * True if there is already specified modifiedItem in the specified Change
     * Management process.
     *
     * @param session
     * @param cmProcess
     * @param modifiedItemOriginItemUid
     * @throws EloraException
     */
    public static boolean existModifiedItemInCMProcess(CoreSession session,
            String cmProcessUid, String modifiedItemOriginItemUid,
            String modifiedItemOriginItemType) throws EloraException {

        String logInitMsg = "[existModifiedItemInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean existModifiedItemInProcess = false;

        // Check specified input parameters and log them
        if (cmProcessUid == null) {
            log.error(logInitMsg + "Specified cmProcessUid is null.");
            throw new EloraException("Specified cmProcessUid is null.");
        }

        if (modifiedItemOriginItemUid == null
                || modifiedItemOriginItemUid.isEmpty()) {
            log.error(logInitMsg
                    + "Specified modifiedItemOriginItemUid is null or Empty.");
            throw new EloraException(
                    "Specified modifiedItemOriginItemUid is null.");
        }

        log.trace(logInitMsg + " enter parameters: cmProcessUid = |"
                + cmProcessUid + "|, modifiedItemOriginItemUid = |"
                + modifiedItemOriginItemUid + "|");

        IterableQueryResult queryResult = null;
        try {
            String query = CMQueryFactory.getCountModifiedItemsByOriginQuery(
                    cmProcessUid, modifiedItemOriginItemUid,
                    modifiedItemOriginItemType);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                String resultCountStr = map.get(
                        "COUNT(" + NXQL.ECM_UUID + ")").toString();
                int resultCount = Integer.valueOf(resultCountStr);

                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");

                if (resultCount > 0) {
                    existModifiedItemInProcess = true;
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }

        log.trace(
                logInitMsg + "--- EXIT --- with existModifiedItemInProcess = |"
                        + existModifiedItemInProcess + "|");

        return existModifiedItemInProcess;
    }

    /**
     * True if there is already specified impactedItem in the specified Change
     * Management process.
     *
     * @param session
     * @param cmProcess
     * @param impactedItemOriginItemUid
     * @throws EloraException
     */
    public static boolean existImpactedItemInCMProcess(CoreSession session,
            String cmProcessUid, String impactedItemOriginItemUid,
            String impactedItemOriginItemType) throws EloraException {

        String logInitMsg = "[existImpactedItemInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean existImpactedItemInProcess = false;

        // Check specified input parameters and log them
        if (cmProcessUid == null) {
            log.error(logInitMsg + "Specified cmProcessUid is null.");
            throw new EloraException("Specified cmProcessUid is null.");
        }

        if (impactedItemOriginItemUid == null
                || impactedItemOriginItemUid.isEmpty()) {
            log.error(logInitMsg
                    + "Specified impactedItemOriginItemUid is null or Empty.");
            throw new EloraException(
                    "Specified impactedItemOriginItemUid is null.");
        }

        log.trace(logInitMsg + " enter parameters: cmProcessUid = |"
                + cmProcessUid + "|, impactedItemOriginItemUid = |"
                + impactedItemOriginItemUid + "|");

        IterableQueryResult queryResult = null;
        try {
            String query = CMQueryFactory.getCountImpactedItemsByOriginQuery(
                    cmProcessUid, impactedItemOriginItemUid,
                    impactedItemOriginItemType);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                String resultCountStr = map.get(
                        "COUNT(" + NXQL.ECM_UUID + ")").toString();
                int resultCount = Integer.valueOf(resultCountStr);

                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");

                if (resultCount > 0) {
                    existImpactedItemInProcess = true;
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }

        log.trace(
                logInitMsg + "--- EXIT --- with existImpactedItemInProcess = |"
                        + existImpactedItemInProcess + "|");

        return existImpactedItemInProcess;
    }

    public static long getModifiedItemsMaxRowNumberInCMProcess(
            CoreSession session, DocumentModel cmProcess, String itemType)
            throws EloraException {

        String logInitMsg = "[getModifiedItemsMaxRowNumberInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        long maxRowNumber = 0;

        // Check specified input parameters and log them
        if (cmProcess == null) {
            log.error(logInitMsg + "Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }

        log.trace(logInitMsg + " enter parameters: cmProcess = |"
                + cmProcess.getId() + "|");

        IterableQueryResult queryResult = null;
        try {
            String query = CMQueryFactory.getModifiedItemsMaxRowNumberQuery(
                    cmProcess.getId(), itemType);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            String pfx = CMHelper.getModifiedItemListMetadaName(itemType);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                if (map.get("MAX(" + pfx + "/*/rowNumber" + ")") != null) {
                    String maxRowNumberStr = map.get(
                            "MAX(" + pfx + "/*/rowNumber" + ")").toString();
                    maxRowNumber = Long.valueOf(maxRowNumberStr);

                    log.trace(logInitMsg + "DB query maxRowNumber = |"
                            + maxRowNumber + "|");
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }

        log.trace(logInitMsg + "--- EXIT --- with maxRowNumber = |"
                + maxRowNumber + "|");

        return maxRowNumber;
    }

    private static DocumentModel getWcDocForOriginItem(CoreSession session,
            DocumentModel originItem) throws EloraException {

        String logInitMsg = "[getWcDocForOriginItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel originItemWcDoc = null;

        // Check the nature of the specified originItem:
        // proxy, wcUid or realUid

        // if modified item is a proxy, throw an error
        if (originItem.isProxy()) {
            String errorMsg = "Specified originItem is a proxy.";
            log.error(errorMsg);
            throw new EloraException(errorMsg);
        }
        // if modified item is a working copy, verify if it is versionable or
        // not.
        else if (!originItem.isVersion()) {
            // if it is versionable, throw an error.
            if (originItem.isVersionable()) {
                String errorMsg = "Specified originItem is a working copy but the document is versionable. A realUid should be provided.";
                log.error(errorMsg);
                throw new EloraException(errorMsg);
            } else {
                originItemWcDoc = originItem;
            }
        }
        // if its a realUid
        else {
            // destination is the working copy of the item
            originItemWcDoc = session.getWorkingCopy(originItem.getRef());
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return originItemWcDoc;
    }

    private static void checkOriginItem(CoreSession session,
            DocumentModel originItem) throws EloraException {

        String logInitMsg = "[checkOriginItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Check the nature of the specified originItem:
        // proxy, wcUid or realUid

        // if modified item is a proxy, throw an error
        if (originItem.isProxy()) {
            String errorMsg = "Specified originItem is a proxy.";
            log.error(errorMsg);
            throw new EloraException(errorMsg);
        }
        // if modified item is a working copy, verify if it is versionable or
        // not.
        else if (!originItem.isVersion()) {
            // if it is versionable, throw an error.
            if (originItem.isVersionable()) {
                String errorMsg = "Specified originItem is a working copy but the document is versionable. A realUid should be provided.";
                log.error(errorMsg);
                throw new EloraException(errorMsg);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    /**
     * Retrieves the item type.
     *
     * @param item
     * @return BOM if it is a BOM Document and DOC otherwise
     */
    public static String getItemType(DocumentModel item) {
        String itemType = "";

        if (EloraDocumentHelper.isBomDocument(item)) {
            itemType = CMConstants.ITEM_TYPE_BOM;
        } else {
            itemType = CMConstants.ITEM_TYPE_DOC;
        }

        return itemType;
    }

    /**
     * @param session
     * @param cmProcess
     * @param originItemUidsToBeRemoved
     * @param modifiedItemsToBeAdded
     * @param changedModifiedItems
     * @throws EloraException
     */

    public static void saveModifiedItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, String itemType,
            List<String> originItemUidsToBeRemoved,
            List<ModifiedItem> modifiedItemsToBeAdded,
            HashMap<String, ModifiedItem> changedModifiedItems)
            throws EloraException {
        String logInitMsg = "[saveModifiedItemChangesInCMProcess] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean docModifiedItemsChanged = false;
        boolean bomModifiedItemsChanged = false;
        boolean docImpactedItemsChanged = false;
        boolean bomImpactedItemsChanged = false;

        try {
            if ((originItemUidsToBeRemoved != null
                    && originItemUidsToBeRemoved.size() > 0)
                    || (modifiedItemsToBeAdded != null
                            && modifiedItemsToBeAdded.size() > 0
                            || (changedModifiedItems != null
                                    && changedModifiedItems.size() > 0))) {

                List<String> currentDocDistinctDerivedModifiedItems = null;
                List<String> currentBomDistinctDerivedModifiedItems = null;

                List<String> docDistinctModifiedUidsImpactMatrixToBeRemoved = new ArrayList<String>();
                List<String> bomDistinctModifiedUidsImpactMatrixToBeRemoved = new ArrayList<String>();

                if (originItemUidsToBeRemoved != null
                        && originItemUidsToBeRemoved.size() > 0) {
                    // Calculate derived bom and doc modified items, since they
                    // have to be removed too.
                    currentDocDistinctDerivedModifiedItems = CMQueryResultFactory.getDistinctDerivedModifiedItemsByOriginList(
                            session, cmProcess.getId(),
                            CMConstants.ITEM_TYPE_DOC,
                            originItemUidsToBeRemoved);

                    currentBomDistinctDerivedModifiedItems = CMQueryResultFactory.getDistinctDerivedModifiedItemsByOriginList(
                            session, cmProcess.getId(),
                            CMConstants.ITEM_TYPE_BOM,
                            originItemUidsToBeRemoved);
                }
                ArrayList<HashMap<String, Object>> currentDocModifiedItems = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.DOC_MODIFIED_ITEM_LIST) != null) {
                    currentDocModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.DOC_MODIFIED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newDocModifiedItems = new ArrayList<HashMap<String, Object>>();

                // Current BOMs Modified Items
                ArrayList<HashMap<String, Object>> currentBomModifiedItems = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.BOM_MODIFIED_ITEM_LIST) != null) {
                    currentBomModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.BOM_MODIFIED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newBomModifiedItems = new ArrayList<HashMap<String, Object>>();

                ArrayList<HashMap<String, Object>> newDocImpactedItems = new ArrayList<HashMap<String, Object>>();
                ArrayList<HashMap<String, Object>> newBomImpactedItems = new ArrayList<HashMap<String, Object>>();

                // ----------------------------------------------
                // FIRST PROCES MODIFIED OR REMOVED ITEMS
                // ----------------------------------------------
                if ((originItemUidsToBeRemoved != null
                        && originItemUidsToBeRemoved.size() > 0)
                        || (changedModifiedItems != null
                                && changedModifiedItems.size() > 0)) {

                    if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                        for (int i = 0; i < currentDocModifiedItems.size(); ++i) {
                            HashMap<String, Object> modifiedItem = currentDocModifiedItems.get(
                                    i);
                            String modifiedItemOriginItemUid = (String) modifiedItem.get(
                                    "originItem");
                            if (changedModifiedItems != null
                                    && changedModifiedItems.containsKey(
                                            modifiedItemOriginItemUid)) {
                                ModifiedItem changedModifiedItem = changedModifiedItems.get(
                                        modifiedItemOriginItemUid);

                                // check if this change implies to remove IM
                                boolean needToRemoveIM = checkIfImpactMatrixShouldBeRemoved(
                                        modifiedItem, changedModifiedItem);
                                if (needToRemoveIM) {
                                    docDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                            modifiedItemOriginItemUid);
                                }

                                updateModifiedItemType(modifiedItem,
                                        changedModifiedItem);
                                newDocModifiedItems.add(modifiedItem);

                            } else if ((originItemUidsToBeRemoved != null
                                    && originItemUidsToBeRemoved.contains(
                                            modifiedItemOriginItemUid))
                                    || currentDocDistinctDerivedModifiedItems != null
                                            && currentDocDistinctDerivedModifiedItems.contains(
                                                    modifiedItemOriginItemUid)) {
                                // not include it in this case since it is
                                // removed

                                // then, we should remove its Impact Matrix
                                docDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                        modifiedItemOriginItemUid);
                            } else {
                                newDocModifiedItems.add(modifiedItem);
                            }
                        }
                        docModifiedItemsChanged = true;

                        // TODO::: hau begiratu?????????????????????????????
                        if (currentBomDistinctDerivedModifiedItems != null
                                && currentBomDistinctDerivedModifiedItems.size() > 0) {
                            for (int i = 0; i < currentBomModifiedItems.size(); ++i) {
                                HashMap<String, Object> modifiedItem = currentBomModifiedItems.get(
                                        i);
                                String modifiedItemOriginItemUid = (String) modifiedItem.get(
                                        "originItem");
                                if (currentBomDistinctDerivedModifiedItems != null
                                        && currentBomDistinctDerivedModifiedItems.contains(
                                                modifiedItemOriginItemUid)) {
                                    // not include it in this case since it is
                                    // removed

                                    // then, we should remove its Impact Matrix
                                    bomDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                            modifiedItemOriginItemUid);
                                } else {
                                    newBomModifiedItems.add(modifiedItem);
                                }
                            }
                            bomModifiedItemsChanged = true;
                        } else {
                            newBomModifiedItems = currentBomModifiedItems;
                        }
                    } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                        for (int i = 0; i < currentBomModifiedItems.size(); ++i) {
                            HashMap<String, Object> modifiedItem = currentBomModifiedItems.get(
                                    i);
                            String modifiedItemOriginItemUid = (String) modifiedItem.get(
                                    "originItem");
                            if (changedModifiedItems != null
                                    && changedModifiedItems.containsKey(
                                            modifiedItemOriginItemUid)) {
                                ModifiedItem changedModifiedItem = changedModifiedItems.get(
                                        modifiedItemOriginItemUid);

                                // check if this change implies to remove IM
                                boolean needToRemoveIM = checkIfImpactMatrixShouldBeRemoved(
                                        modifiedItem, changedModifiedItem);
                                if (needToRemoveIM) {
                                    bomDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                            modifiedItemOriginItemUid);
                                }

                                updateModifiedItemType(modifiedItem,
                                        changedModifiedItem);
                                newBomModifiedItems.add(modifiedItem);
                            } else if ((originItemUidsToBeRemoved != null
                                    && originItemUidsToBeRemoved.contains(
                                            modifiedItemOriginItemUid))
                                    || currentBomDistinctDerivedModifiedItems != null
                                            && currentBomDistinctDerivedModifiedItems.contains(
                                                    modifiedItemOriginItemUid)) {
                                // not include it in this case since it is
                                // removed

                                // then, we should remove its Impact Matrix
                                bomDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                        modifiedItemOriginItemUid);
                            } else {
                                newBomModifiedItems.add(modifiedItem);
                            }
                        }
                        bomModifiedItemsChanged = true;

                        // TODO::: hau begiratu?????????????????????????????
                        if (currentDocDistinctDerivedModifiedItems != null
                                && currentDocDistinctDerivedModifiedItems.size() > 0) {
                            for (int i = 0; i < currentDocModifiedItems.size(); ++i) {
                                HashMap<String, Object> modifiedItem = currentDocModifiedItems.get(
                                        i);
                                String modifiedItemOriginItemUid = (String) modifiedItem.get(
                                        "originItem");
                                if (currentDocDistinctDerivedModifiedItems != null
                                        && currentDocDistinctDerivedModifiedItems.contains(
                                                modifiedItemOriginItemUid)) {

                                    // not include it in this case since it is
                                    // removed

                                    // then, we should remove its Impact Matrix
                                    docDistinctModifiedUidsImpactMatrixToBeRemoved.add(
                                            modifiedItemOriginItemUid);
                                } else {
                                    newDocModifiedItems.add(modifiedItem);
                                }
                            }
                            docModifiedItemsChanged = true;
                        } else {
                            newDocModifiedItems = currentDocModifiedItems;
                        }
                    }
                } else {
                    newDocModifiedItems = currentDocModifiedItems;
                    newBomModifiedItems = currentBomModifiedItems;
                }

                // If there is something to be removed from IM, remove it
                if (docDistinctModifiedUidsImpactMatrixToBeRemoved.size() > 0) {
                    // Retrieve current DOCs Impact Matrix
                    ArrayList<HashMap<String, Object>> currentDocImpactedItems = new ArrayList<HashMap<String, Object>>();
                    if (cmProcess.getPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                        currentDocImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                                CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
                    }
                    for (int i = 0; i < currentDocImpactedItems.size(); ++i) {
                        HashMap<String, Object> docImpactedItem = currentDocImpactedItems.get(
                                i);
                        String docImpactedItemModifiedItem = (String) docImpactedItem.get(
                                "modifiedItem");
                        if (docDistinctModifiedUidsImpactMatrixToBeRemoved.contains(
                                docImpactedItemModifiedItem)) {
                            docImpactedItemsChanged = true;
                        } else {
                            newDocImpactedItems.add(docImpactedItem);
                        }
                    }
                }

                if (bomDistinctModifiedUidsImpactMatrixToBeRemoved.size() > 0) {
                    // Retrieve current BOMs Impact Matrix
                    ArrayList<HashMap<String, Object>> currentBomImpactedItems = new ArrayList<HashMap<String, Object>>();
                    if (cmProcess.getPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST) != null) {
                        currentBomImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                                CMMetadataConstants.BOM_IMPACTED_ITEM_LIST);
                    }
                    for (int i = 0; i < currentBomImpactedItems.size(); ++i) {
                        HashMap<String, Object> bomImpactedItem = currentBomImpactedItems.get(
                                i);
                        String bomImpactedItemModifiedItem = (String) bomImpactedItem.get(
                                "modifiedItem");
                        if (bomDistinctModifiedUidsImpactMatrixToBeRemoved.contains(
                                bomImpactedItemModifiedItem)) {
                            bomImpactedItemsChanged = true;
                        } else {
                            newBomImpactedItems.add(bomImpactedItem);
                        }
                    }
                }

                // Then, add the ones to be be added (and calculate derived
                // modified items)
                if (modifiedItemsToBeAdded != null
                        && modifiedItemsToBeAdded.size() > 0) {

                    // ModifiedItemId : originUid
                    ArrayList<String> newDocModifiedItemIds = extractModifiedItemsIdsFromList(
                            newDocModifiedItems);
                    ArrayList<String> newBomModifiedItemIds = extractModifiedItemsIdsFromList(
                            newBomModifiedItems);

                    long nextDocModifiedItemRowNumber = getModifiedItemsMaxRowNumberInCMProcess(
                            session, cmProcess, CMConstants.ITEM_TYPE_DOC) + 1;

                    long nextBomModifiedItemRowNumber = getModifiedItemsMaxRowNumberInCMProcess(
                            session, cmProcess, CMConstants.ITEM_TYPE_BOM) + 1;

                    for (int i = 0; i < modifiedItemsToBeAdded.size(); ++i) {
                        ModifiedItem modifiedItemToBeAdded = modifiedItemsToBeAdded.get(
                                i);
                        String originItemUid = modifiedItemToBeAdded.getOriginItem();

                        DocumentModel originItem = session.getDocument(
                                new IdRef(originItemUid));
                        checkOriginItem(session, originItem);

                        HashMap<String, Object> modifiedItemType = new HashMap<String, Object>();
                        switch (itemType) {
                        case CMConstants.ITEM_TYPE_DOC:
                            modifiedItemToBeAdded.setRowNumber(
                                    nextDocModifiedItemRowNumber);
                            modifiedItemType = createModifiedItemType(
                                    modifiedItemToBeAdded);
                            newDocModifiedItems.add(modifiedItemType);
                            newDocModifiedItemIds.add(getModifiedItemUniqueId(
                                    modifiedItemToBeAdded));
                            docModifiedItemsChanged = true;
                            nextDocModifiedItemRowNumber++;
                            break;
                        case CMConstants.ITEM_TYPE_BOM:
                            modifiedItemToBeAdded.setRowNumber(
                                    nextBomModifiedItemRowNumber);
                            modifiedItemType = createModifiedItemType(
                                    modifiedItemToBeAdded);
                            newBomModifiedItems.add(modifiedItemType);
                            newBomModifiedItemIds.add(getModifiedItemUniqueId(
                                    modifiedItemToBeAdded));
                            bomModifiedItemsChanged = true;
                            nextBomModifiedItemRowNumber++;
                            break;
                        }

                        // TODO:::: hau agian pauso baten egin datieke.
                        // Guztia gehitu eta ondoren kendu beharrean,
                        // bakarrik fan gehitzen ez dagoena jadanik...

                        List<ModifiedItem> newDerivedDocModifiedItems = new ArrayList<ModifiedItem>();
                        List<ModifiedItem> newDerivedBomModifiedItems = new ArrayList<ModifiedItem>();

                        DocumentModel destinationItem = null;
                        String destinationItemUid = modifiedItemToBeAdded.getDestinationItem();
                        if (destinationItemUid != null
                                && !destinationItemUid.isEmpty()) {
                            destinationItem = session.getDocument(
                                    new IdRef(destinationItemUid));
                        }
                        calculateDerivedModifiedItems(session, originItem,
                                destinationItem,
                                modifiedItemToBeAdded.getAction(),
                                modifiedItemToBeAdded.getComment(), itemType,
                                newDerivedDocModifiedItems,
                                newDerivedBomModifiedItems, false);

                        for (int j = 0; j < newDerivedDocModifiedItems.size(); ++j) {
                            ModifiedItem newDerivedDocModifiedItem = newDerivedDocModifiedItems.get(
                                    j);
                            newDerivedDocModifiedItem.getOriginItem();
                            if (!newDocModifiedItemIds.contains(
                                    newDerivedDocModifiedItem.getOriginItem())) {
                                newDerivedDocModifiedItem.setRowNumber(
                                        nextDocModifiedItemRowNumber);
                                HashMap<String, Object> docModifiedItemType = createModifiedItemType(
                                        newDerivedDocModifiedItem);
                                newDocModifiedItems.add(docModifiedItemType);
                                newDocModifiedItemIds.add(
                                        getModifiedItemUniqueId(
                                                newDerivedDocModifiedItem));
                                docModifiedItemsChanged = true;
                                nextDocModifiedItemRowNumber++;
                            }
                        }

                        for (int j = 0; j < newDerivedBomModifiedItems.size(); ++j) {
                            ModifiedItem newDerivedBomModifiedItem = newDerivedBomModifiedItems.get(
                                    j);
                            if (!newBomModifiedItemIds.contains(
                                    newDerivedBomModifiedItem.getOriginItem())) {
                                newDerivedBomModifiedItem.setRowNumber(
                                        nextBomModifiedItemRowNumber);
                                HashMap<String, Object> bomModifiedItemType = createModifiedItemType(
                                        newDerivedBomModifiedItem);
                                newBomModifiedItems.add(bomModifiedItemType);
                                newBomModifiedItemIds.add(
                                        getModifiedItemUniqueId(
                                                newDerivedBomModifiedItem));
                                bomModifiedItemsChanged = true;
                                nextBomModifiedItemRowNumber++;
                            }
                        }
                    }
                }

                ////////////////////////// ////
                // store new calculated items
                ////////////////////////// ////

                if (docModifiedItemsChanged) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.DOC_MODIFIED_ITEM_LIST,
                            newDocModifiedItems);
                }
                if (bomModifiedItemsChanged) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.BOM_MODIFIED_ITEM_LIST,
                            newBomModifiedItems);
                }

                if (docImpactedItemsChanged) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST,
                            newDocImpactedItems);
                }
                if (bomImpactedItemsChanged) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST,
                            newBomImpactedItems);
                }
                session.saveDocument(cmProcess);
                session.save();
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * @param session
     * @param cmProcess
     * @param itemType
     * @param impactedItems
     * @throws EloraException
     */
    public static void saveImpactedItemListInCMProcess(CoreSession session,
            DocumentModel cmProcess, String itemType,
            List<ImpactedItem> impactedItems) throws EloraException {

        String logInitMsg = "[saveImpactedItemListInCMProcess] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Check specified input parameters
        if (cmProcess == null) {
            log.error(logInitMsg + " Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }

        if (impactedItems == null) {
            log.error(logInitMsg + " Specified impactedItems is null.");
            throw new EloraException("Specified impactedItems is null.");
        }

        log.trace(logInitMsg + " Saving |" + impactedItems.size() + "| "
                + itemType + " Impacted items.");

        try {
            ArrayList<HashMap<String, Object>> impactedItemsContent = new ArrayList<HashMap<String, Object>>();

            for (int i = 0; i < impactedItems.size(); ++i) {
                ImpactedItem impactedItem = impactedItems.get(i);
                HashMap<String, Object> impactedItemType = createImpactedItemType(
                        impactedItem);
                impactedItemsContent.add(impactedItemType);
            }

            // Store new impactedItems list in function of the item type
            String impactedItemPropertyName = CMHelper.getImpactedItemListMetadaName(
                    itemType);
            cmProcess.setPropertyValue(impactedItemPropertyName,
                    impactedItemsContent);

            session.saveDocument(cmProcess);
            session.save();

            log.info(logInitMsg + itemType
                    + " Impacted items successfully saved.");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static ArrayList<String> extractModifiedItemsIdsFromList(
            ArrayList<HashMap<String, Object>> modifiedItemsList) {

        ArrayList<String> modifiedItemIds = new ArrayList<String>();

        for (int i = 0; i < modifiedItemsList.size(); ++i) {
            HashMap<String, Object> modifiedItem = modifiedItemsList.get(i);
            String modifiedItemId = getModifiedItemUniqueId(modifiedItem);
            modifiedItemIds.add(modifiedItemId);
        }
        return modifiedItemIds;
    }

    private static String getModifiedItemUniqueId(
            HashMap<String, Object> modifiedItem) {
        String modifiedItemOriginUid = (String) modifiedItem.get("originItem");
        String modifiedItemUniqueId = modifiedItemOriginUid;
        return modifiedItemUniqueId;
    }

    private static String getModifiedItemUniqueId(ModifiedItem modifiedItem) {
        String modifiedItemOriginUid = modifiedItem.getOriginItem();
        String modifiedItemUniqueId = modifiedItemOriginUid;
        return modifiedItemUniqueId;
    }

    private static void calculateDerivedModifiedItems(CoreSession session,
            DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem,
            String modifiedItemAction, String modifiedItemComment,
            String itemType, List<ModifiedItem> docModifiedItems,
            List<ModifiedItem> bomModifiedItems,
            boolean isDerivedFromImpactMatrix) throws EloraException {

        String logInitMsg = "[calculateDerivedModifiedItems] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {

                List<ModifiedItem> derivedDocModifiedItemsList = calculateDerivedDocModifiedItemsListForBom(
                        session, modifiedItemOriginItem,
                        modifiedItemDestinationItem, modifiedItemAction,
                        modifiedItemComment, isDerivedFromImpactMatrix);
                if (derivedDocModifiedItemsList != null
                        && derivedDocModifiedItemsList.size() > 0) {
                    docModifiedItems.addAll(derivedDocModifiedItemsList);
                }

            } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                List<ModifiedItem> derivedBomModifiedItemsList = calculateDerivedBomModifiedItemsListForDoc(
                        session, modifiedItemOriginItem,
                        modifiedItemDestinationItem, modifiedItemAction,
                        modifiedItemComment, isDerivedFromImpactMatrix);
                if (derivedBomModifiedItemsList != null
                        && derivedBomModifiedItemsList.size() > 0) {
                    bomModifiedItems.addAll(derivedBomModifiedItemsList);
                }

                if (EloraDocumentHelper.isCadDocument(modifiedItemOriginItem)) {
                    List<ModifiedItem> derivedDocModifiedItemsList = calculateDerivedDocModifiedItemsListForDoc(
                            session, modifiedItemOriginItem,
                            modifiedItemDestinationItem, modifiedItemAction,
                            modifiedItemComment, isDerivedFromImpactMatrix);
                    if (derivedDocModifiedItemsList != null
                            && derivedDocModifiedItemsList.size() > 0) {
                        docModifiedItems.addAll(derivedDocModifiedItemsList);
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
                    "General exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static List<ModifiedItem> calculateDerivedDocModifiedItemsListForBom(
            CoreSession session, DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem,
            String modifiedItemAction, String modifiedItemComment,
            boolean isDerivedFromImpactMatrix)
            throws EloraException, DocumentUnreadableException {

        List<ModifiedItem> derivedDocModifiedItemsList = new ArrayList<ModifiedItem>();

        for (String relation : RelationsConfig.docRelationsList) {
            Resource predicateResource = new ResourceImpl(relation);

            // Retrieve related Objects
            List<RelatedItemData> derivedRelatedItems = getRelatedItems(session,
                    predicateResource, modifiedItemOriginItem, false, false,
                    false, null);

            // Put the 3D document (CadPart of CadAseembly) at the beginning of
            // the list. In this way, we avoid having dependency problems during
            // massive promote.
            List<RelatedItemData> sortedDerivedRelatedItems = new LinkedList<RelatedItemData>();
            for (RelatedItemData derivedRelatedItem : derivedRelatedItems) {
                DocumentModel modifiedItemDoc = derivedRelatedItem.getDocModel();
                String docType = modifiedItemDoc.getType();
                if (docType.equals(EloraDoctypeConstants.CAD_PART)
                        || docType.equals(EloraDoctypeConstants.CAD_ASSEMBLY)) {
                    sortedDerivedRelatedItems.add(0, derivedRelatedItem);
                } else {
                    sortedDerivedRelatedItems.add(derivedRelatedItem);
                }
            }

            if (sortedDerivedRelatedItems != null
                    && !sortedDerivedRelatedItems.isEmpty()) {
                for (RelatedItemData derivedRelatedItem : sortedDerivedRelatedItems) {
                    DocumentModel derivedModifiedItemDocM = derivedRelatedItem.getDocModel();
                    String derivedModifiedItemType = getItemType(
                            derivedModifiedItemDocM);

                    // for the instance isDirectObject = false
                    ModifiedItem derivedModifiedItem = createDerivedModifiedItem(
                            session, modifiedItemOriginItem,
                            modifiedItemDestinationItem, modifiedItemAction,
                            modifiedItemComment, derivedModifiedItemDocM,
                            relation, false, derivedRelatedItem.getQuantity(),
                            false, false, derivedModifiedItemType,
                            isDerivedFromImpactMatrix);

                    derivedDocModifiedItemsList.add(derivedModifiedItem);
                }
            }
        }

        return derivedDocModifiedItemsList;
    }

    private static List<ModifiedItem> calculateDerivedBomModifiedItemsListForDoc(
            CoreSession session, DocumentModel sourceDocOriginItem,
            DocumentModel sourceDocDestinationItem, String sourceDocAction,
            String sourceDocComment, boolean isDerivedFromImpactMatrix)
            throws EloraException, DocumentUnreadableException {

        List<ModifiedItem> derivedBomModifiedItemsList = new ArrayList<ModifiedItem>();

        ArrayList<String> relations = new ArrayList<String>();
        relations.add(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        relations.add(EloraRelationConstants.BOM_HAS_DOCUMENT);

        for (String relation : relations) {

            Resource predicateResource = new ResourceImpl(relation);

            // Retrieve related Subjects
            List<RelatedItemData> derivedRelatedItems = getRelatedItems(session,
                    predicateResource, sourceDocOriginItem, true, false, false,
                    null);

            if (derivedRelatedItems != null && !derivedRelatedItems.isEmpty()) {
                for (RelatedItemData derivedRelatedItem : derivedRelatedItems) {
                    DocumentModel derivedModifiedItemDocM = derivedRelatedItem.getDocModel();
                    String derivedModifiedItemType = getItemType(
                            derivedModifiedItemDocM);

                    // for the instance isDirectObject = false
                    ModifiedItem derivedModifiedItem = createDerivedModifiedItem(
                            session, sourceDocOriginItem,
                            sourceDocDestinationItem, sourceDocAction,
                            sourceDocComment, derivedModifiedItemDocM, relation,
                            true, derivedRelatedItem.getQuantity(), false,
                            false, derivedModifiedItemType,
                            isDerivedFromImpactMatrix);

                    derivedBomModifiedItemsList.add(derivedModifiedItem);
                }
            }
        }

        return derivedBomModifiedItemsList;
    }

    private static List<ModifiedItem> calculateDerivedDocModifiedItemsListForDoc(
            CoreSession session, DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem,
            String modifiedItemAction, String modifiedItemComment,
            boolean isDerivedFromImpactMatrix)
            throws EloraException, DocumentUnreadableException {

        List<ModifiedItem> derivedDocModifiedItemsList = new ArrayList<ModifiedItem>();

        // merge cadSpecial and cadDirect relations
        ArrayList<String> relations = new ArrayList<String>();
        relations.addAll(RelationsConfig.cadSpecialRelationsList);
        relations.addAll(RelationsConfig.cadDirectRelationsList);
        // we cannot use a relation list because we don't need all document
        // relations, just the CAD-DOC one
        relations.add(EloraRelationConstants.CAD_HAS_DOCUMENT);

        for (String relation : relations) {

            Resource predicateResource = new ResourceImpl(relation);

            // Retrieve related Objects
            List<RelatedItemData> derivedRelatedItems = getRelatedItems(session,
                    predicateResource, modifiedItemOriginItem, false, false,
                    false, null);

            if (derivedRelatedItems != null && !derivedRelatedItems.isEmpty()) {
                for (RelatedItemData derivedRelatedItem : derivedRelatedItems) {
                    DocumentModel derivedModifiedItemDocM = derivedRelatedItem.getDocModel();
                    String derivedModifiedItemType = getItemType(
                            derivedModifiedItemDocM);

                    // for the instance isDirectObject = false
                    ModifiedItem derivedModifiedItem = createDerivedModifiedItem(
                            session, modifiedItemOriginItem,
                            modifiedItemDestinationItem, modifiedItemAction,
                            modifiedItemComment, derivedModifiedItemDocM,
                            relation, false, derivedRelatedItem.getQuantity(),
                            false, false, derivedModifiedItemType,
                            isDerivedFromImpactMatrix);

                    derivedDocModifiedItemsList.add(derivedModifiedItem);
                }
            }

            // Retrieve related Subjects
            derivedRelatedItems = getRelatedItems(session, predicateResource,
                    modifiedItemOriginItem, true, false, false, null);

            if (derivedRelatedItems != null && !derivedRelatedItems.isEmpty()) {
                for (RelatedItemData derivedRelatedItem : derivedRelatedItems) {
                    DocumentModel derivedModifiedItemDocM = derivedRelatedItem.getDocModel();
                    String derivedModifiedItemType = getItemType(
                            derivedModifiedItemDocM);

                    // for the instance isDirectObject = false
                    ModifiedItem derivedModifiedItem = createDerivedModifiedItem(
                            session, modifiedItemOriginItem,
                            modifiedItemDestinationItem, modifiedItemAction,
                            modifiedItemComment, derivedModifiedItemDocM,
                            relation, true, derivedRelatedItem.getQuantity(),
                            false, false, derivedModifiedItemType,
                            isDerivedFromImpactMatrix);

                    derivedDocModifiedItemsList.add(derivedModifiedItem);
                }
            }
        }

        return derivedDocModifiedItemsList;
    }

    private static DocumentModel calculateDerivedDestinationItem(
            CoreSession session, DocumentModel modifiedItemDestination,
            DocumentModel modifiedItemOriginItem,
            String derivedModifiedItemPredicate,
            boolean derivedModifiedItemIsSubject)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateDerivedDestinationItem] ["
                + session.getPrincipal().getName() + "] ";

        DocumentModel destinationItem = null;

        // First retrieve derived destination items
        Resource predicateResource = new ResourceImpl(
                derivedModifiedItemPredicate);
        QNameResource docResource = RelationHelper.getDocumentResource(
                modifiedItemDestination);

        List<DocumentModel> destinationItemsList = new ArrayList<DocumentModel>();

        Graph graph = RelationHelper.getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);

        List<Statement> stmts = derivedModifiedItemIsSubject
                ? graph.getStatements(null, predicateResource, docResource)
                : graph.getStatements(docResource, predicateResource, null);

        if (stmts != null) {
            String modifiedItemOriginItemType = modifiedItemOriginItem.getType();

            for (Statement stmt : stmts) {
                Node node = derivedModifiedItemIsSubject ? stmt.getSubject()
                        : stmt.getObject();

                DocumentModel d = RelationHelper.getDocumentModel(node,
                        modifiedItemDestination.getCoreSession());
                if (d != null) {
                    String dType = d.getType();
                    if ((dType.equals(EloraDoctypeConstants.CAD_PART)
                            || dType.equals(EloraDoctypeConstants.CAD_ASSEMBLY))
                            && (modifiedItemOriginItemType.equals(
                                    EloraDoctypeConstants.CAD_PART)
                                    || modifiedItemOriginItemType.equals(
                                            EloraDoctypeConstants.CAD_ASSEMBLY))) {
                        destinationItemsList.add(d);
                    } else if (dType.equals(modifiedItemOriginItemType)) {
                        destinationItemsList.add(d);
                    }
                } else {
                    log.trace(logInitMsg
                            + "Throw DocumentUnreadableException since relatedItem is null. stmt = |"
                            + stmt.toString() + "|");
                    throw new DocumentUnreadableException(
                            "Error getting document from statement |"
                                    + stmt.toString() + "|");
                }
            }
        }

        // Then, filter retrieved derived destination items. If the same
        // document appears as subject in
        // different versions, get only the latest version.
        // Ignore the obsolete and the deleted ones ones.
        // if modifiedItemDestination is WC, derivedItemDestination should be WC
        if (destinationItemsList != null && destinationItemsList.size() > 0) {
            destinationItemsList = filterDerivedDestinationItems(session,
                    modifiedItemDestination, destinationItemsList);
        }
        if (destinationItemsList != null && destinationItemsList.size() == 1) {
            destinationItem = destinationItemsList.get(0);
        }

        return destinationItem;
    }

    private static List<DocumentModel> filterDerivedDestinationItems(
            CoreSession session, DocumentModel modifiedItemDestination,
            List<DocumentModel> derivedDestinationItemsList)
            throws EloraException {

        String logInitMsg = "[filterDerivedDestinationItems] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<DocumentModel> filteredDerivedDestinationItems = new LinkedList<DocumentModel>();

        try {
            if (derivedDestinationItemsList != null
                    && !derivedDestinationItemsList.isEmpty()) {

                log.trace(logInitMsg + "derivedDestinationItemsList.size = |"
                        + derivedDestinationItemsList.size() + "|");

                // If there is only one, check if it is valid
                if (derivedDestinationItemsList.size() == 1) {
                    DocumentModel derivedDestinationDocM = derivedDestinationItemsList.get(
                            0);
                    if (checkDerivedDestinationItem(session,
                            modifiedItemDestination.isVersion(),
                            derivedDestinationDocM)) {
                        filteredDerivedDestinationItems.add(
                                derivedDestinationDocM);
                    }
                } else {
                    // Store all the documents in a HashMap for retrieving them
                    // easily without searching in the DB.
                    // --- key: UID of the document
                    // --- value: DocumentModel
                    Map<String, DocumentModel> derivedDestinationItemsMap = new HashMap<String, DocumentModel>();
                    // Insert document list in a HashMap, classified by its
                    // versionableId
                    // --- key: versionableId
                    // --- value: list of UID having the same versionableId
                    Map<String, List<String>> versionableIdMap = new LinkedHashMap<String, List<String>>();
                    structureDerivedItemsAndRemoveObsoletesAndDeletes(session,
                            modifiedItemDestination.isVersion(),
                            derivedDestinationItemsList,
                            derivedDestinationItemsMap, versionableIdMap);

                    // Now start filtering the result
                    if (versionableIdMap != null
                            && !versionableIdMap.isEmpty()) {
                        Iterator<Entry<String, List<String>>> it = versionableIdMap.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, List<String>> entry = it.next();
                            List<String> uidList = entry.getValue();
                            if (uidList != null) {
                                if (uidList.size() == 1) {
                                    String docUid = uidList.get(0);
                                    DocumentModel docM = derivedDestinationItemsMap.get(
                                            docUid);
                                    filteredDerivedDestinationItems.add(docM);
                                } else {
                                    // retrieve the latest released version or
                                    // if it does not exist the latest related
                                    // version
                                    DocumentModel latestDoc = null;
                                    String query = EloraQueryFactory.getRelatedReleasedDocQuery(
                                            uidList);
                                    DocumentModelList relatedReleasedDocs = session.query(
                                            query);
                                    if (relatedReleasedDocs.size() > 0) {
                                        latestDoc = relatedReleasedDocs.get(0);
                                    } else {
                                        // retrieve the latest related version
                                        query = EloraQueryFactory.getLatestRelatedDocQuery(
                                                uidList, false);
                                        relatedReleasedDocs = session.query(
                                                query);
                                        if (relatedReleasedDocs.size() > 0) {
                                            latestDoc = relatedReleasedDocs.get(
                                                    0);
                                        }
                                    }
                                    if (latestDoc != null) {
                                        filteredDerivedDestinationItems.add(
                                                latestDoc);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        log.trace(logInitMsg
                + "--- EXIT --- with filteredDerivedDestinationItems.size = |"
                + filteredDerivedDestinationItems.size() + "|");

        return filteredDerivedDestinationItems;
    }

    private static boolean checkDerivedDestinationItem(CoreSession session,
            boolean modifiedItemDestinationIsVersion,
            DocumentModel derivedDestinationDocM) {

        boolean isValid = false;

        // the document state cannot be obsolete or delete
        String derivedDestinationDocMLifeCycleState = derivedDestinationDocM.getCurrentLifeCycleState();
        if (!LifecyclesConfig.obsoleteStatesList.contains(
                derivedDestinationDocMLifeCycleState)
                && !derivedDestinationDocMLifeCycleState.equals(
                        EloraLifeCycleConstants.NX_DELETED)) {

            // if the modifiedItemDestination is a WC, the derived destination
            // item should also be a WC, otherwise it should be a version
            if (!modifiedItemDestinationIsVersion
                    && !derivedDestinationDocM.isVersion()) {
                isValid = true;
            } else if (modifiedItemDestinationIsVersion
                    && derivedDestinationDocM.isVersion()) {
                // if it is a version, its WC state cannot be obsolete or delete
                // retrieve its WC and check its status
                DocumentModel derivedItemWc = session.getWorkingCopy(
                        derivedDestinationDocM.getRef());
                String derivedItemWcLifeCycleState = derivedItemWc.getCurrentLifeCycleState();
                if (!LifecyclesConfig.obsoleteStatesList.contains(
                        derivedItemWcLifeCycleState)
                        && !derivedItemWcLifeCycleState.equals(
                                EloraLifeCycleConstants.NX_DELETED)) {
                    isValid = true;
                }
            }
        }

        return isValid;
    }

    private static void structureDerivedItemsAndRemoveObsoletesAndDeletes(
            CoreSession session, boolean modifiedItemDestinationIsVersion,
            List<DocumentModel> derivedDestinationItemsList,
            Map<String, DocumentModel> derivedDestinationItemsMap,
            Map<String, List<String>> versionableIdMap) {

        for (DocumentModel derivedItem : derivedDestinationItemsList) {

            boolean isValid = checkDerivedDestinationItem(session,
                    modifiedItemDestinationIsVersion, derivedItem);

            if (isValid) {
                String docUid = derivedItem.getId();
                derivedDestinationItemsMap.put(docUid, derivedItem);

                if (derivedItem.isVersion()) {

                    DocumentModel derivedItemWc = session.getWorkingCopy(
                            derivedItem.getRef());

                    String docVersionableId = derivedItemWc.getId();

                    if (versionableIdMap.containsKey(docVersionableId)) {
                        List<String> uidList = versionableIdMap.get(
                                docVersionableId);
                        uidList.add(docUid);
                        versionableIdMap.put(docVersionableId, uidList);
                    } else {
                        List<String> uidList = new ArrayList<String>();
                        uidList.add(docUid);
                        versionableIdMap.put(docVersionableId, uidList);
                    }
                } else {
                    List<String> uidList = new ArrayList<String>();
                    uidList.add(docUid);
                    versionableIdMap.put(docUid, uidList);
                }
            }
        }
    }

    public static void calculateImpactMatrix(CoreSession session,
            DocumentModel cmProcess, String itemType) throws EloraException {

        String logInitMsg = "[calculateImpactMatrix] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();

            // Current Impact Matrix
            ArrayList<HashMap<String, Object>> currentImpactMatrix = new ArrayList<HashMap<String, Object>>();

            String modifiedItemListMetada = getModifiedItemListMetadaName(
                    itemType);
            String impactedItemListMetadata = getImpactedItemListMetadaName(
                    itemType);

            boolean calculateIM = false;

            // First, retrieve modified items. Check that there is at least one
            // modified element having the attribute Include in IM set to true.
            // Otherwise, the new impact matrix should be empty.
            if (cmProcess.getPropertyValue(modifiedItemListMetada) != null) {
                currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        modifiedItemListMetada);
            }

            for (int i = 0; i < currentModifiedItems.size(); ++i) {
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                boolean includeInImpactMatrix = (boolean) modifiedItem.get(
                        "includeInImpactMatrix");
                if (includeInImpactMatrix) {
                    calculateIM = true;
                }
            }

            ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

            if (calculateIM) {
                if (cmProcess.getPropertyValue(
                        impactedItemListMetadata) != null) {
                    currentImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            impactedItemListMetadata);
                }

                log.trace("currentModifiedItems.size() = |"
                        + currentModifiedItems.size()
                        + "|, currentImpactMatrix.size() = |"
                        + currentImpactMatrix.size() + "|");

                if (currentImpactMatrix.size() > 0) {
                    newImpactMatrix = recalculateAndMergeImpactMatrix(session,
                            cmProcess, currentModifiedItems,
                            currentImpactMatrix, itemType);
                } else {
                    newImpactMatrix = calculateImpactMatrix(session, cmProcess,
                            currentModifiedItems, itemType);
                }
            }

            // -------------------------------------------------------------------
            // Store new calculated impact matrix
            // -------------------------------------------------------------------
            cmProcess.setPropertyValue(impactedItemListMetadata,
                    newImpactMatrix);

            session.saveDocument(cmProcess);
            session.save();

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static String getModifiedItemListMetadaName(String itemType)
            throws EloraException {
        String modifiedItemListMetada = "";
        switch (itemType) {
        case CMConstants.ITEM_TYPE_DOC:
            modifiedItemListMetada = CMMetadataConstants.DOC_MODIFIED_ITEM_LIST;
            break;
        case CMConstants.ITEM_TYPE_BOM:
            modifiedItemListMetada = CMMetadataConstants.BOM_MODIFIED_ITEM_LIST;
            break;
        default:
            String exceptionMessage = "Incorrect itemType. Specified type is |"
                    + itemType + "| and allowed types are ["
                    + CMConstants.ITEM_TYPE_BOM + ", "
                    + CMConstants.ITEM_TYPE_DOC + "]";
            throw new EloraException(exceptionMessage);
        }

        return modifiedItemListMetada;

    }

    public static String getImpactedItemListMetadaName(String itemType)
            throws EloraException {
        String impactedItemListMetadata = "";

        switch (itemType) {
        case CMConstants.ITEM_TYPE_DOC:
            impactedItemListMetadata = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
            break;
        case CMConstants.ITEM_TYPE_BOM:
            impactedItemListMetadata = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
            break;
        default:
            String exceptionMessage = "Incorrect itemType. Specified type is |"
                    + itemType + "| and allowed types are ["
                    + CMConstants.ITEM_TYPE_BOM + ", "
                    + CMConstants.ITEM_TYPE_DOC + "]";
            throw new EloraException(exceptionMessage);
        }

        return impactedItemListMetadata;
    }

    private static ArrayList<HashMap<String, Object>> calculateImpactMatrix(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            String itemType) throws EloraException {

        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        String logInitMsg = "[calculateImpactMatrix] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

            for (int i = 0; i < currentModifiedItems.size(); ++i) {
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                boolean includeInImpactMatrix = (boolean) modifiedItem.get(
                        "includeInImpactMatrix");
                if (includeInImpactMatrix) {

                    String modifiedItemNodeId = (String) modifiedItem.get(
                            "nodeId");

                    String modifiedItemAction = (String) modifiedItem.get(
                            "action");

                    String modifiedItemOriginItemUid = (String) modifiedItem.get(
                            "originItem");

                    String modifiedItemDestinationItemWcUid = (String) modifiedItem.get(
                            "destinationItemWc");

                    DocumentModel modifiedItemOriginItem = session.getDocument(
                            new IdRef(modifiedItemOriginItemUid));

                    String modifiedItemNodePath = modifiedItemOriginItem.getId();

                    // ??????????????? granParent = null
                    ImpactedItemContext impactContext = new ImpactedItemContext(
                            modifiedItemOriginItem, null,
                            modifiedItemOriginItem, new Long(0),
                            modifiedItemNodePath, modifiedItemNodeId,
                            modifiedItemAction,
                            modifiedItemDestinationItemWcUid,
                            modifiedItemAction);

                    if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {

                        impactedItems.addAll(calculateImpactedItemsForBom(
                                session, cmProcess, impactContext));

                    } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {

                        impactedItems.addAll(calculateImpactedItemsForDoc(
                                session, cmProcess, impactContext));
                    }
                }
            }

            for (int i = 0; i < impactedItems.size(); ++i) {
                ImpactedItem impactedItem = impactedItems.get(i);
                HashMap<String, Object> impactedItemType = createImpactedItemType(
                        impactedItem);
                newImpactMatrix.add(impactedItemType);
            }

        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Permission exception thrown: |" + e.getMessage() + "|");
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "General exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return newImpactMatrix;
    }

    private static ArrayList<HashMap<String, Object>> recalculateAndMergeImpactMatrix(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            String itemType) throws EloraException {
        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        String logInitMsg = "[recalculateAndMergeImpactMatrix] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        newImpactMatrix = recalculateImpactedItems(session, cmProcess,
                currentModifiedItems, currentImpactMatrix, itemType);

        log.trace(logInitMsg + "--- EXIT --- ");

        return newImpactMatrix;
    }

    private static ArrayList<HashMap<String, Object>> recalculateImpactedItems(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            String itemType) throws EloraException {

        String logInitMsg = "[recalculateImpactedItems] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            boolean hasSomethingChanged = false;

            // First convert Impact matrix data to a structured list of
            // ImpactedItems
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap = convertImpactMatrixIntoImpactedItemsListMap(
                    currentImpactMatrix);

            // -------------------------------------------------------------------
            // Iterate over each modifiedItem and calculate its impactMatrix
            // -------------------------------------------------------------------
            for (int i = 0; i < currentModifiedItems.size(); ++i) {

                boolean hasSomethingChangedInsideModifiedItem = false;

                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                boolean includeInImpactMatrix = (boolean) modifiedItem.get(
                        "includeInImpactMatrix");
                String modifiedItemOriginItemUid = (String) modifiedItem.get(
                        "originItem");

                boolean existInCurrentImpactMatrix = impactedItemsListMap.containsKey(
                        modifiedItemOriginItemUid);

                // If the modified element has the attribute Include in IM set
                // to false and its impacted items are already in the current
                // matrix,
                // remove them.
                if (!includeInImpactMatrix) {

                    if (existInCurrentImpactMatrix) {
                        // Remove the impacted items related to this modified
                        // item
                        impactedItemsListMap.remove(modifiedItemOriginItemUid);
                        hasSomethingChangedInsideModifiedItem = true;
                    }
                    // else, nothing to do
                }

                // If the modified element has the attribute Include in IM set
                // to true
                else {

                    // calculate new Impact matrix for this modified item
                    String modifiedItemNodeId = (String) modifiedItem.get(
                            "nodeId");
                    String modifiedItemAction = (String) modifiedItem.get(
                            "action");
                    String modifiedItemDestinationItemWcUid = (String) modifiedItem.get(
                            "destinationItemWc");

                    DocumentModel modifiedItemOriginItem = session.getDocument(
                            new IdRef(modifiedItemOriginItemUid));

                    String modifiedItemNodePath = modifiedItemOriginItem.getId();

                    // granParent = null
                    ImpactedItemContext impactContext = new ImpactedItemContext(
                            modifiedItemOriginItem, null,
                            modifiedItemOriginItem, new Long(0),
                            modifiedItemNodePath, modifiedItemNodeId,
                            modifiedItemAction,
                            modifiedItemDestinationItemWcUid,
                            modifiedItemAction);

                    List<ImpactedItem> newImpactedItemsByModifiedItem = new ArrayList<ImpactedItem>();

                    if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                        newImpactedItemsByModifiedItem = calculateImpactedItemsForBom(
                                session, cmProcess, impactContext);

                    } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                        newImpactedItemsByModifiedItem = calculateImpactedItemsForDoc(
                                session, cmProcess, impactContext);
                    }

                    // if new impacted items don't exist in the current matrix,
                    // add them.
                    if (!existInCurrentImpactMatrix) {
                        addModifiedItemImpactedItems(impactedItemsListMap,
                                modifiedItemOriginItemUid,
                                newImpactedItemsByModifiedItem);
                        hasSomethingChangedInsideModifiedItem = true;
                    }
                    // if new impacted items exist in the current matrix, merge
                    // them.
                    // merge means that only new elements will be added, but
                    // nothing will be removed.
                    else {
                        hasSomethingChangedInsideModifiedItem = mergeModifiedItemImpactedItems(
                                session, impactedItemsListMap,
                                modifiedItemOriginItemUid,
                                newImpactedItemsByModifiedItem);
                    }

                    // Apply POST Processor
                    applyPostProcessingRulesInRecalculatedImpactMatrix(session,
                            cmProcess.getId(), impactedItemsListMap, itemType);
                }
                if (hasSomethingChangedInsideModifiedItem) {
                    hasSomethingChanged = true;
                }
            }
            if (hasSomethingChanged) {
                currentImpactMatrix = convertImpactedItemsListMapIntoImpactMatrix(
                        impactedItemsListMap);
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "General exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return currentImpactMatrix;
    }

    private static void addModifiedItemImpactedItems(
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap,
            String modifiedItem,
            List<ImpactedItem> newImpactedItemsByModifiedItem)
            throws EloraException {

        HashMap<String, ArrayList<ImpactedItem>> modifiedItemImpactedItems = new HashMap<String, ArrayList<ImpactedItem>>();

        for (ImpactedItem impactedItem : newImpactedItemsByModifiedItem) {

            if (!modifiedItemImpactedItems.containsKey(
                    impactedItem.getParentNodeId())) {
                ArrayList<ImpactedItem> impactedItemsList = new ArrayList<ImpactedItem>();
                impactedItemsList.add(impactedItem);

                modifiedItemImpactedItems.put(impactedItem.getParentNodeId(),
                        impactedItemsList);
            } else {

                ArrayList<ImpactedItem> impactedItemsList = modifiedItemImpactedItems.get(
                        impactedItem.getParentNodeId());
                impactedItemsList.add(impactedItem);

                // Sort impactedItems by rowNumber
                Collections.sort(impactedItemsList, (o1,
                        o2) -> o1.getRowNumber().compareTo(o2.getRowNumber()));
            }
        }
        impactedItemsListMap.put(modifiedItem, modifiedItemImpactedItems);
    }

    private static boolean mergeModifiedItemImpactedItems(CoreSession session,
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap,
            String modifiedItem,
            List<ImpactedItem> newImpactedItemsByModifiedItem)
            throws EloraException {

        boolean hasSomethingChanged = false;

        HashMap<String, ArrayList<ImpactedItem>> modifiedItemImpactedItems = impactedItemsListMap.get(
                modifiedItem);

        // Firstly, divide into two groups the impacted items:
        // - impacted items which parent is not yet in the matrix => they will
        // be directly added
        // - impacted items which parent is already in the matrix => they need
        // to be merged
        List<ImpactedItem> impactedItemsWithNewParent = new ArrayList<ImpactedItem>();
        List<ImpactedItem> impactedItemsWithExistingParent = new ArrayList<ImpactedItem>();

        for (ImpactedItem impactedItem : newImpactedItemsByModifiedItem) {
            if (modifiedItemImpactedItems.containsKey(
                    impactedItem.getParentNodeId())) {
                impactedItemsWithExistingParent.add(impactedItem);
            } else {
                impactedItemsWithNewParent.add(impactedItem);
            }
        }

        // Add to the matrix items with new parent
        for (ImpactedItem impactedItem : impactedItemsWithNewParent) {

            if (!modifiedItemImpactedItems.containsKey(
                    impactedItem.getParentNodeId())) {
                ArrayList<ImpactedItem> impactedItemsList = new ArrayList<ImpactedItem>();
                impactedItemsList.add(impactedItem);

                modifiedItemImpactedItems.put(impactedItem.getParentNodeId(),
                        impactedItemsList);

            } else {

                ArrayList<ImpactedItem> impactedItemsList = modifiedItemImpactedItems.get(
                        impactedItem.getParentNodeId());
                impactedItemsList.add(impactedItem);

                // Sort impactedItems by rowNumber
                Collections.sort(impactedItemsList, (o1,
                        o2) -> o1.getRowNumber().compareTo(o2.getRowNumber()));

            }
            hasSomethingChanged = true;
        }

        // Merge into the matrix items with existing parent
        for (ImpactedItem impactedItem : impactedItemsWithExistingParent) {

            ArrayList<ImpactedItem> impactedItemsList = modifiedItemImpactedItems.get(
                    impactedItem.getParentNodeId());

            // If the items is already contained in the matrix, there is nothing
            // to do.
            // If it is not contained, we have to add it in the right order
            if (!isContainedInImpactedItemsList(impactedItem.getNodeId(),
                    impactedItemsList)) {

                Long rowNumber = new Long(0);
                // calculate the rowNumber of the new element
                String impactedItemOriginItemUid = impactedItem.getOriginItem();

                DocumentModel impactedItemOriginItem = session.getDocument(
                        new IdRef(impactedItemOriginItemUid));
                String impactedItemOriginItemType = null;
                if (impactedItemOriginItem != null) {
                    impactedItemOriginItemType = impactedItemOriginItem.getType();
                }

                if (impactedItemOriginItemType != null
                        && impactedItemOriginItemType.equals(
                                EloraDoctypeConstants.CAD_DRAWING)) {
                    // first position
                    rowNumber = new Long(1);

                    // TODO:: here, we can increment he row position of the
                    // other ones

                } else {
                    // last position
                    rowNumber = new Long(impactedItemsList.size() + 1);
                }
                impactedItem.setRowNumber(rowNumber);

                impactedItemsList.add(impactedItem);

                // Sort impactedItems by rowNumber
                Collections.sort(impactedItemsList, (o1,
                        o2) -> o1.getRowNumber().compareTo(o2.getRowNumber()));

                hasSomethingChanged = true;
            }
        }

        impactedItemsListMap.put(modifiedItem, modifiedItemImpactedItems);

        return hasSomethingChanged;
    }

    private static boolean isContainedInImpactedItemsList(String nodeId,
            List<ImpactedItem> impactedItemList) {
        boolean isContained = false;

        for (ImpactedItem impactedItem : impactedItemList) {

            if (impactedItem.getNodeId().equals(nodeId)) {
                isContained = true;
                break;
            }
        }

        return isContained;
    }

    private static List<ImpactedItem> calculateImpactedItemsForBom(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpactedItemsForBom] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

        try {
            // Calculate the impacted matrix for the specified modified item

            // find out its related Direct BOMs
            impactedItems.addAll(calculateImpactedDirectBomsForBom(session,
                    cmProcess, impactContext));

            // find out its related Direct Anarchic and Hierarchical BOMs
            impactedItems.addAll(
                    calculateImpactedDirectAnarchicAndHierarchicalBomsForBom(
                            session, cmProcess, impactContext));

            log.trace(logInitMsg + "Number of impacted items  = |"
                    + impactedItems.size() + "|");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return impactedItems;
    }

    private static List<ImpactedItem> calculateImpactedItemsForDoc(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpactedItemsForDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ImpactedItem> impactedItems = new ArrayList<ImpactedItem>();

        try {
            // Calculate the impacted matrix for the specified modified item
            if (EloraDocumentHelper.isCadDocument(
                    impactContext.getModifiedItem())) {

                impactedItems = calculateImpactedHierarchicalCadsForCadDocument(
                        session, cmProcess, impactContext);
            }

            log.trace(logInitMsg + "Number of impacted items  = |"
                    + impactedItems.size() + "|");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return impactedItems;
    }

    private static HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> convertImpactMatrixIntoImpactedItemsListMap(
            List<HashMap<String, Object>> impactMatrix) throws EloraException {

        HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap = new HashMap<String, HashMap<String, ArrayList<ImpactedItem>>>();

        for (HashMap<String, Object> impactedItem : impactMatrix) {

            ImpactedItem impactedItemTyped = createImpactedItem(impactedItem);

            if (!impactedItemsListMap.containsKey(
                    impactedItemTyped.getModifiedItem())) {
                HashMap<String, ArrayList<ImpactedItem>> modifiedItemHM = new HashMap<String, ArrayList<ImpactedItem>>();

                ArrayList<ImpactedItem> impactedItemsList = new ArrayList<ImpactedItem>();
                impactedItemsList.add(impactedItemTyped);

                modifiedItemHM.put(impactedItemTyped.getParentNodeId(),
                        impactedItemsList);

                impactedItemsListMap.put(impactedItemTyped.getModifiedItem(),
                        modifiedItemHM);

            } else {
                HashMap<String, ArrayList<ImpactedItem>> modifiedItemHM = impactedItemsListMap.get(
                        impactedItemTyped.getModifiedItem());
                if (!modifiedItemHM.containsKey(
                        impactedItemTyped.getParentNodeId())) {
                    ArrayList<ImpactedItem> impactedItemsList = new ArrayList<ImpactedItem>();
                    impactedItemsList.add(impactedItemTyped);

                    modifiedItemHM.put(impactedItemTyped.getParentNodeId(),
                            impactedItemsList);
                } else {

                    ArrayList<ImpactedItem> impactedItemsList = modifiedItemHM.get(
                            impactedItemTyped.getParentNodeId());
                    impactedItemsList.add(impactedItemTyped);

                    // Sort impactedItems by rowNumber
                    Collections.sort(impactedItemsList,
                            (o1, o2) -> o1.getRowNumber().compareTo(
                                    o2.getRowNumber()));
                }
            }
        }
        return impactedItemsListMap;
    }

    /**
     * This method applies some processing rules after recalculating an impact
     * matrix.
     *
     * @param impactedItemsListMap
     * @return
     * @throws EloraException
     */
    private static void applyPostProcessingRulesInRecalculatedImpactMatrix(
            CoreSession session, String cmProcessUid,
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap,
            String itemType) throws EloraException {
        String logInitMsg = "[applyPostProcessingRulesInRecalculatedImpactMatrix] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // For the instance, post processing rules are only defined for
        // Recalculated Documents Impact Matrix
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            applyPostProcessingRulesInRecalculatedDocImpactMatrix(session,
                    cmProcessUid, impactedItemsListMap, itemType);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * This method applies some processing rules after recalculating a Document
     * impact matrix.
     *
     * @param session
     * @param cmProcess
     * @param impactedItemsListMap
     * @param itemType
     * @throws EloraException
     */
    private static void applyPostProcessingRulesInRecalculatedDocImpactMatrix(
            CoreSession session, String cmProcessUid,
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap,
            String itemType) throws EloraException {

        String logInitMsg = "[applyPostProcessingRulesInRecalculatedDocImpactMatrix] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- cmProcessUid = |" + cmProcessUid
                + "|");

        // Iterate over each element of the impact matrix:
        for (String modifiedItem : impactedItemsListMap.keySet()) {
            HashMap<String, ArrayList<ImpactedItem>> modifiedItemImpactedItems = impactedItemsListMap.get(
                    modifiedItem);

            for (String parentNodeId : modifiedItemImpactedItems.keySet()) {

                ArrayList<ImpactedItem> impactedItems = modifiedItemImpactedItems.get(
                        parentNodeId);

                for (ImpactedItem impactedItem : impactedItems) {

                    // First, check if the impacted item element is NOT IGNORED
                    // or MANAGED.
                    if (!impactedItem.getAction().equals(
                            CMConstants.ACTION_IGNORE)
                            && !impactedItem.isManaged()) {

                        // Then, check if its related BOM items are IGNORED in
                        // the BOM
                        // impact matrix of this CMProcess.
                        DocumentModel impactedItemOrigin = session.getDocument(
                                new IdRef(impactedItem.getOriginItem()));
                        String checkImpactedOriginResultMsg = checkImpactedDocOriginWithRelatedBomActions(
                                session, cmProcessUid, impactedItemOrigin);

                        // If its related BOMS are IGNORED, IGNORE also this DOC
                        // element, but DO NOT propagate the IGNORE action to
                        // its children.
                        if (checkImpactedOriginResultMsg != null
                                && checkImpactedOriginResultMsg.length() > 0) {
                            impactedItem.setAction(CMConstants.ACTION_IGNORE);
                            impactedItem.setDestinationItem(null);
                            impactedItem.setDestinationItemWc(null);
                            impactedItem.setManaged(true);
                            impactedItem.setComment(
                                    checkImpactedOriginResultMsg);

                            log.trace(logInitMsg + "originItem = |"
                                    + impactedItem.getOriginItem()
                                    + "| action changed to IGNORE. ");
                        }
                    }
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static ArrayList<HashMap<String, Object>> convertImpactedItemsListMapIntoImpactMatrix(
            HashMap<String, HashMap<String, ArrayList<ImpactedItem>>> impactedItemsListMap)
            throws EloraException {

        ArrayList<HashMap<String, Object>> impactMatrix = new ArrayList<HashMap<String, Object>>();

        for (String modifiedItem : impactedItemsListMap.keySet()) {
            HashMap<String, ArrayList<ImpactedItem>> modifiedItemImpactedItems = impactedItemsListMap.get(
                    modifiedItem);

            for (String parentNodeId : modifiedItemImpactedItems.keySet()) {

                ArrayList<ImpactedItem> impactedItems = modifiedItemImpactedItems.get(
                        parentNodeId);

                for (ImpactedItem impactedItem : impactedItems) {
                    HashMap<String, Object> impactedItemType = createImpactedItemType(
                            impactedItem);
                    impactMatrix.add(impactedItemType);
                }
            }
        }
        return impactMatrix;
    }

    private static List<ImpactedItem> calculateImpactedDirectBomsForBom(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        List<ImpactedItem> impactedDirectCads = new ArrayList<ImpactedItem>();

        for (String relation : RelationsConfig.bomDirectRelationsList) {

            Resource predicateResource = new ResourceImpl(relation);

            // In this case, we retrieve the OBJECT of the relation
            boolean isAnarchic = false;
            boolean isDirectObject = true;

            // We must filter that impactedSubjects are not the same as the
            // grandparent node
            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), false,
                    false, true, impactContext.getGrandParentItem());

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {
                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = generateNodeId(nodePath);
                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                            session, impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItemDoc);

                    if (checkImpactedOriginResultMsg != null
                            && checkImpactedOriginResultMsg.length() > 0) {
                        impactedItem = createIgnoredImpactedItem(session,
                                cmProcess, impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                checkImpactedOriginResultMsg);
                    } else {
                        impactedItem = createImpactedItem(session, cmProcess,
                                impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false);
                    }

                    impactedDirectCads.add(impactedItem);
                }
            }
        }

        return impactedDirectCads;
    }

    private static List<ImpactedItem> calculateImpactedDirectAnarchicAndHierarchicalBomsForBom(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        List<ImpactedItem> impactedDirectAnarchicAndHierarchicalBomsList = new ArrayList<ImpactedItem>();

        // merge bomDirect, bomAnarchic and bomHierarchiccal relations
        ArrayList<String> bomDirectAnarchicAndHierarchicalRelations = new ArrayList<String>();
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomDirectRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomAnarchicRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomHierarchicalRelationsList);

        for (String relation : bomDirectAnarchicAndHierarchicalRelations) {
            Resource predicateResource = new ResourceImpl(relation);

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    false, false, null);

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {

                boolean isAnarchic = RelationsConfig.bomAnarchicRelationsList.contains(
                        relation) ? true : false;
                boolean isDirectObject = false;

                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = generateNodeId(nodePath);
                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                            session, impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItemDoc);

                    if (checkImpactedOriginResultMsg == null
                            || checkImpactedOriginResultMsg.length() == 0) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithParent(
                                session, impactContext.getParentItem(),
                                impactedItemDoc, relation);
                    }

                    if (checkImpactedOriginResultMsg != null
                            && checkImpactedOriginResultMsg.length() > 0) {
                        impactedItem = createIgnoredImpactedItem(session,
                                cmProcess, impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                checkImpactedOriginResultMsg);
                    } else {
                        impactedItem = createImpactedItem(session, cmProcess,
                                impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false);
                    }

                    impactedDirectAnarchicAndHierarchicalBomsList.add(
                            impactedItem);

                    ImpactedItemContext childImpactContext = new ImpactedItemContext(
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(), impactedItemDoc,
                            new Long(0), nodePath, nodeId,

                            impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItem.getAction());

                    // find out its related Direct BOMs
                    List<ImpactedItem> directBoms = calculateImpactedDirectBomsForBom(
                            session, cmProcess, childImpactContext);
                    impactedDirectAnarchicAndHierarchicalBomsList.addAll(
                            directBoms);

                    // find out its related Direct, Anarchic and
                    // Hierarchical Boms (recursive call)
                    List<ImpactedItem> recursiveDirectAndAnarchiclBoms = calculateImpactedDirectAnarchicAndHierarchicalBomsForBom(
                            session, cmProcess, childImpactContext);
                    impactedDirectAnarchicAndHierarchicalBomsList.addAll(
                            recursiveDirectAndAnarchiclBoms);

                }
            }
        }
        return impactedDirectAnarchicAndHierarchicalBomsList;

    }

    private static List<ImpactedItem> calculateImpactedSpecialCadsForCadDocument(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        List<ImpactedItem> impactedSpecialCads = new ArrayList<ImpactedItem>();

        for (String relation : RelationsConfig.cadSpecialRelationsList) {

            Resource predicateResource = new ResourceImpl(relation);

            boolean isAnarchic = false;
            boolean isDirectObject = false;

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    true, false, null);

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {
                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = generateNodeId(nodePath);

                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                            session, impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItemDoc);

                    if (checkImpactedOriginResultMsg != null
                            && checkImpactedOriginResultMsg.length() > 0) {
                        impactedItem = createIgnoredImpactedItem(session,
                                cmProcess, impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                checkImpactedOriginResultMsg);
                    } else {
                        impactedItem = createImpactedItem(session, cmProcess,
                                impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false);
                    }

                    impactedSpecialCads.add(impactedItem);
                }
            }
        }

        return impactedSpecialCads;
    }

    private static List<ImpactedItem> calculateImpactedDirectCadsForCadDocument(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        List<ImpactedItem> impactedDirectCads = new ArrayList<ImpactedItem>();

        for (String relation : RelationsConfig.cadDirectRelationsList) {

            Resource predicateResource = new ResourceImpl(relation);

            boolean isAnarchic = false;
            boolean isDirectObject = false;

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    false, false, null);

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {
                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = generateNodeId(nodePath);
                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                            session, impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItemDoc);

                    if (checkImpactedOriginResultMsg != null
                            && checkImpactedOriginResultMsg.length() > 0) {
                        impactedItem = createIgnoredImpactedItem(session,
                                cmProcess, impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                checkImpactedOriginResultMsg);
                    } else {
                        impactedItem = createImpactedItem(session, cmProcess,
                                impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false);
                    }

                    impactedDirectCads.add(impactedItem);
                }
            }
        }

        return impactedDirectCads;
    }

    private static List<ImpactedItem> calculateImpactedHierarchicalCadsForCadDocument(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext)
            throws EloraException, DocumentUnreadableException {

        List<ImpactedItem> hierarchicalCads = new ArrayList<ImpactedItem>();

        // find out its related Hierarchical CADs
        for (String relation : RelationsConfig.cadHierarchicalRelationsList) {

            Resource predicateResource = new ResourceImpl(relation);

            boolean isAnarchic = false;
            boolean isDirectObject = false;

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    false, false, null);

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {
                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();

                    // add the hierarchical one
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = generateNodeId(nodePath);
                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                            session, impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItemDoc);

                    if (checkImpactedOriginResultMsg == null
                            || checkImpactedOriginResultMsg.length() == 0) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithParent(
                                session, impactContext.getParentItem(),
                                impactedItemDoc, relation);

                        if (checkImpactedOriginResultMsg == null
                                || checkImpactedOriginResultMsg.length() == 0) {
                            checkImpactedOriginResultMsg = checkImpactedDocOriginWithRelatedBomActions(
                                    session, cmProcess.getId(),
                                    impactedItemDoc);
                        }
                    }

                    if (checkImpactedOriginResultMsg != null
                            && checkImpactedOriginResultMsg.length() > 0) {
                        impactedItem = createIgnoredImpactedItem(session,
                                cmProcess, impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                checkImpactedOriginResultMsg);
                    } else {
                        impactedItem = createImpactedItem(session, cmProcess,
                                impactContext.getRowNumber(), nodeId,
                                impactContext.getParentNodeId(),
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(),
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false);
                    }

                    hierarchicalCads.add(impactedItem);

                    ImpactedItemContext childImpactContext = new ImpactedItemContext(
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(), impactedItemDoc,
                            new Long(0), nodePath, nodeId,
                            impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItem.getAction());

                    // find out its related Special CADs
                    List<ImpactedItem> specialCads = calculateImpactedSpecialCadsForCadDocument(
                            session, cmProcess, childImpactContext);
                    hierarchicalCads.addAll(specialCads);

                    // find out its related Direct CADs
                    List<ImpactedItem> directCads = calculateImpactedDirectCadsForCadDocument(
                            session, cmProcess, childImpactContext);
                    hierarchicalCads.addAll(directCads);

                    // find out its related Hierarchical CADs(recursive
                    // call)
                    List<ImpactedItem> recursiveHierarchicalCads = calculateImpactedHierarchicalCadsForCadDocument(
                            session, cmProcess, childImpactContext);
                    hierarchicalCads.addAll(recursiveHierarchicalCads);

                }
            }
        }
        return hierarchicalCads;
    }

    /////////////////////////////////////////////////////////

    private static List<RelatedItemData> getRelatedItems(CoreSession session,
            Resource predicate, DocumentModel sourceDocument,
            boolean retrieveSubject, boolean filterByLatestVersion,
            boolean filterByGrandParent, DocumentModel grandParentDocument)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[getRelatedItems] ["
                + session.getPrincipal().getName() + "] ";

        List<RelatedItemData> relatedItems = null;

        // First retrieve the Related Items
        QNameResource docResource = RelationHelper.getDocumentResource(
                sourceDocument);

        Graph graph = RelationHelper.getRelationManager().getGraphByName(
                EloraRelationConstants.ELORA_GRAPH_NAME);

        List<Statement> stmts = retrieveSubject
                ? graph.getStatements(null, predicate, docResource)
                : graph.getStatements(docResource, predicate, null);

        if (stmts != null) {
            relatedItems = new LinkedList<RelatedItemData>();

            for (Statement stmt : stmts) {

                Node node = retrieveSubject ? stmt.getSubject()
                        : stmt.getObject();
                DocumentModel d = RelationHelper.getDocumentModel(node,
                        sourceDocument.getCoreSession());

                if (d != null) {
                    // Check that retrieved document is not the same as the
                    // grandparent, in order to avoid circular dependences
                    if (!filterByGrandParent || grandParentDocument == null
                            || !d.getVersionSeriesId().equals(
                                    grandParentDocument.getVersionSeriesId())) {
                        EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                                stmt);
                        RelatedItemData impSubject = new RelatedItemData(d,
                                stmtInfo.getQuantity());
                        relatedItems.add(impSubject);
                    }
                } else {
                    log.trace(logInitMsg
                            + "Throw DocumentUnreadableException since relatedItem is null. stmt = |"
                            + stmt.toString() + "|");
                    throw new DocumentUnreadableException(
                            "Error getting document from statement |"
                                    + stmt.toString() + "|");
                }
            }
        }

        // Then, filter the subjects. If the same document appears as subject in
        // different versions, get only the latest version
        // Ignore the obsolete and the deleted ones.
        if (relatedItems != null && relatedItems.size() > 0) {
            relatedItems = filterRelatedItems(session, relatedItems,
                    filterByLatestVersion);
        }

        return relatedItems;
    }

    /**
     * This method filters a given list of related items. If the same document
     * appears more than once in the list, it takes only one version of that
     * document. The selected version depends on the byLatestVersion parameter.
     *
     * @param session
     * @param relatedItemsData
     * @param filterByLatestVersion this filter indicates which version will be
     *            retrieved. If true, get the LATEST RELATED version. Otherwise,
     *            get the LATEST RELEASED RELATED version.
     * @return
     * @throws EloraException
     */
    private static List<RelatedItemData> filterRelatedItems(CoreSession session,
            List<RelatedItemData> relatedItemsData,
            boolean filterByLatestVersion) throws EloraException {

        String logInitMsg = "[filterRelatedItems] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<RelatedItemData> filteredImpactedItems = new LinkedList<RelatedItemData>();

        try {
            if (relatedItemsData != null && !relatedItemsData.isEmpty()) {

                log.trace(logInitMsg + "relatedItemsData.size = |"
                        + relatedItemsData.size() + "|");

                // If there is only one, check if it is valid
                if (relatedItemsData.size() == 1) {
                    RelatedItemData relatedItem = relatedItemsData.get(0);
                    DocumentModel relatedItemDocM = relatedItem.getDocModel();
                    if (checkRelatedItem(session, relatedItemDocM)) {
                        filteredImpactedItems.add(relatedItem);
                    }
                } else {
                    // Store all the documents in a HashMap for retrieving them
                    // easily without searching in the DB.
                    // --- key: UID of the document
                    // --- value: ImpactedSubject
                    Map<String, RelatedItemData> relatedItemsMap = new HashMap<String, RelatedItemData>();
                    // Insert document list in a HashMap, classified by its
                    // versionableId
                    // --- key: versionableId
                    // --- value: list of UID having the same versionableId
                    Map<String, List<String>> versionableIdMap = new LinkedHashMap<String, List<String>>();
                    structureRelatedItemsAndRemoveObsoletesAndDeletes(session,
                            relatedItemsData, relatedItemsMap,
                            versionableIdMap);

                    // Now start filtering the result
                    if (versionableIdMap != null
                            && !versionableIdMap.isEmpty()) {
                        Iterator<Entry<String, List<String>>> it = versionableIdMap.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, List<String>> entry = it.next();
                            List<String> uidList = entry.getValue();
                            if (uidList != null) {
                                if (uidList.size() == 1) {
                                    String docUid = uidList.get(0);
                                    RelatedItemData impSubject = relatedItemsMap.get(
                                            docUid);
                                    DocumentModel docM = impSubject.getDocModel();
                                    if (docM.isVersion()) {
                                        filteredImpactedItems.add(impSubject);
                                    }
                                } else {
                                    RelatedItemData latestImpactedSubject = null;
                                    DocumentModel latestDoc = null;
                                    if (filterByLatestVersion) {
                                        // retrieve the latest related version
                                        String query = EloraQueryFactory.getLatestRelatedDocQuery(
                                                uidList, false);
                                        DocumentModelList relatedReleasedDocs = session.query(
                                                query);
                                        if (relatedReleasedDocs.size() > 0) {
                                            latestDoc = relatedReleasedDocs.get(
                                                    0);
                                        }
                                    } else {
                                        // retrieve the latest released version
                                        // or if it does not exist the latest
                                        // related version
                                        String query = EloraQueryFactory.getRelatedReleasedDocQuery(
                                                uidList);
                                        DocumentModelList relatedReleasedDocs = session.query(
                                                query);
                                        if (relatedReleasedDocs.size() > 0) {
                                            latestDoc = relatedReleasedDocs.get(
                                                    0);
                                        } else {
                                            // retrieve the latest related
                                            // version
                                            query = EloraQueryFactory.getLatestRelatedDocQuery(
                                                    uidList, false);
                                            relatedReleasedDocs = session.query(
                                                    query);
                                            if (relatedReleasedDocs.size() > 0) {
                                                latestDoc = relatedReleasedDocs.get(
                                                        0);
                                            }
                                        }
                                    }

                                    if (latestDoc != null) {
                                        latestImpactedSubject = relatedItemsMap.get(
                                                latestDoc.getId());
                                        filteredImpactedItems.add(
                                                latestImpactedSubject);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        log.trace(
                logInitMsg + "--- EXIT --- with filteredImpactedItems.size = |"
                        + filteredImpactedItems.size() + "|");

        return filteredImpactedItems;
    }

    private static boolean checkRelatedItem(CoreSession session,
            DocumentModel relatedItemDocM) {

        boolean isValid = false;

        // the document must be a version
        // its state cannot be obsolete or delete
        // its WC state cannot be obsolete nor delete
        String relatedItemDocMLifeCycleState = relatedItemDocM.getCurrentLifeCycleState();
        if (relatedItemDocM.isVersion()
                && !LifecyclesConfig.obsoleteStatesList.contains(
                        relatedItemDocMLifeCycleState)
                && !relatedItemDocMLifeCycleState.equals(
                        EloraLifeCycleConstants.NX_DELETED)) {
            // retrieve its WC and check its status
            DocumentModel docMWc = session.getWorkingCopy(
                    relatedItemDocM.getRef());
            String docMWcLifeCycleState = docMWc.getCurrentLifeCycleState();
            if (!LifecyclesConfig.obsoleteStatesList.contains(
                    docMWcLifeCycleState)
                    && !docMWcLifeCycleState.equals(
                            EloraLifeCycleConstants.NX_DELETED)) {
                isValid = true;
            }
        }

        return isValid;
    }

    private static void structureRelatedItemsAndRemoveObsoletesAndDeletes(
            CoreSession session, List<RelatedItemData> relatedItemsData,
            Map<String, RelatedItemData> relatedItemsMap,
            Map<String, List<String>> versionableIdMap) {

        for (RelatedItemData relatedItem : relatedItemsData) {

            DocumentModel relatedItemDocM = relatedItem.getDocModel();

            boolean isValid = checkRelatedItem(session, relatedItemDocM);

            if (isValid) {
                String docUid = relatedItemDocM.getId();
                relatedItemsMap.put(docUid, relatedItem);

                DocumentModel impSubjectDocMWc = session.getWorkingCopy(
                        relatedItemDocM.getRef());
                String docVersionableId = impSubjectDocMWc.getId();

                if (versionableIdMap.containsKey(docVersionableId)) {
                    List<String> uidList = versionableIdMap.get(
                            docVersionableId);
                    uidList.add(docUid);
                    versionableIdMap.put(docVersionableId, uidList);
                } else {
                    List<String> uidList = new ArrayList<String>();
                    uidList.add(docUid);
                    versionableIdMap.put(docVersionableId, uidList);
                }
            }
        }
    }

    ///////////////////////////////////////////////////////

    /**
     * This methods returns an ascending ordered list of versions of the
     * specified document.
     *
     * @param session
     * @param docWcUid
     * @param onlyReleasedVersion
     * @return
     * @throws EloraException
     */
    public static Map<String, String> calculateOriginVersionList(
            CoreSession session, String docWcUid, boolean onlyReleasedVersion,
            Map<String, String> messages) throws EloraException {

        String logInitMsg = "[calculateOriginVersionList] ["
                + session.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, String> versionList = new LinkedHashMap<String, String>();

        try {
            DocumentModelList originVersionList = new DocumentModelListImpl();

            if (onlyReleasedVersion) {
                DocumentModel wcDoc = session.getDocument(new IdRef(docWcUid));
                if (wcDoc == null) {
                    log.error(logInitMsg + "Document with uid |" + docWcUid
                            + "| does not exist.");
                    throw new EloraException("Document with uid |" + docWcUid
                            + "| does not exist.");
                }

                String query = EloraQueryFactory.getReleasedDocsQuery(
                        wcDoc.getType(), docWcUid,
                        QueriesConstants.SORT_ORDER_ASC);

                originVersionList = session.query(query);
            } else {

                List<DocumentModel> versions = session.getVersions(
                        new IdRef(docWcUid));
                if (versions != null && versions.size() > 0) {
                    for (int i = 0; i < versions.size(); i++) {
                        originVersionList.add(versions.get(i));
                    }
                }
            }

            fillVersionList(versionList, originVersionList, messages);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        // log.trace(logInitMsg + "--- EXIT --- ");
        return versionList;
    }

    /**
     * This methods returns an ascending ordered list list of the versions of
     * the specified document. This list excludes the obsolete and DELETED
     * versions. This list includes: - all the RELEASED versions - if the last
     * major is not in a RELEASED state, retrieve all the versions related to
     * that major (released or not) - if there is not any released version, all
     * current versions of the document
     *
     * @param session current session.
     * @param docWcUid Working Copy of the document which version list will be
     *            calculated.
     * @return
     * @throws EloraException
     */

    public static Map<String, String> calculateModifiableItemVersionList(
            CoreSession session, String docWcUid, Map<String, String> messages)
            throws EloraException {

        String logInitMsg = "[calculateModifiableItemVersionList] ["
                + session.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, String> versionList = new LinkedHashMap<String, String>();
        try {
            DocumentRef wcRef = new IdRef(docWcUid);
            DocumentModel wcDoc = session.getDocument(wcRef);
            if (wcDoc != null) {

                String wcDocType = wcDoc.getType();

                // if the WC state is an OBSOLETE or DELETED state, ignore the
                // document and all its versions.
                if (!LifecyclesConfig.obsoleteAndDeletedStatesList.contains(
                        wcDoc.getCurrentLifeCycleState())) {
                    Long wcMajorVersion = (Long) wcDoc.getPropertyValue(
                            NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

                    String query = EloraQueryFactory.getReleasedDocsQuery(
                            wcDocType, docWcUid,
                            QueriesConstants.SORT_ORDER_ASC);
                    DocumentModelList releasedDocs = session.query(query);

                    if (releasedDocs != null && releasedDocs.size() > 0) {
                        boolean completed = false;
                        for (DocumentModel releasedDoc : releasedDocs) {
                            String releasedDocMajor = releasedDoc.getPropertyValue(
                                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION).toString();
                            if (releasedDocMajor.equals(
                                    wcMajorVersion.toString())) {
                                // If one of them has majorVersion then finish
                                completed = true;
                                break;
                            }
                        }
                        if (!completed) {
                            // If no one has majorVersion then get all versions
                            // within major, except the obsolete ones
                            String majorVersionDocsQuery = EloraQueryFactory.getMajorVersionDocsQuery(
                                    wcDocType, docWcUid, wcMajorVersion, false,
                                    QueriesConstants.SORT_ORDER_ASC);
                            DocumentModelList majorVersionDocs = session.query(
                                    majorVersionDocsQuery);
                            if (majorVersionDocs != null
                                    && majorVersionDocs.size() > 0) {
                                releasedDocs.addAll(majorVersionDocs);
                            }
                        }
                    } else {
                        // If there is no released docs, get all versions
                        String allVersionsDocsQuery = EloraQueryFactory.getAllVersionsDocsQuery(
                                wcDocType, docWcUid, false,
                                QueriesConstants.SORT_ORDER_ASC);

                        DocumentModelList allVersionDocs = session.query(
                                allVersionsDocsQuery);

                        if (allVersionDocs != null
                                && allVersionDocs.size() > 0) {
                            releasedDocs.addAll(allVersionDocs);
                        }
                    }
                    fillVersionList(versionList, releasedDocs, messages);
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }
        return versionList;
    }

    private static void fillVersionList(Map<String, String> versionList,
            DocumentModelList docsList, Map<String, String> messages) {

        if (docsList != null && docsList.size() > 0) {

            for (DocumentModel doc : docsList) {
                String docUid = doc.getId();
                String versionLabel = doc.getVersionLabel();
                String lifeCycleState = messages.get(
                        doc.getCurrentLifeCycleState()
                                + EloraLifeCycleConstants.ABBR_SUFFIX);
                versionList.put(docUid,
                        versionLabel + "   [" + lifeCycleState + "]");
            }
        }
    }

    /**
     * Returns the last element containted in a version list. If the list is
     * null or empty, returns a null value.
     *
     * @param versionList
     * @return
     */
    public static String getLastElementValueFromVersionList(
            Map<String, String> versionList) {

        String lastElementValue = null;

        if (versionList != null && !versionList.isEmpty()) {
            Set<String> versionListKeys = versionList.keySet();
            Object[] versionListKeysArray = versionListKeys.toArray();

            lastElementValue = (String) versionListKeysArray[versionList.size()
                    - 1];
        }

        return lastElementValue;
    }

    // TODO::: agian izena aldatu honi???
    private static CMImpactableConfig getImpactableConfig(String docTypeName,
            String action) throws EloraException {

        String logInitMsg = "[getImpactableConfig] ";

        DocumentType docType = EloraDocumentTypesHelper.getDocumentType(
                docTypeName);
        if (EloraDocumentTypesHelper.getDocumentType(
                EloraDoctypeConstants.BOM_PART).isSuperTypeOf(docType)) {
            docTypeName = EloraDoctypeConstants.BOM_PART;
        }

        String configId = docTypeName + "_" + action;

        CMImpactableConfig impactableConfigData = CMConfig.docTypeActionsImpactConfigMap.get(
                configId);

        if (impactableConfigData == null) {
            String errorMsg = "No impactable configuration found for docType = |"
                    + docTypeName + "|, action = |" + action + "|";
            log.trace(logInitMsg + errorMsg);
            throw new EloraException(errorMsg);
        }

        return impactableConfigData;
    }

    public static boolean getIsImpactable(String docType, String action) {

        boolean isImpactable = false;

        try {
            CMImpactableConfig impactableConfig = getImpactableConfig(docType,
                    action);

            isImpactable = impactableConfig.getIsImpactable();

        } catch (EloraException e) {
            log.trace("[getIsImpactable] NOT impactable, since  doctype= |"
                    + docType
                    + "| is not defined in elora_cm_doctype_actions_impact.");
        }

        return isImpactable;

    }

    public static boolean getIsImpactable(String docType, String action,
            String destinationItemUid) throws EloraException {

        if (action.equals(CMConstants.ACTION_REPLACE)
                && (destinationItemUid == null
                        || destinationItemUid.isEmpty())) {
            return false;
        }
        return getIsImpactable(docType, action);
    }

    public static boolean getIncludeInImpactMatrixDefaultValue(String docType,
            String action, String destinationItemUid) {

        boolean includeInImpactMatrixDefaultValue = false;

        if (action.equals(CMConstants.ACTION_REPLACE)
                && (destinationItemUid == null
                        || destinationItemUid.isEmpty())) {
            return includeInImpactMatrixDefaultValue;
        }

        CMImpactableConfig impactableConfig;
        try {
            impactableConfig = getImpactableConfig(docType, action);

            includeInImpactMatrixDefaultValue = impactableConfig.getIncludeInImpactMatrixDefaultValue();

        } catch (EloraException e) {
            log.trace(
                    "[getIncludeInImpactMatrixDefaultValue] NOT impactable, since  doctype= |"
                            + docType
                            + "| is not defined in elora_cm_doctype_actions_impact.");
        }

        return includeInImpactMatrixDefaultValue;
    }

    public static String generateNodeId(String nodePath) {

        long h = 98764321261L;
        int l = nodePath.length();
        char[] chars = nodePath.toCharArray();

        for (int i = 0; i < l; i++) {
            h = 31 * h + chars[i];
        }

        String nodeId = String.valueOf(h);

        log.debug("**************** nodePath = |" + nodePath
                + "| ===>   nodeId = |" + nodeId + "|");

        return nodeId;
    }

    public static String getProcessesByModifiedItemOriginQuery(
            DocumentModel doc) throws EloraException {

        String itemType = CMHelper.getItemType(doc);

        String query = CMQueryFactory.getProcessesByModifiedItemOriginQuery(
                doc.getId(), itemType);

        return query;
    }

    public static String getProcessesByModifiedItemDestinationQuery(
            DocumentModel doc) throws EloraException {

        String itemType = CMHelper.getItemType(doc);

        String query = CMQueryFactory.getProcessesByModifiedItemDestinationQuery(
                doc.getId(), itemType);

        return query;
    }

    public static String getProcessesByModifiedItemOriginWcQuery(
            DocumentModel docWc) throws EloraException {

        String itemType = CMHelper.getItemType(docWc);

        String query = CMQueryFactory.getProcessesByModifiedItemOriginWcQuery(
                docWc.getId(), itemType);

        return query;
    }

    public static String getProcessesByModifiedItemDestinationWcQuery(
            DocumentModel docWc) throws EloraException {

        String itemType = CMHelper.getItemType(docWc);

        String query = CMQueryFactory.getProcessesByModifiedItemDestinationWcQuery(
                docWc.getId(), itemType);

        return query;
    }

    public static String getProcessesByImpactedItemOriginQuery(
            DocumentModel doc) throws EloraException {

        String itemType = CMHelper.getItemType(doc);

        String query = CMQueryFactory.getProcessesByImpactedItemOriginQuery(
                doc.getId(), itemType);

        return query;
    }

    public static String getProcessesByImpactedItemDestinationQuery(
            DocumentModel doc) throws EloraException {

        String itemType = CMHelper.getItemType(doc);

        String query = CMQueryFactory.getProcessesByImpactedItemDestinationQuery(
                doc.getId(), itemType);

        return query;
    }

    public static String getProcessesByImpactedItemOriginWcQuery(
            DocumentModel docWc) throws EloraException {

        String itemType = CMHelper.getItemType(docWc);

        String query = CMQueryFactory.getProcessesByImpactedItemOriginWcQuery(
                docWc.getId(), itemType);

        return query;
    }

    public static String getProcessesByImpactedItemDestinationWcQuery(
            DocumentModel docWc) throws EloraException {

        String itemType = CMHelper.getItemType(docWc);

        String query = CMQueryFactory.getProcessesByImpactedItemDestinationWcQuery(
                docWc.getId(), itemType);

        return query;
    }

    // ----------------------------------------------------

    /**
     *
     * Sets as managed the nodeId specified of a given cmProcess.
     *
     * @param session
     * @param cmProcess
     * @param itemType
     * @param nodeId
     * @throws EloraException
     */
    public static void setAsManagedModifiedItemByNodeId(CoreSession session,
            DocumentModel cmProcess, String itemType, String nodeId)
            throws EloraException {

        String logInitMsg = "[setAsManagedModifiedItemByNodeId] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcess = |" + cmProcess + "|, itemType = |"
                + itemType + "|, nodeId = |" + nodeId + "|");

        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();

            String modifiedItemListMetada = getModifiedItemListMetadaName(
                    itemType);

            if (cmProcess.getPropertyValue(modifiedItemListMetada) != null) {
                currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        modifiedItemListMetada);
            }

            if (currentModifiedItems != null
                    && currentModifiedItems.size() > 0) {

                ArrayList<HashMap<String, Object>> newModifiedItems = setAsManaged(
                        session, currentModifiedItems, nodeId);

                // -------------------------------------------------------------------
                // Store new calculated modified items
                // -------------------------------------------------------------------
                cmProcess.setPropertyValue(modifiedItemListMetada,
                        newModifiedItems);

                session.saveDocument(cmProcess);
                session.save();

            } else {
                log.trace(
                        "currentModifiedItems is empty. Nothing to be set as managed");
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static ArrayList<HashMap<String, Object>> setAsManaged(
            CoreSession session,
            ArrayList<HashMap<String, Object>> modifiedItems, String nodeId)
            throws EloraException {

        String logInitMsg = "[setAsManaged]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newModifiedItems = new ArrayList<HashMap<String, Object>>();

        try {

            if (modifiedItems == null || modifiedItems.size() == 0) {
                log.trace(
                        "modifiedItems is empty. Nothing has to be set as managed");
            } else if (nodeId == null || nodeId.length() == 0) {
                log.trace("nodeId is empty. Nothing has to be set as managed");
            } else {

                for (int i = 0; i < modifiedItems.size(); ++i) {
                    HashMap<String, Object> modifiedItem = modifiedItems.get(i);

                    boolean isManaged = (boolean) modifiedItem.get("isManaged");
                    String curretNodeId = (String) modifiedItem.get("nodeId");

                    // if modifiedItem is not already managed and nodeId
                    // is the specified one
                    if (!isManaged && nodeId.equals(curretNodeId)) {

                        setAsManagedCMItem(session, modifiedItem);
                    }
                    newModifiedItems.add(modifiedItem);
                }
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "General exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return newModifiedItems;
    }

    public static void setAsManagedItemsByDestinationWcUidList(
            CoreSession session, DocumentModel cmProcess, String itemType,
            String itemClass, List<String> destinationWcUidList)
            throws EloraException {

        String logInitMsg = "[setAsManagedItemsByDestinationWcUidList] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcess = |" + cmProcess + "|, itemType = |"
                + itemType + "|, itemClass = |" + itemClass
                + "|, destinationWcUidList size = |"
                + destinationWcUidList.size() + "|");

        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentItems = new ArrayList<HashMap<String, Object>>();

            String itemListMetada = "";

            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                itemListMetada = getImpactedItemListMetadaName(itemType);
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                itemListMetada = getModifiedItemListMetadaName(itemType);
            }

            if (cmProcess.getPropertyValue(itemListMetada) != null) {
                currentItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        itemListMetada);
            }

            if (currentItems != null && currentItems.size() > 0) {

                ArrayList<HashMap<String, Object>> newItems = setAsManaged(
                        session, currentItems, destinationWcUidList);

                // -------------------------------------------------------------------
                // Store new calculated impacted items
                // -------------------------------------------------------------------
                cmProcess.setPropertyValue(itemListMetada, newItems);

                session.saveDocument(cmProcess);
                session.save();
            } else {
                log.trace(
                        "currentItems is empty. Nothing to be set as managed");
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /*public static void setAsManagedModifiedItemsByDestinationWcUidList(
            CoreSession session, DocumentModel cmProcess, String itemType,
            List<String> destinationWcUidList) throws EloraException {
    
        String logInitMsg = "[setAsManagedModifiedItemsByDestinationWcUidList] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcess = |" + cmProcess + "|, itemType = |"
                + itemType + "|, destinationWcUidList size = |"
                + destinationWcUidList.size() + "|");
    
        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
    
            String modifiedItemListMetada = getModifiedItemListMetadaName(
                    itemType);
    
            if (cmProcess.getPropertyValue(modifiedItemListMetada) != null) {
                currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        modifiedItemListMetada);
            }
    
            if (currentModifiedItems != null
                    && currentModifiedItems.size() > 0) {
    
                ArrayList<HashMap<String, Object>> newModifiedItems = setAsManaged(
                        session, currentModifiedItems, destinationWcUidList);
    
                // -------------------------------------------------------------------
                // Store new calculated impacted items
                // -------------------------------------------------------------------
                cmProcess.setPropertyValue(modifiedItemListMetada,
                        newModifiedItems);
    
                session.saveDocument(cmProcess);
                session.save();
            } else {
                log.trace(
                        "currentModifiedItems is empty. Nothing to be set as managed");
            }
    
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }
    
    public static void setAsManagedImpactedItemsByDestinationWcUidList(
            CoreSession session, DocumentModel cmProcess, String itemType,
            List<String> destinationWcUidList) throws EloraException {
    
        String logInitMsg = "[setAsManagedImpactedItemsByDestinationWcUidList] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcess = |" + cmProcess + "|, itemType = |"
                + itemType + "|, destinationWcUidList size = |"
                + destinationWcUidList.size() + "|");
    
        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentImpactedItems = new ArrayList<HashMap<String, Object>>();
    
            String impactedItemListMetada = getImpactedItemListMetadaName(
                    itemType);
    
            if (cmProcess.getPropertyValue(impactedItemListMetada) != null) {
                currentImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        impactedItemListMetada);
            }
    
            if (currentImpactedItems != null
                    && currentImpactedItems.size() > 0) {
    
                ArrayList<HashMap<String, Object>> newImpactedItems = setAsManaged(
                        session, currentImpactedItems, destinationWcUidList);
    
                // -------------------------------------------------------------------
                // Store new calculated impacted items
                // -------------------------------------------------------------------
                cmProcess.setPropertyValue(impactedItemListMetada,
                        newImpactedItems);
    
                session.saveDocument(cmProcess);
                session.save();
            } else {
                log.trace(
                        "currentImpactedItems is empty. Nothing to be set as managed");
            }
    
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }*/

    private static ArrayList<HashMap<String, Object>> setAsManaged(
            CoreSession session,
            ArrayList<HashMap<String, Object>> impactMatrix,
            List<String> destinationWcUidList) throws EloraException {

        String logInitMsg = "[setAsManaged]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        try {

            if (impactMatrix == null || impactMatrix.size() == 0) {
                log.trace(
                        "currentImpactMatrix is empty. Nothing has to be set as managed");
            } else if (destinationWcUidList == null
                    || destinationWcUidList.size() == 0) {
                log.trace(
                        "destinationWcUidList is empty. Nothing has to be set as managed");
            } else {

                for (int i = 0; i < impactMatrix.size(); ++i) {
                    HashMap<String, Object> impactedItem = impactMatrix.get(i);

                    boolean isManaged = (boolean) impactedItem.get("isManaged");
                    String destinationItemWc = (String) impactedItem.get(
                            "destinationItemWc");

                    // if impactedItem is not already managed and destinationWc
                    // is contained in the specified destinationWcUidList
                    if (!isManaged && destinationWcUidList.contains(
                            destinationItemWc)) {

                        setAsManagedCMItem(session, impactedItem);
                    }
                    newImpactMatrix.add(impactedItem);
                }
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "General exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return newImpactMatrix;
    }

    private static void setAsManagedCMItem(CoreSession session,
            HashMap<String, Object> cmItem) throws EloraException {

        String logInitMsg = "[setAsManagedCMItem]";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace("set as Managed cmItem with nodeId = |"
                + (String) cmItem.get("nodeId") + "|");

        // change destinationItem to its AV and set the impacted
        // item as managed
        String currentDestinationItemUid = (String) cmItem.get(
                "destinationItem");
        if (currentDestinationItemUid == null
                || currentDestinationItemUid.isEmpty()) {
            log.error(logInitMsg + "destinationItem shouldn't be null.");
            throw new EloraException("destinationItem shouldn't be null.");
        }
        DocumentModel currentDestinationItem = session.getDocument(
                new IdRef(currentDestinationItemUid));

        if (currentDestinationItem == null
                || currentDestinationItem.isVersion()) {
            log.error(logInitMsg
                    + "destinationItem should be a working copy. Current destinationItem = |"
                    + currentDestinationItemUid + "|");
            throw new EloraException(
                    "destinationItem should be a working copy.");
        }

        // Get document version working copy is based on
        DocumentRef newDestinationItemRef = session.getBaseVersion(
                currentDestinationItem.getRef());
        if (newDestinationItemRef == null) {
            log.error(logInitMsg
                    + "Base Version shouldn't be null. Current destinationItem = |"
                    + currentDestinationItemUid + "|");
            throw new EloraException("Base Version shouldn't be null.");
        }

        log.trace("current destinationItem (WC) = |" + currentDestinationItemUid
                + "| new destinationItem (AV) = |"
                + newDestinationItemRef.toString() + "|");

        cmItem.put("destinationItem", newDestinationItemRef.toString());

        cmItem.put("isManaged", true);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public static boolean addModifiedItemsDerivedFromImpactedDocs(
            CoreSession session, DocumentModel cmProcess)
            throws EloraException {
        String logInitMsg = "[addModifiedItemsDerivedFromImpactedDocs] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean derivedModifiedItemsAdded = false;

        try {

            // Retrieve distinct modified and impacted items origins
            ArrayList<String> distinctModifiedItemOriginIds = getDistinctModifiedItemOriginIds(
                    cmProcess);
            ArrayList<String> distinctImpactedItemOriginIds = getDistinctImpactedItemOriginIds(
                    cmProcess);

            // Retrieve current modified items
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.BOM_MODIFIED_ITEM_LIST) != null) {
                currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.BOM_MODIFIED_ITEM_LIST);
            }

            boolean modifiedItemsChanged = false;
            long nextModifiedItemRowNumber = getModifiedItemsMaxRowNumberInCMProcess(
                    session, cmProcess, CMConstants.ITEM_TYPE_BOM) + 1;

            // Retrieve current impacted documents
            ArrayList<HashMap<String, Object>> currentImpactedDocuments = new ArrayList<HashMap<String, Object>>();
            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                currentImpactedDocuments = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
            }

            // Iterate over each impacted document and calculate its related
            // items (BomHasCadDocument)
            for (int i = 0; i < currentImpactedDocuments.size(); ++i) {
                HashMap<String, Object> impactedDocument = currentImpactedDocuments.get(
                        i);
                DocumentModel impactedDocumentOrigin = null;
                String impactedDocumentOriginUid = (String) impactedDocument.get(
                        "originItem");
                if (impactedDocumentOriginUid != null
                        && !impactedDocumentOriginUid.isEmpty()) {
                    impactedDocumentOrigin = session.getDocument(
                            new IdRef(impactedDocumentOriginUid));
                }

                DocumentModel impactedDocumentDestination = null;
                String impactedDocumentDestinationUid = (String) impactedDocument.get(
                        "destinationItem");
                if (impactedDocumentDestinationUid != null
                        && !impactedDocumentDestinationUid.isEmpty()) {
                    impactedDocumentDestination = session.getDocument(
                            new IdRef(impactedDocumentDestinationUid));
                }

                String impactedDocumentAction = (String) impactedDocument.get(
                        "action");
                String impactedDocumentComment = (String) impactedDocument.get(
                        "comment");

                List<ModifiedItem> derivedModifiedItemsList = calculateDerivedBomModifiedItemsListForDoc(
                        session, impactedDocumentOrigin,
                        impactedDocumentDestination, impactedDocumentAction,
                        impactedDocumentComment, true);

                if (derivedModifiedItemsList != null
                        && derivedModifiedItemsList.size() > 0) {
                    for (ModifiedItem derivedModifiedItem : derivedModifiedItemsList) {
                        String derivedModifiedItemOriginUid = derivedModifiedItem.getOriginItem();

                        // If retrieved derived modified item is not already
                        // included as modified or impacted item, add it as
                        // modified item.
                        if (!distinctModifiedItemOriginIds.contains(
                                derivedModifiedItemOriginUid)
                                && !distinctImpactedItemOriginIds.contains(
                                        derivedModifiedItemOriginUid)) {
                            derivedModifiedItem.setRowNumber(
                                    nextModifiedItemRowNumber);
                            HashMap<String, Object> derivedModifiedItemType = createModifiedItemType(
                                    derivedModifiedItem);
                            currentModifiedItems.add(derivedModifiedItemType);
                            distinctModifiedItemOriginIds.add(
                                    getModifiedItemUniqueId(
                                            derivedModifiedItemType));
                            modifiedItemsChanged = true;
                            nextModifiedItemRowNumber++;
                        }
                    }
                }
            }

            if (modifiedItemsChanged) {
                cmProcess.setPropertyValue(
                        CMMetadataConstants.BOM_MODIFIED_ITEM_LIST,
                        currentModifiedItems);
                session.saveDocument(cmProcess);
                session.save();

                derivedModifiedItemsAdded = true;
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "General exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- with derivedModifiedItemsAdded = |"
                + derivedModifiedItemsAdded + "|");

        return derivedModifiedItemsAdded;
    }

    private static ArrayList<String> getDistinctModifiedItemOriginIds(
            DocumentModel cmProcess) throws EloraException {

        ArrayList<String> distinctModifiedItemOriginIds = new ArrayList<String>();

        // Current Modified Items
        ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();

        if (cmProcess.getPropertyValue(
                CMMetadataConstants.BOM_MODIFIED_ITEM_LIST) != null) {
            currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.BOM_MODIFIED_ITEM_LIST);
        }

        for (int i = 0; i < currentModifiedItems.size(); ++i) {
            HashMap<String, Object> modifiedItem = currentModifiedItems.get(i);
            String modifiedItemOriginUid = (String) modifiedItem.get(
                    "originItem");
            if (!distinctModifiedItemOriginIds.contains(
                    modifiedItemOriginUid)) {
                distinctModifiedItemOriginIds.add(modifiedItemOriginUid);
            }
        }

        return distinctModifiedItemOriginIds;
    }

    private static ArrayList<String> getDistinctImpactedItemOriginIds(
            DocumentModel cmProcess) throws EloraException {

        ArrayList<String> distinctImpactedItemOriginIds = new ArrayList<String>();

        // Current Impacted Items
        ArrayList<HashMap<String, Object>> currentImpactedItems = new ArrayList<HashMap<String, Object>>();

        if (cmProcess.getPropertyValue(
                CMMetadataConstants.BOM_IMPACTED_ITEM_LIST) != null) {
            currentImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.BOM_IMPACTED_ITEM_LIST);
        }

        for (int i = 0; i < currentImpactedItems.size(); ++i) {
            HashMap<String, Object> impactedItem = currentImpactedItems.get(i);
            String impactedItemOriginUid = (String) impactedItem.get(
                    "originItem");
            if (!distinctImpactedItemOriginIds.contains(
                    impactedItemOriginUid)) {
                distinctImpactedItemOriginIds.add(impactedItemOriginUid);
            }
        }

        return distinctImpactedItemOriginIds;
    }

}
