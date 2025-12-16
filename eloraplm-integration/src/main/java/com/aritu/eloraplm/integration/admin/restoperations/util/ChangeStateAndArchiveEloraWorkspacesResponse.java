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
import java.util.Map;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "errorListSize", "errorList",
        "skippedListSize", "skippedList" })
public class ChangeStateAndArchiveEloraWorkspacesResponse
        extends EloraGeneralResponse {

    // reference, message
    Map<String, String> errorList;

    // reference, message
    Map<String, String> skippedList;

    public ChangeStateAndArchiveEloraWorkspacesResponse() {
        super();
        errorList = new HashMap<String, String>();

        skippedList = new HashMap<String, String>();
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

    public void addSkippedReference(String reference, String message) {
        skippedList.put(reference, message);
    }

    public Map<String, String> getSkippedList() {
        return skippedList;
    }

    public void emptySkippedList() {
        skippedList.clear();
    }

    public int getSkippedListSize() {
        return skippedList.size();
    }

    @Override
    public String convertToJson() throws EloraException {
        return EloraJsonHelper.convertToJson(this);
    }

}
