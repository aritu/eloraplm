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

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;

/**
 * // TODO: write class general comment
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
    public static String getModifiedItemsQuery(String cmProcessUid) {

        String pfx = CMMetadataConstants.MOD_MODIFIED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/originItem, " + pfx + "/*1/originItemWc, " + pfx
                + "/*1/action, " + pfx + "/*1/destinationItem, " + pfx
                + "/*1/isManaged, " + pfx + "/*1/type "
                + "FROM CmEco, CmEcr WHERE ecm:uuid = '" + cmProcessUid
                + "' AND " + pfx + "/*1/originItem IS NOT NULL " + " ORDER BY "
                + pfx + "/*1/rowNumber";

        return query;
    }

    /**
     *
     * @param cmProcessUid
     * @param originUid
     * @return
     */
    public static String getCountModifiedItemsByOriginQuery(String cmProcessUid,
            String originUid) {

        String pfx = CMMetadataConstants.MOD_MODIFIED_ITEM_LIST;

        String query = "SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM Document WHERE " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem = '"
                + originUid + "'";

        return query;
    }

    public static String getModifiedItemsMaxRowNumberQuery(
            String cmProcessUid) {

        String pfx = CMMetadataConstants.MOD_MODIFIED_ITEM_LIST;

        String query = "SELECT MAX(" + pfx + "/*/rowNumber"
                + ") FROM Document WHERE " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "'";

        return query;
    }

    public static String getCountImpactedItemsQuery(String cmProcessUid,
            String itemType) {

        String pfx = "";
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
        } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            pfx = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
        }

        String query = "SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM Document WHERE " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx + "/*1/originItem IS NOT NULL";

        return query;
    }

    public static String getCountImpactedItemsByModifiedItemQuery(
            String cmProcessUid, String itemType, String modifiedItemUid) {

        String pfx = "";
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
        } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            pfx = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
        }

        String query = "SELECT COUNT(" + NXQL.ECM_UUID
                + ") FROM Document WHERE " + NXQL.ECM_UUID + " = '"
                + cmProcessUid + "' AND " + pfx
                + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/modifiedItem = '" + modifiedItemUid + "'";

        return query;
    }

    public static String getImpactedItemsByParentQuery(String cmProcessUid,
            String itemType, String modifiedItemUid, String parentItemUid) {

        String pfx = "";
        if (itemType.equals(CMConstants.ITEM_TYPE_DOC)) {
            pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;
        } else if (itemType.equals(CMConstants.ITEM_TYPE_BOM)) {
            pfx = CMMetadataConstants.BOM_IMPACTED_ITEM_LIST;
        }

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/modifiedItem, " + pfx + "/*1/parentItem, " + pfx
                + "/*1/originItem, " + pfx + "/*1/originItemWc, " + pfx
                + "/*1/action, " + pfx + "/*1/destinationItem, " + pfx
                + "/*1/isManaged, " + pfx + "/*1/isManual, " + pfx
                + "/*1/type, " + pfx + "/*1/messageType, " + pfx
                + "/*1/messageData " + "FROM CmEco, CmEcr WHERE ecm:uuid = '"
                + cmProcessUid + "' AND " + pfx
                + "/*1/originItem IS NOT NULL AND " + pfx + "/*1/parentItem = '"
                + parentItemUid + "' AND " + pfx + "/*1/modifiedItem = '"
                + modifiedItemUid + "' ORDER BY " + pfx + "/*1/rowNumber";

        return query;
    }

}
