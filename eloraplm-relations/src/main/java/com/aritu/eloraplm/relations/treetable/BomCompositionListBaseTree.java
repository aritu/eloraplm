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
package com.aritu.eloraplm.relations.treetable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.RelationEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.webapp.util.EloraAjax;

/**
 *
 * @author aritu
 *
 */
public abstract class BomCompositionListBaseTree extends CoreTree {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCompositionListBaseTree.class);

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected transient WebActions webActions;

    protected RelationNodeService nodeService;

    // Add Relation form properties

    private String predicateUri;

    private String objectDocumentWcUid;

    private String objectDocumentTitle;

    private DocumentModel objectDocument;

    private String comment;

    private String quantity = "1";

    private Integer ordering = 0;

    private Integer directorOrdering;

    private Integer viewerOrdering;

    private Integer inverseViewerOrdering;

    private boolean isManual = false;

    private String designators;

    private boolean isExcludedFromMapping = false;

    private boolean addDirectRelations = false;

    private int maxOrdering = 0;

    @BypassInterceptors
    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    @BypassInterceptors
    public String getObjectDocumentWcUid() {
        return objectDocumentWcUid;
    }

    public void setObjectDocumentWcUid(String objectDocumentWcUid) {
        this.objectDocumentWcUid = objectDocumentWcUid;
        if (objectDocumentWcUid != null) {
            this.objectDocument = documentManager.getDocument(new IdRef(objectDocumentWcUid));
        }
    }

    @BypassInterceptors
    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
    }

    public DocumentModel getObjectDocument() {
        return objectDocument;
    }

    public void setObjectDocument(DocumentModel objectDocument) {
        this.objectDocument = objectDocument;
    }

    @BypassInterceptors
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @BypassInterceptors
    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @BypassInterceptors
    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    @BypassInterceptors
    public Integer getDirectorOrdering() {
        return directorOrdering;
    }

    public void setDirectorOrdering(Integer directorOrdering) {
        this.directorOrdering = directorOrdering;
    }

    @BypassInterceptors
    public Integer getViewerOrdering() {
        return viewerOrdering;
    }

    public void setViewerOrdering(Integer viewerOrdering) {
        this.viewerOrdering = viewerOrdering;
    }

    @BypassInterceptors
    public Integer getInverseViewerOrdering() {
        return inverseViewerOrdering;
    }

    public void setInverseViewerOrdering(Integer inverseViewerOrdering) {
        this.inverseViewerOrdering = inverseViewerOrdering;
    }

    @BypassInterceptors
    public boolean getIsManual() {
        return isManual;
    }

    public void setIsManual(boolean isManual) {
        this.isManual = isManual;
    }

    public void toggleIsManual(TreeTable table, TreeNode node) {
        BaseRelationNodeData nodeData = (BaseRelationNodeData) node.getData();
        nodeData.toggleIsManual();
        markRelationNodeAsModified(table, node);
    }

    @BypassInterceptors
    public String getDesignators() {
        return designators;
    }

    public void setDesignators(String designators) {
        this.designators = designators;
    }

    @BypassInterceptors
    public boolean getIsExcludedFromMapping() {
        return isExcludedFromMapping;
    }

    public void setIsExcludedFromMapping(boolean isExcludedFromMapping) {
        this.isExcludedFromMapping = isExcludedFromMapping;
    }

    public void toggleIsExcludedFromMapping(TreeTable table, TreeNode node) {
        BaseRelationNodeData nodeData = (BaseRelationNodeData) node.getData();
        nodeData.toggleIsExcludedFromMapping();
        markRelationNodeAsModified(table, node);
    }

    @BypassInterceptors
    public boolean getAddDirectRelations() {
        return addDirectRelations;
    }

    public void setAddDirectRelations(boolean addDirectRelations) {
        this.addDirectRelations = addDirectRelations;
    }

    public int getMaxOrdering() {
        return maxOrdering;
    }

    public void setMaxOrdering(int maxOrdering) {
        this.maxOrdering = maxOrdering;
    }

    public BomCompositionListBaseTree() {
    }

    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic, boolean isInverse) {
        addRelationNode(currentDoc);
    }

    public void addRelationNode(DocumentModel currentDoc) {
        String logInitMsg = "[addRelationNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding node |" + objectDocument.getId()
                + "| to tree of document |" + currentDoc.getId() + "|.");

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }

        try {

            if (areRelationsEditable()) {

                // TODO NOLA KONTROLATZEN DIRA HEMEN ERLAZIO ZIRKULARRAK?
                if (!EloraRelationHelper.isCircularRelation(currentDoc,
                        objectDocument, false, documentManager)) {

                    Map<String, Serializable> ctxProperties = new HashMap<String, Serializable>();

                    ctxProperties.put(
                            EloraGeneralConstants.CONTEXT_KEY_PREDICATE,
                            predicateUri);

                    ctxProperties.put(
                            EloraGeneralConstants.CONTEXT_KEY_STMT_OBJECT_DOC,
                            objectDocument);

                    // Nuxeo Event
                    EloraEventHelper.fireEvent(
                            RelationEventNames.ABOUT_TO_ADD_RELATION_NODE,
                            currentDoc, ctxProperties);

                    String nextNodeId = Integer.toString(
                            getRoot().getChildCount() + 1);

                    // Add a new EditableRelationNode
                    // Some values are not real because we will need and
                    // calculate them after we save the tree
                    TreeNode node = createNewTreeNode(nextNodeId, 1,
                            objectDocument.getId(),
                            objectDocument,
                            objectDocument, null,
                            predicateUri, quantity, comment, ordering,
                            directorOrdering, viewerOrdering,
                            inverseViewerOrdering, isManual, designators,
                            isExcludedFromMapping, false, false, true, false);

                    // Set node's initial expanded state
                    node.setExpanded(false);

                    log.trace(logInitMsg + "Node |" + objectDocument.getId()
                            + "| added.");

                    resetCreateFormValues();

                    // Update max ordering
                    if (getOrdering() != null
                            && getOrdering() > getMaxOrdering()) {
                        setMaxOrdering(getOrdering());
                        setOrdering(getMaxOrdering() + 1);
                    }

                    setIsDirty(true);

                    // When we reset tabs, the current tab/subtab is lost,
                    // so we have to get it before, and reset it after
                    String currentTabId = webActions.getCurrentTabId();
                    String currentSubTabId = webActions.getCurrentSubTabId();
                    webActions.resetTabList();
                    webActions.setCurrentTabId(currentTabId);
                    webActions.setCurrentSubTabId(currentSubTabId);

                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.relation.added"));

                    return;

                } else {
                    log.error(logInitMsg
                            + "Adding this object document will cause a circular relation.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.circularRelation"));
                }
            } else {
                log.error(logInitMsg
                        + "The subject document is locked by another user.");
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } catch (Exception e) {
            Serializable errorKey = currentDoc.getContextData(
                    EloraGeneralConstants.CONTEXT_ERROR_KEY);
            if (errorKey != null) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(errorKey.toString()));
            } else {
                log.error(logInitMsg + "Uncontrolled exception: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);

                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.addRelationNode"));
            }
        }

        resetCreateFormValues();
    }

    protected void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentWcUid = null;
        objectDocumentTitle = null;
        objectDocument = null;
        comment = null;
        quantity = "1";
        directorOrdering = null;
        viewerOrdering = null;
        inverseViewerOrdering = null;
        isManual = false;
        designators = null;
        addDirectRelations = false;
        ordering = maxOrdering + 1;
    }

    protected TreeNode createNewTreeNode(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, String quantity, String comment,
            Integer ordering, Integer directorOrdering, Integer viewerOrdering,
            Integer inverseViewerOrdering, boolean isManual, String designators,
            Boolean isExcludedFromMapping, boolean isSpecial, boolean isDirect,
            boolean isNew, boolean isRemoved) {

        RelationNodeData newNodeData = nodeService.saveRelationNodeData(id,
                level, docId, data, wcDoc, stmt, predicateUri, quantity,
                comment, ordering, directorOrdering, viewerOrdering,
                inverseViewerOrdering, isManual, designators,
                isExcludedFromMapping, isSpecial, isDirect);

        newNodeData.setIsNew(isNew);
        newNodeData.setIsRemoved(isRemoved);

        return new DefaultTreeNode(newNodeData, getRoot());
    }

    public void markRelationNodeAsModified(TreeTable table, TreeNode node) {
        markRelationNodeAsModified(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void markRelationNodeAsModified(TreeNode node) {
        BaseRelationNodeData nodeData = (BaseRelationNodeData) node.getData();
        nodeData.setIsModified(true);
        setIsDirty(true);
    }

    public void markRelationNodeAsRemoved(TreeTable table, TreeNode node,
            DocumentModel currentDoc, boolean isAnarchic) {
        markRelationNodeAsRemoved(node, currentDoc, isAnarchic);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());

    }

    private void markRelationNodeAsRemoved(TreeNode node,
            DocumentModel currentDoc, boolean isAnarchic) {
        toggleRelationNodeAsRemoved(true, isAnarchic, node, currentDoc);
    }

    public void unmarkRelationNodeAsRemoved(TreeTable table, TreeNode node,
            DocumentModel currentDoc, boolean isAnarchic) {
        unmarkRelationNodeAsRemoved(node, currentDoc, isAnarchic);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());

    }

    private void unmarkRelationNodeAsRemoved(TreeNode node,
            DocumentModel currentDoc, boolean isAnarchic) {
        toggleRelationNodeAsRemoved(false, isAnarchic, node, currentDoc);
    }

    private void toggleRelationNodeAsRemoved(boolean isRemoved,
            boolean isAnarchic, TreeNode node, DocumentModel currentDoc) {
        String logInitMsg = "[toggleRelationNodeAsRemoved] ["
                + documentManager.getPrincipal().getName() + "] ";

        String message;

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }

        if (areRelationsEditable()) {
            BaseRelationNodeData nodeData = (BaseRelationNodeData) node.getData();
            // If it is a new node, remove it completely
            if (nodeData.getIsNew()) {
                String docId = nodeData.getDocId();
                message = "eloraplm.message.success.relation.removed";
                getRoot().getChildren().remove(node);

                log.trace(logInitMsg + "Node |" + docId
                        + "| removed as it was marked to remove and was new.");

            } else {
                nodeData.setIsRemoved(isRemoved);

                if (isRemoved) {
                    message = "eloraplm.message.success.relation.markedToRemove";
                    log.trace(logInitMsg + "Node |" + nodeData.getDocId()
                            + "| marked to remove on tree of document |"
                            + currentDoc.getId() + "|.");
                } else {
                    message = "eloraplm.message.success.relation.unmarkedToRemove";
                    log.trace(logInitMsg + "Node |" + nodeData.getDocId()
                            + "| unmarked to remove on tree of document |"
                            + currentDoc.getId() + "|.");
                }

            }

            setIsDirty(true);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get(message));

        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentLockedByOther"));
        }
    }

    public String save(DocumentModel currentDoc, DocumentModel subjectDoc)
            throws EloraException {

        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Saving relation changes to document |"
                + currentDoc.getId() + "|");

        if (getIsDirty()) {

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                if (currentDoc.isProxy()) {
                    currentDoc = documentManager.getWorkingCopy(
                            currentDoc.getRef());
                }
                if (subjectDoc.isProxy()) {
                    subjectDoc = documentManager.getWorkingCopy(
                            subjectDoc.getRef());
                }

                // TODO: Hay que sacar estos chequeos de aqui. Si cambia algo
                // termina afectando a cosas que no deberia. Por ejemplo, al
                // pasar el parametro isAnarchic para el chequeo, me ha hecho
                // cambiar plantillas y clases que les debería dar igual este
                // cambio
                // TODO Begiratu hau. Checkoutakin aldatu egin da
                if (areRelationsEditable()) {

                    List<TreeNode> firstLevelNodes = getRoot().getChildren();
                    if (firstLevelNodes != null && !firstLevelNodes.isEmpty()) {
                        for (TreeNode childNode : firstLevelNodes) {

                            BaseRelationNodeData nodeData = (BaseRelationNodeData) childNode.getData();

                            if (nodeData.getIsNew()) {
                                createRelation(nodeData, subjectDoc);
                                log.trace(logInitMsg + "Added related child |"
                                        + subjectDoc.getId() + "|.");

                            } else if (nodeData.getIsRemoved()) {
                                removeRelation(nodeData, subjectDoc);
                                log.trace(logInitMsg + "Removed related child |"
                                        + subjectDoc.getId() + "|.");
                            } else if (nodeData.getIsModified()) {
                                updateRelation(nodeData, subjectDoc);
                                log.trace(logInitMsg + "Updated related child |"
                                        + subjectDoc.getId() + "|.");
                            }
                        }
                    }

                    log.trace(logInitMsg + "Relation changes saved.");

                    createRoot();
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.relations.saved"));

                } else {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "label.relation.documentLockedByOther"));
                }
                return null;

            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get(e.getMessage()));
                TransactionHelper.setTransactionRollbackOnly();
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

        } else {
            log.trace(logInitMsg + "Nothing to save.");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                    "eloraplm.message.warning.treetable.nothingToSave"));
        }

        return null;
    }

    private void createRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc) throws EloraException {
        // Process new relation
        Node object = null;

        DocumentModel objectDoc = nodeData.getData();
        String objectDocUid = objectDoc.getId();
        String repositoryName = navigationContext.getCurrentServerLocation().getName();
        String localName = repositoryName + "/" + objectDocUid;
        object = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                localName);
        try {
            eloraDocumentRelationManager.addRelation(documentManager,
                    subjectDoc, object, nodeData.getPredicateUri(), false,
                    false, nodeData.getComment(), nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());
        } catch (RelationNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.label.error.relation.notAllowed"));
            throw new EloraException(
                    "Error creating relation: relation not allowed.", e);
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }
    }

    private void removeRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc) throws EloraException {

        String logInitMsg = "[removeRelation] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel objectDoc;

        objectDoc = RelationHelper.getDocumentModel(
                nodeData.getStmt().getObject(), documentManager);

        eloraDocumentRelationManager.softDeleteRelation(documentManager,
                subjectDoc, nodeData.getPredicateUri(), objectDoc);

        log.info(logInitMsg + "Removed relation between subject |"
                + subjectDoc.getId() + "| and object |" + objectDoc.getId()
                + "| with predicate URI |" + nodeData.getPredicateUri() + "|.");
    }

    private void updateRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc) throws EloraException {
        try {

            DocumentModel objectDoc = nodeData.getData();

            eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    documentManager, subjectDoc, nodeData.getPredicateUri(),
                    objectDoc, nodeData.getData(), nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());
        } catch (RelationNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.label.error.relation.notAllowed"));
            throw new EloraException(
                    "Error creating relation: relation not allowed.", e);
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }
    }

    public void refreshNode(TreeTable table, TreeNode node) {
        refreshNode(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void refreshNode(TreeNode node) {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";

        RelationNodeData nodeData = (RelationNodeData) node.getData();
        String oldDocId = nodeData.getData().getId();
        if (!nodeData.getDocId().equals(oldDocId)) {
            DocumentModel newDoc = documentManager.getDocument(
                    new IdRef(nodeData.getDocId()));
            nodeData.setData(newDoc);
            node.getChildren().clear();

            log.trace(logInitMsg + "Refreshed node from |" + oldDocId + "| to |"
                    + newDoc.getId() + "|.");
        }
        nodeData.setIsNew(false);
        nodeData.setIsModified(true);

        setIsDirty(true);
    }

    public boolean areRelationsEditable() {
        return documentManager.hasPermission(getCurrentDocument().getRef(),
                SecurityConstants.WRITE) && !getCurrentDocument().isImmutable()
                && EloraDocumentHelper.isNotLockedOrLockedByMe(
                        getCurrentDocument());
    }

}
