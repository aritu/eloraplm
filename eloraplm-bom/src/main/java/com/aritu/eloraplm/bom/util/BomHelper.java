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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;

import com.aritu.eloraplm.config.util.BomConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Elora Boms.
 *
 * @author aritu
 *
 */
public class BomHelper {

    private static final Log log = LogFactory.getLog(BomHelper.class);

    public static String getBomClassificationMetadataForBomType(
            String bomTypeName) {

        String logInitMsg = "[getBomClassificationMetadataForBomType] ";
        log.trace(logInitMsg + "--- ENTER --- bomType = |" + bomTypeName + "|");

        DocumentType bomType = EloraDocumentTypesHelper.getDocumentType(
                bomTypeName);
        String classificationMetadata = "";

        if (EloraDocumentTypesHelper.getDocumentType(
                EloraDoctypeConstants.BOM_PART).isSuperTypeOf(bomType)) {
            classificationMetadata = EloraMetadataConstants.ELORA_PARTCLASS_PARTCLASSIFICATION;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_PRODUCT)) {
            classificationMetadata = EloraMetadataConstants.ELORA_PRODCLASS_PRODUCTCLASSIFICATION;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_TOOL)) {
            classificationMetadata = EloraMetadataConstants.ELORA_BOMTOOL_TOOLCLASSIFICATION;
        } else if (bomTypeName.equals(EloraDoctypeConstants.BOM_PACKAGING)) {
            classificationMetadata = EloraMetadataConstants.ELORA_BOMPACK_PACKAGINGCLASSIFICATION;
        } else if (bomTypeName.equals(
                EloraDoctypeConstants.BOM_SPECIFICATION)) {
            classificationMetadata = EloraMetadataConstants.ELORA_BOMSPEC_SPECIFICATIONCLASSIFICATION;
        }

        log.trace(logInitMsg + "--- EXIT --- with bomCharacteristicDocType = |"
                + classificationMetadata + "|");

        return classificationMetadata;
    }

    public static String getBomClassificationValue(DocumentModel doc) {
        return getBomClassificationValue(doc,
                BomHelper.getBomClassificationMetadataForBomType(
                        doc.getType()));
    }

    public static String getBomClassificationValue(DocumentModel doc,
            String classificationMetadata) {
        String logInitMsg = "[getClassificationValue]";
        log.trace(logInitMsg + "--- ENTER --- ");

        String classification = null;

        if (doc.getPropertyValue(classificationMetadata) != null) {
            classification = doc.getPropertyValue(
                    classificationMetadata).toString();
        }

        log.trace(logInitMsg + "--- EXIT with classification = |"
                + classification + "|---");
        return classification;
    }

    public static String getBomClassificationLabel(DocumentModel doc) {
        return getBomClassificationLabel(doc.getType(),
                getBomClassificationValue(doc));
    }

    public static String getBomClassificationLabel(String bomTypeName,
            String classification) {
        String logInitMsg = "[getBomClassificationLabel]";
        log.trace(logInitMsg + "--- ENTER --- ");

        String classificationLabel = "";

        DocumentType bomType = EloraDocumentTypesHelper.getDocumentType(
                bomTypeName);
        if (EloraDocumentTypesHelper.getDocumentType(
                EloraDoctypeConstants.BOM_PART).isSuperTypeOf(bomType)) {
            bomTypeName = EloraDoctypeConstants.BOM_PART;
        }
        HashMap<String, String> bomClassificationLabelMap = BomConfig.bomClassificationLabelMap.get(
                bomTypeName);

        if (bomClassificationLabelMap != null
                && bomClassificationLabelMap.size() > 0
                && classification != null && !classification.isEmpty()) {

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

        log.trace(logInitMsg + "--- EXIT with classificationLabel = |"
                + classificationLabel + "|---");
        return classificationLabel;
    }

    public static boolean existManufacturerAndPartNumberInOemList(
            DocumentModel doc, String manufacturer, String partNumber)
            throws EloraException {

        // Check specified input parameters
        if (doc == null) {
            throw new EloraException("Specified doc is null.");
        }

        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[existManufacturerAndPartNumberInOemList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean existManufacturerAndPartNumberInOemList = false;

        IterableQueryResult queryResult = null;
        try {
            String query = BomQueryFactory.getCountOemByManufacturerAndPartNumberQuery(
                    doc, manufacturer, partNumber);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                String resultCountStr = map.get(
                        "COUNT(" + NXQL.ECM_UUID + ")").toString();
                int resultCount = Integer.valueOf(resultCountStr);

                log.trace(logInitMsg + "DB query resultCount = |" + resultCount
                        + "|");

                if (resultCount > 0) {
                    existManufacturerAndPartNumberInOemList = true;
                }
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

        log.trace(logInitMsg
                + "--- EXIT --- with existManufacturerAndPartNumberInOemList = |"
                + existManufacturerAndPartNumberInOemList + "|");

        return existManufacturerAndPartNumberInOemList;
    }

    public static long getMaxPreferenceInOemsList(DocumentModel doc)
            throws EloraException {

        // Check specified input parameters
        if (doc == null) {
            throw new EloraException("Specified doc is null.");
        }

        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[getMaxPreferenceInOemsList] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        long maxPreference = 0;

        IterableQueryResult queryResult = null;
        try {
            String query = BomQueryFactory.getMaxPreferenceInOemsListQuery(doc);

            queryResult = session.queryAndFetch(query, NXQL.NXQL);

            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                if (map.get("MAX(" + EloraMetadataConstants.ELORA_OEM_OEMS
                        + "/*/preference" + ")") != null) {
                    String maxPreferenceStr = map.get(
                            "MAX(" + EloraMetadataConstants.ELORA_OEM_OEMS
                                    + "/*/preference" + ")").toString();
                    maxPreference = Long.valueOf(maxPreferenceStr);

                    log.trace(logInitMsg + "DB query maxPreference = |"
                            + maxPreference + "|");
                }
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

        log.trace(logInitMsg + "--- EXIT --- with maxPreference = |"
                + maxPreference + "|");

        return maxPreference;
    }

}
