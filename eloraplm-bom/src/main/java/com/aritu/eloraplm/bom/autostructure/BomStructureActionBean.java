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

package com.aritu.eloraplm.bom.autostructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.bom.treetable.BomCompositionEbomTreeBean;
import com.aritu.eloraplm.config.util.RelationsConfig;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraEventNames;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("bomStructureAction")
@Scope(ScopeType.CONVERSATION)
public class BomStructureActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomStructureActionBean.class);

    private DocumentModel currentDoc;

    Map<String, List<DocumentModel>> itemList;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected transient BomCompositionEbomTreeBean bomCompositionEbomTreeBean;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public void createStructure() {
        String logInitMsg = "[createStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            setCurrentDocument();
            if (!currentDoc.isLocked()) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("message.error.notLocked"));
                return;
            }

            DocumentModelList directorDocList = getNotDrawingDirectorDocuments();
            // TODO: Por ahora coger solo el primer director
            if (!directorDocList.isEmpty()) {
                removeAllCompositionRelations();
                createItemRelationsFromDirectorHierarchicalStructure(
                        directorDocList.get(0));

                EloraEventHelper.fireEvent(
                        EloraEventNames.ELORA_BOM_STRUCT_UPDATED_EVENT,
                        currentDoc);

            } else {
                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.warning.autostructure.no.director.found"));
            }

            bomCompositionEbomTreeBean.createRoot();

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.autostructure"));
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.autostructure"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            navigationContext.invalidateCurrentDocument();
        }
        log.trace(logInitMsg + "--- EXIT --- ");
    }

    private DocumentModelList getNotDrawingDirectorDocuments()
            throws EloraException {
        String logInitMsg = "[getNotDrawingDirectorDocuments] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModelList docList = new DocumentModelListImpl();
        List<Statement> stmts = RelationHelper.getStatements(currentDoc,
                predicateResource);
        for (Statement stmt : stmts) {
            // TODO: Por ahora se hace sencillo
            // EloraStatementInfoImpl stmtInfo = new
            // EloraStatementInfoImpl(stmt);
            // Integer directorOrdering = stmtInfo.getDirectorOrdering();
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), documentManager);
            // if (directorOrdering > 0
            // && !object.getTitle().equals(
            // EloraDoctypeConstants.CAD_DRAWING)) {

            if (!object.isVersion()) {
                DocumentModel latestObject = EloraDocumentHelper.getLatestVersion(
                        object);
                if (latestObject == null) {
                    throw new EloraException("Document |" + object.getId()
                            + "| has no latest version or it is unreadable.");
                }
                object = latestObject;
            }
            if (!object.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
                docList.add(object);
                log.trace(logInitMsg + "Document |" + object.getId()
                        + "| added as director");
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");
        return docList;
    }

    private void setCurrentDocument() {
        currentDoc = navigationContext.getCurrentDocument();
        currentDoc = currentDoc.isProxy()
                ? documentManager.getWorkingCopy(currentDoc.getRef())
                : currentDoc;
    }

    private void removeAllCompositionRelations() {
        // Resource predicateResource = new ResourceImpl(
        // EloraRelationConstants.BOM_COMPOSED_OF);
        // RelationHelper.removeRelation(currentDoc, predicateResource, null);
        eloraDocumentRelationManager.softDeleteRelation(documentManager,
                currentDoc, EloraRelationConstants.BOM_COMPOSED_OF, null);

    }

    private void createItemRelationsFromDirectorHierarchicalStructure(
            DocumentModel director) {
        String logInitMsg = "[createItemRelationsFromDirectorHierarchicalStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        // TODO Hemen predicateka banaka egin beharrean zerrenda osoa batera
        // pasauta egin ahalko zan??
        for (String predicateUri : RelationsConfig.cadHierarchicalRelationsList) {
            Resource predicateResource = new ResourceImpl(predicateUri);
            processDirectorChildrensParentItems(director, predicateResource);
        }
        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void processDirectorChildrensParentItems(DocumentModel director,
            Resource predicateResource) {
        List<Resource> predicates = new ArrayList<Resource>();
        predicates.add(predicateResource);
        List<Statement> directorStmts = EloraRelationHelper.getStatements(
                director, predicates);
        for (Statement directorStmt : directorStmts) {
            DocumentModel childDoc = RelationHelper.getDocumentModel(
                    directorStmt.getObject(), documentManager);
            EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                    directorStmt);
            String quantity = stmtInfo.getQuantity();
            Integer ordering = stmtInfo.getOrdering();

            log.trace("Process director's child |" + childDoc.getId() + "|");
            processChildParentItems(childDoc, quantity, ordering);
            log.trace("Finish processing director's child |" + childDoc.getId()
                    + "|");
        }
    }

    private void processChildParentItems(DocumentModel childDoc,
            String quantity, Integer ordering) {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
        List<Statement> childDocStmts = EloraRelationHelper.getSubjectStatements(
                childDoc, predicateResource);

        if (!childDocStmts.isEmpty()) {
            itemList = new HashMap<>();
            for (Statement childDocStmt : childDocStmts) {
                DocumentModel item = RelationHelper.getDocumentModel(
                        childDocStmt.getSubject(), documentManager);
                if (!item.isCheckedOut()) {
                    // TODO: Esto hay que mejorarlo por rendimiento
                    getOrderedDocumentList(item);
                }
            }
            for (Map.Entry<String, List<DocumentModel>> entry : itemList.entrySet()) {
                List<DocumentModel> items = entry.getValue();
                DocumentModel relatedItem = null;
                if (items.size() > 1) {
                    log.trace("Multiple items related");
                    relatedItem = getRelatedLastItem(items);
                } else {
                    relatedItem = items.get(0);
                }
                log.trace("Process item |" + relatedItem.getId() + "|");
                processParentItem(childDoc, relatedItem, quantity, ordering);
                log.trace(
                        "Finish processing item |" + relatedItem.getId() + "|");
            }
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.autostructure.missing.item"),
                    childDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE));
        }
    }

    private void getOrderedDocumentList(DocumentModel item) {
        String versionSeriesId = documentManager.getVersionSeriesId(
                item.getRef());
        if (itemList.containsKey(versionSeriesId)) {
            itemList.get(versionSeriesId).add(item);
        } else {
            List<DocumentModel> relatedDocList = new ArrayList<>();
            relatedDocList.add(item);
            itemList.put(versionSeriesId, relatedDocList);
        }
    }

    private DocumentModel getRelatedLastItem(List<DocumentModel> docs) {
        List<String> uidList = EloraDocumentHelper.getUidListFromDocList(docs);
        Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(docs);
        String type = docs.get(0).getType();

        return EloraRelationHelper.getLatestRelatedVersion(documentManager,
                majorVersion, uidList, type);
    }

    private void processParentItem(DocumentModel childDoc, DocumentModel item,
            String quantity, Integer ordering) {
        if (isCorrectItemType(item)) {
            log.trace("Add relation to item |" + item.getId() + "|");
            eloraDocumentRelationManager.addRelation(documentManager,
                    currentDoc, item, EloraRelationConstants.BOM_COMPOSED_OF,
                    "", quantity, ordering);
        } else {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.autostructure.incorrect.item"),
                    childDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE));
        }
    }

    private boolean isCorrectItemType(DocumentModel item) {
        try {
            checkItemType(item);
            return true;
        } catch (EloraException e) {
            return false;
        }
    }

    private void checkItemType(DocumentModel item) throws EloraException {
        List<String> corTypes = getCorrespondingTypesForCurrentDoc();
        if (corTypes == null || corTypes.size() == 0
                || !corTypes.contains(item.getType())) {
            throw new EloraException("No corresponding type found");
        }
    }

    private List<String> getCorrespondingTypesForCurrentDoc() {
        List<String> types = new ArrayList<String>();

        String docType = currentDoc.getType();
        switch (docType) {
        case EloraDoctypeConstants.BOM_PART:
            types.add(EloraDoctypeConstants.BOM_PART);
            break;
        case EloraDoctypeConstants.BOM_PRODUCT:
            types.add(EloraDoctypeConstants.BOM_PART);
            types.add(EloraDoctypeConstants.BOM_PRODUCT);
            break;
        case EloraDoctypeConstants.BOM_PACKAGING:
            types.add(EloraDoctypeConstants.BOM_PACKAGING);
            break;
        case EloraDoctypeConstants.BOM_TOOL:
            types.add(EloraDoctypeConstants.BOM_TOOL);
            break;
        default:
            types = null;
        }

        return types;
    }
}
