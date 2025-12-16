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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomConfig {

    private static final Log log = LogFactory.getLog(BomConfig.class);

    // ---------------------------
    // BOM variables
    // ---------------------------
    public static EloraConfigTable bomPartClassificationConfig = initBomClassificationConfig(
            EloraDoctypeConstants.BOM_PART);

    public static EloraConfigTable bomProductClassificationConfig = initBomClassificationConfig(
            EloraDoctypeConstants.BOM_PRODUCT);

    public static EloraConfigTable bomToolClassificationConfig = initBomClassificationConfig(
            EloraDoctypeConstants.BOM_TOOL);

    public static EloraConfigTable bomPackagingClassificationConfig = initBomClassificationConfig(
            EloraDoctypeConstants.BOM_PACKAGING);

    public static EloraConfigTable bomSpecificationClassificationConfig = initBomClassificationConfig(
            EloraDoctypeConstants.BOM_SPECIFICATION);

    public static HashMap<String, HashMap<String, String>> bomClassificationLabelMap = initBomClassificationLabelMap();

    // --------------------------------------------------
    // BOM variables initialization methods
    // --------------------------------------------------
    private static EloraConfigTable initBomClassificationConfig(
            String bomType) {
        String logInitMsg = "[initBomClassificationConfig] bomType = |"
                + bomType + "|";

        log.trace("********************************* ENTER IN " + logInitMsg);
        EloraConfigTable configTable = null;
        try {
            configTable = BomConfigHelper.getBomClassificationConfig(bomType,
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static HashMap<String, HashMap<String, String>> initBomClassificationLabelMap() {
        String logInitMsg = "[initBomClassificationLabelMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        HashMap<String, HashMap<String, String>> configMap = new HashMap<String, HashMap<String, String>>();

        configMap.put(EloraDoctypeConstants.BOM_PART,
                bomPartClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));
        configMap.put(EloraDoctypeConstants.BOM_PRODUCT,
                bomProductClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));
        configMap.put(EloraDoctypeConstants.BOM_TOOL,
                bomToolClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));
        configMap.put(EloraDoctypeConstants.BOM_PACKAGING,
                bomPackagingClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));
        configMap.put(EloraDoctypeConstants.BOM_SPECIFICATION,
                bomSpecificationClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    public static void reloadBomClassificationConfig(String bomType) {
        EloraConfigTable bomClassificationConfig = initBomClassificationConfig(
                bomType);

        bomClassificationLabelMap.put(bomType,
                bomClassificationConfig.extractConfigTablePropertyValuesAsMap(
                        "id", "label"));
    }

}
