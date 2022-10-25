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
package com.aritu.eloraplm.core.archiver.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.archiver.api.ArchiverDescriptor;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;

public class WorkspaceArchiverHelper {

    private static final Log log = LogFactory.getLog(
            WorkspaceArchiverHelper.class);

    private static FavoritesManager favoritesManager;

    public static DocumentModel getDestinationFolder(CoreSession session,
            DocumentModel workspace, ArchiverDescriptor archiver)
            throws EloraException {
        DocumentModel archivedFolder = getArchivedFolder(session, workspace);
        DocumentModelList l = session.getChildren(archivedFolder.getRef(),
                archiver.destinationFolder);
        if (l.size() == 0) {
            throw new EloraException(
                    "There is no |" + archiver.destinationFolder
                            + "| type folder under Archived.");
        }
        return l.get(0);
    }

    private static DocumentModel getArchivedFolder(CoreSession session,
            DocumentModel workspace) throws EloraException {
        DocumentModel domainChildDoc = EloraStructureHelper.getWorkableDomainChildDocModel(
                workspace, session);
        DocumentModelList l = session.getChildren(domainChildDoc.getRef(),
                EloraDoctypeConstants.STRUCTURE_ARCHIVED);
        if (l.size() == 0) {
            throw new EloraException("There is no Archived folder.");
        }
        return l.get(0);
    }

    public static void updateWorkingCopyProxies(CoreSession session,
            DocumentModel folderish) throws EloraException {
        String logInitMsg = "[updateWorkingCopyProxies] ["
                + session.getPrincipal().getName() + "] ";

        for (DocumentModel child : session.getChildren(folderish.getRef())) {
            if (child.isProxy()) {
                DocumentModel proxySource = session.getDocument(
                        new IdRef(child.getSourceId()));
                if (!proxySource.isVersion()) {
                    DocumentModel proxyBase = EloraDocumentHelper.getBaseVersion(
                            proxySource);
                    if (proxyBase == null) {
                        log.error(logInitMsg + "The document |"
                                + proxySource.getId()
                                + "| has no base version. Probably because it has no AVs.");
                    } else {
                        session.createProxy(proxyBase.getRef(),
                                folderish.getRef());
                        session.removeDocument(child.getRef());
                        session.save();
                    }
                }
            } else if (child.isFolder()) {
                updateWorkingCopyProxies(session, child);
            }
        }
    }

    public static void removeFromFavorites(CoreSession session,
            DocumentModel workspace) {
        if (getFavoritesManager().isFavorite(workspace, session)) {
            getFavoritesManager().removeFromFavorites(workspace, session);
        }
    }

    private static FavoritesManager getFavoritesManager() {
        if (favoritesManager == null) {
            favoritesManager = Framework.getLocalService(
                    FavoritesManager.class);
        }
        return favoritesManager;
    }

    public static DocumentModel getWorkspaceRootFolder(CoreSession session,
            DocumentModel workspace) throws EloraException {
        DocumentModel domainChildDoc = EloraStructureHelper.getWorkableDomainChildDocModel(
                workspace, session);
        DocumentModelList l = session.getChildren(domainChildDoc.getRef(),
                NuxeoDoctypeConstants.WORKSPACE_ROOT);
        if (l.size() == 0) {
            throw new EloraException("There is no WorkspaceRoot folder.");
        }
        return l.get(0);
    }

    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //

    // public static final String DOCUMENT_CHILDREN_CHANGED =
    // "documentChildrenChanged";
    //
    // private static final Log log = LogFactory.getLog(
    // WorkspaceArchiverHelper.class);

    // public static DocumentModel moveToWSRoot(DocumentModel doc,
    // CoreSession session) throws EloraException {
    //
    // DocumentModel sourceFolder = session.getDocument(doc.getParentRef());
    // DocumentModel destinationFolder = getDestinationWSRootFolder(doc,
    // session);
    //
    // new UnrestrictedArchiver(doc.getRef(), destinationFolder.getRef(),
    // session).runUnrestricted();
    //
    // Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED, sourceFolder);
    // Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED,
    // destinationFolder);
    //
    // // session.move(doc.getRef(), destinationFolder.getRef(), null);
    // return destinationFolder;
    // }

    // public static DocumentModel getDestinationWSRootFolder(
    // DocumentModel currentDoc, CoreSession session)
    // throws EloraException {
    // DocumentModel workableDomainChildDoc =
    // EloraStructureHelper.getWorkableDomainChildDocModel(
    // currentDoc, session);
    // DocumentModelList destinationFolderList = session.getChildren(
    // workableDomainChildDoc.getRef(),
    // NuxeoDoctypeConstants.WORKSPACE_ROOT);
    // return destinationFolderList.get(0);
    // }

    // public static DocumentModel getDestinationFolder(DocumentModel
    // currentDoc,
    // String destStructureFolder, String destFolder, CoreSession session)
    // throws EloraException {
    // DocumentModel cmFolder = getDestinationFolderByType(currentDoc,
    // destStructureFolder, session);
    // DocumentModelList destinationFolderList = session.getChildren(
    // cmFolder.getRef(), destFolder);
    // if (destinationFolderList.size() == 0) {
    // throw new EloraException(
    // "There is not |" + destFolder + "| type folder");
    // }
    // return destinationFolderList.get(0);
    // }

    // public static DocumentModel archiveAndUnlock(DocumentModel currentDoc,
    // String destStruct, String destStructFolder, CoreSession session)
    // throws EloraException {
    //
    // updateWorkingCopyProxies(currentDoc, session);
    //
    // DocumentModel destinationFolder = null;
    // if (destStructFolder == null) {
    // destinationFolder = getDestinationFolderByType(currentDoc,
    // destStruct, session);
    // } else {
    // destinationFolder = getDestinationFolder(currentDoc, destStruct,
    // destStructFolder, session);
    // }
    //
    // new UnrestrictedArchiver(currentDoc.getRef(),
    // destinationFolder.getRef(), session).runUnrestricted();
    //
    // return destinationFolder;
    // }

    // private static void updateWorkingCopyProxies(DocumentModel doc,
    // CoreSession session) throws EloraException {
    // String logInitMsg = "[updateWorkingCopyProxies] ["
    // + session.getPrincipal().getName() + "] ";
    //
    // for (DocumentModel child : session.getChildren(doc.getRef())) {
    // if (child.isProxy()) {
    // DocumentModel sourceDocument = session.getDocument(
    // new IdRef(child.getSourceId()));
    // if (!sourceDocument.isVersion()) {
    // DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(
    // sourceDocument);
    // if (baseDoc == null) {
    // log.error(logInitMsg + "The document |"
    // + sourceDocument.getId()
    // + "| has no base version. Probably because it has no AVs.");
    // } else {
    // session.createProxy(baseDoc.getRef(), doc.getRef());
    // session.removeDocument(child.getRef());
    // session.save();
    // }
    // }
    // } else if (child.isFolder()) {
    // updateWorkingCopyProxies(child, session);
    // }
    // }
    // }
    //
    // private static DocumentModel getDestinationFolderByType(
    // DocumentModel currentDoc, String docType, CoreSession session)
    // throws EloraException {
    // DocumentModel workableDomainChildDoc =
    // EloraStructureHelper.getWorkableDomainChildDocModel(
    // currentDoc, session);
    // DocumentModelList destinationFolderList = session.getChildren(
    // workableDomainChildDoc.getRef(), docType);
    //
    // if (destinationFolderList.size() == 0) {
    // throw new EloraException(
    // "There is not |" + docType + "| type folder");
    // }
    //
    // return destinationFolderList.get(0);
    // }

    // public static void navigateToArchivedDoc(DocumentModel doc,
    // CoreSession session, NavigationContext navigationContext,
    // TreeActionsBean treeActions,
    // ContentViewActions contentViewActions) {
    // navigationContext.navigateToDocument(session.getDocument(doc.getRef()));
    // refreshUI(treeActions, contentViewActions);
    // }

    // private static void refreshUI(TreeActionsBean treeActions,
    // ContentViewActions contentViewActions) {
    // treeActions.reset();
    // // TODO: Mirar constante de este contentview
    // contentViewActions.refresh("document_content");
    // }

}
