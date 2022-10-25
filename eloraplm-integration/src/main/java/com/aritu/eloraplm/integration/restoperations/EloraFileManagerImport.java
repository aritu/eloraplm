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

import java.io.IOException;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;

import com.aritu.eloraplm.core.util.EloraStructureHelper;

/**
 * Modifications to FileManager.Import to create proxies in workspaces instead
 * of real documents.
 *
 * @author aritu
 *
 */
@Operation(id = EloraFileManagerImport.ID, category = Constants.CAT_SERVICES, label = "Create Document from file (Proxy-Safe)", description = "Create Document(s) from Blob(s) using the FileManagerService. If in Workspace, create proxies and place real doc in structure.")
public class EloraFileManagerImport {

    public static final String ID = "FileManager.Import";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    @Param(name = "overwite", required = false)
    protected Boolean overwite = false;

    protected DocumentModel getCurrentDocument() throws OperationException {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws OperationException, IOException {
        DocumentModel parentDoc = getCurrentDocument();

        DocumentModel newDoc = fileManager.createDocumentFromBlob(session, blob,
                parentDoc.getPathAsString(), overwite, blob.getFilename());

        EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                newDoc, parentDoc);

        return newDoc;
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs)
            throws OperationException, IOException {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}
