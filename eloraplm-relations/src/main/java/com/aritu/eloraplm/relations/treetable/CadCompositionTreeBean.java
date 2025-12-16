package com.aritu.eloraplm.relations.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;

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
import com.aritu.eloraplm.relations.util.CadCompositionAbstractExcelExporter;
import com.aritu.eloraplm.relations.util.CadCompositionExcelExporter;

@Name(CadCompositionTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class CadCompositionTreeBean extends EditableRelationTree {

    public static final String NAME = "cadCompositionTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            CadCompositionTreeBean.class);

    private boolean showDrawings;

    private boolean showSuppressed;

    private boolean showExcludedFromComposition;

    private boolean showObsoleteStateDocuments;

    private boolean showUniqueVersionsPerDocument;

    private boolean isCadCompositionExportEnabled;

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

    public boolean getShowExcludedFromComposition() {
        return showExcludedFromComposition;
    }

    public void setShowExcludedFromComposition(
            boolean showExcludedFromComposition) {
        this.showExcludedFromComposition = showExcludedFromComposition;
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

    public boolean isCadCompositionExportEnabled() {
        return isCadCompositionExportEnabled;
    }

    public void setCadCompositionExportEnabled(
            boolean isCadCompositionExportEnabled) {
        this.isCadCompositionExportEnabled = isCadCompositionExportEnabled;
    }

    public CadCompositionTreeBean() {
        showDrawings = true;
        showSuppressed = false;
        showExcludedFromComposition = false;
        showObsoleteStateDocuments = true;
        showUniqueVersionsPerDocument = true;
        setIsManual(true);
        setMaxOrdering(1);
        setOrdering(1);
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new CadCompositionNodeService(documentManager,
                    showDrawings, showSuppressed, showExcludedFromComposition,
                    showObsoleteStateDocuments, showUniqueVersionsPerDocument);
            setRoot(nodeService.getRoot(currentDoc));

            // Update max ordering
            setMaxOrdering(
                    ((CadCompositionNodeService) nodeService).getMaxOrdering());
            setOrdering(getMaxOrdering() + 1);

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

    public List<String> getPredicateList() {
        List<String> predicates = new ArrayList<String>();

        switch (getCurrentDocument().getType()) {
            case EloraDoctypeConstants.CAD_DRAWING:
                predicates.add(EloraRelationConstants.CAD_DRAWING_OF);
                break;
            case EloraDoctypeConstants.CAD_ASSEMBLY:
                predicates.add(EloraRelationConstants.CAD_COMPOSED_OF);
                predicates.add(EloraRelationConstants.CAD_HAS_SUPPRESSED);
                predicates.add(
                        EloraRelationConstants.CAD_HAS_EXCLUDED_FROM_COMPOSITION);
                break;
            case EloraDoctypeConstants.CAD_PART:
                predicates.add(EloraRelationConstants.CAD_BASED_ON);
                break;
        }

        if (predicates.size() == 1) {
            setPredicateUri(predicates.get(0));
        }

        return predicates;
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        setIsManual(true);
    }

    public void exportCadStructure() {

        String logInitMsg = "[exportCadStructure] ["
                + documentManager.getPrincipal().getName() + "] ";

        try {

            DocumentModel currentDoc = getCurrentDocument();

            CadCompositionAbstractExcelExporter excelExporter = new CadCompositionExcelExporter(
                    getRoot(), messages);

            excelExporter.export(currentDoc, false);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.exportCadStructure"));
        }
    }
}
