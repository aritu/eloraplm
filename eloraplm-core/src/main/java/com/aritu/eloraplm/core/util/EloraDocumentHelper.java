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

package com.aritu.eloraplm.core.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

public class EloraDocumentHelper {

    private EloraDocumentHelper() {
    }

    /**
     * Given a document property, updates its value with the given blob. The
     * property can be a blob list or a blob. If a blob list the blob is
     * appended to the list, if a blob then it will be set as the property
     * value. Both blob list formats are supported: the file list (blob holder
     * list) and simple blob list.
     */
    public static void addBlob(Property p, Map<String, String> cProps, Blob blob)
            throws PropertyException {
        if (p.isList()) {
            // detect if a list of simple blobs or a list of files (blob
            // holder)
            Type ft = ((ListProperty) p).getType().getFieldType();
            if (ft.isComplexType() && ((ComplexType) ft).getFieldsCount() == 3) {
                p.addValue(createBlobHolderMap(cProps, blob));
            } else {
                p.addValue(blob);
            }
        } else {
            p.setValue(blob);
        }
    }

    public static HashMap<String, Serializable> createBlobHolderMap(
            Map<String, String> cProps, Blob blob) {
        HashMap<String, Serializable> map = new HashMap<>();
        map.put("file", (Serializable) blob);
        map.put("filename", blob.getFilename());
        for (Map.Entry<String, String> entry : cProps.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static EloraLockInfo getLockInfo(DocumentModel doc)
            throws EloraException {
        EloraLockInfo eloraLockInfo;
        if (doc.isLocked()) {
            Lock lockInfo = doc.getLockInfo();
            eloraLockInfo = new EloraLockInfo(doc.isLocked(),
                    lockInfo.getOwner(), lockInfo.getCreated().getTime(),
                    getIsLockable(doc));
        } else {
            eloraLockInfo = new EloraLockInfo(false, "", null,
                    getIsLockable(doc));
        }
        return eloraLockInfo;
    }

    public static boolean getIsLockable(DocumentModel doc)
            throws EloraException {
        boolean isLockable = EloraConfigHelper.getIsLifeCycleStateLockable(doc.getCurrentLifeCycleState());
        return isLockable;
    }

    public static DocumentModel getLatestReleasedVersion(DocumentModel doc,
            CoreSession session) throws EloraException {

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        DocumentModel latestReleased = null;
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);

        String versionVersionableId = session.getWorkingCopy(doc.getRef()).getId();
        String query = EloraQueryFactory.getReleasedDocs(versionVersionableId,
                releasedStates);

        DocumentModelList releasedDocs = session.query(query);
        if (releasedDocs.size() > 0) {
            latestReleased = releasedDocs.get(0);
        } else {
            latestReleased = getLatestVersion(doc, session);
        }
        return latestReleased;
    }

    public static DocumentModel getLatestAliveVersion(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel latestAliveDoc = null;
        EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                new String[0]);
        String versionVersionableId = session.getWorkingCopy(doc.getRef()).getId();
        String query = EloraQueryFactory.getLatestAliveVersionDoc(
                versionVersionableId, obsoleteStates);
        DocumentModelList latestDocs = session.query(query);
        if (latestDocs.size() > 0) {
            latestAliveDoc = latestDocs.get(0);
        }
        return latestAliveDoc;
    }

    public static DocumentModel getLatestVersion(DocumentModel doc,
            CoreSession session) throws EloraException {
        DocumentModel latestVersionDoc = null;
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        if (!wcDoc.isCheckedOut()) {
            // Get document version working copy is based on
            DocumentRef wcBaseRef = session.getBaseVersion(wcDoc.getRef());
            latestVersionDoc = session.getDocument(wcBaseRef);
        } else {
            // If working copy is checked out we can't get it's based
            // version. We get the released doc in latest major or if it doesn't
            // exist latest version
            latestVersionDoc = getMajorReleasedVersion(wcDoc, session);
            if (latestVersionDoc == null) {
                latestVersionDoc = session.getLastDocumentVersion(doc.getRef());
            }
        }
        return latestVersionDoc;
    }

    public static DocumentModel getMajorReleasedVersion(DocumentModel doc,
            CoreSession session) throws EloraException {

        DocumentModel releasedDoc = null;
        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        String majorVersion = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION).toString();
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);
        String versionVersionableId = session.getWorkingCopy(doc.getRef()).getId();
        String query = EloraQueryFactory.getMajorReleasedVersion(
                versionVersionableId, releasedStates, majorVersion);

        DocumentModelList releasedDocs = session.query(query);
        if (releasedDocs.size() > 1) {
            throw new EloraException(
                    "There are multiple released docs in the same major version");
        }
        if (releasedDocs.size() > 0) {
            releasedDoc = releasedDocs.get(0);
        }
        return releasedDoc;
    }

    public static QNameResource getDocumentResource(
            RelationManager relationManager, DocumentModel document) {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = (QNameResource) relationManager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    public static DocumentModel getDocumentModel(
            RelationManager relationManager, CoreSession session, Node node) {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.<String, Object> singletonMap(
                    ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
            Object o = relationManager.getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    // Overload method for optional parameter fileName
    public static void relateBatchWithDoc(DocumentModel doc, int fileId,
            String batchId, String fileType, String hash) throws EloraException {
        relateBatchWithDoc(doc, fileId, batchId, fileType, "", hash);
    }

    public static void relateBatchWithDoc(DocumentModel doc, int fileId,
            String batch, String fileType, String fileName, String hash)
            throws EloraException {
        try {
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            Blob blob = bm.getBlob(batch, String.valueOf(fileId));

            BlobHolder bh = new SimpleBlobHolder(blob);
            String digest = bh.getHash().toUpperCase();
            if (!digest.equals(hash.toUpperCase())) {
                throw new EloraException("Incorrect hash in batch |" + batch
                        + "|");
            }

            Map<String, String> customProps = new HashMap<>();

            if (RequestContext.getActiveContext() != null) {
                RequestContext.getActiveContext().addRequestCleanupHandler(
                        new RequestCleanupHandler() {
                            @Override
                            public void cleanup(HttpServletRequest request) {
                                BatchManager bm = Framework.getLocalService(BatchManager.class);
                                bm.clean(batch);
                            }
                        });
            }

            // Add Blob to document
            switch (fileType) {
            case EloraGeneralConstants.FILE_TYPE_CONTENT:
                EloraDocumentHelper.addBlob(
                        doc.getProperty(NuxeoMetadataConstants.NX_FILE_CONTENT),
                        customProps, blob);

                // session.save();
                //
                // Blob contentBlob = (Blob)
                // doc.getPropertyValue(NuxeoMetadataConstants.NX_FILE_CONTENT);

                break;
            case EloraGeneralConstants.FILE_TYPE_ATTACHED:
                blob.setFilename(fileName);
                customProps.put("isCadAttachment", "1");
                EloraDocumentHelper.addBlob(
                        doc.getProperty(NuxeoMetadataConstants.NX_FILES_FILES),
                        customProps, blob);
                break;
            case EloraGeneralConstants.FILE_TYPE_VIEWER:
                blob.setFilename(fileName);
                EloraDocumentHelper.addBlob(
                        doc.getProperty(EloraMetadataConstants.ELORA_ELOVWR_FILE),
                        customProps, blob);
                break;
            }
        } catch (Exception e) {
            // TODO: handle exception
            throw new EloraException(e.getMessage());
        }

    }

    public static void checkInDocument(EloraConfigTable releasedStatesConfig,
            CoreSession session, EloraVersionLabelService versionLabelService,
            DocumentModel doc, String checkinComment) {
        // Save and check in the document
        doc.putContextData(VersioningService.CHECKIN_COMMENT, checkinComment);

        // Check in the document
        String nextVersionIncrement = calculateNextVersionIncrement(
                releasedStatesConfig, session, versionLabelService, doc);

        if (nextVersionIncrement.equals("major")) {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MAJOR);
        } else {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
        }
    }

    public static void disableVersioningDocument(DocumentModel doc)
            throws EloraException {
        try {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.NONE);
            doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT,
                    Boolean.TRUE);
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                    Boolean.TRUE);
            doc.putContextData(
                    NotificationConstants.DISABLE_NOTIFICATION_SERVICE,
                    Boolean.TRUE);
            doc.putContextData("disableAuditLogger", Boolean.TRUE);
            if (doc.isVersion()) {
                doc.putContextData(CoreSession.ALLOW_VERSION_WRITE,
                        Boolean.TRUE);
            }
        } catch (Exception e) {
            // TODO: handle exception
            throw new EloraException(e.getMessage());
        }
    }

    // TODO::: dejamos este método aquí o lo quitamos????
    public static void setVersioningDocument(DocumentModel realDoc,
            int majorVersion, int minorVersion) throws EloraException {
        try {
            realDoc.setPropertyValue(VersioningService.MAJOR_VERSION_PROP,
                    Long.valueOf(majorVersion));

            realDoc.setPropertyValue(VersioningService.MINOR_VERSION_PROP,
                    Long.valueOf(minorVersion));

            // TODO: Falta cambiar versionLabel un notfyevent??
        } catch (Exception e) {
            // TODO: handle exception
            throw new EloraException(e.getMessage());
        }
    }

    // TODO Probau ondo dabilela
    public static String calculateNextMajorVersion(CoreSession session,
            DocumentModel realDoc) throws EloraException {

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        EloraVersionLabelService versionLabelService = Framework.getService(EloraVersionLabelService.class);
        Long major = (Long) realDoc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                releasedStatesConfig, session, versionLabelService, realDoc);
        if (nextIncrement.equals("major")) {
            major++;
        }

        return versionLabelService.translateMajor(major).toString();

    }

    // TODO Probau ondo dabilela
    public static String calculateNextMinorVersion(CoreSession session,
            DocumentModel realDoc) throws EloraException {

        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        EloraVersionLabelService versionLabelService = Framework.getService(EloraVersionLabelService.class);
        Long minor = (Long) realDoc.getPropertyValue(VersioningService.MINOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                releasedStatesConfig, session, versionLabelService, realDoc);
        if (nextIncrement.equals("minor")) {
            minor++;
        } else {
            minor = (long) 0;
        }

        return versionLabelService.translateMinor(minor).toString();

    }

    // TODO Probau ondo dabilela
    public static String calculateNextVersionLabel(
            EloraConfigTable releasedStatesConfig, CoreSession session,
            EloraVersionLabelService versionLabelService, DocumentModel realDoc)
            throws EloraException {
        String nextVersionLabel = "";

        Long major = (Long) realDoc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP);
        Long minor = (Long) realDoc.getPropertyValue(VersioningService.MINOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                releasedStatesConfig, session, versionLabelService, realDoc);
        switch (nextIncrement) {
        case "major":
            major++;
            minor = (long) 0;
            break;
        case "minor":
            minor++;
            break;
        default:
            throw new EloraException(
                    "Unsupported increment option when calculating the next version label.");
        }

        nextVersionLabel = versionLabelService.translateMajor(major).toString()
                + "." + versionLabelService.translateMinor(minor).toString();

        return nextVersionLabel;

    }

    // TODO Probau ondo dabilela
    public static String calculateNextVersionIncrement(
            EloraConfigTable releasedStatesConfig, CoreSession session,
            EloraVersionLabelService versionLabelService, DocumentModel realDoc) {

        long major = ((Long) realDoc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP)).longValue();

        String nextIncrement = "minor";

        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);

        // Get all versions with the same major, and check their lifecycle state
        List<DocumentModel> docVersions = session.getVersions(realDoc.getRef());
        for (DocumentModel docVersion : docVersions) {
            long versionMajor = ((Long) docVersion.getPropertyValue(VersioningService.MAJOR_VERSION_PROP)).longValue();
            if (versionMajor == major) {
                String versionState = docVersion.getCurrentLifeCycleState();
                if (Arrays.asList(releasedStates).contains(versionState)) {
                    nextIncrement = "major";
                }

            }
        }

        return nextIncrement;
    }

    /**
     * Function to set a document as checked out.
     *
     * @param session
     * @param doc
     * @return
     */
    public static void checkOutDocument(DocumentModel doc) {

        // TODO EloraVersioningService-en followTransitionByOption protected
        // denez,
        // ezin zaio deitu checkout egiteko funtzioetatik, eta gainera Document
        // erabiltzen duenez, ezin da funtzio batera atera.
        //
        // Modu ona izango zen dokumentua Dirty jarri ahal izatea, eta gero
        // saveDocument egitea checked out jartzeko, doc.checkOut() jarri ordez,
        // baina oraingoz ez dugu lortu.

        if (!doc.isCheckedOut()) {
            doc.checkOut();

            // TODO Hau aldatzen bada,
            // EloraVersioningService.followTransitionByOption be aldatu behar
            // da.

            // We have to emulate the saveDocument, the document has to follow a
            // transition in some states
            String lifecycleState = doc.getCurrentLifeCycleState();
            if (EloraLifeCycleConstants.CAD_APPROVED.equals(lifecycleState)) {
                doc.followTransition(EloraLifeCycleConstants.CAD_TRANS_BACK_TO_PRELIMINARY);
            }

        }
    }

    /**
     * Restore document to certain version including relations
     *
     * @param doc
     * @param version
     * @return
     * @throws EloraException
     */
    public static DocumentModel restoreToVersion(DocumentModel doc,
            VersionModel version,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        // If the document is a proxy, get its source
        if (doc.isProxy()) {
            doc = session.getSourceDocument(doc.getRef());
        }

        DocumentModel restoredDocument = session.restoreToVersion(doc.getRef(),
                new IdRef(version.getId()), true, true);

        session.save();

        EloraRelationHelper.restoreRelations(restoredDocument, version,
                eloraDocumentRelationManager, session);

        return restoredDocument;
    }

    public static void relateDocumentWithBinaries(DocumentModel wcDoc,
            EloraFileInfo fileInfo, String type, CoreSession session)
            throws EloraException {

        if (fileInfo != null) {
            if (fileInfo.getBatch() != null && fileInfo.getFileName() != null) {
                EloraDocumentHelper.relateBatchWithDoc(wcDoc,
                        fileInfo.getFileId(), fileInfo.getBatch(), type,
                        fileInfo.getFileName(), fileInfo.getHash());
            }
        }
    }

    /**
     * @param docM
     * @return true if it is a BOM document.
     */
    public static boolean isBomDocument(DocumentModel docM) {

        boolean isBomDoc = false;

        isBomDoc = docM.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT);

        return isBomDoc;
    }

    /**
     * @param docM
     * @return true if it is a CAD document
     */
    public static boolean isCadDocument(DocumentModel docM) {

        boolean isCadDoc = false;

        isCadDoc = docM.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT);

        return isCadDoc;
    }

    /** Gets the filter that hides HiddenInNavigation and deleted objects. */
    public static Filter getDocumentFilter() {
        Filter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION,
                false);
        Filter lcFilter = new LifeCycleFilter(LifeCycleConstants.DELETED_STATE,
                false);
        return new CompoundFilter(facetFilter, lcFilter);
    }

    public static DocumentModelList getPromotableDocList(DocumentRef docRef,
            Statement stmt, boolean isSpecial, CoreSession session)
            throws EloraException {
        // TODO: Si en un futuro habilitamos la posibilidad de hacer restore a
        // una revisión anterior mirar esta función. Si el major no tiene ningún
        // released, al sacar todas las versiones de ese major puede que haya
        // que sacar del latestVersion en vez de tener como referencia el wc
        DocumentModelList promotableDocs = new DocumentModelListImpl();
        if (isSpecial) {
            // Get related released ones. If there is no released document get
            // all related
            promotableDocs = EloraRelationHelper.getSpecialRelatedReleased(
                    docRef, stmt, session);
        } else {
            // Get all released versions + latest versions if they are not
            // included
            promotableDocs = getReleasedAndLatestVersions(docRef, session);
        }
        return promotableDocs;
    }

    public static DocumentModelList getMajorVersionDocList(DocumentRef docRef,
            CoreSession session) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        String majorVersion = wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION).toString();
        String versionVersionableId = wcDoc.getId();

        String query = EloraQueryFactory.getMajorVersionDocs(
                versionVersionableId, majorVersion);

        DocumentModelList releasedDocs = session.query(query);

        return releasedDocs;
    }

    private static DocumentModelList getReleasedAndLatestVersions(
            DocumentRef docRef, CoreSession session) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        String majorVersion = wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION).toString();
        EloraConfigTable releasedStatesConfig = EloraConfigHelper.getReleasedLifecycleStatesConfig();
        EloraConfigTable obsoleteStatesConfig = EloraConfigHelper.getObsoleteLifecycleStatesConfig();
        String[] releasedStates = releasedStatesConfig.getKeys().toArray(
                new String[0]);
        String[] obsoleteStates = obsoleteStatesConfig.getKeys().toArray(
                new String[0]);
        String versionVersionableId = wcDoc.getId();

        String query = EloraQueryFactory.getReleasedDocs(versionVersionableId,
                releasedStates);
        DocumentModelList releasedDocs = session.query(query);
        if (releasedDocs.size() > 0) {
            boolean completed = false;
            for (DocumentModel releasedDoc : releasedDocs) {
                String promotableMajor = releasedDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION).toString();
                if (promotableMajor.equals(majorVersion)) {
                    // If one of them has majorVersion then finish
                    completed = true;
                    break;
                }
            }
            if (!completed) {
                // If no one has majorVersion then get all versions within major
                query = EloraQueryFactory.getMajorVersionDocs(
                        versionVersionableId, majorVersion);
                releasedDocs.addAll(0, session.query(query));
            }
        } else {
            // If there is no released docs, get all versions
            releasedDocs.addAll(session.getVersions(docRef));
        }

        return releasedDocs;
    }

    public static List<Blob> getBomRelatedCadFiles(DocumentModel doc) {
        Resource predicate = new QNameResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT, "");
        DocumentModelList relDocs = RelationHelper.getObjectDocuments(doc,
                predicate);

        List<Blob> fileList = new ArrayList<>();
        for (DocumentModel relDoc : relDocs) {
            BlobHolder bh = new DocumentBlobHolder(relDoc,
                    EloraMetadataConstants.ELORA_ELOVWR_FILE);
            fileList.add(bh.getBlob());
        }

        return fileList;
    }

    public static Blob getDocumentViewerFile(DocumentModel doc) {
        BlobHolder bh = new DocumentBlobHolder(doc,
                EloraMetadataConstants.ELORA_ELOVWR_FILE);
        return bh.getBlob();
    }

    public static void addViewerBlob(DocumentModel doc, Blob blob) {
        DocumentHelper.addBlob(
                doc.getProperty(EloraMetadataConstants.ELORA_ELOVWR_FILE), blob);
    }

}
