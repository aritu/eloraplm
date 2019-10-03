package com.aritu.eloraplm.lifecycles;

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.util.List;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.config.util.LifecyclesConfig;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraEventHelper;

@Name("demoteActions")
@Scope(PAGE)
@Install(precedence = FRAMEWORK)
public class DemoteActionsBean extends LifecycleTransitionsActionsBean {

    private static final long serialVersionUID = 1L;

    @Override
    public List<String> getTransitions() {
        if (transitions.isEmpty()) {
            DocumentModel doc = navigationContext.getCurrentDocument();
            String lifecycle = doc.getLifeCyclePolicy();
            String currentState = doc.getCurrentLifeCycleState();
            if (LifecyclesConfig.demoteTransitions.containsKey(lifecycle)
                    && LifecyclesConfig.demoteTransitions.get(
                            lifecycle).containsKey(currentState)) {
                transitions = LifecyclesConfig.demoteTransitions.get(
                        lifecycle).get(currentState);

                // if (transitions.size() >= 1) {
                // transition = transitions.get(0);
                // init();
                // }
            }
        }
        return transitions;
    }

    @Override
    public void execute() {
        super.execute();

        DocumentModel doc = navigationContext.getCurrentDocument();

        if (hasToFireDefaultEvent()) {
            // Seam event
            Events.instance().raiseEvent(PdmEventNames.PDM_DEMOTED_EVENT, doc);

            // Nuxeo Event
            doc.refresh();
            String comment = doc.getVersionLabel();
            EloraEventHelper.fireEvent(PdmEventNames.PDM_DEMOTED_EVENT, doc,
                    comment);
        }

        navigationContext.invalidateCurrentDocument();
    }
}
