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

package com.aritu.eloraplm.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.CMDoctypeConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.OrderingPriorityConstants;

/**
 * Helper class related to Elora Ordering Priority.
 *
 * @author aritu
 *
 */

public class EloraOrderingPriorityHelper {

    private static final Log log = LogFactory.getLog(
            EloraOrderingPriorityHelper.class);

    public static Integer getOrderingPriority(String docType) {

        String logInitMsg = "[getOrderingPriority]";
        log.trace(
                logInitMsg + "--- ENTER --- with docType = |" + docType + "|");

        Integer orderingPriority = null;

        switch (docType) {
        // --------------------------------------------------------------
        // ITEMS
        // --------------------------------------------------------------
        case EloraDoctypeConstants.BOM_PRODUCT:
            orderingPriority = OrderingPriorityConstants.BOM_PRODUCT;
            break;
        case EloraDoctypeConstants.BOM_PART:
            orderingPriority = OrderingPriorityConstants.BOM_PART;
            break;
        case EloraDoctypeConstants.BOM_TOOL:
            orderingPriority = OrderingPriorityConstants.BOM_TOOL;
            break;
        case EloraDoctypeConstants.BOM_PACKAGING:
            orderingPriority = OrderingPriorityConstants.BOM_PACKAGING;
            break;
        case EloraDoctypeConstants.BOM_SPECIFICATION:
            orderingPriority = OrderingPriorityConstants.BOM_SPECIFICATION;
            break;
        case EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT:
            orderingPriority = OrderingPriorityConstants.BOM_CUSTOMER_PRODUCT;
            break;
        case EloraDoctypeConstants.BOM_MANUFACTURER_PART:
            orderingPriority = OrderingPriorityConstants.BOM_MANUFACTURER_PART;
            break;
        // --------------------------------------------------------------
        // CAD DOCUMENTS
        // --------------------------------------------------------------
        case EloraDoctypeConstants.CAD_DRAWING:
            orderingPriority = OrderingPriorityConstants.CAD_DRAWING;
            break;
        case EloraDoctypeConstants.CAD_ASSEMBLY:
            orderingPriority = OrderingPriorityConstants.CAD_ASSEMBLY;
            break;
        case EloraDoctypeConstants.CAD_PART:
            orderingPriority = OrderingPriorityConstants.CAD_PART;
            break;
        case EloraDoctypeConstants.CAD_DESIGN_TABLE:
            orderingPriority = OrderingPriorityConstants.CAD_DESIGN_TABLE;
            break;
        // --------------------------------------------------------------
        // OTHER DOCUMENTS
        // --------------------------------------------------------------
        case NuxeoDoctypeConstants.FILE:
            orderingPriority = OrderingPriorityConstants.FILE;
            break;
        case NuxeoDoctypeConstants.NOTE:
            orderingPriority = OrderingPriorityConstants.NOTE;
            break;
        case NuxeoDoctypeConstants.PICTURE:
            orderingPriority = OrderingPriorityConstants.PICTURE;
            break;
        case NuxeoDoctypeConstants.VIDEO:
            orderingPriority = OrderingPriorityConstants.VIDEO;
            break;
        case NuxeoDoctypeConstants.AUDIO:
            orderingPriority = OrderingPriorityConstants.AUDIO;
            break;
        // --------------------------------------------------------------
        // ELORA WORKSPACES
        // --------------------------------------------------------------
        case CMDoctypeConstants.CM_ECO:
            orderingPriority = OrderingPriorityConstants.CM_ECO;
            break;
        case CMDoctypeConstants.CM_ECR:
            orderingPriority = OrderingPriorityConstants.CM_ECR;
            break;
        case CMDoctypeConstants.CM_PR:
            orderingPriority = OrderingPriorityConstants.CM_PR;
            break;
        // --------------------------------------------------------------
        // DEFAULT
        // --------------------------------------------------------------
        default:
            orderingPriority = OrderingPriorityConstants.DEFAULT;
            break;
        }

        log.trace(logInitMsg + "--- EXIT --- with orderingPriority = |"
                + orderingPriority + "|");

        return orderingPriority;
    }

}
