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

package com.aritu.eloraplm.integration.cm.restoperations;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.integration.cm.CmProcessInfo;
import com.aritu.eloraplm.integration.cm.restoperations.util.CmProcessStructureResponse;
import com.aritu.eloraplm.integration.cm.util.IntegrationCmHelper;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = GetCmProcessStructure.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Get CM Process Structure", description = "Get the structure of a CM process.")
public class GetCmProcessStructure {

    public static final String ID = "Elora.PlmConnector.GetCmProcessStructure";

    private static final Log log = LogFactory.getLog(
            GetCmProcessStructure.class);

    @Context
    protected CoreSession session;

    @Param(name = "cmProcessUid", required = true)
    protected DocumentRef cmProcessRef;

    protected CmProcessStructureResponse response;

    protected DocumentModel cmProcess;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new CmProcessStructureResponse();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            loadCmProcess();
            CmProcessInfo structInfo = IntegrationCmHelper.getCmEcoProcessInfo(
                    cmProcess);
            response.setCmProcessStructure(structInfo.getStructure());

            response.setLastModified(getLastModified());

            log.info(logInitMsg + "Got structure of provided CM process.");
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

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

        // Create JSON response
        String jsonResponse = response.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }

    private Date getLastModified() {
        Date lastModified = null;
        GregorianCalendar lastModifiedGc = (GregorianCalendar) cmProcess.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        if (lastModifiedGc != null) {
            lastModifiedGc.set(Calendar.MILLISECOND, 0);
            lastModified = lastModifiedGc.getTime();
        }

        return lastModified;
    }

    private void loadCmProcess() throws EloraException {
        if (!session.exists(cmProcessRef)) {
            throw new EloraException(
                    "There is no CM process document with the provided UID.");
        }
        cmProcess = session.getDocument(cmProcessRef);

        if (!cmProcess.hasFacet(EloraFacetConstants.FACET_CM_PROCESS)) {
            throw new EloraException("Provided document is not a CM process.");
        }
    }

}
