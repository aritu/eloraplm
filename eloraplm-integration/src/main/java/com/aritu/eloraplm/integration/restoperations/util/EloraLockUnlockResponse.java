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
package com.aritu.eloraplm.integration.restoperations.util;

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
@JsonPropertyOrder({ "result", "errorMessage", "documents" })
public class EloraLockUnlockResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(EloraLockUnlockResponse.class);

    List<EloraLockUnlockResponseDoc> documents;

    /**
     * Empty constructor. Initializes documents list.
     */
    public EloraLockUnlockResponse() {
        super();
        documents = new ArrayList<EloraLockUnlockResponseDoc>();
    }

    public List<EloraLockUnlockResponseDoc> getDocuments() {
        return documents;
    }

    public void addDocument(EloraLockUnlockResponseDoc document) {
        documents.add(document);
    }

    public void emptyDocuments() {
        documents.clear();
    }

    /**
     * Converts EloraLockResponse to a JSON formatted String.
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