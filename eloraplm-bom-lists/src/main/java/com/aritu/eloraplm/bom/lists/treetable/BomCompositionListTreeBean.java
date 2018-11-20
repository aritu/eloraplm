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
package com.aritu.eloraplm.bom.lists.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.BomListBean;
import com.aritu.eloraplm.bom.lists.BomListHelper;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.EditableRelationTreeBean;

/**
 * @author aritu
 *
 */

@Name("bomCompositionListTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCompositionListTreeBean extends EditableRelationTreeBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCompositionListTreeBean.class);

    @In
    protected transient BomListBean bomList;

    private Map<String, TreeNode> roots;

    private String importSourceType;

    private String importSourceUid;

    private String importList;

    public String getImportSourceType() {
        return importSourceType;
    }

    public void setImportSourceType(String importSourceType) {
        this.importSourceType = importSourceType;
    }

    public String getImportSourceUid() {
        return importSourceUid;
    }

    public void setImportSourceUid(String importSourceUid) {
        this.importSourceUid = importSourceUid;
    }

    public String getImportList() {
        return importList;
    }

    public void setImportList(String importList) {
        this.importList = importList;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        roots = new HashMap<>();

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");
            // TODO Ez dau funtzionauko edizioak ez dalako
            // RelationNodeService-etik heredatzen eta
            // EditableRelationTreeBean-etako nodeService propietatea ezin
            // dauelako erabili
            BomCompositionListNodeService nodeService = new BomCompositionListNodeService(
                    documentManager, bomList.getId());
            setRoot(nodeService.getRoot(currentDoc));
            setIsDirty(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    public TreeNode getRoot() {
        String bomListId = bomList.getId();
        if (roots.isEmpty() || !roots.containsKey(bomListId)) {
            createRoot();
        }
        return roots.get(bomListId);
    }

    @Override
    public void setRoot(TreeNode root) {
        String bomListId = bomList.getId();
        if (roots != null) {
            roots.put(bomListId, root);
        }
    }

    @Override
    public void collapseAll() {
        String bomListId = bomList.getId();
        if (roots != null && roots.containsKey(bomListId)) {
            collapseOrExpandAll(roots.get(bomListId), false);
        }
    }

    @Override
    public void expandAll() {
        String bomListId = bomList.getId();
        if (roots != null && roots.containsKey(bomListId)) {
            collapseOrExpandAll(roots.get(bomListId), true);
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        // Empty all the roots, so we don't get a tree of another document in
        // all the subtabs except in the first loaded
        roots = null;
        createRoot();
    }

    public void addRelationNode(DocumentModel currentDoc) {
        setPredicateUri(EloraRelationConstants.BOM_LIST_HAS_ENTRY);
        super.addRelationNode(currentDoc, false);
    }

    public String save(DocumentModel currentDoc, DocumentModel subjectDoc)
            throws EloraException, CheckinNotAllowedException {
        String superResponse = super.save(currentDoc, subjectDoc, false);

        // After the normal save, we check in the BOM List
        if (subjectDoc.isCheckedOut()) {
            subjectDoc.putContextData(VersioningService.CHECKIN_COMMENT,
                    "BOM list's relation changes saved.");
            subjectDoc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
            documentManager.saveDocument(subjectDoc);

            try {
                EloraRelationHelper.copyRelationsToLastVersion(subjectDoc,
                        eloraDocumentRelationManager, documentManager);
            } catch (EloraException e) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.relations.saved"));
            }
            documentManager.save();
        }
        return superResponse;
    }

    public void importBomList(DocumentModel currentDoc,
            DocumentModel subjectDoc) {

        String logInitMsg = "[importBomList] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // Get importSourceUid, and its BomList document of the current
            // bomList.
            DocumentModel sourceDoc;

            if (importSourceType.equals("current")) {
                sourceDoc = currentDoc;
            } else {
                sourceDoc = documentManager.getDocument(
                        new IdRef(importSourceUid));
            }

            if (sourceDoc == null) {
                throw new EloraException("Source document is null.");
            }

            log.trace(logInitMsg + "Importing BOM List |" + importList
                    + "| from document |" + sourceDoc.getId() + "| to list |"
                    + bomList.getLabel() + "| of document |"
                    + currentDoc.getId() + "|.");

            // Difference between EBOM and BOM Lists
            DocumentModel sourceBomList = null;
            Resource predicateResource = null;
            if (importList.equals("Ebom")) {
                sourceBomList = sourceDoc;
                // TODO Predicate bat baino gehixau????'
                predicateResource = new ResourceImpl(
                        EloraRelationConstants.BOM_COMPOSED_OF);
            } else {
                DocumentModelList sourceBomLists = BomListHelper.getBomListForDocument(
                        sourceDoc, importList, false, documentManager);
                if (sourceBomLists == null || sourceBomLists.isEmpty()) {
                    throw new EloraException(
                            "Source document has no BOM list.");
                } else if (sourceBomLists.size() > 1) {
                    throw new EloraException(
                            "Source document has more than one BOM list.");
                }
                sourceBomList = sourceBomLists.get(0);

                // Get BomList related documents, and foreach, create a relation
                // to the current BomList
                predicateResource = new ResourceImpl(
                        EloraRelationConstants.BOM_LIST_HAS_ENTRY);
            }

            List<Statement> sourceStatements = RelationHelper.getStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, sourceBomList,
                    predicateResource);
            for (Statement sourceStmt : sourceStatements) {
                EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                        sourceStmt);
                eloraDocumentRelationManager.addRelation(documentManager,
                        subjectDoc, stmtInfo.getObject(),
                        EloraRelationConstants.BOM_LIST_HAS_ENTRY,
                        stmtInfo.getComment(), stmtInfo.getQuantity(),
                        stmtInfo.getOrdering());
            }

            log.trace(logInitMsg + "BOM list imported.");

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("eloraplm.message.success.bom.list.import"));

            createRoot();
        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            // TODO Aldatu??
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            // navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            // TODO Aldatu??
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            // navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }

    }

    @Override
    @Factory(value = "bomCompositionListRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}
