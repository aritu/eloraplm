/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package com.aritu.eloraplm.search.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLFulltextExtractorWork;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * JAX-RS component used to do fulltext reindexing of the whole database.
 *
 * ReindexFulltextRoot
 *
 * Check:
 * https://github.com/collectionspace/services/blob/master/services/common/src/main/java/org/collectionspace/services/nuxeo/util/ReindexFulltextRoot.java
 *
 * @since 5.6
 */
@Path("reindexFulltext")
public class ReindexFulltextRoot {

    private static Log log = LogFactory.getLog(ReindexFulltextRoot.class);

    protected static final int DEFAULT_BATCH_SIZE = 100;

    @Context
    protected HttpServletRequest request;

    protected CoreSession session;

    protected SessionImpl sessionImpl;

    protected FulltextConfiguration fulltextConfiguration;

    protected String repositoryName;

    @GET
    public String get(@QueryParam("batchSize") int batchSize,
            @QueryParam("batch") int batch) {

        session = SessionFactory.getSession(request);
        return reindexFulltext(batchSize, batch);
    }

    /**
     * Launches a fulltext reindexing of the database.
     *
     * @param batchSize the batch size, defaults to 100
     * @param batch if present, the batch number to process instead of all
     *            batches; starts at 1
     * @return when done, ok + the total number of docs
     */
    public String reindexFulltext(int batchSize, int batch) {

        String logInitMsg = "[reindexFulltext] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (!((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            log.error(logInitMsg + "unauthorized user");
            return "unauthorized";
        }

        repositoryName = session.getRepositoryName();

        SQLRepositoryService sqlRepositoryService = Framework.getService(
                SQLRepositoryService.class);
        fulltextConfiguration = sqlRepositoryService.getFulltextConfiguration(
                repositoryName);
        RepositoryImpl repositoryImpl = sqlRepositoryService.getRepositoryImpl(
                repositoryName);
        sessionImpl = repositoryImpl.getConnection();

        log.trace(logInitMsg + "Reindexing starting");

        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        List<String> allIds = getAllIds();
        int size = allIds.size();
        int numBatches = (size + batchSize - 1) / batchSize;
        if (batch < 0 || batch > numBatches) {
            batch = 0; // all
        }
        batch--;

        log(logInitMsg
                + "Reindexing of %s documents, batch size: %s, number of batches: %s",
                size, batchSize, numBatches);
        if (batch >= 0) {
            log(logInitMsg + "Reindexing limited to batch: %s", batch + 1);
        }

        boolean tx = TransactionHelper.isTransactionActiveOrMarkedRollback();
        if (tx) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        EventService eventService = Framework.getService(EventService.class);

        int n = 0;
        int errs = 0;
        for (int i = 0; i < numBatches; i++) {
            if (batch >= 0 && batch != i) {
                continue;
            }
            int pos = i * batchSize;
            int end = pos + batchSize;
            if (end > size) {
                end = size;
            }
            List<String> batchIds = allIds.subList(pos, end);
            log(logInitMsg + "Reindexing batch %s/%s, first id: %s", i + 1,
                    numBatches, batchIds.get(0));
            try {
                scheduleBatch(batchIds, sessionImpl);
                eventService.waitForAsyncCompletion();
            } catch (NuxeoException e) {
                log.error(logInitMsg + "Error processing batch " + i + 1, e);
                errs++;
            }
            n += end - pos;
        }

        log.trace(logInitMsg + "Reindexing done");
        if (tx) {
            TransactionHelper.startTransaction();
        }

        String resultMsg = "Fulltext reindexation done: " + n + " total: "
                + size + " batch_errors: " + errs;

        log.trace(logInitMsg + "--- EXIT with resultMsg = |" + resultMsg
                + "--- ");

        return resultMsg;
    }

    protected void log(String format, Object... args) {
        log.warn(String.format(format, args));
    }

    // TODO doesn't scale
    protected List<String> getAllIds() {

        String logInitMsg = "[getAllIds] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<String> ids = new ArrayList<>();

        String query = "SELECT ecm:uuid, ecm:primaryType FROM Document WHERE ecm:isProxy = 0 "
                + " AND ecm:currentLifeCycleState <> 'deleted' ORDER BY ecm:uuid";

        IterableQueryResult it = null;
        try {
            it = session.queryAndFetch(query, NXQL.NXQL);

            for (Map<String, Serializable> map : it) {
                String type = (String) map.get(NXQL.ECM_PRIMARYTYPE);
                if (fulltextConfiguration.isFulltextIndexable(type)) {
                    ids.add((String) map.get(NXQL.ECM_UUID));
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + "Exception thrown: |" + e.getMessage() + "|",
                    e);
        } finally {
            it.close();
        }
        log.trace(logInitMsg + "--- EXIT with |" + ids.size() + "| ids --- ");
        return ids;
    }

    protected void scheduleBatch(List<String> ids, SessionImpl sessionImpl) {

        String logInitMsg = "[scheduleBatch] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // ################################################################
        // Inspired in the code found at: SessionImpl class, method
        // getFulltextBinariesWorks
        // ################################################################

        // Iterate over the nodes related to each document and filter the nodes
        // that are fulltext indexable
        for (Node node : sessionImpl.getNodesByIds(
                new ArrayList<Serializable>(ids))) {

            /*if (!sessionImpl.getModel().getFulltextConfiguration().isFulltextIndexable(
                    node.getPrimaryType())) {
                continue;
            }*/

            if (!fulltextConfiguration.isFulltextIndexable(
                    node.getPrimaryType())) {
                continue;
            }

            // Otherwise store the fulltextJobId property
            node.getSimpleProperty(Model.FULLTEXT_JOBID_PROP).setValue(
                    sessionImpl.getModel().idToString(node.getId()));
        }

        // Save the session
        sessionImpl.save();

        // Schedule Fulltext Extractor Works
        WorkManager workManager = Framework.getService(WorkManager.class);
        for (String id : ids) {
            /* Work work = new FulltextExtractorWork(repositoryName, id, true,
                    true, false); */
            Work work = new SQLFulltextExtractorWork(repositoryName, id);

            // no job id
            // schedule immediately, we're outside a transaction
            workManager.schedule(work, true);

            // workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, true);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

}
