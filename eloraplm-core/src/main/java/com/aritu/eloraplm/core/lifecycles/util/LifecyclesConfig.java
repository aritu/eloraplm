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
package com.aritu.eloraplm.core.lifecycles.util;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.core.lifecycles.api.LifecycleConfigService;

/**
 * @author aritu
 *
 */
public class LifecyclesConfig {

    private static LifecycleConfigService lcs;

    public static final List<String> allStatesList = initAllStatesList();

    public static final List<String> releasedStatesList = initReleasedStatesList();

    public static final List<String> unreleasedStatesList = initUnreleasedStatesList();

    public static final List<String> obsoleteStatesList = initObsoleteStatesList();

    public static final List<String> deletedStatesList = initDeletedStatesList();

    public static final List<String> obsoleteAndDeletedStatesList = initObsoleteAndDeletedStatesList();

    public static final List<String> lockableStatesList = initLockableStatesList();

    public static final List<String> allowedByAllStatesTransitionsList = initAllowedByAllStatesTransitionsList();

    public static final List<String> allowsAllStatesTransitionsList = initAllowsAllStatesTransitionsList();

    private static LifecycleConfigService getLifecycleConfigService() {
        if (lcs == null) {
            lcs = Framework.getService(LifecycleConfigService.class);
        }

        return lcs;
    }

    private static List<String> initAllStatesList() {
        return getLifecycleConfigService().getStatesList();
    }

    private static List<String> initReleasedStatesList() {
        return getLifecycleConfigService().getStateListByStatus(
                EloraLifeCycleConstants.STATUS_RELEASED);
    }

    private static List<String> initUnreleasedStatesList() {
        return getLifecycleConfigService().getStateListByStatus(
                EloraLifeCycleConstants.STATUS_UNRELEASED);
    }

    private static List<String> initObsoleteStatesList() {
        return getLifecycleConfigService().getStateListByStatus(
                EloraLifeCycleConstants.STATUS_OBSOLETE);
    }

    private static List<String> initDeletedStatesList() {
        return getLifecycleConfigService().getStateListByStatus(EloraLifeCycleConstants.STATUS_DELETED);
    }

    private static List<String> initObsoleteAndDeletedStatesList() {
        List<String> list = new ArrayList<String>();
        list.addAll(obsoleteStatesList);
        list.addAll(deletedStatesList);
        return list;
    }

    private static List<String> initLockableStatesList() {
        return getLifecycleConfigService().getLockableStateList();
    }

    private static List<String> initAllowedByAllStatesTransitionsList() {
        return getLifecycleConfigService().getAllowedByAllStatesTransitions();
    }

    private static List<String> initAllowsAllStatesTransitionsList() {
        return getLifecycleConfigService().getAllowsAllStatesTransitions();
    }

    public static boolean isSupported(String parentState, String childState) {
        return getLifecycleConfigService().isSupported(parentState, childState);
    }

    public static int getOrder(String state) {
        return getLifecycleConfigService().getOrder(state);
    }

    public static String getStateStatus(String state) {
        return getLifecycleConfigService().getStateStatus(state);
    }

    public static List<String> getVisibleDemoteTransitions(DocumentModel doc) {
        String lifecycle = doc.getLifeCyclePolicy();
        List<String> allowedTransitions = new ArrayList<String>(
                doc.getAllowedStateTransitions());
        allowedTransitions.retainAll(
                getLifecycleConfigService().getVisibleDemoteTransitions(
                        doc.getCoreSession(), lifecycle));
        return allowedTransitions;
    }

    public static List<String> getVisiblePromoteTransitions(DocumentModel doc) {
        String lifecycle = doc.getLifeCyclePolicy();
        List<String> allowedTransitions = new ArrayList<String>(
                doc.getAllowedStateTransitions());
        allowedTransitions.retainAll(
                getLifecycleConfigService().getVisiblePromoteTransitions(
                        doc.getCoreSession(), lifecycle));
        return allowedTransitions;
    }

}
