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

    // For root documents that are under the domain level: StructureRoot,
    // CollaborationRoot, LibraryRoot, AdminRoot, etc.
    public static final String FACET_ELORA_ROOT_FOLDER = "EloraRootFolder";

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
    // CAUTION! At the moment, we have no way to limit BasicDocument
    // to REAL basic documents, so CAD and BOM docs also have this
    // facet
    public static final String FACET_BASIC_DOCUMENT = "BasicDocument";

    // For automatic code creation
    public static final String FACET_AUTOMATIC_CODE = "AutomaticCode";

    // For domain children that contain workspaces inside
    public static final String FACET_WORKABLE_DOMAIN_CHILD = "WorkableDomainChild";

    // For Change Management processes
    public static final String FACET_CM_PROCESS = "CMProcess";

    // For Quality Management processes
    public static final String FACET_QM_PROCESS = "QmProcess";

    // For Non-Versionable documents than must be locked to edit
    public static final String FACET_LOCK_REQUIRED_TO_EDIT = "LockRequiredToEdit";

    // To define if the document can be overwritten
    public static final String FACET_OVERWRITABLE = "Overwritable";

    // To define if the document stores check in info
    public static final String FACET_STORE_CHECKIN_INFO = "StoreCheckInInfo";

    // To define if the document stores review info
    public static final String FACET_STORE_REVIEW_INFO = "StoreReviewInfo";

    // To define if the document stores states log
    public static final String FACET_STORE_STATES_LOG = "StoreStatesLog";

}
