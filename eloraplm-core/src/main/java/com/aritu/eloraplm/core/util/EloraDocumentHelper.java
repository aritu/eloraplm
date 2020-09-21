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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.constants.QueriesConstants;
import com.aritu.eloraplm.constants.VersionStatusConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.exceptions.DocumentAlreadyLockedException;
import com.aritu.eloraplm.exceptions.DocumentInUnlockableStateException;
import com.aritu.eloraplm.exceptions.DocumentLockRightsException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.UnlockCheckedOutDocumentException;
import com.aritu.eloraplm.queries.EloraQueryFactory;
import com.aritu.eloraplm.queries.util.EloraQueryHelper;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

public class EloraDocumentHelper {

    private static final Log log = LogFactory.getLog(EloraDocumentHelper.class);

    public static DocumentModel lockDocument(DocumentModel doc)
            throws DocumentAlreadyLockedException,
            DocumentInUnlockableStateException, EloraException,
            DocumentLockRightsException {
        String logInitMsg = "[lockDocument] ";

        CoreSession session = doc.getCoreSession();
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        boolean isLockable = EloraDocumentHelper.getIsCurrentStateLockable(
                wcDoc);
        if (isLockable) {
            NuxeoPrincipal user = (NuxeoPrincipal) session.getPrincipal();
            if (!wcDoc.isLocked()) {
                try {
                    session.setLock(wcDoc.getRef());
                } catch (DocumentSecurityException e) {
                    throw new DocumentLockRightsException(doc);
                }
                log.trace(logInitMsg + "Document |" + wcDoc.getId()
                        + "| locked.");
                // We get the document again to have updated lock
                // information
                wcDoc = session.getDocument(wcDoc.getRef());
            } else {
                String lockOwner = wcDoc.getLockInfo().getOwner();
                if (!user.getName().equals(lockOwner)
                        && !user.isAdministrator()) {
                    throw new DocumentAlreadyLockedException(doc, lockOwner);
                }
            }
        } else {
            throw new DocumentInUnlockableStateException(doc,
                    wcDoc.getCurrentLifeCycleState());
        }
        return wcDoc;
    }

    public static DocumentModel unlockDocument(DocumentModel doc)
            throws DocumentLockRightsException,
            UnlockCheckedOutDocumentException {
        String logInitMsg = "[unlockDocument] ";

        CoreSession session = doc.getCoreSession();
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());

        if (wcDoc.isLocked()) {
            String lockOwner = wcDoc.getLockInfo().getOwner();
            NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
            if (principal.isAdministrator()
                    || principal.getName().equals(lockOwner)) {

                EloraVersionLabelService eloraVersionLabelService = Framework.getService(
                        EloraVersionLabelService.class);
                if (wcDoc.isVersionable() && wcDoc.isCheckedOut()
                        && !(wcDoc.getVersionLabel().equals(
                                eloraVersionLabelService.getZeroVersion()))) {
                    throw new UnlockCheckedOutDocumentException(wcDoc);
                }

                try {
                    wcDoc.removeLock();

                    log.trace(logInitMsg + "Document |" + wcDoc.getId()
                            + "| unlocked.");
                    // We get the document again to have updated lock
                    // information
                    doc = session.getDocument(doc.getRef());

                } catch (DocumentSecurityException e) {
                    throw new DocumentLockRightsException(doc);
                }

            } else {
                throw new DocumentLockRightsException(doc);
            }

        }

        return doc;
    }

    public static EloraLockInfo getLockInfo(DocumentModel wcDoc)
            throws EloraException {
        if (wcDoc.isImmutable()) {
            throw new EloraException(
                    "Provided document is not a working copy.");
        }

        EloraLockInfo eloraLockInfo;
        if (wcDoc.isLocked()) {
            Lock lockInfo = wcDoc.getLockInfo();
            eloraLockInfo = new EloraLockInfo(wcDoc.isLocked(),
                    lockInfo.getOwner(), lockInfo.getCreated().getTime(),
                    getIsCurrentStateLockable(wcDoc));
        } else {
            eloraLockInfo = new EloraLockInfo(false, "", null,
                    getIsCurrentStateLockable(wcDoc));
        }
        return eloraLockInfo;
    }

    public static boolean getIsCurrentStateLockable(DocumentModel wcDoc)
            throws EloraException {

        if (wcDoc.isImmutable()) {
            throw new EloraException(
                    "Provided document is not a working copy.");
        }

        boolean isLockable = LifecyclesConfig.lockableStatesList.contains(
                wcDoc.getCurrentLifeCycleState());
        return isLockable;
    }

    public static boolean isLockedByUserOrAdmin(DocumentModel doc,
            CoreSession session) {
        NuxeoPrincipal user = (NuxeoPrincipal) session.getPrincipal();
        DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
        if (wcDoc.isLocked()
                && (user.getName().equals(wcDoc.getLockInfo().getOwner())
                        || user.isAdministrator())) {
            return true;
        } else {
            return false;
        }
    }

    public static DocumentModel setForcedDocVersionLabel(DocumentModel doc,
            String versionLabel) throws EloraException {
        if (versionLabel == null || versionLabel.isEmpty()) {
            throw new EloraException(
                    "Provided forced version label for document |" + doc.getId()
                            + "| is empty.");
        }
        String[] splittedVersionLabel = versionLabel.split("\\.");
        if (splittedVersionLabel.length != 2) {
            throw new EloraException("Provided forced version label |"
                    + versionLabel + "| for document |" + doc.getId()
                    + "| has incorrect format, and it is not possible to get the major and the minor.");
        }
        doc.setPropertyValue(VersioningService.MAJOR_VERSION_PROP,
                Long.valueOf(splittedVersionLabel[0]));
        doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP,
                Long.valueOf(splittedVersionLabel[1]));

        doc.putContextData(EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL,
                versionLabel);

        return doc;

    }

    public static DocumentModel getLatestReleasedVersionOrLatestVersion(
            DocumentModel doc) throws EloraException {

        DocumentModel latestReleased = getLatestReleasedVersion(doc);
        if (latestReleased == null) {
            latestReleased = getLatestVersion(doc);
        }
        return latestReleased;
    }

    public static DocumentModel getLatestReleasedVersion(DocumentModel doc)
            throws EloraException {

        CoreSession session = doc.getCoreSession();
        DocumentModel latestReleased = null;
        String versionVersionableId = session.getWorkingCopy(
                doc.getRef()).getId();

        String query = EloraQueryFactory.getReleasedDocsQuery(doc.getType(),
                versionVersionableId, QueriesConstants.SORT_ORDER_DESC);
        DocumentModelList releasedDocs = session.query(query);
        if (releasedDocs.size() > 0) {
            latestReleased = releasedDocs.get(0);
        }
        return latestReleased;
    }

    public static DocumentModel getLatestVersion(DocumentModel doc)
            throws EloraException {
        CoreSession session = doc.getCoreSession();
        DocumentModel wcDoc = null;
        if (doc.isImmutable()) {
            wcDoc = session.getWorkingCopy(doc.getRef());
        } else {
            wcDoc = doc;
        }

        DocumentModel baseDoc = getBaseVersion(wcDoc);
        return baseDoc;
    }

    public static DocumentModel getMajorReleasedVersion(DocumentModel doc)
            throws EloraException {

        DocumentModel releasedDoc = null;
        Long majorVersion = (Long) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

        // We guess there is at least one released state in configuration. If
        // not it will crash
        if (LifecyclesConfig.releasedStatesList.isEmpty()) {
            throw new EloraException(
                    "There must be at least one released state in configuration");
        }

        CoreSession session = doc.getCoreSession();
        String versionVersionableId = session.getWorkingCopy(
                doc.getRef()).getId();
        String query = EloraQueryFactory.getMajorReleasedVersionQuery(
                doc.getType(), versionVersionableId, majorVersion);

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

    public static DocumentModelList getNotReleasedDocListInMajorVersion(
            DocumentModel doc) throws EloraException {

        Long majorVersion = (Long) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

        // We guess there is at least one unreleased state in configuration.
        // If not it will crash
        if (LifecyclesConfig.unreleasedStatesList.isEmpty()) {
            throw new EloraException(
                    "There must be at least one unreleased state in configuration");
        }

        CoreSession session = doc.getCoreSession();
        String versionVersionableId = session.getWorkingCopy(
                doc.getRef()).getId();
        String query = EloraQueryFactory.getNotReleasedDocListInMajorVersion(
                doc.getType(), versionVersionableId, majorVersion);

        DocumentModelList notReleasedDocs = session.query(query);

        return notReleasedDocs;
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

    public static QNameResourceImpl getDocumentResource(String docUid,
            String repositoryName) {
        String objectDocUid = docUid;
        String localName = repositoryName + "/" + objectDocUid;
        return new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                localName);
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

    public static void relateBatchWithDoc(DocumentModel doc, int fileId,
            String batch, String fileType, String fileName, String hash)
            throws EloraException {
        String logInitMsg = "[relateBatchWithDoc] ";
        try {
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            Blob blob = bm.getBlob(batch, String.valueOf(fileId));

            if (blob == null) {
                throw new EloraException("Blob not found with batch |" + batch
                        + "| and  fileId |" + String.valueOf(fileId) + "|");
            }

            BlobHolder bh = new SimpleBlobHolder(blob);
            String digest = bh.getHash().toUpperCase();
            if (!digest.equals(hash.toUpperCase())) {
                throw new EloraException("Incorrect hash in batch |" + batch
                        + "|. Document hash: |" + digest + "| Passed hash: |"
                        + hash.toUpperCase() + "|");
            }

            if (RequestContext.getActiveContext() != null) {
                RequestContext.getActiveContext().addRequestCleanupHandler(
                        new RequestCleanupHandler() {
                            @Override
                            public void cleanup(HttpServletRequest request) {
                                BatchManager bm = Framework.getLocalService(
                                        BatchManager.class);
                                bm.clean(batch);
                            }
                        });
            }

            if (blob.getFilename() != null) {
                MimetypeRegistryService mtrs = (MimetypeRegistryService) Framework.getLocalService(
                        MimetypeRegistry.class);
                try {
                    String mimetype = mtrs.getMimetypeFromFilename(
                            blob.getFilename());
                    blob.setMimeType(mimetype);
                } catch (MimetypeNotFoundException e) {
                    log.trace(logInitMsg
                            + "An error occurred getting mimetype from filename. Exception: "
                            + e.getMessage());
                }
            }

            // Add Blob to document
            switch (fileType) {
            case EloraGeneralConstants.FILE_TYPE_CONTENT:
                if (fileName != null) {
                    blob.setFilename(fileName);
                }
                DocumentHelper.addBlob(
                        doc.getProperty(NuxeoMetadataConstants.NX_FILE_CONTENT),
                        blob);
                break;
            case EloraGeneralConstants.FILE_TYPE_CAD_ATTACHMENT:
                if (fileName != null) {
                    blob.setFilename(fileName);
                }

                DocumentHelper.addBlob(
                        doc.getProperty(
                                EloraMetadataConstants.ELORA_CADATTS_FILES),
                        blob);
                break;
            case EloraGeneralConstants.FILE_TYPE_VIEWER:
                if (fileName != null) {
                    blob.setFilename(fileName);
                }

                // We add it to the viewer base file and the viewer file
                DocumentHelper.addBlob(
                        doc.getProperty(
                                EloraMetadataConstants.ELORA_ELOVWR_BASEFILE),
                        blob);
                DocumentHelper.addBlob(
                        doc.getProperty(
                                EloraMetadataConstants.ELORA_ELOVWR_FILE),
                        blob);
                break;
            }
        } catch (EloraException e) {
            throw e;
        } catch (Exception e) {
            throw new EloraException(
                    "Unknown exception relating batch with document: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
        }
    }

    public static void setupCheckIn(
            EloraVersionLabelService versionLabelService, DocumentModel doc,
            String checkinComment) {
        // Save and check in the document
        doc.putContextData(VersioningService.CHECKIN_COMMENT, checkinComment);

        // Check in the document
        String nextVersionIncrement = calculateNextVersionIncrement(
                versionLabelService, doc);

        if (nextVersionIncrement.equals("major")) {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MAJOR);
        } else {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
        }
    }

    public static boolean isWorkingCopy(DocumentModel doc) {
        return !doc.isProxy() && !doc.isVersion() ? true : false;
    }

    public static boolean isWcOrAvObsolete(DocumentModel doc)
            throws EloraException {

        if (!doc.isImmutable()) {
            return isDocObsolete(doc);
        } else {
            return isAvObsolete(doc);
        }
    }

    public static boolean isWcObsolete(DocumentModel doc)
            throws EloraException {
        return isDocObsolete(doc);
    }

    public static boolean isAvObsolete(DocumentModel doc)
            throws EloraException {
        boolean isWcObsolete = isDocObsolete(
                doc.getCoreSession().getWorkingCopy(doc.getRef()));
        boolean isAvObsolete = isDocObsolete(doc);
        return isWcObsolete || isAvObsolete;
    }

    private static boolean isDocObsolete(DocumentModel doc)
            throws EloraException {
        if (doc != null) {
            if (LifecyclesConfig.obsoleteStatesList.contains(
                    doc.getCurrentLifeCycleState())) {
                return true;
            }
        }

        return false;
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
                doc.putContextData("disableMajorLetterTranslation",
                        Boolean.TRUE);
            }
        } catch (Exception e) {
            // TODO: handle exception
            throw new EloraException(e.getMessage());
        }
    }

    /**
     * @param session
     * @param reference
     * @param primaryType
     * @return get the first wc document
     * @throws EloraException
     */
    public static DocumentModel getWcDocumentByTypeAndReference(
            CoreSession session, String reference, String primaryType)
            throws EloraException {

        String query = EloraQueryFactory.getWcDocsByTypeAndReferenceQuery(
                primaryType, reference);

        return EloraQueryHelper.executeGetFirstQuery(query, session);
    }

    public static DocumentModel getWcDocumentByTypeListAndReference(
            CoreSession session, String reference, List<String> primaryTypes)
            throws EloraException {

        String query = EloraQueryFactory.getWcDocsByTypeListAndReferenceQuery(
                reference, primaryTypes);

        return EloraQueryHelper.executeGetFirstQuery(query, session);
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
    public static String calculateNextMajorVersion(DocumentModel realDoc)
            throws EloraException {

        EloraVersionLabelService versionLabelService = Framework.getService(
                EloraVersionLabelService.class);
        Long major = (Long) realDoc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                versionLabelService, realDoc);
        if (nextIncrement.equals("major")) {
            major++;
        }

        return versionLabelService.translateMajor(major).toString();

    }

    // TODO Probau ondo dabilela
    public static String calculateNextMinorVersion(DocumentModel realDoc)
            throws EloraException {

        EloraVersionLabelService versionLabelService = Framework.getService(
                EloraVersionLabelService.class);
        Long minor = (Long) realDoc.getPropertyValue(
                VersioningService.MINOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                versionLabelService, realDoc);
        if (nextIncrement.equals("minor")) {
            minor++;
        } else {
            minor = (long) 0;
        }

        return versionLabelService.translateMinor(minor).toString();

    }

    // TODO Probau ondo dabilela
    public static String calculateNextVersionLabel(
            EloraVersionLabelService versionLabelService, DocumentModel realDoc)
            throws EloraException {
        String nextVersionLabel = "";

        Long major = (Long) realDoc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP);
        Long minor = (Long) realDoc.getPropertyValue(
                VersioningService.MINOR_VERSION_PROP);

        String nextIncrement = calculateNextVersionIncrement(
                versionLabelService, realDoc);
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
            EloraVersionLabelService versionLabelService,
            DocumentModel realDoc) {

        long major = ((Long) realDoc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP)).longValue();

        String nextIncrement = "minor";

        // Get all versions with the same major, and check their lifecycle state
        List<DocumentModel> docVersions = realDoc.getCoreSession().getVersions(
                realDoc.getRef());
        for (DocumentModel docVersion : docVersions) {
            long versionMajor = ((Long) docVersion.getPropertyValue(
                    VersioningService.MAJOR_VERSION_PROP)).longValue();
            if (versionMajor == major) {
                String versionState = docVersion.getCurrentLifeCycleState();
                if (LifecyclesConfig.releasedStatesList.contains(
                        versionState)) {
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
    public static DocumentModel checkOutDocument(DocumentModel doc) {

        // TODO EloraVersioningService-en followTransitionByOption protected
        // denez, ezin zaio deitu checkout egiteko funtzioetatik, eta gainera
        // Document erabiltzen duenez, ezin da funtzio batera atera.
        //
        // Modu ona izango zen dokumentua Dirty jarri ahal izatea, eta gero
        // saveDocument egitea checked out jartzeko, doc.checkOut() jarri ordez,
        // baina oraingoz ez dugu lortu.

        doc.refresh();
        if (!doc.isCheckedOut()) {
            doc.checkOut();

            // TODO Hau aldatzen bada,
            // EloraVersioningService.followTransitionByOption be aldatu behar
            // da.

            // We have to emulate the saveDocument, the document has to follow a
            // transition in some states
            String lifecycleState = doc.getCurrentLifeCycleState();
            if (EloraLifeCycleConstants.APPROVED.equals(lifecycleState)) {
                doc.followTransition(
                        EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY);
            }

        }

        doc.refresh();
        return doc;
    }

    /**
     * Restore document to certain version including relations
     *
     * @param doc
     * @param version
     * @return
     * @throws EloraException
     */
    public static DocumentModel restoreWorkingCopyToVersion(DocumentModel wcDoc,
            VersionModel version,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        // If the document is a proxy, get its source
        if (wcDoc.isProxy()) {
            wcDoc = session.getSourceDocument(wcDoc.getRef());
        }

        DocumentModel restoredDocument = restoreToVersion(wcDoc, version, true,
                true, session);

        session.save();

        EloraRelationHelper.restoreRelations(restoredDocument, version,
                eloraDocumentRelationManager, session);

        return restoredDocument;
    }

    public static DocumentModel restoreDocumentToVersion(DocumentModel doc,
            VersionModel version,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) throws EloraException {

        DocumentModel restoredDocument = restoreToVersion(doc, version, true,
                true, session);

        session.save();
        EloraRelationHelper.restoreRelations(restoredDocument, version,
                eloraDocumentRelationManager, session);
        return restoredDocument;
    }

    public static DocumentModel restoreToVersion(DocumentModel doc,
            VersionModel version, boolean skipSnapshotCreation,
            boolean skipCheckout, CoreSession session) {

        return restoreToVersion(doc.getRef(), new IdRef(version.getId()),
                skipSnapshotCreation, skipCheckout, session);
    }

    public static DocumentModel restoreToVersion(DocumentRef docRef,
            DocumentRef versionRef, boolean skipSnapshotCreation,
            boolean skipCheckout, CoreSession session) {

        DocumentModel restoredDocument = session.restoreToVersion(docRef,
                versionRef, skipSnapshotCreation, skipCheckout);

        String comment = restoredDocument.getVersionLabel();
        EloraEventHelper.fireEvent(PdmEventNames.PDM_RESTORED_EVENT,
                restoredDocument, comment);

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
        Long majorVersion = (Long) wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
        String versionVersionableId = wcDoc.getId();

        String query = EloraQueryFactory.getMajorVersionDocsQuery(
                wcDoc.getType(), versionVersionableId, majorVersion, true,
                QueriesConstants.SORT_ORDER_DESC);

        DocumentModelList releasedDocs = session.query(query);

        return releasedDocs;
    }

    public static DocumentModelList getReleasedAndLatestVersions(
            DocumentRef docRef, CoreSession session) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        Long majorVersion = (Long) wcDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
        String versionVersionableId = wcDoc.getId();

        String query = EloraQueryFactory.getReleasedDocsQuery(wcDoc.getType(),
                versionVersionableId, QueriesConstants.SORT_ORDER_DESC);
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
                query = EloraQueryFactory.getMajorVersionDocsQuery(
                        wcDoc.getType(), versionVersionableId, majorVersion,
                        false, QueriesConstants.SORT_ORDER_DESC);
                releasedDocs.addAll(0, session.query(query));
            }
        } else {
            // If there is no released docs, get all versions
            releasedDocs.addAll(session.getVersions(docRef));
        }

        return releasedDocs;
    }

    public static DocumentModelList getOlderReleasedOrObsoleteVersions(
            CoreSession session, DocumentRef docRef, long currentMajorVersion,
            int limit) throws EloraException {

        DocumentModel wcDoc = session.getWorkingCopy(docRef);
        String versionVersionableId = wcDoc.getId();
        String primaryType = wcDoc.getType();

        String query = EloraQueryFactory.getOlderReleasedOrObsoleteVersionsQuery(
                versionVersionableId, primaryType,
                QueriesConstants.SORT_ORDER_DESC, currentMajorVersion);

        DocumentModelList result = session.query(query, limit);

        return result;
    }

    public static boolean isReleased(DocumentModel doc) throws EloraException {
        if (LifecyclesConfig.releasedStatesList.contains(
                doc.getCurrentLifeCycleState())) {
            return true;
        }
        return false;
    }

    /**
     * Returns base AV document, even if WC is checked out
     *
     * @param wcDoc
     * @return
     * @throws EloraException
     */
    public static DocumentModel getBaseVersion(DocumentModel wcDoc) {
        CoreSession session = wcDoc.getCoreSession();

        // It doesn't work with proxies, first we have to get its source
        if (wcDoc.isProxy()) {
            wcDoc = session.getSourceDocument(wcDoc.getRef());
        }

        Session localSession = ((AbstractSession) session).getSession();
        Document doc = localSession.getDocumentByUUID(wcDoc.getId());

        Serializable baseVersionId = doc.getPropertyValue(
                Model.MAIN_BASE_VERSION_PROP);
        if (baseVersionId == null) {
            return null;
        }

        DocumentModel baseDoc = session.getDocument(
                new IdRef((String) baseVersionId));

        return baseDoc;
    }

    /**
     * Returns if the document is editable. Basically: For versionable docs, it
     * has to be checked out by the user. For non-versionable docs, it has to be
     * locked by the user.
     *
     * @param doc
     * @return
     */
    public static boolean isEditable(DocumentModel doc) {
        CoreSession session = doc.getCoreSession();
        String currentUser = doc.getCoreSession().getPrincipal().getName();

        // We don't check that it is not a proxy, because in some cases we enter
        // the function through a proxy
        if (session.hasPermission(doc.getRef(), SecurityConstants.WRITE)
                && !doc.isImmutable()) {

            // Versionable
            if (doc.isVersionable()) {
                if (isCheckedOutByMe(doc)) {
                    return true;
                }
            } else {
                // Non-versionable, but lock required
                if (doc.hasFacet(
                        EloraFacetConstants.FACET_LOCK_REQUIRED_TO_EDIT)) {
                    if (doc.isLocked() && doc.getLockInfo().getOwner().equals(
                            currentUser)) {
                        return true;
                    }
                }
                // Non-versionable and free to edit
                else {
                    if (!doc.isLocked() || doc.getLockInfo().getOwner().equals(
                            currentUser)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isCheckedOutByMe(DocumentModel doc) {
        try {
            checkThatIsCheckedOutByMe(doc);
            return true;
        } catch (DocumentNotCheckedOutException e) {
            return false;
        }
    }

    public static void checkThatIsCheckedOutByMe(DocumentModel doc)
            throws DocumentNotCheckedOutException {
        // String logInitMsg = "[checkThatIsCheckedOutByMe]";

        String currentUser = doc.getCoreSession().getPrincipal().getName();
        if (!doc.isCheckedOut() || !(doc.isLocked()
                && doc.getLockInfo().getOwner().equals(currentUser))) {
            throw new DocumentNotCheckedOutException(doc);
        }
        // log.trace(logInitMsg + "Document checked out by user");
    }

    public static void copyProperties(DocumentModel from, DocumentModel to) {
        copyProperties(from, to, null, false);
    }

    public static void copyProperties(DocumentModel from, DocumentModel to,
            boolean excludeEmptyProperties) {
        copyProperties(from, to, null, excludeEmptyProperties);
    }

    public static void copyProperties(DocumentModel from, DocumentModel to,
            Map<String, List<String>> excludedProperties) {
        copyProperties(from, to, excludedProperties, false);

    }

    public static void copyProperties(DocumentModel from, DocumentModel to,
            Map<String, List<String>> excludedProperties,
            boolean excludeEmptyProperties) {

        // TODO: En un futuro ver la forma de evitar los schemas que no nos
        // interesan pasar de un documento a otro. Ahora añadimos una
        // condicion pero puede que sea mejor solucionarlo dandole una
        // vuelta a como registramos los tipos de documentos (mirar tema
        // draft)
        List<String> ignoredSchemas = new ArrayList<String>();
        ignoredSchemas.add(NuxeoMetadataConstants.NX_SCHEMA_UID);
        ignoredSchemas.add(EloraSchemaConstants.STATES_LOG);

        for (String schema : to.getSchemas()) {
            if (from.hasSchema(schema) && !ignoredSchemas.contains(schema)) {
                // We create a shallow copy so we don't alter the "from" doc
                Map<String, Object> schemaProps = new HashMap<String, Object>(
                        from.getProperties(schema));

                if (excludeEmptyProperties) {
                    for (Map.Entry<String, Object> schemaProp : schemaProps.entrySet()) {
                        boolean isEmpty = false;

                        Object schemaPropValueObject = schemaProp.getValue();
                        if (schemaPropValueObject == null) {
                            isEmpty = true;
                        } else if (schemaPropValueObject instanceof List) {
                            Object[] schemaPropValueArray = ((List<?>) schemaPropValueObject).toArray();
                            if (schemaPropValueArray.length == 0) {
                                isEmpty = true;
                            }
                        } else if (schemaPropValueObject instanceof Long) {
                            long longValue = ((Long) schemaPropValueObject).longValue();
                            if (longValue == 0) {
                                isEmpty = true;
                            }
                        }
                        if (isEmpty) {
                            if (excludedProperties == null) {
                                excludedProperties = new HashMap<String, List<String>>();
                                List<String> excludedMetadataList = new ArrayList<String>();
                                excludedMetadataList.add(schemaProp.getKey());
                                excludedProperties.put(schema,
                                        excludedMetadataList);
                            } else if (!excludedProperties.containsKey(schema)
                                    || excludedProperties.get(schema) == null) {
                                List<String> excludedMetadataList = new ArrayList<String>();
                                excludedMetadataList.add(schemaProp.getKey());
                                excludedProperties.put(schema,
                                        excludedMetadataList);
                            } else if (!excludedProperties.get(schema).contains(
                                    schemaProp.getKey())) {
                                excludedProperties.get(schema).add(
                                        schemaProp.getKey());
                            }
                        }
                    }
                }

                if (excludedProperties != null
                        && !excludedProperties.isEmpty()) {
                    if (excludedProperties.containsKey(schema)) {
                        List<String> excludedSchemaProps = excludedProperties.get(
                                schema);
                        for (String excludedProp : excludedSchemaProps) {
                            schemaProps.remove(excludedProp);
                        }
                    }
                }

                to.setProperties(schema, schemaProps);
            }
        }
    }

    public static List<String> getUidListFromDocList(List<DocumentModel> docs) {
        List<String> uidList = new ArrayList<String>();
        for (DocumentModel doc : docs) {
            uidList.add(doc.getId());
        }
        return uidList;
    }

    public static Long getLatestMajorFromDocList(List<DocumentModel> docs) {
        Long latestMajor = (Long) docs.get(0).getPropertyValue(
                NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);

        for (int i = 1; i < docs.size(); i++) {
            Long major = (Long) docs.get(i).getPropertyValue(
                    NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
            if (latestMajor < major) {
                latestMajor = major;
            }
        }
        return latestMajor;
    }

    public static String getVersionStatus(DocumentModel currentDoc,
            DocumentModel wcDoc) throws EloraException {
        String versionStatus = VersionStatusConstants.VERSION_STATUS_NORMAL;

        if (wcDoc != null) {

            // Obsolete
            if (isWcObsolete(wcDoc)) {
                versionStatus = VersionStatusConstants.VERSION_STATUS_WC_OBSOLETE;
            }
            // Version changes
            else {

                Long currentMajor = (Long) currentDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
                Long currentMinor = (Long) currentDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MINOR_VERSION);
                Long wcMajor = (Long) wcDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
                Long wcMinor = (Long) wcDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MINOR_VERSION);

                if (currentMajor == wcMajor) {
                    if (wcDoc.isCheckedOut()) {
                        versionStatus = VersionStatusConstants.VERSION_STATUS_WC_CHECKED_OUT;
                    } else {
                        if (currentMinor != wcMinor) {
                            if (LifecyclesConfig.releasedStatesList.contains(
                                    wcDoc.getCurrentLifeCycleState())) {
                                versionStatus = VersionStatusConstants.VERSION_STATUS_NEWER_RELEASED_EXISTS;
                            } else {
                                versionStatus = VersionStatusConstants.VERSION_STATUS_NEWER_NON_RELEASED_EXISTS;
                            }
                        }
                    }
                } else {
                    boolean newerReleasedVersionExists = EloraQueryFactory.checkIfNewerReleasedVersionExists(
                            currentMajor, currentMinor,
                            currentDoc.getVersionSeriesId(),
                            currentDoc.getCoreSession());

                    if (newerReleasedVersionExists) {
                        versionStatus = VersionStatusConstants.VERSION_STATUS_NEWER_RELEASED_EXISTS;
                    } else {

                        if (wcDoc.isCheckedOut()) {
                            versionStatus = VersionStatusConstants.VERSION_STATUS_WC_CHECKED_OUT;
                        } else {
                            versionStatus = VersionStatusConstants.VERSION_STATUS_NEWER_NON_RELEASED_EXISTS;
                        }
                    }
                }
            }
        }

        return versionStatus;
    }

    // TODO Onena izango zan truko hau erabili beharrik ez eukitzea dokumentua
    // dirty jarteko
    public static DocumentModel setDocumentDirty(DocumentModel doc) {
        Calendar now = Calendar.getInstance();
        doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_MODIFIED, now);
        return doc;
    }

    public static DocumentModel updateContributorAndModified(
            DocumentModel currentDoc, boolean save) {
        CoreSession session = currentDoc.getCoreSession();
        String principalName = session.getPrincipal().getName();
        currentDoc.setPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR, principalName);
        Calendar now = Calendar.getInstance();
        currentDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_MODIFIED, now);

        String[] contributorsArray = (String[]) currentDoc.getProperty(
                "dublincore", "contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        // make it resizable
        contributorsList = new ArrayList<String>(contributorsList);

        if (!contributorsList.contains(principalName)) {
            contributorsList.add(principalName);
            String[] contributorListIn = new String[contributorsList.size()];
            contributorsList.toArray(contributorListIn);
            currentDoc.setProperty("dublincore", "contributors",
                    contributorListIn);
        }

        if (save) {
            session.saveDocument(currentDoc);
        }

        return currentDoc;
    }

    public static DocumentModel addContributor(DocumentModel doc,
            String contributor, boolean isNew) {

        String[] contributorsArray = (String[]) doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_CONTRIBUTORS);
        List<String> contributorsList = new ArrayList<String>();

        if (contributorsArray != null && contributorsArray.length > 0) {
            contributorsList = Arrays.asList(contributorsArray);
            // make it resizable
            contributorsList = new ArrayList<String>(contributorsList);
        }

        if (!contributorsList.contains(contributor)) {
            contributorsList.add(contributor);
            String[] contributorListIn = new String[contributorsList.size()];
            contributorsList.toArray(contributorListIn);
            doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_CONTRIBUTORS,
                    contributorListIn);
        }

        doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR,
                contributor);
        if (isNew) {
            doc.setPropertyValue(NuxeoMetadataConstants.NX_DC_CREATOR,
                    contributor);
        }

        return doc;
    }

    /**
     * Checks if the specified document satisfies specified filter.
     *
     * @param doc
     * @param filterName
     * @return true if the document satisfies specified filter
     */
    public static boolean checkFilter(DocumentModel doc, String filterName) {
        boolean result = false;

        ActionManager actionManager = Framework.getLocalService(
                ActionManager.class);

        if (actionManager.checkFilter(filterName, createActionContext(doc))) {
            result = true;
        }

        return result;
    }

    /**
     * Creates an ActionContext used for checking document filters. Inspired in
     * ActionContextProvider Nuxeo class.
     *
     * @param document
     * @return
     */
    private static ActionContext createActionContext(DocumentModel document) {
        ActionContext actionCtx;
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            actionCtx = new SeamActionContext();
        } else {
            actionCtx = new JSFActionContext(faces);
        }
        actionCtx.setCurrentDocument(document);
        CoreSession session = document.getCoreSession();
        actionCtx.setDocumentManager(session);
        actionCtx.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        actionCtx.putLocalVariable("SeamContext", new SeamContextHelper());
        return actionCtx;
    }

    public static void updateCheckinComment(DocumentModel docModel,
            String checkinComment) throws EloraException {
        CoreSession session = docModel.getCoreSession();

        if (!docModel.isImmutable()) {
            throw new EloraException(
                    "Document is working copy, so it does not have a checkin comment.");
        }

        if (docModel.isProxy()) {
            docModel = session.getSourceDocument(docModel.getRef());
        }

        Session localSession = ((AbstractSession) session).getSession();
        Document doc = localSession.getDocumentByUUID(docModel.getId());
        doc.setReadOnly(false);
        doc.setPropertyValue(Model.VERSION_DESCRIPTION_PROP, checkinComment);
    }

    public static boolean isDocumentUnderTemplateRoot(DocumentModel doc,
            CoreSession session) {

        boolean result = false;

        DocumentModel parentDoc = session.getParentDocument(doc.getRef());

        if (parentDoc != null) {
            if (parentDoc.getType().equals(
                    NuxeoDoctypeConstants.TEMPLATE_ROOT)) {
                return true;
            } else {
                result = isDocumentUnderTemplateRoot(parentDoc, session);
            }
        }
        return result;
    }

}
