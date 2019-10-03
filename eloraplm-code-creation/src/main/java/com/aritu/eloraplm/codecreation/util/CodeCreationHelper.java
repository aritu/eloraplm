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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.CodeCreationConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.constants.CodeCreationConfigConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class CodeCreationHelper {

    protected static Log log = LogFactory.getLog(CodeCreationHelper.class);

    protected static UIDGeneratorService service = Framework.getService(
            UIDGeneratorService.class);

    /**
     * Create the code automatically
     *
     * @param doc
     * @param username
     * @return
     * @throws Exception
     */
    public static String createCode(DocumentModel doc, String username)
            throws Exception {

        String logInitMsg = "[createCode] [" + username + "] ";

        CodeCreationInfo cci = getCodeCreationInfoForDoctype(doc.getType());
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
    public static CodeCreationInfo getCodeCreationInfoForDoctype(String doctype)
            throws EloraException {

        EloraConfigRow codeMask = CodeCreationConfigHelper.getMaskConfigForDoctype(
                doctype);

        if (codeMask == null || codeMask.isEmpty()) {
            throw new EloraException(
                    "There is no mask configured for current doctype.");
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

    public static int incrementAndReturnNextValue(String sequenceKey)
            throws EloraException {
        if (sequenceKey.isEmpty()) {
            throw new EloraException(
                    "Can not get next value for empty sequence key");
        }

        UIDSequencer seq = service.getSequencer("hibernateSequencer");
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

    public static boolean isGenerateAutomaticCode(DocumentModel doc,
            String value) {

        if (!doc.hasFacet(EloraFacetConstants.FACET_AUTOMATIC_CODE)) {
            return false;
        }

        if (value != null && !value.isEmpty()) {
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
}
