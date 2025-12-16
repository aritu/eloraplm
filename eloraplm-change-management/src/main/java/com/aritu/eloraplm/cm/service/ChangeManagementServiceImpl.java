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
package com.aritu.eloraplm.cm.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.ManualItem;
import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.treetable.CMTreeBeanHelper;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMImpactNode;
import com.aritu.eloraplm.cm.util.CMQueryFactory;
import com.aritu.eloraplm.cm.util.IMOptions;
import com.aritu.eloraplm.cm.util.ImpactedItemContext;
import com.aritu.eloraplm.cm.util.ImpactedItemWithChildren;
import com.aritu.eloraplm.cm.util.RelatedItemData;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.EloraExceptionWithStoredMessage;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Helper class for Elora Change Management.
 *
 * @author aritu
 *
 */
public class ChangeManagementServiceImpl implements ChangeManagementService {

    protected static final Log log = LogFactory.getLog(
            ChangeManagementServiceImpl.class);

    @SuppressWarnings("unchecked")
    @Override
    public void calculateImpactMatrix(CoreSession session,
            DocumentModel cmProcess, IMOptions imOptions,
            FacesMessages facesMessages, Map<String, String> messages)
            throws EloraException, EloraExceptionWithStoredMessage {

        String logInitMsg = "[calculateImpactMatrix] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
            ArrayList<HashMap<String, Object>> currentImpactMatrix = new ArrayList<HashMap<String, Object>>();

            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
                currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.MODIFIED_ITEM_LIST);
            }

            ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();
            if (checkIMHaveToBeCalculated(currentModifiedItems)) {
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.IMPACTED_ITEM_LIST) != null) {
                    currentImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.IMPACTED_ITEM_LIST);
                }

                log.trace("currentModifiedItems.size() = |"
                        + currentModifiedItems.size()
                        + "|, currentImpactMatrix.size() = |"
                        + currentImpactMatrix.size() + "|");

                if (currentImpactMatrix.size() > 0) {
                    if (impactMatrixHasCheckedOutDocs(currentImpactMatrix,
                            session)) {
                        throw new EloraExceptionWithStoredMessage(
                                "Impact matrix with checked out docs.",
                                "eloraplm.message.error.treetable.calculateImpactMatrix.checkedOutImpactedDocs");
                    }

                    if (imOptions.getCalculationMethod().equals(
                            CMConstants.IM_CALCULATION_METHOD_AUTOMATIC)) {
                        newImpactMatrix = recalculateAndMergeImpactMatrix(
                                session, cmProcess, currentModifiedItems,
                                currentImpactMatrix, imOptions);
                    } else {
                        newImpactMatrix = calculateManualImpactMatrix(session,
                                cmProcess, currentModifiedItems,
                                currentImpactMatrix, imOptions, true);
                    }
                } else {
                    if (imOptions.getCalculationMethod().equals(
                            CMConstants.IM_CALCULATION_METHOD_AUTOMATIC)) {
                        newImpactMatrix = calculateAutomaticImpactMatrix(
                                session, cmProcess, currentModifiedItems,
                                imOptions);
                    } else {
                        newImpactMatrix = calculateManualImpactMatrix(session,
                                cmProcess, currentModifiedItems,
                                currentImpactMatrix, imOptions, false);
                    }
                }
            }

            // -------------------------------------------------------------------
            // Store new calculated impact matrix
            // -------------------------------------------------------------------
            cmProcess.setPropertyValue(CMMetadataConstants.IMPACTED_ITEM_LIST,
                    newImpactMatrix);

            setIMOptions(cmProcess, imOptions);

            session.saveDocument(cmProcess);
            session.save();
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    // -------------------------------------------------------------------------
    // Methods related with the creation of CM Items
    // -------------------------------------------------------------------------

    protected void updateModifiedItemType(
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

        // --- destinationItemSourceVersion
        modifiedItemType.put("destinationItemSourceVersion",
                modifiedItem.getDestinationItemSourceVersion());

        // --- isManaged
        modifiedItemType.put("isManaged", modifiedItem.isManaged());

        // --- comment
        modifiedItemType.put("comment", modifiedItem.getComment());

        // --- includeInImpactMatrix
        modifiedItemType.put("includeInImpactMatrix",
                modifiedItem.getIncludeInImpactMatrix());
    }

    protected void updateManualItemType(HashMap<String, Object> manualItemType,
            ManualItem manualItem) {
        // --- action
        manualItemType.put("action", manualItem.getAction());

        // --- comment
        manualItemType.put("comment", manualItem.getComment());
    }

    public HashMap<String, Object> createImpactedItemType(
            ImpactedItem impactedItem) throws EloraException {

        // Create Impacted Item with common fields
        HashMap<String, Object> impactedItemType = CMHelper.createCMItemType(
                impactedItem);

        // Add its specific attributes
        // --- impactedItem
        impactedItemType.put("modifiedItem", impactedItem.getModifiedItem());
        impactedItemType.put("refNodeId", impactedItem.getRefNodeId());
        impactedItemType.put("isDerived", impactedItem.isDerived());
        impactedItemType.put("isFromMbom", impactedItem.getIsFromMbom());
        impactedItemType.put("externalReference",
                impactedItem.getExternalReference());
        impactedItemType.put("externalName", impactedItem.getExternalName());

        return impactedItemType;
    }

    public ImpactedItem createImpactedItem(
            HashMap<String, Object> impactedItem) {

        Long rowNumber = (Long) impactedItem.get("rowNumber");

        String modifiedItem = (String) impactedItem.get("modifiedItem");

        String nodeId = (String) impactedItem.get("nodeId");

        String parentNodeId = (String) impactedItem.get("parentNodeId");

        String derivedFrom = (String) impactedItem.get("derivedFrom");

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

        String destinationItemSourceVersion = (String) impactedItem.get(
                "destinationItemSourceVersion");

        boolean isManaged = (boolean) impactedItem.get("isManaged");

        boolean isManual = (boolean) impactedItem.get("isManual");

        String type = (String) impactedItem.get("type");

        String comment = (String) impactedItem.get("comment");

        boolean isUpdated = (boolean) impactedItem.get("isUpdated");

        boolean isDerived = (boolean) impactedItem.get("isDerived");

        String refNodeId = (String) impactedItem.get("refNodeId");

        boolean isFromMbom = (boolean) impactedItem.get("isFromMbom");

        String externalReference = (String) impactedItem.get(
                "externalReference");

        String externalName = (String) impactedItem.get("externalName");

        ImpactedItem impactedItemTyped = new ImpactedItem(rowNumber, nodeId,
                parentNodeId, derivedFrom, modifiedItem, parentItem, originItem,
                originItemWc, predicate, quantity, isAnarchic, isDirectObject,
                action, destinationItem, destinationItemWc,
                destinationItemSourceVersion, isManaged, isManual, type,
                comment, isUpdated, isDerived, refNodeId, isFromMbom,
                externalReference, externalName);

        return impactedItemTyped;
    }

    private boolean checkIfImpactMatrixShouldBeRemoved(
            HashMap<String, Object> modifiedItemType,
            ModifiedItem changedModifiedItem) {

        boolean needToRemoveIM = false;

        boolean includeInImpactMatrix = (boolean) modifiedItemType.get(
                "includeInImpactMatrix");

        // Mirar si aquí con que sea derivado y se haya modificado es suficiente
        // o hay que mirar si se ha calculado el impacto del mismo tipor que el
        // derivado.
        // Por ejemplo, derivado = doc y que se haya calculado el impacto de
        // docs! Si hay que borrar habrá que meter el id del derivedFrom porque
        // ese será el que tiene colgando todo el impacto
        if (includeInImpactMatrix) {
            String newAction = changedModifiedItem.getAction();
            String currentAction = (String) modifiedItemType.get("action");

            boolean newAndCurrentActionsAreDifferent = newAction != null
                    && currentAction != null
                    && !newAction.equals(currentAction);
            boolean onlyCurrentActionIsPresent = newAction == null
                    && currentAction != null;
            boolean onlyNewActionIsPresent = newAction != null
                    && currentAction == null;
            // if the action has changed the IM should be removed
            if (newAndCurrentActionsAreDifferent || onlyCurrentActionIsPresent
                    || onlyNewActionIsPresent) {
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
    private String checkImpactedOriginWithModifiedDestination(
            CoreSession session, String modifiedItemAction,
            String modifiedItemDestinationWcUid, DocumentModel originItem)
            throws EloraException {

        String resultMsg = "";

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);
        String originItemWcUid = originItemWc.getId();

        // if the modified action is replace and impactedOrigin is the same
        // as the modified destination, impactedItem should not be
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
    private String checkImpactedOriginWithParent(CoreSession session,
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

    private ImpactedItem createImpactedItem(CoreSession session,
            DocumentModel cmProcess, Long rowNumber, String nodeId,
            String parentNodeId, String derivedFrom, DocumentModel modifiedItem,
            DocumentModel parentItem, String parentItemAction,
            DocumentModel originItem, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String originItemType,
            boolean isManual, boolean isUpdated, boolean isDerived,
            String refNodeId, IMOptions imOptions, String comment)
            throws EloraException {

        return createImpactedItem(session, cmProcess, rowNumber, nodeId,
                parentNodeId, derivedFrom, modifiedItem, parentItem,
                parentItemAction, originItem, predicate, quantity, isAnarchic,
                isDirectObject, originItemType, isManual, isUpdated, isDerived,
                refNodeId, imOptions, comment, false, null, null);

    }

    public ImpactedItem createImpactedItem(CoreSession session,
            DocumentModel cmProcess, Long rowNumber, String nodeId,
            String parentNodeId, String derivedFrom, DocumentModel modifiedItem,
            DocumentModel parentItem, String parentItemAction,
            DocumentModel originItem, String predicate, String quantity,
            boolean isAnarchic, boolean isDirectObject, String originItemType,
            boolean isManual, boolean isUpdated, boolean isDerived,
            String refNodeId, IMOptions imOptions, String comment,
            boolean isFromMbom, String externalReference, String externalName)
            throws EloraException {

        String modifiedItemUid = modifiedItem.getId();
        String parentItemUid = parentItem == null ? null : parentItem.getId();
        String originItemUid = originItem.getId();
        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);
        String originItemWcUid = originItemWc.getId();
        String action = null;
        String destinationItemUid = null;
        String destinationItemWcUid = null;
        String destinationItemSourceVersionUid = null;
        boolean isManaged = false;

        if (comment != null && comment.length() > 0) {
            action = CMConstants.ACTION_IGNORE;
            isManaged = true;
        } else {
            if (parentItemAction != null
                    && parentItemAction.equals(CMConstants.ACTION_IGNORE)) {
                action = CMConstants.ACTION_IGNORE;
                isManaged = true;
                comment = CMConstants.COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE;
            } else {
                action = !CMHelper.getItemType(originItem).equals(CMConstants.ITEM_TYPE_DOC)
                        && imOptions.getOverwriteImpacted() && imOptions.getOverwriteOption().equals(
                                CMConstants.OVERWRITE_OPTION_LEVEL1) ? CMConstants.ACTION_OVERWRITE
                                        : CMConstants.ACTION_CHANGE;
                // if (CMHelper.getItemType(originItem).equals(
                // CMConstants.ITEM_TYPE_DOC)
                // || (imOptions.getOverwriteImpacted()
                // && imOptions.getOverwriteOption().equals(
                // CMConstants.OVERWRITE_OPTION_TOP_ASSEMBLIES))) {
                // action = CMConstants.ACTION_CHANGE;
                // } else {
                // action = imOptions.getOverwriteImpacted()
                // ? CMConstants.ACTION_OVERWRITE
                // : CMConstants.ACTION_CHANGE;
                // }

                destinationItemUid = originItemWcUid;
                destinationItemWcUid = originItemWcUid;

                DocumentModel defaultDestinationItemSourceVersion = CMHelper
                        .calculateDefaultDestinationItemSourceVersion(
                                originItemWc, originItem, action);
                destinationItemSourceVersionUid = defaultDestinationItemSourceVersion.getId();

                comment = CMTreeBeanHelper.calculateComment(cmProcess, action);

                if (imOptions.getLockImpacted()
                        && !action.equals(CMConstants.ACTION_IGNORE)) {
                    try {
                        EloraDocumentHelper.lockDocument(originItem);
                    } catch (Exception e) {
                        // We control in the impact tree if there are docs
                        // locked by another user
                    }
                }
            }
        }

        ImpactedItem impactedItem = new ImpactedItem(rowNumber, nodeId,
                parentNodeId, derivedFrom, modifiedItemUid, parentItemUid,
                originItemUid, originItemWcUid, predicate, quantity, isAnarchic,
                isDirectObject, action, destinationItemUid,
                destinationItemWcUid, destinationItemSourceVersionUid,
                isManaged, isManual, originItemType, comment, isUpdated,
                isDerived, refNodeId, isFromMbom, externalReference,
                externalName);

        return impactedItem;
    }

    private DocumentModel getWcDocForOriginItem(CoreSession session,
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

    private void checkOriginItem(CoreSession session, DocumentModel originItem)
            throws EloraException {

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
     * @param session
     * @param cmProcess
     * @param originItemUidsToBeRemoved
     * @param modifiedItemsToBeAdded
     * @param changedModifiedItems
     * @throws EloraException
     */

    @SuppressWarnings("unchecked")
    @Override
    public void saveModifiedItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<String> originItemUidsToBeRemoved,
            List<ModifiedItem> modifiedItemsToBeAdded,
            HashMap<String, ModifiedItem> changedModifiedItems)
            throws EloraException {

        String logInitMsg = "[saveModifiedItemChangesInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean modifiedItemsChanged = false;
        boolean impactedItemsChanged = false;

        try {
            if ((originItemUidsToBeRemoved != null
                    && originItemUidsToBeRemoved.size() > 0)
                    || (modifiedItemsToBeAdded != null
                            && modifiedItemsToBeAdded.size() > 0
                            || (changedModifiedItems != null
                                    && changedModifiedItems.size() > 0))) {

                List<String> distinctModifiedUidsImpactMatrixToBeRemoved = new ArrayList<String>();

                ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
                    currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.MODIFIED_ITEM_LIST);
                }

                ArrayList<HashMap<String, Object>> newModifiedItems = new ArrayList<HashMap<String, Object>>();
                ArrayList<HashMap<String, Object>> newImpactedItems = new ArrayList<HashMap<String, Object>>();

                // TODO: newModifiedItems vuelve vacío y en los de abajo parece
                // que newImpactedItems viene bien. Puede ser que en vez de add
                // le igualo a current...
                modifiedItemsChanged = processRemovedAndModifiedItems(
                        originItemUidsToBeRemoved, changedModifiedItems,
                        distinctModifiedUidsImpactMatrixToBeRemoved,
                        modifiedItemsChanged, currentModifiedItems,
                        newModifiedItems);

                impactedItemsChanged = removeModifiedItemsFromImpactMatrix(
                        cmProcess, impactedItemsChanged,
                        distinctModifiedUidsImpactMatrixToBeRemoved,
                        newImpactedItems, session);

                modifiedItemsChanged = addNewModifiedItemsAndDerived(session,
                        cmProcess, modifiedItemsToBeAdded, modifiedItemsChanged,
                        newModifiedItems);

                saveChanges(session, cmProcess, modifiedItemsChanged, false,
                        impactedItemsChanged, newModifiedItems, null,
                        newImpactedItems);
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void saveChanges(CoreSession session, DocumentModel cmProcess,
            boolean modifiedItemsChanged, boolean manualItemsChanged,
            boolean impactedItemsChanged,
            ArrayList<HashMap<String, Object>> newModifiedItems,
            ArrayList<HashMap<String, Object>> newManualItems,
            ArrayList<HashMap<String, Object>> newImpactedItems) {
        if (modifiedItemsChanged) {
            cmProcess.setPropertyValue(CMMetadataConstants.MODIFIED_ITEM_LIST,
                    newModifiedItems);
        }

        if (impactedItemsChanged) {
            cmProcess.setPropertyValue(CMMetadataConstants.IMPACTED_ITEM_LIST,
                    newImpactedItems);
        }

        if (manualItemsChanged) {
            cmProcess.setPropertyValue(
                    CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST,
                    newManualItems);
        }

        session.saveDocument(cmProcess);
        session.save();
    }

    private boolean processRemovedAndModifiedItems(
            List<String> originItemUidsToBeRemoved,
            HashMap<String, ModifiedItem> changedModifiedItems,
            List<String> distinctModifiedUidsImpactMatrixToBeRemoved,
            boolean modifiedItemsChanged,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> newModifiedItems) {
        if ((originItemUidsToBeRemoved != null
                && originItemUidsToBeRemoved.size() > 0)
                || (changedModifiedItems != null
                        && changedModifiedItems.size() > 0)) {

            modifiedItemsChanged = true;

            for (int i = 0; i < currentModifiedItems.size(); ++i) {
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                String modifiedItemOriginItemUid = (String) modifiedItem.get(
                        "originItem");

                if (changedModifiedItems != null
                        && changedModifiedItems.containsKey(
                                modifiedItemOriginItemUid)) {
                    ModifiedItem changedModifiedItem = changedModifiedItems.get(
                            modifiedItemOriginItemUid);

                    // Check if it is a derived that is marked to remove because
                    // of its parent
                    if (originItemUidsToBeRemoved == null
                            || !originItemUidsToBeRemoved.contains(
                                    modifiedItemOriginItemUid)) {
                        // check if this change implies to remove IM
                        boolean needToRemoveIM = checkIfImpactMatrixShouldBeRemoved(
                                modifiedItem, changedModifiedItem);
                        if (needToRemoveIM) {
                            distinctModifiedUidsImpactMatrixToBeRemoved.add(
                                    modifiedItemOriginItemUid);
                        }

                        updateModifiedItemType(modifiedItem,
                                changedModifiedItem);
                        newModifiedItems.add(modifiedItem);
                    }
                } else if ((originItemUidsToBeRemoved != null
                        && originItemUidsToBeRemoved.contains(
                                modifiedItemOriginItemUid))) {
                    distinctModifiedUidsImpactMatrixToBeRemoved.add(
                            modifiedItemOriginItemUid);
                } else {
                    newModifiedItems.add(modifiedItem);
                }
            }
        } else {
            newModifiedItems.addAll(currentModifiedItems);
        }
        return modifiedItemsChanged;
    }

    private boolean processRemovedAndManualItems(
            List<String> originItemUidsToBeRemoved,
            HashMap<String, ManualItem> changedManualItems,
            List<String> distinctManualUidsImpactMatrixToBeRemoved,
            boolean manualItemsChanged,
            ArrayList<HashMap<String, Object>> currentManualItems,
            ArrayList<HashMap<String, Object>> newManualItems) {
        if ((originItemUidsToBeRemoved != null
                && originItemUidsToBeRemoved.size() > 0)
                || (changedManualItems != null
                        && changedManualItems.size() > 0)) {

            manualItemsChanged = true;

            for (int i = 0; i < currentManualItems.size(); ++i) {
                HashMap<String, Object> manualItem = currentManualItems.get(i);
                String manualItemOriginItemUid = (String) manualItem.get(
                        "originItem");

                if (changedManualItems != null
                        && changedManualItems.containsKey(
                                manualItemOriginItemUid)) {
                    ManualItem changedManualItem = changedManualItems.get(
                            manualItemOriginItemUid);

                    if (originItemUidsToBeRemoved == null
                            || !originItemUidsToBeRemoved.contains(
                                    manualItemOriginItemUid)) {
                        // check if this change implies to remove IM
                        /*
                         * boolean needToRemoveIM = checkIfImpactMatrixShouldBeRemoved(
                         * manualItem, changedManualItem);
                         * if (needToRemoveIM) {
                         * distinctManualUidsImpactMatrixToBeRemoved.add(
                         * manualItemOriginItemUid);
                         * }
                         */

                        updateManualItemType(manualItem, changedManualItem);
                        newManualItems.add(manualItem);
                    }

                } else if ((originItemUidsToBeRemoved != null
                        && originItemUidsToBeRemoved.contains(
                                manualItemOriginItemUid))) {
                    distinctManualUidsImpactMatrixToBeRemoved.add(
                            manualItemOriginItemUid);
                } else {
                    newManualItems.add(manualItem);
                }
            }
        } else {
            newManualItems.addAll(currentManualItems);
        }
        return manualItemsChanged;
    }

    private boolean addNewModifiedItemsAndDerived(CoreSession session,
            DocumentModel cmProcess, List<ModifiedItem> modifiedItemsToBeAdded,
            boolean modifiedItemsChanged,
            ArrayList<HashMap<String, Object>> newModifiedItems)
            throws EloraException {
        if (modifiedItemsToBeAdded != null
                && modifiedItemsToBeAdded.size() > 0) {

            // ModifiedItemId : originUid
            ArrayList<String> newModifiedItemIds = extractModifiedItemsIdsFromList(
                    newModifiedItems);

            long nextModifiedItemRowNumber = CMHelper.getModifiedItemsMaxRowNumberInCMProcess(
                    session, cmProcess) + 1;

            for (int i = 0; i < modifiedItemsToBeAdded.size(); ++i) {
                ModifiedItem modifiedItemToBeAdded = modifiedItemsToBeAdded.get(
                        i);
                String originItemUid = modifiedItemToBeAdded.getOriginItem();

                DocumentModel originItem = session.getDocument(
                        new IdRef(originItemUid));
                checkOriginItem(session, originItem);

                HashMap<String, Object> modifiedItemType = new HashMap<String, Object>();

                modifiedItemToBeAdded.setRowNumber(nextModifiedItemRowNumber);
                modifiedItemType = CMHelper.createModifiedItemType(
                        modifiedItemToBeAdded);
                newModifiedItems.add(modifiedItemType);
                newModifiedItemIds.add(
                        getModifiedItemUniqueId(modifiedItemToBeAdded));
                modifiedItemsChanged = true;
                nextModifiedItemRowNumber++;

                // TODO:::: hau agian pauso baten egin datieke.
                // Guztia gehitu eta ondoren kendu beharrean,
                // bakarrik fan gehitzen ez dagoena jadanik...

                List<ModifiedItem> newDerivedModifiedItems = new ArrayList<ModifiedItem>();

                DocumentModel destinationItem = null;
                String destinationItemUid = modifiedItemToBeAdded.getDestinationItem();
                if (destinationItemUid != null
                        && !destinationItemUid.isEmpty()) {
                    destinationItem = session.getDocument(
                            new IdRef(destinationItemUid));
                }

                calculateDerivedModifiedItems(session, originItem,
                        destinationItem, modifiedItemToBeAdded.getAction(),
                        modifiedItemToBeAdded.getComment(),
                        modifiedItemToBeAdded.getType(),
                        newDerivedModifiedItems, false);

                for (int j = 0; j < newDerivedModifiedItems.size(); ++j) {
                    ModifiedItem newDerivedModifiedItem = newDerivedModifiedItems.get(
                            j);
                    newDerivedModifiedItem.getOriginItem();
                    if (!newModifiedItemIds.contains(
                            newDerivedModifiedItem.getOriginItem())) {
                        newDerivedModifiedItem.setRowNumber(
                                nextModifiedItemRowNumber);
                        HashMap<String, Object> derivedModifiedItemType = CMHelper.createModifiedItemType(
                                newDerivedModifiedItem);
                        newModifiedItems.add(derivedModifiedItemType);
                        newModifiedItemIds.add(getModifiedItemUniqueId(
                                newDerivedModifiedItem));
                        modifiedItemsChanged = true;
                        nextModifiedItemRowNumber++;
                    }
                }

            }
        }
        return modifiedItemsChanged;
    }

    @SuppressWarnings("unchecked")
    private boolean removeModifiedItemsFromImpactMatrix(DocumentModel cmProcess,
            boolean impactedItemsChanged,
            List<String> distinctModifiedUidsImpactMatrixToBeRemoved,
            ArrayList<HashMap<String, Object>> newImpactedItems,
            CoreSession session) {

        if (distinctModifiedUidsImpactMatrixToBeRemoved.size() > 0) {
            // Retrieve current process Impact Matrix
            ArrayList<HashMap<String, Object>> currentDocImpactedItems = new ArrayList<HashMap<String, Object>>();
            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.IMPACTED_ITEM_LIST) != null) {
                currentDocImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.IMPACTED_ITEM_LIST);
            }
            for (int i = 0; i < currentDocImpactedItems.size(); ++i) {
                HashMap<String, Object> impactedItem = currentDocImpactedItems.get(
                        i);
                String impactedItemModifiedItem = (String) impactedItem.get(
                        "modifiedItem");
                if (distinctModifiedUidsImpactMatrixToBeRemoved.contains(
                        impactedItemModifiedItem)) {
                    impactedItemsChanged = true;

                    if (impactedItem.get("refNodeId") == null
                            && impactedItem.get("originItem") != null) {
                        DocumentModel wcDoc = session.getDocument(new IdRef(
                                (String) impactedItem.get("originItemWc")));

                        if (wcDoc != null && !wcDoc.isCheckedOut()
                                && EloraDocumentHelper.isLockedByUserOrAdmin(
                                        wcDoc, session)) {
                            wcDoc.removeLock();
                        }
                    }

                } else {
                    newImpactedItems.add(impactedItem);
                }
            }
        }
        return impactedItemsChanged;
    }

    private void calculateDerivedModifiedItems(CoreSession session,
            DocumentModel modifiedItemOriginItem,
            DocumentModel modifiedItemDestinationItem,
            String modifiedItemAction, String modifiedItemComment,
            String itemType, List<ModifiedItem> modifiedItems,
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
                    modifiedItems.addAll(derivedDocModifiedItemsList);
                }

            } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                List<ModifiedItem> derivedBomModifiedItemsList = calculateDerivedBomModifiedItemsListForDoc(
                        session, modifiedItemOriginItem,
                        modifiedItemDestinationItem, modifiedItemAction,
                        modifiedItemComment, isDerivedFromImpactMatrix);
                if (derivedBomModifiedItemsList != null
                        && derivedBomModifiedItemsList.size() > 0) {
                    modifiedItems.addAll(derivedBomModifiedItemsList);
                }

                if (EloraDocumentHelper.isCadDocument(modifiedItemOriginItem)) {
                    List<ModifiedItem> derivedDocModifiedItemsList = calculateDerivedDocModifiedItemsListForDoc(
                            session, modifiedItemOriginItem,
                            modifiedItemDestinationItem, modifiedItemAction,
                            modifiedItemComment, isDerivedFromImpactMatrix);
                    if (derivedDocModifiedItemsList != null
                            && derivedDocModifiedItemsList.size() > 0) {
                        modifiedItems.addAll(derivedDocModifiedItemsList);
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

    private List<ModifiedItem> calculateDerivedDocModifiedItemsListForBom(
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
                    String derivedModifiedItemType = CMHelper.getItemType(
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

    private List<ModifiedItem> calculateDerivedBomModifiedItemsListForDoc(
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
                    String derivedModifiedItemType = CMHelper.getItemType(
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

    private List<ModifiedItem> calculateDerivedDocModifiedItemsListForDoc(
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
                    String derivedModifiedItemType = CMHelper.getItemType(
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
                    String derivedModifiedItemType = CMHelper.getItemType(
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

    private ModifiedItem createDerivedModifiedItem(CoreSession session,
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

        DocumentModel derivedModifiedItemOriginItemWc = getWcDocForOriginItem(
                session, derivedModifiedItemOriginItem);

        DocumentModel derivedModifiedItemDestinationItem = null;
        DocumentModel derivedModifiedItemDestinationItemWc = null;
        boolean derivedModifiedItemIsManaged = false;
        String derivedModifiedItemComment = null;
        boolean derivedModifiedItemIncludeInImpactMatrix = false;

        // Calculate the action for the derived item
        String derivedModifiedItemAction = calculateDerivedModifiedItemAction(
                modifiedItemOriginItem, modifiedItemAction,
                derivedModifiedItemOriginItem);

        derivedModifiedItemComment = modifiedItemComment;

        if (derivedModifiedItemAction.equals(CMConstants.ACTION_CHANGE)) {
            derivedModifiedItemDestinationItem = derivedModifiedItemOriginItemWc;
            derivedModifiedItemDestinationItemWc = derivedModifiedItemOriginItemWc;

        } else if (derivedModifiedItemAction.equals(CMConstants.ACTION_REMOVE)
                || derivedModifiedItemAction.equals(
                        CMConstants.ACTION_IGNORE)) {
            derivedModifiedItemIsManaged = true;

        } else if (derivedModifiedItemAction.equals(
                CMConstants.ACTION_REPLACE)) {
            derivedModifiedItemDestinationItem = calculateDerivedDestinationItem(
                    session, modifiedItemDestinationItem,
                    derivedModifiedItemOriginItem, derivedModifiedItemPredicate,
                    derivedModifiedItemIsSubject);

            derivedModifiedItemIsManaged = true;

            if (derivedModifiedItemDestinationItem != null) {
                if (derivedModifiedItemDestinationItem.isVersion()) {
                    derivedModifiedItemDestinationItemWc = getWcDocForOriginItem(
                            session, derivedModifiedItemDestinationItem);
                } else {
                    derivedModifiedItemDestinationItemWc = derivedModifiedItemDestinationItem;
                }
            } else {
                // If derived item action is Replace, but derived item
                // destination is null, change the derived action to Remove.
                derivedModifiedItemAction = CMConstants.ACTION_REMOVE;
            }
        }

        DocumentModel derivedDefaultDestinationItemSourceVersion = CMHelper
                .calculateDefaultDestinationItemSourceVersion(
                        derivedModifiedItemDestinationItemWc,
                        derivedModifiedItemOriginItem, derivedModifiedItemAction);

        derivedModifiedItemIncludeInImpactMatrix = // Qué ponemos aquí por
                                                   // defecto? Seguimos con las
                                                   // reglas de hasta ahora y
                                                   // luego ignoramos si son
                                                   // derivados para crear el
                                                   // arbol?
                CMHelper.getIncludeInImpactMatrixDefaultValue(
                        derivedModifiedItemOriginItem.getType(),
                        modifiedItemAction,
                        derivedModifiedItemDestinationItem);

        String nodeId = CMHelper.generateNodeId(
                derivedModifiedItemOriginItem.getId());
        String parentNodeId = null;

        ModifiedItem derivedModifiedItem = new ModifiedItem(null, nodeId,
                parentNodeId, modifiedItemOriginItem
                        .getId(),
                isDerivedFromImpactMatrix,
                null, derivedModifiedItemOriginItem.getId(),
                derivedModifiedItemOriginItemWc.getId(),
                derivedModifiedItemPredicate, derivedModifiedItemQuantity,
                derivedModifiedItemIsAnarchic,
                derivedModifiedItemIsDirectObject, derivedModifiedItemAction,
                derivedModifiedItemDestinationItem == null ? null : derivedModifiedItemDestinationItem.getId(),
                derivedModifiedItemDestinationItemWc == null ? null : derivedModifiedItemDestinationItemWc.getId(),
                derivedDefaultDestinationItemSourceVersion == null ? null
                        : derivedDefaultDestinationItemSourceVersion.getId(),
                derivedModifiedItemIsManaged, false, derivedModifiedItemType,
                derivedModifiedItemComment, false,
                derivedModifiedItemIncludeInImpactMatrix);

        return derivedModifiedItem;
    }

    private DocumentModel calculateDerivedDestinationItem(CoreSession session,
            DocumentModel modifiedItemDestination,
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

    private List<DocumentModel> filterDerivedDestinationItems(
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

    private boolean checkDerivedDestinationItem(CoreSession session,
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

    private void structureDerivedItemsAndRemoveObsoletesAndDeletes(
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

    private String calculateDerivedModifiedItemAction(
            DocumentModel modifiedItemOriginItem, String modifiedItemAction,
            DocumentModel derivedModifiedItemOriginItem) {

        String derivedModifiedItemAction = null;

        if (modifiedItemOriginItem.hasFacet(
                EloraFacetConstants.FACET_CAD_DOCUMENT)
                || modifiedItemOriginItem.hasFacet(
                        EloraFacetConstants.FACET_BOM_DOCUMENT)) {

            // check if the derived modified item is impactable or not for the
            // modified item action
            boolean isImpactable = CMHelper.getIsImpactable(
                    derivedModifiedItemOriginItem.getType(),
                    modifiedItemAction);

            if (isImpactable) {
                derivedModifiedItemAction = modifiedItemAction;
            } else if (!EloraDocumentHelper.isBasicDocument(
                    derivedModifiedItemOriginItem)
                    && modifiedItemAction.equals(CMConstants.ACTION_CHANGE)) {
                derivedModifiedItemAction = modifiedItemAction;
            } else {
                derivedModifiedItemAction = CMConstants.ACTION_IGNORE;
            }

        } else if (modifiedItemAction.equals(CMConstants.ACTION_IGNORE)) {
            derivedModifiedItemAction = modifiedItemAction;
        } else {
            derivedModifiedItemAction = CMConstants.ACTION_CHANGE;
        }

        return derivedModifiedItemAction;
    }

    /**
     * @param session
     * @param cmProcess
     * @param itemType
     * @param impactedItems
     * @throws EloraException
     * @throws DocumentUnreadableException
     */
    @Override
    public void saveImpactedItemListInCMProcess(CoreSession session,
            DocumentModel cmProcess, String itemType,
            List<ImpactedItem> impactedItems,
            List<TreeNode> actionChangeTreeNodeList)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[saveImpactedItemListInCMProcess] ["
                + session.getPrincipal().getName() + "] for itemType = |"
                + itemType + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        checkInputParams(cmProcess, impactedItems, logInitMsg);

        log.trace(logInitMsg + " Saving |" + impactedItems.size() + "| "
                + itemType + " Impacted items.");

        try {
            // TODO: Que aquí llegue en impactedItems solo los nodos que están
            // dirty. Solo lo que hace falta cambiar.
            HashMap<String, HashMap<String, Object>> currentImpactedItemsWithNodeId = addNodeIdIndexToImpactedItemList(
                    impactedItems);
            ArrayList<HashMap<String, Object>> allImpactedItems = getAllImpactedItems(
                    cmProcess);
            HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId = addNodeIdIndexToImpactedItemHM(
                    allImpactedItems);

            // Update allImpactedItemsWithNodeId
            updateImpactedItemsWithCurrentTreeChanges(
                    currentImpactedItemsWithNodeId, allImpactedItemsWithNodeId);

            ArrayList<HashMap<String, Object>> newImpactedItemList = new ArrayList<HashMap<String, Object>>();
            if (actionChangeTreeNodeList.size() > 0
                    && itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                Map<String, ImpactedItem> newImpactedItems = updateDerivedNodes(
                        cmProcess, itemType, actionChangeTreeNodeList,
                        allImpactedItems, allImpactedItemsWithNodeId, session);

                newImpactedItemList = addImpactedItemsToNewImpactMatrix(
                        newImpactedItems);
            }

            ArrayList<HashMap<String, Object>> updatedAllImpactedItems = removeNodeIdIndexToImpactedItemHM(
                    allImpactedItemsWithNodeId);

            if (actionChangeTreeNodeList.size() > 0) {
                updatedAllImpactedItems.addAll(newImpactedItemList);
            }

            cmProcess.setPropertyValue(CMMetadataConstants.IMPACTED_ITEM_LIST,
                    updatedAllImpactedItems);

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

    private Map<String, ImpactedItem> updateDerivedNodes(
            DocumentModel cmProcess, String itemType,
            List<TreeNode> actionChangeTreeNodeList,
            ArrayList<HashMap<String, Object>> allImpactedItems,
            HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId,
            CoreSession session)
            throws EloraException, DocumentUnreadableException {

        AtomicBoolean refreshDocImpactTree = new AtomicBoolean(false);
        AtomicBoolean refreshItemImpactTree = new AtomicBoolean(false);

        Map<String, ImpactedItem> newImpactedItems = updateImpactedItemsDerivedNodesWithActionChanges(
                cmProcess, itemType, actionChangeTreeNodeList, allImpactedItems,
                allImpactedItemsWithNodeId, refreshDocImpactTree,
                refreshItemImpactTree, session);

        if (refreshDocImpactTree.get()) {
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_DOCS_IMPACT_MATRIX);
        }
        if (refreshItemImpactTree.get()) {
            Events.instance().raiseEvent(
                    CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX);
        }

        return newImpactedItems;
    }

    private ArrayList<HashMap<String, Object>> removeNodeIdIndexToImpactedItemHM(
            HashMap<String, HashMap<String, Object>> currentImpactedItemsWithNodeId) {
        ArrayList<HashMap<String, Object>> updatedAllImpactedItems = new ArrayList<HashMap<String, Object>>();
        for (Map.Entry<String, HashMap<String, Object>> entry : currentImpactedItemsWithNodeId.entrySet()) {
            updatedAllImpactedItems.add(entry.getValue());
        }
        return updatedAllImpactedItems;
    }

    private void updateImpactedItemsWithCurrentTreeChanges(
            HashMap<String, HashMap<String, Object>> currentImpactedItemsWithNodeId,
            HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId) {
        for (Map.Entry<String, HashMap<String, Object>> entry : currentImpactedItemsWithNodeId.entrySet()) {
            String nodeId = entry.getKey();
            HashMap<String, Object> impactedItem = entry.getValue();
            if (allImpactedItemsWithNodeId.get(nodeId) != null) {
                allImpactedItemsWithNodeId.get(nodeId).putAll(impactedItem);
            }
        }
    }

    private HashMap<String, HashMap<String, Object>> addNodeIdIndexToImpactedItemHM(
            ArrayList<HashMap<String, Object>> allImpactedItems) {
        HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId = new HashMap<String, HashMap<String, Object>>();
        for (HashMap<String, Object> impactedItem : allImpactedItems) {
            String nodeId = (String) impactedItem.get("nodeId");
            allImpactedItemsWithNodeId.put(nodeId, impactedItem);
        }
        return allImpactedItemsWithNodeId;
    }

    private HashMap<String, HashMap<String, Object>> addNodeIdIndexToImpactedItemList(
            List<ImpactedItem> impactedItems) throws EloraException {
        HashMap<String, HashMap<String, Object>> currentImpactedItemsTreeHM = new HashMap<String, HashMap<String, Object>>();
        for (int i = 0; i < impactedItems.size(); ++i) {
            ImpactedItem impactedItem = impactedItems.get(i);
            HashMap<String, Object> impactedItemType = createImpactedItemType(
                    impactedItem);
            currentImpactedItemsTreeHM.put(impactedItem.getNodeId(),
                    impactedItemType);
        }
        return currentImpactedItemsTreeHM;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<HashMap<String, Object>> getAllImpactedItems(
            DocumentModel cmProcess) {
        ArrayList<HashMap<String, Object>> allImpactedItems = new ArrayList<HashMap<String, Object>>();
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IMPACTED_ITEM_LIST) != null) {
            allImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.IMPACTED_ITEM_LIST);
        }
        return allImpactedItems;
    }

    private void checkInputParams(DocumentModel cmProcess,
            List<ImpactedItem> impactedItems, String logInitMsg)
            throws EloraException {
        if (cmProcess == null) {
            log.error(logInitMsg + " Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }

        if (impactedItems == null) {
            log.error(logInitMsg + " Specified impactedItems is null.");
            throw new EloraException("Specified impactedItems is null.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ImpactedItem> updateImpactedItemsDerivedNodesWithActionChanges(
            DocumentModel cmProcess, String itemType,
            List<TreeNode> actionChangeTreeNodeList,
            ArrayList<HashMap<String, Object>> allImpactedItems,
            HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId,
            AtomicBoolean refreshDocImpactTree,
            AtomicBoolean refreshItemImpactTree, CoreSession session)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItemWithChildren> allImpactedItemsWithChildren = convertImpactedItemsToHierarchicalTree(
                allImpactedItems);

        Map<String, ImpactedItemContext> impactedItemsContextMap = new HashMap<String, ImpactedItemContext>();
        Map<String, ImpactedItem> newImpactedDocs = new HashMap<String, ImpactedItem>();
        Map<String, ImpactedItem> newImpactedItems = new HashMap<String, ImpactedItem>();

        ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
            currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.MODIFIED_ITEM_LIST);
        }

        List<String> modifiedsVersionSeriesId = getModifiedsVersionSeriresId(
                currentModifiedItems, session);

        for (TreeNode node : actionChangeTreeNodeList) {
            ImpactedItemsNodeData impactedNodeData = (ImpactedItemsNodeData) node.getData();
            String changedNodeId = impactedNodeData.getNodeId();
            String action = impactedNodeData.getAction();

            List<String> updatedNodeIds = new ArrayList<String>();

            if (action.equals(CMConstants.ACTION_IGNORE)
                    || action.equals(CMConstants.ACTION_CHANGE)) {
                updatedNodeIds.add(changedNodeId);
                propagateToImpactedItems(cmProcess, itemType,
                        allImpactedItemsWithNodeId,
                        allImpactedItemsWithChildren, changedNodeId, action,
                        true, refreshDocImpactTree, refreshItemImpactTree,
                        updatedNodeIds, session);
            }

            //// Add new nodes for basic documents if needed

            String originItem = (String) allImpactedItemsWithNodeId.get(
                    changedNodeId).get("originItem");

            DocumentModel changedDoc = session.getDocument(
                    new IdRef(originItem));

            if (EloraDocumentHelper.isBasicDocument(changedDoc)
                    && allImpactedItemsWithChildren.get(
                            changedNodeId).getChildren().size() == 0
                    && (action.equals(CMConstants.ACTION_CHANGE)
                            || action.equals(CMConstants.ACTION_OVERWRITE))) {

                // Crear un impactContext y un impactMap

                List<String> nodePathList = new ArrayList<String>();
                TreeNode rootNode = getRootNode(node, nodePathList);
                ImpactedItemsNodeData rootNodeData = (ImpactedItemsNodeData) rootNode.getData();

                ImpactedItemContext impactContext = new ImpactedItemContext(
                        impactedNodeData.getModifiedItem(),
                        impactedNodeData.getParentItem(),
                        impactedNodeData.getOriginItem(),
                        impactedNodeData.getRowNumber(),
                        String.join("|", nodePathList),
                        impactedNodeData.getNodeId(), rootNodeData.getNodeId(),
                        rootNodeData.getAction(),
                        rootNodeData.getDestinationItemWc().getId(),
                        impactedNodeData.getAction(),
                        impactedNodeData.getRefNodeId(), nodePathList, false);

                Map<String, CMImpactNode> impactMap = buildImpactMapFromAllImpactedItems(
                        allImpactedItems);

                IMOptions imOptions = CMHelper.loadIMOptions(cmProcess);

                // TODO: Por ahora se pasa impactedItemsContextMap vacío. Solo
                // afecta cuando hay un item derivado que ya existe de antes. La
                // solución por ahora es ignorarlo y no añadirle el documento
                // básico como hijo. No propagará de item a doc basico. Si no se
                // repite el item sí propaga.
                newImpactedDocs.putAll(
                        calculateImpactedDirectAndHierarchicalsForCad(session,
                                cmProcess, impactContext,
                                impactedItemsContextMap, impactMap,
                                CMConstants.ITEM_TYPE_DOC,
                                modifiedsVersionSeriesId, imOptions));

                if (newImpactedDocs.size() > 0) {
                    refreshDocImpactTree.set(true);
                }

                newImpactedItems.putAll(calculateImpactedItemsForDoc(session,
                        cmProcess, impactContext, impactedItemsContextMap,
                        impactMap, modifiedsVersionSeriesId, imOptions));

                processImpactedDocsToGetImpactedItems(session, cmProcess,
                        imOptions, newImpactedDocs, impactedItemsContextMap,
                        impactMap, newImpactedItems, modifiedsVersionSeriesId,
                        null, false);

                if (newImpactedItems.size() > 0) {
                    refreshItemImpactTree.set(true);
                }

                newImpactedDocs.putAll(newImpactedItems);
            }
        }
        return newImpactedDocs;
    }

    private Map<String, CMImpactNode> buildImpactMapFromAllImpactedItems(
            ArrayList<HashMap<String, Object>> allImpactedItems) {

        Map<String, CMImpactNode> impactMap = new HashMap<String, CMImpactNode>();

        for (HashMap<String, Object> impactedItem : allImpactedItems) {
            String nodeId = (String) impactedItem.get("nodeId");
            Boolean isDerived = (Boolean) impactedItem.get("isDerived");
            String originItem = (String) impactedItem.get("originItem");

            CMImpactNode impactNode = new CMImpactNode(nodeId, isDerived);
            impactMap.put(originItem, impactNode);
            log.trace(
                    "[buildImpactMapFromAllImpactedItems] Add to impactMap originItem |"
                            + originItem + "|, nodeId |" + nodeId
                            + "|, isDerived |" + isDerived + "|");
        }
        return impactMap;
    }

    private TreeNode getRootNode(TreeNode node, List<String> nodePathList) {
        ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) node.getData();
        nodePathList.add(nodeData.getOriginItem().getId());
        ImpactedItemsNodeData parentNodeData = (ImpactedItemsNodeData) node.getParent().getData();
        if (parentNodeData.getLevel() == 0) {
            return node;
        }
        return getRootNode(node.getParent(), nodePathList);
    }

    private void propagateToImpactedItems(DocumentModel cmProcess,
            String itemType,
            HashMap<String, HashMap<String, Object>> allImpactedItemsWithNodeId,
            Map<String, ImpactedItemWithChildren> allImpactedItemsWithChildren,
            String changedNodeId, String action, boolean firstLevelChildren,
            AtomicBoolean refreshDocImpactTree,
            AtomicBoolean refreshItemImpactTree, List<String> updatedNodeIds,
            CoreSession session) throws EloraException {

        ImpactedItemWithChildren impactedItemWithChildren = allImpactedItemsWithChildren.get(
                changedNodeId);
        List<String> childrenNodeIdList = impactedItemWithChildren.getChildren();

        for (String childNodeId : childrenNodeIdList) {
            ImpactedItem childItem = allImpactedItemsWithChildren.get(
                    childNodeId).getImpactedItem();

            if (childItem.getIsFromMbom()
                    && childItem.getOriginItem() == null) {
                continue;
            }

            boolean childIsDerived = childItem.isDerived();

            if (childItem.getRefNodeId() != null) {
                childItem = getNodeWithoutReferenceId(
                        allImpactedItemsWithChildren, childItem);
            }

            childNodeId = childItem.getNodeId();

            DocumentModel childItemDoc = session.getDocument(
                    new IdRef(childItem.getOriginItem()));

            // TODO: Se puede comprobar que childItem no tenga más de un hijo
            // derivado. Si tiene más de uno, todos tendrían que tener la misma
            // acción? Si se pone solo un derivado a ignore, el item no tendría
            // que ponerse a ignore no? Esto sirve tanto para los items como
            // para los cads

            if (updatedNodeIds.contains(childNodeId)
                    || (EloraDocumentHelper.isBasicDocument(childItemDoc)
                            && !action.equals(CMConstants.ACTION_IGNORE))
                    || childItem.getAction().equals(
                            CMConstants.ACTION_OVERWRITE)
                    || (childItem.isManaged() && !childItem.getAction().equals(
                            CMConstants.ACTION_IGNORE))) {
                continue;
            }

            if ((childIsDerived && firstLevelChildren)
                    || (!childIsDerived && !firstLevelChildren)) {

                if (action.equals(CMConstants.ACTION_IGNORE)) {
                    childItem.setAction(action);
                    childItem.setDestinationItem(null);
                    childItem.setDestinationItemWc(null);
                    childItem.setManaged(true);

                    if (childItem.getType().equals(CMConstants.ITEM_TYPE_DOC)) {
                        childItem.setComment(
                                CMConstants.COMMENT_IGNORE_SINCE_DERIVED_FROM_ITEM_IS_IGNORE);
                    } else {
                        childItem.setComment(
                                CMConstants.COMMENT_IGNORE_SINCE_DERIVED_FROM_DOC_IS_IGNORE);
                    }
                } else if (action.equals(CMConstants.ACTION_CHANGE)) {

                    childItem.setAction(action);
                    childItem.setDestinationItem(childItem.getOriginItemWc());
                    childItem.setDestinationItemWc(childItem.getOriginItemWc());
                    childItem.setManaged(false);
                    String comment = CMTreeBeanHelper.calculateComment(
                            cmProcess, action);
                    childItem.setComment(comment);
                }

                if (childItem.getType().equals(CMConstants.ITEM_TYPE_DOC)) {
                    refreshDocImpactTree.set(true);
                } else {
                    refreshItemImpactTree.set(true);
                }

                updatedNodeIds.add(childNodeId);

                allImpactedItemsWithNodeId.put(childNodeId,
                        createImpactedItemType(childItem));

                propagateToImpactedItems(cmProcess, itemType,
                        allImpactedItemsWithNodeId,
                        allImpactedItemsWithChildren, childNodeId, action,
                        false, refreshDocImpactTree, refreshItemImpactTree,
                        updatedNodeIds, session);
            }
        }
    }

    private ImpactedItem getNodeWithoutReferenceId(
            Map<String, ImpactedItemWithChildren> allImpactedItemsWithChildren,
            ImpactedItem childItem) {
        ImpactedItem referencedChildItem = allImpactedItemsWithChildren.get(
                childItem.getRefNodeId()).getImpactedItem();
        if (referencedChildItem.getRefNodeId() != null) {
            referencedChildItem = getNodeWithoutReferenceId(
                    allImpactedItemsWithChildren, referencedChildItem);
        }
        return referencedChildItem;
    }

    private ArrayList<String> extractModifiedItemsIdsFromList(
            ArrayList<HashMap<String, Object>> modifiedItemsList) {

        ArrayList<String> modifiedItemIds = new ArrayList<String>();

        for (int i = 0; i < modifiedItemsList.size(); ++i) {
            HashMap<String, Object> modifiedItem = modifiedItemsList.get(i);
            String modifiedItemId = getModifiedItemUniqueId(modifiedItem);
            modifiedItemIds.add(modifiedItemId);
        }
        return modifiedItemIds;
    }

    private String getModifiedItemUniqueId(
            HashMap<String, Object> modifiedItem) {
        String modifiedItemOriginUid = (String) modifiedItem.get("originItem");
        String modifiedItemUniqueId = modifiedItemOriginUid;
        return modifiedItemUniqueId;
    }

    private String getModifiedItemUniqueId(ModifiedItem modifiedItem) {
        String modifiedItemOriginUid = modifiedItem.getOriginItem();
        String modifiedItemUniqueId = modifiedItemOriginUid;
        return modifiedItemUniqueId;
    }

    public boolean impactMatrixHasCheckedOutDocs(
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            CoreSession session) {

        boolean hasCheckedOutDocs = false;
        for (int i = 0; i < currentImpactMatrix.size(); ++i) {
            HashMap<String, Object> impactItem = currentImpactMatrix.get(i);
            if (impactItem.get("destinationItem") != null
                    && impactItem.get("refNodeId") == null) {
                DocumentModel destinationItem = session.getDocument(
                        new IdRef((String) impactItem.get("destinationItem")));
                if (destinationItem.isCheckedOut()) {
                    hasCheckedOutDocs = true;
                    break;
                }
            }
        }
        return hasCheckedOutDocs;
    }

    public void setIMOptions(DocumentModel cmProcess, IMOptions imOptions) {
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CALCULATION_METHOD,
                imOptions.getCalculationMethod());
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CREATE_DOC_IMPACT,
                imOptions.getCreateDocIM());
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CREATE_ITEM_IMPACT,
                imOptions.getCreateItemIM());
        cmProcess.setPropertyValue(CMMetadataConstants.IM_LOCK_IMPACTED,
                imOptions.getLockImpacted());
        cmProcess.setPropertyValue(CMMetadataConstants.IM_OVERWRITE_IMPACTED,
                imOptions.getOverwriteImpacted());
        cmProcess.setPropertyValue(CMMetadataConstants.IM_OVERWRITE_OPTION,
                imOptions.getOverwriteOption());
    }

    public boolean checkIMHaveToBeCalculated(
            ArrayList<HashMap<String, Object>> currentModifiedItems) {

        boolean calculateIM = false;
        for (int i = 0; i < currentModifiedItems.size(); ++i) {
            HashMap<String, Object> modifiedItem = currentModifiedItems.get(i);
            calculateIM = (boolean) modifiedItem.get("includeInImpactMatrix");
            if (calculateIM) {
                break;
            }
        }
        return calculateIM;
    }

    private ArrayList<HashMap<String, Object>> calculateAutomaticImpactMatrix(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            IMOptions imOptions)
            throws EloraException, EloraExceptionWithStoredMessage {

        String logInitMsg = "[calculateAutomaticImpactMatrix] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newImpactMatrix;
        try {
            Map<String, ImpactedItem> combinedImpactedItemsAndDocs = new HashMap<>();
            Map<String, ImpactedItemContext> impactedItemsContextMap = new HashMap<String, ImpactedItemContext>();

            List<String> modifiedsVersionSeriesId = getModifiedsVersionSeriresId(
                    currentModifiedItems, session);

            for (int i = 0; i < currentModifiedItems.size(); ++i) {
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                String derivedFrom = (String) modifiedItem.get("derivedFrom");

                if (derivedFrom == null) {
                    // Manually added modified
                    ArrayList<HashMap<String, Object>> derivedModifiedItems = getDerivedModifiedItems(
                            currentModifiedItems, modifiedItem);

                    boolean modifiedIncludeInIM = (boolean) modifiedItem.get(
                            "includeInImpactMatrix");

                    boolean atLeastOneModifiedItemIncludeInIM = calculateIMOptionsCreateItemOrDocImpact(
                            imOptions, modifiedItem, derivedModifiedItems,
                            modifiedIncludeInIM);

                    if (modifiedIncludeInIM
                            || atLeastOneModifiedItemIncludeInIM) {
                        Map<String, ImpactedItem> combinedImpactedItemsAndDocsByModified = calculateModifiedsImpactedItemsAndDocs(
                                session, cmProcess, imOptions,
                                impactedItemsContextMap, modifiedItem,
                                modifiedsVersionSeriesId, derivedModifiedItems);

                        combinedImpactedItemsAndDocs.putAll(
                                combinedImpactedItemsAndDocsByModified);
                    }
                }
            }
            newImpactMatrix = addImpactedItemsToNewImpactMatrix(
                    combinedImpactedItemsAndDocs);

        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraExceptionWithStoredMessage(
                    "Permission exception thrown: |\" + e.getMessage() + \"|",
                    "eloraplm.message.error.cm.documentUnreadable");
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

    private List<String> getModifiedsVersionSeriresId(
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            CoreSession session) {
        List<String> modifiedsVersionSeriesId = new ArrayList<String>();

        for (int i = 0; i < currentModifiedItems.size(); ++i) {
            HashMap<String, Object> modifiedItem = currentModifiedItems.get(i);
            String modifiedItemId = (String) modifiedItem.get("originItem");

            DocumentModel modifiedDoc = session.getDocument(
                    new IdRef(modifiedItemId));

            if (modifiedDoc != null) {
                modifiedsVersionSeriesId.add(modifiedDoc.getVersionSeriesId());
            }
        }
        return modifiedsVersionSeriesId;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<HashMap<String, Object>> calculateManualImpactMatrix(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            IMOptions imOptions, boolean recalculateImpactMatrix)
            throws EloraException, EloraExceptionWithStoredMessage {

        String logInitMsg = "[calculateManualImpactMatrix] [RecalculateIM: "
                + recalculateImpactMatrix + "] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newImpactMatrix;
        try {
            Map<String, ImpactedItem> impactedItemsAndDocs = new HashMap<>();

            ArrayList<HashMap<String, Object>> manualImpactedItems = new ArrayList<HashMap<String, Object>>();
            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST) != null) {
                manualImpactedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST);
            }

            Map<String, List<HashMap<String, Object>>> manualImpactedsWithModifiedIndex = convertManualImpactedItemsToModifiedIndexList(
                    manualImpactedItems);

            Map<String, ImpactedItemContext> modifiedContextMap = getManualModifiedImpactedContextMap(
                    session, currentModifiedItems);

            for (HashMap<String, Object> modifiedItem : currentModifiedItems) {

                Map<String, ImpactedItem> modifiedImpactedItemsAndDocs = new HashMap<>();

                boolean modifiedIncludeInIM = (boolean) modifiedItem.get(
                        "includeInImpactMatrix");
                if (modifiedIncludeInIM) {

                    String modifiedItemId = (String) modifiedItem.get(
                            "originItem");

                    List<HashMap<String, Object>> manualImpactList = manualImpactedsWithModifiedIndex.get(
                            modifiedItemId);

                    if (manualImpactList != null
                            && manualImpactList.size() > 0) {
                        for (HashMap<String, Object> manualImpact : manualImpactList) {
                            String manualImpactId = (String) manualImpact.get(
                                    "originItem");

                            String manualImpactAction = (String) manualImpact.get(
                                    "action");

                            DocumentModel manualImpactDoc = session.getDocument(
                                    new IdRef(manualImpactId));

                            LinkedList<RelatedItemData> pathToTarget = calculatePathToTargetDocument(
                                    manualImpactDoc, modifiedItemId,
                                    new LinkedList<RelatedItemData>(), session);

                            modifiedImpactedItemsAndDocs.putAll(
                                    createImpactedItems(cmProcess, modifiedItem,
                                            pathToTarget, imOptions,
                                            manualImpactAction,
                                            modifiedContextMap, session));
                        }

                        if (recalculateImpactMatrix) {
                            Map<String, ImpactedItem> currentImpactedItemsByModifiedItem = filterImpactMatrixByModifiedItem(
                                    modifiedItemId, currentImpactMatrix);

                            if (currentImpactedItemsByModifiedItem.size() > 0) {
                                mergeModifiedItemImpactedItems(
                                        currentImpactedItemsByModifiedItem,
                                        modifiedImpactedItemsAndDocs);
                            }
                        }
                        impactedItemsAndDocs.putAll(
                                modifiedImpactedItemsAndDocs);
                    }
                }
            }

            newImpactMatrix = addImpactedItemsToNewImpactMatrix(
                    impactedItemsAndDocs);

        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraExceptionWithStoredMessage(
                    "Permission exception thrown: |\" + e.getMessage() + \"|",
                    "eloraplm.message.error.cm.documentUnreadable");

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

    private Map<String, ImpactedItemContext> getManualModifiedImpactedContextMap(
            CoreSession session,
            ArrayList<HashMap<String, Object>> currentModifiedItems) {
        Map<String, ImpactedItemContext> modifiedContextMap = new HashMap<String, ImpactedItemContext>();
        for (HashMap<String, Object> modifiedItem : currentModifiedItems) {
            boolean modifiedIncludeInIM = (boolean) modifiedItem.get(
                    "includeInImpactMatrix");
            String derivedFrom = (String) modifiedItem.get("derivedFrom");

            if (derivedFrom == null && modifiedIncludeInIM) {
                String originItem = (String) modifiedItem.get("originItem");
                String modifiedItemNodeId = (String) modifiedItem.get("nodeId");
                ImpactedItemContext impactContext = createModifiedItemContext(
                        session, modifiedItem, modifiedItemNodeId);
                modifiedContextMap.put(originItem, impactContext);
            }
        }
        return modifiedContextMap;
    }

    private Map<String, ImpactedItem> createImpactedItems(
            DocumentModel cmProcess, HashMap<String, Object> modifiedItem,
            LinkedList<RelatedItemData> pathToTarget, IMOptions imOptions,
            String manualImpactAction,
            Map<String, ImpactedItemContext> modifiedContextMap,
            CoreSession session) throws EloraException {

        Map<String, ImpactedItem> impactedManualItems = new HashMap<String, ImpactedItem>();

        ImpactedItemContext impactContext = getModifiedImpactContext(
                modifiedItem, modifiedContextMap, session);

        for (int i = 0; i < pathToTarget.size(); i++) {
            RelatedItemData relatedItemData = pathToTarget.get(i);
            DocumentModel impactedDoc = relatedItemData.getDocModel();

            String impactedItemType = CMHelper.getItemType(impactedDoc);
            boolean isAnarchic = false;
            if (impactedItemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                isAnarchic = RelationsConfig.bomAnarchicRelationsList.contains(
                        relatedItemData.getRelation()) ? true : false;
            }
            boolean isDirectObject = false;

            impactContext.increaseRowNumber();
            String nodePath = impactContext.getParentNodePath() + "|"
                    + impactedDoc.getId();
            String nodeId = CMHelper.generateNodeId(nodePath);

            ImpactedItem impactedItem = null;

            String checkImpactedOriginResultMsg = null;

            checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                    session, impactContext.getModifiedItemAction(),
                    impactContext.getModifiedItemDestinationWcUid(),
                    impactedDoc);

            if (checkImpactedOriginResultMsg == null
                    || checkImpactedOriginResultMsg.length() == 0) {
                checkImpactedOriginResultMsg = checkImpactedOriginWithParent(
                        session, impactContext.getParentItem(), impactedDoc,
                        relatedItemData.getRelation());

                if (impactedItemType.equals(CMConstants.ITEM_TYPE_BOM)
                        && (checkImpactedOriginResultMsg == null
                                || checkImpactedOriginResultMsg.length() == 0)) {
                    if (impactedDoc.getType().equals(
                            EloraDoctypeConstants.BOM_SPECIFICATION)
                            && isAnarchic) {
                        checkImpactedOriginResultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_TYPE_GENERAL_SPECIFICATION;
                    }
                }
            }

            impactedItem = createImpactedItem(session, cmProcess,
                    impactContext.getRowNumber(), nodeId,
                    impactContext.getParentNodeId(), null,
                    impactContext.getModifiedItem(),
                    impactContext.getParentItem(),
                    impactContext.getParentItemAction(), impactedDoc,
                    relatedItemData.getRelation(),
                    relatedItemData.getQuantity(), isAnarchic, isDirectObject,
                    impactedItemType, false, false, false, null, imOptions,
                    checkImpactedOriginResultMsg);

            impactedManualItems.put(nodeId, impactedItem);

            if (i == pathToTarget.size() - 1) {
                // We change the action to the manual impacted item
                // TODO: Check if previously the action is calculated as ignore
                // inside createImpactedItem?
                impactedItem.setAction(manualImpactAction);
            } else {
                impactContext = new ImpactedItemContext(
                        impactContext.getModifiedItem(),
                        impactContext.getParentItem(), impactedDoc, new Long(0),
                        nodePath, nodeId, impactContext.getRootNodeId(),
                        impactContext.getModifiedItemAction(),
                        impactContext.getModifiedItemDestinationWcUid(),
                        impactContext.getParentItemAction(), null,
                        new ArrayList<>(), false);
            }
        }

        return impactedManualItems;
    }

    private ImpactedItemContext getModifiedImpactContext(
            HashMap<String, Object> modifiedItem,
            Map<String, ImpactedItemContext> modifiedContextMap,
            CoreSession session) {
        String modifiedItemNodeId = (String) modifiedItem.get("nodeId");

        String derivedFrom = (String) modifiedItem.get("derivedFrom");
        ImpactedItemContext impactContext = null;

        if (derivedFrom != null) {
            ImpactedItemContext modifiedImpactContext = modifiedContextMap.get(
                    derivedFrom);
            impactContext = createDerivedModifiedItemContextForManualImpact(
                    session, modifiedImpactContext, modifiedItem);
        } else {
            impactContext = createModifiedItemContext(session, modifiedItem,
                    modifiedItemNodeId);
        }
        return impactContext;
    }

    private LinkedList<RelatedItemData> calculatePathToTargetDocument(
            DocumentModel currentDoc, String targetDocId,
            LinkedList<RelatedItemData> path, CoreSession session)
            throws DocumentUnreadableException {

        String logInitMsg = "[calculatePathToTargetDocument] ["
                + session.getPrincipal().getName() + "] ";

        if (targetDocId.equals(currentDoc.getId())) {
            return path;
        }

        String impactType = CMHelper.getItemType(currentDoc);
        ArrayList<String> impactRelations = CMHelper.getManualImpactRelations(
                impactType);

        for (String relation : impactRelations) {
            Resource predicateResource = new ResourceImpl(relation);
            List<Statement> stmts = RelationHelper.getStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, currentDoc,
                    predicateResource);
            for (Statement stmt : stmts) {
                DocumentModel objectDoc = RelationHelper.getDocumentModel(
                        stmt.getObject(), session);

                if (objectDoc == null) {
                    log.trace(logInitMsg
                            + "Throw DocumentUnreadableException since relatedItem is null. stmt = |"
                            + stmt.toString() + "|");
                    throw new DocumentUnreadableException(
                            "Error getting document from statement |"
                                    + stmt.toString() + "|");
                }

                EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                        stmt);

                LinkedList<RelatedItemData> newPath = new LinkedList<>(path);

                RelatedItemData relatedItemData = new RelatedItemData(
                        currentDoc, stmtInfo.getQuantity(), relation);

                newPath.addFirst(relatedItemData);
                LinkedList<RelatedItemData> resultPath = calculatePathToTargetDocument(
                        objectDoc, targetDocId, newPath, session);

                if (resultPath != null) {
                    return resultPath;
                }
            }
        }
        return null;
    }

    private boolean calculateIMOptionsCreateItemOrDocImpact(IMOptions imOptions,
            HashMap<String, Object> modifiedItem,
            ArrayList<HashMap<String, Object>> derivedModifiedItems,
            boolean modifiedIncludeInIM) {

        String logInitMsg = "[calculateIMOptionsCreateItemOrDocImpact]";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean atleastOneModifiedItemHasIM = false;
        imOptions.setCreateItemIM(false);
        imOptions.setCreateDocIM(false);

        String modifiedType = (String) modifiedItem.get("type");

        if (modifiedIncludeInIM) {
            if (modifiedType.equals(CMConstants.ITEM_TYPE_BOM)) {
                imOptions.setCreateItemIM(true);
                log.trace("Set create item IM to true");
            } else {
                imOptions.setCreateDocIM(true);
                log.trace("Set create doc IM to true");
            }
        }

        for (int j = 0; j < derivedModifiedItems.size(); ++j) {
            HashMap<String, Object> derivedModifiedItem = derivedModifiedItems.get(
                    j);
            String derivedModifiedType = (String) derivedModifiedItem.get(
                    "type");
            boolean derivedModifiedIncludeInIM = (boolean) derivedModifiedItem.get(
                    "includeInImpactMatrix");

            if (derivedModifiedIncludeInIM) {
                atleastOneModifiedItemHasIM = true;
                if (derivedModifiedType.equals(CMConstants.ITEM_TYPE_BOM)) {
                    imOptions.setCreateItemIM(true);
                    log.trace("Set create item IM to true");
                } else {
                    imOptions.setCreateDocIM(true);
                    log.trace("Set create doc IM to true");
                }
            }
        }
        return atleastOneModifiedItemHasIM;
    }

    private ArrayList<HashMap<String, Object>> getDerivedModifiedItems(
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            HashMap<String, Object> modifiedItem) {

        ArrayList<HashMap<String, Object>> derivedModifiedItems = new ArrayList<>();
        for (int i = 0; i < currentModifiedItems.size(); ++i) {
            HashMap<String, Object> currentModifiedItem = currentModifiedItems.get(
                    i);
            if (currentModifiedItem.get("derivedFrom") != null
                    && currentModifiedItem.get("derivedFrom").equals(
                            modifiedItem.get("originItem"))) {
                derivedModifiedItems.add(currentModifiedItem);
            }
        }
        return derivedModifiedItems;
    }

    private Map<String, ImpactedItem> calculateModifiedsImpactedItemsAndDocs(
            CoreSession session, DocumentModel cmProcess, IMOptions imOptions,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            HashMap<String, Object> modifiedItem,
            List<String> modifiedsVersionSeriesId,
            ArrayList<HashMap<String, Object>> derivedModifiedItems)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItem> combinedImpactedItemsAndDocsByModified = new HashMap<>();
        Map<String, ImpactedItem> impactedItems = new HashMap<String, ImpactedItem>();
        Map<String, ImpactedItem> impactedDocs = new HashMap<String, ImpactedItem>();
        Map<String, String> modifiedItemsNodeIds = new HashMap<String, String>();

        ImpactedItemContext impactContext = createModifiedItemContext(session,
                modifiedItem, impactedItemsContextMap);

        Map<String, CMImpactNode> impactMap = new HashMap<String, CMImpactNode>();
        impactMap.put((String) modifiedItem.get("originItem"), null);
        log.trace(
                "[calculateModifiedsImpactedItemsAndDocs] Add to impactMap originItem |"
                        + (String) modifiedItem.get("originItem")
                        + "| and null");

        String modifiedItemType = (String) modifiedItem.get("type");

        if (modifiedItemType.equals(CMConstants.ITEM_TYPE_BOM)) {

            addModifiedNodeId(session, modifiedItem, modifiedItemsNodeIds);

            if (imOptions.getCreateItemIM()) {
                // For the modified item get all impacted items
                // recursively
                impactedItems.putAll(calculateImpactedItemsForItem(session,
                        cmProcess, impactContext, impactedItemsContextMap,
                        impactMap, null, modifiedsVersionSeriesId, imOptions));
            }

            for (HashMap<String, Object> derivedModifiedItem : derivedModifiedItems) {
                ImpactedItemContext derivedImpactContext = createDerivedModifiedItemContext(
                        session, impactedItemsContextMap, impactContext,
                        impactMap, derivedModifiedItem);

                if (imOptions.getCreateDocIM()) {
                    // For the modified item get all impacted docs
                    // recursively
                    impactedDocs.putAll(calculateImpactedDocsForDoc(session,
                            cmProcess, derivedImpactContext,
                            impactedItemsContextMap, impactMap,
                            modifiedsVersionSeriesId, imOptions));
                }
            }

        } else if (modifiedItemType.equals(CMConstants.ITEM_TYPE_DOC)) {

            if (imOptions.getCreateDocIM()) {
                impactedDocs.putAll(calculateImpactedDocsForDoc(session,
                        cmProcess, impactContext, impactedItemsContextMap,
                        impactMap, modifiedsVersionSeriesId, imOptions));
            }

            for (HashMap<String, Object> derivedModifiedItem : derivedModifiedItems) {

                ImpactedItemContext derivedImpactContext = createDerivedModifiedItemContext(
                        session, impactedItemsContextMap, impactContext,
                        impactMap, derivedModifiedItem);

                String derivedModifiedItemType = (String) derivedModifiedItem.get(
                        "type");
                if (derivedModifiedItemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                    addModifiedNodeId(session, derivedModifiedItem,
                            modifiedItemsNodeIds);
                    if (imOptions.getCreateItemIM()) {
                        impactedItems.putAll(calculateImpactedItemsForItem(
                                session, cmProcess, derivedImpactContext,
                                impactedItemsContextMap, impactMap, null,
                                modifiedsVersionSeriesId, imOptions));
                    }
                }
            }
        }

        impactedItems.putAll(calculateNewImpactedItems(session, cmProcess,
                modifiedItemType, imOptions, impactedDocs,
                impactedItemsContextMap, impactMap, modifiedsVersionSeriesId,
                derivedModifiedItems, true));

        boolean isOverwriteAction = imOptions.getOverwriteImpacted();
        boolean isOverwriteOptionLevel1 = imOptions.getOverwriteOption().equals(
                CMConstants.OVERWRITE_OPTION_LEVEL1);

        if (imOptions.getCreateItemIM()
                && !(isOverwriteAction && isOverwriteOptionLevel1)) {

            if (modifiedItemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                calculateExternalImpactedMbom(session, cmProcess, imOptions,
                        impactedItemsContextMap, modifiedItem, impactedItems,
                        modifiedItemsNodeIds);
            } else {
                for (HashMap<String, Object> derivedModifiedItem : derivedModifiedItems) {
                    String derivedModifiedItemType = (String) derivedModifiedItem.get(
                            "type");

                    boolean derivedModifiedIncludeInIM = (boolean) derivedModifiedItem.get(
                            "includeInImpactMatrix");

                    if (derivedModifiedIncludeInIM
                            && derivedModifiedItemType.equals(
                                    CMConstants.ITEM_TYPE_BOM)) {
                        calculateExternalImpactedMbom(session, cmProcess,
                                imOptions, impactedItemsContextMap,
                                derivedModifiedItem, impactedItems,
                                modifiedItemsNodeIds);
                    }
                }
            }
        }

        combinedImpactedItemsAndDocsByModified.putAll(impactedItems);
        combinedImpactedItemsAndDocsByModified.putAll(impactedDocs);

        removeRepeatedDerivedChildrenAndUpdateRefNode(
                combinedImpactedItemsAndDocsByModified, impactMap);
        return combinedImpactedItemsAndDocsByModified;
    }

    public void calculateExternalImpactedMbom(CoreSession session,
            DocumentModel cmProcess, IMOptions imOptions,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            HashMap<String, Object> modifiedItem,
            Map<String, ImpactedItem> impactedItems,
            Map<String, String> modifiedItemsNodeIds) throws EloraException {

        // TODO: To be implemented
    }

    private void addModifiedNodeId(CoreSession session,
            HashMap<String, Object> modifiedItem,
            Map<String, String> modifiedItemsNodeIds) {

        String modifiedItemOriginItemUid = (String) modifiedItem.get(
                "originItem");

        DocumentModel modifiedDoc = session.getDocument(
                new IdRef(modifiedItemOriginItemUid));

        String modifiedRef = modifiedDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : modifiedDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        modifiedItemsNodeIds.put(modifiedRef,
                (String) modifiedItem.get("nodeId"));
    }

    private ImpactedItemContext createDerivedModifiedItemContext(
            CoreSession session,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            ImpactedItemContext impactContext,
            Map<String, CMImpactNode> impactMap,
            HashMap<String, Object> derivedModifiedItem) {
        String originItemUid = (String) derivedModifiedItem.get("originItem");

        String nodeId = (String) derivedModifiedItem.get("nodeId");
        String action = (String) derivedModifiedItem.get("action");

        DocumentModel impactedItemDoc = session.getDocument(
                new IdRef(originItemUid));

        List<String> branchDocUids = impactContext.getBranchDocUids();
        boolean isDerived = true;

        String nodePath = impactContext.getParentNodePath() + "|"
                + impactedItemDoc.getId();

        String refNodeId = null;

        CMImpactNode impactNode = new CMImpactNode(nodeId, isDerived);
        impactMap.put(impactedItemDoc.getId(), impactNode);
        log.trace(
                "[createDerivedModifiedItemContext] Add to impactMap impactItemDocId |"
                        + impactedItemDoc.getId() + "|, nodeId |" + nodeId
                        + "|, isDerived |" + isDerived + "|");

        impactContext.increaseRowNumber();

        List<String> newBranchDocUids = new ArrayList<String>();
        newBranchDocUids.addAll(branchDocUids);
        newBranchDocUids.add(impactedItemDoc.getId());

        // update nodeId to retrieve it from impactedItemsContextMap afterwards.
        // TODO: Es necesario¿?
        derivedModifiedItem.put("nodeId", nodeId);

        ImpactedItemContext derivedImpactContext = new ImpactedItemContext(
                impactContext.getModifiedItem(), impactContext.getParentItem(),
                impactedItemDoc, new Long(0), nodePath, nodeId,
                impactContext.getRootNodeId(),
                impactContext.getModifiedItemAction(),
                impactContext.getModifiedItemDestinationWcUid(), action,
                refNodeId, newBranchDocUids, true);

        impactedItemsContextMap.put(nodeId, derivedImpactContext);
        log.trace("Added to impactedItemsContextMap nodeId |" + nodeId + "|");
        return derivedImpactContext;
    }

    private ImpactedItemContext createDerivedModifiedItemContextForManualImpact(
            CoreSession session, ImpactedItemContext impactContext,
            HashMap<String, Object> derivedModifiedItem) {

        String originItemUid = (String) derivedModifiedItem.get("originItem");
        String nodeId = (String) derivedModifiedItem.get("nodeId");
        String action = (String) derivedModifiedItem.get("action");

        DocumentModel impactedItemDoc = session.getDocument(
                new IdRef(originItemUid));

        String nodePath = impactContext.getParentNodePath() + "|"
                + impactedItemDoc.getId();

        impactContext.increaseRowNumber();

        ImpactedItemContext derivedImpactContext = new ImpactedItemContext(
                impactContext.getModifiedItem(), impactContext.getParentItem(),
                impactedItemDoc, new Long(0), nodePath, nodeId,
                impactContext.getRootNodeId(),
                impactContext.getModifiedItemAction(),
                impactContext.getModifiedItemDestinationWcUid(), action, null,
                new ArrayList<>(), false);

        return derivedImpactContext;
    }

    private Map<String, ImpactedItem> calculateNewImpactedItems(
            CoreSession session, DocumentModel cmProcess,
            String modifiedItemType, IMOptions imOptions,
            Map<String, ImpactedItem> impactedDocs,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            List<String> modifiedsVersionSeriesId,
            ArrayList<HashMap<String, Object>> derivedModifiedItems,
            boolean calculateDerivedImpact)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItem> newImpactedItems = new HashMap<String, ImpactedItem>();

        // if (imOptions.getCreateItemIM()) {
        processImpactedDocsToGetImpactedItems(session, cmProcess, imOptions,
                impactedDocs, impactedItemsContextMap, impactMap,
                newImpactedItems, modifiedsVersionSeriesId,
                derivedModifiedItems, calculateDerivedImpact);
        // }
        return newImpactedItems;
    }

    private void processImpactedDocsToGetImpactedItems(CoreSession session,
            DocumentModel cmProcess, IMOptions imOptions,
            Map<String, ImpactedItem> impactedDocs,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            Map<String, ImpactedItem> newImpactedItems,
            List<String> modifiedsVersionSeriesId,
            ArrayList<HashMap<String, Object>> derivedModifiedItems,
            boolean calculateDerivedImpact)
            throws EloraException, DocumentUnreadableException {

        // Here we just process DOC modified derived to get their impacted
        // items. These modified derived are not in the impact matrix so we have
        // to process them apart. If a derived is a BOM we skip it
        if (calculateDerivedImpact) {

            /*
             * SEGUIR AQUI
             * Mirar si aquí estoy metiendo el impacto de los modificados sin tener en
             * cuenta de que puede ser el mismo que esta en la pestaña
             * de modificados. Por ejemplo, si aqui le paso un derivado CAD para calcular
             * sus items "extra" que no saque el item que hemos
             * metido como modificado y que es el que ha traído ese mismo CAD como
             * modificado
             *
             */

            calculateImpactedItemsFromDerived(session, cmProcess, imOptions,
                    impactedItemsContextMap, impactMap, newImpactedItems,
                    modifiedsVersionSeriesId, derivedModifiedItems);
        }

        for (Map.Entry<String, ImpactedItem> entry : impactedDocs.entrySet()) {
            ImpactedItem impactedItem = entry.getValue();
            if (impactedItem.getRefNodeId() == null) {
                ImpactedItemContext curImpactedItemCtx = impactedItemsContextMap.get(
                        impactedItem.getNodeId());

                // Cut the tree for basic documents that we set as ignore by
                // default. We don't want their impacted items at first load
                if (curImpactedItemCtx != null
                        && !EloraDocumentHelper.isBasicDocument(
                                curImpactedItemCtx.getParentItem())) {
                    newImpactedItems.putAll(calculateImpactedItemsForDoc(
                            session, cmProcess, curImpactedItemCtx,
                            impactedItemsContextMap, impactMap,
                            modifiedsVersionSeriesId, imOptions));
                }
            }
        }
    }

    private void calculateImpactedItemsFromDerived(CoreSession session,
            DocumentModel cmProcess, IMOptions imOptions,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            Map<String, ImpactedItem> newImpactedItems,
            List<String> modifiedsVersionSeriesId,
            ArrayList<HashMap<String, Object>> derivedModifiedItems)
            throws EloraException, DocumentUnreadableException {
        for (HashMap<String, Object> derivedModifiedItem : derivedModifiedItems) {

            boolean includeInIM = (boolean) derivedModifiedItem.get(
                    "includeInImpactMatrix");
            if (!includeInIM) {
                continue;
            }

            String derivedType = (String) derivedModifiedItem.get("type");
            String action = (String) derivedModifiedItem.get("action");

            if (derivedType.equals(CMConstants.ITEM_TYPE_BOM)
                    || action.equals(CMConstants.ACTION_IGNORE)) {
                continue;
            }

            String derivedNodeId = (String) derivedModifiedItem.get("nodeId");

            ImpactedItemContext curImpactedItemCtx = impactedItemsContextMap.get(
                    derivedNodeId);

            newImpactedItems.putAll(calculateImpactedItemsForDoc(session,
                    cmProcess, curImpactedItemCtx, impactedItemsContextMap,
                    impactMap, modifiedsVersionSeriesId, imOptions));
        }
    }

    private ArrayList<HashMap<String, Object>> addImpactedItemsToNewImpactMatrix(
            Map<String, ImpactedItem> impactedItems) throws EloraException {

        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        for (Map.Entry<String, ImpactedItem> set : impactedItems.entrySet()) {
            ImpactedItem impactedItem = set.getValue();
            HashMap<String, Object> impactedItemType = createImpactedItemType(
                    impactedItem);
            newImpactMatrix.add(impactedItemType);
        }

        return newImpactMatrix;
    }

    private ImpactedItemContext createModifiedItemContext(CoreSession session,
            HashMap<String, Object> modifiedItem,
            Map<String, ImpactedItemContext> impactedItemsContextMap) {
        String modifiedItemNodeId = (String) modifiedItem.get("nodeId");

        ImpactedItemContext impactContext = createModifiedItemContext(session,
                modifiedItem, modifiedItemNodeId);

        impactedItemsContextMap.put(modifiedItemNodeId, impactContext);
        log.trace("Added to impactedItemsContextMap nodeId |"
                + modifiedItemNodeId + "|");

        return impactContext;
    }

    private ImpactedItemContext createModifiedItemContext(CoreSession session,
            HashMap<String, Object> modifiedItem, String modifiedItemNodeId) {

        String modifiedItemAction = (String) modifiedItem.get("action");

        String modifiedItemOriginItemUid = (String) modifiedItem.get(
                "originItem");

        String modifiedItemDestinationItemWcUid = (String) modifiedItem.get(
                "destinationItemWc");

        DocumentModel modifiedItemOriginItem = session.getDocument(
                new IdRef(modifiedItemOriginItemUid));

        String modifiedItemNodePath = modifiedItemOriginItem.getId();

        ImpactedItemContext impactContext = new ImpactedItemContext(
                modifiedItemOriginItem, null, modifiedItemOriginItem,
                new Long(0), modifiedItemNodePath, modifiedItemNodeId,
                modifiedItemNodeId, modifiedItemAction,
                modifiedItemDestinationItemWcUid, modifiedItemAction, null,
                new ArrayList<String>(
                        Arrays.asList(modifiedItemOriginItem.getId())),
                true);

        return impactContext;
    }

    private void removeRepeatedDerivedChildrenAndUpdateRefNode(
            Map<String, ImpactedItem> impactedItems,
            Map<String, CMImpactNode> impactMap) {
        List<String> removeNodeIds = new ArrayList<String>();

        Map<String, List<String>> impactedItemsParentNodeWithChildren = convertImpactedItemsToParentChildren(
                impactedItems);

        for (Map.Entry<String, CMImpactNode> set : impactMap.entrySet()) {
            CMImpactNode impactNode = set.getValue();
            if (impactNode != null && impactNode.getIsDerived()
                    && impactNode.getRefNodeId() != null) {
                impactedItems.get(impactNode.getNodeId()).setRefNodeId(
                        impactNode.getRefNodeId());
                List<String> childrenNodeIds = impactedItemsParentNodeWithChildren.get(
                        impactNode.getNodeId());
                if (childrenNodeIds != null) {
                    removeNodeIds.addAll(childrenNodeIds);
                    markChildrenNodeIdsToRemove(childrenNodeIds,
                            impactedItemsParentNodeWithChildren, removeNodeIds);
                }
            }
        }
        impactedItems.keySet().removeAll(removeNodeIds);
    }

    private Map<String, List<String>> convertImpactedItemsToParentChildren(
            Map<String, ImpactedItem> impactedItems) {
        Map<String, List<String>> nodeWithChildren = new HashMap<String, List<String>>();
        for (Map.Entry<String, ImpactedItem> set : impactedItems.entrySet()) {
            ImpactedItem impactedItem = set.getValue();

            String parentNodeId = impactedItem.getParentNodeId();

            if (nodeWithChildren.containsKey(parentNodeId)) {
                nodeWithChildren.get(parentNodeId).add(
                        impactedItem.getNodeId());
            } else {
                List<String> nodeIds = new ArrayList<String>();
                nodeIds.add(impactedItem.getNodeId());
                nodeWithChildren.put(parentNodeId, nodeIds);
            }
        }
        return nodeWithChildren;
    }

    private Map<String, List<String>> convertImpactedItemsToParentChildren(
            ArrayList<HashMap<String, Object>> impactedItems) {

        Map<String, List<String>> nodeWithChildren = new HashMap<String, List<String>>();

        for (Map<String, Object> impactedItemMap : impactedItems) {
            String parentNodeId = (String) impactedItemMap.get("parentNodeId");
            String nodeId = (String) impactedItemMap.get("nodeId");

            if (nodeWithChildren.containsKey(parentNodeId)) {
                nodeWithChildren.get(parentNodeId).add(nodeId);
            } else {
                List<String> nodeIds = new ArrayList<String>();
                nodeIds.add(nodeId);
                nodeWithChildren.put(parentNodeId, nodeIds);
            }
        }
        return nodeWithChildren;
    }

    private Map<String, ImpactedItemWithChildren> convertImpactedItemsToHierarchicalTree(
            ArrayList<HashMap<String, Object>> allImpactedItems) {

        Map<String, List<String>> allImpactedItemsWithChildren = convertImpactedItemsToParentChildren(
                allImpactedItems);
        HashMap<String, ImpactedItemWithChildren> impactedItemListWithChildren = new HashMap<String, ImpactedItemWithChildren>();

        for (HashMap<String, Object> impactedItemHM : allImpactedItems) {
            String nodeId = (String) impactedItemHM.get("nodeId");
            ImpactedItem impactedItem = createImpactedItem(impactedItemHM);
            ImpactedItemWithChildren impactedItemWithChildren = new ImpactedItemWithChildren(
                    impactedItem, allImpactedItemsWithChildren.get(nodeId));
            impactedItemListWithChildren.put(nodeId, impactedItemWithChildren);
        }
        return impactedItemListWithChildren;
    }

    private void markChildrenNodeIdsToRemove(List<String> childrenNodeIds,
            Map<String, List<String>> impactedItemsParentNodeWithChildren,
            List<String> removeNodeIds) {
        for (String childNodeId : childrenNodeIds) {
            List<String> nodeList = impactedItemsParentNodeWithChildren.get(
                    childNodeId);
            if (nodeList != null) {
                removeNodeIds.addAll(nodeList);
                markChildrenNodeIdsToRemove(nodeList,
                        impactedItemsParentNodeWithChildren, removeNodeIds);
            }
        }
    }

    private ArrayList<HashMap<String, Object>> recalculateAndMergeImpactMatrix(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            IMOptions imOptions) throws EloraException {
        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        String logInitMsg = "[recalculateAndMergeImpactMatrix] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        newImpactMatrix = recalculateImpactedItems(session, cmProcess,
                currentModifiedItems, currentImpactMatrix, imOptions);

        log.trace(logInitMsg + "--- EXIT --- ");

        return newImpactMatrix;
    }

    private ArrayList<HashMap<String, Object>> recalculateImpactedItems(
            CoreSession session, DocumentModel cmProcess,
            ArrayList<HashMap<String, Object>> currentModifiedItems,
            ArrayList<HashMap<String, Object>> currentImpactMatrix,
            IMOptions imOptions) throws EloraException {

        String logInitMsg = "[recalculateImpactedItems] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newImpactMatrix;
        try {
            Map<String, ImpactedItem> newImpactedItemsMap = new HashMap<String, ImpactedItem>();
            Map<String, ImpactedItemContext> impactedItemsContextMap = new HashMap<String, ImpactedItemContext>();

            List<String> modifiedsVersionSeriesId = getModifiedsVersionSeriresId(
                    currentModifiedItems, session);

            for (int i = 0; i < currentModifiedItems.size(); ++i) {
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
                String derivedFrom = (String) modifiedItem.get("derivedFrom");
                if (derivedFrom == null) {
                    // Manually added modified
                    ArrayList<HashMap<String, Object>> derivedModifiedItems = getDerivedModifiedItems(
                            currentModifiedItems, modifiedItem);
                    boolean modifiedIncludeInIM = (boolean) modifiedItem.get(
                            "includeInImpactMatrix");

                    boolean atLeastOneModifiedItemIncludeInIM = calculateIMOptionsCreateItemOrDocImpact(
                            imOptions, modifiedItem, derivedModifiedItems,
                            modifiedIncludeInIM);

                    if (modifiedIncludeInIM
                            || atLeastOneModifiedItemIncludeInIM) {
                        String modifiedItemOriginItemUid = (String) modifiedItem.get(
                                "originItem");

                        Map<String, ImpactedItem> newImpactedItemsByModifiedItem = calculateModifiedsImpactedItemsAndDocs(
                                session, cmProcess, imOptions,
                                impactedItemsContextMap, modifiedItem,
                                modifiedsVersionSeriesId, derivedModifiedItems);

                        Map<String, ImpactedItem> currentImpactedItemsByModifiedItem = filterImpactMatrixByModifiedItem(
                                modifiedItemOriginItemUid, currentImpactMatrix);

                        if (currentImpactedItemsByModifiedItem.size() > 0) {
                            mergeModifiedItemImpactedItems(
                                    currentImpactedItemsByModifiedItem,
                                    newImpactedItemsByModifiedItem);
                        }
                        newImpactedItemsMap.putAll(
                                newImpactedItemsByModifiedItem);
                    }
                }
            }
            newImpactMatrix = addImpactedItemsToNewImpactMatrix(
                    newImpactedItemsMap);
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

    private void mergeModifiedItemImpactedItems(
            Map<String, ImpactedItem> currentImpactedItemsByModifiedItem,
            Map<String, ImpactedItem> newImpactedItemsByModifiedItem)
            throws EloraException {
        List<String> removeNodeIds = new ArrayList<String>();
        for (Map.Entry<String, ImpactedItem> set : newImpactedItemsByModifiedItem.entrySet()) {
            ImpactedItem newImpactedItem = set.getValue();
            if (currentImpactedItemsByModifiedItem.containsKey(
                    newImpactedItem.getNodeId())) {
                ImpactedItem curImpactedItem = currentImpactedItemsByModifiedItem.get(
                        newImpactedItem.getNodeId());

                if (!curImpactedItem.isManaged()
                        && newImpactedItem.getAction().equals(
                                CMConstants.ACTION_IGNORE)
                        && (newImpactedItem.getComment().equals(
                                CMConstants.COMMENT_IGNORE_SINCE_NO_CHANGES_IN_DESTINATION)
                                || newImpactedItem.getComment().equals(
                                        CMConstants.COMMENT_IGNORE_SINCE_ORIGIN_SAME_AS_REPLACE_DESTINATION))) {
                    removeNodeIds.add(newImpactedItem.getNodeId());
                } else if (curImpactedItem.isManaged()) {
                    copyImpactedItemData(curImpactedItem, newImpactedItem);
                } else if (!curImpactedItem.getAction().equals(
                        newImpactedItem.getAction())
                        || !curImpactedItem.getComment().equals(
                                newImpactedItem.getComment())) {
                    newImpactedItem.setAction(curImpactedItem.getAction());
                    newImpactedItem.setComment(curImpactedItem.getComment());
                }
            }
        }

        if (removeNodeIds.size() > 0) {
            removeNodeIdsFromImpactedItems(newImpactedItemsByModifiedItem,
                    removeNodeIds);
        }
    }

    private void removeNodeIdsFromImpactedItems(
            Map<String, ImpactedItem> newImpactedItemsByModifiedItem,
            List<String> removeParentNodeIds) {

        Map<String, List<String>> impactedItemsParentNodeWithChildren = convertImpactedItemsToParentChildren(
                newImpactedItemsByModifiedItem);

        List<String> removeNodeIds = new ArrayList<String>();
        for (String parentNodeId : removeParentNodeIds) {
            List<String> childrenNodeIds = impactedItemsParentNodeWithChildren.get(
                    parentNodeId);
            removeNodeIds.add(parentNodeId);
            if (childrenNodeIds != null) {
                removeNodeIds.addAll(childrenNodeIds);
                markChildrenNodeIdsToRemove(childrenNodeIds,
                        impactedItemsParentNodeWithChildren, removeNodeIds);
            }
        }
        newImpactedItemsByModifiedItem.keySet().removeAll(removeNodeIds);
    }

    private void copyImpactedItemData(ImpactedItem fromImpactedItem,
            ImpactedItem toImpactedItem) {
        toImpactedItem.setQuantity(fromImpactedItem.getQuantity());
        toImpactedItem.setAction(fromImpactedItem.getAction());
        toImpactedItem.setDestinationItem(
                fromImpactedItem.getDestinationItem());
        toImpactedItem.setDestinationItemWc(
                fromImpactedItem.getDestinationItemWc());
        toImpactedItem.setComment(fromImpactedItem.getComment());
        toImpactedItem.setManaged(fromImpactedItem.isManaged());
    }

    private Map<String, ImpactedItem> calculateImpactedItemsForItem(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            ImpactedItem impactedItem,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpactedItemsForItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, ImpactedItem> impactedItems = new HashMap<String, ImpactedItem>();

        try {
            Map<String, ImpactedItem> impactedDirectAnarchicAndHierarchicals = new HashMap<String, ImpactedItem>();
            impactedDirectAnarchicAndHierarchicals.putAll(
                    calculateImpactedDirectAnarchicAndHierarchicalsForItem(
                            session, cmProcess, impactContext,
                            impactedItemsContextMap, impactMap,
                            modifiedsVersionSeriesId, imOptions));

            if (imOptions.getOverwriteImpacted()
                    && imOptions.getOverwriteOption().equals(CMConstants.OVERWRITE_OPTION_TOP_ASSEMBLIES)
                    && impactedItem != null
                    && impactContext.getIsTopAssembly()
                    && !impactContext.getParentItemAction().equals(
                            CMConstants.ACTION_IGNORE)) {
                impactContext.setParentItemAction(CMConstants.ACTION_OVERWRITE);
                impactedItem.setAction(CMConstants.ACTION_OVERWRITE);
            }

            impactedItems.putAll(impactedDirectAnarchicAndHierarchicals);

            impactedItems.putAll(calculateImpactedDirectsForItem(session,
                    cmProcess, impactContext, impactedItemsContextMap,
                    impactMap, CMConstants.ITEM_TYPE_BOM,
                    modifiedsVersionSeriesId, imOptions));

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

    private Map<String, ImpactedItem> calculateImpactedItemsForDoc(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpactedItemsForDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, ImpactedItem> impactedItems = new HashMap<String, ImpactedItem>();

        try {
            impactedItems.putAll(calculateImpactedDirectAndHierarchicalsForCad(
                    session, cmProcess, impactContext, impactedItemsContextMap,
                    impactMap, CMConstants.ITEM_TYPE_BOM,
                    modifiedsVersionSeriesId, imOptions));

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

    private Map<String, ImpactedItem> calculateImpactedDocsForDoc(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        String logInitMsg = "[calculateImpactedDocsForDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, ImpactedItem> impactedItems = new HashMap<String, ImpactedItem>();

        try {

            // find out its related Subjects
            impactedItems.putAll(calculateImpactedDirectAndHierarchicalsForCad(
                    session, cmProcess, impactContext, impactedItemsContextMap,
                    impactMap, CMConstants.ITEM_TYPE_DOC,
                    modifiedsVersionSeriesId, imOptions));

            if (EloraDocumentHelper.isCadDocument(
                    impactContext.getParentItem())) {
                // find out its related Direct objects (Basic documents)
                impactedItems.putAll(calculateImpactedDirectsForCad(session,
                        cmProcess, impactContext, impactedItemsContextMap,
                        impactMap, imOptions));
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

    private Map<String, ImpactedItem> filterImpactMatrixByModifiedItem(
            String modifiedItemUid, List<HashMap<String, Object>> impactMatrix)
            throws EloraException {
        Map<String, ImpactedItem> filteredImpactMatrix = new HashMap<String, ImpactedItem>();
        for (HashMap<String, Object> impactedItem : impactMatrix) {
            ImpactedItem impactedItemTyped = createImpactedItem(impactedItem);
            if (impactedItemTyped.getModifiedItem().equals(modifiedItemUid)) {
                filteredImpactMatrix.put(impactedItemTyped.getNodeId(),
                        impactedItemTyped);
            }
        }
        return filteredImpactMatrix;
    }

    private Map<String, ImpactedItem> calculateImpactedDirectsForItem(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap, String childType,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItem> impactedDirectItems = new HashMap<String, ImpactedItem>();

        ArrayList<String> itemDirectRelations = new ArrayList<String>();

        List<String> branchDocUids = impactContext.getBranchDocUids();

        if (childType.equals(CMConstants.ITEM_TYPE_BOM)) {
            itemDirectRelations.addAll(RelationsConfig.bomDirectRelationsList);
        } else {
            itemDirectRelations.addAll(RelationsConfig.itemDirectRelationsList);
        }

        for (String relation : itemDirectRelations) {

            Resource predicateResource = new ResourceImpl(relation);

            // In this case, we retrieve the OBJECT of the relation
            boolean isAnarchic = false;
            boolean isDirectObject = true;

            List<RelatedItemData> impactedObjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), false,
                    false, true, impactContext.getGrandParentItem());

            if (impactedObjects != null && !impactedObjects.isEmpty()) {
                for (RelatedItemData impactedObject : impactedObjects) {
                    DocumentModel impactedItemDoc = impactedObject.getDocModel();
                    if (branchDocUids.contains(impactedItemDoc.getId())) {
                        // if this impacted doc is repeated in the branch skip
                        // it to avoid infinite loops
                        continue;
                    }

                    boolean isDerived = false;
                    boolean repeatedDerived = false;
                    boolean skipDirects = false;
                    // If derivedFrom is not null the node will be under
                    // modified item in tree construction.
                    String derivedFrom = null;
                    if (EloraDocumentHelper.isCadDocument(impactedItemDoc)
                            || EloraDocumentHelper.isBasicDocument(
                                    impactedItemDoc)) {
                        isDerived = true;
                        derivedFrom = impactContext.getParentItem().getId();
                    }

                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = CMHelper.generateNodeId(nodePath);

                    // If it is not null we don't show it in tree construction
                    String refNodeId = null;
                    if (impactMap.containsKey(impactedItemDoc.getId())) {
                        if (isDerived) {
                            // If a derived doc is already in the tree skip it
                            repeatedDerived = true;
                            refNodeId = impactMap.get(
                                    impactedItemDoc.getId()).getNodeId();
                        } else {
                            markToRemoveIfDerived(impactedItemsContextMap,
                                    impactMap, impactedItemDoc, nodeId);
                        }
                    } else {
                        CMImpactNode impactNode = new CMImpactNode(nodeId,
                                isDerived);
                        impactMap.put(impactedItemDoc.getId(), impactNode);
                        log.trace(
                                "[calculateImpactedDirectsForItem] Add to impactMap impactItemDocId |"
                                        + impactedItemDoc.getId()
                                        + "|, nodeId |" + nodeId
                                        + "|, isDerived |" + isDerived + "|");
                    }

                    String impactedItemType = CMHelper.getItemType(
                            impactedItemDoc);
                    impactContext.increaseRowNumber();

                    String parentNodeId = impactContext.getParentNodeId();
                    DocumentModel parentItem = impactContext.getParentItem();

                    String checkImpactedOriginResultMsg = null;
                    if (impactedItemDoc.getType().equals(
                            EloraDoctypeConstants.BOM_SPECIFICATION)) {
                        skipDirects = true;
                        checkImpactedOriginResultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_TYPE_SPECIFICATION;
                    } else if (EloraDocumentHelper.isBasicDocument(
                            impactedItemDoc)) {
                        skipDirects = true;
                        checkImpactedOriginResultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_TYPE_BASIC_DOCUMENT;
                    }

                    if ((checkImpactedOriginResultMsg == null
                            || checkImpactedOriginResultMsg.length() == 0)
                            && impactedItemDoc.getType().equals(
                                    impactContext.getModifiedItem().getType())) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                                session, impactContext.getModifiedItemAction(),
                                impactContext.getModifiedItemDestinationWcUid(),
                                impactedItemDoc);
                    }

                    ImpactedItem impactedItem = createImpactedItem(session,
                            cmProcess, impactContext.getRowNumber(), nodeId,
                            parentNodeId, derivedFrom,
                            impactContext.getModifiedItem(), parentItem,
                            impactContext.getParentItemAction(),
                            impactedItemDoc, relation,
                            impactedObject.getQuantity(), isAnarchic,
                            isDirectObject, impactedItemType, false, false,
                            isDerived, refNodeId, imOptions,
                            checkImpactedOriginResultMsg);

                    impactedDirectItems.put(nodeId, impactedItem);

                    List<String> newBranchDocUids = new ArrayList<String>();
                    newBranchDocUids.addAll(branchDocUids);
                    newBranchDocUids.add(impactedItemDoc.getId());

                    ImpactedItemContext childImpactContext = new ImpactedItemContext(
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(), impactedItemDoc,
                            new Long(0), nodePath, nodeId,
                            impactContext.getRootNodeId(),
                            impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItem.getAction(), refNodeId,
                            newBranchDocUids, false);

                    impactedItemsContextMap.put(nodeId, childImpactContext);
                    log.trace("Added to impactedItemsContextMap nodeId |"
                            + nodeId + "|");

                    if (!repeatedDerived && !skipDirects) {
                        if (EloraDocumentHelper.isBomDocument(
                                impactedItemDoc)) {
                            // Cut items direct items, don't propagate
                        } else if (EloraDocumentHelper.isCadDocument(
                                impactedItemDoc)) {
                            Map<String, ImpactedItem> impactedDocs = calculateImpactedDocsForDoc(
                                    session, cmProcess, childImpactContext,
                                    impactedItemsContextMap, impactMap,
                                    modifiedsVersionSeriesId, imOptions);
                            impactedDirectItems.putAll(impactedDocs);
                        }
                    }
                }
            }
        }
        return impactedDirectItems;
    }

    private Map<String, ImpactedItem> calculateImpactedDirectsForCad(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItem> impactedDirectItems = new HashMap<String, ImpactedItem>();

        ArrayList<String> cadDirectRelations = new ArrayList<String>();
        cadDirectRelations.addAll(RelationsConfig.docDirectRelationsList);

        List<String> branchDocUids = impactContext.getBranchDocUids();

        for (String relation : cadDirectRelations) {

            Resource predicateResource = new ResourceImpl(relation);

            // In this case, we retrieve the OBJECT of the relation
            boolean isAnarchic = false;
            boolean isDirectObject = true;

            // We must filter that impactedObjects are not the same as the
            // grandparent node
            List<RelatedItemData> impactedObjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), false,
                    false, true, impactContext.getGrandParentItem());

            if (impactedObjects != null && !impactedObjects.isEmpty()) {
                for (RelatedItemData impactedObject : impactedObjects) {
                    DocumentModel impactedItemDoc = impactedObject.getDocModel();

                    if (branchDocUids.contains(impactedItemDoc.getId())) {
                        // if this impacted doc is repeated in the branch skip
                        // it to avoid infinite loops
                        continue;
                    }

                    String impactedItemType = CMHelper.getItemType(
                            impactedItemDoc);
                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = CMHelper.generateNodeId(nodePath);

                    if (impactMap.containsKey(impactedItemDoc.getId())) {
                        markToRemoveIfDerived(impactedItemsContextMap,
                                impactMap, impactedItemDoc, nodeId);
                    } else {
                        CMImpactNode impactNode = new CMImpactNode(nodeId,
                                false);
                        impactMap.put(impactedItemDoc.getId(), impactNode);
                        log.trace(
                                "[calculateImpactedDirectsForCad] Add to impactMap impactItemDocId |"
                                        + impactedItemDoc.getId()
                                        + "|, nodeId |" + nodeId
                                        + "|, isDerived false");
                    }

                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = null;
                    if (EloraDocumentHelper.isBasicDocument(impactedItemDoc)) {
                        boolean firstLevel = impactContext.getGrandParentItem() == null;
                        if (firstLevel) {
                            // Skip impacted basic documents that are already as
                            // derived modified
                            List<String> derivedDocUids = getDerivedDocUids(
                                    session, cmProcess, impactContext);

                            if (derivedDocUids.contains(
                                    impactedItemDoc.getId())) {
                                continue;
                            }
                        }
                        checkImpactedOriginResultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_TYPE_BASIC_DOCUMENT;
                    }

                    if ((checkImpactedOriginResultMsg == null
                            || checkImpactedOriginResultMsg.length() == 0)
                            && impactedItemDoc.getType().equals(
                                    impactContext.getModifiedItem().getType())) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                                session, impactContext.getModifiedItemAction(),
                                impactContext.getModifiedItemDestinationWcUid(),
                                impactedItemDoc);
                    }

                    impactedItem = createImpactedItem(session, cmProcess,
                            impactContext.getRowNumber(), nodeId,
                            impactContext.getParentNodeId(), null,
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(),
                            impactContext.getParentItemAction(),
                            impactedItemDoc, relation,
                            impactedObject.getQuantity(), isAnarchic,
                            isDirectObject, impactedItemType, false, false,
                            false, null, imOptions,
                            checkImpactedOriginResultMsg);

                    impactedDirectItems.put(nodeId, impactedItem);

                    List<String> newBranchDocUids = new ArrayList<String>();
                    newBranchDocUids.addAll(branchDocUids);
                    newBranchDocUids.add(impactedItemDoc.getId());

                    impactMap.put(impactedItemDoc.getId(), null);
                    log.trace(
                            "[calculateImpactedDirectsForCad] Add to impactMap impactItemDocId |"
                                    + impactedItemDoc.getId() + "| and null");

                    ImpactedItemContext childImpactContext = new ImpactedItemContext(
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(), impactedItemDoc,
                            new Long(0), nodePath, nodeId,
                            impactContext.getRootNodeId(),
                            impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItem.getAction(), null, newBranchDocUids, false);

                    impactedItemsContextMap.put(nodeId, childImpactContext);
                    log.trace("Added to impactedItemsContextMap nodeId |"
                            + nodeId + "|");
                }
            }
        }
        return impactedDirectItems;
    }

    private void markToRemoveIfDerived(
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap, DocumentModel impactedItemDoc,
            String nodeId) {
        if (impactMap.get(impactedItemDoc.getId()).getIsDerived()) {
            impactMap.get(impactedItemDoc.getId()).setRefNodeId(nodeId);
            impactedItemsContextMap.get(impactMap.get(
                    impactedItemDoc.getId()).getNodeId()).setRefNodeId(nodeId);
        }
    }

    private Map<String, ImpactedItem> calculateImpactedDirectAnarchicAndHierarchicalsForItem(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {

        Map<String, ImpactedItem> impactedDirectAnarchicAndHierarchicalBomsList = new HashMap<String, ImpactedItem>();

        ArrayList<String> bomDirectAnarchicAndHierarchicalRelations = new ArrayList<String>();
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomDirectRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomAnarchicRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomHierarchicalRelationsList);

        List<String> branchDocUids = impactContext.getBranchDocUids();

        for (String relation : bomDirectAnarchicAndHierarchicalRelations) {
            Resource predicateResource = new ResourceImpl(relation);

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    false, false, null);

            boolean parentIsTopAssembly = impactContext.getIsTopAssembly();

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {

                boolean isAnarchic = RelationsConfig.bomAnarchicRelationsList.contains(
                        relation) ? true : false;
                boolean isDirectObject = false;

                for (RelatedItemData impactedSubject : impactedSubjects) {
                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();

                    if (branchDocUids.contains(impactedItemDoc.getId())) {
                        // if impacted doc is repeated in the branch skip it to
                        // avoid infinite loops
                        continue;
                    }

                    boolean isTopAssembly = RelationsConfig.bomHierarchicalRelationsList.contains(relation)
                            && parentIsTopAssembly ? true : false;

                    impactContext.increaseRowNumber();
                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = CMHelper.generateNodeId(nodePath);

                    if (impactMap.containsKey(impactedItemDoc.getId())) {
                        markToRemoveIfDerived(impactedItemsContextMap,
                                impactMap, impactedItemDoc, nodeId);
                    } else {
                        CMImpactNode impactNode = new CMImpactNode(nodeId,
                                false);
                        impactMap.put(impactedItemDoc.getId(), impactNode);
                        log.trace(
                                "[calculateImpactedDirectAnarchicAndHierarchicalsForItem] Add to impactMap impactItemDocId |"
                                        + impactedItemDoc.getId()
                                        + "|, nodeId |" + nodeId
                                        + "|, isDerived false");
                    }

                    String impactedItemType = CMHelper.getItemType(
                            impactedItemDoc);
                    ImpactedItem impactedItem = null;

                    String checkImpactedOriginResultMsg = null;
                    if (impactedItemDoc.getType().equals(
                            impactContext.getModifiedItem().getType())) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                                session, impactContext.getModifiedItemAction(),
                                impactContext.getModifiedItemDestinationWcUid(),
                                impactedItemDoc);
                    }

                    if (checkImpactedOriginResultMsg == null
                            || checkImpactedOriginResultMsg.length() == 0) {
                        checkImpactedOriginResultMsg = checkImpactedOriginWithParent(
                                session, impactContext.getParentItem(),
                                impactedItemDoc, relation);

                        if (checkImpactedOriginResultMsg == null
                                || checkImpactedOriginResultMsg.length() == 0) {
                            if (impactedItemDoc.getType().equals(
                                    EloraDoctypeConstants.BOM_SPECIFICATION)
                                    && isAnarchic) {
                                checkImpactedOriginResultMsg = CMConstants.COMMENT_IGNORE_SINCE_ITEM_TYPE_GENERAL_SPECIFICATION;
                            }
                        }
                    }

                    impactedItem = createImpactedItem(session, cmProcess,
                            impactContext.getRowNumber(), nodeId,
                            impactContext.getParentNodeId(), null,
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(),
                            impactContext.getParentItemAction(),
                            impactedItemDoc, relation,
                            impactedSubject.getQuantity(), isAnarchic,
                            isDirectObject, impactedItemType, false, false,
                            false, null, imOptions,
                            checkImpactedOriginResultMsg);

                    impactedDirectAnarchicAndHierarchicalBomsList.put(nodeId,
                            impactedItem);

                    List<String> newBranchDocUids = new ArrayList<String>();
                    newBranchDocUids.addAll(branchDocUids);
                    newBranchDocUids.add(impactedItemDoc.getId());

                    if (isTopAssembly) {
                        impactContext.setIsTopAssembly(false);
                    }

                    ImpactedItemContext childImpactContext = new ImpactedItemContext(
                            impactContext.getModifiedItem(),
                            impactContext.getParentItem(), impactedItemDoc,
                            new Long(0), nodePath, nodeId,
                            impactContext.getRootNodeId(),
                            impactContext.getModifiedItemAction(),
                            impactContext.getModifiedItemDestinationWcUid(),
                            impactedItem.getAction(), null, newBranchDocUids,
                            isTopAssembly);

                    impactedItemsContextMap.put(nodeId, childImpactContext);
                    log.trace("Added to impactedItemsContextMap nodeId |"
                            + nodeId + "|");

                    boolean isOverwriteAction = impactedItem.getAction().equals(
                            CMConstants.ACTION_OVERWRITE);
                    boolean isOverwriteOptionLevel1 = imOptions.getOverwriteOption().equals(
                            CMConstants.OVERWRITE_OPTION_LEVEL1);

                    if (isOverwriteAction && isOverwriteOptionLevel1
                            && !imOptions.getCreateDocIM()) {
                        continue;
                    }

                    Map<String, ImpactedItem> allImpactedItems = calculateImpactedItemsForItem(
                            session, cmProcess, childImpactContext,
                            impactedItemsContextMap, impactMap, impactedItem,
                            modifiedsVersionSeriesId, imOptions);

                    impactedDirectAnarchicAndHierarchicalBomsList.putAll(
                            allImpactedItems);
                }
            }
        }
        return impactedDirectAnarchicAndHierarchicalBomsList;
    }

    private Map<String, ImpactedItem> calculateImpactedDirectAndHierarchicalsForCad(
            CoreSession session, DocumentModel cmProcess,
            ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            Map<String, CMImpactNode> impactMap, String childType,
            List<String> modifiedsVersionSeriesId, IMOptions imOptions)
            throws EloraException, DocumentUnreadableException {
        String logInitMsg = "[calculateImpactedDirectAndHierarchicalsForCad] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, ImpactedItem> hierarchicalCads = new HashMap<String, ImpactedItem>();
        List<String> hierarchicalCadDocIds = new ArrayList<String>();

        ArrayList<String> cadHierarchicalRelations = getCadHierarchicalRelations(
                impactContext, childType);

        List<String> branchDocUids = impactContext.getBranchDocUids();

        for (String relation : cadHierarchicalRelations) {

            Resource predicateResource = new ResourceImpl(relation);

            boolean isAnarchic = false;
            boolean isDirectObject = false;
            boolean filterByLatestVersion = false;

            if (RelationsConfig.cadDirectRelationsList.contains(relation)) {
                filterByLatestVersion = true;
            }

            List<RelatedItemData> impactedSubjects = getRelatedItems(session,
                    predicateResource, impactContext.getParentItem(), true,
                    filterByLatestVersion, false, null);

            if (impactedSubjects != null && !impactedSubjects.isEmpty()) {
                for (RelatedItemData impactedSubject : impactedSubjects) {

                    boolean repeatedDerived = false;

                    DocumentModel impactedItemDoc = impactedSubject.getDocModel();
                    Boolean impactedDocIsBom = EloraDocumentHelper.isBomDocument(
                            impactedItemDoc);
                    Boolean impactedDocIsCad = EloraDocumentHelper.isCadDocument(
                            impactedItemDoc);

                    if (impactedDocIsBom && modifiedsVersionSeriesId.contains(
                            impactedItemDoc.getVersionSeriesId())) {
                        // Calculating "extra" items from cad. Avoid modified
                        // item to paint again
                        continue;
                    }

                    if (branchDocUids.contains(impactedItemDoc.getId())) {
                        // if this impacted doc is repeated in the branch skip
                        // it to avoid infinite loops
                        continue;
                    }

                    boolean isModifiedDrawing = isModifiedDrawing(impactContext,
                            impactedItemDoc, impactedDocIsBom, impactedDocIsCad,
                            childType, cmProcess, modifiedsVersionSeriesId,
                            session);

                    boolean isDerived = false;
                    String derivedFrom = null;
                    if (impactedDocIsBom) {
                        isDerived = true;
                        derivedFrom = impactContext.getParentItem().getId();
                    }

                    String nodePath = impactContext.getParentNodePath() + "|"
                            + impactedItemDoc.getId();
                    String nodeId = CMHelper.generateNodeId(nodePath);

                    String refNodeId = null;
                    Boolean refNodeIdIsDerived = false;
                    if (impactMap.containsKey(impactedItemDoc.getId())) {
                        if (isDerived) {
                            // If a derived doc is already in the tree skip it
                            log.trace("Is derived impactedItemDoc |"
                                    + impactedItemDoc.getId() + "|");
                            repeatedDerived = true;
                            refNodeId = impactMap.get(
                                    impactedItemDoc.getId()).getNodeId();
                            refNodeIdIsDerived = impactMap.get(
                                    impactedItemDoc.getId()).getIsDerived();

                            log.trace("Retrieve refNodeId |" + refNodeId
                                    + "| and refNodeIdIsDerived |"
                                    + refNodeIdIsDerived + "|");
                        } else {
                            markToRemoveIfDerived(impactedItemsContextMap,
                                    impactMap, impactedItemDoc, nodeId);
                        }
                    } else {
                        CMImpactNode impactNode = new CMImpactNode(nodeId,
                                isDerived);
                        impactMap.put(impactedItemDoc.getId(), impactNode);
                        log.trace(logInitMsg
                                + "Add to impactMap impactItemDocId |"
                                + impactedItemDoc.getId() + "|, nodeId |"
                                + nodeId + "|, isDerived |" + isDerived + "|");
                    }

                    // TODO: Qué casos filtra este if¿?¿?¿?
                    if (!hierarchicalCadDocIds.contains(
                            impactedItemDoc.getId())) {
                        // add the hierarchical one
                        String impactedItemType = CMHelper.getItemType(
                                impactedItemDoc);
                        impactContext.increaseRowNumber();

                        Long rowNumber = isDerived
                                ? impactContext.getRowNumber() + 1000
                                : impactContext.getRowNumber();

                        ImpactedItem impactedItem = null;
                        String checkImpactedOriginResultMsg = null;

                        if (impactedItemDoc.getType().equals(
                                impactContext.getModifiedItem().getType())) {
                            checkImpactedOriginResultMsg = checkImpactedOriginWithModifiedDestination(
                                    session,
                                    impactContext.getModifiedItemAction(),
                                    impactContext.getModifiedItemDestinationWcUid(),
                                    impactedItemDoc);
                        }

                        if (checkImpactedOriginResultMsg == null
                                || checkImpactedOriginResultMsg.length() == 0) {
                            checkImpactedOriginResultMsg = checkImpactedOriginWithParent(
                                    session, impactContext.getParentItem(),
                                    impactedItemDoc, relation);
                        }

                        String parentNodeId = impactContext.getParentNodeId();
                        DocumentModel parentItem = impactContext.getParentItem();

                        impactedItem = createImpactedItem(session, cmProcess,
                                rowNumber, nodeId, parentNodeId, derivedFrom,
                                impactContext.getModifiedItem(), parentItem,
                                impactContext.getParentItemAction(),
                                impactedItemDoc, relation,
                                impactedSubject.getQuantity(), isAnarchic,
                                isDirectObject, impactedItemType, false, false,
                                isDerived, refNodeId, imOptions,
                                checkImpactedOriginResultMsg);

                        if (!isModifiedDrawing) {
                            hierarchicalCads.put(nodeId, impactedItem);
                        }

                        hierarchicalCadDocIds.add(impactedItemDoc.getId());

                        List<String> newBranchDocUids = new ArrayList<String>();
                        newBranchDocUids.addAll(branchDocUids);
                        newBranchDocUids.add(impactedItemDoc.getId());

                        ImpactedItemContext childImpactContext = new ImpactedItemContext(
                                impactContext.getModifiedItem(),
                                impactContext.getParentItem(), impactedItemDoc,
                                new Long(0), nodePath, nodeId,
                                impactContext.getRootNodeId(),
                                impactContext.getModifiedItemAction(),
                                impactContext.getModifiedItemDestinationWcUid(),
                                impactedItem.getAction(), refNodeId,
                                newBranchDocUids, false);

                        impactedItemsContextMap.put(nodeId, childImpactContext);
                        log.trace("Added to impactedItemsContextMap nodeId |"
                                + nodeId + "|");

                        if (!repeatedDerived) {
                            if (impactedDocIsCad) {
                                Map<String, ImpactedItem> impactedDocs = calculateImpactedDocsForDoc(
                                        session, cmProcess, childImpactContext,
                                        impactedItemsContextMap, impactMap,
                                        modifiedsVersionSeriesId, imOptions);
                                hierarchicalCads.putAll(impactedDocs);
                            } else if (impactedDocIsBom) {
                                boolean isOverwriteAction = impactedItem.getAction().equals(
                                        CMConstants.ACTION_OVERWRITE);
                                boolean isOverwriteOptionLevel1 = imOptions.getOverwriteOption().equals(
                                        CMConstants.OVERWRITE_OPTION_LEVEL1);

                                if (isOverwriteAction && isOverwriteOptionLevel1
                                        && !imOptions.getCreateDocIM()) {
                                    continue;
                                }

                                // In this case we put the first level of the derived item as top assembly. If
                                // it has impacted items it could be changed from its children
                                childImpactContext.setIsTopAssembly(true);

                                Map<String, ImpactedItem> impactedItems = calculateImpactedItemsForItem(
                                        session, cmProcess, childImpactContext,
                                        impactedItemsContextMap, impactMap, impactedItem,
                                        modifiedsVersionSeriesId, imOptions);

                                // if ((checkImpactedOriginResultMsg == null
                                // || checkImpactedOriginResultMsg.length() == 0)
                                // && imOptions.getOverwriteImpacted()
                                // && imOptions.getOverwriteOption().equals(
                                // CMConstants.OVERWRITE_OPTION_TOP_ASSEMBLIES)) {
                                // childImpactContext.setTopAssemblyNodeIdInBranch(nodeId);
                                // childImpactContext.setIsTopAssembly(true);
                                // }

                                hierarchicalCads.putAll(impactedItems);
                            }
                        } else if (!refNodeIdIsDerived) {

                            // TODO: Cuando llega aquí porque se ha cambiado la
                            // acción de un documento básico y ese doc tiene un
                            // item relacionado que ya está en el árbol, a ese
                            // item no le vamos a añadir el documento básico
                            // debajo como derivado para hacer la propagación
                            // más tarde. No lo hacemos porque en ese momento no
                            // tenemos el impactedItemsContextMap relleno con
                            // todos los contextos y nos hace falta para añadir
                            // un hijo al item. Por ahora no voy a calcular todo
                            // ese map solo para este caso.

                            if (!EloraDocumentHelper.isBasicDocument(
                                    impactContext.getParentItem())) {
                                // Create a new node. This node is
                                // repeatedDerived's
                                // parent node and we add it as child of
                                // refNodeId
                                // item node
                                // This is done to ease the propagation of
                                // actions
                                // on item -> cad

                                log.trace(
                                        "About to create new node when finding a repited derived referencing a node that is not derived.");
                                addNewChildToRefNodeIdItem(session, cmProcess,
                                        impactContext, impactedItemsContextMap,
                                        imOptions, hierarchicalCads, relation,
                                        isAnarchic, impactedSubject, isDerived,
                                        refNodeId,
                                        checkImpactedOriginResultMsg);
                            }
                        }
                    }
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
        return hierarchicalCads;
    }

    private void addNewChildToRefNodeIdItem(CoreSession session,
            DocumentModel cmProcess, ImpactedItemContext impactContext,
            Map<String, ImpactedItemContext> impactedItemsContextMap,
            IMOptions imOptions, Map<String, ImpactedItem> hierarchicalCads,
            String relation, boolean isAnarchic,
            RelatedItemData impactedSubject, boolean isDerived,
            String refNodeId, String checkImpactedOriginResultMsg)
            throws EloraException {
        String logInitMsg = "[addNewChildToRefNodeIdItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(
                "About to get impact context of refNodeId |" + refNodeId + "|");
        ImpactedItemContext refItemImpactContext = impactedItemsContextMap.get(
                refNodeId);

        DocumentModel newImpactedItemDoc = impactContext.getParentItem();
        log.trace("ImpactedItemDoc to add |" + impactContext.getParentItem()
                + "|");

        String newNodePath = refItemImpactContext.getParentNodePath() + "|"
                + newImpactedItemDoc.getId();
        String newNodeId = CMHelper.generateNodeId(newNodePath);

        refItemImpactContext.increaseRowNumber();

        String newDerivedFrom = refItemImpactContext.getParentItem().getId();
        String newParentNodeId = refItemImpactContext.getParentNodeId();
        DocumentModel newParentItem = refItemImpactContext.getParentItem();
        Boolean newIsDirectObject = true;
        String newImpactedItemType = CMHelper.getItemType(newImpactedItemDoc);
        String newRefNodeId = impactContext.getParentNodeId();

        ImpactedItem newImpactedItem = createImpactedItem(session, cmProcess,
                refItemImpactContext.getRowNumber(), newNodeId, newParentNodeId,
                newDerivedFrom, refItemImpactContext.getModifiedItem(),
                newParentItem, refItemImpactContext.getParentItemAction(),
                newImpactedItemDoc, relation, impactedSubject.getQuantity(),
                isAnarchic, newIsDirectObject, newImpactedItemType, false,
                false, isDerived, newRefNodeId, imOptions,
                checkImpactedOriginResultMsg);

        hierarchicalCads.put(newNodeId, newImpactedItem);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private ArrayList<String> getCadHierarchicalRelations(
            ImpactedItemContext impactContext, String childType) {
        ArrayList<String> cadHierarchicalRelations = new ArrayList<String>();

        if (childType.equals(CMConstants.ITEM_TYPE_DOC)) {
            if (!impactContext.getParentItem().getType().equals(
                    EloraDoctypeConstants.CAD_DRAWING)
                    && !impactContext.getParentItem().hasFacet(
                            EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
                cadHierarchicalRelations.addAll(
                        RelationsConfig.cadHierarchicalRelationsList);
                cadHierarchicalRelations.addAll(
                        RelationsConfig.cadDirectRelationsList);
                cadHierarchicalRelations.addAll(
                        RelationsConfig.cadSpecialRelationsList);
            } else if (impactContext.getParentItem().hasFacet(
                    EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
                cadHierarchicalRelations.addAll(
                        RelationsConfig.docDirectRelationsList);
            }
        } else if (childType.equals(CMConstants.ITEM_TYPE_BOM)) {
            if (impactContext.getParentItem().hasFacet(
                    EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
                cadHierarchicalRelations.add(
                        EloraRelationConstants.BOM_HAS_DOCUMENT);
            } else {
                cadHierarchicalRelations.add(
                        EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
            }
        }
        return cadHierarchicalRelations;
    }

    private Boolean isModifiedDrawing(ImpactedItemContext impactContext,
            DocumentModel impactedItemDoc, Boolean impactedDocIsBom,
            Boolean impactedDocIsCad, String childType, DocumentModel cmProcess,
            List<String> modifiedsVersionSeriesId, CoreSession session)
            throws EloraException {

        // Getting subject from CAD. ImpactedDoc can be BOM or CAD.

        // If impacted doc is CAD_DRAWING and its grandparent is null, its
        // parent is the
        // modified item. As we always come from a CAD to this function the
        // modified is a CAD document
        boolean firstLevelAndCadDrawingImpact = impactedItemDoc.getType().equals(
                EloraDoctypeConstants.CAD_DRAWING)
                && impactContext.getGrandParentItem() == null;

        // If impacted doc is CAD_DRAWING and its grandparent is equal to
        // modified item
        // we now we are in second level impact. We don't care the type of the
        // modified item
        boolean secondLevelAndCadDrawingImpact = !firstLevelAndCadDrawingImpact
                && impactedItemDoc.getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING)
                && impactContext.getGrandParentItem().getId().equals(
                        impactContext.getModifiedItem().getId());

        if ((firstLevelAndCadDrawingImpact || secondLevelAndCadDrawingImpact)
                && modifiedsVersionSeriesId.contains(
                        impactedItemDoc.getVersionSeriesId())) {
            return true;
        }
        return false;
    }

    private List<String> getDerivedDocUids(CoreSession session,
            DocumentModel cmProcess, ImpactedItemContext impactContext)
            throws EloraException {

        String derivedFrom = null;
        if (impactContext.getGrandParentItem() == null) {
            derivedFrom = impactContext.getParentItem().getId();
        } else {
            derivedFrom = impactContext.getGrandParentItem().getId();
        }
        String query = CMQueryFactory.getModifiedItemsByDerivedFromQuery(
                cmProcess.getId(), derivedFrom);

        List<String> derivedDocUids = EloraQueryHelper.executeQueryAndGetResultStringList(
                CMMetadataConstants.MODIFIED_ITEM_LIST + "/*1/originItem",
                query, session);
        return derivedDocUids;
    }

    /////////////////////////////////////////////////////////

    private List<RelatedItemData> getRelatedItems(CoreSession session,
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
     *                              retrieved. If true, get the LATEST RELATED
     *                              version. Otherwise,
     *                              get the LATEST RELEASED RELATED version.
     * @return
     * @throws EloraException
     */
    private List<RelatedItemData> filterRelatedItems(CoreSession session,
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

    private boolean checkRelatedItem(CoreSession session,
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

    private void structureRelatedItemsAndRemoveObsoletesAndDeletes(
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

    @SuppressWarnings("unchecked")
    @Override
    public void saveManualItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<String> originItemUidsToBeRemoved,
            List<ManualItem> manualItemsToBeAdded,
            HashMap<String, ManualItem> changedManualItems)
            throws EloraException {

        String logInitMsg = "[saveManualItemChangesInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean manualItemsChanged = false;
        boolean impactedItemsChanged = false;

        try {

            /*
             * if ((originItemUidsToBeRemoved != null
             * && originItemUidsToBeRemoved.size() > 0)
             * || (modifiedItemsToBeAdded != null
             * && modifiedItemsToBeAdded.size() > 0
             * || (changedModifiedItems != null
             * && changedModifiedItems.size() > 0))) {
             */

            if ((originItemUidsToBeRemoved != null
                    && originItemUidsToBeRemoved.size() > 0)
                    || (manualItemsToBeAdded != null
                            && manualItemsToBeAdded.size() > 0
                            || (changedManualItems != null
                                    && changedManualItems.size() > 0))) {

                List<String> distinctManualUidsImpactMatrixToBeRemoved = new ArrayList<String>();

                ArrayList<HashMap<String, Object>> currentManualItems = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST) != null) {
                    currentManualItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST);
                }

                ArrayList<HashMap<String, Object>> newManualItems = new ArrayList<HashMap<String, Object>>();
                ArrayList<HashMap<String, Object>> newImpactedItems = new ArrayList<HashMap<String, Object>>();

                manualItemsChanged = processRemovedAndManualItems(
                        originItemUidsToBeRemoved, changedManualItems,
                        distinctManualUidsImpactMatrixToBeRemoved,
                        manualItemsChanged, currentManualItems, newManualItems);
                /*
                 * impactedItemsChanged = removeModifiedItemsFromImpactMatrix(
                 * cmProcess, impactedItemsChanged,
                 * distinctModifiedUidsImpactMatrixToBeRemoved,
                 * newImpactedItems);
                 */
                manualItemsChanged = addNewManualItems(session, cmProcess,
                        manualItemsToBeAdded, manualItemsChanged,
                        newManualItems);

                saveChanges(session, cmProcess, false, manualItemsChanged,
                        impactedItemsChanged, null, newManualItems,
                        newImpactedItems);
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private boolean addNewManualItems(CoreSession session,
            DocumentModel cmProcess, List<ManualItem> manualItemsToBeAdded,
            boolean manualItemsChanged,
            ArrayList<HashMap<String, Object>> newManualItems)
            throws EloraException {
        if (manualItemsToBeAdded != null && manualItemsToBeAdded.size() > 0) {
            for (ManualItem manualItemToBeAdded : manualItemsToBeAdded) {

                long nextManualItemRowNumber = CMHelper.getManualItemsMaxRowNumberInCMProcess(
                        session, cmProcess.getId()) + 1;

                HashMap<String, Object> manualItemType = new HashMap<String, Object>();

                manualItemToBeAdded.setRowNumber(nextManualItemRowNumber);
                manualItemType = CMHelper.createManualItemType(
                        manualItemToBeAdded);
                newManualItems.add(manualItemType);
                manualItemsChanged = true;
            }
        }
        return manualItemsChanged;
    }

    private Map<String, List<HashMap<String, Object>>> convertManualImpactedItemsToModifiedIndexList(
            ArrayList<HashMap<String, Object>> manualImpactedItems) {

        HashMap<String, List<HashMap<String, Object>>> manualImpactedItemListWithModifiedIndex = new HashMap<String, List<HashMap<String, Object>>>();

        for (HashMap<String, Object> manualImpactedItemHM : manualImpactedItems) {
            String modifiedItem = (String) manualImpactedItemHM.get(
                    "modifiedItem");

            if (manualImpactedItemListWithModifiedIndex.containsKey(
                    modifiedItem)) {
                manualImpactedItemListWithModifiedIndex.get(modifiedItem).add(
                        manualImpactedItemHM);
            } else {
                List<HashMap<String, Object>> manualImpactedItemList = new ArrayList<HashMap<String, Object>>();
                manualImpactedItemList.add(manualImpactedItemHM);
                manualImpactedItemListWithModifiedIndex.put(modifiedItem,
                        manualImpactedItemList);
            }
        }

        return manualImpactedItemListWithModifiedIndex;
    }

}
