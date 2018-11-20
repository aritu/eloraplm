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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "responseDocs",
        "responseFolders" })
public class DoCheckinResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(DoCheckinResponse.class);

    Map<Integer, DoCheckinResponseDoc> documents;

    List<DoCheckinResponseFolder> folders;

    public DoCheckinResponse() {
        super();
        documents = new HashMap<Integer, DoCheckinResponseDoc>();
        folders = new ArrayList<DoCheckinResponseFolder>();
    }

    public List<DoCheckinResponseDoc> getDocuments() {
        List<DoCheckinResponseDoc> documentsList = new ArrayList<DoCheckinResponseDoc>();
        documentsList.addAll(documents.values());
        return documentsList;
    }

    public DoCheckinResponseDoc getDocument(int localId) {
        return documents.get(localId);
    }

    public void addDocument(int localId, DoCheckinResponseDoc responseDoc) {
        documents.put(localId, responseDoc);
    }

    public List<DoCheckinResponseFolder> getFolders() {
        return folders;
    }

    public void addFolder(DoCheckinResponseFolder responseFolder) {
        folders.add(responseFolder);
    }

    public void emptyDocuments() {
        documents.clear();
    }

    public void emptyFolders() {
        folders.clear();
    }

    /**
     * Converts DoCheckinResponse to a JSON formatted String.
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
