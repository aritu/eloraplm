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
package com.aritu.eloraplm.bom.lists;

import java.util.ArrayList;
import java.util.List;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
public class BomListHelper {

    /**
     * @param doc
     * @param bomListId
     * @param inverse
     * @param session
     * @return
     */
    public static DocumentModelList getBomListForDocument(DocumentModel doc,
            String bomListId, boolean inverse, CoreSession session) {

        List<String> allBomListUids = getAllBomListUidsForDocument(doc, inverse,
                session);
        DocumentModelList bomListDocs = null;

        if (!allBomListUids.isEmpty()) {
            String queryLists = EloraQueryFactory.getBomListsByListIdQuery(
                    bomListId, allBomListUids);

            bomListDocs = session.query(queryLists);
        }

        return bomListDocs;
    }

    /**
     * @param doc
     * @param inverse
     * @param session
     * @return
     */
    public static List<String> getAllBomListUidsForDocument(DocumentModel doc,
            boolean inverse, CoreSession session) {

        List<String> allBomListUids = new ArrayList<>();

        if (!inverse) {

            DocumentModelList bomLists = RelationHelper.getObjectDocuments(doc,
                    new ResourceImpl(EloraRelationConstants.BOM_HAS_LIST));

            if (bomLists.size() > 0) {
                for (DocumentModel bomList : bomLists) {
                    allBomListUids.add(bomList.getId());
                }
            }

        } else {

            DocumentModelList bomLists = RelationHelper.getSubjectDocuments(
                    new ResourceImpl(EloraRelationConstants.BOM_LIST_HAS_ENTRY),
                    doc);

            if (bomLists.size() > 0) {
                for (DocumentModel bomList : bomLists) {
                    allBomListUids.add(bomList.getId());
                }
            }

        }

        return allBomListUids;
    }

}
