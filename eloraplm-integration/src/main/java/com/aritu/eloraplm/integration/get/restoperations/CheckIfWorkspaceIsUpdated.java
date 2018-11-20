///*
// * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// */
//
package com.aritu.eloraplm.integration.get.restoperations;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.CheckIfWorkspaceIsUpdatedResponse;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

@Operation(id = CheckIfWorkspaceIsUpdated.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Check If Workspace Is Updated", description = "Check if the provided workspace is up to date locally.")
public class CheckIfWorkspaceIsUpdated {
    public static final String ID = "Elora.PlmConnector.CheckIfWorkspaceIsUpdated";

    private static final Log log = LogFactory.getLog(
            CheckIfWorkspaceIsUpdated.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "plmConnectorClient", required = true)
    private String plmConnectorClient;

    @Param(name = "plmConnectorVersion", required = true)
    private Integer plmConnectorVersion;

    @Param(name = "wcUid", required = true)
    protected DocumentRef wcRef;

    @Param(name = "lastModified", required = true)
    private String lastModifiedString;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        CheckIfWorkspaceIsUpdatedResponse response = new CheckIfWorkspaceIsUpdatedResponse();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            EloraIntegrationHelper.checkThatConnectorIsUpToDate(
                    plmConnectorClient, plmConnectorVersion);

            DocumentModel wsDoc = session.getDocument(wcRef);
            Date currentLastModified = getLastModified(wsDoc);
            boolean isUpdated = false;

            Date lastModified = new Date(Long.parseLong(lastModifiedString));
            if (currentLastModified.equals(lastModified)) {
                isUpdated = true;
                log.info("Workspace data is up to date.");
            } else {
                log.info("Workspace data is outdated.");
            }

            response.setIsUpdated(isUpdated);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (ConnectorIsObsoleteException e) {
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

        // Create JSON response
        String jsonResponse = response.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    private Date getLastModified(DocumentModel wsDoc) {
        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) wsDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            lastModifiedGc.set(Calendar.MILLISECOND, 0);
            lastModified = lastModifiedGc.getTime();
        }

        return lastModified;
    }
}
