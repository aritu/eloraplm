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
package com.aritu.eloraplm.webapp.preview.util;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 *
 * @author aritu
 *
 */
public class EloraPreviewHelper {

    public static String buildPreviewUrlWithViewer(DocumentModel doc,
            String xpath, Blob blob) {
        return PreviewHelper.getViewerURL(doc, xpath, blob, getBaseUrl());
    }

    public static String buildPreviewUrlWithoutViewer(DocumentModel doc,
            String xpath) {
        String fileUrl = PreviewHelper.getPreviewURL(doc, xpath);
        return getBaseUrl() + "/" + fileUrl;
    }

    public static String getBaseUrl() {
        ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String baseUrl = BaseURL.getBaseURL(servletRequest);
        baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return baseUrl;
    }

}
