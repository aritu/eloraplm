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
package com.aritu.eloraplm.webapp.base.services;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.trash.TrashServiceImpl;

/**
 * // TransServiceImpl overridden to allow deleting proxies even if the source
 * document is locked by another user
 *
 * @author aritu
 *
 */
public class EloraTrashServiceImpl extends TrashServiceImpl
        implements TrashService {

    @Override
    protected TrashInfo getInfo(List<DocumentModel> docs, Principal principal,
            boolean checkProxies, boolean checkDeleted) {
        TrashInfo info = new TrashInfo();
        info.docs = new ArrayList<DocumentModel>(docs.size());
        if (docs.isEmpty()) {
            return info;
        }
        CoreSession session = docs.get(0).getCoreSession();
        for (DocumentModel doc : docs) {
            if (checkDeleted && !LifeCycleConstants.DELETED_STATE.equals(
                    doc.getCurrentLifeCycleState())) {
                info.forbidden++;
                continue;
            }
            if (doc.getParentRef() == null) {
                if (doc.isVersion()
                        && !session.getProxies(doc.getRef(), null).isEmpty()) {
                    // do not remove versions used by proxies
                    info.forbidden++;
                    continue;
                }

            } else {
                if (!session.hasPermission(doc.getParentRef(),
                        SecurityConstants.REMOVE_CHILDREN)) {
                    info.forbidden++;
                    continue;
                }
            }
            if (!session.hasPermission(doc.getRef(),
                    SecurityConstants.REMOVE)) {
                info.forbidden++;
                continue;
            }
            if (checkProxies && doc.isProxy()) {
                info.proxies++;
                continue;
            }
            if (doc.isLocked()) {
                String locker = getDocumentLocker(doc);
                // If it is a proxy, we do not mind if it is locked or not
                if (principal == null
                        || (principal instanceof NuxeoPrincipal
                                && ((NuxeoPrincipal) principal).isAdministrator())
                        || principal.getName().equals(locker)
                        || doc.isProxy()) {
                    info.docs.add(doc);
                } else {
                    info.locked++;
                }
            } else {
                info.docs.add(doc);
            }
        }
        return info;
    }

}
