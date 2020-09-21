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
package com.aritu.eloraplm.workflows.restoperations;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = GetLatestDocumentRoute.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Get Latest Document Route", description = "Get the latest workflow route executed on the document.")
public class GetLatestDocumentRoute {

    public static final String ID = "Elora.Plm.GetLatestDocumentRoute";

    @Context
    private CoreSession session;

    @Param(name = "workflowName", required = true)
    private String workflowName;

    @OperationMethod
    public DocumentModel run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws EloraException {
        return getLatestDocumentRouteByName(doc.getId());
    }

    public DocumentModel getLatestDocumentRouteByName(String attachedDocId) {

        String query = String.format("SELECT * FROM DocumentRoute WHERE "
                + NXQL.ECM_LIFECYCLESTATE + " = '"
                + DocumentRouteElement.ElementLifeCycleState.done
                + "' AND docri:participatingDocuments/* = '%s'"
                + " AND (ecm:name like '%s.%%' OR ecm:name = '%s')"
                // ordering by dc:created makes sure that
                // a sub-workflow is listed under its parent
                + " ORDER BY dc:created DESC", attachedDocId, workflowName,
                workflowName);

        DocumentModelList routes = null;
        UnrestrictedQueryRunner queryRunner = new UnrestrictedQueryRunner(
                session, query);
        routes = queryRunner.runQuery();

        return routes.isEmpty() ? null : routes.get(0);
    }

    class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {
        String query;

        DocumentModelList docs;

        protected UnrestrictedQueryRunner(CoreSession session, String query) {
            super(session);
            this.query = query;
        }

        @Override
        public void run() {
            docs = session.query(query);
            for (DocumentModel documentModel : docs) {
                documentModel.detach(true);
            }
        }

        public DocumentModelList runQuery() {
            runUnrestricted();
            return docs;
        }
    }

}
