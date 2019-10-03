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

import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.integration.util.ItemInfo;

/**
 * @author aritu
 *
 */
public class GetFileInfoResponseDoc {

    private String authoringTool;

    private String authoringToolVersion;

    private String currentLifeCycleState;

    private VersionInfo currentVersionInfo;

    private String description;

    private String filename;

    private String hash;

    private EloraLockInfo eloraLockInfo;

    private String lastContributor;

    private Date lastModified;

    private String latestVersionLabel;

    private String path;

    private String realUid;

    private String reference;

    private String summaryUrl;

    private String title;

    private String type;

    private String wcUid;

    private String proxyUid;

    private List<ItemInfo> itemsInfo;

    public GetFileInfoResponseDoc() {
    }

    public GetFileInfoResponseDoc(String realUid, String wcUid, String proxyUid,
            String type, String reference, String title, String filename,
            String hash, String description, VersionInfo currentVersionInfo,
            String currentLifeCycleState, String latestVersionLabel,
            String path, EloraLockInfo eloraLockInfo, String lastContributor,
            Date lastModified, String summaryUrl, String authoringTool,
            String authoringToolVersion) {

        this.realUid = realUid;
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
        this.path = path;
        this.eloraLockInfo = eloraLockInfo;
        this.lastContributor = lastContributor;
        this.lastModified = lastModified;
        this.summaryUrl = summaryUrl;
        this.proxyUid = proxyUid;
        this.authoringTool = authoringTool;
        this.authoringToolVersion = authoringToolVersion;

        // Initialize the lists
        itemsInfo = new ArrayList<ItemInfo>();
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLatestVersionLabel() {
        return latestVersionLabel;
    }

    public void setLatestVersionLabel(String latestVersionLabel) {
        this.latestVersionLabel = latestVersionLabel;
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
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

    public String getWcUid() {
        return wcUid;
    }

    public void setWcUid(String wcUid) {
        this.wcUid = wcUid;
    }

    public String getProxyUid() {
        return proxyUid;
    }

    public void setProxyUid(String proxyUid) {
        this.proxyUid = proxyUid;
    }

    public List<ItemInfo> getItemsInfo() {
        return itemsInfo;
    }

    public void setItemsInfo(List<ItemInfo> itemsInfo) {
        this.itemsInfo = itemsInfo;
    }
}
