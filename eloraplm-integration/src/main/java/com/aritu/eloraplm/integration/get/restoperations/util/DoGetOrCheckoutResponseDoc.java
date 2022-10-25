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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.restoperations.OverrideMetadata;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;

/**
 * @author aritu
 *
 */

@JsonPropertyOrder({ "cadAttachments", "currentLifeCycleState",
        "currentVersionLabel", "description", "downloadUrl", "errorList",
        "filename", "hash", "overrideMetadata", "parentRealUid", "proxyUid",
        "realUid", "reference", "result", "title", "type", "wcUid" })
public class DoGetOrCheckoutResponseDoc {

    private List<CadAttachmentDownloadInfo> cadAttachments;

    private String currentLifeCycleState;

    private String currentVersionLabel;

    private String description;

    private String downloadUrl;

    private List<ValidationErrorItem> errorList;

    private String filename;

    private String hash;

    private List<OverrideMetadata> overrideMetadata;

    private String parentRealUid;

    private String proxyUid;

    private String realUid;

    private String reference;

    private String result;

    private String title;

    private String type;

    private String wcUid;

    public DoGetOrCheckoutResponseDoc(String proxyUid, String realUid,
            String wcUid, String parentRealUid, String type, String reference,
            String title, String description, String currentVersionLabel,
            String currentLifeCycleState, String result,
            List<ValidationErrorItem> errorList) {
        this.proxyUid = proxyUid;
        this.realUid = realUid;
        this.wcUid = wcUid;
        this.parentRealUid = parentRealUid;
        this.type = type;
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.currentVersionLabel = currentVersionLabel;
        this.currentLifeCycleState = currentLifeCycleState;
        this.result = result;
        this.errorList = errorList;
        // Initialize the lists
        overrideMetadata = new ArrayList<>();
        cadAttachments = new ArrayList<CadAttachmentDownloadInfo>();
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

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
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

    public String getCurrentVersionLabel() {
        return currentVersionLabel;
    }

    public void setCurrentVersionLabel(String currentVersionLabel) {
        this.currentVersionLabel = currentVersionLabel;
    }

    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    public void setCurrentLifeCycleState(String currentLifeCycleState) {
        this.currentLifeCycleState = currentLifeCycleState;
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

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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

    public List<ValidationErrorItem> getErrorList() {
        return errorList;
    }

    public void addError(String field, String message) {
        errorList.add(new ValidationErrorItem(field, message));
    }

    public void addErrorList(List<ValidationErrorItem> errorList) {
        this.errorList.addAll(errorList);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<CadAttachmentDownloadInfo> getCadAttachments() {
        return cadAttachments;
    }

    public void addCadAttachment(String filename, String type,
            String downloadUrl, String hash) {
        cadAttachments.add(new CadAttachmentDownloadInfo(filename, type,
                downloadUrl, hash));
    }
}
