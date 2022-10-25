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
package com.aritu.eloraplm.pdm.checkin.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.CheckInInfoHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.ReviewInfoHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CheckinService implements CheckinManager {

    private static final Log log = LogFactory.getLog(CheckinService.class);

    protected VersionLabelService versionLabelService = Framework.getService(
            VersionLabelService.class);

    protected EloraDocumentRelationManager eloraDocumentRelationManager = Framework.getService(
            EloraDocumentRelationManager.class);

    @Override
    public DocumentModel forceCheckinDocument(DocumentModel doc,
            String checkinComment) throws EloraException,
            CheckinNotAllowedException, DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        return forceCheckinDocument(doc, checkinComment, null, null);
    }

    @Override
    public DocumentModel forceCheckinDocument(DocumentModel doc,
            String checkinComment, String clientName, String processReference)
            throws EloraException, CheckinNotAllowedException,
            DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        return checkinDocument(doc, checkinComment, clientName,
                processReference, true, true);
    }

    @Override
    public DocumentModel checkinDocument(DocumentModel doc,
            String checkinComment, boolean unlock) throws EloraException,
            CheckinNotAllowedException, DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        return checkinDocument(doc, checkinComment, null, null, unlock, false);
    }

    @Override
    public DocumentModel checkinDocument(DocumentModel doc,
            String checkinComment, String clientName, String processReference,
            boolean unlock) throws EloraException, CheckinNotAllowedException,
            DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        return checkinDocument(doc, checkinComment, clientName,
                processReference, unlock, false);
    }

    private DocumentModel checkinDocument(DocumentModel doc,
            String checkinComment, String clientName, String processReference,
            boolean unlock, boolean force) throws EloraException,
            CheckinNotAllowedException, DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {

        CoreSession session = doc.getCoreSession();
        String logInitMsg = "[checkinDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        if (!EloraDocumentHelper.isWorkingCopy(doc)) {
            log.trace(logInitMsg + "Get working copy of document |"
                    + doc.getId() + "|");
            doc = session.getWorkingCopy(doc.getRef());
        }

        // If it is forced, we do not check that it is locked by the user
        if (force) {
            EloraDocumentHelper.checkThatIsCheckedOut(doc);
        } else {
            EloraDocumentHelper.checkThatIsCheckedOutByMe(doc);
        }

        if (EloraDocumentHelper.checkFilter(doc,
                BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
            BomCharacteristicsHelper.verifyBomCharacteristicsRequiredConstraint(
                    doc);
        }

        // VersionLabelListener should update the Elora Version Label
        if (!doc.isDirty()) {
            doc.putContextData(
                    VersionLabelService.OPT_UPDATE_ELORA_VERSION_LABEL,
                    Boolean.TRUE);
        }

        // We have to remove overwritten value
        if (doc.hasFacet(EloraFacetConstants.FACET_OVERWRITABLE)) {
            doc.getProperty(
                    EloraMetadataConstants.ELORA_OVERWRITE_OVERWRITTEN).remove();
        }

        setupCheckIn(doc, checkinComment);

        if (doc.hasFacet(EloraFacetConstants.FACET_STORE_CHECKIN_INFO)) {
            if (doc.getContextData(
                    EloraGeneralConstants.CONTEXT_SKIP_CHECKIN_INFO) == null) {
                // Set last Check In Info properties
                CheckInInfoHelper.setLastCheckInInfoProperties(doc,
                        session.getPrincipal().toString());
            }
        }

        if (doc.hasFacet(EloraFacetConstants.FACET_STORE_REVIEW_INFO)) {
            if (doc.getContextData(
                    EloraGeneralConstants.CONTEXT_SKIP_REVIEW_INFO) == null) {
                ReviewInfoHelper.setLastReviewInfoPropertiesByState(doc,
                        doc.getCurrentLifeCycleState(), session);
            }
        }

        boolean skipViewerFileCreation = false;
        if (doc.getContextData().containsKey("default/"
                + EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION)) {
            skipViewerFileCreation = (boolean) doc.getContextData(
                    EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION);
        }

        doc = session.saveDocument(doc);
        log.trace(logInitMsg + "Document |" + doc.getId() + "| saved");

        // TODO LEIRE: Begiratu nola egin inportazioan erlazioak ez
        // konprobatzeko.
        EloraRelationHelper.copyRelationsToLastVersion(doc,
                eloraDocumentRelationManager, session);
        EloraRelationHelper.copyIncomingAnarchicRelationsToLastVersion(doc,
                eloraDocumentRelationManager);

        fireCheckedInEvent(doc, checkinComment, clientName, processReference,
                skipViewerFileCreation);

        if (doc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            overwriteViewerProperty(session, doc);
        }

        if (unlock && doc.isLocked()) {
            doc.removeLock();
        }

        log.trace(logInitMsg + "--- EXIT --- ");
        return doc;
    }

    private void fireCheckedInEvent(DocumentModel doc, String checkinComment,
            String clientName, String processReference,
            boolean skipViewerFileCreation) {

        String comment = constructCommentForCheckedInEvent(doc, checkinComment,
                clientName, processReference);

        Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();
        ctxProperties.put("comment", comment);
        ctxProperties.put(
                EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION,
                skipViewerFileCreation);

        EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKED_IN_EVENT, doc,
                ctxProperties);
    }

    private String constructCommentForCheckedInEvent(DocumentModel doc,
            String checkinComment, String clientName, String processReference) {

        String comment = doc.getVersionLabel();
        if (clientName != null) {
            comment += " #" + clientName;
        }
        if (processReference != null) {
            comment += " @" + processReference;
        }
        if (checkinComment != null) {
            comment += " " + checkinComment;
        }

        return comment;

    }

    private void overwriteViewerProperty(CoreSession session, DocumentModel doc)
            throws EloraException {

        DocumentModel baseVersionDoc = EloraDocumentHelper.getBaseVersion(doc);
        if (baseVersionDoc == null) {
            throw new EloraException(
                    "Document |" + doc.getId() + "| has no base version.");
        }

        baseVersionDoc.setPropertyValue(
                EloraMetadataConstants.ELORA_ELOVWR_FILE,
                doc.getPropertyValue(EloraMetadataConstants.ELORA_ELOVWR_FILE));

        EloraDocumentHelper.disableVersioningDocument(baseVersionDoc);
        session.saveDocument(baseVersionDoc);

        EloraDocumentHelper.restoreToVersion(doc.getRef(),
                baseVersionDoc.getRef(), true, true, session);

    }

    private void setupCheckIn(DocumentModel doc, String checkinComment) {
        CoreSession session = doc.getCoreSession();
        String logInitMsg = "[setupCheckIn] ["
                + session.getPrincipal().getName() + "] ";

        doc.putContextData(VersioningService.CHECKIN_COMMENT, checkinComment);
        String nextVersionIncrement = calculateNextVersionIncrement(
                versionLabelService, doc);

        if (nextVersionIncrement.equals("major")) {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MAJOR);
            log.trace(logInitMsg + "Next version increment MAJOR");
        } else {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
            log.trace(logInitMsg + "Next version increment MINOR");
        }
    }

    private String calculateNextVersionIncrement(
            VersionLabelService versionLabelService, DocumentModel doc) {

        long major = 0;
        if (doc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP) != null) {
            major = ((Long) doc.getPropertyValue(
                    VersioningService.MAJOR_VERSION_PROP)).longValue();
        }

        String nextIncrement = "minor";

        // Get all versions with the same major, and check their lifecycle state
        List<DocumentModel> docVersions = doc.getCoreSession().getVersions(
                doc.getRef());
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

    @Override
    public void checkThatRelationIsAllowed(DocumentModel subjectDoc,
            String predicateUri, DocumentModel objectDoc, String quantity)
            throws EloraException {
        CoreSession session = subjectDoc.getCoreSession();
        String logInitMsg = "[checkThatRelationIsAllowed] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER ---  with subjectDocType = |"
                + subjectDoc.getType() + "|, predicateUri = |" + predicateUri
                + "|, objectDocType = |" + objectDoc.getType()
                + "|, quantity = |" + quantity + "|");

        if (predicateUri == null || predicateUri.length() == 0) {
            String errorMsg = "Predicate cannot be null";
            log.error(logInitMsg + errorMsg);
            throw new EloraException(errorMsg);
        }

        if (quantity.equals("0") || quantity == null) {
            String errorMsg = "Relation quantity cannot be null or 0";
            log.error(logInitMsg + errorMsg);
            throw new EloraException(errorMsg);
        }

        // Check first if predicate is BOM_COMPOSE_OF or BOM_HAS_BOM. Since in
        // those cases, a manual check is required.
        String subjectDocTypeName = subjectDoc.getType();
        String objectDocTypeName = objectDoc.getType();

        //
        DocumentType subjectDocType = subjectDoc.getDocumentType();
        DocumentType objectDocType = objectDoc.getDocumentType();

        if (predicateUri.equals(EloraRelationConstants.BOM_COMPOSED_OF)) {
            if ((EloraDocumentTypesHelper.getDocumentType(
                    EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                            subjectDocType)
                    && !EloraDocumentTypesHelper.getDocumentType(
                            EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                    objectDocType))
                    || (subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_PRODUCT)
                            && !EloraDocumentTypesHelper.getDocumentType(
                                    EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                            objectDocType)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PRODUCT))
                    || (subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_TOOL)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_TOOL))
                    || (subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_PACKAGING)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PACKAGING))
                    || (!EloraDocumentTypesHelper.getDocumentType(
                            EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                    subjectDocType)
                            && !subjectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PRODUCT)
                            && !subjectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_TOOL)
                            && !subjectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PACKAGING))) {
                String errorMsg = "Incorrect relation sent. subject id = |"
                        + subjectDoc.getId() + "|, subject type = |"
                        + subjectDocTypeName + "|, object id = |"
                        + objectDoc.getId() + "|, object type = |"
                        + objectDocTypeName + "|, predicate |" + predicateUri
                        + "|";
                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);
            }
        } else if (predicateUri.equals(EloraRelationConstants.BOM_HAS_BOM)) {
            if ((subjectDocTypeName.equals(EloraDoctypeConstants.BOM_TOOL)
                    && !EloraDocumentTypesHelper.getDocumentType(
                            EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                    objectDocType)
                    && !objectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_PRODUCT))
                    || (subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_PACKAGING)
                            && !EloraDocumentTypesHelper.getDocumentType(
                                    EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                            objectDocType)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PRODUCT))
                    || (subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_SPECIFICATION)
                            && !EloraDocumentTypesHelper.getDocumentType(
                                    EloraDoctypeConstants.BOM_PART).isSuperTypeOf(
                                            objectDocType)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PRODUCT)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_TOOL)
                            && !objectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PACKAGING))
                    || (!subjectDocTypeName.equals(
                            EloraDoctypeConstants.BOM_TOOL)
                            && !subjectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_PACKAGING)
                            && !subjectDocTypeName.equals(
                                    EloraDoctypeConstants.BOM_SPECIFICATION))) {
                String errorMsg = "Incorrect relation sent. subject id = |"
                        + subjectDoc.getId() + "|, subject type = |"
                        + subjectDocTypeName + "|, object id = |"
                        + objectDoc.getId() + "|, object type = |"
                        + objectDocTypeName + "|, predicate |" + predicateUri
                        + "|";
                log.error(logInitMsg + errorMsg);
                throw new EloraException(errorMsg);
            }
        } else {
            Map<String, List<String>> allowedSubjectTypes = getAllowedSubjectTypes();
            Map<String, List<String>> allowedObjectTypes = getAllowedObjectTypes();
            Map<String, String> allowedSubjectFacet = getAllowedSubjectFacet();
            Map<String, String> allowedObjectFacet = getAllowedObjectFacet();

            if (!allowedSubjectTypes.containsKey(predicateUri)
                    || !allowedSubjectTypes.get(predicateUri).contains(
                            subjectDoc.getType())) {
                Set<String> subjectDocFacets = subjectDoc.getFacets();
                if (!allowedSubjectFacet.containsKey(predicateUri)
                        || !subjectDocFacets.contains(
                                allowedSubjectFacet.get(predicateUri))) {
                    String errorMsg = "Incorrect relation sent. Subject not supported in relation: subject |"
                            + subjectDoc.getId() + "|, object |"
                            + objectDoc.getId() + "|, predicate |"
                            + predicateUri + "|";
                    log.error(logInitMsg + errorMsg);
                    throw new EloraException(errorMsg);
                }
            }

            if (!allowedObjectTypes.containsKey(predicateUri)
                    || !allowedObjectTypes.get(predicateUri).contains(
                            objectDoc.getType())) {
                Set<String> objectDocFacets = objectDoc.getFacets();
                if (!allowedObjectFacet.containsKey(predicateUri)
                        || !objectDocFacets.contains(
                                allowedObjectFacet.get(predicateUri))) {
                    String errorMsg = "Incorrect relation sent. Object not supported in relation: subject |"
                            + subjectDoc.getId() + "|, object |"
                            + objectDoc.getId() + "|, predicate |"
                            + predicateUri + "|";
                    log.error(logInitMsg + errorMsg);
                    throw new EloraException(errorMsg);
                }
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private Map<String, List<String>> getAllowedSubjectTypes() {

        Map<String, List<String>> allowedSubjectTypes = new HashMap<String, List<String>>();

        // CADs - Hierarchical relations
        List<String> cadComposedOfSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_COMPOSED_OF,
                cadComposedOfSubjectTypes);

        List<String> cadBasedOnSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_PART);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_BASED_ON,
                cadBasedOnSubjectTypes);

        List<String> cadHasSuppressedSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_HAS_SUPPRESSED,
                cadHasSuppressedSubjectTypes);

        // CADs - Direct relations
        List<String> cadDrawingOfSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_DRAWING);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_DRAWING_OF,
                cadDrawingOfSubjectTypes);

        List<String> cadInContextWithSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                cadInContextWithSubjectTypes);

        List<String> cadHasDesignTableSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                cadHasDesignTableSubjectTypes);

        // ITEMs - Hierarchical relations
        List<String> bomComposedOfSubjectTypes = new ArrayList<>(
                EloraDocumentTypesHelper.getExtendedDocumentTypeNames(
                        EloraDoctypeConstants.BOM_PART));
        allowedSubjectTypes.put(EloraRelationConstants.BOM_COMPOSED_OF,
                bomComposedOfSubjectTypes);

        // ITEMs - Direct relations
        List<String> bomHasSpecificationSubjectTypes = new ArrayList<String>(
                Arrays.asList(EloraDoctypeConstants.BOM_PRODUCT,
                        EloraDoctypeConstants.BOM_TOOL,
                        EloraDoctypeConstants.BOM_PACKAGING));
        bomHasSpecificationSubjectTypes.addAll(
                EloraDocumentTypesHelper.getExtendedDocumentTypeNames(
                        EloraDoctypeConstants.BOM_PART));
        allowedSubjectTypes.put(EloraRelationConstants.BOM_HAS_SPECIFICATION,
                bomHasSpecificationSubjectTypes);

        List<String> bomCustomerHasProductSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT);
        allowedSubjectTypes.put(EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT,
                bomCustomerHasProductSubjectTypes);

        List<String> bomManufacturerHasPartSubjectTypes = Arrays.asList(
                EloraDoctypeConstants.BOM_MANUFACTURER_PART);
        allowedSubjectTypes.put(
                EloraRelationConstants.BOM_MANUFACTURER_HAS_PART,
                bomManufacturerHasPartSubjectTypes);

        return allowedSubjectTypes;
    }

    private Map<String, List<String>> getAllowedObjectTypes() {

        Map<String, List<String>> allowedObjectTypes = new HashMap<String, List<String>>();

        // CADs - Hierarchical relations
        List<String> cadComposedOfObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_COMPOSED_OF,
                cadComposedOfObjectTypes);

        List<String> cadBasedOnObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_BASED_ON,
                cadBasedOnObjectTypes);

        List<String> cadHasSuppressedObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_HAS_SUPPRESSED,
                cadHasSuppressedObjectTypes);

        // CADs - Direct relations
        List<String> cadDrawingOfObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_DRAWING_OF,
                cadDrawingOfObjectTypes);

        List<String> cadInContextWithObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                cadInContextWithObjectTypes);

        List<String> cadHasDesignTableObjectTypes = Arrays.asList(
                EloraDoctypeConstants.CAD_DESIGN_TABLE);
        allowedObjectTypes.put(EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                cadHasDesignTableObjectTypes);

        // ITEMs - Direct relations
        List<String> bomHasSpecificationObjectTypes = Arrays.asList(
                EloraDoctypeConstants.BOM_SPECIFICATION);
        allowedObjectTypes.put(EloraRelationConstants.BOM_HAS_SPECIFICATION,
                bomHasSpecificationObjectTypes);

        List<String> bomCustomerHasProductObjectTypes = Arrays.asList(
                EloraDoctypeConstants.BOM_PRODUCT);
        allowedObjectTypes.put(EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT,
                bomCustomerHasProductObjectTypes);

        List<String> bomManufacturerHasPartObjectTypes = new ArrayList<>(
                EloraDocumentTypesHelper.getExtendedDocumentTypeNames(
                        EloraDoctypeConstants.BOM_PART));
        allowedObjectTypes.put(EloraRelationConstants.BOM_MANUFACTURER_HAS_PART,
                bomManufacturerHasPartObjectTypes);

        return allowedObjectTypes;
    }

    private Map<String, String> getAllowedSubjectFacet() {

        Map<String, String> allowedSubjectFacet = new HashMap<String, String>();

        // CADs
        String cadHasDocumentSubjectFacet = EloraFacetConstants.FACET_CAD_DOCUMENT;
        allowedSubjectFacet.put(EloraRelationConstants.CAD_HAS_DOCUMENT,
                cadHasDocumentSubjectFacet);

        // ITEMs
        String bomHasCadDocumentSubjectFacet = EloraFacetConstants.FACET_BOM_DOCUMENT;
        allowedSubjectFacet.put(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT,
                bomHasCadDocumentSubjectFacet);

        String bomHasDocumentSubjectFacet = EloraFacetConstants.FACET_BOM_DOCUMENT;
        allowedSubjectFacet.put(EloraRelationConstants.BOM_HAS_DOCUMENT,
                bomHasDocumentSubjectFacet);

        // WORKSPACEs
        String containerHasContainerSubjectFacet = EloraFacetConstants.FACET_ELORA_WORKSPACE;
        allowedSubjectFacet.put(EloraRelationConstants.CONTAINER_HAS_CONTAINER,
                containerHasContainerSubjectFacet);

        return allowedSubjectFacet;
    }

    private Map<String, String> getAllowedObjectFacet() {

        Map<String, String> allowedObjectFacet = new HashMap<String, String>();

        // CADs
        String cadHasDocumentObjectFacet = EloraFacetConstants.FACET_BASIC_DOCUMENT;
        allowedObjectFacet.put(EloraRelationConstants.CAD_HAS_DOCUMENT,
                cadHasDocumentObjectFacet);

        // ITEMs
        String bomHasCadDocumentObjectFacet = EloraFacetConstants.FACET_CAD_DOCUMENT;
        allowedObjectFacet.put(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT,
                bomHasCadDocumentObjectFacet);

        String bomHasDocumentObjectFacet = EloraFacetConstants.FACET_BASIC_DOCUMENT;
        allowedObjectFacet.put(EloraRelationConstants.BOM_HAS_DOCUMENT,
                bomHasDocumentObjectFacet);

        // WORKSPACEs
        String containerHasContainerObjectFacet = EloraFacetConstants.FACET_ELORA_WORKSPACE;
        allowedObjectFacet.put(EloraRelationConstants.CONTAINER_HAS_CONTAINER,
                containerHasContainerObjectFacet);

        return allowedObjectFacet;
    }

}
