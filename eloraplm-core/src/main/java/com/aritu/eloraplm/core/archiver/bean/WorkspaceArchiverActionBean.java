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
package com.aritu.eloraplm.core.archiver.bean;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.runtime.api.Framework;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.core.archiver.api.WorkspaceArchiverService;
import com.aritu.eloraplm.exceptions.ArchivingConditionsNotMetException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
@Name("workspaceArchiverActions")
@Scope(ScopeType.CONVERSATION)
public class WorkspaceArchiverActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            WorkspaceArchiverActionBean.class);

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

    private static final String EVENT_DOCUMENT_CHILDREN_CHANGED = "documentChildrenChanged";

    private WorkspaceArchiverService was;

    private WorkspaceArchiverService getWorkspaceArchiverService() {
        if (was == null) {
            was = Framework.getService(WorkspaceArchiverService.class);
        }
        return was;
    }

    public void archive() {
        archive(navigationContext.getCurrentDocument());
    }

    public void archive(DocumentModel workspace) {

        String logInitMsg = "[archive] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Archiving workspace |" + workspace.getId()
                + "|...");

        try {

            was = Framework.getService(WorkspaceArchiverService.class);

            DocumentModel sourceParent = documentManager.getDocument(
                    workspace.getParentRef());

            workspace = was.archive(workspace);

            Events.instance().raiseEvent(EVENT_DOCUMENT_CHILDREN_CHANGED,
                    sourceParent);
            Events.instance().raiseEvent(EVENT_DOCUMENT_CHILDREN_CHANGED,
                    documentManager.getDocument(workspace.getParentRef()));
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

            navigateToWorkspace(workspace);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.archive.workspace"));

        } catch (ArchivingConditionsNotMetException e) {

            // TODO Arrazoia gehitu errorera, eta gero facesmessagesen
            // erakutsi?!

            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.archive.workspace.conditionsNotMet"));
            navigationContext.invalidateCurrentDocument();
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.archive.workspace"));
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.archive.workspace"));
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public boolean canBeArchived() {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (!currentDoc.hasFacet(EloraFacetConstants.FACET_ARCHIVABLE)) {
            return false;
        }

        DocumentModel parentDoc = documentManager.getDocument(
                currentDoc.getParentRef());
        if (!parentDoc.getType().equals(NuxeoDoctypeConstants.WORKSPACE_ROOT)) {
            return false;
        }

        if (getWorkspaceArchiverService().isArchiverDefinedForType(
                currentDoc.getType())) {
            return true;
        } else {
            return false;
        }
    }

    public void unarchive() {
        unarchive(navigationContext.getCurrentDocument());
    }

    public void unarchive(DocumentModel workspace) {

        String logInitMsg = "[unarchive] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Unarchiving workspace |" + workspace.getId()
                + "|...");

        try {

            was = Framework.getService(WorkspaceArchiverService.class);

            DocumentModel sourceParent = documentManager.getDocument(
                    workspace.getParentRef());

            workspace = was.unarchive(workspace);

            Events.instance().raiseEvent(EVENT_DOCUMENT_CHILDREN_CHANGED,
                    sourceParent);
            Events.instance().raiseEvent(EVENT_DOCUMENT_CHILDREN_CHANGED,
                    documentManager.getDocument(workspace.getParentRef()));
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

            navigateToWorkspace(workspace);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                    "eloraplm.message.success.unarchive.workspace"));

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.unarchive.workspace"));
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.unarchive.workspace"));
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    public boolean canBeUnarchived() {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (!currentDoc.hasFacet(EloraFacetConstants.FACET_ARCHIVABLE)) {
            return false;
        }

        DocumentModel parentDoc = documentManager.getDocument(
                currentDoc.getParentRef());
        if (parentDoc.getType().equals(NuxeoDoctypeConstants.WORKSPACE_ROOT)) {
            return false;
        }

        if (getWorkspaceArchiverService().isArchiverDefinedForType(
                currentDoc.getType())) {
            return true;
        } else {
            return false;
        }
    }

    private void navigateToWorkspace(DocumentModel workspace) {
        navigationContext.navigateToDocument(workspace);
        refreshUI();
    }

    private void refreshUI() {
        treeActions.reset();
        contentViewActions.refresh("document_content");
    }
}
