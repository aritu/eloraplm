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
import java.util.List;
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

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
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
import com.aritu.eloraplm.pdm.util.RelationSwitchHelper;
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

    protected VersionLabelService versionLabelService = Framework
            .getService(VersionLabelService.class);

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
        CheckinManager checkinManager = Framework
                .getService(CheckinManager.class);
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages
                        .get("eloraplm.message.error.pdm.checkinNotAllowed"));
                throw new EloraException("Cannot checkin a version");
            }
            try {
                doc = checkinManager.checkinDocument(doc, checkinComment, true);
            } catch (CheckinNotAllowedException e) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(
                                "eloraplm.message.error.pdm.checkinNotAllowed"),
                        e.getErrorDocument()
                                .getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                        e.getErrorDocument().getTitle());
                throw new EloraException(e.getMessage());
            } catch (DocumentNotCheckedOutException e) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.documentNotCheckedOut"));
                throw new EloraException(e.getMessage());

            } catch (BomCharacteristicsValidatorException e) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.characteristicsRequired"),
                        e.getDocument()
                                .getPropertyValue(
                                        EloraMetadataConstants.ELORA_ELO_REFERENCE),
                        e.getDocument().getTitle());
                throw new EloraException(e.getMessage());
            }

            invalidateAndResetTabList();

            // Seam Event
            Events.instance()
                    .raiseEvent(PdmEventNames.PDM_CHECKED_IN_EVENT, doc);

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
            DocumentModel latestVersion = EloraDocumentHelper
                    .getLatestVersion(doc);
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

            invalidateAndResetTabList(currentTabAction, currentSubTabAction);

            // Seam event
            Events.instance()
                    .raiseEvent(PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT, doc);

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
        checkOut(doc, true);
    }

    public void checkOut(DocumentModel doc, boolean handleCharacteristics) {
        String logInitMsg = "[checkOut] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with handleCharacteristics = |"
                + handleCharacteristics + "|");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages
                        .get("eloraplm.message.error.pdm.checkoutNotAllowed"));
            }
            DocumentModel wcDoc = getWcDoc(doc);

            wcDoc = EloraDocumentHelper.lockDocument(wcDoc);
            wcDoc = EloraDocumentHelper.checkOutDocument(wcDoc);

            switchRelatedChildren(wcDoc);

            // if handleCharacteristics is true and the document may have
            // characteristics
            if (handleCharacteristics && EloraDocumentHelper.checkFilter(wcDoc,
                    BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
                BomCharacteristicsHelper.loadCharacteristicMasters(wcDoc);
            }

            // We set the document dirty to update lastContributor and modified
            wcDoc = EloraDocumentHelper.setDocumentDirty(wcDoc);
            wcDoc = documentManager.saveDocument(wcDoc);

            invalidateAndResetTabList();

            // Seam event
            Events.instance()
                    .raiseEvent(PdmEventNames.PDM_CHECKED_OUT_EVENT, doc);

            // Nuxeo Event
            String comment = wcDoc.getVersionLabel();
            EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKED_OUT_EVENT,
                    wcDoc, comment);

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

    public void switchRelatedChildren(DocumentModel subjectWcDoc)
            throws EloraException {

        if (!subjectWcDoc.isCheckedOut()) {
            throw new EloraException("Document is not checked out.");
        }

        RelationSwitchHelper.switchRelations(documentManager,
                eloraDocumentRelationManager, subjectWcDoc);
    }

    private void invalidateAndResetTabList() {
        Action currentTabAction = webActions.getCurrentTabAction();
        Action currentSubTabAction = webActions.getCurrentSubTabAction();
        invalidateAndResetTabList(currentTabAction, currentSubTabAction);
    }

    private void invalidateAndResetTabList(Action currentTabAction,
            Action currentSubTabAction) {
        navigationContext.invalidateCurrentDocument();
        webActions.resetTabList();
        List<Action> tabActionsList = webActions.getTabsList();
        if (tabActionsList.contains(currentTabAction)) {
            webActions.setCurrentTabAction(currentTabAction);
            List<Action> subTabActionsList = webActions.getSubTabsList();
            if (subTabActionsList.contains(currentSubTabAction)) {
                webActions.setCurrentSubTabAction(currentSubTabAction);
            }
        }
    }

}
