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
package com.aritu.eloraplm.history.pageprovider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.elasticsearch.audit.pageprovider.ESAuditPageProvider;

import com.aritu.eloraplm.history.api.EloraCommentProcessorHelper;

/**
 * @author aritu
 *
 */
public class ESFullDocumentHistoryPageProvider extends ESAuditPageProvider {

    private static final long serialVersionUID = 1L;

    protected Log log = LogFactory.getLog(
            ESFullDocumentHistoryPageProvider.class);

    protected Object[] newParams;

    // protected static String fullQuery = "{\n" + " \"filtered\" : {\n"
    // + " \"query\" : {\n" + " \"match_all\" : { }\n"
    // + " },\n" + " \"filter\" : {\n"
    // + " \"terms\" : { \"docUUID\" : ? }\n" + " }\n"
    // + " }\n" + "}\n" + "\n" + "";

    // @Override
    // protected String getFixedPart() {
    // return fullQuery;
    // }

    @Override
    public List<SortInfo> getSortInfos() {

        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<SortInfo>();
            sort.add(new SortInfo("eventDate", true));
            sort.add(new SortInfo("id", true));
        }
        return sort;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] getParameters() {
        if (newParams == null) {
            Object[] params = super.getParameters();
            if (params.length != 1) {
                log.error(this.getClass().getSimpleName()
                        + " Expect only one parameter the document uuid, unexpected behavior may occur");
            }

            CoreSession session = (CoreSession) getProperties().get(
                    CORE_SESSION_PROPERTY);
            if (session == null) {
                log.warn(
                        "No core session found: cannot compute all info to get complete audit entries");
                return params;
            }

            List<String> uuidList = new ArrayList<String>();

            if (params[0] instanceof ArrayList<?>) {
                uuidList = (ArrayList<String>) params[0];
            } else {
                String uuid = params[0].toString();

                // Get the UUIDs of all AVs and add them with the WC
                uuidList.add("\"" + uuid + "\"");
                DocumentModel wcDoc = session.getDocument(new IdRef(uuid));
                List<DocumentRef> versionRefs = session.getVersionsRefs(
                        wcDoc.getRef());
                for (DocumentRef docRef : versionRefs) {
                    uuidList.add("\"" + docRef.toString() + "\"");
                }
            }

            newParams = new Object[] { uuidList };

        }
        return newParams;
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(this.parameters, parameters);
    }

    @Override
    protected void preprocessCommentsIfNeeded(List<LogEntry> entries) {
        Serializable preprocess = getProperties().get(UICOMMENTS_PROPERTY);

        if (preprocess != null
                && "true".equalsIgnoreCase(preprocess.toString())) {
            CoreSession session = getCoreSession();
            if (session != null) {
                EloraCommentProcessorHelper cph = new EloraCommentProcessorHelper(
                        session);
                cph.processComments(entries);
            }
        }
    }

}
