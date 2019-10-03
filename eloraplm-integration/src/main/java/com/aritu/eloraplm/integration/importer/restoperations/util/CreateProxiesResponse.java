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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.aritu.eloraplm.core.util.json.EloraJsonHelper;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "result", "errorMessage", "errorList" })
public class CreateProxiesResponse extends EloraGeneralResponse {

    List<CreateProxiesResponseError> errorList;

    public CreateProxiesResponse() {
        super();
        errorList = new ArrayList<CreateProxiesResponseError>();
    }

    public List<CreateProxiesResponseError> getErrorList() {
        return errorList;
    }

    public void addError(CreateProxiesResponseError error) {
        errorList.add(error);
    }

    public void emptyErrorList() {
        errorList.clear();
    }

    @Override
    public String convertToJson() throws EloraException {
        return EloraJsonHelper.convertToJson(this);
    }

}
