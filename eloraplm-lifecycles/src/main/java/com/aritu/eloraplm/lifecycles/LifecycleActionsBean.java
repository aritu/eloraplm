package com.aritu.eloraplm.lifecycles;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.LifecyclesConfig;

@Name("lifecycleActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class LifecycleActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getLifeCycleStateColor(String lifeCycleState) {
        String color = "#888888";

        if (LifecyclesConfig.allStatesConfig.containsKey(lifeCycleState)) {
            EloraConfigRow lifecycleConfigRow = LifecyclesConfig.allStatesConfig.getRow(
                    lifeCycleState);
            color = (String) lifecycleConfigRow.getProperty("color");
        }

        return color;
    }

    public boolean isReleasedState(String lifeCycleState) {
        if (LifecyclesConfig.releasedStatesList.contains(lifeCycleState)) {
            return true;
        }
        return false;
    }

    public String getLifeCycleStateAbbreviation(String lifeCycleState) {
        String abbreviation = "";

        if (LifecyclesConfig.allStatesConfig.containsKey(lifeCycleState)) {
            EloraConfigRow lifecycleConfigRow = LifecyclesConfig.allStatesConfig.getRow(
                    lifeCycleState);
            abbreviation = (String) lifecycleConfigRow.getProperty(
                    "abbreviation");
        }

        return abbreviation;
    }

}
