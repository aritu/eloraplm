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

/**
 * @author aritu
 *
 */
public class EloraFacetConstants {

    // For documents derived from CadDocument: CadAssembly, CadPart, CadDrawing,
    // Design Table,...
    public static final String FACET_CAD_DOCUMENT = "CadDocument";

    // For drafts, used in the checkin process (integration)
    public static final String FACET_ELORA_DRAFT = "EloraDraft";

    // For doctypes that will be placed under the WorkspaceRoot: Workspace,
    // Project, CmEco, ...
    public static final String FACET_ELORA_WORKSPACE = "EloraWorkspace";

    // For BOM documents: BomPart, BomManufacturerPart, BomCustomerProduct,...
    public static final String FACET_BOM_DOCUMENT = "BomDocument";

    // For basic documents: File, Note, Audio, Picture, Video
    public static final String FACET_BASIC_DOCUMENT = "BasicDocument";

}
