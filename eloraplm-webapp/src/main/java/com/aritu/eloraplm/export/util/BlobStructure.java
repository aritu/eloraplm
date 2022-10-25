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
package com.aritu.eloraplm.export.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BlobStructure {

    private static BlobList blobs;

    private static List<Resource> hierarchicalAndSuppressedPredicates;

    private static List<Resource> specialPredicates;

    private static List<Resource> directAndIconOnlyPredicates;

    private static List<String> versionSeriesIdList;

    public static BlobList getStructureBlobs(DocumentModel doc,
            RelationManager relationManager, CoreSession session)
            throws EloraException {

        loadConfiguration();
        blobs = new BlobList();
        versionSeriesIdList = new ArrayList<String>();
        versionSeriesIdList.add(doc.getVersionSeriesId());
        addDocumentBlob(doc);
        processRelations(doc,
                doc.getType().equals(EloraDoctypeConstants.CAD_DRAWING),
                relationManager, session);
        return blobs;
    }

    private static void loadConfiguration() {
        loadHierarchicalAndSuppressedRelations();
        loadSpecialRelations();
        loadDirectAndIconOnlyRelations();
    }

    private static void loadHierarchicalAndSuppressedRelations() {
        List<String> hierarchicalAndSuppressedRelationsList = new ArrayList<String>();
        hierarchicalAndSuppressedRelationsList.addAll(
                RelationsConfig.cadHierarchicalRelationsList);
        hierarchicalAndSuppressedRelationsList.addAll(
                RelationsConfig.cadSuppressedRelationsList);

        hierarchicalAndSuppressedPredicates = new ArrayList<Resource>();
        for (String predicateUri : hierarchicalAndSuppressedRelationsList) {
            hierarchicalAndSuppressedPredicates.add(
                    new ResourceImpl(predicateUri));
        }
    }

    private static void loadSpecialRelations() {
        specialPredicates = new ArrayList<Resource>();
        for (String predicateUri : RelationsConfig.cadSpecialRelationsList) {
            specialPredicates.add(new ResourceImpl(predicateUri));
        }
    }

    private static void loadDirectAndIconOnlyRelations() {
        List<String> directAndIconOnlyRelationsList = new ArrayList<String>();
        directAndIconOnlyRelationsList.addAll(
                RelationsConfig.cadDirectRelationsList);
        directAndIconOnlyRelationsList.addAll(
                RelationsConfig.cadIconOnlyRelationsList);
        directAndIconOnlyPredicates = new ArrayList<Resource>();
        for (String predicateUri : directAndIconOnlyRelationsList) {
            directAndIconOnlyPredicates.add(new ResourceImpl(predicateUri));
        }
    }

    private static void processRelations(DocumentModel doc,
            boolean isRootSpecial, RelationManager relationManager,
            CoreSession session) throws EloraException {
        processHierarchicalRelations(doc, relationManager, session);
        processSpecialRelations(doc, isRootSpecial, relationManager, session);
        processDirectAndIconOnlyRelations(doc, relationManager, session);
    }

    private static void processHierarchicalRelations(DocumentModel doc,
            RelationManager relationManager, CoreSession session)
            throws EloraException {
        List<Statement> statements = getStatements(doc, true,
                hierarchicalAndSuppressedPredicates);
        processStatements(doc, statements, false, false, relationManager,
                session);
    }

    private static void processSpecialRelations(DocumentModel doc,
            boolean isRootSpecial, RelationManager relationManager,
            CoreSession session) throws EloraException {
        List<Statement> statements = getStatements(doc, isRootSpecial,
                specialPredicates);
        processSpecialStatements(doc, statements, isRootSpecial,
                relationManager, session);
    }

    private static void processDirectAndIconOnlyRelations(DocumentModel doc,
            RelationManager relationManager, CoreSession session)
            throws EloraException {
        List<Statement> statements = getStatements(doc, true,
                directAndIconOnlyPredicates);
        processStatements(doc, statements, false, false, relationManager,
                session);
    }

    private static List<Statement> getStatements(DocumentModel doc,
            boolean isRootSpecial, List<Resource> predicateList) {
        List<Statement> statements = new ArrayList<Statement>();
        if (!predicateList.isEmpty()) {
            if (isRootSpecial) {
                statements = EloraRelationHelper.getStatements(doc,
                        predicateList);
            } else {
                statements = EloraRelationHelper.getSubjectStatementsByPredicateList(
                        doc, predicateList);
            }
        }
        return statements;
    }

    private static void processSpecialStatements(DocumentModel doc,
            List<Statement> statements, boolean isRootSpecial,
            RelationManager relationManager, CoreSession session)
            throws EloraException {
        if (!statements.isEmpty()) {
            processStatements(doc, statements, true, isRootSpecial,
                    relationManager, session);
        }
    }

    private static void processStatements(DocumentModel cadParent,
            List<Statement> statements, boolean isSpecial,
            boolean isRootSpecial, RelationManager relationManager,
            CoreSession session) throws EloraException {
        for (Statement statement : statements) {
            DocumentModel cadChild = null;
            cadChild = getCadChildDocument(statement, isSpecial, isRootSpecial,
                    relationManager, session);
            if (cadChild != null) {
                // En el caso de los drawings que no son el root se mira esto
                // por segunda vez...
                String cadChildVersionSeriesId = cadChild.getVersionSeriesId();
                if (!versionSeriesIdList.contains(cadChildVersionSeriesId)) {
                    versionSeriesIdList.add(cadChildVersionSeriesId);
                    addDocumentBlob(cadChild);
                    processRelations(cadChild, false, relationManager, session);
                }
            }
        }
    }

    private static void addDocumentBlob(DocumentModel cadChild) {
        Blob blob = (Blob) cadChild.getPropertyValue(
                NuxeoMetadataConstants.NX_FILE_CONTENT);
        if (blob != null) {
            blobs.add(blob);
        }

        // Add CAD attachments
        if (cadChild.hasSchema(EloraSchemaConstants.CAD_ATTACHMENTS)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cadAtts = (List<Map<String, Object>>) cadChild.getPropertyValue(
                    EloraMetadataConstants.ELORA_CADATTS_ATTACHMENTS);
            if (cadAtts != null && !cadAtts.isEmpty()) {
                for (Map<String, Object> cadAtt : cadAtts) {
                    Blob attBlob = (Blob) cadAtt.get("file");
                    if (attBlob != null) {
                        blobs.add(attBlob);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static DocumentModel getCadChildDocument(Statement statement,
            boolean isSpecial, boolean isRootSpecial,
            RelationManager relationManager, CoreSession session)
            throws EloraException {
        DocumentModel cadChild = null;
        if (isSpecial) {
            if (isRootSpecial) {
                cadChild = EloraDocumentHelper.getDocumentModel(relationManager,
                        session, statement.getObject());
            } else {
                DocumentModel cadParent = EloraDocumentHelper.getDocumentModel(
                        relationManager, session, statement.getSubject());
                // Miro aqui si ya se ha calculado antes para no tener que
                // llamar de nuevo a la funcion de sacar el último relacionado
                String cadParentVersionSeriesId = cadParent.getVersionSeriesId();
                if (!versionSeriesIdList.contains(cadParentVersionSeriesId)) {
                    versionSeriesIdList.add(cadParentVersionSeriesId);
                    cadChild = EloraRelationHelper.getLatestRelatedReleasedVersion(
                            cadParent, statement, session);
                    addDocumentBlob(cadChild);
                }
            }
        } else {
            cadChild = EloraDocumentHelper.getDocumentModel(relationManager,
                    session, statement.getObject());
        }
        return cadChild;
    }

}
