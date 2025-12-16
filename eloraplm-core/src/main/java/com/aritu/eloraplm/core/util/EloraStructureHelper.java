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
package com.aritu.eloraplm.core.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraDocumentEventNames;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * Elora Structure helper class.
 *
 * @author aritu
 *
 */
public class EloraStructureHelper {

    private static final Log log = LogFactory.getLog(
            EloraStructureHelper.class);

    /**
     *
     * @param structureRootRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getWsRootDocModel(DocumentRef structureRootRef,
            CoreSession session) throws EloraException {

        if (structureRootRef == null) {
            throw new EloraException("structureRootRef cannot be null.");
        }

        DocumentModel wsRootDocModel = getChildrenByType(structureRootRef,
                NuxeoDoctypeConstants.WORKSPACE_ROOT, session);

        return wsRootDocModel;
    }

    /**
     *
     * @param structureRootRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getWsRootPath(DocumentRef structureRootRef,
            CoreSession session) throws EloraException {

        DocumentModel wsRootDocModel = getWsRootDocModel(structureRootRef,
                session);

        if (wsRootDocModel == null) {
            throw new EloraException(
                    "Workspace root doc model cannot be retrieved.");
        }

        return wsRootDocModel.getPathAsString();
    }

    /**
     *
     * @param structureRootRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getTempFolderDocModel(
            DocumentRef structureRootRef, CoreSession session)
            throws EloraException {

        if (structureRootRef == null) {
            throw new EloraException("structureRootRef cannot be null.");
        }

        DocumentModel tempFolderDocModel = getChildrenByType(structureRootRef,
                EloraDoctypeConstants.TEMP_FOLDER, session);

        return tempFolderDocModel;
    }

    /**
     *
     * @param structureRootRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getTempFolderPath(DocumentRef structureRootRef,
            CoreSession session) throws EloraException {

        DocumentModel tmpFolderDocModel = getTempFolderDocModel(
                structureRootRef, session);

        if (tmpFolderDocModel == null) {
            throw new EloraException(
                    "Temp folder doc model cannot be retrieved.");
        }

        return tmpFolderDocModel.getPathAsString();
    }

    /**
     *
     * @param realRef
     * @param type
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getChildrenByType(DocumentRef realRef,
            String type, CoreSession session) throws EloraException {

        if (realRef == null) {
            throw new EloraException("Parent realRef cannot be null.");
        }

        if (type == null) {
            throw new EloraException("Children type cannot be null.");
        }

        DocumentModelList documentObjects = session.getChildren(realRef, type);
        if (documentObjects.isEmpty()) {
            throw new EloraException("Children of type =|" + type
                    + "| cannot be retrieved from parent realRef= |"
                    + realRef.toString() + "|");
        }
        return documentObjects.get(0);
    }

    /**
     * Get the CAD document model of a document type, in the structure root path
     *
     * @param structureRootRef : The structure root path
     * @param type: Type of the document to search
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getCadDocModelByType(
            DocumentRef structureRootRef, String type, CoreSession session)
            throws EloraException {

        return getDocModelByType(structureRootRef, type,
                EloraDoctypeConstants.STRUCTURE_CAD_DOCS, session);

    }

    /**
     * Get the BOM document model of a document type, in the structure root path
     *
     * @param structureRootRef : The structure root path
     * @param type: Type of the document to search
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getBomDocModelByType(
            DocumentRef structureRootRef, String type, CoreSession session)
            throws EloraException {

        return getDocModelByType(structureRootRef, type,
                EloraDoctypeConstants.STRUCTURE_EBOM, session);

    }

    /**
     * Get the document model of a document type, in the structure root path
     *
     * @param structureRootRef : The structure root path
     * @param type: Type of the document to search
     * @param structDoc: Type of the parent document
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getDocModelByType(DocumentRef structureRootRef,
            String type, String structDoc, CoreSession session)
            throws EloraException {

        if (type == null) {
            throw new IllegalArgumentException("null type");
        }

        DocumentModel documentsFolder = getChildrenByType(structureRootRef,
                structDoc, session);

        if (documentsFolder == null) {
            throw new IllegalArgumentException("null documentsRef");
        }
        String parentType = EloraConfig.autocopyParentTypesMap.get(type);
        if (parentType == null) {
            return getChildrenByType(documentsFolder.getRef(),
                    EloraDoctypeConstants.OTHER_CAD_DOCUMENTS_FOLDER, session);
        } else {
            return getChildrenByType(documentsFolder.getRef(), parentType,
                    session);
        }
    }

    public static DocumentModel getDocModelByType(DocumentRef structureRootRef,
            String type, CoreSession session) throws EloraException {

        Set<String> facets = EloraDocumentTypesHelper.getFacetsByDocumentType(
                type);

        if (facets.contains(EloraFacetConstants.FACET_CAD_DOCUMENT)
                || facets.contains(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {

            return EloraStructureHelper.getCadDocModelByType(structureRootRef,
                    type, session);

        } else if (facets.contains(EloraFacetConstants.FACET_BOM_DOCUMENT)) {

            return EloraStructureHelper.getBomDocModelByType(structureRootRef,
                    type, session);

        } else if (facets.contains(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            return EloraStructureHelper.getWsRootDocModel(structureRootRef,
                    session);
        } else {
            log.error("type = |" + type + "| has not defined right facet.");
            throw new EloraException("UndefinedFacetForDocType");
        }

    }

    /**
     * Retrieves the path of the CAD document in function of the specified
     * structure root and CAD document type.
     *
     * @param structureRootRef
     * @param type
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getCadPathByType(DocumentRef structureRootRef,
            String type, CoreSession session) throws EloraException {
        return getPathByType(structureRootRef, type,
                EloraDoctypeConstants.STRUCTURE_CAD_DOCS, session);

    }

    public static String getBomPathByType(DocumentRef structureRootRef,
            String type, CoreSession session) throws EloraException {
        return getPathByType(structureRootRef, type,
                EloraDoctypeConstants.STRUCTURE_EBOM, session);

    }

    /**
     * Retrieves the path of a document in function of the specified structure
     * root and document type.
     *
     * @param structureRootRef
     * @param type
     * @param structType
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getPathByType(DocumentRef structureRootRef,
            String type, String structType, CoreSession session)
            throws EloraException {

        DocumentModel docModel = getStructureDocumentByType(structureRootRef,
                type, structType, session);

        return docModel.getPathAsString();
    }

    public static DocumentModel getStructureDocumentByType(
            DocumentRef structureRootRef, String type, String structType,
            CoreSession session) throws EloraException {
        DocumentModel docModel = getDocModelByType(structureRootRef, type,
                structType, session);

        if (docModel == null) {
            throw new EloraException("Document path cannot be retrieved.");
        }
        return docModel;
    }

    /**
     * Returns the first parent document which type is the specified one.
     *
     * @param doc
     * @param type
     * @return
     */
    public static DocumentModel getParent(DocumentModel doc, String type) {
        CoreSession session = doc.getCoreSession();
        DocumentModel parent = session.getParentDocument(doc.getRef());
        while (parent != null && !type.equals(parent.getType())) {
            parent = session.getParentDocument(parent.getRef());
        }
        if (parent == null) {
            return null;
        }
        return parent;
    }

    /**
     *
     * @param doc
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getEloraRootFolderUid(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel eloraRootFolder = null;
        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.hasFacet(EloraFacetConstants.FACET_ELORA_ROOT_FOLDER)) {
                eloraRootFolder = doc;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }
        if (eloraRootFolder == null) {
            throw new EloraException(
                    "Elora Root Folder document does not exist");
        }

        return eloraRootFolder.getId();
    }

    public static DocumentModel getWorkableDomainChildDocModel(
            DocumentModel doc, CoreSession session) throws EloraException {
        DocumentModel structRootDoc = null;

        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.hasFacet(EloraFacetConstants.FACET_WORKABLE_DOMAIN_CHILD)) {
                structRootDoc = doc;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }
        if (structRootDoc == null) {
            throw new EloraException("structure root document does not exist");
        }

        return structRootDoc;
    }

    public static DocumentModel getDocumentWorkspace(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel workspaceDoc = null;

        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
                workspaceDoc = doc;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }
        if (workspaceDoc == null) {
            throw new EloraException("workspace document does not exist");
        }

        return workspaceDoc;
    }

    public static boolean isDocUnderWorkspaceRootOrDocWithContent(
            DocumentModel doc) {
        boolean isUnderWsRootOrDocWithContent = false;
        CoreSession session = doc.getCoreSession();

        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.getType().equals(NuxeoDoctypeConstants.WORKSPACE_ROOT)
                    || doc.hasFacet(
                            EloraFacetConstants.FACET_DOCUMENT_WITH_CONTENT)) {
                isUnderWsRootOrDocWithContent = true;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }

        return isUnderWsRootOrDocWithContent;
    }

    public static boolean isDocUnderLibraryRoot(DocumentModel doc) {
        boolean isUnderWsRoot = false;
        CoreSession session = doc.getCoreSession();

        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.getType().equals(EloraDoctypeConstants.LIBRARY_ROOT)) {
                isUnderWsRoot = true;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }

        return isUnderWsRoot;
    }

    /////////////////////////////////////////////////////////////
    // TODO::: HURRENGO HIRU METODOEN IZENAK HOBETO AUKERATU
    /////////////////////////////////////////////////////////////
    public static DocumentModel moveDocToEloraStructureAndCreateProxyIfRequired(
            DocumentModel doc, DocumentModel parentDocument) {

        String logInitMsg = "[moveDocToEloraStructureAndCreateProxyIfRequired] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        // Retrieve which actions will be performed in the document related to
        // the Elora Structure.
        // For the instance, actions are:
        // - Move the document to another position in Elora Sctructure
        // - Is Vault Folder Metadata Changed
        // - Create a proxy of the document
        EloraStructureHelper.DocumentActionsInEloraStructure documentActions = EloraStructureHelper.getDocumentActionsInEloraStructure(
                doc, parentDocument);

        if (documentActions.getMoveDocument()) {
            DocumentModel returnDoc = EloraStructureHelper.moveToEloraStructure(
                    doc, documentActions.isVaultFolderMetadataChanged());

            if (returnDoc != null && documentActions.getCreateProxy()) {
                DocumentRef initialParentRef = doc.getParentRef();
                returnDoc = doc.getCoreSession().createProxy(doc.getRef(),
                        initialParentRef);
                log.trace(logInitMsg + "Proxy created for document |"
                        + doc.getId() + "| in |" + parentDocument.getId()
                        + "|.");
            }

            return returnDoc != null ? returnDoc : doc;
        }

        return doc;
    }

    /**
     * Check if the new document has to be moved to another position in Elora
     * Structure. There are two main reasons to move a document during creation:
     * <p>
     * 1) New document is being created under a workspace or
     * <p>
     * 2) New document vault folder metadata is different from document current
     * location
     *
     * @param document
     * @param parentDoc
     * @return
     */
    private static DocumentActionsInEloraStructure getDocumentActionsInEloraStructure(
            DocumentModel document, DocumentModel parentDoc) {
        String logInitMsg = "[getDocumentActionsInEloraStructure] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        boolean moveDocument = false;
        boolean vaultFolderMetadataChanged = false;
        boolean createProxy = false;

        if (!document.isProxy()) {
            if (document.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                    || document.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                    || document.hasFacet(
                            EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                if (EloraStructureHelper.isDocUnderWorkspaceRootOrDocWithContent(
                        parentDoc)) {
                    log.trace(logInitMsg + "Document |" + document.getId()
                            + "| is basic, cad or item and parent doc is a workspace.");
                    moveDocument = true;
                    createProxy = true;
                }
            }
            if (document.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                    && hasVaultFolderMetadataBeenChanged(document)) {
                moveDocument = true;
                vaultFolderMetadataChanged = true;
            }
        }

        EloraStructureHelper.DocumentActionsInEloraStructure booleanPairTest = new EloraStructureHelper.DocumentActionsInEloraStructure(
                moveDocument, vaultFolderMetadataChanged, createProxy);

        log.trace(logInitMsg + "--- EXIT with moveDocument = |" + moveDocument
                + "|, vaultFolderMetadataChanged = |"
                + vaultFolderMetadataChanged + "| and createProxy = |"
                + createProxy + "| ---");

        return booleanPairTest;
    }

    public static class DocumentActionsInEloraStructure {
        private boolean moveDocument;

        private boolean vaultFolderMetadataChanged;

        private boolean createProxy;

        public DocumentActionsInEloraStructure(boolean moveDocument,
                boolean vaultFolderMetadataChanged, boolean createProxy) {
            this.moveDocument = moveDocument;
            this.vaultFolderMetadataChanged = vaultFolderMetadataChanged;
            this.createProxy = createProxy;
        }

        public boolean getMoveDocument() {
            return moveDocument;
        }

        public boolean isVaultFolderMetadataChanged() {
            return vaultFolderMetadataChanged;
        }

        public boolean getCreateProxy() {
            return createProxy;
        }
    }

    /**
     * Move the specified document to its position in Elora Structure.
     *
     * @param document
     * @return
     */
    public static DocumentModel moveToEloraStructure(DocumentModel document,
            boolean vaultFolderMetadataChanged) {
        String logInitMsg = "[moveToEloraStructure] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel returnDoc = null;
        PathRef targetDocPath = null;

        try {
            CoreSession documentManager = document.getCoreSession();

            targetDocPath = obtainTargetDocPath(document,
                    vaultFolderMetadataChanged, documentManager);

            if (targetDocPath != null
                    && documentManager.exists(targetDocPath)) {
                returnDoc = documentManager.move(document.getRef(),
                        targetDocPath, document.getName());
                log.trace(logInitMsg + "Document |" + document.getId()
                        + "| moved to |" + targetDocPath.toString() + "|.");
            }

        } catch (NuxeoException | EloraException e) {
            log.error(
                    logInitMsg + "Exception moving the document |"
                            + document.getId() + "| to |"
                            + targetDocPath.toString() + "|:" + e.getMessage(),
                    e);
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return returnDoc;
    }

    private static PathRef obtainTargetDocPath(DocumentModel doc,
            boolean vaultFolderMetadataChanged, CoreSession session)
            throws EloraException {
        String docPath = null;
        DocumentModel structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(
                doc, session);

        if (vaultFolderMetadataChanged) {
            String targetDocId = (String) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER);
            if (targetDocId != null) {
                DocumentModel targetDoc = session.getDocument(
                        new IdRef(targetDocId));
                docPath = targetDoc.getPathAsString();
            }
        }

        if (docPath == null) {
            if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                docPath = EloraStructureHelper.getBomPathByType(
                        structureRoot.getRef(), doc.getType(), session);
            } else {
                docPath = EloraStructureHelper.getCadPathByType(
                        structureRoot.getRef(), doc.getType(), session);
            }
        }

        return new PathRef(docPath);
    }

    // TODO LEIRE::: hau ere begiratu
    private static PathRef obtainTargetDocPathInEloraRootFolder(
            DocumentRef targetEloraRootFolderRef, DocumentModel doc,
            CoreSession session) throws EloraException {
        String docPath = null;

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            docPath = EloraStructureHelper.getBomPathByType(
                    targetEloraRootFolderRef, doc.getType(), session);
        } else if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                || doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)) {
            docPath = EloraStructureHelper.getCadPathByType(
                    targetEloraRootFolderRef, doc.getType(), session);
        } else if (doc.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {

            docPath = EloraStructureHelper.getWsRootPath(
                    targetEloraRootFolderRef, session);

        }

        return new PathRef(docPath);
    }

    ////////////////////////////////////////////////////////////////
    // Helper methods related to Vault Folders
    ////////////////////////////////////////////////////////////////

    public static boolean hasVaultFolderMetadataBeenChanged(DocumentModel doc) {
        boolean hasBeenChanged = false;

        CoreSession session = doc.getCoreSession();
        DocumentModel parentDoc = session.getDocument(doc.getParentRef());
        String vaultFolder;

        if (doc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER) != null) {
            vaultFolder = (String) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER);
            if (!vaultFolder.equals(parentDoc.getId())) {
                hasBeenChanged = true;
            }
        } else if (parentDoc.getType().equals(
                EloraDoctypeConstants.OTHER_DOC_VAULT_FOLDER)) {
            hasBeenChanged = true;
        }

        return hasBeenChanged;
    }

    /**
     * This method updates the vault folder metadata value, in function of the
     * destination folder. It is called after doing a move action.
     *
     * @param doc
     * @param destinationFolder
     * @throws EloraException
     */
    public static void updateVaultFolderMetadataByDestinationFolder(
            DocumentModel doc, DocumentModel destinationFolder)
            throws EloraException {
        String currentVaultFolder = null;
        if (doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                && doc.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER) != null) {
            currentVaultFolder = (String) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER);
        }

        String newVaultFolder = null;
        if (destinationFolder.getType().equals(
                EloraDoctypeConstants.OTHER_DOC_VAULT_FOLDER)) {
            newVaultFolder = destinationFolder.getId();
        }

        if (currentVaultFolder != null || newVaultFolder != null) {
            doc.setPropertyValue(EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER,
                    newVaultFolder);
            EloraDocumentHelper.disableVersioningDocument(doc);
            doc = doc.getCoreSession().saveDocument(doc);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Methods related with Switch Elora Root Folder functionality
    ////////////////////////////////////////////////////////////////

    public static DocumentModel switchSingleDocumentEloraRootFolder(
            DocumentModel doc, DocumentRef targetEloraRootFolderRef,
            DocumentRef switchOriginDocRef) throws EloraException {

        String logInitMsg = "[switchSingleDocumentEloraRootFolder] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        log.trace(logInitMsg + "Document to switch: id = |" + doc.getId()
                + "|, name = |" + doc.getName() + "|");

        CoreSession documentManager = doc.getCoreSession();

        PathRef targetDocPath = obtainTargetDocPathInEloraRootFolder(
                targetEloraRootFolderRef, doc, documentManager);

        if (targetDocPath != null && documentManager.exists(targetDocPath)) {

            // If the document is a basic document and its vault folder value is
            // not empty, empty its value.
            if (doc.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                    && doc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER) != null) {

                doc.setPropertyValue(
                        EloraMetadataConstants.ELORA_ELO_VAULT_FOLDER, null);

                EloraDocumentHelper.disableVersioningDocument(doc);
                doc = documentManager.saveDocument(doc);
            }

            doc = documentManager.move(doc.getRef(), targetDocPath,
                    doc.getName());
        }

        documentManager.save();

        log.trace(logInitMsg + "document switched.");

        // Nuxeo Event
        String comment = "Switched document :" + switchOriginDocRef.reference();
        EloraEventHelper.fireEvent(
                EloraDocumentEventNames.DOCUMENT_ELORA_ROOT_FOLDER_SWITCHED,
                doc, comment);

        log.trace(logInitMsg + "--- EXIT ---");
        return doc;
    }

    public static DocumentModel switchDocumentListEloraRootFolder(
            DocumentModelList docList, DocumentRef targetEloraRootFolderRef,
            DocumentRef switchOriginDocRef, CoreSession documentManager)
            throws EloraException {

        String logInitMsg = "[switchDocumentListEloraRootFolder] ["
                + documentManager.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel switchOriginDoc = documentManager.getDocument(
                switchOriginDocRef);

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (docList != null && docList.size() > 0) {

                for (DocumentModel doc : docList) {

                    doc = switchSingleDocumentEloraRootFolder(doc,
                            targetEloraRootFolderRef, switchOriginDocRef);

                    if (doc.getId().equals(switchOriginDocRef.reference())) {
                        switchOriginDoc = doc;
                    }
                }
            }
        } catch (Exception e) {
            String message = "Exception switching Elora Root Folder for specified document list.";
            log.error(
                    logInitMsg + message + " Exception details: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);

            TransactionHelper.setTransactionRollbackOnly();

            throw new EloraException(message, e);

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return switchOriginDoc;
    }

    public static DocumentModelList retrieveDocListToBeSwitchedForBomDoc(
            DocumentModel doc, boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef, boolean recursiveSwitch)
            throws EloraException {

        String logInitMsg = "[retrieveDocListToBeSwitchedForBomDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER with recursiveSwitch = |"
                + recursiveSwitch + "|---");

        DocumentModelList docListToBeSwitched = new DocumentModelListImpl();

        retrieveRelatedDocumentsToBeSwitchedForBomDoc(docListToBeSwitched, doc,
                originRootFolderIsLibraryRoot, targetEloraRootFolderRef,
                recursiveSwitch);

        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");

        return docListToBeSwitched;
    }

    private static void retrieveRelatedDocumentsToBeSwitchedForBomDoc(
            DocumentModelList docListToBeSwitched, DocumentModel doc,
            boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef, boolean recursiveSwitch)
            throws EloraException {

        String logInitMsg = "[retrieveRelatedDocumentsToBeSwitchedForBomDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(
                logInitMsg + "--- ENTER --- with docListToBeSwitched.size() = "
                        + docListToBeSwitched.size() + "|");

        // Hierarchical related documents (recursive ones)
        DocumentModelList hierarchicalRelatedDocuments = new DocumentModelListImpl();
        hierarchicalRelatedDocuments.addAll(RelationHelper.getObjectDocuments(
                doc, new ResourceImpl(EloraRelationConstants.BOM_COMPOSED_OF)));

        log.trace(logInitMsg + "hierarchicalRelatedDocuments.size() = "
                + hierarchicalRelatedDocuments.size() + "|");

        // Hierarchical relations
        if (!hierarchicalRelatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : hierarchicalRelatedDocuments) {

                if (hastToBeSwitchedRelatedElement(relatedDoc,
                        originRootFolderIsLibraryRoot,
                        targetEloraRootFolderRef)) {
                    addToDocListToBeSwitchedIfNotAlreadyContained(
                            docListToBeSwitched, relatedDoc);

                    if (recursiveSwitch) {
                        // For each related document, retrieve RECURSIVELY its
                        // hierarchically related documents.
                        retrieveRelatedDocumentsToBeSwitchedForBomDoc(
                                docListToBeSwitched, relatedDoc,
                                originRootFolderIsLibraryRoot,
                                targetEloraRootFolderRef, recursiveSwitch);
                    }
                }
            }
        }
        // One level relations
        retrieveOneLevelRelatedDocumentsToBeSwitchedForBomDoc(
                docListToBeSwitched, doc, originRootFolderIsLibraryRoot,
                targetEloraRootFolderRef);

        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");
    }

    private static void retrieveOneLevelRelatedDocumentsToBeSwitchedForBomDoc(
            DocumentModelList docListToBeSwitched, DocumentModel doc,
            boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef) throws EloraException {
        String logInitMsg = "[retrieveOneLevelRelatedDocumentsToBeSwitchedForBomDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(
                logInitMsg + "--- ENTER --- with docListToBeSwitched.size() = "
                        + docListToBeSwitched.size() + "|");

        DocumentModelList oneLevelRelatedDocuments = new DocumentModelListImpl();

        oneLevelRelatedDocuments.addAll(
                RelationHelper.getObjectDocuments(doc, new ResourceImpl(
                        EloraRelationConstants.BOM_HAS_SPECIFICATION)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT)));

        log.trace(logInitMsg + "oneLevelRelatedDocuments.size() = "
                + oneLevelRelatedDocuments.size() + "|");

        if (!oneLevelRelatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : oneLevelRelatedDocuments) {
                if (hastToBeSwitchedRelatedElement(relatedDoc,
                        originRootFolderIsLibraryRoot,
                        targetEloraRootFolderRef)) {
                    addToDocListToBeSwitchedIfNotAlreadyContained(
                            docListToBeSwitched, relatedDoc);
                }
            }
        }
        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");
    }

    public static DocumentModelList retrieveDocListToBeSwitchedForCadDoc(
            DocumentModel doc, boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef, boolean recursiveSwitch)
            throws EloraException {

        String logInitMsg = "[retrieveDocListToBeSwitchedForCadDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER with recursiveSwitch = |"
                + recursiveSwitch + "|---");

        DocumentModelList docListToBeSwitched = new DocumentModelListImpl();

        retrieveRelatedDocumentsToBeSwitchedForCadDoc(docListToBeSwitched, doc,
                originRootFolderIsLibraryRoot, targetEloraRootFolderRef,
                recursiveSwitch);

        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");

        return docListToBeSwitched;
    }

    private static void retrieveRelatedDocumentsToBeSwitchedForCadDoc(
            DocumentModelList docListToBeSwitched, DocumentModel doc,
            boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef, boolean recursiveSwitch)
            throws EloraException {

        String logInitMsg = "[retrieveRelatedDocumentsToBeSwitchedForCadDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(
                logInitMsg + "--- ENTER --- with docListToBeSwitched.size() = "
                        + docListToBeSwitched.size() + "|");

        // Hierarchical related documents (recursive ones)
        DocumentModelList hierarchicalRelatedDocuments = new DocumentModelListImpl();
        hierarchicalRelatedDocuments.addAll(RelationHelper.getObjectDocuments(
                doc, new ResourceImpl(EloraRelationConstants.CAD_BASED_ON)));
        hierarchicalRelatedDocuments.addAll(RelationHelper.getObjectDocuments(
                doc, new ResourceImpl(EloraRelationConstants.CAD_COMPOSED_OF)));

        log.trace(logInitMsg + "hierarchicalRelatedDocuments.size() = "
                + hierarchicalRelatedDocuments.size() + "|");

        // Hierarchical relations
        if (!hierarchicalRelatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : hierarchicalRelatedDocuments) {

                if (hastToBeSwitchedRelatedElement(relatedDoc,
                        originRootFolderIsLibraryRoot,
                        targetEloraRootFolderRef)) {
                    addToDocListToBeSwitchedIfNotAlreadyContained(
                            docListToBeSwitched, relatedDoc);

                    if (recursiveSwitch) {
                        // For each related document, retrieve RECURSIVELY its
                        // hierarchically related documents.
                        retrieveRelatedDocumentsToBeSwitchedForCadDoc(
                                docListToBeSwitched, relatedDoc,
                                originRootFolderIsLibraryRoot,
                                targetEloraRootFolderRef, recursiveSwitch);
                    }
                }
            }
        }
        // One level relations
        retrieveOneLevelRelatedDocumentsToBeSwitchedForCadDoc(
                docListToBeSwitched, doc, originRootFolderIsLibraryRoot,
                targetEloraRootFolderRef);

        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");
    }

    private static void retrieveOneLevelRelatedDocumentsToBeSwitchedForCadDoc(
            DocumentModelList docListToBeSwitched, DocumentModel doc,
            boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef) throws EloraException {

        String logInitMsg = "[retrieveOneLevelRelatedDocumentsToBeSwitchedForCadDoc] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(
                logInitMsg + "--- ENTER --- with docListToBeSwitched.size() = "
                        + docListToBeSwitched.size() + "|");

        DocumentModelList oneLevelRelatedDocuments = new DocumentModelListImpl();

        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.CAD_HAS_DESIGN_TABLE)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF), doc));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.CAD_HAS_SUPPRESSED)));
        oneLevelRelatedDocuments.addAll(
                RelationHelper.getObjectDocuments(doc, new ResourceImpl(
                        EloraRelationConstants.CAD_HAS_EXCLUDED_FROM_COMPOSITION)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.CAD_IN_CONTEXT_WITH)));
        oneLevelRelatedDocuments.addAll(RelationHelper.getObjectDocuments(doc,
                new ResourceImpl(EloraRelationConstants.CAD_HAS_DOCUMENT)));

        log.trace(logInitMsg + "oneLevelRelatedDocuments.size() = "
                + oneLevelRelatedDocuments.size() + "|");

        if (!oneLevelRelatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : oneLevelRelatedDocuments) {
                if (hastToBeSwitchedRelatedElement(relatedDoc,
                        originRootFolderIsLibraryRoot,
                        targetEloraRootFolderRef)) {
                    addToDocListToBeSwitchedIfNotAlreadyContained(
                            docListToBeSwitched, relatedDoc);
                }
            }
        }
        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");
    }

    private static boolean hastToBeSwitchedRelatedElement(DocumentModel doc,
            boolean originRootFolderIsLibraryRoot,
            DocumentRef targetEloraRootFolderRef) throws EloraException {

        String logInitMsg = "[hastToBeSwitchedRelatedElement] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "current doc: id = |" + doc.getId() + "|");

        boolean hasToBeSwitched = false;

        if (originRootFolderIsLibraryRoot || !isDocUnderLibraryRoot(doc)) {

            String docEloraRootFolderUid = EloraStructureHelper.getEloraRootFolderUid(
                    doc, doc.getCoreSession());

            if (!docEloraRootFolderUid.equals(
                    targetEloraRootFolderRef.reference())) {
                hasToBeSwitched = true;
            } else {
                log.trace(logInitMsg + "current doc: id = |" + doc.getId()
                        + "| don't added since it is already under targetEloraRootFolderRef = |"
                        + targetEloraRootFolderRef.reference() + "|.");
            }

        } else {
            log.trace(logInitMsg + "current doc: id = |" + doc.getId()
                    + "| don't added since it is under library root.");
        }

        log.trace(logInitMsg + "--- EXIT --- with hasToBeSwitched = |"
                + hasToBeSwitched + "|");

        return hasToBeSwitched;
    }

    private static void addToDocListToBeSwitchedIfNotAlreadyContained(
            DocumentModelList docListToBeSwitched, DocumentModel doc) {

        String logInitMsg = "[addToDocListToBeSwitchedIfNotAlreadyContained] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "current doc: id = |" + doc.getId() + "|");

        DocumentModel wcDoc;

        if (doc.isImmutable()) {
            wcDoc = doc.getCoreSession().getWorkingCopy(doc.getRef());
        } else {
            wcDoc = doc;
        }
        log.trace(logInitMsg + "current wcDoc: id = |" + wcDoc.getId() + "|");

        if (!docListToBeSwitched.contains(wcDoc)) {
            docListToBeSwitched.add(wcDoc);
            log.trace(logInitMsg + "current wcDoc: id = |" + wcDoc.getId()
                    + "| added.");
        } else {
            log.trace(logInitMsg + "current wcDoc: id = |" + wcDoc.getId()
                    + "| don't added since it is already contained in the list.");
        }

        log.trace(
                logInitMsg + "--- EXIT --- with docListToBeSwitched.size() = |"
                        + docListToBeSwitched.size() + "|");
    }

    public static Map<String, String> getVaultFoldersMap(DocumentModel doc)
            throws EloraException {
        String logInitMsg = "[getVaultFoldersMap] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        Map<String, String> vaultFoldersMap = new LinkedHashMap<String, String>();

        CoreSession session = doc.getCoreSession();

        String eloraRootFolderUid = EloraStructureHelper.getEloraRootFolderUid(
                doc, session);

        DocumentModel eloraRootFolder = session.getDocument(
                new IdRef(eloraRootFolderUid));

        if (eloraRootFolder != null) {

            if (EloraDocumentHelper.isDocumentUnderTemplateRoot(eloraRootFolder,
                    session)) {

                String sourceRootFolderUid = (String) eloraRootFolder.getPropertyValue(
                        NuxeoMetadataConstants.NX_DC_SOURCE);

                if (sourceRootFolderUid != null
                        && sourceRootFolderUid.length() > 0) {

                    DocumentModel sourceEloraRootFolder = session.getDocument(
                            new IdRef(sourceRootFolderUid));

                    if (sourceEloraRootFolder != null) {
                        eloraRootFolder = sourceEloraRootFolder;
                    } else {
                        log.error(logInitMsg
                                + "No document found for sourceRootFolderUid = |"
                                + sourceRootFolderUid + "|");
                    }
                } else {
                    log.error(logInitMsg + "|"
                            + NuxeoMetadataConstants.NX_DC_SOURCE
                            + "| metadata value is empty for document |"
                            + eloraRootFolder.getId() + "|");
                }
            }

            String vaultFolderQuery = EloraQueryFactory.getOtherDocumentsVaultFoldersInsideAncestorQuery(
                    eloraRootFolder.getId(),
                    NuxeoMetadataConstants.NX_DC_TITLE);

            DocumentModelList vaultFolders = session.query(vaultFolderQuery);

            if (vaultFolders != null && vaultFolders.size() > 0) {
                for (DocumentModel vaultFolder : vaultFolders) {
                    // Check if the user has Write permission in the Vault
                    // Folder
                    if (session.hasPermission(vaultFolder.getRef(),
                            SecurityConstants.WRITE)) {
                        vaultFoldersMap.put(vaultFolder.getId(),
                                vaultFolder.getTitle());
                    }
                }
            }
        }

        return vaultFoldersMap;
    }

    public static DocumentModel getDocumentTemplatesFolder(
            DocumentModel domain) {
        CoreSession session = domain.getCoreSession();
        String logInitMsg = "[getDocumentTemplatesFolder] ["
                + session.getPrincipal().getName() + "] ";

        DocumentModel documentTemplatesFolder = null;
        String documentTemplatesFolderQuery = EloraQueryFactory.getDocumentTemplatesFolderQuery(
                domain.getId());
        DocumentModelList documentTemplatesFolders = session.query(
                documentTemplatesFolderQuery);

        if (documentTemplatesFolders.size() == 1) {
            documentTemplatesFolder = documentTemplatesFolders.get(0);
        } else if (documentTemplatesFolders.size() == 0) {
            String errorMsg = EloraDoctypeConstants.DOCUMENT_TEMPLATES
                    + " document missing in the structure.";
            log.error(logInitMsg + errorMsg);
        } else if (documentTemplatesFolders.size() > 1) {
            String errorMsg = "More than one "
                    + EloraDoctypeConstants.DOCUMENT_TEMPLATES
                    + " documents found in the structure. There should be only one.";
            log.error(logInitMsg + errorMsg);
        }

        return documentTemplatesFolder;
    }

}
