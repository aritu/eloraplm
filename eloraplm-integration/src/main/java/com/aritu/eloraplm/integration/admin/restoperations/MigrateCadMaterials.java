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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.util.EloraIntegrationHelper;

/**
 * @author aritu
 *
 */
@Operation(id = MigrateCadMaterials.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_ADMIN, label = "Admin - Migrate CAD Materials", description = "Copy CAD material strings, separating them and inserting new individual material entries.")
public class MigrateCadMaterials {
    public static final String ID = "Elora.Admin.MigrateCadMaterials";

    private static final Log log = LogFactory.getLog(MigrateCadMaterials.class);

    private static final String ELORA_OLD_CAD_CAD_MATERIAL = "cad:cadMaterial";

    @Param(name = "query", required = false)
    private String inputQuery;

    @Context
    private OperationContext ctx;

    @Context
    private CoreSession session;

    private EloraGeneralResponse response;

    @OperationMethod
    public Blob run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new EloraGeneralResponse();

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (!session.getPrincipal().getName().equals("Administrator")) {
                throw new EloraException(
                        "Admin operations can only be executed by administrators.");
            }

            log.debug(logInitMsg + "Starting CAD materials migration...");

            String query;
            if (inputQuery != null && !inputQuery.isEmpty()) {
                // If user has defined a query, use it
                query = inputQuery;
                log.debug(logInitMsg + "Using input query: |" + query + "|");
            } else {
                // Get WCs of all the CAD documents
                query = "SELECT * FROM CadDocument WHERE " + NXQL.ECM_ISVERSION
                        + " = 0 AND " + NXQL.ECM_ISPROXY + " = 0 AND "
                        + ELORA_OLD_CAD_CAD_MATERIAL + " IS NOT NULL";
            }

            DocumentModelList cadDocs = session.query(query);

            // Process all documents
            for (DocumentModel cadDoc : cadDocs) {

                // a. Get all versions, process as AV
                List<DocumentModel> versions = session.getVersions(
                        cadDoc.getRef());
                for (DocumentModel version : versions) {
                    processVersionOrWc(version);
                }

                // b. Process WC
                processVersionOrWc(cadDoc);

            }

            log.info(logInitMsg + "All CAD document materials migrated.");

            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);
            session.save();

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

        log.trace(logInitMsg + "--- EXIT ---");

        return EloraIntegrationHelper.createJSONBlob(response.convertToJson());
    }

    private void processVersionOrWc(DocumentModel doc) throws EloraException {

        String oldCadMaterialString = (String) doc.getPropertyValue(
                ELORA_OLD_CAD_CAD_MATERIAL);
        if (oldCadMaterialString != null && !oldCadMaterialString.isEmpty()) {
            updateCadMaterials(doc, oldCadMaterialString);

            EloraDocumentHelper.disableVersioningDocument(doc);
            session.saveDocument(doc);
        }
    }

    private DocumentModel updateCadMaterials(DocumentModel doc,
            String oldCadMaterialString) {
        String[] cadMaterialsArray = oldCadMaterialString.split("\\|");
        List<String> cadMaterialsList = Arrays.asList(cadMaterialsArray);
        doc.setPropertyValue(EloraMetadataConstants.ELORA_CAD_MATERIALS,
                (Serializable) cadMaterialsList);
        return doc;
    }

}
