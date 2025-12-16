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

import java.util.List;

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * CM Query Factory class.
 *
 * @author aritu
 *
 */
public class CMQueryFactory {

    // -------------------------------------------------------------------------
    // Methods related to MODIFIED and IMPACTED ITEMS
    // -------------------------------------------------------------------------

    /**
     * Retrieves the modified items related to the specified change management
     * process.
     *
     * @param cmProcessUid Uid related to the CM process.
     * @return
     * @throws EloraException
     */
    public static String getModifiedItemsQuery(String cmProcessUid)
            throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/parentItem, " + pfx + "/*1/originItem, " + pfx
                + "/*1/originItemWc, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/destinationItemSourceVersion, " + pfx
                + "/*1/derivedFrom, " + pfx + "/*1/isManaged, " + pfx
                + "/*1/isManual, " + pfx + "/*1/type, " + pfx + "/*1/comment, "
                + pfx + "/*1/isUpdated, " + pfx + "/*1/includeInImpactMatrix "
                + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem IS NOT NULL "
                + " ORDER BY " + pfx + "/*1/rowNumber";

        return query;
    }

    public static String getModifiedItemsByDerivedFromQuery(String cmProcessUid,
            String derivedFrom) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/parentItem, " + pfx + "/*1/originItem, " + pfx
                + "/*1/originItemWc, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/destinationItemSourceVersion, " + pfx
                + "/*1/derivedFrom, " + pfx + "/*1/isManaged, " + pfx
                + "/*1/isManual, " + pfx + "/*1/type, " + pfx + "/*1/comment, "
                + pfx + "/*1/isUpdated, " + pfx + "/*1/includeInImpactMatrix "
                + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem IS NOT NULL";

        if (derivedFrom == null) {
            query += " AND " + pfx + "/*1/derivedFrom IS NULL ";
        } else {
            query += " AND " + pfx + "/*1/derivedFrom = '" + derivedFrom + "' ";
        }

        query += " ORDER BY " + pfx + "/*1/rowNumber";

        return query;
    }

    /**
     *
     * @param cmProcessUid
     * @param originUid
     * @return
     * @throws EloraException
     */
    public static String getCountModifiedItemsByOriginQuery(String cmProcessUid,
            String originUid) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem = '" + originUid + "'";

        return query;
    }

    public static String getCountImpactedItemsByOriginQuery(String cmProcessUid,
            String originUid, String itemType) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem = '" + originUid + "' AND " + pfx
                + "/*1/type = '" + itemType + "'";

        return query;
    }

    public static String getCountModifiedItemsByTypeQuery(String cmProcessUid,
            String type) {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/type = '" + type + "'";

        return query;
    }

    public static String getModifiedItemsMaxRowNumberQuery(String cmProcessUid)
            throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT MAX(" + pfx + "/*/rowNumber" + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "'";

        return query;
    }

    public static String getModifiedsManualItemsMaxRowNumberQuery(
            String cmProcessUid) throws EloraException {

        String pfx = CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST;

        String query = "SELECT MAX(" + pfx + "/*/rowNumber" + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "'";

        return query;
    }

    public static String getDistinctDerivedModifiedItemsByOriginListQuery(
            String cmProcessUid, List<String> originItemUids)
            throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String originList = EloraQueryHelper.formatList(originItemUids);

        String query = String.format("SELECT DISTINCT " + pfx
                + "/*1/originItem " + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/derivedFrom IN (%s)",
                originList);

        return query;
    }

    public static String getDistinctImpacteItemsActionsByOriginListQuery(
            String cmProcessUid, List<String> originItemUids)
            throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String originList = EloraQueryHelper.formatList(originItemUids);

        String query = String.format("SELECT DISTINCT " + pfx + "/*1/action "
                + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem IN (%s)",
                originList);

        return query;
    }

    public static String getDistinctImpactedItemsOriginsQuery(
            String cmProcessUid, String type) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = String.format("SELECT DISTINCT " + pfx
                + "/*1/originItem " + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/type = '" + type + "' ");

        return query;
    }

    public static String getCountImpactedItemsQuery(String cmProcessUid)
            throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND ("
                + pfx + "/*1/originItem IS NOT NULL OR " + pfx
                + "/*1/externalReference IS NOT NULL)";

        return query;
    }

    public static String getCountImpactedItemsByModifiedItemQuery(
            String cmProcessUid, String modifiedItemUid) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND ("
                + pfx + "/*1/originItem IS NOT NULL OR " + pfx
                + "/*1/externalReference IS NOT NULL) AND " + pfx
                + "/*1/modifiedItem = '" + modifiedItemUid + "'";

        return query;
    }

    public static String getCountManualImpactedItemsByModifiedItemQuery(
            String cmProcessUid, String modifiedItemUid) throws EloraException {

        String pfx = CMMetadataConstants.MANUAL_IMPACTED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/modifiedItem = '" + modifiedItemUid + "'";

        return query;
    }

    public static String getImpactedItemsByModifiedQuery(String cmProcessUid,
            String modifiedItemUid, String itemType, boolean processDeriveds)
            throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId,  " + pfx
                + "/*1/derivedFrom, " + pfx + "/*1/modifiedItem, " + pfx
                + "/*1/parentItem, " + pfx + "/*1/originItem, " + pfx
                + "/*1/originItemWc, " + pfx + "/*1/predicate, " + pfx
                + "/*1/quantity, " + pfx + "/*1/isAnarchic, " + pfx
                + "/*1/isDirectObject, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/destinationItemSourceVersion, " + pfx
                + "/*1/isManaged, " + pfx + "/*1/isManual, " + pfx
                + "/*1/type, " + pfx + "/*1/comment, " + pfx + "/*1/isUpdated, "
                + pfx + "/*1/isDerived, " + pfx + "/*1/isFromMbom, " + pfx
                + "/*1/externalReference, " + pfx + "/*1/externalName "
                + "FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/modifiedItem = '"
                + modifiedItemUid + "' AND " + pfx + "/*1/type = '" + itemType
                + "' AND (" + pfx + "/*1/originItem IS NOT NULL OR " + pfx
                + "/*1/externalReference IS NOT NULL) AND " + pfx
                + "/*1/refNodeId IS NULL ";

        if (!processDeriveds) {
            query += " AND " + pfx + "/*1/isDerived = 0 ";
        }

        query += " ORDER BY " + pfx + "/*1/isDerived , " + pfx
                + "/*1/parentNodeId ," + pfx + "/*1/rowNumber";

        return query;
    }

    public static String getCountDerivedImpactedItemsQuery(String cmProcessUid,
            String modifiedItemUid, String itemType) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") " + "FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/modifiedItem = '" + modifiedItemUid + "' AND "
                + pfx + "/*1/type = '" + itemType + "' AND " + pfx
                + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/refNodeId IS NULL AND " + pfx + "/*1/isDerived = 1 ";

        return query;
    }

    public static String getProcessesByModifiedItemOriginQuery(String docUid)
            throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemDestinationQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemOriginWcQuery(String docUid)
            throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItemWc = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemDestinationWcQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItemWc = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByImpactedItemOriginQuery(String docUid)
            throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByImpactedItemDestinationQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByImpactedItemOriginWcQuery(String docUid)
            throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItemWc = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByImpactedItemDestinationWcQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                + CMDoctypeConstants.CM_ECO + ", " + CMDoctypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDoctypeConstants.CM_ECO + "', '" + CMDoctypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItemWc = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is a modified item
    // origin
    public static String getProcessDocumentModelsByModifiedItemOriginQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT * FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + pfx
                + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is a modified
    // destination origin
    public static String getProcessDocumentModelsByModifiedItemDestinationQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

        String query = "SELECT * FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + pfx
                + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is an impacted item
    // origin
    public static String getProcessDocumentModelsByImpactedItemOriginQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT * FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + pfx
                + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is an impacted item
    // destination
    public static String getProcessDocumentModelsByImpactedItemDestinationQuery(
            String docUid) throws EloraException {

        String pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

        String query = "SELECT * FROM " + CMDoctypeConstants.CM_ECO + ", "
                + CMDoctypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDoctypeConstants.CM_ECO + "', '"
                + CMDoctypeConstants.CM_ECR + "') AND " + pfx
                + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    public static String getCountItemsByItemClassTypeAndManagedQuery(
            String cmProcessUid, String itemClass, int isManaged)
            throws EloraException {

        String pfx;
        String query = null;
        if (itemClass.equals(CMConstants.ITEM_CLASS_IMPACTED)) {
            pfx = CMMetadataConstants.IMPACTED_ITEM_LIST;

            query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                    + CMDoctypeConstants.CM_ECO + ", "
                    + CMDoctypeConstants.CM_ECR + " WHERE "
                    + NXQL.ECM_PRIMARYTYPE + " IN ('"
                    + CMDoctypeConstants.CM_ECO + "', '"
                    + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID
                    + " = '" + cmProcessUid + "' AND " + pfx
                    + "/*1/originItem IS NOT NULL AND " + pfx
                    + "/*1/isManaged = " + isManaged + " AND " + pfx
                    + "/*1/refNodeId IS NULL ";
        } else {
            pfx = CMMetadataConstants.MODIFIED_ITEM_LIST;

            query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                    + CMDoctypeConstants.CM_ECO + ", "
                    + CMDoctypeConstants.CM_ECR + " WHERE "
                    + NXQL.ECM_PRIMARYTYPE + " IN ('"
                    + CMDoctypeConstants.CM_ECO + "', '"
                    + CMDoctypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID
                    + " = '" + cmProcessUid + "' AND " + pfx
                    + "/*1/originItem IS NOT NULL AND " + pfx
                    + "/*1/isManaged = " + isManaged;
        }

        return query;
    }

}
