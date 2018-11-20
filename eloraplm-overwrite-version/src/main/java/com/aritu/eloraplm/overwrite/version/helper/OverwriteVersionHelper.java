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
package com.aritu.eloraplm.overwrite.version.helper;

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
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.ViewerActionConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.util.EloraCheckinHelper;
import com.aritu.eloraplm.viewer.ViewerPdfUpdater;

public class OverwriteVersionHelper {

    private static final Log log = LogFactory.getLog(
            OverwriteVersionHelper.class);

    public static void overwriteDocument(DocumentModel sourceDoc,
            DocumentModel targetDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            DocumentValidationService validator, CoreSession session)
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

        overwriteMetadata(sourceDoc, targetDoc);
        // if (currentDoc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT))
        // {
        log.trace(logInitMsg + "Start overwriting relations from |"
                + sourceDoc.getId() + "| to |" + targetDoc.getId() + "| ");
        overwriteRelations(sourceDoc, targetDoc, eloraDocumentRelationManager,
                session);
        log.trace(logInitMsg + "Finished overwriting relations from |"
                + sourceDoc.getId() + "| to |" + targetDoc.getId() + "| ");
        // }

        EloraDocumentHelper.disableVersioningDocument(targetDoc);
        checkForErrors(targetDoc, validator, session);
        // baseVersionDoc.putContextData(VersioningService.CHECKIN_COMMENT,
        // justification);
        session.saveDocument(targetDoc);
        restoreValidation(targetDoc);
    }

    private static void overwriteMetadata(DocumentModel currentDoc,
            DocumentModel baseVersionDoc) {
        Calendar modified = (Calendar) baseVersionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED);
        String lastContributor = baseVersionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR).toString();

        ViewerPdfUpdater.createViewer(currentDoc,
                ViewerActionConstants.ACTION_OVERWRITE);

        EloraDocumentHelper.copyProperties(currentDoc, baseVersionDoc);

        baseVersionDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_MODIFIED,
                modified);
        baseVersionDoc.setPropertyValue(
                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR, lastContributor);
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

        List<String> bomHierarchicalDirectAndDocumentRelations = new ArrayList<>();
        bomHierarchicalDirectAndDocumentRelations.addAll(
                RelationsConfig.bomHierarchicalRelationsList);
        bomHierarchicalDirectAndDocumentRelations.addAll(
                RelationsConfig.bomDirectRelationsList);
        bomHierarchicalDirectAndDocumentRelations.addAll(
                RelationsConfig.bomDocumentRelationsList);
        bomHierarchicalDirectAndDocumentRelations.addAll(
                RelationsConfig.cadRelationsList);

        for (String predicate : bomHierarchicalDirectAndDocumentRelations) {
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
