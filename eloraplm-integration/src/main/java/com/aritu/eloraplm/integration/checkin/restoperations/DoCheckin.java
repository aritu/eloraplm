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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.codehaus.jackson.JsonNode;
//import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.checkin.api.CheckinManager;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraFileInfo;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.api.DraftManager;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinRequestDoc;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinRequestFolder;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinRequestRel;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinResponse;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinResponseDoc;
import com.aritu.eloraplm.integration.checkin.restoperations.util.DoCheckinResponseFolder;
import com.aritu.eloraplm.integration.util.EloraCheckinHelper;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.IntegrationEventTypes;
import com.aritu.eloraplm.viewer.ViewerPdfUpdater;

/**
 * @author aritu
 *
 */
@Operation(id = DoCheckin.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Do Checkin", description = "Check in documents from the Elora Plm Connector.")
public class DoCheckin {

    public static final String ID = "Elora.PlmConnector.DoCheckin";

    private static final Log log = LogFactory.getLog(DoCheckin.class);

    private CheckinManager checkinManager = Framework.getService(
            CheckinManager.class);

    private DoCheckinResponse doCheckinResponse;

    private List<DoCheckinRequestDoc> requestDocs;

    private List<DoCheckinRequestRel> requestRels;

    private List<DoCheckinRequestFolder> requestFolders;

    @Context
    private CoreSession session;

    @Context
    private EloraDocumentRelationManager eloraDocumentRelationManager;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = false)
    private ArrayList<JsonNode> documents;

    @Param(name = "folders", required = false)
    private ArrayList<JsonNode> folders;

    @Param(name = "relations", required = false)
    private ArrayList<JsonNode> relations;

    private DraftManager draftManager;

    private DocumentModel workspaceDoc;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        draftManager = Framework.getService(DraftManager.class);
        doCheckinResponse = new DoCheckinResponse();
        requestDocs = new ArrayList<DoCheckinRequestDoc>();
        requestRels = new ArrayList<DoCheckinRequestRel>();
        requestFolders = new ArrayList<DoCheckinRequestFolder>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            // Process folders
            if (folders != null) {
                loadRequestFolders();
            }

            if (!requestFolders.isEmpty()) {
                for (DoCheckinRequestFolder requestFolder : requestFolders) {
                    processSingleFolder(requestFolder);
                }
            }

            if (documents != null) {
                loadRequestDocs();
            }

            if (!requestDocs.isEmpty()) {
                for (DoCheckinRequestDoc requestDoc : requestDocs) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            if (!requestDocs.isEmpty()) {
                for (DoCheckinRequestDoc requestDoc : requestDocs) {
                    checkInSingleDocument(checkinManager, requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents checked in.");
            } else {
                log.info(logInitMsg + "No documents to check in.");
            }

            // Process relations
            if (relations != null) {
                loadRequestRels();
            }

            if (!requestRels.isEmpty()) {
                for (DoCheckinRequestRel requestRel : requestRels) {
                    processSingleRelation(requestRel);
                }
            } else {
                log.info(logInitMsg + "No relations to process.");
            }

            session.save();

            // TODO: Esto es temporal para hacer una presentacion
            // Process the BOMs
            /*
             * if (documents != null) { for (RequestDocument docItem :
             * docStruct) { processSingleBOM(docItem); } for (RequestDocument
             * docItem : docStruct) { processSingleBOMRelation(docItem); } }
             */
            doCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            doCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            doCheckinResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            doCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            doCheckinResponse.setErrorMessage(e.getMessage());
            doCheckinResponse.emptyDocuments();
            doCheckinResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            doCheckinResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            doCheckinResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            doCheckinResponse.emptyDocuments();
            doCheckinResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        // Create JSON response
        return doCheckinResponse.convertToJson();
    }

    private void loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + documents.size() + " documents found.");

        DocumentRef tempParentRealRef = null;

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            int dbId = EloraJsonHelper.getJsonFieldAsInt(docItem, "dbId", true);
            int localId = EloraJsonHelper.getJsonFieldAsInt(docItem, "localId",
                    true);
            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "wcUid", false);
            DocumentRef parentRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "parentRealUid", false);
            if (tempParentRealRef == null && parentRealRef != null) {
                tempParentRealRef = parentRealRef;
            }

            String comment = EloraJsonHelper.getJsonFieldAsString(docItem,
                    "comment", false);

            // TODO: Cual es el comportamiento por defecto de unlock si no se
            // pasa ningun valor
            boolean unlock = EloraJsonHelper.getJsonFieldAsBoolean(docItem,
                    "unlock", false);

            DocumentRef structureRootRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "structureRootRealUid", false);

            DoCheckinRequestDoc requestDoc = new DoCheckinRequestDoc(dbId,
                    localId, wcRef, parentRealRef, comment, unlock,
                    structureRootRealRef);

            // Get content file
            JsonNode mainFile = EloraJsonHelper.getJsonNode(docItem,
                    "contentFile", false);
            if (mainFile != null) {
                String fileName = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "filename", false);
                // TODO Hau mandatory jarri integradoria eguneratzen danien.
                // String fileName =
                // EloraJsonHelper.getJsonFieldAsString(mainFile,
                // "filename", true);
                int fileId = EloraJsonHelper.getJsonFieldAsInt(mainFile,
                        "fileId", true);
                String batch = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "batch", true);
                String hash = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "hash", true);
                requestDoc.setContentFile(fileId, fileName, batch, hash);
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
            requestDocs.add(requestDoc);
        }

        if (tempParentRealRef != null) {
            getCheckinWorkspaceDoc(tempParentRealRef);
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void getCheckinWorkspaceDoc(DocumentRef tempParentRealRef)
            throws EloraException {
        DocumentModel parentDoc = session.getDocument(tempParentRealRef);
        if (!parentDoc.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            workspaceDoc = EloraStructureHelper.getDocumentWorkspace(parentDoc,
                    session);
        } else {
            workspaceDoc = parentDoc;
        }
    }

    private void loadRequestRels() throws EloraException {
        String logInitMsg = "[loadRequestRels] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + relations.size() + " relations found.");

        for (int i = 0; i < relations.size(); ++i) {
            JsonNode relItem = relations.get(i);

            DocumentRef subjectWcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    relItem, "subjectWcUid", true);
            DocumentRef objectWcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    relItem, "objectWcUid", false);
            DocumentRef objectRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    relItem, "objectRealUid", false);

            if ((objectRealRef == null && objectWcRef == null)
                    || (objectRealRef != null && objectWcRef != null)) {
                throw new EloraException(
                        "objectUid and objectWcUid are both null or passed both together");
            }

            String predicate = EloraJsonHelper.getJsonFieldAsString(relItem,
                    "predicate", true);

            String quantity = EloraJsonHelper.getJsonFieldAsString(relItem,
                    "quantity", true);

            Integer ordering = EloraJsonHelper.getJsonFieldAsInt(relItem,
                    "ordering", false);

            DoCheckinRequestRel requestRel = new DoCheckinRequestRel(
                    objectRealRef, subjectWcRef, objectWcRef, predicate,
                    quantity, ordering);

            requestRels.add(requestRel);
        }
        log.trace(logInitMsg + requestRels.size() + " relations loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void loadRequestFolders() throws EloraException {
        String logInitMsg = "[loadRequestFolders] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + folders.size() + " folders found.");

        for (int i = 0; i < folders.size(); ++i) {
            JsonNode folderItem = folders.get(i);

            int dbId = EloraJsonHelper.getJsonFieldAsInt(folderItem, "dbId",
                    true);

            int localId = EloraJsonHelper.getJsonFieldAsInt(folderItem,
                    "localId", true);

            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "wcUid", true);

            DocumentRef parentRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    folderItem, "parentRealUid", true);

            if (parentRealRef != null) {
                if (!session.getDocument(parentRealRef).isFolder()) {
                    throw new EloraException(
                            "parentRealUid is not a folder in loadFolderStruct (localId: |"
                                    + localId + "|)");
                }
            }

            DoCheckinRequestFolder requestFolder = new DoCheckinRequestFolder(
                    dbId, localId, wcRef, parentRealRef);

            requestFolders.add(requestFolder);
        }
        log.trace(logInitMsg + requestFolders.size() + " folders loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void processSingleDocument(DoCheckinRequestDoc requestDoc)
            throws EloraException, COSVisitorException, IOException {
        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel wcDoc;
        DoCheckinResponseDoc responseDoc;
        wcDoc = session.getDocument(requestDoc.getWcRef());

        draftManager.copyDraftDataAndRemoveIt(session, wcDoc);

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        wcDoc.putContextData("workspaceType", workspaceDoc.getType());
        EloraCheckinHelper.notifyEvent(
                IntegrationEventTypes.AFTER_DRAFT_PROPERTIES_COPIED, wcDoc,
                options, true, session);

        // Check if it is a new document
        if (session.getVersionsRefs(wcDoc.getRef()).isEmpty()) {
            responseDoc = processNewDocument(requestDoc, wcDoc);
        } else {
            responseDoc = processExistingDocument(requestDoc, wcDoc);
        }
        doCheckinResponse.addDocument(requestDoc.getLocalId(), responseDoc);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private DoCheckinResponseDoc processNewDocument(
            DoCheckinRequestDoc requestDoc, DocumentModel wcDoc)
            throws EloraException, COSVisitorException, IOException {

        String logInitMsg = "[processNewDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // TODO: Mira si dependiendo de alguna configuracion el estado no tiene
        // que ser "create"
        // Change document state to project
        if (wcDoc.getCurrentLifeCycleState().equals(
                EloraLifeCycleConstants.PRECREATED)) {
            if (session.getAllowedStateTransitions(wcDoc.getRef()).contains(
                    EloraLifeCycleConstants.TRANS_CREATE)) {
                session.followTransition(wcDoc.getRef(),
                        EloraLifeCycleConstants.TRANS_CREATE);
            }
        }

        relateDocumentWithBinaries(wcDoc, requestDoc);
        updateViewerBlobWithDocInfo(wcDoc);
        wcDoc = session.saveDocument(wcDoc);

        // Move document to the classification folder
        DocumentRef targetFolder = getDocumentTargetFolder(wcDoc,
                requestDoc.getStructureRootRealRef());
        // We recalculate the name to avoid timestamps
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String nameForPath = pss.generatePathSegment(wcDoc.getTitle());
        session.move(wcDoc.getRef(), targetFolder, nameForPath);

        String proxyUid = null;
        if (requestDoc.getParentRealRef() != null) {
            proxyUid = createDocumentProxy(requestDoc, wcDoc, true);
        }

        session.save();

        DoCheckinResponseDoc responseDoc = new DoCheckinResponseDoc(
                requestDoc.getDbId(), requestDoc.getLocalId(),
                requestDoc.getContentFile() == null ? ""
                        : requestDoc.getContentFile().getHash(),
                requestDoc.getParentRealRef() == null ? ""
                        : requestDoc.getParentRealRef().toString(),
                proxyUid, wcDoc.getId());

        log.trace(logInitMsg + "Document |" + requestDoc.getWcRef().toString()
                + "| processed");
        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    private DoCheckinResponseDoc processExistingDocument(
            DoCheckinRequestDoc requestDoc, DocumentModel wcDoc)
            throws EloraException, COSVisitorException, IOException {

        String logInitMsg = "[processExistingDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Remove old binaries
        DocumentHelper.removeProperty(wcDoc,
                NuxeoMetadataConstants.NX_FILE_CONTENT);

        // Remove old viewer binary
        if (wcDoc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            DocumentHelper.removeProperty(wcDoc,
                    EloraMetadataConstants.ELORA_ELOVWR_FILE);
            DocumentHelper.removeProperty(wcDoc,
                    EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
            DocumentHelper.removeProperty(wcDoc,
                    EloraMetadataConstants.ELORA_ELOVWR_FILENAME);
        }

        // Remove old CAD attachments
        if (wcDoc.hasSchema(EloraSchemaConstants.CAD_ATTACHMENTS)) {
            DocumentHelper.removeProperty(wcDoc,
                    EloraMetadataConstants.ELORA_CADATTS_FILES);
        }

        // Remove all CAD relations where this wc is subject
        for (String predicate : RelationsConfig.cadRelationsList) {
            eloraDocumentRelationManager.softDeleteRelation(session, wcDoc,
                    predicate, null);
        }

        wcDoc = session.saveDocument(wcDoc);

        relateDocumentWithBinaries(wcDoc, requestDoc);
        updateViewerBlobWithDocInfo(wcDoc);
        wcDoc = session.saveDocument(wcDoc);

        String proxyUid = null;
        if (requestDoc.getParentRealRef() != null) {
            proxyUid = createDocumentProxy(requestDoc, wcDoc, false);
        }

        session.save();

        DoCheckinResponseDoc responseDoc = new DoCheckinResponseDoc(
                requestDoc.getDbId(), requestDoc.getLocalId(),
                requestDoc.getContentFile() == null ? null
                        : requestDoc.getContentFile().getHash(),
                requestDoc.getParentRealRef() == null ? ""
                        : requestDoc.getParentRealRef().toString(),
                proxyUid, wcDoc.getId());

        log.trace(logInitMsg + "Document |" + requestDoc.getWcRef().toString()
                + "| processed");
        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    private void updateViewerBlobWithDocInfo(DocumentModel wcDoc)
            throws COSVisitorException, IOException {
        ViewerPdfUpdater.createViewer(wcDoc,
                ViewerActionConstants.ACTION_CHECK_IN);
    }

    private String createDocumentProxy(DoCheckinRequestDoc requestDoc,
            DocumentModel wcDoc, boolean createIfNotExist)
            throws EloraException {

        String logInitMsg = "[createDocumentProxy] ["
                + session.getPrincipal().getName() + "] ";

        DocumentModel parentDoc = session.getDocument(
                requestDoc.getParentRealRef());

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

        DocumentModel proxyDoc = null;
        DocumentModelList proxies = session.getProxies(wcDoc.getRef(),
                requestDoc.getParentRealRef());

        if (proxies.isEmpty()) {

            if (createIfNotExist) {
                proxyDoc = session.createProxy(wcDoc.getRef(),
                        requestDoc.getParentRealRef());
            }

        } else {

            for (DocumentModel proxy : proxies) {
                if (proxy.getSourceId() == wcDoc.getId()) {
                    proxyDoc = proxy;
                    break;
                } else {
                    session.removeDocument(proxy.getRef());
                }
            }
            if (proxyDoc == null) {
                proxyDoc = session.createProxy(wcDoc.getRef(),
                        requestDoc.getParentRealRef());
            }

        }

        if (proxyDoc != null) {
            log.trace(logInitMsg + "Proxy |" + proxyDoc.getRef().toString()
                    + "| created.");

            return proxyDoc.getId();
        } else {
            return null;
        }

    }

    private void processSingleRelation(DoCheckinRequestRel requestRel)
            throws EloraException {

        String logInitMsg = "[processSingleRelation] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel objectWcDoc = null;
        DocumentModel objectDoc = null;
        DocumentModel subjectWcDoc = session.getDocument(
                requestRel.getSubjectWcRef());
        DocumentModel subjectDoc = null;

        // Check that subject is not in precreated state
        if (subjectWcDoc.getCurrentLifeCycleState().equals(
                EloraLifeCycleConstants.PRECREATED)) {
            throw new EloraException(
                    "subjectWcUid is null or is not checked in");
        } else {
            subjectDoc = EloraDocumentHelper.getLatestVersion(subjectWcDoc);
        }

        // Get the object from wc or realUid
        if (requestRel.getObjectRealRef() != null) {
            objectDoc = session.getDocument(requestRel.getObjectRealRef());
            objectWcDoc = session.getWorkingCopy(objectDoc.getRef());
        } else if (requestRel.getObjectWcRef() != null) {
            objectWcDoc = session.getDocument(requestRel.getObjectWcRef());
            // Don't create relations if object is precreated
            if (objectWcDoc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.PRECREATED)) {
                log.trace(logInitMsg + "objectWcDoc |" + objectWcDoc.getId()
                        + "| is precreated. Relation is not created.");
                return;
            } else {
                objectDoc = EloraDocumentHelper.getLatestVersion(objectWcDoc);
            }
        }

        checkinManager.checkThatRelationIsAllowed(subjectWcDoc,
                requestRel.getPredicate(), objectWcDoc,
                requestRel.getQuantity());
        checkinManager.checkThatRelationIsAllowed(subjectDoc,
                requestRel.getPredicate(), objectDoc, requestRel.getQuantity());

        try {
            // Add relation wcSubject-->wcObject except when object is a
            // version, in
            // this case add relation wcSubject-->objectRealUid

            eloraDocumentRelationManager.addRelation(session, subjectWcDoc,
                    objectWcDoc, requestRel.getPredicate(), null,
                    requestRel.getQuantity(), requestRel.getOrdering());

            log.trace(logInitMsg + "Relation between wc-s with subjectWcDoc: |"
                    + subjectWcDoc.getId() + "|, objectWcDoc: |"
                    + objectWcDoc.getId() + "| and predicate: |"
                    + requestRel.getPredicate() + "| processed");
        } catch (RelationAlreadyExistsException e) {
            log.trace(logInitMsg
                    + "Relation already exists between subjectWcDoc: |"
                    + subjectWcDoc.getId() + "| and objectWcDoc: |"
                    + objectWcDoc.getId() + "|. It is ignored");
        }
        // Add relation between versions
        eloraDocumentRelationManager.addRelation(session, subjectDoc, objectDoc,
                requestRel.getPredicate(), null, requestRel.getQuantity(),
                requestRel.getOrdering());

        log.trace(logInitMsg + "Relation between versions with subjectDoc: |"
                + subjectDoc.getId() + "|, objectDoc: |" + objectDoc.getId()
                + "| and predicate: |" + requestRel.getPredicate()
                + "| processed");

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void processSingleFolder(DoCheckinRequestFolder requestFolder)
            throws EloraException {

        String logInitMsg = "[processSingleFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel folder = session.getDocument(requestFolder.getWcRef());

        draftManager.copyDraftDataAndRemoveIt(session, folder);

        // Move folder to the workspace (if it isn't already there)
        DocumentModel targetFolder = session.getDocument(
                new IdRef(requestFolder.getParentRealRef().toString()));

        if (targetFolder != null
                && !folder.getParentRef().equals(targetFolder.getRef())) {
            // We recalculate the name to avoid timestamps
            PathSegmentService pss = Framework.getService(
                    PathSegmentService.class);
            String nameForPath = pss.generatePathSegment(folder.getTitle());
            session.move(folder.getRef(), targetFolder.getRef(), nameForPath);
        }

        session.saveDocument(folder);
        session.save();

        DoCheckinResponseFolder responseFolder = new DoCheckinResponseFolder(
                requestFolder.getDbId(), requestFolder.getLocalId(),
                requestFolder.getParentRealRef().toString(),
                requestFolder.getWcRef().toString());

        doCheckinResponse.addFolder(responseFolder);

        log.trace(logInitMsg + "Folder |" + requestFolder.getWcRef().toString()
                + "| processed");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void checkInSingleDocument(CheckinManager checkinManager,
            DoCheckinRequestDoc requestDoc) throws EloraException,
            CheckinNotAllowedException, DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        String logInitMsg = "[checkInSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel doc = session.getDocument(requestDoc.getWcRef());

        doc = checkinManager.checkinDocument(doc, requestDoc.getComment(),
                requestDoc.isUnlock());

        // Nuxeo Event
        String comment = doc.getVersionLabel();
        if (requestDoc.getComment() != null) {
            comment += " " + requestDoc.getComment();
        }
        EloraEventHelper.fireEvent(PdmEventNames.PDM_INT_CHECKED_IN_EVENT, doc,
                comment);

        DoCheckinResponseDoc responseDoc = doCheckinResponse.getDocument(
                requestDoc.getLocalId());
        responseDoc.setRealUid(
                EloraDocumentHelper.getLatestVersion(doc).getId());
        responseDoc.setVersionLabel(doc.getVersionLabel());
        doCheckinResponse.addDocument(requestDoc.getLocalId(), responseDoc);

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private DocumentRef getDocumentTargetFolder(DocumentModel doc,
            DocumentRef structureRootRealRef) throws EloraException {

        if (structureRootRealRef == null) {
            throw new EloraException(
                    "New document without structureRootRealRef. wcUid: |"
                            + doc.getId() + "|");
        }

        String type = doc.getType();
        if (type.equals("Folder")) {
            return null;
        } else {
            return EloraStructureHelper.getCadDocModelByType(
                    structureRootRealRef, type, session).getRef();
        }
    }

    private void relateDocumentWithBinaries(DocumentModel wcDoc,
            DoCheckinRequestDoc requestDoc) throws EloraException {

        String logInitMsg = "[relateDocumentWithBinaries] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        EloraFileInfo contentFile = requestDoc.getContentFile();
        if (contentFile != null) {
            if (contentFile.getBatch() != null) {

                // TODO Filename mandatory jarri integradoria eguneratzen
                // danien.
                // if (contentFile.getBatch() != null
                // && contentFile.getFileName() != null) {

                EloraDocumentHelper.relateBatchWithDoc(wcDoc,
                        contentFile.getFileId(), contentFile.getBatch(),
                        EloraGeneralConstants.FILE_TYPE_CONTENT,
                        contentFile.getFileName(), contentFile.getHash());
            }
        }

        EloraFileInfo viewerFile = requestDoc.getViewerFile();
        if (viewerFile != null) {
            if (viewerFile.getBatch() != null
                    && viewerFile.getFileName() != null) {

                EloraDocumentHelper.relateBatchWithDoc(wcDoc,
                        viewerFile.getFileId(), viewerFile.getBatch(),
                        EloraGeneralConstants.FILE_TYPE_VIEWER,
                        viewerFile.getFileName(), viewerFile.getHash());
            }
        }

        List<EloraFileInfo> cadAttachments = requestDoc.getCadAttachments();
        if (cadAttachments != null) {
            for (EloraFileInfo cadAttachment : cadAttachments) {
                if (cadAttachment.getBatch() != null
                        && cadAttachment.getFileName() != null) {
                    EloraDocumentHelper.relateBatchWithDoc(wcDoc,
                            cadAttachment.getFileId(), cadAttachment.getBatch(),
                            EloraGeneralConstants.FILE_TYPE_CAD_ATTACHMENT,
                            cadAttachment.getFileName(),
                            cadAttachment.getHash());
                }
            }
        }

        log.trace(logInitMsg + "Binaries related to document |"
                + wcDoc.getRef().toString() + "| ");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

}
