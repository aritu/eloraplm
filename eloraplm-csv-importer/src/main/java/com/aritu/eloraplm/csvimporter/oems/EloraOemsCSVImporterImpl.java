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

package com.aritu.eloraplm.csvimporter.oems;

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

import com.aritu.eloraplm.csvimporter.EloraCSVImportLog;
import com.aritu.eloraplm.csvimporter.EloraCSVImportResult;
import com.aritu.eloraplm.csvimporter.EloraCSVImportStatus;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterOptions;

public class EloraOemsCSVImporterImpl implements EloraOemsCSVImporter {

    @Override
    public String launchImport(CoreSession session,
            DocumentModel currentDocument, File csvFile, String csvFileName,
            EloraCSVImporterOptions options) {
        EloraOemsCSVImporterWork work = new EloraOemsCSVImporterWork(
                session.getRepositoryName(), session.getPrincipal().getName(),
                csvFile, csvFileName, options);
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
                    EloraOemsCSVImporterWork.CATEGORY_CSV_IMPORTER);
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
    public List<EloraCSVImportLog> getLastImportLogs(String id, int max) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, null);
        if (work == null) {
            work = workManager.find(id, State.COMPLETED);
            if (work == null) {
                return Collections.emptyList();
            }
        }
        List<EloraCSVImportLog> importLogs = ((EloraOemsCSVImporterWork) work).getImportLogs();
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
    public EloraCSVImportResult getImportResult(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        List<EloraCSVImportLog> importLogs = ((EloraOemsCSVImporterWork) work).getImportLogs();
        return EloraCSVImportResult.fromImportLogs(importLogs);
    }

    @Override
    public DocumentModel getImportResultDoc(String id) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = workManager.find(id, State.COMPLETED);
        if (work == null) {
            return null;
        }

        DocumentModel resultDoc = ((EloraOemsCSVImporterWork) work).getResultDoc();
        return resultDoc;
    }

}
