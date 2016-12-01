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
package com.aritu.eloraplm.promote.treetable;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public interface PromoteNodeManager {

    EloraConfigTable getHierarchicalRelationsConfig();

    EloraConfigTable getDirectRelationsConfig();

    Map<String, List<String>> getIconOnlyRelationDocs(DocumentModel currentDoc)
            throws EloraException;

    EloraConfigTable getRelationDescendingPropagationConfig();

    void processNodeInfo(DocumentModel doc, NodeDynamicInfo nodeInfo,
            int level, boolean isSpecial, String parentState, Statement stmt,
            String promoteTransition) throws EloraException;

}
