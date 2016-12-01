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
import com.aritu.eloraplm.bom.lists.treetable.BomListNodeService;
import com.aritu.eloraplm.treetable.NodeManager;

/**
 * @author aritu
 *
 */
public class BomWhereUsedListNodeService extends BomListNodeService implements
        NodeManager {

    public BomWhereUsedListNodeService(CoreSession session, String bomListId) {
        super(session);

        treeDirection = TREE_DIRECTION_WHERE_USED;
        nodeId = 0;
        this.bomListId = bomListId;
    }

}
