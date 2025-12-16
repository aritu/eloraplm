package com.aritu.eloraplm.cm.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.CMConfig;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMEventNames;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.core.util.EloraEventHelper;

@Name(DocImpactedItemsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class DocImpactedItemsTreeBean extends AbstractImpactedItemsTree {

    public static final String NAME = "cmDocImpactedItemsTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            DocImpactedItemsTreeBean.class);

    private boolean reloadDocImpactedItems = false;

    private List<String> cadLifeCycleStates = CMConfig.cadLifeCycleStates;

    private List<String> basicDocLifeCycleStates = CMConfig.basicDocLifeCycleStates;

    private Map<String, String> docLifeCycleStatesFilterMap;

    public DocImpactedItemsTreeBean() {
        super(CMConstants.ITEM_TYPE_DOC);
        fillDocFilterValues();
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

    private void fillDocFilterValues() {
        fillFilterValues();

        docLifeCycleStatesFilterMap = new HashMap<String, String>();
        for (String state : cadLifeCycleStates) {
            if (!state.equals(EloraLifeCycleConstants.OBSOLETE)
                    && !state.equals(EloraLifeCycleConstants.PRECREATED)) {
                docLifeCycleStatesFilterMap.put(state.concat(".abbr"), state);
            }
        }
        for (String state : basicDocLifeCycleStates) {
            if (!state.equals(EloraLifeCycleConstants.OBSOLETE)
                    && !docLifeCycleStatesFilterMap.containsKey(state)) {
                docLifeCycleStatesFilterMap.put(state.concat(".abbr"), state);
            }
        }
    }

    @Override
    public TreeNode getRoot() {
        if (reloadDocImpactedItems) {
            createRoot();
            reloadDocImpactedItems = false;
        }
        return super.getRoot();
    }

    @Observer(value = { CMEventNames.CM_MODIFIED_ITEMS_SAVED,
            CMEventNames.CM_IMPACT_MATRIX_CALCULATED,
            CMEventNames.CM_REFRESH_DOCS_IMPACT_MATRIX })
    @BypassInterceptors
    public void markTreeToBeReloaded() {
        reloadDocImpactedItems = true;
    }

    public Map<String, String> getDocLifeCycleStatesFilterMap() {
        return docLifeCycleStatesFilterMap;
    }

}
