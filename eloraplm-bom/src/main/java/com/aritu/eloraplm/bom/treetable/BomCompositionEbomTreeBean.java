package com.aritu.eloraplm.bom.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTreeBean;

@Name("bomCompositionEbomTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCompositionEbomTreeBean extends EditableRelationTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCompositionEbomTreeBean.class);

    private boolean showUniqueVersionsPerDocument;

    private boolean showObsoleteStateDocuments;

    private boolean showDirectDocuments;

    private int maxOrdering;

    public boolean getShowUniqueVersionsPerDocument() {
        return showUniqueVersionsPerDocument;
    }

    public void setShowUniqueVersionsPerDocument(
            boolean showUniqueVersionsPerDocument) {
        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
    }

    public boolean getShowObsoleteStateDocuments() {
        return showObsoleteStateDocuments;
    }

    public void setShowObsoleteStateDocuments(
            boolean showObsoleteStateDocuments) {
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
    }

    public boolean getShowDirectDocuments() {
        return showDirectDocuments;
    }

    public void setShowDirectDocuments(boolean showDirectDocuments) {
        this.showDirectDocuments = showDirectDocuments;
    }

    public BomCompositionEbomTreeBean() throws EloraException {
        showUniqueVersionsPerDocument = false;
        showObsoleteStateDocuments = true;
        showDirectDocuments = false;
        setIsManual(true);
        maxOrdering = 0;
        setOrdering(1);
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new BomCompositionNodeService(documentManager,
                    showUniqueVersionsPerDocument, showObsoleteStateDocuments,
                    showDirectDocuments);
            setRoot(nodeService.getRoot(currentDoc));

            // Update max ordering
            maxOrdering = ((BomCompositionNodeService) nodeService).getMaxOrdering();
            setOrdering(maxOrdering + 1);

            setIsDirty(false);
            setHasUnreadableNodes(false);
            setIsInvalid(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + e.getMessage());
            // empty root attribute and set hasUnreadableNodes attribute to true
            setRoot(new DefaultTreeNode());
            setHasUnreadableNodes(true);
            setIsInvalid(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty root attribute and set isInvalid attribute to true
            setRoot(new DefaultTreeNode());
            setIsInvalid(true);
            setHasUnreadableNodes(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic,
            boolean isInverse) {
        setPredicateUri(EloraRelationConstants.BOM_COMPOSED_OF);
        super.addRelationNode(currentDoc, isAnarchic, isInverse);

        // Update max ordering
        if (getOrdering() != null && getOrdering() > maxOrdering) {
            maxOrdering = getOrdering();
            setOrdering(maxOrdering + 1);
        }
    }

    @Override
    @Factory(value = "bomCompositionEbomRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        setIsManual(true);
        setOrdering(maxOrdering + 1);
    }

}
