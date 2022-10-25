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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.constants.CodeCreationConfigConstants;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CodeCreationConfigHelper {

    protected static EloraConfigManager configService = Framework.getService(
            EloraConfigManager.class);

    // ----------------------------------------------------------------
    // Code Reservation configs
    // ----------------------------------------------------------------

    /**
     * @param maskId
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getMaskConfig(String maskId)
            throws EloraException {
        return getMaskConfig(maskId, false);
    }

    /**
     * @param maskId
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getMaskConfig(String maskId,
            boolean includeObsoletes) throws EloraException {

        // Then get the sequence properties
        List<String> properties = new ArrayList<>();
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_PREFIX);
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_SUFFIX);
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_DIGITS);
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_MINVALUE);
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_MAXVALUE);
        properties.add(CodeCreationConfigConstants.PROP_CODE_MASKS_SEQUENCEKEY);

        EloraConfigRow maskConfig = configService.getConfigProperties(
                CodeCreationConfigConstants.VOC_CODE_MASKS, maskId, properties);

        return maskConfig;
    }

    // TODO: berriak

    /**
     * @param docType
     * @return
     * @throws EloraException
     */
    public static EloraConfigTable getCodeCreationConfig(String docType)
            throws EloraException {
        return getCodeCreationConfig(docType, false);
    }

    public static EloraConfigTable getCodeCreationConfig(String docType,
            boolean includeObsoletes) throws EloraException {

        Map<String, Serializable> filter = new HashMap<>();
        if (!includeObsoletes) {
            filter.put(CodeCreationConfigConstants.PROP_CODE_TYPES_DOC_TYPE,
                    docType);
            filter.put(EloraConfigConstants.PROP_OBSOLETE, "0");
        }

        EloraConfigTable codeCreationTypesConfig = EloraConfigHelper.configService.getConfigTable(
                CodeCreationConfigConstants.VOC_CODE_TYPES, filter, null,
                false);

        return codeCreationTypesConfig;
    }

    /**
     * @param conditionId
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getConditionConfig(String conditionId)
            throws EloraException {
        return getConditionConfig(conditionId, false);
    }

    /**
     * @param conditionId
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getConditionConfig(String conditionId,
            boolean includeObsoletes) throws EloraException {

        // Then get the sequence properties
        List<String> properties = new ArrayList<>();
        properties.add(
                CodeCreationConfigConstants.PROP_CODE_CONDITIONS_CLASSNAME);
        properties.add(
                CodeCreationConfigConstants.PROP_CODE_CONDITIONS_METHODNAME);
        properties.add(
                CodeCreationConfigConstants.PROP_CODE_CONDITIONS_METHODPARAMS);
        properties.add(
                CodeCreationConfigConstants.PROP_CODE_CONDITIONS_OPERATOR);
        properties.add(CodeCreationConfigConstants.PROP_CODE_CONDITIONS_VALUE);

        EloraConfigRow conditionConfig = configService.getConfigProperties(
                CodeCreationConfigConstants.VOC_CODE_CONDITIONS, conditionId,
                properties);

        return conditionConfig;
    }

}
