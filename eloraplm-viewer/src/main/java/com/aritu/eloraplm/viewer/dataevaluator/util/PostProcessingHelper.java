/**
 *
 */
package com.aritu.eloraplm.viewer.dataevaluator.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.tag.fn.UserNameResolverHelper;

import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;

/**
 * @author aritu
 *
 */
public class PostProcessingHelper {

    private static final Log log = LogFactory.getLog(
            PostProcessingHelper.class);

    public static String callPostProcessor(String id, String value) {

        if (value == null || value.length() == 0) {
            return value;
        }

        if (id != null) {
            switch (id) {
            case ViewerConstants.POST_PROCESSOR_RESOLVE_USERNAME:
                return resolveUsername(value);
            case ViewerConstants.POST_PROCESSOR_TO_UPPER_CASE:
                return toUpperCase(value);
            case ViewerConstants.POST_PROCESSOR_TRANSFORM_DATE_FROM_UTC_TO_MAD:
                return transformDateFromUtcToMad(value);
            case ViewerConstants.POST_PROCESSOR_TRANSFORM_DATE_FROM_UTC_TO_MAD_HYPHEN:
                return transformDateFromUtcToMadHyphen(value);
            case ViewerConstants.POST_PROCESSOR_TRANSLATE_TO_ES_UPPER_CASE:
                return translateToEsUpperCase(value);
            case ViewerConstants.POST_PROCESSOR_DISPLAY_MAJOR:
                return displayMajor(value);
            }
        }
        return value;
    }

    private static String resolveUsername(String value) {
        UserNameResolverHelper unr = new UserNameResolverHelper();
        return unr.getUserFullName(value);
    }

    private static String toUpperCase(String value) {
        if (!value.isEmpty()) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value;
    }

    /**
     * Transform a date received in UTC time zone into Europe/Madrid time zone,
     * using / character as separator.
     *
     * @param utcDateStr this parameter is a string representation of a date as
     *            specified in ValueObtainerHelper.stringifyObject(Object o)
     *            method, which is: 'yyyy-MM-dd HH:mm UTC'
     * @return date transformed to Europe/Madrid time zone: dd/MM/yyyy
     */
    private static String transformDateFromUtcToMad(String utcDateStr) {

        return transformDateFromUtcToMad(utcDateStr, "/");
    }

    /**
     * Transform a date received in UTC time zone into Europe/Madrid time zone,
     * using - character as separator.
     *
     * @param utcDateStr this parameter is a string representation of a date as
     *            specified in ValueObtainerHelper.stringifyObject(Object o)
     *            method, which is: 'yyyy-MM-dd HH:mm UTC'
     * @return date transformed to Europe/Madrid time zone: dd-MM-yyyy
     */
    private static String transformDateFromUtcToMadHyphen(String utcDateStr) {

        return transformDateFromUtcToMad(utcDateStr, "-");
    }

    private static String transformDateFromUtcToMad(String utcDateStr,
            String separator) {

        String logInitMsg = "[transformDateFromUtcToMad] ";
        log.trace(logInitMsg + "--- ENTER --- with utcDateStr = |" + utcDateStr
                + "|, separator = |" + separator + "|");

        String madDateStr = utcDateStr;

        SimpleDateFormat sdfmad = new SimpleDateFormat(
                "dd" + separator + "MM" + separator + "yyyy");

        sdfmad.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

        Date utcDate = getUtcDateFromUtcDateString(utcDateStr);

        if (utcDate != null) {
            madDateStr = sdfmad.format(utcDate);
        }

        log.trace(logInitMsg + "--- EXIT --- with madDateStr = |" + madDateStr
                + "|");
        return madDateStr;
    }

    private static Date getUtcDateFromUtcDateString(String utcDateStr) {

        String logInitMsg = "[getUtcDateFromUtcDateString] ";

        Date utcDate = null;

        // Received date format is: 'yyyy-MM-dd HH:mm UTC'
        // Remove ' UTC' from the end.
        utcDateStr = utcDateStr.substring(0,
                utcDateStr.length() - (" UTC").length());

        SimpleDateFormat sdfutc = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdfutc.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            utcDate = sdfutc.parse(utcDateStr);

        } catch (ParseException e) {
            log.error(logInitMsg + "Error parsing UTC date. utcDateStr = |"
                    + utcDateStr + "|", e);
        }

        return utcDate;
    }

    private static String translateToEsUpperCase(String value) {
        String resultValue = EloraMessageHelper.getTranslatedEsMessage(value);
        resultValue = resultValue.toUpperCase(Locale.ROOT);
        return resultValue;
    }

    private static String displayMajor(String value) {
        if (!value.isEmpty()) {

            if (value.contains(".")) {
                return value.substring(0, value.indexOf("."));
            }

        }
        return value;
    }

    /*private static String displayMinor(String value) {
        if (!value.isEmpty()) {
            if (value.contains(".")) {
                return value.substring(value.indexOf("."), value.length());
            }
        }
        return value;
    }*/

}
