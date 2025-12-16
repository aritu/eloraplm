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
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

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
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.cm.CMItem;
import com.aritu.eloraplm.cm.ManualItem;
import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.config.util.CMConfig;
import com.aritu.eloraplm.config.util.CMImpactableConfig;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.QueriesConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
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
                    cmProcessUid, modifiedItemOriginItemUid);

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
            ArrayList<HashMap<String, Object>> currentdItems = new ArrayList<HashMap<String, Object>>();

            String itemListMetada = "";
            if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
                itemListMetada = CMMetadataConstants.IMPACTED_ITEM_LIST;
            } else if (itemClass.equals(CMConstants.ITEM_CLASS_MODIFIED)) {
                itemListMetada = CMMetadataConstants.MODIFIED_ITEM_LIST;
            }

            if (cmProcess.getPropertyValue(itemListMetada) != null) {
                currentdItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                        itemListMetada);
            }

            if (currentdItems != null && currentdItems.size() > 0) {
                ArrayList<HashMap<String, Object>> newItems = new ArrayList<HashMap<String, Object>>();
                newItems.addAll(setAsManaged(session, currentdItems,
                        destinationWcUidList, itemType));

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

    private static ArrayList<HashMap<String, Object>> setAsManaged(
            CoreSession session,
            ArrayList<HashMap<String, Object>> impactMatrix,
            List<String> destinationWcUidList, String itemType)
            throws EloraException {

        String logInitMsg = "[setAsManaged]";
        log.trace(logInitMsg + "--- ENTER --- ");

        ArrayList<HashMap<String, Object>> newImpactMatrix = new ArrayList<HashMap<String, Object>>();

        try {
            for (int i = 0; i < impactMatrix.size(); ++i) {
                HashMap<String, Object> impactedItem = impactMatrix.get(i);

                boolean isManaged = (boolean) impactedItem.get("isManaged");
                String destinationItemWc = (String) impactedItem.get(
                        "destinationItemWc");
                String impactedItemType = (String) impactedItem.get("type");

                // if impactedItem is not already managed and destinationWc
                // is contained in the specified destinationWcUidList
                if (impactedItemType.equals(itemType) && !isManaged
                        && destinationWcUidList.contains(destinationItemWc)) {
                    setAsManagedCMItem(session, impactedItem);
                }
                newImpactMatrix.add(impactedItem);
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

    /**
     * This methods returns a descending ordered list of versions of
     * the specified document. This list excludes the obsolete and DELETED
     * versions. This list includes: - all the RELEASED versions - if the last
     * major is not in a RELEASED state, retrieve all the versions related to
     * that major (released or not) - if there is not any released version, all
     * current versions of the document
     *
     * @param session  current session.
     * @param docWcUid Working Copy of the document which version list will be
     *                 calculated.
     * @return
     * @throws EloraException
     */
    public static List<SelectItem> calculateModifiableItemVersionList(
            CoreSession session, String docWcUid, Map<String, String> messages)
            throws EloraException {

        String logInitMsg = "[calculateModifiableItemVersionList] ["
                + session.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        List<SelectItem> versionList = new ArrayList<SelectItem>();
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
                            QueriesConstants.SORT_ORDER_DESC);
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
                                    QueriesConstants.SORT_ORDER_DESC);
                            DocumentModelList majorVersionDocs = session.query(
                                    majorVersionDocsQuery);
                            if (majorVersionDocs != null
                                    && majorVersionDocs.size() > 0) {
                                releasedDocs.addAll(0, majorVersionDocs);
                            }
                        }
                    } else {
                        // If there is no released docs, get all versions
                        String allVersionsDocsQuery = EloraQueryFactory.getAllVersionsDocsQuery(
                                wcDocType, docWcUid, false,
                                QueriesConstants.SORT_ORDER_DESC);

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

    /**
     * This methods returns a descending ordered list of all versions of the
     * specified document. This list excludes the obsolete and DELETED versions.
     *
     * @param session              current session.
     * @param destinationItemWcUid Working Copy of the document which version
     *                             list will be calculated.
     * @param messages
     * @return
     * @throws EloraException
     */
    public static List<SelectItem> calculateDestinationItemSourceVersionList(
            CoreSession session, String destinationItemWcUid,
            Map<String, String> messages) throws EloraException {

        String logInitMsg = "[calculateDestinationItemSourceVersionList] ["
                + session.getPrincipal().getName() + "] ";

        List<SelectItem> versionList = new ArrayList<SelectItem>();
        try {
            DocumentRef wcRef = new IdRef(destinationItemWcUid);
            DocumentModel wcDoc = session.getDocument(wcRef);
            if (wcDoc != null) {

                String wcDocType = wcDoc.getType();

                // if the WC state is an OBSOLETE or DELETED state, ignore the
                // document and all its versions.
                if (!LifecyclesConfig.obsoleteAndDeletedStatesList.contains(
                        wcDoc.getCurrentLifeCycleState())) {

                    // Get all versions
                    String allVersionsDocsQuery = EloraQueryFactory.getAllVersionsDocsQuery(
                            wcDocType, destinationItemWcUid, false,
                            QueriesConstants.SORT_ORDER_DESC);

                    DocumentModelList allVersionDocs = session.query(
                            allVersionsDocsQuery);

                    fillVersionList(versionList, allVersionDocs, messages);
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }
        return versionList;
    }

    public static void fillVersionList(List<SelectItem> versionList,
            DocumentModelList docsList, Map<String, String> messages) {

        if (docsList != null && docsList.size() > 0) {

            for (DocumentModel doc : docsList) {
                String versionLabel = doc.getVersionLabel();
                versionList.add(new SelectItem(doc, versionLabel));
            }
        }
    }

    public static void setDestinationItemSourceVersionInformation(
            ImpactedItemsNodeData nodeData, Map<String, String> messages) {

        DocumentModel destinationItemSourceVersion = calculateDefaultDestinationItemSourceVersion(
                nodeData.getDestinationItemWc(), nodeData.getOriginItem(), nodeData.getAction());

        nodeData.setDestinationItemSourceVersion(destinationItemSourceVersion);

        List<SelectItem> destinationItemSourceVersionList = new ArrayList<SelectItem>();
        DocumentModelList destinationItemSourceVersionDocuments = new DocumentModelListImpl();
        destinationItemSourceVersionDocuments.add(destinationItemSourceVersion);
        CMHelper.fillVersionList(destinationItemSourceVersionList,
                destinationItemSourceVersionDocuments, messages);

        nodeData.setDestinationItemSourceVersionList(
                destinationItemSourceVersionList);
    }

    public static DocumentModel calculateDefaultDestinationItemSourceVersion(
            DocumentModel destinationItemWc, DocumentModel originItem, String action) {

        DocumentModel defaultDestinationItemSourceVersion = null;

        if (destinationItemWc != null && originItem != null) {
            // by default destination source version is the base version of
            // destination WC
            defaultDestinationItemSourceVersion = EloraDocumentHelper.getBaseVersion(
                    destinationItemWc);

            // if something else is defined by configuration, modify it
            String defaultDestinationSourceVersion = Framework.getProperty(
                    EloraPropertiesConstants.PROP_CM_DEFAULT_DESTINATION_SOURCE_VERSION);
            if (defaultDestinationSourceVersion != null
                    && defaultDestinationSourceVersion.equals(
                            CMConstants.SOURCE_VERSION_ORIGIN_VERSION)) {
                // If the action if replace, default destinatio item source should be
                // destination WC, since origin should not change
                if (!action.equals(CMConstants.ACTION_REPLACE)) {
                    defaultDestinationItemSourceVersion = originItem;
                }
            }
        }

        return defaultDestinationItemSourceVersion;
    }

    public static boolean getIsImpactable(String docType, String action,
            DocumentModel destinationItem) throws EloraException {

        if (action.equals(CMConstants.ACTION_REPLACE)
                && (destinationItem == null)) {
            return false;
        }
        return getIsImpactable(docType, action);
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

    public static IMOptions loadIMOptions(DocumentModel cmProcess) {
        String calculationMethod = CMConstants.IM_CALCULATION_METHOD_AUTOMATIC;
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IM_CALCULATION_METHOD) != null) {
            calculationMethod = (String) cmProcess.getPropertyValue(
                    CMMetadataConstants.IM_CALCULATION_METHOD);
        }

        // TODO: Solo para desarrollo porque con el automatico no funciona luego
        // el manual
        // calculationMethod = CMConstants.IM_CALCULATION_METHOD_MANUAL;

        // These two values are calculated while impact matrix calculation
        Boolean createItemIM = false;
        Boolean createDocIM = false;

        Boolean lockImpacted = true;
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IM_LOCK_IMPACTED) != null) {
            lockImpacted = (Boolean) cmProcess.getPropertyValue(
                    CMMetadataConstants.IM_LOCK_IMPACTED);
        }

        Boolean overwriteImpacted = false;
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IM_OVERWRITE_IMPACTED) != null) {
            overwriteImpacted = (Boolean) cmProcess.getPropertyValue(
                    CMMetadataConstants.IM_OVERWRITE_IMPACTED);
        }

        String overwriteOption = "";
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IM_OVERWRITE_OPTION) != null) {
            overwriteOption = (String) cmProcess.getPropertyValue(
                    CMMetadataConstants.IM_OVERWRITE_OPTION);
        }

        IMOptions IMOptions = new IMOptions(calculationMethod, createItemIM,
                createDocIM, lockImpacted, overwriteImpacted, overwriteOption);
        return IMOptions;
    }

    public static void setIMOptionsToDefault(DocumentModel cmProcess) {
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CALCULATION_METHOD,
                null);
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CREATE_ITEM_IMPACT,
                false);
        cmProcess.setPropertyValue(CMMetadataConstants.IM_CREATE_DOC_IMPACT,
                false);
        cmProcess.setPropertyValue(CMMetadataConstants.IM_LOCK_IMPACTED, true);
        cmProcess.setPropertyValue(CMMetadataConstants.IM_OVERWRITE_IMPACTED,
                false);
        cmProcess.setPropertyValue(CMMetadataConstants.IM_OVERWRITE_OPTION, "");
    }

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
            DocumentModel cmProcess, String nodeId) throws EloraException {

        String logInitMsg = "[setAsManagedModifiedItemByNodeId] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + "cmProcess = |" + cmProcess + "|, nodeId = |"
                + nodeId + "|");

        try {
            // Current Modified Items
            ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();

            String modifiedItemListMetada = CMMetadataConstants.MODIFIED_ITEM_LIST;

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

    /**
     * This methods returns a descending ordered list of versions of the
     * specified document.
     *
     * @param session
     * @param docWcUid
     * @param onlyReleasedVersion
     * @return
     * @throws EloraException
     */
    public static List<SelectItem> calculateOriginVersionList(
            CoreSession session, String docWcUid, boolean onlyReleasedVersion,
            Map<String, String> messages) throws EloraException {

        String logInitMsg = "[calculateOriginVersionList] ["
                + session.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        List<SelectItem> versionList = new ArrayList<SelectItem>();

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
                        QueriesConstants.SORT_ORDER_DESC);

                originVersionList = session.query(query);
            } else {

                List<DocumentModel> versions = session.getVersions(
                        new IdRef(docWcUid));
                if (versions != null && versions.size() > 0) {
                    for (int i = 0; i < versions.size(); i++) {
                        originVersionList.add(versions.get(i));
                    }
                }
                Collections.reverse(originVersionList);
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
     * Returns the first document containted in a version list. If the list is
     * null or empty, returns a null value.
     *
     * @param versionList
     * @return
     */
    public static DocumentModel getFirstDocumentFromVersionList(
            List<SelectItem> versionList) {

        DocumentModel firstDoc = null;

        if (versionList != null && !versionList.isEmpty()) {

            firstDoc = (DocumentModel) versionList.get(
                    0).getValue();
        }

        return firstDoc;
    }

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

    public static boolean getIncludeInImpactMatrixDefaultValue(String docType,
            String action, DocumentModel destinationItem) {

        boolean includeInImpactMatrixDefaultValue = false;

        if (action.equals(CMConstants.ACTION_REPLACE)
                && destinationItem == null) {
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

    public static boolean checkModifiedsWithIMAreManaged(
            DocumentModel cmProcess) {

        ArrayList<HashMap<String, Object>> currentModifiedItems = new ArrayList<HashMap<String, Object>>();
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.MODIFIED_ITEM_LIST) != null) {
            currentModifiedItems = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.MODIFIED_ITEM_LIST);
        }

        boolean calculateIM;
        boolean isManaged;
        for (int i = 0; i < currentModifiedItems.size(); ++i) {
            HashMap<String, Object> modifiedItem = currentModifiedItems.get(i);
            calculateIM = (boolean) modifiedItem.get("includeInImpactMatrix");
            if (calculateIM) {
                isManaged = (boolean) modifiedItem.get("isManaged");
                if (!isManaged) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean IMAlreadyExists(DocumentModel cmProcess) {

        ArrayList<HashMap<String, Object>> currentImpactMatrix = new ArrayList<HashMap<String, Object>>();
        if (cmProcess.getPropertyValue(
                CMMetadataConstants.IMPACTED_ITEM_LIST) != null) {
            currentImpactMatrix = (ArrayList<HashMap<String, Object>>) cmProcess.getPropertyValue(
                    CMMetadataConstants.IMPACTED_ITEM_LIST);
        }

        if (currentImpactMatrix.size() > 0) {
            return true;
        } else {
            return false;
        }
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

            String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

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

    public static long getManualItemsMaxRowNumberInCMProcess(
            CoreSession session, String cmProcessId) throws EloraException {

        String logInitMsg = "[getManualItemsMaxRowNumberInCMProcess] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        long maxRowNumber = 0;

        // Check specified input parameters and log them
        if (cmProcessId == null) {
            log.error(logInitMsg + "Specified cmProcess is null.");
            throw new EloraException("Specified cmProcess is null.");
        }

        log.trace(logInitMsg + " enter parameters: cmProcess = |" + cmProcessId
                + "|");

        IterableQueryResult queryResult = null;
        try {
            String query = CMQueryFactory.getModifiedsManualItemsMaxRowNumberQuery(
                    cmProcessId);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            String pfx = CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST;

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

    public static HashMap<String, Object> createModifiedItemType(
            ModifiedItem modifiedItem) throws EloraException {

        // Create Modified Item with common fields
        HashMap<String, Object> modifiedItemType = createCMItemType(
                modifiedItem);

        // --- isDerivedFromImpactMatrix
        modifiedItemType.put("isDerivedFromImpactMatrix",
                modifiedItem.getIsDerivedFromImpactMatrix());

        // --- includeInImpactMatrix
        modifiedItemType.put("includeInImpactMatrix",
                modifiedItem.getIncludeInImpactMatrix());

        return modifiedItemType;
    }

    public static HashMap<String, Object> createManualItemType(
            ManualItem manualItem) throws EloraException {

        if (manualItem.getRowNumber() == null) {
            log.error("[createManualItemType] rowNumber is null.");
            throw new EloraException("rowNumber is null.");
        }

        HashMap<String, Object> manualItemType = new HashMap<>();

        manualItemType.put("rowNumber", manualItem.getRowNumber());

        manualItemType.put("nodeId", manualItem.getNodeId());

        manualItemType.put("modifiedItem", manualItem.getModifiedItem());

        manualItemType.put("originItem", manualItem.getOriginItem());

        manualItemType.put("action", manualItem.getAction());

        manualItemType.put("type", manualItem.getType());

        manualItemType.put("comment", manualItem.getComment());

        return manualItemType;
    }

    public static HashMap<String, Object> createCMItemType(CMItem cmItem)
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

        // --- derivedFrom
        cmItemType.put("derivedFrom", cmItem.getDerivedFrom());

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

        // --- destinationItemSourceVersion
        cmItemType.put("destinationItemSourceVersion",
                cmItem.getDestinationItemSourceVersion());

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

    public static ArrayList<String> getManualImpactRelations(String type) {

        if (type.equals(CMConstants.ITEM_TYPE_BOM)) {
            return getBomDirectAnarchicAndHierarchicalRelations();
        } else {
            return getCadDirectAndHierarchicalRelations();
        }
    }

    public static ArrayList<String> getBomDirectAnarchicAndHierarchicalRelations() {
        ArrayList<String> bomDirectAnarchicAndHierarchicalRelations = new ArrayList<String>();
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomDirectRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomAnarchicRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.addAll(
                RelationsConfig.bomHierarchicalRelationsList);
        bomDirectAnarchicAndHierarchicalRelations.add(
                EloraRelationConstants.BOM_HAS_DOCUMENT);
        return bomDirectAnarchicAndHierarchicalRelations;
    }

    public static ArrayList<String> getCadDirectAndHierarchicalRelations() {
        ArrayList<String> cadDirectAndHierarchicalRelations = new ArrayList<String>();

        cadDirectAndHierarchicalRelations.addAll(
                RelationsConfig.cadHierarchicalRelationsList);
        cadDirectAndHierarchicalRelations.addAll(
                RelationsConfig.cadDirectRelationsList);
        cadDirectAndHierarchicalRelations.addAll(
                RelationsConfig.cadSpecialRelationsList);
        cadDirectAndHierarchicalRelations.addAll(
                RelationsConfig.docDirectRelationsList);

        return cadDirectAndHierarchicalRelations;
    }

}
