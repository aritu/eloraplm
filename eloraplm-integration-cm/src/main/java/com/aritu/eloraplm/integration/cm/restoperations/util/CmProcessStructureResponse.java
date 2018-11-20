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
package com.aritu.eloraplm.integration.cm.restoperations.util;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.restoperations.util.CmProcessNode;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "cmProcessStructure",
        "lastModified" })
public class CmProcessStructureResponse extends EloraGeneralResponse {

    private static final Log log = LogFactory.getLog(
            CmProcessStructureResponse.class);

    List<CmProcessNode> cmProcessStructure;

    Date lastModified;

    /**
     * Empty constructor. Initializes documents list.
     */
    public CmProcessStructureResponse() {
        super();
    }

    public List<CmProcessNode> getCmProcessStructure() {
        return cmProcessStructure;
    }

    public void setCmProcessStructure(List<CmProcessNode> cmProcessStructure) {
        this.cmProcessStructure = cmProcessStructure;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Converts GetFileStructInfoResponse to a JSON formatted String.
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
