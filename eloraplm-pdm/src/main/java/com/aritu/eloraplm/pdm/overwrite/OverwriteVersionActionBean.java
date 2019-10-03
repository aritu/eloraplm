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

package com.aritu.eloraplm.pdm.overwrite;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.overwrite.helper.OverwriteVersionHelper;

@Name("overwriteVersionAction")
@Scope(ScopeType.EVENT)
public class OverwriteVersionActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            OverwriteVersionActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    protected DocumentValidationService validator = Framework.getService(
            DocumentValidationService.class);

    private String justification;

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public void overwriteVersion() throws EloraException {
        String logInitMsg = "[overwriteVersion] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel currentDoc = getCurrentDocument();
            log.trace(logInitMsg + "Overwrite document |" + currentDoc.getId()
                    + "|");
            checkCheckout(currentDoc);
            DocumentModel baseVersionDoc = EloraDocumentHelper.getBaseVersion(
                    currentDoc);
            if (baseVersionDoc == null) {
                throw new EloraException("Document |" + currentDoc.getId()
                        + "| has no base version. Probably because it has no AVs.");
            }

            // We have to obtain current tab actions before restoring the
            // document
            Action currentTabAction = webActions.getCurrentTabAction();
            Action currentSubTabAction = webActions.getCurrentSubTabAction();

            OverwriteVersionHelper.overwriteDocument(currentDoc, baseVersionDoc,
                    eloraDocumentRelationManager, validator, documentManager,
                    justification, null, null);

            if (currentDoc.isLocked()) {
                currentDoc.removeLock();
            }

            resetTabList(currentTabAction, currentSubTabAction);
            navigationContext.invalidateCurrentDocument();

            // Seam Event
            Events.instance().raiseEvent(PdmEventNames.PDM_OVERWRITTEN_EVENT,
                    currentDoc);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.overwriteVersion"));

        } catch (CheckinNotAllowedException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.pdm.checkinNotAllowed"),
                    e.getErrorDocument().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    e.getErrorDocument().getTitle());
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (BomCharacteristicsValidatorException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(
                            "eloraplm.message.error.pdm.characteristicsRequired"),
                    e.getDocument().getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE),
                    e.getDocument().getTitle());
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.overwriteVersion"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.overwriteVersion"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private void checkCheckout(DocumentModel currentDoc) {
        try {
            EloraDocumentHelper.checkThatIsCheckedOutByMe(currentDoc);
        } catch (DocumentNotCheckedOutException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.documentNotCheckedOutByMe"));
        }
    }

    private DocumentModel getCurrentDocument() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        currentDoc = currentDoc.isProxy()
                ? documentManager.getWorkingCopy(currentDoc.getRef())
                : currentDoc;
        return currentDoc;
    }

    private void resetTabList(Action currentTabAction,
            Action currentSubTabAction) {
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
