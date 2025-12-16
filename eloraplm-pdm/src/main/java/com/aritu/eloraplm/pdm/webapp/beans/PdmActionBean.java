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

package com.aritu.eloraplm.pdm.webapp.beans;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.pdm.util.PdmHelper;
import com.aritu.eloraplm.versioning.VersionLabelService;

@Name("pdmAction")
@Scope(ScopeType.EVENT)
public class PdmActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PdmActionBean.class);

    @In(create = true)
    protected transient WebActions webActions;

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

    protected VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    private String checkinComment;

    public String getCheckinComment() {
        return checkinComment;
    }

    public void setCheckinComment(String checkinComment) {
        this.checkinComment = checkinComment;
    }

    public void checkIn(DocumentModel doc) {
        String logInitMsg = "[checkIn] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        CheckinManager checkinManager = Framework.getService(
                CheckinManager.class);
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.checkinNotAllowed"));
                throw new EloraException("Cannot checkin a version");
            }
            try {
                doc = checkinManager.checkinDocument(doc, checkinComment, true);
            } catch (CheckinNotAllowedException e) {
                DocumentModel errorDoc = e.getErrorDocument();
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(
                                "eloraplm.message.error.pdm.checkinNotAllowed"),
                        (errorDoc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)
                                ? errorDoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                : ""),
                        errorDoc.getTitle());
                throw new EloraException(e.getMessage());
            } catch (DocumentNotCheckedOutException e) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.documentNotCheckedOut"));
                throw new EloraException(e.getMessage());

            } catch (BomCharacteristicsValidatorException e) {
                DocumentModel errorDoc = e.getDocument();
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.characteristicsRequired"),
                        (errorDoc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)
                                ? errorDoc.getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                : ""),
                        errorDoc.getTitle());
                throw new EloraException(e.getMessage());
            }

            PdmHelper.invalidateAndResetTabList(webActions, navigationContext);

            // Seam Event
            Events.instance().raiseEvent(PdmEventNames.PDM_CHECKED_IN_EVENT,
                    doc);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.checkin"));
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.checkin"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.checkin"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public void undoCheckout(DocumentModel doc) {
        String logInitMsg = "[undoCheckout] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.undoCheckoutNotAllowed"));
                throw new EloraException(
                        "Cannot do undo checkout in a version");
            }

            VersionModel version = new VersionModelImpl();
            DocumentModel latestVersion = EloraDocumentHelper.getLatestVersion(
                    doc);
            if (latestVersion == null) {
                throw new EloraException("Document |" + doc.getId()
                        + "| has no latest version or it is unreadable.");
            }
            version.setId(latestVersion.getId());

            // We have to obtain current tab actions before restoring the
            // document
            Action currentTabAction = webActions.getCurrentTabAction();
            Action currentSubTabAction = webActions.getCurrentSubTabAction();

            doc = EloraDocumentHelper.restoreWorkingCopyToVersion(doc, version,
                    eloraDocumentRelationManager, documentManager);

            // TODO: Mirar en que nos afectan los eventos de abajo
            // same as edit basically
            // XXX AT: do edit events need to be sent?
            EventManager.raiseEventsOnDocumentChange(doc);

            if (doc.isLocked()) {
                doc.removeLock();
            }

            PdmHelper.invalidateAndResetTabList(webActions, navigationContext,
                    currentTabAction, currentSubTabAction);

            // Seam event
            Events.instance().raiseEvent(
                    PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, doc);

            // Nuxeo Event
            String comment = doc.getVersionLabel();
            EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT,
                    doc, comment);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.undoCheckout"));
            log.info("Document |" + doc.getId() + "| has been unchecked out.");

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.undoCheckout"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.undoCheckout"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public void checkOut(DocumentModel doc) {
        String logInitMsg = "[checkOut] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        checkOut(doc, null);
    }

    public void checkOutFromVersion(DocumentModel doc,
            VersionModel versionFrom) {
        String logInitMsg = "[checkOutFromVersion] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        checkOut(doc, versionFrom);
    }

    private void checkOut(DocumentModel doc, VersionModel versionFrom) {
        String logInitMsg = "[checkOut] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.checkoutNotAllowed"));
            }
            // TODO Honek ez dauka zentzu askorik. Immutable proxyekin nola
            // dabilen aztertu beharko litzateke, baina WC bada, aurreko lerroan
            // CHECKOUT NOT ALLOWED errore mezua erakutsiko du, baina aurrera
            // egingo du?? Seguruenik action-en filtroei esker ez da inoiz
            // iristen kasu horretara, baina konpondu egin behar da, eta
            // behintzat CheckoutService-era errore hori ez pasa.
            DocumentModel wcDoc = getWcDoc(doc);

            CheckoutService cos = Framework.getService(CheckoutService.class);

            wcDoc = cos.checkOutFromVersion(wcDoc, versionFrom);

            PdmHelper.invalidateAndResetTabList(webActions, navigationContext);

            // Seam event
            Events.instance().raiseEvent(PdmEventNames.PDM_CHECKED_OUT_EVENT,
                    doc);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.checkout"));
            log.info("Document |" + wcDoc.getId() + "| has been checked out.");

        } catch (DocumentAlreadyLockedException
                | DocumentInUnlockableStateException | EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.checkout"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.checkout"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private DocumentModel getWcDoc(DocumentModel doc) {
        if (doc.isProxy()) {
            doc = documentManager.getWorkingCopy(doc.getRef());
        }
        if (doc.isImmutable()) {
            doc = documentManager.getWorkingCopy(doc.getRef());
        }
        return doc;
    }

}
