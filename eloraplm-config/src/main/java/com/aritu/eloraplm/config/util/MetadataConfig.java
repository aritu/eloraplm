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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class MetadataConfig {
    private static final Log log = LogFactory.getLog(MetadataConfig.class);

    private static final String[] AUTHORING_TOOLS = { "SolidWorks", "Catia" };

    private static final String[] DOCTYPES = {
            EloraDoctypeConstants.CAD_DRAWING, EloraDoctypeConstants.CAD_PART,
            EloraDoctypeConstants.CAD_ASSEMBLY,
            EloraDoctypeConstants.CAD_DESIGN_TABLE };

    private static final String[] OVERRIDE_UPDATE_MODES = { "PlmToCad",
            "Bidirectional" };

    // ----------------------------
    // Life cycles states variables
    // ----------------------------

    public static final Map<String, Map<String, List<String>>> realMetadataMapByType = initRealMetadataMapByType();

    public static final Map<String, Map<String, List<String>>> virtualMetadataMapByType = initVirtualMetadataMapByType();

    public static final Map<String, Map<String, List<String>>> realOverrideMetadataMapByType = initRealOverrideMetadataMapByType();

    public static final Map<String, Map<String, List<String>>> virtualOverrideMetadataMapByType = initVirtualOverrideMetadataMapByType();

    // ---------------------------------------------------
    // Life cycles states variables initialization methods
    // ---------------------------------------------------

    private static Map<String, Map<String, List<String>>> initRealMetadataMapByType() {
        String logInitMsg = "[initRealMetadataMapByType] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
        try {
            for (String authoringTool : AUTHORING_TOOLS) {
                map.put(authoringTool,
                        getMetadataAndConvertToList(
                                EloraConfigConstants.VAL_METADATA_MAPPING_PLM_METADATA_TYPE_REAL,
                                authoringTool, null));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static Map<String, Map<String, List<String>>> initVirtualMetadataMapByType() {
        String logInitMsg = "[initVirtualMetadataMapByType] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
        try {
            for (String authoringTool : AUTHORING_TOOLS) {
                map.put(authoringTool,
                        getMetadataAndConvertToList(
                                EloraConfigConstants.VAL_METADATA_MAPPING_PLM_METADATA_TYPE_VIRTUAL,
                                authoringTool, null));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static Map<String, Map<String, List<String>>> initRealOverrideMetadataMapByType() {
        String logInitMsg = "[initRealOverrideMetadataMapByType] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
        try {
            for (String authoringTool : AUTHORING_TOOLS) {
                map.put(authoringTool,
                        getMetadataAndConvertToList(
                                EloraConfigConstants.VAL_METADATA_MAPPING_PLM_METADATA_TYPE_REAL,
                                authoringTool, OVERRIDE_UPDATE_MODES));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static Map<String, Map<String, List<String>>> initVirtualOverrideMetadataMapByType() {
        String logInitMsg = "[initVirtualOverrideMetadataMapByType] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
        try {
            for (String authoringTool : AUTHORING_TOOLS) {
                map.put(authoringTool,
                        getMetadataAndConvertToList(
                                EloraConfigConstants.VAL_METADATA_MAPPING_PLM_METADATA_TYPE_VIRTUAL,
                                authoringTool, OVERRIDE_UPDATE_MODES));
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return map;
    }

    private static Map<String, List<String>> getMetadataAndConvertToList(
            String type, String authoringTool, String[] updateMode)
            throws EloraException {

        Map<String, List<String>> map = new HashMap<String, List<String>>();

        for (String doctype : DOCTYPES) {
            EloraConfigTable configTable = MetadataConfigHelper.getMetadataConfig(
                    authoringTool, type, doctype, updateMode, false);
            map.put(doctype, configTable.extractConfigTablePropertyValuesAsList(
                    EloraConfigConstants.PROP_METADATA_MAPPING_PLM_METADATA));
        }

        return map;
    }

}
