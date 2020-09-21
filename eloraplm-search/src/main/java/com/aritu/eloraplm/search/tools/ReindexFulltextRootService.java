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
package com.aritu.eloraplm.search.tools;

/**
 * // TODO: write class general comment
 *
 *
 *https://github.com/collectionspace/services/blob/master/services/common/src/main/java/org/collectionspace/services/nuxeo/util/ReindexFulltextRoot.java
 *
 * @author aritu
 *
 */
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLFulltextExtractorWork;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS component used to do fulltext reindexing of the whole database.
 *
 * @since 5.6
 */
@Path("reindexFulltextService")
public class ReindexFulltextRootService {

    public static Logger log = LoggerFactory.getLogger(
            ReindexFulltextRootService.class);

    protected static final String DC_TITLE = "dc:title";

    protected static final int DEFAULT_BATCH_SIZE = 100;

    @Context
    protected HttpServletRequest request;

    protected CoreSession coreSession;

    protected Session session;

    protected FulltextConfiguration fulltextConfiguration;

    public static class ReindexInfo {
        public final Serializable id;

        public final String type;

        public ReindexInfo(Serializable id, String type) {
            this.id = id;
            this.type = type;
        }
    }

    @GET
    public String get(@QueryParam("batchSize") int batchSize,
            @QueryParam("batch") int batch) throws NuxeoException {
        coreSession = SessionFactory.getSession(request);
        return reindexFulltext(batchSize, batch, null);
    }

    /**
     * Launches a fulltext reindexing of the database.
     *
     * @param batchSize the batch size, defaults to 100
     * @param batch if present, the batch number to process instead of all
     *            batches; starts at 1
     * @return when done, ok + the total number of docs
     * @throws StorageException
     */
    public String reindexFulltext(int batchSize, int batch, String query)
            throws NuxeoException {
        Principal principal = coreSession.getPrincipal();
        if (!(principal instanceof NuxeoPrincipal)) {
            return "unauthorized";
        }
        NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
        if (!nuxeoPrincipal.isAdministrator()) {
            return "unauthorized";
        }

        log("Reindexing starting");
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        //
        // A default query that gets ALL the documents
        //
        if (query == null) {
            query = "SELECT ecm:uuid, ecm:primaryType FROM Document"
                    + " WHERE ecm:isProxy = 0"
                    + " AND ecm:currentLifeCycleState <> 'deleted'"
                    + " ORDER BY ecm:uuid";
        }

        List<ReindexInfo> infos = getInfos(query);
        int size = infos.size();
        int numBatches = (size + batchSize - 1) / batchSize;
        if (batch < 0 || batch > numBatches) {
            batch = 0; // all
        }
        batch--;

        log("Reindexing of %s documents, batch size: %s, number of batches: %s",
                size, batchSize, numBatches);
        if (batch >= 0) {
            log("Reindexing limited to batch: %s", batch + 1);
        }

        //
        // Commit and close the transaction that was started by our standard
        // request lifecycle.
        //
        boolean tx = TransactionHelper.isTransactionActive();
        if (tx) {
            TransactionHelper.commitOrRollbackTransaction();
        }

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
            List<ReindexInfo> batchInfos = infos.subList(pos, end);
            log("Reindexing batch %s/%s, first id: %s", i + 1, numBatches,
                    batchInfos.get(0).id);
            try {
                doBatch(batchInfos);
            } catch (NuxeoException e) {
                log.error("Error processing batch " + i + 1, e);
                errs++;
            }
            n += end - pos;
        }

        log("Reindexing done");
        //
        // Start a new transaction so our standard request lifecycle can
        // complete.
        //
        if (tx) {
            TransactionHelper.startTransaction();
        }
        return "done: " + n + " total: " + size + " batch_errors: " + errs;
    }

    protected void log(String format, Object... args) {
        log.warn(String.format(format, args));
    }

    /**
     * This has to be called once the transaction has been started.
     *
     * @throws StorageException
     */
    protected void getLowLevelSession() throws NuxeoException {
        try {
            SQLSession s = (SQLSession) ((AbstractSession) coreSession).getSession();
            Field f2 = SQLSession.class.getDeclaredField("session");
            f2.setAccessible(true);
            session = (Session) f2.get(s);
            fulltextConfiguration = session.getModel().getFulltextConfiguration();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected List<ReindexInfo> getInfos(String query) throws NuxeoException {
        getLowLevelSession();
        List<ReindexInfo> infos = new ArrayList<ReindexInfo>();
        IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL,
                QueryFilter.EMPTY);
        try {
            for (Map<String, Serializable> map : it) {
                Serializable id = map.get(NXQL.ECM_UUID);
                String type = (String) map.get(NXQL.ECM_PRIMARYTYPE);
                infos.add(new ReindexInfo(id, type));
            }
        } finally {
            it.close();
        }
        return infos;
    }

    protected void doBatch(List<ReindexInfo> infos) throws NuxeoException {
        boolean tx;
        boolean ok;

        // transaction for the sync batch
        tx = TransactionHelper.startTransaction();

        getLowLevelSession(); // for fulltextInfo
        List<Serializable> ids = new ArrayList<Serializable>(infos.size());
        Set<String> asyncIds = new HashSet<String>();
        Model model = session.getModel();
        for (ReindexInfo info : infos) {
            ids.add(info.id);
            if (fulltextConfiguration.isFulltextIndexable(info.type)) {
                asyncIds.add(model.idToString(info.id));
            }
        }
        ok = false;
        try {
            runSyncBatch(ids, asyncIds);
            ok = true;
        } finally {
            if (tx) {
                if (!ok) {
                    TransactionHelper.setTransactionRollbackOnly();
                    log.error("Rolling back sync");
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }

        runAsyncBatch(asyncIds);

        // wait for async completion after transaction commit
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    /*
     * Do this at the low-level session level because we may have to modify
     * things like versions which aren't usually modifiable, and it's also good
     * to bypass all listeners.
     */
    protected void runSyncBatch(List<Serializable> ids, Set<String> asyncIds)
            throws NuxeoException {
        getLowLevelSession();

        session.getNodesByIds(ids); // batch fetch

        Map<Serializable, String> titles = new HashMap<Serializable, String>();
        for (Serializable id : ids) {
            Node node = session.getNodeById(id);
            if (asyncIds.contains(id)) {
                node.setSimpleProperty(Model.FULLTEXT_JOBID_PROP, id);
            }
            SimpleProperty prop;
            try {
                prop = node.getSimpleProperty(DC_TITLE);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String title = (String) prop.getValue();
            titles.put(id, title);
            prop.setValue(title + " ");
        }
        session.save();

        for (Serializable id : ids) {
            Node node = session.getNodeById(id);
            SimpleProperty prop;
            try {
                prop = node.getSimpleProperty(DC_TITLE);
            } catch (IllegalArgumentException e) {
                continue;
            }
            prop.setValue(titles.get(id));
        }
        session.save();
    }

    protected void runAsyncBatch(Set<String> asyncIds) {
        if (asyncIds.isEmpty()) {
            return;
        }
        String repositoryName = coreSession.getRepositoryName();
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        for (String id : asyncIds) {
            Work work = new SQLFulltextExtractorWork(repositoryName, id);
            // schedule immediately, we're outside a transaction
            workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, false);
        }
    }

}