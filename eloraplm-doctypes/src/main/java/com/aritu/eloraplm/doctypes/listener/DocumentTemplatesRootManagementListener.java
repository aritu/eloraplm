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

package com.aritu.eloraplm.doctypes.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * This class listens creations, deletions or changes in StructureRoot,
 * CollaborationRoot or LibraryRoot documents, in order to apply them in
 * DocumentTemplatesRoots.
 *
 * @author aritu
 *
 */
public class DocumentTemplatesRootManagementListener implements EventListener {

    protected static Log log = LogFactory.getLog(
            DocumentTemplatesRootManagementListener.class);

    @Override
    public void handleEvent(Event event) {
        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel sourceDocument = docEventContext.getSourceDocument();

                // Check if document's type is handled
                if (isDocTypeHandled(sourceDocument.getType())) {
                    String eventName = event.getName();
                    log.trace(logInitMsg + "doc id = |" + sourceDocument.getId()
                            + "|, doctype = |" + sourceDocument.getType()
                            + "|, eventName = |" + eventName + "|");

                    CoreSession session = docEventContext.getCoreSession();

                    try {
                        switch (eventName) {
                        case DocumentEventTypes.DOCUMENT_CREATED:
                            handleDocumentCreation(sourceDocument);
                            break;

                        case DocumentEventTypes.DOCUMENT_REMOVED:
                            handleDocumentDeletion(sourceDocument);
                            break;

                        case DocumentEventTypes.DOCUMENT_UPDATED:
                            handleDocumentModification(sourceDocument);
                            break;
                        }
                    } catch (Exception e) {
                        log.error(
                                "Error managing document templates root. Error message: "
                                        + e.getMessage());
                        // Tell the event the exception must be bubbled
                        event.markBubbleException();

                        // Rollback the transaction
                        TransactionHelper.setTransactionRollbackOnly();
                        event.markRollBack();

                        // Throw NuxeoException to be handled in UI layer
                        throw new NuxeoException(
                                "Error managing document templates root. Error message:"
                                        + e.getMessage());
                    }

                    session.save();
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
        return Arrays.asList(DocumentEventTypes.DOCUMENT_CREATED,
                DocumentEventTypes.DOCUMENT_REMOVED,
                DocumentEventTypes.DOCUMENT_UPDATED);
    }

    protected boolean isDocTypeHandled(String docType) {
        for (String handledDocType : getHandledDocTypes()) {
            if (handledDocType.equals(docType)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledDocTypes() {
        return Arrays.asList(
                new String[] { EloraDoctypeConstants.STRUCTURE_ROOT,
                        EloraDoctypeConstants.COLLABORATION_ROOT,
                        EloraDoctypeConstants.LIBRARY_ROOT });
    }

    private void handleDocumentCreation(DocumentModel sourceDocument)
            throws EloraException {
        createDocumentTemplatesRoot(sourceDocument);
    }

    private DocumentModel createDocumentTemplatesRoot(
            DocumentModel sourceDocument) throws EloraException {
        String logInitMsg = "[createDocumentTemplatesRoot] ["
                + sourceDocument.getCoreSession().getPrincipal().getName()
                + "] ";

        CoreSession session = sourceDocument.getCoreSession();
        DocumentModel documentTemplatesRoot = null;

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String name = pss.generatePathSegment(sourceDocument.getTitle());

        DocumentModel domain = EloraStructureHelper.getParent(sourceDocument,
                NuxeoDoctypeConstants.DOMAIN);
        DocumentModel documentTemplatesFolder = EloraStructureHelper.getDocumentTemplatesFolder(
                domain);

        if (documentTemplatesFolder != null) {

            String parentPath = documentTemplatesFolder.getPathAsString();

            DocumentModel doc = session.createDocumentModel(parentPath, name,
                    EloraDoctypeConstants.DOCUMENT_TEMPLATES_ROOT);

            doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                    sourceDocument.getTitle());
            doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_SOURCE,
                    sourceDocument.getId());

            documentTemplatesRoot = session.createDocument(doc);
        } else {
            String errorMsg = "Missing "
                    + EloraDoctypeConstants.DOCUMENT_TEMPLATES + " folder.";
            log.error(logInitMsg + errorMsg);
            throw new EloraException(errorMsg);
        }

        return documentTemplatesRoot;
    }

    private void handleDocumentDeletion(DocumentModel sourceDocument) {
        deleteDocumentTemplatesRoot(sourceDocument);
    }

    private void deleteDocumentTemplatesRoot(DocumentModel sourceDocument) {
        CoreSession session = sourceDocument.getCoreSession();

        String documentTemplatesRootFolderQuery = EloraQueryFactory.getDocumentTemplatesRootFolderQuery(
                sourceDocument.getId());
        DocumentModelList documentTemplatesRootFolders = session.query(
                documentTemplatesRootFolderQuery);

        if (!documentTemplatesRootFolders.isEmpty()) {
            for (DocumentModel documentTemplatesRootFolder : documentTemplatesRootFolders) {
                session.removeDocument(documentTemplatesRootFolder.getRef());
            }
        }
    }

    private void handleDocumentModification(DocumentModel sourceDocument) {
        CoreSession session = sourceDocument.getCoreSession();
        String newTitle = sourceDocument.getTitle();

        String documentTemplatesRootFolderQuery = EloraQueryFactory.getDocumentTemplatesRootFolderQuery(
                sourceDocument.getId());
        DocumentModelList documentTemplatesRootFolders = session.query(
                documentTemplatesRootFolderQuery);

        if (!documentTemplatesRootFolders.isEmpty()) {
            for (DocumentModel documentTemplatesRootFolder : documentTemplatesRootFolders) {

                // Manage title modification
                String currentTitle = documentTemplatesRootFolder.getTitle();

                if (currentTitle != null && !currentTitle.equals(newTitle)) {
                    documentTemplatesRootFolder.setPropertyValue(
                            NuxeoMetadataConstants.NX_DC_TITLE, newTitle);
                    session.saveDocument(documentTemplatesRootFolder);
                }
            }
        }
    }

}
