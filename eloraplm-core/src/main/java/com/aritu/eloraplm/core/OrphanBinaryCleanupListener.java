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
package com.aritu.eloraplm.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.storage.sql.management.SQLRepositoryStatus;
import org.nuxeo.ecm.core.storage.sql.management.SQLRepositoryStatusMBean;

/**
 *
 * @author aritu
 *
 */
public class OrphanBinaryCleanupListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(
            OrphanBinaryCleanupListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        log.debug("Starting orphan binaries cleanup");
        SQLRepositoryStatusMBean status = new SQLRepositoryStatus();
        if (!status.isBinariesGCInProgress()) {
            BinaryManagerStatus binaryManagerStatus = status.gcBinaries(true);
            log.info("Orphaned binaries garbage collecting result: "
                    + binaryManagerStatus);
        } else {
            log.info(
                    "Orphaned binaries garbage collecting is already in progress.");
        }
        log.info("Orphan binaries deleted.");
    }
}
