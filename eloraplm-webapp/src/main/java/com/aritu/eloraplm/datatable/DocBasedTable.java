package com.aritu.eloraplm.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.SortMeta;

import com.aritu.eloraplm.core.DocumentContextBoundActions;

public abstract class DocBasedTable extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected TableService tableService;

    private List<RowData> data;

    private int size;

    private boolean hasUnreadableRows;

    private boolean isInvalid;

    protected List<SortMeta> sortBy;

    private RowData selectedRow;

    private List<RowData> selectedRows;

    private List<RowData> filteredValue;

    public DocBasedTable() {
        data = new ArrayList<RowData>();
        selectedRows = new ArrayList<RowData>();
        filteredValue = new ArrayList<RowData>();
    }

    protected abstract void createData();

    public List<RowData> getData() {
        return data;
    }

    public void setData(List<RowData> data) {
        this.data = data;
        size = data.size();
    }

    public boolean getIsFiltered(DataTable dataTable) {
        if (dataTable != null) {
            return dataTable.isFilteringCurrentlyActive();
        }
        return false;
    }

    public int getSize() {
        return size;
    }

    public boolean getHasUnreadableRows() {
        return hasUnreadableRows;
    }

    public void setHasUnreadableRows(boolean hasUnreadableRows) {
        this.hasUnreadableRows = hasUnreadableRows;
    }

    public boolean getIsInvalid() {
        return isInvalid;
    }

    public void setIsInvalid(boolean isInvalid) {
        this.isInvalid = isInvalid;
    }

    public List<SortMeta> getSortBy() {
        return sortBy;
    }

    public RowData getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(RowData selectedRow) {
        this.selectedRow = selectedRow;
    }

    public List<RowData> getSelectedRows() {
        return selectedRows;
    }

    public void setSelectedRows(List<RowData> selectedRows) {
        this.selectedRows = selectedRows;
    }

    public void deselect() {
        selectedRow = null;
        selectedRows.clear();
    }

    public List<RowData> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<RowData> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public void reload() {
        deselect();
        createData();
        // clearFilters();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        size = 0;
        hasUnreadableRows = false;
        isInvalid = false;
        // HAU BAI ? filteredValue = null;
        deselect();
        createData();
        // clearFilters();
    }

    public void clearFilters(DataTable dataTable) {
        if (dataTable != null) {
            PrimeFaces.current().executeScript(
                    "PF('" + dataTable.getWidgetVar() + "').clearFilters()");
        }
    }

    public void onRowSelect(SelectEvent event) {
        // By default, do nothing
    }

    public void onRowUnselect(UnselectEvent event) {
        // By default, do nothing
    }

    public void onToggleSelect(ToggleSelectEvent event) {
        // By default, do nothing
    }

    protected String getNewRowId() {
        int newRowId = 0;
        int maxRowId = 0;
        for (RowData r : getData()) {
            BaseRowData row = (BaseRowData) r;
            if (row.getId() != null) {
                int rowId = Integer.parseInt(row.getId());
                if (rowId > maxRowId) {
                    maxRowId = rowId;
                }
            }
        }

        newRowId = maxRowId + 1;
        return String.valueOf(newRowId);
    }

    public String getTableBeanName() {
        try {
            return (String) this.getClass().getField("NAME").get(null);
        } catch (Exception e) {
            return null;
        }
    }

}
