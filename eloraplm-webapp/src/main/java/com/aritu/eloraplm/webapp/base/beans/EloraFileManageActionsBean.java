/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Andreas Kalogeropoulos
 *     Anahide Tchertchian
 *     Thierry Delprat
 *     Florent Guillaume
 */
package com.aritu.eloraplm.webapp.base.beans;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.ConcurrentModificationException;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.webapp.filemanager.FileManageActions;
import org.nuxeo.ecm.webapp.filemanager.FileManageActionsBean;
import org.nuxeo.ecm.webapp.filemanager.NxUploadedFile;
import org.nuxeo.runtime.api.Framework;

@Name("eloraFileManageActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class EloraFileManageActionsBean extends FileManageActionsBean
        implements FileManageActions {

    private static final Log log = LogFactory.getLog(
            EloraFileManageActionsBean.class);

    @Override
    public void validateMultiplesUpload()
            throws FileNotFoundException, IOException {
        DocumentModel current = navigationContext.getCurrentDocument();
        validateMultipleUploadForDocument(current);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void validateMultipleUploadForDocument(DocumentModel current)
            throws FileNotFoundException, IOException {
        if (!current.hasSchema(FILES_SCHEMA)) {
            return;
        }
        Collection<NxUploadedFile> nxuploadFiles = getUploadedFiles();
        try {
            ArrayList files = (ArrayList) current.getPropertyValue(
                    FILES_PROPERTY);
            if (nxuploadFiles != null) {
                for (NxUploadedFile uploadItem : nxuploadFiles) {
                    Blob blob = uploadItem.getBlob();
                    FileUtils.configureFileBlob(blob);
                    HashMap<String, Object> fileMap = new HashMap<String, Object>(
                            2);
                    fileMap.put("file", blob);
                    fileMap.put("filename", blob.getFilename());
                    if (!files.contains(fileMap)) {
                        files.add(fileMap);
                    }
                }
            }
            current.setPropertyValue(FILES_PROPERTY, files);
            documentActions.updateDocument(current, Boolean.TRUE);
        } catch (ConcurrentModificationException e) {
            // We don't know why it happens this exception, but the file is
            // removed, so we are going to hide the exception message.
            log.error(e, e);
        } finally {
            if (nxuploadFiles != null) {
                for (NxUploadedFile uploadItem : nxuploadFiles) {
                    File tempFile = uploadItem.getFile();
                    // Tmp file that have been moved are assumed to not be
                    // temporary anymore
                    if (tempFile != null && tempFile.exists()
                            && tmpFilePaths.contains(tempFile.getPath())) {
                        Framework.trackFile(tempFile, tempFile);
                    }
                }
            }
            tmpFilePaths.clear();
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes" })
    public void performAction(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        String index = eContext.getRequestParameterMap().get("index");

        try {
            DocumentModel current = navigationContext.getCurrentDocument();
            if (!current.hasSchema(FILES_SCHEMA)) {
                return;
            }
            ArrayList files = (ArrayList) current.getPropertyValue(
                    FILES_PROPERTY);
            Object file = CollectionUtils.get(files,
                    Integer.valueOf(index).intValue());
            files.remove(file);
            current.setPropertyValue(FILES_PROPERTY, files);
            documentActions.updateDocument(current, Boolean.TRUE);
        } catch (ConcurrentModificationException e) {
            // We don't know why it happens this exception, but the file is
            // removed, so we are going to hide the exception message.
            log.error(e, e);
        } catch (IndexOutOfBoundsException | NuxeoException e) {
            log.error(e, e);
            throw e;
        }
    }

}
