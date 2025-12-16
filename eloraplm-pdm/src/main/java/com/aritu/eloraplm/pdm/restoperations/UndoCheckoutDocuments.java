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
package com.aritu.eloraplm.pdm.restoperations;

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
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */

@Operation(id = UndoCheckoutDocuments.ID, category = "EloraPLM - Pdm", label = "EloraPlm - Undo checkout not locked documents", description = "Undo checkout working copy documents that are not locked. Restore to base version")
public class UndoCheckoutDocuments {

    public static final String ID = "Elora.Plm.UndoCheckoutDocuments";

    private static final Log log = LogFactory.getLog(
            UndoCheckoutDocuments.class);

    @Context
    protected CoreSession session;

    @Param(name = "nxql", required = true)
    private String nxql;

    private UndoCheckoutDocumentsResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new UndoCheckoutDocumentsResponse();

        try {
            CheckoutService cos = Framework.getService(CheckoutService.class);

            DocumentModelList queryResultDocs = session.query(nxql);
            Integer countDocs = 0;

            for (DocumentModel doc : queryResultDocs) {
                try {
                    if (doc.isCheckedOut() && !doc.isLocked()) {

                        DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(
                                doc);
                        if (baseVersion == null) {
                            log.trace(logInitMsg + "Document |" + doc.getId()
                                    + "| has no base version");
                            continue;
                        }

                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        cos.undoCheckout(doc);
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
