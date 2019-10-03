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

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class RelationsConfigHelper {

    // ----------------------------------------------------------------
    // CAD relations
    // ----------------------------------------------------------------
    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadHierarchicalRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_HIERARCHICAL,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadSuppressedRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_SUPPRESSED,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadDirectRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadSpecialRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_SPECIAL,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadIconOnlyRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_ICONONLY,
                includeObsoletes);
    }

    public static EloraConfigTable getCadRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD, "",
                includeObsoletes);
    }

    // ----------------------------------------------------------------
    // BOM relations
    // ----------------------------------------------------------------
    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomHierarchicalRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_HIERARCHICAL,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomDirectRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomDocumentRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_DOCUMENT,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomAnarchicRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_ANARCHIC,
                includeObsoletes);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM, "",
                includeObsoletes);
    }

    public static EloraConfigTable getContainerRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CONTAINER,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT,
                includeObsoletes);
    }

    /**
     * @param type
     * @param subtype
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    private static EloraConfigTable getRelationsConfig(String type,
            String subtype, boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        if (type != null && !type.isEmpty()) {
            filter.put(EloraConfigConstants.PROP_RELATIONS_CONFIG_TYPE, type);
        }

        if (subtype != null && !subtype.isEmpty()) {
            filter.put(EloraConfigConstants.PROP_RELATIONS_CONFIG_SUBTYPE,
                    subtype);
        }

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable relationsConfig = EloraConfigHelper.configService.getConfigTable(
                EloraConfigConstants.VOC_RELATIONS_CONFIG, filter, null);

        return relationsConfig;
    }

}
