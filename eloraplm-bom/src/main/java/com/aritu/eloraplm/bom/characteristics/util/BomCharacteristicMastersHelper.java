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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;

import com.aritu.eloraplm.bom.characteristics.BomCharacteristic;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsDoctypeConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Elora Bom Characteristics.
 *
 * @author aritu
 *
 */
public class BomCharacteristicMastersHelper {

    private static final Log log = LogFactory.getLog(
            BomCharacteristicMastersHelper.class);

    public static String getBomCharacteristicMasterDocTypeForAction(
            String action) {

        /*String logInitMsg = "[getBomCharacteristicDocTypeForAction] ";
        log.trace(logInitMsg + "--- ENTER --- action = |" + action + "|");*/

        String bomCharacteristicDocType = "";

        switch (action) {
        case BomCharacteristicsConstants.ADMIN_BOM_PART_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PART_CHARAC_MASTER_DOCUMENT_TYPE;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_PRODUCT_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENT_TYPE;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_TOOL_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENT_TYPE;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_PACKAGING_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENT_TYPE;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_SPECIFICATION_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENT_TYPE;
            break;
        }

        /*log.trace(logInitMsg + "--- EXIT --- with bomCharacteristicDocType = |"
                + bomCharacteristicDocType + "|");*/

        return bomCharacteristicDocType;
    }

    public static String getBomCharacteristicMasterDocTypeForBomType(
            String bomTypeName) {

        /*String logInitMsg = "[getBomCharacteristicMasterDocTypeForBomType] ";
        log.trace(logInitMsg + "--- ENTER --- bomType = |" + bomType + "|");*/

        DocumentType bomType = EloraDocumentTypesHelper.getDocumentType(
                bomTypeName);

        String bomCharacteristicDocType = "";
        if (EloraDocumentTypesHelper.getDocumentType(
                EloraDoctypeConstants.BOM_PART).isSuperTypeOf(bomType)) {
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PART_CHARAC_MASTER_DOCUMENT_TYPE;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_PRODUCT)) {
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENT_TYPE;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_TOOL)) {
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENT_TYPE;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_PACKAGING)) {
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENT_TYPE;
        } else if (bomTypeName.equals(
                EloraDoctypeConstants.BOM_SPECIFICATION)) {
            bomCharacteristicDocType = BomCharacteristicsDoctypeConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENT_TYPE;
        }

        /*log.trace(logInitMsg + "--- EXIT --- with bomCharacteristicDocType = |"
                + bomCharacteristicDocType + "|");*/

        return bomCharacteristicDocType;
    }

    public static String getBomTypeForBomCharacteristicMasterDocType(
            String bomCharacteristicDocType) {

        /* String logInitMsg = "[getBomTypeForBomCharacteristicMasterDocType] ";
        log.trace(logInitMsg + "--- ENTER --- bomCharacteristicDocType = |"
                + bomCharacteristicDocType + "|");*/

        String bomType = "";

        switch (bomCharacteristicDocType) {
        case BomCharacteristicsDoctypeConstants.BOM_PART_CHARAC_MASTER_DOCUMENT_TYPE:
            bomType = EloraDoctypeConstants.BOM_PART;
            break;
        case BomCharacteristicsDoctypeConstants.BOM_PRODUCT_CHARAC_MASTER_DOCUMENT_TYPE:
            bomType = EloraDoctypeConstants.BOM_PRODUCT;
            break;
        case BomCharacteristicsDoctypeConstants.BOM_TOOL_CHARAC_MASTER_DOCUMENT_TYPE:
            bomType = EloraDoctypeConstants.BOM_TOOL;
            break;
        case BomCharacteristicsDoctypeConstants.BOM_PACKAGING_CHARAC_MASTER_DOCUMENT_TYPE:
            bomType = EloraDoctypeConstants.BOM_PACKAGING;
            break;
        case BomCharacteristicsDoctypeConstants.BOM_SPECIFICATION_CHARAC_MASTER_DOCUMENT_TYPE:
            bomType = EloraDoctypeConstants.BOM_SPECIFICATION;
            break;
        }

        /*log.trace(logInitMsg + "--- EXIT --- with bomType = |" + bomType + "|");*/

        return bomType;
    }

    public static String getBomCharacteristicMasterContentViewForAction(
            String action) {

        /*String logInitMsg = "[getBomCharacteristicContentViewForAction] ";
        log.trace(
                logInitMsg + "--- ENTER --- currentAction = |" + action + "|");*/

        String bomCharacteristicContentView = "";

        switch (action) {
        case BomCharacteristicsConstants.ADMIN_BOM_PART_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicContentView = BomCharacteristicsConstants.BOM_PART_CHARAC_MASTER_CONTENT_VIEW;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_PRODUCT_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicContentView = BomCharacteristicsConstants.BOM_PRODUCT_CHARAC_MASTER_CONTENT_VIEW;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_TOOL_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicContentView = BomCharacteristicsConstants.BOM_TOOL_CHARAC_MASTER_CONTENT_VIEW;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_PACKAGING_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicContentView = BomCharacteristicsConstants.BOM_PACKAGING_CHARAC_MASTER_CONTENT_VIEW;
            break;
        case BomCharacteristicsConstants.ADMIN_BOM_SPECIFICATION_CHARAC_MASTERS_ACTION_ID:
            bomCharacteristicContentView = BomCharacteristicsConstants.BOM_SPECIFICATION_CHARAC_MASTER_CONTENT_VIEW;
            break;
        }

        /*log.trace(logInitMsg
                + "--- EXIT --- with bomCharacteristicContentView = |"
                + bomCharacteristicContentView + "|");*/

        return bomCharacteristicContentView;
    }

    public static String getBomCharacteristicMasterType(
            DocumentModel bomCharacteristicMaster) throws EloraException {

        String logInitMsg = "[getBomCharacteristicMasterType] ";
        /* log.trace(logInitMsg + "--- ENTER --- bomCharacteristisMasterId = |"
                + bomCharacteristicMaster.getId() + "|");*/

        String bomCharacMasterType = "";

        if (bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_TYPE) != null
                && !bomCharacteristicMaster.getPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_TYPE).toString().isEmpty()) {

            bomCharacMasterType = bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_TYPE).toString();

        } else {
            throw new EloraException(
                    "type attribute is missing in Bom Characteristic Master. bomCharacteristisMasterId = |"
                            + bomCharacteristicMaster.getId() + "|");
        }

        /* log.trace(logInitMsg + "--- EXIT --- with type = |"
                + bomCharacMasterType + "|");*/

        return bomCharacMasterType;
    }

    public static BomCharacteristic createBomCharacteristicFromMaster(
            DocumentModel bomCharacteristicMaster) throws EloraException {

        String bomCharacteristicId = BomCharacteristicsHelper.generateBomCharacteristicId();

        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) bomCharacteristicMaster.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            // Taken from Nuxeo code:
            // -------------------------------------------------
            // remove milliseconds as they are not stored in some
            // databases, which could make the comparison fail just
            // after a document creation (see NXP-8783)
            // -------------------------------------------------
            lastModifiedGc.set(Calendar.MILLISECOND, 0);
            lastModified = lastModifiedGc.getTime();
        }

        BomCharacteristic bomCharacteristic = new BomCharacteristic(
                bomCharacteristicId, bomCharacteristicMaster.getId(),
                lastModified);

        String classification = (String) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_CLASSIFICATION);
        bomCharacteristic.setClassification(classification);

        Long order = (Long) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_ORDER);
        bomCharacteristic.setOrder(order);

        String title = (String) bomCharacteristicMaster.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE);
        bomCharacteristic.setTitle(title);

        String description = (String) bomCharacteristicMaster.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION);
        bomCharacteristic.setDescription(description);

        String type = BomCharacteristicMastersHelper.getBomCharacteristicMasterType(
                bomCharacteristicMaster);
        bomCharacteristic.setType(type);

        BomCharacteristicMastersHelper.fillBomCharacteristicValuesFromMaster(
                bomCharacteristicMaster, type, bomCharacteristic);

        String unit = (String) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_UNIT);
        bomCharacteristic.setUnit(unit);

        // Initialize showInReport with the master value
        Boolean showInReport = (Boolean) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_SHOW_IN_REPORT);
        bomCharacteristic.setShowInReport(showInReport);

        Boolean required = (Boolean) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_REQUIRED);
        bomCharacteristic.setRequired(required);

        Boolean includeInTitle = (Boolean) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_INCLUDE_IN_TITLE);
        bomCharacteristic.setIncludeInTitle(includeInTitle);

        Boolean unmodifiable = (Boolean) bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_UNMODIFIABLE);
        bomCharacteristic.setUnmodifiable(unmodifiable);

        return bomCharacteristic;
    }

    private static void fillBomCharacteristicValuesFromMaster(
            DocumentModel bomCharacteristicMaster,
            String bomCharacteristicMasterType,
            BomCharacteristic bomCharacteristic) throws EloraException {

        switch (bomCharacteristicMasterType) {
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
            Long numberMaxIntegerPlaces = (Long) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_MAX_INTEGER_PLACES);
            bomCharacteristic.setNumberMaxIntegerPlaces(numberMaxIntegerPlaces);

            Long numberMaxDecimalPlaces = (Long) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_MAX_DECIMAL_PLACES);
            bomCharacteristic.setNumberMaxDecimalPlaces(numberMaxDecimalPlaces);

            String numberDefaultValue = (String) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_NUMBER_DEFAULT_VALUE);
            if (numberDefaultValue != null) {
                bomCharacteristic.setNumberDefaultValue(numberDefaultValue);
                bomCharacteristic.setNumberValue(numberDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
            Long stringMaxLength = (Long) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_STRING_MAX_LENGTH);
            bomCharacteristic.setStringMaxLength(stringMaxLength);

            String stringDefaultValue = (String) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_STRING_DEFAULT_VALUE);
            if (stringDefaultValue != null) {
                bomCharacteristic.setStringDefaultValue(stringDefaultValue);
                bomCharacteristic.setStringValue(stringDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
            GregorianCalendar cal = (GregorianCalendar) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_DATE_DEFAULT_VALUE);
            if (cal != null) {
                Date dateDefaultValue = cal.getTime();
                bomCharacteristic.setDateDefaultValue(dateDefaultValue);
                bomCharacteristic.setDateValue(dateDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
            Boolean booleanDefaultValue = (Boolean) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_BOOLEAN_DEFAULT_VALUE);
            if (booleanDefaultValue != null) {
                bomCharacteristic.setBooleanDefaultValue(booleanDefaultValue);
                bomCharacteristic.setBooleanValue(booleanDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
            List<Map<String, String>> listContent = getBomCharacteristicListContentFromMaster(
                    bomCharacteristicMaster);
            if (listContent != null) {
                bomCharacteristic.setListContent(listContent);
            }
            String listDefaultValue = (String) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_LIST_DEFAULT_VALUE);
            if (listDefaultValue != null) {
                bomCharacteristic.setListDefaultValue(listDefaultValue);
                bomCharacteristic.setListValue(listDefaultValue);
            }
            break;
        }

    }

    public static List<Map<String, String>> getBomCharacteristicListContentFromMaster(
            DocumentModel bomCharacteristicMaster) throws EloraException {

        String logInitMsg = "[getBomCharacteristicListContentFromMaster] ";
        log.trace(logInitMsg + "--- ENTER --- bomCharacteristisMasterId = |"
                + bomCharacteristicMaster.getId() + "|");

        // Retrieve the Master List Content sorted in function of the listOrder
        ArrayList<HashMap<String, Object>> masterListContent = new ArrayList<HashMap<String, Object>>();
        Map<Long, String> sortedMasterListContent = new TreeMap<Long, String>();
        if (bomCharacteristicMaster.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_LIST_CONTENT) != null) {
            masterListContent = (ArrayList<HashMap<String, Object>>) bomCharacteristicMaster.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_LIST_CONTENT);
            for (int i = 0; i < masterListContent.size(); ++i) {
                HashMap<String, Object> listElement = masterListContent.get(i);
                long listOrder = (Long) listElement.get("listOrder");
                String listValue = (String) listElement.get("listValue");
                sortedMasterListContent.put(listOrder, listValue);
            }
        }

        List<Map<String, String>> listContent = new ArrayList<Map<String, String>>();

        for (Map.Entry<Long, String> entry : sortedMasterListContent.entrySet()) {
            Map<String, String> listContentEntry = new LinkedHashMap<String, String>();
            listContentEntry.put("listOrder", entry.getKey().toString());
            listContentEntry.put("listValue", entry.getValue());
            listContent.add(listContentEntry);
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return listContent;
    }

    public static int countBomCharacteristicMasters(CoreSession session)
            throws EloraException {

        String logInitMsg = "[countBomCharacteristicMasters] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        int resultCount = 0;

        IterableQueryResult queryResult = null;
        try {
            String query = BomCharacteristicMastersQueryFactory.getCountBomCharacteristicMastersQuery();

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                String resultCountStr = map.get(
                        "COUNT(" + NXQL.ECM_UUID + ")").toString();
                resultCount = Integer.valueOf(resultCountStr);

                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }

        log.trace(logInitMsg + "--- EXIT --- with resultCount = |" + resultCount
                + "|");

        return resultCount;
    }

}
