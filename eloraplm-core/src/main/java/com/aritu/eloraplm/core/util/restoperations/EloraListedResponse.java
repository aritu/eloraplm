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
package com.aritu.eloraplm.core.util.restoperations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * This class encapsulates a listed Elora response for a REST operation. It
 * extends the general Elora Response class and it adds a list with succeeded
 * processes and a list with the processes in error.
 *
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "succeededProcesses",
        "errorProcesses" })
public class EloraListedResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(EloraListedResponse.class);

    // List of SUCCEEDED processes.
    List<EloraSuccessResponse> succeededProcesses;

    // List of processes in ERROR.
    List<EloraErrorResponse> errorProcesses;

    /**
     * Empty constructor. Initializes succeeded and error lists.
     */
    public EloraListedResponse() {
        super();
        succeededProcesses = new ArrayList<EloraSuccessResponse>();
        errorProcesses = new ArrayList<EloraErrorResponse>();
    }

    public List<EloraSuccessResponse> getSucceededProcesses() {
        return succeededProcesses;
    }

    public void addSucceededProcess(EloraSuccessResponse successResponse) {
        succeededProcesses.add(successResponse);
    }

    public List<EloraErrorResponse> getErrorProcesses() {
        return errorProcesses;
    }

    public void addErrorProcess(EloraErrorResponse errorResponse) {
        errorProcesses.add(errorResponse);
    }

    /**
     * Converts EloraListedResponse to a JSON formatted String.
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
