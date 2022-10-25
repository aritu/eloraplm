package com.aritu.eloraplm.datatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
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

import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.dataexporter.CsvEloraExporter;
import com.aritu.eloraplm.dataexporter.ExcelXStreamEloraExporter;
import com.aritu.eloraplm.dataexporter.PdfEloraExporter;

public abstract class DocBasedTableBean extends EloraDocContextBoundActionBean
        implements Serializable {

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

    private DataTable dataTable;

    private int size;

    private RowData selectedRow;

    private List<RowData> selectedRows;

    private List<RowData> filteredRows;

    private CsvEloraExporter csvExporter;

    private PdfEloraExporter pdfExporter;

    private ExcelXStreamEloraExporter xlsxExporter;

    public DocBasedTableBean() {
        csvExporter = new CsvEloraExporter(null);
        pdfExporter = new PdfEloraExporter();
        xlsxExporter = new ExcelXStreamEloraExporter();
        data = new ArrayList<RowData>();
        selectedRows = new ArrayList<RowData>();
        filteredRows = new ArrayList<RowData>();
    }

    protected abstract void createData();

    public List<RowData> getData() {
        return data;
    }

    public void setData(List<RowData> data) {
        this.data = data;
        size = data.size();
    }

    public DataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public int getSize() {
        return size;
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

    public List<RowData> getFilteredRows() {
        return filteredRows;
    }

    public void setFilteredRows(List<RowData> filteredRows) {
        this.filteredRows = filteredRows;
    }

    public CsvEloraExporter getCsvExporter() {
        return csvExporter;
    }

    public void setCsvExporter(CsvEloraExporter csvExporter) {
        this.csvExporter = csvExporter;
    }

    public PdfEloraExporter getPdfExporter() {
        return pdfExporter;
    }

    public void setPdfExporter(PdfEloraExporter pdfExporter) {
        this.pdfExporter = pdfExporter;
    }

    public ExcelXStreamEloraExporter getXlsxExporter() {
        return xlsxExporter;
    }

    public void setXlsxExporter(ExcelXStreamEloraExporter xlsxExporter) {
        this.xlsxExporter = xlsxExporter;
    }

    public void reload() {
        selectedRow = null;
        selectedRows.clear();
        createData();
        clearFilters();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        selectedRow = null;
        selectedRows.clear();
        createData();
        clearFilters();
    }

    protected void clearFilters() {
        if (dataTable != null) {
            PrimeFaces.current().executeScript(
                    "PF('" + dataTable.getWidgetVar() + "').clearFilters()");
            if (!dataTable.getFilters().isEmpty()) {
                dataTable.reset();
            }
        }
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

    public void onRowSelect(SelectEvent event) {
        // By default, do nothing
    }

    public void onRowUnselect(UnselectEvent event) {
        // By default, do nothing
    }

    public void onToggleSelect(ToggleSelectEvent event) {
        // By default, do nothing
    }

    protected abstract List<RowData> getDataFromFactory();

}
