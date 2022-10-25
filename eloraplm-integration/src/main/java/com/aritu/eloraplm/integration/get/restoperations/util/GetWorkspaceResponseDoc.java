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
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * @author aritu
 *
 */

public class GetWorkspaceResponseDoc {

    @JsonIgnore
    private String source;

    private String currentVersionLabel;

    private String downloadUrl;

    private String filename;

    private String hash;

    private List<String> parentRealUids;

    private String parentRealUid;

    private String realUid;

    private boolean saveInWorkspace;

    private String type;

    private String wcUid;

    private List<CadAttachmentDownloadInfo> cadAttachments;

    public GetWorkspaceResponseDoc() {
        parentRealUids = new ArrayList<String>();
        cadAttachments = new ArrayList<CadAttachmentDownloadInfo>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCurrentVersionLabel() {
        return currentVersionLabel;
    }

    public void setCurrentVersionLabel(String currentVersionLabel) {
        this.currentVersionLabel = currentVersionLabel;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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

    public String getParentRealUid() {
        return parentRealUid;
    }

    public void setParentRealUid(String parentRealUid) {
        this.parentRealUid = parentRealUid;
    }

    public List<String> getParentRealUids() {
        return parentRealUids;
    }

    public void addParentRealUid(String parentRealUid) {
        parentRealUids.add(parentRealUid);
    }

    public boolean hasParentRealUid(String parentRealUid) {
        return parentRealUids.contains(parentRealUid);
    }

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public boolean getSaveInWorkspace() {
        return saveInWorkspace;
    }

    public void setSaveInWorkspace(boolean saveInWorkspace) {
        this.saveInWorkspace = saveInWorkspace;
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

    public List<CadAttachmentDownloadInfo> getCadAttachments() {
        return cadAttachments;
    }

    public void addCadAttachment(String filename, String type,
            String downloadUrl, String hash) {
        cadAttachments.add(new CadAttachmentDownloadInfo(filename, type,
                downloadUrl, hash));
    }

}
