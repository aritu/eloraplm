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
package com.aritu.eloraplm.datatable;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.primefaces.event.RowEditEvent;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public abstract class EditableAdminTable extends AdminTable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EditableAdminTable.class);

    @In(create = true)
    protected transient WebActions webActions;

    private String rowId;

    private boolean isDirty = false;

    private RowData newRow;

    private RowData editingRow;

    private Integer editingRowIndex;

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public boolean getIsDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public RowData getNewRow() {
        return newRow;
    }

    public void setNewRow(RowData newRow) {
        this.newRow = newRow;
    }

    public RowData getEditingRow() {
        return editingRow;
    }

    public void setEditingRow(RowData editingRow) {
        this.editingRow = editingRow;
        if (editingRow == null) {
            editingRowIndex = null;
        } else {
            editingRowIndex = getData().indexOf(editingRow);
        }
    }

    public void setEditingRowByIndex(int rowIndex) {
        RowData row = getData().get(rowIndex);
        setEditingRow(row);
    }

    public Integer getEditingRowIndex() {
        return editingRowIndex;
    }

    public EditableAdminTable() {
    }

    public void addRow() {
        String logInitMsg = "[addRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding row |" + rowId + "| to datatable.");

        try {

            RowData row = createAndAddRowData(rowId, true, false);

            log.trace(logInitMsg + "Row |" + rowId + "| added.");

            resetCreateFormValues();
            setIsDirty(true);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.datatable.row.add"));

        } catch (

        Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.datatable.row.add"));
        }
    }

    protected RowData createAndAddRowData(String rowId, boolean isNew,
            boolean isRemoved) {
        RowData row = ((EditableTableService) tableService).createRowData(rowId,
                isNew, false, isRemoved);
        getData().add(row);

        return row;
    }

    public void updateRow() {
        String logInitMsg = "[updateRow] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Modifying row |" + editingRow.getId()
                + "| in datatable.");

        try {
            int i = getData().indexOf(editingRow);
            if (i == -1) {
                throw new EloraException(
                        "Current editing row does not exist in the datatable.");
            }

            markRowAsModified(editingRow, String.valueOf(i));

            log.trace(logInitMsg + "Row |" + editingRow.getId() + "| updated.");

            editingRow = null;

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.datatable.row.update"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.datatable.row.update"));
        }

    }

    public void markRowAsModified(RowData row, String rowIndex) {
        row.setIsModified(true);
        setIsDirty(true);
    }

    public void markRowAsRemoved(RowData row, String rowIndex) {
        toggleRowAsRemoved(row, rowIndex, true);
    }

    public void unmarkRowAsRemoved(RowData row, String rowIndex) {
        toggleRowAsRemoved(row, rowIndex, false);

    }

    private void toggleRowAsRemoved(RowData row, String rowIndex,
            boolean isRemoved) {
        if (isRemoved && row.getIsNew()) {
            removeRow(row.getId());
        }

        row.setIsRemoved(isRemoved);
        setIsDirty(true);
    }

    protected void removeRow(String rowId) {
        // TODO ALDATU Hashmap batekin???

        Iterator<RowData> i = getData().listIterator();
        while (i.hasNext()) {
            RowData row = i.next();
            if (row.getId().equals(rowId)) {
                i.remove();
                break;
            }
        }
    }

    protected abstract void save();

    protected void resetCreateFormValues() {
        rowId = null;
    }

    public void onRowEdit(RowEditEvent event) {
        RowData row = (RowData) event.getObject();
        row.setIsModified(true);
        setIsDirty(true);
    }
}
