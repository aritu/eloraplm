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
package com.aritu.eloraplm.core.duplicating.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class DuplicatingServiceImpl extends DefaultComponent
        implements DuplicatingService {

    private static Log log = LogFactory.getLog(DuplicatingServiceImpl.class);

    private static final String XP_TYPES = "types";

    private static final String XP_DUPLICATORS = "duplicators";

    private static final String DUPLICATE_PREFIX = "Copy of ";

    private Map<String, TypeDescriptor> types;

    private Map<String, DuplicatorDescriptor> duplicators;

    @Override
    public void activate(ComponentContext context) {
        types = new HashMap<String, TypeDescriptor>();
        duplicators = new HashMap<String, DuplicatorDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        types = null;
        duplicators = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        switch (extensionPoint) {
        case XP_TYPES:
            TypeDescriptor type = (TypeDescriptor) contribution;
            if (type.id != null) {

                if (types.containsKey(type.id)) {
                    TypeDescriptor currentType = types.get(type.id);
                    currentType.merge(type);
                    if (!currentType.enabled) {
                        types.remove(currentType.id);
                    } else {
                        types.put(currentType.id, currentType);
                    }
                } else {
                    if (type.enabled) {
                        types.put(type.id, type);
                    }
                }
            } else {
                throw new NuxeoException("Type sent without an id");
            }
            break;
        case XP_DUPLICATORS:
            DuplicatorDescriptor duplicator = (DuplicatorDescriptor) contribution;
            if (duplicator.id != null) {
                duplicators.put(duplicator.id, duplicator);
            } else {
                throw new NuxeoException("Duplicator sent without an id");
            }
            break;
        default:
            throw new NuxeoException("Unknown extension point defined.");
        }
    }

    @Override
    public boolean canDocBeDuplicated(CoreSession session, DocumentModel doc)
            throws EloraException {

        String logInitMsg = "[canDocBeDuplicated] ["
                + session.getPrincipal().getName() + "] ";

        if (doc == null) {
            throw new EloraException("Doc cannot be null");
        }

        log.trace("Checking if document |" + doc.getId()
                + "| can be duplicated...");

        boolean response = false;

        try {

            // Only allow WC and not checked out
            if (!doc.isCheckedOut() && !doc.isImmutable()) {

                if (types.containsKey(doc.getType())) {
                    TypeDescriptor td = types.get(doc.getType());
                    if (td.enabled) {
                        response = true;
                    }
                }
            }

            log.trace("Response: |" + String.valueOf(response) + "|");

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw e;
        }

        return response;
    }

    @Override
    public DocumentModel duplicate(CoreSession session, DocumentModel sourceDoc,
            String reference, String title) throws EloraException {

        String logInitMsg = "[duplicate] [" + session.getPrincipal().getName()
                + "] ";

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (sourceDoc == null) {
                throw new EloraException("source doc cannot be null");
            }

            if (title == null) {
                title = DUPLICATE_PREFIX + sourceDoc.getTitle();
            }

            // Metadata
            DocumentModel duplicatedDoc = duplicateMetadata(session, sourceDoc);
            duplicatedDoc.setPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE, reference);
            duplicatedDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                    title);

            // Doc creation
            DocumentModel parentDoc = session.getParentDocument(
                    sourceDoc.getRef());
            duplicatedDoc = createDuplicatedDocument(session, parentDoc,
                    duplicatedDoc);

            // Relations
            duplicateRelations(session, sourceDoc, duplicatedDoc);

            // Documents
            if (sourceDoc.isFolder()) {
                duplicateContent(session, sourceDoc, duplicatedDoc);
            }

            // Finish (fire event)
            fireEvent(sourceDoc, duplicatedDoc);

            // Move & create proxy if needed
            duplicatedDoc = EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                    duplicatedDoc, parentDoc);

            return duplicatedDoc;

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            throw e;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public DocumentModel prepareDuplicatedDocument(CoreSession session,
            DocumentModel sourceDoc) throws EloraException {

        String logInitMsg = "[prepareDuplicatedDocument] ["
                + session.getPrincipal().getName() + "] ";

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            return duplicateMetadata(session, sourceDoc);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            throw e;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public DocumentModel completeDuplication(CoreSession session,
            DocumentModel sourceDoc, DocumentModel duplicatedDoc)
            throws EloraException {

        String logInitMsg = "[completeDuplication] ["
                + session.getPrincipal().getName() + "] ";
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel parentDoc = session.getParentDocument(
                    sourceDoc.getRef());
            duplicatedDoc = createDuplicatedDocument(session, parentDoc,
                    duplicatedDoc);

            // Move & create proxy if needed
            DocumentModel returnDoc = EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                    duplicatedDoc, parentDoc);

            duplicateRelations(session, sourceDoc, duplicatedDoc);

            if (sourceDoc.isFolder()) {
                duplicateContent(session, sourceDoc, duplicatedDoc);
            }

            fireEvent(sourceDoc, duplicatedDoc);

            return returnDoc;

        } catch (DocumentValidationException e) {
            TransactionHelper.setTransactionRollbackOnly();
            throw e;
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            throw e;
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            TransactionHelper.setTransactionRollbackOnly();
            throw e;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private DocumentModel duplicateMetadata(CoreSession session,
            DocumentModel sourceDoc) throws EloraException {

        if (sourceDoc == null) {
            throw new EloraException("source doc cannot be null");
        }

        DocumentModel duplicatedDoc = session.createDocumentModel(
                sourceDoc.getType());
        Map<String, List<String>> excludedProperties = new HashMap<String, List<String>>();
        excludedProperties.put(NuxeoSchemaConstants.DUBLINCORE,
                Arrays.asList(
                        new String[] { NuxeoMetadataConstants.NX_DC_CREATED,
                                NuxeoMetadataConstants.NX_DC_CREATOR,
                                NuxeoMetadataConstants.NX_DC_MODIFIED,
                                NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR,
                                NuxeoMetadataConstants.NX_DC_CONTRIBUTORS,
                                NuxeoMetadataConstants.NX_DC_TITLE }));
        excludedProperties.put(EloraSchemaConstants.ELORA_OBJECT, Arrays.asList(
                new String[] { EloraMetadataConstants.ELORA_ELO_REFERENCE }));

        // TODO EXCLUDE EXTRA PROPERTIES FROM XPs

        EloraDocumentHelper.copyProperties(sourceDoc, duplicatedDoc,
                excludedProperties);

        // TODO TAKE DUPLICATE_PREFIX FROM XPs
        duplicatedDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_TITLE,
                DUPLICATE_PREFIX + sourceDoc.getTitle());

        return duplicatedDoc;
    }

    private DocumentModel createDuplicatedDocument(CoreSession session,
            DocumentModel parentDoc, DocumentModel duplicatedDoc) {

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        duplicatedDoc.setPathInfo(parentDoc.getPathAsString(),
                pss.generatePathSegment(duplicatedDoc));

        duplicatedDoc = session.createDocument(duplicatedDoc);
        session.save();

        return duplicatedDoc;
    }

    private void duplicateRelations(CoreSession session,
            DocumentModel sourceDoc, DocumentModel duplicatedDoc)
            throws EloraException {

        if (sourceDoc == null || duplicatedDoc == null) {
            throw new EloraException(
                    "source doc and duplicate doc cannot be null");
        }

        EloraDocumentRelationManager edrm = Framework.getService(
                EloraDocumentRelationManager.class);

        // As the source doc is checked in, we get its base version for
        // relations

        // TODO If we allow copying 0.0+ or checked out documents, this MUST
        // be
        // changed to get WC relations

        DocumentModel sourceBaseDoc = EloraDocumentHelper.getBaseVersion(
                sourceDoc);
        if (sourceBaseDoc == null) {
            throw new EloraException(
                    "Could not get base version of source document.");
        }

        List<Statement> stmts = getSourceDocRelations(sourceBaseDoc);
        for (Statement stmt : stmts) {

            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(stmt);
            edrm.addRelation(session, duplicatedDoc, stmtInfo.getObject(),
                    stmtInfo.getPredicate().getUri(), false, false,
                    stmtInfo.getComment(), stmtInfo.getQuantity(),
                    stmtInfo.getOrdering(), stmtInfo.getDirectorOrdering(),
                    stmtInfo.getViewerOrdering(),
                    stmtInfo.getInverseViewerOrdering(), stmtInfo.getIsManual(),
                    stmtInfo.getDesignators(),
                    stmtInfo.getIsExcludedFromMapping());
        }

    }

    private List<Statement> getSourceDocRelations(DocumentModel sourceDoc) {

        List<Resource> predicateList = loadPredicateList();
        return EloraRelationHelper.getStatements(sourceDoc, predicateList);

    }

    private List<Resource> loadPredicateList() {
        List<String> predicateUris = new ArrayList<String>();
        predicateUris.addAll(RelationsConfig.cadRelationsList);
        predicateUris.addAll(RelationsConfig.bomDirectRelationsList);
        predicateUris.addAll(RelationsConfig.bomHierarchicalRelationsList);
        predicateUris.addAll(RelationsConfig.containerRelationsList);
        predicateUris.addAll(RelationsConfig.docRelationsList);

        // TODO BomList???? ORAINGOZ EZ DIRA SORTUKO

        // TODO XPtik HARTU EXCLUDED, eta ZERRENDATIK KENDU

        return EloraRelationHelper.getPredicateResourceList(predicateUris);
    }

    private void duplicateContent(CoreSession session, DocumentModel sourceDoc,
            DocumentModel duplicatedDoc) throws EloraException {

        if (sourceDoc == null || duplicatedDoc == null) {
            throw new EloraException(
                    "source doc and duplicate doc cannot be null");
        }

        // TODO Implement content duplication

    }

    public void fireEvent(DocumentModel sourceDoc,
            DocumentModel duplicatedDoc) {
        // Nuxeo Event
        String comment = sourceDoc.getRepositoryName() + ':'
                + sourceDoc.getId();
        EloraEventHelper.fireEvent(EloraEventNames.ELORA_DOCUMENT_DUPLICATED,
                duplicatedDoc, comment);
    }

}
