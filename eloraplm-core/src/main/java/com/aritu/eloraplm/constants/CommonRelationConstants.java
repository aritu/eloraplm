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

public class CommonRelationConstants {

    // Graphs
    public static final String GRAPH_CORE_RELATION = "CoreRelationGraph";

    public static final String ELORA_METADATA_NAMESPACE = "http://www.eloraplm.aritu.com/metadata/";

    // Relations' metadata resources
    public static final Resource RES_COMMREL_IS_EXCLUDED_FROM_MAPPING = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "IsExcludedFromMapping");

    /*
    public static final Resource IS_MANUAL = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "IsManual");
    
    public static final Resource IS_TEMPLATE = new ResourceImpl(
            ELORA_METADATA_NAMESPACE + "IsTemplate");
     */

    // Relations' metadata
    public static final String COMMREL_IS_EXCLUDED_FROM_MAPPING = "commrel:isExcludedFromMapping";

}
