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
 * General constants related to the Change Management
 *
 * @author aritu
 *
 */
public class CMBatchProcessingConstants {

    // Batch process actions
    public static final String EXECUTE_ACTIONS = "executeActions";

    public static final String FIX_RELATIONS = "fixRelations";

    public static final String CHECKIN = "checkin";

    public static final String OVERWRITE = "overwrite";

    public static final String PROMOTE = "promote";

    public static final String UNDO_CHECKOUT = "undoCheckout";

    public static final String CHECKOUT = "checkout";

    // Errors
    public static final String ERR_ALREADY_LOCKED = "AlreadyLocked";

    public static final String ERR_UNLOCKABLE_DOC = "Unlockable";

    public static final String ERR_MISSING_LOCK_RIGHTS = "MissingRights";

}
