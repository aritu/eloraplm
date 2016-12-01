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

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

@Name("bomStructureAction")
@Scope(ScopeType.CONVERSATION)
public class BomStructureActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(BomStructureActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    public void createStructure() throws EloraException {
        String logInitMsg = "[createStructure] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            currentDoc = currentDoc.isProxy() ? documentManager.getWorkingCopy(currentDoc.getRef())
                    : currentDoc;

            // Check if it is locked
            if (!currentDoc.isLocked()) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("message.error.notLocked"));
                return;
            }

            // Get director cad documents
            DocumentModelList directorDocList = getDirectorDocuments(currentDoc);

            // TODO: Por ahora coger solo el primer director
            // for (DocumentModel director : directorDocList) {
            // createItemRelations(currentDoc, director);
            // }

            if (!directorDocList.isEmpty()) {
                // Remove all structure relations
                Resource predicateResource = new ResourceImpl(
                        EloraRelationConstants.BOM_COMPOSED_OF);
                RelationHelper.removeRelation(currentDoc, predicateResource,
                        null);
                createItemRelations(currentDoc, directorDocList.get(0));
                EloraDocumentHelper.checkOutDocument(currentDoc);
                navigationContext.invalidateCurrentDocument();
            } else {
                facesMessages.add(
                        StatusMessage.Severity.INFO,
                        messages.get("eloraplm.message.warning.autostructure.no.director.found"));
            }

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.autostructure"));

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(logInitMsg + "Uncontrolled exception: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    private void createItemRelations(DocumentModel item, DocumentModel director)
            throws EloraException {
        EloraConfigTable hierarchicalRelationsConfig = EloraConfigHelper.getCadHierarchicalRelationsConfig();
        // TODO: Tener en cuenta los special ??? En algun momento puede haber
        // hierarchical y special
        for (EloraConfigRow relationConfig : hierarchicalRelationsConfig.getValues()) {
            String predicateUri = relationConfig.getProperty("id").toString();
            Resource predicateResource = new ResourceImpl(predicateUri);
            List<Statement> directorStmts = RelationHelper.getStatements(
                    director, predicateResource);
            for (Statement directorStmt : directorStmts) {
                DocumentModel childDoc = RelationHelper.getDocumentModel(
                        directorStmt.getObject(), documentManager);
                predicateResource = new ResourceImpl(
                        EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);
                List<Statement> childDocStmts = EloraRelationHelper.getSubjectStatements(
                        childDoc, predicateResource);
                if (!childDocStmts.isEmpty()) {
                    for (Statement childDocStmt : childDocStmts) {
                        DocumentModel itemObject = RelationHelper.getDocumentModel(
                                childDocStmt.getSubject(), documentManager);
                        itemObject = documentManager.getWorkingCopy(itemObject.getRef());
                        eloraDocumentRelationManager.addRelation(
                                documentManager, item, itemObject,
                                EloraRelationConstants.BOM_COMPOSED_OF, false);
                    }
                } else {
                    facesMessages.add(
                            StatusMessage.Severity.WARN,
                            messages.get("eloraplm.message.warning.autostructure.missing.item"),
                            childDoc.getPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE));
                }
            }
        }
    }

    private DocumentModelList getDirectorDocuments(DocumentModel doc)
            throws EloraException {
        Resource predicateResource = new ResourceImpl(
                EloraRelationConstants.BOM_HAS_CAD_DOCUMENT);

        DocumentModelList docList = new DocumentModelListImpl();
        List<Statement> stmts = RelationHelper.getStatements(doc,
                predicateResource);
        for (Statement stmt : stmts) {
            // TODO: Por ahora se hace sencillo
            // EloraStatementInfoImpl stmtInfo = new
            // EloraStatementInfoImpl(stmt);
            // int directorOrdering = stmtInfo.getDirectorOrdering();
            DocumentModel object = RelationHelper.getDocumentModel(
                    stmt.getObject(), documentManager);
            // if (directorOrdering > 0
            // && !object.getTitle().equals(
            // EloraDoctypeConstants.CAD_DRAWING)) {

            object = EloraDocumentHelper.getLatestVersion(object,
                    documentManager);

            if (!object.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
                docList.add(object);
            }
        }
        return docList;
    }
}
