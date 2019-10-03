/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package com.aritu.eloraplm.thumbnails;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Preview adapter factory for all documents that have a blob holder adapter.
 *
 * @author ldoguin
 */
public class EloraViewerPreviewAdapterFactory implements PreviewAdapterFactory {

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        ConverterBasedHtmlPreviewAdapter adapter = new ConverterBasedHtmlPreviewAdapter();
        adapter.setDefaultPreviewFieldXPath("elovwr:file");
        adapter.setAdaptedDocument(doc);
        return adapter;
    }

}
