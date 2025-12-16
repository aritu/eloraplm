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
package com.aritu.eloraplm.bom.linked.characteristics.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsQueryFactory;
import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristicInfo;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsEventNames;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Linked Characteristics.
 *
 * @author aritu
 *
 */
public class LinkedCharacteristicsHelper {

    private static final Log log = LogFactory.getLog(
            LinkedCharacteristicsHelper.class);

    public static LinkedCharacteristicInfo createLinkedCharacteristicInfo(
            HashMap<String, Object> linkedDocumentBomCharacteristicType) {

        // --- bomCharacteristicId
        String bomCharacteristicId = (String) linkedDocumentBomCharacteristicType.get(
                BomCharacteristicsConstants.BOM_CHARACTERISTIC_ID);

        // --- title
        String title = (String) linkedDocumentBomCharacteristicType.get(
                BomCharacteristicsConstants.TITLE);

        // --- type
        String type = (String) linkedDocumentBomCharacteristicType.get(
                BomCharacteristicsConstants.TYPE);

        // --- values
        String numberValue = null;
        String stringValue = null;
        Date dateValue = null;
        Boolean booleanValue = null;
        String listValue = null;
        switch (type) {
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
            numberValue = (String) linkedDocumentBomCharacteristicType.get(
                    BomCharacteristicsConstants.NUMBER_VALUE);
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
            stringValue = (String) linkedDocumentBomCharacteristicType.get(
                    BomCharacteristicsConstants.STRING_VALUE);
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
            GregorianCalendar dateValueeGc = (GregorianCalendar) linkedDocumentBomCharacteristicType.get(
                    BomCharacteristicsConstants.DATE_VALUE);
            if (dateValueeGc != null) {
                // TODO:: uste dot kasu honetan eza dala beharrezkoa
                /*
                 * // Taken from Nuxeo code: //
                 * ------------------------------------------------- // remove
                 * milliseconds as they are not stored in some // databases,
                 * which could make the comparison fail just // after a document
                 * creation (see NXP-8783) //
                 * -------------------------------------------------
                 * dateValueeGc.set(Calendar.MILLISECOND, 0);
                 */
                dateValue = dateValueeGc.getTime();
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
            booleanValue = (Boolean) linkedDocumentBomCharacteristicType.get(
                    BomCharacteristicsConstants.BOOLEAN_VALUE);
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
            listValue = (String) linkedDocumentBomCharacteristicType.get(
                    BomCharacteristicsConstants.LIST_VALUE);
            break;
        }

        // --- unit
        String unit = (String) linkedDocumentBomCharacteristicType.get(
                BomCharacteristicsConstants.UNIT);

        // --- orderInReport
        Long orderInReport = (Long) linkedDocumentBomCharacteristicType.get(
                BomCharacteristicsConstants.ORDER_IN_REPORT);

        if (orderInReport == null) {
            orderInReport = 0L;
        }

        LinkedCharacteristicInfo linkedCharacteristicInfo = new LinkedCharacteristicInfo(
                bomCharacteristicId, title, type, numberValue, stringValue,
                dateValue, booleanValue, listValue, unit, orderInReport);

        return linkedCharacteristicInfo;
    }

    public static LinkedCharacteristicInfo retrieveLinkedCharacteristicInfo(
            CoreSession session, String linkedDocumentId,
            String linkedCharacteristicId) throws EloraException {

        String logInitMsg = "[retrieveLinkedCharacteristicInfo] ["
                + session.getPrincipal().getName() + "] ";

        LinkedCharacteristicInfo linkedCharacteristicInfo = null;

        IterableQueryResult it = null;
        try {
            String query = BomCharacteristicsQueryFactory.getBomCharacteristicInfoQuery(
                    linkedDocumentId, linkedCharacteristicId);
            it = session.queryAndFetch(query, NXQL.NXQL);

            // TODO: if it.size > 0 return null
            if (it.size() > 0) {

                if (it.size() > 1) {
                    log.error(logInitMsg + "There are |" + it.size()
                            + "| characteristics for linkedCharacteristicId = |"
                            + linkedCharacteristicId
                            + "|. There must be only one.");
                    throw new EloraException(
                            "More than one characteristic found for same linkedCharacteristicId ("
                                    + linkedCharacteristicId + ")");
                } else {
                    String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

                    for (Map<String, Serializable> map : it) {
                        String title = (String) map.get(pfx + "/*1/"
                                + BomCharacteristicsConstants.TITLE);
                        String type = (String) map.get(pfx + "/*1/"
                                + BomCharacteristicsConstants.TYPE);
                        String numberValue = null;
                        String stringValue = null;
                        Date dateValue = null;
                        Boolean booleanValue = null;
                        String listValue = null;

                        switch (type) {
                        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
                            numberValue = (String) map.get(pfx + "/*1/"
                                    + BomCharacteristicsConstants.NUMBER_VALUE);
                            break;

                        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
                            stringValue = (String) map.get(pfx + "/*1/"
                                    + BomCharacteristicsConstants.STRING_VALUE);
                            break;

                        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
                            GregorianCalendar dateValueCalendar = (GregorianCalendar) map.get(
                                    pfx + "/*1/"
                                            + BomCharacteristicsConstants.DATE_VALUE);
                            if (dateValueCalendar != null) {
                                dateValue = dateValueCalendar.getTime();
                            }
                            break;

                        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
                            booleanValue = (Boolean) map.get(pfx + "/*1/"
                                    + BomCharacteristicsConstants.BOOLEAN_VALUE);
                            break;

                        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
                            listValue = (String) map.get(pfx + "/*1/"
                                    + BomCharacteristicsConstants.LIST_VALUE);
                            break;
                        }

                        String unit = (String) map.get(pfx + "/*1/"
                                + BomCharacteristicsConstants.UNIT);

                        Long orderInReport = (Long) map.get(pfx + "/*1/"
                                + BomCharacteristicsConstants.ORDER_IN_REPORT);

                        linkedCharacteristicInfo = new LinkedCharacteristicInfo(
                                linkedCharacteristicId, title, type,
                                numberValue, stringValue, dateValue,
                                booleanValue, listValue, unit, orderInReport);
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

        return linkedCharacteristicInfo;
    }

    /**
     * This methods checks if linked characteristics should be updated since
     * linked documents have changed or not. If they should be updated, the
     * method updates them.
     *
     * @param session
     * @param doc
     * @param firedBy the process that has fired the update
     * @return a boolean indicating if linked characteristics have been changed
     *         or not.
     */
    @SuppressWarnings("unchecked")
    public static boolean updateLinkedCharacteristicsIfRequired(
            CoreSession session, DocumentModel doc, String firedBy) {

        String logInitMsg = "[updateLinkedCharacteristicsIfRequired] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean linkedCharacteristicsUpdated = false;

        // linkedDocumentNewVersionId, linkedDocumentNewVersionBomCharacsIds
        Map<String, Set<String>> linkedDocsNewVersionsCharacteristics = new HashMap<String, Set<String>>();

        if (doc.isProxy()) {
            doc = session.getWorkingCopy(doc.getRef());
        }

        ArrayList<HashMap<String, Object>> linkedCharacteristics = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> updatedLinkedCharacteristics = new ArrayList<HashMap<String, Object>>();

        if (doc.getPropertyValue(
                BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST) != null) {
            linkedCharacteristics = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                    BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST);
        }
        log.trace(logInitMsg + "|" + linkedCharacteristics.size()
                + "| linked characteristics found.");

        if (linkedCharacteristics.size() > 0) {

            // versionableId, docId
            Map<String, String> linkableDocumentsVersion = retrieveLinkableDocumentsVersions(
                    doc);

            for (int i = 0; i < linkedCharacteristics.size(); i++) {
                log.trace(logInitMsg + "Processing linkedCharac number |" + i
                        + "|");

                HashMap<String, Object> linkedCharac = linkedCharacteristics.get(
                        i);
                String linkedDocumentUid = (String) linkedCharac.get(
                        BomCharacteristicsConstants.LINKED_DOCUMENT);
                DocumentModel linkedDocument = null;
                if (linkedDocumentUid != null) {
                    try {
                        linkedDocument = session.getDocument(
                                new IdRef(linkedDocumentUid));

                        String linkedDocumentVersionableId = linkedDocument.getVersionSeriesId();

                        if (linkableDocumentsVersion.containsKey(
                                linkedDocumentVersionableId)) {

                            String linkedDocumentNewVersion = linkableDocumentsVersion.get(
                                    linkedDocumentVersionableId);

                            if (!linkedDocumentNewVersion.equals(
                                    linkedDocument.getId())) {
                                String linkedDocumentCharacteristicId = (String) linkedCharac.get(
                                        BomCharacteristicsConstants.LINKED_DOCUMENT_CHARAC_ID);

                                if (linkedDocumentCharacteristicId != null) {

                                    Set<String> linkedDocumentNewVersionCharacsIds = new HashSet<String>();
                                    if (linkedDocsNewVersionsCharacteristics.containsKey(
                                            linkedDocumentNewVersion)) {
                                        linkedDocumentNewVersionCharacsIds = linkedDocsNewVersionsCharacteristics.get(
                                                linkedDocumentNewVersion);
                                    } else {
                                        linkedDocumentNewVersionCharacsIds = retrieveCharacteristicIs(
                                                session,
                                                linkedDocumentNewVersion);
                                        linkedDocsNewVersionsCharacteristics.put(
                                                linkedDocumentNewVersion,
                                                linkedDocumentNewVersionCharacsIds);
                                    }

                                    if (linkedDocumentNewVersionCharacsIds.contains(
                                            linkedDocumentCharacteristicId)) {
                                        linkedCharac.put(
                                                BomCharacteristicsConstants.LINKED_DOCUMENT,
                                                linkedDocumentNewVersion);
                                        updatedLinkedCharacteristics.add(
                                                linkedCharac);
                                        linkedCharacteristicsUpdated = true;
                                    } else {
                                        linkedCharacteristicsUpdated = true;
                                    }
                                }
                            } else {
                                updatedLinkedCharacteristics.add(linkedCharac);
                            }
                        } else {
                            linkedCharacteristicsUpdated = true;
                        }
                    } catch (DocumentNotFoundException e) {
                        log.error(logInitMsg
                                + "Error getting linked document with uid = |"
                                + linkedDocumentUid + "|: " + e.getMessage());
                    }
                }
            }

            if (linkedCharacteristicsUpdated) {
                doc.setPropertyValue(
                        BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST,
                        updatedLinkedCharacteristics);
                session.saveDocument(doc);
                session.save();

                fireLinkedCharacteristicsUpdatedEvent(doc, firedBy);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return linkedCharacteristicsUpdated;
    }

    private static Map<String, String> retrieveLinkableDocumentsVersions(
            DocumentModel doc) {

        // versionableId, docId
        Map<String, String> linkableDocumentsVersion = new HashMap<String, String>();

        // First, find related items (direct and hierarchical)
        DocumentModelList relatedDocuments = new DocumentModelListImpl();
        relatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_COMPOSED_OF)));
        relatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_HAS_BOM)));
        relatedDocuments.addAll(
                RelationHelper.getObjectDocuments(doc, new ResourceImpl(
                        EloraRelationConstants.BOM_HAS_SPECIFICATION)));

        for (DocumentModel relatedDoc : relatedDocuments) {
            if (EloraDocumentHelper.checkFilter(doc,
                    BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
                // Hemen ez dugu kontutan hartuko dokumentu beraren bi bertsio
                // desberdin egon ahal dira konposizioan.
                // Oso gutxitan gertatuko da hau eta ez du merezi ezaugarrien
                // kalkuluan hau sartzea.
                linkableDocumentsVersion.put(relatedDoc.getVersionSeriesId(),
                        relatedDoc.getId());

                // recursive call to find whole structure
                linkableDocumentsVersion.putAll(
                        retrieveLinkableDocumentsVersions(relatedDoc));
            }
        }

        return linkableDocumentsVersion;
    }

    private static Set<String> retrieveCharacteristicIs(CoreSession session,
            String docId) {

        Set<String> characteristicIds = new HashSet<String>();

        DocumentModel doc = session.getDocument(new IdRef(docId));

        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> characteristics = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);
        if (characteristics.size() > 0) {
            for (int j = 0; j < characteristics.size(); j++) {
                HashMap<String, Object> chacracteristic = characteristics.get(
                        j);
                String bomCharacteristicId = (String) chacracteristic.get(
                        "bomCharacteristicId");
                characteristicIds.add(bomCharacteristicId);
            }
        }

        return characteristicIds;
    }

    public static void fireLinkedCharacteristicsUpdatedEvent(DocumentModel doc,
            String comment) {
        EloraEventHelper.fireEvent(
                BomCharacteristicsEventNames.LINKED_CHARACS_UPDATED, doc,
                comment);
    }

}
