package com.aritu.eloraplm.lifecycles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.exceptions.TransitionNotAllowedException;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuterFactory;

public abstract class LifecycleTransitionsActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            LifecycleTransitionsActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String transition;

    protected TransitionExecuter te;

    protected List<String> transitions = new ArrayList<String>();

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    protected TransitionExecuter getTransitionExecuter() {
        if (te == null) {
            te = obtainTransitionExecuter();
        }
        return te;
    }

    protected TransitionExecuter obtainTransitionExecuter() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        String lifecycle = doc.getLifeCyclePolicy();

        TransitionExecuterFactory factory = Framework.getService(
                TransitionExecuterFactory.class);
        return factory.getTransitionExecuter(transition, lifecycle);
    }

    public abstract List<String> getTransitions();

    public void setTransitions(List<String> transitions) {
        this.transitions = transitions;
    }

    public void init() {

        DocumentModel doc = getCurrentNonProxyDocument();

        if (transition != null) {
            te = obtainTransitionExecuter();
            te.init(doc);
        }

    }

    public String getPreviousScreen() {
        if (transition != null) {
            return getTransitionExecuter().getPreviousScreen();
        }
        return null;
    }

    public boolean canBeExecuted() {
        if (transition != null) {
            return getTransitionExecuter().canBeExecuted();
        }
        return false;
    }

    public void execute() {

        String logInitMsg = "[execute] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel doc = getCurrentNonProxyDocument();
        try {
            getTransitionExecuter().execute(doc);

            navigationContext.invalidateCurrentDocument();

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get(
                            "eloraplm.message.success.lifecycles.transition.execute"),
                    messages.get(transition));

            log.info(logInitMsg + "Document |" + doc.getId()
                    + "| followed transition | " + transition + " |.");

        } catch (DocumentSecurityException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.execute.lockedByOther"));

            log.error(logInitMsg + e.getMessage(), e);

        } catch (TransitionNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.lifecycles.transition.execute.notAllowed"),
                    messages.get(transition));
            log.error(logInitMsg + e.getMessage(), e);

        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.lifecycles.transition.execute"),
                    messages.get(transition));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
    }

    private DocumentModel getCurrentNonProxyDocument() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc != null && currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }
        return currentDoc;
    }

    protected boolean hasToFireDefaultEvent() {
        if (transition != null) {
            return getTransitionExecuter().hasToFireDefaultEvent();
        }
        return true;
    }

}
