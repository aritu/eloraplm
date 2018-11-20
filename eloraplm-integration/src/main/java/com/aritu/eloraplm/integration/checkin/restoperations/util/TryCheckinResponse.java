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
import java.util.List;

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
@JsonPropertyOrder({ "result", "errorMessage", "documents", "folders" })
public class TryCheckinResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(TryCheckinResponse.class);

    List<TryCheckinResponseDoc> documents;

    List<TryCheckinResponseFolder> folders;

    /**
     * Empty constructor. Initializes documents and folders lists.
     */
    public TryCheckinResponse() {
        super();
        documents = new ArrayList<TryCheckinResponseDoc>();
        folders = new ArrayList<TryCheckinResponseFolder>();
    }

    public List<TryCheckinResponseDoc> getDocuments() {
        return documents;
    }

    public void addDocument(TryCheckinResponseDoc document) {
        documents.add(document);
    }

    public List<TryCheckinResponseFolder> getFolders() {
        return folders;
    }

    public void addFolder(TryCheckinResponseFolder folder) {
        folders.add(folder);
    }

    public void emptyOverrideMetadata() {
        for (TryCheckinResponseDoc doc : documents) {
            doc.emptyOverrideMetadata();
        }
    }

    public void emptyDocuments() {
        documents.clear();
    }

    public void emptyFolders() {
        folders.clear();
    }

    /**
     * Converts TryCheckinResponse to a JSON formatted String.
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
