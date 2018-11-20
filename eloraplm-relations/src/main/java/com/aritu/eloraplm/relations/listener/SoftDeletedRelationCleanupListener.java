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
package com.aritu.eloraplm.relations.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
public class SoftDeletedRelationCleanupListener implements EventListener {

    private static final Log log = LogFactory.getLog(
            SoftDeletedRelationCleanupListener.class);

    @Override
    public void handleEvent(Event event) {

        String logInitMsg = "[handleEvent] ";
        log.trace(logInitMsg + "About to remove soft deleted relations...");

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        Repository defaultRepository = mgr.getDefaultRepository();
        try (CoreSession session = CoreInstance.openCoreSession(
                defaultRepository.getName())) {

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModelList softDeletedRelations = session.query(
                    EloraQueryFactory.getSoftDeletedRelationsQuery());
            log.trace(logInitMsg + " " + softDeletedRelations.size()
                    + " soft deleted relations found.");
            for (DocumentModel relation : softDeletedRelations) {
                session.removeDocument(relation.getRef());
                session.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

            log.trace(logInitMsg + "All soft deleted relations removed.");
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

}
