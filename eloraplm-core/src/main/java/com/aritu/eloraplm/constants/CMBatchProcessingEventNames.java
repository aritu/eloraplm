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
 * This class contains the constants related to the Events of Elora Change
 * Management.
 *
 * @author aritu
 *
 */
public class CMBatchProcessingEventNames {

    // Batch processing related events

    // ITEMS
    public static final String EXECUTE_ACTIONS_ITEMS = "CMBPExecuteActionsItems";

    public static final String CHECKIN_ITEMS = "CMBPCheckinItems";

    public static final String OVERWRITE_ITEMS = "CMBPOverwriteItems";

    public static final String PROMOTE_ITEMS = "CMBPPromoteItems";

    public static final String UNDO_CHECKOUT_ITEMS = "CMBPUndoCheckoutItems";

    // DOCS
    public static final String PROMOTE_DOCS = "CMBPPromoteDocs";

    // MODIFIED ITEMS
    public static final String CHECKOUT_MODIFIED_ITEMS = "CMBPCheckoutModifiedItems";

    public static final String CHECKIN_MODIFIED_ITEMS = "CMBPCheckinModifiedItems";

    public static final String PROMOTE_MODIFIED_ITEMS = "CMBPPromoteModifiedItems";

    public static final String UNDO_CHECKOUT_MODIFIED_ITEMS = "CMBPUndoCheckoutModifiedItems";

    // MODIFIED DOCS
    public static final String PROMOTE_MODIFIED_DOCS = "CMBPPromoteModifiedDocs";

    // GENERAL
    public static final String IN_PROGRESS = "CMBPInProgress";

    public static final String INCREASE_PROCESSED_COUNTER = "CMBPIncreaseProcessedCounter";

    public static final String RESET_PROCESSED_COUNTER = "CMBPResetProcessedCounter";

    public static final String FINISHED = "CMBPFinished";

    public static final String REFRESHED = "CMBRefreshed";

}
