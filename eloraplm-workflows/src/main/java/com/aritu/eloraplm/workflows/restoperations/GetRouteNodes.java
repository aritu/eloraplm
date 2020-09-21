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
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = GetRouteNodes.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Get Route Nodes", description = "Get all route nodes that match the filter from the provided document route.")
public class GetRouteNodes {

    public static final String ID = "Elora.Plm.GetRouteNodes";

    @Context
    private CoreSession session;

    @Param(name = "nodeId", required = true)
    private String nodeId;

    @OperationMethod
    public DocumentModelList run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod
    public DocumentModelList run(DocumentModel doc) throws EloraException {
        return getRouteNodesByNodeId(doc.getId());
    }

    public DocumentModelList getRouteNodesByNodeId(String drId) {

        String query = String.format("SELECT * FROM RouteNode WHERE "
                + NXQL.ECM_PARENTID + " = '%s'" + " AND rnode:nodeId = '%s'"
                + " ORDER BY dc:created DESC", drId, nodeId);

        UnrestrictedQueryRunner queryRunner = new UnrestrictedQueryRunner(
                session, query);

        return queryRunner.runQuery();
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
