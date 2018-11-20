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
public class CMEventNames {

    public static final String CM_BOM_MODIFIED_ITEMS_SAVED = "BomModifiedItemsSaved";

    public static final String CM_BOM_IMPACT_MATRIX_CALCULATED = "BomIMCalculated";

    public static final String CM_DOC_MODIFIED_ITEMS_SAVED = "DocModifiedItemsSaved";

    public static final String CM_DOC_IMPACT_MATRIX_CALCULATED = "DocIMCalculated";

    // Refresh
    public static final String CM_REFRESH_ITEMS_IMPACT_MATRIX = "RefreshItemsImpactMatrix";

    public static final String CM_REFRESH_DOCS_IMPACT_MATRIX = "RefreshDocsImpactMatrix";

    // Events for audit log

    public static final String CM_ITEMS_BATCH_LOCK_EVENT = "CmItemsBatchLockEvent";

    public static final String CM_ITEMS_BATCH_UNLOCK_EVENT = "CmItemsBatchUnlockEvent";

    public static final String CM_ITEMS_BATCH_EXECUTE_ACTIONS_EVENT = "CmItemsBatchExecuteActionsEvent";

    public static final String CM_ITEMS_BATCH_CHECK_IN_EVENT = "CmItemsBatchCheckInEvent";

    public static final String CM_ITEMS_BATCH_OVERWRITE_EVENT = "CmItemsBatchOverwriteEvent";

    public static final String CM_ITEMS_BATCH_PROMOTE_EVENT = "CmItemsBatchPromoteEvent";

    public static final String CM_DOCS_BATCH_LOCK_EVENT = "CmDocsBatchLockEvent";

    public static final String CM_DOCS_BATCH_UNLOCK_EVENT = "CmDocsBatchUnlockEvent";

    public static final String CM_DOCS_BATCH_PROMOTE_EVENT = "CmDocsBatchPromoteEvent";

    public static final String CM_IMPACTED_ITEMS_CHANGES_SAVED_EVENT = "CmImpactedItemsChangesSavedEvent";

    public static final String CM_IMPACTED_DOCS_CHANGES_SAVED_EVENT = "CmImpactedDocsChangesSavedEvent";

}
