package com.aritu.eloraplm.bom.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTreeBean;

@Name("bomCompositionEbomTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCompositionEbomTreeBean extends EditableRelationTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    protected boolean showUniqueVersionsPerDocument;

    public boolean getShowUniqueVersionsPerDocument() {
        return showUniqueVersionsPerDocument;
    }

    public void setShowUniqueVersionsPerDocument(
            boolean showUniqueVersionsPerDocument) {
        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
    }

    public BomCompositionEbomTreeBean() {
        showUniqueVersionsPerDocument = true;
    }

    @Override
    public void createRoot() {
        DocumentModel currentDoc = getCurrentDocument();
        try {
            BomCompositionNodeService nodeService = new BomCompositionNodeService(
                    documentManager, showUniqueVersionsPerDocument);
            setRoot(nodeService.getRoot(currentDoc));
        } catch (EloraException e) {
            // TODO Logetan idatzi

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    public void addRelationNode(DocumentModel currentDoc) {
        setPredicateUri(EloraRelationConstants.BOM_COMPOSED_OF);
        super.addRelationNode(currentDoc);
    }

}