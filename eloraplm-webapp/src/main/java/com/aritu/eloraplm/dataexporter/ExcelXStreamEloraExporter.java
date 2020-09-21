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

import java.io.IOException;

import javax.faces.context.ExternalContext;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
*
* @author aritu
*
*/
/**
 * Different implementation of ExcelXExporter using the POI streaming API:
 *
 * SXSSF (package: org.apache.poi.xssf.streaming) is an API-compatible streaming
 * extension of XSSF to be used when very large spreadsheets have to be
 * produced, and heap space is limited. SXSSF achieves its low memory footprint
 * by limiting access to the rows that are within a sliding window.
 */
public class ExcelXStreamEloraExporter extends ExcelXEloraExporter {

    @Override
    protected Workbook createWorkBook() {
        SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(100);
        sxssfWorkbook.setCompressTempFiles(true);
        return sxssfWorkbook;
    }

    @Override
    protected void writeExcelToResponse(ExternalContext externalContext,
            Workbook generatedExcel, String filename) throws IOException {
        super.writeExcelToResponse(externalContext, generatedExcel, filename);
        ((SXSSFWorkbook) generatedExcel).dispose();
    }

    @Override
    protected Sheet createSheet(Workbook wb, String sheetName) {
        SXSSFWorkbook workbook = (SXSSFWorkbook) wb;
        SXSSFSheet sheet = (SXSSFSheet) workbook.createSheet(sheetName);
        // Does not exist in our POI version
        // sheet.trackAllColumnsForAutoSizing();
        return sheet;
    }
}
