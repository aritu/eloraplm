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
package com.aritu.eloraplm.webapp.base.convert;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;

/**
 *
 * @author aritu
 *
 */
@Name("documentModelConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class DocumentModelConverterBean implements Converter {

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {

        String convertedValue = "";

        if (value == null) {
            return "";
        }

        if (value instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) value;
            convertedValue = doc.getId();
        } else {
            throw new ConverterException(
                    value + " is not a valid document model.");
        }

        return convertedValue;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {

        if (value == null || value.isEmpty()) {
            return null;
        }

        @SuppressWarnings("resource")
        CoreSession session = (CoreSession) component.getAttributes().get(
                "session");
        if (session == null) {
            throw new ConverterException("No session provided.");
        }

        try {
            DocumentModel doc = session.getDocument(new IdRef(value));
            return doc;
        } catch (DocumentNotFoundException e) {
            throw new ConverterException(
                    "No document found with id '" + value + "'", e);
        }
    }

}
