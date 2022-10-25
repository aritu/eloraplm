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

package com.aritu.eloraplm.integration.admin.restoperations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.sql.management.SQLRepositoryStatus;
import org.nuxeo.ecm.core.storage.sql.management.SQLRepositoryStatusMBean;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.admin.restoperations.util.CleanOrphanBinariesResponse;

/**
 * @author aritu
 *
 */
@Operation(id = CleanOrphanBinaries.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_ADMIN, label = "Admin - Clean Orphan Binaries", description = "Clean binaries that are not used by any document.")
public class CleanOrphanBinaries {
    public static final String ID = "Elora.Admin.CleanOrphanBinaries";

    private static final Log log = LogFactory.getLog(CleanOrphanBinaries.class);

    @Param(name = "clean", required = true)
    private boolean clean;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private CleanOrphanBinariesResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new CleanOrphanBinariesResponse();

        try {

            if (!session.getPrincipal().getName().equals("Administrator")) {
                throw new EloraException(
                        "Admin operations can only be executed by administrators.");
            }

            log.debug(logInitMsg + "Starting orphan binaries cleanup");
            SQLRepositoryStatusMBean status = new SQLRepositoryStatus();
            if (!status.isBinariesGCInProgress()) {
                BinaryManagerStatus binaryManagerStatus = status.gcBinaries(
                        true);

                response.setGcDuration(
                        String.valueOf(binaryManagerStatus.gcDuration));
                response.setNumBinaries(
                        String.valueOf(binaryManagerStatus.numBinaries));
                response.setSizeBinaries(
                        String.valueOf(binaryManagerStatus.sizeBinaries));
                response.setNumBinariesGC(
                        String.valueOf(binaryManagerStatus.numBinariesGC));
                response.setSizeBinariesGC(
                        String.valueOf(binaryManagerStatus.sizeBinariesGC));

                if (clean) {
                    log.info(logInitMsg + "Orphan binaries deleted.");
                }

                log.info(logInitMsg
                        + "Orphaned binaries garbage collecting result: "
                        + binaryManagerStatus);
            } else {
                log.info(logInitMsg
                        + "Orphaned binaries garbage collecting is already in progress.");
            }

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

        }

        log.trace(logInitMsg + "--- EXIT ---");

        return response.convertToJson();
    }
}
