package com.aritu.eloraplm.datatable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.dataexporter.CsvEloraExporter;
import com.aritu.eloraplm.dataexporter.ExcelXStreamEloraExporter;
import com.aritu.eloraplm.dataexporter.PdfEloraExporter;

public abstract class CoreTableBean extends EloraDocContextBoundActionBean
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

    private boolean isDirty = false;

    private List<RowData> data;

    private int size;

    private RowData selectedRow;

    private RowData[] selectedRows;

    private CsvEloraExporter csvExporter;

    private PdfEloraExporter pdfExporter;

    private ExcelXStreamEloraExporter xlsxExporter;

    public CoreTableBean() {
        csvExporter = new CsvEloraExporter(null);
        pdfExporter = new PdfEloraExporter();
        xlsxExporter = new ExcelXStreamEloraExporter();
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

    public RowData getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(RowData selectedRow) {
        this.selectedRow = selectedRow;
    }

    public RowData[] getSelectedRows() {
        return selectedRows;
    }

    public void setSelectedRows(RowData[] selectedRows) {
        this.selectedRows = selectedRows;
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
        createData();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        createData();
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

    public boolean getIsDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    protected abstract List<RowData> getDataFromFactory();

}
