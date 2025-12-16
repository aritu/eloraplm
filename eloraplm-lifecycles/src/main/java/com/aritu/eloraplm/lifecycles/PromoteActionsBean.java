package com.aritu.eloraplm.lifecycles;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.util.EloraEventHelper;

@Name("promoteActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
@AutomaticDocumentBasedInvalidation
public class PromoteActionsBean extends LifecycleTransitionsActions {

    private static final long serialVersionUID = 1L;

    @Override
    public List<String> getTransitions() {
        if (transitions.isEmpty()) {
            DocumentModel doc = getCurrentDocument();
            if (doc != null) {
                transitions = LifecyclesConfig.getVisiblePromoteTransitions(
                        doc);
                /* Disabled because after executing it loads the next transition data and we cannot avoid it for now
                if (transitions.size() == 1) {
                    setTransition(transitions.get(0));
                    init();
                }
                */
            }

        }
        return transitions;
    }

    @Override
    public void execute() {
        DocumentModel doc = getCurrentDocument();
        String previousState = doc.getCurrentLifeCycleState();

        // Will promote the document
        super.execute();

        // Seam event
        Events.instance().raiseEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc);

        if (hasToFireDefaultEvent()) {

            // Nuxeo Event
            doc.refresh();
            String comment = doc.getVersionLabel();
            Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
            ctxProperties.put("comment", comment);
            ctxProperties.put("previousState", previousState);
            EloraEventHelper.fireEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc,
                    ctxProperties);

            // Fire Approved event
            if (transition.equals(EloraLifeCycleConstants.TRANS_APPROVE)) {
                EloraEventHelper.fireEvent(PdmEventNames.PDM_APPROVED_EVENT,
                        doc);
            }
        }

        navigationContext.invalidateCurrentDocument();
    }
}
