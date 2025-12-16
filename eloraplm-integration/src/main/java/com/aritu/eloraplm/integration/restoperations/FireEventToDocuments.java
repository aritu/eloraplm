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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */

@Operation(id = FireEventToDocuments.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Fire Event to Documents", description = "Fire the given event to the documents returned by the specified nxql query.")
public class FireEventToDocuments {

    public static final String ID = "Elora.Plm.FireEventToDocuments";

    private static final Log log = LogFactory.getLog(
            FireEventToDocuments.class);

    @Context
    protected CoreSession session;

    @Param(name = "nxql", required = true)
    private String nxql;

    @Param(name = "event", required = true)
    private String event;

    @Param(name = "comment", required = false)
    private String comment;

    private EloraGeneralResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";

        response = new EloraGeneralResponse();

        try {

            DocumentModelList queryResultDocs = session.query(nxql);

            int totalCount = queryResultDocs.size();

            log.trace(logInitMsg + "|" + totalCount + "| documents found");

            int counter = 0;
            for (DocumentModel doc : queryResultDocs) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    counter++;
                    log.trace(logInitMsg + "Processing document |" + counter
                            + "| of |" + totalCount + "|.");
                    log.trace(logInitMsg + "Fire |" + event
                            + "| event on document with uid |" + doc.getId()
                            + "|");

                    EloraEventHelper.fireEvent(event, doc, comment);

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
