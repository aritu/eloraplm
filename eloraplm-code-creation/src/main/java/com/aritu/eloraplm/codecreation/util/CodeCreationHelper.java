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
package com.aritu.eloraplm.codecreation.util;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.codecreation.api.CodeCreationService;
import com.aritu.eloraplm.config.util.CodeCreationConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.CodeCreationConfigConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.ExpressionEvaluator;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CodeCreationHelper {

    protected static Log log = LogFactory.getLog(CodeCreationHelper.class);

    protected static UIDGeneratorService service = Framework.getService(
            UIDGeneratorService.class);

    protected static CodeCreationService ccs = Framework.getService(
            CodeCreationService.class);

    public static String getModeForType(String type) {
        return ccs.getModeForType(type);
    }

    /**
     * Create the code automatically
     *
     * @param doctype
     * @param username
     * @return
     * @throws Exception
     */
    public static String createCode(DocumentModel doc, String username)
            throws EloraException {

        String logInitMsg = "[createCode] [" + username + "] ";

        CodeCreationInfo cci = getCodeCreationInfoForDoc(doc);
        int nextValue = incrementAndReturnNextValue(cci.getSequenceKey());
        String code = buildCode(cci, nextValue);
        log.trace(logInitMsg + "Code created for new document: |" + code + "|");

        return code;
    }

    /**
     * @param doctype
     * @return
     * @throws EloraException
     */
    public static CodeCreationInfo getCodeCreationInfoForDoc(DocumentModel doc)
            throws EloraException {

        String doctype = doc.getType();
        EloraConfigTable codeCreationConfig = CodeCreationConfigHelper.getCodeCreationConfig(
                doctype);
        List<String> maskIds = new ArrayList<String>();

        for (EloraConfigRow configRow : codeCreationConfig.getValues()) {
            String maskId = (String) configRow.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_TYPES_MASK_ID);
            String conditionId = (String) configRow.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_TYPES_CONDITION_ID);
            // Check if condition is satisfied
            if (conditionId != null && conditionId.length() > 0) {
                EloraConfigRow codeCondition = CodeCreationConfigHelper.getConditionConfig(
                        conditionId);
                boolean conditionIsSatisfied = evaluateCondition(doc,
                        conditionId, codeCondition);
                if (conditionIsSatisfied) {
                    maskIds.add(maskId);
                }
            } else {
                maskIds.add(maskId);
            }
        }

        if (maskIds.size() == 0) {
            throw new EloraException(
                    "There is no mask configured for current doctype |"
                            + doctype + "|.");
        } else if (maskIds.size() > 1) {
            throw new EloraException(
                    "There are more than one mask configured for current doctype |"
                            + doctype + "|.");
        } else {
            EloraConfigRow codeMask = CodeCreationConfigHelper.getMaskConfig(
                    maskIds.get(0));
            if (codeMask == null || codeMask.isEmpty()) {
                throw new EloraException(
                        "There is no mask configured for current doctype |"
                                + doctype + "|.");
            }

            String prefix = "";
            Object prefixValue = codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_PREFIX);
            if (prefixValue != null) {
                prefix = prefixValue.toString();
            }
            String suffix = "";
            Object suffixValue = codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_SUFFIX);
            if (suffixValue != null) {
                suffix = suffixValue.toString();
            }

            int digits = (int) (long) codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_DIGITS);
            int minValue = (int) (long) codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_MINVALUE);
            int maxValue = (int) (long) codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_MAXVALUE);

            String sequenceKey = "";
            Object sequenceKeyValue = codeMask.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_MASKS_SEQUENCEKEY);
            if (sequenceKeyValue != null) {
                sequenceKey = sequenceKeyValue.toString();
            }

            CodeCreationInfo cci = new CodeCreationInfo(prefix, suffix, digits,
                    minValue, maxValue, sequenceKey);

            return cci;
        }
    }

    public static int incrementAndReturnNextValue(String sequenceKey)
            throws EloraException {
        if (sequenceKey.isEmpty()) {
            throw new EloraException(
                    "Can not get next value for empty sequence key");
        }

        UIDSequencer seq = service.getSequencer("hibernateSequencer");
        sequenceKey = replaceDateVariables(sequenceKey);
        int nextValue = seq.getNext(sequenceKey);

        if (nextValue == 0) {
            throw new EloraException(
                    "An error occurred while getting the next value for the sequence, with key |"
                            + sequenceKey + "|");
        }
        return nextValue;
    }

    /**
     * @param cci
     * @return
     * @throws EloraException
     */
    public static String buildCode(CodeCreationInfo cci, int nextValue)
            throws EloraException {

        if ((nextValue < cci.getMinValue())
                || (nextValue > cci.getMaxValue())) {

            throw new EloraException(
                    "The next value of the sequence is not within the allowed range of values.");

        }

        // Format prefix and suffix replacing date variables with values (if
        // there are)
        String prefix = replaceDateVariables(cci.getPrefix());
        String suffix = replaceDateVariables(cci.getSuffix());

        String paddedNextValue = StringUtils.leftPad(String.valueOf(nextValue),
                cci.getDigits(), '0');
        return prefix + paddedNextValue + suffix;
    }

    private static String replaceDateVariables(String initialText) {
        Date now = new Date();

        StringBuffer sb = new StringBuffer();
        Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher m = p.matcher(initialText);
        while (m.find()) {
            String repString = new SimpleDateFormat(m.group(1)).format(now);
            m.appendReplacement(sb, repString);

        }
        m.appendTail(sb);

        return sb.toString();
    }

    public static boolean isGenerateAutomaticCode(DocumentModel doc) {

        String mode = getModeForType(doc.getType());
        if (mode.equals(CodeCreationService.CODE_CREATION_TYPE_MODE_MANUAL)
                || mode.equals(
                        CodeCreationService.CODE_CREATION_TYPE_MODE_MANUAL_REQUIRED)) {
            return false;
        }

        if (doc.getContextData(
                EloraGeneralConstants.CONTEXT_SKIP_AUTOMATIC_CODE_CREATION) != null) {

            boolean skipAutoCodeCreation = (boolean) doc.getContextData(
                    EloraGeneralConstants.CONTEXT_SKIP_AUTOMATIC_CODE_CREATION);

            if (skipAutoCodeCreation) {
                return false;
            }
        }

        return true;
    }

    private static boolean evaluateCondition(DocumentModel doc,
            String conditionId, EloraConfigRow codeCondition)
            throws EloraException {
        String logInitMsg = "[evaluateCondition] ";

        boolean conditionIsSatisfied = false;

        try {
            String className = (String) codeCondition.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_CONDITIONS_CLASSNAME);
            String methodName = (String) codeCondition.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_CONDITIONS_METHODNAME);

            String methodParams = "";
            Class[] methodParamsClassesArray = null;
            Object[] methodParamsObjectsArray = null;
            Object methodParamsValue = codeCondition.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_CONDITIONS_METHODPARAMS);
            if (methodParamsValue != null) {
                methodParams = methodParamsValue.toString();
                // convert methodParams to Class array
                String[] methodParamsArrayStr = methodParams.split(",");
                methodParamsClassesArray = new Class[methodParamsArrayStr.length
                        + 1];
                methodParamsObjectsArray = new Object[methodParamsClassesArray.length];
                methodParamsClassesArray[0] = DocumentModel.class;
                methodParamsObjectsArray[0] = doc;
                for (int i = 0; i < methodParamsArrayStr.length; i++) {
                    methodParamsClassesArray[i
                            + 1] = methodParamsArrayStr[i].getClass();
                    methodParamsObjectsArray[i + 1] = methodParamsArrayStr[i];
                }
            }

            String operator = (String) codeCondition.getProperty(
                    CodeCreationConfigConstants.PROP_CODE_CONDITIONS_OPERATOR);

            // Validate that the operator has a valid value. For the instance,
            // there is a list of allowed operators.
            if (isValidOperator(operator)) {
                String conditionValue = (String) codeCondition.getProperty(
                        CodeCreationConfigConstants.PROP_CODE_CONDITIONS_VALUE);

                // Evaluate the condition
                Class<?> conditionClass = Class.forName(className);
                Object conditionClassObject = conditionClass.newInstance();
                Method conditionMethod = conditionClassObject.getClass().getMethod(
                        methodName, methodParamsClassesArray);

                Object conditionEvalutionResult = conditionMethod.invoke(null,
                        methodParamsObjectsArray);

                // compare result with expected value in function of the
                // operator
                conditionIsSatisfied = ExpressionEvaluator.evaluateExpression(
                        conditionEvalutionResult, conditionValue, operator);

            }
        } catch (Exception e) {
            log.error(logInitMsg
                    + "Exception evaluating the code creation condition. conditonId = |"
                    + conditionId + "|", e);
            throw new EloraException(
                    "Error evaluating the code creation condition: "
                            + e.getMessage());
        }

        return conditionIsSatisfied;
    }

    private static boolean isValidOperator(String operator) {
        boolean isValid = false;
        if (ExpressionEvaluator.getSupportedOperators().contains(operator)) {
            isValid = true;
        }
        return isValid;
    }

}
