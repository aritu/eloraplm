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
package com.aritu.eloraplm.elasticsearch.commands;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BINARYTEXT_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PROXY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_TAG_UPDATED;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.PdmEventNames;

/**
 * Contains logic to stack ElasticSearch commands depending on Document events.
 *
 * Elora changes: index also document's related proxies.
 *
 * @author aritu
 *
 */
public abstract class EloraIndexingCommandsStacker
        extends IndexingCommandsStacker {

    protected static final Log log = LogFactory.getLog(
            EloraIndexingCommandsStacker.class);

    @Override
    public void stackCommand(DocumentEventContext docCtx, String eventId) {
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null) {
            return;
        }
        Boolean block = (Boolean) docCtx.getProperty(
                ElasticSearchConstants.DISABLE_AUTO_INDEXING);
        if (block != null && block) {
            if (log.isDebugEnabled()) {
                log.debug("Indexing is disable, skip indexing command for doc "
                        + doc);
            }
            return;
        }
        boolean sync = isSynchronous(docCtx, doc);
        stackCommand(doc, eventId, sync, false);
    }

    protected void stackCommand(DocumentModel doc, String eventId, boolean sync,
            boolean proxyRelated) {
        IndexingCommands cmds = getOrCreateCommands(doc);
        Type type;
        boolean recurse = false;
        switch (eventId) {
        case DOCUMENT_CREATED:
            type = Type.INSERT;
            break;
        case DOCUMENT_CREATED_BY_COPY:
            type = Type.INSERT;
            recurse = isFolderish(doc);
            break;
        case BEFORE_DOC_UPDATE:
        case DOCUMENT_CHECKEDOUT:
        case BINARYTEXT_UPDATED:
        case DOCUMENT_TAG_UPDATED:
        case DOCUMENT_PROXY_UPDATED:
        case PdmEventNames.PDM_CHECKOUT_UNDONE_EVENT:
        case LifeCycleConstants.TRANSITION_EVENT:
            if (doc.isProxy() && !doc.isImmutable() && !proxyRelated) {
                stackCommand(
                        doc.getCoreSession().getDocument(
                                new IdRef(doc.getSourceId())),
                        BEFORE_DOC_UPDATE, false, true);
            }
            // -------- Elora Change: index also working copy
            else if (!doc.isProxy() && doc.isImmutable() && !proxyRelated) {
                stackCommand(
                        doc.getCoreSession().getWorkingCopy(
                                new IdRef(doc.getSourceId())),
                        BEFORE_DOC_UPDATE, false, false);
            }
            // -------- Elora Change: index also document's proxies
            else if (!doc.isProxy() && !doc.isImmutable() && !proxyRelated) {
                DocumentModelList proxies = doc.getCoreSession().getProxies(
                        doc.getRef(), null);
                if (proxies != null && proxies.size() > 0) {
                    for (Iterator<DocumentModel> iterator = proxies.iterator(); iterator.hasNext();) {
                        DocumentModel proxy = iterator.next();
                        if (!proxy.getId().equals(doc.getId())) {
                            stackCommand(proxy, BEFORE_DOC_UPDATE, false, true);
                        }
                    }
                }
            }

            type = Type.UPDATE;
            break;
        case DOCUMENT_CHECKEDIN:
            if (indexIsLatestVersion()) {
                CoreSession session = doc.getCoreSession();
                if (session != null) {
                    // The previous doc version with isLastestVersion and
                    // isLatestMajorVersion need to be updated
                    // Here we have no way to get this exact doc version so we
                    // reindex all versions
                    for (DocumentModel version : doc.getCoreSession().getVersions(
                            doc.getRef())) {
                        stackCommand(version, BEFORE_DOC_UPDATE, false);
                    }
                }
            }
            type = Type.UPDATE;
            break;
        case DOCUMENT_MOVED:
            type = Type.UPDATE;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_REMOVED:
            type = Type.DELETE;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_SECURITY_UPDATED:
            type = Type.UPDATE_SECURITY;
            recurse = isFolderish(doc);
            break;
        case DOCUMENT_CHILDREN_ORDER_CHANGED:
            type = Type.UPDATE_DIRECT_CHILDREN;
            recurse = true;
            break;
        default:
            return;
        }
        if (sync && recurse) {
            // split into 2 commands one sync and an async recurse
            cmds.add(type, true, false);
            cmds.add(type, false, true);
        } else {
            cmds.add(type, sync, recurse);
        }
    }

    private boolean indexIsLatestVersion() {
        return !Framework.isBooleanPropertyTrue(
                AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY);
    }

    private boolean isFolderish(DocumentModel doc) {
        return doc.isFolder() && !doc.isVersion();
    }

}
