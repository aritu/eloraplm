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

@Name("cmBomImpactedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomImpactedItemsTreeBean extends ImpactedItemsTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomImpactedItemsTreeBean.class);

    private boolean reloadBomImpactedItems = false;

    public BomImpactedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_BOM);
    }

    @Override
    public void saveTree() {
        String logInitMsg = "[saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // verify if something has changed in the tree
        if (getIsDirty()) {
            super.saveTree();
            reloadBomImpactedItems = true;

            // Nuxeo event
            EloraEventHelper.fireEvent(
                    CMEventNames.CM_IMPACTED_ITEMS_CHANGES_SAVED_EVENT,
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
        if (reloadBomImpactedItems) {
            createRoot();
            reloadBomImpactedItems = false;
        }
        return super.getRoot();
    }

    @Observer(value = { CMEventNames.CM_BOM_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_DOC_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_BOM_IMPACT_MATRIX_CALCULATED,
            CMEventNames.CM_REFRESH_ITEMS_IMPACT_MATRIX })
    @BypassInterceptors
    public void markTreeToBeRealoded() {
        reloadBomImpactedItems = true;
    }

    @Override
    @Factory(value = "bomImpactedItemsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}