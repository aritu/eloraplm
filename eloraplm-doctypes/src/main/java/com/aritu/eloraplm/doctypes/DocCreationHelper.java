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

package com.aritu.eloraplm.doctypes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

/**
 * @author aritu
 *
 */
public class DocCreationHelper {

    protected static Log log = LogFactory.getLog(DocCreationHelper.class);

    public static void executeInitialActions(CoreSession session,
            DocumentModel doc) {
        executeInitialActions(session, doc, false);
    }

    public static void executeInitialActions(CoreSession session,
            DocumentModel doc, boolean skipLock) {

        if (!skipLock) {
            // Lock document if it is required to edit
            if ((doc.isVersionable() || doc.hasFacet(
                    EloraFacetConstants.FACET_LOCK_REQUIRED_TO_EDIT))
                    && !doc.isLocked()) {
                doc.setLock();
            }
        }

        markAsTemplateIfRequired(session, doc);

        addDocToFavoritesIfRequired(session, doc);
    }

    private static void addDocToFavoritesIfRequired(CoreSession session,
            DocumentModel doc) {

        String logInitMsg = "[addDocToFavoritesIfRequired] ["
                + session.getPrincipal().getName() + "] ";

        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {

            boolean isTemplate = EloraDocumentHelper.isTemplate(doc);

            // If the document is a workspace and it is configured to add new
            // workspaces to favorites, add the document to favorites.
            if (!isTemplate && doc.hasFacet(
                    EloraFacetConstants.FACET_ELORA_WORKSPACE)) {

                boolean addWorkspaceToFavorites = Boolean.valueOf(
                        Framework.getProperty(
                                EloraPropertiesConstants.PROP_ADD_NEW_WORKSPACE_TO_FAVORITES_BY_DEFAULT,
                                Boolean.toString(false)));

                if (addWorkspaceToFavorites) {

                    final FavoritesManager favoritesManager = Framework.getLocalService(
                            FavoritesManager.class);
                    final CollectionManager collectionManager = Framework.getLocalService(
                            CollectionManager.class);

                    if (collectionManager.isCollectable(doc)) {
                        favoritesManager.addToFavorites(doc, session);
                        log.trace(logInitMsg + "Document docId = |"
                                + doc.getId() + "| added to favorites.");
                    }
                }
            }
        }

    }

    private static void markAsTemplateIfRequired(CoreSession session,
            DocumentModel doc) {
        if (EloraDocumentHelper.isDocumentUnderTemplateRoot(doc, session)
                && (doc.hasFacet(EloraFacetConstants.FACET_TEMPLATABLE))) {
            doc.setPropertyValue(EloraMetadataConstants.ELORA_TEMPL_IS_TEMPLATE,
                    true);
        }
    }

    // #############################################################
    // TODO: Begiratu dokumentu bat sortzerakoan, proxy-a sortu behar den.
    // Proxy-a sortzeko logika hona ekar daiteke???
    // #############################################################

}
