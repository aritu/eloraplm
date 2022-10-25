package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.core.util.EloraEventHelper;

@Name("cmDocImpactedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class DocImpactedItemsTreeBean extends ImpactedItemsTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            DocImpactedItemsTreeBean.class);

    private boolean reloadDocImpactedItems = false;

    public DocImpactedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_DOC);
    }

    @Override
    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            super.saveTree();
            reloadDocImpactedItems = true;

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_IMPACTED_DOCS_CHANGES_SAVED_EVENT,
                    navigationContext.getCurrentDocument());

        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    @Override
    public TreeNode getRoot() {
        if (reloadDocImpactedItems) {
            createRoot();
            reloadDocImpactedItems = false;
        }
        return super.getRoot();
    }

    @Observer(value = { CMEventNames.CM_BOM_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_DOC_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_DOC_IMPACT_MATRIX_CALCULATED,
            CMEventNames.CM_REFRESH_DOCS_IMPACT_MATRIX })
    @BypassInterceptors
    public void markTreeToBeReloaded() {
        reloadDocImpactedItems = true;
    }

    @Override
    @Factory(value = "docImpactedItemsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}
