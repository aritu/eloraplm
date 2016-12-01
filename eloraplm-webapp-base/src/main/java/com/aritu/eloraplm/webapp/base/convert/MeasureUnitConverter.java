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

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

import com.aritu.eloraplm.core.util.EloraUnitConversionHelper;

/**
 * This class converts a given measure unit in the valid format to be displayed
 * or stored in the system.
 *
 * @author aritu
 *
 */
@Name("measureUnitConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class MeasureUnitConverter implements Converter {

    private static final Log log = LogFactory.getLog(
            MeasureUnitConverter.class);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        /*
        For the instance we don't have to convert the unit value when we store it,
        since it is initialized at the beginning and we don't change it.
        */
        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {

        String logInitMsg = "[getAsString / convertingUnitToDisplay] ";
        log.trace(logInitMsg + "--- ENTER --- unit = |" + value + "|");

        String convertedUnit = "";
        try {

            convertedUnit = EloraUnitConversionHelper.convertUnitToDisplay(
                    (String) value);
        } catch (Exception e) {

            log.error(
                    logInitMsg + "Exception thrown. Exception class = |"
                            + e.getClass() + "|, message: " + e.getMessage(),
                    e);

            throw new ConverterException(e.getMessage());
        }

        log.trace(logInitMsg + "--- EXIT --- convertedUnit = |" + convertedUnit
                + "|");

        return convertedUnit;
    }

}
