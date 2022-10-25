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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.notification.MailTemplateHelper;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoFacetConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;

/**
 * Work task to import form a CSV file. Because the file is read from the local
 * filesystem, this must be executed in a local queue. Since NXP-15252 the CSV
 * reader manages "records", not "lines".
 *
 * @since 5.7
 */
public class EloraCSVImporterWork extends AbstractWork {

    // TODO:: konstante haek konstanteen klase batera atera kanpora???

    public static final String NUXEO_CSV_MAIL_TO = "nuxeo.csv.mail.to";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD = "label.csv.importer.notExistingField";

    public static final String LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE = "label.csv.importer.cannotConvertFieldValue";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_FILE = "label.csv.importer.notExistingFile";

    public static final String NUXEO_CSV_BLOBS_FOLDER = "nuxeo.csv.blobs.folder";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_ALREADY_EXISTS = "label.csv.importer.documentAlreadyExists";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_UPDATE = "label.csv.importer.unableToUpdate";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_UPDATED = "label.csv.importer.documentUpdated";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_CREATE = "label.csv.importer.unableToCreate";

    public static final String LABEL_CSV_IMPORTER_PARENT_DOES_NOT_EXIST = "label.csv.importer.parentDoesNotExist";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_CREATED = "label.csv.importer.documentCreated";

    public static final String LABEL_CSV_IMPORTER_NOT_ALLOWED_SUB_TYPE = "label.csv.importer.notAllowedSubType";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_SAVE = "label.csv.importer.unableToSave";

    public static final String LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE = "label.csv.importer.errorImportingLine";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE = "label.csv.importer.notExistingType";

    public static final String LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE = "label.csv.importer.missingTypeValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_NAME_VALUE = "label.csv.importer.missingNameValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_PARENT_PATH_VALUE = "label.csv.importer.missingParentPathValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_DO_CHECKIN_VALUE = "eloraplm.label.csv.importer.missingDoCheckinValue";

    public static final String LABEL_CSV_IMPORTER_WRONG_DO_CHECKIN_VALUE = "eloraplm.label.csv.importer.wrongDoCheckinValue";

    public static final String LABEL_CSV_IMPORTER_INCOMPATIBLE_LIFECYCLE_STATE = "eloraplm.label.csv.importer.incompatibleLifecycleState";

    public static final String LABEL_CSV_SAME_REFERENCE_AND_TYPE_EXISTS = "eloraplm.label.csv.importer.sameReferenceAndTypeExists";

    public static final String LABEL_CSV_IMPORTER_EMPTY_PROPERTIES = "eloraplm.label.csv.importer.emptyProperties";

    // public static final String LABEL_CSV_IMPORTER_MISSING_NAME_TYPE_COLUMN =
    // "label.csv.importer.missingNameOrTypeColumn";

    public static final String LABEL_CSV_IMPORTER_MISSING_NAME_TYPE_PARENT_PATH_OR_DO_CHECKIN_COLUMN = "eloraplm.label.csv.importer.missingNameTypeParentPathOrDoCheckinColumn";

    // ---------------------------------------------------------
    // Relations...
    public static final String LABEL_CSV_IMPORTER_SOURCE_DOC_CANNOT_BE_IDENTIFIED = "eloraplm.label.csv.importer.sourceDocumentCannotBeIdentified";

    public static final String LABEL_CSV_IMPORTER_MISSING_RELATION_PREDICATE_VALUE = "eloraplm.label.csv.importer.missingRelationPredicateValue";

    public static final String LABEL_CSV_IMPORTER_TARGET_DOC_CANNOT_BE_IDENTIFIED = "eloraplm.label.csv.importer.targetDocumentCannotBeIdentified";

    public static final String LABEL_CSV_IMPORTER_WRONG_IS_MANUAL_VALUE = "eloraplm.label.csv.importer.wrongIsManualValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_RELATIONS_COLUMNS = "eloraplm.label.csv.importer.missingRelationsColumns";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_CREATE_RELATION = "eloraplm.label.csv.importer.unableToCreateRelation";

    public static final String LABEL_CSV_IMPORTER_RELATION_CREATED = "eloraplm.label.csv.importer.relationCreated";

    // ---------------------------------------------------------
    // PROXIES...
    public static final String LABEL_CSV_IMPORTER_MISSING_PROXIES_COLUMNS = "eloraplm.label.csv.importer.missingProxiesColumns";

    public static final String LABEL_CSV_IMPORT_PROXY_DOC_CANNOT_BE_IDENTIFIED = "eloraplm.label.csv.proxyImporter.documentCannotBeIdentified";

    public static final String LABEL_CSV_IMPORT_PROXY_FOLDER_CANNOT_BE_IDENTIFIED = "eloraplm.label.csv.proxyImporter.folderCannotBeIdentified";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_CREATE_PROXY = "eloraplm.label.csv.importer.unableToCreateProxy";

    public static final String LABEL_CSV_IMPORTER_PROXY_CREATED = "eloraplm.label.csv.importer.proxyCreated";

    // ---------------------------------------------------------

    public static final String LABEL_CSV_IMPORTER_EMPTY_FILE = "label.csv.importer.emptyFile";

    public static final String LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT = "label.csv.importer.errorDuringImport";

    public static final String LABEL_CSV_IMPORTER_EMPTY_LINE = "label.csv.importer.emptyLine";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraCSVImporterWork.class);

    private static final String TEMPLATE_IMPORT_RESULT = "templates/eloraCsvImportResult.ftl";

    public static final String CATEGORY_CSV_IMPORTER = "eloraCsvImporter";

    public static final String CONTENT_FILED_TYPE_NAME = "content";

    // -----------------------------------------------
    public static final String CSV_IMPORT_RESULTS_FOLDER = "___importResults___";

    public static final String CSV_IMPORT_RESULTS_SUFFIX = "___result";

    // -----------------------------------------------
    private static final String IMPORT_RESULT_EMAIL_SUBJECT = "Inportazioaren emaitza / Resultado de la importación";

    private static final String IMPORT_RESULT_EMAIL_SUBJECT_PREFIX = "[EloraPLM]";

    private static final ObjectMapper mapper = new ObjectMapper();

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

    protected File relationsCsvFile;

    protected String relationsCsvFileName;

    protected File proxiesCsvFile;

    protected String proxiesCsvFileName;

    protected EloraCSVImporterOptions options;

    protected transient DateFormat dateformat;

    protected Date startDate;

    protected List<EloraCSVImportLog> importLogs = new ArrayList<>();

    protected List<EloraCSVImportLog> relationsImportLogs = new ArrayList<>();

    protected List<EloraCSVImportLog> proxiesImportLogs = new ArrayList<>();

    protected DocumentModel resultDoc;

    public EloraCSVImporterWork(String id) {
        super(id);
    }

    public EloraCSVImporterWork(String repositoryName, String parentPath,
            String username, File csvFile, String csvFileName,
            File relationsCsvFile, String relationsCsvFileName,
            File proxiesCsvFile, String proxiesCsvFileName,
            EloraCSVImporterOptions options) {

        super(EloraCSVImportId.create(repositoryName, parentPath, csvFile,
                relationsCsvFile, proxiesCsvFile));
        setDocument(repositoryName, null);
        setOriginatingUsername(username);
        this.parentPath = parentPath;
        this.username = username;
        this.csvFile = csvFile;
        this.csvFileName = csvFileName;
        this.relationsCsvFile = relationsCsvFile;
        this.relationsCsvFileName = relationsCsvFileName;
        this.proxiesCsvFile = proxiesCsvFile;
        this.proxiesCsvFileName = proxiesCsvFileName;
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

    public List<EloraCSVImportLog> getRelationsImportLogs() {
        return new ArrayList<>(relationsImportLogs);
    }

    public List<EloraCSVImportLog> getProxiesImportLogs() {
        return new ArrayList<>(proxiesImportLogs);
    }

    public DocumentModel getResultDoc() {
        return resultDoc;
    }

    @Override
    public void work() {
        setStatus("Importing");
        openUserSession();

        try {
            // Create result document
            resultDoc = createResultDocument();

            Reader inDocuments = null;
            Reader inRelations = null;
            Reader inProxies = null;
            if (csvFile != null) {
                inDocuments = newReader(csvFile);
            }
            if (relationsCsvFile != null) {
                inRelations = newReader(relationsCsvFile);
            }
            if (proxiesCsvFile != null) {
                inProxies = newReader(proxiesCsvFile);
            }

            // First import documents
            if (csvFile != null) {
                // Reader in = newReader(csvFile);
                CSVParser parser = CSVFormat.DEFAULT.withEscape(
                        options.getEscapeCharacter()).withHeader().parse(
                                inDocuments);
                File outputFile = createOutputFile(csvFileName);
                FileWriter outputFileWriter = new FileWriter(outputFile);
                CSVPrinter csvFilePrinter = createCsvFilePrinter(
                        outputFileWriter, parser.getHeaderMap());

                doImport(parser, csvFilePrinter);

                outputFileWriter.flush();
                outputFileWriter.close();
                csvFilePrinter.close();

                appendOutputFileToResultDocument(resultDoc, outputFile);
            }

            // Then, if defined, import relations between documents
            if (relationsCsvFile != null) {
                // Reader relationsIn = newReader(relationsCsvFile);
                CSVParser relationsParser = CSVFormat.DEFAULT.withEscape(
                        options.getEscapeCharacter()).withHeader().parse(
                                inRelations);
                File outputFile = createOutputFile(relationsCsvFileName);
                FileWriter outputFileWriter = new FileWriter(outputFile);
                CSVPrinter csvFilePrinter = createCsvFilePrinter(
                        outputFileWriter, relationsParser.getHeaderMap());

                doImportRelations(relationsParser, csvFilePrinter);

                outputFileWriter.flush();
                outputFileWriter.close();
                csvFilePrinter.close();

                appendOutputFileToResultDocument(resultDoc, outputFile);
            }

            // Then, if defined, import proxies
            if (proxiesCsvFile != null) {
                // Reader proxiesIn = newReader(proxiesCsvFile);
                CSVParser proxiesParser = CSVFormat.DEFAULT.withEscape(
                        options.getEscapeCharacter()).withHeader().parse(
                                inProxies);
                File outputFile = createOutputFile(proxiesCsvFileName);
                FileWriter outputFileWriter = new FileWriter(outputFile);
                CSVPrinter csvFilePrinter = createCsvFilePrinter(
                        outputFileWriter, proxiesParser.getHeaderMap());

                doImportProxies(proxiesParser, csvFilePrinter);

                outputFileWriter.flush();
                outputFileWriter.close();
                csvFilePrinter.close();

                appendOutputFileToResultDocument(resultDoc, outputFile);
            }

        } catch (IOException | EloraException e) {
            logError(importLogs, 0, "Error while doing the import: %s",
                    LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT, e.getMessage());
            log.error(e, e);
        }
        if (options.sendEmail()) {
            setStatus("Sending email");
            sendMail(resultDoc);
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

    protected void doImport(CSVParser parser, CSVPrinter csvFilePrinter)
            throws IOException {
        log.info(String.format("Importing CSV file: %s", csvFileName));
        Map<String, Integer> header = parser.getHeaderMap();
        if (header == null) {
            String errorMsg = "No header line or empty file.";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(importLogs, 0, errorMsg, LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }
        if (!header.containsKey(EloraCSVImportConstants.CSV_NAME_COL)
                || !header.containsKey(EloraCSVImportConstants.CSV_TYPE_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_PARENT_PATH_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_DO_CHECKIN_COL)) {

            String errorMsg = "Missing 'name', 'type', 'parentPath' or 'doCheckin' column";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(importLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_MISSING_NAME_TYPE_PARENT_PATH_OR_DO_CHECKIN_COLUMN);
            return;
        }
        try {
            int batchSize = options.getBatchSize();
            long docsCreatedCount = 0;
            for (CSVRecord record : parser) {
                if (record.size() == 0) {
                    // empty record
                    String errorMsg = "Error at line |"
                            + record.getRecordNumber() + "|: Empty record";
                    log.error(errorMsg);
                    csvFilePrinter.print(errorMsg);
                    importLogs.add(new EloraCSVImportLog(
                            record.getRecordNumber(),
                            EloraCSVImportLog.Status.SKIPPED, "Empty record",
                            LABEL_CSV_IMPORTER_EMPTY_LINE));
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
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_OK_VAL, "");
                    } else {
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                                resultMsg);
                    }
                    /*} catch (EloraException e) {
                    // try next line
                    // Throwable unwrappedException = unwrapException(e);
                    logError(importLogs, parser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            e.getMessage());
                    log.error(e.getMessage(), e);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            e.getMessage());
                    } catch (NuxeoException e) {
                    // try next line
                    Throwable ue = unwrapException(e);
                    logError(importLogs, parser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            ue.getMessage());
                    log.error(ue.getMessage(), ue);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            ue.getMessage());
                    }*/
                } catch (Exception e) {
                    // try next line
                    // Throwable unwrappedException = unwrapException(e);
                    logError(importLogs, parser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            e.getMessage());
                    log.error(e.getMessage(), e);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            e.getMessage());
                }
            }
            try {
                session.save();
            } catch (NuxeoException e) {
                Throwable ue = unwrapException(e);
                logError(importLogs, parser.getRecordNumber(),
                        "Unable to save: %s", LABEL_CSV_IMPORTER_UNABLE_TO_SAVE,
                        ue.getMessage());
                String errorMsg = "Unable to save. Error message: |"
                        + ue.getMessage() + "|";
                log.error(errorMsg, ue);
                csvFilePrinter.print(errorMsg);
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

        final String name = record.get(EloraCSVImportConstants.CSV_NAME_COL);
        final String type = record.get(EloraCSVImportConstants.CSV_TYPE_COL);
        final String parentPath = record.get(
                EloraCSVImportConstants.CSV_PARENT_PATH_COL);
        boolean doCheckin = false;
        String doCheckinStr = record.get(
                EloraCSVImportConstants.CSV_DO_CHECKIN_COL);

        String checkinComment = "";
        if (header.containsKey(
                EloraCSVImportConstants.CSV_CHECHIN_COMMENT_COL)) {
            checkinComment = record.get(
                    EloraCSVImportConstants.CSV_CHECHIN_COMMENT_COL);
        }
        if (StringUtils.isBlank(checkinComment)) {
            checkinComment = EloraCSVImportConstants.CHECKIN_DEFAULT_COMMENT;
        }
        String reference = "";
        if (header.containsKey(EloraMetadataConstants.ELORA_ELO_REFERENCE)) {
            reference = record.get(EloraMetadataConstants.ELORA_ELO_REFERENCE);
        }

        if (StringUtils.isBlank(name)) {
            resultMsg = "Missing 'name' value";
            log.error("record.isSet="
                    + record.isSet(EloraCSVImportConstants.CSV_NAME_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MISSING_NAME_VALUE);
            return resultMsg;
        }
        if (StringUtils.isBlank(type)) {
            resultMsg = "Missing 'type' value";
            log.error("record.isSet="
                    + record.isSet(EloraCSVImportConstants.CSV_TYPE_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE);
            return resultMsg;
        }
        if (StringUtils.isBlank(parentPath)) {
            resultMsg = "Missing 'parentPath' value";
            log.error("record.isSet=" + record.isSet(
                    EloraCSVImportConstants.CSV_PARENT_PATH_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MISSING_PARENT_PATH_VALUE);
            return resultMsg;
        }

        DocumentType docType = Framework.getLocalService(
                SchemaManager.class).getDocumentType(type);
        if (docType == null) {
            resultMsg = "The type |" + type + "does not exist";
            logError(importLogs, record.getRecordNumber(),
                    "The type '%s' does not exist",
                    LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE, type);
            return resultMsg;
        }

        if (StringUtils.isBlank(doCheckinStr)) {
            resultMsg = "Missing 'doCheckin' value";
            log.error("record.isSet="
                    + record.isSet(EloraCSVImportConstants.CSV_DO_CHECKIN_COL));
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MISSING_DO_CHECKIN_VALUE);
            return resultMsg;
        } else {
            if (doCheckinStr.equalsIgnoreCase("true")
                    || doCheckinStr.equalsIgnoreCase("false")) {
                doCheckin = Boolean.valueOf(doCheckinStr).booleanValue();

                // if doCheckin == false for a versionable docType, and state
                // column is present, state cannot be a released, obsolete or
                // delete state.
                if (!doCheckin && docType.hasFacet(
                        NuxeoFacetConstants.FACET_VERSIONABLE)) {

                    if (record.isMapped(NXQL.ECM_LIFECYCLESTATE)) {
                        final String lifecycleState = record.get(
                                NXQL.ECM_LIFECYCLESTATE);

                        if (StringUtils.isNotBlank(lifecycleState)) {
                            if (LifecyclesConfig.releasedStatesList.contains(
                                    lifecycleState)
                                    || LifecyclesConfig.obsoleteAndDeletedStatesList.contains(
                                            lifecycleState)) {
                                resultMsg = "Incompatible lifecycle state. If 'doCheckin' is false, lifecycle state cannot be |"
                                        + lifecycleState + "|";
                                logError(importLogs, record.getRecordNumber(),
                                        resultMsg,
                                        LABEL_CSV_IMPORTER_INCOMPATIBLE_LIFECYCLE_STATE);
                                return resultMsg;
                            }
                        }
                    }
                }
            } else {
                resultMsg = "Not allowed value for "
                        + EloraCSVImportConstants.CSV_DO_CHECKIN_COL
                        + " column. Value =  |" + doCheckinStr + "|";
                log.error(resultMsg);
                logError(importLogs, record.getRecordNumber(),
                        "Value '%s' is not allowed for doCheckin column.",
                        LABEL_CSV_IMPORTER_WRONG_DO_CHECKIN_VALUE,
                        doCheckinStr);
                return resultMsg;
            }
        }

        if (!StringUtils.isBlank(reference)) {
            // Check that there is not already another document with the
            // same reference and type
            String query = EloraQueryFactory.getCountWcDocsByTypeAndReferenceQuery(
                    type, reference);

            Long countWcDocs = EloraQueryHelper.executeUnrestrictedCountQuery(
                    session, query, NXQL.ECM_UUID);

            if (countWcDocs > 0) {
                resultMsg = "It is not possible to create the document, since there is already another document of the same type |"
                        + type + "| with same reference |" + reference + "|";
                log.error(resultMsg);
                logError(importLogs, record.getRecordNumber(), resultMsg,
                        LABEL_CSV_SAME_REFERENCE_AND_TYPE_EXISTS);
                return resultMsg;
            }
        }

        Map<String, Serializable> values = computePropertiesMap(record, docType,
                header);
        if (values == null) {
            // skip this line
            resultMsg = "Empty properties";
            logError(importLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_EMPTY_PROPERTIES);
            return resultMsg;

        }
        return createOrUpdateDocument(record.getRecordNumber(), name, type,
                parentPath, doCheckin, checkinComment, values);
    }

    /**
     * @since 6.0
     */
    protected Map<String, Serializable> computePropertiesMap(CSVRecord record,
            DocumentType docType, Map<String, Integer> header) {
        Map<String, Serializable> values = new HashMap<>();
        for (String headerValue : header.keySet()) {
            String lineValue = record.get(headerValue);
            lineValue = lineValue.trim();
            String fieldName = headerValue;
            if (!EloraCSVImportConstants.CSV_NAME_COL.equals(headerValue)
                    && !EloraCSVImportConstants.CSV_TYPE_COL.equals(headerValue)
                    && !EloraCSVImportConstants.CSV_PARENT_PATH_COL.equals(
                            headerValue)
                    && !EloraCSVImportConstants.CSV_DO_CHECKIN_COL.equals(
                            headerValue)
                    && !EloraCSVImportConstants.CSV_CHECHIN_COMMENT_COL.equals(
                            headerValue)) {
                if (AUTHORIZED_HEADERS.contains(headerValue)
                        && !StringUtils.isBlank(lineValue)) {
                    values.put(headerValue, lineValue);
                } else {
                    if (!docType.hasField(fieldName)) {
                        fieldName = fieldName.split(":")[1];
                    }
                    if (docType.hasField(fieldName)
                            && !StringUtils.isBlank(lineValue)) {
                        Serializable convertedValue = convertValue(docType,
                                fieldName, headerValue, lineValue,
                                record.getRecordNumber());
                        if (convertedValue == null) {
                            return null;
                        }
                        values.put(headerValue, convertedValue);
                    }
                }
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    protected Serializable convertValue(DocumentType docType, String fieldName,
            String headerValue, String stringValue, long lineNumber) {
        if (docType.hasField(fieldName)) {
            Field field = docType.getField(fieldName);
            if (field != null) {
                try {
                    Serializable fieldValue = null;
                    Type fieldType = field.getType();
                    if (fieldType.isComplexType()) {
                        if (fieldType.getName().equals(
                                CONTENT_FILED_TYPE_NAME)) {

                            fieldValue = (Serializable) createBlobFromFilePath(
                                    stringValue);
                            if (fieldValue == null) {
                                logError(importLogs, lineNumber,
                                        "The file '%s' does not exist",
                                        LABEL_CSV_IMPORTER_NOT_EXISTING_FILE,
                                        stringValue);
                                return null;
                            }
                        } else {
                            fieldValue = (Serializable) ComplexTypeJSONDecoder.decode(
                                    (ComplexType) fieldType, stringValue);

                            // Changes to apply fix
                            // https://jira.nuxeo.com/browse/NXP-22746
                            // in NX 7.10
                            replaceBlobs((Map<String, Object>) fieldValue);

                        }
                    } else {
                        if (fieldType.isListType()) {
                            Type listFieldType = ((ListType) fieldType).getFieldType();
                            if (listFieldType.isSimpleType()) {
                                /*
                                 * Array.
                                 */
                                fieldValue = stringValue.split(
                                        options.getListSeparatorRegex());
                            } else {
                                /*
                                 * Complex list.
                                 */

                                // Changes to apply fix
                                // https://jira.nuxeo.com/browse/NXP-22746
                                // in NX 7.10
                                if (fieldName.equals("files")) {
                                    fieldValue = (Serializable) decodeListForFiles(
                                            (ListType) fieldType, stringValue);
                                } else {
                                    fieldValue = (Serializable) ComplexTypeJSONDecoder.decodeList(
                                            (ListType) fieldType, stringValue);
                                }

                                // Changes to apply fix
                                // https://jira.nuxeo.com/browse/NXP-22746
                                // in NX 7.10
                                replaceBlobs((List<Object>) fieldValue);
                            }
                        } else {
                            /*
                             * Primitive type.
                             */
                            Type type = field.getType();
                            if (type instanceof SimpleTypeImpl) {
                                type = type.getSuperType();
                            }
                            if (type.isSimpleType()) {
                                if (type instanceof StringType) {
                                    fieldValue = stringValue;
                                } else if (type instanceof IntegerType) {
                                    fieldValue = Integer.valueOf(stringValue);
                                } else if (type instanceof LongType) {
                                    fieldValue = Long.valueOf(stringValue);
                                } else if (type instanceof DoubleType) {
                                    fieldValue = Double.valueOf(stringValue);
                                } else if (type instanceof BooleanType) {
                                    fieldValue = Boolean.valueOf(stringValue);
                                } else if (type instanceof DateType) {
                                    fieldValue = getDateFormat().parse(
                                            stringValue);
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch (ParseException | NumberFormatException
                        | IOException e) {
                    logError(importLogs, lineNumber,
                            "Unable to convert field '%s' with value '%s'",
                            LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE,
                            headerValue, stringValue);
                    log.error(e, e);
                }
            }
        } else {
            logError(importLogs, lineNumber,
                    "Field '%s' does not exist on type '%s'",
                    LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD, headerValue,
                    docType.getName());
        }
        return null;
    }

    protected DateFormat getDateFormat() {
        // transient field so may become null
        if (dateformat == null) {
            dateformat = new SimpleDateFormat(options.getDateFormat());
        }
        return dateformat;
    }

    protected String createOrUpdateDocument(long lineNumber, String name,
            String type, String parentPath, boolean doCheckin,
            String checkinComment, Map<String, Serializable> properties)
            throws EloraException {

        String resultMsg = null;

        Path targetPath = new Path(parentPath).append(name);

        // TODO LEIRE: For the instance, update is disabled (Elora choice ????)
        DocumentRef docRef = new PathRef(targetPath.toString());
        if (options.getCSVImporterDocumentFactory().exists(session, parentPath,
                name, type, properties)) {

            // return updateDocument(lineNumber, docRef, properties);
            resultMsg = "Document |" + docRef + "| already exists.";
            logError(importLogs, lineNumber, "Document '%s' already exists.",
                    LABEL_CSV_IMPORTER_DOCUMENT_ALREADY_EXISTS, parentPath);
            log.error(resultMsg);
            return resultMsg;

        } else {
            return createDocument(lineNumber, parentPath, name, type, doCheckin,
                    checkinComment, properties);
        }
    }

    protected String createDocument(long lineNumber, String parentPath,
            String name, String type, boolean doCheckin, String checkinComment,
            Map<String, Serializable> properties) {
        String resultMsg = null;

        try {
            DocumentRef parentRef = new PathRef(parentPath);
            if (session.exists(parentRef)) {
                DocumentModel parent = session.getDocument(parentRef);

                TypeManager typeManager = Framework.getLocalService(
                        TypeManager.class);
                if (options.checkAllowedSubTypes()
                        && !typeManager.isAllowedSubType(type,
                                parent.getType())) {
                    logError(importLogs, lineNumber,
                            "'%s' type is not allowed in '%s'",
                            LABEL_CSV_IMPORTER_NOT_ALLOWED_SUB_TYPE, type,
                            parent.getType());
                } else {
                    options.getCSVImporterDocumentFactory().createDocument(
                            session, parentPath, name, type, doCheckin,
                            checkinComment, properties);
                    importLogs.add(new EloraCSVImportLog(lineNumber,
                            EloraCSVImportLog.Status.SUCCESS,
                            "Document created",
                            LABEL_CSV_IMPORTER_DOCUMENT_CREATED));
                    return resultMsg;
                }
            } else {
                resultMsg = "Parent document |" + parentPath
                        + "| does not exist";
                logError(importLogs, lineNumber,
                        "Parent document '%s' does not exist",
                        LABEL_CSV_IMPORTER_PARENT_DOES_NOT_EXIST, parentPath);
            }
        } catch (RuntimeException e) {
            Throwable unwrappedException = unwrapException(e);
            resultMsg = e.toString();
            logError(importLogs, lineNumber, "Unable to create document: %s",
                    LABEL_CSV_IMPORTER_UNABLE_TO_CREATE,
                    unwrappedException.getMessage());
            log.error(unwrappedException, unwrappedException);
        }
        return resultMsg;
    }

    protected boolean updateDocument(long lineNumber, DocumentRef docRef,
            Map<String, Serializable> properties) {
        if (options.updateExisting()) {
            try {
                options.getCSVImporterDocumentFactory().updateDocument(session,
                        docRef, properties);
                importLogs.add(new EloraCSVImportLog(lineNumber,
                        EloraCSVImportLog.Status.SUCCESS, "Document updated",
                        LABEL_CSV_IMPORTER_DOCUMENT_UPDATED));
                return true;
            } catch (RuntimeException e) {
                Throwable unwrappedException = unwrapException(e);
                logError(importLogs, lineNumber,
                        "Unable to update document: %s",
                        LABEL_CSV_IMPORTER_UNABLE_TO_UPDATE,
                        unwrappedException.getMessage());
                log.error(unwrappedException, unwrappedException);
            }
        } else {
            importLogs.add(new EloraCSVImportLog(lineNumber,
                    EloraCSVImportLog.Status.SKIPPED, "Document already exists",
                    LABEL_CSV_IMPORTER_DOCUMENT_ALREADY_EXISTS));
        }
        return false;
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

    protected void sendMail(DocumentModel resultDocument) {
        try {
            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            NuxeoPrincipal principal = userManager.getPrincipal(username);
            String email = principal.getEmail();
            if (email == null) {
                log.info(String.format(
                        "Not sending import result email to '%s', no email configured",
                        username));
                return;
            }

            OperationContext ctx = new OperationContext(session);
            ctx.setInput(session.getRootDocument());

            EloraCSVImporter csvImporter = Framework.getLocalService(
                    EloraCSVImporter.class);
            List<EloraCSVImportLog> importerLogs = csvImporter.getImportLogs(
                    getId());
            EloraCSVImportResult importResult = EloraCSVImportResult.fromImportLogs(
                    importerLogs);
            List<EloraCSVImportLog> skippedAndErrorImportLogs = csvImporter.getImportLogs(
                    getId(), EloraCSVImportLog.Status.SKIPPED,
                    EloraCSVImportLog.Status.ERROR);
            ctx.put("importResult", importResult);
            ctx.put("skippedAndErrorImportLogs", skippedAndErrorImportLogs);
            ctx.put("csvFilename", csvFileName);
            ctx.put("startDate", DateFormat.getInstance().format(startDate));
            ctx.put("username", username);

            // Relations logs
            List<EloraCSVImportLog> importerRelationsLogs = csvImporter.getRelationsImportLogs(
                    getId());
            EloraCSVImportResult relationsImportResult = EloraCSVImportResult.fromImportLogs(
                    importerRelationsLogs);
            List<EloraCSVImportLog> relationsSkippedAndErrorImportLogs = csvImporter.getRelationsImportLogs(
                    getId(), EloraCSVImportLog.Status.SKIPPED,
                    EloraCSVImportLog.Status.ERROR);
            ctx.put("relationsImportResult", relationsImportResult);
            ctx.put("relationsSkippedAndErrorImportLogs",
                    relationsSkippedAndErrorImportLogs);

            // Proxys logs
            List<EloraCSVImportLog> proxiesRelationsLogs = csvImporter.getProxiesImportLogs(
                    getId());
            EloraCSVImportResult proxiesImportResult = EloraCSVImportResult.fromImportLogs(
                    proxiesRelationsLogs);
            List<EloraCSVImportLog> proxiesSkippedAndErrorImportLogs = csvImporter.getProxiesImportLogs(
                    getId(), EloraCSVImportLog.Status.SKIPPED,
                    EloraCSVImportLog.Status.ERROR);
            ctx.put("proxiesImportResult", proxiesImportResult);
            ctx.put("proxiesSkippedAndErrorImportLogs",
                    proxiesSkippedAndErrorImportLogs);

            // Result document
            String resultDocumentUrl = getDocumentUrl(resultDocument);

            /*try {
                String docUrl = DocumentModelFunctions.documentUrl(
                        resultDocument);
                log.info("************ docUrl = |" + docUrl + "|");
            } catch (Exception e) {
                log.error("Exception ************", e);
            }*/

            ctx.put("resultDocument", resultDocument);
            ctx.put("resultDocumentUrl", resultDocumentUrl);

            // TODO LEIRE: HAUEK ZIURRENIK EZ DITUGU BEHAR
            // DocumentModel importFolder = session.getDocument(
            // String importFolderUrl = getDocumentUrl(importFolder);
            // ctx.put("importFolderTitle", importFolder.getTitle());
            // ctx.put("importFolderUrl", importFolderUrl);
            ctx.put("userUrl", getUserUrl());

            StringList to = buildRecipientsList(email);
            Expression from = Scripting.newExpression("Env[\"mail.from\"]");

            // TODO: ez dot lortu aldagai hau entornotik hartzerik
            /*Expression emailSubjectPrefix = Scripting.newExpression(
                    "Env[\"nuxeo.notification.eMailSubjectPrefix\"]");*/
            String subject = IMPORT_RESULT_EMAIL_SUBJECT_PREFIX + " "
                    + IMPORT_RESULT_EMAIL_SUBJECT;

            String message = loadTemplate(TEMPLATE_IMPORT_RESULT);

            OperationChain chain = new OperationChain("SendMail");
            chain.add(SendMail.ID).set("from", from).set("to", to).set("HTML",
                    true).set("subject", subject).set("message", message);
            Framework.getLocalService(AutomationService.class).run(ctx, chain);
        } catch (Exception e) {
            // TODO LEIRE: horrela ondo?
            // ExceptionUtils.checkInterrupt(e);
            log.error(String.format(
                    "Unable to notify user '%s' for import result of '%s': %s",
                    username, csvFileName, e.getMessage()));
            log.error(e, e);
            // throw ExceptionUtils.runtimeException(e);
        }
    }

    protected String getDocumentUrl(DocumentModel doc) {
        // return MailTemplateHelper.getDocumentUrl(doc, null);
        String documentUrl = "";
        try {

            documentUrl = MailTemplateHelper.getDocumentUrl(doc, null);
            /*
            
            NotificationService notificationService = NotificationServiceHelper.getNotificationService();
            URLPolicyService urlPolicyService = Framework.getLocalService(
                    URLPolicyService.class);
            documentUrl = urlPolicyService.getUrlFromDocumentView(
                    new DocumentViewImpl(document),
                    notificationService.getServerUrlPrefix());
            log.info("************ documentUrl = |" + documentUrl + "|");*/

            /*
            // String url = viewCodecManager.getUrlFromDocumentView(new
            // DocumentViewImpl(doc), true, "http://localhost/nuxeo/");
            DocumentViewCodecManager documentViewCodecManager = Framework.getService(
                    DocumentViewCodecManager.class);
            String url = documentViewCodecManager.getUrlFromDocumentView(
                    new DocumentViewImpl(doc), true, BaseURL.getBaseURL());

            String docUrl = DocumentModelFunctions.documentUrl(doc);
            log.info("************ docUrl = |" + docUrl + "|");
            */

        } catch (Exception e) {
            // nothing to do
            log.error("Error retrieving documentUrl. Error message = |"
                    + e.getMessage() + "|", e);
        }
        return documentUrl;
    }

    protected String getUserUrl() {
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        DocumentView docView = new DocumentViewImpl(null, null, params);
        URLPolicyService urlPolicyService = Framework.getLocalService(
                URLPolicyService.class);
        return urlPolicyService.getUrlFromDocumentView("user", docView,
                notificationService.getServerUrlPrefix());
    }

    protected StringList buildRecipientsList(String userEmail) {
        String csvMailTo = Framework.getProperty(NUXEO_CSV_MAIL_TO);
        if (StringUtils.isBlank(csvMailTo)) {
            return new StringList(new String[] { userEmail });
        } else {
            return new StringList(new String[] { userEmail, csvMailTo });
        }
    }

    private static String loadTemplate(String key) {
        InputStream io = EloraCSVImporterWork.class.getClassLoader().getResourceAsStream(
                key);
        if (io != null) {
            try {
                return IOUtils.toString(io, Charsets.UTF_8);
            } catch (IOException e) {
                // cannot happen
                throw new NuxeoException(e);
            } finally {
                try {
                    io.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }
        return null;
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

    //////////////////////////////////////////////////////////////////////

    protected void doImportRelations(CSVParser relationsParser,
            CSVPrinter csvFilePrinter) throws IOException {
        log.info(String.format("Importing Relations CSV file: %s",
                relationsCsvFileName));
        Map<String, Integer> header = relationsParser.getHeaderMap();
        if (header == null) {
            String errorMsg = "No header line or empty file.";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(relationsImportLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }
        if (!header.containsKey(EloraCSVImportConstants.CSV_SOURCE_NAME_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_SOURCE_PARENT_PATH_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_SOURCE_DOC_UID_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_RELATION_PREDICATE_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_TARGET_NAME_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_TARGET_PARENT_PATH_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_TARGET_DOC_UID_COL)
                || !header.containsKey(EloraCSVImportConstants.CSV_QUANTITY_COL)
                || !header.containsKey(EloraCSVImportConstants.CSV_ORDERING_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_DIRECTOR_ORDERING_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_VIEWER_ORDERING_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_INVERSE_VIEWER_ORDERING_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IS_MANUAL_COL)) {
            // TODO LEIRE: errorea aldatu
            String errorMsg = "Missing relations columns";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(relationsImportLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_MISSING_RELATIONS_COLUMNS);
            return;
        }
        try {
            int batchSize = options.getBatchSize();
            long relationsCreatedCount = 0;
            for (CSVRecord record : relationsParser) {
                if (record.size() == 0) {
                    // empty record
                    String errorMsg = "Error at line |"
                            + record.getRecordNumber() + "|: Empty record";
                    log.error(errorMsg);
                    csvFilePrinter.print(errorMsg);
                    relationsImportLogs.add(new EloraCSVImportLog(
                            record.getRecordNumber(),
                            EloraCSVImportLog.Status.SKIPPED, "Empty record",
                            LABEL_CSV_IMPORTER_EMPTY_LINE));
                    continue;
                }
                try {
                    String resultMsg = importRelation(record, header);
                    if (StringUtils.isBlank(resultMsg)) {
                        relationsCreatedCount++;
                        if (relationsCreatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                        String infoMsg = "Record at line |"
                                + record.getRecordNumber()
                                + "| successfully imported.";
                        log.info(infoMsg);
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_OK_VAL, "");
                    } else {
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                                resultMsg);
                    }
                } catch (EloraException e) {
                    // try next line
                    // Throwable unwrappedException = unwrapException(e);
                    logError(relationsImportLogs,
                            relationsParser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            e.getMessage());
                    log.error(e.getMessage(), e);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            e.getMessage());
                } catch (NuxeoException e) {
                    // try next line
                    Throwable ue = unwrapException(e);
                    logError(relationsImportLogs,
                            relationsParser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            ue.getMessage());
                    log.error(ue.getMessage(), ue);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            ue.getMessage());
                }
            }
            try {
                session.save();
            } catch (NuxeoException e) {
                Throwable ue = unwrapException(e);
                logError(relationsImportLogs, relationsParser.getRecordNumber(),
                        "Unable to save: %s", LABEL_CSV_IMPORTER_UNABLE_TO_SAVE,
                        ue.getMessage());
                String errorMsg = "Unable to save. Error message: |"
                        + ue.getMessage() + "|";
                log.error(errorMsg, ue);
                csvFilePrinter.print(errorMsg);
            }
        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing CSV file: %s",
                relationsCsvFileName));
    }

    /**
     * Import a line from the Relations CSV file.
     *
     * @return {@code true} if a document has been created or updated,
     *         {@code false} otherwise.
     * @throws EloraException
     */
    protected String importRelation(CSVRecord record,
            Map<String, Integer> header) throws EloraException {
        String resultMsg = null;

        final String sourceName = record.get(
                EloraCSVImportConstants.CSV_SOURCE_NAME_COL);
        final String sourceParentPath = record.get(
                EloraCSVImportConstants.CSV_SOURCE_PARENT_PATH_COL);
        final String sourceDocUid = record.get(
                EloraCSVImportConstants.CSV_SOURCE_DOC_UID_COL);
        final String predicate = record.get(
                EloraCSVImportConstants.CSV_RELATION_PREDICATE_COL);
        final String targetName = record.get(
                EloraCSVImportConstants.CSV_TARGET_NAME_COL);
        final String targetParentPath = record.get(
                EloraCSVImportConstants.CSV_TARGET_PARENT_PATH_COL);
        final String targetDocUid = record.get(
                EloraCSVImportConstants.CSV_TARGET_DOC_UID_COL);
        String quantity = record.get(EloraCSVImportConstants.CSV_QUANTITY_COL);
        Integer ordering = null;
        final String orderingStr = record.get(
                EloraCSVImportConstants.CSV_ORDERING_COL);
        Integer directorOrdering = null;
        final String directorOrderingStr = record.get(
                EloraCSVImportConstants.CSV_DIRECTOR_ORDERING_COL);
        Integer viewerOrdering = null;
        final String viewerOrderingStr = record.get(
                EloraCSVImportConstants.CSV_VIEWER_ORDERING_COL);
        Integer inverseViewerOrdering = null;
        final String inverseViewerOrderingStr = record.get(
                EloraCSVImportConstants.CSV_INVERSE_VIEWER_ORDERING_COL);
        Boolean isManual = false;
        final String isManualStr = record.get(
                EloraCSVImportConstants.CSV_IS_MANUAL_COL);

        if ((StringUtils.isBlank(sourceName)
                || StringUtils.isBlank(sourceParentPath))
                && StringUtils.isBlank(sourceDocUid)) {
            resultMsg = "Source document cannot be identified. Source document name and parent path or source document uid should be defined. sourceName = |"
                    + sourceName + "|, sourceParentPath = |" + sourceParentPath
                    + "|, sourceDocUid = |" + sourceDocUid + "|";
            log.error(resultMsg);
            logError(relationsImportLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_SOURCE_DOC_CANNOT_BE_IDENTIFIED);
            return resultMsg;
        }
        if (StringUtils.isBlank(predicate)) {
            resultMsg = "Missing 'predicate' value";
            log.error("record.isSet=" + record.isSet(
                    EloraCSVImportConstants.CSV_RELATION_PREDICATE_COL));
            logError(relationsImportLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_MISSING_RELATION_PREDICATE_VALUE);
            return resultMsg;
        }
        if ((StringUtils.isBlank(targetName)
                || StringUtils.isBlank(targetParentPath))
                && StringUtils.isBlank(targetDocUid)) {
            resultMsg = "Target document cannot be identified. Target document  name and parent path or target document uid should be defined. targetName = |"
                    + targetName + "|, targetParentPath = |" + targetParentPath
                    + "|, targetDocUid = |" + targetDocUid + "|";
            log.error(resultMsg);
            logError(relationsImportLogs, record.getRecordNumber(), resultMsg,
                    LABEL_CSV_IMPORTER_TARGET_DOC_CANNOT_BE_IDENTIFIED);
            return resultMsg;
        }
        if (StringUtils.isBlank(quantity)) {
            quantity = "1";
        }
        if (!StringUtils.isBlank(orderingStr)) {
            ordering = Integer.valueOf(orderingStr);
        }
        if (!StringUtils.isBlank(directorOrderingStr)) {
            directorOrdering = Integer.valueOf(directorOrderingStr);
        }
        if (!StringUtils.isBlank(viewerOrderingStr)) {
            viewerOrdering = Integer.valueOf(viewerOrderingStr);
        }
        if (!StringUtils.isBlank(inverseViewerOrderingStr)) {
            inverseViewerOrdering = Integer.valueOf(inverseViewerOrderingStr);
        }

        if (!StringUtils.isBlank(isManualStr)) {
            if (isManualStr.equalsIgnoreCase("true")
                    || isManualStr.equalsIgnoreCase("false")) {
                isManual = Boolean.valueOf(isManualStr);

            } else {
                resultMsg = "Not allowed value for "
                        + EloraCSVImportConstants.CSV_IS_MANUAL_COL
                        + " column. Value =  |" + isManualStr + "|";
                log.error(resultMsg);
                logError(relationsImportLogs, record.getRecordNumber(),
                        "Value '%s' is not allowed for isManual column.",
                        LABEL_CSV_IMPORTER_WRONG_IS_MANUAL_VALUE, isManualStr);
                return resultMsg;
            }
        }

        return createRelation(record.getRecordNumber(), sourceName,
                sourceParentPath, sourceDocUid, predicate, targetName,
                targetParentPath, targetDocUid, quantity, ordering,
                directorOrdering, viewerOrdering, inverseViewerOrdering,
                isManual);
    }

    protected String createRelation(long lineNumber, String sourceName,
            String sourceParentPath, String sourceDocUid, String predicate,
            String targetName, String targetParentPath, String targetDocUid,
            String quantity, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual) {

        String resultMsg = null;

        try {

            // Retrieve source document
            // TODO LEIRE:: sourceDocWc ez dauka zertan WC-a izan beharrik.
            // Begiratu hau.
            DocumentModel sourceDocWc = getDocument(sourceName,
                    sourceParentPath, sourceDocUid);
            DocumentModel sourceDocAv = EloraDocumentHelper.getLatestVersion(
                    sourceDocWc);
            if (sourceDocAv == null) {
                throw new EloraException(
                        "Source document |" + sourceDocWc.getId()
                                + "| has no latest version or is unreadable.");
            }

            // Retrieve target document
            DocumentModel targetDocWc = getDocument(targetName,
                    targetParentPath, targetDocUid);
            DocumentModel targetDocAv = EloraDocumentHelper.getLatestVersion(
                    targetDocWc);
            if (targetDocAv == null) {
                throw new EloraException(
                        "Target document |" + targetDocWc.getId()
                                + "| has no latest version or is unreadable.");
            }

            // Check if relation already exists between source and target WCs
            if (EloraRelationHelper.existsRelation(sourceDocWc, targetDocWc,
                    predicate, session)) {
                String errorMsg = "Relation already exists. sourceDocWc = |"
                        + sourceDocWc.getId() + "|, predicate = |" + predicate
                        + "|, targetDocWc = |" + targetDocWc.getId() + "|";
                log.error(errorMsg);
                throw new EloraException(errorMsg);
            }

            // Check if relation already exists between source and target AVs
            if (EloraRelationHelper.existsRelation(sourceDocAv, targetDocAv,
                    predicate, session)) {
                String errorMsg = "Relation already exists. sourceDocAv = |"
                        + sourceDocAv.getId() + "|, predicate = |" + predicate
                        + "|, targetDocAv = |" + targetDocAv.getId() + "|";
                log.error(errorMsg);
                throw new EloraException(errorMsg);
            }

            // Check relation is allowed
            CheckinManager checkinManager = Framework.getService(
                    CheckinManager.class);
            checkinManager.checkThatRelationIsAllowed(sourceDocWc, predicate,
                    targetDocWc, quantity);
            checkinManager.checkThatRelationIsAllowed(sourceDocAv, predicate,
                    targetDocAv, quantity);

            // Add relation between WCs
            EloraDocumentRelationManager eloraDocumentRelationManager = Framework.getService(
                    EloraDocumentRelationManager.class);
            eloraDocumentRelationManager.addRelation(session, sourceDocWc,
                    targetDocWc, predicate, null, quantity, ordering,
                    directorOrdering, viewerOrdering, inverseViewerOrdering,
                    isManual);

            // Add relation between versions
            eloraDocumentRelationManager.addRelation(session, sourceDocAv,
                    targetDocAv, predicate, null, quantity, ordering,
                    directorOrdering, viewerOrdering, inverseViewerOrdering,
                    isManual);

            relationsImportLogs.add(new EloraCSVImportLog(lineNumber,
                    EloraCSVImportLog.Status.SUCCESS, "Relation created",
                    LABEL_CSV_IMPORTER_RELATION_CREATED));

            return resultMsg;

        } catch (RuntimeException | EloraException e) {
            Throwable unwrappedException = unwrapException(e);
            resultMsg = e.toString();
            logError(relationsImportLogs, lineNumber,
                    "Unable to create relation: %s",
                    LABEL_CSV_IMPORTER_UNABLE_TO_CREATE_RELATION,
                    unwrappedException.getMessage());
            log.error(unwrappedException, unwrappedException);
        }
        return resultMsg;
    }

    protected DocumentModel getDocument(String docName, String docParentPath,
            String docUid) throws EloraException {

        DocumentModel document = null;
        DocumentRef docRef = null;

        if (!StringUtils.isBlank(docName)
                && !StringUtils.isBlank(docParentPath)) {
            String docPath = new Path(docParentPath).append(docName).toString();
            docRef = new PathRef(docPath);
        } else if (!StringUtils.isBlank(docUid)) {
            docRef = (new IdRef(docUid));
        } else {
            // TODO LEIRE: launch excpetion
            throw new EloraException("document not specified");
        }
        if (docRef != null) {
            document = session.getDocument(docRef);
        }

        return document;
    }

    protected void doImportProxies(CSVParser proxiesParser,
            CSVPrinter csvFilePrinter) throws IOException {
        log.info(String.format("Importing Proxies CSV file: %s",
                proxiesCsvFileName));
        Map<String, Integer> header = proxiesParser.getHeaderMap();
        if (header == null) {
            String errorMsg = "No header line or empty file.";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(proxiesImportLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }
        if (!header.containsKey(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_NAME_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_PARENT_PATH_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_UID_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_NAME_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_PARENT_PATH_COL)
                || !header.containsKey(
                        EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_UID_COL)) {
            // TODO LEIRE: errorea aldatu
            String errorMsg = "Missing proxies columns";
            log.error(errorMsg);
            csvFilePrinter.print(errorMsg);
            logError(proxiesImportLogs, 0, errorMsg,
                    LABEL_CSV_IMPORTER_MISSING_PROXIES_COLUMNS);
            return;
        }

        try {
            int batchSize = options.getBatchSize();
            long proxiesCreatedCount = 0;
            for (CSVRecord record : proxiesParser) {
                if (record.size() == 0) {
                    // empty record
                    String errorMsg = "Error at line |"
                            + record.getRecordNumber() + "|: Empty record";
                    log.error(errorMsg);
                    csvFilePrinter.print(errorMsg);
                    proxiesImportLogs.add(new EloraCSVImportLog(
                            record.getRecordNumber(),
                            EloraCSVImportLog.Status.SKIPPED, "Empty record",
                            LABEL_CSV_IMPORTER_EMPTY_LINE));
                    continue;
                }
                try {
                    String resultMsg = importProxy(record, header);
                    if (StringUtils.isBlank(resultMsg)) {
                        proxiesCreatedCount++;
                        if (proxiesCreatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                        String infoMsg = "Record at line |"
                                + record.getRecordNumber()
                                + "| successfully imported.";
                        log.info(infoMsg);
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_OK_VAL, "");
                    } else {
                        writeResultRecord(csvFilePrinter, record,
                                EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                                resultMsg);
                    }
                } catch (EloraException e) {
                    // try next line
                    // Throwable unwrappedException = unwrapException(e);
                    logError(proxiesImportLogs, proxiesParser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            e.getMessage());
                    log.error(e.getMessage(), e);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            e.getMessage());
                } catch (NuxeoException e) {
                    // try next line
                    Throwable ue = unwrapException(e);
                    logError(proxiesImportLogs, proxiesParser.getRecordNumber(),
                            "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE,
                            ue.getMessage());
                    log.error(ue.getMessage(), ue);
                    writeResultRecord(csvFilePrinter, record,
                            EloraCSVImportConstants.CSV_RESULT_ERROR_VAL,
                            ue.getMessage());
                }
            }

            try {
                session.save();
            } catch (NuxeoException e) {
                Throwable ue = unwrapException(e);
                logError(proxiesImportLogs, proxiesParser.getRecordNumber(),
                        "Unable to save: %s", LABEL_CSV_IMPORTER_UNABLE_TO_SAVE,
                        ue.getMessage());
                String errorMsg = "Unable to save. Error message: |"
                        + ue.getMessage() + "|";
                log.error(errorMsg, ue);
                csvFilePrinter.print(errorMsg);
            }
        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing CSV file: %s",
                proxiesCsvFileName));
    }

    /**
     * Import a line from the Proxies CSV file.
     *
     * @return {@code true} if a document has been created or updated,
     *         {@code false} otherwise.
     * @throws EloraException
     */
    protected String importProxy(CSVRecord record, Map<String, Integer> header)
            throws EloraException {
        String resultMsg = null;

        final String docName = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_NAME_COL);
        final String docParentPath = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_PARENT_PATH_COL);
        final String docUid = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_DOC_UID_COL);
        final String folderName = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_NAME_COL);
        final String folderParentPath = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_PARENT_PATH_COL);
        final String folderUid = record.get(
                EloraCSVImportConstants.CSV_IMPORT_PROXY_FOLDER_UID_COL);

        if ((StringUtils.isBlank(docName) || StringUtils.isBlank(docParentPath))
                && StringUtils.isBlank(docUid)) {
            resultMsg = "Document cannot be identified. Document name and parent path or document uid should be defined. docName = |"
                    + docName + "|, docParentPath = |" + docParentPath
                    + "|, docUid = |" + docUid + "|";
            log.error(resultMsg);
            logError(proxiesImportLogs, record.getRecordNumber(),
                    "Document cannot be identified. Document name and parent path or document uid should be defined",
                    LABEL_CSV_IMPORT_PROXY_DOC_CANNOT_BE_IDENTIFIED);
            return resultMsg;
        }

        if ((StringUtils.isBlank(folderName)
                || StringUtils.isBlank(folderParentPath))
                && StringUtils.isBlank(folderUid)) {
            resultMsg = "Folder cannot be identified. Folder name and parent path or folder uid should be defined. folderName = |"
                    + folderName + "|, folderParentPath = |" + folderParentPath
                    + "|, folderUid = |" + folderUid + "|";
            log.error(resultMsg);
            logError(proxiesImportLogs, record.getRecordNumber(),
                    "Folder cannot be identified. Folder name and parent path or folder uid should be defined.",
                    LABEL_CSV_IMPORT_PROXY_FOLDER_CANNOT_BE_IDENTIFIED);
            return resultMsg;
        }

        return createProxy(record.getRecordNumber(), docName, docParentPath,
                docUid, folderName, folderParentPath, folderUid);
    }

    protected String createProxy(long lineNumber, String docName,
            String docParentPath, String docUid, String folderName,
            String folderParentPath, String folderUid) {
        String resultMsg = null;

        try {

            // Retrieve document
            DocumentModel doc = getDocument(docName, docParentPath, docUid);

            // Retrieve folder
            DocumentModel folder = getDocument(folderName, folderParentPath,
                    folderUid);

            session.createProxy(doc.getRef(), folder.getRef());

            proxiesImportLogs.add(new EloraCSVImportLog(lineNumber,
                    EloraCSVImportLog.Status.SUCCESS, "Proxy created",
                    LABEL_CSV_IMPORTER_PROXY_CREATED));

            return resultMsg;

        } catch (RuntimeException | EloraException e) {
            Throwable unwrappedException = unwrapException(e);
            resultMsg = e.toString();
            logError(proxiesImportLogs, lineNumber,
                    "Unable to create proxy: %s",
                    LABEL_CSV_IMPORTER_UNABLE_TO_CREATE_PROXY,
                    unwrappedException.getMessage());
            log.error(unwrappedException, unwrappedException);
        }
        return resultMsg;
    }

    protected DocumentModel createResultDocument()
            throws IOException, EloraException {

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMdd-hh:mm:ss.SSS");
        String importResultDocTitle = dateFormat.format(new Date());

        PathSegmentService pss = Framework.getService(PathSegmentService.class);

        DocumentModel importResultsFolder = retrieveImportResultsFolder();
        String importResultPath = importResultsFolder.getPath().toString();
        String importResultDocName = pss.generatePathSegment(
                importResultDocTitle);

        DocumentModel importResultDocModel = session.createDocumentModel(
                importResultPath, importResultDocName,
                NuxeoDoctypeConstants.FILE);

        // Set document properties:
        importResultDocModel.setPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE, importResultDocName);

        // Attach uploaded files
        // documents file
        if (csvFile != null) {
            Blob docBlob = Blobs.createBlob(csvFile);
            // TODO LEIRE: hau konstante batetik hartu????
            docBlob.setMimeType("text/csv");
            Framework.trackFile(csvFile, docBlob);
            docBlob.setFilename(csvFileName);
            DocumentHelper.addBlob(importResultDocModel.getProperty(
                    NuxeoMetadataConstants.NX_FILES_FILES), docBlob);
        }

        // relations file
        if (relationsCsvFile != null) {
            Blob relationsBlob = Blobs.createBlob(relationsCsvFile);
            // TODO LEIRE: hau konstante batetik hartu????
            relationsBlob.setMimeType("text/csv");
            Framework.trackFile(relationsCsvFile, relationsBlob);
            relationsBlob.setFilename(relationsCsvFileName);
            DocumentHelper.addBlob(
                    importResultDocModel.getProperty(
                            NuxeoMetadataConstants.NX_FILES_FILES),
                    relationsBlob);
        }

        // proxies file
        if (proxiesCsvFile != null) {
            Blob proxiesBlob = Blobs.createBlob(proxiesCsvFile);
            // TODO LEIRE: hau konstante batetik hartu????
            proxiesBlob.setMimeType("text/csv");
            Framework.trackFile(proxiesCsvFile, proxiesBlob);
            proxiesBlob.setFilename(proxiesCsvFileName);
            DocumentHelper.addBlob(
                    importResultDocModel.getProperty(
                            NuxeoMetadataConstants.NX_FILES_FILES),
                    proxiesBlob);
        }

        // Create the document
        importResultDocModel = session.createDocument(importResultDocModel);
        session.save();

        return importResultDocModel;
    }

    protected DocumentModel retrieveImportResultsFolder()
            throws EloraException {

        String logInitMsg = "[retrieveImportResultsFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel importResultsFolder;

        try {
            String query = EloraCSVImportQueryFactory.getImportResultsFolderQuery();
            DocumentModelList res = session.query(query);

            if (res == null || res.isEmpty()) {
                String errorMsg = EloraCSVImportConstants.IMPORT_RESULTS_FOLDER_DOCUMENT_TYPE
                        + " is missing in the system.";
                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);

            } else if (res.size() > 1) {
                String errorMsg = "More han one "
                        + EloraCSVImportConstants.IMPORT_RESULTS_FOLDER_DOCUMENT_TYPE
                        + " found:";
                for (DocumentModel model : res) {
                    errorMsg += " - " + model.getName() + ", "
                            + model.getPathAsString();
                }

                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);

            } else {
                importResultsFolder = res.get(0);

                if (importResultsFolder != null) {

                    log.trace(logInitMsg + "importResultsFolder = |"
                            + importResultsFolder.getId() + "|");

                } else {
                    String errorMsg = EloraCSVImportConstants.IMPORT_RESULTS_FOLDER_DOCUMENT_TYPE
                            + " is null";
                    log.error(logInitMsg + errorMsg);
                    throw new EloraException(errorMsg);
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(e.getMessage());
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return importResultsFolder;

    }

    protected DocumentModel appendOutputFileToResultDocument(
            DocumentModel resultDocument, File outputFile) throws IOException {

        if (outputFile != null) {
            Blob resultBlob = Blobs.createBlob(outputFile);
            // TODO LEIRE: hau konstante batetik hartu????
            resultBlob.setMimeType("text/csv");
            Framework.trackFile(outputFile, resultBlob);
            resultBlob.setFilename(outputFile.getName());
            DocumentHelper.addBlob(resultDocument.getProperty(
                    NuxeoMetadataConstants.NX_FILES_FILES), resultBlob);
        }

        session.saveDocument(resultDocument);
        session.save();

        return resultDocument;

    }

    protected File createOutputFile(String inputFileName) {

        String outputFileName = "";
        int dotIndex = inputFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            outputFileName = inputFileName + CSV_IMPORT_RESULTS_SUFFIX;
        } else {
            outputFileName = inputFileName.substring(0, dotIndex)
                    + CSV_IMPORT_RESULTS_SUFFIX
                    + inputFileName.substring(dotIndex);
        }
        String blobsFolderPath = Framework.getProperty(NUXEO_CSV_BLOBS_FOLDER);
        String outputFilePath = FilenameUtils.normalize(blobsFolderPath + "/"
                + CSV_IMPORT_RESULTS_FOLDER + "/" + outputFileName);
        File outputFile = new File(outputFilePath);

        return outputFile;
    }

    protected CSVPrinter createCsvFilePrinter(FileWriter outputFileWriter,
            Map<String, Integer> header) throws IOException {

        Map<String, Integer> resultHeader = new LinkedHashMap<>(header);

        Integer lastValueFromHeader = getLastValueFromHeader(header);
        lastValueFromHeader++;
        resultHeader.put(EloraCSVImportConstants.CSV_RESULT_COL,
                lastValueFromHeader);
        lastValueFromHeader++;
        resultHeader.put(EloraCSVImportConstants.CSV_RESULT_MSG_COL,
                lastValueFromHeader);

        Set<String> resultHeaderSet = resultHeader.keySet();

        String[] headerValues = resultHeaderSet.toArray(
                new String[resultHeaderSet.size()]);

        CSVPrinter csvFilePrinter = new CSVPrinter(outputFileWriter,
                CSVFormat.DEFAULT.withHeader(headerValues).withQuote(
                        '"').withQuoteMode(QuoteMode.ALL));

        return csvFilePrinter;
    }

    protected Integer getLastValueFromHeader(Map<String, Integer> header) {
        Integer lastValue = null;
        for (Map.Entry<String, Integer> headerValue : header.entrySet()) {
            lastValue = headerValue.getValue();
        }
        return lastValue;
    }

    protected void writeResultRecord(CSVPrinter csvFilePrinter,
            CSVRecord record, String resultValue, String resultMsg)
            throws IOException {

        Iterator<String> recordValues = record.iterator();
        Collection<String> resultRecordValues = new ArrayList<String>();
        while (recordValues.hasNext()) {
            resultRecordValues.add(recordValues.next());
        }
        resultRecordValues.add(resultValue);
        resultRecordValues.add(resultMsg);

        csvFilePrinter.printRecord(resultRecordValues);

        csvFilePrinter.flush();
    }

    /* Changes to apply fix https://jira.nuxeo.com/browse/NXP-22746 in NX 7.10
     * =======================================================================
     */

    /**
     * Creates a {@code Blob} from a relative file path. The File will be looked
     * up in the folder registered by the {@code nuxeo.csv.blobs.folder}
     * property.
     *
     * @since 9.3
     */
    protected Blob createBlobFromFilePath(String fileRelativePath)
            throws IOException {
        String blobsFolderPath = Framework.getProperty(NUXEO_CSV_BLOBS_FOLDER);
        String path = FilenameUtils.normalize(
                blobsFolderPath + "/" + fileRelativePath);
        File file = new File(path);
        if (file.exists()) {
            return Blobs.createBlob(file, null, null,
                    FilenameUtils.getName(fileRelativePath));
        } else {
            return null;
        }
    }

    /**
     * Creates a {@code Blob} from a {@code StringBlob}. Assume that the
     * {@code StringBlob} content is the relative file path. The File will be
     * looked up in the folder registered by the {@code nuxeo.csv.blobs.folder}
     * property.
     *
     * @since 9.3
     */
    protected Blob createBlobFromStringBlob(Blob stringBlob)
            throws IOException {
        String fileRelativePath = stringBlob.getString();
        Blob blob = createBlobFromFilePath(fileRelativePath);
        if (blob == null) {
            throw new IOException(
                    String.format("File %s does not exist", fileRelativePath));
        }

        blob.setMimeType(stringBlob.getMimeType());
        blob.setEncoding(stringBlob.getEncoding());
        String filename = stringBlob.getFilename();
        if (filename != null) {
            blob.setFilename(filename);
        }
        return blob;
    }

    /**
     * Recursively replaces all {@code Blob}s with {@code Blob}s created from
     * Files stored in the folder registered by the
     * {@code nuxeo.csv.blobs.folder} property.
     *
     * @since 9.3
     */
    @SuppressWarnings("unchecked")
    protected void replaceBlobs(Map<String, Object> map) throws IOException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Blob) {
                Blob blob = (Blob) value;
                entry.setValue(createBlobFromStringBlob(blob));
            } else if (value instanceof List) {
                replaceBlobs((List<Object>) value);
            } else if (value instanceof Map) {
                replaceBlobs((Map<String, Object>) value);
            }
        }
    }

    /**
     * Recursively replaces all {@code Blob}s with {@code Blob}s created from
     * Files stored in the folder registered by the
     * {@code nuxeo.csv.blobs.folder} property.
     *
     * @since 9.3
     */
    @SuppressWarnings("unchecked")
    protected void replaceBlobs(List<Object> list) throws IOException {
        for (ListIterator<Object> it = list.listIterator(); it.hasNext();) {
            Object value = it.next();
            if (value instanceof Blob) {
                Blob blob = (Blob) value;
                it.set(createBlobFromStringBlob(blob));
            } else if (value instanceof List) {
                replaceBlobs((List<Object>) value);
            } else if (value instanceof Map) {
                replaceBlobs((Map<String, Object>) value);
            }
        }
    }

    /*
    * METHODS TO FIX A BUG FROM NUXEO THAT IGNORES THE NAME OF ATTACHED FILES
    * These methods have been copied from ComplexTypeJSONDecoder and
    * JSONStringBlobDecoder.
    *
    * */

    /* ComplexTypeJSONDecoder */
    private List<Object> decodeListForFiles(ListType lt, String json)
            throws IOException {
        ArrayNode jsonArray = (ArrayNode) mapper.readTree(json);
        return decodeListForFiles(lt, jsonArray);
    }

    private List<Object> decodeListForFiles(ListType lt, ArrayNode jsonArray) {
        List<Object> result = new ArrayList<Object>();
        Type currentObjectType = lt.getFieldType();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode node = jsonArray.get(i);
            if (node.isArray()) {
                result.add(decodeListForFiles((ListType) currentObjectType,
                        (ArrayNode) node));
            } else if (node.isObject()) {
                result.add(decode((ComplexType) currentObjectType,
                        (ObjectNode) node));
            } else if (node.isTextual()) {
                result.add(node.getTextValue());
            } else if (node.isNumber()) {
                result.add(node.getNumberValue());
            } else if (node.isBoolean()) {
                result.add(node.getBooleanValue());
            }
        }
        return result;
    }

    private Object decode(ComplexType ct, ObjectNode jsonObject) {

        Map<String, Object> result = new HashMap<String, Object>();

        String jsonType = "";
        if (jsonObject.has("type")) {
            jsonType = jsonObject.get("type").getTextValue();
        }
        if (jsonType.equals("blob") || ct.getName().equals("content")) {
            return getBlobFromJSON(jsonObject);
        }

        Iterator<Map.Entry<String, JsonNode>> it = jsonObject.getFields();

        while (it.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = it.next();
            if (ct.hasField(nodeEntry.getKey())) {

                Field field = ct.getField(nodeEntry.getKey());
                Type fieldType = field.getType();
                if (fieldType.isSimpleType()) {
                    Object value;
                    if (fieldType == DateType.INSTANCE
                            && nodeEntry.getValue().isIntegralNumber()) {
                        value = Calendar.getInstance();
                        ((Calendar) value).setTimeInMillis(
                                nodeEntry.getValue().getValueAsLong());
                    } else {
                        value = ((SimpleType) fieldType).decode(
                                nodeEntry.getValue().getValueAsText());
                    }
                    result.put(nodeEntry.getKey(), value);
                } else {
                    JsonNode subNode = nodeEntry.getValue();
                    if (subNode.isArray()) {
                        result.put(nodeEntry.getKey(), decodeListForFiles(
                                ((ListType) fieldType), (ArrayNode) subNode));
                    } else {
                        result.put(nodeEntry.getKey(),
                                decode(((ComplexType) fieldType),
                                        (ObjectNode) subNode));
                    }
                }
            }
        }

        return result;
    }

    /* JSONStringBlobDecoder fixed */
    private Blob getBlobFromJSON(ObjectNode jsonObject) {
        Blob blob = null;

        String filename = null;
        if (jsonObject.has("filename")) {
            filename = jsonObject.get("filename").getTextValue();
        }
        if (filename == null && jsonObject.has("name")) {
            filename = jsonObject.get("name").getTextValue();
        }
        String encoding = "UTF-8";
        if (jsonObject.has("encoding")) {
            encoding = jsonObject.get("encoding").getTextValue();
        }

        String mimetype = "text/plain";
        if (jsonObject.has("mime-type")) {
            mimetype = jsonObject.get("mime-type").getTextValue();
        }
        String data = null;
        if (jsonObject.has("data")) {
            data = jsonObject.get("data").getTextValue();
            // try to avoid the bug NXP-18488: data contains the blob url
            // and must not be recognized as a new blob content
            if (data.startsWith("http")) {
                data = null;
            }
        } else if (jsonObject.has("content")) {
            data = jsonObject.get("content").getTextValue();
        }
        if (data == null) {
            return null;
        } else {
            blob = Blobs.createBlob(data, mimetype, encoding, filename);
        }
        return blob;
    }
}
