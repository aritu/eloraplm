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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;

import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.sun.faces.util.MessageFactory;

/**
 * This class converts decimal values from the locale representation to the
 * correct format, considering the limits set on the configuration
 *
 * @author aritu
 *
 */
@Name("decimalValueConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class DecimalValueConverter implements Converter {

    private static final Log log = LogFactory.getLog(
            DecimalValueConverter.class);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {

        String logInitMsg = "[getAsObject / convertingValueToStore] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        String convertedValue = null;

        try {
            if (value != null && !value.isEmpty()) {

                convertedValue = EloraDecimalHelper.fromLocalizedToStandard(
                        context, value);
            }
        } catch (ConverterException e) {
            throw e;
        } catch (Exception e) {

            log.trace(
                    logInitMsg
                            + "Conversion failed: Exception thrown. Exception class = |"
                            + e.getClass() + "|, message: " + e.getMessage(),
                    e);

            FacesMessage message = MessageFactory.getMessage(context,
                    "eloraplm.message.error.decimalValueConverter", value);

            throw new ConverterException(message);
        }

        log.trace(logInitMsg + "--- EXIT --- convertedValue = |"
                + convertedValue + "|");

        return convertedValue;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {

        String logInitMsg = "[getAsString / convertingValueToDisplay] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        String convertedValue = "";

        try {

            if (value != null) {

                convertedValue = EloraDecimalHelper.fromStandardToLocalized(
                        context, (String) value);
            }

        } catch (ConverterException e) {
            throw e;
        } catch (Exception e) {

            // Here we can't throw a ConverterException, so we log it as an
            // error and we use FacesMessages to notify the user
            log.error(
                    logInitMsg
                            + "Conversion of saved value failed: Exception thrown. Exception class = |"
                            + e.getClass() + "|, message: " + e.getMessage(),
                    e);

            Object[] params = { value };
            FacesMessages.instance().add(StatusMessage.Severity.ERROR,
                    EloraMessageHelper.getTranslatedMessage(context,
                            "eloraplm.message.error.decimalValueConverter",
                            params));
        }

        log.trace(logInitMsg + "--- EXIT --- convertedValue = |"
                + convertedValue + "|");

        return convertedValue;
    }

}
