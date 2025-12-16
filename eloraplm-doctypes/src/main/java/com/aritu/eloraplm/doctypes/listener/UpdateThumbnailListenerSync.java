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
package com.aritu.eloraplm.doctypes.listener;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

public class UpdateThumbnailListenerSync implements EventListener {

    private static Log log = LogFactory.getLog(
            UpdateThumbnailListenerSync.class);

    private static final String EVENT_UPDATE_THUMB_SYNC = "scheduleThumbnailUpdateSync";

    public static final String THUMBNAIL_UPDATED = "thumbnailUpdated";

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                String logInitMsg = "[handleEvent] ["
                        + event.getContext().getPrincipal().getName() + "] ";
                log.trace(logInitMsg + "--- ENTER --- ");

                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                if (doc instanceof DeletedDocumentModel) {
                    return;
                }
                if (doc.isProxy()) {
                    return;
                }

                processDoc(docEventContext.getCoreSession(), doc);
            }
        }
    }

    private void processDoc(CoreSession session, DocumentModel doc) {
        Blob thumbnailBlob = getManagedThumbnail(doc);
        if (thumbnailBlob == null) {
            ThumbnailAdapter thumbnailAdapter = doc.getAdapter(
                    ThumbnailAdapter.class);
            if (thumbnailAdapter == null) {
                return;
            }
            thumbnailBlob = thumbnailAdapter.computeThumbnail(session);
        }
        if (thumbnailBlob != null) {
            if (!doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
            }
            doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                    (Serializable) thumbnailBlob);
        } else {
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                        null);
                doc.removeFacet(ThumbnailConstants.THUMBNAIL_FACET);
            }
        }
        if (doc.isDirty()) {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.NONE);
            doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT,
                    Boolean.TRUE);
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                    Boolean.TRUE);
            doc.putContextData(
                    NotificationConstants.DISABLE_NOTIFICATION_SERVICE,
                    Boolean.TRUE);
            doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER,
                    Boolean.TRUE);
            if (doc.isVersion()) {
                doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            }
            doc.putContextData(THUMBNAIL_UPDATED, true);
            session.saveDocument(doc);
        }
    }

    private Blob getManagedThumbnail(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            return null;
        }
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            InputStream is = blobManager.getThumbnail(blob);
            if (is == null) {
                return null;
            }
            return Blobs.createBlob(is);
        } catch (IOException e) {
            throw new NuxeoException("Failed to get managed blob thumbnail", e);
        }
    }

    private boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getHandledEventsName() {
        return Arrays.asList(EVENT_UPDATE_THUMB_SYNC);
    }
}
