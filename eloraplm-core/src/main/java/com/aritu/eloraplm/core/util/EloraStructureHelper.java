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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoFacetConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Elora Structure helper class.
 *
 * @author aritu
 *
 */
public class EloraStructureHelper {

    /**
     *
     * @param structureRootRealRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getWsRootDocModel(
            DocumentRef structureRootRealRef, CoreSession session)
            throws EloraException {

        if (structureRootRealRef == null) {
            throw new EloraException("structureRootRealRef cannot be null.");
        }

        DocumentModel wsRootDocModel = getChildrenByType(structureRootRealRef,
                NuxeoDoctypeConstants.WORKSPACE_ROOT, session);

        return wsRootDocModel;
    }

    /**
     *
     * @param structureRootRealRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getWsRootPath(DocumentRef structureRootRealRef,
            CoreSession session) throws EloraException {

        DocumentModel wsRootDocModel = getWsRootDocModel(structureRootRealRef,
                session);

        if (wsRootDocModel == null) {
            throw new EloraException(
                    "Workspace root doc model cannot be retrieved.");
        }

        return wsRootDocModel.getPathAsString();
    }

    /**
     *
     * @param structureRootRealRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getTempFolderDocModel(
            DocumentRef structureRootRealRef, CoreSession session)
            throws EloraException {

        if (structureRootRealRef == null) {
            throw new EloraException("structureRootRealRef cannot be null.");
        }

        DocumentModel tempFolderDocModel = getChildrenByType(
                structureRootRealRef, EloraDoctypeConstants.TEMP_FOLDER,
                session);

        return tempFolderDocModel;
    }

    /**
     *
     * @param structureRootRealRef
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getTempFolderPath(DocumentRef structureRootRealRef,
            CoreSession session) throws EloraException {

        DocumentModel tmpFolderDocModel = getTempFolderDocModel(
                structureRootRealRef, session);

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
     * @param structureRootRealRef : The structure root path
     * @param type: Type of the document to search
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getCadDocModelByType(
            DocumentRef structureRootRealRef, String type, CoreSession session)
            throws EloraException {

        return getDocModelByType(structureRootRealRef, type,
                EloraDoctypeConstants.STRUCTURE_CAD_DOCS, session);

    }

    /**
     * Get the BOM document model of a document type, in the structure root path
     *
     * @param structureRootRealRef : The structure root path
     * @param type: Type of the document to search
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getBomDocModelByType(
            DocumentRef structureRootRealRef, String type, CoreSession session)
            throws EloraException {

        return getDocModelByType(structureRootRealRef, type,
                EloraDoctypeConstants.STRUCTURE_EBOM, session);

    }

    /**
     * Get the document model of a document type, in the structure root path
     *
     * @param structureRootRealRef : The structure root path
     * @param type: Type of the document to search
     * @param structDoc: Type of the parent document
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getDocModelByType(
            DocumentRef structureRootRealRef, String type, String structDoc,
            CoreSession session) throws EloraException {

        if (type == null) {
            throw new IllegalArgumentException("null type");
        }

        DocumentModel documentsFolder = getChildrenByType(structureRootRealRef,
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

    /**
     * Retrieves the path of the CAD document in function of the specified
     * structure root and CAD document type.
     *
     * @param structureRootRealRef
     * @param type
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getCadPathByType(DocumentRef structureRootRealRef,
            String type, CoreSession session) throws EloraException {
        return getPathByType(structureRootRealRef, type,
                EloraDoctypeConstants.STRUCTURE_CAD_DOCS, session);

    }

    /**
     * Retrieves the path of the BOM document in function of the specified
     * structure root and BOM document type.
     *
     * @param structureRootRealRef
     * @param type
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getBomPathByType(DocumentRef structureRootRealRef,
            String type, CoreSession session) throws EloraException {
        return getPathByType(structureRootRealRef, type,
                EloraDoctypeConstants.STRUCTURE_EBOM, session);

    }

    /**
     * Retrieves the path of a document in function of the specified structure
     * root and document type.
     *
     * @param structureRootRealRef
     * @param type
     * @param structType
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getPathByType(DocumentRef structureRootRealRef,
            String type, String structType, CoreSession session)
            throws EloraException {

        DocumentModel docModel = getDocModelByType(structureRootRealRef, type,
                structType, session);

        if (docModel == null) {
            throw new EloraException("Document path cannot be retrieved.");
        }

        return docModel.getPathAsString();
    }

    /**
     *
     * @param doc
     * @param session
     * @return
     * @throws EloraException
     */

    // TODO: Mirar si se puede hacer mas rapido utilizando
    // documentManager.getSuperSpace(). Sin tener que dar tantas vueltas al
    // while
    public static String getStructureRootUid(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel structRootDoc = null;
        while (doc != null && !("/".equals(doc.getPathAsString()))) {
            if (doc.hasFacet(NuxeoFacetConstants.FACET_SUPER_SPACE)) {
                structRootDoc = doc;
                break;
            }
            doc = session.getDocument(doc.getParentRef());
        }
        if (structRootDoc == null) {
            throw new EloraException("structure root document does not exist");
        }

        return structRootDoc.getId();
    }

    // TODO: Mirar si se puede hacer mas rapido utilizando
    // documentManager.getRootDocument(). Sin tener que dar tantas vueltas al
    // while
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
}
