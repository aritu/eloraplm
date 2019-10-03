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
package com.aritu.eloraplm.config.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.BomConfigConstants;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class BomConfigHelper {

    private static final Log log = LogFactory.getLog(BomConfigHelper.class);

    public static EloraConfigTable getBomClassificationConfig(String bomType,
            boolean includeObsoletes) throws EloraException {
        String logInitMsg = "[getBomClassificationConfig]";

        String vocabularyName = "";
        switch (bomType) {
        case EloraDoctypeConstants.BOM_PART:
            vocabularyName = BomConfigConstants.VOC_BOM_PART_CLASS_HIERARCH;
            break;
        case EloraDoctypeConstants.BOM_PRODUCT:
            vocabularyName = BomConfigConstants.VOC_BOM_PRODUCT_CLASS_HIERARCH;
            break;
        case EloraDoctypeConstants.BOM_TOOL:
            vocabularyName = BomConfigConstants.VOC_BOM_TOOL_CLASS_HIERARCH;
            break;
        case EloraDoctypeConstants.BOM_PACKAGING:
            vocabularyName = BomConfigConstants.VOC_BOM_PACKAGING_CLASS_HIERARCH;
            break;
        case EloraDoctypeConstants.BOM_SPECIFICATION:
            vocabularyName = BomConfigConstants.VOC_BOM_SPECIFICATION_CLASS_HIERARCH;
            break;
        default:
            String exceptionMessage = "Incorrect bomType. Specified type is |"
                    + bomType + "| and allowed types are ["
                    + EloraDoctypeConstants.BOM_PART + ", "
                    + EloraDoctypeConstants.BOM_PRODUCT + ", "
                    + EloraDoctypeConstants.BOM_TOOL + ", "
                    + EloraDoctypeConstants.BOM_PACKAGING + ", "
                    + EloraDoctypeConstants.BOM_SPECIFICATION + "]";
            log.error(logInitMsg + exceptionMessage);
            throw new EloraException(exceptionMessage);
        }

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable bomPartClassificationConfig = EloraConfigHelper.configService.getConfigTable(
                vocabularyName, filter, null, false);

        return bomPartClassificationConfig;
    }

}
