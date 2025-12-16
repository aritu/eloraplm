/**
 *
 */
package com.aritu.eloraplm.viewer.dataevaluator.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.ViewerConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.dataevaluator.api.DataObtainerAdapter;
import com.aritu.eloraplm.viewer.methodexecuter.MethodExecuter;

/**
 * @author aritu
 *
 */
public class ValueObtainerHelper {

    private static final Log log = LogFactory.getLog(ValueObtainerHelper.class);

    public static String getValue(DocumentModel currentDoc, String action,
            DataObtainerAdapter doa) throws EloraException {
        return getValue(currentDoc, null, action, doa);
    }

    public static String getValue(DocumentModel currentDoc,
            DocumentModel relatedDoc, String action, DataObtainerAdapter doa)
            throws EloraException {
        DocumentModel target = getTargetConsideringExceptions(currentDoc,
                relatedDoc, action, doa);
        Object o = getValueConsideringExceptions(action, target, doa);
        String value = stringifyObject(o);
        return value;
    }

    private static DocumentModel getTargetConsideringExceptions(
            DocumentModel currentDoc, DocumentModel relatedDoc, String action,
            DataObtainerAdapter doa) throws EloraException {

        DocumentModel doc = currentDoc;

        // Check if the source is a related document or not.
        String source = doa.getSource();
        if (source != null
                && source.equals(ViewerConstants.SOURCE_RELATED_DOC)) {
            doc = relatedDoc;
        }

        // Check if the document is a WC or is a AV.
        if (doc.isImmutable()) {
            // If it is an AV, return the document itself.
            return doc;
        } else {
            // Otherwise, check if it needs to retrieve the base version or not.
            boolean needsBase = false;
            if (action.equals(ViewerConstants.ACTION_OVERWRITE_AV)) {
                needsBase = false;
            } else if (doa.getMethod() != null
                    && doa.getMethod().equals("getVersionLabel")) {
                needsBase = true;
            } else if (action.equals(ViewerConstants.ACTION_OVERWRITE)) {
                if (doa.getMethod() != null
                        && doa.getMethod().equals("getCurrentLifeCycleState")) {
                    needsBase = true;
                } else if (doa.getXpath() != null && (doa.getXpath().equals(
                        NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR)
                        || doa.getXpath().equals(
                                NuxeoMetadataConstants.NX_DC_MODIFIED)
                        || doa.getXpath().equals(
                                EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED)
                        || doa.getXpath().equals(
                                EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER))) {
                    needsBase = true;
                }
            }
            if (needsBase) {
                DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                        doc);
                if (baseVersion != null) {
                    return baseVersion;
                }
            } else {
                return doc;
            }
        }

        return doc;
    }

    private static Object getValueConsideringExceptions(String action,
            DocumentModel target, DataObtainerAdapter doa)
            throws EloraException {

        String logInitMsg = "[getValueConsideringExceptions] ";

        switch (doa.getType()) {
        case ViewerConstants.TYPE_XPATH:
            Serializable pty = target.getPropertyValue(doa.getXpath());
            if (pty != null) {
                return pty;
            } else {
                return "";
            }
        case ViewerConstants.TYPE_METHOD:
            try {
                Method method;
                MethodExecuter methodExec = target.getAdapter(
                        MethodExecuter.class);
                if (methodExec != null) {
                    if (doa.getMethodParams() != null
                            && doa.getMethodParams().length > 0) {
                        method = methodExec.getClass().getMethod(
                                doa.getMethod(), new Class[] { String.class });
                        return method.invoke(methodExec, doa.getMethodParams());
                    } else {
                        method = methodExec.getClass().getMethod(
                                doa.getMethod());
                        return method.invoke(methodExec);
                    }
                } else {
                    throw new EloraException(
                            "Error calling to method |" + doa.getMethod()
                                    + "|. No MethodExecuterAdapter found.");
                }
            } catch (SecurityException | NoSuchMethodException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new EloraException(
                        "Error calling to method |" + doa.getMethod() + "|.",
                        e);
            }
        case "fixed":
            return doa.getValue();
        default:
            String exceptionMessage = "DataObtainerAdapter type must be xpath, method or fixed, and provided value was |"
                    + doa.getType() + "|.";
            log.error(logInitMsg + exceptionMessage);
            throw new EloraException(exceptionMessage);
        }
    }

    private static String stringifyObject(Object o) {
        if (o != null) {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof Boolean) {
                return o.toString();
            } else if (o instanceof Long) {
                int i = (int) (long) o;
                return String.valueOf(i);
            } else if (o instanceof Date) {
                Date d = (Date) o;
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(d);
                return formatGregorianCalendarToString(cal);
            } else if (o instanceof GregorianCalendar) {
                GregorianCalendar cal = (GregorianCalendar) o;
                return formatGregorianCalendarToString(cal);
            }
        }
        return null;
    }

    private static String formatGregorianCalendarToString(
            GregorianCalendar cal) {
        // -------------------------------------------------------------
        // NOTE: in the following lines, we are converting a Date object
        // into a String object in the following format:
        // 'yyyy-MM-dd HH:mm UTC'.
        // If this format is changed, please, look at
        // PostProcessingHelper.transformDateFromUtcToMad(String)
        // method, since this method take as parameter the String
        // resulting the following lines and maybe it should also be
        // changed.
        // -------------------------------------------------------------
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fmt.setCalendar(cal);
        return fmt.format(cal.getTime()) + " UTC";
    }

}
