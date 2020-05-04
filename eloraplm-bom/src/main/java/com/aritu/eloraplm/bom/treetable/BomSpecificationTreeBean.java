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

@Name("bomSpecificationTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomSpecificationTreeBean extends EditableRelationTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomSpecificationTreeBean.class);

    protected boolean showUniqueVersionsPerDocument;

    protected boolean showObsoleteStateDocuments;

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

    public BomSpecificationTreeBean() throws EloraException {
        showUniqueVersionsPerDocument = true;
        showObsoleteStateDocuments = false;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new BomSpecificationNodeService(documentManager,
                    showUniqueVersionsPerDocument, showObsoleteStateDocuments);
            setRoot(nodeService.getRoot(currentDoc));
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
    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic) {
        setPredicateUri(EloraRelationConstants.BOM_HAS_SPECIFICATION);
        super.addRelationNode(currentDoc, isAnarchic);
    }

    @Override
    @Factory(value = "bomSpecificationRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }
}
