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

package com.aritu.eloraplm.csvimporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
public class EloraCSVImporterImpl implements EloraCSVImporter {

    @Override
    public String launchImport(CoreSession session, String parentPath,
            File csvFile, String csvFileName, File relationsCsvFile,
            String relationsCsvFileName, File proxiesCsvFile,
            String proxiesCsvFileName, EloraCSVImporterOptions options) {
        EloraCSVImporterWork work = new EloraCSVImporterWork(
                session.getRepositoryName(), parentPath,
                session.getPrincipal().getName(), csvFile, csvFileName,
                relationsCsvFile, relationsCsvFileName, proxiesCsvFile,
                proxiesCsvFileName, options);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work,
                WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        return work.getId();
    }

    @Override
    public EloraCSVImportStatus getImportStatus(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        State state = workManager.getWorkState(id);
        if (state == null) {
            return null;
        } else if (state == State.COMPLETED) {
            return new EloraCSVImportStatus(
                    EloraCSVImportStatus.State.COMPLETED);
        } else if (state == State.SCHEDULED) {
            String queueId = workManager.getCategoryQueueId(
                    EloraCSVImporterWork.CATEGORY_CSV_IMPORTER);
            int queueSize = workManager.getQueueSize(queueId, State.SCHEDULED);
            return new EloraCSVImportStatus(
                    EloraCSVImportStatus.State.SCHEDULED, 0, queueSize);
        } else { // RUNNING
            return new EloraCSVImportStatus(EloraCSVImportStatus.State.RUNNING);
        }
    }

    @Override
    public List<EloraCSVImportLog> getImportLogs(String id) {
        return getLastImportLogs(id, -1);
    }

    @Override
    public List<EloraCSVImportLog> getImportLogs(String id,
            EloraCSVImportLog.Status... status) {
        return getLastImportLogs(id, -1, status);
    }

    @Override
    public List<EloraCSVImportLog> getRelationsImportLogs(String id) {
        return getRelationsLastImportLogs(id, -1);
    }

    @Override
    public List<EloraCSVImportLog> getRelationsImportLogs(String id,
            EloraCSVImportLog.Status... status) {
        return getRelationsLastImportLogs(id, -1, status);
    }

    @Override
    public List<EloraCSVImportLog> getProxiesImportLogs(String id) {
        return getProxiesLastImportLogs(id, -1);
    }

    @Override
    public List<EloraCSVImportLog> getProxiesImportLogs(String id,
            EloraCSVImportLog.Status... status) {
        return getProxiesLastImportLogs(id, -1, status);
    }

    @Override
    public List<EloraCSVImportLog> getLastImportLogs(String id, int max) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, null);
        if (work == null) {
            work = workManager.find(id, State.COMPLETED);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<EloraCSVImportLog> importLogs = ((EloraCSVImporterWork) work).getImportLogs();
        max = (max == -1 || max > importLogs.size()) ? importLogs.size() : max;
        return importLogs.subList(importLogs.size() - max, importLogs.size());
    }

    @Override
    public List<EloraCSVImportLog> getLastImportLogs(String id, int max,
            EloraCSVImportLog.Status... status) {
        List<EloraCSVImportLog> importLogs = getLastImportLogs(id, max);
        return status.length == 0 ? importLogs
                : filterImportLogs(importLogs, status);
    }

    protected List<EloraCSVImportLog> filterImportLogs(
            List<EloraCSVImportLog> importLogs,
            EloraCSVImportLog.Status... status) {
        List<EloraCSVImportLog.Status> statusList = Arrays.asList(status);
        List<EloraCSVImportLog> filteredLogs = new ArrayList<EloraCSVImportLog>();
        for (EloraCSVImportLog log : importLogs) {
            if (statusList.contains(log.getStatus())) {
                filteredLogs.add(log);
            }
        }
        return filteredLogs;
    }

    @Override
    public List<EloraCSVImportLog> getRelationsLastImportLogs(String id,
            int max) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, null);
        if (work == null) {
            work = workManager.find(id, State.COMPLETED);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<EloraCSVImportLog> relationsImportLogs = ((EloraCSVImporterWork) work).getRelationsImportLogs();
        max = (max == -1 || max > relationsImportLogs.size())
                ? relationsImportLogs.size()
                : max;
        return relationsImportLogs.subList(relationsImportLogs.size() - max,
                relationsImportLogs.size());
    }

    @Override
    public List<EloraCSVImportLog> getRelationsLastImportLogs(String id,
            int max, EloraCSVImportLog.Status... status) {
        List<EloraCSVImportLog> relationsImportLogs = getRelationsLastImportLogs(
                id, max);
        return status.length == 0 ? relationsImportLogs
                : filterImportLogs(relationsImportLogs, status);
    }

    @Override
    public List<EloraCSVImportLog> getProxiesLastImportLogs(String id,
            int max) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, null);
        if (work == null) {
            work = workManager.find(id, State.COMPLETED);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<EloraCSVImportLog> proxiesImportLogs = ((EloraCSVImporterWork) work).getProxiesImportLogs();
        max = (max == -1 || max > proxiesImportLogs.size())
                ? proxiesImportLogs.size()
                : max;
        return proxiesImportLogs.subList(proxiesImportLogs.size() - max,
                proxiesImportLogs.size());
    }

    @Override
    public List<EloraCSVImportLog> getProxiesLastImportLogs(String id, int max,
            EloraCSVImportLog.Status... status) {
        List<EloraCSVImportLog> proxiesImportLogs = getProxiesLastImportLogs(id,
                max);
        return status.length == 0 ? proxiesImportLogs
                : filterImportLogs(proxiesImportLogs, status);
    }

    @Override
    public EloraCSVImportResult getImportResult(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        List<EloraCSVImportLog> importLogs = ((EloraCSVImporterWork) work).getImportLogs();
        return EloraCSVImportResult.fromImportLogs(importLogs);
    }

    @Override
    public EloraCSVImportResult getRelationsImportResult(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        List<EloraCSVImportLog> relationsImportLogs = ((EloraCSVImporterWork) work).getRelationsImportLogs();
        return EloraCSVImportResult.fromImportLogs(relationsImportLogs);
    }

    @Override
    public EloraCSVImportResult getProxiesImportResult(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        List<EloraCSVImportLog> proxiesImportLogs = ((EloraCSVImporterWork) work).getProxiesImportLogs();
        return EloraCSVImportResult.fromImportLogs(proxiesImportLogs);
    }

    @Override
    public DocumentModel getImportResultDoc(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        DocumentModel resultDoc = ((EloraCSVImporterWork) work).getResultDoc();
        return resultDoc;
    }

}
