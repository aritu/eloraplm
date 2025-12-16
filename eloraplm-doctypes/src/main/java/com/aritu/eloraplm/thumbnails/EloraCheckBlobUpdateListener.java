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

package com.aritu.eloraplm.thumbnails;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static com.aritu.eloraplm.constants.ViewerEventNames.VIEWER_FILE_CREATED_EVENT;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;

/**
 * Thumbnail listener handling document blob update and checking changes. Fire
 * an event if it's the case
 *
 * @since 5.7
 */
public class EloraCheckBlobUpdateListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ec = event.getContext();
        if (!(ec instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) ec;
        DocumentModel doc = context.getSourceDocument();
        if (!doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)
                && !doc.hasSchema(NuxeoSchemaConstants.MAIN_BLOB_SCHEMA)) {
            return;
        }

        Property viewerFile = null;
        if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            viewerFile = doc.getProperty(
                    EloraMetadataConstants.ELORA_ELOVWR_FILE);
        }

        Property content = null;
        if (doc.hasSchema(NuxeoSchemaConstants.MAIN_BLOB_SCHEMA)) {
            content = doc.getProperty(NuxeoMetadataConstants.NX_FILE_CONTENT);
        }

        if (DOCUMENT_CREATED.equals(event.getName())
                || PdmEventNames.PDM_RESTORED_EVENT.equals(event.getName())
                || (content != null && content.isDirty())
                || (viewerFile != null && viewerFile.isDirty())) {

            if ((BEFORE_DOC_UPDATE.equals(event.getName())
                    || VIEWER_FILE_CREATED_EVENT.equals(event.getName())
                    || PdmEventNames.PDM_RESTORED_EVENT.equals(event.getName()))
                    && doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                        null);
            }

            if ((viewerFile != null && viewerFile.getValue() != null)
                    || (content != null && content.getValue() != null)) {
                doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                Framework.getLocalService(EventService.class).fireEvent(
                        ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name(),
                        context);
            }
        }
    }

}
