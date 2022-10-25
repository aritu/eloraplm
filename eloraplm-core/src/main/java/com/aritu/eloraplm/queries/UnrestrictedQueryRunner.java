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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final static String OP_QUERY_AND_COUNT = "QUERY_AND_COUNT";

    private final static String OP_QUERY_AND_FETCH = "QUERY_AND_FETCH";

    String operation = OP_QUERY;

    String query;

    IterableQueryResult itQueryResult;

    DocumentModelList queryResult;

    String countColumn = "";

    String fetchColumn = "";

    long countResult;

    List<String> fetchResult;

    public UnrestrictedQueryRunner(CoreSession session, String query) {
        super(session);
        this.query = query;
    }

    @Override
    public void run() {

        switch (operation) {
        case OP_QUERY_AND_COUNT:
            itQueryResult = null;
            try {
                itQueryResult = session.queryAndFetch(query, NXQL.NXQL);
                if (itQueryResult.iterator().hasNext()) {
                    Map<String, Serializable> map = itQueryResult.iterator().next();
                    countResult = (long) map.get("COUNT(" + countColumn + ")");
                }
            } catch (Exception e) {
                countResult = -1;
            } finally {
                if (itQueryResult != null) {
                    itQueryResult.close();
                }
            }
            break;
        case OP_QUERY_AND_FETCH:
            fetchResult = new ArrayList<String>();
            itQueryResult = null;
            try {
                itQueryResult = session.queryAndFetch(query, NXQL.NXQL);
                while (itQueryResult.iterator().hasNext()) {
                    Map<String, Serializable> map = itQueryResult.iterator().next();
                    fetchResult.add((String) map.get(fetchColumn));
                }
            } catch (Exception e) {
                fetchResult.clear();
            } finally {
                if (itQueryResult != null) {
                    itQueryResult.close();
                }
            }
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

    public long queryAndCount(String countColumn) {
        operation = OP_QUERY_AND_COUNT;
        this.countColumn = countColumn;
        runUnrestricted();
        return countResult;
    }

    public List<String> queryAndFetch(String fetchColumn) {
        operation = OP_QUERY_AND_FETCH;
        this.fetchColumn = fetchColumn;
        runUnrestricted();
        return fetchResult;
    }

}
