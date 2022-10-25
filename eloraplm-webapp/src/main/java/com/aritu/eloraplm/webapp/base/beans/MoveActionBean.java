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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.EVENT;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * @author aritu
 *
 */

@Name("moveActions")
@Scope(EVENT)
public class MoveActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MoveActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String targetParentUid;

    public String getTargetParentUid() {
        return targetParentUid;
    }

    public void setTargetParentUid(String targetParentUid) {
        this.targetParentUid = targetParentUid;
    }

    public Boolean getCanMoveCurrentDoc() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanMoveDoc(currentDocument);
    }

    public Boolean getCanMoveDoc(DocumentModel document) {
        String logInitMsg = "[getCanMoveDoc] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        boolean canMove = false;

        if (document == null) {
            log.warn(logInitMsg
                    + "Can't evaluate move action : currentDocument is null");
        } else {
            if (!document.isImmutable() && !document.isProxy()) {

                NuxeoPrincipal currentUser = (NuxeoPrincipal) documentManager.getPrincipal();

                if (currentUser.isAdministrator()) {
                    canMove = true;
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT with canMove = |" + canMove + "|---");
        return canMove;
    }

    public void moveCurrentDocument() {
        String logInitMsg = "[moveCurrentDocument] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        moveDocument(currentDocument);

        log.trace(logInitMsg + "--- EXIT ---");
    }

    public void moveDocument(DocumentModel document) {
        String logInitMsg = "[moveDocument] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        DocumentRef targetParentRef = new IdRef(targetParentUid);

        document = documentManager.move(document.getRef(), targetParentRef,
                document.getName());
        documentManager.save();

        navigationContext.navigateToDocument(document);

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("eloraplm.message.success.move"));

        log.trace(logInitMsg + "--- EXIT ---");
    }
}
