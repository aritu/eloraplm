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
package com.aritu.eloraplm.search.convert;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;

import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.sun.faces.util.MessageFactory;

/**
 * Due to a problem with the ILIKE search, this converter adds/removes a * in
 * the last position of the query.
 *
 * The problem is the following: if the searched value contains only one '*'
 * (wildcard) and it is located at the end, it launches the query as a
 * "phrase_prefix" search to ElasticSearch instead of a "wildcard" search. And
 * this type of query does not return all the documents, depending of the
 * max_expansions attribute. In our case, we want Nuxeo to convert all ILIKE
 * queries in wildcard queries and to avoid using phrase_prefix queries. For
 * this reason, we have implemented this ilikeSearchValueConverter Converter.
 *
 * The problem comes from NxqlQueryConverter class, in makeLikeQuery method.
 *
 *
 * @author aritu
 *
 */
@Name("ilikeSearchValueConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class IlikeSearchValueConverter implements Converter {

    private static final Log log = LogFactory.getLog(
            IlikeSearchValueConverter.class);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {

        String logInitMsg = "[getAsObject / convertingValueToStore] ";
        log.trace(logInitMsg + "--- ENTER --- value = |" + value + "|");

        String convertedValue = null;

        try {
            if (value != null && !value.isEmpty()) {

                convertedValue = value;

                // When handling the entered value, if it has one '*' wildcard
                // and it is at the end, add an additional '*'.
                if (StringUtils.countMatches(convertedValue, "*") == 1
                        && convertedValue.endsWith("*")) {
                    convertedValue += "*";
                }
            }
        } catch (Exception e) {
            log.trace(
                    logInitMsg
                            + "Conversion failed: Exception thrown. Exception class = |"
                            + e.getClass() + "|, message: " + e.getMessage(),
                    e);

            FacesMessage message = MessageFactory.getMessage(context,
                    "eloraplm.message.error.ilikeSearchValueConverter", value);

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

                convertedValue = (String) value;

                // When displaying the stored value, if it has two '**'
                // wildcards at the end, remove the last one.
                if (convertedValue.length() > 0
                        && StringUtils.countMatches(convertedValue, "*") == 2
                        && convertedValue.endsWith("**")) {
                    convertedValue = convertedValue.substring(0,
                            convertedValue.length() - 1);
                }
            }
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
                            "eloraplm.message.error.ilikeSearchValueConverter",
                            params));
        }

        log.trace(logInitMsg + "--- EXIT --- convertedValue = |"
                + convertedValue + "|");

        return convertedValue;
    }

}
