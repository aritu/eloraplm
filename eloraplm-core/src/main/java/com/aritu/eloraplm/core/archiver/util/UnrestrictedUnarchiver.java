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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.archiver.util.WorkspaceArchiverHelper;

public class UnrestrictedUnarchiver extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(
            UnrestrictedUnarchiver.class);

    private DocumentModel workspace;

    public UnrestrictedUnarchiver(CoreSession session,
            DocumentModel workspace) {
        super(session);
        this.workspace = workspace;
    }

    @Override
    public void run() {
        try {

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // Mark as unarchived
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ISARCHIVED, false);
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ARCHIVINGDATE, null);
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ARCHIVINGUSER, null);
            session.saveDocument(workspace);

            DocumentModel destinationFolder = WorkspaceArchiverHelper.getWorkspaceRootFolder(
                    session, workspace);

            workspace = session.move(workspace.getRef(),
                    destinationFolder.getRef(), null);

        } catch (Exception e) {
            TransactionHelper.setTransactionRollbackOnly();
            log.error("Exception");
            throw new NuxeoException("Exception unarchiving the workspace.");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public DocumentModel unarchive() {
        runUnrestricted();
        return workspace;
    }

}
