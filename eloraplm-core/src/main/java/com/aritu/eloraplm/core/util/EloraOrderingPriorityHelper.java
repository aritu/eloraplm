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

        if (docType.equals(EloraDoctypeConstants.BOM_PRODUCT)) {
            orderingPriority = OrderingPriorityConstants.BOM_PRODUCT;
        } else if (EloraDocumentTypesHelper.getDocumentType(
                EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                        EloraDocumentTypesHelper.getDocumentType(docType))) {
            orderingPriority = OrderingPriorityConstants.BOM_PART;
        } else if (docType.equals(EloraDoctypeConstants.BOM_TOOL)) {
            orderingPriority = OrderingPriorityConstants.BOM_TOOL;
        } else if (docType.equals(EloraDoctypeConstants.BOM_PACKAGING)) {
            orderingPriority = OrderingPriorityConstants.BOM_PACKAGING;
        } else if (docType.equals(EloraDoctypeConstants.BOM_SPECIFICATION)) {
            orderingPriority = OrderingPriorityConstants.BOM_SPECIFICATION;
        } else if (docType.equals(EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT)) {
            orderingPriority = OrderingPriorityConstants.BOM_CUSTOMER_PRODUCT;
        } else if (docType.equals(
                EloraDoctypeConstants.BOM_MANUFACTURER_PART)) {
            orderingPriority = OrderingPriorityConstants.BOM_MANUFACTURER_PART;
        } else if (docType.equals(EloraDoctypeConstants.CAD_DRAWING)) {
            orderingPriority = OrderingPriorityConstants.CAD_DRAWING;
        } else if (docType.equals(EloraDoctypeConstants.CAD_ASSEMBLY)) {
            orderingPriority = OrderingPriorityConstants.CAD_ASSEMBLY;
        } else if (docType.equals(EloraDoctypeConstants.CAD_PART)) {
            orderingPriority = OrderingPriorityConstants.CAD_PART;
        } else if (docType.equals(EloraDoctypeConstants.CAD_DESIGN_TABLE)) {
            orderingPriority = OrderingPriorityConstants.CAD_DESIGN_TABLE;
        } else if (docType.equals(NuxeoDoctypeConstants.FILE)) {
            orderingPriority = OrderingPriorityConstants.FILE;
        } else if (docType.equals(NuxeoDoctypeConstants.NOTE)) {
            orderingPriority = OrderingPriorityConstants.NOTE;
        } else if (docType.equals(NuxeoDoctypeConstants.PICTURE)) {
            orderingPriority = OrderingPriorityConstants.PICTURE;
        } else if (docType.equals(NuxeoDoctypeConstants.VIDEO)) {
            orderingPriority = OrderingPriorityConstants.VIDEO;
        } else if (docType.equals(NuxeoDoctypeConstants.AUDIO)) {
            orderingPriority = OrderingPriorityConstants.AUDIO;
        } else if (docType.equals(CMDoctypeConstants.CM_ECO)) {
            orderingPriority = OrderingPriorityConstants.CM_ECO;
        } else if (docType.equals(CMDoctypeConstants.CM_ECR)) {
            orderingPriority = OrderingPriorityConstants.CM_ECR;
        } else if (docType.equals(CMDoctypeConstants.CM_PR)) {
            orderingPriority = OrderingPriorityConstants.CM_PR;
        } else {
            orderingPriority = OrderingPriorityConstants.DEFAULT;
        }

        log.trace(logInitMsg + "--- EXIT --- with orderingPriority = |"
                + orderingPriority + "|");

        return orderingPriority;
    }

}
