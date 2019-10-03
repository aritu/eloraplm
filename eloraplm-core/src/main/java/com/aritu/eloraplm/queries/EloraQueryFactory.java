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
package com.aritu.eloraplm.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;

import com.aritu.eloraplm.config.util.LifecyclesConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * This class provides a set of custom Elora queries.
 *
 * @author aritu
 *
 */
public class EloraQueryFactory {

    public static String getWcDocsByReference(String reference) {
        String query = "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY
                + " = 0 AND " + EloraMetadataConstants.ELORA_ELO_REFERENCE
                + " = '" + reference + "'" + " AND " + NXQL.ECM_ISVERSION
                + " = 0 ";
        return query;
    }

    public static String getDocsByTypeReferenceVersion(String type,
            String reference, long major, long minor) {
        // TODO: que hacer cuando el documento está en la papelera
        String query = "SELECT * FROM " + type + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = " + major
                + " AND " + NuxeoMetadataConstants.NX_UID_MINOR_VERSION + " = "
                + minor + " AND " + EloraMetadataConstants.ELORA_ELO_REFERENCE
                + " = '" + reference + "'" + " AND " + NXQL.ECM_ISVERSION
                + " = 1 ";
        return query;
    }

    public static String getDocsByTypeReferenceVersionQuery(String type,
            String reference, String versionLabel) {
        // TODO: que hacer cuando el documento está en la papelera
        String query = "SELECT * FROM " + type + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + NXQL.ECM_VERSIONLABEL + " = '" + versionLabel + "'" + " AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "'" + " AND " + NXQL.ECM_ISVERSION + " = 1 ";
        return query;
    }

    public static String getWcDocsByTypeListAndReferenceQuery(String reference,
            List<String> lstTypes) {

        String typeList = EloraQueryHelper.formatUnquotedList(lstTypes);
        String quotedTypeList = EloraQueryHelper.formatList(lstTypes);

        String query = String.format("SELECT * FROM %s WHERE "
                + NXQL.ECM_ISPROXY + " = 0 AND " + NXQL.ECM_PRIMARYTYPE
                + " IN (%s) AND " + EloraMetadataConstants.ELORA_ELO_REFERENCE
                + " = '" + reference + "' " + " AND " + NXQL.ECM_ISVERSION
                + " = 0 ", typeList, quotedTypeList);

        return query;

    }

    public static String getWcDocsByTypeAndReferenceQuery(String type,
            String reference) {

        String query = "SELECT * FROM " + type + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' " + " AND " + NXQL.ECM_ISVERSION + " = 0 AND "
                + NXQL.ECM_ISPROXY + " = 0 ";

        return query;
    }

    public static String getWcDocsByTypeAndReferenceExcludingUidQuery(
            String type, String reference, String uid) {

        String query = "SELECT * FROM " + type + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' " + " AND " + NXQL.ECM_UUID + " <> '" + uid
                + "' AND " + NXQL.ECM_ISVERSION + " = 0 AND " + NXQL.ECM_ISPROXY
                + " = 0 ";

        return query;
    }

    public static String getWcDocsByTypeAndReferenceAndCreatorExcludingUidQuery(
            String type, String reference, String username, String uid) {

        String query = "SELECT * FROM " + type + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' " + " AND "
                + NuxeoMetadataConstants.NX_DC_CREATOR + " = '" + username
                + "' AND " + NXQL.ECM_UUID + " <> '" + uid + "' AND "
                + NXQL.ECM_ISVERSION + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0 ";

        return query;
    }

    public static long countWcDocsByTypeAndReference(CoreSession session,
            String type, String reference) {

        return countWcDocsByTypeAndReferenceExcludingUid(session, type,
                reference, null);
    }

    public static long countWcDocsByTypeAndReferenceExcludingUid(
            CoreSession session, String type, String reference, String uid) {

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM " + type
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' AND " + NXQL.ECM_ISVERSION + " = 0 AND "
                + NXQL.ECM_ISPROXY + " = 0 ";

        if (uid != null && !uid.isEmpty()) {
            query += "AND " + NXQL.ECM_UUID + " <> '" + uid + "'";
        }

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    public static String getWcDocsByFacetListReference(String reference,
            List<String> facets) {
        String facetList = EloraQueryHelper.formatList(facets);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_MIXINTYPE + " IN (%s) AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '%s'"
                        + " AND " + NXQL.ECM_ISVERSION + " = 0 ",
                facetList, reference);

        return query;
    }

    public static String getMaxReferenceByTypeQuery(String type) {
        String query = "SELECT MAX("
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + ") FROM " + type
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND ";

        return query;
    }

    public static String getMajorReleasedVersionQuery(String type,
            String versionVersionableId, Long majorVersion) {
        // Only one version can be released per mayor letter

        String releasedStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.releasedStatesList);

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND "
                        + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                        + " = %d AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ",
                type, releasedStatesList, majorVersion, versionVersionableId);

        return query;
    }

    public static String getMajorReleasedOrObsoleteVersionQuery(String type,
            String versionVersionableId, Long majorVersion) {

        List<String> releasedAndObsoleteStates = new ArrayList<>(
                LifecyclesConfig.releasedStatesList);
        releasedAndObsoleteStates.addAll(LifecyclesConfig.obsoleteStatesList);
        String releasedAndObsoleteStatesList = EloraQueryHelper.formatList(
                releasedAndObsoleteStates);

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND "
                        + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                        + " = %d AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ",
                type, releasedAndObsoleteStatesList, majorVersion,
                versionVersionableId);

        return query;
    }

    public static String getReleasedDocsQuery(String type,
            String versionVersionableId, String sortOrder) {

        String releasedStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.releasedStatesList);

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_ISPROXY
                        + " = 0 ORDER BY %s " + sortOrder + ", %s " + sortOrder,
                type, releasedStatesList, versionVersionableId,
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return query;
    }

    public static String getOlderReleasedOrObsoleteVersionsQuery(
            String versionVersionableId, String primaryType, String sortOrder,
            long currentMajorVersion) {

        List<String> releasedAndObsoleteList = new ArrayList<String>();
        releasedAndObsoleteList.addAll(LifecyclesConfig.releasedStatesList);
        releasedAndObsoleteList.addAll(LifecyclesConfig.obsoleteStatesList);
        String releasedAndObsoleteStatesList = EloraQueryHelper.formatList(
                releasedAndObsoleteList);

        String query = String.format("SELECT * FROM %s WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '%s' AND "
                + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' AND "
                + NXQL.ECM_ISPROXY + " = 0 AND " + NXQL.ECM_ISVERSION
                + " = 1 AND " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " < %d ORDER BY %s " + sortOrder + ", %s " + sortOrder,
                primaryType, primaryType, releasedAndObsoleteStatesList,
                versionVersionableId, currentMajorVersion,
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return query;
    }

    public static String getTemplateByNameQuery(String templateName) {
        String query = String.format(
                "SELECT * FROM TemplateSource WHERE "
                        + NuxeoMetadataConstants.NX_DC_TITLE + " = '%s'",
                templateName);
        return query;
    }

    public static boolean checkIfNewerReleasedVersionExists(Long majorVersion,
            Long minorVersion, String versionVersionableId,
            CoreSession session) {

        String releasedStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.releasedStatesList);

        String query = String.format("SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' AND "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " >= %d AND ("
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " <> %d OR "
                + NuxeoMetadataConstants.NX_UID_MINOR_VERSION + " <> %d)",
                releasedStatesList, versionVersionableId, majorVersion,
                majorVersion, minorVersion);

        long countResult = EloraQueryHelper.executeCountQuery(query,
                NXQL.ECM_UUID, session);
        return countResult > 0;
    }

    public static String getMajorVersionDocsQuery(String type,
            String versionVersionableId, Long majorVersion,
            boolean includeObsoletes, String sortOrder) {

        String query = String.format("SELECT * FROM %s WHERE "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = %d AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ", type,
                majorVersion, versionVersionableId);

        if (!includeObsoletes) {
            String obsoleteStatesList = EloraQueryHelper.formatList(
                    LifecyclesConfig.obsoleteStatesList);

            query += String.format(
                    " AND " + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s)",
                    obsoleteStatesList);
        }
        query += " ORDER BY " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " " + sortOrder + ", "
                + NuxeoMetadataConstants.NX_UID_MINOR_VERSION + " " + sortOrder;

        return query;
    }

    public static String getAllVersionsDocsQuery(String type,
            String versionVersionableId, boolean includeObsoletes,
            String sortOrder) {

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_ISVERSION + " = 1",
                type, versionVersionableId);

        if (!includeObsoletes) {
            String obsoleteStatesList = EloraQueryHelper.formatList(
                    LifecyclesConfig.obsoleteStatesList);

            query += String.format(
                    " AND " + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s)",
                    obsoleteStatesList);
        }

        query += " ORDER BY " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " " + sortOrder + ", "
                + NuxeoMetadataConstants.NX_UID_MINOR_VERSION + " " + sortOrder;

        return query;
    }

    // Returns last released version within uids
    public static String getRelatedReleasedDocQuery(String type,
            String versionVersionableId, List<String> uids) {

        String uidList = EloraQueryHelper.formatList(uids);
        String obsoleteStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.obsoleteStatesList);
        String releasedStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.releasedStatesList);

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_UUID + " IN (%s) AND "
                        + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' AND "
                        + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s) AND "
                        + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                        + NXQL.ECM_ISVERSION + " = 1 ORDER BY %s DESC, %s DESC",
                type, uidList, versionVersionableId, obsoleteStatesList,
                releasedStatesList, NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return query;
    }

    // Returns last released version within uids
    public static String getRelatedReleasedDocQuery(List<String> uids) {

        String releasedStateList = EloraQueryHelper.formatList(
                LifecyclesConfig.releasedStatesList);
        String uidList = EloraQueryHelper.formatList(uids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                        + " IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY %s DESC, %s DESC ",
                uidList, releasedStateList,
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return query;
    }

    // Returns last version within uids
    public static String getLatestRelatedDocQuery(String type,
            String versionVersionableId, List<String> uids,
            boolean includeObsoletes) {

        String uidList = EloraQueryHelper.formatList(uids);
        String obsoleteStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.obsoleteStatesList);

        String queryString = "SELECT * FROM " + type + " WHERE " + NXQL.ECM_UUID
                + " IN (%s) AND " + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ";
        if (!includeObsoletes) {
            queryString += " AND " + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s) ";
        }
        queryString += " AND " + NXQL.ECM_ISVERSION
                + " = 1 ORDER BY %s DESC, %s DESC ";

        if (!includeObsoletes) {
            return String.format(queryString, uidList, versionVersionableId,
                    obsoleteStatesList,
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                    NuxeoMetadataConstants.NX_UID_MINOR_VERSION);
        } else {
            return String.format(queryString, uidList, versionVersionableId,
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                    NuxeoMetadataConstants.NX_UID_MINOR_VERSION);
        }
    }

    // Returns last version within uids
    public static String getLatestRelatedDocQuery(List<String> uids,
            boolean includeObsoletes) {
        String obsoleteStatesList = EloraQueryHelper.formatList(
                LifecyclesConfig.obsoleteStatesList);
        String uidList = EloraQueryHelper.formatList(uids);

        String queryString = "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                + " IN (%s) ";
        if (!includeObsoletes) {
            queryString += " AND " + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s) ";
        }
        queryString += " AND " + NXQL.ECM_ISVERSION
                + " = 1 ORDER BY %s DESC, %s DESC ";

        if (!includeObsoletes) {
            return String.format(queryString, uidList, obsoleteStatesList,
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                    NuxeoMetadataConstants.NX_UID_MINOR_VERSION);
        } else {
            return String.format(queryString, uidList,
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION,
                    NuxeoMetadataConstants.NX_UID_MINOR_VERSION);
        }

    }

    /**
     * Returns the list of proxies of the specified document.
     *
     * @param uid document id
     * @return
     */
    public static String getDocProxiesQuery(String type, String uid) {

        String query = "SELECT " + NXQL.ECM_PARENTID + " FROM " + type
                + " WHERE " + NXQL.ECM_ISPROXY + " = 1 AND "
                + NXQL.ECM_PROXY_TARGETID + " = '" + uid + "'";

        return query;
    }

    /**
     * Returns the list of proxies of the specified documents list.
     *
     * @param uids list of document ids
     * @return
     */
    public static String getDocProxiesQuery(String type, List<String> uids) {

        String uidList = EloraQueryHelper.formatList(uids);

        String query = String.format("SELECT " + NXQL.ECM_PARENTID
                + " FROM %s WHERE " + NXQL.ECM_ISPROXY + " = 1 AND "
                + NXQL.ECM_PROXY_TARGETID + " IN (%s) ", type, uidList);

        return query;
    }

    /**
     * Returns all folders that have the provided workspace as ancestor.
     *
     * @param workspaceId
     * @return
     */
    public static String getFoldersInWorkspaceQuery(String workspaceId) {

        return getDocumentsByFacetInsideAncestorQuery(FacetNames.FOLDERISH,
                workspaceId);
    }

    /**
     * Returns all CAD documents that have the provided workspace as ancestor.
     *
     * @param workspaceId
     * @return
     */
    public static String getCadDocumentsInWorkspaceQuery(String workspaceId) {

        return getDocumentsByFacetInsideAncestorQuery(
                EloraFacetConstants.FACET_CAD_DOCUMENT, workspaceId);
    }

    /**
     * Returns all BOM items that have the provided workspace as ancestor.
     *
     * @param workspaceId
     * @return
     */
    public static String getItemsInWorkspaceQuery(String workspaceId) {

        return getDocumentsByFacetInsideAncestorQuery(
                EloraFacetConstants.FACET_BOM_DOCUMENT, workspaceId);
    }

    /**
     * Returns all documents with the provided facet which have the specified
     * ancestor.
     *
     * @param facet
     * @param ancestorId
     * @return
     */
    public static String getDocumentsByFacetInsideAncestorQuery(String facet,
            String ancestorId) {

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " <> '" + LifeCycleConstants.DELETED_STATE + "'"
                        + " AND " + NXQL.ECM_MIXINTYPE + " = '%s' AND "
                        + NXQL.ECM_ANCESTORID + " = '%s'",
                facet, ancestorId);

        return query;
    }

    /**
     * @param bomListId
     * @param bomListUids
     * @return
     */
    public static String getBomListsByListIdQuery(String bomListId,
            List<String> bomListUids) {

        String bomListUidsFormatted = EloraQueryHelper.formatList(bomListUids);

        String query = String.format("SELECT * FROM BomList WHERE "
                + NXQL.ECM_PRIMARYTYPE
                + " = 'BomList' AND ecm:uuid IN (%s) AND bomlst:bomList = '%s'",
                bomListUidsFormatted, bomListId);

        return query;
    }

    /**
     * @param bomListId
     * @param bomListUids
     * @return
     */
    public static String countBomListsByListIdQuery(String bomListId,
            List<String> bomListUids) {

        String bomListUidsFormatted = EloraQueryHelper.formatList(bomListUids);

        String query = String.format("SELECT COUNT(" + NXQL.ECM_UUID + ") "
                + "FROM BomList WHERE " + NXQL.ECM_PRIMARYTYPE
                + " = 'BomList' AND ecm:uuid IN (%s) AND bomlst:bomList = '%s'",
                bomListUidsFormatted, bomListId);

        return query;
    }

    public static String getStructureRootsForSelectQuery() {

        String query = "SELECT " + NXQL.ECM_UUID + ", "
                + NuxeoMetadataConstants.NX_DC_TITLE + " FROM "
                + EloraDoctypeConstants.STRUCTURE_ROOT + " WHERE "
                + NXQL.ECM_ISPROXY + " = 0";

        return query;
    }

    public static DocumentModel getLatestByStatesInMajorVersion(
            CoreSession session, Long majorVersion, List<String> states,
            List<String> uids, String type) {

        String stateList = EloraQueryHelper.formatList(states);
        String uidList = EloraQueryHelper.formatList(uids);

        String query = String.format("SELECT * FROM %s WHERE " + NXQL.ECM_UUID
                + " IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = %d AND "
                + NXQL.ECM_ISVERSION + " = 1 ORDER BY %s DESC", type, uidList,
                stateList, majorVersion,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return EloraQueryHelper.executeGetFirstQuery(query, session);
    }

    public static DocumentModel getLatestInMajorVersion(CoreSession session,
            Long majorVersion, List<String> uids, String type) {

        String uidList = EloraQueryHelper.formatList(uids);

        String query = String.format("SELECT * FROM %s WHERE " + NXQL.ECM_UUID
                + " IN (%s) AND " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " = %d AND " + NXQL.ECM_ISVERSION + " = 1 ORDER BY %s DESC",
                type, uidList, majorVersion,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

        return EloraQueryHelper.executeGetFirstQuery(query, session);

    }

    public static Map<String, String> getAllVersionsInfo(CoreSession session,
            String versionVersionableId, boolean includeObsoletes) {

        String query = getAllVersionsInfoQuery(versionVersionableId,
                includeObsoletes);

        Map<String, String> versionsInfo = new LinkedHashMap<>();

        IterableQueryResult queryResult = session.queryAndFetch(query,
                NXQL.NXQL);

        try {
            for (Map<String, Serializable> map : queryResult) {
                String versionUid = map.get(NXQL.ECM_UUID).toString();
                String versionLabel = map.get(NXQL.ECM_VERSIONLABEL).toString();

                versionsInfo.put(versionUid, versionLabel);
            }
        } finally {
            queryResult.close();
        }

        return versionsInfo;
    }

    public static String getAllVersionsInfoQuery(String versionVersionableId,
            boolean includeObsoletes) {

        String query = String.format("SELECT " + NXQL.ECM_UUID + ", "
                + NXQL.ECM_VERSIONLABEL + " FROM Document WHERE "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' AND "
                + NXQL.ECM_ISVERSION + " = 1", versionVersionableId);

        if (!includeObsoletes) {
            String obsoleteStatesList = EloraQueryHelper.formatList(
                    LifecyclesConfig.obsoleteStatesList);

            query += String.format(
                    " AND " + NXQL.ECM_LIFECYCLESTATE + " NOT IN (%s)",
                    obsoleteStatesList);
        }

        query += " ORDER BY " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " DESC, " + NuxeoMetadataConstants.NX_UID_MINOR_VERSION
                + " DESC";

        return query;
    }

    public static String getAllDraftsByCreatorQuery(String creator) {
        String query = "SELECT * FROM Document WHERE " + NXQL.ECM_MIXINTYPE
                + " = '" + EloraFacetConstants.FACET_ELORA_DRAFT + "' AND "
                + NuxeoMetadataConstants.NX_DC_CREATOR + " = '" + creator
                + "' AND " + NXQL.ECM_ISPROXY + " = 0 AND " + NXQL.ECM_ISVERSION
                + " = 0 ";
        return query;
    }

    public static String getWorkspaceRootUidsByAncestorQuery(String ancestor) {

        String query = String.format("SELECT " + NXQL.ECM_UUID
                + " FROM WorkspaceRoot WHERE " + NXQL.ECM_ANCESTORID
                + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0", ancestor);
        return query;
    }

    public static String getNotDeletedWorkspacesForWsRootsQuery(
            List<String> wsRootUids) {

        String wsRootUidList = EloraQueryHelper.formatList(wsRootUids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " <> '%s' AND " + NXQL.ECM_MIXINTYPE + " = '%s' AND "
                        + NXQL.ECM_PARENTID + " IN (%s)" + " ORDER BY dc:title",
                LifeCycleConstants.DELETED_STATE,
                EloraFacetConstants.FACET_ELORA_WORKSPACE, wsRootUidList);

        return query;
    }

    public static String getNotDeletedWorkspacesForWsRootsAndTypeQuery(
            List<String> wsRootUids, String type) {

        String wsRootUidList = EloraQueryHelper.formatList(wsRootUids);

        String query = String.format(
                "SELECT * FROM " + type + "  WHERE " + NXQL.ECM_PRIMARYTYPE
                        + " = '" + type + "' AND " + NXQL.ECM_LIFECYCLESTATE
                        + " <> '%s' AND " + NXQL.ECM_PARENTID + " IN (%s)"
                        + " ORDER BY dc:title",
                LifeCycleConstants.DELETED_STATE, wsRootUidList);

        return query;
    }

    public static String getWorkspacesForWsRootsAndLifeCycleStateQuery(
            List<String> wsRootUids, String lifeCycleState) {

        String wsRootUidList = EloraQueryHelper.formatList(wsRootUids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " = '%s' AND " + NXQL.ECM_MIXINTYPE + " = '%s' AND "
                        + NXQL.ECM_PARENTID + " IN (%s)" + " ORDER BY dc:title",
                lifeCycleState, EloraFacetConstants.FACET_ELORA_WORKSPACE,
                wsRootUidList);

        return query;
    }

    public static String getWorkspacesForWsRootsLifeCycleStateAndTypeQuery(
            List<String> wsRootUids, String lifeCycleState, String type) {

        String wsRootUidList = EloraQueryHelper.formatList(wsRootUids);

        String query = String.format("SELECT * FROM " + type + "  WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "' AND "
                + NXQL.ECM_LIFECYCLESTATE + " = '%s' AND " + NXQL.ECM_PARENTID
                + " IN (%s)" + " ORDER BY dc:title", lifeCycleState,
                wsRootUidList);

        return query;
    }

    public static String getSoftDeletedRelationsQuery() {

        String query = String.format("SELECT * FROM Relation WHERE "
                + NuxeoMetadataConstants.NX_RELATION_SOURCE + " = '%s' AND "
                + NuxeoMetadataConstants.NX_RELATION_TARGET + " = '%s' AND "
                + NuxeoMetadataConstants.NX_RELATION_PREDICATE + " = '%s'",
                EloraRelationConstants.SOFT_DELETED_RELATION_SOURCE,
                EloraRelationConstants.SOFT_DELETED_RELATION_TARGET,
                EloraRelationConstants.SOFT_DELETED_RELATION_PREDICATE);

        return query;
    }

    public static long countCharacteristicsShownInReport(CoreSession session,
            String type, String uid) {

        String query = String.format(
                "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM %s WHERE "
                        + NXQL.ECM_PRIMARYTYPE + " = '%s' AND " + NXQL.ECM_UUID
                        + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST
                        + "/*/showInReport = 1",
                type, type, uid);

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    public static long countObjectRelationsForDocumentByPredicateList(
            CoreSession session, String uid, List<String> predicateUris) {

        String predicateList = EloraQueryHelper.formatList(predicateUris);

        String query = String.format("SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM Relation WHERE "
                + NuxeoMetadataConstants.NX_RELATION_SOURCE + " = '%s' AND "
                + NuxeoMetadataConstants.NX_RELATION_PREDICATE + " IN (%s)",
                uid, predicateList);

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    public static String getTaskStatusByTaskDocId(String taskDocId) {
        String query = String.format(
                "SELECT " + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_STATUS
                        + " FROM RouteNode WHERE "
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_TASKDOCID
                        + " = '%s'",
                taskDocId);

        return query;
    }
}
