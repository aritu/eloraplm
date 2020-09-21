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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectMany;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputFormat;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import com.sun.faces.facelets.compiler.UIInstructions;
import com.sun.faces.facelets.tag.ui.ComponentRef;

import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.Exporter;
import org.primefaces.component.export.ExporterOptions;
import org.primefaces.component.overlaypanel.OverlayPanel;

/**
 *
 * @author aritu
 *
 */
public abstract class EloraExporter extends Exporter {

    @Override
    public abstract void export(FacesContext facesContext, DataTable table,
            String outputFileName, boolean pageOnly, boolean selectionOnly,
            String encodingType, MethodExpression preProcessor,
            MethodExpression postProcessor, ExporterOptions options,
            MethodExpression onTableRender) throws IOException;

    @Override
    public abstract void export(FacesContext facesContext,
            List<String> clientIds, String outputFileName, boolean pageOnly,
            boolean selectionOnly, String encodingType,
            MethodExpression preProcessor, MethodExpression postProcessor,
            ExporterOptions options, MethodExpression onTableRender)
            throws IOException;

    @Override
    public abstract void export(FacesContext facesContext,
            String outputFileName, List<DataTable> tables, boolean pageOnly,
            boolean selectionOnly, String encodingType,
            MethodExpression preProcessor, MethodExpression postProcessor,
            ExporterOptions options, MethodExpression onTableRender)
            throws IOException;

    @Override
    protected abstract void exportCells(DataTable table, Object document);

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
        } else if (component instanceof UIParameter) {
            UIParameter p = (UIParameter) component;
            return p.getValue().toString();
        } else if (component instanceof UINamingContainer
                || component instanceof UIPanel
                || component instanceof HtmlPanelGroup
                || component instanceof ComponentRef
                || component instanceof HtmlOutputFormat) {
            String childValues = "";
            // https://forum.primefaces.org/viewtopic.php?t=15904
            Collection<UIComponent> children = component.getChildren();
            for (UIComponent childComponent : children) {
                // We remove HTML tags
                if (!(childComponent instanceof UIInstructions)) {
                    String childValue = exportValue(context, childComponent);
                    if (childValue != null) {
                        childValues += childValue;
                    }
                }
            }

            return childValues;
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
