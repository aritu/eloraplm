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

package com.aritu.eloraplm.cm.container.archive.actions;

import java.io.Serializable;
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
import org.nuxeo.ecm.collections.jsf.actions.FavoritesActionBean;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.treetable.BomImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.DocImpactedItemsTreeBean;
import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.container.archive.util.ContainerArchiveHelper;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("archiveEcoAction")
@Scope(ScopeType.CONVERSATION)
public class ArchiveEcoActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ArchiveEcoActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected TreeActionsBean treeActions;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected FavoritesActionBean favoritesActions;

    @In(create = true)
    protected transient DocImpactedItemsTreeBean cmDocImpactedItemsTreeBean;

    @In(create = true)
    protected transient BomImpactedItemsTreeBean cmBomImpactedItemsTreeBean;

    public void archiveECO() throws EloraException {
        String logInitMsg = "[archiveECO] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (isFullECOArchived()) {
                DocumentModel currentDoc = navigationContext.getCurrentDocument();

                // TODO: This helper must be a service. Try to configure so it
                // can work for all container types
                DocumentModel destinationFolder = ContainerArchiveHelper.archiveAndUnlock(
                        currentDoc, EloraDoctypeConstants.STRUCTURE_ARCHIVED,
                        EloraDoctypeConstants.FOLDER_ARCHIVED_CHANGE_MANAGEMENT,
                        documentManager);

                // TODO: This calls could change when we create the service
                // mentioned above
                DocumentModel sourceFolder = documentManager.getDocument(
                        currentDoc.getParentRef());

                Events.instance().raiseEvent(
                        ContainerArchiveHelper.DOCUMENT_CHILDREN_CHANGED,
                        sourceFolder);
                Events.instance().raiseEvent(
                        ContainerArchiveHelper.DOCUMENT_CHILDREN_CHANGED,
                        destinationFolder);

                removeFromFavorites();

                ContainerArchiveHelper.navigateToArchivedDoc(currentDoc,
                        documentManager, navigationContext, treeActions,
                        contentViewActions);

                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get("eloraplm.message.success.archiveECO"));
            }
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.archiveECO"));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private boolean isFullECOArchived() {
        try {
            checkFullECOIsArchived();
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private void checkFullECOIsArchived() throws EloraException {
        TreeNode docRoot = cmDocImpactedItemsTreeBean.getRoot();
        checkImpactedDocs(docRoot);
        TreeNode bomRoot = cmBomImpactedItemsTreeBean.getRoot();
        checkImpactedBoms(bomRoot);
    }

    private void checkImpactedDocs(TreeNode docRoot) throws EloraException {
        for (TreeNode childNode : docRoot.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (!nodeData.getIsManaged() || nodeData.getIsDirty()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.cadNotManagedOrDirty"));
                throw new EloraException(
                        "There are dirty or not managed CADs. Cannot archive ECO");
            }
            checkImpactedDocs(childNode);
        }
    }

    private void checkImpactedBoms(TreeNode bomRoot) throws EloraException {
        for (TreeNode childNode : bomRoot.getChildren()) {
            ImpactedItemsNodeData nodeData = (ImpactedItemsNodeData) childNode.getData();
            if (!nodeData.getIsManaged() || nodeData.getIsDirty()) {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.bomNotManagedOrDirty"));
                throw new EloraException(
                        "There are dirty or not managed BOMs. Cannot archive ECO");
            }
            checkImpactedBoms(childNode);
        }
    }

    public void activateEco() throws EloraException {
        String logInitMsg = "[activateEco] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");
        try {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            ContainerArchiveHelper.moveToWSRoot(currentDoc, documentManager);
            ContainerArchiveHelper.navigateToArchivedDoc(currentDoc,
                    documentManager, navigationContext, treeActions,
                    contentViewActions);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.activateECO"));
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.activateECO"));
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private void removeFromFavorites() {
        if (favoritesActions.canCurrentDocumentBeRemovedFromFavorites()) {
            favoritesActions.removeCurrentDocumentFromFavorites();
        }
    }

}
