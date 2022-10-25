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
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface EloraCSVImporter {

    String launchImport(CoreSession session, String parentPath, File csvFile,
            String csvFileName, File relationsCsvFile,
            String relationsCsvFileName, File proxiesCsvFile,
            String proxiesCsvFileName, EloraCSVImporterOptions options);

    EloraCSVImportStatus getImportStatus(String id);

    List<EloraCSVImportLog> getImportLogs(String id);

    List<EloraCSVImportLog> getImportLogs(String id,
            EloraCSVImportLog.Status... status);

    List<EloraCSVImportLog> getLastImportLogs(String id, int max);

    List<EloraCSVImportLog> getLastImportLogs(String id, int max,
            EloraCSVImportLog.Status... status);

    List<EloraCSVImportLog> getRelationsImportLogs(String id);

    List<EloraCSVImportLog> getRelationsImportLogs(String id,
            EloraCSVImportLog.Status... status);

    List<EloraCSVImportLog> getRelationsLastImportLogs(String id, int max);

    List<EloraCSVImportLog> getRelationsLastImportLogs(String id, int max,
            EloraCSVImportLog.Status... status);

    List<EloraCSVImportLog> getProxiesImportLogs(String id);

    List<EloraCSVImportLog> getProxiesImportLogs(String id,
            EloraCSVImportLog.Status... status);

    List<EloraCSVImportLog> getProxiesLastImportLogs(String id, int max);

    List<EloraCSVImportLog> getProxiesLastImportLogs(String id, int max,
            EloraCSVImportLog.Status... status);

    EloraCSVImportResult getImportResult(String id);

    EloraCSVImportResult getRelationsImportResult(String id);

    EloraCSVImportResult getProxiesImportResult(String id);

    DocumentModel getImportResultDoc(String id);
}
