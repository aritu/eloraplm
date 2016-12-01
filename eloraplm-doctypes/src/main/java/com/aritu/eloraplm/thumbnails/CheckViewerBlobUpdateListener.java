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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.core.listener.MimetypeIconUpdater;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;
import com.aritu.eloraplm.constants.EloraSchemaConstants;

/**
 * Thumbnail listener handling document blob update and checking changes. Fire
 * an event if it's the case
 *
 * @since 5.7
 */
public class CheckViewerBlobUpdateListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ec = event.getContext();
        if (!(ec instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) ec;
        DocumentModel doc = context.getSourceDocument();
        if (!doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)
                || !doc.hasSchema(MimetypeIconUpdater.MAIN_BLOB_SCHEMA)) {
            return;
        }

        // This listener complements CheckBlobUpdateListener, it is only
        // activated if the viewer file is
        // modified and the content file isn't.
        if (!doc.getProperty("file:content").isDirty()) {
            Property viewerFile = doc.getProperty("elovwr:file");
            if (viewerFile.isDirty()) {

                if (BEFORE_DOC_UPDATE.equals(event.getName())
                        && doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                    doc.setPropertyValue(
                            ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, null);
                }

                if (viewerFile.getValue() != null) {
                    doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                    Framework.getLocalService(EventService.class).fireEvent(
                            ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name(),
                            context);
                }
            }
        }
    }

}
