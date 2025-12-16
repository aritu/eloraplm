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

/**
 *
 * @author aritu
 *
 */
@Name("triStateBooleanConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class TriStateBooleanConverterBean implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
        case "0":
            return null;
        case "1":
            return Boolean.TRUE;
        case "2":
            return Boolean.FALSE;
        default:
            throw new ConverterException();
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        Boolean bool = (Boolean) value;
        if (bool == null) {
            return "0";
        }
        return bool ? "1" : "2";
    }

}