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
package com.aritu.eloraplm.integration.importer.restoperations.util;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author aritu
 *
 */

@JsonPropertyOrder({ "docUid", "destinationUid", "error" })
public class CreateProxiesResponseError {

    private String docUid;

    private String destinationUid;

    private String error;

    public CreateProxiesResponseError(String docUid, String destinationUid,
            String error) {
        this.docUid = docUid;
        this.destinationUid = destinationUid;
        this.error = error;
    }

    public String getDocUid() {
        return docUid;
    }

    public void setDocUid(String docUid) {
        this.docUid = docUid;
    }

    public String getDestinationUid() {
        return destinationUid;
    }

    public void setDestinationUid(String destinationUid) {
        this.destinationUid = destinationUid;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
