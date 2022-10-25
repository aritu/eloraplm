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
import org.nuxeo.ecm.core.schema.DocumentType;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;

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
