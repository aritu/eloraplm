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

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;

/**
 * CM Query Factory class.
 *
 * @author aritu
 *
 */
public class IntegrationCmQueryFactory {

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
    public static String getCmEcoRootItemsQuery(String cmProcessUid) {

        String pfx = CMMetadataConstants.DOC_MODIFIED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/originItem, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/isManaged, " + pfx + "/*1/comment "
                + "FROM CmEco, CmEcr WHERE ecm:uuid = '" + cmProcessUid
                + "' AND " + pfx + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/action <> '" + CMConstants.ACTION_IGNORE + "' ORDER BY "
                + pfx + "/*1/rowNumber";

        return query;
    }

    public static String getCmEcoSubitemsQuery(String cmProcessUid) {

        String pfx = CMMetadataConstants.DOC_IMPACTED_ITEM_LIST;

        String query = "SELECT " + pfx + "/*1/rowNumber, " + pfx
                + "/*1/nodeId, " + pfx + "/*1/parentNodeId, " + pfx
                + "/*1/modifiedItem, " + pfx + "/*1/parentItem, " + pfx
                + "/*1/originItem, " + pfx + "/*1/action, " + pfx
                + "/*1/destinationItem, " + pfx + "/*1/destinationItemWc, "
                + pfx + "/*1/isManaged, " + pfx + "/*1/comment "
                + "FROM CmEco, CmEcr WHERE ecm:uuid = '" + cmProcessUid
                + "' AND " + pfx + "/*1/originItem IS NOT NULL AND " + pfx
                + "/*1/action <> '" + CMConstants.ACTION_IGNORE + "' ORDER BY "
                + pfx + "/*1/rowNumber";

        return query;
    }

}
