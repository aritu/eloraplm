package com.aritu.eloraplm.bom.linked.characteristics.datatable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristic;
import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristicInfo;
import com.aritu.eloraplm.bom.linked.characteristics.util.LinkedCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.datatable.EditableDocBasedTable;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;

@Name(LinkedCharacteristicsTableBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class LinkedCharacteristicsTableBean extends EditableDocBasedTable {

    public static final String NAME = "linkedCharacteristicsTableBean";

    private static final long serialVersionUID = 1L;

    private DocumentModel linkedDocument;

    private List<SelectItem> linkableDocumentList;

    private List<LinkedCharacteristic> linkedDocumentCharacteristics;

    private List<LinkedCharacteristic> selectedLinkedDocumentCharacteristics;

    private static final Log log = LogFactory.getLog(
            LinkedCharacteristicsTableBean.class);

    public LinkedCharacteristicsTableBean() {
        tableService = new LinkedCharacteristicsTableServiceImpl();
        linkedDocumentCharacteristics = new ArrayList<LinkedCharacteristic>();
        selectedLinkedDocumentCharacteristics = new ArrayList<LinkedCharacteristic>();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");

            setData(tableService.getData(getCurrentDocument()));

            linkableDocumentList = getLinkableDocumentList(
                    getCurrentDocument());

            setIsDirty(false);
            log.trace(logInitMsg + "Table created.");
        } catch (DocumentUnreadableException e) {
            log.error(logInitMsg + e.getMessage());
            // empty data attribute and set hasUnreadableRows attribute to true
            setData(new ArrayList<RowData>());
            setHasUnreadableRows(true);
            setIsInvalid(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty data attribute and set isInvalid attribute to true
            setData(new ArrayList<RowData>());
            setIsInvalid(true);
            setHasUnreadableRows(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.createData"));
        }
    }

    @Override
    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "Adding linked characteristics rows to document |"
                        + getCurrentDocument().getId() + "|.");

        try {

            LinkedCharacteristicsTableServiceImpl ts = (LinkedCharacteristicsTableServiceImpl) tableService;

            boolean newCharacteristicLinked = false;

            if (selectedLinkedDocumentCharacteristics.size() > 0) {
                Long currentMaxOrderInReport = 0L;

                if (getData() != null && getData().size() > 0) {
                    LinkedCharacteristicRowData lastRow = (LinkedCharacteristicRowData) getData().get(
                            getData().size() - 1);
                    currentMaxOrderInReport = lastRow.getLinkedCharacteristic().getOrderInReport();
                }

                // Sort by orderInReport
                selectedLinkedDocumentCharacteristics.sort(Comparator.comparing(
                        LinkedCharacteristic::getOrderInReport));

                for (LinkedCharacteristic selectedLinkedDocumentCharacteristic : selectedLinkedDocumentCharacteristics) {

                    String characteristicId = selectedLinkedDocumentCharacteristic.getLinkedCharacteristicInfo()
                            .getLinkedCharacteristicId();

                    if (!characteristicAlreadyLinked(characteristicId)) {

                        if (selectedLinkedDocumentCharacteristic.getShowInReport()) {
                            currentMaxOrderInReport++;
                            selectedLinkedDocumentCharacteristic.setOrderInReport(
                                    currentMaxOrderInReport);
                        } else {
                            selectedLinkedDocumentCharacteristic.setOrderInReport(
                                    0L);
                        }

                        String rowId = getNewRowId();

                        RowData row = ts.createRowData(rowId,
                                selectedLinkedDocumentCharacteristic, true,
                                false, false);

                        getData().add(row);

                        log.trace(logInitMsg + "Row |" + rowId + "| added.");

                        newCharacteristicLinked = true;
                    }
                }

                if (newCharacteristicLinked) {
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.datatable.linkedCharacteristics.add"));
                } else {
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.datatable.linkedCharacteristics.noNewCharacteristicsSelected"));
                }

            } else {
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.warning.datatable.linkedCharacteristics.nothingSelected"));
            }

            resetCreateFormValues();
            setIsDirty(true);

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.linkedCharacteristics.add"));
        }

    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (getIsDirty()) {
            try {

                DocumentModel doc = getCurrentDocument();
                if (doc.isProxy()) {
                    doc = documentManager.getWorkingCopy(doc.getRef());
                }

                ArrayList<HashMap<String, Object>> linkedCharacteristicsList = new ArrayList<HashMap<String, Object>>();

                for (RowData r : getData()) {
                    LinkedCharacteristicRowData row = (LinkedCharacteristicRowData) r;
                    if (!row.getIsRemoved()) {
                        HashMap<String, Object> linkedCharacteristic = new HashMap<String, Object>();

                        linkedCharacteristic.put(
                                BomCharacteristicsConstants.LINKED_DOCUMENT,
                                row.getLinkedCharacteristic().getLinkedDocument().getId());

                        linkedCharacteristic.put(
                                BomCharacteristicsConstants.LINKED_DOCUMENT_CHARAC_ID,
                                row.getLinkedCharacteristic().getLinkedCharacteristicInfo()
                                        .getLinkedCharacteristicId());

                        linkedCharacteristic.put(
                                BomCharacteristicsConstants.SHOW_IN_REPORT,
                                row.getLinkedCharacteristic().getShowInReport());

                        linkedCharacteristic.put(
                                BomCharacteristicsConstants.ORDER_IN_REPORT,
                                row.getLinkedCharacteristic().getOrderInReport());

                        linkedCharacteristicsList.add(linkedCharacteristic);

                        row.setIsModified(false);
                    }
                }

                doc.setPropertyValue(
                        BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST,
                        linkedCharacteristicsList);

                documentManager.saveDocument(doc);
                documentManager.save();

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.datatable.linkedCharacteristics.save"));

            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.datatable.linkedCharacteristics.save"));
            }
        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.datatable.nothingToSave"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void updateLinkedCharacteristics() {

        String logInitMsg = "[updateLinkedCharacteristics] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel doc = getCurrentDocument();

            boolean linkedCharacteristicsUpdated = LinkedCharacteristicsHelper.updateLinkedCharacteristicsIfRequired(
                    documentManager, doc,
                    BomCharacteristicsConstants.LINKED_CHARACS_UPDATED_BY_UPDATE_BUTTON);

            if (linkedCharacteristicsUpdated) {
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.datatable.linkedCharacteristics.update"));
            } else {
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.datatable.linkedCharacteristics.nothingToUpdate"));
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.linkedCharacteristics.update"));
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private List<SelectItem> getLinkableDocumentList(DocumentModel doc) {

        List<SelectItem> linkableDocumentList = new ArrayList<SelectItem>();

        DocumentModelList linkableDocuments = retrieveLinkableDocuments(doc);
        for (DocumentModel linkableDoc : linkableDocuments) {

            String selectedLabel = "";
            String reference = (String) linkableDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (reference != null) {
                selectedLabel += reference + " ";
            }
            selectedLabel += linkableDoc.getTitle();
            linkableDocumentList.add(
                    new SelectItem(linkableDoc, selectedLabel));
        }

        return linkableDocumentList;
    }

    public DocumentModel getLinkedDocument() {
        return linkedDocument;
    }

    @SuppressWarnings("unchecked")
    public void setLinkedDocument(DocumentModel linkedDocument) {

        if (!linkedDocument.equals(this.linkedDocument)) {
            this.linkedDocument = linkedDocument;

            // Update linked document characteristics content in function of the
            // selected linked document
            ArrayList<HashMap<String, Object>> linkedDocumentCharacteristicsContent = new ArrayList<HashMap<String, Object>>();
            if (linkedDocument.getPropertyValue(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
                linkedDocumentCharacteristicsContent = (ArrayList<HashMap<String, Object>>) linkedDocument
                        .getPropertyValue(
                                BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);
            }

            List<LinkedCharacteristic> linkedDocumentCharacteristics = new ArrayList<LinkedCharacteristic>();
            for (int i = 0; i < linkedDocumentCharacteristicsContent.size(); ++i) {
                HashMap<String, Object> linkedDocumentBomCharacteristicType = linkedDocumentCharacteristicsContent.get(
                        i);
                LinkedCharacteristicInfo linkedCharacteristicInfo = LinkedCharacteristicsHelper
                        .createLinkedCharacteristicInfo(
                                linkedDocumentBomCharacteristicType);

                LinkedCharacteristic linkedCharacteristic = new LinkedCharacteristic(
                        linkedDocument, linkedCharacteristicInfo, true,
                        linkedCharacteristicInfo.getOrderInReport());

                linkedDocumentCharacteristics.add(linkedCharacteristic);
            }

            // Sort by orderInReport
            linkedDocumentCharacteristics.sort(Comparator.comparing(
                    LinkedCharacteristic::getOrderInReport));

            setLinkedDocumentCharacteristics(linkedDocumentCharacteristics);
            setSelectedLinkedDocumentCharacteristics(
                    linkedDocumentCharacteristics);
        }
    }

    public List<SelectItem> getLinkableDocumentList() {
        return linkableDocumentList;
    }

    public void setLinkableDocumentList(List<SelectItem> linkableDocumentList) {
        this.linkableDocumentList = linkableDocumentList;
    }

    public List<LinkedCharacteristic> getLinkedDocumentCharacteristics() {
        return linkedDocumentCharacteristics;
    }

    public void setLinkedDocumentCharacteristics(
            List<LinkedCharacteristic> linkedDocumentCharacteristics) {
        this.linkedDocumentCharacteristics = linkedDocumentCharacteristics;
    }

    public List<LinkedCharacteristic> getSelectedLinkedDocumentCharacteristics() {
        return selectedLinkedDocumentCharacteristics;
    }

    public void setSelectedLinkedDocumentCharacteristics(
            List<LinkedCharacteristic> selectedLinkedDocumentCharacteristics) {
        this.selectedLinkedDocumentCharacteristics = selectedLinkedDocumentCharacteristics;
    }

    private DocumentModelList retrieveLinkableDocuments(DocumentModel doc) {
        DocumentModelList linkableDocuments = new DocumentModelListImpl();

        // First, find related items (direct and hierarchical)
        DocumentModelList relatedDocuments = new DocumentModelListImpl();
        relatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_COMPOSED_OF)));
        relatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_HAS_BOM)));
        relatedDocuments.addAll(
                RelationHelper.getObjectDocuments(doc, new ResourceImpl(
                        EloraRelationConstants.BOM_HAS_SPECIFICATION)));

        // Then, filter the items that have characteristics
        for (DocumentModel relatedDoc : relatedDocuments) {

            if (EloraDocumentHelper.checkFilter(doc,
                    BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
                if (relatedDoc.getPropertyValue(
                        BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST) != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<HashMap<String, Object>> relatedDocCharacteristics = (ArrayList<HashMap<String, Object>>) relatedDoc
                            .getPropertyValue(
                                    BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST);

                    if (relatedDocCharacteristics.size() > 0
                            && !linkableDocuments.contains(relatedDoc)) {
                        linkableDocuments.add(relatedDoc);
                    }

                    linkableDocuments.addAll(
                            retrieveLinkableDocuments(relatedDoc));
                }
            }
        }

        return linkableDocuments;
    }

    private boolean characteristicAlreadyLinked(String characteristicId) {

        for (RowData r : getData()) {
            LinkedCharacteristicRowData row = (LinkedCharacteristicRowData) r;

            if (row.getLinkedCharacteristic().getLinkedCharacteristicInfo().getLinkedCharacteristicId().equals(
                    characteristicId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();

        linkedDocument = null;
        linkedDocumentCharacteristics = new ArrayList<LinkedCharacteristic>();
        selectedLinkedDocumentCharacteristics = new ArrayList<LinkedCharacteristic>();
    }

}