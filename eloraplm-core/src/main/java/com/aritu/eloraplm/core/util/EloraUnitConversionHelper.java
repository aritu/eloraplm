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

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraUnitConversionHelper {

    public static double convertValueToDisplay(double storedValue,
            String baseUnit) throws EloraException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        EloraConfigTable unitConverterConfig = EloraConfigHelper.getUnitConversionConfig(
                baseUnit);
        double conversionFactor = (double) unitConverterConfig.getFirst().getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);

        long decimalPlaces = (long) unitConverterConfig.getFirst().getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DECIMAL_PLACES);

        double convertedValue = storedValue * conversionFactor;

        double truncateFactor = Math.pow(10, decimalPlaces);
        double truncatedConvertedValue = Math.floor(
                convertedValue * truncateFactor) / truncateFactor;

        return truncatedConvertedValue;
    }

    public static String convertUnitToDisplay(String baseUnit)
            throws EloraException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        EloraConfigTable unitConverterConfig = EloraConfigHelper.getUnitConversionConfig(
                baseUnit);
        String convertedUnit = (String) unitConverterConfig.getFirst().getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_DISPLAY_UNIT);

        return convertedUnit;
    }

    public static double convertValueToStore(double value, String baseUnit)
            throws EloraException {

        if (baseUnit == null) {
            throw new EloraException("baseUnit is null");
        }

        EloraConfigTable unitConverterConfig = EloraConfigHelper.getUnitConversionConfig(
                baseUnit);
        double conversionFactor = (double) unitConverterConfig.getFirst().getProperty(
                EloraConfigConstants.PROP_UNIT_CONVERSION_MAPPING_CONVERSION_FACTOR);

        double convertedValue = value / conversionFactor;

        return convertedValue;
    }

}
