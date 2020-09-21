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
import javax.faces.view.facelets.FaceletException;

import com.lowagie.text.*;
import org.primefaces.component.datatable.DataTable;

/**
 *
 * @author aritu
 *
 */
public class PdfEloraExportVisitCallback implements VisitCallback {

    private final PdfEloraExporter exporter;

    private final Document document;

    private final boolean pageOnly;

    private final boolean selectionOnly;

    private final String encoding;

    public PdfEloraExportVisitCallback(PdfEloraExporter exporter,
            Document document, boolean pageOnly, boolean selectionOnly,
            String encoding) {
        this.exporter = exporter;
        this.document = document;
        this.pageOnly = pageOnly;
        this.selectionOnly = selectionOnly;
        this.encoding = encoding;
    }

    @Override
    public VisitResult visit(VisitContext context, UIComponent target) {
        DataTable dt = (DataTable) target;
        try {
            document.add(exporter.exportPDFTable(context.getFacesContext(), dt,
                    pageOnly, selectionOnly, encoding));

            Paragraph preface = new Paragraph();
            exporter.addEmptyLine(preface, 3);
            document.add(preface);

        } catch (DocumentException e) {
            throw new FaceletException(e.getMessage());
        }

        return VisitResult.ACCEPT;
    }

}
