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
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraLifeCycleConstants {

    // --------------------------------------------------------
    // NUXEO LifeCycle constants ------------------------------
    // --------------------------------------------------------
    // -- STATES
    public static final String NX_DELETED = "deleted";

    // -- TRANSITIONS
    public static final String NX_TRANS_DELETE = "delete";

    public static final String NX_TRANS_UNDELETE = "undelete";

    // --------------------------------------------------------
    // ELORA LifeCycle constants ------------------------------
    // --------------------------------------------------------
    // -- STATES
    public static final String CREATED = "created";

    // TODO:: remove CAD_ prefix from all those constants
    // -- STATES
    public static final String PRECREATED = "precreated";

    public static final String PRELIMINARY = "preliminary";

    public static final String APPROVED = "approved";

    public static final String OBSOLETE = "obsolete";

    // -- TRANSITIONS
    public static final String TRANS_APPROVE = "approve";

    public static final String TRANS_OBSOLETE = "makeObsolete";

    public static final String TRANS_BACK_TO_PRELIMINARY = "backToPreliminary";

    public static final String TRANS_CREATE = "create";

    // -- WORKSPACE TRANSITIONS
    public static final String TRANS_COMPLETE = "complete";

    public static final String TRANS_BACK_TO_WORKING = "backToWorking";

}
