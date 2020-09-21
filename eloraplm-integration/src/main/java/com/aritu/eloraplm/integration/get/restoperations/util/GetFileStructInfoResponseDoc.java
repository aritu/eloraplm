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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.integration.util.ItemInfo;
import com.aritu.eloraplm.integration.util.RelationInfo;
import com.aritu.eloraplm.integration.util.VersionItem;

/**
 * @author aritu
 *
 */

@JsonPropertyOrder({ "cadParentRealUid", "currentLifeCycleState",
        "currentVersionInfo", "description", "eloraLockInfo", "enforce",
        "filename", "hash", "itemsInfo", "lastContributor", "lastModified",
        "latestVersionLabel", "parentRealUid", "proxyUid", "realUid",
        "reference", "relations", "selected", "summaryUrl", "title", "type",
        "versions", "wcUid" })
public class GetFileStructInfoResponseDoc {

    private String cadParentRealUid;

    private String currentLifeCycleState;

    private VersionInfo currentVersionInfo;

    private String description;

    private EloraLockInfo eloraLockInfo;

    private boolean enforce;

    private String filename;

    private String hash;

    private List<ItemInfo> itemsInfo;

    private String lastContributor;

    private Date lastModified;

    private String latestVersionLabel;

    private String proxyUid;

    private String realUid;

    private String parentRealUid;

    private String reference;

    private List<RelationInfo> relations;

    private boolean selected;

    private String summaryUrl;

    private String title;

    private String type;

    private List<VersionItem> versions;

    private String wcUid;

    public GetFileStructInfoResponseDoc() {
        // Initialize relations list
        relations = new ArrayList<RelationInfo>();
    }

    public GetFileStructInfoResponseDoc(String proxyUid, String realUid,
            String parentRealUid, String wcUid, String type, String reference,
            String title, String filename, String hash, String description,
            VersionInfo currentVersionInfo, String currentLifeCycleState,
            String latestVersionLabel, List<VersionItem> versions,
            EloraLockInfo eloraLockInfo, String lastContributor,
            Date lastModified, String summaryUrl) {
        this.proxyUid = proxyUid;
        this.realUid = realUid;
        this.parentRealUid = parentRealUid;
        this.wcUid = wcUid;
        this.type = type;
        this.reference = reference;
        this.title = title;
        this.filename = filename;
        this.hash = hash;
        this.description = description;
        this.currentVersionInfo = currentVersionInfo;
        this.currentLifeCycleState = currentLifeCycleState;
        this.latestVersionLabel = latestVersionLabel;
        this.versions = versions;
        this.eloraLockInfo = eloraLockInfo;
        this.lastContributor = lastContributor;
        this.lastModified = lastModified;
        this.summaryUrl = summaryUrl;
        // Initialize the lists
        itemsInfo = new ArrayList<ItemInfo>();
        relations = new ArrayList<RelationInfo>();
    }

    public String getCadParentRealUid() {
        return cadParentRealUid;
    }

    public void setCadParentRealUid(String cadParentRealUid) {
        this.cadParentRealUid = cadParentRealUid;
    }

    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    public void setCurrentLifeCycleState(String currentLifeCycleState) {
        this.currentLifeCycleState = currentLifeCycleState;
    }

    public VersionInfo getCurrentVersionInfo() {
        return currentVersionInfo;
    }

    public void setCurrentVersionInfo(VersionInfo currentVersionInfo) {
        this.currentVersionInfo = currentVersionInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getEnforce() {
        return enforce;
    }

    public void setEnforce(boolean enforce) {
        this.enforce = enforce;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<ItemInfo> getItemsInfo() {
        return itemsInfo;
    }

    public void emptyItemsInfo() {
        itemsInfo.clear();
    }

    public void addItemInfo(ItemInfo itemInfo) {
        itemsInfo.add(itemInfo);
    }

    public void setItemsInfo(List<ItemInfo> itemsInfo) {
        this.itemsInfo = itemsInfo;
    }

    public EloraLockInfo getEloraLockInfo() {
        return eloraLockInfo;
    }

    public void setEloraLockInfo(EloraLockInfo eloraLockInfo) {
        this.eloraLockInfo = eloraLockInfo;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLatestVersionLabel() {
        return latestVersionLabel;
    }

    public void setLatestVersionLabel(String latestVersionLabel) {
        this.latestVersionLabel = latestVersionLabel;
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

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getSummaryUrl() {
        return summaryUrl;
    }

    public void setSummaryUrl(String summaryUrl) {
        this.summaryUrl = summaryUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<VersionItem> getVersions() {
        return versions;
    }

    public void setVersions(List<VersionItem> versions) {
        this.versions = versions;
    }

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public List<RelationInfo> getRelations() {
        return relations;
    }

    public void emptyRelations() {
        relations.clear();
    }

    public void addRelation(RelationInfo relation) {
        relations.add(relation);
    }

    @JsonIgnore
    public PropagationProperty getPropagationProperty() {
        return new PropagationProperty(selected, enforce);
    }

    public void setPropagationProperty(PropagationProperty propagationProp) {
        selected = propagationProp.getSelected();
        enforce = propagationProp.getEnforce();
    }

}
