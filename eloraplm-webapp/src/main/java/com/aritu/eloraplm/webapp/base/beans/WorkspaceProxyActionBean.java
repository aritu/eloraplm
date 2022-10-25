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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * @author aritu
 *
 */

@Name("wsProxyActions")
@Scope(EVENT)
public class WorkspaceProxyActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class DocumentVersionInformation {

        private final String id;

        private final String label;

        public DocumentVersionInformation(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    protected List<DocumentVersionInformation> documentVersions;

    protected String targetWorkspaceUid;

    protected String sourceVersionUid;

    public List<DocumentVersionInformation> getDocumentVersions() {
        try {
            if (documentVersions == null || documentVersions.isEmpty()) {
                getDocumentVersionsForDocument();
            }
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.wsProxy.getDocumentVersions"));
        }

        return documentVersions;
    }

    public void setDocumentVersions(
            List<DocumentVersionInformation> documentVersions) {
        this.documentVersions = documentVersions;
    }

    public String getTargetWorkspaceUid() {
        return targetWorkspaceUid;
    }

    public void setTargetWorkspaceUid(String targetWorkspaceUid) {
        this.targetWorkspaceUid = targetWorkspaceUid;
    }

    public String getSourceVersionUid() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc != null && currentDoc.isVersion()) {
            sourceVersionUid = currentDoc.getId();
        }
        return sourceVersionUid;
    }

    public void setSourceVersionUid(String sourceVersionUid) {
        this.sourceVersionUid = sourceVersionUid;
    }

    public void getDocumentVersionsForDocument() throws EloraException {
        documentVersions = new ArrayList<>();
        DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (currentDoc != null) {
            getVersionsList(currentDoc);
        }

    }

    private void getVersionsList(DocumentModel currentDoc)
            throws EloraException {
        DocumentRef docRef = currentDoc.getRef();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(docRef);
        }
        DocumentModel wcDoc = currentDoc;
        if (currentDoc.isVersion()) {
            wcDoc = documentManager.getWorkingCopy(docRef);
        }
        DocumentRef wcDocRef = wcDoc.getRef();

        documentVersions.add(new DocumentVersionInformation(wcDoc.getId(),
                messages.get("eloraplm.label.wcVersion")));

        List<VersionModel> docVersions = documentManager.getVersionsForDocument(
                wcDocRef);
        if (!docVersions.isEmpty()) {

            DocumentModel latestVersion = EloraDocumentHelper.getLatestVersion(
                    wcDoc);
            if (latestVersion == null) {
                throw new EloraException("Document |" + wcDoc.getId()
                        + "| has no latest version or it is unreadable, but it has AVs.");
            }

            String latestVersionLabel = "";
            if (latestVersion != null) {
                latestVersionLabel = latestVersion.getVersionLabel();
            }

            for (VersionModel version : docVersions) {
                String label = versionLabelService.translateVersionLabel(
                        version.getLabel());
                if (label.equals(latestVersionLabel)) {
                    label += " (" + messages.get("eloraplm.label.latestVersion")
                            + ")";
                }
                documentVersions.add(
                        new DocumentVersionInformation(version.getId(), label));
            }
        }
    }

    public void createWorkspaceProxy() {

        DocumentRef targetDocRef = new IdRef(targetWorkspaceUid);

        if (!documentManager.exists(targetDocRef)) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.wsProxy.create.targetDocDoesNotExist"));
            return;
        }

        DocumentModel targetDoc = documentManager.getDocument(targetDocRef);
        if (!targetDoc.isFolder()) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.wsProxy.create.targetDocNotAFolder"));
            return;
        }

        DocumentRef sourceVersionRef = new IdRef(sourceVersionUid);
        documentManager.createProxy(sourceVersionRef, targetDocRef);

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("eloraplm.message.success.wsProxy.create"));
    }
}
