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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;

import com.aritu.eloraplm.bom.characteristics.BomCharacteristic;
import com.aritu.eloraplm.bom.util.BomHelper;
import com.aritu.eloraplm.config.util.BomConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.ibm.icu.text.DateFormat;
import com.sun.faces.util.MessageFactory;

/**
 * Helper class for BOM Characteristics.
 *
 * @author aritu
 *
 */
public class BomCharacteristicsHelper {

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsHelper.class);

    public static void saveBomCharacteristicsChangesInDocument(
            CoreSession session, DocumentModel document,
            List<String> bomCharacteristicIdsToBeRemoved,
            List<BomCharacteristic> bomCharacteristicsToBeAdded,
            HashMap<String, BomCharacteristic> changedBomCharacteristics)
            throws EloraException {

        String logInitMsg = "[saveBomCharacteristicsChangesInDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO:: TO BE REMOVED
        /*
         * // Check specified input parameters if (document == null) {
         * log.error(logInitMsg + " Specified document is null."); throw new
         * EloraException("Specified document is null."); }
         *
         * if (bomCharacteristics == null) { log.error(logInitMsg +
         * " Specified bomCharacteristics is null."); throw new
         * EloraException("Specified bomCharacteristics is null."); }
         *
         * log.trace(logInitMsg + " Saving |" + bomCharacteristics.size() +
         * "| Bom Characteristics.");
         */

        try {
            if ((bomCharacteristicIdsToBeRemoved != null
                    && bomCharacteristicIdsToBeRemoved.size() > 0)
                    || (bomCharacteristicsToBeAdded != null
                            && bomCharacteristicsToBeAdded.size() > 0
                            || (changedBomCharacteristics != null
                                    && changedBomCharacteristics.size() > 0))) {

                ArrayList<HashMap<String, Object>> currentBomCharacteristicsContent = new ArrayList<HashMap<String, Object>>();
                if (document.getPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
                    currentBomCharacteristicsContent = (ArrayList<HashMap<String, Object>>) document.getPropertyValue(
                            BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);
                }
                ArrayList<HashMap<String, Object>> newBomCharacteristicsContent = new ArrayList<HashMap<String, Object>>();

                // -----------------------------------------------------
                // First process modified or removed BOM Characteristics
                // -----------------------------------------------------
                if ((bomCharacteristicIdsToBeRemoved != null
                        && bomCharacteristicIdsToBeRemoved.size() > 0)
                        || (changedBomCharacteristics != null
                                && changedBomCharacteristics.size() > 0)) {

                    for (int i = 0; i < currentBomCharacteristicsContent.size(); ++i) {
                        HashMap<String, Object> bomCharacteristic = currentBomCharacteristicsContent.get(
                                i);
                        String bomCharacteristicId = (String) bomCharacteristic.get(
                                "bomCharacteristicId");
                        if (changedBomCharacteristics != null
                                && changedBomCharacteristics.containsKey(
                                        bomCharacteristicId)) {
                            BomCharacteristic changedBomCharacteristic = changedBomCharacteristics.get(
                                    bomCharacteristicId);
                            updateBomCharacteristicType(bomCharacteristic,
                                    changedBomCharacteristic);
                            newBomCharacteristicsContent.add(bomCharacteristic);
                        } else if (bomCharacteristicIdsToBeRemoved != null
                                && bomCharacteristicIdsToBeRemoved.contains(
                                        bomCharacteristicId)) {
                            // not include it in this case since it is
                            // removed
                        } else {
                            newBomCharacteristicsContent.add(bomCharacteristic);
                        }
                    }
                } else {
                    newBomCharacteristicsContent = currentBomCharacteristicsContent;
                }

                // -----------------------------------------------------
                // Then, add the ones to be added
                // -----------------------------------------------------
                if (bomCharacteristicsToBeAdded != null
                        && bomCharacteristicsToBeAdded.size() > 0) {

                    for (int i = 0; i < bomCharacteristicsToBeAdded.size(); ++i) {

                        BomCharacteristic bomCharacteristicToBeAdded = bomCharacteristicsToBeAdded.get(
                                i);

                        // TODO:: check that the specified
                        // bomCharacteristicMasterUid is not already included in
                        // this document

                        HashMap<String, Object> bomCharacteristicType = createBomCharacteristicType(
                                bomCharacteristicToBeAdded);
                        newBomCharacteristicsContent.add(bomCharacteristicType);
                    }
                }
                // -----------------------------------------------------
                // Finally store new BOM Characteristic list
                // -----------------------------------------------------
                document.setPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST,
                        newBomCharacteristicsContent);

                session.saveDocument(document);
                session.save();

            }

            log.info(logInitMsg + " BOM Characteristics successfully saved.");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public static void saveBomCharacteriticsListInDocument(CoreSession session,
            DocumentModel document,
            List<BomCharacteristic> bomCharacteristicsList)
            throws EloraException {

        String logInitMsg = "[saveBomCharacteriticsListInDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO:: TO BE REMOVED
        /*
         * // Check specified input parameters if (document == null) {
         * log.error(logInitMsg + " Specified document is null."); throw new
         * EloraException("Specified document is null."); }
         *
         * if (bomCharacteristicsList == null) { log.error(logInitMsg +
         * " Specified bomCharacteristicsList is null."); throw new
         * EloraException("Specified bomCharacteristicsList is null."); }
         *
         * log.trace(logInitMsg + " Saving |" + bomCharacteristicsList.size() +
         * "| Bom Characteristics.");
         */

        try {

            setBomCharacteristicsListToDocument(document,
                    bomCharacteristicsList);

            session.saveDocument(document);
            session.save();

            log.info(logInitMsg + " BOM Characteristics successfully saved.");
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static void setBomCharacteristicsListToDocument(
            DocumentModel document,
            List<BomCharacteristic> bomCharacteristicsList) {

        ArrayList<HashMap<String, Object>> bomCharacteristicsContent = new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < bomCharacteristicsList.size(); ++i) {

            BomCharacteristic bomCharacteristic = bomCharacteristicsList.get(i);
            HashMap<String, Object> bomCharacteristicType = createBomCharacteristicType(
                    bomCharacteristic);
            bomCharacteristicsContent.add(bomCharacteristicType);
        }

        // -----------------------------------------------------
        // Set BOM Characteristic list property
        // -----------------------------------------------------
        document.setPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST,
                bomCharacteristicsContent);
    }

    public static HashMap<String, Object> createBomCharacteristicType(
            BomCharacteristic bomCharacteristic) {

        HashMap<String, Object> bomCharacteristicType = new HashMap<>();

        // Following fields are set during creation. They are not modified
        // during update.
        // --- bomCharacteristicId
        bomCharacteristicType.put("bomCharacteristicId",
                bomCharacteristic.getBomCharacteristicId());

        // --- classification
        bomCharacteristicType.put("classification",
                bomCharacteristic.getClassification());

        // --- bomCharacMaster
        bomCharacteristicType.put("bomCharacMaster",
                bomCharacteristic.getBomCharacteristicMaster());

        // --- type
        String type = bomCharacteristic.getType();
        bomCharacteristicType.put("type", type);

        // Remaining fields are common for both creation and update
        bomCharacteristicType = updateBomCharacteristicType(
                bomCharacteristicType, bomCharacteristic);

        return bomCharacteristicType;
    }

    public static HashMap<String, Object> updateBomCharacteristicType(
            HashMap<String, Object> bomCharacteristicType,
            BomCharacteristic bomCharacteristic) {

        // --- order
        bomCharacteristicType.put("order", bomCharacteristic.getOrder());

        // --- bomCharacMasterLastModified
        bomCharacteristicType.put("bomCharacMasterLastModified",
                bomCharacteristic.getBomCharacteristicMasterLastModified());

        // --- title
        bomCharacteristicType.put("title", bomCharacteristic.getTitle());

        // --- description
        bomCharacteristicType.put("description",
                bomCharacteristic.getDescription());

        // --- values management
        switch (bomCharacteristic.getType()) {
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
            // -- number value related fields
            bomCharacteristicType.put("numberMaxIntegerPlaces",
                    bomCharacteristic.getNumberMaxIntegerPlaces());

            bomCharacteristicType.put("numberMaxDecimalPlaces",
                    bomCharacteristic.getNumberMaxDecimalPlaces());

            bomCharacteristicType.put("numberDefaultValue",
                    bomCharacteristic.getNumberDefaultValue());

            bomCharacteristicType.put("numberValue",
                    bomCharacteristic.getNumberValue());
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
            // -- string value related fields
            bomCharacteristicType.put("stringMaxLength",
                    bomCharacteristic.getStringMaxLength());

            bomCharacteristicType.put("stringDefaultValue",
                    bomCharacteristic.getStringDefaultValue());

            bomCharacteristicType.put("stringValue",
                    bomCharacteristic.getStringValue());
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
            // -- date value related fields
            // Convert dateValue to Calendar before storing its value
            if (bomCharacteristic.getDateDefaultValue() != null) {
                Calendar dateDefaultValueCal = Calendar.getInstance();
                dateDefaultValueCal.setTime(
                        bomCharacteristic.getDateDefaultValue());
                bomCharacteristicType.put("dateDefaultValue",
                        dateDefaultValueCal);
            } else {
                bomCharacteristicType.put("dateDefaultValue",
                        bomCharacteristic.getDateDefaultValue());
            }

            if (bomCharacteristic.getDateValue() != null) {
                Calendar dateValueCal = Calendar.getInstance();
                dateValueCal.setTime(bomCharacteristic.getDateValue());
                bomCharacteristicType.put("dateValue", dateValueCal);
            } else {
                bomCharacteristicType.put("dateValue",
                        bomCharacteristic.getDateValue());
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
            // -- boolean value related fields
            bomCharacteristicType.put("booleanDefaultValue",
                    bomCharacteristic.getBooleanDefaultValue());

            bomCharacteristicType.put("booleanValue",
                    bomCharacteristic.getBooleanValue());
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
            // -- list value related fields
            bomCharacteristicType.put("listContent",
                    bomCharacteristic.getListContent());

            bomCharacteristicType.put("listDefaultValue",
                    bomCharacteristic.getListDefaultValue());

            bomCharacteristicType.put("listValue",
                    bomCharacteristic.getListValue());
            break;
        }

        // --- unit
        bomCharacteristicType.put("unit", bomCharacteristic.getUnit());

        // --- showInReport
        bomCharacteristicType.put("showInReport",
                bomCharacteristic.getShowInReport());

        // --- orderInReport
        bomCharacteristicType.put("orderInReport",
                bomCharacteristic.getOrderInReport());

        // --- required
        bomCharacteristicType.put("required", bomCharacteristic.getRequired());

        // --- includeInTitle
        bomCharacteristicType.put("includeInTitle",
                bomCharacteristic.getIncludeInTitle());

        // --- unmodifiable
        bomCharacteristicType.put("unmodifiable",
                bomCharacteristic.getUnmodifiable());

        // --- messageType
        String messageType = bomCharacteristic.getMessageType();
        bomCharacteristicType.put("messageType", messageType);

        // --- message
        String message = bomCharacteristic.getMessage();
        bomCharacteristicType.put("message", message);

        return bomCharacteristicType;
    }

    public static BomCharacteristic createBomCharacteristic(
            HashMap<String, Object> bomCharacteristicType) {

        // --- bomCharacteristicId
        String bomCharacteristicId = (String) bomCharacteristicType.get(
                "bomCharacteristicId");

        // --- classification
        String classification = (String) bomCharacteristicType.get(
                "classification");

        // --- bomCharacMaster
        String bomCharacteristicMaster = (String) bomCharacteristicType.get(
                "bomCharacMaster");

        // --- bomCharacMasterLastModified
        Date bomCharacteristicMasterLastModified = null;
        GregorianCalendar bomCharacteristicMasterLastModifiedGc = (GregorianCalendar) bomCharacteristicType.get(
                "bomCharacMasterLastModified");
        if (bomCharacteristicMasterLastModifiedGc != null) {
            // TODO:: uste dot kasu honetan eza dala beharrezkoa
            /*
             * // Taken from Nuxeo code: //
             * ------------------------------------------------- // remove
             * milliseconds as they are not stored in some // databases, which
             * could make the comparison fail just // after a document creation
             * (see NXP-8783) //
             * -------------------------------------------------
             * bomCharacteristicMasterLastModifiedGc.set( Calendar.MILLISECOND,
             * 0);
             */
            bomCharacteristicMasterLastModified = bomCharacteristicMasterLastModifiedGc.getTime();
        }

        // --- order
        Long order = (Long) bomCharacteristicType.get("order");

        // --- title
        String title = (String) bomCharacteristicType.get("title");

        // --- description
        String description = (String) bomCharacteristicType.get("description");

        // --- type
        String type = (String) bomCharacteristicType.get("type");

        // --- values management
        Long numberMaxIntegerPlaces = null;
        Long numberMaxDecimalPlaces = null;
        String numberDefaultValue = null;
        String numberValue = null;
        Long stringMaxLength = null;
        String stringDefaultValue = null;
        String stringValue = null;
        Date dateDefaultValue = null;
        Date dateValue = null;
        Boolean booleanDefaultValue = null;
        Boolean booleanValue = null;
        List<Map<String, String>> listContent = null;
        String listDefaultValue = null;
        String listValue = null;
        switch (type) {
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
            // -- number value related fields
            numberMaxIntegerPlaces = (Long) bomCharacteristicType.get(
                    "numberMaxIntegerPlaces");

            numberMaxDecimalPlaces = (Long) bomCharacteristicType.get(
                    "numberMaxDecimalPlaces");

            numberDefaultValue = (String) bomCharacteristicType.get(
                    "numberDefaultValue");

            numberValue = (String) bomCharacteristicType.get("numberValue");
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
            // -- string value related fields
            stringMaxLength = (Long) bomCharacteristicType.get(
                    "stringMaxLength");

            stringDefaultValue = (String) bomCharacteristicType.get(
                    "stringDefaultValue");

            stringValue = (String) bomCharacteristicType.get("stringValue");
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
            // -- date value related fields
            GregorianCalendar dateDefaultValueGc = (GregorianCalendar) bomCharacteristicType.get(
                    "dateDefaultValue");
            if (dateDefaultValueGc != null) {
                // TODO:: uste dot kasu honetan eza dala beharrezkoa
                /*
                 * // Taken from Nuxeo code: //
                 * ------------------------------------------------- // remove
                 * milliseconds as they are not stored in some // databases,
                 * which could make the comparison fail just // after a document
                 * creation (see NXP-8783) //
                 * -------------------------------------------------
                 * bomCharacteristicMasterLastModifiedGc.set(
                 * Calendar.MILLISECOND, 0);
                 */
                dateDefaultValue = dateDefaultValueGc.getTime();
            }

            GregorianCalendar dateValueeGc = (GregorianCalendar) bomCharacteristicType.get(
                    "dateValue");
            if (dateValueeGc != null) {
                // TODO:: uste dot kasu honetan eza dala beharrezkoa
                /*
                 * // Taken from Nuxeo code: //
                 * ------------------------------------------------- // remove
                 * milliseconds as they are not stored in some // databases,
                 * which could make the comparison fail just // after a document
                 * creation (see NXP-8783) //
                 * -------------------------------------------------
                 * bomCharacteristicMasterLastModifiedGc.set(
                 * Calendar.MILLISECOND, 0);
                 */
                dateValue = dateValueeGc.getTime();
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
            // -- boolean value related fields
            booleanDefaultValue = (Boolean) bomCharacteristicType.get(
                    "booleanDefaultValue");

            booleanValue = (Boolean) bomCharacteristicType.get("booleanValue");
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
            // -- list value related fields
            listContent = (List<Map<String, String>>) bomCharacteristicType.get(
                    "listContent");

            listDefaultValue = (String) bomCharacteristicType.get(
                    "listDefaultValue");

            listValue = (String) bomCharacteristicType.get("listValue");
            break;
        }

        // --- unit
        String unit = (String) bomCharacteristicType.get("unit");

        // --- showInReport
        Boolean showInReport = (Boolean) bomCharacteristicType.get(
                "showInReport");

        // --- orderInReport
        Long orderInReport = (Long) bomCharacteristicType.get("orderInReport");

        // --- required
        Boolean required = (Boolean) bomCharacteristicType.get("required");

        // --- includeInTitle
        Boolean includeInTitle = (Boolean) bomCharacteristicType.get(
                "includeInTitle");

        // --- unmodifiable
        Boolean unmodifiable = (Boolean) bomCharacteristicType.get(
                "unmodifiable");

        // --- messageType
        String messageType = (String) bomCharacteristicType.get("messageType");

        // --- message
        String message = (String) bomCharacteristicType.get("message");

        BomCharacteristic bomCharacteristic = new BomCharacteristic(
                bomCharacteristicId, classification, bomCharacteristicMaster,
                bomCharacteristicMasterLastModified, order, title, description,
                type, numberMaxIntegerPlaces, numberMaxDecimalPlaces,
                numberDefaultValue, numberValue, stringMaxLength,
                stringDefaultValue, stringValue, dateDefaultValue, dateValue,
                booleanDefaultValue, booleanValue, listContent,
                listDefaultValue, listValue, unit, showInReport, orderInReport,
                required, includeInTitle, unmodifiable, messageType, message);

        return bomCharacteristic;
    }

    public static String getBomClassificationLabel(String bomType,
            String classification) {
        String classificationLabel = "";

        HashMap<String, String> bomClassificationLabelMap = BomConfig.bomClassificationLabelMap.get(
                bomType);

        if (classification != null && !classification.isEmpty()) {

            // First split the classification, since it can be hierarchical
            String[] classificationLevels = classification.split(
                    ChainSelect.DEFAULT_KEY_SEPARATOR);
            for (int i = 0; i < classificationLevels.length; i++) {

                String classificationItem = classificationLevels[i];

                if (bomClassificationLabelMap.containsKey(classificationItem)) {
                    String classificationItemLabel = bomClassificationLabelMap.get(
                            classificationItem);
                    if (classificationLabel != null
                            && !classificationLabel.isEmpty()) {
                        classificationLabel += ChainSelect.DEFAULT_KEY_SEPARATOR
                                + classificationItemLabel;
                    } else {
                        classificationLabel += classificationItemLabel;
                    }
                }
            }
        }

        return classificationLabel;
    }

    /*
     * If we already know the bomCharacteristicMasterType, we can pass it to the
     * method. In order to avoid retrieving again this known metadata.
     */
    public static String getBomCharacteristicConstraints(
            BomCharacteristic bomCharacteristic, Map<String, String> messages,
            Locale locale) throws EloraException {

        String logInitMsg = "[getBomCharacteristicConstraints] ";
        log.trace(logInitMsg + "--- ENTER --- bomCharacteristisMasterId = |"
                + bomCharacteristic.getBomCharacteristicMaster() + "|, type = |"
                + bomCharacteristic.getType() + "|");

        String constraints = "";

        switch (bomCharacteristic.getType()) {
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
            if (bomCharacteristic.getNumberMaxDecimalPlaces() != null) {
                constraints += messages.get(
                        "label.widget.bomcharac.number.maxIntegerPlaces") + ": "
                        + bomCharacteristic.getNumberMaxIntegerPlaces();
            }
            if (bomCharacteristic.getNumberMaxDecimalPlaces() != null) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                constraints += messages.get(
                        "label.widget.bomcharac.number.maxDecimalPlaces") + ": "
                        + bomCharacteristic.getNumberMaxDecimalPlaces();
            }
            String numberDefaultNumber = bomCharacteristic.getNumberDefaultValue();
            if (numberDefaultNumber != null && !numberDefaultNumber.isEmpty()) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                constraints += messages.get(
                        "label.widget.bomcharac.defaultValue") + ": "
                        + EloraDecimalHelper.fromStandardToLocalized(locale,
                                numberDefaultNumber);
            }
            break;
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
            if (bomCharacteristic.getStringMaxLength() != null) {
                constraints += messages.get(
                        "label.widget.bomcharac.string.maxLength") + ": "
                        + bomCharacteristic.getStringMaxLength();
            }
            String stringDefaultNumber = bomCharacteristic.getStringDefaultValue();
            if (stringDefaultNumber != null && !stringDefaultNumber.isEmpty()) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                constraints += messages.get(
                        "label.widget.bomcharac.defaultValue") + ": "
                        + stringDefaultNumber;
            }
            break;
        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
            Date dateDefaultValue = bomCharacteristic.getDateDefaultValue();
            if (dateDefaultValue != null) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                DateFormat dateFormat = DateFormat.getDateInstance(
                        DateFormat.MEDIUM, locale);
                constraints += messages.get(
                        "label.widget.bomcharac.defaultValue") + ": "
                        + dateFormat.format(dateDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
            Boolean booleanDefaultValue = bomCharacteristic.getBooleanDefaultValue();
            if (booleanDefaultValue != null) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                constraints += messages.get(
                        "label.widget.bomcharac.defaultValue") + ": "
                        + messages.get("label." + booleanDefaultValue);
            }
            break;

        case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
            String listDefaultValue = bomCharacteristic.getListDefaultValue();
            if (listDefaultValue != null && !listDefaultValue.isEmpty()) {
                if (constraints.length() > 0) {
                    constraints += ", ";
                }
                constraints += messages.get(
                        "label.widget.bomcharac.defaultValue") + ": "
                        + listDefaultValue;
            }
            break;
        }

        if (bomCharacteristic.getUnmodifiable() != null
                && bomCharacteristic.getUnmodifiable()) {
            if (constraints.length() > 0) {
                constraints += ", ";
            }
            constraints += messages.get("label.widget.bomcharac.unmodifiable");
        }

        log.trace(logInitMsg + "--- EXIT --- with constraints = |" + constraints
                + "|");

        return constraints;
    }

    public static String generateBomCharacteristicId() {
        return UUID.randomUUID().toString();

    }

    // -----------------------------------------------------------

    public static void loadCharacteristicMasters(DocumentModel currentDoc)
            throws EloraException {

        String classification = BomHelper.getBomClassificationValue(currentDoc);

        loadCharacteristicMasters(currentDoc, classification, false);
    }

    // This is a special case.
    public static void loadCharacteristicMastersFromListener(
            DocumentModel currentDoc, String newClassification)
            throws EloraException {

        loadCharacteristicMasters(currentDoc, newClassification, true);
    }

    private static void loadCharacteristicMasters(DocumentModel currentDoc,
            String classification, boolean fromListener) throws EloraException {
        String logInitMsg = "[loadCharacteristicMasters] classification = |"
                + classification + "|, fromListener = |" + fromListener + "|";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            // ###########################################################################
            // ------------ STEP 1:
            // Retrieve characteristic masters related to the document (in
            // function of the document's classification)
            // ###########################################################################
            Map<String, BomCharacteristic> classificationRelatedBomCharacteristicMastersMap = retrieveBomDocumentRelatedBomCharacteristicMasters(
                    currentDoc, classification);

            // ###########################################################################
            // ------------ STEP 2:
            // Compare and merge loaded characteristic masters with the
            // characteristics already stored in the document
            // ###########################################################################
            ArrayList<HashMap<String, Object>> currentBomCharacteristicsContent = new ArrayList<HashMap<String, Object>>();
            if (currentDoc.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
                currentBomCharacteristicsContent = (ArrayList<HashMap<String, Object>>) currentDoc.getPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);
            }

            // Construct a list with current Bom Characteristics
            List<BomCharacteristic> currentBomCharacteristics = new LinkedList<BomCharacteristic>();
            for (int i = 0; i < currentBomCharacteristicsContent.size(); ++i) {
                HashMap<String, Object> bomCharacteristicType = currentBomCharacteristicsContent.get(
                        i);
                BomCharacteristic currentBomCharacteristic = createBomCharacteristic(
                        bomCharacteristicType);
                currentBomCharacteristics.add(currentBomCharacteristic);
            }

            List<BomCharacteristic> newBomCharacteristics = mergeLoadedCharacteristicMastersWithCurrentCharacteristics(
                    classificationRelatedBomCharacteristicMastersMap,
                    currentBomCharacteristics);

            // ###########################################################################
            // ------------ STEP 3:
            // Set and save new structure
            // ###########################################################################
            if (fromListener) {
                // Set BOM characteristics without saving the document
                setBomCharacteristicsListToDocument(currentDoc,
                        newBomCharacteristics);
            } else {
                // Set BOM characteristics and save the document
                CoreSession session = currentDoc.getCoreSession();
                saveBomCharacteriticsListInDocument(session, currentDoc,
                        newBomCharacteristics);
            }

            log.trace(logInitMsg + "--- EXIT ---");

        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public static Map<String, BomCharacteristic> retrieveBomDocumentRelatedBomCharacteristicMasters(
            DocumentModel currentDoc) throws EloraException {

        String classification = BomHelper.getBomClassificationValue(currentDoc);

        return retrieveBomDocumentRelatedBomCharacteristicMasters(currentDoc,
                classification);
    }

    public static Map<String, BomCharacteristic> retrieveBomDocumentRelatedBomCharacteristicMasters(
            DocumentModel currentDoc, String classification)
            throws EloraException {
        String logInitMsg = "[retrieveBomDocumentRelatedBomCharacteristicMasters] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String bomCharacMasterDocType = BomCharacteristicMastersHelper.getBomCharacteristicMasterDocTypeForBomType(
                currentDoc.getType());

        String query = BomCharacteristicMastersQueryFactory.getLoadBomCharacteristicMastersQuery(
                bomCharacMasterDocType, classification);

        CoreSession session = currentDoc.getCoreSession();
        DocumentModelList bomCharacteristicMasterDocs = session.query(query);

        Map<String, BomCharacteristic> classificationRelatedBomCharacteristicMastersMap = new HashMap<String, BomCharacteristic>();

        if (bomCharacteristicMasterDocs != null
                && !bomCharacteristicMasterDocs.isEmpty()) {

            // Set the orderInReport by default, as the ordering shown in the
            // screen
            Long orderInReport = new Long(0);
            for (DocumentModel bomCharacteristicMasterDoc : bomCharacteristicMasterDocs) {
                orderInReport++;

                BomCharacteristic bomCharacteristic = BomCharacteristicMastersHelper.createBomCharacteristicFromMaster(
                        bomCharacteristicMasterDoc);

                bomCharacteristic.setOrderInReport(orderInReport);

                classificationRelatedBomCharacteristicMastersMap.put(
                        bomCharacteristic.getBomCharacteristicMaster(),
                        bomCharacteristic);
            }
        }
        log.trace(logInitMsg
                + "--- EXIT --- with classificationRelatedBomCharacteristicMastersMap.size() =|"
                + classificationRelatedBomCharacteristicMastersMap.size()
                + "|");

        return classificationRelatedBomCharacteristicMastersMap;
    }

    public static List<BomCharacteristic> mergeLoadedCharacteristicMastersWithCurrentCharacteristics(
            Map<String, BomCharacteristic> classificationRelatedBomCharacteristicMastersMap,
            List<BomCharacteristic> currentBomCharacteristics)
            throws EloraException {

        String logInitMsg = "[mergeLoadedCharacteristicMastersWithCurrentCharacteristics] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<BomCharacteristic> newBomCharacteristics = new LinkedList<BomCharacteristic>();

        // Loop current BOM Characteristics and add, remove or update
        // characteristics in function of the new classification:
        for (Iterator<BomCharacteristic> iterator = currentBomCharacteristics.iterator(); iterator.hasNext();) {
            BomCharacteristic currentBomCharacteristic = iterator.next();
            String bomCharacteristicId = currentBomCharacteristic.getBomCharacteristicId();
            String bomCharacteristicMasterUid = currentBomCharacteristic.getBomCharacteristicMaster();
            Date bomCharacteristicMasterLastModified = currentBomCharacteristic.getBomCharacteristicMasterLastModified();

            if (!classificationRelatedBomCharacteristicMastersMap.containsKey(
                    bomCharacteristicMasterUid)) {
                // as it has been set obsolete it should be removed, so
                // don't add it to the new BOM characteristics list
            } else {
                BomCharacteristic updatedBomCharacteristic = classificationRelatedBomCharacteristicMastersMap.get(
                        bomCharacteristicMasterUid);

                Date updatedBomCharacteristicMasterLastModified = updatedBomCharacteristic.getBomCharacteristicMasterLastModified();

                log.trace("bomCharacteristicMasterLastModified = |"
                        + bomCharacteristicMasterLastModified
                        + "|, updatedBomCharacteristicMasterLastModified = |"
                        + updatedBomCharacteristicMasterLastModified + "|");

                // if masterLastModified value is different, it means that
                // it has changed
                if (!bomCharacteristicMasterLastModified.equals(
                        updatedBomCharacteristicMasterLastModified)) {

                    // set BomCharacteristicId as current BomCharacteristicId,
                    // since it is replacing it
                    updatedBomCharacteristic.setBomCharacteristicId(
                            bomCharacteristicId);

                    FacesContext context = FacesContext.getCurrentInstance();
                    fillReloadingCharacteristicGeneralInfoMessage(context,
                            updatedBomCharacteristic);

                    processReloadingCharacteristicValue(
                            currentBomCharacteristic, updatedBomCharacteristic);

                    // If already set, update showInReport and orderInReport
                    if (currentBomCharacteristic.getShowInReport()) {
                        updatedBomCharacteristic.setShowInReport(true);
                        updatedBomCharacteristic.setOrderInReport(
                                currentBomCharacteristic.getOrderInReport());
                    }

                    newBomCharacteristics.add(updatedBomCharacteristic);
                } else {
                    newBomCharacteristics.add(currentBomCharacteristic);
                }

                classificationRelatedBomCharacteristicMastersMap.remove(
                        bomCharacteristicMasterUid);
            }
        }

        if (classificationRelatedBomCharacteristicMastersMap.size() > 0) {
            Iterator<Entry<String, BomCharacteristic>> currentBomCharacteristicMastersMapIt = classificationRelatedBomCharacteristicMastersMap.entrySet().iterator();
            while (currentBomCharacteristicMastersMapIt.hasNext()) {
                Map.Entry<String, BomCharacteristic> entry = currentBomCharacteristicMastersMapIt.next();
                newBomCharacteristics.add(entry.getValue());
            }

        }
        log.trace(logInitMsg + "--- EXIT ---");

        return newBomCharacteristics;
    }

    private static void processReloadingCharacteristicValue(
            BomCharacteristic currentBomCharacteristic,
            BomCharacteristic updatedBomCharacteristic) {

        String logInitMsg = "[processRelaodingCharacteristicValue] ] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + "nodeDataBomCharacteristic.getType() = |"
                + currentBomCharacteristic.getType() + "|");

        // If updated characteristic is unmodifiable, set directly its value.
        // Don't merge it.
        if (!updatedBomCharacteristic.getUnmodifiable()) {

            switch (currentBomCharacteristic.getType()) {
            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
                String numberValue = currentBomCharacteristic.getNumberValue();
                log.trace(logInitMsg + "numberValue = |" + numberValue + "|");

                if (numberValue != null && !numberValue.isEmpty()) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    Locale locale = context.getViewRoot().getLocale();
                    try {
                        Long numberMaxIntegerPlacesL = updatedBomCharacteristic.getNumberMaxIntegerPlaces();
                        Long numberMaxDecimalPlacesL = updatedBomCharacteristic.getNumberMaxDecimalPlaces();
                        EloraDecimalHelper.validateDecimalValue(locale,
                                numberValue,
                                (numberMaxIntegerPlacesL != null
                                        ? numberMaxIntegerPlacesL.intValue()
                                        : null),
                                (numberMaxDecimalPlacesL != null
                                        ? numberMaxDecimalPlacesL.intValue()
                                        : null));
                        updatedBomCharacteristic.setNumberValue(numberValue);

                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_INFO,
                                numberValue.toString(),
                                updatedBomCharacteristic);

                    } catch (ValidatorException e) {
                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR,
                                numberValue.toString(),
                                updatedBomCharacteristic);
                    }
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
                String stringValue = currentBomCharacteristic.getStringValue();
                log.trace(logInitMsg + "stringValue = |" + stringValue + "|");

                if (stringValue != null && !stringValue.isEmpty()) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    try {
                        BomCharacteristicsValidatorHelper.validateStringValue(
                                context, stringValue,
                                updatedBomCharacteristic.getStringMaxLength());
                        updatedBomCharacteristic.setStringValue(stringValue);

                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_INFO,
                                stringValue, updatedBomCharacteristic);

                    } catch (ValidatorException e) {
                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR,
                                stringValue, updatedBomCharacteristic);
                    }
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
                Date dateValue = currentBomCharacteristic.getDateValue();
                log.trace(logInitMsg + "dateValue = |" + dateValue + "|");

                if (dateValue != null) {
                    updatedBomCharacteristic.setDateValue(dateValue);
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
                Boolean booleanValue = currentBomCharacteristic.getBooleanValue();
                log.trace(logInitMsg + "booleanValue = |" + booleanValue + "|");

                if (booleanValue != null) {
                    updatedBomCharacteristic.setBooleanValue(booleanValue);
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
                String listValue = currentBomCharacteristic.getListValue();
                log.trace(logInitMsg + "listValue = |" + listValue + "|");

                if (listValue != null && !listValue.isEmpty()) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    try {
                        BomCharacteristicsValidatorHelper.validateListValue(
                                context, listValue,
                                updatedBomCharacteristic.getListContent());

                        updatedBomCharacteristic.setListValue(listValue);

                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_INFO,
                                listValue, updatedBomCharacteristic);

                    } catch (ValidatorException e) {
                        fillReloadingCharacteristicValueRelatedMessage(context,
                                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR,
                                listValue, updatedBomCharacteristic);
                    }
                }
                break;
            }

        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private static void fillReloadingCharacteristicGeneralInfoMessage(
            FacesContext context, BomCharacteristic updatedBomCharacteristic) {
        String logInitMsg = "[fillReloadingCharacteristicGeneralInfoMessage] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + "BomCharacteristic |"
                + updatedBomCharacteristic.getBomCharacteristicId()
                + "| has been reloaded since its related master "
                + updatedBomCharacteristic.getBomCharacteristicMaster()
                + " has been changed. ");

        String message = MessageFactory.getMessage(context,
                "message.info.bomcharac.loadCharacteristic").getDetail();

        updatedBomCharacteristic.setMessageType(
                BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_INFO);
        updatedBomCharacteristic.setMessage(message);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private static void fillReloadingCharacteristicValueRelatedMessage(
            FacesContext context, String messageType, String currentValue,
            BomCharacteristic updatedBomCharacteristic) {
        String logInitMsg = "fillReloadingCharacteristicValueRelatedMessage] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String message = null;
        switch (messageType) {
        case BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_INFO:

            log.trace(logInitMsg + "BomCharacteristic |"
                    + updatedBomCharacteristic.getBomCharacteristicId()
                    + "| has been reloaded since its related master "
                    + updatedBomCharacteristic.getBomCharacteristicMaster()
                    + " has been changed. Characteristic's old value |"
                    + currentValue + " is maintained since it is still valid.");

            message = MessageFactory.getMessage(context,
                    "message.info.bomcharac.loadCharacteristic.validValue",
                    currentValue).getDetail();

            break;

        case BomCharacteristicsConstants.BOM_CHARAC_MSG_TYPE_ERROR:

            message = MessageFactory.getMessage(context,
                    "message.error.bomcharac.loadCharacteristic.invalidValue",
                    currentValue).getDetail();

            break;
        }

        updatedBomCharacteristic.setMessageType(messageType);
        updatedBomCharacteristic.appendMessage(message);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    // -------------------------------------------

    /**
     * This method checks that all BOM characteristics of the specified document
     * that are marked as Required have a value.
     *
     * @param document
     * @return false if there is at least one required characteristic without an
     *         assigned value. True if Required constraint is respected.
     * @throws BomCharacteristicsValidatorException
     */
    @SuppressWarnings("unchecked")
    public static boolean verifyBomCharacteristicsRequiredConstraint(
            DocumentModel document)
            throws BomCharacteristicsValidatorException {
        boolean result = true;

        ArrayList<HashMap<String, Object>> currentBomCharacteristicsContent = new ArrayList<HashMap<String, Object>>();
        if (document.getPropertyValue(
                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
            currentBomCharacteristicsContent = (ArrayList<HashMap<String, Object>>) document.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);
        }

        for (int i = 0; i < currentBomCharacteristicsContent.size(); ++i) {
            HashMap<String, Object> bomCharacteristicType = currentBomCharacteristicsContent.get(
                    i);

            // if at least there is one BOM Characteristic that doesn't match
            // the required constraint, return false
            if (verifyBomCharacteristicRequiredConstraint(
                    bomCharacteristicType) == false) {
                throw new BomCharacteristicsValidatorException(document);
            }
        }

        return result;
    }

    private static boolean verifyBomCharacteristicRequiredConstraint(
            HashMap<String, Object> bomCharacteristicType) {

        boolean fieldRequiredConstraintOk = false;

        Boolean required = (Boolean) bomCharacteristicType.get("required");
        if (required) {

            String type = (String) bomCharacteristicType.get("type");

            switch (type) {
            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
                String numberValue = (String) bomCharacteristicType.get(
                        "numberValue");
                if (numberValue != null && !numberValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
                String stringValue = (String) bomCharacteristicType.get(
                        "stringValue");
                if (stringValue != null && !stringValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
                Date dateValue = null;
                GregorianCalendar dateValueeGc = (GregorianCalendar) bomCharacteristicType.get(
                        "dateValue");
                if (dateValueeGc != null) {
                    dateValue = dateValueeGc.getTime();
                }
                if (dateValue != null) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
                Boolean booleanValue = (Boolean) bomCharacteristicType.get(
                        "booleanValue");
                if (booleanValue != null) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
                String listValue = (String) bomCharacteristicType.get(
                        "listValue");
                if (listValue != null && !listValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;
            }
        } else {
            fieldRequiredConstraintOk = true;
        }

        return fieldRequiredConstraintOk;

    }

    public static boolean verifyBomCharacteristicRequiredConstraint(
            BomCharacteristic bomCharacteristic) {

        boolean fieldRequiredConstraintOk = false;

        if (bomCharacteristic.getRequired()) {

            switch (bomCharacteristic.getType()) {
            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_NUMBER:
                String numberValue = bomCharacteristic.getNumberValue();
                if (numberValue != null && !numberValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_STRING:
                String stringValue = bomCharacteristic.getStringValue();
                if (stringValue != null && !stringValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_DATE:
                Date dateValue = bomCharacteristic.getDateValue();
                if (dateValue != null) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_BOOLEAN:
                Boolean booleanValue = bomCharacteristic.getBooleanValue();
                if (booleanValue != null) {
                    fieldRequiredConstraintOk = true;
                }
                break;

            case BomCharacteristicsConstants.BOM_CHARAC_TYPE_LIST:
                String listValue = bomCharacteristic.getListValue();
                if (listValue != null && !listValue.isEmpty()) {
                    fieldRequiredConstraintOk = true;
                }
                break;
            }
        } else {
            fieldRequiredConstraintOk = true;
        }

        return fieldRequiredConstraintOk;
    }

    public static boolean verifyBomCharacteristicOrderInReportConstraint(
            BomCharacteristic bomCharacteristic) {
        boolean displayOrderRequiredConstraintOk = false;

        // orderInReport is required if showInReport is true
        if (bomCharacteristic.getShowInReport()) {
            Long orderInReport = bomCharacteristic.getOrderInReport();
            if (orderInReport != null) {
                displayOrderRequiredConstraintOk = true;
            }
        } else {
            displayOrderRequiredConstraintOk = true;
        }

        return displayOrderRequiredConstraintOk;
    }
}
