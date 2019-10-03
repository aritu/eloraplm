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

    public static final String PRECREATED = "precreated";

    public static final String PRELIMINARY = "preliminary";

    public static final String APPROVED = "approved";

    public static final String OBSOLETE = "obsolete";

    public static final String ASYNC_PROCESSING = "asyncProcessing";

    // -- STATUS
    public static final String STATUS_RELEASED = "released";

    public static final String STATUS_NOT_RELEASED = "notReleased";

    public static final String STATUS_OBSOLETE = "obsolete";

    public static final String STATUS_DELETED = "deleted";

    // -- TRANSITIONS
    public static final String TRANS_APPROVE = "approve";

    public static final String TRANS_OBSOLETE = "makeObsolete";

    public static final String TRANS_BACK_TO_PRELIMINARY = "backToPreliminary";

    public static final String TRANS_CREATE = "create";

    public static final String TRANS_REJECT = "reject";

    public static final String TRANS_COMPLETE = "complete";

    public static final String TRANS_MANAGE = "manage";

    public static final String TRANS_BACK_TO_CREATED = "backToCreated";

    public static final String TRANS_BACK_TO_WORKING = "backToWorking";

    public static final String TRANS_START_ASYNC_PROCESS = "startAsyncProcess";

    // -- LIFECYCLES
    public static final String CAD_LIFE_CYCLE = "eloraCadLifeCycle";

    public static final String BOM_LIFE_CYCLE = "eloraBomLifeCycle";

}
