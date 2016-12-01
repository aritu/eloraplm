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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.core.util.EloraUtilHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * Helper class for Elora Change Management.
 *
 * @author aritu
 *
 */
public class CMHelper {

    private static final Log log = LogFactory.getLog(CMHelper.class);

    /**
     * @param session
     * @param cmProcess
     * @param originItemDocRef
     * @throws EloraException
     */
    // TODO:::: TO BE REMOVED
    @SuppressWarnings("unchecked")
    /*public static void addModifiedItemToCMProcess(CoreSession session,
            DocumentModel cmProcess, DocumentRef originItemDocRef)
            throws EloraException {

        String logInitMsg = "[addModifiedItemToCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Check specified input parameters
        if (cmProcess == null) {
            log.error(logInitMsg + " Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }
        if (originItemDocRef == null) {
            log.error(logInitMsg + " Specified originItemDocRef is null.");
            throw new EloraException("Specified originItemDocRef is null.");
        }

        try {

            String originItemUid = originItemDocRef.toString();

            // Verify that specified modifiedItem is not already included in the
            // modifiedItems
            if (existModifiedItemInCMProcess(session, cmProcess,
                    originItemUid)) {

                log.info(logInitMsg + "Specified modifiedItem (" + originItemUid
                        + ") already exist CM Process (" + cmProcess.getId()
                        + ").");
                throw new EloraException(
                        "modifiedItem already exist CM Process.");

            } else {
                DocumentModel originItem = session.getDocument(
                        originItemDocRef);

                checkOriginItem(session, originItem);

                // Create the modified item
                ModifiedItem modifiedItem = createModifiedItem(session,
                        originItem, cmProcess);

                // calculate the Impact Matrixes (for the instance docs and
                // boms)
                List<ImpactedItem> docsImpactMatrix = new ArrayList<ImpactedItem>();
                List<ImpactedItem> bomsImpactMatrix = new ArrayList<ImpactedItem>();

                calculateImpactMatrixes(session, docsImpactMatrix,
                        bomsImpactMatrix, originItem);

                // Store @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ TODO:: ZATI HAU
                // BESTE METODO BATERA PASA??? IMPACT MATRIXEKIN BATERA
                ArrayList<HashMap<String, Object>> modifiedItemsContent = new ArrayList<HashMap<String, Object>>();
                ArrayList<HashMap<String, Object>> docsImpactMatrixContent = new ArrayList<HashMap<String, Object>>();
                ArrayList<HashMap<String, Object>> bomsImpactMatrixContent = new ArrayList<HashMap<String, Object>>();
                try {
                    // First, retrieve the modifications and impact matrixes
                    // that are currently stored in the process, if any.
                    if (cmProcess.getPropertyValue(
                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST) != null) {
                        modifiedItemsContent = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                                CMMetadataConstants.MOD_MODIFIED_ITEM_LIST);
                    }

                    if (cmProcess.getPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                        docsImpactMatrixContent = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                                CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
                    }

                    if (cmProcess.getPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST) != null) {
                        bomsImpactMatrixContent = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                                CMMetadataConstants.BOM_IMPACTED_ITEM_LIST);
                    }

                    // Create new modifiedItem and impactedItems types as stored
                    // in the system.
                    HashMap<String, Object> modifiedItemType = createModifiedItemType(
                            modifiedItem);
                    modifiedItemsContent.add(modifiedItemType);

                    for (int i = 0; i < docsImpactMatrix.size(); ++i) {
                        ImpactedItem docImpactedItem = docsImpactMatrix.get(i);
                        HashMap<String, Object> docImpactedItemType = createImpactedItemType(
                                docImpactedItem);
                        docsImpactMatrixContent.add(docImpactedItemType);
                    }

                    for (int i = 0; i < bomsImpactMatrix.size(); ++i) {
                        ImpactedItem bomImpactedItem = bomsImpactMatrix.get(i);
                        HashMap<String, Object> bomImpactedItemType = createImpactedItemType(
                                bomImpactedItem);
                        bomsImpactMatrixContent.add(bomImpactedItemType);
                    }

                    // Store new modifiedItem and impactedItems.
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST,
                            modifiedItemsContent);

                    cmProcess.setPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST,
                            docsImpactMatrixContent);

                    cmProcess.setPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST,
                            bomsImpactMatrixContent);

                    // save the document
                    session.saveDocument(cmProcess);
                    session.save();

                    log.info(logInitMsg + "ModifiedItem successfully added.");

                } catch (NuxeoException e) {
                    log.error(logInitMsg + e.getMessage(), e);
                    throw new EloraException(
                            "Nuxeo exception thrown: |" + e.getMessage() + "|");
                } catch (Exception e) { // TODO???????????????? begiratu hau???
                                        // beharrezkoa da???
                    log.error(logInitMsg + e.getMessage(), e);
                    throw new EloraException(
                            "Nuxeo exception thrown: |" + e.getMessage() + "|");
                }

                // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

                log.info(logInitMsg + "modified Item added to the process.");
            }

        } catch (EloraException e) { // TODO???????????????? begiratu hau???
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }*/

    /* private static ModifiedItem createModifiedItem(CoreSession session,
            ModifiedItem modifiedItem, DocumentModel cmProcess)
            throws EloraException {
    
        String originItemUid = originItem.getId();
    
        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);
        String originItemWcUid = originItemWc.getId();
    
        // TODO:: to be removed
        long maxRowNumber = getModifiedItemsMaxRowNumberInCMProcess(session,
                cmProcess);
    
        long rowNumber = maxRowNumber + 1;
    
        String destinationItemUid = originItemWcUid;
    
        String itemType = getItemType(originItem);
    
        ModifiedItem modifiedItem = new ModifiedItem(originItemUid,
                originItemWcUid, CMConstants.ACTION_CHANGE, destinationItemUid,
                false, itemType);
    
        return modifiedItem;
    }*/

    private static HashMap<String, Object> createModifiedItemType(
            ModifiedItem modifiedItem, long rowNumber) {

        // items
        HashMap<String, Object> modifiedItemType = new HashMap<>();

        // --- rowNumber
        modifiedItemType.put("rowNumber", rowNumber);

        // --- originItem
        modifiedItemType.put("originItem", modifiedItem.getOriginItem());

        // --- originItemWc
        modifiedItemType.put("originItemWc", modifiedItem.getOriginItemWc());

        // --- action
        modifiedItemType.put("action", modifiedItem.getAction());

        // --- destinationItem
        modifiedItemType.put("destinationItem",
                modifiedItem.getDestinationItem());

        // --- isManaged
        modifiedItemType.put("isManaged", modifiedItem.isManaged());

        // --- type
        modifiedItemType.put("type", modifiedItem.getType());

        return modifiedItemType;
    }

    private static void updateModifiedItemType(
            HashMap<String, Object> modifiedItemType,
            ModifiedItem modifiedItem) {

        /*
        // --- originItem
        modifiedItemType.put("originItem", modifiedItem.getOriginItem());
        
        // --- originItemWc
        modifiedItemType.put("originItemWc", modifiedItem.getOriginItemWc());
        
        // --- action
        modifiedItemType.put("action", modifiedItem.getAction());
        
        
        // --- type
        modifiedItemType.put("type", modifiedItem.getType());*/

        // --- isManaged
        modifiedItemType.put("isManaged", modifiedItem.isManaged());

        // --- destinationItem
        modifiedItemType.put("destinationItem",
                modifiedItem.getDestinationItem());

        // return modifiedItemType;
    }

    private static ImpactedItem createImpactedItem(CoreSession session,
            long rowNumber, DocumentModel modifiedItem,
            DocumentModel parentItem, DocumentModel originItem,
            String originItemType, boolean isManual, String messageType,
            String messageData) throws EloraException {

        String modifiedItemUid = modifiedItem.getId();

        String parentItemUid = parentItem.getId();

        String originItemUid = originItem.getId();

        DocumentModel originItemWc = getWcDocForOriginItem(session, originItem);
        String originItemWcUid = originItemWc.getId();

        String destinationItemUid = originItemWcUid;

        ImpactedItem impactedItem = new ImpactedItem(rowNumber, modifiedItemUid,
                parentItemUid, originItemUid, originItemWcUid,
                CMConstants.ACTION_CHANGE, destinationItemUid, false, isManual,
                originItemType, messageType, messageData);

        return impactedItem;
    }

    private static HashMap<String, Object> createImpactedItemType(
            ImpactedItem impactedItem) {

        // items
        HashMap<String, Object> impactedItemType = new HashMap<>();

        // --- rowNumber
        impactedItemType.put("rowNumber", impactedItem.getRowNumber());

        // --- modifiedItem
        impactedItemType.put("modifiedItem", impactedItem.getModifiedItem());

        // --- parentItem
        impactedItemType.put("parentItem", impactedItem.getParentItem());

        // --- originItem
        impactedItemType.put("originItem", impactedItem.getOriginItem());

        // --- originItemWc
        impactedItemType.put("originItemWc", impactedItem.getOriginItemWc());

        // --- action
        impactedItemType.put("action", impactedItem.getAction());

        // --- destinationItem
        impactedItemType.put("destinationItem",
                impactedItem.getDestinationItem());

        // --- isManaged
        impactedItemType.put("isManaged", impactedItem.isManaged());

        // --- isManual
        impactedItemType.put("isManual", impactedItem.isManual());

        // --- type
        impactedItemType.put("type", impactedItem.getType());

        // --- messageType
        impactedItemType.put("messageType", impactedItem.getMessageType());

        // --- messageData
        impactedItemType.put("messageData", impactedItem.getMessageData());

        return impactedItemType;
    }

    private static void calculateImpactMatrixes(CoreSession session,
            List<ImpactedItem> docsImpactMatrix,
            List<ImpactedItem> bomsImpactMatrix,
            DocumentModel modifiedItemOriginItem) throws EloraException {

        String logInitMsg = "[calculateImpactMatrixes] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO::: begiratu ea check hau utzi edo kendu
        // Check specified input parameters
        if (modifiedItemOriginItem == null) {
            log.error(
                    logInitMsg + " Specified modifiedItemOriginItem is null.");
            throw new EloraException(
                    "Specified modifiedItemOriginItem is null.");
        }
        try {

            // ---- modified item origin uid
            String modifiedItemOriginUid = modifiedItemOriginItem.getId();

            // Initialize impacted items context
            ImpactedItemsContext impactedItemsContext = new ImpactedItemsContext();

            // -----------------------------------------------------------------------------------------------------
            // -- Get relations types from configuration
            EloraConfigTable bomDirectRelsConfig = EloraConfigHelper.getBomDirectRelationsConfig();
            EloraConfigTable bomHierarchicalRelsConfig = EloraConfigHelper.getBomHierarchicalRelationsConfig();
            EloraConfigTable bomDocumentRelsConfig = EloraConfigHelper.getBomDocumentRelationsConfig();
            EloraConfigTable cadDirectRelsConfig = EloraConfigHelper.getCadDirectRelationsConfig();
            EloraConfigTable cadHierarchicalRelsConfig = EloraConfigHelper.getCadHierarchicalRelationsConfig();

            // -- Get released and obsolete states from configuration
            EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
            EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
            // -----------------------------------------------------------------------------------------------------
            // Calculate the impacted matrix for the specified modified item
            String itemType = getItemType(modifiedItemOriginItem);
            if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                impactedItemsContext.addImpactedBom(modifiedItemOriginUid);

                bomsImpactMatrix = calculateImpactedBomsForBom(session,
                        bomDirectRelsConfig, bomHierarchicalRelsConfig,
                        releasedStatesConfig, obsoleteStatesConfig,
                        modifiedItemOriginItem, modifiedItemOriginItem,
                        impactedItemsContext);

                docsImpactMatrix = calculateImpactedDocumentsForBom(session,
                        bomDocumentRelsConfig, cadDirectRelsConfig,
                        cadHierarchicalRelsConfig, releasedStatesConfig,
                        obsoleteStatesConfig, modifiedItemOriginItem,
                        modifiedItemOriginItem, impactedItemsContext);

            } else if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {

                bomsImpactMatrix = calculateImpactedBomsForDocument(session,
                        bomDocumentRelsConfig, bomDirectRelsConfig,
                        bomHierarchicalRelsConfig, releasedStatesConfig,
                        obsoleteStatesConfig, modifiedItemOriginItem,
                        modifiedItemOriginItem, impactedItemsContext);

                if (EloraDocumentHelper.isCadDocument(modifiedItemOriginItem)) {// TODO??????????????
                                                                                // begiratu
                                                                                // hau
                                                                                // ????????????
                    List<ImpactedItem> cadImpactedItemsList = new ArrayList<ImpactedItem>();
                    cadImpactedItemsList = calculateImpactedCadsAndBomsForCadDocument(
                            session, bomDocumentRelsConfig, cadDirectRelsConfig,
                            cadHierarchicalRelsConfig, releasedStatesConfig,
                            obsoleteStatesConfig, modifiedItemOriginItem,
                            modifiedItemOriginItem, impactedItemsContext);

                    docsImpactMatrix.addAll(cadImpactedItemsList); // TODO:
                                                                   // begiratu
                                                                   // hau???????????????
                }

            } else {
                // TODO:: THROW AN ERROR OR NOTHING TO DO????
            }

            log.trace(logInitMsg
                    + "impact matrixes have been calculated. Number of impacted items in BOMs Impact Matrix = |"
                    + bomsImpactMatrix.size()
                    + "|. Number of impacted items in DOCs Impact Matrix = |"
                    + docsImpactMatrix.size() + "|");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");

    }

    /**
     * True if there is already specified modifiedItem in the specified Change
     * Management process.
     *
     * @param session
     * @param cmProcess
     * @param modifiedItemOriginItemRealUid
     * @throws EloraException
     */
    // TODO:: izena aldatu: modifiedItemOriginItemRealUid =>
    // modifiedItemOriginItemUid
    public static boolean existModifiedItemInCMProcess(CoreSession session,
            String cmProcessUid, String modifiedItemOriginItemRealUid)
            throws EloraException {

        String logInitMsg = "[existModifiedItemInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean existModifiedItemInProcess = false;

        // Check specified input parameters and log them
        if (cmProcessUid == null) {
            log.error(logInitMsg + "Specified cmProcessUid is null.");
            throw new EloraException("Specified cmProcessUid is null.");
        }

        if (modifiedItemOriginItemRealUid == null
                || modifiedItemOriginItemRealUid.isEmpty()) {
            log.error(logInitMsg
                    + "Specified modifiedItemOriginItemRealUid is null or Empty.");
            throw new EloraException(
                    "Specified modifiedItemOriginItemRealUid is null.");
        }

        log.trace(logInitMsg + " enter parameters: cmProcessUid = |"
                + cmProcessUid + "|, modifiedItemOriginItemRealUid = |"
                + modifiedItemOriginItemRealUid + "|");

        IterableQueryResult queryResult = null;
        try {
            String query = CMQueryFactory.getCountModifiedItemsByOriginQuery(
                    cmProcessUid, modifiedItemOriginItemRealUid);

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

    // TODO:::: TO BE REMOVED ( OPERAZIORAKO UTZI ????)
    /* public static void removeModifiedItemFromCMProcess(CoreSession session,
            DocumentModel cmProcess, String originItemUidToBeRemoved)
            throws EloraException {
    
        String logInitMsg = "[removeModifiedItemFromCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
    
        // Check specified input parameters
        if (cmProcess == null) {
            log.error(logInitMsg + "Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }
        if (originItemUidToBeRemoved == null) {
            log.error(
                    logInitMsg + "Specified originItemUidToBeRemoved is null.");
            throw new EloraException(
                    "Specified originItemUidToBeRemoved is null.");
        }
    
        log.trace(logInitMsg + " enter parameters: cmProcess = |"
                + cmProcess.getId() + "|, originItemUidToBeRemoved = |"
                + originItemUidToBeRemoved + "|");
    
        try {
    
            // First, retrieve the modifications that are currently stored in
            // the process, if any.
            if (cmProcess.getPropertyValue(
                    CMMetadataConstants.MOD_MODIFIED_ITEM_LIST) == null) {
                log.error(logInitMsg
                        + "Specified cmProcess has not any modified items.");
                throw new EloraException(
                        "Specified cmProcess has not any modified items.");
            }
    
            ArrayList<HashMap<String, Object>> currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.MOD_MODIFIED_ITEM_LIST);
            if (currentModifiedItems.size() == 0) {
                log.error(logInitMsg
                        + "Specified cmProcess has not any modified items.");
                throw new EloraException(
                        "Specified cmProcess has not any modified items.");
            }
    
            ArrayList<HashMap<String, Object>> newModifiedItems = new ArrayList<HashMap<String, Object>>();
            boolean existModifiedItem = false;
    
            for (int i = 0; i < currentModifiedItems.size(); ++i) {
    
                HashMap<String, Object> modifiedItem = currentModifiedItems.get(
                        i);
    
                String modifiedItemOriginItemUid = (String) modifiedItem.get(
                        "originItem");
    
                if (originItemUidToBeRemoved.equals(
                        modifiedItemOriginItemUid)) {
                    existModifiedItem = true;
                } else {
                    newModifiedItems.add(modifiedItem);
                }
            }
    
            if (existModifiedItem) {
                // DOCS IMPACT MATRIX
                ArrayList<HashMap<String, Object>> currentDocsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                    currentDocsImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newDocsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                boolean existDocImpactedItem = false;
                for (int i = 0; i < currentDocsImpactMatrix.size(); ++i) {
    
                    HashMap<String, Object> docImpactedItem = currentDocsImpactMatrix.get(
                            i);
    
                    String docImpactedItemModifiedItem = (String) docImpactedItem.get(
                            "modifiedItem");
    
                    if (originItemUidToBeRemoved.equals(
                            docImpactedItemModifiedItem)) {
                        existDocImpactedItem = true;
                    } else {
                        newDocsImpactMatrix.add(docImpactedItem);
                    }
                }
    
                // BOMS IMPACT MATRIX
                ArrayList<HashMap<String, Object>> currentBomsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.BOM_IMPACTED_ITEM_LIST) != null) {
                    currentBomsImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newBomsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                boolean existBomImpactedItem = false;
                for (int i = 0; i < currentBomsImpactMatrix.size(); ++i) {
    
                    HashMap<String, Object> bomImpactedItem = currentBomsImpactMatrix.get(
                            i);
    
                    String bomImpactedItemModifiedItem = (String) bomImpactedItem.get(
                            "modifiedItem");
    
                    if (originItemUidToBeRemoved.equals(
                            bomImpactedItemModifiedItem)) {
                        existDocImpactedItem = true;
                    } else {
                        newBomsImpactMatrix.add(bomImpactedItem);
                    }
                }
    
                //////////////////////////
                // store new calculated items
                ////////////////////////// //////////////////
    
                cmProcess.setPropertyValue(
                        CMMetadataConstants.MOD_MODIFIED_ITEM_LIST,
                        newModifiedItems);
    
                if (existDocImpactedItem) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST,
                            newDocsImpactMatrix);
                }
    
                if (existBomImpactedItem) {
                    cmProcess.setPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST,
                            newBomsImpactMatrix);
                }
    
                session.saveDocument(cmProcess);
                session.save();
    
                String logMsg = "modifiedItem";
                if (existDocImpactedItem) {
                    logMsg = logMsg + " , related DOC ImpactedItems";
                }
                if (existBomImpactedItem) {
                    logMsg = logMsg + " , related BOM ImpactedItem";
                }
    
                log.info(logInitMsg + logMsg
                        + " successfully removed from cmProcess.");
    
            } else {
                log.error(logInitMsg
                        + "Specified cmProcess has not specified modifiedItem.");
                throw new EloraException(
                        "Specified cmProcess has not specified modifiedItem.");
            }
    
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
    
        log.trace(logInitMsg + "--- EXIT --- ");
    }*/

    public static long getModifiedItemsMaxRowNumberInCMProcess(
            CoreSession session, DocumentModel cmProcess)
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
                    cmProcess.getId());

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                if (map.get("MAX(" + CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                        + "/*/rowNumber" + ")") != null) {
                    String maxRowNumberStr = map.get(
                            "MAX(" + CMMetadataConstants.MOD_MODIFIED_ITEM_LIST
                                    + "/*/rowNumber" + ")").toString();
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
     * @throws EloraException
     */
    // TODO::: hau aldatu eta agian izena aldatu. Egin behar duena da:
    // - berriak gehitu + impact matrix kalkulatu
    // - ezabatzeko daudenak ezabatu + impact matrixa eguneratu
    // - lehenagotik zeudenak gorde
    public static void saveModifiedItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<String> originItemUidsToBeRemoved,
            List<ModifiedItem> modifiedItemsToBeAdded,
            HashMap<String, ModifiedItem> changedModifiedItems)
            throws EloraException {
        String logInitMsg = "[saveModifiedItemChangesInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            if ((originItemUidsToBeRemoved != null
                    && originItemUidsToBeRemoved.size() > 0)
                    || (modifiedItemsToBeAdded != null
                            && modifiedItemsToBeAdded.size() > 0
                            || (changedModifiedItems != null
                                    && changedModifiedItems.size() > 0))) {

                // Current Modified Items
                ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.MOD_MODIFIED_ITEM_LIST) != null) {
                    currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.MOD_MODIFIED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newModifiedItems = new ArrayList<HashMap<String, Object>>();

                // Current DOCs Impact Matrix
                ArrayList<HashMap<String, Object>> currentDocsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                    currentDocsImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newDocsImpactMatrix = new ArrayList<HashMap<String, Object>>();

                // Current BOMs Impact Matrix
                ArrayList<HashMap<String, Object>> currentBomsImpactMatrix = new ArrayList<HashMap<String, Object>>();
                if (cmProcess.getPropertyValue(
                        CMMetadataConstants.BOM_IMPACTED_ITEM_LIST) != null) {
                    currentBomsImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                            CMMetadataConstants.BOM_IMPACTED_ITEM_LIST);
                }
                ArrayList<HashMap<String, Object>> newBomsImpactMatrix = new ArrayList<HashMap<String, Object>>();

                // First, iterate current modified items. If they have to be
                // modified or removed, do it.
                // For the removed ones, remove also their impacted matrix

                if ((originItemUidsToBeRemoved != null
                        && originItemUidsToBeRemoved.size() > 0)
                        || (changedModifiedItems != null
                                && changedModifiedItems.size() > 0)) {
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
                            updateModifiedItemType(modifiedItem,
                                    changedModifiedItem);
                            newModifiedItems.add(modifiedItem);
                        } else if (originItemUidsToBeRemoved != null
                                && !originItemUidsToBeRemoved.contains(
                                        modifiedItemOriginItemUid)) {

                            newModifiedItems.add(modifiedItem);
                        }
                    }
                    for (int i = 0; i < currentDocsImpactMatrix.size(); ++i) {
                        HashMap<String, Object> docImpactedItem = currentDocsImpactMatrix.get(
                                i);
                        String docImpactedItemModifiedItem = (String) docImpactedItem.get(
                                "modifiedItem");
                        if (originItemUidsToBeRemoved.contains(
                                docImpactedItemModifiedItem)) {
                        } else {
                            newDocsImpactMatrix.add(docImpactedItem);
                        }
                    }
                    for (int i = 0; i < currentBomsImpactMatrix.size(); ++i) {
                        HashMap<String, Object> bomImpactedItem = currentBomsImpactMatrix.get(
                                i);
                        String bomImpactedItemModifiedItem = (String) bomImpactedItem.get(
                                "modifiedItem");
                        if (!originItemUidsToBeRemoved.contains(
                                bomImpactedItemModifiedItem)) {
                            newBomsImpactMatrix.add(bomImpactedItem);
                        }
                    }
                } else {
                    newModifiedItems = currentModifiedItems;
                    newDocsImpactMatrix = currentDocsImpactMatrix;
                    newBomsImpactMatrix = currentBomsImpactMatrix;
                }

                // Then, add the ones to be be added (and calculate impacted
                // items)
                if (modifiedItemsToBeAdded != null
                        && modifiedItemsToBeAdded.size() > 0) {
                    long rowNumber = getModifiedItemsMaxRowNumberInCMProcess(
                            session, cmProcess) + 1;

                    for (int i = 0; i < modifiedItemsToBeAdded.size(); ++i) {
                        ModifiedItem modifiedItemToBeAdded = modifiedItemsToBeAdded.get(
                                i);
                        String originItemUid = modifiedItemToBeAdded.getOriginItem();

                        if (!existModifiedItemInCMProcess(session,
                                cmProcess.getId(), originItemUid)) {
                            DocumentModel originItem = session.getDocument(
                                    new IdRef(
                                            modifiedItemToBeAdded.getOriginItem()));
                            checkOriginItem(session, originItem);
                            // TODO::: to be removed
                            /*// Create the modified item
                            ModifiedItem modifiedItem = createModifiedItem(
                                    session, modifiedItemToBeAdded, cmProcess);*/

                            HashMap<String, Object> modifiedItemType = createModifiedItemType(
                                    modifiedItemToBeAdded, rowNumber);
                            newModifiedItems.add(modifiedItemType);
                            rowNumber++;

                            List<ImpactedItem> docsImpactMatrix = new ArrayList<ImpactedItem>();
                            List<ImpactedItem> bomsImpactMatrix = new ArrayList<ImpactedItem>();

                            calculateImpactMatrixes(session, docsImpactMatrix,
                                    bomsImpactMatrix, originItem);

                            for (int j = 0; j < docsImpactMatrix.size(); ++j) {
                                ImpactedItem docImpactedItem = docsImpactMatrix.get(
                                        j);
                                HashMap<String, Object> docImpactedItemType = createImpactedItemType(
                                        docImpactedItem);
                                newDocsImpactMatrix.add(docImpactedItemType);
                            }

                            for (int j = 0; j < bomsImpactMatrix.size(); ++j) {
                                ImpactedItem bomImpactedItem = bomsImpactMatrix.get(
                                        j);
                                HashMap<String, Object> bomImpactedItemType = createImpactedItemType(
                                        bomImpactedItem);
                                newBomsImpactMatrix.add(bomImpactedItemType);
                            }

                        }
                    }
                }

                //////////////////////////
                // store new calculated items
                ////////////////////////// //////////////////
                cmProcess.setPropertyValue(
                        CMMetadataConstants.MOD_MODIFIED_ITEM_LIST,
                        newModifiedItems);

                cmProcess.setPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST,
                        newDocsImpactMatrix);

                cmProcess.setPropertyValue(
                        CMMetadataConstants.BOM_IMPACTED_ITEM_LIST,
                        newBomsImpactMatrix);

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

    /*public static void saveModifiedItemListInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<ModifiedItem> modifiedItems)
            throws EloraException {
    
        String logInitMsg = "[saveModifiedItemListInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
    
        // Check specified input parameters
        if (cmProcess == null) {
            log.error(logInitMsg + " Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }
    
        if (modifiedItems == null) {
            log.error(logInitMsg + " Specified modifiedItems is null.");
            throw new EloraException("Specified modifiedItems is null.");
        }
    
        ArrayList<HashMap<String, Object>> modifiedItemsContent = new ArrayList<HashMap<String, Object>>();
    
        try {
            for (int i = 0; i < modifiedItems.size(); ++i) {
    
                ModifiedItem modifiedItem = modifiedItems.get(i);
                HashMap<String, Object> modifiedItemType = createModifiedItemType(
                        modifiedItem);
                modifiedItemsContent.add(modifiedItemType);
            }
    
            // Store new modifiedItem
            cmProcess.setPropertyValue(
                    CMMetadataConstants.MOD_MODIFIED_ITEM_LIST,
                    modifiedItemsContent);
    
            session.saveDocument(cmProcess);
            session.save();
    
            log.info(logInitMsg + "ModifiedItems successfully saved.");
    
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    
    }*/

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

        try {
            ArrayList<HashMap<String, Object>> impactedItemsContent = new ArrayList<HashMap<String, Object>>();

            for (int i = 0; i < impactedItems.size(); ++i) {

                ImpactedItem impactedItem = impactedItems.get(i);
                HashMap<String, Object> impactedItemType = createImpactedItemType(
                        impactedItem);
                impactedItemsContent.add(impactedItemType);
            }

            // Store new impactedItems list in function of the impacted item
            // type
            String impactedItemPropertyName = "";
            if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
                impactedItemPropertyName = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
            } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
                impactedItemPropertyName = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
            }
            cmProcess.setPropertyValue(impactedItemPropertyName,
                    impactedItemsContent);

            session.saveDocument(cmProcess);
            session.save();

            log.info(logInitMsg + "ImpactedItems successfully saved.");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");

    }

    private static List<ImpactedItem> calculateImpactedDirectBomsForBom(
            CoreSession session, EloraConfigTable bomDirectRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedDirectBomsList = new ArrayList<ImpactedItem>();

        for (EloraConfigRow relationConfig : bomDirectRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, parentItem);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, "", "");
                    impactedDirectBomsList.add(impactedItem);
                    impactedItemsContext.addImpactedBom(
                            impactedItemDoc.getId());
                }
            }
        }

        return impactedDirectBomsList;
    }

    private static List<ImpactedItem> calculateImpactedHierarchicalBomsForBom(
            CoreSession session, EloraConfigTable bomDirectRelsConfig,
            EloraConfigTable bomHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedHierarchicalBomsList = new ArrayList<ImpactedItem>();

        for (EloraConfigRow relationConfig : bomHierarchicalRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, parentItem);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, "", "");
                    impactedHierarchicalBomsList.add(impactedItem);
                    impactedItemsContext.addImpactedBom(
                            impactedItemDoc.getId());

                    // calculate its related BOMS (recursive call)
                    List<ImpactedItem> recursiveImpactedBomList = new ArrayList<ImpactedItem>();
                    recursiveImpactedBomList = calculateImpactedBomsForBom(
                            session, bomDirectRelsConfig,
                            bomHierarchicalRelsConfig, releasedStatesConfig,
                            obsoleteStatesConfig, modifiedItem, impactedItemDoc,
                            impactedItemsContext);

                    impactedHierarchicalBomsList.addAll(
                            recursiveImpactedBomList);
                }
            }
        }

        return impactedHierarchicalBomsList;

    }

    private static List<ImpactedItem> calculateImpactedBomsForBom(
            CoreSession session, EloraConfigTable bomDirectRelsConfig,
            EloraConfigTable bomHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        // TODO: izena aldatu => impactedBomsList
        List<ImpactedItem> impactedBomsList = new ArrayList<ImpactedItem>();

        impactedBomsList.addAll(calculateImpactedDirectBomsForBom(session,
                bomDirectRelsConfig, releasedStatesConfig, obsoleteStatesConfig,
                modifiedItem, parentItem, impactedItemsContext));

        impactedBomsList.addAll(calculateImpactedHierarchicalBomsForBom(session,
                bomDirectRelsConfig, bomHierarchicalRelsConfig,
                releasedStatesConfig, obsoleteStatesConfig, modifiedItem,
                parentItem, impactedItemsContext));

        return impactedBomsList;
    }

    private static List<ImpactedItem> calculateImpactedDocumentsForBom(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable cadDirectRelsConfig,
            EloraConfigTable cadHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedDocumentsList = new ArrayList<ImpactedItem>();

        for (EloraConfigRow relationConfig : bomDocumentRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getObjectDocuments(
                    parentItem, predicateResource);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    String messageType = "";
                    String messageData = "";
                    if (EloraDocumentHelper.isCadDocument(impactedItemDoc)) {
                        List<String> impactedBomsList = verifyIfExistOtherImpactedBomsForDocument(
                                session, bomDocumentRelsConfig,
                                releasedStatesConfig, obsoleteStatesConfig,
                                impactedItemDoc, impactedItemsContext);
                        if (!impactedBomsList.isEmpty()) {
                            messageType = CMConstants.MSG_TYPE_WARNING_BOM;
                            for (String impactedBom : impactedBomsList) {
                                if (!messageData.isEmpty()) {
                                    messageData += ", ";
                                }
                                messageData += impactedBom;
                            }
                        }
                    }

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, messageType, messageData);
                    impactedDocumentsList.add(impactedItem);

                    // if it is a CAD document, find out its related
                    // BOMs and CADs
                    if (EloraDocumentHelper.isCadDocument(impactedItemDoc)) {
                        // Find out its related CADs and BOMs
                        List<ImpactedItem> cadImpactedItemsList = new ArrayList<ImpactedItem>();
                        cadImpactedItemsList = calculateImpactedCadsAndBomsForCadDocument(
                                session, bomDocumentRelsConfig,
                                cadDirectRelsConfig, cadHierarchicalRelsConfig,
                                releasedStatesConfig, obsoleteStatesConfig,
                                modifiedItem, impactedItemDoc,
                                impactedItemsContext);
                        impactedDocumentsList.addAll(cadImpactedItemsList);
                    }
                }
            }
        }

        return impactedDocumentsList;
    }

    private static List<ImpactedItem> calculateImpactedBomsForDocument(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable bomDirectRelsConfig,
            EloraConfigTable bomHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedBomsList = new ArrayList<ImpactedItem>();

        for (EloraConfigRow relationConfig : bomDocumentRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, parentItem);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, "", "");
                    impactedBomsList.add(impactedItem);
                    impactedItemsContext.addImpactedBom(
                            impactedItemDoc.getId());

                    // For each impacted BOM, find out its related other BOMs
                    List<ImpactedItem> bomRelatedBomsList = new ArrayList<ImpactedItem>();
                    bomRelatedBomsList = calculateImpactedBomsForBom(session,
                            bomDirectRelsConfig, bomHierarchicalRelsConfig,
                            releasedStatesConfig, obsoleteStatesConfig,
                            modifiedItem, parentItem, impactedItemsContext);
                    impactedBomsList.addAll(bomRelatedBomsList);
                }
            }
        }

        return impactedBomsList;
    }

    // TODO::: izena aldatu honi => verifyIfExistOtherImpactedBomsForDocument
    private static List<String> verifyIfExistOtherImpactedBomsForDocument(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel itemOriginDocM,
            ImpactedItemsContext impactedItemsContext) throws EloraException {

        List<String> otherImpactedBomsList = new ArrayList<String>();

        for (EloraConfigRow relationConfig : bomDocumentRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, itemOriginDocM);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    // Verify that found BOM is not already defined in
                    // impactedItemBomList
                    if (!impactedItemsContext.existImpactedBom(
                            impactedItemDoc.getId())) {
                        otherImpactedBomsList.add(impactedItemDoc.getId());
                    }
                }
            }
        }

        return otherImpactedBomsList;

    }

    // TODO IZENA ALDATU
    private static List<ImpactedItem> calculateImpactedDirectCadsAndBomsForCadDocument(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable cadDirectRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedDirectCadsAndBomsList = new ArrayList<ImpactedItem>();

        // find out its related Direct CADS and for each CAD the related BOMs
        for (EloraConfigRow relationConfig : cadDirectRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, parentItem);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    String messageType = "";
                    String messageData = "";
                    List<String> impactedBomsList = verifyIfExistOtherImpactedBomsForDocument(
                            session, bomDocumentRelsConfig,
                            releasedStatesConfig, obsoleteStatesConfig,
                            impactedItemDoc, impactedItemsContext);
                    if (!impactedBomsList.isEmpty()) {
                        messageType = CMConstants.MSG_TYPE_WARNING_BOM;
                        for (String impactedBom : impactedBomsList) {
                            if (!messageData.isEmpty()) {
                                messageData += ", ";
                            }
                            messageData += impactedBom;
                        }
                    }

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, messageType, messageData);
                    impactedDirectCadsAndBomsList.add(impactedItem);
                }
            }
        }

        return impactedDirectCadsAndBomsList;

    }

    // TODO IZENA ALDATU
    private static List<ImpactedItem> calculateImpactedHierarchicalCadsAndBomsForCadDocument(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable cadDirectRelsConfig,
            EloraConfigTable cadHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> hierarchicalCadsAndBomsList = new ArrayList<ImpactedItem>();

        // find out its related Direct CADS and for each CAD the related BOMs
        for (EloraConfigRow relationConfig : cadHierarchicalRelsConfig.getValues()) {
            String predicate = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicate);

            DocumentModelList docMList = RelationHelper.getSubjectDocuments(
                    predicateResource, parentItem);

            DocumentModelList filteredDocMList = filterLastestReleasedDocuments(
                    session, releasedStatesConfig, obsoleteStatesConfig,
                    docMList);

            if (filteredDocMList != null && !filteredDocMList.isEmpty()) {
                for (DocumentModel impactedItemDoc : filteredDocMList) {
                    String impactedItemType = getItemType(impactedItemDoc);
                    impactedItemsContext.increaseRowNumber(impactedItemType);

                    String messageType = "";
                    String messageData = "";
                    List<String> impactedBomsList = verifyIfExistOtherImpactedBomsForDocument(
                            session, bomDocumentRelsConfig,
                            releasedStatesConfig, obsoleteStatesConfig,
                            impactedItemDoc, impactedItemsContext);
                    if (!impactedBomsList.isEmpty()) {
                        messageType = CMConstants.MSG_TYPE_WARNING_BOM;
                        for (String impactedBom : impactedBomsList) {
                            if (!messageData.isEmpty()) {
                                messageData += ", ";
                            }
                            messageData += impactedBom;
                        }
                    }

                    ImpactedItem impactedItem = createImpactedItem(session,
                            impactedItemsContext.getRowNumber(impactedItemType),
                            modifiedItem, parentItem, impactedItemDoc,
                            impactedItemType, false, messageType, messageData);
                    hierarchicalCadsAndBomsList.add(impactedItem);

                    // find out its related CADs (recursive call)
                    List<ImpactedItem> recursiveCadItemsList = new ArrayList<ImpactedItem>();
                    recursiveCadItemsList = calculateImpactedCadsAndBomsForCadDocument(
                            session, bomDocumentRelsConfig, cadDirectRelsConfig,
                            cadHierarchicalRelsConfig, releasedStatesConfig,
                            obsoleteStatesConfig, modifiedItem, impactedItemDoc,
                            impactedItemsContext);

                    hierarchicalCadsAndBomsList.addAll(recursiveCadItemsList);
                }
            }
        }

        return hierarchicalCadsAndBomsList;

    }

    // TODO IZENA ALDATU
    private static List<ImpactedItem> calculateImpactedCadsAndBomsForCadDocument(
            CoreSession session, EloraConfigTable bomDocumentRelsConfig,
            EloraConfigTable cadDirectRelsConfig,
            EloraConfigTable cadHierarchicalRelsConfig,
            EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModel modifiedItem,
            DocumentModel parentItem, ImpactedItemsContext impactedItemsContext)
            throws EloraException {

        List<ImpactedItem> impactedCadsAndBomsList = new ArrayList<ImpactedItem>();

        impactedCadsAndBomsList.addAll(
                calculateImpactedDirectCadsAndBomsForCadDocument(session,
                        bomDocumentRelsConfig, cadDirectRelsConfig,
                        releasedStatesConfig, obsoleteStatesConfig,
                        modifiedItem, parentItem, impactedItemsContext));

        impactedCadsAndBomsList.addAll(
                calculateImpactedHierarchicalCadsAndBomsForCadDocument(session,
                        bomDocumentRelsConfig, cadDirectRelsConfig,
                        cadHierarchicalRelsConfig, releasedStatesConfig,
                        obsoleteStatesConfig, modifiedItem, parentItem,
                        impactedItemsContext));

        return impactedCadsAndBomsList;

    }

    /////////////////////////////////////////////////////////

    private static DocumentModelList filterLastestReleasedDocuments(
            CoreSession session, EloraConfigTable releasedStatesConfig,
            EloraConfigTable obsoleteStatesConfig, DocumentModelList docMList)
            throws EloraException {

        String logInitMsg = "[filterLastestReleasedDocuments] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModelList filteredDocMList = new DocumentModelListImpl();

        try {
            if (docMList != null && !docMList.isEmpty()) {

                log.trace(logInitMsg + "docMList.size = |" + docMList.size()
                        + "|");

                if (docMList.size() == 1) {
                    DocumentModel docM = docMList.get(0);
                    if (docM.isVersion()) {
                        filteredDocMList = docMList;
                    }
                } else {
                    // Store all the documents in a HashMap for retrieving them
                    // easily without searching in the DB.
                    // --- key: UID of the document
                    // --- value: DocumentModel
                    Map<String, DocumentModel> docMMap = new HashMap<String, DocumentModel>();
                    // Insert document list in a HashMap, classified by its
                    // versionableId
                    // --- key: versionableId
                    // --- value: list of UID having the same versionableId
                    Map<String, List<String>> versionableIdMap = new LinkedHashMap<String, List<String>>();

                    for (DocumentModel docM : docMList) {
                        String docUid = docM.getId();
                        docMMap.put(docUid, docM);

                        String docVersionableId = session.getWorkingCopy(
                                docM.getRef()).getId();

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
                                    DocumentModel docM = docMMap.get(docUid);
                                    if (docM.isVersion()) {
                                        filteredDocMList.add(docM);
                                    }
                                } else {
                                    String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                                            new String[0]);
                                    String[] uids = uidList.toArray(
                                            new String[0]);

                                    // retrieve the latest released version
                                    DocumentModel latestReleased = null;
                                    String query = EloraQueryFactory.getRelatedReleasedDoc(
                                            releasedStates, uids);
                                    DocumentModelList relatedReleasedDocs = session.query(
                                            query);
                                    if (relatedReleasedDocs.size() > 0) {
                                        latestReleased = relatedReleasedDocs.get(
                                                0);
                                    } else {
                                        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                                                new String[0]);
                                        // retrieve the latest related version
                                        query = EloraQueryFactory.getLatestRelatedDoc(
                                                obsoleteStates, uids);
                                        relatedReleasedDocs = session.query(
                                                query);
                                        if (relatedReleasedDocs.size() > 0) {
                                            latestReleased = relatedReleasedDocs.get(
                                                    0);
                                        }
                                    }
                                    filteredDocMList.add(latestReleased);
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

        log.trace(logInitMsg + "--- EXIT --- with filteredDocMList.size = |"
                + filteredDocMList.size() + "|");

        return filteredDocMList;
    }

    ///////////////////////////////////////////////////////

    // ????????????????????
    /*public static void recalculateAndSaveAffectedItemList(CoreSession session,
            DocumentModel cmProcessDM) throws EloraException {
    
        String logInitMsg = "[recalculateAndSaveAffectedItemList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
    
        // Check specified input parameters
        if (cmProcessDM == null) {
            log.error(logInitMsg + " Specified cmProcessDM is null.");
            throw new EloraException("Specified cmProcessDM is null.");
        }
        try {
    
            // ------------------------------------------------------------
            // 1) Recalculate affected items for the specified process
            // ------------------------------------------------------------
            ArrayList<HashMap<String, Object>> currentAffectedItems = new ArrayList<HashMap<String, Object>>();
            ArrayList<HashMap<String, Object>> mergedAffectedItems = new ArrayList<HashMap<String, Object>>();
    
            // First, retrieve the modifications that are currently stored in
            // the process, if any.
            if (cmProcessDM.getPropertyValue(
                    CMMetadataConstants.AFF_AFFECTED_ITEM_LIST) != null) {
                currentAffectedItems = (ArrayList<HashMap<String, Object>>) cmProcessDM.getPropertyValue(
                        CMMetadataConstants.AFF_AFFECTED_ITEM_LIST);
            }
            log.trace(logInitMsg + "currentAffectedItems.size() = |"
                    + currentAffectedItems.size() + "|");
            if (currentAffectedItems.size() == 0) {
                // there is nothing to do. Exit.
                log.info("Specified process |" + cmProcessDM.getId()
                        + "| has not any modification.");
                return;
            }
    
            LinkedHashMap<String, LinkedHashMap<String, HashMap<String, Object>>> currentAffectedItemHM = convertAffectedItemListIntoHashMap(
                    currentAffectedItems);
    
            // Iterate over each modifiedItem (currentAffectedItemHM)
            Iterator modifiedItemsIt = currentAffectedItemHM.entrySet().iterator();
            while (modifiedItemsIt.hasNext()) {
    
                long processRowNumber = 1;
    
                Map.Entry modifiedItemPair = (Map.Entry) modifiedItemsIt.next();
                String modifiedItem = (String) modifiedItemPair.getKey();
                HashMap<String, HashMap<String, Object>> modifiedItemCurrentAffectedItemHM = (HashMap<String, HashMap<String, Object>>) modifiedItemPair.getValue();
    
                // For each modifiedItem, calculate the new affected items and
                // compare with its current affected items
                List<ModifiedOrAffectedItem> modifiedItemNewAffectedItemList = calculateAffectedItemList(
                        session, new IdRef(modifiedItem));
                LinkedHashMap<String, HashMap<String, Object>> modifiedItemNewAffectedItemHM = convertModificationAffectedItemListIntoHashMap(
                        modifiedItemNewAffectedItemList);
    
                // Compare both HashMaps
                Iterator modifiedItemCurrentAffectedItemsKeysIt = modifiedItemCurrentAffectedItemHM.keySet().iterator();
                while (modifiedItemCurrentAffectedItemsKeysIt.hasNext()) {
                    String currentAffecItemKey = (String) modifiedItemCurrentAffectedItemsKeysIt.next();
                    if (modifiedItemNewAffectedItemHM.containsKey(
                            currentAffecItemKey)) {
                        // retrieve current instance (since maybe it has already
                        // be managed)
                        HashMap<String, Object> currentAffecItemValue = modifiedItemCurrentAffectedItemHM.get(
                                currentAffecItemKey);
                        // set new processRowNumber
                        currentAffecItemValue.put("processRowNumber",
                                processRowNumber);
                        // add it to mergedAffectedItems
                        mergedAffectedItems.add(currentAffecItemValue);
                        processRowNumber++;
                        // remove it from the newAffectedItemHM
                        modifiedItemNewAffectedItemHM.remove(
                                currentAffecItemKey);
                    }
                    // else, nothing to do. It means that it has been removed.
                }
                // iterate over new calculated items to add the new ones
                Iterator modifiedItemNewAffectedItemsKeysIt = modifiedItemNewAffectedItemHM.keySet().iterator();
                while (modifiedItemNewAffectedItemsKeysIt.hasNext()) {
                    String newAffecItemKey = (String) modifiedItemNewAffectedItemsKeysIt.next();
                    // retrieve new instance (since is a new one)
                    HashMap<String, Object> newAffecItemValue = modifiedItemNewAffectedItemHM.get(
                            newAffecItemKey);
                    // set new processRowNumber
                    newAffecItemValue.put("processRowNumber", processRowNumber);
                    // add it to mergedAffectedItems
                    mergedAffectedItems.add(newAffecItemValue);
                    processRowNumber++;
                }
            }
            log.trace(logInitMsg + "mergedAffectedItems.size() = |"
                    + mergedAffectedItems.size() + "|");
    
            // ----------------------------------------------------------------------------
            // 2) Store merged affected items in the Change Management process
            // -----------------------------------------------------------------------------
            cmProcessDM.setPropertyValue(
                    CMMetadataConstants.AFF_AFFECTED_ITEM_LIST,
                    mergedAffectedItems);
    
            session.saveDocument(cmProcessDM);
            session.save();
    
            log.info(logInitMsg + "affectedItemList recalculated and saved.");
    
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
    
        log.trace(logInitMsg + "--- EXIT --- ");
    }*/

    // TODO: dokumentatu ze nolako estruktura sortzen dugun gero konparazioa
    // errazteko
    // Lehenengo HashMap-aren key-a: modifiedItem
    // Bigarren HashMap-aren key-a: originItem.'|'.parentItem
    // Hirugarren HashMap-aren key-a: propitatearen izena
    /* private static LinkedHashMap<String, LinkedHashMap<String, HashMap<String, Object>>> convertAffectedItemListIntoHashMap(
            List<HashMap<String, Object>> affectedItemList) {
    
        LinkedHashMap<String, LinkedHashMap<String, HashMap<String, Object>>> affectedItemHM = new LinkedHashMap<String, LinkedHashMap<String, HashMap<String, Object>>>();
    
        for (HashMap<String, Object> modifiedOrAffectedItem : affectedItemList) {
    
            String modifiedItem = (String) modifiedOrAffectedItem.get(
                    "modifiedItem");
            String originItemConcatParentItem = (String) modifiedOrAffectedItem.get(
                    "originItem") + '|'
                    + (String) modifiedOrAffectedItem.get("parentItem");
    
            if (!affectedItemHM.containsKey(modifiedItem)) {
                LinkedHashMap<String, HashMap<String, Object>> modifiedItemHM = new LinkedHashMap<String, HashMap<String, Object>>();
                modifiedItemHM.put(originItemConcatParentItem,
                        modifiedOrAffectedItem);
                affectedItemHM.put(modifiedItem, modifiedItemHM);
    
            } else {
                affectedItemHM.get(modifiedItem).put(originItemConcatParentItem,
                        modifiedOrAffectedItem);
            }
        }
        return affectedItemHM;
    }*/

    // TODO::: izen egokiagoa pentsatu
    /*private static LinkedHashMap<String, HashMap<String, Object>> convertModificationAffectedItemListIntoHashMap(
            List<ModifiedOrAffectedItem> modificationAffectedItemList) {
    
        LinkedHashMap<String, HashMap<String, Object>> affectedItemHM = new LinkedHashMap<String, HashMap<String, Object>>();
    
        for (ModifiedOrAffectedItem affectedItem : modificationAffectedItemList) {
    
            String originItemConcatParentItem = affectedItem.getOriginItem()
                    + '|' + affectedItem.getParentItem();
            HashMap<String, Object> affectedItemType = createAffectedItemType(
                    affectedItem);
            affectedItemHM.put(originItemConcatParentItem, affectedItemType);
        }
        return affectedItemHM;
    }*/

    public static Map<String, String> calculateReleasedVersionList(
            CoreSession session, String docWcUid,
            boolean returnDefaultSelectedKey) throws EloraException {

        /*String logInitMsg = "[calculateReleasedVersionList] ["
                + session.getPrincipal().getName() + "] ";*/
        // log.trace(logInitMsg + "--- ENTER --- ");

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();

        Map<String, String> versionList = calculateVersionList(session,
                docWcUid, releasedStatesConfig, returnDefaultSelectedKey);

        // log.trace(logInitMsg + "--- EXIT --- ");
        return versionList;
    }

    public static Map<String, String> calculateVersionList(CoreSession session,
            String docWcUid, boolean returnDefaultSelectedKey)
            throws EloraException {

        Map<String, String> versionList = calculateVersionList(session,
                docWcUid, null, returnDefaultSelectedKey);

        return versionList;
    }

    /**
     * This methods returns an ordered list of the versions of the specified
     * document.
     *
     * @param session current session.
     * @param docWcUid Working Copy of the document which version list will be
     *            calculated.
     * @param docStatesConfig states to be taken into account for construing the
     *            version list.
     * @param returnDefaultSelectedKey if true, the version of the list that
     *            should be selected by default will be marked with an special
     *            constant.
     * @return
     * @throws EloraException
     */
    private static Map<String, String> calculateVersionList(CoreSession session,
            String docWcUid, EloraConfigTable docStatesConfig,
            boolean returnDefaultSelectedKey) throws EloraException {

        String logInitMsg = "[calculateVersionList] ["
                + session.getPrincipal().getName() + "] ";

        Map<String, String> sortedVersionList = new LinkedHashMap<String, String>();

        try {
            DocumentRef wcRef = new IdRef(docWcUid);
            DocumentModel wcDoc = session.getDocument(wcRef);
            if (wcDoc != null) {
                String lastVersionUid = "";
                try {
                    DocumentModel lastVersionDoc = EloraDocumentHelper.getLatestVersion(
                            wcDoc, session);
                    if (lastVersionDoc != null) {
                        lastVersionUid = lastVersionDoc.getId();
                    }

                    Map<String, String> versionList = new HashMap<String, String>();
                    for (DocumentModel versionDoc : session.getVersions(
                            wcRef)) {
                        String versionState = versionDoc.getCurrentLifeCycleState();
                        if (docStatesConfig == null
                                || docStatesConfig.containsKey(versionState)) {
                            String versionRealUid = versionDoc.getId();
                            String versionLabel = versionDoc.getVersionLabel();
                            if (versionRealUid.equals(lastVersionUid)) {
                                versionLabel += " ("
                                        + EloraMessageHelper.getTranslatedMessage(
                                                "eloraplm.label.document.version.latest")
                                        + ")";
                                if (returnDefaultSelectedKey) {
                                    // mark the element with the latest version
                                    // as selected by default.
                                    versionList.put(
                                            CMConstants.DEFAULT_SELECTED_KEY,
                                            versionRealUid);
                                }
                            }
                            versionList.put(versionRealUid, versionLabel);
                        }
                    }

                    if (versionList.size() > 0) {
                        // order the version list
                        sortedVersionList = EloraUtilHelper.sortMapByValueAsc(
                                versionList);

                        if (returnDefaultSelectedKey) {
                            // if the last element of the list is not the latest
                            // version, mark the last element as selected by
                            // default.
                            if (!sortedVersionList.containsKey(
                                    CMConstants.DEFAULT_SELECTED_KEY)) {
                                Set<String> sortedVersionListKeys = sortedVersionList.keySet();
                                Object[] sortedVersionListKeysArray = sortedVersionListKeys.toArray();
                                String lastElementRealUid = (String) sortedVersionListKeysArray[sortedVersionList.size()
                                        - 1];

                                sortedVersionList.put(
                                        CMConstants.DEFAULT_SELECTED_KEY,
                                        lastElementRealUid);
                            }
                        }
                    }
                } catch (EloraException e) {
                    return sortedVersionList;
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }
        return sortedVersionList;
    }

}
