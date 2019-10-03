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

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.ANY_TO_THUMBNAIL_CONVERTER_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.thumbnail.factories.ThumbnailDocumentFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author aritu
 *
 */
public class ThumbnailEloraViewerFactory extends ThumbnailDocumentFactory
        implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(ThumbnailEloraViewerFactory.class);

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        ConversionService conversionService;
        Blob thumbnailBlob = null;
        try {
            conversionService = Framework.getService(ConversionService.class);
            // Instead of getting a normal BlobHolder with file:content, we get
            // one with elovwr metadata
            BlobHolder bh = new DocumentBlobHolder(doc, "elovwr:file",
                    "elovwr:filename");

            if (bh != null) {
                Map<String, Serializable> params = new HashMap<String, Serializable>();
                // Thumbnail converter
                params.put(ThumbnailConstants.THUMBNAIL_SIZE_PARAMETER_NAME,
                        ThumbnailConstants.THUMBNAIL_DEFAULT_SIZE);
                bh = conversionService.convert(ANY_TO_THUMBNAIL_CONVERTER_NAME,
                        bh, params);
                if (bh != null) {
                    thumbnailBlob = bh.getBlob();
                }
            }
        } catch (NuxeoException e) {
            log.warn("Cannot compute document thumbnail", e);
        }
        return thumbnailBlob;
    }

}
