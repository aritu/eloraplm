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
 * This class contains the constants related to the events of PDM actions
 *
 * @author aritu
 *
 */
public class PdmEventNames {

    public static final String PDM_CHECKED_OUT_EVENT = "PdmCheckedOutEvent";

    public static final String PDM_CHECKOUT_UNDONE_EVENT = "PdmCheckoutUndoneEvent";

    public static final String PDM_CHECKED_IN_EVENT = "PdmCheckedInEvent";

    public static final String PDM_ABOUT_TO_OVERWRITE_EVENT = "PdmAboutToOverwriteEvent";

    public static final String PDM_OVERWRITTEN_EVENT = "PdmOverwrittenEvent";

    public static final String PDM_ITEM_DOC_OVERWRITTEN_EVENT = "PdmItemDocOverwrittenEvent";

    public static final String PDM_DEMOTED_EVENT = "PdmDemotedEvent";

    public static final String PDM_PROMOTED_EVENT = "PdmPromotedEvent";

    public static final String PDM_RESTORED_EVENT = "PdmRestoredEvent";

    // This event is thrown after the viewer has been created and document has
    // been saved
    public static final String PDM_APPROVED_EVENT = "PdmApprovedEvent";

}
