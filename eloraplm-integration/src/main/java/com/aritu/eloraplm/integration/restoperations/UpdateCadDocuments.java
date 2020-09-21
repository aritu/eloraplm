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
package com.aritu.eloraplm.integration.restoperations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraFileInfo;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.restoperations.util.UpdateCadDocumentsRequestDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.google.common.collect.Lists;

/**
 * Update metadata or binaries of CAD documents; overwrite the archived version
 * If it is the base document, if WC is checked out, update its metadata too,
 * else restore WC to the base version
 *
 *
 * @author aritu
 *
 */
@Operation(id = UpdateCadDocuments.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Update CAD Documents", description = "Overwrite metadata and binaries of CAD documents.")
public class UpdateCadDocuments {

    public static final String ID = "Elora.PlmConnector.UpdateCadDocuments";

    private static final Log log = LogFactory.getLog(UpdateCadDocuments.class);

    private EloraGeneralResponse response;

    private List<UpdateCadDocumentsRequestDoc> requestDocs;

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

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new EloraGeneralResponse();
        requestDocs = new ArrayList<UpdateCadDocumentsRequestDoc>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (documents != null) {
                loadRequestDocs();
            }

            if (!requestDocs.isEmpty()) {
                for (UpdateCadDocumentsRequestDoc requestDoc : requestDocs) {
                    processSingleDocument(requestDoc);
                }

                log.info(logInitMsg + requestDocs.size()
                        + " documents updated.");

            } else {
                log.info(logInitMsg + "No documents to process.");
            }

            session.save();

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        // Create JSON response
        return response.convertToJson();
    }

    private void loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        log.trace(logInitMsg + documents.size() + " documents found.");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", true);

            UpdateCadDocumentsRequestDoc requestDoc = new UpdateCadDocumentsRequestDoc(
                    realRef);

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
                        // TODO Round decimal values
                        // EloraUnitConversionHelper
                        requestDoc.addProperty(property, value);
                    }
                }
            }

            // Get content file
            JsonNode mainFile = EloraJsonHelper.getJsonNode(docItem,
                    "contentFile", false);
            if (mainFile != null) {
                String fileName = EloraJsonHelper.getJsonFieldAsString(mainFile,
                        "filename", true);
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

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void processSingleDocument(UpdateCadDocumentsRequestDoc requestDoc)
            throws EloraException, COSVisitorException, IOException {
        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel realDoc = session.getDocument(requestDoc.getRealRef());
        if (realDoc == null) {
            throw new EloraException("Document not found for uid |"
                    + requestDoc.getRealRef().toString()
                    + "|, or is unreadable by the user.");
        }

        DocumentModel wcDoc = session.getWorkingCopy(realDoc.getRef());
        if (wcDoc == null) {
            throw new EloraException(
                    "Document has no Working Copy or is unreadable by the user.");
        }
        if (wcDoc.isLocked() && !wcDoc.getLockInfo().getOwner().equals(
                session.getPrincipal().getName())) {
            throw new EloraException(
                    "Document is locked by another user, so it cannot be updated.");
        }

        EloraDocumentHelper.disableVersioningDocument(realDoc);
        realDoc = updateDocument(realDoc, requestDoc);
        realDoc = session.saveDocument(realDoc);

        fireEvent(realDoc, requestDoc);

        // Check if it is the base of WC
        if (wcDoc != null) {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
            if (baseDoc != null && baseDoc.getId().equals(realDoc.getId())) {
                treatWorkingCopy(wcDoc, requestDoc);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void treatWorkingCopy(DocumentModel wcDoc,
            UpdateCadDocumentsRequestDoc requestDoc) throws EloraException {
        String logInitMsg = "[treatWorkingCopy] ["
                + session.getPrincipal().getName() + "] ";

        if (wcDoc.isCheckedOut()) {
            wcDoc = updateDocument(wcDoc, requestDoc);
            wcDoc = session.saveDocument(wcDoc);

            fireEvent(wcDoc, requestDoc);
            log.trace(logInitMsg + "Working copy |" + wcDoc.getId()
                    + "| was checked out. Metadata has been updated.");
            return;
        } else {
            EloraDocumentHelper.restoreToVersion(wcDoc.getRef(),
                    requestDoc.getRealRef(), true, true, session);
        }

    }

    private DocumentModel updateDocument(DocumentModel doc,
            UpdateCadDocumentsRequestDoc requestDoc) throws EloraException {
        doc = updateProperties(doc, requestDoc.getProperties());
        relateDocumentWithBinaries(doc, requestDoc);
        return doc;
    }

    private DocumentModel updateProperties(DocumentModel doc,
            Properties properties) {
        for (Entry<String, String> entry : properties.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }

        return doc;
    }

    private void relateDocumentWithBinaries(DocumentModel doc,
            UpdateCadDocumentsRequestDoc requestDoc) throws EloraException {

        String logInitMsg = "[relateDocumentWithBinaries] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        EloraFileInfo contentFile = requestDoc.getContentFile();
        if (contentFile != null) {
            if (contentFile.getBatch() != null
                    && contentFile.getFileName() != null) {

                EloraDocumentHelper.relateBatchWithDoc(doc,
                        contentFile.getFileId(), contentFile.getBatch(),
                        EloraGeneralConstants.FILE_TYPE_CONTENT,
                        contentFile.getFileName(), contentFile.getHash());
            }
        }

        EloraFileInfo viewerFile = requestDoc.getViewerFile();
        if (viewerFile != null) {
            if (viewerFile.getBatch() != null
                    && viewerFile.getFileName() != null) {

                EloraDocumentHelper.relateBatchWithDoc(doc,
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
                    EloraDocumentHelper.relateBatchWithDoc(doc,
                            cadAttachment.getFileId(), cadAttachment.getBatch(),
                            EloraGeneralConstants.FILE_TYPE_CAD_ATTACHMENT,
                            cadAttachment.getFileName(),
                            cadAttachment.getHash());
                }
            }
        }

        log.trace(logInitMsg + "Binaries related to document |"
                + doc.getRef().toString() + "| ");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * Calculate the comment and fire the Nuxeo event
     *
     * @param doc
     * @param requestDoc
     */
    private void fireEvent(DocumentModel doc,
            UpdateCadDocumentsRequestDoc requestDoc) {
        String comment = "#" + plmConnectorClient;
        List<String> propList = new ArrayList<String>();
        Properties properties = requestDoc.getProperties();
        if (properties != null && !properties.isEmpty()) {
            propList = Lists.newArrayList(properties.keySet());
        }
        if (requestDoc.getContentFile() != null) {
            propList.add(NuxeoMetadataConstants.NX_FILE_CONTENT);
        }
        if (requestDoc.getViewerFile() != null) {
            propList.add(EloraMetadataConstants.ELORA_ELOVWR_FILE);
        }
        if (requestDoc.getCadAttachments() != null) {
            propList.add(EloraMetadataConstants.ELORA_CADATTS_FILES);
        }
        comment += " " + String.join(", ", propList);

        EloraEventHelper.fireEvent(
                PdmEventNames.PDM_DOCUMENT_METADATA_UPDATED_EVENT, doc,
                comment);
    }
}
