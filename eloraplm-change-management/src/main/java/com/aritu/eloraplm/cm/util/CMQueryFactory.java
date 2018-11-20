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

import com.aritu.eloraplm.constants.CMDocTypeConstants;
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
     */
    public static String getModifiedItemsQuery(String cmProcessUid,
            String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/derivedFrom, " + pfx + "/*1/parentItem, " + pfx
                + "/*1/originItem, " + pfx + "/*1/originItemWc, " + pfx
                + "/*1/action, " + pfx + "/*1/destinationItem, " + pfx
                + "/*1/destinationItemWc, " + pfx + "/*1/isManaged, " + pfx
                + "/*1/isManual, " + pfx + "/*1/type, " + pfx + "/*1/comment, "
                + pfx + "/*1/isUpdated, " + pfx + "/*1/includeInImpactMatrix "
                + "FROM " + CMDocTypeConstants.CM_ECO + ", "
                + CMDocTypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDocTypeConstants.CM_ECO + "', '"
                + CMDocTypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem IS NOT NULL "
                + " ORDER BY " + pfx + "/*1/rowNumber";

        return query;
    }

    /**
     *
     * @param cmProcessUid
     * @param originUid
     * @return
     */
    public static String getCountModifiedItemsByOriginQuery(String cmProcessUid,
            String originUid, String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem = '" + originUid + "'";

        return query;
    }

    public static String getModifiedItemsMaxRowNumberQuery(String cmProcessUid,
            String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT MAX(" + pfx + "/*/rowNumber" + ") FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "'";

        return query;
    }

    public static String getDistinctDerivedModifiedItemsByOriginListQuery(
            String cmProcessUid, String itemType, List<String> originItemUids) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String originList = EloraQueryHelper.formatList(originItemUids);

        String query = String.format("SELECT DISTINCT " + pfx
                + "/*1/originItem " + "FROM " + CMDocTypeConstants.CM_ECO + ", "
                + CMDocTypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDocTypeConstants.CM_ECO + "', '"
                + CMDocTypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/derivedFrom IN (%s)",
                originList);

        return query;
    }

    public static String getCountImpactedItemsQuery(String cmProcessUid,
            String itemType) {

        String pfx = CMHelper.geImpactedItemListPrefix(itemType);

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem IS NOT NULL";

        return query;
    }

    public static String getCountImpactedItemsByModifiedItemQuery(
            String cmProcessUid, String itemType, String modifiedItemUid) {

        String pfx = CMHelper.geImpactedItemListPrefix(itemType);

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + NXQL.ECM_UUID + " = '" + cmProcessUid + "' AND "
                + pfx + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/modifiedItem = '" + modifiedItemUid + "'";

        return query;
    }

    public static String getImpactedItemsByModifiedQuery(String cmProcessUid,
            String itemType, String modifiedItemUid) {

        String pfx = CMHelper.geImpactedItemListPrefix(itemType);

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/modifiedItem, " + pfx + "/*1/parentItem, " + pfx
                + "/*1/originItem, " + pfx + "/*1/originItemWc, " + pfx
                + "/*1/predicate, " + pfx + "/*1/quantity, " + pfx
                + "/*1/isAnarchic, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/isManaged, " + pfx + "/*1/isManual, " + pfx
                + "/*1/type, " + pfx + "/*1/comment, " + pfx + "/*1/isUpdated "
                + "FROM " + CMDocTypeConstants.CM_ECO + ", "
                + CMDocTypeConstants.CM_ECR + " WHERE " + NXQL.ECM_PRIMARYTYPE
                + " IN ('" + CMDocTypeConstants.CM_ECO + "', '"
                + CMDocTypeConstants.CM_ECR + "') AND " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/modifiedItem = '"
                + modifiedItemUid + "'AND " + pfx
                + "/*1/originItem IS NOT NULL ORDER BY " + pfx
                + "/*1/parentNodeId ," + pfx + "/*1/rowNumber";

        return query;
    }

    public static String getProcessesByModifiedItemOriginQuery(String docUid,
            String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT " + NXQL.ECM_UUID + " FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemDestinationQuery(
            String docUid, String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT " + NXQL.ECM_UUID + " FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemOriginWcQuery(String docUid,
            String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT " + NXQL.ECM_UUID + " FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/originItemWc = '" + docUid + "'";

        return query;
    }

    public static String getProcessesByModifiedItemDestinationWcQuery(
            String docUid, String itemType) {

        String pfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT " + NXQL.ECM_UUID + " FROM "
                + CMDocTypeConstants.CM_ECO + ", " + CMDocTypeConstants.CM_ECR
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + CMDocTypeConstants.CM_ECO + "', '" + CMDocTypeConstants.CM_ECR
                + "') AND " + pfx + "/*1/destinationItemWc = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is a modified item
    // origin
    public static String getProcessDocumentModelsByModifiedItemOriginQuery(
            String processType, String docUid, String itemType) {

        String modifiedItemPfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT * FROM " + processType + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + processType + "' AND "
                + modifiedItemPfx + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is a modified
    // destination origin
    public static String getProcessDocumentModelsByModifiedItemDestinationQuery(
            String processType, String docUid, String itemType) {

        String modifiedItemPfx = CMHelper.getModifiedItemListPrefix(itemType);

        String query = "SELECT * FROM " + processType + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + processType + "' AND "
                + modifiedItemPfx + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is an impacted item
    // origin
    public static String getProcessDocumentModelsByImpactedItemOriginQuery(
            String processType, String docUid, String itemType) {

        String impactedItemPfx = CMHelper.geImpactedItemListPrefix(itemType);

        String query = "SELECT * FROM " + processType + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + processType + "' AND "
                + impactedItemPfx + "/*1/originItem = '" + docUid + "'";

        return query;
    }

    // Query for retrieving the processes where a document is an impacted item
    // destination
    public static String getProcessDocumentModelsByImpactedItemDestinationQuery(
            String processType, String docUid, String itemType) {

        String impactedItemPfx = CMHelper.geImpactedItemListPrefix(itemType);

        String query = "SELECT * FROM " + processType + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + processType + "' AND "
                + impactedItemPfx + "/*1/destinationItem = '" + docUid + "'";

        return query;
    }

}
