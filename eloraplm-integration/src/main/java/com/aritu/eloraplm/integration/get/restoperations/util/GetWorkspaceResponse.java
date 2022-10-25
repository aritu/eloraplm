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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.restoperations.util.CmProcessNode;
import com.aritu.eloraplm.integration.util.FolderInfo;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "documents", "folders",
        "cmProcessStructure", "duplicateProxies", "lastModified" })
@JsonIgnoreProperties({ "contentProxyParents" })
public class GetWorkspaceResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(
            GetWorkspaceResponse.class);

    Map<String, GetWorkspaceResponseDoc> documents;

    List<FolderInfo> folders;

    List<CmProcessNode> cmProcessStructure;

    Map<String, ContentProxyParents> contentProxyParents;

    Date lastModified;

    /**
     * Empty constructor. Initializes documents list.
     */
    public GetWorkspaceResponse() {
        super();
        documents = new LinkedHashMap<String, GetWorkspaceResponseDoc>();
        folders = new ArrayList<FolderInfo>();
        contentProxyParents = new LinkedHashMap<String, ContentProxyParents>();
    }

    public List<GetWorkspaceResponseDoc> getDocuments() {
        List<GetWorkspaceResponseDoc> documentsList = new ArrayList<GetWorkspaceResponseDoc>();
        for (GetWorkspaceResponseDoc document : documents.values()) {
            documentsList.add(document);
        }
        return documentsList;
    }

    public List<FolderInfo> getFolders() {
        return folders;
    }

    public GetWorkspaceResponseDoc getDocument(String versionSeriesId) {
        return documents.get(versionSeriesId);
    }

    public boolean hasDocument(String versionSeriesId) {
        return documents.containsKey(versionSeriesId);
    }

    public void addDocument(String versionSeriesId,
            GetWorkspaceResponseDoc document) {
        documents.put(versionSeriesId, document);
    }

    public void addFolder(FolderInfo folder) {
        folders.add(folder);
    }

    public void emptyDocuments() {
        documents.clear();
    }

    public void emptyFolders() {
        folders.clear();
    }

    public List<CmProcessNode> getCmProcessStructure() {
        return cmProcessStructure;
    }

    public void setCmProcessStructure(List<CmProcessNode> cmProcessStructure) {
        this.cmProcessStructure = cmProcessStructure;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public List<ContentProxyParents> getDuplicateProxies() {
        List<ContentProxyParents> dpList = new ArrayList<ContentProxyParents>();
        for (ContentProxyParents cpp : contentProxyParents.values()) {
            if (cpp.hasMultipleParents()) {
                dpList.add(cpp);
            }
        }
        return dpList;
    }

    public ContentProxyParents getContentProxyParents(String versionSeriesId) {
        return contentProxyParents.get(versionSeriesId);
    }

    public boolean hasContentProxyParents(String versionSeriesId) {
        return contentProxyParents.containsKey(versionSeriesId);
    }

    public void addContentProxyParent(String wcUid, String parentRealUid) {
        ContentProxyParents cpp;
        if (hasContentProxyParents(wcUid)) {
            cpp = getContentProxyParents(wcUid);
        } else {
            cpp = new ContentProxyParents(wcUid);
            contentProxyParents.put(wcUid, cpp);
        }
        cpp.addParentRealUid(parentRealUid);
    }

    @Override
    public String convertToJson() throws EloraException {
        String methodName = "[convertToJson] ";
        log.trace(methodName + "--- ENTER ---");

        String json = EloraJsonHelper.convertToJson(this);

        log.trace(methodName + "--- EXIT ---");

        return json;
    }

}
