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
package com.aritu.eloraplm.integration.cm.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.cm.util.CMQueryResultFactory;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.cm.CmProcessInfo;
import com.aritu.eloraplm.integration.restoperations.util.CmProcessNode;

/**
 * @author aritu
 *
 */
public class IntegrationCmHelper {

    private static final Log log = LogFactory.getLog(IntegrationCmHelper.class);

    /**
     * Gets the information related to a given ECO. The information is completed
     * by the structure of the ECO, the modified documents list and the impacted
     * documents list.
     *
     * @param doc The ECO document.
     * @param includeDocuments If false, it only retrieves the structure of the
     *            ECO. If true, it retrieves the structure, the
     *            rootItemDocuments list and the subitemDocuments list.
     * @return Information of the ECO
     * @throws EloraException
     */
    public static CmProcessInfo getCmEcoProcessInfo(DocumentModel doc,
            boolean includeDocuments) throws EloraException {

        String logInitMsg = "[getCmEcoProcessInfo] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        List<CmProcessNode> structure = new ArrayList<CmProcessNode>();
        List<DocumentModel> rootItemDocuments = new ArrayList<DocumentModel>();
        List<DocumentModel> subitemDocuments = new ArrayList<DocumentModel>();

        List<String> rootItemsOriginItemUids = fillCmEcoRootItems(doc,
                structure, rootItemDocuments, includeDocuments);

        fillCmEcoSubitems(doc, rootItemsOriginItemUids, structure,
                subitemDocuments, includeDocuments);

        CmProcessInfo processInfo = new CmProcessInfo();
        processInfo.setStructure(structure);

        processInfo.setRootItemDocuments(rootItemDocuments);

        processInfo.setSubitemDocuments(subitemDocuments);

        log.trace(logInitMsg + "--- EXIT ---");

        return processInfo;
    }

    /**
     * @param doc The ECO document.
     * @param managedDestinationUids This Map contains managed destination
     *            items. Each key element contains the destinationUid value
     *            before performing the action and its related value is the new
     *            destinationUid, after performing the action.
     * @throws EloraException
     */
    @SuppressWarnings("unchecked")
    public static void setCmEcoItemsAsManaged(DocumentModel doc,
            Map<String, String> managedDestinationUids) throws EloraException {

        String logInitMsg = "[setCmEcoItemsAsManaged] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        try {

            ArrayList<HashMap<String, Object>> rootItems = new ArrayList<HashMap<String, Object>>();
            if (doc.getPropertyValue(
                    CMMetadataConstants.DOC_MODIFIED_ITEM_LIST) != null) {
                rootItems = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                        CMMetadataConstants.DOC_MODIFIED_ITEM_LIST);
            }

            ArrayList<HashMap<String, Object>> subitems = new ArrayList<HashMap<String, Object>>();
            if (doc.getPropertyValue(
                    CMMetadataConstants.DOC_IMPACTED_ITEM_LIST) != null) {
                subitems = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                        CMMetadataConstants.DOC_IMPACTED_ITEM_LIST);
            }

            boolean rootItemsChanged = setCmEcoItemsAsManaged(rootItems,
                    managedDestinationUids);

            boolean subitemsChanged = setCmEcoItemsAsManaged(subitems,
                    managedDestinationUids);

            // if something has changed, store updated items
            if (rootItemsChanged || subitemsChanged) {
                if (rootItemsChanged) {
                    doc.setPropertyValue(
                            CMMetadataConstants.DOC_MODIFIED_ITEM_LIST,
                            rootItems);
                }
                if (subitemsChanged) {
                    doc.setPropertyValue(
                            CMMetadataConstants.DOC_IMPACTED_ITEM_LIST,
                            subitems);
                }
                CoreSession session = doc.getCoreSession();
                session.saveDocument(doc);
                session.save();
            }

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static List<String> fillCmEcoRootItems(DocumentModel doc,
            List<CmProcessNode> structure,
            List<DocumentModel> rootItemDocuments, boolean includeDocuments)
            throws EloraException {

        String logInitMsg = "[fillCmEcoRootItems] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        List<String> rootItemsOriginItemUids = new ArrayList<String>();

        IterableQueryResult it = null;
        IterableQueryResult itID = null;
        try {
            CoreSession session = doc.getCoreSession();
            List<String> rootItemsDocumentsIds = new ArrayList<String>();

            // First, fill structure and rootItems regarding MODIFIED DOCUMENTS
            String itemType = CMConstants.ITEM_TYPE_DOC;
            String query = IntegrationCmQueryFactory.getCmEcoRootItemsQuery(
                    doc.getId(), itemType);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {
                String pfx = CMHelper.getModifiedItemListMetadaName(itemType);

                for (Map<String, Serializable> map : it) {
                    String nodeId = (String) map.get(pfx + "/*1/nodeId");
                    String parentNodeId = (String) map.get(
                            pfx + "/*1/parentNodeId");
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");
                    String originItemWcUid = (String) map.get(
                            pfx + "/*1/originItemWc");
                    String action = (String) map.get(pfx + "/*1/action");
                    String destinationItemUid = (String) map.get(
                            pfx + "/*1/destinationItem");
                    String destinationWcItemUid = (String) map.get(
                            pfx + "/*1/destinationItemWc");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    String comment = (String) map.get(pfx + "/*1/comment");

                    // The code below is not needed, since IGNORED items are not
                    // returned in the query
                    /*
                    // If action is IGNORE, set destinationWc as originWc
                    if (action.equals(CMConstants.ACTION_IGNORE)) {
                        destinationWcItemUid = originItemWcUid;
                    }*/


                    DocumentModel originItem = session.getDocument(
                            new IdRef(originItemUid));

                    // Add only CAD documents in structure
                    if (originItem.hasFacet(
                            EloraFacetConstants.FACET_CAD_DOCUMENT)) {
                        CmProcessNode cmProcessNode = new CmProcessNode(nodeId,
                                parentNodeId, originItemUid, true, null,
                                originItemUid, originItemWcUid,
                                destinationItemUid, destinationWcItemUid,
                                action, isManaged, comment);
                        structure.add(cmProcessNode);
                        rootItemsOriginItemUids.add(originItemUid);
                    }

                    if (includeDocuments) {
                        try {
                            if (action.equals(CMConstants.ACTION_REMOVE)
                                    || action.equals(
                                            CMConstants.ACTION_REPLACE)) {
                                if (originItemUid != null
                                        && !rootItemsDocumentsIds.contains(
                                                originItemUid)) {
                                    rootItemsDocumentsIds.add(originItemUid);
                                    rootItemDocuments.add(originItem);
                                }
                            }
                            if (action.equals(CMConstants.ACTION_CHANGE)
                                    || action.equals(
                                            CMConstants.ACTION_REPLACE)) {
                                if (destinationItemUid != null
                                        && !rootItemsDocumentsIds.contains(
                                                destinationItemUid)) {
                                    DocumentModel destinationItem = session.getDocument(
                                            new IdRef(destinationItemUid));
                                    rootItemsDocumentsIds.add(
                                            destinationItemUid);
                                    rootItemDocuments.add(destinationItem);
                                }
                            }
                        } catch (DocumentNotFoundException e) {
                            log.error(logInitMsg + "Exception thrown: "
                                    + e.getClass() + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Now include DOCUMENTS that are related to the MODIFIED ITEMS that
            // are not already included in rootItems list and are not included
            // as IMPACTED DOCUMENTS, without changing the structure.
            if (includeDocuments) {
                itemType = CMConstants.ITEM_TYPE_BOM;
                query = IntegrationCmQueryFactory.getCmEcoRootItemsQuery(
                        doc.getId(), itemType);

                itID = session.queryAndFetch(query, NXQL.NXQL);
                if (itID.size() > 0) {
                    // Retrieve the list of distinct IMPACTED DOCUMENTS
                    List<String> distinctImpactedDocumentsOriginUids = CMQueryResultFactory.getDistinctImpactedItemsOriginsQuery(
                            session, doc.getId(), CMConstants.ITEM_TYPE_DOC);
                    String pfx = CMHelper.getModifiedItemListMetadaName(
                            itemType);

                    for (Map<String, Serializable> map : itID) {

                        String originItemUid = (String) map.get(
                                pfx + "/*1/originItem");
                        String action = (String) map.get(pfx + "/*1/action");
                        DocumentModel originItem = session.getDocument(
                                new IdRef(originItemUid));

                        // Retrieve originItem related CAD documents
                        DocumentModelList relatedCads = EloraRelationHelper.getAllRelatedCadDocsForItem(
                                originItem);
                        for (DocumentModel relatedCad : relatedCads) {
                            String relatedCadUid = relatedCad.getId();

                            if (!distinctImpactedDocumentsOriginUids.contains(
                                    relatedCadUid)) {
                                try {
                                    if (action.equals(CMConstants.ACTION_REMOVE)
                                            || action.equals(
                                                    CMConstants.ACTION_REPLACE)) {
                                        if (!rootItemsDocumentsIds.contains(
                                                relatedCadUid)) {
                                            rootItemsDocumentsIds.add(
                                                    relatedCadUid);
                                            rootItemDocuments.add(relatedCad);
                                        }
                                    }
                                    if (action.equals(CMConstants.ACTION_CHANGE)
                                            || action.equals(
                                                    CMConstants.ACTION_REPLACE)) {

                                        DocumentModel relatedCadWc = session.getWorkingCopy(
                                                relatedCad.getRef());
                                        String relatedCadWcUid = relatedCadWc.getId();

                                        if (!rootItemsDocumentsIds.contains(
                                                relatedCadWcUid)) {
                                            rootItemsDocumentsIds.add(
                                                    relatedCadWcUid);
                                            rootItemDocuments.add(relatedCadWc);
                                        }
                                    }
                                } catch (DocumentNotFoundException e) {
                                    log.error(logInitMsg + "Exception thrown: "
                                            + e.getClass() + ": "
                                            + e.getMessage());
                                }
                            }
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
            it.close();
            if (itID != null && itID.mustBeClosed()) {
                itID.close();
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return rootItemsOriginItemUids;
    }

    private static void fillCmEcoSubitems(DocumentModel doc,
            List<String> rootItemsOriginItemUids, List<CmProcessNode> structure,
            List<DocumentModel> subitemDocuments, boolean includeDocuments)
            throws EloraException {

        // ################################################################
        // Changes related to JIRA ELO-806:
        //
        // - Fill structure with all impacted items (also items with IGNORE
        // action)
        // - For impacted items with IGNORE action, add the originWc to the
        // documents list
        //
        // ################################################################

        String logInitMsg = "[fillCmEcoSubitems] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        IterableQueryResult it = null;
        IterableQueryResult itID = null;
        try {
            CoreSession session = doc.getCoreSession();
            List<String> subitemsDocumentsIds = new ArrayList<String>();

            // structure should contain modified items structure (root items)
            for (Iterator<String> iterator = rootItemsOriginItemUids.iterator(); iterator.hasNext();) {
                String rootItemOriginUid = iterator.next();

                String itemType = CMConstants.ITEM_TYPE_DOC;
                String query = IntegrationCmQueryFactory.getCmEcoSubitemsByRootItemOriginUidQuery(
                        doc.getId(), rootItemOriginUid, itemType);
                it = session.queryAndFetch(query, NXQL.NXQL);

                if (it.size() > 0) {
                    String pfx = CMHelper.getImpactedItemListMetadaName(
                            itemType);
                    for (Map<String, Serializable> map : it) {
                        String nodeId = (String) map.get(pfx + "/*1/nodeId");
                        String parentNodeId = (String) map.get(
                                pfx + "/*1/parentNodeId");
                        String modifiedItemUid = (String) map.get(
                                pfx + "/*1/modifiedItem");
                        String parentItemUid = (String) map.get(
                                pfx + "/*1/parentItem");
                        String originItemUid = (String) map.get(
                                pfx + "/*1/originItem");
                        String originWcItemUid = (String) map.get(
                                pfx + "/*1/originItemWc");
                        String action = (String) map.get(pfx + "/*1/action");
                        String destinationItemUid = (String) map.get(
                                pfx + "/*1/destinationItem");
                        String destinationWcItemUid = (String) map.get(
                                pfx + "/*1/destinationItemWc");
                        boolean isManaged = (boolean) map.get(
                                pfx + "/*1/isManaged");
                        String comment = (String) map.get(pfx + "/*1/comment");

                        boolean includeInStructure = false;
                        if (action.equals(CMConstants.ACTION_CHANGE)) {
                            includeInStructure = true;
                        } else {
                            // if action is ignore, check if it has at least one
                            // change in its children nodes
                            includeInStructure = checkIfIgnoreMustBeIncluded(
                                    doc.getId(), nodeId, session);
                        }

                        if (includeInStructure) {
                            // If action is IGNORE, set destinationWc as
                            // originWc
                            if (action.equals(CMConstants.ACTION_IGNORE)) {
                                destinationWcItemUid = originWcItemUid;
                            }

                            CmProcessNode cmProcessNode = new CmProcessNode(
                                    nodeId, parentNodeId, modifiedItemUid,
                                    false, parentItemUid, originItemUid,
                                    originWcItemUid, destinationItemUid,
                                    destinationWcItemUid, action, isManaged,
                                    comment);
                            structure.add(cmProcessNode);

                            if (includeDocuments) {
                                try {
                                    if (action.equals(
                                            CMConstants.ACTION_CHANGE)) {
                                        if (destinationItemUid != null
                                                && !subitemsDocumentsIds.contains(
                                                        destinationItemUid)) {
                                            DocumentModel destinationItem = session.getDocument(
                                                    new IdRef(
                                                            destinationItemUid));
                                            subitemsDocumentsIds.add(
                                                    destinationItemUid);
                                            subitemDocuments.add(
                                                    destinationItem);
                                        }
                                    } else if (action.equals(
                                            CMConstants.ACTION_IGNORE)) {
                                        if (originWcItemUid != null
                                                && !subitemsDocumentsIds.contains(
                                                        originWcItemUid)) {
                                            DocumentModel originWcItem = session.getDocument(
                                                    new IdRef(originWcItemUid));
                                            subitemsDocumentsIds.add(
                                                    originWcItemUid);
                                            subitemDocuments.add(originWcItem);
                                        }
                                    }
                                } catch (DocumentNotFoundException e) {
                                    log.error(logInitMsg + "Exception thrown: "
                                            + e.getClass() + ": "
                                            + e.getMessage());
                                }
                            }
                        }
                    }
                }
                it.close();
            }

            if (includeDocuments) {
                // Now include DOCUMENTS that are related to the IMPACTED ITEMS
                // that are not already included in subItems without changing
                // the structure.
                String itemType = CMConstants.ITEM_TYPE_BOM;
                String query = IntegrationCmQueryFactory.getCmEcoSubitemsQuery(
                        doc.getId(), itemType);

                itID = session.queryAndFetch(query, NXQL.NXQL);
                if (itID.size() > 0) {
                    String pfx = CMHelper.getImpactedItemListMetadaName(
                            itemType);

                    for (Map<String, Serializable> map : itID) {
                        String originItemUid = (String) map.get(
                                pfx + "/*1/originItem");
                        String action = (String) map.get(pfx + "/*1/action");
                        DocumentModel originItem = session.getDocument(
                                new IdRef(originItemUid));

                        // Retrieve originItem related CAD documents
                        DocumentModelList relatedCads = EloraRelationHelper.getAllRelatedCadDocsForItem(
                                originItem);
                        for (DocumentModel relatedCad : relatedCads) {
                            try {
                                // In this case IGNORE impacted elements are not
                                // taking into account. Only CHANGE ones.
                                if (action.equals(CMConstants.ACTION_CHANGE)) {
                                    DocumentModel relatedCadWc = session.getWorkingCopy(
                                            relatedCad.getRef());
                                    String relatedCadWcUid = relatedCadWc.getId();

                                    if (!subitemsDocumentsIds.contains(
                                            relatedCadWcUid)) {
                                        subitemsDocumentsIds.add(
                                                relatedCadWcUid);
                                        subitemDocuments.add(relatedCadWc);
                                    }
                                }
                            } catch (DocumentNotFoundException e) {
                                log.error(logInitMsg + "Exception thrown: "
                                        + e.getClass() + ": " + e.getMessage());
                            }
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
            if (it != null && it.mustBeClosed()) {
                it.close();
            }
            if (itID != null && itID.mustBeClosed()) {
                itID.close();
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private static boolean checkIfIgnoreMustBeIncluded(String cmProcessUid,
            String nodeId, CoreSession session) {

        boolean mustBeIncluded = false;

        String query = IntegrationCmQueryFactory.getCmEcoSubitemsByParentNodeIdQuery(
                cmProcessUid, nodeId);

        IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);

        if (it.size() > 0) {
            List<String> childNodeIds = new ArrayList<String>();

            String pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
            for (Map<String, Serializable> map : it) {
                String childNodeId = (String) map.get(pfx + "/*1/nodeId");
                String childAction = (String) map.get(pfx + "/*1/action");

                if (childAction.equals(CMConstants.ACTION_CHANGE)) {
                    mustBeIncluded = true;
                    break;
                }

                childNodeIds.add(childNodeId);
            }

            // If direct children are not change, verify recursively
            if (!mustBeIncluded) {
                for (String childNodeId : childNodeIds) {
                    mustBeIncluded = checkIfIgnoreMustBeIncluded(cmProcessUid,
                            childNodeId, session);
                    if (mustBeIncluded) {
                        break;
                    }
                }
            }
        }
        it.close();

        return mustBeIncluded;
    }

    private static boolean setCmEcoItemsAsManaged(
            List<HashMap<String, Object>> items,
            Map<String, String> managedDestinationUids) {

        boolean itemsChanged = false;

        for (int i = 0; i < items.size(); ++i) {
            HashMap<String, Object> itemContent = items.get(i);
            Boolean isManaged = (Boolean) itemContent.get("isManaged");

            if (!isManaged) {
                String currentDestinationItemUid = (String) itemContent.get(
                        "destinationItem");
                if (managedDestinationUids.containsKey(
                        currentDestinationItemUid)) {
                    String newDestinationItemUid = managedDestinationUids.get(
                            currentDestinationItemUid);
                    itemContent.put("destinationItem", newDestinationItemUid);
                    itemContent.put("isManaged", true);
                    itemContent.put("isUpdated", true);
                    itemsChanged = true;
                }
            }
        }
        return itemsChanged;
    }
}
