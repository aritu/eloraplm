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
package com.aritu.eloraplm.core.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.sun.faces.util.MessageFactory;

/**
 * Helper for decimal values conversion
 *
 * @author aritu
 *
 */
public class EloraDecimalHelper {

    private static final Log log = LogFactory.getLog(EloraDecimalHelper.class);

    private static final char DECIMAL_SEPARATOR_STANDARD = '.';

    public static String fromLocalizedToStandard(Locale locale, String value) {

        String separator = getLocaleSeparator(locale);
        if (!separator.equals(String.valueOf(DECIMAL_SEPARATOR_STANDARD))) {
            value = value.replace(separator,
                    String.valueOf(DECIMAL_SEPARATOR_STANDARD));
        }

        return value;
    }

    public static String fromStandardToLocalized(Locale locale, String value) {
        String separator = getLocaleSeparator(locale);
        if (!separator.equals(String.valueOf(DECIMAL_SEPARATOR_STANDARD))) {
            value = value.replace(String.valueOf(DECIMAL_SEPARATOR_STANDARD),
                    separator);
        }
        return value;
    }

    public static BigDecimal fromStandardToDecimal(String value)
            throws ParseException {
        DecimalFormat format = getStandardFormat();

        return parseAsDecimal(format, value);
    }

    public static String fromDecimalToStandard(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public static BigDecimal fromLocalizedToDecimal(Locale locale, String value)
            throws ParseException {
        // Instead of using localized DecimalFormat, we do it this way to allow
        // using the dot as a decimal separator always, even if the locale uses
        // another separator normally
        value = fromLocalizedToStandard(locale, value);
        DecimalFormat format = getStandardFormat();

        return parseAsDecimal(format, value);
    }

    public static String fromDecimalToLocalized(Locale locale,
            BigDecimal value) {
        String standardValue = value.stripTrailingZeros().toPlainString();
        return fromStandardToLocalized(locale, standardValue);
    }

    private static DecimalFormat getStandardFormat() {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance();
        DecimalFormatSymbols dfs = format.getDecimalFormatSymbols();
        dfs.setDecimalSeparator(DECIMAL_SEPARATOR_STANDARD);
        format.setDecimalFormatSymbols(dfs);
        return format;
    }

    private static DecimalFormat getLocaleFormat(Locale locale) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
        return format;
    }

    private static String getLocaleSeparator(Locale locale) {
        DecimalFormat format = getLocaleFormat(locale);
        char separator = format.getDecimalFormatSymbols().getDecimalSeparator();

        return String.valueOf(separator);
    }

    private static BigDecimal parseAsDecimal(DecimalFormat format, String value)
            throws ParseException {
        // We want to ensure that all the value is a valid decimal, so we force
        // parsing from the beginning to the end
        ParsePosition position = new ParsePosition(0);
        format.setParseBigDecimal(true);
        format.setGroupingUsed(false);

        BigDecimal valueAsDecimal = (BigDecimal) format.parse(value, position);

        if (position.getIndex() != value.length()) {
            throw new ParseException(
                    "Failed to parse entire String to BigDecimal: " + value,
                    position.getIndex());
        }
        return valueAsDecimal;
    }

    public static void validateDecimalValue(Locale locale, String value,
            Integer maxIntegers, Integer maxDecimals)
            throws ValidatorException {
        validateDecimalValue(locale, value, value, maxIntegers, maxDecimals);
    }

    public static void validateDecimalValue(Locale locale,
            String submittedValue, String convertedValue, Integer maxIntegers,
            Integer maxDecimals) throws ValidatorException {

        String logInitMsg = "[validateDecimalValue] ";
        log.trace(
                logInitMsg + "--- ENTER --- value = |" + convertedValue + "|");

        if (convertedValue != null) {
            try {
                BigDecimal valueAsDecimal = EloraDecimalHelper.fromStandardToDecimal(
                        convertedValue);

                int decimalPlaces = valueAsDecimal.stripTrailingZeros().scale() > 0
                        ? valueAsDecimal.stripTrailingZeros().scale()
                        : 0;
                int integerPlaces = valueAsDecimal.stripTrailingZeros().precision()
                        - decimalPlaces;

                if (maxIntegers == null) {
                    maxIntegers = Integer.parseInt(
                            EloraConfig.generalConfigMap.get(
                                    EloraConfigConstants.KEY_DECIMAL_MAX_INTEGER_PLACES));
                }
                int maxIntegerPlaces = maxIntegers;

                if (maxDecimals == null) {
                    maxDecimals = Integer.parseInt(
                            EloraConfig.generalConfigMap.get(
                                    EloraConfigConstants.KEY_DECIMAL_MAX_DECIMAL_PLACES));
                }
                int maxDecimalPlaces = maxDecimals;

                if (integerPlaces > maxIntegerPlaces
                        || decimalPlaces > maxDecimalPlaces) {
                    log.trace(logInitMsg + "Validation failed: "
                            + convertedValue + " has more than " + maxIntegers
                            + " integers and/or " + maxDecimals + " decimals.");

                    FacesMessage message = MessageFactory.getMessage(locale,
                            "eloraplm.message.error.decimalValueOutOfLimits",
                            maxIntegers, maxDecimals);

                    throw new ValidatorException(message);
                }
            } catch (ValidatorException e) {
                throw e;
            } catch (Exception e) {
                log.trace(logInitMsg
                        + "Validation failed: Exception thrown. Exception class = |"
                        + e.getClass() + "|, message: " + e.getMessage(), e);

                FacesMessage message = MessageFactory.getMessage(locale,
                        "eloraplm.message.error.decimalValueValidator",
                        submittedValue);

                throw new ValidatorException(message);
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
