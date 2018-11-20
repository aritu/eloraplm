package com.aritu.eloraplm.relations.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("cadCompositionTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CadCompositionTreeBean extends EditableRelationTreeBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CadCompositionTreeBean.class);

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

    public CadCompositionTreeBean() {
        showDrawings = true;
        showObsoleteStateDocuments = true;
        showUniqueVersionsPerDocument = true;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new CadCompositionNodeService(documentManager,
                    showDrawings, showSuppressed, showObsoleteStateDocuments,
                    showUniqueVersionsPerDocument);
            setRoot(nodeService.getRoot(currentDoc));
            setIsDirty(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    public Map<String, String> getPredicateList() {
        Map<String, String> predicates = new HashMap<String, String>();

        switch (getCurrentDocument().getType()) {
        case EloraDoctypeConstants.CAD_DRAWING:
            predicates.put(EloraRelationConstants.CAD_DRAWING_OF,
                    EloraRelationConstants.CAD_DRAWING_OF);
            break;
        case EloraDoctypeConstants.CAD_ASSEMBLY:
            predicates.put(EloraRelationConstants.CAD_COMPOSED_OF,
                    EloraRelationConstants.CAD_COMPOSED_OF);
            predicates.put(EloraRelationConstants.CAD_BASED_ON,
                    EloraRelationConstants.CAD_BASED_ON);
            // predicates.put(EloraRelationConstants.CAD_HAS_SUPPRESSED,
            // EloraRelationConstants.CAD_HAS_SUPPRESSED);
            break;
        case EloraDoctypeConstants.CAD_PART:
            predicates.put(EloraRelationConstants.CAD_BASED_ON,
                    EloraRelationConstants.CAD_BASED_ON);
            break;
        }

        return predicates;
    }

    @Override
    @Factory(value = "cadCompositionRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }
}