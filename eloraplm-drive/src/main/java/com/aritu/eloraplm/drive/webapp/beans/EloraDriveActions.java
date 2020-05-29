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
package com.aritu.eloraplm.drive.webapp.beans;

import java.io.Serializable;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.user.center.UserCenterViewManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author aritu
 *
 */
@Name("eloraDriveActions")
@Scope(ScopeType.PAGE)
@Install(precedence = Install.FRAMEWORK)
public class EloraDriveActions extends InputController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EloraDriveActions.class);

    private static final String NXDRIVE_PROTOCOL = "nxdrive";

    private static final String PROTOCOL_COMMAND_EDIT = "edit";

    private static final String NEW_DRIVE_EDIT_URL_PROP_KEY = "org.nuxeo.drive.new.edit.url";

    @SuppressWarnings("hiding")
    @In(create = true, required = false)
    private transient NavigationContext navigationContext;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true, required = false)
    private transient UserCenterViewManager userCenterViews;

    @In(create = true)
    private transient DocumentActions documentActions;

    public boolean canEditDocument(DocumentModel doc) {
        if (doc == null || !documentManager.exists(doc.getRef())) {
            return false;
        }
        if (doc.isFolder()) {
            return false;
        }
        if (doc.isProxy()) {
            doc = documentManager.getSourceDocument(doc.getRef());
        }
        if (!documentManager.hasPermission(doc.getRef(),
                SecurityConstants.WRITE)) {
            return false;
        }
        // Check if current document can be adapted as a FileSystemItem
        return getFileSystemItem(doc) != null;
    }

    private FileSystemItem getFileSystemItem(DocumentModel doc) {
        // Force parentItem to null to avoid computing ancestors
        FileSystemItem fileSystemItem = Framework.getLocalService(
                FileSystemItemAdapterService.class).getFileSystemItem(doc,
                        null);
        if (fileSystemItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s (%s) is not adaptable as a FileSystemItem.",
                        doc.getPathAsString(), doc.getId()));
            }
        }
        return fileSystemItem;
    }

    /**
     * Returns the Drive edit URL for the current document.
     *
     * @see #getDriveEditURL(DocumentModel)
     */
    public String getDriveEditURL() {
        @SuppressWarnings("hiding")
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getDriveEditURL(currentDocument);
    }

    /**
     * Returns the Drive edit URL for the given document.
     * <p>
     * {@link #NXDRIVE_PROTOCOL} must be handled by a protocol handler
     * configured on the client side (either on the browser, or on the OS).
     *
     * @since 7.4
     * @return Drive edit URL in the form "{@link #NXDRIVE_PROTOCOL}://
     *         {@link #PROTOCOL_COMMAND_EDIT}
     *         /protocol/server[:port]/webappName/[user/userName/]repo/repoName/nxdocid/docId/filename/fileName[/
     *         downloadUrl/downloadUrl]"
     */
    public String getDriveEditURL(
            @SuppressWarnings("hiding") DocumentModel currentDocument) {
        if (currentDocument == null) {
            return null;
        }

        // If the document is a proxy, get the real doc
        if (currentDocument.isProxy()) {
            currentDocument = documentManager.getSourceDocument(
                    currentDocument.getRef());
        }

        // TODO NXP-15397: handle Drive not started exception
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new NuxeoException(String.format(
                    "Document %s (%s) is not a BlobHolder, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(),
                    currentDocument.getId()));
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new NuxeoException(String.format(
                    "Document %s (%s) has no blob, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(),
                    currentDocument.getId()));
        }
        String fileName = blob.getFilename();
        ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String baseURL = VirtualHostHelper.getBaseURL(servletRequest);
        StringBuffer sb = new StringBuffer();
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        sb.append(baseURL.replaceFirst("://", "/"));
        if (Boolean.valueOf(
                Framework.getProperty(NEW_DRIVE_EDIT_URL_PROP_KEY))) {
            sb.append("user/");
            sb.append(documentManager.getPrincipal().getName());
            sb.append("/");
        }
        sb.append("repo/");
        sb.append(documentManager.getRepositoryName());
        sb.append("/nxdocid/");
        sb.append(currentDocument.getId());
        sb.append("/filename/");
        String escapedFilename = fileName.replaceAll(
                "(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
        sb.append(URIUtils.quoteURIPathComponent(escapedFilename, true));
        if (Boolean.valueOf(
                Framework.getProperty(NEW_DRIVE_EDIT_URL_PROP_KEY))) {
            sb.append("/downloadUrl/");
            DownloadService downloadService = Framework.getService(
                    DownloadService.class);
            String downloadUrl = downloadService.getDownloadUrl(currentDocument,
                    DownloadService.BLOBHOLDER_0, "");
            sb.append(downloadUrl);
        }
        return sb.toString();
    }

}
