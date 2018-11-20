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

import java.util.ArrayList;
import java.util.List;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.constants.CodeCreationConfigConstants;
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
     * @param doctype
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getMaskConfigForDoctype(String doctype)
            throws EloraException {
        return getMaskConfigForDoctype(doctype, false);
    }

    /**
     * @param doctype
     * @param includeObsoletes
     * @return
     * @throws EloraException
     */
    public static EloraConfigRow getMaskConfigForDoctype(String doctype,
            boolean includeObsoletes) throws EloraException {

        EloraConfigRow maskConfig = null;
        
        // First get the sequence id
        String maskId = getMaskId(doctype);
        if(maskId != null) {
            maskConfig = getMaskConfig(maskId, includeObsoletes);
        }
        return maskConfig;
    }

    /**
     * @param doctype
     * @return
     * @throws EloraException
     */
    private static String getMaskId(String doctype) throws EloraException {

        String maskId = configService.getConfig(
                CodeCreationConfigConstants.VOC_CODE_TYPES, doctype);

        return maskId;
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

}
