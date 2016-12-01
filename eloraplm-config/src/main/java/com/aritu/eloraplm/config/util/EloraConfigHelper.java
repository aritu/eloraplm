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

    protected static EloraConfigManager configService = Framework.getService(EloraConfigManager.class);

    /**
     * Returns the metadata override configuration (Bidirectional and PlmToCad
     * properties) for the provided doctype, ignoring the obsolete items.
     *
     * @param type
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getOverrideMetadataConfig(String type)
            throws EloraException {
        return getOverrideMetadataConfig(type, false);
    }

    /**
     * Returns the metadata override configuration (Bidirectional and PlmToCad
     * properties) for the provided doctype.
     *
     * @param type
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getOverrideMetadataConfig(String type,
            boolean includeObsoletes) throws EloraException {
        // Get Bidirectional properties
        Map<String, Serializable> bidirectionalFilter = new HashMap<>();
        bidirectionalFilter.put("plm_doctype", type);
        bidirectionalFilter.put("update_mode", "Bidirectional");
        if (!includeObsoletes) {
            bidirectionalFilter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable bidirectionalConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_METADATA_MAPPING, "plm_metadata",
                bidirectionalFilter, null);

        // Get PlmToCad properties
        Map<String, Serializable> plmToCadFilter = new HashMap<>();
        plmToCadFilter.put("plm_doctype", type);
        plmToCadFilter.put("update_mode", "PlmToCad");
        if (!includeObsoletes) {
            plmToCadFilter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable plmToCadConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_METADATA_MAPPING, "plm_metadata",
                plmToCadFilter, null);

        // Merge properties
        EloraConfigTable overrideConfig = new EloraConfigTable();
        overrideConfig.mergeWithTable(bidirectionalConfig);
        overrideConfig.mergeWithTable(plmToCadConfig);

        return overrideConfig;
    }

    /**
     * Returns the metadata configuration (CadToPlm, Bidirectional and PlmToCad
     * properties) for the provided doctype.
     *
     * @param type
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getAllMetadataConfig(String type)
            throws EloraException {
        return getAllMetadataConfig(type, false);
    }

    /**
     * Returns the metadata configuration (CadToPlm, Bidirectional and PlmToCad
     * properties) for the provided doctype.
     *
     * @param type
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getAllMetadataConfig(String type,
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        filter.put("plm_doctype", type);
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable overrideConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_METADATA_MAPPING, "plm_metadata",
                filter, null);

        return overrideConfig;
    }

    /**
     * Returns the released lifecycle states configuration, ignoring the
     * obsolete items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getReleasedLifecycleStatesConfig()
            throws EloraException {
        return getLifecycleStatesConfig("released", false);
    }

    /**
     * Returns obsolete lifecycle states configuration, ignoring the obsolete
     * items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getObsoleteLifecycleStatesConfig()
            throws EloraException {
        return getLifecycleStatesConfig(EloraConfigConstants.PROP_OBSOLETE,
                false);
    }

    /**
     * Returns lifecycle states configuration, ignoring the obsolete items.
     *
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getLifecycleStatesConfig()
            throws EloraException {
        return getLifecycleStatesConfig(null, false);
    }

    // TODO: Hau goikoarekin elkartu daiteke
    /**
     * Returns lifecycle states configuration.
     *
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getLifecycleStatesConfig(String status,
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (status != null) {
            filter.put(EloraConfigConstants.PROP_LIFECYCLE_STATES_STATUS,
                    status);
        }
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
        EloraConfigTable statesConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_LIFECYCLE_STATES, filter, null);

        return statesConfig;
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getGetForCheckoutPropagationConfig()
            throws EloraException {
        return getActionPropagationConfig("getCheckout",
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

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
    public static EloraConfigTable getGetForOpenPropagationConfig()
            throws EloraException {
        return getActionPropagationConfig("getOpen",
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION,
                EloraConfigConstants.VAL_RELATION_PROPAGATION_DESCENDING, null,
                false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getApprovePropagationConfig(
            String predicateUri) throws EloraException {

        return getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_APPROVE, null,
                null, predicateUri, false);
    }

    public static EloraConfigTable getApproveDescendingPropagationConfig()
            throws EloraException {

        return getActionPropagationConfig(
                EloraConfigConstants.VAL_RELATION_PROPAGATION_APPROVE,
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
                    EloraConfigConstants.VOC_RELATION_PROPAGATION, filter, null);
        }
        return propagationConfig;
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getSupportedStatesConfig(String parentState)
            throws EloraException {
        return getSupportedStatesConfig(parentState, false);
    }

    /**
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getSupportedStatesConfig(String parentState,
            boolean includeObsoletes) throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("parent_state", parentState);
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable propagationConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_CHILDREN_SUPPORTED_STATES, filter,
                null);

        return propagationConfig;
    }

    // ----------------------------------------------------------------
    // CAD relations
    // ----------------------------------------------------------------
    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadHierarchicalRelationsConfig()
            throws EloraException {
        return getCadHierarchicalRelationsConfig(false);
    }

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
                includeObsoletes, false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadDirectRelationsConfig()
            throws EloraException {
        return getCadDirectRelationsConfig(false);
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
                includeObsoletes, false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCadRelationsConfig()
            throws EloraException {
        return getCadRelationsConfig(false, true);
    }

    public static EloraConfigTable getCadRelationsConfig(
            boolean includeObsoletes, boolean includeIconOnly)
            throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_CAD, "",
                includeObsoletes, includeIconOnly);
    }

    // ----------------------------------------------------------------
    // BOM relations
    // ----------------------------------------------------------------
    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomHierarchicalRelationsConfig()
            throws EloraException {
        return getBomHierarchicalRelationsConfig(false);
    }

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
                includeObsoletes, false);
    }

    public static EloraConfigTable getBomDirectRelationsConfig()
            throws EloraException {
        return getBomDirectRelationsConfig(false);
    }

    public static EloraConfigTable getBomDirectRelationsConfig(
            boolean includeObsoletes) throws EloraException {
        return getRelationsConfig(
                EloraConfigConstants.VAL_RELATIONS_CONFIG_TYPE_BOM,
                EloraConfigConstants.VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT,
                includeObsoletes, false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomDocumentRelationsConfig()
            throws EloraException {
        return getBomDocumentRelationsConfig(false);
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
                includeObsoletes, false);
    }

    /**
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getBomRelationsConfig()
            throws EloraException {
        return getBomRelationsConfig(false);
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
                includeObsoletes, false);
    }

    /**
     * @param type
     * @param subtype
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    private static EloraConfigTable getRelationsConfig(String type,
            String subtype, boolean includeObsoletes, boolean includeIconOnly)
            throws EloraException {

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

        if (!includeIconOnly) {
            filter.put(EloraConfigConstants.PROP_ICON_ONLY, "0");
        }

        EloraConfigTable relationsConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_RELATIONS_CONFIG, filter, null);

        return relationsConfig;
    }

    public static EloraConfigTable getBomLists() throws EloraException {
        return getBomLists(false);
    }

    public static EloraConfigTable getBomLists(boolean includeObsoletes)
            throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();

        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable bomLists = configService.getConfigTable(
                EloraConfigConstants.VOC_BOM_LISTS, filter, null);

        return bomLists;
    }

    // ----------------------------------------------------------------

    /**
     * @param type
     * @return
     * @throws EloraException
     */
    public static String getAutocopyParentTypeConfig(String type)
            throws EloraException {
        return configService.getConfig(
                EloraConfigConstants.VOC_AUTOCOPY_PARENT_TYPES, type, true);
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

    /**
     * @param state
     * @return
     * @throws EloraException
     */
    public static boolean getIsLifeCycleStateLockable(String state)
            throws EloraException {
        long isLockable = (long) configService.getConfig(
                EloraConfigConstants.VOC_LIFECYCLE_STATES, state,
                EloraConfigConstants.PROP_LIFECYCLE_STATE_ISLOCKABLE, false);
        return (isLockable == 1);
    }

    // ----------------------------------------------------------------
    // Unit Conversion Mapping
    // ----------------------------------------------------------------
    /**
     * @param baseUnit
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getUnitConversionConfig(String baseUnit)
            throws EloraException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("base_unit", baseUnit.toLowerCase());
        filter.put("display", "1");

        EloraConfigTable unitConversionConfig = configService.getConfigTable(
                EloraConfigConstants.VOC_UNIT_CONVERSION_MAPPING, filter, null,
                false);

        if (unitConversionConfig.size() > 1) {
            throw new EloraException(
                    "It is not possible to have more than one active display unit for a base unit. Base Unit =|"
                            + baseUnit + "|");
        }

        return unitConversionConfig;
    }

    // ----------------------------------------------------------------
    // General configurations
    // ----------------------------------------------------------------

    public static String getProtocol() throws EloraException {
        return configService.getGeneralConfig(
                EloraConfigConstants.KEY_PROTOCOL, false);
    }

}
