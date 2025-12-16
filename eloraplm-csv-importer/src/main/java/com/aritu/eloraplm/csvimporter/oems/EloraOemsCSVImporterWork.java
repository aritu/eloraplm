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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.AbstractWork;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.OEMConstants;
import com.aritu.eloraplm.bom.util.BomHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.csvimporter.EloraCSVImportLog;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterOptions;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterWork;
import com.aritu.eloraplm.exceptions.EloraException;

public class EloraOemsCSVImporterWork extends AbstractWork {

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_REFERENCE = "eloraplm.label.csv.importer.notExistingReference";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_HAS_NOT_OEM_SCHEMA = "eloraplm.label.csv.importer.documentHasNotOemSchema";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_IS_LOCKED = "eloraplm.label.csv.importer.documentIsLocked";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_IS_CHECKED_OUT = "eloraplm.label.csv.importer.documentIsCheckedOut";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_CONTAINS_MANUFACTURER_AND_PART_NUMBER = "eloraplm.label.csv.importer.documentContainsManufacturerAndPartNumber";

    public static final String LABEL_CSV_IMPORTER_MISSING_OEMS_COLUMNS = "eloraplm.label.csv.importer.missingOemsColumns";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_ADD_OEM = "eloraplm.label.csv.importer.unableToAddOem";

    public static final String LABEL_CSV_IMPORTER_OEM_ADDED = "eloraplm.label.csv.importer.oemAdded";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraOemsCSVImporterWork.class);

    public static final String CATEGORY_CSV_IMPORTER = "eloraOemsCsvImporter";

    /**
     * CSV headers that won't be checked if the field exists on the document
     * type.
     *
     * @since 7.3
     */
    public static List<String> AUTHORIZED_HEADERS = Arrays.asList(
            NXQL.ECM_LIFECYCLESTATE);

    protected String parentPath;

    protected String username;

    protected File csvFile;

    protected String csvFileName;

    protected EloraCSVImporterOptions options;

    protected transient DateFormat dateformat;

    protected Date startDate;

    protected List<EloraCSVImportLog> importLogs = new ArrayList<>();

    protected DocumentModel resultDoc;

    public EloraOemsCSVImporterWork(String id) {
        super(id);
    }

    public EloraOemsCSVImporterWork(String repositoryName, String username,
            File csvFile, String csvFileName, EloraCSVImporterOptions options) {
        super(EloraOemsCSVImportId.create(repositoryName, csvFile));
        setDocument(repositoryName, null);
        setOriginatingUsername(username);
        this.username = username;
        this.csvFile = csvFile;
        this.csvFileName = csvFileName;
        this.options = options;
        startDate = new Date();
    }

    @Override
    public String getCategory() {
        return CATEGORY_CSV_IMPORTER;
    }

    @Override
    public String getTitle() {
        return String.format("CSV import in '%s'", parentPath);
    }

    public List<EloraCSVImportLog> getImportLogs() {
        return new ArrayList<>(importLogs);
    }

    public DocumentModel getResultDoc() {
        return resultDoc;
    }

    @Override
    public void work() {
        setStatus("Importing OEMs");
        openUserSession();

        try {

            Reader inDocuments = null;
            if (csvFile != null) {
                inDocuments = newReader(csvFile);

                CSVParser parser = CSVFormat.DEFAULT.withEscape(
                        options.getEscapeCharacter()).withHeader().parse(
                                inDocuments);

                doImport(parser);
            }
        } catch (IOException e) {
            logError(importLogs, 0, "Error while doing the import: %s",
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT,
                    e.getMessage());
            log.error(e, e);
        }

        setStatus(null);
    }

    /**
     * @since 7.3
     */
    protected BufferedReader newReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(
                new BOMInputStream(new FileInputStream(file))));
    }

    protected void doImport(CSVParser parser) throws IOException {
        log.info(String.format("Importing CSV file: %s", csvFileName));
        Map<String, Integer> header = parser.getHeaderMap();
        if (header == null || header.size() == 0) {
            String errorMsg = "No header line or empty file.";
            log.error(errorMsg);
            logError(importLogs, 0, errorMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }

        String[] mandatoryColumns = {
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_REFERENCE_COL,
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_MANUFACTURER_COL,
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PARTNUMBER_COL };
        String missingColumns = "";

        for (String mandatoryColumn : mandatoryColumns) {
            if (!header.containsKey(mandatoryColumn)) {
                if (!missingColumns.isEmpty()) {
                    missingColumns += ", ";
                }
                missingColumns += mandatoryColumn;
            }
        }
        if (!missingColumns.isEmpty()) {
            String errorMsg = "Missing " + missingColumns + " columns.";
            log.error(errorMsg);
            logError(importLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_MISSING_OEMS_COLUMNS, missingColumns);
            return;
        }
        try {
            int batchSize = options.getBatchSize();
            long docsCreatedCount = 0;

            List<CSVRecord> recordsList = parser.getRecords();

            if (recordsList == null || recordsList.size() == 0) {
                String errorMsg = "No content on file.";
                log.error(errorMsg);
                logError(importLogs, 0, errorMsg,
                        EloraCSVImporterWork.LABEL_CSV_IMPORTER_EMPTY_LINE);
                return;
            }

            // Import OEMs
            for (int i = 0; i < recordsList.size(); i++) {
                CSVRecord record = recordsList.get(i);

                if (record.size() == 0) {
                    // empty record
                    String errorMsg = "Error at line |"
                            + record.getRecordNumber() + "|: Empty record";
                    log.error(errorMsg);
                    importLogs.add(new EloraCSVImportLog(
                            record.getRecordNumber(),
                            EloraCSVImportLog.Status.SKIPPED, "Empty record",
                            EloraCSVImporterWork.LABEL_CSV_IMPORTER_EMPTY_LINE));
                    continue;
                }
                try {
                    String resultMsg = importRecord(record, header);
                    if (StringUtils.isBlank(resultMsg)) {
                        docsCreatedCount++;
                        if (docsCreatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                        String infoMsg = "Record at line |"
                                + record.getRecordNumber()
                                + "| successfully imported.";
                        log.info(infoMsg);
                    } else {
                        String errorMsg = "Error at line |"
                                + record.getRecordNumber() + "|: " + resultMsg;
                        log.error(errorMsg);
                    }

                } catch (Exception e) {
                    // try next line
                    // Throwable unwrappedException = unwrapException(e);
                    logError(importLogs, parser.getRecordNumber(),
                            "Error while importing line: %s",
                            EloraCSVImporterWork.LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            e.getMessage());
                    log.error(e.getMessage(), e);
                }
            }

            try {
                session.save();
            } catch (NuxeoException e) {
                Throwable ue = unwrapException(e);
                logError(importLogs, parser.getRecordNumber(),
                        "Unable to save: %s",
                        EloraCSVImporterWork.LABEL_CSV_IMPORTER_UNABLE_TO_SAVE,
                        ue.getMessage());
                String errorMsg = "Unable to save. Error message: |"
                        + ue.getMessage() + "|";
                log.error(errorMsg, ue);
            }

        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing CSV file: %s", csvFileName));
    }

    /**
     * Import a line from the CSV file.
     *
     * @return {@code true} if a document has been created or updated,
     *         {@code false} otherwise.
     * @throws EloraException
     * @since 6.0
     */
    protected String importRecord(CSVRecord record, Map<String, Integer> header)
            throws EloraException {

        String resultMsg = null;

        final String reference = record.get(
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_REFERENCE_COL);
        final String manufacturer = record.get(
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_MANUFACTURER_COL);
        final String partNumber = record.get(
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PARTNUMBER_COL);

        String preference = null;
        if (header.containsKey(
                EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PREFERENCE_COL)) {
            preference = record.get(
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PREFERENCE_COL);
        }

        if (StringUtils.isBlank(reference)) {
            resultMsg = "Missing "
                    + EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PREFERENCE_COL
                    + " value";
            log.error("record.isSet=" + record.isSet(
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PREFERENCE_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_MISSING_COLUMN_VALUE,
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PREFERENCE_COL);
            return resultMsg;
        }
        if (StringUtils.isBlank(manufacturer)) {
            resultMsg = "Missing "
                    + EloraOemsCSVImportConstants.CSV_IMPORT_OEM_MANUFACTURER_COL
                    + " value";
            log.error("record.isSet=" + record.isSet(
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_MANUFACTURER_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_MISSING_COLUMN_VALUE,
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_MANUFACTURER_COL);
            return resultMsg;
        }

        if (StringUtils.isBlank(partNumber)) {
            resultMsg = "Missing "
                    + EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PARTNUMBER_COL
                    + " value";
            log.error("record.isSet=" + record.isSet(
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PARTNUMBER_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_MISSING_COLUMN_VALUE,
                    EloraOemsCSVImportConstants.CSV_IMPORT_OEM_PARTNUMBER_COL);
            return resultMsg;
        }

        DocumentModel documentWc = null;

        // For the instance, only ElectronicComponent items
        documentWc = EloraDocumentHelper.getWcDocumentByTypeAndReference(
                session, reference, EloraDoctypeConstants.ELECTRONIC_COMPONENT);

        if (documentWc == null) {
            resultMsg = "The reference |" + reference + "| does not exist";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_NOT_EXISTING_REFERENCE, reference);
            return resultMsg;
        }

        if (documentWc.hasSchema(EloraSchemaConstants.RELATED_OEM)) {

            if (!documentWc.isCheckedOut()) {
                if (!documentWc.isLocked()) {

                    boolean existManufacturerAndPartNumberInOemList = BomHelper.existManufacturerAndPartNumberInOemList(
                            documentWc, manufacturer, partNumber);

                    if (!existManufacturerAndPartNumberInOemList) {
                        return addOem(record.getRecordNumber(), documentWc,
                                manufacturer, preference, partNumber);
                    } else {
                        resultMsg = "Document having reference |" + reference
                                + "| has already manufacturer '" + manufacturer
                                + "' in the OEM list.";
                        logError(importLogs, record.getRecordNumber(),
                                resultMsg,
                                LABEL_CSV_IMPORTER_DOCUMENT_CONTAINS_MANUFACTURER_AND_PART_NUMBER,
                                reference);
                        return resultMsg;
                    }

                } else {
                    resultMsg = "Document having reference |" + reference
                            + "| is locked by '"
                            + documentWc.getLockInfo().getOwner()
                            + "'. Please, unlock the document first.";
                    logError(importLogs, record.getRecordNumber(), resultMsg,
                            LABEL_CSV_IMPORTER_DOCUMENT_IS_LOCKED, reference);
                    return resultMsg;
                }
            } else {
                resultMsg = "Document having reference |" + reference
                        + "| is checked out. It cannot be checked out in order to import OEMs.";
                logError(importLogs, record.getRecordNumber(), resultMsg,
                        LABEL_CSV_IMPORTER_DOCUMENT_IS_CHECKED_OUT, reference);
                return resultMsg;
            }

        } else {
            resultMsg = "Document having reference |" + reference
                    + "| does not have '" + EloraSchemaConstants.RELATED_OEM
                    + "' schema.";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_DOCUMENT_HAS_NOT_OEM_SCHEMA, reference);
            return resultMsg;
        }

    }

    @SuppressWarnings("unchecked")
    protected String addOem(long lineNumber, DocumentModel sourceDoc,
            String manufacturer, String preference, String partNumber) {

        String resultMsg = null;
        try {
            List<HashMap<String, Object>> oemsList = (ArrayList<HashMap<String, Object>>) sourceDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_OEM_OEMS);

            // If preference is null, calculate preference based on the maximum
            // preference number stored in the OEMs list.
            if (preference == null) {
                long maxPreference = BomHelper.getMaxPreferenceInOemsList(
                        sourceDoc);
                preference = String.valueOf(maxPreference + 1);
            }

            HashMap<String, Object> itemOem = new HashMap<String, Object>();

            itemOem.put(OEMConstants.MANUFACTURER, manufacturer);
            itemOem.put(OEMConstants.PREFERENCE, preference);
            itemOem.put(OEMConstants.PART_NUMBER, partNumber);
            oemsList.add(itemOem);

            CoreSession documentManager = sourceDoc.getCoreSession();

            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
                    sourceDoc);
            if (baseDoc == null) {
                throw new EloraException("The document |" + sourceDoc.getId()
                        + "| has no base version.");
            }

            EloraDocumentHelper.disableVersioningDocument(baseDoc);
            baseDoc.setPropertyValue(EloraMetadataConstants.ELORA_OEM_OEMS,
                    (Serializable) oemsList);
            baseDoc = documentManager.saveDocument(baseDoc);
            documentManager.save();

            EloraDocumentHelper.restoreToVersion(sourceDoc.getRef(),
                    baseDoc.getRef(), true, true, documentManager);

            // Nuxeo Event
            EloraEventHelper.fireEvent(EloraEventNames.ELORA_OEMS_CSV_IMPORTED,
                    sourceDoc, "");

            importLogs.add(new EloraCSVImportLog(lineNumber,
                    EloraCSVImportLog.Status.SUCCESS, "OEM added",
                    LABEL_CSV_IMPORTER_OEM_ADDED));

            return resultMsg;

        } catch (RuntimeException | EloraException e) {
            Throwable unwrappedException = unwrapException(e);
            resultMsg = e.toString();
            logError(importLogs, lineNumber, "Unable to add OEM: %s",
                    LABEL_CSV_IMPORTER_UNABLE_TO_ADD_OEM,
                    unwrappedException.getMessage());
            log.error(unwrappedException, unwrappedException);
        }
        return resultMsg;
    }

    protected void logError(List<EloraCSVImportLog> importLogs, long lineNumber,
            String message, String localizedMessage, String... params) {
        importLogs.add(new EloraCSVImportLog(lineNumber,
                EloraCSVImportLog.Status.ERROR,
                String.format(message, (Object[]) params), localizedMessage,
                params));
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, (Object[]) params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;
        if (t != null) {
            cause = t.getCause();
        }
        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

}
