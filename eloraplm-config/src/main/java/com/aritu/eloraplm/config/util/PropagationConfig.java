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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class PropagationConfig {
    private static final Log log = LogFactory.getLog(PropagationConfig.class);

    // ----------------------------
    // Propagation config variables
    // ----------------------------
    public static final EloraConfigTable getPropagationConfig = initGetPropagationConfig();

    public static final EloraConfigTable checkoutPropagationConfig = initCheckoutPropagationConfig();

    public static final HashMap<String, List<EloraConfigRow>> approvePropagationMap = initApprovePropagationMap();

    public static final EloraConfigTable approveDescendingPropagationConfig = initApproveDescendingPropagationConfig();

    public static final HashMap<String, List<EloraConfigRow>> obsoletePropagationMap = initObsoletePropagationMap();

    public static final EloraConfigTable obsoleteDescendingPropagationConfig = initObsoleteDescendingPropagationConfig();

    // ---------------------------------------------------
    // Propagation config variables initialization methods
    // ---------------------------------------------------

    private static EloraConfigTable initGetPropagationConfig() {
        String logInitMsg = "[initGetPropagationConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = PropagationConfigHelper.getGetPropagationConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static EloraConfigTable initCheckoutPropagationConfig() {
        String logInitMsg = "[initCheckoutPropagationConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = PropagationConfigHelper.getCheckoutPropagationConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static HashMap<String, List<EloraConfigRow>> initApprovePropagationMap() {
        String logInitMsg = "[initApprovePropagationConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        HashMap<String, List<EloraConfigRow>> configMap = null;
        try {
            configMap = PropagationConfigHelper.getApprovePropagationConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    private static EloraConfigTable initApproveDescendingPropagationConfig() {
        String logInitMsg = "[initApproveDescendingPropagationConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = PropagationConfigHelper.getApproveDescendingPropagationConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

    private static HashMap<String, List<EloraConfigRow>> initObsoletePropagationMap() {
        String logInitMsg = "[initObsoletePropagationConfig] ";
        log.trace("********************************* ENTER IN " + logInitMsg);

        HashMap<String, List<EloraConfigRow>> configMap = new HashMap<String, List<EloraConfigRow>>();
        try {
            configMap = PropagationConfigHelper.getObsoletePropagationMap();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configMap;
    }

    private static EloraConfigTable initObsoleteDescendingPropagationConfig() {
        String logInitMsg = "[initObsoleteDescendingPropagationConfig] ";

        log.trace("********************************* ENTER IN " + logInitMsg);

        EloraConfigTable configTable = null;
        try {
            configTable = PropagationConfigHelper.getObsoleteDescendingPropagationConfig();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace("********************************* EXIT FROM " + logInitMsg);
        return configTable;
    }

}
