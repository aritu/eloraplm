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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;

import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoFacetConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.QueriesConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * This class provides a set of custom Elora queries.
 *
 * @author aritu
 *
 */
public class EloraQueryFactory {

    public static String getEloraStructDocuments() {
        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_MIXINTYPE
                        + " = '%s' AND " + NXQL.ECM_MIXINTYPE + " != '%s' AND "
                        + NXQL.ECM_LIFECYCLESTATE + " != '%s' ORDER BY "
                        + NuxeoMetadataConstants.NX_DC_TITLE,
                EloraFacetConstants.FACET_ELORA_STRUCT,
                NuxeoFacetConstants.FACET_HIDDEN_IN_NAVIGATION,
                EloraLifeCycleConstants.NX_DELETED);
        return query;
    }

    public static String getWcDocsByReference(String reference) {
        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " = '%s' AND " + NXQL.ECM_ISVERSION + " = 0 ",
                EloraQueryHelper.escapeSpecialChars(reference));
        return query;
    }

    public static String getDocsByTypeReferenceVersion(String type,
            String reference, long major, long minor) {
        // TODO: que hacer cuando el documento está en la papelera
        String query = String.format(

                "SELECT * FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE
                        + " = '%s' AND "
                        + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                        + " = %s AND "
                        + NuxeoMetadataConstants.NX_UID_MINOR_VERSION
                        + " = %s AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '%s'"
                        + " AND " + NXQL.ECM_ISVERSION + " = 1 ",
                type, type, major, minor,
                EloraQueryHelper.escapeSpecialChars(reference));
        return query;
    }

    public static String getDocsByTypeReferenceVersionQuery(String type,
            String reference, String versionLabel) {
        // TODO: que hacer cuando el documento está en la papelera
        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE
                        + " = '%s' AND " + NXQL.ECM_VERSIONLABEL + " = '%s'"
                        + " AND " + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " = '%s'" + " AND " + NXQL.ECM_ISVERSION + " = 1 ",
                type, type, versionLabel,
                EloraQueryHelper.escapeSpecialChars(reference));
        return query;
    }

    public static String getWcDocsByTypeListAndReferenceQuery(String reference,
            List<String> lstTypes) {

        String typeList = EloraQueryHelper.formatUnquotedList(lstTypes);
        String quotedTypeList = EloraQueryHelper.formatList(lstTypes);

        String query = String.format(
                "SELECT " + NXQL.ECM_UUID + " FROM %s WHERE " + NXQL.ECM_ISPROXY
                        + " = 0 AND " + NXQL.ECM_PRIMARYTYPE + " IN (%s) AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " = '%s' " + " AND " + NXQL.ECM_ISVERSION + " = 0 ",
                typeList, quotedTypeList,
                EloraQueryHelper.escapeSpecialChars(reference));

        return query;

    }

    public static String getWcIdsByTypeListAndReferenceQuery(String reference,
            List<String> lstTypes) {

        String typeList = EloraQueryHelper.formatUnquotedList(lstTypes);
        String quotedTypeList = EloraQueryHelper.formatList(lstTypes);

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_PRIMARYTYPE + " IN (%s) AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " = '%s' " + " AND " + NXQL.ECM_ISVERSION + " = 0 ",
                typeList, quotedTypeList,
                EloraQueryHelper.escapeSpecialChars(reference));

        return query;

    }

    public static String getWcDocsByTypeAndReferenceQuery(String type,
            String reference) {

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE
                        + " = '%s' AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " = '%s' " + " AND " + NXQL.ECM_ISVERSION
                        + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0 ",
                type, type, EloraQueryHelper.escapeSpecialChars(reference));

        return query;
    }

    /**
     * Case insensitive reference
     *
     * @param type
     * @param reference
     * @return
     */
    public static String getWcDocsByTypeAndIReferenceQuery(String type,
            String reference) {

        String query = String.format(
                "SELECT * FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE
                        + " = '%s' AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + " ILIKE '%s' " + " AND " + NXQL.ECM_ISVERSION
                        + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0",
                type, type, EloraQueryHelper.escapeSpecialChars(reference));

        return query;
    }

    /**
     * Count By Reference & Type only
     *
     * @param session
     * @param type
     * @param reference
     * @return
     */
    public static long countWcDocsByTypeAndReference(CoreSession session,
            String type, String reference) {

        return countWcDocsByTypeAndReferenceAndCreatorExcludingUid(session,
                type, reference, null, null);
    }

    /**
     * Count by Reference & Type, Excluding UID
     *
     * @param session
     * @param type
     * @param reference
     * @param excludedUid
     * @return
     */
    public static long countWcDocsByTypeAndReferenceExcludingUid(
            CoreSession session, String type, String reference,
            String excludedUid) {
        return countWcDocsByTypeAndReferenceAndCreatorExcludingUid(session,
                type, reference, null, excludedUid);
    }

    /**
     * Count by Reference & Type & Creator, Excluding UID
     *
     * @param session
     * @param type
     * @param reference
     * @param excludedUid
     * @param creator
     * @return
     */
    public static long countWcDocsByTypeAndReferenceAndCreatorExcludingUid(
            CoreSession session, String type, String reference, String creator,
            String excludedUid) {
        String query = getCountWcDocsByTypeAndReferenceAndCreatorExcludingUidQuery(
                session, type, reference, creator, excludedUid);

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    /**
     * Count by Reference & Type & Creator, Excluding UID. Execute the count in
     * Unrestricted mode.
     *
     * @param session
     * @param type
     * @param reference
     * @param creator
     * @param excludedUid
     * @return
     * @throws EloraException
     */
    public static long unrestrictedCountWcDocsByTypeAndReferenceAndCreatorExcludingUid(
            CoreSession session, String type, String reference, String creator,
            String excludedUid) throws EloraException {
        String query = getCountWcDocsByTypeAndReferenceAndCreatorExcludingUidQuery(
                session, type, reference, creator, excludedUid);

        return EloraQueryHelper.executeUnrestrictedCountQuery(session, query,
                NXQL.ECM_UUID);
    }

    public static String getCountWcDocsByTypeAndReferenceAndCreatorExcludingUidQuery(
            CoreSession session, String type, String reference, String creator,
            String excludedUid) {

        String query = String.format("SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE + " = '%s' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '%s' AND "
                + NXQL.ECM_ISVERSION + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0",
                type, type, EloraQueryHelper.escapeSpecialChars(reference));

        if (excludedUid != null && !excludedUid.isEmpty()) {
            query += String.format(" AND " + NXQL.ECM_UUID + " <> '%s'",
                    excludedUid);
        }

        if (creator != null && !creator.isEmpty()) {
            query += String.format(
                    " AND " + NuxeoMetadataConstants.NX_DC_CREATOR + " = '%s'",
                    creator);
        }

        return query;
    }

    public static String getCountWcDocsByTypeAndReferenceQuery(String type,
            String reference) {
        String query = String.format("SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM %s WHERE " + NXQL.ECM_PRIMARYTYPE + " = '%s' AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '%s' AND "
                + NXQL.ECM_ISVERSION + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0",
                type, type, EloraQueryHelper.escapeSpecialChars(reference));
        return query;
    }

    public static String getWcDocsByFacetListReference(String reference,
            List<String> facets) {
        String facetList = EloraQueryHelper.formatList(facets);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_MIXINTYPE + " IN (%s) AND "
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '%s'"
                        + " AND " + NXQL.ECM_ISVERSION + " = 0 ",
                facetList, EloraQueryHelper.escapeSpecialChars(reference));

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

    public static String getNotReleasedDocListInMajorVersion(String type,
            String versionVersionableId, Long majorVersion) {

        String notReleasedStates = EloraQueryHelper.formatList(
                LifecyclesConfig.unreleasedStatesList);

        String query = String.format("SELECT * FROM %s WHERE "
                + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = %d AND "
                + NXQL.ECM_ISPROXY + " = 0 AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ORDER BY %s DESC",
                type, notReleasedStates, majorVersion, versionVersionableId,
                NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

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

    public static String getPreviousVersionsQuery(String type, String wcId,
            long major, long minor) {

        String query = String.format("SELECT * FROM %s WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '%s' AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' AND "
                + NXQL.ECM_ISPROXY + " = 0 AND " + NXQL.ECM_ISVERSION
                + " = 1 AND (" + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + " < %d " + " OR ("
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = %d AND "
                + NuxeoMetadataConstants.NX_UID_MINOR_VERSION + " < %d)) "
                + "ORDER BY " + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                + ", " + NuxeoMetadataConstants.NX_UID_MINOR_VERSION, type,
                type, wcId, major, major, minor);

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

        String query = String.format(
                "SELECT * FROM %s WHERE "
                        + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION
                        + " = %d AND " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_ISPROXY + "= 0 ",
                type, majorVersion, versionVersionableId);

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
                        + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + NXQL.ECM_ISVERSION + " = 1",
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
     * Returns the list of proxies of the specified document under the specified
     * ancestor.
     *
     * @param type document type
     * @param uid document id
     * @param ancestorId ancestor document id
     * @return
     */
    public static String getDocProxiesQuery(String type, String uid,
            String ancestorId) {

        String query = "SELECT * FROM " + type + " WHERE " + NXQL.ECM_ISPROXY
                + " = 1 AND " + NXQL.ECM_PROXY_TARGETID + " = '" + uid
                + "' AND " + NXQL.ECM_ANCESTORID + " = '" + ancestorId + "'";

        return query;
    }

    /**
     * Returns the list of parents of the specified document's proxies.
     *
     * @param type document type
     * @param uid document id
     * @return
     */
    public static String getDocProxyParentsQuery(String type, String uid) {

        String query = "SELECT " + NXQL.ECM_PARENTID + " FROM " + type
                + " WHERE " + NXQL.ECM_ISPROXY + " = 1 AND "
                + NXQL.ECM_PROXY_TARGETID + " = '" + uid + "'";

        return query;
    }

    /**
     * Returns the list of parents of the specified documents list proxies.
     *
     * @param type document type
     * @param uids list of document ids
     * @return
     */
    public static String getDocProxyParentsQuery(String type,
            List<String> uids) {

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
     * Returns all other document proxies which have the specified ancestor.
     *
     * @param ancestorId
     * @return
     */
    // Same as other_document_proxy_in_content_suggestion core page provider
    public static String getOtherDocumentProxiesInsideAncestorQuery(
            String ancestorId) {

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_MIXINTYPE
                        + " NOT IN ('"
                        + NuxeoFacetConstants.FACET_HIDDEN_IN_NAVIGATION
                        + "', '" + FacetNames.FOLDERISH + "', '"
                        + EloraFacetConstants.FACET_BOM_DOCUMENT + "', '"
                        + EloraFacetConstants.FACET_CAD_DOCUMENT + "') AND "
                        + NXQL.ECM_ISPROXY + " = 1 AND " + NXQL.ECM_ISVERSION
                        + " = 0 AND " + NXQL.ECM_LIFECYCLESTATE + " <> '"
                        + LifeCycleConstants.DELETED_STATE + "'" + " AND "
                        + NXQL.ECM_ANCESTORID + " = '%s' ORDER BY %s "
                        + QueriesConstants.SORT_ORDER_ASC,
                ancestorId, NuxeoMetadataConstants.NX_DC_TITLE);
        return query;
    }

    public static String getDocumentInsideAncestorQuery(String docId,
            String ancestorId) {
        String query = String.format("SELECT * FROM Document WHERE "
                + NXQL.ECM_UUID + "  = '%s' AND " + NXQL.ECM_LIFECYCLESTATE
                + " <> '" + LifeCycleConstants.DELETED_STATE + "'" + " AND "
                + NXQL.ECM_ANCESTORID + " = '%s'", docId, ancestorId);
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

    public static String getWorkspaceRootUidsByAncestorQuery(String ancestor,
            boolean includeArchived) {

        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append(String.format("SELECT " + NXQL.ECM_UUID
                + " FROM WorkspaceRoot WHERE " + NXQL.ECM_ANCESTORID
                + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0", ancestor));

        if (!includeArchived) {
            queryBuilder.append(
                    String.format(" AND " + NXQL.ECM_MIXINTYPE + " <> '%s' ",
                            EloraFacetConstants.FACET_ARCHIVED_WORKSPACE_ROOT));
        }

        String query = queryBuilder.toString();
        return query;
    }

    public static String getWorkspacesByWsUidsWsRootUidsLifeCycleStateAndTypeQuery(
            List<String> wsUids, List<String> wsRootUids, String lifeCycleState,
            String type) {

        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("SELECT * FROM ");

        if (type != null && type.length() > 0) {
            queryBuilder.append(type);
        } else {
            queryBuilder.append("Document");
        }
        queryBuilder.append(
                String.format(" WHERE " + NXQL.ECM_MIXINTYPE + " = '%s' ",
                        EloraFacetConstants.FACET_ELORA_WORKSPACE));
        if (wsUids != null && !wsUids.isEmpty()) {
            String wsUidList = EloraQueryHelper.formatList(wsUids);
            queryBuilder.append(" AND "
                    + String.format(NXQL.ECM_UUID + " IN (%s) ", wsUidList));
        }
        if (type != null && type.length() > 0) {
            queryBuilder.append(String.format(
                    " AND " + NXQL.ECM_PRIMARYTYPE + " = '%s' ", type));
        }
        if (lifeCycleState != null && lifeCycleState.length() > 0) {
            queryBuilder.append(String.format(
                    " AND " + NXQL.ECM_LIFECYCLESTATE + " = '%s' ",
                    lifeCycleState));
        } else {
            queryBuilder.append(String.format(
                    " AND " + NXQL.ECM_LIFECYCLESTATE + " <> '%s' ",
                    LifeCycleConstants.DELETED_STATE));
        }
        if (wsRootUids != null && !wsRootUids.isEmpty()) {
            String wsRootUidList = EloraQueryHelper.formatList(wsRootUids);
            queryBuilder.append(" AND " + String.format(
                    NXQL.ECM_PARENTID + " IN (%s) ", wsRootUidList));
        }

        String query = queryBuilder.toString();
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

    /**
     * We get all the subjects with an active viewer ordering and all the
     * objects with an active inverse viewer ordering, excluding soft deleted
     * ones.
     *
     * @param uid
     * @return
     */
    public static String getRelationsWithViewerUse(String uid) {

        String query = String.format("SELECT * FROM Relation WHERE " + "("
                + NuxeoMetadataConstants.NX_RELATION_TARGET + " = '%s' AND "
                + EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING + " > 0)"
                + " OR (" + NuxeoMetadataConstants.NX_RELATION_SOURCE
                + " = '%s' AND "
                + EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING
                + " > 0)" + " AND "
                + NuxeoMetadataConstants.NX_RELATION_PREDICATE + " <> '"
                + EloraRelationConstants.SOFT_DELETED_RELATION_PREDICATE + "'",
                uid, uid);

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

    public static String countDocumentContentByTypeList(CoreSession session,
            String docId, List<String> lstTypes) {

        String quotedTypeList = EloraQueryHelper.formatList(lstTypes);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_PARENTID
                        + " = '%s' AND " + NXQL.ECM_PRIMARYTYPE + " IN (%s)",
                docId, quotedTypeList);

        return query;

        // return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
        // session);
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

    public static String getTaskActorByTaskDocId(String taskDocId) {
        String query = String.format(
                "SELECT " + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_ACTOR
                        + " FROM RouteNode WHERE "
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO + "/*1/"
                        + NuxeoMetadataConstants.NX_RNODE_TASKS_INFO_TASKDOCID
                        + " = '%s'",
                taskDocId);

        return query;
    }

    public static String getWcDocumentsByContentFilename(String filename) {
        List<String> fs = new ArrayList<String>();
        fs.add(filename);

        String[] f = filename.split("\\.", 2);
        if (f.length == 2) {
            String name = f[0];
            String ext = f[1];

            fs.add(name + "." + ext.toLowerCase());
            fs.add(name + "." + ext.toUpperCase());
            fs.add(name.toLowerCase() + "." + ext);
            fs.add(name.toUpperCase() + "." + ext);
            fs.add(name.toLowerCase() + "." + ext.toLowerCase());
            fs.add(name.toLowerCase() + "." + ext.toUpperCase());
            fs.add(name.toUpperCase() + "." + ext.toLowerCase());
            fs.add(name.toUpperCase() + "." + ext.toUpperCase());
        }

        Set<String> filenames = new HashSet<String>(fs);

        String query = String.format("SELECT * FROM Document WHERE "
                + NXQL.ECM_ISPROXY + " = 0 AND " + NXQL.ECM_ISVERSION
                + " = 0 AND " + NuxeoMetadataConstants.NX_FILE_CONTENT_NAME
                + " IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE + " <> '%s'",
                EloraQueryHelper.formatList(filenames),
                LifeCycleConstants.DELETED_STATE);

        return query;
    }

}
