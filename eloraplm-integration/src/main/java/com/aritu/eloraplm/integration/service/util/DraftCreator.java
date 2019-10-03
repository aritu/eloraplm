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
package com.aritu.eloraplm.integration.service.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.api.DraftManager;
import com.aritu.eloraplm.integration.service.DraftService;

/**
 * @author aritu
 *
 */
public class DraftCreator {

    private static final Log log = LogFactory.getLog(DraftService.class);

    private CoreSession session;

    private DocumentModel originDoc;

    private DocumentModel originWcDoc;

    private DocumentModel draftDoc;

    private String tempFolderPath;

    private DocumentRelationManager docRelManager;

    private RelationManager relManager;

    public DraftCreator(DocumentModel originDoc, String tempFolderPath) {
        session = originDoc.getCoreSession();
        this.originDoc = originDoc;
        this.tempFolderPath = tempFolderPath;
        originWcDoc = getOriginWcDoc();
        docRelManager = Framework.getService(DocumentRelationManager.class);
        relManager = Framework.getService(RelationManager.class);
    }

    private DocumentModel getOriginWcDoc() {
        if (originDoc.isVersion() || originDoc.isProxy()) {
            return session.getWorkingCopy(originDoc.getRef());
        } else {
            return originDoc;
        }
    }

    /**
     * @return
     * @throws EloraException
     */
    public DocumentModel create() throws EloraException {

        String logInitMsg = "[create] [" + session.getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "Creating draft for document |"
                + originDoc.getId() + "|");

        createDocumentModel();
        copyMetadataFromOriginDoc();
        createDocWithoutValidation();

        relateDraftWithOriginDoc();

        if (!draftDoc.isLocked()) {
            draftDoc.setLock();
        }

        log.trace(logInitMsg + "Draft created for document |"
                + originDoc.getId() + "| of type |" + originDoc.getType()
                + "|, draft uid |" + draftDoc.getId() + "|");

        return draftDoc;
    }

    private void createDocumentModel() throws EloraException {

        String draftType = getDraftType();
        draftDoc = session.createDocumentModel(tempFolderPath,
                originDoc.getName() + "_Draft", draftType);
    }

    private String getDraftType() throws EloraException {
        switch (originDoc.getType()) {
        case "CadAssembly":
            return "CadAssemblyDraft";
        case "CadPart":
            return "CadPartDraft";
        case "CadDrawing":
            return "CadDrawingDraft";
        case "CadDesignTable":
            return "CadDesignTableDraft";
        case "Folder":
            return "FolderDraft";
        case "OrderedFolder":
            return "OrderedFolderDraft";
        default:
            throw new EloraException("Document with type |"
                    + originDoc.getType() + "| can't have a draft.");
        }
    }

    private void copyMetadataFromOriginDoc() {

        DraftManager draftManager = Framework.getService(DraftManager.class);

        EloraDocumentHelper.copyProperties(originDoc, draftDoc,
                draftManager.getExcludedPropertiesFromCopy());
    }

    private void createDocWithoutValidation() {

        draftDoc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.TURN_OFF);
        draftDoc = session.createDocument(draftDoc);
        session.save();
    }

    private void relateDraftWithOriginDoc() {
        String logInitMsg = "[relateDraftWithOriginDoc] ["
                + session.getPrincipal().getName() + "] ";

        QNameResource draftNode = (QNameResource) relManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, draftDoc, null);
        docRelManager.addRelation(session, originWcDoc, draftNode,
                EloraRelationConstants.HAS_ELORA_DRAFT_RELATION);

        log.trace(logInitMsg + "Created relation between document |"
                + originWcDoc.getId() + "| and its draft |" + draftDoc.getId()
                + "|");
    }
}
