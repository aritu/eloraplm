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
package com.aritu.eloraplm.relations.treetable;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class CadCompositionNodeService extends RelationNodeService
        implements NodeManager {

    public CadCompositionNodeService(CoreSession session, boolean showDrawings,
            boolean showUniqueVersionsPerDocument) {

        super(session);

        // try {
        // EloraConfigTable predicates =
        // EloraConfigHelper.getCadRelationsConfig();
        // } catch (EloraException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // TODO Hau konfiguraziotik bete!!
        iconOnlyPredicates.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                false);

        if (showDrawings) {
            predicates.put(EloraRelationConstants.CAD_DRAWING_OF, true);
        }

        predicates.put(EloraRelationConstants.CAD_COMPOSED_OF, false);
        predicates.put(EloraRelationConstants.CAD_BASED_ON, false);
        predicates.put(EloraRelationConstants.CAD_HAS_DESIGN_TABLE, false);
        predicates.put(EloraRelationConstants.CAD_HAS_SUPPRESSED, false);

        treeDirection = TREE_DIRECTION_COMPOSITION;

        relationType = RELATION_TYPE_CAD;

        nodeId = 0;

        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;

    }

    /**
     * Save the relationNodeData
     *
     * @param id
     * @param level
     * @param docId
     * @param data
     * @param predicateUri
     * @param quantity
     * @param comment
     * @param isObjectWc
     * @param isSpecial
     * @return
     */
    @Override
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, int quantity, String comment,
            boolean isObjectWc, int ordering, boolean isSpecial) {

        CadRelationNodeData nodeData = new CadRelationNodeData(id, level, docId,
                data, wcDoc, stmt, predicateUri, quantity, comment, isObjectWc,
                ordering, isSpecial);

        DocumentModelList relatedBomDocs = getRelatedBoms(nodeData);
        if (relatedBomDocs != null && !relatedBomDocs.isEmpty()) {
            List<DocumentModel> relatedBoms = new ArrayList<DocumentModel>();
            for (DocumentModel relatedBom : relatedBomDocs) {
                relatedBoms.add(relatedBom);
            }

            nodeData.setRelatedBoms(relatedBoms);
        }

        return nodeData;
    }

    protected DocumentModelList getRelatedBoms(CadRelationNodeData nodeData) {
        DocumentModelList relatedBomDocs = null;

        DocumentModel cadDocument = nodeData.getData();

        String predicateUri = EloraRelationConstants.BOM_HAS_CAD_DOCUMENT;
        Resource predicateResource = new ResourceImpl(predicateUri);

        relatedBomDocs = RelationHelper.getSubjectDocuments(predicateResource,
                cadDocument);

        return relatedBomDocs;
    }

}
