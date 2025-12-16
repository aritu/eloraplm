/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.bom.datatable;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.datatable.EditableDocBasedTable;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
@Name(ItemSuppliersTableBean.NAME)
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class ItemSuppliersTableBean extends EditableDocBasedTable {

    public static final String NAME = "itemSuppliersTableBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ItemSuppliersTableBean.class);

    private String supplier;

    private int preference;

    private int maxPreference;

    private String partNumber;

    // validity related fields
    private String validationMsg;

    private boolean isValid;

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;

        // Supplier and partNumber pair values cannot be repeated in the
        // list.
        if (supplier != null) {
            String validationResultMessage = validateSupplierAndPartNumberValues(
                    supplier, partNumber);

            if (validationResultMessage != null
                    && validationResultMessage.length() > 0) {
                setValidationMsg(messages.get(validationResultMessage));
                setIsValid(false);
            } else {
                setValidationMsg(null);
                setIsValid(true);
            }

        } else {
            setValidationMsg(null);
            setIsValid(false);
        }
    }

    public int getPreference() {
        return preference;
    }

    public void setPreference(int preference) {
        this.preference = preference;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;

        // Supplier and partNumber pair values cannot be repeated in the
        // list.
        String validationResultMessage = validateSupplierAndPartNumberValues(
                supplier, this.partNumber);

        if (validationResultMessage != null
                && validationResultMessage.length() > 0) {
            setValidationMsg(messages.get(validationResultMessage));
            setIsValid(false);
        } else {
            setValidationMsg(null);
            setIsValid(true);
        }

    }

    public String getValidationMsg() {
        return validationMsg;
    }

    public void setValidationMsg(String validationMsg) {
        this.validationMsg = validationMsg;
    }

    public boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public ItemSuppliersTableBean() {
        tableService = new ItemSuppliersTableServiceImpl();
    }

    @Override
    public void createData() {
        String logInitMsg = "[createData] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            log.trace(logInitMsg + "Creating table...");
            setData(tableService.getData(getCurrentDocument()));
            calculateMaxPreference();
            setPreference(maxPreference);

            setIsDirty(false);
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

    private void calculateMaxPreference() {
        String logInitMsg = "[calculateMaxPreference] ["
                + documentManager.getPrincipal().getName() + "] ";

        maxPreference = 1;
        for (RowData r : getData()) {
            ItemSupplierRowData row = (ItemSupplierRowData) r;

            if (row.getPreference() >= maxPreference) {
                maxPreference = row.getPreference() + 1;
            }
        }
        log.trace(logInitMsg + "maxPreference = |" + maxPreference + "|");
    }

    private String validateSupplierAndPartNumberValues(String supplier,
            String partNumber) {
        String logInitMsg = "[validateSupplierAndPartNumberValues] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        boolean isValid = true;

        String validationResultMessage = null;

        for (RowData r : getData()) {
            ItemSupplierRowData row = (ItemSupplierRowData) r;

            if (row.getSupplier().equalsIgnoreCase(supplier)) {

                if (row.getPartNumber() == null && partNumber == null) {
                    isValid = false;
                    break;
                }
                if (row.getPartNumber() != null
                        && row.getPartNumber().equalsIgnoreCase(partNumber)) {
                    isValid = false;
                    break;
                }
            }
        }

        if (!isValid) {
            validationResultMessage = "eloraplm.message.error.item.suppliers.supplierPartNumberPairAlreadyExist";
        }

        log.trace(logInitMsg + "--- EXIT with validationResultMessage = |"
                + validationResultMessage + "| ---");

        return validationResultMessage;
    }

    @Override
    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding row to suppliers datatable of item |"
                + getCurrentDocument().getId() + "|.");

        try {
            ItemSuppliersTableServiceImpl ts = (ItemSuppliersTableServiceImpl) tableService;

            String rowId = getNewRowId();

            RowData row = ts.createRowData(rowId, getSupplier(),
                    getPreference(), getPartNumber(), true, false, false);

            getData().add(row);

            log.trace(logInitMsg + "Row |" + rowId + "| added.");

            if (getPreference() >= maxPreference) {
                maxPreference = getPreference() + 1;
            }

            resetCreateFormValues();
            setIsDirty(true);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.datatable.row.add"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.datatable.row.add"));
        }
    }

    @Override
    public void save() {
        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";

        if (getIsDirty()) {
            try {

                DocumentModel doc = getCurrentDocument();
                if (doc.isProxy()) {
                    doc = documentManager.getWorkingCopy(doc.getRef());
                }

                List<HashMap<String, Object>> itemSupplierList = new ArrayList<HashMap<String, Object>>();

                for (RowData r : getData()) {
                    ItemSupplierRowData row = (ItemSupplierRowData) r;
                    if (!row.getIsRemoved()) {
                        HashMap<String, Object> itemSupplier = new HashMap<String, Object>();

                        itemSupplier.put(
                                EloraMetadataConstants.ELORA_SUPP_SUPPLIERS_SUPPLIER,
                                row.getSupplier());
                        itemSupplier.put(
                                EloraMetadataConstants.ELORA_SUPP_SUPPLIERS_PREFERENCE,
                                row.getPreference());
                        itemSupplier.put(
                                EloraMetadataConstants.ELORA_SUPP_SUPPLIERS_PARTNUMBER,
                                row.getPartNumber());

                        itemSupplierList.add(itemSupplier);

                        row.setIsModified(false);
                    }
                }
                if (doc.isCheckedOut()) {
                    doc.setPropertyValue(
                            EloraMetadataConstants.ELORA_SUPP_SUPPLIERS,
                            (Serializable) itemSupplierList);
                    doc = documentManager.saveDocument(doc);
                    documentManager.save();
                } else {
                    DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                            doc);
                    if (baseDoc == null) {
                        throw new EloraException("The document |" + doc.getId()
                                + "| has no base version.");
                    }

                    EloraDocumentHelper.disableVersioningDocument(baseDoc);
                    baseDoc.setPropertyValue(
                            EloraMetadataConstants.ELORA_SUPP_SUPPLIERS,
                            (Serializable) itemSupplierList);
                    baseDoc = documentManager.saveDocument(baseDoc);
                    documentManager.save();

                    EloraDocumentHelper.restoreToVersion(doc.getRef(),
                            baseDoc.getRef(), true, true, documentManager);
                }

                doc.refresh();
                createData();
                calculateMaxPreference();

            } catch (Exception e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.item.suppliers.save"));
            }
        }

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("eloraplm.message.success.item.suppliers.save"));
    }

    @Override
    protected void resetCreateFormValues() {
        super.resetCreateFormValues();
        supplier = null;
        preference = maxPreference;
        partNumber = null;
        validationMsg = null;
        isValid = false;
    }

}
