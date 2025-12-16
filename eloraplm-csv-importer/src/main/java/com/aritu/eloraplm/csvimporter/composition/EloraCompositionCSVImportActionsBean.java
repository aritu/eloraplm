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

package com.aritu.eloraplm.csvimporter.composition;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.csvimporter.EloraCSVImportLog;
import com.aritu.eloraplm.csvimporter.EloraCSVImportResult;
import com.aritu.eloraplm.csvimporter.EloraCSVImportStatus;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterOptions;

@Scope(ScopeType.CONVERSATION)
@Name("eloraCompositionCsvImportActions")
@Install(precedence = Install.FRAMEWORK)
public class EloraCompositionCSVImportActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraCompositionCSVImportActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    // Composition CSV file
    protected File csvFile;

    protected String csvFileName;

    protected String csvImportId;

    protected boolean isElectronicEnabled = false;

    @Create
    public void initialize() {
        log.debug("Initializing...");

        isElectronicEnabled = Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_ELECTRONIC_ENABLED,
                Boolean.toString(false)));

        log.debug("isElectronicEnabled initalized to |" + isElectronicEnabled
                + "|");
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

    public void importCompositionCSVFile() {
        String logInitMsg = "[importCompositionCSVFile] ";

        log.debug(logInitMsg + "Importing Composition CSV file.");

        if (csvFile != null) {
            EloraCSVImporterOptions options = new EloraCSVImporterOptions.Builder().build();
            EloraCompositionCSVImporter csvCompositionImporter = Framework.getLocalService(
                    EloraCompositionCSVImporter.class);
            csvImportId = csvCompositionImporter.launchImport(documentManager,
                    navigationContext.getCurrentDocument(), csvFile,
                    csvFileName, options);
        } else {
            log.info(logInitMsg + "Nothing to do. csvfile is null.");
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("eloraplm.label.csv.import.emptyCsvFile"));
        }
    }

    public String getImportingCSVFilename() {
        return csvFileName;
    }

    public EloraCSVImportStatus getImportStatus() {
        if (csvImportId == null) {
            return null;
        }
        EloraCompositionCSVImporter csvImporter = Framework.getLocalService(
                EloraCompositionCSVImporter.class);
        return csvImporter.getImportStatus(csvImportId);
    }

    public List<EloraCSVImportLog> getLastLogs(int maxLogs) {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCompositionCSVImporter csvImporter = Framework.getLocalService(
                EloraCompositionCSVImporter.class);
        return csvImporter.getLastImportLogs(csvImportId, maxLogs);
    }

    public List<EloraCSVImportLog> getSkippedAndErrorLogs() {
        if (csvImportId == null) {
            return Collections.emptyList();
        }
        EloraCompositionCSVImporter csvImporter = Framework.getLocalService(
                EloraCompositionCSVImporter.class);
        return csvImporter.getImportLogs(csvImportId,
                EloraCSVImportLog.Status.SKIPPED,
                EloraCSVImportLog.Status.ERROR);
    }

    public EloraCSVImportResult getImportResult() {
        if (csvImportId == null) {
            return null;
        }
        EloraCompositionCSVImporter csvImporter = Framework.getLocalService(
                EloraCompositionCSVImporter.class);
        return csvImporter.getImportResult(csvImportId);
    }

    public DocumentModel getImportResultDoc() {
        if (csvImportId == null) {
            return null;
        }
        EloraCompositionCSVImporter csvImporter = Framework.getLocalService(
                EloraCompositionCSVImporter.class);
        return csvImporter.getImportResultDoc(csvImportId);
    }

    @Observer(EventNames.NAVIGATE_TO_DOCUMENT)
    public void resetState() {
        csvFile = null;
        csvFileName = null;
        csvImportId = null;
    }

    public boolean getIsElectronicEnabled() {
        return isElectronicEnabled;
    }
}
