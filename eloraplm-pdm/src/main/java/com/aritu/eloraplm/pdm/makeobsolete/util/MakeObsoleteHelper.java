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
package com.aritu.eloraplm.pdm.makeobsolete.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * This class is a helper class for make obsolete functionality
 *
 * @author aritu
 *
 */
public class MakeObsoleteHelper {

    private static final Log log = LogFactory.getLog(MakeObsoleteHelper.class);

    public static boolean impliesMakingObsoleteAllVersions(CoreSession session,
            DocumentModel doc) throws EloraException {
        String logInitMsg = "[impliesMakingObsoleteAllVersions] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- doc id  = |" + doc.getId() + "|");

        boolean impliesMOAllVersions;

        // If the specified document is a WC or is the AV where the WC is based
        // on, making obsolete the document implies making obsolete all the
        // versions of the document.

        if (!doc.isImmutable()) {
            impliesMOAllVersions = true;
        } else {
            DocumentModel wcDoc = session.getWorkingCopy(doc.getRef());
            DocumentModel baseDoc = EloraDocumentHelper.getBaseVersion(wcDoc);

            if (doc.getId().equals(baseDoc.getId())) {
                impliesMOAllVersions = true;
            } else {

                // Add to the tree only the current AV
                impliesMOAllVersions = false;
            }
        }

        log.trace(logInitMsg + "--- EXIT --- with impliesMOAllVersions = |"
                + impliesMOAllVersions + "|");

        return impliesMOAllVersions;
    }

    /**
     * @param doc
     * @return the list of documents which state is not compatible for making
     *         obsolete specified document
     * @throws EloraException
     */
    public static List<DocumentModel> checkRelatedDocumentsCompatibility(
            CoreSession session, DocumentModel doc) throws EloraException {

        if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            return checkRelatedDocumentsCompatibilityForItem(session, doc);
        } else if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            return checkRelatedDocumentsCompatibilityForCadDoc(session, doc);
        } else {
            throw new EloraException(
                    "The document should be a BOM or a CAD document");
        }

    }

    /**
     * @param doc
     * @return the list of documents which state is not compatible for making
     *         obsolete specified document
     * @throws EloraException
     */
    protected static List<DocumentModel> checkRelatedDocumentsCompatibilityForItem(
            CoreSession session, DocumentModel doc) throws EloraException {
        String logInitMsg = "[checkRelatedDocumentsCompatibilityForItem] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");

        List<DocumentModel> incompatibleRelatedDocs = new ArrayList<DocumentModel>();

        // For items, we retrieve the subject of BOM_COMPOSED_OF and
        // BOM_HAS_SPECIFICATION relations (Hierarchical and Direct relations)
        DocumentModelList relatedDocuments = new DocumentModelListImpl();
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.BOM_COMPOSED_OF), doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_SPECIFICATION),
                doc));

        if (!relatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : relatedDocuments) {
                if (!LifecyclesConfig.isSupported(
                        relatedDoc.getCurrentLifeCycleState(),
                        EloraLifeCycleConstants.STATUS_OBSOLETE)) {

                    if (!incompatibleRelatedDocs.contains(relatedDoc)) {
                        incompatibleRelatedDocs.add(relatedDoc);
                    }
                    log.trace(logInitMsg + "related doc id = |"
                            + relatedDoc.getId() + "| has state = |"
                            + relatedDoc.getCurrentLifeCycleState()
                            + "|. It does not support "
                            + EloraLifeCycleConstants.STATUS_OBSOLETE + " children.");
                }
            }
        }

        log.trace(logInitMsg
                + "--- EXIT --- with incompatibleRelatedDocs.size() = |"
                + incompatibleRelatedDocs.size() + "|");

        return incompatibleRelatedDocs;
    }

    /**
     * @param doc
     * @return the list of documents which state is not compatible for making
     *         obsolete specified document
     * @throws EloraException
     */
    protected static List<DocumentModel> checkRelatedDocumentsCompatibilityForCadDoc(
            CoreSession session, DocumentModel doc) throws EloraException {
        String logInitMsg = "[checkRelatedDocumentsCompatibilityForDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- with doc id = |" + doc.getId()
                + "|");

        List<DocumentModel> incompatibleRelatedDocs = new ArrayList<DocumentModel>();

        // For cad docs, we retrieve the subject of BOM_HAS_CAD_DOCUMENT,
        // CAD_BASED_ON, CAD_COMPOSED_OF, CAD_HAS_DESIGN_TABLE, CAD_DRAWING_OF,
        // CAD_HAS_SUPPRESSED, CAD_IN_CONTEXT_WITH
        DocumentModelList relatedDocuments = new DocumentModelListImpl();
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT),
                doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_BASED_ON), doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_COMPOSED_OF), doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_HAS_DESIGN_TABLE),
                doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF), doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_HAS_SUPPRESSED),
                doc));
        relatedDocuments.addAll(RelationHelper.getSubjectDocuments(
                new ResourceImpl(EloraRelationConstants.CAD_IN_CONTEXT_WITH),
                doc));

        if (!relatedDocuments.isEmpty()) {
            for (DocumentModel relatedDoc : relatedDocuments) {

                if (!LifecyclesConfig.isSupported(
                        relatedDoc.getCurrentLifeCycleState(),
                        EloraLifeCycleConstants.STATUS_OBSOLETE)) {

                    incompatibleRelatedDocs.add(relatedDoc);

                    log.trace(logInitMsg + "related doc id = |"
                            + relatedDoc.getId() + "| has state = |"
                            + relatedDoc.getCurrentLifeCycleState()
                            + "|. It does not support "
                            + EloraLifeCycleConstants.STATUS_OBSOLETE + " children.");
                }
            }
        }

        log.trace(logInitMsg
                + "--- EXIT --- with incompatibleRelatedDocs.size() = |"
                + incompatibleRelatedDocs.size() + "|");

        return incompatibleRelatedDocs;
    }

    public static List<String> getErrorMsgListFromCannotMakeObsoleteResultList(
            CoreSession session,
            Map<String, CanMakeObsoleteResult> makeObsoleteResultList,
            Map<String, String> messages) {
        String logInitMsg = "[checkRelatedDocumentsCompatibilityForDoc] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        List<String> errorMsgList = new ArrayList<String>();

        if (makeObsoleteResultList != null) {
            Iterator<Entry<String, CanMakeObsoleteResult>> iterator = makeObsoleteResultList.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CanMakeObsoleteResult> docResult = iterator.next();
                String errorMsg = "";
                String errorDocId = docResult.getKey();
                try {
                    DocumentModel errorDoc = session.getDocument(
                            new IdRef(errorDocId));
                    if (errorDoc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
                        errorMsg += "[" + errorDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                + "] ";
                    }
                    errorMsg += errorDoc.getTitle() + " "
                            + errorDoc.getVersionLabel() + ": " + messages.get(
                                    docResult.getValue().getCannotMakeObsoleteReasonMsg());
                } catch (DocumentNotFoundException e) {
                    errorMsg += " (" + errorDocId + ")";
                }
                String reasonMsgParam = docResult.getValue().getCannotMakeObsoleteReasonMsgParam();
                if (reasonMsgParam != null && reasonMsgParam.length() > 0) {
                    try {
                        DocumentModel paramDoc = session.getDocument(
                                new IdRef(reasonMsgParam));
                        errorMsg += " (";
                        if (paramDoc.hasSchema(
                                EloraSchemaConstants.ELORA_OBJECT)) {
                            errorMsg += "[" + paramDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE)
                                    + "] ";
                        }
                        errorMsg += paramDoc.getTitle() + " "
                                + paramDoc.getVersionLabel() + ")";
                    } catch (DocumentNotFoundException e) {
                        errorMsg += " (" + reasonMsgParam + ")";
                    }
                }
                // Add the error message to the error messages list
                errorMsgList.add(errorMsg);
            }
        }

        log.trace(logInitMsg + "--- EXIT ---|");

        return errorMsgList;
    }
}
