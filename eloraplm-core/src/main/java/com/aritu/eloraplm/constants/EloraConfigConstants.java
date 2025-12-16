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

    public static final String VOC_CAD_DERIVED_OUTPUTS_CONFIG = "elora_cad_derived_outputs_config";

    public static final String VOC_CHECKOUT_CONFIG = "elora_checkout_config";

    public static final String VOC_DOCTYPE_MAPPING = "elora_doctype_mapping";

    public static final String VOC_GENERAL_CONFIG = "elora_general_config";

    public static final String VOC_MCAD_PLUGIN_CONFIG = "elora_mcad_plugin_config";

    public static final String VOC_METADATA_MAPPING = "elora_metadata_mapping";

    public static final String VOC_RELATION_PROPAGATION = "elora_relation_propagation";

    public static final String VOC_RELATIONS_CONFIG = "elora_relations_config";

    public static final String VOC_UNIT_CONVERSION_MAPPING = "elora_unit_conversion_mapping";

    public static final String VOC_ERP_CUSTOMER = "elora_erpCustomers";

    public static final String VOC_ERP_SUPPLIER = "elora_erpSuppliers";

    public static final String VOC_INTEGRATION_VERSION_CONTROL = "elora_integration_version_control";

    public static final String VOC_VOCABULARIES_TIMESTAMPS = "elora_vocabularies_timestamps";

    public static final String VOC_DOCUMENT_METADATA_MAPPING = "elora_document_metadata_mapping";

    public static final String VOC_ELORAS_BOMS_LIFECYCLE = "eloras_boms_lifeCycle";

    public static final String VOC_ELORAS_CAD_DOCUMENTS_LIFECYCLE = "eloras_cadDocuments_lifeCycle";

    public static final String VOC_ELORAS_BASIC_DOCUMENTS_LIFECYCLE = "eloras_basicDocuments_lifeCycle";

    public static final String VOC_ELORA_UNITS_OF_MEASURE = "elora_units_of_measure";

    /*
     * Keys
     */
    public static final String KEY_DECIMAL_MAX_INTEGER_PLACES = "decimalMaxIntegerPlaces";

    public static final String KEY_DECIMAL_MAX_DECIMAL_PLACES = "decimalMaxDecimalPlaces";

    public static final String KEY_IMPLANTATION_DATE = "implantationDate";

    public static final String KEY_INTEGRATOR_BINARY_VERSION = "integratorBinaryVersion";

    public static final String KEY_INTEGRATOR_BINARY_URL = "integratorBinaryUrl";

    public static final String KEY_SHOW_MAX_COMPONENTS = "showMaxComponents";

    /*
     * Properties
     */
    public static final String PROP_ID = "id";

    public static final String PROP_LABEL = "label";

    public static final String PROP_OBSOLETE = "obsolete";

    public static final String PROP_ORDERING = "ordering";

    public static final String PROP_LABEL_EN = "label_en";

    public static final String PROP_LABEL_ES = "label_es";

    public static final String PROP_LABEL_EU = "label_eu";

    public static final String PROP_CAD_METADATA_MAPPING_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_METADATA_TYPE = "cad_metadata_type";

    public static final String PROP_CAD_METADATA_MAPPING_CAD_METADATA_NAME = "cad_metadata_name";

    public static final String PROP_CAD_METADATA_MAPPING_DESCRIPTION = "description";

    public static final String PROP_CAD_DERIVED_OUTPUTS_CONFIG_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_CAD_DERIVED_OUTPUTS_CONFIG_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_CAD_DERIVED_OUTPUTS_CONFIG_OUTPUT_TYPE = "output_type";

    public static final String PROP_DOCTYPE_MAPPING_AUTHORING_TOOL = "authoring_tool";

    public static final String PROP_DOCTYPE_MAPPING_CAD_DOCTYPE = "cad_doctype";

    public static final String PROP_DOCTYPE_MAPPING_PLM_DOCTYPE = "plm_doctype";

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

    public static final String PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE = "orgDocType";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_ORG_METADATA = "orgMetadata";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE = "dstDocType";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_PROCESS = "dstProcess";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA = "dstMetadata";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE = "dstConsolidationMode";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE = "dstMetadataOverwrite";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_DST_EXEC_METHOD_NAME = "dstExecMethodName";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE = "relPredicate";

    public static final String PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE = "relOrgRole";

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

    public static final String VAL_RELATIONS_CONFIG_TYPE_DOC = "DOC";

    public static final String VAL_RELATIONS_CONFIG_TYPE_CONTAINER = "CONTAINER";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_HIERARCHICAL = "Hierarchical";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_SUPPRESSED = "Suppressed";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_DIRECT = "Direct";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_ITEM_DIRECT = "ItemDirect";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_CAD_DIRECT = "CadDirect";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_BASIC = "Basic";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_CAD = "Cad";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_ANARCHIC = "Anarchic";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_BOMLIST = "BomList";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_SPECIAL = "Special";

    public static final String VAL_RELATIONS_CONFIG_SUBTYPE_ICONONLY = "IconOnly";

    public static final String VAL_RELATION_PROPAGATION_APPROVE = "approve";

    public static final String VAL_RELATION_PROPAGATION_OBSOLETE = "obsolete";

    public static final String VAL_RELATION_PROPAGATION_DESCENDING = "0";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_SET_METADATA_VALUE = "setMetadataValue";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_EXEC_METHOD = "execMethod";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE_SUM = "sum";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_N = "N";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_Y = "Y";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_Y_ONE = "Y_ONE";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_SUBJECT = "subject";

    public static final String VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_OBJECT = "object";

}
