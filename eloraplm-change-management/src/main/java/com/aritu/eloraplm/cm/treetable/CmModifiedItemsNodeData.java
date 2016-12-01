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
package com.aritu.eloraplm.cm.treetable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author aritu
 *
 */

public class CmModifiedItemsNodeData extends CmItemsNodeData {

    private static final Log log = LogFactory.getLog(
            CmModifiedItemsNodeData.class);

    private static final long serialVersionUID = 1L;

    public CmModifiedItemsNodeData(String id, int level) {

        this(id, level, false, false, false, null, null, null, null, false,
                false, null);
    }

    public CmModifiedItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, DocumentModel originItem,
            DocumentModel originItemWc, String action,
            DocumentModel destinationItem, boolean isManaged,
            boolean isManagedIsReadOnly, String type) {

        // In case of modified items, the attributes isModifiedItem and
        // actionIsReadOnly are always true.
        super(id, level, isNew, isRemoved, isModified, true, originItem,
                originItemWc, action, true, destinationItem, isManaged,
                isManagedIsReadOnly, type);

    }

    // TODO::: hau beharrezkoa da????
    @Override
    public int compareTo(Object obj) {
        CmModifiedItemsNodeData objNode = (CmModifiedItemsNodeData) obj;

        String itemUniqueId = getOriginItem().getId();

        String objItemUniqueId = objNode.getOriginItem().getId();

        return itemUniqueId.compareTo(objItemUniqueId);
    }

}
