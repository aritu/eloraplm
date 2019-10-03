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

import com.aritu.eloraplm.constants.CMConfigConstants;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CMConfigHelper {

    public static EloraConfigTable getDoctypeActionsImpactConfigTable(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable doctypeActionsImpactConfig = EloraConfigHelper.configService.getConfigTable(
                CMConfigConstants.VOC_CM_DOCTYPE_ACTIONS_IMPACT, filter, null,
                false);

        return doctypeActionsImpactConfig;
    }

    // TODO:: don't used????
    /*public static EloraConfigRow getDoctypeActionsImpactConfigRow(String id)
            throws EloraException {
    
        EloraConfigRow doctypeActionsImpactConfig = EloraConfigHelper.configService.getConfigProperties(
                CMConfigConstants.VOC_CM_DOCTYPE_ACTIONS_IMPACT, id, null,
                false);
    
        return doctypeActionsImpactConfig;
    }*/

    // TODO:: don't used????
    /*public static EloraConfigTable getImpactedActionsConfig(
            boolean includeObsoletes) throws EloraException {
    
        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }
    
        EloraConfigTable impactedActionsConfig = EloraConfigHelper.configService.getConfigTable(
                CMConfigConstants.VOC_CM_ACTION_IMPACTED, filter, null, false);
    
        return impactedActionsConfig;
    }*/

    // TODO:: don't used????
    /* public static String getImpactedActionLabel(String id)
            throws EloraException {
        return EloraConfigHelper.configService.getConfig(
                CMConfigConstants.VOC_CM_ACTION_IMPACTED, id);
    }*/

    public static EloraConfigTable getModifiedActionsConfig(
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable modifiedActionsConfig = EloraConfigHelper.configService.getConfigTable(
                CMConfigConstants.VOC_CM_ACTION_MODIFIED, filter, null, false);

        return modifiedActionsConfig;
    }

    // TODO:: don't used????
    /*public static String getModifiedActionLabel(String id)
            throws EloraException {
        return EloraConfigHelper.configService.getConfig(
                CMConfigConstants.VOC_CM_ACTION_MODIFIED, id);
    }*/

}
