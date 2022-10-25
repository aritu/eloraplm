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
package com.aritu.eloraplm.workflows.restoperations;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Operation(id = RestoreToVersion.ID, category = EloraGeneralConstants.OPERATIONS_CATEGORY_DEFAULT, label = "EloraPlm - RestoreToVersion", description = "Restore WC document to the given version.")
public class RestoreToVersion {

    public static final String ID = "Elora.Plm.RestoreToVersion";

    @Context
    private CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) throws EloraException {
        DocumentModel doc = session.getDocument(docRef);
        return run(doc);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());

        EloraDocumentHelper.restoreToVersion(wcDoc.getRef(), doc.getRef(), true,
                true, session);

        return doc;
    }

}
