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

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.constants.CMDocTypeConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Elora Structure helper class.
 *
 * @author aritu
 *
 */
public class EloraStructureHelper {

    private static final String OTHER_CAD_DOCUMENTS_FOLDER = "FolderOtherDoc";

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
     * @param type
     * @return
     * @throws EloraException
     */
    private static String getParentTypeByDocType(String type)
            throws EloraException {

        if (type == null) {
            throw new EloraException("Document type cannot be null.");
        }

        String parentType = EloraConfigHelper.getAutocopyParentTypeConfig(type);
        return parentType;
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
        String parentType = getParentTypeByDocType(type);
        if (parentType == null) {
            return getChildrenByType(documentsFolder.getRef(),
                    OTHER_CAD_DOCUMENTS_FOLDER, session);
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
     * @param structureRootRealRef
     * @param cmProcessType
     * @param session
     * @return
     * @throws EloraException
     */
    public static DocumentModel getCMDocModelByProcessType(
            DocumentRef structureRootRealRef, String cmProcessType,
            CoreSession session) throws EloraException {

        if (structureRootRealRef == null) {
            throw new EloraException("structureRootRealRef cannot be null.");
        }

        // Retrieve Change Management folder path
        DocumentModel cmFolderDocModel = getChildrenByType(structureRootRealRef,
                CMDocTypeConstants.STRUCTURE_CM, session);
        if (cmFolderDocModel == null) {
            throw new EloraException(
                    "Change Mangement folder cannot be retrieved.");
        }

        // Retrieve the parent type in function of the CM process type
        if (cmProcessType == null) {
            throw new EloraException(
                    "The Change Management process type must be defined.");
        }
        String cmProcessParentType = getParentTypeByDocType(cmProcessType);
        if (cmProcessParentType == null) {
            throw new EloraException(
                    "Change Management process parent type cannot be retrieved.");
        }

        // Retrieve the Change Management document model
        DocumentModel docModel = getChildrenByType(cmFolderDocModel.getRef(),
                cmProcessParentType, session);
        if (docModel == null) {
            throw new EloraException(
                    "Change Management process folder cannot be retrieved.");
        }

        return docModel;
    }

    /**
     * Retrieves the path of the Change Management process in function of the
     * specified structure root and the process type.
     *
     * @param structureRootRealRef
     * @param cmProcessType
     * @param session
     * @return
     * @throws EloraException
     */
    public static String getCMPathByProcessType(
            DocumentRef structureRootRealRef, String cmProcessType,
            CoreSession session) throws EloraException {

        DocumentModel cmDocModel = getCMDocModelByProcessType(
                structureRootRealRef, cmProcessType, session);

        if (cmDocModel == null) {
            throw new EloraException(
                    "Change Management path cannot be retrieved.");
        }

        return cmDocModel.getPathAsString();
    }

    /**
     *
     * @param doc
     * @param session
     * @return
     * @throws EloraException
     */

    // TODO: Mirar si se puede hacer mas rapido utilizando
    // documentManager.getRootDocument(). Sin tener que dar tastas vueltas al
    // while
    public static String getStructureRootUid(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel structRootDoc = null;
        while (doc != null && !"/".equals(doc.getPath())) {
            if (doc.getType().equals(EloraDoctypeConstants.STRUCTURE_ROOT)) {
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

}
