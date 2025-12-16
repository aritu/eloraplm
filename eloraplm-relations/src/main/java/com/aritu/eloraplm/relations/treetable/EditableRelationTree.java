package com.aritu.eloraplm.relations.treetable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextInvalidation;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.component.treetable.TreeTable;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.RelationEventNames;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;
import com.aritu.eloraplm.treetable.CoreTree;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class EditableRelationTree extends CoreTree {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EditableRelationTree.class);

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected transient WebActions webActions;

    protected RelationNodeService nodeService;

    private Date currentLastModified;

    // Add Relation form properties

    private String predicateUri;

    private String objectDocumentWcUid;

    private String objectDocumentTitle;

    private List<SelectItem> objectDocumentVersionList;

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
            refreshObjectDocumentVersionList(documentManager.getDocument(
                    new IdRef(objectDocumentWcUid)));
        }
    }

    @BypassInterceptors
    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
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

    public EditableRelationTree() {
    }

    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic) {
        addRelationNode(currentDoc, isAnarchic, false);
    }

    public void addRelationNode(DocumentModel currentDoc, boolean isAnarchic,
            boolean isInverse) {
        String logInitMsg = "[addRelationNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding node |" + objectDocument.getId()
                + "| to tree of document |" + currentDoc.getId() + "|.");

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getSourceDocument(currentDoc.getRef());
        }

        try {

            if (areRelationsEditable(currentDoc, isAnarchic)) {
                // TODO txekeo gehixau??

                // TODO Inverse erlazioetan ez dabil?, eta Save egin gabe badago
                // beste nodo bat (benetako erlazioak sortu gabe), bere azpiko
                // dokumentuak ere ez ditu txekeatzen. Baina kasu normaletarako
                // balio du.
                if (!EloraRelationHelper.isCircularRelation(currentDoc,
                        objectDocument, isInverse, documentManager)) {
                    boolean objectHasVersion = documentManager.getLastDocumentVersion(
                            objectDocument.getRef()) != null;
                    boolean docHasVersion = documentManager.getLastDocumentVersion(
                            currentDoc.getRef()) != null;

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

                    if (!isAnarchic || (docHasVersion && objectHasVersion)) {
                        String nextNodeId = Integer.toString(
                                getRoot().getChildCount() + 1);

                        DocumentModel wcDoc = null;
                        if (objectDocument.isImmutable()) {
                            wcDoc = documentManager.getWorkingCopy(
                                    objectDocument.getRef());
                        } else {
                            wcDoc = objectDocument;
                        }

                        // Add a new EditableRelationNode
                        // Some values are not real because we will need and
                        // calculate them after we save the tree
                        TreeNode node = createNewTreeNode(nextNodeId, 1,
                                objectDocument.getId(),
                                objectDocument, wcDoc, null,
                                predicateUri, quantity, comment, ordering,
                                directorOrdering, viewerOrdering,
                                inverseViewerOrdering, isManual, designators,
                                isExcludedFromMapping, false, false, true,
                                false);

                        // Set node's initial expanded state
                        node.setExpanded(false);

                        // Add direct relations checkbox is selected
                        if (addDirectRelations) {
                            addDirectRelations(currentDoc, objectDocument);
                        }

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

                        facesMessages.add(StatusMessage.Severity.INFO,
                                messages.get(
                                        "eloraplm.message.success.relation.added"));

                        return;

                    } else {
                        log.error(
                                logInitMsg + "The object document has no AVs.");
                        facesMessages.add(StatusMessage.Severity.WARN,
                                messages.get(
                                        "eloraplm.message.warning.relations.edit.documentWithoutVersion"));
                    }
                } else {
                    log.error(logInitMsg
                            + "Adding this object document will cause a circular relation.");
                    facesMessages.add(StatusMessage.Severity.WARN, messages.get(
                            "eloraplm.message.error.circularRelation"));
                }
            } else if (!currentDoc.isLocked()) {
                log.error(logInitMsg + "The subject document is not locked.");
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentNotLocked"));
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

        if (areRelationsEditable(currentDoc, isAnarchic)) {
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

        } else if (!currentDoc.isLocked()) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentLockedByOther"));
        }
    }

    public String save(DocumentModel currentDoc, DocumentModel subjectDoc,
            boolean isAnarchic) throws EloraException {
        return save(currentDoc, subjectDoc, isAnarchic, false);
    }

    public String save(DocumentModel currentDoc, DocumentModel subjectDoc,
            boolean isAnarchic, boolean isInverse) throws EloraException {

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
                if (areRelationsEditable(currentDoc, isAnarchic)) {

                    List<TreeNode> firstLevelNodes = getRoot().getChildren();
                    if (firstLevelNodes != null && !firstLevelNodes.isEmpty()) {
                        for (TreeNode childNode : firstLevelNodes) {

                            BaseRelationNodeData nodeData = (BaseRelationNodeData) childNode.getData();

                            if (nodeData.getIsNew()) {
                                createRelation(nodeData, subjectDoc, isAnarchic,
                                        isInverse);
                                log.trace(logInitMsg + "Added related child |"
                                        + subjectDoc.getId() + "|.");

                            } else if (nodeData.getIsRemoved()) {
                                removeRelation(nodeData, subjectDoc, isAnarchic,
                                        isInverse);
                                log.trace(logInitMsg + "Removed related child |"
                                        + subjectDoc.getId() + "|.");
                            } else if (nodeData.getIsModified()) {
                                updateRelation(nodeData, subjectDoc, isAnarchic,
                                        isInverse);
                                log.trace(logInitMsg + "Updated related child |"
                                        + subjectDoc.getId() + "|.");
                            }
                        }
                    }

                    log.trace(logInitMsg + "Relation changes saved.");

                    if (!isAnarchic) {
                        EloraDocumentHelper.updateContributorAndModified(
                                currentDoc, true);
                    }

                    createRoot();
                    facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                            "eloraplm.message.success.relations.saved"));

                } else if (!currentDoc.isLocked()) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get("label.relation.documentNotLocked"));
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
            DocumentModel subjectDoc, boolean isAnarchic, boolean isInverse)
            throws EloraException {
        // Process new relation
        Node object = null;

        // TODO If it is Anarchic, it will always be the WC. Check?

        DocumentModel objectDoc = nodeData.getData();
        String objectDocUid = objectDoc.getId();
        String repositoryName = navigationContext.getCurrentServerLocation().getName();
        String localName = repositoryName + "/" + objectDocUid;
        object = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                localName);
        try {
            if (!isInverse) {
                eloraDocumentRelationManager.addRelation(documentManager,
                        subjectDoc, object, nodeData.getPredicateUri(),
                        isInverse, false, nodeData.getComment(),
                        nodeData.getQuantity(), nodeData.getOrdering(),
                        nodeData.getDirectorOrdering(),
                        nodeData.getViewerOrdering(),
                        nodeData.getInverseViewerOrdering(),
                        nodeData.getIsManual(), nodeData.getDesignators(),
                        nodeData.getIsExcludedFromMapping());
            } else {
                if (EloraDocumentHelper.isNotLockedOrLockedByMe(objectDoc)) {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            subjectDoc, object, nodeData.getPredicateUri(),
                            isInverse, false, nodeData.getComment(),
                            nodeData.getQuantity(), nodeData.getOrdering(),
                            nodeData.getDirectorOrdering(),
                            nodeData.getViewerOrdering(),
                            nodeData.getInverseViewerOrdering(),
                            nodeData.getIsManual(), nodeData.getDesignators(),
                            nodeData.getIsExcludedFromMapping());
                } else {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            messages.get(
                                    "eloraplm.message.error.relations.edition.locked.by.another"),
                            objectDoc.getPropertyValue(
                                    EloraMetadataConstants.ELORA_ELO_REFERENCE));
                }
            }

            if (isAnarchic) {
                // Create relation among base documents
                DocumentModel baseSubjectDoc = EloraDocumentHelper.getLatestVersion(
                        subjectDoc);
                DocumentModel baseObjectDoc = EloraDocumentHelper.getLatestVersion(
                        objectDoc);

                if (baseSubjectDoc != null && baseObjectDoc != null) {
                    // TODO: Mirar si hay otra forma de sacar el node. Si no
                    // pensar en sobrecargar el metodo addrelation para poder
                    // pasarle el documentModel
                    localName = repositoryName + "/" + baseObjectDoc.getId();
                    Node baseObject = new QNameResourceImpl(
                            RelationConstants.DOCUMENT_NAMESPACE, localName);

                    eloraDocumentRelationManager.addRelation(documentManager,
                            baseSubjectDoc, baseObject,
                            nodeData.getPredicateUri(), isInverse, false,
                            nodeData.getComment(), nodeData.getQuantity(),
                            nodeData.getOrdering(),
                            nodeData.getDirectorOrdering(),
                            nodeData.getViewerOrdering(),
                            nodeData.getInverseViewerOrdering(),
                            nodeData.getIsManual(), nodeData.getDesignators(),
                            nodeData.getIsExcludedFromMapping());
                } else {
                    throw new EloraException(messages.get(
                            "label.archived.relations.not.created"));
                }
            }

            // TODO logak

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
            DocumentModel subjectDoc, boolean isAnarchic, boolean isInverse)
            throws EloraException {

        if (isAnarchic) {
            removeAnarchicRelation(nodeData, subjectDoc, isInverse);
        } else {
            removeNormalRelation(nodeData, subjectDoc, isInverse);
        }
    }

    private void removeNormalRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc, boolean isInverse) throws EloraException {
        String logInitMsg = "[removeNormalRelation] ["
                + documentManager.getPrincipal().getName() + "] ";

        DocumentModel objectDoc;
        if (!isInverse) {
            objectDoc = RelationHelper.getDocumentModel(
                    nodeData.getStmt().getObject(), documentManager);
        } else {
            objectDoc = subjectDoc;
            subjectDoc = RelationHelper.getDocumentModel(
                    nodeData.getStmt().getSubject(), documentManager);
        }

        eloraDocumentRelationManager.softDeleteRelation(documentManager,
                subjectDoc, nodeData.getPredicateUri(), objectDoc);

        log.info(logInitMsg + "Removed relation between subject |"
                + subjectDoc.getId() + "| and object |" + objectDoc.getId()
                + "| with predicate URI |" + nodeData.getPredicateUri() + "|.");
    }

    private void removeAnarchicRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc, boolean isInverse) throws EloraException {

        String logInitMsg = "[removeAnarchicRelation] ["
                + documentManager.getPrincipal().getName() + "] ";

        // If object isn't a WC, get the WC
        DocumentModel objectDoc = nodeData.getData();
        if (objectDoc.isImmutable()) {
            objectDoc = documentManager.getWorkingCopy(objectDoc.getRef());
        }

        // Resource predicateResource = new ResourceImpl(
        // nodeData.getPredicateUri());
        // Process removed relation
        if (!isInverse) {
            // RelationHelper.removeRelation(subjectDoc, predicateResource,
            // objectDoc);
            eloraDocumentRelationManager.softDeleteRelation(documentManager,
                    subjectDoc, nodeData.getPredicateUri(), objectDoc);

            log.info(logInitMsg + "Removed anarchic relation between subject |"
                    + subjectDoc.getId() + "| and object |" + objectDoc.getId()
                    + "| with predicate URI |" + nodeData.getPredicateUri()
                    + "|.");

        } else {
            // Check if parent is locked by someone else
            if (EloraDocumentHelper.isNotLockedOrLockedByMe(objectDoc)) {
                eloraDocumentRelationManager.softDeleteRelation(documentManager,
                        objectDoc, nodeData.getPredicateUri(), subjectDoc);
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                        "eloraplm.message.error.relations.edition.locked.by.another"),
                        objectDoc.getPropertyValue(
                                EloraMetadataConstants.ELORA_ELO_REFERENCE));
            }
        }

        // Remove relation from working copy based versions
        DocumentModel baseSubjectDoc = EloraDocumentHelper.getLatestVersion(
                subjectDoc);
        DocumentModel baseObjectDoc = EloraDocumentHelper.getLatestVersion(
                objectDoc);
        if (baseSubjectDoc != null && baseObjectDoc != null) {
            if (!isInverse) {
                eloraDocumentRelationManager.softDeleteRelation(documentManager,
                        baseSubjectDoc, nodeData.getPredicateUri(),
                        baseObjectDoc);
                log.info(logInitMsg
                        + "Removed anarchic relation between subject |"
                        + baseSubjectDoc.getId() + "| and object |"
                        + baseObjectDoc.getId() + "| with predicate URI |"
                        + nodeData.getPredicateUri() + "|.");

            } else {
                eloraDocumentRelationManager.softDeleteRelation(documentManager,
                        baseObjectDoc, nodeData.getPredicateUri(),
                        baseSubjectDoc);

                log.info(logInitMsg
                        + "Removed anarchic relation between subject |"
                        + baseSubjectDoc.getId() + "| and object |"
                        + baseObjectDoc.getId() + "| with predicate URI |"
                        + nodeData.getPredicateUri() + "| (inverse).");
            }
        }

    }

    private void updateRelation(RelationNodeData nodeData, DocumentModel doc,
            boolean isAnarchic, boolean isInverse) throws EloraException {
        try {
            if (isAnarchic) {
                updateAnarchicRelation(nodeData, doc, isInverse);
            } else {
                updateNormalRelation(nodeData, doc);
            }
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

    private void updateNormalRelation(RelationNodeData nodeData,
            DocumentModel subjectDoc) throws EloraException {

        // TODO Ezin da hau beste moduren baten atara?
        DocumentModelList versionsOfRelatedObject = EloraRelationHelper.getAllVersionsOfRelatedObject(
                subjectDoc, nodeData.getData(), nodeData.getPredicateUri(),
                documentManager);

        if (versionsOfRelatedObject.isEmpty()) {
            eloraDocumentRelationManager.addRelation(documentManager,
                    subjectDoc, nodeData.getData(), nodeData.getPredicateUri(),
                    false, false, nodeData.getComment(), nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());

        } else {

            if (versionsOfRelatedObject.size() > 1) {
                throw new EloraException(
                        "More than one version of the object is related to the subject with the same predicate.");
            }

            DocumentModel objectDoc = versionsOfRelatedObject.get(0);

            eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    documentManager, subjectDoc, nodeData.getPredicateUri(),
                    objectDoc, nodeData.getData(), nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());
        }
    }

    private void updateAnarchicRelation(RelationNodeData nodeData,
            DocumentModel currentWcDoc, boolean isInverse)
            throws EloraException {

        // For the moment, we consider that we only change simple relation
        // properties (ordering, ...) in anarchic relations.

        DocumentModel currentBaseDoc = EloraDocumentHelper.getBaseVersion(
                currentWcDoc);
        DocumentModel relatedDoc = nodeData.getData();

        DocumentModel relatedWcDoc = documentManager.getWorkingCopy(
                nodeData.getData().getRef());

        // Current base - AV relations
        if (!isInverse) {
            eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    documentManager, currentBaseDoc, nodeData.getPredicateUri(),
                    relatedDoc, relatedDoc, nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());
        } else {
            eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    documentManager, relatedDoc, nodeData.getPredicateUri(),
                    currentBaseDoc, currentBaseDoc, nodeData.getQuantity(),
                    nodeData.getOrdering(), nodeData.getDirectorOrdering(),
                    nodeData.getViewerOrdering(),
                    nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
                    nodeData.getDesignators(),
                    nodeData.getIsExcludedFromMapping());
        }

        // Current WC - Related WC
        DocumentModel relatedBaseDoc = EloraDocumentHelper.getBaseVersion(
                relatedWcDoc);
        if (relatedBaseDoc != null) {
            if (relatedBaseDoc.getId().equals(relatedDoc.getId())) {

                if (!isInverse) {
                    eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                            documentManager, currentWcDoc,
                            nodeData.getPredicateUri(), relatedWcDoc,
                            relatedWcDoc, nodeData.getQuantity(),
                            nodeData.getOrdering(),
                            nodeData.getDirectorOrdering(),
                            nodeData.getViewerOrdering(),
                            nodeData.getInverseViewerOrdering(),
                            nodeData.getIsManual(), nodeData.getDesignators(),
                            nodeData.getIsExcludedFromMapping());
                } else {
                    eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                            documentManager, relatedWcDoc,
                            nodeData.getPredicateUri(), currentWcDoc,
                            currentWcDoc, nodeData.getQuantity(),
                            nodeData.getOrdering(),
                            nodeData.getDirectorOrdering(),
                            nodeData.getViewerOrdering(),
                            nodeData.getInverseViewerOrdering(),
                            nodeData.getIsManual(), nodeData.getDesignators(),
                            nodeData.getIsExcludedFromMapping());
                }

            }
        }

    }

    protected void addDirectRelations(DocumentModel currentDoc,
            DocumentModel objectDoc) {
        String logInitMsg = "[addDirectRelations] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding direct relations of |"
                + objectDoc.getId() + "| to tree of document |"
                + currentDoc.getId() + "|.");

        if (objectDoc.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
            List<Statement> stmts = RelationHelper.getStatements(
                    EloraRelationConstants.ELORA_GRAPH_NAME, objectDoc,
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));
            for (Statement stmt : stmts) {
                NodeInfo relatedObjectInfo = new NodeInfoImpl(stmt.getObject(),
                        RelationHelper.getDocumentModel(stmt.getObject(),
                                documentManager),
                        true);
                // If document is not visible we don't relate it
                if (relatedObjectInfo.isDocumentVisible()) {
                    String nextNodeId = Integer.toString(
                            getRoot().getChildCount() + 1);

                    DocumentModel relatedObjectDoc = relatedObjectInfo.getDocumentModel();
                    DocumentModel relatedObjectWcDoc = relatedObjectDoc;
                    if (relatedObjectDoc.isImmutable()) {
                        relatedObjectWcDoc = documentManager.getSourceDocument(
                                relatedObjectDoc.getRef());
                    }

                    String type = relatedObjectDoc.getType();
                    Integer relatedDirectorOrdering = type.equals(
                            EloraDoctypeConstants.CAD_ASSEMBLY)
                            || type.equals(EloraDoctypeConstants.CAD_PART) ? 1
                                    : null;
                    Integer relatedViewerOrdering = type.equals(
                            EloraDoctypeConstants.CAD_DRAWING) ? 1 : null;

                    BaseRelationNodeData newNodeData = new BaseRelationNodeData(
                            nextNodeId, 1, relatedObjectDoc.getId(),
                            relatedObjectDoc, relatedObjectWcDoc, null,
                            predicateUri, "1", comment, null,
                            relatedDirectorOrdering, relatedViewerOrdering,
                            false, false, true, false);

                    TreeNode node = new DefaultTreeNode(newNodeData, getRoot());
                    // Set node's initial expanded state
                    node.setExpanded(false);

                    log.trace(logInitMsg + "Added direct relation |"
                            + relatedObjectDoc.getId() + "|.");
                }
            }

        } else {
            List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                    objectDoc,
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));
            for (Statement stmt : stmts) {
                NodeInfo relatedSubjectInfo = new NodeInfoImpl(
                        stmt.getSubject(), RelationHelper.getDocumentModel(
                                stmt.getSubject(), documentManager),
                        true);
                // If document is not visible we don't relate it
                if (relatedSubjectInfo.isDocumentVisible()) {

                    String nextNodeId = Integer.toString(
                            getRoot().getChildCount() + 1);

                    DocumentModel relatedSubjectDoc = relatedSubjectInfo.getDocumentModel();
                    DocumentModel relatedSubjectWcDoc = relatedSubjectDoc;
                    if (relatedSubjectDoc.isImmutable()) {
                        relatedSubjectWcDoc = documentManager.getSourceDocument(
                                relatedSubjectDoc.getRef());
                    }

                    Integer relatedDirectorOrdering = relatedSubjectDoc.getType().equals(
                            EloraDoctypeConstants.CAD_ASSEMBLY) ? 1 : null;
                    Integer relatedViewerOrdering = relatedSubjectDoc.getType().equals(
                            EloraDoctypeConstants.CAD_DRAWING) ? 1 : null;

                    BaseRelationNodeData newNodeData = new BaseRelationNodeData(
                            nextNodeId, 1, relatedSubjectDoc.getId(),
                            relatedSubjectDoc, relatedSubjectWcDoc, null,
                            predicateUri, "1", comment, null,
                            relatedDirectorOrdering, relatedViewerOrdering,
                            false, false, true, false);

                    TreeNode node = new DefaultTreeNode(newNodeData, getRoot());
                    // Set node's initial expanded state
                    node.setExpanded(false);

                    log.trace(logInitMsg + "Added direct relation |"
                            + relatedSubjectDoc.getId() + "|.");
                }
            }
        }
    }

    public void refreshVersionList(RelationNodeData nodeData) {

        DocumentModel doc = nodeData.getData();
        DocumentModel wcDoc = nodeData.getWcDoc();

        List<SelectItem> versionList = getVersionList(doc, wcDoc);
        nodeData.setVersionList(versionList);
    }

    public void refreshObjectDocumentVersionList(DocumentModel doc) {
        String logInitMsg = "[refreshObjectDocumentVersionList] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {

            DocumentModel wcDoc = documentManager.getWorkingCopy(doc.getRef());

            List<SelectItem> versionList = getVersionList(doc, wcDoc);
            setObjectDocumentVersionList(versionList);

            // We select the WC by default, so that it works when list is hidden too
            setObjectDocument(wcDoc);
        } catch (Exception e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.label.error.relation.getVersions"));
            log.error(logInitMsg + "Uncontrolled exception: "
                    + e.getClass().getName() + ". " + e.getMessage(), e);
        }
    }

    private List<SelectItem> getVersionList(DocumentModel doc, DocumentModel wcDoc) {
        List<SelectItem> versionList = new ArrayList<SelectItem>();

        for (DocumentModel version : documentManager.getVersions(
                doc.getRef())) {
            String versionLabel = version.getVersionLabel();
            versionList.add(new SelectItem(version, versionLabel));
        }

        // Add WC
        String wcVersionLabel = wcDoc.getVersionLabel() + " (WC)";
        versionList.add(new SelectItem(wcDoc, wcVersionLabel));

        // Reverse order
        Collections.reverse(versionList);

        return versionList;
    }

    public void refreshNode(TreeTable table, TreeNode node) {
        refreshNode(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    private void refreshNode(TreeNode node) {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";

        RelationNodeData nodeData = (RelationNodeData) node.getData();
        String oldDocId = nodeData.getDocId();
        if (!nodeData.getData().getId().equals(oldDocId)) {
            DocumentModel newDoc = nodeData.getData();
            nodeData.setDocId(newDoc.getId());
            node.getChildren().clear();

            log.trace(logInitMsg + "Refreshed node from |" + oldDocId + "| to |"
                    + newDoc.getId() + "|.");
        }
        nodeData.setIsNew(false);
        nodeData.setIsModified(true);

        setIsDirty(true);
    }

    protected void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentWcUid = null;
        objectDocument = null;
        objectDocumentTitle = null;
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

    @Override
    @DocumentContextInvalidation
    public DocumentModel onContextChange(DocumentModel doc) {
        String logInitMsg = "[onContextChange] ["
                + documentManager.getPrincipal().getName() + "] ";

        doc = super.onContextChange(doc);

        boolean invalidate = false;
        Date newLastModified = null;
        if (doc != null) {
            GregorianCalendar lastModifiedGc = (GregorianCalendar) doc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_MODIFIED);
            if (lastModifiedGc != null) {
                // Taken from Nuxeo code:
                // -------------------------------------------------
                // remove milliseconds as they are not stored in some
                // databases, which could make the comparison fail just
                // after a document creation (see NXP-8783)
                // -------------------------------------------------
                lastModifiedGc.set(Calendar.MILLISECOND, 0);
                newLastModified = lastModifiedGc.getTime();
            }
        }

        if (currentLastModified == null || newLastModified == null) {
            if (!(currentLastModified == null && newLastModified == null)) {
                invalidate = true;
            }
        } else {
            if (currentLastModified.compareTo(newLastModified) != 0) {
                invalidate = true;
            }
        }

        if (invalidate) {
            currentLastModified = newLastModified;
            setCurrentDocument(doc);
            resetBeanCache(doc);
            log.trace(logInitMsg
                    + "Document invalidated: current and new have different modification date. Current: |"
                    + currentLastModified + "| New: |" + newLastModified + "|");
        }

        return doc;
    }

    private boolean areRelationsEditable(DocumentModel doc,
            boolean isAnarchic) {
        if (
        // If it is anarchic, we only need to not be locked by others
        (isAnarchic && EloraDocumentHelper.isNotLockedOrLockedByMe(doc))
                // If not anarchic, document has to be editable
                || EloraDocumentHelper.isEditable(doc)) {
            return true;
        }

        return false;
    }

    /**
     * @return the objectDocumentVersionList
     */
    public List<SelectItem> getObjectDocumentVersionList() {
        return objectDocumentVersionList;
    }

    /**
     * @param objectDocumentVersionList the objectDocumentVersionList to set
     */
    public void setObjectDocumentVersionList(
            List<SelectItem> objectDocumentVersionList) {
        this.objectDocumentVersionList = objectDocumentVersionList;
    }

    /**
     * @return the objectDocument
     */
    public DocumentModel getObjectDocument() {
        return objectDocument;
    }

    /**
     * @param objectDocument the objectDocument to set
     */
    public void setObjectDocument(DocumentModel objectDocument) {
        this.objectDocument = objectDocument;
    }
}
