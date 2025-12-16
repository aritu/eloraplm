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
package com.aritu.eloraplm.core.converter;

import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraMimeTypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Blob conversion class
 *
 * @author aritu
 *
 */
public class BlobConverter {

    public BlobConverter() {
        super();
    }

    public Blob convertBlobToWord(Blob blob) throws EloraException {
        Blob result = null;

        BlobHolder bh = new SimpleBlobHolder(blob);
        ConversionService cs = Framework.getService(ConversionService.class);
        if (cs.getConverterName(blob.getMimeType(),
                EloraMimeTypeConstants.APPLICATION_OPENXML_WORD) == null) {
            throw new EloraException(
                    "No converter available for the source file to create a Word. Source file mime type = |"
                            + blob.getMimeType() + "|");
        }
        BlobHolder resultBh = cs.convertToMimeType(
                EloraMimeTypeConstants.APPLICATION_OPENXML_WORD, bh,
                new HashMap<String, Serializable>());

        if (resultBh != null) {
            result = resultBh.getBlob();
        }

        return result;
    }

}
