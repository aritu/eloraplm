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
package com.aritu.eloraplm.codecreation.listener;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.codecreation.util.CodeCreationHelper;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class AutomaticCodeCreationListener implements EventListener {

    private static Log log = LogFactory.getLog(
            AutomaticCodeCreationListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                CoreSession session = docEventContext.getCoreSession();

                // Execute only for auto and autoIfEmpty modes
                if (!CodeCreationHelper.isGenerateAutomaticCode(doc)) {
                    return;
                }

                // Ignore if template
                if (EloraDocumentHelper.isDocumentUnderTemplateRoot(doc,
                        session)) {
                    return;
                }

                // If reference is already filled, do nothing
                Serializable oldReference = doc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE);
                if (oldReference != null
                        && !oldReference.toString().isEmpty()) {
                    return;
                }

                String logInitMsg = "[handleEvent] ["
                        + event.getContext().getPrincipal().getName() + "] ";
                log.trace(logInitMsg + "Generating automatic code for doc |"
                        + doc.getId() + "|");

                String reference;
                try {
                    reference = CodeCreationHelper.createCode(doc,
                            session.getPrincipal().getName());
                    log.trace(
                            logInitMsg + "new reference = |" + reference + "|");

                    // Check if obtained reference is valid
                    String docUid = doc.isProxy() ? doc.getSourceId()
                            : doc.getId();
                    EloraDocumentHelper.validateDocumentReference(session,
                            reference, doc.getType(), docUid);
                    log.trace(logInitMsg + "new reference = |" + reference
                            + "| is valid.");

                    doc.setPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE,
                            reference);
                    log.trace(logInitMsg + "Automatic code |" + reference
                            + "| generated for doc |" + doc.getId() + "|");

                } catch (EloraException e) {
                    log.error("Error creating automatic code. Error message: "
                            + e.getMessage());
                    // Tell the event the exception must be bubbled
                    event.markBubbleException();

                    // Send error message TO UI layer
                    doc.putContextData(EloraGeneralConstants.CONTEXT_ERROR_KEY,
                            "eloraplm.message.error.automaticCodeCreation");

                    // Rollback the transaction
                    TransactionHelper.setTransactionRollbackOnly();
                    event.markRollBack();

                    // Throw NuxeoException to be handled in UI layer
                    throw new NuxeoException(
                            "Error creating automatic code. Error message:"
                                    + e.getMessage());
                }
            }
        }
    }

    protected boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledEventsName() {
        return Arrays.asList(DocumentEventTypes.ABOUT_TO_CREATE);
    }

}
