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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTreeBean;

@Name("bomCadDocumentsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCadDocumentsTreeBean extends EditableRelationTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCadDocumentsTreeBean.class);

    private boolean showObsoleteStateDocuments;

    public boolean getShowObsoleteStateDocuments() {
        return showObsoleteStateDocuments;
    }

    public void setShowObsoleteStateDocuments(
            boolean showObsoleteStateDocuments) {
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;
    }

    public BomCadDocumentsTreeBean() throws EloraException {
        showObsoleteStateDocuments = false;
        setAddDirectRelations(true);
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new BomCadDocumentsNodeService(documentManager,
                    showObsoleteStateDocuments);
            setRoot(nodeService.getRoot(currentDoc));
            setIsDirty(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic) {
        setPredicateUri(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        super.addRelationNode(currentDoc, isAnarchic);
    }

    @Override
    public void setObjectDocumentUid(String objectDocumentUid) {
        super.setObjectDocumentUid(objectDocumentUid);

        // Set default value for orderings
        if (objectDocumentUid != null) {
            DocumentModel doc = documentManager.getDocument(
                    new IdRef(objectDocumentUid));

            switch (doc.getType()) {
            case EloraDoctypeConstants.CAD_DRAWING:
                setViewerOrdering(1);
                setDirectorOrdering(null);
                break;
            case EloraDoctypeConstants.CAD_ASSEMBLY:
                setViewerOrdering(null);
                setDirectorOrdering(1);
                break;
            default:
                setViewerOrdering(null);
                setDirectorOrdering(null);
                break;
            }
        } else {
            resetCreateFormValues();
        }
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        setAddDirectRelations(true);
    }

    @Override
    @Factory(value = "bomCadDocumentsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }
}