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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.aritu.eloraplm.constants.ExpressionEvaluatorConstants;

/**
 *
 * @author aritu
 *
 */
public class ExpressionEvaluator {

    protected static List<String> supportedOperators = new ArrayList<>(
            Arrays.asList(ExpressionEvaluatorConstants.OPERATOR_EQUALS,
                    ExpressionEvaluatorConstants.OPERATOR_NOT_EQUALS,
                    ExpressionEvaluatorConstants.OPERATOR_IN,
                    ExpressionEvaluatorConstants.OPERATOR_NOT_IN));

    public static List<String> getSupportedOperators() {
        return supportedOperators;
    }

    public static boolean evaluateExpression(Object leftValue,
            String rightValue, String operator) {
        boolean result = false;
        if (operator.equals(ExpressionEvaluatorConstants.OPERATOR_IN)
                || operator.equals(
                        ExpressionEvaluatorConstants.OPERATOR_NOT_IN)) {

            List<String> conditionValues = Arrays.asList(
                    rightValue.split(Pattern.quote(
                            ExpressionEvaluatorConstants.VALUE_LIST_SEPARATOR)));

            switch (operator) {
            case ExpressionEvaluatorConstants.OPERATOR_IN:
                if (conditionValues.contains(leftValue)) {
                    result = true;
                }
                break;
            case ExpressionEvaluatorConstants.OPERATOR_NOT_IN:
                if (!conditionValues.contains(leftValue)) {
                    result = true;
                }
                break;
            default:
                break;
            }
        } else if (operator.equals(ExpressionEvaluatorConstants.OPERATOR_EQUALS)
                || operator.equals(
                        ExpressionEvaluatorConstants.OPERATOR_NOT_EQUALS)) {
            switch (operator) {
            case ExpressionEvaluatorConstants.OPERATOR_EQUALS:
                if (rightValue.equals(leftValue)) {
                    result = true;
                }
                break;
            case ExpressionEvaluatorConstants.OPERATOR_NOT_EQUALS:
                if (!rightValue.equals(leftValue)) {
                    result = true;
                }
                break;
            default:
                break;
            }
        }
        return result;
    }

}
