package com.aritu.eloraplm.promote.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.promote.constants.PromoteConstants;

@Name("promoteDocumentBean")
@Scope(ScopeType.PAGE)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class PromoteDocumentBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private DocumentModel promotedDocument;

    private String selectedPromoteOption;

    private String selectedRelationOption;

    public String getSelectedRelationOption() {
        return selectedRelationOption;
    }

    public void setSelectedRelationOption(String selectedRelationOption) {
        this.selectedRelationOption = selectedRelationOption;
    }

    protected Map<String, String> promoteOptions;

    public DocumentModel getPromotedDocument() {
        return promotedDocument;
    }

    public void setPromotedDocument(DocumentModel currentDocument) {
        promotedDocument = currentDocument;
    }

    public String getSelectedPromoteOption() {
        return selectedPromoteOption;
    }

    public void setSelectedPromoteOption(String selectedPromoteOption) {
        this.selectedPromoteOption = selectedPromoteOption;
    }

    public Map<String, String> getPromoteOptions() {
        return promoteOptions;
    }

    public void setPromoteOptions(Map<String, String> promoteOptions) {
        this.promoteOptions = promoteOptions;
    }

    public PromoteDocumentBean() {
        // TODO: Cargar de BBDD y poner con constantes. Meter en
        // configuraciones?
        promoteOptions = new HashMap<String, String>();
        promoteOptions.put(EloraLifeCycleConstants.APPROVED,
                EloraLifeCycleConstants.APPROVED);
        promoteOptions.put(EloraLifeCycleConstants.OBSOLETE,
                EloraLifeCycleConstants.OBSOLETE);

        // Default value
        // selectedPromoteOption = EloraLifeCycleConstants.CAD_OBSOLETE;
        selectedRelationOption = PromoteConstants.AS_STORED;
    }

    public void applyFilters() {
        // firstLoad = false;
        // createRoot();
    }

}