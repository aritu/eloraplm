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
 * @author aritu
 *
 */
public class MetadataConfigHelper {

    protected static EloraConfigManager configService = Framework.getService(
            EloraConfigManager.class);

    /**
     * Returns metadata configuration for the provided doctype, filtered by the
     * metadata type and (if specified) update mode.
     *
     * @param authoringTool
     * @param type
     * @param plmMetadataType
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getMetadataConfig(String authoringTool,
            String plmMetadataType, String plmDoctype, String[] updateModes,
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        filter.put(EloraConfigConstants.PROP_METADATA_MAPPING_AUTHORING_TOOL,
                authoringTool);

        filter.put(EloraConfigConstants.PROP_METADATA_MAPPING_PLM_DOCTYPE,
                plmDoctype);

        filter.put(EloraConfigConstants.PROP_METADATA_MAPPING_PLM_METADATA_TYPE,
                plmMetadataType);

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable metadataConfig = new EloraConfigTable();
        if (updateModes == null) {
            metadataConfig = getMetadataConfigForSingleUpdateMode(filter, null);
        } else {
            for (String updateMode : updateModes) {
                EloraConfigTable tempMetadataConfig = getMetadataConfigForSingleUpdateMode(
                        filter, updateMode);

                // Merge properties
                metadataConfig.mergeWithTable(tempMetadataConfig);
            }
        }

        return metadataConfig;
    }

    private static EloraConfigTable getMetadataConfigForSingleUpdateMode(
            Map<String, Serializable> filter, String updateMode)
            throws EloraException {
        if (updateMode != null) {
            filter.put(EloraConfigConstants.PROP_METADATA_MAPPING_UPDATE_MODE,
                    updateMode);
        }

        return configService.getConfigTable(
                EloraConfigConstants.VOC_METADATA_MAPPING, filter, null);
    }

}
