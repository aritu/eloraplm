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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class LifecyclesConfigHelper {

    /**
     * Returns the RELEASED states configuration, ignoring the obsolete items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getReleasedStatesConfig()
            throws EloraException {
        return getStatesConfig(EloraLifeCycleConstants.STATUS_RELEASED, false);
    }

    /**
     * Returns OBSOLETE states configuration, ignoring the obsolete items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getObsoleteStatesConfig()
            throws EloraException {
        return getStatesConfig(EloraLifeCycleConstants.STATUS_OBSOLETE, false);
    }

    /**
     * Returns OBSOLETE and DELETED states configuration, ignoring the obsolete
     * items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getObsoleteAndDeletedStatesConfig()
            throws EloraException {

        EloraConfigTable configTable = getObsoleteStatesConfig();

        configTable.mergeWithTable(
                getStatesConfig(EloraLifeCycleConstants.STATUS_DELETED, false));

        return configTable;
    }

    /**
     * Returns lifecycle states configuration, ignoring the obsolete items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getAllStatesConfig() throws EloraException {
        return getStatesConfig(null, false);
    }

    // TODO: Hau goikoarekin elkartu daiteke
    /**
     * Returns lifecycle states configuration.
     *
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    private static EloraConfigTable getStatesConfig(String status,
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (status != null) {
            filter.put(EloraConfigConstants.PROP_LIFECYCLE_STATES_STATUS,
                    status);
        }
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable statesConfig = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_LIFECYCLE_STATES, filter, null);

        return statesConfig;
    }

    public static EloraConfigTable getLockableStatesConfig(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        filter.put(EloraConfigConstants.PROP_LIFECYCLE_STATE_ISLOCKABLE, "1");

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable statesConfig = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_LIFECYCLE_STATES, filter, null);

        return statesConfig;
    }

    public static EloraConfigTable getAllowedByAllStatesTransitionsConfig(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        filter.put(EloraConfigConstants.PROP_ALLOWED_BY_ALL_STATES, "1");

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable statesConfig = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_TRANSITION_CONFIG, filter, null);

        return statesConfig;
    }

    public static EloraConfigTable getAllowsAllStatesTransitionsConfig(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        filter.put(EloraConfigConstants.PROP_ALLOWS_ALL_STATES, "1");

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable statesConfig = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_TRANSITION_CONFIG, filter, null);

        return statesConfig;
    }

    public static HashMap<String, List<String>> getSupportedStatesMap()
            throws EloraException {
        return getSupportedStatesMap(false);
    }

    public static HashMap<String, List<String>> getSupportedStatesMap(
            boolean includeObsoletes) throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable configTable = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_CHILDREN_SUPPORTED_STATES, null, null);

        HashMap<String, List<String>> configMap = new HashMap<String, List<String>>();
        for (EloraConfigRow configRow : configTable.getValues()) {
            String parentState = configRow.getProperty(
                    EloraConfigConstants.PROP_LIFECYCLE_PARENT_STATE).toString();
            String childState = configRow.getProperty(
                    EloraConfigConstants.PROP_LIFECYCLE_CHILDREN_STATE).toString();
            if (configMap.containsKey(parentState)) {
                configMap.get(parentState).add(childState);
            } else {
                List<String> childrenList = new ArrayList<String>();
                childrenList.add(childState);
                configMap.put(parentState, childrenList);
            }
        }
        return configMap;
    }

    public static Map<String, Map<String, List<String>>> getDemoteTransitionsMap()
            throws EloraException {
        return getTransitionsMap(false,
                EloraConfigConstants.PROP_LIFECYCLE_TRANSITIONS_DEMOTE);
    }

    public static Map<String, Map<String, List<String>>> getPromoteTransitionsMap()
            throws EloraException {
        return getTransitionsMap(false,
                EloraConfigConstants.PROP_LIFECYCLE_TRANSITIONS_PROMOTE);
    }

    public static Map<String, Map<String, List<String>>> getTransitionsMap(
            boolean includeObsoletes, String type) throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable configTable = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_LIFECYCLE_TRANSITIONS, null, null);

        Map<String, Map<String, List<String>>> configMap = new HashMap<String, Map<String, List<String>>>();
        for (EloraConfigRow configRow : configTable.getValues()) {
            String lifecycle = configRow.getProperty(
                    EloraConfigConstants.PROP_LIFECYCLE_TRANSITIONS_LIFECYCLE).toString();
            String state = configRow.getProperty(
                    EloraConfigConstants.PROP_LIFECYCLE_TRANSITIONS_STATE).toString();

            String transitions = configRow.getProperty(type) != null
                    ? configRow.getProperty(type).toString()
                    : "";
            List<String> transitionList = new ArrayList<String>();
            if (!transitions.isEmpty()) {
                transitionList = Arrays.asList(transitions.split(","));
            }

            if (configMap.containsKey(lifecycle)) {
                configMap.get(lifecycle).put(state, transitionList);
            } else {
                Map<String, List<String>> lifecycleConfig = new HashMap<String, List<String>>();
                lifecycleConfig.put(state, transitionList);
                configMap.put(lifecycle, lifecycleConfig);
            }
        }

        return configMap;
    }
}
