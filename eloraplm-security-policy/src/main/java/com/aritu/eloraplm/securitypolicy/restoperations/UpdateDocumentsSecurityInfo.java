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
package com.aritu.eloraplm.securitypolicy.restoperations;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.securitypolicy.api.SecurityConfigService;
import com.aritu.eloraplm.securitypolicy.constants.SecurityFacetConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyConstants;
import com.aritu.eloraplm.securitypolicy.util.SecurityInfo;

/**
 *
 * @author aritu
 *
 */

@Operation(id = UpdateDocumentsSecurityInfo.ID, category = SecurityPolicyConstants.SECURITY_POLICY_OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Update Documents Security Info", description = "Update documents security metadata and create security viewer file.")
public class UpdateDocumentsSecurityInfo {

    public static final String ID = "Elora.Plm.UpdateDocumentsSecurityInfo";

    private static final Log log = LogFactory.getLog(
            UpdateDocumentsSecurityInfo.class);

    @Context
    protected CoreSession session;

    @Param(name = "nxql", required = true)
    private String nxql;

    @Param(name = "levelSetDate", required = false)
    private Date levelSetDate;

    @Param(name = "levelSetBy", required = false)
    private String levelSetBy;

    @Param(name = "levelId", required = false)
    private Integer levelId;

    private UpdateDocumentsSecurityResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new UpdateDocumentsSecurityResponse();

        try {
            SecurityConfigService scs = Framework.getService(
                    SecurityConfigService.class);

            DocumentModelList queryResultDocs = session.query(nxql);
            Integer countDocs = 0;
            if (levelSetDate == null) {
                levelSetDate = new Date();
            }
            if (levelSetBy == null) {
                levelSetBy = session.getPrincipal().getName();
            }
            for (DocumentModel doc : queryResultDocs) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    if (doc.hasFacet(
                            SecurityFacetConstants.FACET_HAS_SECURITY_INFO)) {

                        if (levelId == null) {
                            levelId = scs.getTypeLevel(doc.getType());
                        }

                        SecurityInfo secInfo = new SecurityInfo(levelId,
                                levelSetDate, levelSetBy);

                        scs.updateSecurityInfo(doc, secInfo);

                        countDocs++;

                        log.trace(logInitMsg + "Document number: " + countDocs);
                    }
                } catch (EloraException e) {
                    log.error(logInitMsg + e.getMessage(), e);
                    TransactionHelper.setTransactionRollbackOnly();
                } catch (Exception e) {
                    log.error(logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                            e);
                    TransactionHelper.setTransactionRollbackOnly();
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }
            }

            response.setUpdatedDocQty(countDocs);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

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
