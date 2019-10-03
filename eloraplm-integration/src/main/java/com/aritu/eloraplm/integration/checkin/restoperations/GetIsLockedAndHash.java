/**
 *
 */

package com.aritu.eloraplm.integration.checkin.restoperations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.checkin.restoperations.util.GetIsLockedAndHashResponse;
import com.aritu.eloraplm.integration.checkin.restoperations.util.GetIsLockedAndHashResponseDoc;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author jalzola
 */
@Operation(id = GetIsLockedAndHash.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get locked and hash", description = "")
public class GetIsLockedAndHash {

    public static final String ID = "Elora.PlmConnector.GetIsLockedAndHash";

    private static final Log log = LogFactory.getLog(DoCheckin.class);

    @Context
    protected CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "documents", required = false)
    protected ArrayList<JsonNode> documents;

    // public ArrayList<DocumentRef> realRefList;

    @OperationMethod
    public String run() throws EloraException {
        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        GetIsLockedAndHashResponse getIsLockedAndHashResponse = new GetIsLockedAndHashResponse();

        try {

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            List<DocumentRef> requestDocs = documents != null
                    ? loadRequestDocs() : new ArrayList<DocumentRef>();

            for (DocumentRef realRef : requestDocs) {

                DocumentModel doc = session.getDocument(realRef);
                if (doc.isProxy() || !doc.isVersion()) {
                    throw new EloraException("Proxy or Working Copy found");
                }

                // Get working copy because this object is the one with blocking
                // info
                DocumentModel wcDoc = session.getSourceDocument(doc.getRef());
                EloraLockInfo lockInfo = EloraDocumentHelper.getLockInfo(wcDoc);

                Blob contentBlob = (Blob) doc.getPropertyValue("file:content");
                String hash = null;
                if (contentBlob != null) {
                    hash = contentBlob.getDigest();
                }

                GetIsLockedAndHashResponseDoc responseDoc = new GetIsLockedAndHashResponseDoc();
                responseDoc.setEloraLockInfo(lockInfo);
                responseDoc.setHash(hash);
                responseDoc.setRealUid(doc.getId());

                getIsLockedAndHashResponse.AddDocument(responseDoc);
            }
            getIsLockedAndHashResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getIsLockedAndHashResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getIsLockedAndHashResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            getIsLockedAndHashResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            getIsLockedAndHashResponse.setErrorMessage(e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            getIsLockedAndHashResponse.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            getIsLockedAndHashResponse.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        return getIsLockedAndHashResponse.convertToJson();
    }

    /**
     * Loads the documents to check whether it is lock and get its hash
     *
     * @throws EloraException
     */
    protected List<DocumentRef> loadRequestDocs() throws EloraException {
        String logInitMsg = "[loadRequestDocs] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + documents.size() + " documents found.");

        List<DocumentRef> requestDocs = new ArrayList<DocumentRef>();

        for (int i = 0; i < documents.size(); ++i) {
            JsonNode docItem = documents.get(i);
            String wcUid = EloraJsonHelper.getJsonFieldAsString(docItem,
                    "realUid", true);
            requestDocs.add(new IdRef(wcUid));
        }

        log.trace(logInitMsg + requestDocs.size() + " documents loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");

        return requestDocs;
    }
}
