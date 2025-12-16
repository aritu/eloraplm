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
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelListCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;

/**
 *
 * @author aritu
 *
 */
@Operation(id = EloraGetChildren.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_INTEGRATION, label = "EloraPlmConnector - Get Children", description = "Get the children of a document, with options to filter deleted ones.")
public class EloraGetChildren {

    public static final String ID = "Elora.PlmConnector.GetChildren";

    @Context
    protected CoreSession session;

    @Param(name = "includeDeletedChildren", required = false)
    protected boolean includeDeletedChildren = false;

    @Param(name = "type", required = false)
    protected String type;

    @OperationMethod(collector = DocumentModelListCollector.class)
    public DocumentModelList run(DocumentModel doc) {
        return run(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelListCollector.class)
    public DocumentModelList run(DocumentRef docRef) {
        Filter filter = null;
        Sorter sorter = null;

        if (!includeDeletedChildren) {
            filter = new Filter() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean accept(DocumentModel docModel) {
                    return !docModel.getCurrentLifeCycleState().equals(
                            EloraLifeCycleConstants.NX_DELETED);
                }
            };
        }

        return session.getChildren(docRef, type, filter, sorter);
    }

}
