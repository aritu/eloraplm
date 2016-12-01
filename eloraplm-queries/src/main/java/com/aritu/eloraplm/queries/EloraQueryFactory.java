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

import java.util.List;

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;

/**
 * This class provides a set of custom Elora queries.
 *
 * @author aritu
 *
 */
public class EloraQueryFactory {

    public static String getDocsByTypeReferenceVersion(String type,
            String reference, String versionLabel) {
        // TODO: que hacer cuando el documento está en la papelera
        String query = "SELECT * FROM Document " + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + type + "'" + " AND "
                + NXQL.ECM_VERSIONLABEL + " = '" + versionLabel + "'" + " AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "'" + " AND " + NXQL.ECM_ISVERSION + " = 1";
        return query;
    }

    public static String getWcDocsByTypeAndReference(String type,
            String reference) {

        String query = "SELECT * FROM Document WHERE " + NXQL.ECM_PRIMARYTYPE
                + " = '" + type + "' " + " AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' " + " AND " + NXQL.ECM_ISVERSION + " = 0 AND "
                + NXQL.ECM_ISPROXY + " = 0 ";

        return query;
    }

    public static String getWcDocsByTypeAndReferenceExcludingUid(String type,
            String reference, String uid) {

        String query = "SELECT * FROM Document WHERE " + NXQL.ECM_PRIMARYTYPE
                + " = '" + type + "' " + " AND "
                + EloraMetadataConstants.ELORA_ELO_REFERENCE + " = '"
                + reference + "' " + " AND " + NXQL.ECM_UUID + " <> '" + uid
                + "' AND " + NXQL.ECM_ISVERSION + " = 0 AND " + NXQL.ECM_ISPROXY
                + " = 0 ";

        return query;
    }

    public static String getMaxReferenceByType(String type) {
        String query = "SELECT MAX("
                + EloraMetadataConstants.ELORA_ELO_REFERENCE
                + ") FROM Document WHERE " + NXQL.ECM_PRIMARYTYPE + " = '"
                + type + "'";

        return query;
    }

    public static String getMajorReleasedVersion(String versionVersionableId,
            String[] states, String majorVersion) {
        // Only one version can be released per mayor letter
        String stateList = "";
        for (String state : states) {
            stateList += "'" + state + "',";
        }
        // We guess there is at least one released state in configuration. If
        // not it will crash
        stateList = stateList.substring(0, stateList.length() - 1);

        String query = String.format("SELECT * FROM Document WHERE "
                + NXQL.ECM_LIFECYCLESTATE + " IN (%s) AND "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = '%s' AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ", stateList,
                majorVersion, versionVersionableId);

        return query;
    }

    public static String getReleasedDocs(String versionVersionableId,
            String[] releasedStates) {
        String releasedStateList = formatList(releasedStates);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' ORDER BY "
                        + NuxeoMetadataConstants.NX_ECM_VERSION_LABEL + " DESC",
                releasedStateList, versionVersionableId);

        return query;
    }

    public static String getMajorVersionDocs(String versionVersionableId,
            String majorVersion) {

        String query = String.format("SELECT * FROM Document WHERE "
                + NuxeoMetadataConstants.NX_UID_MAJOR_VERSION + " = '%s' AND "
                + NXQL.ECM_VERSION_VERSIONABLEID + " = '%s' ORDER BY "
                + NuxeoMetadataConstants.NX_ECM_VERSION_LABEL + " DESC",
                majorVersion, versionVersionableId);

        return query;
    }

    // Returns last released version within uids
    public static String getRelatedReleasedDoc(String versionVersionableId,
            String[] releasedStates, String[] obsoleteStates, String[] uids) {

        String releasedStateList = formatList(releasedStates);
        String obsoleteStateList = formatList(obsoleteStates);
        String uidList = formatList(uids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                        + " IN (%s) AND " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_LIFECYCLESTATE
                        + " NOT IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY " + NXQL.ECM_VERSIONLABEL + " DESC ",
                uidList, versionVersionableId, obsoleteStateList,
                releasedStateList);

        return query;
    }

    // Returns last released version within uids
    public static String getRelatedReleasedDoc(String[] releasedStates,
            String[] uids) {

        String releasedStateList = formatList(releasedStates);
        String uidList = formatList(uids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                        + " IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE
                        + " IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY " + NXQL.ECM_VERSIONLABEL + " DESC ",
                uidList, releasedStateList);

        return query;
    }

    // Returns last version within uids
    public static String getLatestRelatedDoc(String versionVersionableId,
            String[] obsoleteStates, String[] uids) {

        String obsoleteStateList = formatList(obsoleteStates);
        String uidList = formatList(uids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                        + " IN (%s) AND " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_LIFECYCLESTATE
                        + " NOT IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY " + NXQL.ECM_VERSIONLABEL + " DESC ",
                uidList, versionVersionableId, obsoleteStateList);

        return query;
    }

    // Returns last version within uids
    public static String getLatestRelatedDoc(String[] obsoleteStates,
            String[] uids) {

        String obsoleteStateList = formatList(obsoleteStates);
        String uidList = formatList(uids);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_UUID
                        + " IN (%s) AND " + NXQL.ECM_LIFECYCLESTATE
                        + " NOT IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY " + NXQL.ECM_VERSIONLABEL + " DESC ",
                uidList, obsoleteStateList);

        return query;
    }

    // Returns latest alive version. Doc can't be in obsolete state
    public static String getLatestAliveVersionDoc(String versionVersionableId,
            String[] obsoleteStates) {
        String obsoleteStateList = formatList(obsoleteStates);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_VERSION_VERSIONABLEID
                        + " = '%s' AND " + NXQL.ECM_LIFECYCLESTATE
                        + " NOT IN (%s) AND " + NXQL.ECM_ISVERSION
                        + " = 1 ORDER BY " + NXQL.ECM_VERSIONLABEL + " DESC ",
                versionVersionableId, obsoleteStateList);

        return query;
    }

    // Returns the list of proxies of the document (can be only AV, or AV + WC)
    // in the given workspace
    public static String getDocProxiesInWorkspace(String[] docIds,
            String workspaceId) {

        String proxyTargetDocList = formatList(docIds);

        String query = String.format(
                "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 1 AND "
                        + NXQL.ECM_PROXY_TARGETID + " IN (%s) AND "
                        + NXQL.ECM_ANCESTORID + " = '%s'",
                proxyTargetDocList, workspaceId);

        return query;
    }

    /**
     * @param subjectUid
     * @param predicate
     * @return
     */
    public static String getRelatedDocsByPredicate(String subjectUid,
            String predicate) {
        return getRelatedDocsByPredicate(subjectUid, predicate, false);
    }

    /**
     * @param subjectUid
     * @param predicate
     * @param inverse
     * @return
     */
    public static String getRelatedDocsByPredicate(String subjectUid,
            String predicate, boolean inverse) {

        String selectClause = null;
        String parentField = null;
        if (inverse) {
            selectClause = "relation:source";
            parentField = "relation:target";
        } else {
            selectClause = "relation:target";
            parentField = "relation:source";
        }

        String query = String.format(
                "SELECT %s FROM Relation "
                        + "WHERE %s = '%s' AND relation:predicate = '%s'",
                selectClause, parentField, subjectUid, predicate);

        return query;
    }

    /**
     * @param bomListId
     * @param bomListUids
     * @return
     */
    public static String getBomListsByListId(String bomListId,
            List<String> bomListUids) {

        String bomListUidsFormatted = formatList(
                bomListUids.toArray(new String[0]));

        String query = String.format(
                "SELECT * FROM BomList WHERE ecm:uuid IN (%s) AND bomlst:bomList = '%s'",
                bomListUidsFormatted, bomListId);

        return query;
    }

    /**
     * @param bomListId
     * @param bomListUids
     * @return
     */
    public static String countBomListsByListId(String bomListId,
            List<String> bomListUids) {

        String bomListUidsFormatted = formatList(
                bomListUids.toArray(new String[0]));

        String query = String.format(
                "SELECT COUNT(" + NXQL.ECM_UUID + ") "
                        + "FROM BomList WHERE ecm:uuid IN (%s) AND bomlst:bomList = '%s'",
                bomListUidsFormatted, bomListId);

        return query;
    }

    public static String getStructureRootsForSelect() {

        String query = "SELECT " + NXQL.ECM_UUID + ", "
                + NuxeoMetadataConstants.NX_DC_TITLE + " FROM "
                + EloraDoctypeConstants.STRUCTURE_ROOT + " WHERE "
                + NXQL.ECM_ISPROXY + " = 0";

        return query;
    }

    /**
     * @param list
     * @return
     */
    private static String formatList(String[] list) {

        String formattedList = "";
        for (String item : list) {
            formattedList += "'" + item + "',";
        }
        if (!formattedList.equals("")) {
            formattedList = formattedList.substring(0,
                    formattedList.length() - 1);
        }
        return formattedList;
    }
}
