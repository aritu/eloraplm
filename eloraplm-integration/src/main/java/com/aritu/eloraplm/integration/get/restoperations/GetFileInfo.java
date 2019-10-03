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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.FileInfoBuilder;
import com.aritu.eloraplm.integration.get.restoperations.util.GetFileInfoResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.UidRequestDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author aritu
 *
 */
@Operation(id = GetFileInfo.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get File Info", description = "Get information of the document.")
public class GetFileInfo {
    public static final String ID = "Elora.PlmConnector.GetFileInfo";

    private static final Log log = LogFactory.getLog(GetFileInfo.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = true)
    protected ArrayList<JsonNode> documents;

    @Param(name = "getItemsInfo", required = true)
    protected boolean getItemsInfo;

    protected Map<String, UidRequestDoc> requestDocs;

    protected GetFileInfoResponse fileInfoResponse;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        fileInfoResponse = new GetFileInfoResponse();
        requestDocs = new HashMap<String, UidRequestDoc>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            if (documents != null) {
                loadRequestDocs();
            }

            FileInfoBuilder fib = new FileInfoBuilder(session, ctx);
            fileInfoResponse = fib.processDocuments(requestDocs, getItemsInfo);

            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            fileInfoResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            fileInfoResponse.setErrorMessage(e.getMessage());
            fileInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            fileInfoResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            fileInfoResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            fileInfoResponse.emptyDocuments();

            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = fileInfoResponse.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    protected void loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);
            DocumentRef wcRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "wcUid", false);
            DocumentRef realRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "realUid", false);
            DocumentRef parentRealRef = EloraJsonHelper.getJsonFieldAsDocumentRef(
                    docItem, "parentRealUid", false);

            UidRequestDoc requestDoc = new UidRequestDoc(wcRef, realRef,
                    parentRealRef);
            if (wcRef != null || realRef != null) {
                // We want to avoid duplicated documents (but an AV of a
                // document and its WC are different documents)
                String indexUid = realRef == null ? wcRef.toString()
                        : realRef.toString();
                if (!requestDocs.containsKey(indexUid)) {
                    requestDocs.put(indexUid, requestDoc);
                }
            } else {
                throw new EloraException("Null wcUid and realUid");
            }
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");
        log.trace(logInitMsg + "--- EXIT --- ");
    }

}
