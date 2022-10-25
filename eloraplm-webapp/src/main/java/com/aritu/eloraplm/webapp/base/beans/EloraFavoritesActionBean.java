/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package com.aritu.eloraplm.webapp.base.beans;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.4
 */
@Name("eloraFavoritesActions")
@Scope(ScopeType.EVENT)
public class EloraFavoritesActionBean {

    private static final Log log = LogFactory.getLog(
            EloraFavoritesActionBean.class);

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    // Override favoritesActions.canCurrentDocumentBeRemovedFromFavorites Nuxeo
    // method
    public boolean canCurrentDocumentBeRemovedFromFavorites() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            return canDocumentBeRemovedFromFavorites(currentDocument);
        }
        return false;
    }

    public boolean canDocumentBeRemovedFromFavorites(DocumentModel document) {
        String logInitMsg = "[canDocumentBeRemovedFromFavorites] ";

        if (document != null) {
            try {
                if (document.isProxy()) {
                    return false;
                }
                final FavoritesManager favoritesManager = Framework.getLocalService(
                        FavoritesManager.class);
                final CoreSession session = (CoreSession) Component.getInstance(
                        "documentManager", true);
                return favoritesManager.isFavorite(document, session);
            } catch (Exception e) {
                log.error(logInitMsg
                        + "Error checkinf if document can be removed from favorites.",
                        e);
            }
        }
        return false;
    }

    public void removeDocumentFromFavorites(DocumentModel document) {

        String logInitMsg = "[removeDocumentFromFavorites] ";
        log.trace(logInitMsg + "--- ENTER --- docId = |" + document.getId()
                + "|");
        if (document != null) {
            try {
                final FavoritesManager favoritesManager = Framework.getLocalService(
                        FavoritesManager.class);
                final CoreSession session = (CoreSession) Component.getInstance(
                        "documentManager", true);
                if (favoritesManager.isFavorite(document, session)) {
                    favoritesManager.removeFromFavorites(document, session);

                    Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED,
                            document);

                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get("favorites.removedFromFavorites"));

                    final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                            "navigationContext", true);
                    navigationContext.invalidateCurrentDocument();
                }
            } catch (Exception e) {
                log.error(
                        logInitMsg + "Error removing document from favorites.",
                        e);
            }
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

}
