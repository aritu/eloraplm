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
package com.aritu.eloraplm.integration.restoperations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.restoperations.EloraGeneralResponse;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Operation to check session status
 *
 * @author aritu
 *
 */
@Operation(id = CheckSessionStatus.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - CheckSessionStatus", description = "Check if session is active.")
public class CheckSessionStatus {

    public static final String ID = "Elora.PlmConnector.CheckSessionStatus";

    @Context
    protected CoreSession session;

    protected EloraGeneralResponse response;

    @OperationMethod
    public String run() throws EloraException {

        response = new EloraGeneralResponse();
        response.setResult(EloraGeneralConstants.RESPONSE_STATUS_SUCCESS);

        return response.convertToJson();
    }
}
