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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Elora Document Metadata Mapping configuration helper class.
 *
 * @author aritu
 *
 */
public class DocumentMetadataMappingConfigHelper {

    protected static EloraConfigManager configService = Framework.getService(
            EloraConfigManager.class);

    public static EloraConfigTable getDocumentMetadataMappingConfig(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable docMetMapConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_DOCUMENT_METADATA_MAPPING, filter,
                null, true);

        return docMetMapConfig;
    }

    public static EloraConfigTable getDocumentMetadataMappingConfigForDestDocType(
            String dstDocType, String dstProcess, boolean includeObsoletes)
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
            filter.put(
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE,
                    dstDocType);
            filter.put(
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_PROCESS,
                    dstProcess);
        }

        EloraConfigTable docMetMapConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_DOCUMENT_METADATA_MAPPING, filter,
                null, true);

        return docMetMapConfig;
    }

    public static EloraConfigTable getDocumentMetadataMappingConfigForOrgDocType(
            String orgDocType, String dstProcess, boolean includeObsoletes)
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
            filter.put(
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE,
                    orgDocType);
            filter.put(
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_PROCESS,
                    dstProcess);
        }

        EloraConfigTable docMetMapConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_DOCUMENT_METADATA_MAPPING, filter,
                null, true);

        return docMetMapConfig;
    }

}
