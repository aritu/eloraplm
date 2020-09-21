/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package com.aritu.eloraplm.core.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.listener.StateLogListener;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for managing StateLog metadata.
 *
 *
 * @author aritu
 *
 */
public class StateLogHelper {

    private static final Log log = LogFactory.getLog(StateLogListener.class);

    @SuppressWarnings("unchecked")
    public static void addStateLogProperties(DocumentModel doc, String user,
            String stateFrom, String stateTo, String transition,
            String versionDocId, String comment) {

        if (!doc.hasFacet(EloraFacetConstants.FACET_STORE_STATES_LOG)) {
            // nothing to do
            return;
        }

        ArrayList<Map<String, Object>> currentStateLogList = new ArrayList<Map<String, Object>>();

        if (doc.getPropertyValue(
                EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST) != null) {
            currentStateLogList = (ArrayList<Map<String, Object>>) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST);
        }

        StateLog stateLog = new StateLog(user, new Date(), stateFrom, stateTo,
                transition, versionDocId, comment);

        Map<String, Object> stateLogType = createStateLogType(stateLog);

        currentStateLogList.add(stateLogType);

        doc.setPropertyValue(EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST,
                currentStateLogList);

    }

    public static void addStateLogProperties(DocumentModel doc, String user,
            String stateFrom, String stateTo, String transition,
            String versionDocId) {
        addStateLogProperties(doc, user, stateFrom, stateTo, transition,
                versionDocId, null);
    }

    private static Map<String, Object> createStateLogType(StateLog stateLog) {

        Map<String, Object> stateLogType = new HashMap<>();

        stateLogType.put("user", stateLog.getUser());
        stateLogType.put("date", stateLog.getDate());
        stateLogType.put("state_from", stateLog.getStateFrom());
        stateLogType.put("state_to", stateLog.getStateTo());
        stateLogType.put("transition", stateLog.getTransition());
        stateLogType.put("versionDocId", stateLog.getVersionDocId());
        stateLogType.put("comment", stateLog.getComment());

        return stateLogType;
    }

    public static StateLog createStateLog(HashMap<String, Object> stateLogType)
            throws EloraException {

        String user = (String) stateLogType.get("user");
        Date date = null;
        GregorianCalendar dateCalendar = (GregorianCalendar) stateLogType.get(
                "date");
        if (dateCalendar != null) {
            // Taken from Nuxeo code:
            // -------------------------------------------------
            // remove milliseconds as they are not stored in some
            // databases, which could make the comparison fail just
            // after a document creation (see NXP-8783)
            // -------------------------------------------------
            dateCalendar.set(Calendar.MILLISECOND, 0);
            date = dateCalendar.getTime();
        }

        String stateFrom = (String) stateLogType.get("state_from");
        String stateTo = (String) stateLogType.get("state_to");
        String transition = (String) stateLogType.get("transition");
        String versionDocId = (String) stateLogType.get("versionDocId");
        String comment = (String) stateLogType.get("comment");

        StateLog stateLog = new StateLog(user, date, stateFrom, stateTo,
                transition, versionDocId, comment);

        return stateLog;
    }

    /**
     * This method verifies if the versionId of the last logged StateLog entry
     * is a version or not. If it is not a version, it updates with the
     * specified version value. If it is a version, there is nothing to do.
     *
     * @param versionDoc
     */
    @SuppressWarnings("unchecked")
    public static boolean updateVersionDocIdInStateLogIfRequired(
            DocumentModel versionDoc, CoreSession session) {

        String logInitMsg = "[updateVersionDocIdInStateLogIfRequired] ["
                + session.getPrincipal().getName() + "] ";

        boolean updated = false;

        if (!versionDoc.hasFacet(EloraFacetConstants.FACET_STORE_STATES_LOG)) {
            // nothing to do
            return updated;
        }

        ArrayList<Map<String, Object>> currentStateLogList = new ArrayList<Map<String, Object>>();

        if (versionDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST) != null) {
            currentStateLogList = (ArrayList<Map<String, Object>>) versionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST);
        }

        // Retrieve the versionId of the last StateLog entry
        if (currentStateLogList.size() > 0) {
            Map<String, Object> stateLogType = currentStateLogList.get(
                    currentStateLogList.size() - 1);

            String storedVersionId = (String) stateLogType.get("versionDocId");

            DocumentModel storedDoc = session.getDocument(
                    new IdRef(storedVersionId));

            // If it is not a version, update it with the versionId
            if (storedDoc != null && !storedDoc.isVersion()) {
                String versionDocId = versionDoc.getId();
                stateLogType.put("versionDocId", versionDocId);
                versionDoc.setPropertyValue(
                        EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST,
                        currentStateLogList);
                updated = true;
                log.trace(logInitMsg + "versionDocId = |" + versionDocId
                        + "| updated in StateLog entry.");
            }
        }
        return updated;
    }

    /**
     * This method verifies if the versionId of the last logged StateLog entry
     * is a version or not. If it is not a version, it removes the last StateLog
     * entry.
     *
     * @param doc
     * @param session
     * @return
     */
    @SuppressWarnings("unchecked")
    public static boolean removeLastStateInStateLogIfRequired(DocumentModel doc,
            CoreSession session) {

        String logInitMsg = "[removeLastStateInStateLog] ["
                + session.getPrincipal().getName() + "] ";

        boolean updated = false;

        if (!doc.hasFacet(EloraFacetConstants.FACET_STORE_STATES_LOG)) {
            // nothing to do
            return updated;
        }

        ArrayList<Map<String, Object>> currentStateLogList = new ArrayList<Map<String, Object>>();

        if (doc.getPropertyValue(
                EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST) != null) {
            currentStateLogList = (ArrayList<Map<String, Object>>) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST);
        }

        // Retrieve the versionId of the last StateLog entry
        if (currentStateLogList.size() > 0) {
            Map<String, Object> stateLogType = currentStateLogList.get(
                    currentStateLogList.size() - 1);

            String storedVersionId = (String) stateLogType.get("versionDocId");

            DocumentModel storedDoc = session.getDocument(
                    new IdRef(storedVersionId));

            // If it is not a version, remove the last entry
            if (storedDoc != null && !storedDoc.isVersion()) {
                currentStateLogList.remove(currentStateLogList.size() - 1);
                doc.setPropertyValue(
                        EloraMetadataConstants.ELORA_STLOG_STATE_LOG_LIST,
                        currentStateLogList);
                updated = true;
                log.trace(logInitMsg + "last StateLog entry removed.");
            }
        }
        return updated;
    }

}
