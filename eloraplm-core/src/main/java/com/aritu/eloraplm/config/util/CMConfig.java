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

import com.aritu.eloraplm.constants.CMConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CMConfig {

    private static final Log log = LogFactory.getLog(CMConfig.class);

    // ---------------------------
    // Change Management variables
    // ---------------------------
    public static final EloraConfigTable doctypeActionsImpactConfig = initDoctypeActionsImpactConfig();

    public static final HashMap<String, CMImpactableConfig> docTypeActionsImpactConfigMap = initDoctypeActionsImpactMap();

    public static final EloraConfigTable modifiedActionsConfig = initModifiedActionsConfig();

    public static final HashMap<String, String> modifiedActionsLabelMap = initModifiedActionsLabelMap();

    // --------------------------------------------------
    // Change Management variables initialization methods
    // --------------------------------------------------
    private static EloraConfigTable initDoctypeActionsImpactConfig() {
        String logInitMsg = "[initDoctypeActionsImpactConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = CMConfigHelper.getDoctypeActionsImpactConfigTable(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static HashMap<String, CMImpactableConfig> initDoctypeActionsImpactMap() {
        String logInitMsg = "[initDoctypeActionsImpactMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        HashMap<String, CMImpactableConfig> configMap = new HashMap<String, CMImpactableConfig>();

        try {

            for (EloraConfigRow configRow : doctypeActionsImpactConfig.getValues()) {

                String id = configRow.getProperty("id").toString();

                String docType = configRow.getProperty(
                        CMConfigConstants.PROP_CM_DOCTYPE_ACTIONS_IMPACT_DOCTYPE).toString();

                String action = configRow.getProperty(
                        CMConfigConstants.PROP_CM_DOCTYPE_ACTIONS_IMPACT_ACTION).toString();
                Object impactableValue = configRow.getProperty(
                        CMConfigConstants.PROP_CM_DOCTYPE_ACTIONS_IMPACT_IMPACTABLE);

                boolean isImpactable = ((long) impactableValue == 1);

                Object defaultValue = configRow.getProperty(
                        CMConfigConstants.PROP_CM_DOCTYPE_ACTIONS_IMPACT_DEFAULT);

                boolean includeInImpactMatrixDefaultValue = ((long) defaultValue == 1);

                CMImpactableConfig impactableConfigData = new CMImpactableConfig(
                        docType, action, isImpactable,
                        includeInImpactMatrixDefaultValue);

                configMap.put(id, impactableConfigData);
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    private static EloraConfigTable initModifiedActionsConfig() {
        String logInitMsg = "[initModifiedActionsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);
        EloraConfigTable configTable = null;
        try {
            configTable = CMConfigHelper.getModifiedActionsConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static HashMap<String, String> initModifiedActionsLabelMap() {
        String logInitMsg = "[initModifiedActionsLabelMap] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        HashMap<String, String> configMap = modifiedActionsConfig.extractConfigTablePropertyValuesAsMap(
                "id", "label");

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

}
