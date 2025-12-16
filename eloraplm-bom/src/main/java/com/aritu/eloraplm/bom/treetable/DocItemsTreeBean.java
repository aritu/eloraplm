package com.aritu.eloraplm.bom.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.primefaces.model.DefaultTreeNode;

import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.treetable.CoreTree;

@Name(DocItemsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class DocItemsTreeBean extends CoreTree {

    public static final String NAME = "docItemsTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocItemsTreeBean.class);

    private boolean showUniqueVersionsPerDocument;

    private boolean showObsoleteStateDocuments;

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

    public DocItemsTreeBean() {
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
            DocItemsNodeService nodeService = new DocItemsNodeService(
                    documentManager, showUniqueVersionsPerDocument,
                    showObsoleteStateDocuments);
            setRoot(nodeService.getRoot(currentDoc));
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
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";
        doc = super.onContextChange(doc);

        if (doc != null) {
            if (!(doc.isCheckedOut() == getCurrentDocument().isCheckedOut())) {
                setCurrentDocument(doc);
                resetBeanCache(doc);
                log.trace(logInitMsg
                        + "Document invalidated: current and new have different checked out status.");
            }
        }

        return doc;
    }
}
