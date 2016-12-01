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
package com.aritu.eloraplm.doctypes;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.core.listener.MimetypeIconUpdater;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;

import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;

/**
 * @author aritu
 *
 */
public class EloraMimetypeIconUpdater extends MimetypeIconUpdater {

    @Override
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {

            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();

            // Don't update icon for immutable documents nor hidden documents
            if (doc.hasFacet(FacetNames.IMMUTABLE)
                    || doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
                return;
            }

            try {
                // ensure the document main icon is not null
                setDefaultIcon(doc);

                // update mimetypes of blobs in the document
                for (Property prop : blobExtractor.getBlobsProperties(doc)) {
                    if (prop.isDirty()) {
                        updateBlobProperty(doc, getMimetypeRegistry(), prop);
                    }
                }

                // update the document icon and size according to the main
                // blob
                if (doc.hasSchema(MAIN_BLOB_SCHEMA)
                        && doc.getProperty(MAIN_BLOB_FIELD).isDirty()) {
                    updateIconAndSizeFields(
                            doc,
                            getMimetypeRegistry(),
                            doc.getProperty(MAIN_BLOB_FIELD).getValue(
                                    Blob.class));
                }

            } catch (PropertyException e) {
                e.addInfo("Error in MimetypeIconUpdater listener");
                throw e;
            }
        }
    }

    /**
     * @param doc
     * @param mimetypeService
     * @param blob
     * @throws PropertyException
     * @throws ClientException
     *
     *             Enable icon update depending on content only for File
     *             doctype.
     */
    private void updateIconAndSizeFields(DocumentModel doc,
            MimetypeRegistry mimetypeService, Blob blob)
            throws PropertyException {
        // update the icon field of the document
        if (blob != null && doc.getType().equals(NuxeoDoctypeConstants.FILE)) {
            MimetypeEntry mimetypeEntry = mimetypeService.getMimetypeEntryByMimeType(blob.getMimeType());
            updateIconField(mimetypeEntry, doc);
        } else {
            // reset to document type icon
            updateIconField(null, doc);
        }
    }
}
