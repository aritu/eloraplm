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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Queries related with Bom Characteristics.
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

    public static String getBomCharacteristicInfoQuery(String documentId,
            String bomCharacteristicId) {

        String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

        String query = "SELECT " + NXQL.ECM_UUID + ", " + pfx
                + "/*1/classification, " + pfx + "/*1/order, " + pfx
                + "/*1/bomCharacMaster, " + pfx
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
                + documentId + "' AND " + pfx + "/*1/bomCharacteristicId = '"
                + bomCharacteristicId + "'";

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

    public static long countCharacteristicsShownInReport(CoreSession session,
            String documentType, String documentId) {
        return countCharacteristics(session, documentType, documentId, 1);
    }

    public static long countCharacteristics(CoreSession session,
            String documentType, String documentId, int showInReport) {

        String query = String.format(
                "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM %s WHERE "
                        + NXQL.ECM_PRIMARYTYPE + " = '%s' AND " + NXQL.ECM_UUID
                        + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST
                        + "/*/showInReport = %d",
                documentType, documentType, documentId, showInReport);

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    // -----------------------------------------------------------------
    // Linked Characteristics
    // -----------------------------------------------------------------
    public static long countLinkedCharacteristicsShownInReport(
            CoreSession session, String documentType, String documentId) {
        return countLinkedCharacteristics(session, documentType, documentId, 1);
    }

    public static long countLinkedCharacteristics(CoreSession session,
            String documentType, String documentId, int showInReport) {

        String query = String.format(
                "SELECT COUNT(" + NXQL.ECM_UUID + ") FROM %s WHERE "
                        + NXQL.ECM_PRIMARYTYPE + " = '%s' AND " + NXQL.ECM_UUID
                        + " = '%s' AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST
                        + "/*/showInReport = %d",
                documentType, documentType, documentId, showInReport);

        return EloraQueryHelper.executeCountQuery(query, NXQL.ECM_UUID,
                session);
    }

    public static String getDocsHavingLinkedCharacByLinkedDocQuery(
            String linkedDocumentId) {

        String pfx = BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST;

        String query = "SELECT DISTINCT " + NXQL.ECM_UUID
                + " FROM Document WHERE " + pfx + "/*1/linkedDocument = '"
                + linkedDocumentId + "' AND " + NXQL.ECM_ISPROXY + " = 0";

        return query;
    }

}
