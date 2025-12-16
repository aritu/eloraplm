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
package com.aritu.eloraplm.webapp.preview.adapter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.adapter.AbstractPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

import com.aritu.eloraplm.constants.EloraMimeTypeConstants;

/**
 *
 * @author aritu
 *
 */
public class EloraImagePreviewer extends AbstractPreviewer
        implements MimeTypePreviewer {

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();
        StringBuffer htmlPage = new StringBuffer();
        htmlPage.append("<html><head>");
        htmlPage.append("<style>" + "body {" + "  display: flex;"
                + " justify-content: center;" + " align-items: center;"
                + " height: 100%;" + " padding: 0;" + " margin: 0;" + "}"
                + " img.preview {" + " height: fit-content;" + "}"
                + "</style>");
        htmlPage.append("<title>");
        htmlPage.append(getPreviewTitle(dm));
        htmlPage.append("</title>");
        htmlPage.append("</head><body>");
        appendPreviewSettings(htmlPage);
        htmlPage.append("<img class=\"preview\" src=\"image\">");
        Blob mainBlob = Blobs.createBlob(htmlPage.toString(),
                EloraMimeTypeConstants.TEXT_HTML, null, "index.html");
        blob.setFilename("image");
        blobResults.add(mainBlob);
        blobResults.add(blob);
        return blobResults;
    }

    private static void appendPreviewSettings(StringBuffer sb) {
        sb.append("<script type=\"text/javascript\">");
        sb.append("var previewSettings = { ");
        sb.append("imageOnly: true");
        sb.append("}");
        sb.append("</script>");
    }

}
