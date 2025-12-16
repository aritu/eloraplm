package com.aritu.eloraplm.lifecycles;

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
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.core.DocumentContextBoundActions;
import com.aritu.eloraplm.exceptions.TransitionNotAllowedException;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuterFactory;
import com.aritu.eloraplm.lifecycles.util.ErrorInfo;

public abstract class LifecycleTransitionsActions
        extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            LifecycleTransitionsActions.class);

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

    protected String currentState;

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
        DocumentModel doc = getCurrentDocument();
        String lifecycle = doc.getLifeCyclePolicy();

        TransitionExecuterFactory factory = Framework.getService(
                TransitionExecuterFactory.class);
        return factory.getTransitionExecuter(lifecycle, transition);
    }

    public abstract List<String> getTransitions();

    public void setTransitions(List<String> transitions) {
        this.transitions = transitions;
    }

    public void init() {
        String logInitMsg = "[init] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            DocumentModel doc = getCurrentNonProxyDocument();

            if (transition != null) {
                te = obtainTransitionExecuter();
                te.init(doc);
            }
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.init"));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
    }

    public String getPreviousScreen() {
        String logInitMsg = "[getPreviousScreen] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            if (transition != null) {
                return getTransitionExecuter().getPreviousScreen();
            }
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.init"));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return null;
    }

    public boolean canBeExecuted() {
        String logInitMsg = "[canBeExecuted] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            if (transition != null) {
                return getTransitionExecuter().canBeExecuted();
            }
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.init"));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return false;
    }

    public List<ErrorInfo> getErrorList() {
        String logInitMsg = "[getErrorList] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            if (transition != null) {
                return getTransitionExecuter().getErrorList();
            }
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.init"));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return new ArrayList<ErrorInfo>();
    }

    public void execute() {

        String logInitMsg = "[execute] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel doc = getCurrentNonProxyDocument();
        try {
            getTransitionExecuter().execute(doc);

            navigationContext.invalidateCurrentDocument();

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.lifecycles.transition.execute"),
                    messages.get(transition));

            log.info(logInitMsg + "Document |" + doc.getId()
                    + "| followed transition | " + transition + " |.");

        } catch (DocumentSecurityException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.execute.lockedByOther"));

            log.error(logInitMsg + e.getMessage(), e);

        } catch (TransitionNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.execute.notAllowed"),
                    messages.get(transition));
            log.error(logInitMsg + e.getMessage(), e);

        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.execute"),
                    messages.get(transition));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
    }

    private DocumentModel getCurrentNonProxyDocument() {
        DocumentModel currentDoc = getCurrentDocument();
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

    public boolean hasInnerForm() {
        String logInitMsg = "[hasInnerForm] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {
            if (transition != null) {
                return getTransitionExecuter().hasInnerForm();
            }
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.lifecycles.transition.init"));
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
        return false;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        te = null;
        transition = null;
        transitions.clear();
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        boolean invalidate = false;
        String newState = null;
        if (doc != null) {
            newState = doc.getCurrentLifeCycleState();
        }

        if (currentState == null || newState == null) {
            if (!(currentState == null && newState == null)) {
                invalidate = true;
            }
        } else {
            if (!currentState.equals(newState)) {
                invalidate = true;
            }
        }

        if (invalidate) {
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different state. Current: |"
                    + currentState + "| New: |" + newState + "|");
            currentState = newState;
        }

        return doc;
    }

}
