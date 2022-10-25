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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.archiver.api.ArchiverDescriptor;
import com.aritu.eloraplm.core.archiver.api.ArchiverExecuterDescriptor;
import com.aritu.eloraplm.core.archiver.util.WorkspaceArchiverHelper;

public class UnrestrictedArchiver extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(
            UnrestrictedArchiver.class);

    private static final String EXECUTER_TYPE_PRE = "pre";

    private static final String EXECUTER_TYPE_POST = "post";

    private DocumentModel workspace;

    private ArchiverDescriptor archiver;

    public UnrestrictedArchiver(CoreSession session, DocumentModel workspace,
            ArchiverDescriptor archiver) {
        super(session);
        this.workspace = workspace;
        this.archiver = archiver;
    }

    @Override
    public void run() {
        try {

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // PRE executers
            if (archiver.executers.length > 0) {
                for (ArchiverExecuterDescriptor executer : archiver.executers) {
                    if (executer.type.equals(EXECUTER_TYPE_PRE)) {
                        processExecuter(executer);
                    }
                }
            }

            // Mark as archived
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ISARCHIVED, true);
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ARCHIVINGDATE,
                    Calendar.getInstance());
            workspace.setPropertyValue(
                    EloraMetadataConstants.ELORA_ARC_ARCHIVINGUSER,
                    originatingUsername);
            session.saveDocument(workspace);

            // Update proxies
            WorkspaceArchiverHelper.updateWorkingCopyProxies(session,
                    workspace);

            // Remove from Favorites
            WorkspaceArchiverHelper.removeFromFavorites(session, workspace);

            // Unlock
            session.removeLock(workspace.getRef());

            // Move
            DocumentModel destinationFolder = WorkspaceArchiverHelper.getDestinationFolder(
                    session, workspace, archiver);

            workspace = session.move(workspace.getRef(),
                    destinationFolder.getRef(), null);

            // POST executers
            if (archiver.executers.length > 0) {
                for (ArchiverExecuterDescriptor executer : archiver.executers) {
                    if (executer.type.equals(EXECUTER_TYPE_POST)) {
                        processExecuter(executer);
                    }
                }
            }

        } catch (Exception e) {
            TransactionHelper.setTransactionRollbackOnly();
            log.error("Exception");
            throw new NuxeoException("Exception archiving the workspace.");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private void processExecuter(ArchiverExecuterDescriptor excuter) {
        String executerClass = excuter.executerClass;
        String method = excuter.method;
        if (executerClass == null || method == null) {
            throw new NuxeoException(
                    "Executer class and method must not be null.");
        }

        try {
            Class<?> c = Class.forName(executerClass);
            Method m = c.getMethod(method, DocumentModel.class);
            workspace = (DocumentModel) m.invoke(null, workspace);

        } catch (ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {

            log.error("Error when executing condition methods for archiver |"
                    + archiver.name + "|");
            throw new NuxeoException(
                    "Error when executing condition methods. Error: "
                            + e.getClass().getName() + " - " + e.getMessage(),
                    e);
        }
    }

    public DocumentModel archive() {
        runUnrestricted();
        return workspace;
    }

}
