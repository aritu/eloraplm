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
import com.aritu.eloraplm.bom.linked.characteristics.util.LinkedCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.CheckInInfoHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
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
            LinkedCharacteristicsHelper.updateLinkedCharacteristicsIfRequired(session,
                    doc,
                    BomCharacteristicsConstants.LINKED_CHARACS_UPDATED_BY_CHECKIN);
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

        fireAboutToCheckedInEvent(doc, clientName, processReference);

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

    private void fireAboutToCheckedInEvent(DocumentModel doc, String clientName,
            String processReference) {

        EloraEventHelper.fireEvent(PdmEventNames.PDM_ABOUT_TO_CHECK_IN_EVENT,
                doc);
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

}
