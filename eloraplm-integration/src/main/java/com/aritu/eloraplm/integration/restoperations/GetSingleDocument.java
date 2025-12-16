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

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import com.aritu.eloraplm.constants.EloraGeneralConstants;

/**
 *
 * @author aritu
 *
 */
@Operation(id = GetSingleDocument.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Get Single Document", description = "Treat input for single doc, array of size 1 and array of size >1.")
public class GetSingleDocument {

    public static final String ID = "Elora.Plm.GetSingleDocument";

    @Context
    private CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModelList docs) {
        if (docs == null || docs.isEmpty()) {
            throw new RuntimeException("No document passed as input.");
        }
        if (docs.isEmpty() || docs.size() > 1) {
            throw new RuntimeException(
                    "More than one document passed as input.");
        }

        return docs.get(0);
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        if (doc == null) {
            throw new RuntimeException("No document passed as input.");
        }
        return doc;
    }

}
