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
package com.aritu.eloraplm.pdm.util;

import java.util.List;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * This class is a helper class for PDM bundle.
 *
 * @author aritu
 *
 */
public class PdmHelper {

    public static void invalidateAndResetTabList(WebActions webActions,
            NavigationContext navigationContext) {
        Action currentTabAction = webActions.getCurrentTabAction();
        Action currentSubTabAction = webActions.getCurrentSubTabAction();
        invalidateAndResetTabList(webActions, navigationContext,
                currentTabAction, currentSubTabAction);
    }

    public static void invalidateAndResetTabList(WebActions webActions,
            NavigationContext navigationContext, Action currentTabAction,
            Action currentSubTabAction) {
        navigationContext.invalidateCurrentDocument();
        webActions.resetTabList();
        List<Action> tabActionsList = webActions.getTabsList();
        if (tabActionsList.contains(currentTabAction)) {
            webActions.setCurrentTabAction(currentTabAction);
            List<Action> subTabActionsList = webActions.getSubTabsList();
            if (subTabActionsList.contains(currentSubTabAction)) {
                webActions.setCurrentSubTabAction(currentSubTabAction);
            }
        }
    }

}
