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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class RelationsConfig {

    private static final Log log = LogFactory.getLog(RelationsConfig.class);

    // -----------------------
    // CAD relations variables
    // -----------------------
    public static final EloraConfigTable cadHierarchicalRelationsConfig = initCadHierarchicalRelationsConfig();

    public static final List<String> cadHierarchicalRelationsList = initCadHierarchicalRelationsList();

    public static final EloraConfigTable cadSuppressedRelationsConfig = initCadSuppressedRelationsConfig();

    public static final List<String> cadSuppressedRelationsList = initCadSuppressedRelationsList();

    public static final EloraConfigTable cadDirectRelationsConfig = initCadDirectRelationsConfig();

    public static final List<String> cadDirectRelationsList = initCadDirectRelationsList();

    public static final EloraConfigTable cadSpecialRelationsConfig = initCadSpecialRelationsConfig();

    public static final List<String> cadSpecialRelationsList = initCadSpecialRelationsList();

    public static final EloraConfigTable cadIconOnlyRelationsConfig = initCadIconOnlyRelationsConfig();

    public static final List<String> cadIconOnlyRelationsList = initIconOnlyRelationsList();

    public static final EloraConfigTable cadRelationsConfig = initCadRelationsConfig();

    public static final List<String> cadRelationsList = initCadRelationsList();

    // -----------------------
    // BOM relations variables
    // -----------------------
    public static final EloraConfigTable bomHierarchicalRelationsConfig = initBomHierarchicalRelationsConfig();

    public static final List<String> bomHierarchicalRelationsList = initBomHierarchicalRelationsList();

    public static final EloraConfigTable bomDirectRelationsConfig = initBomDirectRelationsConfig();

    public static final List<String> bomDirectRelationsList = initBomDirectRelationsList();

    public static final EloraConfigTable bomDocumentRelationsConfig = initBomDocumentRelationsConfig();

    public static final List<String> bomDocumentRelationsList = initBomDocumentRelationsList();

    public static final EloraConfigTable bomAnarchicRelationsConfig = initBomAnarchicRelationsConfig();

    public static final List<String> bomAnarchicRelationsList = initBomAnarchicRelationsList();

    public static final EloraConfigTable bomRelationsConfig = initBomRelationsConfig();

    public static final List<String> bomRelationsList = initBomRelationsList();

    // -----------------------------
    // CONTAINER relations variables
    // -----------------------------

    public static final EloraConfigTable containerRelationsConfig = initContainerRelationsConfig();

    public static final List<String> containerRelationsList = initContainerRelationsList();

    // ----------------------------------------------
    // CAD relations variables initialization methods
    // ----------------------------------------------
    private static EloraConfigTable initCadHierarchicalRelationsConfig() {
        String logInitMsg = "[initCadHierarchicalRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadHierarchicalRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initCadHierarchicalRelationsList() {
        String logInitMsg = "[initCadHierarchicalRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadHierarchicalRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initCadSuppressedRelationsConfig() {
        String logInitMsg = "[initCadSuppressedRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadSuppressedRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initCadSuppressedRelationsList() {
        String logInitMsg = "[initCadSuppressedRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadSuppressedRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initCadDirectRelationsConfig() {
        String logInitMsg = "[initCadDirectRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadDirectRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initCadDirectRelationsList() {
        String logInitMsg = "[initCadDirectRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadDirectRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initCadSpecialRelationsConfig() {
        String logInitMsg = "[initCadSpecialRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadSpecialRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initCadSpecialRelationsList() {
        String logInitMsg = "[initCadSpecialRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadSpecialRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initCadIconOnlyRelationsConfig() {
        String logInitMsg = "[initCadIconOnlyRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadIconOnlyRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initIconOnlyRelationsList() {
        String logInitMsg = "[initIconOnlyRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadIconOnlyRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initCadRelationsConfig() {
        String logInitMsg = "[initCadRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getCadRelationsConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initCadRelationsList() {
        String logInitMsg = "[initCadRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                cadRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    // ----------------------------------------------
    // BOM relations variables initialization methods
    // ----------------------------------------------
    private static EloraConfigTable initBomHierarchicalRelationsConfig() {
        String logInitMsg = "[initBomHierarchicalRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getBomHierarchicalRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initBomHierarchicalRelationsList() {
        String logInitMsg = "[initBomHierarchicalRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                bomHierarchicalRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initBomDirectRelationsConfig() {
        String logInitMsg = "[initBomDirectRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getBomDirectRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initBomDirectRelationsList() {
        String logInitMsg = "[initBomDirectRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                bomDirectRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initBomDocumentRelationsConfig() {
        String logInitMsg = "[initBomDocumentRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getBomDocumentRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initBomDocumentRelationsList() {
        String logInitMsg = "[initBomDocumentRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                bomDocumentRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initBomAnarchicRelationsConfig() {
        String logInitMsg = "[initBomAnarchicRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getBomAnarchicRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initBomAnarchicRelationsList() {
        String logInitMsg = "[initBomAnarchicRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                bomAnarchicRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initBomRelationsConfig() {
        String logInitMsg = "[initBomRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getBomRelationsConfig(false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initBomRelationsList() {
        String logInitMsg = "[initBomRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                bomRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

    private static EloraConfigTable initContainerRelationsConfig() {
        String logInitMsg = "[initContainerRelationsConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = RelationsConfigHelper.getContainerRelationsConfig(
                    false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static List<String> initContainerRelationsList() {
        String logInitMsg = "[initContainerRelationsList] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        List<String> configList = Collections.unmodifiableList(
                containerRelationsConfig.extractConfigTablePropertyValuesAsList(
                        "id"));

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configList;
    }

}
