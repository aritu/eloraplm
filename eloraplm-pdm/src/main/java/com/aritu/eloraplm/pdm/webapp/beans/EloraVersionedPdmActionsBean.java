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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.versioning.VersionedActionsBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.pdm.checkout.api.CheckoutService;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.util.PdmHelper;

/**
 * PDM Actions for History tab
 *
 * @author Aritu
 */
@Name("eloraVersionedPdmActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class EloraVersionedPdmActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            VersionedActionsBean.class);

    @In(create = true)
    protected transient WebActions webActions;

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

    public void checkOutFromVersion() throws EloraException {
        if (versionedActions.getSelectedVersionId() != null) {
            VersionModel selectedVersion = new VersionModelImpl();
            selectedVersion.setId(versionedActions.getSelectedVersionId());

            checkOutFromVersion(selectedVersion);
        }
    }

    private void checkOutFromVersion(VersionModel selectedVersion)
            throws EloraException {

        String logInitMsg = "[checkOutFromVersion] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel doc = navigationContext.getCurrentDocument();
            if (!EloraDocumentHelper.isWorkingCopy(doc)) {
                doc = documentManager.getWorkingCopy(doc.getRef());
            }

            if (doc.isImmutable()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.pdm.checkoutNotAllowed"));
            }

            CheckoutService cos = Framework.getService(CheckoutService.class);

            doc = cos.checkOutFromVersion(doc, selectedVersion);

            PdmHelper.invalidateAndResetTabList(webActions, navigationContext);

            // Seam event
            Events.instance().raiseEvent(PdmEventNames.PDM_CHECKED_OUT_EVENT,
                    doc);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.checkout"));
            log.info("Document |" + doc.getId() + "| has been checked out.");

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

}
