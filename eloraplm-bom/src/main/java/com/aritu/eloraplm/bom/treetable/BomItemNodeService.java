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
package com.aritu.eloraplm.bom.treetable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeService;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class BomItemNodeService extends RelationNodeService
        implements NodeManager {

    public BomItemNodeService(CoreSession session,
            boolean showUniqueVersionsPerDocument,
            boolean showObsoleteStateDocuments, boolean isInverse)
            throws EloraException {

        super(session);
        if (!isInverse) {
            treeDirection = TREE_DIRECTION_COMPOSITION;
        } else {
            treeDirection = TREE_DIRECTION_WHERE_USED;
        }

        relationType = RELATION_TYPE_BOM;
        nodeId = 0;

        this.showUniqueVersionsPerDocument = showUniqueVersionsPerDocument;
        this.showObsoleteStateDocuments = showObsoleteStateDocuments;

        directRelationsList.addAll(RelationsConfig.bomAnarchicRelationsList);

        predicates.add(new ResourceImpl(EloraRelationConstants.BOM_HAS_BOM));

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
     * @param isSpecial
     * @return
     */
    @Override
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, String quantity,
            String comment, Integer ordering, Integer directorOrdering,
            Integer viewerOrdering, Integer inverseViewerOrdering,
            Boolean isManual, boolean isSpecial, boolean isDirect) {

        return new BaseRelationNodeData(id, level, docId, data, wcDoc, stmt,
                predicateUri, quantity, comment, ordering, directorOrdering,
                viewerOrdering, inverseViewerOrdering, isManual, isSpecial,
                isDirect);
    }

    @Override
    protected DocumentModel getTreeRootDocument(DocumentModel doc)
            throws EloraException {
        if (doc.isProxy()) {
            throw new EloraException("The root document is a proxy.");
        }
        // In this case we always want to show relations of the archived
        // version
        DocumentModel versionDoc = null;
        if (doc.isVersion()) {
            versionDoc = doc;
        } else {
            versionDoc = EloraDocumentHelper.getLatestVersion(doc);
            if (versionDoc == null) {
                versionDoc = doc;
            }
        }
        return versionDoc;
    }
}
