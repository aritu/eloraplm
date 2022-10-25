/**
 *
 */
package com.aritu.eloraplm.core.lifecycles.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface LifecycleConfigService {

    // States

    public List<String> getStatesList();

    public List<String> getStateListByStatus(String status);

    public List<String> getLockableStateList();

    public StateDescriptor getStateConfig(String state);

    public String getStateColor(String state);

    public String getStateStatus(String state);

    public List<String> getSupportedStates(String state);

    public boolean isSupported(String parent, String child);

    public int getOrder(String state);

    public List<String> getFinalStateList();

    public boolean isFinalState(String state);

    // Transitions

    public TransitionDescriptor getTransitionConfig(String lifecycle,
            String transition) throws EloraException;

    public List<String> getVisibleDemoteTransitions(CoreSession session,
            String lifecycle);

    public List<String> getVisiblePromoteTransitions(CoreSession session,
            String lifecycle);

    // Config

    public List<String> getAllowedByAllStatesTransitions();

    public List<String> getAllowsAllStatesTransitions();

}
