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
package com.aritu.eloraplm.integration.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.VersionStatusConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraFileInfo;
import com.aritu.eloraplm.core.util.EloraMessageHelper;
import com.aritu.eloraplm.exceptions.ConnectorIsObsoleteException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.restoperations.util.VersionInfo;
import com.aritu.eloraplm.integration.restoperations.util.CadFileInfoRequestDoc;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraIntegrationHelper {

    public static final String OPERATION_TRY_CHECKIN = "TryCheckin";

    public static final String OPERATION_TRY_OVERWRITE = "TryOverwrite";

    public static final String OPERATION_DO_GET_OR_CHECKOUT = "DoGetOrCheckout";

    private static final Log log = LogFactory.getLog(
            EloraIntegrationHelper.class);

    // TODO: llamar desde las clases necesarias: GetCheckoutInfo
    /**
     * Gets the real document to check out, and checks that it is not a proxy
     * nor a working copy
     *
     * @return
     * @throws EloraException
     */
    public static DocumentModel getRealDoc(DocumentRef realRef,
            CoreSession session) throws EloraException {

        DocumentModel realDoc = null;
        try {
            realDoc = session.getDocument(realRef);
        } catch (DocumentNotFoundException e) {
            throw new EloraException(
                    "Document |" + realRef.toString() + "| does not exist");
        }

        if (realDoc.isProxy()) {
            throw new EloraException(
                    "Document |" + realRef.toString() + "| is a proxy");
        }
        if (!realDoc.isVersion()) {
            throw new EloraException(
                    "Document |" + realRef.toString() + "| is a working copy");
        }

        return realDoc;
    }

    public static DocumentModel getWcDoc(DocumentRef wcRef, CoreSession session)
            throws EloraException {

        DocumentModel wcDoc = null;
        try {
            wcDoc = session.getDocument(wcRef);
        } catch (DocumentNotFoundException e) {
            throw new EloraException(
                    "Document |" + wcRef.toString() + "| does not exist");
        }

        if (wcDoc.isProxy()) {
            throw new EloraException(
                    "Document |" + wcRef.toString() + "| is a proxy");
        }
        if (wcDoc.isVersion()) {
            throw new EloraException(
                    "Document |" + wcRef.toString() + "| is a version");
        }

        return wcDoc;
    }

    public static DocumentModel getRealOrWcDoc(DocumentRef docRef,
            CoreSession session) throws EloraException {

        DocumentModel doc = null;
        try {
            doc = session.getDocument(docRef);
        } catch (DocumentNotFoundException e) {
            throw new EloraException(
                    "Document |" + docRef.toString() + "| does not exist");
        }

        if (doc.isProxy()) {
            throw new EloraException(
                    "Document |" + docRef.toString() + "| is a proxy");
        }

        return doc;
    }

    // TODO Konstanteak erabili. LifeCycleState hola ondo dau??
    /**
     * @param session
     * @param doc
     * @param property
     * @param operation
     * @return
     * @throws EloraException
     */
    public static Serializable getVirtualMetadata(CoreSession session,
            DocumentModel doc, String property, String operation)
            throws EloraException {
        VersionLabelService versionLabelService = Framework.getService(
                VersionLabelService.class);

        Serializable value = null;
        switch (property) {
        case "lifeCycleState":

            switch (operation) {
            case OPERATION_TRY_CHECKIN:
                // TODO Hau konfiguraziotik hartu beharko zen, baina oraingoz ez
                // dago initial state zein den jarrita, eta ez du zentzu
                // askorik. Badakigu Try-ean beti "preliminary"ra doala, eta
                // zuzenean pasatuko dugu balio hori.
                value = "preliminary";
                break;
            case OPERATION_TRY_OVERWRITE:
            case OPERATION_DO_GET_OR_CHECKOUT:
                value = doc.getCurrentLifeCycleState();
                break;
            }
            break;

        case "majorVersion":
            switch (operation) {
            case OPERATION_TRY_CHECKIN:
                value = EloraDocumentHelper.calculateNextMajorVersion(doc);
                break;
            case OPERATION_TRY_OVERWRITE:
            case OPERATION_DO_GET_OR_CHECKOUT:
                Long major = (Long) doc.getPropertyValue(
                        VersioningService.MAJOR_VERSION_PROP);
                value = versionLabelService.translateMajor(major);
                break;
            }
            break;

        case "minorVersion":
            switch (operation) {
            case OPERATION_TRY_CHECKIN:
                value = EloraDocumentHelper.calculateNextMinorVersion(doc);
                break;
            case OPERATION_TRY_OVERWRITE:
            case OPERATION_DO_GET_OR_CHECKOUT:
                Long minor = (Long) doc.getPropertyValue(
                        VersioningService.MINOR_VERSION_PROP);
                value = versionLabelService.translateMinor(minor);
                break;
            }
            break;

        }

        return value;
    }

    public static List<ItemInfo> getItemsInfo(CoreSession session,
            DocumentModel doc, boolean returnUniqueVersionsPerDocument)
            throws EloraException {

        // TODO: queda por definir si hay que buscar los item de un documento
        // según lo definido en las configuraciones

        if (!doc.isImmutable()) {
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(doc);
            if (baseDoc != null) {
                doc = baseDoc;
            }
        }

        String logInitMsg = "[getItemsInfo] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Getting items' info for doc |" + doc.getId()
                + "|");

        Map<String, List<DocumentModel>> itemDocsByVersionSeriesId = new HashMap<String, List<DocumentModel>>();
        List<ItemInfo> itemInfoList = new ArrayList<ItemInfo>();

        Resource predicate = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModelList subjectDocuments = RelationHelper.getSubjectDocuments(
                predicate, doc);

        // Fill list per versionSeriesId
        for (DocumentModel subjectDocument : subjectDocuments) {
            String versionSeriesId = subjectDocument.getVersionSeriesId();
            if (itemDocsByVersionSeriesId.containsKey(versionSeriesId)) {
                itemDocsByVersionSeriesId.get(versionSeriesId).add(
                        subjectDocument);
            } else {
                List<DocumentModel> list = new ArrayList<DocumentModel>();
                list.add(subjectDocument);
                itemDocsByVersionSeriesId.put(versionSeriesId, list);
            }
        }

        // Get latest related items only
        for (List<DocumentModel> docVersionsList : itemDocsByVersionSeriesId.values()) {
            if (docVersionsList.size() == 1) {
                itemInfoList.add(
                        createItemInfo(docVersionsList.get(0), session));
            } else {
                if (returnUniqueVersionsPerDocument) {
                    DocumentModel latestRelatedDoc = getLatestRelatedItemFromList(
                            session, docVersionsList);
                    itemInfoList.add(createItemInfo(latestRelatedDoc, session));
                } else {
                    for (DocumentModel docVersion : docVersionsList) {
                        itemInfoList.add(createItemInfo(docVersion, session));
                    }
                }

            }
        }

        log.trace(logInitMsg + "Got " + itemInfoList.size() + " items' info.");

        return itemInfoList;
    }

    private static DocumentModel getLatestRelatedItemFromList(
            CoreSession session, List<DocumentModel> docs)
            throws EloraException {
        List<String> uidList = EloraDocumentHelper.getUidListFromDocList(docs);
        Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(docs);

        String type = docs.get(0).getType();
        DocumentModel latestRelatedDoc = EloraRelationHelper.getLatestRelatedVersion(
                session, majorVersion, uidList, type);

        if (latestRelatedDoc == null) {
            throw new EloraException(
                    "Null value retrieved getting latest related version");
        }

        return latestRelatedDoc;
    }

    private static ItemInfo createItemInfo(DocumentModel itemDoc,
            CoreSession session) throws EloraException {
        ItemInfo itemInfo = new ItemInfo();

        Serializable reference = itemDoc.getPropertyValue(
                EloraMetadataConstants.ELORA_ELO_REFERENCE);

        itemInfo.setReference(reference == null ? "" : reference.toString());
        itemInfo.setTitle(itemDoc.getTitle());
        itemInfo.setCurrentVersionInfo(createVersionInfo(itemDoc,
                session.getSourceDocument(itemDoc.getRef())));
        itemInfo.setCurrentLifeCycleState(itemDoc.getCurrentLifeCycleState());
        itemInfo.setType(itemDoc.getType());

        return itemInfo;
    }

    public static VersionInfo createVersionInfo(DocumentModel currentDoc,
            DocumentModel wcDoc) throws EloraException {
        String label = currentDoc.getVersionLabel();
        String status = EloraDocumentHelper.getVersionStatus(currentDoc, wcDoc);
        String statusMessage = status.equals(
                VersionStatusConstants.VERSION_STATUS_NORMAL)
                        ? ""
                        : EloraMessageHelper.getTranslatedMessage(
                                currentDoc.getCoreSession(),
                                "eloraplm.message.versionStatus." + status);

        VersionInfo versionInfo = new VersionInfo(label, status, statusMessage);
        return versionInfo;
    }

    public static void checkThatConnectorIsUpToDate(String plmConnectorClient,
            Integer plmConnectorVersion) throws ConnectorIsObsoleteException {

        if (plmConnectorClient != null) {
            if (EloraConfig.integrationMinAllowedVersionsMap.containsKey(
                    plmConnectorClient)) {
                Integer minAllowedVersion = EloraConfig.integrationMinAllowedVersionsMap.get(
                        plmConnectorClient);
                if (plmConnectorVersion == null
                        || minAllowedVersion > plmConnectorVersion) {
                    throw new ConnectorIsObsoleteException(plmConnectorClient,
                            plmConnectorVersion, minAllowedVersion);
                }
            }
        }
    }

    public static DocumentModel relateDocumentWithCadBinaries(
            CoreSession session, DocumentModel doc,
            CadFileInfoRequestDoc requestDoc) throws EloraException {

        String logInitMsg = "[relateDocumentWithCadBinaries] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Relating CAD binaries to document |"
                + doc.getId() + "|");

        // Content
        EloraDocumentHelper.relateDocumentWithBinary(doc,
                requestDoc.getContentFile(),
                EloraGeneralConstants.FILE_TYPE_CONTENT);

        // Viewer
        EloraDocumentHelper.relateDocumentWithBinary(doc,
                requestDoc.getViewerFile(),
                EloraGeneralConstants.FILE_TYPE_VIEWER);

        // CAD attachments
        // First we have to cast them to simple EloraFileInfo
        List<EloraFileInfo> cadatts = requestDoc.getCadAttachments().stream().map(
                a -> (EloraFileInfo) a).collect(Collectors.toList());
        EloraDocumentHelper.relateDocumentWithBinaries(doc, cadatts,
                EloraGeneralConstants.FILE_TYPE_CAD_ATTACHMENT);

        log.trace(logInitMsg + "CAD binaries related to document |"
                + doc.getId() + "| ");

        return doc;
    }

}
