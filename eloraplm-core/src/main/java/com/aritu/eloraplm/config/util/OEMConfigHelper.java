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

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.OEMConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class OEMConfigHelper {

    public static EloraConfigTable getOEMConfig(boolean includeObsoletes)
            throws EloraException {

        String vocabularyName = OEMConfigConstants.VOC_ERP_OEMS;

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable oemConfig = EloraConfigHelper.configService.getConfigTable(
                vocabularyName, filter, null, false);

        return oemConfig;
    }

    public static HashMap<String, String> getOEMLabelMap()
            throws EloraException {

        HashMap<String, String> oemLabelMap = new HashMap<String, String>();

        EloraConfigTable oemConfig = getOEMConfig(false);

        oemLabelMap = oemConfig.extractConfigTablePropertyValuesAsMap("id",
                "label");

        return oemLabelMap;
    }

}
