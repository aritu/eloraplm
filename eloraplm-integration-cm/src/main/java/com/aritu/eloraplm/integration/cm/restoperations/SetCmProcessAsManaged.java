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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
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
import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = SetCmProcessAsManaged.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Set CM Process As Managed", description = "Set the entries of the CM process as managed.")
public class SetCmProcessAsManaged {

    public static final String ID = "Elora.PlmConnector.SetCmProcessAsManaged";

    private static final Log log = LogFactory.getLog(
            SetCmProcessAsManaged.class);

    @Context
    protected CoreSession session;

    @Param(name = "cmProcessUid", required = true)
    protected DocumentRef cmProcessRef;

    @Param(name = "destinationUidList", required = true)
    protected ArrayList<JsonNode> destinationUidList;

    protected Map<String, String> managedDestinationUids;

    protected CmProcessStructureResponse response;

    protected DocumentModel cmProcess;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new CmProcessStructureResponse();
        managedDestinationUids = new HashMap<String, String>();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            loadCmProcess();
            loadManagedDestinationUids();

            IntegrationCmHelper.setCmEcoItemsAsManaged(cmProcess,
                    managedDestinationUids);

            CmProcessInfo structInfo = IntegrationCmHelper.getCmEcoProcessInfo(
                    cmProcess, false);
            response.setCmProcessStructure(structInfo.getStructure());

            response.setLastModified(getLastModified());

            log.info(logInitMsg
                    + "Provided CM process entries have been marked as managed.");
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

    /**
     * Loads the destination list
     *
     * @throws EloraException
     */
    private void loadManagedDestinationUids() throws EloraException {
        String logInitMsg = "[loadManagedDestinationUids] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        log.trace(logInitMsg + destinationUidList.size()
                + " destinationUids found.");

        for (int i = 0; i < destinationUidList.size(); ++i) {
            JsonNode destinationUid = destinationUidList.get(i);
            String destinationWcUidBeforeCheckin = EloraJsonHelper.getJsonFieldAsString(
                    destinationUid, "destinationWcUidBeforeCheckin", true);
            String destinationRealUidAfterCheckin = EloraJsonHelper.getJsonFieldAsString(
                    destinationUid, "destinationRealUidAfterCheckin", true);

            managedDestinationUids.put(destinationWcUidBeforeCheckin,
                    destinationRealUidAfterCheckin);
        }

        log.trace(logInitMsg + managedDestinationUids.size()
                + " destinationUids loaded.");

        log.trace(logInitMsg + "--- EXIT --- ");
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
