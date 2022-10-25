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
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Scope(ScopeType.CONVERSATION)
@Name("eloraCsvImportActions")
@Install(precedence = Install.FRAMEWORK)
public class EloraCSVImportActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraCSVImportActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    // documents CSV file
    protected File csvFile;

    protected String csvFileName;

    // relations CSV file
    protected File relationsCsvFile;

    protected String relationsCsvFileName;

    // proxies CSV file
    protected File proxiesCsvFile;

    protected String proxiesCsvFileName;

    protected boolean notifyUserByEmail = true;

    protected String csvImportId;

    public boolean getNotifyUserByEmail() {
        return notifyUserByEmail;
    }

    public void setNotifyUserByEmail(boolean notifyUserByEmail) {
        this.notifyUserByEmail = notifyUserByEmail;
    }

    // This listener uploads the file containing the document descriptions
    public void uploadListener(FileUploadEvent event) throws Exception {
        UploadedFile item = event.getUploadedFile();
        // FIXME: check if this needs to be tracked for deletion
        csvFile = File.createTempFile("FileManageActionsFile", null);
        InputStream in = event.getUploadedFile().getInputStream();
        org.nuxeo.common.utils.FileUtils.copyToFile(in, csvFile);
        csvFileName = FilenameUtils.getName(item.getName());
    }

    // This listener uploads the file containing documents relations
    // descriptions
    public void relationsUploadListener(FileUploadEvent event)
            throws Exception {
        UploadedFile item = event.getUploadedFile();
        // FIXME: check if this needs to be tracked for deletion
        relationsCsvFile = File.createTempFile("FileManageActionsFile", null);
        InputStream in = event.getUploadedFile().getInputStream();
        org.nuxeo.common.utils.FileUtils.copyToFile(in, relationsCsvFile);
        relationsCsvFileName = FilenameUtils.getName(item.getName());
    }

    // This listener uploads the file containing documents proxies descriptions
    public void proxiesUploadListener(FileUploadEvent event) throws Exception {
        UploadedFile item = event.getUploadedFile();
        // FIXME: check if this needs to be tracked for deletion
        proxiesCsvFile = File.createTempFile("FileManageActionsFile", null);
        InputStream in = event.getUploadedFile().getInputStream();
        org.nuxeo.common.utils.FileUtils.copyToFile(in, proxiesCsvFile);
        proxiesCsvFileName = FilenameUtils.getName(item.getName());
    }

    public void importCSVFile() {
        String logInitMsg = "[importCSVFile] ";

        if (csvFile != null || relationsCsvFile != null
                || proxiesCsvFile != null) {
            EloraCSVImporterOptions options = new EloraCSVImporterOptions.Builder().sendEmail(
                    notifyUserByEmail).build();
            EloraCSVImporter csvImporter = Framework.getLocalService(
                    EloraCSVImporter.class);
            csvImportId = csvImporter.launchImport(documentManager,
                    navigationContext.getCurrentDocument().getPathAsString(),
                    csvFile, csvFileName, relationsCsvFile,
                    relationsCsvFileName, proxiesCsvFile, proxiesCsvFileName,
                    options);
        } else {
            log.info(logInitMsg
                    + "Nothing to do. csvfile, relationsCsvFile and proxiesCsvFile are null.");
        }
    }

    public String getImportingCSVFilename() {
        return csvFileName;
    }

    public EloraCSVImportStatus getImportStatus() {
        if (csvImportId == null) {
            return null;
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getImportStatus(csvImportId);
    }

    public List<EloraCSVImportLog> getLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getLastImportLogs(csvImportId, maxLogs);
    }

    public List<EloraCSVImportLog> getSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getImportLogs(csvImportId,
                EloraCSVImportLog.Status.SKIPPED,
                EloraCSVImportLog.Status.ERROR);
    }

    public List<EloraCSVImportLog> getRelationsLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getRelationsLastImportLogs(csvImportId, maxLogs);
    }

    public List<EloraCSVImportLog> getRelationsSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getRelationsImportLogs(csvImportId,
                EloraCSVImportLog.Status.SKIPPED,
                EloraCSVImportLog.Status.ERROR);
    }

    public List<EloraCSVImportLog> getProxiesLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getProxiesLastImportLogs(csvImportId, maxLogs);
    }

    public List<EloraCSVImportLog> getProxiesSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getProxiesImportLogs(csvImportId,
                EloraCSVImportLog.Status.SKIPPED,
                EloraCSVImportLog.Status.ERROR);
    }

    public EloraCSVImportResult getImportResult() {
        if (csvImportId == null) {
            return null;
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getImportResult(csvImportId);
    }

    public EloraCSVImportResult getRelationsImportResult() {
        if (csvImportId == null) {
            return null;
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getRelationsImportResult(csvImportId);
    }

    public EloraCSVImportResult getProxiesImportResult() {
        if (csvImportId == null) {
            return null;
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getProxiesImportResult(csvImportId);
    }

    public DocumentModel getImportResultDoc() {
        if (csvImportId == null) {
            return null;
        }
        EloraCSVImporter csvImporter = Framework.getLocalService(
                EloraCSVImporter.class);
        return csvImporter.getImportResultDoc(csvImportId);
    }

    @Observer(EventNames.NAVIGATE_TO_DOCUMENT)
    public void resetState() {
        csvFile = null;
        csvFileName = null;
        relationsCsvFile = null;
        relationsCsvFileName = null;
        proxiesCsvFile = null;
        proxiesCsvFileName = null;
        csvImportId = null;
        notifyUserByEmail = true;
    }
}
