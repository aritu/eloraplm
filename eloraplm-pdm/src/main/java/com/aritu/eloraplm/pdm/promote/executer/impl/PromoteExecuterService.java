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
package com.aritu.eloraplm.pdm.promote.executer.impl;

import java.util.LinkedHashMap;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.promote.executer.PromoteExecuterManager;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public abstract class PromoteExecuterService implements PromoteExecuterManager {

    protected void unlockDocument(DocumentModel doc, CoreSession session) {
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        // boolean isUnlockable = true;
        // If document is checked out it is not possible to unlock
        // if (wcDoc.isCheckedOut()) {
        // isUnlockable = false;
        // }
        session.removeLock(wcDoc.getRef());
    }

    protected void doPromote(DocumentModel doc, String promoteTransition,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        doc.followTransition(promoteTransition);

        // Update dc:lastContributor, dc:contributors and dc:modified
        doc = EloraDocumentHelper.updateContributorAndModified(doc, false);

        // Nuxeo Event
        String comment = doc.getVersionLabel();
        EloraEventHelper.fireEvent(PdmEventNames.PDM_PROMOTED_EVENT, doc,
                comment);

        EloraDocumentHelper.disableVersioningDocument(doc);
        doc = session.saveDocument(doc);

        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        DocumentRef wcBaseRef = session.getBaseVersion(wcDoc.getRef());
        DocumentModel baseDoc = session.getDocument(wcBaseRef);

        VersionModel version = new VersionModelImpl();
        version.setId(doc.getId());
        if (doc.getId().equals(baseDoc.getId())) {
            // We cannot follow transition instead of restoring, because it
            // checks the document out always. This is the only way we know to
            // change the state without checkin the document out.
            EloraDocumentHelper.restoreToVersion(wcDoc.getRef(),
                    new IdRef(version.getId()), true, true, session);
        } else if (isMajorVersionEqual(doc, wcDoc)) {
            EloraDocumentHelper.restoreWorkingCopyToVersion(wcDoc, version,
                    eloraDocumentRelationManager, session);
        }

        // TODO: Hemos decidido no actualizar las relaciones de todos los
        // subjects que apuntan a cualquier version de este major de este
        // documento(todos
        // los statement que tienen como object cualquier version dentro del
        // major del documento). Por ahora, no queremos cambiar cosas de
        // otros documentos sin que otro usuario que este utilizando ese
        // documento se de cuenta

    }

    private boolean isMajorVersionEqual(DocumentModel doc,
            DocumentModel wcDoc) {
        return doc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP).equals(
                        wcDoc.getPropertyValue(
                                VersioningService.MAJOR_VERSION_PROP));
    }

    @Override
    public abstract void processPromote(TreeNode node, String transition,
            String finalState, EloraConfigTable lifeCycleStatesConfig,
            EloraDocumentRelationManager eloraDocumentRelationManager)
            throws EloraException, DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, DocumentLockRightsException;

    @Override
    public abstract LinkedHashMap<String, String> getVersionMap(
            DocumentModel doc, Statement stmt, boolean isSpecial, int level)
            throws EloraException;

}
