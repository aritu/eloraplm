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
package com.aritu.eloraplm.relations.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;

/**
 *
 *
 * @author aritu
 *
 */
public abstract class CadCompositionAbstractExcelExporter {

    private static final Log log = LogFactory.getLog(
            CadCompositionAbstractExcelExporter.class);

    // Constants
    protected static final String SHEET_NAME_CAD_COMPOSITION = "Cad Composition";

    protected static final String COLUMN_TITLE = "columnTitle";

    protected static final String COLUMN_VALUE = "columnValue";

    protected static final String FILENAME_EXTENSION = ".xlsx";

    protected static final String BOOLEAN_YES = "SI";

    protected static final String BOOLEAN_NO = "NO";

    protected static final String contentType = "application/vnd.ms-excel";

    protected static final String[] columnNames = { "Level", "Order", "Parent",
            "Ref", "Title", "Version", "State", "Quantity" };

    // Global variables
    protected TreeNode compositionTree;

    protected Map<String, String> messages;

    protected Map<String, XSSFFont> fonts;

    protected Map<String, CellStyle> cellStyles;

    protected String filename;

    /**
     * @param fonts
     * @param messages
     */
    public CadCompositionAbstractExcelExporter(TreeNode compositionTree,
            Map<String, String> messages) {
        super();
        this.compositionTree = compositionTree;
        this.messages = messages;
    }

    // Setter methods
    protected void setFonts(Map<String, XSSFFont> fonts) {
        this.fonts = fonts;
    }

    protected void setCellStyles(Map<String, CellStyle> cellStyles) {
        this.cellStyles = cellStyles;
    }

    protected void setFilename(String filename) {
        this.filename = filename;
    }

    public void export(DocumentModel document, boolean hasAdditionalColumns)
            throws EloraException {
        String logInitMsg = "[export] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";

        File file = null;

        try {

            // Create export file
            file = createFile(document);

            // Create Excel Workbook and Sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(SHEET_NAME_CAD_COMPOSITION);

            // Initial setup
            initWorkbook(workbook);

            int rowNb = 0;
            // write column names
            int lastColumnNamesColumNb = writeExcelColumnNames(workbook, rowNb,
                    document);

            if (hasAdditionalColumns) {
                lastColumnNamesColumNb = writeAdditionalExcelColumnNames(
                        workbook, rowNb, lastColumnNamesColumNb);
            }

            rowNb++;

            // write CAD Structure data
            writeCadStructureData(workbook, rowNb, document,
                    hasAdditionalColumns);

            // adjust file
            finalSettings(sheet, 0, columnNames.length);

            OutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();

            // Create Blob
            Blob blob = Blobs.createBlob(file, contentType);

            // Download Blob
            downloadFile(blob);

            file.deleteOnExit();

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException("Error exporting CAD Composition.");
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
        columnTitleFont.setFontName("Calibri");
        columnTitleFont.setFontHeightInPoints((short) 11);
        fonts.put(COLUMN_TITLE, columnTitleFont);

        XSSFFont columnValueFont = ((XSSFWorkbook) workbook).createFont();
        columnValueFont.setFontName("Calibri");
        columnValueFont.setFontHeightInPoints((short) 11);
        fonts.put(COLUMN_VALUE, columnValueFont);

        setFonts(fonts);
    }

    private void initializeCellStyles(Workbook workbook) {
        Map<String, CellStyle> cellStyles = new HashMap<String, CellStyle>();

        CellStyle cellStyleColumnTitle = workbook.createCellStyle();
        cellStyleColumnTitle.setFont(fonts.get(COLUMN_TITLE));

        cellStyleColumnTitle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex());
        cellStyleColumnTitle.setFillPattern(CellStyle.SOLID_FOREGROUND);

        cellStyleColumnTitle.setWrapText(true);
        cellStyles.put(COLUMN_TITLE, cellStyleColumnTitle);

        CellStyle cellStyleColumnValue = workbook.createCellStyle();
        cellStyleColumnValue.setFont(fonts.get(COLUMN_VALUE));
        cellStyleColumnValue.setWrapText(true);
        cellStyles.put(COLUMN_VALUE, cellStyleColumnValue);

        setCellStyles(cellStyles);
    }

    protected void finalSettings(Sheet sheet, int columnNamesRowNb,
            int lastColumnNamesColumNb) {

        // Auto size all the columns
        for (int x = 0; x < lastColumnNamesColumNb + 1; x++) {
            sheet.autoSizeColumn(x);
        }

        // Create and fill with an empty String null or empty cells
        for (int x = columnNamesRowNb + 1; x < sheet.getLastRowNum() + 1; x++) {
            Row row = sheet.getRow(x);
            for (int y = 0; y < lastColumnNamesColumNb + 1; y++) {
                Cell cell = row.getCell(y);
                if (cell == null
                        || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    cell = row.createCell(y);
                    cell.setCellValue("");
                    cell.setCellStyle(cellStyles.get(COLUMN_VALUE));
                }
            }
        }
    }

    protected File createFile(DocumentModel document) throws IOException {

        String reference = document.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : document.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        String filenamePrefix = reference;
        String filenameSuffix = FILENAME_EXTENSION;
        String filename = reference + FILENAME_EXTENSION;

        setFilename(filename);

        File file = File.createTempFile(filenamePrefix, filenameSuffix);

        return file;
    }

    protected int writeExcelColumnNames(Workbook workbook, int rowNb,
            DocumentModel document) {
        Sheet sheet = workbook.getSheet(SHEET_NAME_CAD_COMPOSITION);

        int columnNb = -1;

        Row columnNamesRow = sheet.createRow(rowNb);

        for (String columnName : columnNames) {
            Cell columnNamesRowCell = columnNamesRow.createCell(++columnNb);
            columnNamesRowCell.setCellValue(columnName);
            columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));
        }

        return columnNb;

    }

    protected void writeCadStructureData(Workbook workbook, int rowNb,
            DocumentModel document, boolean hasAdditionalColumns)
            throws EloraException {

        int level = 0;

        // First, write the document itself.
        writeCadInfoLine(workbook, level, "", document, 0, 1,
                hasAdditionalColumns);

        level++;

        String reference = String.valueOf(document.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE));

        // Then, write the compositionTree content
        writeNodeCadInfo(workbook, compositionTree, level, reference,
                hasAdditionalColumns);
    }

    private void writeNodeCadInfo(Workbook workbook, TreeNode parentNode,
            int level, String parentReference, boolean hasAdditionalColumns)
            throws EloraException {

        for (TreeNode childNode : parentNode.getChildren()) {
            RelationNodeData childNodeData = (RelationNodeData) childNode.getData();
            DocumentModel childNodeDocument = childNodeData.getData();

            float quantity = 0.0f;
            if (childNodeData.getQuantity() != null) {
                try {
                    quantity = Float.parseFloat(childNodeData.getQuantity());
                } catch (NumberFormatException e) {
                    quantity = 0.0f;
                }
            }
            writeCadInfoLine(workbook, level, parentReference,
                    childNodeData.getData(), childNodeData.getOrdering(),
                    quantity, hasAdditionalColumns);

            String childNodeReference = String.valueOf(
                    childNodeDocument.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE));

            writeNodeCadInfo(workbook, childNode, level + 1, childNodeReference,
                    hasAdditionalColumns);
        }
    }

    private void writeCadInfoLine(Workbook workbook, int level,
            String parentReference, DocumentModel document, Integer ordering,
            float quantity, boolean hasAdditionalColumns) {

        Sheet sheet = workbook.getSheet(SHEET_NAME_CAD_COMPOSITION);

        int currentRow = sheet.getLastRowNum() + 1;

        int columnNb = -1;

        // Level
        Row compositionInfoRow = sheet.createRow(currentRow);
        Cell cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        cadInfoRowCell.setCellValue(level);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Ordering
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        if (ordering == null) {
            cadInfoRowCell.setCellValue("");
        } else {
            cadInfoRowCell.setCellValue(ordering);
        }
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Parent reference
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        cadInfoRowCell.setCellValue(parentReference);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Current document reference
        String reference = String.valueOf(document.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE));
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        cadInfoRowCell.setCellValue(reference);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Title
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        String title = String.valueOf(
                document.getPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE));
        cadInfoRowCell.setCellValue(title);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Major version
        // cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        // VersionLabelService versionLabelService = Framework.getService(
        // VersionLabelService.class);
        // String version = versionLabelService.getMajor(document);
        // String paddedVersion = String.format("%02d",
        // Integer.parseInt(version));
        // cadInfoRowCell.setCellValue(paddedVersion);
        // cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        cadInfoRowCell.setCellValue(document.getVersionLabel());
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // State
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        String state = document.getCurrentLifeCycleState();
        cadInfoRowCell.setCellValue(state);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        // Quantity
        cadInfoRowCell = compositionInfoRow.createCell(++columnNb);
        cadInfoRowCell.setCellValue(quantity);
        cadInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        if (hasAdditionalColumns) {
            columnNb = writeAdditionalExcelColumValues(workbook, currentRow,
                    columnNb, document);
        }
    }

    // Abstract methods
    protected abstract int writeAdditionalExcelColumnNames(Workbook workbook,
            int rowNb, int lastColumnNamesColumNb);

    protected abstract int writeAdditionalExcelColumValues(Workbook workbook,
            int rowNb, int lastColumnNamesColumNb,
            DocumentModel currentDocworkbook);

    private void downloadFile(Blob blob) throws EloraException {
        if (blob == null) {
            throw new EloraException(
                    "Cannot download file. Export file is null.");
        }

        ComponentUtils.download(null, null, blob, filename, "operation");
    }

}
