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
package com.aritu.eloraplm.queries.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.queries.UnrestrictedQueryRunner;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraQueryHelper {

    public static String formatList(Collection<String> list) {

        StringJoiner sj = new StringJoiner("','", "'", "'");
        list.forEach(sj::add);

        return sj.toString();
    }

    public static String formatUnquotedList(Collection<String> list) {

        StringJoiner sj = new StringJoiner(",");
        list.forEach(sj::add);

        return sj.toString();
    }

    public static String formatRefList(List<DocumentRef> objectVersionRefs) {
        return objectVersionRefs.stream().map(x -> x.toString()).collect(
                Collectors.joining("','", "'", "'"));
    }

    public static DocumentModel executeGetFirstQuery(String query,
            CoreSession session) {

        DocumentModelList docList = session.query(query);
        if (docList.size() > 0) {
            return docList.get(0);
        } else {
            return null;
        }

    }

    public static long executeCountQuery(String query, String countColumn,
            CoreSession session) {
        IterableQueryResult queryResult = session.queryAndFetch(query,
                NXQL.NXQL);

        long count = 0;
        try {
            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                count = (long) map.get("COUNT(" + countColumn + ")");
            }
        } finally {
            queryResult.close();
        }
        return count;
    }

    /**
     * This method executes the specified query and returns the resulting UID
     * set.
     *
     * @param query
     * @param session
     * @return
     */
    public static Set<String> executeQueryAndGetResultUidList(String query,
            CoreSession session) {

        // We use Set in order to avoid duplicates, since we don't need
        // duplicated uid-s
        Set<String> uidList = new HashSet<String>();

        DocumentModelList queryResultDocs = session.query(query);
        if (queryResultDocs != null && !queryResultDocs.isEmpty()) {
            for (DocumentModel doc : queryResultDocs) {
                uidList.add(doc.getId());
            }
        }

        return uidList;
    }

    /**
     * This method executes the specified query and appends the resulting UID
     * collection to the given UID set.
     *
     * @param query
     * @param linksUIList
     * @param session
     */
    public static void executeQueryAndAppendResultUidList(String query,
            Set<String> linksUIList, CoreSession session) {

        Set<String> partialUIList = new HashSet<String>();

        partialUIList = executeQueryAndGetResultUidList(query, session);
        if (!partialUIList.isEmpty()) {
            if (linksUIList == null) {
                linksUIList = new HashSet<String>();
            }

            linksUIList.addAll(partialUIList);
        }
    }

    /**
     * This method executes the specified query and returns the resulting
     * property list as strings.
     *
     * @param query
     * @param session
     * @return
     */
    public static List<String> executeQueryAndGetResultStringList(String column,
            String query, CoreSession session) {

        List<String> results = new ArrayList<String>();

        IterableQueryResult queryResult = session.queryAndFetch(query,
                NXQL.NXQL);

        try {
            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                String value = (String) map.get(column);
                results.add(value);
            }
        } finally {
            queryResult.close();
        }

        return results;
    }

    /***************** UNRESTRICTED QUERIES ******************/

    public static DocumentModelList executeUnrestrictedQuery(
            CoreSession session, String query) {
        UnrestrictedQueryRunner uqr = new UnrestrictedQueryRunner(session,
                query);
        return uqr.query();
    }

    public static IterableQueryResult executeUnrestrictedQueryAndFetch(
            CoreSession session, String query) {
        UnrestrictedQueryRunner uqr = new UnrestrictedQueryRunner(session,
                query);
        return uqr.queryAndFetch();
    }

    public static long executeUnrestrictedCountQuery(CoreSession session,
            String query, String countColumn) {
        IterableQueryResult queryResult = executeUnrestrictedQueryAndFetch(
                session, query);

        long count = 0;
        try {
            if (queryResult.iterator().hasNext()) {
                Map<String, Serializable> map = queryResult.iterator().next();
                count = (long) map.get("COUNT(" + countColumn + ")");
            }
        } finally {
            queryResult.close();
        }
        return count;
    }

}
