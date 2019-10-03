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
package com.aritu.eloraplm.integration.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.api.DraftManager;
import com.aritu.eloraplm.integration.service.util.DraftCreator;

/**
 * @author aritu
 *
 */
public class DraftService implements DraftManager {

    private static final Log log = LogFactory.getLog(DraftService.class);

    private Map<DocumentRef, String> tempFolderPaths;

    private Map<String, List<String>> excludedPropertiesFromCopy;

    public DraftService() {
        tempFolderPaths = new HashMap<DocumentRef, String>();

        excludedPropertiesFromCopy = new HashMap<String, List<String>>();
        List<String> dcList = new ArrayList<String>();
        dcList.add(NuxeoMetadataConstants.NX_DC_CREATED);
        dcList.add(NuxeoMetadataConstants.NX_DC_CREATOR);
        dcList.add(NuxeoMetadataConstants.NX_DC_MODIFIED);
        dcList.add(NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
        dcList.add(NuxeoMetadataConstants.NX_DC_CONTRIBUTORS);
        excludedPropertiesFromCopy.put(NuxeoSchemaConstants.DUBLINCORE, dcList);
        List<String> ltrversList = new ArrayList<String>();
        ltrversList.add(EloraMetadataConstants.ELORA_LTRVERS_MAJOR);
        excludedPropertiesFromCopy.put(
                EloraSchemaConstants.MAJOR_LETTER_VERSIONING, ltrversList);
    }

    @Override
    public DocumentModel createDraftForDocument(DocumentModel doc,
            DocumentRef structureRootRealRef) throws EloraException {

        DraftCreator draftCreator = new DraftCreator(doc,
                getTempFolderPath(doc.getCoreSession(), structureRootRealRef));
        return draftCreator.create();
    }

    @Override
    public String getTempFolderPath(CoreSession session,
            DocumentRef structureRootRealRef) throws EloraException {
        if (!tempFolderPaths.containsKey(structureRootRealRef)) {
            populateTempFoldersMap(session, structureRootRealRef);
        }
        return tempFolderPaths.get(structureRootRealRef);
    }

    /**
     * Populates the temp folders map if there is not any item for the given
     * structure root; if structure root is null, it is calculated from the real
     * document
     *
     * @param session
     * @param structureRootRealRef
     * @return
     * @throws EloraException
     */
    private void populateTempFoldersMap(CoreSession session,
            DocumentRef structureRootRealRef) throws EloraException {
        if (structureRootRealRef == null) {
            throw new EloraException("structureRootRealRef is null.");
        }

        if (!tempFolderPaths.containsKey(structureRootRealRef)) {
            String tempFolderPath = EloraStructureHelper.getTempFolderPath(
                    structureRootRealRef, session);
            tempFolderPaths.put(structureRootRealRef, tempFolderPath);
        }
    }

    @Override
    public DocumentModel getDraftForDocument(CoreSession session,
            DocumentModel wcDoc, String username, boolean required)
            throws EloraException {

        String logInitMsg = "[getDraftForDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Getting draft for document |" + wcDoc.getId()
                + "|");

        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.HAS_ELORA_DRAFT_RELATION);
        DocumentModelList drafts = RelationHelper.getObjectDocuments(wcDoc,
                predicateResource);

        List<DocumentModel> userDrafts = new ArrayList<DocumentModel>();

        // We are only interested in the drafts created by the current user
        for (DocumentModel draft : drafts) {
            if (draft.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_CREATOR).toString().equals(
                            username)) {
                userDrafts.add(draft);
            }
        }

        if (userDrafts.size() > 1) {
            throw new EloraException(
                    "Same document has more than one draft for the user |"
                            + username + "|.");
        }
        if (userDrafts.isEmpty()) {
            if (required) {
                throw new EloraException(
                        "Document |" + wcDoc.getId() + "| has no draft");
            } else {
                return null;
            }
        } else {
            DocumentModel draftDoc = userDrafts.get(0);
            if (!draftDoc.hasFacet(EloraFacetConstants.FACET_ELORA_DRAFT)) {
                throw new EloraException(
                        "Document should be a draft, but it is not.");
            }

            if (!draftDoc.isLocked()) {
                draftDoc.setLock();
            }

            log.trace(logInitMsg + "Draft found for document |" + wcDoc.getId()
                    + "|, draft uid |" + draftDoc.getId() + "|");

            return draftDoc;
        }
    }

    @Override
    public void removeDocumentDraft(CoreSession session, DocumentRef wcDocRef)
            throws EloraException {

        String logInitMsg = "[removeDocumentDraft] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Removing draft for document |"
                + wcDocRef.toString() + "|");

        if (session.exists(wcDocRef)) {
            DocumentModel wcDoc = session.getDocument(wcDocRef);

            // TODO Txekeo honeik beharrezkoak die???
            if (wcDoc.isVersion() || wcDoc.isProxy()) {
                throw new EloraException("Document is not a working copy.");
            }

            // Remove the draft
            DocumentModel draftDoc = getDraftForDocument(session, wcDoc,
                    session.getPrincipal().getName(), true);
            session.removeDocument(draftDoc.getRef());

            cleanTempDocument(wcDoc);

        }

        log.trace(logInitMsg + "Draft removed.");
    }

    private void cleanTempDocument(DocumentModel wcDoc) {
        CoreSession session = wcDoc.getCoreSession();
        if (wcDoc.isFolder()) {
            // If it is in Temp, remove the folder (twice to remove it
            // completely, instead of sending it to the Trash folder
            DocumentModel parentDoc = session.getDocument(wcDoc.getParentRef());
            if (parentDoc.getType().equals(EloraDoctypeConstants.TEMP_FOLDER)) {
                session.removeDocument(wcDoc.getRef());
            }
        } else {
            // If it is precreated, remove the document
            if (wcDoc.getCurrentLifeCycleState().equals(
                    EloraLifeCycleConstants.PRECREATED)) {
                session.removeDocument(wcDoc.getRef());
            }
        }
    }

    @Override
    public void copyDraftDataAndRemoveIt(CoreSession session,
            DocumentModel destinationDoc) throws EloraException {
        DocumentModel draftDoc = getDraftForDocument(session, destinationDoc,
                session.getPrincipal().getName(), true);

        // Copy all properties from draft to working copy document
        EloraDocumentHelper.copyProperties(draftDoc, destinationDoc,
                excludedPropertiesFromCopy);

        // Remove draft document
        session.removeDocument(draftDoc.getRef());
    }

    @Override
    public Map<String, List<String>> getExcludedPropertiesFromCopy() {
        return excludedPropertiesFromCopy;
    }

}