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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class PropagationConfigHelper {

    protected static EloraConfigManager configService = Framework.getService(
            EloraConfigManager.class);

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCheckoutPropagationConfig()
            throws EloraException {
        return getActionPropagationConfig("checkout",
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getGetPropagationConfig()
            throws EloraException {
        return getActionPropagationConfig("get",
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static HashMap<String, List<EloraConfigRow>> getApprovePropagationConfig()
            throws EloraException {
        EloraConfigTable configTable = getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_APPROVE, null,
                null, null, false);
        return getConfigMap(configTable,
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION);
    }

    public static HashMap<String, List<EloraConfigRow>> getObsoletePropagationMap()
            throws EloraException {
        EloraConfigTable configTable = getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_OBSOLETE, null,
                null, null, false);

        return getConfigMap(configTable,
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION);
    }

    private static HashMap<String, List<EloraConfigRow>> getConfigMap(
            EloraConfigTable configTable, String key) {
        HashMap<String, List<EloraConfigRow>> configMap = new HashMap<String, List<EloraConfigRow>>();
        for (EloraConfigRow configRow : configTable.getValues()) {
            String relation = configRow.getProperty(key).toString();

            if (configMap.containsKey(relation)) {
                configMap.get(relation).add(configRow);
            } else {
                List<EloraConfigRow> rowList = new ArrayList<EloraConfigRow>();
                rowList.add(configRow);
                configMap.put(relation, rowList);
            }
        }
        return configMap;
    }

    public static EloraConfigTable getApproveDescendingPropagationConfig()
            throws EloraException {

        return getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_APPROVE,
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

    public static EloraConfigTable getObsoleteDescendingPropagationConfig()
            throws EloraException {

        return getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_OBSOLETE,
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getActionPropagationConfig(String action,
            String key, String direction, String predicateUri,
            boolean includeObsoletes) throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("action", action);
        if (direction != null) {
            filter.put("direction", direction);
        }
        if (predicateUri != null) {
            filter.put("relation", predicateUri);
        }
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable propagationConfig;
        if (key != null) {
            propagationConfig = configService.getConfigTable(
                    EloraConfigConstants.VOC_RELATION_PROPAGATION, key, filter,
                    null);
        } else {
            propagationConfig = configService.getConfigTable(
                    EloraConfigConstants.VOC_RELATION_PROPAGATION, filter,
                    null);
        }
        return propagationConfig;
    }

}
