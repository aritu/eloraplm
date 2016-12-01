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

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.treetable.BaseNodeData;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CmItemsNodeData extends BaseNodeData {

    private static final long serialVersionUID = 1L;

    protected boolean isModifiedItem;

    protected DocumentModel originItem;

    protected DocumentModel originItemWc;

    protected String action;

    protected boolean actionIsReadOnly;

    protected DocumentModel destinationItem;

    protected boolean isManaged;

    protected boolean isManagedIsReadOnly;

    protected String type;

    // Constructors:
    public CmItemsNodeData(String id, int level, boolean isModifiedItem,
            DocumentModel originItem, DocumentModel originItemWc, String action,
            boolean actionIsReadOnly, DocumentModel destinationItem,
            boolean isManaged, boolean isManagedIsReadOnly, String type) {

        this(id, level, false, false, false, isModifiedItem, originItem,
                originItemWc, action, actionIsReadOnly, destinationItem,
                isManaged, isManagedIsReadOnly, type);
    }

    public CmItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModifiedItem, DocumentModel originItem,
            DocumentModel originItemWc, String action, boolean actionIsReadOnly,
            DocumentModel destinationItem, boolean isManaged,
            boolean isManagedIsReadOnly, String type) {

        this(id, level, isNew, isRemoved, false, isModifiedItem, originItem,
                originItemWc, action, actionIsReadOnly, destinationItem,
                isManaged, isManagedIsReadOnly, type);
    }

    public CmItemsNodeData(String id, int level, boolean isNew,
            boolean isRemoved, boolean isModified, boolean isModifiedItem,
            DocumentModel originItem, DocumentModel originItemWc, String action,
            boolean actionIsReadOnly, DocumentModel destinationItem,
            boolean isManaged, boolean isManagedIsReadOnly, String type) {

        super(id, level, isNew, isRemoved, isModified);
        this.isModifiedItem = isModifiedItem;
        this.originItem = originItem;
        this.originItemWc = originItemWc;
        this.action = action;
        this.actionIsReadOnly = actionIsReadOnly;
        this.destinationItem = destinationItem;
        this.isManaged = isManaged;
        this.isManagedIsReadOnly = isManagedIsReadOnly;
        this.type = type;
    }

    // Setters and getters
    public void setIsModifiedItem(boolean isModifiedItem) {
        this.isModifiedItem = isModifiedItem;
    }

    public boolean getIsModifiedItem() {
        return isModifiedItem;
    }

    public DocumentModel getOriginItem() {
        return originItem;
    }

    public void setOriginItem(DocumentModel originItem) {
        this.originItem = originItem;
    }

    public DocumentModel getOriginItemWc() {
        return originItemWc;
    }

    public void setOriginItemWc(DocumentModel originItemWc) {
        this.originItemWc = originItemWc;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean getActionIsReadOnly() {
        return actionIsReadOnly;
    }

    public void setActionIsReadOnly(boolean actionIsReadOnly) {
        this.actionIsReadOnly = actionIsReadOnly;
    }

    public DocumentModel getDestinationItem() {
        return destinationItem;
    }

    public void setDestinationItem(DocumentModel destinationItem) {
        this.destinationItem = destinationItem;
    }

    public boolean getIsManaged() {
        return isManaged;
    }

    public void setIsManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public boolean getIsManagedIsReadOnly() {
        return isManagedIsReadOnly;
    }

    public void setIsManagedIsReadOnly(boolean isManagedIsReadOnly) {
        this.isManagedIsReadOnly = isManagedIsReadOnly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
