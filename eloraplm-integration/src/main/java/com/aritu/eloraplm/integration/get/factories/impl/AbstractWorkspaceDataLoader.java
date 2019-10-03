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
package com.aritu.eloraplm.integration.get.factories.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraUrlHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoader;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspaceResponse;
import com.aritu.eloraplm.integration.get.restoperations.util.GetWorkspaceResponseDoc;
import com.aritu.eloraplm.integration.util.FolderInfo;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
public abstract class AbstractWorkspaceDataLoader
        implements WorkspaceDataLoader {

    protected static final String CHILDREN_VERSIONS_AS_STORED = "AsStored";

    protected static final String CHILDREN_VERSIONS_LATEST_VERSIONS = "LatestVersions";

    protected static final String CHILDREN_VERSIONS_LATEST_RELEASED = "LatestReleased";

    protected static final String SOURCE_CONTENT = "Content";

    protected static final String SOURCE_CM_PROCESS_ROOT_ITEM = "CmProcessRootItem";

    protected static final String SOURCE_CM_PROCESS_SUBITEM = "CmProcessSubitem";

    protected DocumentModel workspaceDoc;

    protected CoreSession session;

    protected HttpServletRequest request;

    protected String contentChildrenVersions;

    protected String cmProcessRootItemChildrenVersions;

    protected String cmProcessSubitemChildrenVersions;

    protected GetWorkspaceResponse response;

    protected Map<String, GetWorkspaceResponseDoc> tempRootItemDocMap;

    protected Map<String, GetWorkspaceResponseDoc> tempSubitemDocMap;

    protected Map<String, GetWorkspaceResponseDoc> tempContentDocMap;

    protected class DocumentWithoutArchivedVersionsException extends Exception {
        private static final long serialVersionUID = 1L;

        public DocumentWithoutArchivedVersionsException() {
            super();
        }
    }

    public AbstractWorkspaceDataLoader(DocumentModel workspaceDoc)
            throws EloraException {
        initializeTempDocMap();
        response = new GetWorkspaceResponse();
        session = workspaceDoc.getCoreSession();
        this.workspaceDoc = workspaceDoc;

        checkIfWorkspaceIsCorrect();
    }

    private void initializeTempDocMap() {
        tempRootItemDocMap = new HashMap<String, GetWorkspaceResponseDoc>();
        tempSubitemDocMap = new HashMap<String, GetWorkspaceResponseDoc>();
        tempContentDocMap = new HashMap<String, GetWorkspaceResponseDoc>();
    }

    protected void checkIfWorkspaceIsCorrect() throws EloraException {
        if (!workspaceDoc.hasFacet(EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
            throw new EloraException(
                    "The provided document is not a workspace.");
        }
    }

    @Override
    public GetWorkspaceResponse getDataAndCreateResponse(
            HttpServletRequest request, String contentChildrenVersions,
            String cmProcessRootItemChildrenVersions,
            String cmProcessSubitemChildrenVersions) throws EloraException {
        this.request = request;
        this.contentChildrenVersions = contentChildrenVersions;
        this.cmProcessRootItemChildrenVersions = cmProcessRootItemChildrenVersions;
        this.cmProcessSubitemChildrenVersions = cmProcessSubitemChildrenVersions;
        // We process it in this order, so that the documents' priority is:
        // 1.- RootItem
        // 2.- Subitem
        // 3.- RootItem and Subitem structure
        // 4.- Content
        // 5.- Content structure

        processCmProcessStructure();
        processDocRelations(tempRootItemDocMap);
        processDocRelations(tempSubitemDocMap);

        // initializeTempDocMap();
        processContent();
        processDocRelations(tempContentDocMap);

        return response;
    }

    protected void processContent() throws EloraException {
        for (DocumentModel folder : getContentFolders()) {
            processFolder(folder);
        }

        for (DocumentModel doc : getContentDocs()) {
            try {
                processDocument(0, SOURCE_CONTENT, doc, true,
                        doc.getParentRef().toString(), contentChildrenVersions);
            } catch (DocumentWithoutArchivedVersionsException e) {
                continue;
            }
        }

        for (DocumentModel item : getContentItems()) {
            String parentUid = item.getParentRef().toString();
            try {
                DocumentModel realAvItem = getNonProxyArchivedVersion(item);
                if (realAvItem != null) {
                    for (DocumentModel doc : getAllRelatedCadDocsForItem(
                            realAvItem)) {
                        try {
                            processDocument(0, SOURCE_CONTENT, doc, true,
                                    parentUid, contentChildrenVersions);
                        } catch (DocumentWithoutArchivedVersionsException e) {
                            continue;
                        }
                    }
                }
            } catch (DocumentWithoutArchivedVersionsException e) {
                continue;
            }
        }
    }

    protected DocumentModelList getContentFolders() {
        String query = EloraQueryFactory.getFoldersInWorkspaceQuery(
                workspaceDoc.getId());
        return session.query(query);
    }

    protected void processFolder(DocumentModel folder) {
        FolderInfo responseFolder = new FolderInfo();
        responseFolder.setRealUid(folder.getId());
        responseFolder.setParentRealUid(folder.getParentRef().toString());
        responseFolder.setTitle(folder.getTitle());
        responseFolder.setPath(folder.getPathAsString());

        response.addFolder(responseFolder);
    }

    protected DocumentModelList getContentDocs() {
        String query = EloraQueryFactory.getCadDocumentsInWorkspaceQuery(
                workspaceDoc.getId());
        return session.query(query);
    }

    protected DocumentModelList getContentItems() {
        String query = EloraQueryFactory.getItemsInWorkspaceQuery(
                workspaceDoc.getId());
        return session.query(query);
    }

    private DocumentModelList getAllRelatedCadDocsForItem(DocumentModel item) {

        // TODO Orokorra
        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        return RelationHelper.getObjectDocuments(item, predicate);
    }

    protected void processDocument(int level, String source, DocumentModel doc,
            boolean saveInWorkspace, String parentUid, String childrenVersions)
            throws EloraException, DocumentWithoutArchivedVersionsException {

        boolean isDocSpecial = doc.getType().equals(
                EloraDoctypeConstants.CAD_DRAWING);
        processDocAndRelations(level, source, doc, saveInWorkspace, parentUid,
                childrenVersions, false, isDocSpecial);
    }

    protected void processDocAndRelations(int level, String source,
            DocumentModel doc, boolean saveInWorkspace, String parentUid,
            String childrenVersions, boolean isStructure, boolean isRootSpecial)
            throws EloraException, DocumentWithoutArchivedVersionsException {

        Map<String, GetWorkspaceResponseDoc> tempDocMap = null;
        switch (source) {
        case SOURCE_CM_PROCESS_ROOT_ITEM:
            tempDocMap = tempRootItemDocMap;
            break;
        case SOURCE_CM_PROCESS_SUBITEM:
            tempDocMap = tempSubitemDocMap;
            break;
        case SOURCE_CONTENT:
            tempDocMap = tempContentDocMap;
            break;
        }

        DocumentModel realAvDoc = getNonProxyArchivedVersion(doc);

        if (realAvDoc != null) {
            realAvDoc = switchDocVersionIfNeeded(realAvDoc, source, level,
                    isRootSpecial);
            if (realAvDoc != null) {
                if (!isAlreadyProcessed(realAvDoc)
                        || mustUpdateVersion(level, isRootSpecial, isStructure,
                                tempDocMap, realAvDoc.getVersionSeriesId())) {
                    GetWorkspaceResponseDoc responseDoc = getDocData(realAvDoc);
                    responseDoc.setSource(source);
                    responseDoc.setSaveInWorkspace(saveInWorkspace);
                    if (parentUid != null) {
                        responseDoc.addParentRealUid(parentUid);
                    }

                    tempDocMap.put(realAvDoc.getVersionSeriesId(), responseDoc);
                    response.addDocument(realAvDoc.getVersionSeriesId(),
                            responseDoc);

                    if (isStructure) {
                        processRelations(level, source, realAvDoc,
                                childrenVersions, isRootSpecial);
                    }
                } else {
                    if (saveInWorkspace) {
                        GetWorkspaceResponseDoc responseDoc = response.getDocument(
                                realAvDoc.getVersionSeriesId());
                        if (!responseDoc.hasParentRealUid(parentUid)) {
                            responseDoc.addParentRealUid(parentUid);
                            responseDoc.setSaveInWorkspace(true);
                        }
                    }
                }
            }
        }
    }

    protected boolean mustUpdateVersion(int level, boolean isRootSpecial,
            boolean isStructure,
            Map<String, GetWorkspaceResponseDoc> tempDocMap,
            String versionSeriesId) {
        return (isStructure && isSwitchNeeded(level, isRootSpecial)
                && tempDocMap.containsKey(versionSeriesId));
    }

    protected DocumentModel getNonProxyArchivedVersion(DocumentModel doc)
            throws EloraException, DocumentWithoutArchivedVersionsException {
        if (doc.isProxy()) {
            doc = session.getSourceDocument(doc.getRef());
        }
        if (!doc.isImmutable()) {
            if (session.getVersionsRefs(doc.getRef()).isEmpty()) {
                // If the document does not have yet an AV, we exclude it
                throw new DocumentWithoutArchivedVersionsException();
            }
            doc = EloraDocumentHelper.getLatestVersion(doc);
        }
        return doc;
    }

    protected DocumentModel switchDocVersionIfNeeded(DocumentModel doc,
            String source, int level, boolean isRootSpecial)
            throws EloraException {

        // We don't have to treat special relations, because in special
        // predicates we only get the objects (when root is special), the
        // drawings related to the other documents don't get downloaded
        if (isSwitchNeeded(level, isRootSpecial)) {

            String childrenVersionsOption = null;
            switch (source) {
            case SOURCE_CONTENT:
                childrenVersionsOption = contentChildrenVersions;
                break;
            case SOURCE_CM_PROCESS_ROOT_ITEM:
                childrenVersionsOption = cmProcessRootItemChildrenVersions;
                break;
            case SOURCE_CM_PROCESS_SUBITEM:
                childrenVersionsOption = cmProcessSubitemChildrenVersions;
                break;
            }

            if (childrenVersionsOption.equals(
                    CHILDREN_VERSIONS_LATEST_VERSIONS)) {
                doc = EloraDocumentHelper.getLatestVersion(doc);
            } else if (childrenVersionsOption.equals(
                    CHILDREN_VERSIONS_LATEST_RELEASED)) {
                doc = EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                        doc);
            }
        }

        return doc;

    }

    protected boolean isSwitchNeeded(int level, boolean isRootSpecial) {
        return (isRootSpecial && level == 2) || (!isRootSpecial && level == 1);
    }

    protected boolean isAlreadyProcessed(DocumentModel doc) {
        return response.hasDocument(doc.getVersionSeriesId());
    }

    // TODO Seguruenik, klase orokorrak sortzen badie, hau kanpora atara beharko
    // zan, funtzio orokor bat bihurtu
    protected GetWorkspaceResponseDoc getDocData(DocumentModel doc)
            throws EloraException {
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());

        GetWorkspaceResponseDoc responseDoc = new GetWorkspaceResponseDoc();
        responseDoc.setRealUid(doc.getId());
        responseDoc.setWcUid(wcDoc.getId());
        responseDoc.setType(doc.getType());
        responseDoc.setCurrentVersionLabel(doc.getVersionLabel());

        // TODO Kanpora
        Blob contentBlob = (Blob) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        if (contentBlob != null) {
            String filename = contentBlob.getFilename();

            responseDoc.setFilename(filename);
            responseDoc.setHash(contentBlob.getDigest());
            responseDoc.setDownloadUrl(EloraUrlHelper.getDocumentDownloadUrl(
                    request, doc, filename));
        }

        return responseDoc;
    }

    protected void processRelations(int level, String source, DocumentModel doc,
            String childrenVersions, boolean isRootSpecial)
            throws EloraException, DocumentWithoutArchivedVersionsException {

        // Get the Hierarchical relations
        List<String> hierarchicalAndSuppressedRelations = new ArrayList<String>();
        hierarchicalAndSuppressedRelations.addAll(
                RelationsConfig.cadHierarchicalRelationsList);
        hierarchicalAndSuppressedRelations.addAll(
                RelationsConfig.cadSuppressedRelationsList);

        List<Resource> hierarchicalAndSuppressedPredicates = new ArrayList<Resource>();
        for (String predicateUri : hierarchicalAndSuppressedRelations) {
            Resource predicate = new ResourceImpl(predicateUri);
            hierarchicalAndSuppressedPredicates.add(predicate);
        }

        List<Statement> hierarchicalAndSuppressedObjects = new ArrayList<Statement>();
        hierarchicalAndSuppressedObjects.addAll(
                EloraRelationHelper.getStatements(doc,
                        hierarchicalAndSuppressedPredicates));

        // Treat Hierarchical objects
        if (!hierarchicalAndSuppressedObjects.isEmpty()) {
            processStatements(level, source, doc,
                    hierarchicalAndSuppressedObjects, false, childrenVersions,
                    isRootSpecial);
        }

        // Get the Direct+Special relations
        List<String> directAndSpecialRelations = new ArrayList<String>();
        directAndSpecialRelations.addAll(
                RelationsConfig.cadDirectRelationsList);
        directAndSpecialRelations.addAll(
                RelationsConfig.cadSpecialRelationsList);

        List<Resource> directAndSpecialPredicates = new ArrayList<Resource>();
        for (String predicateUri : directAndSpecialRelations) {
            Resource predicate = new ResourceImpl(predicateUri);
            directAndSpecialPredicates.add(predicate);
        }

        List<Statement> directAndSpecialObjects = new ArrayList<Statement>();
        directAndSpecialObjects.addAll(EloraRelationHelper.getStatements(doc,
                directAndSpecialPredicates));

        // Treat Direct+Special objects
        if (!directAndSpecialObjects.isEmpty()) {
            processStatements(level, source, doc, directAndSpecialObjects, true,
                    childrenVersions, isRootSpecial);
        }

    }

    protected void processStatements(int level, String source,
            DocumentModel cadParent, List<Statement> statements, boolean direct,
            String childrenVersions, boolean isRootSpecial)
            throws EloraException, DocumentWithoutArchivedVersionsException {

        level++;
        for (Statement statement : statements) {
            if ((statement.getObject() instanceof QNameResource)
                    && (statement.getSubject() instanceof QNameResource)) {
                // Check if subject and object are documents. If something goes
                // wrong we don't want to process relations between doc->url,
                // doc->txt, etc.
                DocumentModel cadChild;
                cadChild = RelationHelper.getDocumentModel(
                        statement.getObject(), session);

                if (cadChild == null) {
                    throw new EloraException(
                            "The child document in the relation does not exist or the user has no right to read it. Statement: |"
                                    + statement.toString() + "|");
                }
                if (cadChild.isProxy()) {
                    throw new EloraException(
                            "The child document in the relation is a proxy. Statement: |"
                                    + statement.toString() + "|");
                }
                if (!cadChild.isVersion()) {
                    throw new EloraException(
                            "The child document in the relation is a working copy. Statement: |"
                                    + statement.toString() + "|");
                }

                processDocAndRelations(level, source, cadChild, false, null,
                        childrenVersions, true, isRootSpecial);
            }
        }

    }

    protected void processDocRelations(
            Map<String, GetWorkspaceResponseDoc> tempDocMap)
            throws EloraException {

        Map<String, GetWorkspaceResponseDoc> itTempDocMap = new HashMap<String, GetWorkspaceResponseDoc>(
                tempDocMap);
        // Empty temp map so we do not override top level documents with the
        // ones in the structure, even if they are marked to switch
        tempDocMap.clear();
        for (GetWorkspaceResponseDoc doc : itTempDocMap.values()) {
            DocumentModel realAvDoc = session.getDocument(
                    new IdRef(doc.getRealUid()));
            try {
                boolean isDocSpecial = doc.getType().equals(
                        EloraDoctypeConstants.CAD_DRAWING);
                processRelations(0, doc.getSource(), realAvDoc,
                        getChildrenVersionsBySource(doc.getSource()),
                        isDocSpecial);
            } catch (DocumentWithoutArchivedVersionsException e) {
                continue;
            }
        }
    }

    protected String getChildrenVersionsBySource(String source)
            throws EloraException {
        switch (source) {
        case SOURCE_CM_PROCESS_ROOT_ITEM:
            return cmProcessRootItemChildrenVersions;
        case SOURCE_CM_PROCESS_SUBITEM:
            return cmProcessSubitemChildrenVersions;
        case SOURCE_CONTENT:
            return contentChildrenVersions;
        default:
            throw new EloraException("Invalid source option.");
        }
    }

    protected abstract void processCmProcessStructure() throws EloraException;

}
