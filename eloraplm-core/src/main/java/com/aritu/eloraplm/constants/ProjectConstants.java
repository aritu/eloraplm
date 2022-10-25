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
 *
 * @author aritu
 *
 */
public class ProjectConstants {

    /* Phase types */

    public static final String PROJECT_PHASE_TYPE_PHASE = "phase";

    public static final String PROJECT_PHASE_TYPE_SUBPHASE = "subphase";

    public static final String PROJECT_PHASE_TYPE_GATE = "gate";

    /* Phase row data */

    public static final String PROJECT_PHASE_ID = "phaseId";

    public static final String PROJECT_PHASE_PARENTID = "parentId";

    public static final String PROJECT_PHASE_TYPE = "type";

    public static final String PROJECT_PHASE_TITLE = "phaseTitle";

    public static final String PROJECT_PHASE_DESCRIPTION = "phaseDescription";

    public static final String PROJECT_PHASE_MANAGER = "phaseManager";

    public static final String PROJECT_PHASE_DELIVERABLES = "deliverables";

    public static final String PROJECT_PHASE_REALSTARTDATE = "phaseRealStartDate";

    public static final String PROJECT_PHASE_PLANNEDENDDATE = "phasePlannedEndDate";

    public static final String PROJECT_PHASE_REALENDDATE = "phaseRealEndDate";

    public static final String PROJECT_PHASE_PROGRESS = "phaseProgress";

    public static final String PROJECT_PHASE_COMMENT = "comment";

    public static final String PROJECT_PHASE_RESULT = "result";

    public static final String PROJECT_PHASE_OBSOLETE = "obsolete";

    /* Deliverables info */

    public static final String PROJECT_PHASE_DELIVERABLES_ISREQUIRED = "isRequired";

    public static final String PROJECT_PHASE_DELIVERABLES_NAME = "name";

    public static final String PROJECT_PHASE_DELIVERABLES_DOCUMENTWCPROXY = "documentWCProxy";

    public static final String PROJECT_PHASE_DELIVERABLES_DOCUMENTAV = "documentAV";

    public static final String PROJECT_PHASE_DELIVERABLES_ANCHORINGMSG = "anchoringMsg";

    public static final String PROJECT_PHASE_DELIVERABLES_LINK = "link";

    /* Deliverables status */

    public static final String PROJECT_PHASE_DELIVERABLES_STATUS_ALL_OPTIONAL = "allOptional";

    public static final String PROJECT_PHASE_DELIVERABLES_STATUS_NOT_DEFINED = "notDefined";

    public static final String PROJECT_PHASE_DELIVERABLES_STATUS_NOT_ANCHORED = "notAnchored";

    public static final String PROJECT_PHASE_DELIVERABLES_STATUS_COMPLETED = "completed";

    /* Actions */

    public static final String PROJECT_PHASE_MOVE_DOWN = "down";

    public static final String PROJECT_PHASE_MOVE_TOP = "top";

    /* Misc */
    public static final String PROJECT_PHASE_FIRST_ROW_ID = "001";

    public static final String PROJECT_PHASE_ROW_ID_SEPARATOR = "_";
}
