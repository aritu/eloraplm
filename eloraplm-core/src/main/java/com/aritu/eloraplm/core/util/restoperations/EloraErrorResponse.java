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

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * This class encapsulates an Elora Error response for a REST operation.
 *
 * @author aritu
 *
 */
@JsonPropertyOrder({ "localId", "errorMessage" })
public class EloraErrorResponse {

    String localId;

    String errorMessage;

    /**
     * @param localId
     * @param errorMessage
     */
    public EloraErrorResponse(String localId, String errorMessage) {
        super();
        this.localId = localId;
        this.errorMessage = errorMessage;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
