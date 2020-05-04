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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class LifecyclesConfig {
    private static final Log log = LogFactory.getLog(LifecyclesConfig.class);

    // ----------------------------
    // Life cycles states variables
    // ----------------------------
    public static final EloraConfigTable allStatesConfig = initAllStatesConfig();

    public static final EloraConfigTable releasedStatesConfig = initReleasedStatesConfig();

    public static final List<String> releasedStatesList = initReleasedStatesList();

    public static final EloraConfigTable notReleasedStatesConfig = initNotReleasedStatesConfig();

    public static final List<String> notReleasedStatesList = initNotReleasedStatesList();

    public static final EloraConfigTable obsoleteStatesConfig = initObsoleteStatesConfig();

    public static final List<String> obsoleteStatesList = initObsoleteStatesList();

    public static final EloraConfigTable lockableStatesConfig = initLockableStatesConfig();

    public static final List<String> lockableStatesList = initLockableStatesList();

    public static final EloraConfigTable allowedByAllStatesTransitionsConfig = initAllowedByAllStatesTransitionsConfig();

    public static final List<String> allowedByAllStatesTransitionsList = initAllowedByAllStatesTransitionsList();

    public static final EloraConfigTable allowsAllStatesTransitionsConfig = initAllowsAllStatesTransitionsConfig();

    public static final List<String> allowsAllStatesTransitionsList = initAllowsAllStatesTransitionsList();

    public static final Map<String, List<String>> supportedStatesMap = initSupportedStatesMap();

    public static final Map<String, Map<String, List<String>>> demoteTransitions = initDemoteTransitionsMap();

    public static final Map<String, Map<String, List<String>>> promoteTransitions = initPromoteTransitionsMap();

    // ---------------------------------------------------
    // Life cycles states variables initialization methods
    // ---------------------------------------------------
    private static EloraConfigTable initAllStatesConfig() {
        String logInitMsg = "[initAllStatesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getAllStatesConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static EloraConfigTable initReleasedStatesConfig() {
        String logInitMsg = "[initReleasedStatesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getReleasedStatesConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initReleasedStatesList() {
        String logInitMsg = "[initReleasedStatesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                releasedStatesConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initNotReleasedStatesConfig() {
        String logInitMsg = "[initNotReleasedStatesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getNotReleasedStatesConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initNotReleasedStatesList() {
        String logInitMsg = "[initNotReleasedStatesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                notReleasedStatesConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initObsoleteStatesConfig() {
        String logInitMsg = "[initObsoleteStatesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getObsoleteStatesConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initObsoleteStatesList() {
        String logInitMsg = "[initObsoleteStatesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                obsoleteStatesConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initLockableStatesConfig() {
        String logInitMsg = "[initLockableStatesConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getLockableStatesConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initLockableStatesList() {
        String logInitMsg = "[initLockableStatesList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                lockableStatesConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initAllowedByAllStatesTransitionsConfig() {
        String logInitMsg = "[initAllowedByAllStatesTransitionsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getAllowedByAllStatesTransitionsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initAllowedByAllStatesTransitionsList() {
        String logInitMsg = "[initAllowedByAllStatesTransitionsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                allowedByAllStatesTransitionsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initAllowsAllStatesTransitionsConfig() {
        String logInitMsg = "[initAllowsAllStatesTransitionsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = LifecyclesConfigHelper.getAllowsAllStatesTransitionsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initAllowsAllStatesTransitionsList() {
        String logInitMsg = "[initAllowsAllStatesTransitionsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                allowsAllStatesTransitionsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static Map<String, List<String>> initSupportedStatesMap() {
        String logInitMsg = "[initObsoletePropagationConfig] ";
        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, List<String>> configMap = new HashMap<String, List<String>>();
        try {
            configMap = LifecyclesConfigHelper.getSupportedStatesMap();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    private static Map<String, Map<String, List<String>>> initDemoteTransitionsMap() {
        String logInitMsg = "[initDemoteTransitionsMap] ";
        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> configMap = new HashMap<String, Map<String, List<String>>>();
        try {
            configMap = LifecyclesConfigHelper.getDemoteTransitionsMap();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    private static Map<String, Map<String, List<String>>> initPromoteTransitionsMap() {
        String logInitMsg = "[initDemoteTransitionsMap] ";
        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> configMap = new HashMap<String, Map<String, List<String>>>();
        try {
            configMap = LifecyclesConfigHelper.getPromoteTransitionsMap();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }
}
