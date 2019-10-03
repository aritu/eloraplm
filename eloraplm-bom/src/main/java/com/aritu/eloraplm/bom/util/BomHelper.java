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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;

/**
 * Helper class for Elora Boms.
 *
 * @author aritu
 *
 */
public class BomHelper {

    private static final Log log = LogFactory.getLog(BomHelper.class);

    public static String getBomClassificationMetadataForBomType(
            String bomType) {

        String logInitMsg = "[getBomClassificationMetadataForBomType] ";
        log.trace(logInitMsg + "--- ENTER --- bomType = |" + bomType + "|");

        String classificationMetadata = "";

        switch (bomType) {
        case EloraDoctypeConstants.BOM_PART:
            classificationMetadata = EloraMetadataConstants.ELORA_PARTCLASS_PARTCLASSIFICATION;
            break;
        case EloraDoctypeConstants.BOM_PRODUCT:
            classificationMetadata = EloraMetadataConstants.ELORA_PRODCLASS_PRODUCTCLASSIFICATION;
            break;
        case EloraDoctypeConstants.BOM_TOOL:
            classificationMetadata = EloraMetadataConstants.ELORA_BOMTOOL_TOOLCLASSIFICATION;
            break;
        case EloraDoctypeConstants.BOM_PACKAGING:
            classificationMetadata = EloraMetadataConstants.ELORA_BOMPACK_PACKAGINGCLASSIFICATION;
            break;
        case EloraDoctypeConstants.BOM_SPECIFICATION:
            classificationMetadata = EloraMetadataConstants.ELORA_BOMSPEC_SPECIFICATIONCLASSIFICATION;
            break;
        }

        log.trace(logInitMsg + "--- EXIT --- with bomCharacteristicDocType = |"
                + classificationMetadata + "|");

        return classificationMetadata;
    }

    public static String getBomClassificationValue(DocumentModel doc) {
        String logInitMsg = "[getClassificationValue]";
        log.trace(logInitMsg + "--- ENTER --- ");

        String classificationMetadata = BomHelper.getBomClassificationMetadataForBomType(
                doc.getType());

        return getBomClassificationValue(doc, classificationMetadata);
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

}
