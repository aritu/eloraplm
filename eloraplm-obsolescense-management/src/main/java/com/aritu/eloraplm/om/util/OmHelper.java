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
package com.aritu.eloraplm.om.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.makeobsolete.util.CanMakeObsoleteResult;

/**
 *
 * @author aritu
 *
 */
public class OmHelper {

    private static final Log log = LogFactory.getLog(OmHelper.class);

    public static List<RowData> getOmProcessDocList(CoreSession session,
            DocumentModel doc, String list) {

        List<RowData> processedDocList = new ArrayList<RowData>();
        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> processedDocsProps = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                list);

        for (HashMap<String, Object> processedDocProp : processedDocsProps) {
            OmRowData processedDoc = createOmProcessDoc(session,
                    processedDocProp);
            processedDocList.add(processedDoc);
        }

        // Sort by versionable id
        processedDocList = sortListByVersionableId(processedDocList);

        return processedDocList;
    }

    public static Map<String, OmRowData> getOmProcessDocMap(CoreSession session,
            DocumentModel doc, String list) {

        Map<String, OmRowData> processedDocMap = new HashMap<String, OmRowData>();
        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> processedDocsProps = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                list);

        for (HashMap<String, Object> processedDocProp : processedDocsProps) {
            OmRowData processedDoc = createOmProcessDoc(session,
                    processedDocProp);

            processedDocMap.put(processedDoc.getUid(), processedDoc);
        }
        return processedDocMap;
    }

    private static OmRowData createOmProcessDoc(CoreSession session,
            HashMap<String, Object> processedDocProp) {

        String processedDocUid = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_UID);
        boolean isAnarchic = (boolean) processedDocProp.get(
                OmMetadataConstants.OM_PDL_IS_ANARCHIC);
        String originState = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_ORIGIN_STATE);
        String finalState = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_DESTINATION_STATE);
        String classification = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_CLASSIFICATION);
        @SuppressWarnings("unchecked")
        List<String> anarchicTopDocs = (List<String>) processedDocProp.get(
                OmMetadataConstants.OM_PDL_ANARCHIC_TOP_DOCS);
        boolean isOk = (boolean) processedDocProp.get(
                OmMetadataConstants.OM_PDL_IS_OK);
        String errorMsg = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_ERROR_MSG);
        String errorMsgParam = (String) processedDocProp.get(
                OmMetadataConstants.OM_PDL_ERROR_MSG_PARAM);

        OmRowData processedDoc = new OmRowData(session, processedDocUid,
                processedDocUid, isAnarchic, originState, finalState,
                classification, anarchicTopDocs, isOk, errorMsg, errorMsgParam);

        return processedDoc;
    }

    public static void saveOmProcessDocs(CoreSession session,
            DocumentModel omProcess, Map<String, OmRowData> docMap, String list)
            throws EloraException {

        String logInitMsg = "[saveOmProcessDocs] ["
                + session.getPrincipal().getName() + "]";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + " Saving |" + docMap.size() + "| ");

        try {
            ArrayList<HashMap<String, Object>> docList = new ArrayList<HashMap<String, Object>>();

            if (docMap.size() > 0) {
                for (Map.Entry<String, OmRowData> entry : docMap.entrySet()) {
                    HashMap<String, Object> docInfo = createOmProcessDocMetadataMap(
                            entry.getValue());
                    docList.add(docInfo);
                }
            }

            omProcess.setPropertyValue(list, docList);

            session.saveDocument(omProcess);
            session.save();

            log.info("Doc List successfully saved.");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static List<RowData> sortListByVersionableId(
            List<RowData> unorderedList) {

        Comparator<OmRowData> versionableIdComparator = new Comparator<OmRowData>() {
            @Override
            public int compare(OmRowData d1, OmRowData d2) {
                int c1 = d1.getVersionableId().compareTo(d2.getVersionableId());
                if (c1 == 0) {
                    return d1.getVersionNumber().compareTo(
                            d2.getVersionNumber());
                } else {
                    return c1;
                }
            }
        };

        List<RowData> sortedList = unorderedList.stream().map(
                obj -> (OmRowData) obj).sorted(
                        versionableIdComparator.reversed()).collect(
                                Collectors.toList());

        return sortedList;
    }

    private static HashMap<String, Object> createOmProcessDocMetadataMap(
            OmRowData processedDoc) throws EloraException {

        HashMap<String, Object> processedDocMap = new HashMap<>();

        processedDocMap.put(OmMetadataConstants.OM_PDL_UID,
                processedDoc.getUid());
        processedDocMap.put(OmMetadataConstants.OM_PDL_IS_ANARCHIC,
                processedDoc.getIsAnarchic());
        processedDocMap.put(OmMetadataConstants.OM_PDL_ORIGIN_STATE,
                processedDoc.getOriginState());
        processedDocMap.put(OmMetadataConstants.OM_PDL_DESTINATION_STATE,
                processedDoc.getDestinationState());
        processedDocMap.put(OmMetadataConstants.OM_PDL_CLASSIFICATION,
                processedDoc.getClassification());
        processedDocMap.put(OmMetadataConstants.OM_PDL_ANARCHIC_TOP_DOCS,
                processedDoc.getAnarchicTopDocs());
        processedDocMap.put(OmMetadataConstants.OM_PDL_IS_OK,
                processedDoc.getIsOk());
        processedDocMap.put(OmMetadataConstants.OM_PDL_ERROR_MSG,
                processedDoc.getErrorMsg());
        processedDocMap.put(OmMetadataConstants.OM_PDL_ERROR_MSG_PARAM,
                processedDoc.getErrorMsgParam());

        return processedDocMap;
    }

    public static OmRowData createOmProcessDoc(CoreSession session,
            DocumentModel doc, Statement stmt, CanMakeObsoleteResult result) {

        String docUid = doc.getId();
        String predicate = null;
        boolean isAnarchic = false;

        if (stmt != null) {
            predicate = stmt.getPredicate().getUri();

            isAnarchic = RelationsConfig.bomAnarchicRelationsList.contains(
                    predicate) ? true : false;
        }

        String originState = doc.getCurrentLifeCycleState();
        String finalState = result.getCanMakeObsolete()
                ? EloraLifeCycleConstants.OBSOLETE
                : originState;

        String classification = doc.getType();

        return new OmRowData(session, docUid, docUid, isAnarchic, originState,
                finalState, classification, result.getCanMakeObsolete(),
                result.getCannotMakeObsoleteReasonMsg(),
                result.getCannotMakeObsoleteReasonMsgParam());
    }

    public static List<String> getNonSelectedAnarchicImpactedDocs(
            CoreSession session, String omProcessUid,
            List<String> selectedAnarchics) throws EloraException {

        List<String> allAnarchics = getAnarchicImpactedDocUids(session,
                omProcessUid);
        List<String> nonSelectedAnarchics = new ArrayList<String>(allAnarchics);
        nonSelectedAnarchics.removeAll(selectedAnarchics);

        return nonSelectedAnarchics;

    }

    private static List<String> getAnarchicImpactedDocUids(CoreSession session,
            String omProcessUid) throws EloraException {

        List<String> anarchicImpactedUids = new ArrayList<String>();

        String query = OmQueryFactory.getAnarchicImpactedDocUidsQuery(
                omProcessUid);

        IterableQueryResult result = null;
        try {
            result = session.queryAndFetch(query, NXQL.NXQL);
            if (result.size() > 0) {
                for (Map<String, Serializable> map : result) {
                    Serializable uidValue = map.get(
                            OmMetadataConstants.OM_IMPACTED_DOC_LIST + "/*1/"
                                    + OmMetadataConstants.OM_PDL_UID);
                    if (uidValue != null) {
                        String uid = uidValue.toString();
                        anarchicImpactedUids.add(uid);
                    }
                }
            }
        } catch (NuxeoException e) {
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (result != null) {
                result.close();
            }
        }

        return anarchicImpactedUids;
    }

    public static List<String> getImpactedDocUids(CoreSession session,
            String omProcessUid) throws EloraException {
        List<String> impactedUids = new ArrayList<String>();

        String query = OmQueryFactory.getImpactedDocUidsQuery(omProcessUid);

        IterableQueryResult result = null;
        try {
            result = session.queryAndFetch(query, NXQL.NXQL);
            if (result.size() > 0) {
                for (Map<String, Serializable> map : result) {
                    if (!map.isEmpty()) {
                        Serializable uidValue = map.get(
                                OmMetadataConstants.OM_IMPACTED_DOC_LIST
                                        + "/*1/"
                                        + OmMetadataConstants.OM_PDL_UID);
                        if (uidValue != null) {
                            String uid = uidValue.toString();
                            impactedUids.add(uid);
                        }
                    }
                }
            }

        } catch (NuxeoException e) {
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (result != null) {
                result.close();
            }
        }

        return impactedUids;
    }

    public static List<String> getProcessedDocUids(CoreSession session,
            String omProcessUid) throws EloraException {
        List<String> processedUids = new ArrayList<String>();

        String query = OmQueryFactory.getProcessedDocUidsQuery(omProcessUid);

        IterableQueryResult result = null;
        try {
            result = session.queryAndFetch(query, NXQL.NXQL);
            if (result.size() > 0) {
                for (Map<String, Serializable> map : result) {
                    if (!map.isEmpty()) {
                        Serializable uidValue = map.get(
                                OmMetadataConstants.OM_PROCESSED_DOC_LIST
                                        + "/*1/"
                                        + OmMetadataConstants.OM_PDL_UID);
                        if (uidValue != null) {
                            String uid = uidValue.toString();
                            processedUids.add(uid);
                        }
                    }
                }
            }

        } catch (NuxeoException e) {
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (result != null) {
                result.close();
            }
        }

        return processedUids;
    }

    public static Map<String, List<String>> getAnarchicTopDocUids(
            CoreSession session, DocumentModel omProcess)
            throws EloraException {

        Map<String, List<String>> anarchicTopDocUids = new HashMap<String, List<String>>();

        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> impactedDocProps = (ArrayList<HashMap<String, Object>>) omProcess.getPropertyValue(
                OmMetadataConstants.OM_IMPACTED_DOC_LIST);

        for (HashMap<String, Object> prop : impactedDocProps) {
            boolean isAnarchic = (boolean) prop.get(
                    OmMetadataConstants.OM_PDL_IS_ANARCHIC);
            if (!isAnarchic) {
                continue;
            }
            String uid = (String) prop.get(OmMetadataConstants.OM_PDL_UID);
            @SuppressWarnings("unchecked")
            List<String> topDocs = (List<String>) prop.get(
                    OmMetadataConstants.OM_PDL_ANARCHIC_TOP_DOCS);
            if (uid != null && topDocs != null) {
                anarchicTopDocUids.put(uid, topDocs);
            }
        }
        return anarchicTopDocUids;
    }

}
