/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Razvan Caraghin
 *     Florent Guillaume
 *     Thierry Martins
 *     Antoine Taillefer
 */

package com.aritu.eloraplm.history;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Extra actions for History tab
 *
 * @author Aritu
 */
@Name("eloraPlmVersionHistoryActions")
@Scope(EVENT)
@Install(precedence = FRAMEWORK)
public class EloraPlmVersionHistoryActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient DocumentsListsManager documentsListsManager;

    private List<VersionModel> versions;

    public List<VersionModel> getVersions() {
        if (versions == null || versions.isEmpty()) {
            versions = new ArrayList<VersionModel>();
            List<DocumentModel> versionDocs = documentsListsManager.getWorkingList(
                    DocumentsListsManager.CURRENT_VERSION_SELECTION);
            for (DocumentModel versionDoc : versionDocs) {
                versionDoc.refresh();
                VersionModel version = new VersionModelImpl();
                version.setId(versionDoc.getId());
                version.setLabel(versionDoc.getVersionLabel());
                version.setDescription(versionDoc.getCheckinComment());

                versions.add(version);
            }
        }
        return versions;
    }

    public void setVersions(List<VersionModel> versions) {
        this.versions = versions;
    }

    public DocumentModel getArchivedVersionDocument(String versionId) {
        DocumentModel versionDoc = null;

        DocumentRef docRef = new IdRef(versionId);
        if (docRef != null) {
            versionDoc = documentManager.getDocument(docRef);
        }

        return versionDoc;
    }

    public boolean isCurrent(DocumentModel versionDoc,
            DocumentModel currentDoc) {

        if (currentDoc != null && versionDoc != null
                && !currentDoc.isCheckedOut()) {
            if (currentDoc.isProxy()) {
                currentDoc = documentManager.getSourceDocument(
                        currentDoc.getRef());
            }
            if (!currentDoc.isImmutable()) {
                DocumentRef currentDocRef = documentManager.getBaseVersion(
                        currentDoc.getRef());
                currentDoc = documentManager.getDocument(currentDocRef);
            }

            if (versionDoc.getId().equals(currentDoc.getId())) {
                return true;
            }
        }

        return false;
    }

    public int countSelectedVersions() {
        List<DocumentModel> currentVersionSelection = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_VERSION_SELECTION);
        if (currentVersionSelection != null) {
            return currentVersionSelection.size();
        }
        return 0;
    }

    public void saveCheckinComments() {
        if (versions != null && !versions.isEmpty()) {
            try {
                for (VersionModel version : versions) {

                    DocumentModel versionDoc = documentManager.getDocument(
                            new IdRef(version.getId()));

                    String previousComment = versionDoc.getCheckinComment();

                    EloraDocumentHelper.updateCheckinComment(versionDoc,
                            version.getDescription());

                    // Nuxeo Event
                    String comment = "'" + previousComment + "' => '"
                            + version.getDescription() + "'";
                    EloraEventHelper.fireEvent(
                            EloraEventNames.ELORA_VERSION_COMMENT_CHANGED_EVENT,
                            versionDoc, comment);
                }

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.history.changeCheckinComments"));
            } catch (EloraException e) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.history.changeCheckinComments"));
            }
        }
    }

}
