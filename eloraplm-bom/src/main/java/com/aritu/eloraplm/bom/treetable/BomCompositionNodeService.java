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
import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;
import com.aritu.eloraplm.relations.treetable.RelationNodeService;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class BomCompositionNodeService extends RelationNodeService
        implements NodeManager {

    public BomCompositionNodeService(CoreSession session,
            boolean showUniqueVersionsPerDocument) throws EloraException {
        super(session);

        // TODO: Hay que poner que coja de la configuracion los subtipos de
        // relaciones que se quieren!!!
        // predicates.put(EloraRelationConstants.BOM_COMPOSED_OF, false);
        // predicates.put(EloraRelationConstants.BOM_HAS_SPECIFICATION, false);

        loadConfigurations();

        treeDirection = TREE_DIRECTION_COMPOSITION;

        relationType = RELATION_TYPE_BOM;

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
     * @param isObjectWc
     * @param isSpecial
     * @return
     */
    @Override
    protected RelationNodeData saveRelationNodeData(String id, int level,
            String docId, DocumentModel data, DocumentModel wcDoc,
            Statement stmt, String predicateUri, int quantity, String comment,
            boolean isObjectWc, int ordering, boolean isSpecial) {

        return new BaseRelationNodeData(id, level, docId, data, wcDoc, stmt,
                predicateUri, quantity, comment, isObjectWc, ordering,
                isSpecial);
    }

    private void loadConfigurations() throws EloraException {
        // Hierarchical and direct relations
        EloraConfigTable hierarchicalRelationsConfig = EloraConfigHelper.getBomHierarchicalRelationsConfig();
        EloraConfigTable directRelationsConfig = EloraConfigHelper.getBomDirectRelationsConfig();

        loadPredicates(hierarchicalRelationsConfig);
        loadPredicates(directRelationsConfig);
    }

    private void loadPredicates(EloraConfigTable relationsConfig) {
        for (EloraConfigRow relationConfig : relationsConfig.getValues()) {
            boolean isSpecial = ((long) relationConfig.getProperty(
                    "isSpecial") == 1);
            String predicateUri = relationConfig.getProperty("id").toString();
            predicates.put(predicateUri, isSpecial);
        }
    }

}
