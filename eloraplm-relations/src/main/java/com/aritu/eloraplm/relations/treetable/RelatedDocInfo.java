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
package com.aritu.eloraplm.relations.treetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;

import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class RelatedDocInfo {

    String versionSeriesId;

    String predicateUri;

    List<DocumentModel> docVersions;

    Map<String, Statement> stmts;

    public RelatedDocInfo(String versionSeriesId, String predicateUri) {
        this.versionSeriesId = versionSeriesId;
        this.predicateUri = predicateUri;
        docVersions = new ArrayList<DocumentModel>();
        stmts = new HashMap<String, Statement>();
    }

    public String getVersionSeriesId() {
        return versionSeriesId;
    }

    public void setVersionSeriesId(String versionSeriesId) {
        this.versionSeriesId = versionSeriesId;
    }

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    public void addDocVersionAndStatement(DocumentModel docVersion,
            Statement stmt) {
        if (!docVersions.contains(docVersion)) {
            docVersions.add(docVersion);
        }
        if (!stmts.containsKey(docVersion.getId())) {
            stmts.put(docVersion.getId(), stmt);
        }
    }

    public List<DocumentModel> getDocVersions() {
        return docVersions;
    }

    public DocumentModel getUniqueVersion() throws EloraException {
        return getUniqueVersion(false);
    }

    public DocumentModel getUniqueVersion(boolean prefereWc)
            throws EloraException {
        DocumentModel selectedDoc = null;

        if (docVersions.size() > 0) {
            DocumentModel firstDoc = docVersions.get(0);
            CoreSession session = firstDoc.getCoreSession();
            DocumentModel wcDoc = session.getWorkingCopy(firstDoc.getRef());

            if (prefereWc) {
                // Try to return WC
                if (docVersions.contains(wcDoc)) {
                    return wcDoc;
                }
            }

            String type = firstDoc.getType();

            // Exclude working copies to avoid errors if the base and the WC don't have the
            // same relations (then, the selection fails because it is looking for a version
            // with the same major as the WC, but it isn't related to the parent)
            docVersions.remove(wcDoc);

            // TODO IBAI getLatestRelatedVersion(CoreSession session,
            // List<DocumentModel> wcDocs) erabili eta testeatu
            List<String> uidList = EloraDocumentHelper.getUidListFromDocList(
                    docVersions);
            Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                    docVersions);

            selectedDoc = EloraRelationHelper.getLatestRelatedVersion(session,
                    majorVersion, uidList, type);

            if (selectedDoc == null) {
                throw new EloraException(
                        "Null value retrieved getting latest related version");
            }

            if (!docVersions.contains(selectedDoc)) {
                selectedDoc = null;
            }
        } else if (docVersions.size() == 1) {
            selectedDoc = docVersions.get(0);
        }

        return selectedDoc;
    }

    public Statement getStatementForDocVersion(String docId) {
        if (stmts.containsKey(docId)) {
            return stmts.get(docId);
        }
        return null;
    }

}
