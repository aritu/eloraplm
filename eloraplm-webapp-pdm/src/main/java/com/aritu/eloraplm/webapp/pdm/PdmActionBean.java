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

package com.aritu.eloraplm.webapp.pdm;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

@Name("pdmAction")
@Scope(ScopeType.EVENT)
public class PdmActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PdmActionBean.class);

    @Context
    protected RelationManager relationManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    protected EloraVersionLabelService eloraVersionLabelService = Framework.getService(EloraVersionLabelService.class);

    public void checkIn() throws EloraException {
        String logInitMsg = "[checkIn] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            boolean isProxy = currentDoc.isProxy();

            // We always get the working copy because it is possible to execute
            // checkin from a proxy document and we need wc to get all its
            // relations

            DocumentModel doc = null;
            if (isProxy) {
                doc = documentManager.getWorkingCopy(currentDoc.getRef());
            } else {
                doc = currentDoc;
            }

            // TODO: ¿Hay que mirar los permisos del usuario aquí?
            // Check if it is locked
            if (!doc.isLocked()) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("message.error.checkin.notLocked"));
                return;
            }
            // Check if it is checked out, or else, do checkout
            EloraDocumentHelper.checkOutDocument(doc);

            String checkinComment = "Updated and checked in from Elora UI.";
            EloraDocumentHelper.checkInDocument(
                    EloraConfigHelper.getReleasedLifecycleStatesConfig(),
                    documentManager, eloraVersionLabelService, doc,
                    checkinComment);

            // Update viewer before save
            // Blob viewerBlob = ViewerPdfUpdater.createViewer(doc);
            // if (viewerBlob != null) {
            // EloraDocumentHelper.addViewerBlob(doc, viewerBlob);
            // }

            if (isProxy) {
                // If checkin is executed from a proxy we save wc document
                // (because we checked in wc) but we wan't to refresh current
                // document (proxy) to see last changes
                doc = documentManager.saveDocument(doc);
                documentManager.save();
                navigationContext.invalidateCurrentDocument();
            } else {
                // If checkin is executed from wc is enough to save current
                // document because it coincides (wc == currentDoc)
                navigationContext.saveCurrentDocument();
            }

            // Duplicate relations from doc to docLastVersion
            EloraRelationHelper.copyRelationsToLastVersion(doc,
                    eloraDocumentRelationManager, documentManager);

            // Unlock the document
            if (doc.isLocked()) {
                doc.removeLock();
            }

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("message.checkin.checkinSuccess"));

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(logInitMsg + "Uncontrolled exception: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public String undoCheckout() {
        String logInitMsg = "[undoCheckout] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            DocumentModel doc = navigationContext.getCurrentDocument();
            VersionModel version = new VersionModelImpl();
            version.setId(EloraDocumentHelper.getLatestVersion(doc,
                    documentManager).getId());

            DocumentModel restoredDoc = EloraDocumentHelper.restoreToVersion(
                    doc, version, eloraDocumentRelationManager, documentManager);

            // TODO: Mirar en que nos afectan los eventos de abajo
            // same as edit basically
            // XXX AT: do edit events need to be sent?
            EventManager.raiseEventsOnDocumentChange(restoredDoc);

            navigationContext.invalidateCurrentDocument();

            // We don't return to restoredDoc because if we are in a proxy it
            // navigates to its source
            return navigationContext.navigateToDocument(
                    navigationContext.getCurrentDocument(), "after-edit");

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(logInitMsg + "Uncontrolled exception: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        return logInitMsg;
    }

}
