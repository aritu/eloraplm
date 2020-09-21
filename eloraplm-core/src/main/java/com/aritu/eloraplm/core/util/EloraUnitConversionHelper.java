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
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraUnitConversionHelper {

    public static String convertValueToDisplay(Locale locale,
            String storedValue, String baseUnit)
            throws EloraException, ParseException {

        BigDecimal convertedValueAsDecimal = convertValueToUnit(storedValue,
                baseUnit);

        return EloraDecimalHelper.fromDecimalToLocalized(locale,
                convertedValueAsDecimal);
    }

    public static BigDecimal convertValueToUnitRounding(String storedValue,
            String baseUnit) throws EloraException, ParseException {
        return convertValueToUnit(storedValue, baseUnit, true);
    }

    public static BigDecimal convertValueToUnit(String storedValue,
            String baseUnit) throws EloraException, ParseException {
        return convertValueToUnit(storedValue, baseUnit, false);

    }

    private static BigDecimal convertValueToUnit(String storedValue,
            String baseUnit, boolean roundIfTooMuchDecimals)
            throws EloraException, ParseException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        BigDecimal storedValueAsDecimal = EloraDecimalHelper.fromStandardToDecimal(
                storedValue);

        // Convert to the correct unit
        EloraConfigRow configRow = EloraConfig.unitConversionConfigMap.get(
                baseUnit);

        double conversionFactor = (double) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);
        long maxDecimalPlaces = (long) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DECIMAL_PLACES);

        BigDecimal convertedValueAsDecimal = storedValueAsDecimal.multiply(
                BigDecimal.valueOf(conversionFactor));

        // Check decimalPlaces and if bigger, throw exception.
        int decimalPlaces = convertedValueAsDecimal.stripTrailingZeros().scale() > 0
                ? convertedValueAsDecimal.stripTrailingZeros().scale()
                : 0;
        if (decimalPlaces > maxDecimalPlaces) {
            if (roundIfTooMuchDecimals) {
                convertedValueAsDecimal = convertedValueAsDecimal.setScale(
                        Math.toIntExact(maxDecimalPlaces),
                        RoundingMode.HALF_UP);
            }
            // We don't throw an exception for now

            // else {
            //
            // throw new EloraException(
            // "Value has more decimals than expected after conversion: |"
            // + convertedValueAsDecimal.toPlainString()
            // + "|");
            // }
        }

        return convertedValueAsDecimal;
    }

    public static String convertUnitToDisplay(String baseUnit)
            throws EloraException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        EloraConfigRow configRow = EloraConfig.unitConversionConfigMap.get(
                baseUnit);
        String convertedUnit = (String) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DISPLAY_UNIT);

        return convertedUnit;
    }

    public static String convertValueToStore(Locale locale, String value,
            String baseUnit) throws EloraException, ParseException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        // Convert the value decimal separator depending on the locale
        BigDecimal valueAsDecimal = EloraDecimalHelper.fromLocalizedToDecimal(
                locale, value);

        EloraConfigRow configRow = EloraConfig.unitConversionConfigMap.get(
                baseUnit);
        double conversionFactor = (double) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);

        BigDecimal convertedValueAsDecimal = valueAsDecimal.divide(
                BigDecimal.valueOf(conversionFactor));

        return EloraDecimalHelper.fromDecimalToStandard(
                convertedValueAsDecimal);
    }

}
