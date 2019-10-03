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

package com.aritu.eloraplm.qm.container.archive.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.jsf.actions.FavoritesActionBean;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.container.archive.util.ContainerArchiveHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("archiveQmProcessAction")
@Scope(ScopeType.EVENT)
public class ArchiveQmProcessActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ArchiveQmProcessActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected TreeActionsBean treeActions;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected FavoritesActionBean favoritesActions;

    public void archiveQmProcess() throws EloraException {
        String logInitMsg = "[archiveQmProcess] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel currentDoc = navigationContext.getCurrentDocument();

            currentDoc = replaceSubjectsWithArchivedVersion(currentDoc);

            DocumentModel destinationFolder = ContainerArchiveHelper.archiveAndUnlock(
                    currentDoc, EloraDoctypeConstants.STRUCTURE_ARCHIVED,
                    EloraDoctypeConstants.FOLDER_ARCHIVED_QUALITY_MANAGEMENT,
                    documentManager);

            removeFromFavorites();

            ContainerArchiveHelper.navigateToArchivedFolder(destinationFolder,
                    navigationContext, treeActions, contentViewActions);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.archive.qm.process"));
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.archive.qm.process"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.archive.qm.process"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private DocumentModel replaceSubjectsWithArchivedVersion(
            DocumentModel doc) {
        String[] subjectList = (String[]) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_QM_SUBJECT);

        List<String> avSubjectList = new ArrayList<String>();
        if (subjectList != null && subjectList.length > 0) {
            for (String subject : subjectList) {
                String avSubject = getArchivedVersionSubject(doc, subject);
                avSubjectList.add(avSubject);
            }

            doc.setPropertyValue(EloraMetadataConstants.ELORA_QM_SUBJECT,
                    avSubjectList.toArray());
            doc = documentManager.saveDocument(doc);
        }

        return doc;
    }

    private String getArchivedVersionSubject(DocumentModel doc,
            String subject) {
        String logInitMsg = "[getArchivedVersionSubject] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentRef subjectRef = new IdRef(subject);
        if (documentManager.exists(subjectRef)) {
            DocumentModel subjectDoc = documentManager.getDocument(subjectRef);
            DocumentModel subjectAv;
            try {
                subjectAv = EloraDocumentHelper.getLatestVersion(subjectDoc);

                if (subjectAv != null) {
                    return subjectAv.getId();
                }
            } catch (EloraException e) {
                log.trace(logInitMsg
                        + "Could not get latest version for subject document. Will not be replaced.");
            }
        }

        return subject;
    }

    public void activateQmProcess() {
        String logInitMsg = "[activateQmProcess] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            DocumentModel destinationFolder = ContainerArchiveHelper.moveToWSRoot(
                    currentDoc, documentManager);
            ContainerArchiveHelper.navigateToArchivedFolder(destinationFolder,
                    navigationContext, treeActions, contentViewActions);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.activate.qm.process"));
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.activate.qm.process"));
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void removeFromFavorites() {
        if (favoritesActions.canCurrentDocumentBeRemovedFromFavorites()) {
            favoritesActions.removeCurrentDocumentFromFavorites();
        }
    }

}
