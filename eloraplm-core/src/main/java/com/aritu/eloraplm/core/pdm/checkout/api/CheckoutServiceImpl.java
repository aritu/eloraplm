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
package com.aritu.eloraplm.core.pdm.checkout.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.RelationSwitchHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Checkout service implementation.
 *
 * @author aritu
 *
 */
public class CheckoutServiceImpl implements CheckoutService {

    private static final Log log = LogFactory.getLog(CheckoutServiceImpl.class);

    protected EloraDocumentRelationManager eloraDocumentRelationManager = Framework.getService(
            EloraDocumentRelationManager.class);

    @Override
    public DocumentModel checkOut(DocumentModel doc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {
        doc = checkOut(doc, null);
        return doc;
    }

    @Override
    public DocumentModel checkOutFromVersion(DocumentModel doc,
            VersionModel versionFrom)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

        doc = checkOut(doc, versionFrom);
        return doc;
    }

    /**
     * @param doc the document that will be checked out
     * @param versionFrom the version that will be used as based to do the
     *            checkout. If it is null, document base version will be used.
     * @return
     * @throws EloraException
     * @throws DocumentAlreadyLockedException
     * @throws DocumentInUnlockableStateException
     * @throws DocumentLockRightsException
     */
    protected DocumentModel checkOut(DocumentModel doc,
            VersionModel versionFrom)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

        CoreSession session = doc.getCoreSession();
        String logInitMsg = "[checkOut] [" + session.getPrincipal().getName()
                + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Checks
        // If Proxy, get source document
        if (doc.isProxy()) {
            doc = session.getSourceDocument(doc.getRef());
        }
        // Is WC
        if (doc.isImmutable()) {
            log.error(logInitMsg + "Document with docUid = |" + doc.getId()
                    + "| is not a WC. It is not possible to do checkout in a version.");
            throw new EloraException("Cannot do checkout in a version");
        }

        // Lock document
        doc = EloraDocumentHelper.lockDocument(doc);

        if (versionFrom == null || versionFrom.getId() == null) {

            // Check out document from WC
            doc = checkOutDocument(doc);

            // Switch relations
            RelationSwitchHelper.switchRelations(session,
                    eloraDocumentRelationManager, doc);
        } else {

            // Check out document from given version
            doc = checkOutDocumentFromVersion(doc, versionFrom);

            // Switch relations
            RelationSwitchHelper.switchRelationsFromVersion(session,
                    eloraDocumentRelationManager, doc, versionFrom);
        }

        // We set the document dirty to update lastContributor and modified
        doc = EloraDocumentHelper.setDocumentDirty(doc);
        doc = session.saveDocument(doc);

        // Nuxeo Event
        String checkoutComment = doc.getVersionLabel();
        EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKED_OUT_EVENT, doc,
                checkoutComment);

        log.trace(logInitMsg + "--- EXIT --- ");
        return doc;

    }

    @Override
    public DocumentModel checkOutInCmProcess(DocumentModel doc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

        doc = checkOutDocument(doc);
        return doc;
    }

    @Override
    public DocumentModel checkOutFromVersionInCmProcess(DocumentModel doc,
            DocumentModel versionFromDoc)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException {

        if (versionFromDoc == null) {
            doc = checkOutDocument(doc);
        } else {
            DocumentModel docBaseVersion = EloraDocumentHelper.getBaseVersion(
                    doc);
            if (versionFromDoc.getId().equals(docBaseVersion.getId())) {
                doc = checkOutDocument(doc);
            } else {
                VersionModel versionFrom = new VersionModelImpl();
                versionFrom.setId(versionFromDoc.getId());
                doc = checkOutDocumentFromVersion(doc, versionFrom);
            }
        }

        return doc;
    }

    /**
     * Function to set a document as checked out.
     *
     * @param session
     * @param doc
     * @return
     */
    protected DocumentModel checkOutDocument(DocumentModel doc) {

        doc.refresh();

        if (!doc.isCheckedOut()) {

            doc.checkOut();

            // set version info metadata
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);

            doc = updateVersionInfoMetadata(doc, baseDoc.getId(), false);

            doc = followTransitionAfterCheckout(doc);

            CoreSession session = doc.getCoreSession();

            doc = session.saveDocument(doc);

            session.save();
        }

        doc.refresh();
        return doc;
    }

    protected DocumentModel checkOutDocumentFromVersion(DocumentModel doc,
            VersionModel versionFrom) throws EloraException {

        CoreSession session = doc.getCoreSession();

        doc.refresh();
        if (!doc.isCheckedOut()) {

            doc.checkOut();

            DocumentModel versionFromDoc = session.getDocument(
                    new IdRef(versionFrom.getId()));

            EloraDocumentHelper.copyProperties(versionFromDoc, doc);

            // set version info metadata
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            boolean nonCorrelativeVersion = !baseDoc.getId().equals(
                    versionFrom.getId());

            doc = updateVersionInfoMetadata(doc, versionFrom.getId(),
                    nonCorrelativeVersion);

            doc = followTransitionAfterCheckout(doc);

            // disable validation since old values maybe are not valid now.
            doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                    Forcing.TURN_OFF);

            doc = session.saveDocument(doc);

            doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                    Forcing.USUAL);

            session.save();
        }

        doc.refresh();
        return doc;
    }

    protected DocumentModel followTransitionAfterCheckout(DocumentModel doc) {

        // TODO EloraVersioningService-en followTransitionByOption protected
        // denez, ezin zaio deitu checkout egiteko funtzioetatik, eta gainera
        // Document erabiltzen duenez, ezin da funtzio batera atera.
        //
        // Modu ona izango zen dokumentua Dirty jarri ahal izatea, eta gero
        // saveDocument egitea checked out jartzeko, doc.checkOut() jarri ordez,
        // baina oraingoz ez dugu lortu.

        // ##################################################################
        // NOTICE: Hau aldatzen bada,
        // EloraVersioningService.followTransitionByOption be aldatu behar
        // da.
        // ##################################################################
        // We have to emulate the saveDocument, the document has to follow a
        // transition in some states
        String lifecycleState = doc.getCurrentLifeCycleState();
        boolean isReleasedOrObsoleteState = EloraDocumentHelper.isReleasedState(
                lifecycleState)
                || EloraDocumentHelper.isObsoleteState(lifecycleState);
        if (isReleasedOrObsoleteState
                && doc.getAllowedStateTransitions().contains(
                        EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY)) {
            doc.followTransition(
                    EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY);
        }
        // ##################################################################

        return doc;
    }

    @Override
    public DocumentModel undoCheckout(DocumentModel doc) throws EloraException {

        CoreSession session = doc.getCoreSession();
        String logInitMsg = "[undoCheckout] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (doc.isImmutable()) {
                throw new EloraException(
                        "Cannot do undo checkout in a version");
            }

            if (!doc.isCheckedOut()) {
                throw new EloraException("Document |" + doc.getId()
                        + "| is already checked out");
            }

            VersionModel version = new VersionModelImpl();

            DocumentModel baseVersion = EloraDocumentHelper.getBaseVersion(doc);
            if (baseVersion == null) {
                throw new EloraException(
                        "Document |" + doc.getId() + "| has no base version.");
            }
            version.setId(baseVersion.getId());

            // TODO: Esto se debe utilizar cuando llamemos desde pdmActions a
            // este servicio. Por ahora, este servicio lo vamos a utilizar para
            // corregir los documentos que han quedado checked out en Ecenarro y
            // esto no es necesario
            // doc = EloraDocumentHelper.restoreWorkingCopyToVersion(doc,
            // version,
            // eloraDocumentRelationManager, session);

            // TODO: Esto se utiliza para corregir los checked out de Ecenarro.
            // Es puntual. Luego hay que quitar esto y dejar
            // restoreWorkingCopyToVersion
            doc = EloraDocumentHelper.restoreToVersion(doc, version, true, true,
                    session);

            // TODO: Mirar en que nos afectan los eventos de abajo. Para
            // corregir Ecenarro no lo utilizamos.
            // same as edit basically
            // XXX AT: do edit events need to be sent?
            // EventManager.raiseEventsOnDocumentChange(doc);

            // TODO: Quitamos esto para corrección de Ecenarro
            /*
            if (doc.isLocked()) {
                doc.removeLock();
            }
            */

            // TODO: Quitamos esto para la corrección de Ecenarro. Para volver a
            // ponerlo y utilizar este servicio desde pdmActions hay que
            // asegurarse de que este evento no nos da problemas
            // Nuxeo Event
            /*String comment = doc.getVersionLabel();
            EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT,
                    doc, comment);
            */
            log.info("Document |" + doc.getId() + "| has been unchecked out.");

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        return doc;
    }

    protected DocumentModel updateVersionInfoMetadata(DocumentModel doc,
            String sourceVersion, boolean nonCorrelativeVersion) {
        doc.setPropertyValue(EloraMetadataConstants.ELORA_VINF_SOURCE_VERSION,
                sourceVersion);
        doc.setPropertyValue(
                EloraMetadataConstants.ELORA_VINF_NON_CORRELATIVE_VERSION,
                nonCorrelativeVersion);

        return doc;
    }

}
