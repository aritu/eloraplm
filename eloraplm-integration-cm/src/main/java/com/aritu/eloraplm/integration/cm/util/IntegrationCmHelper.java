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

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.cm.CmProcessInfo;
import com.aritu.eloraplm.integration.restoperations.util.CmProcessNode;

/**
 * @author aritu
 *
 */
public class IntegrationCmHelper {

    private static final Log log = LogFactory.getLog(IntegrationCmHelper.class);

    public static CmProcessInfo getCmEcoProcessInfo(DocumentModel doc)
            throws EloraException {

        String logInitMsg = "[getCmEcoProcessInfo] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        List<CmProcessNode> structure = new ArrayList<CmProcessNode>();
        List<DocumentModel> rootItemDocuments = new ArrayList<DocumentModel>();
        List<DocumentModel> subitemDocuments = new ArrayList<DocumentModel>();

        fillCmEcoRootItems(doc, structure, rootItemDocuments);

        fillCmEcoSubitems(doc, structure, subitemDocuments);

        CmProcessInfo processInfo = new CmProcessInfo();
        processInfo.setStructure(structure);

        processInfo.setRootItemDocuments(rootItemDocuments);

        processInfo.setSubitemDocuments(subitemDocuments);

        log.trace(logInitMsg + "--- EXIT ---");

        return processInfo;
    }

    // *********************************************************
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

    private static void fillCmEcoRootItems(DocumentModel doc,
            List<CmProcessNode> structure,
            List<DocumentModel> rootItemDocuments) throws EloraException {

        String logInitMsg = "[fillCmEcoRootItems] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        IterableQueryResult it = null;
        try {

            CoreSession session = doc.getCoreSession();

            String query = IntegrationCmQueryFactory.getCmEcoRootItemsQuery(
                    doc.getId());
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {
                List<String> rootItemsDocumentsIds = new ArrayList<String>();

                String pfx = CMMetadataConstants.DOC_MODIFIED_ITEM_LIST;
                for (Map<String, Serializable> map : it) {
                    String nodeId = (String) map.get(pfx + "/*1/nodeId");
                    String parentNodeId = (String) map.get(
                            pfx + "/*1/parentNodeId");
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");
                    String action = (String) map.get(pfx + "/*1/action");
                    String destinationItemUid = (String) map.get(
                            pfx + "/*1/destinationItem");
                    String destinationWcItemUid = (String) map.get(
                            pfx + "/*1/destinationItemWc");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    String comment = (String) map.get(pfx + "/*1/comment");

                    String destinationRealUid = null;
                    String destinationWcUid = null;
                    if (destinationItemUid != null) {
                        if (destinationItemUid.equals(destinationWcItemUid)) {
                            destinationWcUid = destinationItemUid;
                        } else {
                            destinationRealUid = destinationItemUid;
                        }
                    }

                    CmProcessNode cmProcessNode = new CmProcessNode(nodeId,
                            parentNodeId, originItemUid, true, null,
                            originItemUid, destinationRealUid, destinationWcUid,
                            action, isManaged, comment);
                    structure.add(cmProcessNode);

                    try {
                        if (action.equals(CMConstants.ACTION_REMOVE)
                                || action.equals(CMConstants.ACTION_REPLACE)) {
                            if (originItemUid != null
                                    && !rootItemsDocumentsIds.contains(
                                            originItemUid)) {
                                DocumentModel originItem = session.getDocument(
                                        new IdRef(originItemUid));
                                rootItemsDocumentsIds.add(originItemUid);
                                rootItemDocuments.add(originItem);
                            }
                        }
                        if (action.equals(CMConstants.ACTION_CHANGE)
                                || action.equals(CMConstants.ACTION_REPLACE)) {

                            if (destinationItemUid != null
                                    && !rootItemsDocumentsIds.contains(
                                            destinationItemUid)) {
                                DocumentModel destinationItem = session.getDocument(
                                        new IdRef(destinationItemUid));
                                rootItemsDocumentsIds.add(destinationItemUid);
                                rootItemDocuments.add(destinationItem);
                            }
                        }
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
    }

    private static void fillCmEcoSubitems(DocumentModel doc,
            List<CmProcessNode> structure, List<DocumentModel> subitemDocuments)
            throws EloraException {

        String logInitMsg = "[fillCmEcoSubitems] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        IterableQueryResult it = null;
        try {

            CoreSession session = doc.getCoreSession();

            String query = IntegrationCmQueryFactory.getCmEcoSubitemsQuery(
                    doc.getId());
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {
                List<String> subitemsDocumentsIds = new ArrayList<String>();

                String pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
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
                    String action = (String) map.get(pfx + "/*1/action");
                    String destinationItemUid = (String) map.get(
                            pfx + "/*1/destinationItem");
                    String destinationWcItemUid = (String) map.get(
                            pfx + "/*1/destinationItemWc");
                    boolean isManaged = (boolean) map.get(
                            pfx + "/*1/isManaged");
                    String comment = (String) map.get(pfx + "/*1/comment");

                    String destinationRealUid = null;
                    String destinationWcUid = null;
                    if (destinationItemUid.equals(destinationWcItemUid)) {
                        destinationWcUid = destinationItemUid;
                    } else {
                        destinationRealUid = destinationItemUid;
                    }

                    CmProcessNode cmProcessNode = new CmProcessNode(nodeId,
                            parentNodeId, modifiedItemUid, false, parentItemUid,
                            originItemUid, destinationRealUid, destinationWcUid,
                            action, isManaged, comment);
                    structure.add(cmProcessNode);

                    try {
                        if (action.equals(CMConstants.ACTION_CHANGE)) {

                            if (destinationItemUid != null
                                    && !subitemsDocumentsIds.contains(
                                            destinationItemUid)) {
                                DocumentModel destinationItem = session.getDocument(
                                        new IdRef(destinationItemUid));
                                subitemsDocumentsIds.add(destinationItemUid);
                                subitemDocuments.add(destinationItem);
                            }
                        }
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
