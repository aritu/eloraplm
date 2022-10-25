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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.core.lifecycles.api.LifecycleConfigService;

/**
 * @author aritu
 *
 */
@Name("lifecycleStateActions")
@Scope(CONVERSATION)
public class LifecycleStateActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            LifecycleStateActionsBean.class);

    @In(create = true)
    protected Map<String, String> messages;

    private LifeCycleService lcs;

    private LifecycleConfigService lccs;

    private Collection<LifeCycleState> lifecycleStates;

    private Map<String, String> lifecycleStateNames;

    private Map<String, String> lifecycleStateAbbrs;

    private Map<String, String> lifecycleStateColors;

    private LifeCycleService getLifeCycleService() {
        if (lcs == null) {
            lcs = NXCore.getLifeCycleService();
        }
        return lcs;
    }

    private LifecycleConfigService getLifecycleConfigService() {
        if (lccs == null) {
            lccs = Framework.getService(LifecycleConfigService.class);
        }
        return lccs;
    }

    public Collection<LifeCycleState> getLifecycleStates() {
        Collection<LifeCycleState> states = new ArrayList<LifeCycleState>();
        if (lifecycleStates == null) {
            Collection<LifeCycle> lifecycles = getLifeCycleService().getLifeCycles();
            for (LifeCycle lc : lifecycles) {
                states.addAll(lc.getStates());
            }
            lifecycleStates = states;
            createLifecycleStateConfigMaps();
        }
        return lifecycleStates;
    }

    public void setLifeCycleStates(Collection<LifeCycleState> lifecycleStates) {
        this.lifecycleStates = lifecycleStates;
    }

    public Map<String, String> getLifecycleStateNames() {
        if (lifecycleStateNames == null) {
            createLifecycleStateConfigMaps();
        }
        return lifecycleStateNames;
    }

    public void setLifecycleStateNames(
            Map<String, String> lifecycleStateNames) {
        this.lifecycleStateNames = lifecycleStateNames;
    }

    public Map<String, String> getLifecycleStateAbbrs() {
        if (lifecycleStateAbbrs == null) {
            createLifecycleStateConfigMaps();
        }
        return lifecycleStateAbbrs;
    }

    public void setLifecycleStateAbbrs(
            Map<String, String> lifecycleStateAbbrs) {
        this.lifecycleStateAbbrs = lifecycleStateAbbrs;
    }

    public Map<String, String> getLifecycleStateColors() {
        if (lifecycleStateColors == null) {
            createLifecycleStateConfigMaps();
        }
        return lifecycleStateColors;
    }

    public void setLifecycleStateColors(
            Map<String, String> lifecycleStateColors) {
        this.lifecycleStateColors = lifecycleStateColors;
    }

    private void createLifecycleStateConfigMaps() {
        lifecycleStateNames = new HashMap<String, String>();
        lifecycleStateAbbrs = new HashMap<String, String>();
        lifecycleStateColors = new HashMap<String, String>();

        try {
            for (LifeCycleState lcs : getLifecycleStates()) {
                // For now we don't differentiate states with same name in
                // different lifecycles, as we set the same color, name, abbr to
                // all of them.
                String state = lcs.getName();
                String stateName = messages.get(state);
                String stateAbbr = messages.get(
                        state + EloraLifeCycleConstants.ABBR_SUFFIX);
                String stateColor = getLifecycleConfigService().getStateColor(
                        state);
                lifecycleStateNames.put(state, stateName);
                lifecycleStateAbbrs.put(state, stateAbbr);
                lifecycleStateColors.put(state, stateColor);
            }
        } catch (Exception e) {
            log.error(
                    "Uncontrolled exception while getting lifecycle configs for JS. Exception: "
                            + e.getClass() + " - Message: " + e.getMessage(),
                    e);
        }
    }

}
