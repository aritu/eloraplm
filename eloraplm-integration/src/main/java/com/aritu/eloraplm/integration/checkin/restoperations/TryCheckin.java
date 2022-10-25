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

package com.aritu.eloraplm.integration.checkin.restoperations;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.MetadataConfig;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.VersionStatusConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraEventTypes;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.core.util.EloraUnitConversionHelper;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.api.DraftManager;
import com.aritu.eloraplm.integration.checkin.restoperations.util.TryCheckinRequestDoc;
import com.aritu.eloraplm.integration.checkin.restoperations.util.TryCheckinRequestFolder;
import com.aritu.eloraplm.integration.checkin.restoperations.util.TryCheckinResponse;
import com.aritu.eloraplm.integration.checkin.restoperations.util.TryCheckinResponseDoc;
import com.aritu.eloraplm.integration.checkin.restoperations.util.TryCheckinResponseFolder;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.ItemInfo;
import com.aritu.eloraplm.pdm.checkin.util.EloraCheckinHelper;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * @author aritu
 *
 */
@Operation(id = TryCheckin.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Try Checkin", description = "Try to check in documents from the Elora Plm Connector.")
public class TryCheckin {
    public static final String ID = "Elora.PlmConnector.TryCheckin";

    private static final Log log = LogFactory.getLog(TryCheckin.class);

    private static final String RESULT_OK = "OK";

    private static final String RESULT_KO = "KO";

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "calculateOverrideMetadata", required = true)
    private boolean calculateOverrideMetadata;

    @Param(name = "documents", required = false)
    private ArrayList<JsonNode> documents;

    @Param(name = "folders", required = false)
    private ArrayList<JsonNode> folders;

    @Param(name = "setProperties", required = true)
    private boolean setProperties;

    @Param(name = "removeDrafts", required = true)
    private boolean removeDrafts;

    @Param(name = "getItemsInfo", required = true)
    private boolean getItemsInfo;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private TypeManager typeManager = Framework.getService(TypeManager.class);

    private DocumentValidationService validator = Framework.getService(
            DocumentValidationService.class);

    private VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    private Map<Integer, TryCheckinRequestFolder> requestFolders;

    private Map<Integer, TryCheckinRequestDoc> requestDocs;

    private Map<DocumentRef, String> tempFolderPaths;

    private boolean isAllOk;

    private TryCheckinResponse tryCheckinResponse;

    private DraftManager draftManager;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        draftManager = Framework.getService(DraftManager.class);
        tryCheckinResponse = new TryCheckinResponse();
        requestFolders = new LinkedHashMap<Integer, TryCheckinRequestFolder>();
        requestDocs = new LinkedHashMap<Integer, TryCheckinRequestDoc>();
        tempFolderPaths = new HashMap<DocumentRef, String>();
        isAllOk = true;

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (folders != null) {
                loadRequestFolders();
            }
            if (documents != null) {
                loadRequestDocs();
            }

            // Remove all docs and folders' drafts first
            if (removeDrafts) {
                removeExistingDrafts();
            }

            // Process the folders
            if (!requestFolders.isEmpty()) {
                for (TryCheckinRequestFolder requestFolder : requestFolders.values()) {
                    processSingleFolder(requestFolder);
                }
                log.info(logInitMsg + requestFolders.size()
                        + " folders processed.");
            } else {
                log.info(logInitMsg + "No folder to process.");
            }
            // Process the documents
            if (!requestDocs.isEmpty()) {
                for (TryCheckinRequestDoc requestDoc : requestDocs.values()) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            session.save();

            log.info(logInitMsg
                    + "Documents and/or folders successfuly created or modified.");
            tryCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            tryCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            tryCheckinResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            tryCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            tryCheckinResponse.setErrorMessage(e.getMessage());
            tryCheckinResponse.emptyDocuments();
            tryCheckinResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            tryCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            tryCheckinResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            tryCheckinResponse.emptyDocuments();
            tryCheckinResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // If all is not OK, empty override metadata
        if (!isAllOk) {
            tryCheckinResponse.emptyOverrideMetadata();
        }

        // Create JSON response
        String jsonResponse = tryCheckinResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    /**
     * Loads the folders to check in
     *
     * @throws EloraException
     */
    private void loadRequestFolders() throws EloraException {

        String logInitMsg = "[loadRequestFolders] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + folders.size() + " folders found.");

        for (int i = 0; i < folders.size(); ++i) {
            JsonNode folderItem = folders.get(i);

            int localId = EloraJsonHelper.getJsonFieldAsInt(folderItem,
                    "localId", true);

            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "wcUid", false);

            String type = EloraJsonHelper.getJsonFieldAsString(folderItem,
                    "type", true);
            if (!typeManager.hasType(type)) {
                throw new EloraException("Invalid type |" + type
                        + "| for localId |" + localId + "|.");
            }
            // TODO tipo batzuk filtrau?? Folderish begiratu?

            String title = EloraJsonHelper.getJsonFieldAsString(folderItem,
                    "title", false);

            DocumentRef structureRootRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "structureRootRealUid", false);

            tempFolderPaths = populateTempFoldersMap(session, tempFolderPaths,
                    structureRootRealRef);

            TryCheckinRequestFolder requestFolder = new TryCheckinRequestFolder(
                    localId, wcRef, type, title, structureRootRealRef);
            requestFolders.put(localId, requestFolder);
        }

        log.trace(logInitMsg + requestFolders.size() + " folders loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Loads the documents to check in
     *
     * @throws EloraException
     */
    private void loadRequestDocs() throws EloraException {

        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + documents.size() + " documents found.");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            int localId = EloraJsonHelper.getJsonFieldAsInt(docItem, "localId",
                    true);

            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", false);

            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "wcUid", false);

            String type = EloraJsonHelper.getJsonFieldAsString(docItem, "type",
                    true);
            if (!typeManager.hasType(type)) {
                throw new EloraException("Invalid type |" + type
                        + "| for localId |" + localId + "|.");
            }
            // TODO tipo batzuk filtrau??

            String filename = EloraJsonHelper.getJsonFieldAsString(docItem,
                    "filename", false);

            DocumentRef structureRootRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "structureRootRealUid", false);

            if (structureRootRealRef == null) {
                structureRootRealRef = getStructureRootRealRef(session,
                        realRef);
            }

            boolean overwrite = EloraJsonHelper.getJsonFieldAsBoolean(docItem,
                    "overwrite", false);

            tempFolderPaths = populateTempFoldersMap(session, tempFolderPaths,
                    structureRootRealRef);

            TryCheckinRequestDoc requestDoc = new TryCheckinRequestDoc(localId,
                    realRef, wcRef, type, filename, structureRootRealRef,
                    overwrite);

            // TODO Hau "kendu" 2+ TRYean?
            JsonNode properties = EloraJsonHelper.getJsonNode(docItem,
                    "properties", false);
            if (properties != null) {
                for (JsonNode propItem : properties) {
                    String property = EloraJsonHelper.getJsonFieldAsString(
                            propItem, "property", false);
                    String value = EloraJsonHelper.getJsonFieldAsString(
                            propItem, "value", false, false, true);
                    if (property != null && value != null) {

                        // Uppercase the reference
                        if (property.equals(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)) {
                            value = value.toUpperCase();
                        }
                        // Round decimal values
                        else if (EloraUnitConversionHelper.isDecimalProperty(
                                property)
                                && (value != null && !value.isEmpty()
                                        && !value.equals("0"))) {
                            value = EloraUnitConversionHelper.roundDecimalValue(
                                    property, value);
                        }

                        requestDoc.addProperty(property, value);
                    }
                }
            }

            requestDocs.put(localId, requestDoc);
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void removeExistingDrafts() throws EloraException {

        String query = EloraQueryFactory.getAllDraftsByCreatorQuery(
                session.getPrincipal().getName());

        DocumentModelList userDrafts = session.query(query);
        if (!userDrafts.isEmpty()) {
            for (DocumentModel draft : userDrafts) {
                session.removeDocument(draft.getRef());
            }
        }
    }

    public static DocumentRef getStructureRootRealRef(CoreSession session,
            DocumentRef realRef) throws EloraException {
        DocumentRef structureRootRealRef;
        if (realRef == null) {
            throw new EloraException(
                    "A new document without realRef does not have the structureRootRealRef.");
        } else {

            DocumentModel wcDoc = session.getWorkingCopy(realRef);

            String structureRootRealUid = EloraStructureHelper.getEloraRootFolderUid(
                    wcDoc, session);
            structureRootRealRef = new IdRef(structureRootRealUid);
        }

        return structureRootRealRef;
    }

    /**
     * Populates the temp folders map if there is not any item for the given
     * structure root; if structure root is null, it is calculated from the real
     * document
     *
     * @param tempFolderPaths
     * @param structureRootRealRef
     * @param realRef
     * @return
     * @throws EloraException
     */
    private static Map<DocumentRef, String> populateTempFoldersMap(
            CoreSession session, Map<DocumentRef, String> tempFolderPaths,
            DocumentRef structureRootRealRef) throws EloraException {
        if (structureRootRealRef == null) {
            throw new EloraException("structureRootRealRef is null.");
        }

        if (!tempFolderPaths.containsKey(structureRootRealRef)) {
            String tempFolderPath = EloraStructureHelper.getTempFolderPath(
                    structureRootRealRef, session);
            tempFolderPaths.put(structureRootRealRef, tempFolderPath);
        }

        return tempFolderPaths;
    }

    /**
     * Creates a new folder in temp directory, or updates its metadata if it
     * already exists
     *
     * @param requestFolder
     * @throws EloraException
     */
    private void processSingleFolder(TryCheckinRequestFolder requestFolder)
            throws EloraException {

        String logInitMsg = "[processSingleFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // If it is a new folder, create an empty folder and a draft in Temp
        if (requestFolder.getWcRef() == null) {
            processNewFolder(requestFolder);

        } else {
            processExistingFolder(requestFolder);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Processes a new folder, creating an empty one and its draft
     *
     * @param requestFolder
     * @param draftType
     * @throws EloraException
     */
    private void processNewFolder(TryCheckinRequestFolder requestFolder)
            throws EloraException {
        String logInitMsg = "[processNewFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String nameForPath = pss.generatePathSegment(requestFolder.getTitle());

        // Create empty folder
        DocumentModel newFolder = session.createDocumentModel(
                draftManager.getTempFolderPath(session,
                        requestFolder.getStructureRootRealRef()),
                nameForPath, requestFolder.getType());
        newFolder.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                requestFolder.getTitle());
        newFolder = session.createDocument(newFolder);

        log.trace(logInitMsg + "Empty folder |" + newFolder.getId()
                + "| created.");

        // Create draft folder
        DocumentModel draftFolder = draftManager.createDraftForDocument(
                newFolder, requestFolder.getStructureRootRealRef());

        session.save();

        // Raise BEFORE_TCI_FOLDER_VALIDATION event
        EloraEventHelper.fireEvent(EloraEventTypes.BEFORE_TCI_FOLDER_VALIDATION,
                draftFolder);

        TryCheckinResponseFolder responseFolder = new TryCheckinResponseFolder(
                requestFolder.getLocalId(), newFolder.getId(),
                requestFolder.getType(), requestFolder.getTitle());

        // Validate the folder
        List<ValidationErrorItem> errorList = EloraCheckinHelper.checkForErrors(
                draftFolder, validator, session);
        errorList.addAll(
                EloraCheckinHelper.validateCadDocument(draftFolder, session));
        if (errorList.isEmpty()) {
            responseFolder.setResult(RESULT_OK);
        } else {
            responseFolder.setResult(RESULT_KO);
            responseFolder.addErrorList(errorList);
            isAllOk = false;
        }

        tryCheckinResponse.addFolder(responseFolder);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Processes an existing folder, creating its draft
     *
     * @param requestFolder
     * @param draftType
     * @throws EloraException
     */
    private void processExistingFolder(TryCheckinRequestFolder requestFolder)
            throws EloraException {
        String logInitMsg = "[processExistingFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (session.exists(requestFolder.getWcRef())) {
            DocumentModel existingFolder = session.getDocument(
                    requestFolder.getWcRef());
            if (existingFolder == null) {
                throw new EloraException("Folder with uid |"
                        + requestFolder.getWcRef().toString()
                        + "| does not exist.");
            }
            if (!existingFolder.isFolder()) {
                throw new EloraException(
                        "Provided folder |" + existingFolder.getId()
                                + "| is not of a folderish type. Its type is |"
                                + existingFolder.getType() + "|");
            }

            DocumentModel draftFolder = draftManager.getDraftForDocument(
                    session, existingFolder, session.getPrincipal().getName(),
                    false);

            if (draftFolder == null) {
                draftFolder = draftManager.createDraftForDocument(
                        existingFolder,
                        requestFolder.getStructureRootRealRef());
            }

            // Raise BEFORE_TCI_FOLDER_VALIDATION event
            EloraEventHelper.fireEvent(
                    EloraEventTypes.BEFORE_TCI_FOLDER_VALIDATION, draftFolder);

            TryCheckinResponseFolder responseFolder = new TryCheckinResponseFolder(
                    requestFolder.getLocalId(), existingFolder.getId(),
                    requestFolder.getType(), requestFolder.getTitle());

            // Validate the folder
            List<ValidationErrorItem> errorList = EloraCheckinHelper.checkForErrors(
                    draftFolder, validator, session);
            errorList.addAll(EloraCheckinHelper.validateCadDocument(draftFolder,
                    session));
            if (errorList.isEmpty()) {
                responseFolder.setResult(RESULT_OK);
            } else {
                responseFolder.setResult(RESULT_KO);
                responseFolder.addErrorList(errorList);
                isAllOk = false;
            }

            tryCheckinResponse.addFolder(responseFolder);

            log.trace(logInitMsg + "--- EXIT --- ");

        } else {
            // Create a new folder
            processNewFolder(requestFolder);
        }
    }

    /**
     * Processes a document depending if it is new or an existing one
     *
     * @param requestDoc
     * @throws IOException
     * @throws EloraException
     */
    private void processSingleDocument(TryCheckinRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        TryCheckinResponseDoc responseDoc;
        if (requestDoc.getRealRef() == null && requestDoc.getWcRef() == null) {
            // New document, 1st TryCheckin
            responseDoc = processNewDocument(requestDoc);
        } else {
            if (requestDoc.getRealRef() == null) {
                // New document, 2+ TryCheckin
                responseDoc = processNewDocumentWithDraft(requestDoc);
            } else {
                // Existing document
                responseDoc = processExistingDocument(requestDoc);
            }
        }
        tryCheckinResponse.addDocument(responseDoc);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Creates a new document according to the provided metadata and its
     * response
     *
     * @param requestDoc
     * @return
     * @throws IOException
     * @throws EloraException
     */
    private TryCheckinResponseDoc processNewDocument(
            TryCheckinRequestDoc requestDoc)
            throws IOException, EloraException {
        String logInitMsg = "[processNewDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String nameForPath = pss.generatePathSegment(requestDoc.getFilename());

        // Create and process an empty document
        DocumentModel newDoc = session.createDocumentModel(
                draftManager.getTempFolderPath(session,
                        requestDoc.getStructureRootRealRef()),
                nameForPath, requestDoc.getType());
        newDoc.putContextData(
                LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                EloraLifeCycleConstants.PRECREATED);
        newDoc = createDocWithoutValidation(newDoc);
        log.trace(logInitMsg + "Empty document |" + newDoc.getId()
                + "| created.");

        DocumentModel draftDoc = draftManager.createDraftForDocument(newDoc,
                requestDoc.getStructureRootRealRef());

        errorList = updateDraft(requestDoc, draftDoc, errorList, null);

        draftDoc = saveDocWithoutValidation(draftDoc);

        try {
            newDoc = EloraDocumentHelper.lockDocument(newDoc);
        } catch (DocumentAlreadyLockedException
                | DocumentInUnlockableStateException
                | DocumentLockRightsException e) {
            throw new EloraException(e);
        }

        // Create the response
        TryCheckinResponseDoc responseDoc = createNewDocResponse(requestDoc,
                newDoc, draftDoc, errorList);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    /**
     * Updates a new document with existing draft (2+ round TryCheckIn)
     * according to the provided metadata and creates a response
     *
     * @param requestDoc
     * @return
     * @throws IOException
     * @throws EloraException
     */
    private TryCheckinResponseDoc processNewDocumentWithDraft(
            TryCheckinRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processNewDocumentWithDraft] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();

        if (!session.exists(requestDoc.getWcRef())) {
            errorList.add(new ValidationErrorItem("wcUid",
                    EloraMessageHelper.getTranslatedMessage(session,
                            "com.aritu.eloraplm.integration.error.wcUidDoesNotExist")));
            log.trace(logInitMsg + "No document with wc uid |"
                    + requestDoc.getWcRef().toString() + "|");
            return processDocNotExistingError(requestDoc, errorList);
        }
        DocumentModel newDoc = session.getDocument(requestDoc.getWcRef());

        DocumentModel draftDoc = draftManager.getDraftForDocument(session,
                newDoc, session.getPrincipal().getName(), false);

        // If the draft doesn't exist, we asume that it is a new document that
        // has been saved once: create its draft
        if (draftDoc == null) {
            draftDoc = draftManager.createDraftForDocument(newDoc,
                    requestDoc.getStructureRootRealRef());
        }

        errorList = updateDraft(requestDoc, draftDoc, errorList,
                newDoc.getId());
        draftDoc = saveDocWithoutValidation(draftDoc);
        log.trace(logInitMsg + "Document draft |" + draftDoc.getId()
                + "| updated.");

        TryCheckinResponseDoc responseDoc = createNewDocResponse(requestDoc,
                newDoc, draftDoc, errorList);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    /**
     * Updates an existing document's draft with the provided metadata and
     * creates a response
     *
     * @param requestDoc
     * @return
     * @throws IOException
     * @throws EloraException
     */
    private TryCheckinResponseDoc processExistingDocument(
            TryCheckinRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processExistingDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();

        // If document does not exist, return KO.
        if (!session.exists(requestDoc.getRealRef())) {
            errorList.add(new ValidationErrorItem("realUid",
                    EloraMessageHelper.getTranslatedMessage(session,
                            "com.aritu.eloraplm.integration.error.realUidDoesNotExist")));
            log.trace(logInitMsg + "No document with real uid |"
                    + requestDoc.getRealRef().toString() + "|");
            return processDocNotExistingError(requestDoc, errorList);
        }

        // Get the existing document and check that it is correct
        DocumentModel existingDoc = session.getDocument(
                requestDoc.getRealRef());
        if (existingDoc.isProxy()) {
            throw new EloraException(
                    "Document's real uid is a proxy, and it should be an archived version.");
        }
        if (!existingDoc.isVersion()) {
            throw new EloraException(
                    "Document's real uid is a working copy, and it should be an archived version.");
        }

        // Get the working copy
        DocumentModel wcDoc = null;
        if (requestDoc.getWcRef() == null) {
            wcDoc = session.getSourceDocument(requestDoc.getRealRef());
        } else {
            if (!session.exists(requestDoc.getWcRef())) {
                // TODO erroreak kodifikau
                errorList.add(new ValidationErrorItem("wcUid",
                        "The document with the given wc uid does not exist."));
                log.trace(logInitMsg + "No document with wc uid |"
                        + requestDoc.getWcRef().toString() + "|");
                return processDocNotExistingError(requestDoc, errorList);
            }

            wcDoc = session.getDocument(requestDoc.getWcRef());
        }

        // Get or create the draft
        DocumentModel draftDoc = null;
        draftDoc = draftManager.getDraftForDocument(session, wcDoc,
                session.getPrincipal().getName(), false);
        if (draftDoc == null) {
            draftDoc = draftManager.createDraftForDocument(existingDoc,
                    requestDoc.getStructureRootRealRef());
        }

        // Update the draft
        errorList = updateDraft(requestDoc, draftDoc, errorList, wcDoc.getId());

        // Save the document (we turn off validation because we control it later
        // with the result)
        draftDoc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.TURN_OFF);

        draftDoc = session.saveDocument(draftDoc);

        log.trace(logInitMsg + "Document |" + wcDoc.getId() + "| updated.");

        draftDoc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.USUAL);
        session.save();

        // Create the response
        TryCheckinResponseDoc responseDoc = createExistingDocResponse(
                requestDoc, wcDoc, existingDoc, draftDoc, errorList);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    /**
     * Creates the response when a "DocNotExisting" error has occurred
     *
     * @param requestDoc
     * @param errorList
     * @return
     */
    private TryCheckinResponseDoc processDocNotExistingError(
            TryCheckinRequestDoc requestDoc,
            List<ValidationErrorItem> errorList) {

        String logInitMsg = "[processDocNotExistingError] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String title = null;
        String description = null;
        String reference = null;
        Properties properties = requestDoc.getProperties();
        if (properties.containsKey(NuxeoMetadataConstants.NX_DC_TITLE)
                && properties.get(NuxeoMetadataConstants.NX_DC_TITLE) != null) {
            title = properties.get(
                    NuxeoMetadataConstants.NX_DC_TITLE).toString();
        }
        if (properties.containsKey(NuxeoMetadataConstants.NX_DC_DESCRIPTION)
                && properties.get(
                        NuxeoMetadataConstants.NX_DC_DESCRIPTION) != null) {
            description = properties.get(
                    NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        }
        if (properties.containsKey(EloraMetadataConstants.ELORA_ELO_REFERENCE)
                && properties.get(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE) != null) {
            reference = properties.get(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        }

        String wcUid = requestDoc.getWcRef() != null
                ? requestDoc.getWcRef().toString()
                : "";
        TryCheckinResponseDoc responseDoc = new TryCheckinResponseDoc(
                requestDoc.getLocalId(), wcUid, requestDoc.getType(), reference,
                title, description, requestDoc.getFilename(), RESULT_KO,
                errorList);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    /**
     * Process PlmToCad, Bidirectional and CadToPlm metadata to override in the
     * binary file
     *
     * @param responseDoc
     * @param wcDoc
     * @return
     * @throws EloraException
     */
    private TryCheckinResponseDoc processMetadata(
            TryCheckinResponseDoc responseDoc, DocumentModel draftDoc,
            DocumentModel wcDoc, String type, boolean overwrite)
            throws EloraException {

        String logInitMsg = "[processMetadata] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Serializable authoringTool = draftDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL);
        if (authoringTool == null) {
            throw new EloraException(
                    "The draft has no cad:authoringTool metadata.");
        }

        processRealMetadata(responseDoc, draftDoc, authoringTool.toString(),
                type);

        processVirtualMetadata(responseDoc, wcDoc, authoringTool.toString(),
                type, overwrite);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    private void processRealMetadata(TryCheckinResponseDoc responseDoc,
            DocumentModel draftDoc, String authoringTool, String type) {
        if (MetadataConfig.getRealMetadataMapByType().containsKey(authoringTool)
                && MetadataConfig.getRealMetadataMapByType().get(
                        authoringTool).containsKey(type)) {
            for (String property : MetadataConfig.getRealMetadataMapByType().get(
                    authoringTool).get(type)) {
                // Data for real metadata is in draftDoc
                Serializable value = draftDoc.getPropertyValue(property);
                responseDoc.addOverrideMetadata(property, value);
            }
        }
    }

    private void processVirtualMetadata(TryCheckinResponseDoc responseDoc,
            DocumentModel wcDoc, String authoringTool, String type,
            boolean overwrite) throws EloraException {
        if (MetadataConfig.getVirtualMetadataMapByType().containsKey(
                authoringTool)
                && MetadataConfig.getVirtualMetadataMapByType().get(
                        authoringTool).containsKey(type)) {
            for (String property : MetadataConfig.getVirtualMetadataMapByType().get(
                    authoringTool).get(type)) {
                // If it is a virtual metadata, call to getDataFromMethod
                String operation = overwrite
                        ? EloraIntegrationHelper.OPERATION_TRY_OVERWRITE
                        : EloraIntegrationHelper.OPERATION_TRY_CHECKIN;
                Serializable value = EloraIntegrationHelper.getVirtualMetadata(
                        session, wcDoc, property, operation);
                responseDoc.addOverrideMetadata(property, value);
            }
        }
    }

    /**
     * Updates draft's properties (if it is a 2+ Try), raises
     * BEFORE_TCI_DOC_VALIDATION event, and checks that reference + type is
     * unique
     *
     * @param requestDoc
     * @param draftDoc
     * @param errorList
     * @return
     * @throws PropertyException
     * @throws IOException
     */
    private List<ValidationErrorItem> updateDraft(
            TryCheckinRequestDoc requestDoc, DocumentModel draftDoc,
            List<ValidationErrorItem> errorList, String wcUid)
            throws PropertyException, IOException {

        // The properties will be overriden only if it is the first call to
        // TryCheckin, so that we don't loose any change made between the first
        // TryCheckin and this one, to correct any metadata.
        if (setProperties) {
            for (Entry<String, String> entry : requestDoc.getProperties().entrySet()) {
                draftDoc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        }

        // Raise BEFORE_TCI_DOC_VALIDATION event
        EloraEventHelper.fireEvent(EloraEventTypes.BEFORE_TCI_DOC_VALIDATION,
                draftDoc);

        UniqueReferenceChecker checker = new UniqueReferenceChecker(session,
                errorList, draftDoc, requestDoc, wcUid);
        errorList = checker.check();

        return errorList;
    }

    /**
     * Creates the document (we turn off validation because we control it later
     * with the result)
     *
     * @param doc
     * @return
     */
    private DocumentModel createDocWithoutValidation(DocumentModel doc) {
        doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.TURN_OFF);
        doc = session.createDocument(doc);
        doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.USUAL);
        session.save();

        return doc;
    }

    /**
     * Saves the document (we turn off validation because we control it later
     * with the result)
     *
     * @param doc
     * @return
     */
    private DocumentModel saveDocWithoutValidation(DocumentModel doc) {
        doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.TURN_OFF);
        doc = session.saveDocument(doc);
        doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.USUAL);
        session.save();

        return doc;
    }

    /**
     * Creates the response for new documents
     *
     * @param requestDoc
     * @param newDoc
     * @param draftDoc
     * @param errorList
     * @return
     * @throws EloraException
     */
    private TryCheckinResponseDoc createNewDocResponse(
            TryCheckinRequestDoc requestDoc, DocumentModel newDoc,
            DocumentModel draftDoc, List<ValidationErrorItem> errorList)
            throws EloraException {
        String wcUid = newDoc.getId();

        String fileContentHash = "";

        String currentVersionLabel = versionLabelService.getZeroVersion();
        VersionInfo currentVersionInfo = new VersionInfo(currentVersionLabel,
                VersionStatusConstants.VERSION_STATUS_NORMAL, "");

        String nextVersionLabel = versionLabelService.getZeroNextVersion();
        String latestVersionLabel = currentVersionLabel;

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(newDoc);
        String username = session.getPrincipal().getName();

        // Get the edition URL
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String editionUrl = EloraUrlHelper.getDocumentEditionInPrintModeUrl(
                request, draftDoc);

        String title = draftDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : draftDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = draftDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : draftDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String reference = draftDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : draftDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        TryCheckinResponseDoc responseDoc = new TryCheckinResponseDoc(
                requestDoc.getLocalId(), wcUid, requestDoc.getType(), reference,
                title, description, requestDoc.getFilename(), fileContentHash,
                currentVersionInfo, newDoc.getCurrentLifeCycleState(),
                nextVersionLabel, latestVersionLabel, eloraLockInfo, username,
                new Date(), editionUrl);

        // Get the metadata to override (PlmToCad + Bidirectional) if it is
        // needed
        if (calculateOverrideMetadata) {
            responseDoc = processMetadata(responseDoc, draftDoc, newDoc,
                    requestDoc.getType(), requestDoc.isOverwrite());
        }

        // Validate the document (CAD + schema validation)
        errorList.addAll(
                EloraCheckinHelper.validateCadDocument(draftDoc, session));
        errorList.addAll(EloraCheckinHelper.checkForErrors(draftDoc, validator,
                session));
        if (errorList.isEmpty()) {
            responseDoc.setResult(RESULT_OK);
        } else {
            responseDoc.setResult(RESULT_KO);
            responseDoc.addErrorList(errorList);
            isAllOk = false;
        }

        return responseDoc;
    }

    /**
     * Creates the response for existing documents
     *
     * @param requestDoc
     * @param newDoc
     * @param draftDoc
     * @param errorList
     * @return
     * @throws EloraException
     */
    private TryCheckinResponseDoc createExistingDocResponse(
            TryCheckinRequestDoc requestDoc, DocumentModel wcDoc,
            DocumentModel existingDoc, DocumentModel draftDoc,
            List<ValidationErrorItem> errorList) throws EloraException {
        String wcUid = wcDoc.getId();

        String fileContentHash = "";
        Blob contentBlob = (Blob) existingDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        if (contentBlob != null) {
            fileContentHash = contentBlob.getDigest();
        }

        VersionInfo currentVersionInfo = EloraIntegrationHelper.createVersionInfo(
                existingDoc, wcDoc);

        String nextVersionLabel = EloraDocumentHelper.calculateNextVersionLabel(
                versionLabelService, wcDoc);
        // For EloraPlm, WC is always the "latest" version, even if it is
        // restored to an older version
        String latestVersionLabel = wcDoc.getVersionLabel();

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(wcDoc);
        String username = session.getPrincipal().getName();

        // Get the edition URL
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String editionUrl = EloraUrlHelper.getDocumentEditionInPrintModeUrl(
                request, draftDoc);

        String title = draftDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : draftDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = draftDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : draftDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String reference = draftDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : draftDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        TryCheckinResponseDoc responseDoc = new TryCheckinResponseDoc(
                requestDoc.getLocalId(), wcUid, requestDoc.getType(), reference,
                title, description, requestDoc.getFilename(), fileContentHash,
                currentVersionInfo, existingDoc.getCurrentLifeCycleState(),
                nextVersionLabel, latestVersionLabel, eloraLockInfo, username,
                new Date(), editionUrl);

        // Get the metadata to override (PlmToCad + Bidirectional) if it is
        // needed
        if (calculateOverrideMetadata) {
            responseDoc = processMetadata(responseDoc, draftDoc, wcDoc,
                    requestDoc.getType(), requestDoc.isOverwrite());
        }

        // Get items info
        if (getItemsInfo) {
            List<ItemInfo> itemsInfo = EloraIntegrationHelper.getItemsInfo(
                    session, wcDoc, true);
            responseDoc.setItemsInfo(itemsInfo);
        }

        // Validate the document (CAD + schema validation)
        errorList.addAll(
                EloraCheckinHelper.validateCadDocument(draftDoc, session));
        errorList.addAll(EloraCheckinHelper.checkForErrors(draftDoc, validator,
                session));
        if (errorList.isEmpty()) {
            responseDoc.setResult(RESULT_OK);
        } else {
            responseDoc.setResult(RESULT_KO);
            responseDoc.addErrorList(errorList);
            isAllOk = false;
        }

        return responseDoc;
    }

    class UniqueReferenceChecker extends UnrestrictedSessionRunner {
        List<ValidationErrorItem> errorList;

        DocumentModel draftDoc;

        TryCheckinRequestDoc requestDoc;

        String wcUid;

        private UniqueReferenceChecker(CoreSession session,
                List<ValidationErrorItem> errorList, DocumentModel draftDoc,
                TryCheckinRequestDoc requestDoc, String wcUid) {
            super(session);
            this.errorList = errorList;
            this.draftDoc = draftDoc;
            this.requestDoc = requestDoc;
            this.wcUid = wcUid;
        }

        @Override
        public void run() {

            String logInitMsg = "[UniqueReferenceChecker] ["
                    + session.getPrincipal().getName() + "] ";

            // Check that reference + type is unique, if not => KO
            String reference = draftDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                            : draftDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

            if (reference != null && !reference.isEmpty()) {
                long uniqueErrorDrafts = 0;
                long uniqueErrorDocs = 0;

                // Check drafts
                log.trace(logInitMsg + "Checking that draft's reference |"
                        + reference + "| for type |" + draftDoc.getType()
                        + "| and user |" + session.getPrincipal().getName()
                        + "| is unique...");

                uniqueErrorDrafts = EloraQueryFactory.countWcDocsByTypeAndReferenceAndCreatorExcludingUid(
                        session, draftDoc.getType(), reference,
                        session.getPrincipal().getName(), draftDoc.getId());

                // Check documents
                log.trace(logInitMsg + "Checking that doc's reference |"
                        + reference + "| for type |" + requestDoc.getType()
                        + "| is unique...");
                if (wcUid == null) {
                    uniqueErrorDocs = EloraQueryFactory.countWcDocsByTypeAndReference(
                            session, requestDoc.getType(), reference);
                } else {
                    uniqueErrorDocs = EloraQueryFactory.countWcDocsByTypeAndReferenceExcludingUid(
                            session, requestDoc.getType(), reference, wcUid);
                }

                if (uniqueErrorDrafts > 0 || uniqueErrorDocs > 0) {

                    log.error(logInitMsg + "Reference |" + reference
                            + "| already exists for type |"
                            + requestDoc.getType() + "|. Drafts: |"
                            + uniqueErrorDrafts + "| Docs: |" + uniqueErrorDocs
                            + "|");

                    errorList.add(new ValidationErrorItem(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE,
                            EloraMessageHelper.getTranslatedMessage(session,
                                    "com.aritu.eloraplm.integration.error.sameReferenceOfSameType")));
                } else {
                    log.trace(logInitMsg + "References are unique.");
                }
            }
        }

        public List<ValidationErrorItem> check() {
            runUnrestricted();
            return errorList;
        }
    }
}
