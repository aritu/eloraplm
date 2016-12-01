package com.aritu.eloraplm.promote.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.exceptions.EloraException;
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

    protected Map<String, String> promoteOptions;

    protected Map<String, String> relationOptions;

    private String selectedPromoteOption;

    private String selectedRelationOption;

    private boolean firstLoad;

    public PromoteTreeBean() {
        // TODO: Cargar de BBDD y poner con constantes. Meter en
        // configuraciones?
        promoteOptions = new HashMap<String, String>();
        promoteOptions.put(EloraLifeCycleConstants.CAD_APPROVED,
                EloraLifeCycleConstants.CAD_APPROVED);
        promoteOptions.put(EloraLifeCycleConstants.CAD_PRELIMINARY,
                EloraLifeCycleConstants.CAD_PRELIMINARY);

        // Default value
        selectedPromoteOption = EloraLifeCycleConstants.CAD_APPROVED;
        selectedRelationOption = "latestVersion";
    }

    @Override
    public void createRoot() {

        relationOptions = new HashMap<String, String>();

        relationOptions.put("latestReleased",
                messages.get("eloraplm.label.latest.released"));
        relationOptions.put("asStored",
                messages.get("eloraplm.label.as.stored"));
        relationOptions.put("latestVersion",
                messages.get("eloraplm.label.latest.version"));

        DocumentModel currentDoc = getCurrentDocument();
        try {
            nodeService = new PromoteNodeService(selectedPromoteOption,
                    selectedRelationOption, documentManager,
                    eloraDocumentRelationManager, firstLoad, messages);
            setRoot(nodeService.getRoot(currentDoc));

        } catch (EloraException e) {
            // TODO Logetan idatzi
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.createRoot"));
        }
    }

    public void applyFilters() {
        firstLoad = false;
        createRoot();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        firstLoad = true;
        createRoot();
    }

    public String runPromote() throws EloraException {
        String logInitMsg = "[runPromote] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            nodeService.runPromote(super.getRoot());

            navigationContext.invalidateCurrentDocument();
            webActions.resetTabList();

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.promote.success"));
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR, e.getMessage());
        } catch (Exception e) {
            log.error(logInitMsg + "Uncontrolled exception: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.promote.error"));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        return null;
    }

    public void refreshNode(AjaxBehaviorEvent event) throws EloraException {
        TreeNode node = (TreeNode) event.getComponent().getAttributes().get(
                "node");

        nodeService.processPartialTreeNode(super.getRoot(), node);
    }

    public void refreshNodePropagation(AjaxBehaviorEvent event)
            throws EloraException {
        TreeNode node = (TreeNode) event.getComponent().getAttributes().get(
                "node");

        nodeService.processPartialTreeNodePropagation(super.getRoot(), node);
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

    public String getSelectedRelationOption() {
        return selectedRelationOption;
    }

    public void setSelectedRelationOption(String selectedRelationOption) {
        this.selectedRelationOption = selectedRelationOption;
    }

    public String getSelectedPromoteOption() {
        return selectedPromoteOption;
    }

    public void setSelectedPromoteOption(String selectedPromoteOption) {
        this.selectedPromoteOption = selectedPromoteOption;
    }

}