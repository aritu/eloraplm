package com.aritu.eloraplm.lifecycles;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("lifecycleActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class LifecycleActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected EloraConfigTable lifecycleConfigTable;

    protected EloraConfigTable releasedConfigTable;

    @Create
    public void initBean() {
        try {
            lifecycleConfigTable = EloraConfigHelper.getLifecycleStatesConfig();
            releasedConfigTable = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        } catch (EloraException e) {
            lifecycleConfigTable = new EloraConfigTable();
            releasedConfigTable = new EloraConfigTable();
        }
    }

    public String getLifeCycleStateColor(String lifeCycleState) {
        String color = "#888888";

        if (lifecycleConfigTable.containsKey(lifeCycleState)) {
            EloraConfigRow lifecycleConfigRow = lifecycleConfigTable.getRow(lifeCycleState);
            color = (String) lifecycleConfigRow.getProperty("color");
        }

        return color;
    }

    public boolean isReleasedState(String lifeCycleState) {
        if (releasedConfigTable.containsKey(lifeCycleState)) {
            return true;
        }
        return false;
    }

}
