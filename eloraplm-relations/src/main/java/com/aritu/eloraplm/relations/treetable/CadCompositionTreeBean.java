package com.aritu.eloraplm.relations.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("cadCompositionTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CadCompositionTreeBean extends EditableRelationTreeBean implements
        Serializable {
    private static final long serialVersionUID = 1L;

    protected boolean showDrawings;

    protected boolean showUniqueVersionsPerDocument;

    public boolean getShowDrawings() {
        return showDrawings;
    }

    public void setShowDrawings(boolean showDrawings) {
        this.showDrawings = showDrawings;
    }

    public boolean getShowUniqueVersionsPerDocument() {
        return showUniqueVersionsPerDocument;
    }

    public void setShowUniqueVersionsPerDocument(
            boolean showUniqueVersionsPerDocument) {
        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
    }

    public CadCompositionTreeBean() {
        showDrawings = true;
        showUniqueVersionsPerDocument = true;
    }

    @Override
    public void createRoot() {
        DocumentModel currentDoc = getCurrentDocument();
        try {
            CadCompositionNodeService nodeService = new CadCompositionNodeService(
                    documentManager, showDrawings,
                    showUniqueVersionsPerDocument);
            setRoot(nodeService.getRoot(currentDoc));
        } catch (EloraException e) {
            // TODO Logetan idatzi

            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.treetable.createRoot"));
        }
    }
}