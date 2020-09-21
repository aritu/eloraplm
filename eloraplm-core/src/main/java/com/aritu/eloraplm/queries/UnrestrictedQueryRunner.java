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
package com.aritu.eloraplm.queries;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 *
 * @author aritu
 *
 */
public class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

    private final static String OP_QUERY = "QUERY";

    private final static String OP_QUERY_AND_FETCH = "QUERY_AND_FETCH";

    String operation = OP_QUERY;

    String query;

    DocumentModelList queryResult;

    IterableQueryResult iterableQueryResult;

    long countResult;

    public UnrestrictedQueryRunner(CoreSession session, String query) {
        super(session);
        this.query = query;
    }

    @Override
    public void run() {

        switch (operation) {
        case OP_QUERY_AND_FETCH:
            iterableQueryResult = session.queryAndFetch(query, NXQL.NXQL);
            break;
        case OP_QUERY:
        default:
            queryResult = session.query(query);
            break;
        }

    }

    public DocumentModelList query() {
        operation = OP_QUERY;
        runUnrestricted();
        return queryResult;
    }

    public IterableQueryResult queryAndFetch() {
        operation = OP_QUERY_AND_FETCH;
        runUnrestricted();
        return iterableQueryResult;
    }

}