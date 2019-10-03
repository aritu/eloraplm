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
package com.aritu.eloraplm.bom.characteristics.util;

import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.sun.faces.util.MessageFactory;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomCharacteristicsValidatorHelper {

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsValidatorHelper.class);

    public static void validateListValue(FacesContext context, String listValue,
            List<Map<String, String>> listContent) {

        String logInitMsg = "[validateListValue] ";
        log.trace(logInitMsg + "--- ENTER --- listValue = |" + listValue + "|");

        if (listValue != null && !listValue.isEmpty()) {
            try {

                if (listContent == null || listContent.size() == 0) {
                    log.error(logInitMsg + "listContent is empty.");
                    log.trace(logInitMsg + "--- EXIT ---");
                    FacesMessage message = MessageFactory.getMessage(context,
                            "message.error.bomcharac.listContentIsEmpty");
                    throw new ValidatorException(message);
                }

                boolean isValueDefinedInList = false;

                for (int i = 0; i < listContent.size(); i++) {
                    Map<String, String> listContentElements = listContent.get(
                            i);
                    if (listContentElements.get(
                            BomCharacteristicsConstants.BOM_CHARAC_LIST_VALUE) != null) {
                        String listContentValue = listContentElements.get(
                                BomCharacteristicsConstants.BOM_CHARAC_LIST_VALUE);
                        if (listContentValue.equals(listValue)) {
                            isValueDefinedInList = true;
                            // if the value is defined, break the loop
                            break;
                        }
                    }
                }
                log.trace(logInitMsg + "isValueDefinedInList = |"
                        + isValueDefinedInList + "|");

                if (!isValueDefinedInList) {
                    log.error(logInitMsg + "defaultValue=|" + listValue
                            + "| is not defined in list content = |"
                            + listContent + "|");
                    log.trace(logInitMsg + "--- EXIT ---");
                    FacesMessage message = MessageFactory.getMessage(context,
                            "message.error.bomcharac.valueNotDefinedInListContent",
                            listValue);
                    throw new ValidatorException(message);
                }
            } catch (ValidatorException e) {
                throw e;
            } catch (Exception e) {
                log.trace(logInitMsg
                        + "Validation failed: Exception thrown. Exception class = |"
                        + e.getClass() + "|, message: " + e.getMessage(), e);

                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.listValueValidator",
                        listValue);
                throw new ValidatorException(message);
            }
        }
    }

    public static void validateStringValue(FacesContext context,
            String stringValue, Long maxLength) {

        String logInitMsg = "[validateStringValue] ";
        log.trace(logInitMsg + "--- ENTER --- stringValue = |" + stringValue
                + "|");

        if (stringValue != null && !stringValue.isEmpty()) {
            try {

                if (maxLength == null) {
                    log.error(logInitMsg + "maxLength is null.");
                    log.trace(logInitMsg + "--- EXIT ---");
                    FacesMessage message = MessageFactory.getMessage(context,
                            "message.error.bomcharac.maxLenghIsNull");
                    throw new ValidatorException(message);
                }

                if (stringValue.length() > maxLength) {

                    log.error(logInitMsg + "stringValue=|" + stringValue
                            + "| is longer than maxLength = |" + maxLength
                            + "|");
                    log.trace(logInitMsg + "--- EXIT ---");
                    FacesMessage message = MessageFactory.getMessage(context,
                            "message.error.bomcharac.stringLengthTooLong",
                            maxLength);
                    throw new ValidatorException(message);

                }
            } catch (ValidatorException e) {
                throw e;
            } catch (Exception e) {
                log.trace(logInitMsg
                        + "Validation failed: Exception thrown. Exception class = |"
                        + e.getClass() + "|, message: " + e.getMessage(), e);

                FacesMessage message = MessageFactory.getMessage(context,
                        "message.error.bomcharac.stringValueValidator",
                        stringValue);

                throw new ValidatorException(message);
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
