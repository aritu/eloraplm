package com.aritu.eloraplm.cm.treetable;

import java.io.Serializable;
import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

/*@Name("cmBomImpactedItemsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
*/
public class CmImpactedItemsTreeBean extends CoreTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CmImpactedItemsTreeBean.class);

    private String itemType;
    // private static final String itemType = CMConstants.ITEM_TYPE_BOM;

    private CmImpactedItemsNodeService nodeService;

    public CmImpactedItemsTreeBean(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public void createRoot() {

        String logInitMsg = "[" + itemType + "] [createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService = new CmImpactedItemsNodeService(documentManager,
                    itemType);
            setRoot(nodeService.getRoot(currentDoc));

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void saveTree() {
        String logInitMsg = "[" + itemType + "] [saveTree] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel currentDoc = getCurrentDocument();

            nodeService.saveTree(currentDoc, getRoot());
            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.treetable.saveRoot"));

            // refresh current treebean
            resetBeanCache(null);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.saveRoot"));
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void refreshNode(TreeNode node, String triggeredField)
            throws EloraException {
        String logInitMsg = "[" + itemType + "] [refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        try {

            nodeService.refreshNode(node, triggeredField);

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.refreshNode"));
        }
        // log.trace(logInitMsg + "--- EXIT --- ");
    }
}