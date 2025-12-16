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
package com.aritu.eloraplm.config.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Elora Document Metadata Mapping configuration helper class.
 *
 * @author aritu
 *
 */
public class DocumentMetadataMappingConfig {
    private static final Log log = LogFactory.getLog(
            DocumentMetadataMappingConfig.class);

    // -------------------------------------------------
    // Document Metadata Mapping configuration variables
    // -------------------------------------------------

    // Document Metadata Mapping Configurations
    public static EloraConfigTable documentMetadataMappingConfig = initDocumentMetadataMappingConfig();

    public static List<String> originDocumentTypes = initOriginDocumentTypesList();

    public static List<String> destinationDocumentTypes = initDestinationDocumentTypesList();

    // ------------------------------------------------------------------------
    // Document Metadata Mapping configuration variables initialization methods
    // ------------------------------------------------------------------------

    private static EloraConfigTable initDocumentMetadataMappingConfig() {
        String logInitMsg = "[initDocumentMetadataMappingConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = DocumentMetadataMappingConfigHelper.getDocumentMetadataMappingConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initOriginDocumentTypesList() {
        String logInitMsg = "[initOriginDocumentTypesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = documentMetadataMappingConfig.extractConfigTablePropertyValuesAsList(
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE);

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static List<String> initDestinationDocumentTypesList() {
        String logInitMsg = "[initDestinationDocumentTypesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = documentMetadataMappingConfig.extractConfigTablePropertyValuesAsList(
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE);

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    public static void reloadConfig() {
        documentMetadataMappingConfig = initDocumentMetadataMappingConfig();
        originDocumentTypes = initOriginDocumentTypesList();
        destinationDocumentTypes = initDestinationDocumentTypesList();
    }
}
