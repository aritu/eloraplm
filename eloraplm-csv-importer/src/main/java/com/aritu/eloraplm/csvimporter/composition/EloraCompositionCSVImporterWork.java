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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.csvimporter.EloraCSVImportLog;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterOptions;
import com.aritu.eloraplm.csvimporter.EloraCSVImporterWork;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;

public class EloraCompositionCSVImporterWork extends AbstractWork {

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_REFERENCE = "eloraplm.label.csv.importer.notExistingReference";

    public static final String LABEL_CSV_IMPORTER_CIRCULAR_RELATION = "eloraplm.label.csv.importer.circularRelation";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_RELEASED_REFERENCE = "eloraplm.label.csv.importer.notExistingReleasedReference";

    public static final String LABEL_CSV_IMPORTER_MANUAL_RELATION_ALREADY_EXISTS = "eloraplm.label.csv.importer.manualRelationAlreadyExists";

    public static final String LABEL_CSV_IMPORTER_MISSING_COMPOSITION_COLUMNS = "eloraplm.label.csv.importer.missingCompositionColumns";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraCompositionCSVImporterWork.class);

    public static final String CATEGORY_CSV_IMPORTER = "eloraCompositionCsvImporter";

    /**
     * CSV headers that won't be checked if the field exists on the document
     * type.
     *
     * @since 7.3
     */
    public static List<String> AUTHORIZED_HEADERS = Arrays.asList(
            NXQL.ECM_LIFECYCLESTATE);

    protected DocumentModel currentDoc;

    protected String parentPath;

    protected String username;

    protected File csvFile;

    protected String csvFileName;

    protected EloraCSVImporterOptions options;

    protected transient DateFormat dateformat;

    protected Date startDate;

    protected List<EloraCSVImportLog> importLogs = new ArrayList<>();

    protected DocumentModel resultDoc;

    public EloraCompositionCSVImporterWork(String id) {
        super(id);
    }

    public EloraCompositionCSVImporterWork(String repositoryName,
            DocumentModel currentDocument, String username, File csvFile,
            String csvFileName, EloraCSVImporterOptions options) {

        super(EloraCompositionCSVImportId.create(repositoryName,
                currentDocument.getPathAsString(), csvFile));
        setDocument(repositoryName, null);
        setOriginatingUsername(username);
        currentDoc = currentDocument;
        parentPath = currentDocument.getPathAsString();
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
        setStatus("Importing composition");
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

    private Set<String> removeCompositionButManuals(DocumentModel item) {

        EloraDocumentRelationManager edrm = Framework.getService(
                EloraDocumentRelationManager.class);
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_COMPOSED_OF);
        List<Resource> predicateList = new ArrayList<Resource>();
        predicateList.add(predicateResource);
        List<Statement> itemStmts = EloraRelationHelper.getStatements(item,
                predicateList);

        Set<String> manualReferenceList = new HashSet<String>();
        for (Statement itemStmt : itemStmts) {
            DocumentModel objectDoc = RelationHelper.getDocumentModel(
                    itemStmt.getObject(), session);
            EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                    itemStmt);
            Boolean isManual = stmtInfo.getIsManual();
            if (isManual == null || !isManual) {
                edrm.softDeleteRelation(session, item,
                        EloraRelationConstants.BOM_COMPOSED_OF, objectDoc);

                log.trace("Relation soft deleted to item |" + objectDoc.getId()
                        + "|");
            } else {
                manualReferenceList.add(objectDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE).toString());
            }
        }
        return manualReferenceList;
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
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_REFERENCE_COL,
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_QUANTITY_COL };
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
                    LABEL_CSV_IMPORTER_MISSING_COMPOSITION_COLUMNS,
                    missingColumns);
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

            // Before importing new relations, remove current relations
            // excepting manual ones
            Set<String> manualReferenceList = removeCompositionButManuals(
                    currentDoc);

            // Import relations
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
                    String resultMsg = importRecord(record, header,
                            manualReferenceList);
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

            // Mark the document as modified
            EloraDocumentHelper.updateContributorAndModified(currentDoc, true);

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
    protected String importRecord(CSVRecord record, Map<String, Integer> header,
            Set<String> manualReferenceList) throws EloraException {

        String resultMsg = null;

        final String reference = record.get(
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_REFERENCE_COL);
        final String quantity = record.get(
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_QUANTITY_COL);

        String ordering = null;
        if (header.containsKey(
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_ORDER_COL)) {
            ordering = record.get(
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_ORDER_COL);
        }
        String designators = null;
        if (header.containsKey(
                EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_DESIGNATORS_COL)) {
            designators = record.get(
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_DESIGNATORS_COL);
        }

        if (StringUtils.isBlank(reference)) {
            resultMsg = "Missing "
                    + EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_REFERENCE_COL
                    + " value";
            log.error("record.isSet=" + record.isSet(
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_REFERENCE_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_MISSING_COLUMN_VALUE,
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_REFERENCE_COL);
            return resultMsg;
        }
        if (StringUtils.isBlank(quantity)) {
            resultMsg = "Missing "
                    + EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_QUANTITY_COL
                    + " value";
            log.error("record.isSet=" + record.isSet(
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_QUANTITY_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_MISSING_COLUMN_VALUE,
                    EloraCompositionCSVImportConstants.CSV_IMPORT_COMP_QUANTITY_COL);
            return resultMsg;
        }

        DocumentModel targetDoc = null;
        targetDoc = EloraDocumentHelper.getWcDocumentByTypeAndReference(session,
                reference, EloraDoctypeConstants.ELECTRONIC_COMPONENT);

        if (targetDoc != null) {
            targetDoc = EloraDocumentHelper.getLatestReleasedVersion(targetDoc);
            if (targetDoc == null) {
                resultMsg = "The reference |" + reference
                        + "| does not have a released version";
                logError(importLogs, record.getRecordNumber(), resultMsg,
                        LABEL_CSV_IMPORTER_NOT_EXISTING_RELEASED_REFERENCE);
                return resultMsg;
            }
        } else {
            targetDoc = EloraDocumentHelper.getWcDocumentByTypeAndReference(
                    session, reference, EloraDoctypeConstants.SOFTWARE);
            if (targetDoc != null) {
                resultMsg = checkTargetDocument(record, reference, targetDoc);
                if (resultMsg != null) {
                    return resultMsg;
                }

            } else {
                targetDoc = EloraDocumentHelper.getWcDocumentByTypeAndReference(
                        session, reference, EloraDoctypeConstants.BOM_PART);
                if (targetDoc != null) {
                    resultMsg = checkTargetDocument(record, reference,
                            targetDoc);
                    if (resultMsg != null) {
                        return resultMsg;
                    }
                } else {
                    targetDoc = EloraDocumentHelper.getWcDocumentByTypeAndReference(
                            session, reference,
                            EloraDoctypeConstants.BOM_PRODUCT);
                    if (targetDoc != null) {
                        resultMsg = checkTargetDocument(record, reference,
                                targetDoc);
                        if (resultMsg != null) {
                            return resultMsg;
                        }
                    } else {
                        resultMsg = "The reference |" + reference
                                + "| does not exist";
                        logError(importLogs, record.getRecordNumber(),
                                resultMsg,
                                LABEL_CSV_IMPORTER_NOT_EXISTING_REFERENCE);
                        return resultMsg;
                    }
                }
            }
        }

        if (manualReferenceList.contains(targetDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString())) {
            resultMsg = "A relation marked as manual already exists to reference |"
                    + reference + "|";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MANUAL_RELATION_ALREADY_EXISTS);
            return resultMsg;
        }

        return createRelation(record.getRecordNumber(), currentDoc,
                EloraRelationConstants.BOM_COMPOSED_OF, targetDoc, quantity,
                ordering, designators);
    }

    protected String checkTargetDocument(CSVRecord record, String reference,
            DocumentModel targetDoc) throws EloraException {

        String resultMsg = null;

        if (EloraRelationHelper.isCircularRelation(currentDoc, targetDoc, false,
                currentDoc.getCoreSession())) {
            resultMsg = "Adding this object document will cause a circular relation.";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_CIRCULAR_RELATION);
            return resultMsg;
        }

        targetDoc = EloraDocumentHelper.getLatestReleasedVersion(targetDoc);

        if (targetDoc == null) {
            resultMsg = "The reference |" + reference
                    + "| does not have a released version";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_NOT_EXISTING_RELEASED_REFERENCE);
            return resultMsg;
        }

        return resultMsg;
    }

    protected String createRelation(long lineNumber, DocumentModel sourceDoc,
            String predicate, DocumentModel targetDoc, String quantity,
            String ordering, String designators) {

        String resultMsg = null;
        try {

            Integer orderingInt = null;
            if (!StringUtils.isBlank(ordering)) {
                orderingInt = Integer.valueOf(ordering);
            } else {
                orderingInt = ((int) lineNumber) - 1;
            }

            // Add relation between WCs
            EloraDocumentRelationManager eloraDocumentRelationManager = Framework.getService(
                    EloraDocumentRelationManager.class);
            eloraDocumentRelationManager.addRelation(session, sourceDoc,
                    targetDoc, predicate, null, quantity, orderingInt, null,
                    null, null, false, designators, false);

            importLogs.add(new EloraCSVImportLog(lineNumber,
                    EloraCSVImportLog.Status.SUCCESS, "Relation created",
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_RELATION_CREATED));

            return resultMsg;

        } catch (RuntimeException | RelationNotAllowedException e) {
            Throwable unwrappedException = unwrapException(e);
            resultMsg = e.toString();
            logError(importLogs, lineNumber, "Unable to create relation: %s",
                    EloraCSVImporterWork.LABEL_CSV_IMPORTER_UNABLE_TO_CREATE_RELATION,
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
