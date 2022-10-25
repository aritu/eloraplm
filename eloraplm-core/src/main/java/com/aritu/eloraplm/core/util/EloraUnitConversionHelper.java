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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraUnitConversionHelper {

    private static final Log log = LogFactory.getLog(
            EloraUnitConversionHelper.class);

    /*** Conversion between displayUnit and KMS ***/

    private static BigDecimal convertValueFromKmsToDisplayUnit(
            BigDecimal kmsValue, String baseUnit,
            boolean roundIfTooMuchDecimals)
            throws EloraException, ParseException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        // Convert to the correct unit
        EloraConfigRow configRow = EloraConfig.unitConversionConfigMap.get(
                baseUnit);

        double conversionFactor = (double) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);
        long maxDecimalPlaces = (long) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DECIMAL_PLACES);

        BigDecimal displayUnitValue = kmsValue.multiply(
                BigDecimal.valueOf(conversionFactor));

        // Check decimalPlaces and if bigger, --throw exception--.
        int decimalPlaces = displayUnitValue.stripTrailingZeros().scale() > 0
                ? displayUnitValue.stripTrailingZeros().scale()
                : 0;
        if (decimalPlaces > maxDecimalPlaces) {
            if (roundIfTooMuchDecimals) {
                displayUnitValue = displayUnitValue.setScale(
                        Math.toIntExact(maxDecimalPlaces),
                        RoundingMode.HALF_UP);
            }
            // We don't throw an exception for now

            // else {
            //
            // throw new EloraException(
            // "Value has more decimals than expected after conversion: |"
            // + displayUnitValue.toPlainString()
            // + "|");
            // }
        }

        return displayUnitValue;
    }

    private static BigDecimal convertValueFromDisplayUnitToKms(
            BigDecimal displayUnitValue, String baseUnit,
            boolean roundIfTooMuchDecimals)
            throws EloraException, ParseException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        // Convert to the correct unit
        EloraConfigRow configRow = EloraConfig.unitConversionConfigMap.get(
                baseUnit);

        double conversionFactor = (double) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);
        long maxDecimalPlaces = (long) configRow.getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DECIMAL_PLACES);

        // Check decimalPlaces and if bigger, --throw exception--.
        int decimalPlaces = displayUnitValue.stripTrailingZeros().scale() > 0
                ? displayUnitValue.stripTrailingZeros().scale()
                : 0;
        if (decimalPlaces > maxDecimalPlaces) {
            if (roundIfTooMuchDecimals) {
                displayUnitValue = displayUnitValue.setScale(
                        Math.toIntExact(maxDecimalPlaces),
                        RoundingMode.HALF_UP);
            }
            // We don't throw an exception for now

            // else {
            //
            // throw new EloraException(
            // "Value has more decimals than expected before conversion: |"
            // + displayUnitValue.toPlainString()
            // + "|");
            // }
        }

        BigDecimal kmsValue = displayUnitValue.divide(
                BigDecimal.valueOf(conversionFactor));

        return kmsValue;
    }

    /*** Form Converter methods for display/store ***/

    public static String convertValueToDisplay(Locale locale,
            String storedValue, String baseUnit)
            throws EloraException, ParseException {

        BigDecimal valueAsDecimal = EloraDecimalHelper.fromStandardToDecimal(
                storedValue);
        BigDecimal displayUnitValue = convertValueFromKmsToDisplayUnit(
                valueAsDecimal, baseUnit, false);
        return EloraDecimalHelper.fromDecimalToLocalized(locale,
                displayUnitValue);
    }

    public static String convertValueToStore(Locale locale,
            String introducedValue, String baseUnit)
            throws EloraException, ParseException {

        BigDecimal valueAsDecimal = EloraDecimalHelper.fromLocalizedToDecimal(
                locale, introducedValue);
        BigDecimal kmsValue = convertValueFromDisplayUnitToKms(valueAsDecimal,
                baseUnit, false);
        return EloraDecimalHelper.fromDecimalToStandard(kmsValue);
    }

    /*** Auxiliar methods for value conversion ***/

    public static BigDecimal convertValueFromKmsToDisplayUnitRounding(
            String kmsValue, String baseUnit)
            throws EloraException, ParseException {

        BigDecimal valueAsDecimal = EloraDecimalHelper.fromStandardToDecimal(
                kmsValue);
        return convertValueFromKmsToDisplayUnit(valueAsDecimal, baseUnit, true);
    }

    public static String convertValueFromDisplayUnitToKmsString(
            BigDecimal displayUnitValue, String baseUnit)
            throws EloraException, ParseException {

        BigDecimal kmsValue = EloraUnitConversionHelper.convertValueFromDisplayUnitToKms(
                displayUnitValue, baseUnit, false);
        return EloraDecimalHelper.fromDecimalToStandard(kmsValue);
    }

    /*** Unit conversion ***/

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

    /**
     * Checks if given property is a decimal value.
     *
     * @param property
     * @return
     */
    public static boolean isDecimalProperty(String property) {
        if (property.equals(EloraMetadataConstants.ELORA_CAD_MASS)
                || property.equals(EloraMetadataConstants.ELORA_CAD_SURFACE)
                || property.equals(EloraMetadataConstants.ELORA_CAD_VOLUME)) {
            return true;
        }

        return false;
    }

    /**
     * Rounds given property value in function of the configuration defined in
     * elora_unit_conversion_mapping vocabulary.
     *
     * @param property
     * @param kmsValue
     * @return
     */
    public static String roundDecimalValue(String property, String kmsValue) {

        String value = kmsValue;
        String baseUnit = getBaseUnit(property);
        if (baseUnit != null) {

            try {
                BigDecimal displayUnitValue = EloraUnitConversionHelper.convertValueFromKmsToDisplayUnitRounding(
                        kmsValue, baseUnit);
                value = EloraUnitConversionHelper.convertValueFromDisplayUnitToKmsString(
                        displayUnitValue, baseUnit);
            } catch (Exception e) {
                log.error("Value |" + kmsValue
                        + "| could not be converted and rounded.");
            }
        }

        return value;
    }

    private static String getBaseUnit(String property) {
        String baseUnit = null;
        switch (property) {
        case EloraMetadataConstants.ELORA_CAD_MASS:
            baseUnit = "kg";
            break;
        case EloraMetadataConstants.ELORA_CAD_SURFACE:
            baseUnit = "m2";
            break;
        case EloraMetadataConstants.ELORA_CAD_VOLUME:
            baseUnit = "m3";
            break;
        default:
            // No baseUnit. Value won't be rounded.
        }

        return baseUnit;
    }
}
