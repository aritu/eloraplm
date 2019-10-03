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
package com.aritu.eloraplm.cm.util;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This class encapsulates data used in the context of Impacted Item Creation.
 *
 * @author aritu
 *
 */
public class ImpactedItemContext {

    private DocumentModel modifiedItem;

    private DocumentModel grandParentItem;

    private DocumentModel parentItem;

    private Long rowNumber;

    private String parentNodePath;

    private String parentNodeId;

    private String modifiedItemAction;

    private String modifiedItemDestinationWcUid;

    private String parentItemAction;

    /**
     * @param modifiedItem
     * @param grandParentItem
     * @param parentItem
     * @param rowNumber
     * @param parentNodePath
     * @param parentNodeId
     * @param modifiedItemAction
     * @param modifiedItemDestinationWcUid
     * @param parentItemAction
     */
    public ImpactedItemContext(DocumentModel modifiedItem,
            DocumentModel grandParentItem, DocumentModel parentItem,
            Long rowNumber, String parentNodePath, String parentNodeId,
            String modifiedItemAction, String modifiedItemDestinationWcUid,
            String parentItemAction) {
        super();
        this.modifiedItem = modifiedItem;
        this.grandParentItem = grandParentItem;
        this.parentItem = parentItem;
        this.rowNumber = rowNumber;
        this.parentNodePath = parentNodePath;
        this.parentNodeId = parentNodeId;
        this.modifiedItemAction = modifiedItemAction;
        this.modifiedItemDestinationWcUid = modifiedItemDestinationWcUid;
        this.parentItemAction = parentItemAction;
    }

    public DocumentModel getModifiedItem() {
        return modifiedItem;
    }

    public DocumentModel getGrandParentItem() {
        return grandParentItem;
    }

    public DocumentModel getParentItem() {
        return parentItem;
    }

    public Long getRowNumber() {
        return rowNumber;
    }

    public void increaseRowNumber() {
        rowNumber++;
    }

    public String getParentNodePath() {
        return parentNodePath;
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public String getModifiedItemAction() {
        return modifiedItemAction;
    }

    public String getModifiedItemDestinationWcUid() {
        return modifiedItemDestinationWcUid;
    }

    public String getParentItemAction() {
        return parentItemAction;
    }

}
