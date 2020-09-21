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

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIPanel;
import javax.faces.component.UISelectMany;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.faces.facelets.tag.ui.ComponentRef;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.ExporterOptions;
import org.primefaces.component.overlaypanel.OverlayPanel;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.Constants;

/**
 *
 * @author aritu
 *
 */
public class PdfEloraExporter extends EloraExporter {

    private Font cellFont;

    private Font facetFont;

    private Color facetBgColor;

    private ExporterOptions expOptions;

    private MethodExpression onTableRender;

    private static final Log log = LogFactory.getLog(PdfEloraExporter.class);

    @Override
    public void export(FacesContext context, DataTable table, String filename,
            boolean pageOnly, boolean selectionOnly, String encodingType,
            MethodExpression preProcessor, MethodExpression postProcessor,
            ExporterOptions options, MethodExpression onTableRender)
            throws IOException {

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            this.onTableRender = onTableRender;

            if (preProcessor != null) {
                preProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            if (!document.isOpen()) {
                document.open();
            }

            if (options != null) {
                expOptions = options;
            }

            document.add(exportPDFTable(context, table, pageOnly, selectionOnly,
                    encodingType));

            if (postProcessor != null) {
                postProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            document.close();

            writePDFToResponse(context.getExternalContext(), baos, filename);

        } catch (DocumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void export(FacesContext context, List<String> clientIds,
            String outputFileName, boolean pageOnly, boolean selectionOnly,
            String encodingType, MethodExpression preProcessor,
            MethodExpression postProcessor, ExporterOptions options,
            MethodExpression onTableRender) throws IOException {

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            this.onTableRender = onTableRender;

            if (preProcessor != null) {
                preProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            if (!document.isOpen()) {
                document.open();
            }

            if (options != null) {
                expOptions = options;
            }

            VisitContext visitContext = VisitContext.createVisitContext(context,
                    clientIds, null);
            VisitCallback visitCallback = new PdfEloraExportVisitCallback(this,
                    document, pageOnly, selectionOnly, encodingType);
            context.getViewRoot().visitTree(visitContext, visitCallback);

            if (postProcessor != null) {
                postProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            document.close();

            writePDFToResponse(context.getExternalContext(), baos,
                    outputFileName);

        } catch (DocumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void export(FacesContext context, String outputFileName,
            List<DataTable> tables, boolean pageOnly, boolean selectionOnly,
            String encodingType, MethodExpression preProcessor,
            MethodExpression postProcessor, ExporterOptions options,
            MethodExpression onTableRender) throws IOException {

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            this.onTableRender = onTableRender;

            if (preProcessor != null) {
                preProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            if (!document.isOpen()) {
                document.open();
            }

            if (options != null) {
                expOptions = options;
            }

            for (DataTable table : tables) {
                document.add(exportPDFTable(context, table, pageOnly,
                        selectionOnly, encodingType));

                Paragraph preface = new Paragraph();
                addEmptyLine(preface, 3);
                document.add(preface);
            }

            if (postProcessor != null) {
                postProcessor.invoke(context.getELContext(),
                        new Object[] { document });
            }

            document.close();

            writePDFToResponse(context.getExternalContext(), baos,
                    outputFileName);

        } catch (DocumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected PdfPTable exportPDFTable(FacesContext context, DataTable table,
            boolean pageOnly, boolean selectionOnly, String encoding) {
        int columnsCount = getColumnsCount(table);
        PdfPTable pdfTable = new PdfPTable(columnsCount);
        cellFont = FontFactory.getFont(FontFactory.TIMES, encoding);
        facetFont = FontFactory.getFont(FontFactory.TIMES, encoding,
                Font.DEFAULTSIZE, Font.BOLD);

        if (onTableRender != null) {
            onTableRender.invoke(context.getELContext(),
                    new Object[] { pdfTable, table });
        }

        if (expOptions != null) {
            applyFacetOptions(expOptions);
            applyCellOptions(expOptions);
        }

        addTableFacets(context, table, pdfTable, "header");

        addColumnFacets(table, pdfTable, ColumnType.HEADER);

        if (pageOnly) {
            exportPageOnly(context, table, pdfTable);
        } else if (selectionOnly) {
            exportSelectionOnly(context, table, pdfTable);
        } else {
            exportAll(context, table, pdfTable);
        }

        if (table.hasFooterColumn()) {
            addColumnFacets(table, pdfTable, ColumnType.FOOTER);
        }

        addTableFacets(context, table, pdfTable, "footer");

        table.setRowIndex(-1);

        return pdfTable;
    }

    protected void addTableFacets(FacesContext context, DataTable table,
            PdfPTable pdfTable, String facetType) {
        String facetText = null;
        UIComponent facet = table.getFacet(facetType);
        if (facet != null) {
            if (facet instanceof UIPanel) {
                for (UIComponent child : facet.getChildren()) {
                    if (child.isRendered()) {
                        String value = ComponentUtils.getValueToRender(context,
                                child);

                        if (value != null) {
                            facetText = value;
                            break;
                        }
                    }
                }
            } else {
                facetText = ComponentUtils.getValueToRender(context, facet);
            }
        }

        if (facetText != null) {
            int colspan = 0;

            for (UIColumn col : table.getColumns()) {
                if (col.isRendered() && col.isExportable()) {
                    colspan++;
                }
            }

            PdfPCell cell = new PdfPCell(new Paragraph(facetText, facetFont));
            if (facetBgColor != null) {
                cell.setBackgroundColor(facetBgColor);
            }

            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setColspan(colspan);
            pdfTable.addCell(cell);
        }
    }

    @Override
    protected void exportCells(DataTable table, Object document) {
        PdfPTable pdfTable = (PdfPTable) document;
        for (UIColumn col : table.getColumns()) {
            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyStatelessModel();
            }

            if (col.isRendered() && col.isExportable()) {
                addColumnValue(pdfTable, col.getChildren(), cellFont, col);
            }
        }
    }

    protected void addColumnFacets(DataTable table, PdfPTable pdfTable,
            ColumnType columnType) {
        for (UIColumn col : table.getColumns()) {
            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyStatelessModel();
            }

            if (col.isRendered() && col.isExportable()) {
                UIComponent facet = col.getFacet(columnType.facet());
                String textValue;
                switch (columnType) {
                case HEADER:
                    textValue = (col.getExportHeaderValue() != null)
                            ? col.getExportHeaderValue()
                            : col.getHeaderText();
                    break;

                case FOOTER:
                    textValue = (col.getExportFooterValue() != null)
                            ? col.getExportFooterValue()
                            : col.getFooterText();
                    break;

                default:
                    textValue = null;
                    break;
                }

                if (textValue != null) {
                    addColumnValue(pdfTable, textValue);
                } else if (facet != null) {
                    addColumnValue(pdfTable, facet);
                } else {
                    addColumnValue(pdfTable, "");
                }
            }
        }
    }

    protected void addColumnValue(PdfPTable pdfTable, UIComponent component) {
        String value = component == null ? ""
                : exportValue(FacesContext.getCurrentInstance(), component);
        addColumnValue(pdfTable, value);
    }

    protected void addColumnValue(PdfPTable pdfTable, String value) {
        PdfPCell cell = new PdfPCell(new Paragraph(value, facetFont));
        if (facetBgColor != null) {
            cell.setBackgroundColor(facetBgColor);
        }

        pdfTable.addCell(cell);
    }

    protected void addColumnValue(PdfPTable pdfTable,
            List<UIComponent> components, Font font, UIColumn column) {
        FacesContext context = FacesContext.getCurrentInstance();

        if (column.getExportFunction() != null) {
            pdfTable.addCell(new Paragraph(
                    exportColumnByFunction(context, column), font));
        } else {
            StringBuilder builder = new StringBuilder();
            for (UIComponent component : components) {
                if (component.isRendered()) {
                    String value = exportValue(context, component);

                    if (value != null) {
                        builder.append(value);
                    }
                }
            }

            pdfTable.addCell(new Paragraph(builder.toString(), font));
        }
    }

    protected void writePDFToResponse(ExternalContext externalContext,
            ByteArrayOutputStream baos, String fileName)
            throws IOException, DocumentException {

        externalContext.setResponseContentType("application/pdf");
        externalContext.setResponseHeader("Expires", "0");
        externalContext.setResponseHeader("Cache-Control",
                "must-revalidate, post-check=0, pre-check=0");
        externalContext.setResponseHeader("Pragma", "public");
        externalContext.setResponseHeader("Content-disposition",
                ComponentUtils.createContentDisposition("attachment",
                        fileName + ".pdf"));
        externalContext.setResponseContentLength(baos.size());
        externalContext.addResponseCookie(Constants.DOWNLOAD_COOKIE, "true",
                Collections.<String, Object> emptyMap());
        OutputStream out = externalContext.getResponseOutputStream();
        baos.writeTo(out);
        externalContext.responseFlushBuffer();
    }

    protected int getColumnsCount(DataTable table) {
        int count = 0;

        for (UIColumn col : table.getColumns()) {
            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyStatelessModel();
            }

            if (!col.isRendered() || !col.isExportable()) {
                continue;
            }

            count++;
        }

        return count;
    }

    protected void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    protected void applyFacetOptions(ExporterOptions options) {
        String facetBackground = options.getFacetBgColor();
        if (facetBackground != null) {
            facetBgColor = Color.decode(facetBackground);
        }

        String facetFontColor = options.getFacetFontColor();
        if (facetFontColor != null) {
            facetFont.setColor(Color.decode(facetFontColor));
        }

        String facetFontSize = options.getFacetFontSize();
        if (facetFontSize != null) {
            facetFont.setSize(Integer.valueOf(facetFontSize));
        }

        String facetFontStyle = options.getFacetFontStyle();
        if (facetFontStyle != null) {
            if (facetFontStyle.equalsIgnoreCase("NORMAL")) {
                facetFontStyle = "" + Font.NORMAL;
            }
            if (facetFontStyle.equalsIgnoreCase("BOLD")) {
                facetFontStyle = "" + Font.BOLD;
            }
            if (facetFontStyle.equalsIgnoreCase("ITALIC")) {
                facetFontStyle = "" + Font.ITALIC;
            }

            facetFont.setStyle(facetFontStyle);
        }
    }

    protected void applyCellOptions(ExporterOptions options) {
        String cellFontColor = options.getCellFontColor();
        if (cellFontColor != null) {
            cellFont.setColor(Color.decode(cellFontColor));
        }

        String cellFontSize = options.getCellFontSize();
        if (cellFontSize != null) {
            cellFont.setSize(Integer.valueOf(cellFontSize));
        }

        String cellFontStyle = options.getCellFontStyle();
        if (cellFontStyle != null) {
            if (cellFontStyle.equalsIgnoreCase("NORMAL")) {
                cellFontStyle = "" + Font.NORMAL;
            }
            if (cellFontStyle.equalsIgnoreCase("BOLD")) {
                cellFontStyle = "" + Font.BOLD;
            }
            if (cellFontStyle.equalsIgnoreCase("ITALIC")) {
                cellFontStyle = "" + Font.ITALIC;
            }

            cellFont.setStyle(cellFontStyle);
        }
    }

    @Override
    protected String exportValue(FacesContext context, UIComponent component) {

        if (component instanceof HtmlCommandLink) { // support for PrimeFaces
                                                    // and standard
                                                    // HtmlCommandLink
            HtmlCommandLink link = (HtmlCommandLink) component;
            Object value = link.getValue();

            if (value != null) {
                return String.valueOf(value);
            } else {
                // export first value holder
                for (UIComponent child : link.getChildren()) {
                    if (child instanceof ValueHolder) {
                        return exportValue(context, child);
                    }
                }

                return "";
            }
        } else if (component instanceof ValueHolder) {

            if (component instanceof EditableValueHolder) {
                Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
                if (submittedValue != null) {
                    return submittedValue.toString();
                }
            }

            ValueHolder valueHolder = (ValueHolder) component;
            Object value = valueHolder.getValue();
            if (value == null) {
                return "";
            }

            Converter converter = valueHolder.getConverter();
            if (converter == null) {
                Class valueType = value.getClass();
                converter = context.getApplication().createConverter(valueType);
            }

            if (converter != null) {
                if (component instanceof UISelectMany) {
                    StringBuilder builder = new StringBuilder();
                    List collection = null;

                    if (value instanceof List) {
                        collection = (List) value;
                    } else if (value.getClass().isArray()) {
                        collection = Arrays.asList(value);
                    } else {
                        throw new FacesException(
                                "Value of " + component.getClientId(context)
                                        + " must be a List or an Array.");
                    }

                    int collectionSize = collection.size();
                    for (int i = 0; i < collectionSize; i++) {
                        Object object = collection.get(i);
                        builder.append(converter.getAsString(context, component,
                                object));

                        if (i < (collectionSize - 1)) {
                            builder.append(",");
                        }
                    }

                    String valuesAsString = builder.toString();
                    builder.setLength(0);

                    return valuesAsString;
                } else {
                    return converter.getAsString(context, component, value);
                }
            } else {
                return value.toString();
            }
        } else if (component instanceof CellEditor) {
            return exportValue(context,
                    ((CellEditor) component).getFacet("output"));
        } else if (component instanceof HtmlGraphicImage) {
            return (String) component.getAttributes().get("alt");
        } else if (component instanceof OverlayPanel) {
            return "";
        } else if (component instanceof UINamingContainer
                || component instanceof HtmlPanelGroup
                || component instanceof ComponentRef) {
            String childValues = "";
            // https://forum.primefaces.org/viewtopic.php?t=15904
            Collection<UIComponent> children = component.getChildren();
            for (UIComponent childComponent : children) {
                String childValue = exportValue(context, childComponent);
                if (childValue != null) {
                    childValues += childValue;
                }
            }

            return childValues;
        } else {
            // This would get the plain texts on UIInstructions when using
            // Facelets
            String value = component.toString();

            if (value != null) {
                return value.trim();
            } else {
                return "";
            }
        }
    }

}
