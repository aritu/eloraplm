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
package com.aritu.eloraplm.integration.checkin.restoperations.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aritu.eloraplm.core.util.EloraLockInfo;
import com.aritu.eloraplm.core.util.restoperations.OverrideMetadata;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.util.ItemInfo;

/**
 * @author aritu
 *
 */
public class TryCheckinResponseDoc {

    private int localId;

    private String wcUid;

    private String type;

    private String reference;

    private String title;

    private String description;

    private String filename;

    private String fileContentHash;

    private List<ItemInfo> itemsInfo;

    private VersionInfo currentVersionInfo;

    private String currentLifeCycleState;

    private String nextVersionLabel;

    private String latestVersionLabel;

    private EloraLockInfo eloraLockInfo;

    private String lastContributor;

    private Date lastModified;

    private String editionUrl;

    private List<OverrideMetadata> overrideMetadata;

    private String result;

    private List<ValidationErrorItem> errorList;

    public TryCheckinResponseDoc(int localId, String wcUid, String type,
            String reference, String title, String description, String filename,
            String fileContentHash, VersionInfo currentVersionInfo,
            String currentLifeCycleState, String nextVersionLabel,
            String latestVersionLabel, EloraLockInfo eloraLockInfo,
            String lastContributor, Date lastModified, String editionUrl) {
        this.localId = localId;
        this.wcUid = wcUid;
        this.type = type;
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.filename = filename;
        this.fileContentHash = fileContentHash;
        this.currentVersionInfo = currentVersionInfo;
        this.currentLifeCycleState = currentLifeCycleState;
        this.nextVersionLabel = nextVersionLabel;
        this.latestVersionLabel = latestVersionLabel;
        this.eloraLockInfo = eloraLockInfo;
        this.lastContributor = lastContributor;
        this.lastModified = lastModified;
        this.editionUrl = editionUrl;
        // Initialize the lists
        errorList = new ArrayList<ValidationErrorItem>();
        itemsInfo = new ArrayList<ItemInfo>();
        overrideMetadata = new ArrayList<OverrideMetadata>();
    }

    public TryCheckinResponseDoc(int localId, String wcUid, String type,
            String reference, String title, String description, String filename,
            String result, List<ValidationErrorItem> errorList) {
        this.localId = localId;
        this.wcUid = wcUid;
        this.type = type;
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.filename = filename;
        this.result = result;
        this.errorList = errorList;
        // Initialize the lists
        itemsInfo = new ArrayList<ItemInfo>();
        overrideMetadata = new ArrayList<OverrideMetadata>();
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
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

    public String getFileContentHash() {
        return fileContentHash;
    }

    public void setFileContentHash(String fileContentHash) {
        this.fileContentHash = fileContentHash;
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

    public String getNextVersionLabel() {
        return nextVersionLabel;
    }

    public void setNextVersionLabel(String nextVersionLabel) {
        this.nextVersionLabel = nextVersionLabel;
    }

    public String getLatestVersionLabel() {
        return latestVersionLabel;
    }

    public void setLatestVersionLabel(String latestVersionLabel) {
        this.latestVersionLabel = latestVersionLabel;
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

    public String getEditionUrl() {
        return editionUrl;
    }

    public void setEditionUrl(String editionUrl) {
        this.editionUrl = editionUrl;
    }

    public List<OverrideMetadata> getOverrideMetadata() {
        return overrideMetadata;
    }

    public void addOverrideMetadata(String property, Serializable value) {
        overrideMetadata.add(new OverrideMetadata(property, value));
    }

    public void addOverrideMetadataList(
            List<OverrideMetadata> overrideMetadata) {
        this.overrideMetadata.addAll(overrideMetadata);
    }

    public void emptyOverrideMetadata() {
        overrideMetadata.clear();
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<ValidationErrorItem> getErrorList() {
        return errorList;
    }

    public void addError(String field, String message) {
        errorList.add(new ValidationErrorItem(field, message));
    }

    public void addErrorList(List<ValidationErrorItem> errorList) {
        this.errorList.addAll(errorList);
    }

}
