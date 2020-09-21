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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * TODO: Beste WS motentzako balio izateko aldaketak jarri!!! TODO: Sortu
 * ganorazko zerbitzu bat artxibatzeko
 *
 * @author aritu
 *
 */
@Operation(id = ArchiveWorkspace.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - Archive Workspace", description = "Archive the input workspace.")
public class ArchiveWorkspace {

    public static final String ID = "Elora.Plm.ArchiveWorkspace";

    @Context
    private CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws EloraException {
        List<Object> params = new ArrayList<Object>();
        params.add(doc);

        if (doc.hasFacet(EloraFacetConstants.FACET_QM_PROCESS)) {

            SeamComponentCallHelper.callSeamComponentByName(
                    "archiveQmProcessAction", "archiveQmProcess",
                    params.toArray());
        }

        return doc;
    }

}
