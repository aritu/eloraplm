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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
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
@Operation(id = MigrateRelatedManufacturersToSuppliers.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_ADMIN, label = "Admin - Migrate Related Manufacturers to Suppliers", description = "Migrate related manufacturers to related suppliers.")
public class MigrateRelatedManufacturersToSuppliers {
    public static final String ID = "Elora.Admin.MigrateRelatedManufacturersToSuppliers";

    private static final Log log = LogFactory.getLog(
            MigrateRelatedManufacturersToSuppliers.class);

    private static final String ELORA_RELATED_MANUFACTURERS = "manu:manufacturers";

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

            log.debug(logInitMsg
                    + "Starting related manufacturers to suppliers migration...");

            String query;
            if (inputQuery != null && !inputQuery.isEmpty()) {
                // If user has defined a query, use it
                query = inputQuery;
                log.debug(logInitMsg + "Using input query: |" + query + "|");
            } else {
                // Get all EloraWorkspaces, since the schema
                // related_manufacturers is only added to them
                query = "SELECT * FROM Document WHERE " + NXQL.ECM_MIXINTYPE
                        + " = '" + EloraFacetConstants.FACET_ELORA_WORKSPACE
                        + "' AND " + ELORA_RELATED_MANUFACTURERS
                        + "/0 IS NOT NULL";
            }

            DocumentModelList docsWithRelatedManufacturers = session.query(
                    query);

            // Process all documents
            for (DocumentModel docWithRelatedManufacturers : docsWithRelatedManufacturers) {

                processDoc(docWithRelatedManufacturers);

            }

            log.info(logInitMsg
                    + "All related manufacturers migrated to suppliers.");

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

    private void processDoc(DocumentModel doc) throws EloraException {

        String[] relatedManufacturers = (String[]) doc.getPropertyValue(
                ELORA_RELATED_MANUFACTURERS);
        if (relatedManufacturers.length > 0) {
            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> relatedSuppliers = (List<Map<String, Serializable>>) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_SUPP_SUPPLIERS);
            for (String relatedManufacturer : relatedManufacturers) {
                Map<String, Serializable> relatedSupplier = new HashMap<String, Serializable>();
                relatedSupplier.put(
                        EloraMetadataConstants.ELORA_SUPP_SUPPLIERS_SUPPLIER,
                        relatedManufacturer);
                relatedSuppliers.add(relatedSupplier);
            }
            doc.setPropertyValue(EloraMetadataConstants.ELORA_SUPP_SUPPLIERS,
                    (Serializable) relatedSuppliers);
            // disable validation as we do not want to change invalid values
            doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                    Forcing.TURN_OFF);
            // Disable notifications, etc. or users that follow the workspaces
            // will receive emails
            EloraDocumentHelper.disableVersioningDocument(doc);
            session.saveDocument(doc);
        }
    }

}
