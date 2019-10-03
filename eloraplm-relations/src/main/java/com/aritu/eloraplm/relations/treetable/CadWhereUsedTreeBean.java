package com.aritu.eloraplm.relations.treetable;

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
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.treetable.CoreTreeBean;

@Name("cadWhereUsedTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CadWhereUsedTreeBean extends CoreTreeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CadWhereUsedTreeBean.class);

    private boolean showDrawings;

    private boolean showSuppressed;

    private boolean showObsoleteStateDocuments;

    private boolean showUniqueVersionsPerDocument;

    public boolean getShowDrawings() {
        return showDrawings;
    }

    public void setShowDrawings(boolean showDrawings) {
        this.showDrawings = showDrawings;
    }

    public boolean getShowSuppressed() {
        return showSuppressed;
    }

    public void setShowSuppressed(boolean showSuppressed) {
        this.showSuppressed = showSuppressed;
    }

    public boolean getShowObsoleteStateDocuments() {
        return showObsoleteStateDocuments;
    }

    public void setShowObsoleteStateDocuments(
            boolean showObsoleteStateDocuments) {
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
    }

    public boolean getShowUniqueVersionsPerDocument() {
        return showUniqueVersionsPerDocument;
    }

    public void setShowUniqueVersionsPerDocument(
            boolean showUniqueVersionsPerDocument) {
        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
    }

    public CadWhereUsedTreeBean() {
        showDrawings = true;
        showSuppressed = false;
        showObsoleteStateDocuments = false;
        showUniqueVersionsPerDocument = true;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            CadWhereUsedNodeService nodeService = new CadWhereUsedNodeService(
                    documentManager, showDrawings, showSuppressed,
                    showObsoleteStateDocuments, showUniqueVersionsPerDocument);
            setRoot(nodeService.getRoot(currentDoc));
            log.trace(logInitMsg + "Tree created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        if (!(doc.isCheckedOut() == getCurrentDocument().isCheckedOut())) {
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different checked out status.");
        }

        return doc;
    }

    @Override
    @Factory(value = "cadWhereUsedRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}