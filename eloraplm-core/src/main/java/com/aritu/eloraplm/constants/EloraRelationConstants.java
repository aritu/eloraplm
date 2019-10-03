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

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

public class EloraRelationConstants {

    public static final String ELORA_GRAPH_NAME = "EloraRelations";

    public static final String ELORA_METADATA_NAMESPACE = "http://www.eloraplm.aritu.com/metadata/";

    // CAD relations
    public static final String CAD_BASED_ON = "http://eloraplm.aritu.com/relations/CadBasedOn";

    public static final String CAD_COMPOSED_OF = "http://eloraplm.aritu.com/relations/CadComposedOf";

    public static final String CAD_DRAWING_OF = "http://eloraplm.aritu.com/relations/CadDrawingOf";

    public static final String CAD_HAS_DESIGN_TABLE = "http://eloraplm.aritu.com/relations/CadHasDesignTable";

    public static final String CAD_HAS_SUPPRESSED = "http://eloraplm.aritu.com/relations/CadHasSuppressed";

    // public static final String CAD_HAS_DOCUMENT =
    // "http://eloraplm.aritu.com/relations/CadHasDocument";

    // public static final String CAD_HAS_VIRTUAL_COMPONENT =
    // "http://eloraplm.aritu.com/relations/CadHasVirtualComponent";

    public static final String CAD_IN_CONTEXT_WITH = "http://eloraplm.aritu.com/relations/CadInContextWith";

    // BOM relations
    public static final String BOM_HAS_CAD_DOCUMENT = "http://eloraplm.aritu.com/relations/BomHasCadDocument";

    public static final String BOM_HAS_DOCUMENT = "http://eloraplm.aritu.com/relations/BomHasDocument";

    public static final String BOM_COMPOSED_OF = "http://eloraplm.aritu.com/relations/BomComposedOf";

    public static final String BOM_HAS_BOM = "http://eloraplm.aritu.com/relations/BomHasBom";

    public static final String BOM_HAS_SPECIFICATION = "http://eloraplm.aritu.com/relations/BomHasSpecification";

    public static final String BOM_HAS_LIST = "http://eloraplm.aritu.com/relations/BomHasList";

    public static final String BOM_LIST_HAS_ENTRY = "http://eloraplm.aritu.com/relations/BomListHasEntry";

    public static final String BOM_MANUFACTURER_HAS_PART = "http://eloraplm.aritu.com/relations/BomManufacturerHasPart";

    public static final String BOM_CUSTOMER_HAS_PRODUCT = "http://eloraplm.aritu.com/relations/BomCustomerHasProduct";

    public static final String CM_PROCESS_IS_MANAGED_IN = "http://eloraplm.aritu.com/relations/CmProcessIsManagedIn";

    // Container relations
    public static final String CONTAINER_HAS_CONTAINER = "http://eloraplm.aritu.com/relations/ContainerHasContainer";

    // Drafts
    // TODO Hasieran gehitu http://eloraplm.aritu.com/relations/ ????
    public static final String HAS_ELORA_DRAFT_RELATION = "HasEloraDraft";

    // Relations metadata
    public static final Resource QUANTITY = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "Quantity");

    public static final Resource ORDERING = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "Ordering");

    public static final Resource DIRECTOR_ORDERING = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "DirectorOrdering");

    public static final Resource VIEWER_ORDERING = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "ViewerOrdering");

    public static final Resource INVERSE_VIEWER_ORDERING = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "InverseViewerOrdering");

    public static final Resource IS_MANUAL = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "IsManual");

    // Constants for soft delete
    public static final String SOFT_DELETED_RELATION_SOURCE = "SoftDeleted";

    public static final String SOFT_DELETED_RELATION_PREDICATE = "SoftDeleted";

    public static final String SOFT_DELETED_RELATION_TARGET = "SoftDeleted";
}
