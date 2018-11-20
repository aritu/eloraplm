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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventTypes;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.checkin.restoperations.util.SaveRequestDoc;
import com.aritu.eloraplm.integration.checkin.restoperations.util.SaveResponse;
import com.aritu.eloraplm.integration.checkin.restoperations.util.SaveResponseDoc;
import com.aritu.eloraplm.integration.util.EloraCheckinHelper;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

/**
 * @author aritu
 *
 */
@Deprecated
@Operation(id = Save.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Save", description = "Save document from the Elora Plm Connector.")
public class Save {
    public static final String ID = "Elora.PlmConnector.Save";

    private static final Log log = LogFactory.getLog(Save.class);

    protected static final String RESULT_OK = "OK";

    protected static final String RESULT_KO = "KO";

    @Param(name = "documents", required = false)
    protected ArrayList<JsonNode> documents;

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    protected TypeManager typeManager = Framework.getService(TypeManager.class);

    protected DocumentValidationService validator = Framework.getService(
            DocumentValidationService.class);

    protected EloraVersionLabelService versionLabelService = Framework.getService(
            EloraVersionLabelService.class);

    protected Map<Integer, SaveRequestDoc> requestDocs;

    protected SaveResponse saveResponse;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        saveResponse = new SaveResponse();
        requestDocs = new LinkedHashMap<Integer, SaveRequestDoc>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (documents != null) {
                loadRequestDocs();
            }

            // Process the documents
            if (!requestDocs.isEmpty()) {
                for (SaveRequestDoc requestDoc : requestDocs.values()) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            session.save();

            log.info(logInitMsg + "Document successfuly saved.");
            saveResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            saveResponse.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            saveResponse.setErrorMessage(e.getMessage());
            saveResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            saveResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            saveResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            saveResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = saveResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    /**
     * Loads the documents to check in
     *
     * @throws EloraException
     */
    // TODO: En un futuro unir los elementos comunes que se recogen en los
    // loadRequest y crear una clase. Por ejemplo, se puede crear una clase
    // loadRequest general para docheckin y save.
    protected void loadRequestDocs() throws EloraException {

        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            int localId = EloraJsonHelper.getJsonFieldAsInt(docItem, "localId",
                    true);
            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", false);
            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "wcUid", false);
            DocumentRef parentRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "parentRealUid", false);

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

            SaveRequestDoc requestDoc = new SaveRequestDoc(localId, realRef,
                    wcRef, parentRealRef, type, filename, structureRootRealRef);

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

                        requestDoc.addProperty(property, value);
                    }
                }
            }

            // Get content file
            JsonNode mainFile = EloraJsonHelper.getJsonNode(docItem,
                    "contentFile", false);
            if (mainFile != null) {
                int fileId = EloraJsonHelper.getJsonFieldAsInt(mainFile,
                        "fileId", true);
                String batch = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "batch", true);
                String hash = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "hash", true);
                requestDoc.setContentFile(fileId, batch, hash);
            }

            // Get elora viewer file
            JsonNode eloraViewerFile = EloraJsonHelper.getJsonNode(docItem,
                    "eloraViewerFile", false);

            if (eloraViewerFile != null) {
                String fileName = EloraJsonHelper.getJsonFieldAsString(
                        eloraViewerFile, "filename", true);
                int fileId = EloraJsonHelper.getJsonFieldAsInt(eloraViewerFile,
                        "fileId", true);
                String batch = EloraJsonHelper.getJsonFieldAsString(
                        eloraViewerFile, "batch", true);
                String hash = EloraJsonHelper.getJsonFieldAsString(
                        eloraViewerFile, "hash", true);
                requestDoc.setViewerFile(fileId, fileName, batch, hash);
            }

            // Get attached files
            JsonNode cadAttachments = EloraJsonHelper.getJsonNode(docItem,
                    "cadAttachments", false);
            if (cadAttachments != null) {
                for (JsonNode attItem : cadAttachments) {
                    String fileName = EloraJsonHelper.getJsonFieldAsString(
                            attItem, "filename", true);
                    int fileId = EloraJsonHelper.getJsonFieldAsInt(attItem,
                            "fileId", true);
                    String batch = EloraJsonHelper.getJsonFieldAsString(attItem,
                            "batch", true);
                    String hash = EloraJsonHelper.getJsonFieldAsString(attItem,
                            "hash", true);
                    requestDoc.addCadAttachmentFile(fileId, fileName, batch,
                            hash);
                }
            }
            requestDocs.put(localId, requestDoc);
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Processes a document depending if it is new or an existing one
     *
     * @param requestDoc
     * @throws IOException
     * @throws EloraException
     */
    protected void processSingleDocument(SaveRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        SaveResponseDoc responseDoc;
        if (requestDoc.getRealRef() == null && requestDoc.getWcRef() == null) {
            // New document, 1st Save
            responseDoc = processNewDocument(requestDoc);
        } else {

            // Existing document
            responseDoc = processExistingDocument(requestDoc);
        }
        log.trace(logInitMsg + "--- EXIT --- ");

        saveResponse.addDocument(responseDoc);
    }

    /**
     * Creates a new document according to the provided metadata and its
     * response
     *
     * @param requestDoc
     * @return
     * @throws IOException
     * @throws EloraException
     * @throws DocumentLockRightsException
     */
    protected SaveResponseDoc processNewDocument(SaveRequestDoc requestDoc)
            throws IOException, EloraException {
        String logInitMsg = "[processNewDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentRef structureRootRealRef = requestDoc.getStructureRootRealRef();
        if (structureRootRealRef == null) {
            throw new EloraException(
                    "New document without structureRootRealRef. wcUid: |"
                            + requestDoc.getWcRef().toString() + "|");
        }

        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();

        String type = requestDoc.getType();
        String targetFolderPath = EloraStructureHelper.getCadPathByType(
                structureRootRealRef, type, session);

        // Create and process document
        DocumentModel newDoc = session.createDocumentModel(targetFolderPath,
                requestDoc.getFilename(), requestDoc.getType());

        // Add uploaded files
        EloraCheckinHelper.addDocumentFiles(newDoc, requestDoc, session);

        errorList = updateDoc(requestDoc, newDoc, errorList, null);
        newDoc = createDocWithoutValidation(newDoc);
        log.trace(logInitMsg + "New document |" + newDoc.getId() + "| saved.");

        // Lock document
        try {
            newDoc = EloraDocumentHelper.lockDocument(newDoc);
        } catch (DocumentAlreadyLockedException
                | DocumentInUnlockableStateException
                | DocumentLockRightsException e) {
            throw new EloraException(e);
        }

        // Create the response
        SaveResponseDoc responseDoc = createNewDocResponse(requestDoc, newDoc,
                errorList);

        String proxyUid = null;
        if (requestDoc.getParentRealRef() != null) {
            // Create a proxy in the input path (if it doesn't exist)
            proxyUid = createDocumentProxy(requestDoc, newDoc);
            responseDoc.setProxyUid(proxyUid);
        }

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
    protected SaveResponseDoc processExistingDocument(SaveRequestDoc requestDoc)
            throws IOException, EloraException {

        String logInitMsg = "[processExistingDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel wcDoc = null;
        DocumentModel realDoc = null;
        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();
        // wcRef != null if saved document is not checked in. Saving process but
        // never checked in realRef != null if saved document already checked
        // in.
        if (requestDoc.getRealRef() != null) {
            realDoc = session.getDocument(requestDoc.getRealRef());
            wcDoc = session.getSourceDocument(requestDoc.getRealRef());
        } else {
            wcDoc = session.getDocument(requestDoc.getWcRef());
        }

        NuxeoPrincipal user = (NuxeoPrincipal) session.getPrincipal();
        if (!wcDoc.isLocked() || (!user.isAdministrator()
                && !user.getName().equals(wcDoc.getLockInfo().getOwner()))) {
            throw new EloraException("Document with wcRef |"
                    + wcDoc.getRef().toString()
                    + "| must be locked by owner or administrator to be saved");
        }

        // Check if document exists
        // if (wcDoc == null) {
        // // TODO: Save barruan hau exception bat izango dela pentsatu da
        // throw new EloraException("Document with wcRef |"
        // + wcDoc.getRef().toString() + "| doesn not exist");
        // }

        // If realRef is passed, check that it is not a proxy
        if (realDoc != null && realDoc.isProxy()) {
            throw new EloraException("Document with realRef |"
                    + realDoc.getRef().toString()
                    + "| is a proxy, and it should be an archived version or a working copy.");
        }

        // Remove previous content file
        DocumentHelper.removeProperty(wcDoc,
                NuxeoMetadataConstants.NX_FILE_CONTENT);

        // Remove previous viewer file
        DocumentHelper.removeProperty(wcDoc,
                EloraMetadataConstants.ELORA_ELOVWR_FILE);
        DocumentHelper.removeProperty(wcDoc,
                EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
        DocumentHelper.removeProperty(wcDoc,
                EloraMetadataConstants.ELORA_ELOVWR_FILENAME);

        // Remove old CAD attachments
        DocumentHelper.removeProperty(wcDoc,
                EloraMetadataConstants.ELORA_CADATTS_FILES);

        wcDoc = session.saveDocument(wcDoc);

        EloraCheckinHelper.addDocumentFiles(wcDoc, requestDoc, session);

        // Update the document
        errorList = updateDoc(requestDoc, wcDoc, errorList, wcDoc.getId());
        wcDoc = saveDocWithoutValidation(wcDoc);
        log.trace(logInitMsg + "Document |" + wcDoc.getId() + "| updated.");

        // Create the response
        SaveResponseDoc responseDoc = createExistingDocResponse(requestDoc,
                wcDoc, realDoc, errorList);

        session.save();

        String proxyUid = null;
        if (requestDoc.getParentRealRef() != null) {
            // Create a proxy in the input path (if it doesn't exist)
            proxyUid = createDocumentProxy(requestDoc, wcDoc);
            responseDoc.setProxyUid(proxyUid);
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    /**
     * Updates document's properties, raises BEFORE_TCI_DOC_VALIDATION event,
     * and checks that reference + type is unique
     *
     * @param requestDoc
     * @param draftDoc
     * @param errorList
     * @return
     * @throws PropertyException
     * @throws IOException
     */
    protected List<ValidationErrorItem> updateDoc(SaveRequestDoc requestDoc,
            DocumentModel doc, List<ValidationErrorItem> errorList,
            String wcUid) throws PropertyException, IOException {

        DocumentHelper.setProperties(session, doc, requestDoc.getProperties());

        // Raise BEFORE_TCI_DOC_VALIDATION event
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        EloraCheckinHelper.notifyEvent(
                EloraEventTypes.BEFORE_SAVE_DOC_VALIDATION, doc, options, true,
                session);

        // Check that reference + type is unique, if not => KO
        String reference = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : doc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        errorList = EloraCheckinHelper.checkUniqueReferenceByType(wcUid,
                reference, requestDoc.getType(), errorList, session);

        return errorList;
    }

    /**
     * Creates the document (we turn off validation because we control it later
     * with the result)
     *
     * @param doc
     * @return
     */
    protected DocumentModel createDocWithoutValidation(DocumentModel doc) {
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
    protected DocumentModel saveDocWithoutValidation(DocumentModel doc) {
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
    protected SaveResponseDoc createNewDocResponse(SaveRequestDoc requestDoc,
            DocumentModel newDoc, List<ValidationErrorItem> errorList)
            throws EloraException {
        String wcUid = newDoc.getId();

        String fileContentHash = "";

        String currentVersionLabel = versionLabelService.getZeroVersion();
        String nextVersionLabel = versionLabelService.getZeroNextVersion();
        String latestVersionLabel = currentVersionLabel;

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(newDoc);
        String username = session.getPrincipal().getName();

        // Get the edition URL
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String editionUrl = EloraUrlHelper.getDocumentEditionUrl(request,
                newDoc);

        String title = newDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : newDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = newDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : newDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String reference = newDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : newDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        SaveResponseDoc responseDoc = new SaveResponseDoc(
                requestDoc.getLocalId(), wcUid, requestDoc.getType(), reference,
                title, description, requestDoc.getFilename(), fileContentHash,
                currentVersionLabel, newDoc.getCurrentLifeCycleState(),
                nextVersionLabel, latestVersionLabel, eloraLockInfo, username,
                new Date(), editionUrl);

        // Validate the document
        errorList.addAll(
                EloraCheckinHelper.checkForErrors(newDoc, validator, session));
        errorList.addAll(
                EloraCheckinHelper.validateCadDocument(newDoc, session));
        if (errorList.isEmpty()) {
            responseDoc.setResult(RESULT_OK);
        } else {
            responseDoc.setResult(RESULT_KO);
            responseDoc.addErrorList(errorList);
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
    protected SaveResponseDoc createExistingDocResponse(
            SaveRequestDoc requestDoc, DocumentModel wcDoc,
            DocumentModel realDoc, List<ValidationErrorItem> errorList)
            throws EloraException {

        String fileContentHash = "";
        Blob contentBlob = (Blob) wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        if (contentBlob != null) {
            fileContentHash = contentBlob.getDigest();
        }

        String currentVersionLabel = null;
        if (realDoc != null) {
            currentVersionLabel = realDoc.getVersionLabel();
        } else {
            currentVersionLabel = wcDoc.getVersionLabel();
        }

        String nextVersionLabel = EloraDocumentHelper.calculateNextVersionLabel(
                versionLabelService, wcDoc);
        // For EloraPlm, WC is always the "latest" version, even if it is
        // restored to an older version
        String latestVersionLabel = wcDoc.getVersionLabel();

        EloraLockInfo eloraLockInfo = EloraDocumentHelper.getLockInfo(wcDoc);
        String username = session.getPrincipal().getName();

        // Get the edition URL
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String editionUrl = EloraUrlHelper.getDocumentEditionUrl(request,
                wcDoc);

        String title = wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : wcDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();
        String description = wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : wcDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();
        String reference = wcDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : wcDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        SaveResponseDoc responseDoc = new SaveResponseDoc(
                requestDoc.getLocalId(), wcDoc.getId(), requestDoc.getType(),
                reference, title, description, requestDoc.getFilename(),
                fileContentHash, currentVersionLabel,
                wcDoc.getCurrentLifeCycleState(), nextVersionLabel,
                latestVersionLabel, eloraLockInfo, username, new Date(),
                editionUrl);

        // Validate the document
        errorList.addAll(
                EloraCheckinHelper.checkForErrors(wcDoc, validator, session));
        errorList.addAll(
                EloraCheckinHelper.validateCadDocument(wcDoc, session));
        if (errorList.isEmpty()) {
            responseDoc.setResult(RESULT_OK);
        } else {
            responseDoc.setResult(RESULT_KO);
            responseDoc.addErrorList(errorList);
        }

        return responseDoc;
    }

    protected String createDocumentProxy(SaveRequestDoc requestDoc,
            DocumentModel wcDoc) throws EloraException {

        String logInitMsg = "[createDocumentProxy] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel parentDoc = session.getDocument(
                requestDoc.getParentRealRef());
        DocumentModel proxyDoc = null;
        if (parentDoc == null) {
            throw new EloraException(
                    "parent document does not exist for localId: "
                            + requestDoc.getLocalId() + ")");
        }

        if (parentDoc.isProxy()) {
            throw new EloraException("parent document is a proxy for localId: |"
                    + requestDoc.getLocalId() + "|, parentRealRef |"
                    + requestDoc.getParentRealRef() + "|)");
        }

        // Check if it exists a proxy to the working copy. If it doesn't, create
        // it.
        // TODO: Devuelve todos los proxys al documento general. Mirar si se
        // pueden sacar los proxies que solo apuntan al wc
        DocumentModelList proxies = session.getProxies(wcDoc.getRef(),
                requestDoc.getParentRealRef());
        if (proxies.isEmpty()) {
            proxyDoc = session.createProxy(wcDoc.getRef(),
                    requestDoc.getParentRealRef());
        } else {
            for (DocumentModel proxy : proxies) {
                if (proxy.getSourceId() == wcDoc.getId()) {
                    return proxy.getId();
                }
            }
            proxyDoc = session.createProxy(wcDoc.getRef(),
                    requestDoc.getParentRealRef());
        }

        log.trace(logInitMsg + "Proxy |" + proxyDoc.getRef().toString()
                + "| created.");
        log.trace(logInitMsg + "--- EXIT --- ");

        return proxyDoc.getId();
    }

}
