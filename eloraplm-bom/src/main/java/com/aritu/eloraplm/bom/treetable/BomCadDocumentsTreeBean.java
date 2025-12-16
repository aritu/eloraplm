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
import org.primefaces.model.DefaultTreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTree;

@Name(BomCadDocumentsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCadDocumentsTreeBean extends EditableRelationTree {

    public static final String NAME = "bomCadDocumentsTreeBean";

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
        setPredicateUri(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        super.addRelationNode(currentDoc, isAnarchic, isInverse);
    }

    @Override
    public void setObjectDocumentWcUid(String objectDocumentWcUid) {
        super.setObjectDocumentWcUid(objectDocumentWcUid);

        // Set default value for orderings
        if (objectDocumentWcUid != null && getObjectDocument() != null) {
            switch (getObjectDocument().getType()) {
                case EloraDoctypeConstants.CAD_DRAWING:
                    setViewerOrdering(1);
                    setDirectorOrdering(null);
                    break;
                case EloraDoctypeConstants.CAD_ASSEMBLY:
                case EloraDoctypeConstants.CAD_PART:
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

}
