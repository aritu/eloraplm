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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * This class encapsulates the General Elora response for a REST operation.
 *
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage" })
public class EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(
            EloraGeneralResponse.class);

    String result;

    String errorMessage;

    /**
     * Empty constructor
     */
    public EloraGeneralResponse() {
        super();
        result = "";
        errorMessage = "";
    }

    /**
     * @param result
     * @param errorMessage
     */
    public EloraGeneralResponse(String result, String errorMessage) {
        super();
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Converts EloraGeneralResponse to a JSON formatted String.
     *
     * @return
     * @throws EloraException
     */
    public String convertToJson() throws EloraException {
        String methodName = "[convertToJson] ";
        log.trace(methodName + "--- ENTER ---");

        String json = EloraJsonHelper.convertToJson(this);

        log.trace(methodName + "--- EXIT ---");

        return json;
    }
}
