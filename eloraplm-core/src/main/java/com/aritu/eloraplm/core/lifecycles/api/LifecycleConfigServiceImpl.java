/**
 *
 */
package com.aritu.eloraplm.core.lifecycles.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class LifecycleConfigServiceImpl extends DefaultComponent
        implements LifecycleConfigService {

    private static final Log log = LogFactory.getLog(
            LifecycleConfigServiceImpl.class);

    private static final String XP_STATES = "states";

    private static final String XP_TRANSITIONS = "transitions";

    private static final String XP_CONFIG = "config";

    private static final String TRANSITION_DIRECTION_DEMOTE = "demote";

    private static final String TRANSITION_DIRECTION_PROMOTE = "promote";

    private Map<String, StateDescriptor> states;

    private Map<String, TransitionDescriptor> transitions;

    private Map<String, ConfigDescriptor> config;

    private UserManager umgr;

    @Override
    public void activate(ComponentContext context) {
        states = new HashMap<String, StateDescriptor>();
        transitions = new HashMap<String, TransitionDescriptor>();
        config = new HashMap<String, ConfigDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        states = null;
        transitions = null;
        config = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_STATES:
            StateDescriptor sd = (StateDescriptor) contribution;
            if (sd != null) {
                if (sd.name == null) {
                    throw new NuxeoException("State sent without a name");
                }
                if (states.containsKey(sd.name)) {
                    StateDescriptor currentSd = states.get(sd.name);
                    currentSd.merge(sd);
                    states.put(currentSd.name, currentSd);
                } else {
                    states.put(sd.name, sd);
                }
            }
            break;
        case XP_TRANSITIONS:
            TransitionDescriptor td = (TransitionDescriptor) contribution;
            if (td != null) {
                if (td.lifecycle == null) {
                    throw new NuxeoException(
                            "Transition sent without a lifecycle");
                }
                if (td.name == null) {
                    throw new NuxeoException("Transition sent without a name");
                }
                String key = td.lifecycle + "." + td.name;
                if (transitions.containsKey(key)) {
                    TransitionDescriptor currentTd = transitions.get(key);
                    currentTd.merge(td);
                } else {
                    transitions.put(key, td);
                }
            }
            break;
        case XP_CONFIG:
            ConfigDescriptor cd = (ConfigDescriptor) contribution;
            if (cd != null) {
                if (cd.transition == null) {
                    throw new NuxeoException(
                            "Config sent without a transition");
                }
                config.put(cd.transition, cd);
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    private UserManager getUserManager() {
        if (umgr == null) {
            umgr = Framework.getLocalService(UserManager.class);
        }
        return umgr;
    }

    // ------------------------
    // States
    // ------------------------

    @Override
    public List<String> getStatesList() {
        return new ArrayList<String>(states.keySet());
    }

    @Override
    public List<String> getStateListByStatus(String status) {
        List<String> matchingStates = new ArrayList<String>();
        for (Map.Entry<String, StateDescriptor> e : states.entrySet()) {
            if (e.getValue().status != null
                    && e.getValue().status.equals(status)) {
                matchingStates.add(e.getKey());
            }
        }
        return matchingStates;
    }

    @Override
    public List<String> getLockableStateList() {
        List<String> matchingStates = new ArrayList<String>();
        for (Map.Entry<String, StateDescriptor> e : states.entrySet()) {
            if (e.getValue().lockable) {
                matchingStates.add(e.getKey());
            }
        }
        return matchingStates;
    }

    @Override
    public StateDescriptor getStateConfig(String state) {
        if (state != null && states.containsKey(state)) {
            return states.get(state);
        }
        return null;
    }

    @Override
    public String getStateColor(String state) {
        StateDescriptor sd = getStateConfig(state);
        if (sd != null && sd.color != null) {
            return sd.color;
        }
        return EloraLifeCycleConstants.DEFAULT_STATE_COLOR;
    }

    @Override
    public String getStateStatus(String state) {
        StateDescriptor sd = getStateConfig(state);
        if (sd != null && sd.status != null) {
            return sd.status;
        }
        return "";
    }

    @Override
    public List<String> getSupportedStates(String state) {
        StateDescriptor sd = getStateConfig(state);
        if (sd != null && !sd.supportedStates.isEmpty()) {
            return sd.supportedStates;
        }
        return null;
    }

    @Override
    public boolean isSupported(String parent, String child) {
        List<String> supportedStates = getSupportedStates(parent);
        if (supportedStates == null || supportedStates.contains(child)) {
            // No supported states == All supported
            return true;
        }
        return false;
    }

    @Override
    public int getOrder(String state) {
        StateDescriptor sd = getStateConfig(state);
        if (sd != null && sd.order != null) {
            return sd.order;
        }
        return 0;
    }

    @Override
    public List<String> getFinalStateList() {
        List<String> matchingStates = new ArrayList<String>();
        for (Map.Entry<String, StateDescriptor> e : states.entrySet()) {
            if (e.getValue().finalState) {
                matchingStates.add(e.getKey());
            }
        }
        return matchingStates;
    }

    @Override
    public boolean isFinalState(String state) {
        StateDescriptor sd = getStateConfig(state);
        if (sd != null) {
            return sd.finalState;
        }
        return false;
    }

    // ------------------------
    // Transitions
    // ------------------------

    @Override
    public TransitionDescriptor getTransitionConfig(String lifecycle,
            String transition) throws EloraException {
        if (lifecycle != null && transition != null) {
            String key = lifecycle + "." + transition;
            if (transitions.containsKey(key)) {
                return transitions.get(key);
            }
        }
        return null;
    }

    @Override
    public List<String> getVisibleDemoteTransitions(CoreSession session,
            String lifecycle) {
        return getVisibleTransitions(session, lifecycle,
                TRANSITION_DIRECTION_DEMOTE);
    }

    @Override
    public List<String> getVisiblePromoteTransitions(CoreSession session,
            String lifecycle) {
        return getVisibleTransitions(session, lifecycle,
                TRANSITION_DIRECTION_PROMOTE);
    }

    private List<String> getVisibleTransitions(CoreSession session,
            String lifecycle, String direction) {
        List<String> matchingTransitions = new ArrayList<String>();
        for (Map.Entry<String, TransitionDescriptor> e : transitions.entrySet()) {
            TransitionDescriptor td = e.getValue();
            if (!td.lifecycle.equals(lifecycle)) {
                continue;
            }
            if (td.visible == null || !td.visible) {
                continue;
            }
            if (td.direction == null || !td.direction.equals(direction)) {
                continue;
            }
            if (isTransitionPermitted(session, td.permissionFilters)) {
                matchingTransitions.add(td.name);
            }
        }
        return matchingTransitions;
    }

    private boolean isTransitionPermitted(CoreSession session,
            List<String> permissionFilters) {

        String logInitMsg = "[isTransitionPermitted] ["
                + session.getPrincipal().getName() + "] ";

        if (permissionFilters == null || permissionFilters.isEmpty()) {
            return true;
        }

        try {

            Principal principal = session.getPrincipal();
            if (principal != null) {

                // Admin and power users can always follow the transition
                NuxeoPrincipal user = (NuxeoPrincipal) principal;
                if (user.isAdministrator() || user.isMemberOf("powerusers")) {
                    return true;
                }

                List<String> userFilters = permissionFilters.stream().filter(
                        x -> x.startsWith("user:")).collect(
                                Collectors.toList());
                List<String> groupFilters = permissionFilters.stream().filter(
                        x -> x.startsWith("group:")).collect(
                                Collectors.toList());

                List<String> allowedUsers = new ArrayList<String>();
                for (String u : userFilters) {
                    allowedUsers.add(u.substring("user:".length()));
                }

                for (String g : groupFilters) {
                    String groupName = g.substring("group:".length());
                    if (getUserManager().getGroup(groupName) == null) {
                        log.error(logInitMsg + "Group = |" + groupName
                                + "| does not exist in Elora. Nobody will be granted to execute the transition.");
                        return false;
                    }
                    allowedUsers.addAll(
                            getUserManager().getUsersInGroupAndSubGroups(
                                    groupName));
                }

                if (allowedUsers.contains(principal.getName())) {
                    return true;
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            return false;
        }

        return false;
    }

    // ------------------------
    // Config
    // ------------------------

    @Override
    public List<String> getAllowedByAllStatesTransitions() {
        List<String> matchingTransitions = new ArrayList<String>();
        for (Map.Entry<String, ConfigDescriptor> e : config.entrySet()) {
            if (e.getValue().allowedByAllParentStates != null
                    && e.getValue().allowedByAllParentStates) {
                matchingTransitions.add(e.getValue().transition);
            }
        }
        return matchingTransitions;
    }

    @Override
    public List<String> getAllowsAllStatesTransitions() {
        List<String> matchingTransitions = new ArrayList<String>();
        for (Map.Entry<String, ConfigDescriptor> e : config.entrySet()) {
            if (e.getValue().allowsAllChildStates != null
                    && e.getValue().allowsAllChildStates) {
                matchingTransitions.add(e.getValue().transition);
            }
        }
        return matchingTransitions;
    }

}
