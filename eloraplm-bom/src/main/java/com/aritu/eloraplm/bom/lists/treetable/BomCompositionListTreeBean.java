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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.lists.BomListConstants;
import com.aritu.eloraplm.bom.lists.BomListHelper;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.BomCompositionListBaseTree;
import com.aritu.eloraplm.treetable.NodeData;

/**
 * @author aritu
 *
 */

@Name(BomCompositionListTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomCompositionListTreeBean extends BomCompositionListBaseTree {

    public static final String NAME = "bomCompositionListTreeBean";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCompositionListTreeBean.class);

    public static final String IMPORT_SOURCE_TYPE_CURRENT = "current";

    private Map<String, TreeNode> roots;

    private String importSourceType;

    private String importSourceUid;

    private String importList;

    private boolean clearBeforeImport;

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

    public boolean getClearBeforeImport() {
        return clearBeforeImport;
    }

    public void setClearBeforeImport(boolean clearBeforeImport) {
        this.clearBeforeImport = clearBeforeImport;
    }

    public BomCompositionListTreeBean() {
        setIsManual(true);
        setMaxOrdering(1);
        setOrdering(1);
        importSourceType = IMPORT_SOURCE_TYPE_CURRENT;
        importList = BomListConstants.BOM_LIST_EBOM;
        clearBeforeImport = false;
    }

    private String bomListId;

    private String getBomListId() {
        if (bomListId == null) {
            bomListId = webActions.getCurrentSubTabId().replace(
                    webActions.getCurrentTabId().concat("_"), "");
        }
        return bomListId;
    }

    @Observer(WebActions.CURRENT_TAB_CHANGED_EVENT
            + "_TAB_BOM_COMPOSITION_sub_tab")
    @BypassInterceptors
    public void resetBomListId() {
        bomListId = null;
    }

    @Override
    public void createRoot() {
        String logInitMsg = "[createRoot] ["
                + documentManager.getPrincipal().getName() + "] ";

        roots = new HashMap<>();

        DocumentModel currentDoc = getCurrentDocument();
        try {
            log.trace(logInitMsg + "Creating tree...");

            nodeService = new BomCompositionListNodeService(documentManager,
                    getBomListId());

            setRoot(nodeService.getRoot(currentDoc));

            // Update max ordering
            setMaxOrdering(((BomListNodeService) nodeService).getMaxOrdering());
            setOrdering(getMaxOrdering() + 1);

            setIsDirty(false);
            setHasUnreadableNodes(false);
            setIsInvalid(false);
            log.trace(logInitMsg + "Tree created.");
        } catch (DocumentSecurityException e) {
            log.error(logInitMsg + e.getMessage());
            // empty root attribute and set hasUnreadableNodes attribute to true
            setRoot(new DefaultTreeNode());
            setHasUnreadableNodes(true);
            setIsInvalid(false);
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            // empty root attribute and set isInvalid attribute to true
            setRoot(new DefaultTreeNode());
            setIsInvalid(true);
            setHasUnreadableNodes(false);

            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.treetable.createRoot"));
        }
    }

    @Override
    public TreeNode getRoot() {
        if (roots.isEmpty() || !roots.containsKey(getBomListId())) {
            createRoot();
        }
        return roots.get(getBomListId());
    }

    @Override
    public void setRoot(TreeNode root) {
        if (roots != null) {
            roots.put(getBomListId(), root);
        }
        firstLevelChildrenCount = root.getChildCount();
        childrenCount = countChildren(root, 0);
    }

    @Override
    public void collapseAll(TreeTable treeTable) {
        if (roots != null && roots.containsKey(getBomListId())) {
            collapseOrExpandAll(roots.get(getBomListId()), false);
            clearFilters(treeTable);
        }
    }

    @Override
    public void expandAll(TreeTable treeTable) {
        if (roots != null && roots.containsKey(getBomListId())) {
            collapseOrExpandAll(roots.get(getBomListId()), true);
            clearFilters(treeTable);
        }
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        // Empty all the roots, so we don't get a tree of another document in
        // all the subtabs except in the first loaded
        roots = null;
        emptyImportFormProperties();
        createRoot();
    }

    public void emptyImportFormProperties() {
        importSourceType = IMPORT_SOURCE_TYPE_CURRENT;
        importSourceUid = null;
        importList = BomListConstants.BOM_LIST_EBOM;
        clearBeforeImport = false;
    }

    @Override
    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic, boolean isInverse) {
        setPredicateUri(EloraRelationConstants.BOM_LIST_HAS_ENTRY);
        super.addRelationNode(currentDoc, isAnarchic, isInverse);
    }

    public void importBomList(DocumentModel currentDoc,
            DocumentModel subjectDoc) {

        String logInitMsg = "[importBomList] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if (areRelationsEditable()) {

                // Get importSourceUid, and its BomList document of the current
                // bomList.
                DocumentModel sourceDoc;

                if (importSourceType.equals(IMPORT_SOURCE_TYPE_CURRENT)) {
                    sourceDoc = currentDoc;
                } else {
                    sourceDoc = documentManager.getDocument(
                            new IdRef(importSourceUid));
                }

                if (sourceDoc == null) {
                    throw new EloraException("Source document is null.");
                }

                log.trace(logInitMsg + "Importing BOM List |" + importList
                        + "| from document |" + sourceDoc.getId()
                        + "| to list |" + getBomListId() + "| of document |"
                        + currentDoc.getId() + "|.");

                // Difference between EBOM and BOM Lists
                Resource predicateResource = null;
                if (importList.equals(BomListConstants.BOM_LIST_EBOM)) {
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
                    sourceDoc = sourceBomLists.get(0);

                    // Get BomList related documents, and foreach, create a
                    // relation to the current BomList
                    predicateResource = new ResourceImpl(
                            EloraRelationConstants.BOM_LIST_HAS_ENTRY);
                }

                if (clearBeforeImport) {
                    // Delete all BomListHasEntry relations
                    eloraDocumentRelationManager.softDeleteRelation(
                            documentManager, subjectDoc,
                            EloraRelationConstants.BOM_LIST_HAS_ENTRY, null);
                }

                List<Statement> sourceStatements = RelationHelper.getStatements(
                        EloraRelationConstants.ELORA_GRAPH_NAME, sourceDoc,
                        predicateResource);
                for (Statement sourceStmt : sourceStatements) {
                    EloraStatementInfoImpl stmtInfo = new EloraStatementInfoImpl(
                            sourceStmt);
                    try {
                        eloraDocumentRelationManager.addRelation(
                                documentManager, subjectDoc,
                                stmtInfo.getObject(),
                                EloraRelationConstants.BOM_LIST_HAS_ENTRY,
                                stmtInfo.getComment(), stmtInfo.getQuantity(),
                                stmtInfo.getOrdering());
                    } catch (RelationAlreadyExistsException e) {
                        // If relation exists, ignore it and continue.
                    }
                }

                log.trace(logInitMsg + "BOM list imported.");

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.bom.list.import"));

                createRoot();
                emptyImportFormProperties();
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } catch (EloraException e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.bom.list.import"));
            TransactionHelper.setTransactionRollbackOnly();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.bom.list.import"));
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

    @Override
    public List<NodeData> getTreeAsList() {
        List<NodeData> nodeList = new ArrayList<NodeData>();
        if (!roots.isEmpty() && roots.containsKey(getBomListId())) {
            nodeList = processNodeAndChildren(nodeList, roots.get(getBomListId()));
        }
        return nodeList;
    }

    private List<NodeData> processNodeAndChildren(List<NodeData> nodeList, TreeNode node) {
        for (TreeNode child : node.getChildren()) {
            nodeList.add((NodeData) child.getData());
            nodeList = processNodeAndChildren(nodeList, child);
        }
        return nodeList;
    }

}
