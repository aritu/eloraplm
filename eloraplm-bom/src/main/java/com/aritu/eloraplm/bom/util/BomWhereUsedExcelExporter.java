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
package com.aritu.eloraplm.bom.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraMimeTypeConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;

/**
 * Bom Where is Used Excel Exporter class.
 *
 * @author aritu
 *
 */
public class BomWhereUsedExcelExporter {

    private static final Log log = LogFactory.getLog(
            BomWhereUsedExcelExporter.class);

    // Constants
    protected static final String SHEET_NAME_BOM = "Where is used";

    protected static final String TITLE_EU = "titleEu";

    protected static final String TITLE_ES = "titleEs";

    protected static final String TITLE_EN = "titleEn";

    protected static final String METADATA_TITLE = "metadataTitle";

    protected static final String METADATA_VALUE = "metadataValue";

    protected static final String COLUMN_TITLE = "columnTitle";

    protected static final String COLUMN_VALUE = "columnValue";

    protected static final String FILENAME_SUFFIX = "item_where_is_used";

    protected static final String FILENAME_EXTENSION = ".xlsx";

    protected static final int COLUMN_NAMES_ROW_NUMBER = 11;

    protected static final int TITLE_MERGE_FIRST_COLUMN = 3;

    protected static final int TITLE_MERGE_LAST_COLUMN = 9;

    protected static final int METADATA_TITLE_MERGE_FIRST_COLUMN = 3;

    protected static final int METADATA_TITLE_MERGE_LAST_COLUMN = 5;

    protected static final int METADATA_VALUE_MERGE_FIRST_COLUMN = 6;

    protected static final int METADATA_VALUE_MERGE_LAST_COLUMN = 9;

    // Global variables
    protected TreeNode whereUsedTree;

    protected Map<String, String> messages;

    protected Map<String, XSSFFont> fonts;

    protected Map<String, CellStyle> cellStyles;

    protected String filename;

    /**
     * @param fonts
     * @param messages
     */
    public BomWhereUsedExcelExporter(TreeNode whereUsedTree,
            Map<String, String> messages) {
        super();
        this.whereUsedTree = whereUsedTree;
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

    public void export(DocumentModel document, boolean insertLogo)
            throws EloraException {
        String logInitMsg = "[export] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";

        File file = null;

        try {

            // Create export file
            file = createFile(document);

            // Create Excel Workbook and Sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(SHEET_NAME_BOM);

            // Initial setup
            initWorkbook(workbook);

            // Write file header
            int rowNb = writeFileHeader(workbook, document);

            // if required, insert logo
            if (insertLogo) {
                insertLogo(workbook);
            }

            // write column names
            int columnNamesRowNb = rowNb + 2;
            int lastColumnNamesColumNb = writeExcelColumnNames(workbook,
                    columnNamesRowNb, document);

            // write WHERE IS USED data
            writeWhereUsedData(workbook, document, columnNamesRowNb,
                    lastColumnNamesColumNb);

            // adjust file
            finalSettings(sheet, columnNamesRowNb, lastColumnNamesColumNb);

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

        XSSFFont titleEuFont = ((XSSFWorkbook) workbook).createFont();
        titleEuFont.setFontName("Arial");
        titleEuFont.setFontHeightInPoints((short) 16);
        titleEuFont.setBold(true);
        fonts.put(TITLE_EU, titleEuFont);

        XSSFFont titleEsFont = ((XSSFWorkbook) workbook).createFont();
        titleEsFont.setFontName("Arial");
        titleEsFont.setFontHeightInPoints((short) 14);
        titleEsFont.setBold(true);
        fonts.put(TITLE_ES, titleEsFont);

        XSSFFont titleEnFont = ((XSSFWorkbook) workbook).createFont();
        titleEnFont.setFontName("Arial");
        titleEnFont.setFontHeightInPoints((short) 12);
        titleEnFont.setBold(true);
        fonts.put(TITLE_EN, titleEnFont);

        XSSFFont metadataTitleFont = ((XSSFWorkbook) workbook).createFont();
        metadataTitleFont.setFontName("Arial");
        metadataTitleFont.setFontHeightInPoints((short) 10);
        fonts.put(METADATA_TITLE, metadataTitleFont);

        XSSFFont metadataValueFont = ((XSSFWorkbook) workbook).createFont();
        metadataValueFont.setFontName("Arial");
        metadataValueFont.setFontHeightInPoints((short) 10);
        metadataValueFont.setBold(true);
        fonts.put(METADATA_VALUE, metadataValueFont);

        XSSFFont columnTitleFont = ((XSSFWorkbook) workbook).createFont();
        columnTitleFont.setFontName("Arial");
        columnTitleFont.setFontHeightInPoints((short) 10);
        columnTitleFont.setBold(true);
        fonts.put(COLUMN_TITLE, columnTitleFont);

        XSSFFont columnValueFont = ((XSSFWorkbook) workbook).createFont();
        columnValueFont.setFontName("Arial");
        columnValueFont.setFontHeightInPoints((short) 10);
        fonts.put(COLUMN_VALUE, columnValueFont);

        setFonts(fonts);
    }

    private void initializeCellStyles(Workbook workbook) {
        Map<String, CellStyle> cellStyles = new HashMap<String, CellStyle>();

        CellStyle cellStyleTitleEu = workbook.createCellStyle();
        cellStyleTitleEu.setFont(fonts.get(TITLE_EU));
        cellStyleTitleEu.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyleTitleEu.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyles.put(TITLE_EU, cellStyleTitleEu);

        CellStyle cellStyleTitleEs = workbook.createCellStyle();
        cellStyleTitleEs.setFont(fonts.get(TITLE_ES));
        cellStyleTitleEs.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyleTitleEs.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyles.put(TITLE_ES, cellStyleTitleEs);

        CellStyle cellStyleTitleEn = workbook.createCellStyle();
        cellStyleTitleEn.setFont(fonts.get(TITLE_EN));
        cellStyleTitleEn.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyleTitleEn.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyles.put(TITLE_EN, cellStyleTitleEn);

        CellStyle cellStyleMetadataTitle = workbook.createCellStyle();
        cellStyleMetadataTitle.setFont(fonts.get(METADATA_TITLE));
        cellStyles.put(METADATA_TITLE, cellStyleMetadataTitle);

        CellStyle cellStyleMetadataValue = workbook.createCellStyle();
        cellStyleMetadataValue.setFont(fonts.get(METADATA_VALUE));
        cellStyles.put(METADATA_VALUE, cellStyleMetadataValue);

        CellStyle cellStyleColumnTitle = workbook.createCellStyle();
        cellStyleColumnTitle.setFont(fonts.get(COLUMN_TITLE));
        cellStyleColumnTitle.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyleColumnTitle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyleColumnTitle.setBorderTop(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderRight(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderBottom(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setBorderLeft(CellStyle.BORDER_HAIR);
        cellStyleColumnTitle.setWrapText(true);
        cellStyles.put(COLUMN_TITLE, cellStyleColumnTitle);

        CellStyle cellStyleColumnValue = workbook.createCellStyle();
        cellStyleColumnValue.setFont(fonts.get(COLUMN_VALUE));
        cellStyleColumnValue.setBorderTop(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderRight(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderBottom(CellStyle.BORDER_HAIR);
        cellStyleColumnValue.setBorderLeft(CellStyle.BORDER_HAIR);
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
        String filenamePrefix = reference + "_" + FILENAME_SUFFIX;
        String filenameSuffix = FILENAME_EXTENSION;
        String filename = filenamePrefix + filenameSuffix;

        setFilename(filename);

        File file = File.createTempFile(filenamePrefix, filenameSuffix);

        return file;
    }

    protected int writeFileHeader(Workbook workbook, DocumentModel document) {
        Sheet sheet = workbook.getSheet(SHEET_NAME_BOM);

        int rowNb = 0;
        Row headerRow = sheet.createRow(rowNb);
        Cell headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("NON ERABILI DA");
        headerRowCell.setCellStyle(cellStyles.get(TITLE_EU));
        headerRow.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                TITLE_MERGE_FIRST_COLUMN, TITLE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("DÓNDE SE USA");
        headerRowCell.setCellStyle(cellStyles.get(TITLE_ES));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                TITLE_MERGE_FIRST_COLUMN, TITLE_MERGE_LAST_COLUMN));
        headerRow.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("WHERE IS USED");
        headerRowCell.setCellStyle(cellStyles.get(TITLE_EN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                TITLE_MERGE_FIRST_COLUMN, TITLE_MERGE_LAST_COLUMN));
        headerRow.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());

        rowNb = rowNb + 2;
        headerRow = sheet.createRow(rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("Erreferentzia / Referencia / Reference:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        String reference = String.valueOf(document.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE));
        headerRowCell.setCellValue(reference);
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("Izenburua / Título / Title:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        String title = String.valueOf(
                document.getPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE));
        headerRowCell.setCellValue(title);
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("Bertsioa / Revisión / Revision:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue(document.getVersionLabel());
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("Egoera / Estado / State:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue(
                messages.get(document.getCurrentLifeCycleState()));
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue("Data / Fecha / Date:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        GregorianCalendar lastModifiedDate = (GregorianCalendar) document.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        headerRowCell.setCellValue(
                formatInternationalDateTime(lastModifiedDate));
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        headerRow = sheet.createRow(++rowNb);
        headerRowCell = headerRow.createCell(TITLE_MERGE_FIRST_COLUMN);
        headerRowCell.setCellValue(
                "Azken kolaboratzailea / Último colaborador / Last contributor:");
        headerRowCell.setCellStyle(cellStyles.get(METADATA_TITLE));
        headerRowCell = headerRow.createCell(METADATA_VALUE_MERGE_FIRST_COLUMN);
        String lastModifier = String.valueOf(document.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR));
        UserNameResolverHelper unr = new UserNameResolverHelper();
        String lastModifierFullName = unr.getUserFullName(lastModifier);
        headerRowCell.setCellValue(lastModifierFullName);
        headerRowCell.setCellStyle(cellStyles.get(METADATA_VALUE));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_TITLE_MERGE_FIRST_COLUMN,
                METADATA_TITLE_MERGE_LAST_COLUMN));
        sheet.addMergedRegion(new CellRangeAddress(rowNb, rowNb,
                METADATA_VALUE_MERGE_FIRST_COLUMN,
                METADATA_VALUE_MERGE_LAST_COLUMN));

        return rowNb;
    }

    protected int writeExcelColumnNames(Workbook workbook, int rowNb,
            DocumentModel document) {
        Sheet sheet = workbook.getSheet(SHEET_NAME_BOM);

        int columnNb = -1;

        Row columnNamesRow = sheet.createRow(rowNb);
        columnNamesRow.setHeightInPoints(
                3 * sheet.getDefaultRowHeightInPoints());

        Cell columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue("Maila\nNivel\nLevel");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue(
                "Seme erreferentzia\nReferencia hijo\nChild Reference");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue("Erreferentzia\nReferencia\nReference");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue(
                "Bertsioa EloraPLMn\nVersión en EloraPLM\nVersion in EloraPLM");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue(
                "Kanpoko bertsioa\nVersión externa\nExternal Version");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue("Egoera\nEstado\nState");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue("Izenburua\nTítulo\nTitle");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        columnNamesRowCell = columnNamesRow.createCell(++columnNb);
        columnNamesRowCell.setCellValue(
                "Sailkapena\nClasificación\nClassification");
        columnNamesRowCell.setCellStyle(cellStyles.get(COLUMN_TITLE));

        return columnNb;
    }

    // TODO LEIRE: hau klase orokorrago batetara eraman??
    private String formatInternationalDateTime(GregorianCalendar calendar) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fmt.setCalendar(calendar);
        String formattedDate = fmt.format(calendar.getTime());
        return formattedDate;
    }

    protected void writeWhereUsedData(Workbook workbook,
            DocumentModel currentDoc, int columnNamesRowNb,
            int lastColumnNbForColumnNames) throws EloraException {

        writeNodeBomInfo(workbook, whereUsedTree, 1, "", columnNamesRowNb);

    }

    private void writeNodeBomInfo(Workbook workbook, TreeNode node, int level,
            String parentReference, int columnNamesRowNb)
            throws EloraException {

        for (TreeNode childNode : node.getChildren()) {
            RelationNodeData childNodeData = (RelationNodeData) childNode.getData();
            DocumentModel childNodeDocument = childNodeData.getData();

            writeBomInfoLine(workbook, level, parentReference, childNodeData,
                    columnNamesRowNb);

            String childNodeReference = String.valueOf(
                    childNodeDocument.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE));
            writeNodeBomInfo(workbook, childNode, level + 1, childNodeReference,
                    columnNamesRowNb);
        }
    }

    protected void writeBomInfoLine(Workbook workbook, int level,
            String parentReference, RelationNodeData nodeData,
            int columnNamesRowNb) throws EloraException {

        Sheet sheet = workbook.getSheet(SHEET_NAME_BOM);

        int currentRow = sheet.getLastRowNum() + 1;

        int columnNb = -1;

        DocumentModel nodeDocument = nodeData.getData();

        Row bomInfoRow = sheet.createRow(currentRow);
        Cell bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        bomInfoRowCell.setCellValue(level);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        bomInfoRowCell.setCellValue(parentReference);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        String reference = String.valueOf(nodeDocument.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE));
        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        bomInfoRowCell.setCellValue(reference);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        bomInfoRowCell.setCellValue(nodeDocument.getVersionLabel());
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        String externalVersion = "";
        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        if (nodeDocument.getPropertyValue(
                EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION) != null) {
            externalVersion = String.valueOf(nodeDocument.getPropertyValue(
                    EloraMetadataConstants.ELORA_BOMITEM_EXTERNAL_VERSION));
        }
        bomInfoRowCell.setCellValue(externalVersion);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        bomInfoRowCell.setCellValue(
                messages.get(nodeDocument.getCurrentLifeCycleState()));
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        String title = String.valueOf(nodeDocument.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE));
        bomInfoRowCell.setCellValue(title);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));

        bomInfoRowCell = bomInfoRow.createCell(++columnNb);
        String classification = BomHelper.getBomClassificationLabel(
                nodeDocument);
        bomInfoRowCell.setCellValue(classification);
        bomInfoRowCell.setCellStyle(cellStyles.get(COLUMN_VALUE));
    }

    protected void insertLogo(Workbook workbook) {
        String logInitMsg = "[insertLogo] ";

        try {
            String companyLogo = Framework.getProperty(
                    EloraPropertiesConstants.PROP_COMPANY_LOGO);
            if (companyLogo != null && companyLogo.length() > 0) {
                InputStream stream = this.getClass().getClassLoader().getResource(
                        "nuxeo.war/img/" + companyLogo).openStream();
                CreationHelper helper = workbook.getCreationHelper();
                Sheet sheet = workbook.getSheet(SHEET_NAME_BOM);
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setAnchorType(ClientAnchor.MOVE_DONT_RESIZE);
                int pictureIndex = workbook.addPicture(
                        IOUtils.toByteArray(stream), Workbook.PICTURE_TYPE_PNG);
                anchor.setCol1(3);
                anchor.setRow1(0);
                Picture pict = drawing.createPicture(anchor, pictureIndex);
                pict.resize(0.5);
            } else {
                log.error(logInitMsg
                        + "Company Logo is not defined. It should be defined in "
                        + EloraPropertiesConstants.PROP_COMPANY_LOGO
                        + " property.");
            }
        } catch (IOException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }
    }

    protected void downloadFile(Blob blob) throws EloraException {
        if (blob == null) {
            throw new EloraException(
                    "Cannot download file. Export file is null.");
        }

        ComponentUtils.download(null, null, blob, filename, "operation");
    }

}
