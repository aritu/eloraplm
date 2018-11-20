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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;

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
}
