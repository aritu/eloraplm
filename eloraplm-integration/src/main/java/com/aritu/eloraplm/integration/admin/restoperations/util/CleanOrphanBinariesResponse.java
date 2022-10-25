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

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "gcDuration", "numBinaries",
        "sizeBinaries", "numBinariesGC", "sizeBinariesGC" })
public class CleanOrphanBinariesResponse extends EloraGeneralResponse {

    String gcDuration;

    String numBinaries;

    String sizeBinaries;

    String numBinariesGC;

    String sizeBinariesGC;

    public CleanOrphanBinariesResponse() {
        super();
    }

    public String getGcDuration() {
        return gcDuration;
    }

    public void setGcDuration(String gcDuration) {
        this.gcDuration = gcDuration;
    }

    public String getNumBinaries() {
        return numBinaries;
    }

    public void setNumBinaries(String numBinaries) {
        this.numBinaries = numBinaries;
    }

    public String getSizeBinaries() {
        return sizeBinaries;
    }

    public void setSizeBinaries(String sizeBinaries) {
        this.sizeBinaries = sizeBinaries;
    }

    public String getNumBinariesGC() {
        return numBinariesGC;
    }

    public void setNumBinariesGC(String numBinariesGC) {
        this.numBinariesGC = numBinariesGC;
    }

    public String getSizeBinariesGC() {
        return sizeBinariesGC;
    }

    public void setSizeBinariesGC(String sizeBinariesGC) {
        this.sizeBinariesGC = sizeBinariesGC;
    }

    @Override
    public String convertToJson() throws EloraException {
        return EloraJsonHelper.convertToJson(this);
    }

}
