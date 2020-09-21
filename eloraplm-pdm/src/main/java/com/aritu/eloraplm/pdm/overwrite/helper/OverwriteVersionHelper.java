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
package com.aritu.eloraplm.pdm.overwrite.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.PdmEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.util.EloraCheckinHelper;

public class OverwriteVersionHelper {

    private static final Log log = LogFactory.getLog(
            OverwriteVersionHelper.class);

    public static void overwriteDocument(DocumentModel sourceDoc,
            DocumentModel targetDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            DocumentValidationService validator, CoreSession session,
            String justification, String clientName, String processReference)
            throws CheckinNotAllowedException, EloraException,
            BomCharacteristicsValidatorException {

        String logInitMsg = "[overwriteDocument] ["
                + session.getPrincipal().getName() + "] ";

        if (sourceDoc.isImmutable()) {
            throw new EloraException("Cannot overwrite from archived version");
        }

        if (EloraDocumentHelper.checkFilter(sourceDoc,
                BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
            BomCharacteristicsHelper.verifyBomCharacteristicsRequiredConstraint(
                    sourceDoc);
        }

        // Nuxeo Event (viewer creation listens this event)
        EloraEventHelper.fireEvent(PdmEventNames.PDM_ABOUT_TO_OVERWRITE_EVENT,
                sourceDoc);

        overwriteMetadata(sourceDoc, targetDoc);

        log.trace(logInitMsg + "Start overwriting relations from |"
                + sourceDoc.getId() + "| to |" + targetDoc.getId() + "| ");
        overwriteRelations(sourceDoc, targetDoc, eloraDocumentRelationManager,
                session);
        log.trace(logInitMsg + "Finished overwriting relations from |"
                + sourceDoc.getId() + "| to |" + targetDoc.getId() + "| ");

        EloraDocumentHelper.disableVersioningDocument(targetDoc);
        checkForErrors(targetDoc, validator, session);

        session.saveDocument(targetDoc);
        restoreValidation(targetDoc);

        // Restore WC to Base
        log.trace(logInitMsg + "Start restoring to version document |"
                + sourceDoc.getId() + "|");
        EloraDocumentHelper.restoreToVersion(sourceDoc.getRef(),
                targetDoc.getRef(), true, true, session);
        log.trace(logInitMsg + "Finished restoring to version document |"
                + sourceDoc.getId() + "|");

        // Nuxeo Event
        sourceDoc.refresh();
        String comment = sourceDoc.getVersionLabel();
        if (clientName != null) {
            comment += " #" + clientName;
        }
        if (processReference != null) {
            comment += " @" + processReference;
        }
        if (justification != null) {
            comment += " " + justification;
        }
        EloraEventHelper.fireEvent(PdmEventNames.PDM_OVERWRITTEN_EVENT,
                sourceDoc, comment);

    }

    private static void overwriteMetadata(DocumentModel currentDoc,
            DocumentModel baseVersionDoc) {
        Calendar modified = (Calendar) baseVersionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        String lastContributor = baseVersionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR).toString();

        Calendar lastReviewed = null;
        String lastReviewer = null;
        if (currentDoc.hasFacet(EloraFacetConstants.FACET_STORE_REVIEW_INFO)) {
            if (baseVersionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED) != null) {
                lastReviewed = (Calendar) baseVersionDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED);
            }
            if (baseVersionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER) != null) {
                lastReviewer = baseVersionDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER).toString();
            }
        }

        EloraDocumentHelper.copyProperties(currentDoc, baseVersionDoc);

        baseVersionDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_MODIFIED,
                modified);
        baseVersionDoc.setPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR, lastContributor);

        if (currentDoc.hasFacet(EloraFacetConstants.FACET_STORE_REVIEW_INFO)) {
            baseVersionDoc.setPropertyValue(
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED,
                    lastReviewed);
            baseVersionDoc.setPropertyValue(
                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER,
                    lastReviewer);
        }

        // Write overwrite_data
        baseVersionDoc.setPropertyValue(
                EloraMetadataConstants.ELORA_OVERWRITE_OVERWRITTEN,
                Calendar.getInstance());
    }

    private static void overwriteRelations(DocumentModel currentDoc,
            DocumentModel baseVersionDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session)
            throws CheckinNotAllowedException, EloraException {
        log.trace("Start removing relations of doc |" + baseVersionDoc.getId()
                + "|");
        removeRelations(baseVersionDoc, eloraDocumentRelationManager, session);
        log.trace("Finished removing relations of doc |"
                + baseVersionDoc.getId() + "|");
        log.trace("Start to copy all relations to doc |"
                + baseVersionDoc.getId() + "|");
        EloraRelationHelper.copyAllRelationsButAnarchicsToVersion(currentDoc,
                baseVersionDoc, eloraDocumentRelationManager);
        log.trace("Finished to copy all relations to doc |"
                + baseVersionDoc.getId() + "|");
    }

    private static void removeRelations(DocumentModel baseVersionDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session) {
        // TODO: A futuro podemos tener problemas con las relaciones de
        // abajo si se añaden nuevas que haya que tener en cuenta. Hay que
        // buscar la forma de sacar todas menos las anárquicas

        List<String> relations = new ArrayList<>();
        relations.addAll(RelationsConfig.bomHierarchicalRelationsList);
        relations.addAll(RelationsConfig.bomDirectRelationsList);
        relations.addAll(RelationsConfig.docRelationsList);
        relations.addAll(RelationsConfig.cadRelationsList);

        for (String predicate : relations) {
            // RelationHelper.removeRelation(RelationConstants.GRAPH_NAME,
            // baseVersionDoc, new ResourceImpl(predicate), null);
            eloraDocumentRelationManager.softDeleteRelation(session,
                    baseVersionDoc, predicate, null);
        }
    }

    private static void checkForErrors(DocumentModel baseVersionDoc,
            DocumentValidationService validator, CoreSession session) {
        // TODO: La función de abajo esta dentro del paquete integration.
        // Sería mejor pasarla a un paquete más genérico ya que no se
        // utiliza solo desde la integración.
        List<ValidationErrorItem> errorList = EloraCheckinHelper.checkForErrors(
                baseVersionDoc, validator, session);
        if (errorList.size() == 0) {
            baseVersionDoc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                    Forcing.TURN_OFF);
        }
    }

    public static void restoreValidation(DocumentModel doc) {
        // Enable validation?
        doc.putContextData(DocumentValidationService.CTX_MAP_KEY,
                Forcing.USUAL);

    }

}
