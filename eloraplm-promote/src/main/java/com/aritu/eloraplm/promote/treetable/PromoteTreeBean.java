package com.aritu.eloraplm.promote.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.checker.PromoteCheckerFactory;
import com.aritu.eloraplm.promote.checker.PromoteCheckerManager;
import com.aritu.eloraplm.promote.checker.impl.PromoteCheckerFactoryImpl;
import com.aritu.eloraplm.promote.constants.PromoteConstants;
import com.aritu.eloraplm.promote.constants.util.PromoteHelper;
import com.aritu.eloraplm.promote.executer.PromoteExecuterFactory;
import com.aritu.eloraplm.promote.executer.PromoteExecuterManager;
import com.aritu.eloraplm.promote.executer.impl.PromoteExecuterFactoryImpl;
import com.aritu.eloraplm.treetable.CoreTreeBean;

@Name("promoteTreeBean")
@Scope(ScopeType.PAGE)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class PromoteTreeBean extends CoreTreeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected transient WebActions webActions;

    private static final Log log = LogFactory.getLog(PromoteTreeBean.class);

    PromoteNodeService nodeService;

    PromoteCheckerManager promoteCheckerManager;

    protected Map<String, String> promoteOptions;

    protected Map<String, String> relationOptions;

    private String selectedPromoteOption;

    private String selectedRelationOption;

    private boolean firstLoad;

    private boolean allOK;

    private String transition;

    private String finalState;

    private DocumentModel currentDoc;

    public PromoteTreeBean() {
        // TODO: Cargar de BBDD y poner con constantes. Meter en
        // configuraciones?
        promoteOptions = new HashMap<String, String>();
        promoteOptions.put(EloraLifeCycleConstants.TRANS_APPROVE,
                EloraLifeCycleConstants.TRANS_APPROVE);
        promoteOptions.put(EloraLifeCycleConstants.TRANS_OBSOLETE,
                EloraLifeCycleConstants.TRANS_OBSOLETE);

        // Default value
        // selectedPromoteOption = EloraLifeCycleConstants.CAD_OBSOLETE;
        selectedRelationOption = PromoteConstants.AS_STORED;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        // relationOptions = new HashMap<String, String>();
        // relationOptions.put("latestReleased",
        // messages.get("eloraplm.label.latest.released"));
        // relationOptions.put("asStored",
        // messages.get("eloraplm.label.as.stored"));
        // relationOptions.put("latestVersion",
        // messages.get("eloraplm.label.latest.version"));

        try {
            log.trace(logInitMsg + "Creating tree...");
            boolean rootIsSpecial = currentDoc.getDocumentType().getName().equals(
                    EloraDoctypeConstants.CAD_DRAWING);

            transition = selectedPromoteOption;
            finalState = PromoteHelper.getFinalStateFromTransition(currentDoc,
                    transition);

            PromoteCheckerFactory promoteCheckerFactory = new PromoteCheckerFactoryImpl();
            promoteCheckerManager = promoteCheckerFactory.getChecker(
                    currentDoc);

            // TODO: Mirar esto como poner bien. Se crea otro igual abajo, igual
            // hay que poner como global. No se si las clases estan bien
            // separadas...
            PromoteExecuterFactory promoteExecuterFactory = new PromoteExecuterFactoryImpl();
            PromoteExecuterManager promoteExecuterManager = null;
            if (!firstLoad) {
                promoteExecuterManager = promoteExecuterFactory.getExecuter(
                        transition);
            }

            nodeService = new PromoteNodeService(finalState, transition,
                    selectedRelationOption, documentManager,
                    eloraDocumentRelationManager, promoteCheckerManager,
                    promoteExecuterManager, firstLoad, rootIsSpecial, messages);
            setRoot(nodeService.getRoot(currentDoc));

            allOK = promoteCheckerManager.isTopLevelOK();
            log.trace(logInitMsg + "Tree created.");
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    public void applyFilters(DocumentModel doc) {
        if (doc != null) {
            firstLoad = false;
            currentDoc = doc;
            createRoot();
        }
    }

    public void setTransitionAndCreateRoot(String transition,
            DocumentModel doc) {
        setSelectedPromoteOption(transition);
        applyFilters(doc);
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        firstLoad = true;

        nodeService = new PromoteNodeService();
        setRoot(nodeService.getEmptyRoot());

        // createRoot();
    }

    public void runPromote() throws EloraException {
        runPromoteAction();
        navigationContext.invalidateCurrentDocument();
        webActions.resetTabList();
    }

    public void runPromoteAction() throws EloraException {
        String logInitMsg = "[runPromoteAction] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            TreeNode topNode = super.getRoot().getChildren().get(0);
            promoteCheckerManager.resetValues();
            promoteCheckerManager.processTreeResult(topNode, 1, transition,
                    finalState, messages);

            if (promoteCheckerManager.isTopLevelOK()) {
                PromoteExecuterFactory promoteExecuterFactory = new PromoteExecuterFactoryImpl();
                PromoteExecuterManager promoteExecuterManager = promoteExecuterFactory.getExecuter(
                        transition);
                try {
                    promoteExecuterManager.processPromote(topNode, transition,
                            finalState,
                            promoteCheckerManager.getLifeCycleStatesConfig(),
                            eloraDocumentRelationManager);

                } catch (DocumentAlreadyLockedException e) {
                    log.trace(logInitMsg + "Document |"
                            + e.getDocument().getId() + "| already locked");
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.promote.documentAlreadyLocked"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle(), e.getOwner());
                    throw new EloraException(e.getMessage(), e.getCause());
                } catch (DocumentInUnlockableStateException e) {
                    log.trace(
                            logInitMsg + "Document |" + e.getDocument().getId()
                                    + "| is in unlockable state |"
                                    + e.getCurrentLifeCycleState() + "|");
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.promote.documentInUnlockableState"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle(),
                            e.getCurrentLifeCycleState());
                    throw new EloraException(e.getMessage(), e.getCause());
                } catch (DocumentLockRightsException e) {
                    log.trace(logInitMsg + "Cannot lock document |"
                            + e.getDocument().getId()
                            + "|. Rights exception. ");
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get("eloraplm.message.error.lock.rights"),
                            e.getDocument().getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE),
                            e.getDocument().getTitle());
                    throw new EloraException(e.getMessage(), e.getCause());
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.promote.KONodes"));
                throw new EloraException("There are KO nodes in tree");
            }

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.promote.success"));
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.promote.error"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.promote.error"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public void refreshNode(AjaxBehaviorEvent event) throws EloraException {
        promoteCheckerManager.resetValues();
        TreeNode node = (TreeNode) event.getComponent().getAttributes().get(
                "node");

        nodeService.processPartialTreeNode(super.getRoot(), node);
        allOK = promoteCheckerManager.isTopLevelOK();
    }

    public void refreshNodePropagation(AjaxBehaviorEvent event)
            throws EloraException {
        TreeNode node = (TreeNode) event.getComponent().getAttributes().get(
                "node");

        nodeService.processPartialTreeNodePropagation(super.getRoot(), node);
        allOK = promoteCheckerManager.isTopLevelOK();
    }

    public Map<String, String> getPromoteOptions() {
        return promoteOptions;
    }

    public void setPromoteOptions(Map<String, String> promoteOptions) {
        this.promoteOptions = promoteOptions;
    }

    public Map<String, String> getRelationOptions() {
        return relationOptions;
    }

    public void setRelationOptions(Map<String, String> relationOptions) {
        this.relationOptions = relationOptions;
    }

    public String getSelectedPromoteOption() {
        return selectedPromoteOption;
    }

    public void setSelectedPromoteOption(String selectedPromoteOption) {
        this.selectedPromoteOption = selectedPromoteOption;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    public boolean getAllOK() {
        return allOK;
    }

    @Override
    @Factory(value = "promoteRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}