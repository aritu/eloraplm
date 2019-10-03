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
package com.aritu.eloraplm.thumbnails.beans;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author aritu
 *
 */

@Name("thumbActions")
@Scope(EVENT)
public class ThumbnailActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ThumbnailActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    public void regenerateThumbnail() {

        String logInitMsg = "[regenerateThumbnail] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentModel document = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (currentDocument.isProxy()) {
            document = documentManager.getSourceDocument(
                    currentDocument.getRef());
        } else {
            document = currentDocument;
        }

        DocumentEventContext ctx = new DocumentEventContext(documentManager,
                documentManager.getPrincipal(), document);

        Framework.getLocalService(EventService.class).fireEvent(
                ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name(),
                ctx);
        log.info(logInitMsg + "Thumbnail regeneration requested for document |"
                + document.getId() + "|");

        log.trace(logInitMsg + "--- EXIT --- ");
    }
}
