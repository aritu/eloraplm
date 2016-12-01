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
 * This class encapsulates an Elora Success response for a REST operation.
 *
 * @author aritu
 *
 */
@JsonPropertyOrder({ "localId", "uid" })
public class EloraSuccessResponse {

    String localId;

    String uid;

    /**
     * @param localId
     * @param uid
     */
    public EloraSuccessResponse(String localId, String uid) {
        super();
        this.localId = localId;
        this.uid = uid;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

}
