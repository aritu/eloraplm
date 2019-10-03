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

package com.aritu.eloraplm.history.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.versioning.VersionedActionsBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.history.api.VersionRemoveCheckerService;

/**
 * Extra actions for History tab
 *
 * @author Aritu
 */
@Name("eloraVersionedActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class EloraVersionedActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            VersionedActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient VersionedActionsBean versionedActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public void removeVersion() throws EloraException {
        if (versionedActions.getSelectedVersionId() != null) {
            VersionModel selectedVersion = new VersionModelImpl();
            selectedVersion.setId(versionedActions.getSelectedVersionId());
            removeVersion(selectedVersion);
        }
    }

    private void removeVersion(VersionModel selectedVersion)
            throws EloraException {

        String logInitMsg = "[removeVersion] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel doc = navigationContext.getCurrentDocument();
            if (!EloraDocumentHelper.isWorkingCopy(doc)) {
                doc = documentManager.getWorkingCopy(doc.getRef());
            }
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc == null) {
                log.error(logInitMsg + "The document |" + doc.getId()
                        + "| has no base version.");
                throw new EloraException("The document |" + doc.getId()
                        + "| has no base version.");
            }

            VersionRemoveCheckerService versionRemoveCheckerService = Framework.getService(
                    VersionRemoveCheckerService.class);

            if (versionRemoveCheckerService.canRemoveDocument(baseDoc, doc,
                    facesMessages, messages, documentManager)) {
                restoreAndRemoveDocument(selectedVersion, doc);
                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get("eloraplm.message.success.removeVersion"));
                // same as edit basically // XXX AT: do edit events need to be
                // sent?
                // //
                // EventManager.raiseEventsOnDocumentChange(restoredDocument);
                // //
                // navigationContext.navigateToDocument(restoredDocument,
                // "after-edit");
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.removeVersion"));
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.removeVersion"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private DocumentModel restoreAndRemoveDocument(VersionModel selectedVersion,
            DocumentModel doc) throws EloraException {
        VersionModel previousVersion = getWorkingCopysPreviousVersion(doc);
        DocumentModel restoredDocument = EloraDocumentHelper.restoreDocumentToVersion(
                documentManager.getWorkingCopy(doc.getRef()), previousVersion,
                eloraDocumentRelationManager, documentManager);
        versionedActions.removeArchivedVersion(selectedVersion);
        return restoredDocument;
    }

    private VersionModel getWorkingCopysPreviousVersion(DocumentModel doc)
            throws EloraException {

        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
        if (baseDoc == null) {
            throw new EloraException(
                    "Document |" + doc.getId() + "| has no base version.");
        }
        DocumentModel latestVersionDoc = documentManager.getLastDocumentVersion(
                doc.getRef());
        VersionModel version = new VersionModelImpl();
        DocumentModel newBaseDoc = null;

        if (baseDoc.getId().equals(latestVersionDoc.getId())) {
            List<DocumentModel> documentModelList = documentManager.getVersions(
                    doc.getRef());
            DocumentModel previousDoc = documentModelList.get(
                    documentModelList.size() - 2);

            Long majorVersion = (Long) baseDoc.getPropertyValue(
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
            Long prevMajorVersion = (Long) previousDoc.getPropertyValue(
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

            if (prevMajorVersion.equals(majorVersion)) {
                newBaseDoc = previousDoc;
            } else {
                newBaseDoc = EloraDocumentHelper.getMajorReleasedOrObsoleteVersion(
                        previousDoc);
            }
        } else {
            newBaseDoc = latestVersionDoc;
        }
        version.setId(newBaseDoc.getId());
        return version;
    }

    public boolean isBaseVersion(String selectedVersionId)
            throws EloraException {

        VersionModel selectedVersion = new VersionModelImpl();
        selectedVersion.setId(selectedVersionId);

        DocumentRef docRef = navigationContext.getCurrentDocument().getRef();
        DocumentModel wcDoc = documentManager.getWorkingCopy(docRef);
        DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);
        if (baseDoc != null
                && selectedVersion.getId().equals(baseDoc.getId())) {
            return true;
        } else {
            return false;
        }
    }
}
