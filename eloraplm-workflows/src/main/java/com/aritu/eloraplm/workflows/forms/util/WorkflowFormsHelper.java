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
package com.aritu.eloraplm.workflows.forms.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.WorkflowMetadataConstants;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.webapp.util.LocalizedLabel;
import com.aritu.eloraplm.webapp.util.LocalizedLabelHelper;
import com.aritu.eloraplm.workflows.forms.api.WorkflowFormsService;

/**
 *
 * @author aritu
 *
 */
public class WorkflowFormsHelper {

    public static final String WORKFLOW_LABEL_PREFIX = "eloraplm.wf.label.";

    private static final Log log = LogFactory.getLog(WorkflowFormsHelper.class);

    public static Map<String, List<String>> getDistinctAnsweredWorkflowAndInstanceIdsForDoc(
            CoreSession session, DocumentModel doc) {

        return new UnrestrictedSessionRunner("default") {
            Map<String, List<String>> answeredWorkflowsAndInstances = new HashMap<String, List<String>>();

            @Override
            public void run() {

                IterableQueryResult it = null;
                try {
                    String q = WorkflowFormsQueryFactory.getDistinctAnsweredWorkflowInstancesForDoc(
                            doc.getId());
                    it = session.queryAndFetch(q, NXQL.NXQL);
                    if (it.size() > 0) {
                        String pfx = WorkflowMetadataConstants.WFANS_ANSWERS
                                + "/*1/";

                        for (Map<String, Serializable> map : it) {

                            String ansWfId = (String) map.get(pfx
                                    + WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_ID);
                            String ansInstanceId = (String) map.get(pfx
                                    + WorkflowMetadataConstants.WFANS_ANSWERS_WORKFLOW_INSTANCE_ID);
                            if (ansWfId != null && ansInstanceId != null) {

                                if (!answeredWorkflowsAndInstances.containsKey(
                                        ansWfId)) {
                                    answeredWorkflowsAndInstances.put(ansWfId,
                                            new ArrayList<String>());
                                }
                                if (!answeredWorkflowsAndInstances.get(
                                        ansWfId).contains(ansInstanceId)) {
                                    answeredWorkflowsAndInstances.get(
                                            ansWfId).add(ansInstanceId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("ERROR: " + e.getMessage());
                    // We only can throw a runtime exception here, like
                    // NuxeoException
                    throw new NuxeoException(e.getMessage());
                } finally {
                    if (it != null) {
                        it.close();
                    }
                }
            }

            public Map<String, List<String>> runAndReturnList() {
                runUnrestricted();
                return answeredWorkflowsAndInstances;
            }
        }.runAndReturnList();
    }

    public static String getDisplayLabel(String locale, String questionId,
            List<LocalizedLabel> labels) {
        String displayLabel = "";

        if (locale == null) {
            locale = LocalizedLabelHelper.getDefaultLocale();
        }

        if (labels != null) {
            String twoDigitLocale = locale.substring(0, 2);
            displayLabel = LocalizedLabelHelper.getLocalizedLabel(labels,
                    twoDigitLocale, questionId);

        } else {

            if (questionId.equals(
                    WorkflowMetadataConstants.FIXED_QUESTION_ID_COMMENT)
                    || questionId.equals(
                            WorkflowMetadataConstants.FIXED_QUESTION_ID_ACTION)
                    || questionId.startsWith(
                            WorkflowFormsService.NODE_VARS_WFANS_PREFIX)) {
                displayLabel = EloraMessageHelper.getTranslatedMessage(locale,
                        WORKFLOW_LABEL_PREFIX + questionId);
            }
        }

        return displayLabel;
    }

    public static String getDisplayAnswer(String locale, Object answer) {
        // 1. Tratatu iruzkina eta ekintza
        // 2. Tratatu mota desberdinak

        return "";
    }
}
