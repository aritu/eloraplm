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

package com.aritu.eloraplm.core.util;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

public class EloraUrlHelper {

    protected static final String NXDOC_URL = "nxdoc";

    protected static final String NXFILE_URL = "nxfile";

    protected static final String NXBIGFILE_URL = "nxbigfile";

    public static final String DOCUMENT_PAGE_SUMMARY = "document_page_summary";

    public static final String DOCUMENT_PAGE_EDITION = "document_page_edition";

    private EloraUrlHelper() {
    }

    /**
     * @param request
     * @param doc
     * @return
     * @throws EloraException
     */
    public static String getDocumentEditionUrl(HttpServletRequest request,
            DocumentModel doc) throws EloraException {
        return getDocumentPageUrl(request, doc, DOCUMENT_PAGE_EDITION);
    }

    /**
     * @param request
     * @param doc
     * @return
     * @throws EloraException
     */
    public static String getDocumentEditionInPrintModeUrl(
            HttpServletRequest request, DocumentModel doc)
            throws EloraException {
        return getDocumentPageUrl(request, doc, DOCUMENT_PAGE_EDITION, true);
    }

    /**
     * @param request
     * @param doc
     * @return
     * @throws EloraException
     */
    public static String getDocumentSummaryUrl(HttpServletRequest request,
            DocumentModel doc) throws EloraException {
        return getDocumentPageUrl(request, doc, DOCUMENT_PAGE_SUMMARY);
    }

    /**
     * @param request
     * @param doc
     * @return
     * @throws EloraException
     */
    public static String getDocumentSummaryInPrintModeUrl(
            HttpServletRequest request, DocumentModel doc)
            throws EloraException {
        return getDocumentPageUrl(request, doc, DOCUMENT_PAGE_SUMMARY, true);
    }

    /**
     * @param request
     * @param doc
     * @param page
     * @return
     * @throws EloraException
     */
    private static String getDocumentPageUrl(HttpServletRequest request,
            DocumentModel doc, String page) throws EloraException {
        return getDocumentPageUrl(request, doc, page, false);
    }

    /**
     * @param request
     * @param doc
     * @param page
     * @param mode
     * @return
     * @throws EloraException
     */
    private static String getDocumentPageUrl(HttpServletRequest request,
            DocumentModel doc, String page, boolean printMode)
            throws EloraException {
        String serverUrl = getServerUrl(request);
        String pageTabUrl;
        switch (page) {
        case DOCUMENT_PAGE_SUMMARY:
            pageTabUrl = "view_documents?tabIds=%3ATAB_VIEW";
            break;
        case DOCUMENT_PAGE_EDITION:
            pageTabUrl = "view_documents?tabIds=%3ATAB_EDIT";
            break;
        default:
            pageTabUrl = "view_documents";
            break;
        }

        if (printMode) {
            if (pageTabUrl.contains("?")) {
                pageTabUrl += "&page=galaxy%2Fprint";
            } else {
                pageTabUrl += "?page=galaxy%2Fprint";
            }
        }

        String docPageUrl = String.join("/", new String[] { serverUrl,
                NXDOC_URL, doc.getRepositoryName(), doc.getId(), pageTabUrl });
        return docPageUrl;
    }

    /**
     * @param request
     * @param doc
     * @param filename
     * @return
     * @throws EloraException
     */
    public static String getDocumentDownloadUrl(HttpServletRequest request,
            DocumentModel doc, String filename) throws EloraException {

        String serverUrl = getServerUrl(request);
        String docDownloadUrl = String.join("/",
                new String[] { serverUrl, NXBIGFILE_URL,
                        doc.getRepositoryName(), doc.getId(),
                        NuxeoMetadataConstants.NX_FILE_CONTENT, filename });
        return docDownloadUrl;
    }

    /**
     * @param request
     * @param doc
     * @return
     * @throws EloraException
     */
    public static String getDocumentViewerFileUrl(HttpServletRequest request,
            DocumentModel doc) throws EloraException {

        String serverUrl = getServerUrl(request);
        String docViewerFileUrl = String.join("/",
                new String[] { serverUrl, NXBIGFILE_URL,
                        doc.getRepositoryName(), doc.getId(),
                        EloraMetadataConstants.ELORA_ELOVWR_FILE });
        return docViewerFileUrl;
    }

    /**
     * @param request
     * @param doc
     * @param i
     * @return
     * @throws EloraException
     */
    public static String getDocumentCadAttachmentFileUrl(
            HttpServletRequest request, DocumentModel doc, int i, String filename)
            throws EloraException {

        String serverUrl = getServerUrl(request);
        String cadAttachmentUrl = String.join("/",
                new String[] { serverUrl, NXBIGFILE_URL,
                        doc.getRepositoryName(), doc.getId(),
                        EloraMetadataConstants.ELORA_CADATTS_ATTACHMENTS,
                        String.valueOf(i), "file", filename });
        return cadAttachmentUrl;
    }

    /**
     * @param request
     * @return
     * @throws EloraException
     */
    private static String getServerUrl(HttpServletRequest request)
            throws EloraException {
        if (request == null) {
            throw new EloraException("Request is null.");
        }

        String serverUrl;
        String nuxeoVirtualHost = request.getHeader("nuxeo-virtual-host");

        // If nuxeoVirtualHost is null, we don't have a reverse proxy, so we
        // calculate the URL as always
        if (nuxeoVirtualHost == null) {
            serverUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort() + "/";
        } else {
            serverUrl = nuxeoVirtualHost;
        }

        serverUrl += "nuxeo";

        return serverUrl;
    }
}
