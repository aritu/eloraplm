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
package com.aritu.eloraplm.bom.util;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.OEMConstants;

/**
 * BOM Query Factory class.
 *
 * @author aritu
 *
 */
public class BomQueryFactory {

    public static String getCountOemByManufacturerAndPartNumberQuery(
            DocumentModel doc, String manufacturer, String partNumber) {

        String query = "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM "
                + doc.getType() + " WHERE " + NXQL.ECM_UUID + " = '"
                + doc.getId() + "' AND " + EloraMetadataConstants.ELORA_OEM_OEMS
                + "/*1/manufacturer" + " = '" + manufacturer + "' AND "
                + EloraMetadataConstants.ELORA_OEM_OEMS + "/*1/partNumber"
                + " = '" + partNumber + "'";

        return query;
    }

    public static String getMaxPreferenceInOemsListQuery(DocumentModel doc) {

        String query = "SELECT MAX(" + EloraMetadataConstants.ELORA_OEM_OEMS
                + "/*/preference" + ") FROM " + doc.getType() + " WHERE "
                + NXQL.ECM_UUID + " = '" + doc.getId() + "'";

        return query;
    }

    /**
     * Retrieve the OME list of the given document, ordered by Preference
     *
     * @param doc
     * @return
     */
    public static String getOemsListQuery(DocumentModel doc) {
        String query = "SELECT " + EloraMetadataConstants.ELORA_OEM_OEMS
                + "/*1/" + OEMConstants.MANUFACTURER + ", "
                + EloraMetadataConstants.ELORA_OEM_OEMS + "/*1/"
                + OEMConstants.PART_NUMBER + ", "
                + EloraMetadataConstants.ELORA_OEM_OEMS + "/*1/"
                + OEMConstants.PREFERENCE + " FROM " + doc.getType() + " WHERE "
                + NXQL.ECM_UUID + " = '" + doc.getId() + "' ORDER BY "
                + EloraMetadataConstants.ELORA_OEM_OEMS + "/*1/"
                + OEMConstants.PREFERENCE;
        return query;
    }

}
