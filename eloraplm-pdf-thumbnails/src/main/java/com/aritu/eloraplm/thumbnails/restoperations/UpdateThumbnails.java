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
package com.aritu.eloraplm.thumbnails.restoperations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author aritu
 *
 */

@Operation(id = UpdateThumbnails.ID, category = Constants.CAT_DOCUMENT, label = "EloraImport - Update Thumbnails", description = "Update all the thumbnails of a document type.")
public class UpdateThumbnails {

    public static final String ID = "Elora.Import.UpdateThumbnails";

    private static final Log log = LogFactory.getLog(UpdateThumbnails.class);

    @Param(name = "doctype", required = false)
    protected String doctype;

    @Param(name = "ancestorUid", required = false)
    protected String ancestorUid;

    @Param(name = "filterQuery", required = false)
    protected String filterQuery;

    @Param(name = "regenerate", required = true)
    protected boolean regenerate;

    @Param(name = "limited", required = true)
    protected boolean limited;

    @Param(name = "offset", required = false)
    protected long offset;

    @Param(name = "limit", required = false)
    protected long limit;

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    protected TypeManager typeManager = Framework.getService(TypeManager.class);

    protected EloraGeneralResponse response;

    @OperationMethod
    public String run() throws EloraException {

        String logInitMsg = "[run] [" + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        response = new EloraGeneralResponse();

        try {
            // TransactionHelper.commitOrRollbackTransaction();
            // TransactionHelper.startTransaction();

            String query = "select * from Document where ecm:mixinType != 'HiddenInNavigation' ";

            if (doctype != null) {

                if (!typeManager.hasType(doctype)) {
                    throw new EloraException("Provided doctype |" + doctype
                            + "| does not exist.");
                }
                query += " and ecm:primaryType = '" + doctype + "' ";
            }

            if (ancestorUid != null && session.exists(new IdRef(ancestorUid))) {
                query += " and ecm:ancestorId = '" + ancestorUid + "' ";
            }

            if (filterQuery != null) {
                query += " and " + filterQuery + " ";
            }

            if (!regenerate) {
                query += " and thumb:thumbnail/name is null";
            }

            DocumentModelList docs = null;
            if (limited) {
                docs = session.query(query, null, limit, offset, false);
            } else {
                docs = session.query(query);
            }

            if (!docs.isEmpty()) {
                for (DocumentModel doc : docs) {
                    log.trace(logInitMsg
                            + " Thumbnail regeneration requested for document |"
                            + doc.getId() + "|.");

                    DocumentEventContext ctx = new DocumentEventContext(session,
                            session.getPrincipal(), doc);

                    Framework.getLocalService(EventService.class).fireEvent(
                            ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name(),
                            ctx);
                }
            }

            log.info(logInitMsg
                    + "Thumbnails regeneration for documents requested (doctype: |"
                    + doctype + "| ancestorUid: |" + ancestorUid
                    + "| filterQuery: |" + filterQuery + "| regenerate: |"
                    + regenerate + "| offset: |" + offset + "| limit: |" + limit
                    + "|).");
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            response.setResult(EloraGeneralConstants.RESPONSE_STATUS_ERROR);
            response.setErrorMessage(e.getMessage());

            // TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            response.setResult(
                    EloraGeneralConstants.RESPONSE_STATUS_UNCONTROLLED_ERROR);
            response.setErrorMessage(
                    e.getClass().getName() + ". " + e.getMessage());

            // TransactionHelper.setTransactionRollbackOnly();
        } finally {
            // TransactionHelper.commitOrRollbackTransaction();
            // TransactionHelper.startTransaction();
        }

        // Create JSON response
        String jsonResponse = response.convertToJson();

        log.trace(logInitMsg + "--- EXIT ---");
        return jsonResponse;
    }
}
