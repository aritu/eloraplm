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
package com.aritu.eloraplm.core.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import com.aritu.eloraplm.constants.EloraMimeTypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Xlsx Exporter class.
 *
 *
 * @author aritu
 *
 */
public class XlsxExporter {

    private static final Log log = LogFactory.getLog(XlsxExporter.class);

    // Class constants
    protected static final String STYLE_COLUMN_TITLE = "columnTitleStyle";

    protected static final String STYLE_COLUMN_VALUE = "columnValueStyle";

    protected static final String FILE_EXTENSION = ".xlsx";

    // Global variables
    protected Map<String, XSSFFont> fonts;

    protected Map<String, CellStyle> cellStyles;

    // protected String filename;

    // The file name without the extension
    protected String filenamePrefix;

    protected String sheetName;

    protected List<String> columnNames;

    protected List<List<String>> columnValues;

    // Constructor
    /**
     * @param fonts
     * @param messages
     */
    public XlsxExporter(String filenamePrefix, String sheetName,
            List<String> columnNames, List<List<String>> columnValues) {
        super();
        this.filenamePrefix = filenamePrefix;
        this.sheetName = sheetName;
        this.columnNames = columnNames;
        this.columnValues = columnValues;
    }

    // Getters and setters
    public Map<String, XSSFFont> getFonts() {
        return fonts;
    }

    public void setFonts(Map<String, XSSFFont> fonts) {
        this.fonts = fonts;
    }

    public Map<String, CellStyle> getCellStyles() {
        return cellStyles;
    }

    public void setCellStyles(Map<String, CellStyle> cellStyles) {
        this.cellStyles = cellStyles;
    }

    /* public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }*/

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public void setFilenamePrefix(String filenamePrefix) {
        this.filenamePrefix = filenamePrefix;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<List<String>> getColumnValues() {
        return columnValues;
    }

    public void setColumnValues(List<List<String>> columnValues) {
        this.columnValues = columnValues;
    }

    public void export() throws EloraException {
        String logInitMsg = "[export] ";

        File file = null;

        try {

            // Create export file
            file = File.createTempFile(filenamePrefix, FILE_EXTENSION);

            // Create Excel Workbook and Sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);

            // Initial setup
            initWorkbook(workbook);

            int rowNb = 0;

            // Write column names
            writeExcelColumnNames(workbook, rowNb);

            rowNb++;

            // Write column values
            writeColumnValues(workbook, rowNb);

            // adjust file
            finalSettings(sheet);

            OutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();

            // Create Blob
            Blob blob = Blobs.createBlob(file,
                    EloraMimeTypeConstants.APPLICATION_EXCEL);

            // Download Blob
            downloadFile(blob);

            file.deleteOnExit();

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException("Error exporting BOM Composition.");
        }
    }

    protected void initWorkbook(Workbook workbook) {
        // Initialize fonts and cell styles
        initializeFonts(workbook);
        initializeCellStyles(workbook);
    }

    private void initializeFonts(Workbook workbook) {
        Map<String, XSSFFont> fonts = new HashMap<String, XSSFFont>();

        XSSFFont columnTitleFont = ((XSSFWorkbook) workbook).createFont();
        columnTitleFont.setFontName("Arial");
        columnTitleFont.setFontHeightInPoints((short) 10);
        columnTitleFont.setBold(true);
        fonts.put(STYLE_COLUMN_TITLE, columnTitleFont);

        XSSFFont columnValueFont = ((XSSFWorkbook) workbook).createFont();
        columnValueFont.setFontName("Arial");
        columnValueFont.setFontHeightInPoints((short) 10);
        fonts.put(STYLE_COLUMN_VALUE, columnValueFont);

        setFonts(fonts);
    }

    private void initializeCellStyles(Workbook workbook) {
        Map<String, CellStyle> cellStyles = new HashMap<String, CellStyle>();

        CellStyle cellStyleColumnTitle = workbook.createCellStyle();
        cellStyleColumnTitle.setFont(fonts.get(STYLE_COLUMN_TITLE));
        cellStyleColumnTitle.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyleColumnTitle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        // Posible izango litzateke, estiloa parametro bezala pasatzea edota
        // setter bidez definitzea.
        /*cellStyleColumnTitle.setBorderTop(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderRight(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderBottom(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderLeft(CellStyle.BORDER_HAIR);*/
        cellStyleColumnTitle.setWrapText(true);
        cellStyles.put(STYLE_COLUMN_TITLE, cellStyleColumnTitle);

        CellStyle cellStyleColumnValue = workbook.createCellStyle();
        cellStyleColumnValue.setFont(fonts.get(STYLE_COLUMN_VALUE));
        // Posible izango litzateke, estiloa parametro bezala pasatzea edota
        // setter bidez definitzea.
        /*cellStyleColumnValue.setBorderTop(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderRight(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderBottom(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderLeft(CellStyle.BORDER_HAIR);*/
        cellStyleColumnValue.setWrapText(true);
        cellStyles.put(STYLE_COLUMN_VALUE, cellStyleColumnValue);

        setCellStyles(cellStyles);
    }

    protected void finalSettings(Sheet sheet) {

        int columnsNb = columnNames.size();

        // Auto size all the columns
        for (int x = 0; x < columnsNb + 1; x++) {
            sheet.autoSizeColumn(x);
        }

        /*
        // Create and fill with an empty String null or empty cells
        for (int x = columnNamesRowNb + 1; x < sheet.getLastRowNum() + 1; x++) {
            Row row = sheet.getRow(x);
            for (int y = 0; y < columnsNb + 1; y++) {
                Cell cell = row.getCell(y);
                if (cell == null
                        || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    cell = row.createCell(y);
                    cell.setCellValue("");
                    cell.setCellStyle(cellStyles.get(STYLE_COLUMN_VALUE));
                }
            }
        }*/
    }

    /*protected File createFile() throws IOException {
    
        // String filename = filenameSuffix + FILE_EXTENSION;
    
        // setFilename(filename);
    
        File file = File.createTempFile(filenameSuffix, FILE_EXTENSION);
    
        return file;
    }*/

    protected void writeExcelColumnNames(Workbook workbook, int rowNb) {

        Sheet sheet = workbook.getSheet(sheetName);

        int columnNb = -1;

        Row columnNamesRow = sheet.createRow(rowNb);

        for (String columnName : columnNames) {
            Cell columnNamesRowCell = columnNamesRow.createCell(++columnNb);
            columnNamesRowCell.setCellValue(columnName);
            columnNamesRowCell.setCellStyle(cellStyles.get(STYLE_COLUMN_TITLE));
        }
    }

    private void writeColumnValues(Workbook workbook, int rowNb)
            throws EloraException {

        Sheet sheet = workbook.getSheet(sheetName);

        for (List<String> columnValueLine : columnValues) {

            int columnNb = -1;
            Row lineRow = sheet.createRow(rowNb);

            for (String columnValue : columnValueLine) {
                Cell columnNamesRowCell = lineRow.createCell(++columnNb);
                columnNamesRowCell.setCellValue(columnValue);
                columnNamesRowCell.setCellStyle(
                        cellStyles.get(STYLE_COLUMN_VALUE));
            }
            rowNb++;
        }

    }

    protected void downloadFile(Blob blob) throws EloraException {
        if (blob == null) {
            throw new EloraException(
                    "Cannot download file. Export file is null.");
        }

        String filename = filenamePrefix + FILE_EXTENSION;

        ComponentUtils.download(null, null, blob, filename, "operation");
    }

}
