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

import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.primefaces.component.datatable.DataTable;

/**
 *
 * @author aritu
 *
 */
public class ExcelEloraExportVisitCallback implements VisitCallback {

    private final ExcelEloraExporter exporter;

    private final boolean pageOnly;

    private final boolean selectionOnly;

    private final Workbook workbook;

    public ExcelEloraExportVisitCallback(ExcelEloraExporter exporter,
            Workbook workbook, boolean pageOnly, boolean selectionOnly) {
        this.exporter = exporter;
        this.pageOnly = pageOnly;
        this.selectionOnly = selectionOnly;
        this.workbook = workbook;
    }

    @Override
    public VisitResult visit(VisitContext context, UIComponent target) {
        DataTable dt = (DataTable) target;
        FacesContext facesContext = context.getFacesContext();
        String sheetName = exporter.getSheetName(facesContext, dt);
        if (sheetName == null) {
            sheetName = dt.getClientId().replaceAll(":", "_");
        }

        Sheet sheet = workbook.createSheet(sheetName);
        exporter.exportTable(facesContext, dt, sheet, pageOnly, selectionOnly);
        return VisitResult.ACCEPT;
    }

}
