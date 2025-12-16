package com.aritu.eloraplm.datatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.SortMeta;

public abstract class AdminTable implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = true)
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

    public AdminTable() {
        data = new ArrayList<RowData>();
        selectedRows = new ArrayList<RowData>();
        filteredValue = new ArrayList<RowData>();
    }

    @Create
    public void init() {
        createData();
    }

    protected abstract void createData();

    public List<RowData> getData() {
        return data;
    }

    public void setData(List<RowData> data) {
        this.data = data;
        size = data.size();
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

    public List<RowData> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<RowData> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public boolean getIsFiltered(DataTable dataTable) {
        if (dataTable != null) {
            return dataTable.isFilteringCurrentlyActive();
        }
        return false;
    }

    public void reload() {
        selectedRow = null;
        selectedRows.clear();
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

    public String getTableBeanName() {
        try {
            return (String) this.getClass().getField("NAME").get(null);
        } catch (Exception e) {
            return null;
        }
    }

}
