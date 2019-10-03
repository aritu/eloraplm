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

package com.aritu.eloraplm.constants;

public class EloraConfigConstants {

    /*
     * Vocabularies
     */
    public static final String VOC_AUTOCOPY_PARENT_TYPES = "elora_autocopy_parent_types";

    public static final String VOC_BOM_LISTS = "elora_bom_lists";

    public static final String VOC_CAD_METADATA_MAPPING = "elora_cad_metadata_mapping";

    public static final String VOC_CHECKOUT_CONFIG = "elora_checkout_config";

    public static final String VOC_CHILDREN_SUPPORTED_STATES = "elora_children_supported_states";

    public static final String VOC_DEFAULT_CONFIG = "elora_default_config";

    public static final String VOC_DOCTYPE_MAPPING = "elora_doctype_mapping";

    public static final String VOC_GENERAL_CONFIG = "elora_general_config";

    public static final String VOC_LIFECYCLE_STATES = "elora_lifecycle_states";

    public static final String VOC_LIFECYCLE_TRANSITIONS = "elora_lifecycle_transitions";

    public static final String VOC_METADATA_MAPPING = "elora_metadata_mapping";

    public static final String VOC_RELATION_PROPAGATION = "elora_relation_propagation";

    public static final String VOC_RELATIONS_CONFIG = "elora_relations_config";

    public static final String VOC_TRANSITION_CONFIG = "elora_transition_config";

    public static final String VOC_UNIT_CONVERSION_MAPPING = "elora_unit_conversion_mapping";

    public static final String VOC_ERP_MANUFACTURER = "elora_erpManufacturers";

    public static final String VOC_ERP_CUSTOMER = "elora_erpCustomers";

    public static final String VOC_INTEGRATION_VERSION_CONTROL = "elora_integration_version_control";

    public static final String VOC_VOCABULARIES_TIMESTAMPS = "elora_vocabularies_timestamps";

    /*
     * Keys
     */
    public static final String KEY_CHILDREN_CHECKOUT_VERSION = "children_checkout_version";

    public static final String KEY_PROTOCOL = "protocol";

    public static final String KEY_DECIMAL_MAX_INTEGER_PLACES = "decimalMaxIntegerPlaces";

    public static final String KEY_DECIMAL_MAX_DECIMAL_PLACES = "decimalMaxDecimalPlaces";

    public static final String KEY_IMPLANTATION_DATE = "implantationDate";

    /*
     * Properties
     */
    public static final String PROP_ID = "id";

    public static final String PROP_LABEL = "label";

    public static final String PROP_OBSOLETE = "obsolete";

    public static final String PROP_ORDERING = "ordering";

    public static final String PROP_CAD_METADATA_MAPPING_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_METADATA_TYPE = "cad_metadata_type";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_METADATA_NAME = "cad_metadata_name";

    public static final String PROP_CAD_METADATA_MAPPING_DESCRIPTION = "description";

    public static final String PROP_DOCTYPE_MAPPING_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_DOCTYPE_MAPPING_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_DOCTYPE_MAPPING_PLM_DOCTYPE = "plm_doctype";

    public static final String PROP_LIFECYCLE_STATES_STATUS = "status";

    public static final String PROP_LIFECYCLE_STATE_ISLOCKABLE = "isLockable";

    public static final String PROP_LIFECYCLE_PARENT_STATE = "parent_state";

    public static final String PROP_LIFECYCLE_CHILDREN_STATE = "children_state";

    public static final String PROP_LIFECYCLE_TRANSITIONS_LIFECYCLE = "lifecycle";

    public static final String PROP_LIFECYCLE_TRANSITIONS_STATE = "state";

    public static final String PROP_LIFECYCLE_TRANSITIONS_DEMOTE = "demoteTransitions";

    public static final String PROP_LIFECYCLE_TRANSITIONS_PROMOTE = "promoteTransitions";

    public static final String PROP_METADATA_MAPPING_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_METADATA_MAPPING_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_METADATA_MAPPING_CAD_METADATA_NAME = "cad_metadata_name";

    public static final String PROP_METADATA_MAPPING_PLM_DOCTYPE = "plm_doctype";

    public static final String PROP_METADATA_MAPPING_PLM_METADATA = "plm_metadata";

    public static final String PROP_METADATA_MAPPING_PLM_METADATA_TYPE = "plm_metadata_type";

    public static final String PROP_METADATA_MAPPING_UPDATE_MODE = "update_mode";

    public static final String PROP_RELATIONS_CONFIG_TYPE = "type";

    public static final String PROP_RELATIONS_CONFIG_SUBTYPE = "subtype";

    public static final String PROP_UNIT_CONVERSION_MAPPING_BASE_UNIT = "base_unit";

    public static final String PROP_UNIT_CONVERSION_MAPPING_DISPLAY = "display";

    public static final String PROP_UNIT_CONVERSION_MAPPING_DISPLAY_UNIT = "display_unit";

    public static final String PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR = "conversion_factor";

    public static final String PROP_UNIT_CONVERSION_MAPPING_DECIMAL_PLACES = "decimal_places";

    public static final String PROP_RELATION_PROPAGATION_ACTION = "action";

    public static final String PROP_RELATION_PROPAGATION_RELATION = "relation";

    public static final String PROP_RELATION_PROPAGATION_DIRECTION = "direction";

    public static final String PROP_RELATION_PROPAGATION_PROPAGATE = "propagate";

    public static final String PROP_RELATION_PROPAGATION_ENFORCE = "enforce";

    public static final String PROP_ALLOWED_BY_ALL_STATES = "allowedByAllStates";

    public static final String PROP_ALLOWS_ALL_STATES = "allowsAllStates";

    public static final String PROP_INTEGRATION_VERSION_CONTROL_MIN_ALLOWED_VERSION = "min_allowed_version";

    /*
     * Values
     */
    public static final String VAL_METADATA_MAPPING_PLM_METADATA_TYPE_REAL = "real";

    public static final String VAL_METADATA_MAPPING_PLM_METADATA_TYPE_VIRTUAL = "virtual";

    public static final String VAL_METADATA_MAPPING_UPDATE_MODE_PLMTOCAD = "PlmToCad";

    public static final String VAL_METADATA_MAPPING_UPDATE_MODE_BIDIRECTIONAL = "Bidirectional";

    public static final String VAL_METADATA_MAPPING_UPDATE_MODE_CADTOPLM = "CadToPlm";

    public static final String VAL_RELATIONS_CONFIG_TYPE_CAD = "CAD";

    public static final String VAL_RELATIONS_CONFIG_TYPE_BOM = "BOM";

    public static final String VAL_RELATIONS_CONFIG_TYPE_CONTAINER = "CONTAINER";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_HIERARCHICAL = "Hierarchical";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_SUPPRESSED = "Suppressed";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT = "Direct";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_DOCUMENT = "Document";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_ANARCHIC = "Anarchic";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_SPECIAL = "Special";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_ICONONLY = "IconOnly";

    public static final String VAL_RELATION_PROPAGATION_APPROVE = "approve";

    public static final String VAL_RELATION_PROPAGATION_OBSOLETE = "obsolete";

    public static final String VAL_RELATION_PROPAGATION_DESCENDING = "0";

}
