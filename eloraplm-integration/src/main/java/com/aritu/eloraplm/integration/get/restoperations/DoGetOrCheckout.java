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

package com.aritu.eloraplm.integration.get.restoperations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.MetadataConfig;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.DoGetOrCheckoutRequestDoc;
import com.aritu.eloraplm.integration.get.restoperations.util.DoGetOrCheckoutResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.DoGetOrCheckoutResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;
import com.aritu.eloraplm.integration.util.FolderInfo;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
@Operation(id = DoGetOrCheckout.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Do Get Or Checkout", description = "Gets or checks out the selected documents and returns necessary information for the client.")
public class DoGetOrCheckout {
    public static final String ID = "Elora.PlmConnector.DoGetOrCheckout";

    private static final Log log = LogFactory.getLog(DoGetOrCheckout.class);

    private static final String ACTION_CHECKOUT = "Checkout";

    private static final String ACTION_GET = "Get";

    private static final String[] ACTIONS = { ACTION_CHECKOUT, ACTION_GET };

    private static final String RESULT_OK = "OK";

    private static final String RESULT_KO = "KO";

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = true)
    private ArrayList<JsonNode> documents;

    @Param(name = "action", required = true)
    private String action;

    @Param(name = "workspaceRealUid", required = false)
    private String workspaceRealUid;

    @Param(name = "getParentFolders", required = true)
    private boolean getParentFolders;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private Map<String, DoGetOrCheckoutRequestDoc> requestDocs;

    private DoGetOrCheckoutResponse doGetOrCheckoutResponse;

    private List<String> processedFolders;

    private String rootItemParentUid;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        doGetOrCheckoutResponse = new DoGetOrCheckoutResponse();
        requestDocs = new HashMap<>();
        processedFolders = new ArrayList<>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (!Arrays.asList(ACTIONS).contains(action)) {
                throw new EloraException("The action must be "
                        + String.join(" or ", Arrays.asList(ACTIONS)) + ".");
            }

            if (documents != null) {
                loadRequestDocs();
            }

            // Process the documents
            if (!requestDocs.isEmpty()) {
                for (DoGetOrCheckoutRequestDoc requestDoc : requestDocs.values()) {
                    processSingleDocument(requestDoc);
                }
                log.info(logInitMsg + requestDocs.size()
                        + " documents processed.");
            } else {
                log.info(logInitMsg + "No document to process.");
            }

            log.info(logInitMsg
                    + "Document and related children successfuly checked out.");
            doGetOrCheckoutResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            doGetOrCheckoutResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            doGetOrCheckoutResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            doGetOrCheckoutResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            doGetOrCheckoutResponse.setErrorMessage(e.getMessage());
            doGetOrCheckoutResponse.emptyDocuments();
            doGetOrCheckoutResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            doGetOrCheckoutResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            doGetOrCheckoutResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            doGetOrCheckoutResponse.emptyDocuments();
            doGetOrCheckoutResponse.emptyFolders();
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = doGetOrCheckoutResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    /**
     * @throws EloraException
     */
    protected void loadRequestDocs() throws EloraException {

        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + documents.size() + " documents found.");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);

            DocumentRef proxyRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "proxyUid", false);
            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", true);

            boolean selected = EloraJsonHelper.getJsonFieldAsBoolean(docItem,
                    "selected", true);

            boolean lock = EloraJsonHelper.getJsonFieldAsBoolean(docItem,
                    "lock", true);

            boolean isRootElement = EloraJsonHelper.getJsonFieldAsBoolean(
                    docItem, "isRootElement", true);

            DoGetOrCheckoutRequestDoc requestDoc = new DoGetOrCheckoutRequestDoc(
                    proxyRef, realRef, selected, lock, isRootElement);
            requestDocs.put(realRef.toString(), requestDoc);

            if (isRootElement) {
                if (proxyRef != null) {
                    DocumentRef parentRef = session.getParentDocumentRef(
                            proxyRef);
                    rootItemParentUid = parentRef.toString();
                } else {
                    // No workspaceRealUid = has to go to the local store =
                    // rootItemParentUid is null
                    if (workspaceRealUid != null) {
                        rootItemParentUid = workspaceRealUid;
                    }
                }
            }

        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * @param requestDoc
     * @throws EloraException
     */
    protected void processSingleDocument(DoGetOrCheckoutRequestDoc requestDoc)
            throws EloraException {

        String logInitMsg = "[processSingleDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Processing document |"
                + requestDoc.getRealRef().toString() + "| ...");

        DocumentModel realDoc = session.getDocument(requestDoc.getRealRef());
        boolean isDocWc = checkDocumentData(requestDoc, realDoc);

        // If doc is not a WC, get the WC
        DocumentRef wcDocRef;
        if (isDocWc) {
            wcDocRef = requestDoc.getRealRef();
        } else {
            wcDocRef = session.getSourceDocument(realDoc.getRef()).getRef();
        }

        // If document is selected, get proxies and send as selected; if it is
        // locked, also lock the document
        DocumentModel wcDoc = session.getDocument(wcDocRef);
        boolean docWasCheckedOut = wcDoc.isCheckedOut();
        String result = RESULT_OK;
        List<ValidationErrorItem> errorList = new ArrayList<>();

        String parentRealUid = null;
        String proxyUid = null;
        String realUid = realDoc.getId();

        if (requestDoc.getSelected()) {

            if (requestDoc.getLock()) {

                // Lock document
                try {
                    wcDoc = EloraDocumentHelper.lockDocument(wcDoc);

                    log.trace(logInitMsg + "Document |" + realDoc.getId()
                            + "| has been locked to check out.");
                } catch (DocumentAlreadyLockedException e) {
                    result = RESULT_KO;
                    errorList.add(new ValidationErrorItem("-",
                            "Document is locked by user '" + e.getOwner()
                                    + "'"));
                    log.trace(logInitMsg + "Document |" + realDoc.getId()
                            + "| is locked by user |" + e.getOwner() + "|.");
                } catch (DocumentInUnlockableStateException e) {
                    result = RESULT_KO;
                    errorList.add(new ValidationErrorItem("-",
                            "Document is in an unlockable state: "
                                    + e.getCurrentLifeCycleState()));
                    log.trace(logInitMsg + "Document |" + realDoc.getId()
                            + "| is in an unlockable state: |"
                            + e.getCurrentLifeCycleState() + "|.");
                } catch (DocumentLockRightsException e) {
                    result = RESULT_KO;
                    errorList.add(new ValidationErrorItem("-",
                            "User has no rights to lock the document"));
                    log.trace(
                            logInitMsg + "User has no rights to lock document |"
                                    + realDoc.getId() + "|");
                } catch (EloraException e) {
                    result = RESULT_KO;
                    errorList.add(new ValidationErrorItem("-", e.getMessage()));
                    log.trace(logInitMsg + e.getMessage());
                }
            }

            // Get parents from proxies
            if (getParentFolders) {
                DocumentRef proxyRef = requestDoc.getProxyRef();
                if (proxyRef == null) {
                    DocumentModel foundProxy = findProxyForDocument(wcDoc);
                    if (foundProxy != null) {
                        parentRealUid = foundProxy.getParentRef().toString();
                        proxyUid = foundProxy.getId();
                    } else {
                        parentRealUid = rootItemParentUid;
                    }
                } else {
                    proxyUid = proxyRef.toString();
                    parentRealUid = session.getParentDocumentRef(
                            proxyRef).toString();
                }

                // Get the parent folders
                log.trace(logInitMsg + "Document |" + realUid
                        + "| is in the workspace. Processing folders...");

                if (parentRealUid != null
                        && !parentRealUid.equals(workspaceRealUid)) {
                    processSingleFolder(parentRealUid);
                }

                log.trace(logInitMsg + "Document |" + realUid
                        + "| folders processed.");
            }

        }

        String versionLabel;
        String lifeCycleState;
        if (isDocWc) {
            DocumentModel lastVersion = EloraDocumentHelper.getLatestVersion(
                    realDoc);

            if (lastVersion == null) {
                throw new EloraException(
                        "Cannot get the last version of the document.");
            }
            realUid = lastVersion.getId();
            versionLabel = lastVersion.getVersionLabel();
            lifeCycleState = lastVersion.getCurrentLifeCycleState();
        } else {
            versionLabel = realDoc.getVersionLabel();
            lifeCycleState = realDoc.getCurrentLifeCycleState();
        }

        String reference = realDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE) == null ? ""
                        : realDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();

        String title = realDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE) == null ? ""
                        : realDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_TITLE).toString();

        String description = realDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_DESCRIPTION) == null ? ""
                        : realDoc.getPropertyValue(
                                NuxeoMetadataConstants.NX_DC_DESCRIPTION).toString();

        DoGetOrCheckoutResponseDoc responseDoc = new DoGetOrCheckoutResponseDoc(
                proxyUid, realUid, wcDoc.getId(), parentRealUid,
                realDoc.getType(), reference, title, description, versionLabel,
                lifeCycleState, result, errorList);

        // Get override metadata and document properties, if the document is
        // last version or WC
        // If the document is not checked out, we are sure that the binary has
        // the correct metadata, if not, we have to ensure
        // that all the override metadata is processed to update the binary
        String realVersionLabelWithPlus = realDoc.getVersionLabel() + "+";
        if (docWasCheckedOut && (isDocWc
                || realVersionLabelWithPlus.equals(wcDoc.getVersionLabel()))) {
            responseDoc = processMetadata(responseDoc, realDoc);
        } else {
            log.trace(logInitMsg + "Document |" + realUid
                    + "| was not checked out or is not the last version. No metadata to override.");
        }

        // Get content file hash and name
        Blob contentBlob = (Blob) realDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        if (contentBlob != null) {
            HttpServletRequest request = (HttpServletRequest) ctx.get(
                    "request");
            String filename = contentBlob.getFilename();
            String downloadUrl = EloraUrlHelper.getDocumentDownloadUrl(request,
                    realDoc, filename);

            responseDoc.setFilename(filename);
            responseDoc.setHash(contentBlob.getDigest());
            responseDoc.setDownloadUrl(downloadUrl);
        }

        // Add to responseDocs
        doGetOrCheckoutResponse.addDocument(responseDoc);

        log.trace(logInitMsg + "Document |" + realUid + "| processed.");
    }

    private DocumentModel findProxyForDocument(DocumentModel wcDoc) {
        if (workspaceRealUid != null) {
            String query = EloraQueryFactory.getDocProxiesQuery(wcDoc.getType(),
                    wcDoc.getId(), workspaceRealUid);
            DocumentModelList proxies = session.query(query);
            if (!proxies.isEmpty()) {
                // We get the first, even if there are more
                return proxies.get(0);
            }
        }

        return null;
    }

    protected void processSingleFolder(String realUid) {

        String logInitMsg = "[processSingleFolder] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Processing folder |" + realUid + "| ...");

        // If it is already processed, return; else, add to list
        if (processedFolders.contains(realUid)) {
            return;
        } else {
            processedFolders.add(realUid);
        }

        DocumentModel doc = session.getDocument(new IdRef(realUid));
        String parentRealUid = doc.getParentRef().toString();
        String title = doc.getTitle();
        String path = doc.getPathAsString();

        FolderInfo responseFolder = new FolderInfo(realUid, parentRealUid,
                title, path);
        doGetOrCheckoutResponse.addFolder(responseFolder);

        if (!parentRealUid.equals(workspaceRealUid)) {
            processSingleFolder(parentRealUid);
        }

        log.trace(logInitMsg + "Folder |" + realUid + "| processed.");

    }

    // TODO Hau atara leike TryCheckin-ek be berdina erabiltzeko????
    // Interfazeren batekin izan biharko zan???
    /**
     * Process PlmToCad and Bidirectional metadata to override in the binary
     * file
     *
     * @param responseDoc
     * @param realDoc
     * @return
     * @throws EloraException
     */
    protected DoGetOrCheckoutResponseDoc processMetadata(
            DoGetOrCheckoutResponseDoc responseDoc, DocumentModel realDoc)
            throws EloraException {

        String logInitMsg = "[processMetadata] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        String authoringTool = realDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_CAD_AUTHORING_TOOL).toString();

        String type = realDoc.getType();

        processRealMetadata(responseDoc, realDoc, authoringTool, type);

        processVirtualMetadata(responseDoc, realDoc, authoringTool, type);

        log.trace(logInitMsg + "--- EXIT --- ");

        return responseDoc;
    }

    private void processRealMetadata(DoGetOrCheckoutResponseDoc responseDoc,
            DocumentModel draftDoc, String authoringTool, String type) {
        if (MetadataConfig.getRealOverrideMetadataMapByType().containsKey(
                authoringTool)
                && MetadataConfig.getRealOverrideMetadataMapByType().get(
                        authoringTool).containsKey(type)) {
            for (String property : MetadataConfig.getRealOverrideMetadataMapByType().get(
                    authoringTool).get(type)) {
                // Data for real metadata is in draftDoc
                Serializable value = draftDoc.getPropertyValue(property);
                responseDoc.addOverrideMetadata(property, value);
            }
        }
    }

    private void processVirtualMetadata(DoGetOrCheckoutResponseDoc responseDoc,
            DocumentModel wcDoc, String authoringTool, String type)
            throws EloraException {
        if (MetadataConfig.getVirtualOverrideMetadataMapByType().containsKey(
                authoringTool)
                && MetadataConfig.getVirtualOverrideMetadataMapByType().get(
                        authoringTool).containsKey(type)) {
            for (String property : MetadataConfig.getVirtualOverrideMetadataMapByType().get(
                    authoringTool).get(type)) {
                // If it is a virtual metadata, call to getDataFromMethod
                Serializable value = EloraIntegrationHelper.getVirtualMetadata(
                        session, wcDoc, property, "DoGetOrCheckout");
                responseDoc.addOverrideMetadata(property, value);
            }
        }
    }

    protected boolean checkDocumentData(DoGetOrCheckoutRequestDoc requestDoc,
            DocumentModel doc) throws EloraException {
        boolean isDocWc = false;

        if (doc.isProxy()) {
            throw new EloraException("Document is a proxy.");
        }
        if (!doc.isVersion()) {
            if (action.equals("Checkout")) {
                throw new EloraException("Document is a working copy.");
            }
            isDocWc = true;
        }

        return isDocWc;
    }
}
