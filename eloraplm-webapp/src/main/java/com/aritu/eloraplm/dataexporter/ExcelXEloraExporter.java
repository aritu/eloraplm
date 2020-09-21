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
package com.aritu.eloraplm.dataexporter;

import java.awt.Color;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.*;
import org.primefaces.component.export.ExporterOptions;
import org.primefaces.util.ComponentUtils;

/**
 *
 * @author aritu
 *
 */
public class ExcelXEloraExporter extends ExcelEloraExporter {

    @Override
    protected Workbook createWorkBook() {
        return new XSSFWorkbook();
    }

    @Override
    protected RichTextString createRichTextString(String value) {
        return new XSSFRichTextString(value);
    }

    @Override
    protected String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    protected String getContentDisposition(String filename) {
        return ComponentUtils.createContentDisposition("attachment",
                filename + ".xlsx");
    }

    @Override
    protected void applyFacetOptions(Workbook wb, ExporterOptions options,
            CellStyle facetStyle) {
        Font facetFont = wb.createFont();
        facetFont.setFontName("Arial");

        if (options != null) {
            String facetFontStyle = options.getFacetFontStyle();
            if (facetFontStyle != null) {
                if (facetFontStyle.equalsIgnoreCase("BOLD")) {
                    facetFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
                }
                if (facetFontStyle.equalsIgnoreCase("ITALIC")) {
                    facetFont.setItalic(true);
                }
            }

            String facetBackground = options.getFacetBgColor();
            if (facetBackground != null) {
                XSSFColor backgroundColor = new XSSFColor(
                        Color.decode(facetBackground));
                ((XSSFCellStyle) facetStyle).setFillForegroundColor(
                        backgroundColor);
                facetStyle.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
            }

            String facetFontColor = options.getFacetFontColor();
            if (facetFontColor != null) {
                XSSFColor facetColor = new XSSFColor(
                        Color.decode(facetFontColor));
                ((XSSFFont) facetFont).setColor(facetColor);
            }

            String facetFontSize = options.getFacetFontSize();
            if (facetFontSize != null) {
                facetFont.setFontHeightInPoints(Short.valueOf(facetFontSize));
            }
        }

        facetStyle.setFont(facetFont);
    }

    @Override
    protected void applyCellOptions(Workbook wb, ExporterOptions options,
            CellStyle cellStyle) {
        Font cellFont = wb.createFont();
        cellFont.setFontName("Arial");

        if (options != null) {
            String cellFontColor = options.getCellFontColor();
            if (cellFontColor != null) {
                XSSFColor cellColor = new XSSFColor(
                        Color.decode(cellFontColor));
                ((XSSFFont) cellFont).setColor(cellColor);
            }

            String cellFontSize = options.getCellFontSize();
            if (cellFontSize != null) {
                cellFont.setFontHeightInPoints(Short.valueOf(cellFontSize));
            }

            String cellFontStyle = options.getCellFontStyle();
            if (cellFontStyle != null) {
                if (cellFontStyle.equalsIgnoreCase("BOLD")) {
                    cellFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
                }
                if (cellFontStyle.equalsIgnoreCase("ITALIC")) {
                    cellFont.setItalic(true);
                }
            }
        }

        cellStyle.setFont(cellFont);
    }
}
