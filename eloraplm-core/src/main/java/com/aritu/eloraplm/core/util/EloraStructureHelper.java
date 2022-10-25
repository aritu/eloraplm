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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraDocumentEventNames;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

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

    public static boolean isDocUnderWorkspaceRoot(DocumentModel doc) {
        boolean isUnderWsRoot = false;
        CoreSession session = doc.getCoreSession();

        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.getType().equals(NuxeoDoctypeConstants.WORKSPACE_ROOT)) {
                isUnderWsRoot = true;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }

        return isUnderWsRoot;
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
    public static void moveDocToEloraStructureAndCreateProxyIfRequired(
            DocumentModel doc, DocumentModel parentDocument) {

        String logInitMsg = "[moveDocToEloraStructureAndCreateProxyIfRequired] ["
                + doc.getCoreSession().getPrincipal().getName() + "] ";

        // Check if it has to be moved to its position in Elora Structure.
        // If yes, move it to the Elora Structure and create a Proxy
        // pointing to the moved document.
        if (EloraStructureHelper.hasToBeMovedToEloraStructure(doc,
                parentDocument)) {
            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| has to be moved to the vault when creating under |"
                    + parentDocument.getId() + "|.");

            EloraStructureHelper.moveToEloraStructureAndCreateProxy(doc);

            log.trace(logInitMsg + "Document |" + doc.getId()
                    + "| moved to vault and created proxy in |"
                    + parentDocument.getId() + "|.");
        }
    }

    /**
     * Check if the new document has to be moved to its position in Elora
     * Structure.
     *
     * @param document
     * @param parentDoc
     * @return
     */
    private static boolean hasToBeMovedToEloraStructure(DocumentModel document,
            DocumentModel parentDoc) {
        String logInitMsg = "[hasToBeMovedToEloraStructure] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        boolean moveToEloraStructure = false;

        if (!document.isProxy()) {
            log.trace(logInitMsg + "Document |" + document.getId()
                    + "| is not a proxy.");
            if (document.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                    || document.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                    || document.hasFacet(
                            EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                if (EloraStructureHelper.isDocUnderWorkspaceRoot(parentDoc)) {
                    log.trace(logInitMsg + "Document |" + document.getId()
                            + "| is basic, cad or item and parent doc is a workspace.");
                    moveToEloraStructure = true;
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT with moveToEloraStructure = |"
                + moveToEloraStructure + "|---");

        return moveToEloraStructure;
    }

    /**
     * Move the specified document to its position in Elora Structure and create
     * a Proxy pointing to the moved document.
     *
     * @param document
     * @param documentManager
     * @return
     */
    private static DocumentModel moveToEloraStructureAndCreateProxy(
            DocumentModel document) {
        String logInitMsg = "[moveToEloraStructureAndCreateProxy] ["
                + document.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel returnDoc = null;
        PathRef targetDocPath = null;

        try {
            CoreSession documentManager = document.getCoreSession();

            targetDocPath = obtainTargetDocPath(document, documentManager);

            if (targetDocPath != null
                    && documentManager.exists(targetDocPath)) {
                DocumentRef initialParentRef = document.getParentRef();
                documentManager.move(document.getRef(), targetDocPath,
                        document.getName());
                returnDoc = documentManager.createProxy(document.getRef(),
                        initialParentRef);
            }

        } catch (NuxeoException | EloraException e) {
            log.error(logInitMsg + "Exception moving the document |"
                    + document.getId()
                    + "| to Elora Structure and Creating the proxy: "
                    + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");

        return returnDoc;
    }

    private static PathRef obtainTargetDocPath(DocumentModel doc,
            CoreSession session) throws EloraException {
        String docPath = null;

        DocumentModel structureRoot;
        structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(doc,
                session);

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            docPath = EloraStructureHelper.getBomPathByType(
                    structureRoot.getRef(), doc.getType(), session);
        } else {
            docPath = EloraStructureHelper.getCadPathByType(
                    structureRoot.getRef(), doc.getType(), session);
        }

        return new PathRef(docPath);
    }

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

    /////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////
    // Methods related with Switch Elora Root Folder functionality

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

    /////////////////////////////////////////////////////////////////
}
