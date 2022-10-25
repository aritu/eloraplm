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

package com.aritu.eloraplm.doctypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.aritu.eloraplm.codecreation.api.CodeCreationService;
import com.aritu.eloraplm.codecreation.util.CodeCreationHelper;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraDocumentEventNames;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraPropertiesConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.core.util.EloraStructureHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.versioning.VersionLabelService;

/**
 * @author aritu
 *
 */
public class DocCreationFromTemplateHelper {

    protected static Log log = LogFactory.getLog(
            DocCreationFromTemplateHelper.class);

    private static final String RESET_CREATOR_PROPERTY = "nuxeo.template.reset-creator-on-creation-from-template";

    public static DocumentModelList getDocumentTemplates(CoreSession session,
            String templateDocType) {

        DocumentModelList templates = new DocumentModelListImpl();

        DocumentModelList templateRoots;
        // String query = "SELECT * FROM Document where ecm:primaryType = '%s'
        // AND ecm:path STARTSWITH %s";
        String query = "SELECT * FROM Document where ecm:primaryType = '%s'";
        templateRoots = session.query(
                String.format(query, NuxeoDoctypeConstants.DOCUMENT_TEMPLATES));
        // NXQL.escapeString(navigationContext.getCurrentDomainPath()))); ????

        if (!templateRoots.isEmpty()) {

            Map<String, String> order = new HashMap<String, String>();
            order.put("title", "asc");
            DocumentModelComparator dmc = new DocumentModelComparator(
                    "dublincore", order);

            // Iterate over each templateRoot in order to retrieve all templates
            for (Iterator<DocumentModel> it = templateRoots.iterator(); it.hasNext();) {
                // Get all document templates ordered by ascending title
                DocumentModel templateRoot = it.next();
                DocumentModelList tts = session.getChildren(
                        templateRoot.getRef(), templateDocType, null, dmc);
                templates.addAll(tts);
            }

            // remove delete templates
            List<DocumentModel> deleted = new ArrayList<DocumentModel>();
            for (Iterator<DocumentModel> it = templates.iterator(); it.hasNext();) {
                DocumentModel current = it.next();
                if (LifeCycleConstants.DELETED_STATE.equals(
                        current.getCurrentLifeCycleState())) {
                    deleted.add(current);
                }
            }
            templates.removeAll(deleted);
        }
        return templates;
    }

    public static DocumentModel createDocumentFromTemplate(CoreSession session,
            DocumentRef destinationFolderRef, String templateDocId, String name,
            DocumentModel documentWithPropertiesToBeCopied,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService, boolean skipLock)
            throws EloraException {

        DocumentModel createdDoc = null;

        createdDoc = createCopyFromTemplate(session, destinationFolderRef,
                templateDocId, name, eloraDocumentRelationManager,
                versionLabelService);

        // Obtain base viewer files before properties copy
        Serializable templateBaseViewer = null;
        Serializable docBaseViewer = null;
        if (createdDoc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            templateBaseViewer = createdDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
            if (documentWithPropertiesToBeCopied != null) {
                docBaseViewer = documentWithPropertiesToBeCopied.getPropertyValue(
                        EloraMetadataConstants.ELORA_ELOVWR_BASEFILE);
            }
        }

        updateRequiredProperties(session, documentWithPropertiesToBeCopied,
                createdDoc, templateDocId, eloraDocumentRelationManager,
                versionLabelService);

        // If viewer base file from template differs from the user input, we
        // must apply the change to the viewer file too.
        if (createdDoc.hasSchema(EloraSchemaConstants.ELORA_VIEWER)) {
            if ((templateBaseViewer != null && docBaseViewer != null
                    && !templateBaseViewer.equals(docBaseViewer))
                    || (templateBaseViewer == null && docBaseViewer != null)
                    || (templateBaseViewer != null && docBaseViewer == null)) {
                createdDoc.setPropertyValue(
                        EloraMetadataConstants.ELORA_ELOVWR_FILE,
                        docBaseViewer);
            }
        }

        // Execute initial actions
        DocCreationHelper.executeInitialActions(session, createdDoc, skipLock);
        resetDocumentVersion(versionLabelService, createdDoc);

        createdDoc = session.saveDocument(createdDoc);
        session.save();

        // TODO: Los dos métodos de abajo recorren de nuevo la estructura del
        // documento creado para actualizar las propiedades. A futuro se puede
        // mirar si es posible meterlo dentro de updateChildrenProperties

        // If it is defined in Elora Properties to append the document
        // reference to children title, and reference is set, do it
        updateTitleWithReferenceIfRequired(session, createdDoc);

        // If required, update children's main content file name
        updateMainContentFileNameIfRequiredToChildren(session, createdDoc);

        // If required, move the document to Elora Structure and create a
        // proxy
        DocumentModel parentDoc = EloraDocumentHelper.getParentDoc(createdDoc,
                session);
        EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                createdDoc, parentDoc);

        copyTemplateRelations(templateDocId, createdDoc,
                eloraDocumentRelationManager, versionLabelService, session);

        return createdDoc;
    }

    private static void updateTitleWithReferenceIfRequired(CoreSession session,
            DocumentModel createdDoc) {
        if (Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_APPEND_REFERENCE_TO_CHILDREN_TITLE,
                Boolean.toString(false)))) {
            String currentReference = getDocumenReference(createdDoc);
            if (currentReference != null && currentReference.length() > 0) {
                appendReferenceToChildrenTitle(session, createdDoc,
                        currentReference);
            }
        }
    }

    private static void updateRequiredProperties(CoreSession session,
            DocumentModel documentWithPropertiesToBeCopied,
            DocumentModel created, String templateDocId,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService) throws EloraException {
        // Update properties from user input.
        if (documentWithPropertiesToBeCopied != null) {
            EloraDocumentHelper.copyProperties(documentWithPropertiesToBeCopied,
                    created, true);
        }

        // Update reference with automatic code if required
        updateReferenceWithAutomaticCodeIfRequired(session, created);

        // Iterate over its children in order to modify some of their properties
        updateChildrenProperties(session, created, eloraDocumentRelationManager,
                versionLabelService);

        // Update main content file name if required
        updateMainContentFileNameIfRequired(session, created);
    }

    private static DocumentModel createCopyFromTemplate(CoreSession session,
            DocumentRef destinationFolderRef, String templateDocId, String name,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService) throws EloraException {

        CopyOption opt = null;
        if (Framework.getService(
                ConfigurationService.class).isBooleanPropertyTrue(
                        RESET_CREATOR_PROPERTY)) {
            opt = CopyOption.RESET_CREATOR;
        }
        DocumentModel created = session.copy(new IdRef(templateDocId),
                destinationFolderRef, name, opt, CopyOption.RESET_LIFE_CYCLE);

        created.setPropertyValue(EloraMetadataConstants.ELORA_TEMPL_IS_TEMPLATE,
                false);

        created = setSourceDoc(created, templateDocId, session);

        return created;
    }

    private static DocumentModel setSourceDoc(DocumentModel created,
            String templateDocId, CoreSession session) {
        DocumentModel templateDoc = session.getDocument(
                new IdRef(templateDocId));

        DocumentModel baseVersionDoc = EloraDocumentHelper.getBaseVersion(
                templateDoc);
        String sourceDocId = baseVersionDoc == null ? templateDoc.getId()
                : baseVersionDoc.getId();
        created.setPropertyValue(
                EloraMetadataConstants.ELORA_TEMPL_SOURCE_DOC_UID, sourceDocId);

        return created;
    }

    private static void resetDocumentVersion(
            VersionLabelService versionLabelService, DocumentModel created) {

        if (created.isVersionable()) {
            created.setPropertyValue(VersioningService.MAJOR_VERSION_PROP,
                    null);
            created.setPropertyValue(VersioningService.MINOR_VERSION_PROP,
                    null);

            String newMajor = versionLabelService.translateMajor(
                    (Long) created.getPropertyValue(
                            VersioningService.MAJOR_VERSION_PROP));
            String newMinor = versionLabelService.translateMinor(
                    (Long) created.getPropertyValue(
                            VersioningService.MINOR_VERSION_PROP));

            versionLabelService.setMajor(created, newMajor);
            versionLabelService.setMinor(created, newMinor);
        }
    }

    private static void copyTemplateRelations(String templateDocId,
            DocumentModel createdDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService, CoreSession session)
            throws EloraException {

        DocumentModel templateDoc = session.getDocument(
                new IdRef(templateDocId));

        manageObjectRelations(createdDoc, eloraDocumentRelationManager,
                versionLabelService, session, templateDoc);

        manageSubjectRelations(createdDoc, eloraDocumentRelationManager,
                session, templateDoc);
    }

    private static void manageSubjectRelations(DocumentModel createdDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            CoreSession session, DocumentModel templateDoc)
            throws EloraException {

        String logInitMsg = "[manageSubjectRelations] ["
                + session.getPrincipal().getName() + "] ";

        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_BOM);
        List<Statement> subjStmts = EloraRelationHelper.getSubjectStatements(
                templateDoc, predicateResource);

        for (Statement stmt : subjStmts) {
            DocumentModel subject = RelationHelper.getDocumentModel(
                    stmt.getSubject(), session);
            if (subject == null) {
                log.error(logInitMsg
                        + "Subject is null. Relation is broken or unreadable. predicateUri = |"
                        + stmt.getPredicate().getUri() + "|, object docId = |"
                        + templateDoc.getId() + "|");
                throw new EloraException(
                        "Subject is null. Relation is broken or unreadable.");
            }

            if (!EloraDocumentHelper.isTemplate(subject)) {
                if (LifecyclesConfig.isSupported(
                        createdDoc.getCurrentLifeCycleState(),
                        subject.getCurrentLifeCycleState())) {
                    EloraRelationHelper.copyRelation(subject, createdDoc,
                            eloraDocumentRelationManager, stmt);
                } else {
                    throw new EloraException(
                            "No supported lifecycle state of object |"
                                    + subject.getId() + "|.");
                }
            }

        }
    }

    private static void manageObjectRelations(DocumentModel createdDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService, CoreSession session,
            DocumentModel templateDoc) throws EloraException {

        String logInitMsg = "[manageObjectRelations] ["
                + session.getPrincipal().getName() + "] ";

        List<Statement> stmts = RelationHelper.getStatements(
                EloraRelationConstants.ELORA_GRAPH_NAME, templateDoc, null);
        for (Statement stmt : stmts) {
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), session);
            if (object == null) {
                log.error(logInitMsg
                        + "Object is null. Relation is broken or unreadable. predicateUri = |"
                        + stmt.getPredicate().getUri() + "|, subject docId = |"
                        + createdDoc.getId() + "|");
                throw new EloraException(
                        "Object is null. Relation is broken or unreadable.");
            }

            if (EloraDocumentHelper.isTemplate(object)) {
                object = createNewObject(createdDoc,
                        eloraDocumentRelationManager, versionLabelService,
                        session, object);
            }

            if (LifecyclesConfig.isSupported(
                    createdDoc.getCurrentLifeCycleState(),
                    object.getCurrentLifeCycleState())) {
                EloraRelationHelper.copyRelation(createdDoc, object,
                        eloraDocumentRelationManager, stmt);
            } else {
                throw new EloraException(
                        "No supported lifecycle state of object |"
                                + object.getId() + "|.");
            }
        }
    }

    private static DocumentModel createNewObject(DocumentModel newDoc,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService, CoreSession session,
            DocumentModel object) throws EloraException {
        DocumentModel destStructureDoc = null;
        if (object.isProxy()) {
            DocumentModel sourceDoc = session.getDocument(
                    new IdRef(object.getSourceId()));
            object = sourceDoc;
        }

        // TODO: Si el template relacionado no tiene estos facet se
        // relacionará directamente sin crear un documento nuevo
        if (object.hasFacet(EloraFacetConstants.FACET_BASIC_DOCUMENT)
                || object.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)
                || object.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {

            DocumentModel structureRoot = EloraStructureHelper.getWorkableDomainChildDocModel(
                    newDoc, session);

            if (object.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                destStructureDoc = EloraStructureHelper.getStructureDocumentByType(
                        structureRoot.getRef(), object.getType(),
                        EloraDoctypeConstants.STRUCTURE_EBOM, session);
            } else {
                destStructureDoc = EloraStructureHelper.getStructureDocumentByType(
                        structureRoot.getRef(), object.getType(),
                        EloraDoctypeConstants.STRUCTURE_CAD_DOCS, session);
            }

            PathSegmentService pss = Framework.getService(
                    PathSegmentService.class);
            String name = pss.generatePathSegment(object);

            DocumentModel newObject = createDocumentFromTemplate(session,
                    destStructureDoc.getRef(), object.getId(), name, null,
                    eloraDocumentRelationManager, versionLabelService, true);

            object = newObject;
        }
        return object;
    }

    private static void updateReferenceWithAutomaticCodeIfRequired(
            CoreSession session, DocumentModel doc) {
        String logInitMsg = "[updateReferenceWithAutomaticCodeIfRequired] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        // Check if automatic code must be generated for this document
        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
            String currentReference = null;
            Serializable docReferencePropertyValue = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (docReferencePropertyValue != null) {
                currentReference = docReferencePropertyValue.toString();
            }
            log.trace(logInitMsg + "Current reference: currentReference = |"
                    + currentReference + "|");
            if (currentReference == null || currentReference.isEmpty()) {
                String generatedReference = null;
                boolean isGenerateAutomaticCode = CodeCreationHelper.isGenerateAutomaticCode(
                        doc);
                log.trace(logInitMsg + "isGenerateAutomaticCode = |"
                        + isGenerateAutomaticCode + "|");

                // If automatic code must be generated, generate it
                if (isGenerateAutomaticCode) {
                    try {
                        generatedReference = CodeCreationHelper.createCode(doc,
                                session.getPrincipal().getName());
                        log.trace(logInitMsg + " generatedReference = |"
                                + generatedReference + "|");

                        // Check if obtained reference is valid
                        String docUid = doc.isProxy() ? doc.getSourceId()
                                : doc.getId();
                        EloraDocumentHelper.validateDocumentReference(session,
                                generatedReference, doc.getType(), docUid);
                        log.trace(logInitMsg + " generatedReference = |"
                                + generatedReference + "| is valid.");
                    } catch (EloraException e) {
                        log.error(logInitMsg
                                + "Exception when generating automatic document reference: "
                                + e.getMessage(), e);
                    }
                }
                // If reference is required but not filled automatically
                // (manualRequired mode),
                // set a current date in milliseconds value.
                if (CodeCreationHelper.getModeForType(doc.getType()).equals(
                        CodeCreationService.CODE_CREATION_TYPE_MODE_MANUAL_REQUIRED)
                        && (generatedReference == null
                                || generatedReference.isEmpty())) {
                    generatedReference = String.valueOf(new Date().getTime());
                }

                // Store generated reference
                if (generatedReference != null
                        && generatedReference.length() > 0) {
                    doc.setPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE,
                            generatedReference);
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    /**
     * This method iterates over the document children and set their properties
     * if required. For each children: if it has automatic code, it generates
     * its code. Mark not modified documents as dirty, in order to force to
     * modify its creator and creation date. CreatedBy, CreatedDate, create
     * proxy
     *
     * @param parentDocument
     * @throws EloraException
     */
    private static void updateChildrenProperties(CoreSession session,
            DocumentModel parentDocument,
            EloraDocumentRelationManager eloraDocumentRelationManager,
            VersionLabelService versionLabelService) throws EloraException {
        String logInitMsg = "[updateChildrenProperties] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Iterate over its children
        DocumentModelIterator it = session.getChildrenIterator(
                parentDocument.getRef());
        for (DocumentModel childDoc : it) {
            if (childDoc.isProxy()) {
                DocumentModel sourceDoc = session.getDocument(
                        new IdRef(childDoc.getSourceId()));
                if (EloraDocumentHelper.isTemplate(sourceDoc)) {

                    PathSegmentService pss = Framework.getService(
                            PathSegmentService.class);
                    String name = pss.generatePathSegment(sourceDoc);

                    if (!sourceDoc.hasFacet(
                            EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
                        createDocumentFromTemplate(session,
                                parentDocument.getRef(), sourceDoc.getId(),
                                name, null, eloraDocumentRelationManager,
                                versionLabelService, true);

                        session.removeDocument(childDoc.getRef());
                    }
                    /*else {
                        // TODO: Sacar mensaje diciendo que por ahora no es
                        // posible meter como proxy un ws dentro de un template.
                        // Aunque si filtramos en los proxies el poder busar
                        // templates no se podrá meter nunca un proxy dentro de
                        // una plantilla
                    }
                    */
                    return;
                }
            } else {
                if (EloraDocumentHelper.isTemplatable(childDoc)) {
                    childDoc.setPropertyValue(
                            EloraMetadataConstants.ELORA_TEMPL_IS_TEMPLATE,
                            false);
                }

                resetDocumentVersion(versionLabelService, childDoc);

                /*
                if (childDoc.hasFacet(
                        EloraFacetConstants.FACET_ELORA_WORKSPACE)) {
                    // TODO: Sacar mensaje diciendo que por ahora no es
                    // posible meter como hijo un ws dentro de un template.
                }
                */

                // Update reference with automatic code if required
                updateReferenceWithAutomaticCodeIfRequired(session, childDoc);

                modifyCreatorAndCreated(session, childDoc);

                // We set the document dirty to update lastContributor and
                // modified
                if (!childDoc.isDirty()) {
                    EloraDocumentHelper.setDocumentDirty(childDoc);
                }

                // Execute initial actions
                DocCreationHelper.executeInitialActions(session, childDoc,
                        true);

                // Save the document
                session.saveDocument(childDoc);

                // Launch document created inside template event, in order to be
                // handled by StateLogListener
                EloraEventHelper.fireEvent(
                        EloraDocumentEventNames.DOCUMENT_ELORA_CREATED_INSIDE_TEMPLATE,
                        childDoc);

                // If required, move the document to Elora Structure and create
                // a proxy
                EloraStructureHelper.moveDocToEloraStructureAndCreateProxyIfRequired(
                        childDoc, parentDocument);

                // Recursively call its children
                updateChildrenProperties(session, childDoc,
                        eloraDocumentRelationManager, versionLabelService);
            }

        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static void modifyCreatorAndCreated(CoreSession session,
            DocumentModel childDoc) {
        childDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_CREATOR,
                session.getPrincipal().toString());
        childDoc.setPropertyValue(NuxeoMetadataConstants.NX_DC_CREATED,
                new Date());
    }

    private static String getDocumenReference(DocumentModel doc) {
        String reference = null;
        if (doc.hasSchema(EloraSchemaConstants.ELORA_OBJECT)) {
            Serializable referencePropertyValue = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (referencePropertyValue != null) {
                reference = referencePropertyValue.toString();
            }
        }
        return reference;
    }

    private static void appendReferenceToChildrenTitle(CoreSession session,
            DocumentModel parentDocument, String reference) {
        String logInitMsg = "[appendReferenceToChildrenTitle] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        if (reference != null && reference.length() > 0) {

            // Iterate over its children
            DocumentModelIterator it = session.getChildrenIterator(
                    parentDocument.getRef());

            // For the instance reference is added only to BasicDocument types.
            for (DocumentModel childrenDoc : it) {
                if (childrenDoc.hasFacet(
                        EloraFacetConstants.FACET_BASIC_DOCUMENT)
                        && childrenDoc.hasSchema(
                                NuxeoSchemaConstants.DUBLINCORE)) {
                    String title = getDocumentTitle(childrenDoc);
                    if (title != null) {
                        if (title.length() > 0) {
                            title += " ";
                        }
                        title += reference;
                    }
                    childrenDoc.setPropertyValue(
                            NuxeoMetadataConstants.NX_DC_TITLE, title);
                    session.saveDocument(childrenDoc);
                }

                // Recursively call its children
                appendReferenceToChildrenTitle(session, childrenDoc, reference);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static void updateMainContentFileNameIfRequired(CoreSession session,
            DocumentModel document) {
        String logInitMsg = "[updateMainContentFileNameIfRequired] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        if (Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_CREATE_DOCUMENT_FROM_TEMPLATE_RENAME_MAIN_FILE,
                Boolean.toString(false)))) {

            // check if it has a main file attached
            Blob mainFileBlob = getDocumentMainFile(document);

            if (mainFileBlob != null) {
                String newFilename = "";
                String currentReference = getDocumenReference(document);
                String currentTitle = getDocumentTitle(document);
                if (currentReference != null) {
                    newFilename = currentReference + " " + newFilename;
                }
                if (currentTitle != null) {
                    newFilename = newFilename + " " + currentTitle;
                }
                newFilename += "." + FilenameUtils.getExtension(
                        mainFileBlob.getFilename());

                mainFileBlob.setFilename(newFilename);
                document.setPropertyValue(
                        NuxeoMetadataConstants.NX_FILE_CONTENT,
                        (Serializable) mainFileBlob);
                log.trace(logInitMsg + "filename updated. newFilename = |"
                        + newFilename + "|");

                session.saveDocument(document);
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static void updateMainContentFileNameIfRequiredToChildren(
            CoreSession session, DocumentModel parentDocument) {
        String logInitMsg = "[updateMainContentFileNameIfRequiredToChildren] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Iterate over its children
        DocumentModelIterator it = session.getChildrenIterator(
                parentDocument.getRef());

        //
        for (DocumentModel childrenDoc : it) {

            updateMainContentFileNameIfRequired(session, childrenDoc);

            updateMainContentFileNameIfRequiredToChildren(session, childrenDoc);
        }

        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private static String getDocumentTitle(DocumentModel doc) {
        String title = null;
        if (doc.hasSchema(NuxeoSchemaConstants.DUBLINCORE)) {
            Serializable titlePropertyValue = doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_TITLE);
            if (titlePropertyValue != null) {
                title = titlePropertyValue.toString();
            }
        }
        return title;
    }

    private static Blob getDocumentMainFile(DocumentModel doc) {
        Blob blob = null;
        if (doc.hasSchema(NuxeoSchemaConstants.MAIN_BLOB_SCHEMA)) {
            Serializable blobPropertyValue = doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_FILE_CONTENT);
            if (blobPropertyValue != null) {
                blob = (Blob) blobPropertyValue;
            }
        }
        return blob;
    }

}
