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
package com.aritu.eloraplm.viewer.restoperations;

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
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.constants.ViewerFileConstants;

/**
 *
 * @author aritu
 *
 */

@Operation(id = CreateDocumentsViewerFiles.ID, category = ViewerFileConstants.VIEWER_FILE_OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Create/Regenerate documents viewer files", description = "Create or regenerate documents viewer files")
public class CreateDocumentsViewerFiles {

    public static final String ID = "Elora.Plm.CreateDocumentsViewerFiles";

    private static final Log log = LogFactory.getLog(
            CreateDocumentsViewerFiles.class);

    @Context
    protected CoreSession session;

    @Param(name = "nxql", required = true)
    private String nxql;

    private CreateDocumentsViewerFilesResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.info(logInitMsg + "--- ENTER --- ");
        response = new CreateDocumentsViewerFilesResponse();

        try {
            DocumentModelList queryResultDocs = session.query(nxql);
            Integer countDocs = 0;
            Integer countTotalDocs = 0;

            if (queryResultDocs != null) {
                countTotalDocs = queryResultDocs.size();
            }

            log.info(logInitMsg + "There are |" + countTotalDocs
                    + "| documents to be processed.");

            for (DocumentModel doc : queryResultDocs) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    String comment = "";

                    if (!doc.isImmutable()) {
                        log.error(logInitMsg + "Document |" + doc.getId()
                                + "| is working copy and is ignored.");
                        continue;
                    }

                    EloraEventHelper.fireEvent(
                            PdmEventNames.PDM_VIEWER_DOC_OVERWRITTEN_EVENT, doc,
                            comment);

                    EloraDocumentHelper.disableVersioningDocument(doc);

                    session.saveDocument(doc);

                    if (EloraDocumentHelper.isBaseVersion(doc)) {
                        DocumentModel wcDoc = session.getWorkingCopy(
                                doc.getRef());
                        if (!wcDoc.isCheckedOut()) {
                            wcDoc = EloraDocumentHelper.restoreToVersion(
                                    wcDoc.getRef(), doc.getRef(), true, true,
                                    session);
                            // session.saveDocument(wcDoc);
                        }
                    }
                    countDocs++;

                    log.trace(logInitMsg + "Document number: " + countDocs);

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

            log.info(logInitMsg + "Document " + countDocs + " of "
                    + countTotalDocs + " processed.");

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

        log.info(logInitMsg + "--- EXIT ---");

        return response.convertToJson();

    }

}
