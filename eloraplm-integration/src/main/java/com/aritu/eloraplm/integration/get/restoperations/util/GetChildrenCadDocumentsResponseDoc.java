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
package com.aritu.eloraplm.integration.get.restoperations.util;

import java.util.List;

import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.integration.util.ItemInfo;

/**
 * @author aritu
 *
 */

public class GetChildrenCadDocumentsResponseDoc {

    private String parentRealUid;

    private String proxyUid;

    private String realUid;

    private String wcUid;

    private String type;

    private String reference;

    private String title;

    private VersionInfo currentVersionInfo;

    private String currentLifeCycleState;

    private String path;

    private String authoringTool;

    private String authoringToolVersion;

    private EloraLockInfo eloraLockInfo;

    private List<ItemInfo> itemsInfo;

    private boolean hasChildren;

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
    }

    public String getProxyUid() {
        return proxyUid;
    }

    public void setProxyUid(String proxyUid) {
        this.proxyUid = proxyUid;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public VersionInfo getCurrentVersionInfo() {
        return currentVersionInfo;
    }

    public void setCurrentVersionInfo(VersionInfo currentVersionInfo) {
        this.currentVersionInfo = currentVersionInfo;
    }

    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    public void setCurrentLifeCycleState(String currentLifeCycleState) {
        this.currentLifeCycleState = currentLifeCycleState;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAuthoringTool() {
        return authoringTool;
    }

    public void setAuthoringTool(String authoringTool) {
        this.authoringTool = authoringTool;
    }

    public String getAuthoringToolVersion() {
        return authoringToolVersion;
    }

    public void setAuthoringToolVersion(String authoringToolVersion) {
        this.authoringToolVersion = authoringToolVersion;
    }

    // public String getStructureRootUid() {
    // return structureRootUid;
    // }
    //
    // public void setStructureRootUid(String pStructureRootUid) {
    // structureRootUid = pStructureRootUid;
    // }
    //
    public EloraLockInfo getEloraLockInfo() {
        return eloraLockInfo;
    }

    public void setEloraLockInfo(EloraLockInfo eloraLockInfo) {
        this.eloraLockInfo = eloraLockInfo;
    }

    public List<ItemInfo> getItemsInfo() {
        return itemsInfo;
    }

    public void setItemsInfo(List<ItemInfo> itemsInfo) {
        this.itemsInfo = itemsInfo;
    }

    public boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

}
