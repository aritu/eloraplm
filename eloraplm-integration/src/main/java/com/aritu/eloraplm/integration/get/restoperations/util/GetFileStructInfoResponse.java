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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.util.FolderInfo;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "documents" })
public class GetFileStructInfoResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(
            GetFileStructInfoResponse.class);

    // The first map's key is the document real uid. The value is the inner map.
    // The inner map's key is the cad parent uid. The value is a
    // GetFileStructInfoResponseDoc, with all the metadata.
    // For each unique document in the tree, there are multiple rows (one for
    // each parent), that besides the cadParentRealUid, have the same data.
    Map<String, Map<String, GetFileStructInfoResponseDoc>> documents;

    List<FolderInfo> folders;

    /**
     * Empty constructor. Initializes documents list.
     */
    public GetFileStructInfoResponse() {
        super();
        documents = new LinkedHashMap<String, Map<String, GetFileStructInfoResponseDoc>>();
        folders = new ArrayList<FolderInfo>();
    }

    /* Documents methods */

    public List<GetFileStructInfoResponseDoc> getDocuments() {
        List<GetFileStructInfoResponseDoc> documentsList = new ArrayList<GetFileStructInfoResponseDoc>();
        for (Map<String, GetFileStructInfoResponseDoc> documentEntries : documents.values()) {
            documentsList.addAll(documentEntries.values());
        }
        return documentsList;
    }

    public Map<String, GetFileStructInfoResponseDoc> getDocumentEntries(
            String docRealUid) {
        return documents.get(docRealUid);
    }

    public boolean hasDocument(String docRealUid) {
        return documents.containsKey(docRealUid);
    }

    public void addDocument(String docRealUid, String cadParentRealUid,
            GetFileStructInfoResponseDoc document) {
        Map<String, GetFileStructInfoResponseDoc> docEntries;
        if (documents.containsKey(docRealUid)) {
            docEntries = documents.get(docRealUid);
        } else {
            docEntries = new LinkedHashMap<String, GetFileStructInfoResponseDoc>();
        }

        if (cadParentRealUid == null) {
            cadParentRealUid = "-";
        }
        docEntries.put(cadParentRealUid, document);
        documents.put(docRealUid, docEntries);
    }

    public void addDocumentEntries(String docRealUid,
            Map<String, GetFileStructInfoResponseDoc> documentEntries) {
        documents.put(docRealUid, documentEntries);
    }

    public void emptyDocuments() {
        documents.clear();
    }

    /* Folders methods */

    public List<FolderInfo> getFolders() {
        return folders;
    }

    public void addFolder(FolderInfo folder) {
        folders.add(folder);
    }

    public void emptyFolders() {
        folders.clear();
    }

    /**
     * Converts GetFileStructInfoResponse to a JSON formatted String.
     */
    @Override
    public String convertToJson() throws EloraException {
        String methodName = "[convertToJson] ";
        log.trace(methodName + "--- ENTER ---");

        String json = EloraJsonHelper.convertToJson(this);

        log.trace(methodName + "--- EXIT ---");

        return json;
    }

}
