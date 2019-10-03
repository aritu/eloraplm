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
package com.aritu.eloraplm.integration.restoperations.util;

/**
 * @author aritu
 *
 */
public class CmProcessNode {

    private String nodeId;

    private String parentNodeId;

    private String rootItemUid;

    private boolean isRootItem;

    private String parentUid;

    private String originUid;

    private String originWcUid;

    private String destinationUid;

    private String destinationWcUid;

    private String action;

    private boolean isManaged;

    private String comment;

    public CmProcessNode(String nodeId, String parentNodeId, String rootItemUid,
            boolean isRootItem, String parentUid, String originUid,
            String originWcUid, String destinationUid, String destinationWcUid,
            String action, boolean isManaged, String comment) {
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        this.rootItemUid = rootItemUid;
        this.isRootItem = isRootItem;
        this.parentUid = parentUid;
        this.originUid = originUid;
        this.originWcUid = originWcUid;
        this.destinationUid = destinationUid;
        this.destinationWcUid = destinationWcUid;
        this.action = action;
        this.isManaged = isManaged;
        this.comment = comment;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public String getRootItemUid() {
        return rootItemUid;
    }

    public void setRootItemUid(String rootItemUid) {
        this.rootItemUid = rootItemUid;
    }

    public boolean getIsRootItem() {
        return isRootItem;
    }

    public void setIsRootItem(boolean isRootItem) {
        this.isRootItem = isRootItem;
    }

    public String getParentUid() {
        return parentUid;
    }

    public void setParentUid(String parentUid) {
        this.parentUid = parentUid;
    }

    public String getOriginUid() {
        return originUid;
    }

    public void setOriginUid(String originUid) {
        this.originUid = originUid;
    }

    public String getOriginWcUid() {
        return originWcUid;
    }

    public void setOriginWcUid(String originWcUid) {
        this.originWcUid = originWcUid;
    }

    public String getDestinationUid() {
        return destinationUid;
    }

    public void setDestinationUid(String destinationUid) {
        this.destinationUid = destinationUid;
    }

    public String getDestinationWcUid() {
        return destinationWcUid;
    }

    public void setDestinationWcUid(String destinationWcUid) {
        this.destinationWcUid = destinationWcUid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean getIsManaged() {
        return isManaged;
    }

    public void setIsManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
