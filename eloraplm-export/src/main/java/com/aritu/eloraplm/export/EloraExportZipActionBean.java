
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

package com.aritu.eloraplm.export;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip.ZIP_ENTRY_ENCODING_OPTIONS;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.export.util.BlobStructure;

/**
 * @author aritu
 *
 */
@Name("eloraExportAction")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
public class EloraExportZipActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EloraExportZipActionBean.class);

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    public static final String ZIP_FILENAME = "Composition";

    @In
    private transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    private RelationManager relationManager = Framework.getService(
            RelationManager.class);

    public void exportStructureZip() throws IOException, EloraException {
        DocumentModel doc = getCurrentDocAV();
        String filename = getZipName(doc);
        BlobList blobList = BlobStructure.getStructureBlobs(doc,
                relationManager, documentManager);
        Blob zipBlob = compressFile(blobList, filename);

        // Nuxeo Event
        String comment = doc.getVersionLabel();
        EloraEventHelper.fireEvent(EloraEventNames.ELORA_EXPORT_ZIP_EVENT, doc,
                comment);

        downloadFile(zipBlob, filename);
    }

    private String getZipName(DocumentModel doc) {
        String filename = doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE).toString();
        if (filename == null) {
            filename = ZIP_FILENAME;
        }
        return filename.concat(".zip");
    }

    private DocumentModel getCurrentDocAV() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc.isProxy()) {
            doc = documentManager.getSourceDocument(doc.getRef());
        }
        if (!doc.isVersion()) {
            doc = EloraDocumentHelper.getBaseVersion(doc);
        }
        return doc;
    }

    private Blob compressFile(BlobList blobs, String filename)
            throws IOException {
        // String fileName = blobs.isEmpty() ? null :
        // blobs.get(0).getFilename();

        File file = File.createTempFile("nxops-createzip-", ".tmp");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        Framework.trackFile(file, file);
        try {
            zip(blobs, out);
        } finally {
            out.finish();
            out.close();
        }
        return Blobs.createBlob(file, "application/zip", null, filename);
    }

    private void zip(BlobList blobs, ZipOutputStream out) throws IOException {
        // use a set to avoid zipping entries with same names
        Collection<String> names = new HashSet<String>();
        int cnt = 1;
        for (Blob blob : blobs) {
            String entry = getFileName(blob);
            if (!names.add(entry)) {
                entry = "renamed_" + (cnt++) + "_" + entry;
            }
            InputStream in = blob.getStream();
            try {
                ZipUtils._zip(entry, in, out);
            } finally {
                in.close();
            }
        }
    }

    private String getFileName(Blob blob) {
        String entry = blob.getFilename();
        if (entry == null) {
            entry = "Unknown_" + System.identityHashCode(blob);
        }
        return escapeEntryPath(entry);
    }

    private String escapeEntryPath(String path) {
        String zipEntryEncoding = Framework.getProperty(
                ZIP_ENTRY_ENCODING_PROPERTY);
        if (zipEntryEncoding != null && zipEntryEncoding.equals(
                ZIP_ENTRY_ENCODING_OPTIONS.ascii.toString())) {
            return StringUtils.toAscii(path, true);
        }
        return path;
    }

    private void downloadFile(Blob blob, String filename)
            throws EloraException {
        if (blob == null) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.export.fileDoesNotExist"));
            throw new EloraException(
                    "Cannot download file. Compressed file is null!");
        }
        // if (blob.getLength() > Functions.getBigFileSizeLimit()) {
        //
        // ExternalContext externalContext =
        // FacesContext.getCurrentInstance().getExternalContext();
        // HttpServletRequest request = (HttpServletRequest)
        // externalContext.getRequest();
        // HttpServletResponse response = (HttpServletResponse)
        // externalContext.getResponse();
        //
        // String sid = UUID.randomUUID().toString();
        // request.getSession(true).setAttribute(sid, blob);
        //
        // String bigDownloadURL = BaseURL.getBaseURL(request);
        // bigDownloadURL += DownloadService.NXBIGBLOB + "/" + sid;
        //
        // try {
        // // Operation was probably triggered by a POST
        // // so we need to de-activate the ResponseWrapper that would
        // // rewrite the URL
        // request.setAttribute(
        // NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
        // new Boolean(true));
        // // send the redirect
        // response.sendRedirect(bigDownloadURL);
        // // mark all JSF processing as completed
        // response.flushBuffer();
        // FacesContext.getCurrentInstance().responseComplete();
        // // set Outcome to null (just in case)
        // // ctx.getVars().put("Outcome", null);
        // } catch (IOException e) {
        // log.error("Error while redirecting for big blob downloader", e);
        // }
        // } else {
        ComponentUtils.download(null, null, blob, filename, "operation");
        // }
    }

}