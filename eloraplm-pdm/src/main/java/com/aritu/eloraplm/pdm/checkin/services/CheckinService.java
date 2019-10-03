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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.config.util.LifecyclesConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.EloraVersionLabelService;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class CheckinService implements CheckinManager {

    private static final Log log = LogFactory.getLog(CheckinService.class);

    protected EloraVersionLabelService eloraVersionLabelService = Framework.getService(
            EloraVersionLabelService.class);

    protected EloraDocumentRelationManager eloraDocumentRelationManager = Framework.getService(
            EloraDocumentRelationManager.class);

    @Override
    public DocumentModel checkinDocument(DocumentModel doc,
            String checkinComment, boolean unlock) throws EloraException,
            CheckinNotAllowedException, DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException {
        return checkinDocument(doc, checkinComment, null, null, unlock);
    }

    @Override
    public DocumentModel checkinDocument(DocumentModel doc,
            String checkinComment, String clientName, String processReference,
            boolean unlock) throws EloraException, CheckinNotAllowedException,
            DocumentNotCheckedOutException,
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
        EloraDocumentHelper.checkThatIsCheckedOutByMe(doc);

        if (EloraDocumentHelper.checkFilter(doc,
                BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
            BomCharacteristicsHelper.verifyBomCharacteristicsRequiredConstraint(
                    doc);
        }

        if (!doc.isDirty()) {
            // We set the document dirty to update lastContributor and modified
            doc = EloraDocumentHelper.setDocumentDirty(doc);
        }

        // We have to remove overwritten value
        doc.getProperty(
                EloraMetadataConstants.ELORA_OVERWRITE_OVERWRITTEN).remove();

        setupCheckIn(doc, checkinComment);
        doc = session.saveDocument(doc);
        log.trace(logInitMsg + "Document |" + doc.getId() + "| saved");

        EloraRelationHelper.copyRelationsToLastVersion(doc,
                eloraDocumentRelationManager, session);
        EloraRelationHelper.copyIncomingAnarchicRelationsToLastVersion(doc,
                eloraDocumentRelationManager);

        fireCheckedInEvent(doc, checkinComment, clientName, processReference);

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
            String clientName, String processReference) {
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

        EloraEventHelper.fireEvent(PdmEventNames.PDM_CHECKED_IN_EVENT, doc,
                comment);
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
                eloraVersionLabelService, doc);

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
            EloraVersionLabelService versionLabelService, DocumentModel doc) {

        long major = ((Long) doc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP)).longValue();

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
        log.trace(logInitMsg + "--- ENTER --- ");

        Map<String, List<String>> allowedSubjectTypes = getAllowedSubjectTypes();
        Map<String, List<String>> allowedObjectTypes = getAllowedObjectTypes();

        if (!allowedSubjectTypes.containsKey(predicateUri)) {
            throw new EloraException("Predicate |" + predicateUri
                    + "| does not exist in Elora ");
        }

        if (!allowedSubjectTypes.get(predicateUri).contains(
                subjectDoc.getType())) {
            throw new EloraException(
                    "Incorrect relation sent. Subject not supported in relation: subject |"
                            + subjectDoc.getId() + "|, object |"
                            + objectDoc.getId() + "|, predicate |"
                            + predicateUri + "|");
        }

        if (!allowedObjectTypes.get(predicateUri).contains(
                objectDoc.getType())) {

            throw new EloraException(
                    "Incorrect relation sent. Object not supported in relation: subject |"
                            + subjectDoc.getId() + "|, object |"
                            + objectDoc.getId() + "|, predicate |"
                            + predicateUri + "|");
        }

        if (quantity.equals("0") || quantity == null) {
            throw new EloraException("Relation quantity cannot be null or 0");
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private Map<String, List<String>> getAllowedSubjectTypes() {

        Map<String, List<String>> allowedSubjectTypes = new HashMap<String, List<String>>();

        List<String> cadComposedOf = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_COMPOSED_OF,
                cadComposedOf);

        List<String> cadDrawingOf = Arrays.asList(
                EloraDoctypeConstants.CAD_DRAWING);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_DRAWING_OF,
                cadDrawingOf);

        List<String> cadBasedOn = Arrays.asList(EloraDoctypeConstants.CAD_PART);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_BASED_ON,
                cadBasedOn);

        List<String> cadHasSuppressed = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_HAS_SUPPRESSED,
                cadHasSuppressed);

        List<String> cadHasDesignTable = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                cadHasDesignTable);

        List<String> cadInContextWith = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY);
        allowedSubjectTypes.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                cadInContextWith);

        return allowedSubjectTypes;
    }

    private Map<String, List<String>> getAllowedObjectTypes() {

        Map<String, List<String>> allowedObjectTypes = new HashMap<String, List<String>>();

        List<String> cadComposedOf = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART,
                EloraDoctypeConstants.CAD_DESIGN_TABLE);
        allowedObjectTypes.put(EloraRelationConstants.CAD_COMPOSED_OF,
                cadComposedOf);

        List<String> cadDrawingOf = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_DRAWING_OF,
                cadDrawingOf);

        List<String> cadBasedOn = Arrays.asList(EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_BASED_ON, cadBasedOn);

        List<String> cadHasSuppressed = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_HAS_SUPPRESSED,
                cadHasSuppressed);

        List<String> cadHasDesignTable = Arrays.asList(
                EloraDoctypeConstants.CAD_DESIGN_TABLE);
        allowedObjectTypes.put(EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                cadHasDesignTable);

        List<String> cadInContextWith = Arrays.asList(
                EloraDoctypeConstants.CAD_ASSEMBLY,
                EloraDoctypeConstants.CAD_PART);
        allowedObjectTypes.put(EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                cadInContextWith);

        return allowedObjectTypes;
    }

}
