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
package com.aritu.eloraplm.bom.lists.treetable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;

/**
 * @author aritu
 *
 */
public interface BomListNodeService {

    public static final String TREE_DIRECTION_COMPOSITION = "Composition";

    public static final String TREE_DIRECTION_WHERE_USED = "WhereUsed";

    public static final boolean NODES_EXPANDED_BY_DEFAULT = false;

    /**
     * Receives the parent document, and returns all the TreeNode structure
     *
     * @param parentObject
     * @return
     * @throws EloraException
     * @throws DocumentUnreadableException
     */
    TreeNode getRoot(Object parentObject)
            throws EloraException, DocumentUnreadableException;

    void init(CoreSession session, int nodeId, String treeDirection,
            String bomListId);

}
