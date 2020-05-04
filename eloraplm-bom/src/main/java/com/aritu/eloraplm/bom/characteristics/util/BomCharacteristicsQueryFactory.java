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
package com.aritu.eloraplm.bom.characteristics.util;

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomCharacteristicsQueryFactory {

    public static String getBomCharacteristicsByDocumentQuery(
            String documentId) {

        String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

        String query = "SELECT " + NXQL.ECM_UUID + ", " + pfx
                + "/*1/bomCharacteristicId, " + pfx + "/*1/classification, "
                + pfx + "/*1/order, " + pfx + "/*1/bomCharacMaster, " + pfx
                + "/*1/bomCharacMasterLastModified, " + pfx + "/*1/title, "
                + pfx + "/*1/description, " + pfx + "/*1/type, " + pfx
                + "/*1/numberMaxIntegerPlaces, " + pfx
                + "/*1/numberMaxDecimalPlaces, " + pfx
                + "/*1/numberDefaultValue, " + pfx + "/*1/numberValue, " + pfx
                + "/*1/stringMaxLength, " + pfx + "/*1/stringDefaultValue, "
                + pfx + "/*1/stringValue, " + pfx + "/*1/dateDefaultValue, "
                + pfx + "/*1/dateValue, " + pfx + "/*1/booleanDefaultValue, "
                + pfx + "/*1/booleanValue, " + pfx + "/*1/listDefaultValue, "
                + pfx + "/*1/listValue, " + pfx + "/*1/unit, " + pfx
                + "/*1/showInReport, " + pfx + "/*1/orderInReport, " + pfx
                + "/*1/required, " + pfx + "/*1/includeInTitle, " + pfx
                + "/*1/unmodifiable, " + pfx + "/*1/messageType, " + pfx
                + "/*1/message " + "FROM Document WHERE ecm:uuid = '"
                + documentId + "' AND " + pfx + "/*1/type IS NOT NULL "
                + " ORDER BY " + pfx + "/*1/classification, " + pfx
                + "/*1/order";

        return query;
    }

    public static String getBomCharacteristicListContentQuery(String documentId,
            String bomCharacteristicId) {

        String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

        String query = "SELECT " + pfx + "/*1/listContent/*2/listValue, " + pfx
                + "/*1/listContent/*2/listOrder " + "FROM Document "
                + " WHERE ecm:uuid = '" + documentId + "' AND " + pfx
                + "/*1/bomCharacteristicId = '" + bomCharacteristicId
                + "'  ORDER BY " + pfx + "/*1/listContent/*2/listOrder, " + pfx
                + "/*1/listContent/*2/listValue";

        return query;
    }

}
