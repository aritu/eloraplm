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
package com.aritu.eloraplm.integration.admin.restoperations.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "errorList", "processedDocs" })
public class MakeItemsObsoleteResponse extends EloraGeneralResponse {

    Map<String, String> errorList;

    Map<String, List<String>> processedDocs;

    public MakeItemsObsoleteResponse() {
        super();
        processedDocs = new HashMap<String, List<String>>();
        errorList = new HashMap<String, String>();
    }

    public void addError(String reference, String message) {
        errorList.put(reference, message);
    }

    public Map<String, String> getErrorList() {
        return errorList;
    }

    public void emptyErrorList() {
        errorList.clear();
    }

    public int getErrorListSize() {
        return errorList.size();
    }

    // public void addProcessedDoc(String reference, String uid) {
    // if (processedDocs.containsKey(reference)) {
    // processedDocs.get(reference).add(uid);
    // } else {
    // List<String> uidList = new ArrayList<String>();
    // uidList.add(uid);
    // processedDocs.put(reference, uidList);
    // }
    // }

    public void addProcessedDocs(String reference, List<String> uids) {
        if (processedDocs.containsKey(reference)) {
            processedDocs.get(reference).addAll(uids);
        } else {
            processedDocs.put(reference, uids);
        }
    }

    public Map<String, List<String>> getProcessedDocs() {
        return processedDocs;
    }

    public boolean isProcessedDoc(String reference, String uid) {
        if (processedDocs.containsKey(reference)) {
            return processedDocs.get(reference).contains(uid);
        }
        return false;
    }

    public void removeProcessedReference(String reference) {
        processedDocs.remove(reference);
    }

    @Override
    public String convertToJson() throws EloraException {
        return EloraJsonHelper.convertToJson(this);
    }

}
