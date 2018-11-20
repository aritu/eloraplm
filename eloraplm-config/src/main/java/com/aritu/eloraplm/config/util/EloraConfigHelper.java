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
public class EloraConfigHelper {

    protected static EloraConfigManager configService = Framework.getService(
            EloraConfigManager.class);

    public static EloraConfigTable getAutocopyParentTypeConfig(
            boolean includeObsoletes) throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        return configService.getConfigTable(
                EloraConfigConstants.VOC_AUTOCOPY_PARENT_TYPES, filter, null);
    }

    public static EloraConfigTable getCheckoutConfig(boolean includeObsoletes)
            throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        return configService.getConfigTable(
                EloraConfigConstants.VOC_CHECKOUT_CONFIG, filter, null);
    }

    public static String getManufacturerConfig(String key)
            throws EloraException {

        return configService.getConfig(
                EloraConfigConstants.VOC_ERP_MANUFACTURER, key, true);
    }

    public static String getCustomerConfig(String key) throws EloraException {
        return configService.getConfig(EloraConfigConstants.VOC_ERP_CUSTOMER,
                key, true);
    }

    public static EloraConfigTable getUnitConversionConfig()
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        filter.put(EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DISPLAY,
                "1");

        EloraConfigTable unitConversionConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_UNIT_CONVERSION_MAPPING, filter, null);

        return unitConversionConfig;
    }

    public static EloraConfigTable getIntegrationVersionControlConfig()
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");

        EloraConfigTable integrationVersionControlConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_INTEGRATION_VERSION_CONTROL, filter,
                null);

        return integrationVersionControlConfig;
    }

    public static EloraConfigTable getGeneralConfig(boolean includeObsoletes)
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        return configService.getConfigTable(
                EloraConfigConstants.VOC_GENERAL_CONFIG, filter, null);
    }

    public static String getVocabularyTimestamp(String vocabulary)
            throws EloraException {

        String timestamp = configService.getConfig(
                EloraConfigConstants.VOC_VOCABULARIES_TIMESTAMPS, vocabulary,
                false);

        return timestamp;
    }

    public static EloraConfigTable getConfigTable(String vocabulary)
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        EloraConfigTable configTable = configService.getConfigTable(vocabulary,
                filter, null);

        return configTable;
    }

}
