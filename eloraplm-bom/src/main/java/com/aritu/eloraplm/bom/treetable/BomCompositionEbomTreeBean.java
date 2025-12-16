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

import com.aritu.eloraplm.bom.util.BomCompositionAbstractExcelExporter;
import com.aritu.eloraplm.bom.util.BomCompositionExcelExporter;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTree;

@Name(BomCompositionEbomTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCompositionEbomTreeBean extends EditableRelationTree {

    public static final String NAME = "bomCompositionEbomTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCompositionEbomTreeBean.class);

    private boolean showUniqueVersionsPerDocument;

    private boolean showObsoleteStateDocuments;

    private boolean showDirectDocuments;

    private boolean hasElectronicComponents;

    protected boolean isBomCompositionExportEnabled;

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

    public boolean getShowDirectDocuments() {
        return showDirectDocuments;
    }

    public void setShowDirectDocuments(boolean showDirectDocuments) {
        this.showDirectDocuments = showDirectDocuments;
    }

    public boolean getHasElectronicComponents() {
        return hasElectronicComponents;
    }

    public void setHasElectronicComponents(boolean hasElectronicComponents) {
        this.hasElectronicComponents = hasElectronicComponents;
    }

    public boolean getIsBomCompositionExportEnabled() {
        return isBomCompositionExportEnabled;
    }

    public BomCompositionEbomTreeBean() throws EloraException {
        showUniqueVersionsPerDocument = false;
        showObsoleteStateDocuments = true;
        showDirectDocuments = false;
        setIsManual(true);
        setMaxOrdering(1);
        setOrdering(1);
        hasElectronicComponents = false;
        isBomCompositionExportEnabled = false;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            nodeService = new BomCompositionNodeService(documentManager,
                    showUniqueVersionsPerDocument, showObsoleteStateDocuments,
                    showDirectDocuments);
            setRoot(nodeService.getRoot(currentDoc));

            // Update max ordering
            setMaxOrdering(
                    ((BomCompositionNodeService) nodeService).getMaxOrdering());
            setOrdering(getMaxOrdering() + 1);

            // Update hasElectronicComponents
            hasElectronicComponents = ((BomCompositionNodeService) nodeService).getHasElectronicComponents();

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
        setPredicateUri(EloraRelationConstants.BOM_COMPOSED_OF);
        super.addRelationNode(currentDoc, isAnarchic, isInverse);
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        setIsManual(true);
    }

    public void exportBomStructure() {
        String logInitMsg = "[exportBomStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            DocumentModel currentDoc = getCurrentDocument();
            BomCompositionAbstractExcelExporter excelExporter = new BomCompositionExcelExporter(
                    getRoot(), messages);
            excelExporter.export(currentDoc, false, false);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.exportBomStructure"));
        }
    }

}
