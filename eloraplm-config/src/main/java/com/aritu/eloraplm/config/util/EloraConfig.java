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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraConfig {
    private static final Log log = LogFactory.getLog(EloraConfig.class);

    // ----------------------------
    // Elora config variables
    // ----------------------------
    public static final EloraConfigTable autocopyParentTypesConfig = initAutocopyParentTypesConfig();

    public static final Map<String, String> autocopyParentTypesMap = initAutocopyParentTypesMap();

    public static final EloraConfigTable generalConfig = initGeneralConfig();

    public static final Map<String, String> generalConfigMap = initGeneralConfigMap();

    public static final EloraConfigTable unitConversionConfig = initUnitConversionConfig();

    public static final Map<String, EloraConfigRow> unitConversionConfigMap = initUnitConversionConfigMap();

    public static final EloraConfigTable checkoutConfig = initCheckoutConfig();

    public static final Map<String, String> checkoutSwitchChildrenMap = initCheckoutSwitchChildrenMap();

    public static final EloraConfigTable integrationVersionControlConfig = initIntegrationVersionControlConfig();

    public static final Map<String, Integer> integrationMinAllowedVersionsMap = initIntegrationMinAllowedVersionsMap();

    // ---------------------------------------------------
    // Elora config variables initialization methods
    // ---------------------------------------------------
    private static EloraConfigTable initAutocopyParentTypesConfig() {
        String logInitMsg = "[initAutocopyParentTypesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = EloraConfigHelper.getAutocopyParentTypeConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static Map<String, String> initAutocopyParentTypesMap() {
        String logInitMsg = "[initAutocopyParentTypesMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, String> map = autocopyParentTypesConfig.extractConfigTablePropertyValuesAsMap(
                "id", "label");

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static EloraConfigTable initGeneralConfig() {
        String logInitMsg = "[initGeneralConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = EloraConfigHelper.getGeneralConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static Map<String, String> initGeneralConfigMap() {
        String logInitMsg = "[initGeneralConfigMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, String> map = generalConfig.extractConfigTablePropertyValuesAsMap(
                "id", "label");

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static EloraConfigTable initUnitConversionConfig() {
        String logInitMsg = "[initUnitConversionConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = EloraConfigHelper.getUnitConversionConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static Map<String, EloraConfigRow> initUnitConversionConfigMap() {
        String logInitMsg = "[initUnitConversionConfigMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);
        Map<String, EloraConfigRow> map = new HashMap<String, EloraConfigRow>();

        for (EloraConfigRow row : unitConversionConfig.getValues()) {
            map.put((String) row.getProperty(
                    EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_BASE_UNIT),
                    row);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static EloraConfigTable initCheckoutConfig() {
        String logInitMsg = "[initCheckoutConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = EloraConfigHelper.getCheckoutConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static Map<String, String> initCheckoutSwitchChildrenMap() {
        String logInitMsg = "[initCheckoutSwitchChildrenMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, String> map = checkoutConfig.extractConfigTablePropertyValuesAsMap(
                "id", "defaultSwitchChildrenOption");

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static EloraConfigTable initIntegrationVersionControlConfig() {
        String logInitMsg = "[initIntegrationVersionControlConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = EloraConfigHelper.getIntegrationVersionControlConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static Map<String, Integer> initIntegrationMinAllowedVersionsMap() {
        String logInitMsg = "[initIntegrationMinAllowedVersionsMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);
        Map<String, Integer> map = new HashMap<String, Integer>();

        for (EloraConfigRow row : integrationVersionControlConfig.getValues()) {
            Long minAllowedVersionLong = (long) row.getProperty(
                    EloraConfigConstants.PROP_INTEGRATION_VERSION_CONTROL_MIN_ALLOWED_VERSION);
            Integer minAllowedVersion = minAllowedVersionLong != null
                    ? minAllowedVersionLong.intValue() : null;

            map.put((String) row.getProperty(EloraConfigConstants.PROP_ID),
                    minAllowedVersion);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

}
