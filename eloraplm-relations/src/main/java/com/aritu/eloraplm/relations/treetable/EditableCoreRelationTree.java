package com.aritu.eloraplm.relations.treetable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
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
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.relations.util.CommonRelationHelper;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.RelationNotAllowedException;
import com.aritu.eloraplm.treetable.EditableTree;
import com.aritu.eloraplm.treetable.NodeManager;
import com.aritu.eloraplm.webapp.util.EloraAjax;

public abstract class EditableCoreRelationTree<T extends CoreRelationNodeData>
        extends EditableTree<T> {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            EditableCoreRelationTree.class);

    @In(create = true)
    protected transient WebActions webActions;

    protected NodeManager nodeService;

    private Date currentLastModified;

    // <-- Relation form properties [

    private String predicateUri;

    private String objectDocumentWcUid;

    private String objectDocumentTitle;

    private String comment;

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

    // ] --->

    public EditableCoreRelationTree() {
    }

    /*
     * BEGIRATZEKO Hemendik aurrerakoa nola txukundu / ondo lotu
     * EditableTreeBeanekin
     * |
     * |
     * v
     */

    @Override
    protected TreeNode createAndAddTreeNode() {
        return addRelationNode(getCurrentDocument());
    }

    public TreeNode addRelationNode(DocumentModel currentDoc) {
        String logInitMsg = "[addRelationNode] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Adding node |" + objectDocumentWcUid
                + "| to tree of document |" + currentDoc.getId() + "|.");

        try {
            // TODO HAU pasau? nola nundik
            boolean isAnarchic = false;
            boolean isInverse = false;
            if (areRelationsEditable(currentDoc, isAnarchic)) {

                DocumentModel objectDoc = documentManager.getDocument(
                        new IdRef(objectDocumentWcUid));

                // TODO Inverse erlazioetan ez dabil?, eta Save egin gabe badago
                // beste nodo bat (benetako erlazioak sortu gabe), bere azpiko
                // dokumentuak ere ez ditu txekeatzen. Baina kasu normaletarako
                // balio du.
                // TODO Hau Relation motakoentzat da. Balio du denentzat? Helper
                // berrira
                if (!EloraRelationHelper.isCircularRelation(currentDoc,
                        objectDoc, isInverse, documentManager)) {
                    boolean objectHasVersion = documentManager.getLastDocumentVersion(
                            objectDoc.getRef()) != null;
                    boolean docHasVersion = documentManager.getLastDocumentVersion(
                            currentDoc.getRef()) != null;

                    if (!isAnarchic || (docHasVersion && objectHasVersion)) {
                        String nextNodeId = Integer.toString(
                                getRoot().getChildCount() + 1);

                        DocumentModel wcDoc = null;
                        if (objectDoc.isImmutable()) {
                            wcDoc = documentManager.getWorkingCopy(
                                    objectDoc.getRef());
                        } else {
                            wcDoc = objectDoc;
                        }

                        // Add a new EditableRelationNode
                        // Some values are not real because we will need and
                        // calculate them after we save the tree
                        TreeNode node = createNewTreeNode(nextNodeId, 1,
                                objectDocumentWcUid, objectDoc, wcDoc, null,
                                predicateUri, true, false);

                        log.trace(logInitMsg + "Node |" + objectDocumentWcUid
                                + "| added.");

                        // // When we reset tabs, the current tab/subtab is
                        // lost,
                        // // so we have to get it before, and reset it after
                        // String currentTabId = webActions.getCurrentTabId();
                        // String currentSubTabId =
                        // webActions.getCurrentSubTabId();
                        // webActions.resetTabList();
                        // webActions.setCurrentTabId(currentTabId);
                        // webActions.setCurrentSubTabId(currentSubTabId);

                        facesMessages.add(StatusMessage.Severity.INFO,
                                messages.get(
                                        "eloraplm.message.success.relation.added"));

                        return node;

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
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.addRelationNode"));
        }

        // resetCreateFormValues();

        return null;

    }

    // protected void executePostAddActions(DocumentModel currentDoc,
    // DocumentModel objectDoc) {
    //
    // }

    protected TreeNode createNewTreeNode(String id, int level, String docId,
            DocumentModel data, DocumentModel wcDoc, Statement stmt,
            String predicateUri, boolean isNew, boolean isRemoved) {
        // TODO
        return null;
    }

    // TODO isAnarchic ez balego, EditableTreeBeanen estandarra erabili....

    /*
     * ^
     * |
     * |
     */

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

        if (areRelationsEditable(currentDoc, isAnarchic)) {
            String message;

            @SuppressWarnings("unchecked")
            T nodeData = (T) node.getData();

            toggleNodeAsRemoved(isRemoved, node, currentDoc);

            // Treat messages

            if (nodeData.getIsNew()) {
                String docId = nodeData.getDocId();
                message = "eloraplm.message.success.relation.removed";
                log.trace(logInitMsg + "Node |" + docId
                        + "| removed as it was marked to remove and was new.");
            } else {
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

    /*
     * TODO BEGIRATU / TXUKUNDU
     * |
     * |
     * v
     *
     */

    // public String save(DocumentModel currentDoc, DocumentModel subjectDoc,
    // boolean isAnarchic) throws EloraException {
    // return save(currentDoc, subjectDoc, isAnarchic, false);
    // }

    @Override
    public String save(DocumentModel subjectDoc) throws EloraException {

        String logInitMsg = "[save] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Saving relation changes to document |"
                + getCurrentDocument().getId() + "|");
        if (getIsDirty()) {

            try {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

                DocumentModel currentDoc = getCurrentDocument();
                if (currentDoc.isProxy()) {
                    currentDoc = documentManager.getWorkingCopy(
                            currentDoc.getRef());
                }
                if (subjectDoc.isProxy()) {
                    subjectDoc = documentManager.getWorkingCopy(
                            subjectDoc.getRef());
                }

                // initialChecks()
                //
                // TODO: Hay que sacar estos chequeos de aqui. Si cambia algo
                // termina afectando a cosas que no deberia. Por ejemplo, al
                // pasar el parametro isAnarchic para el chequeo, me ha hecho
                // cambiar plantillas y clases que les debería dar igual este
                // cambio
                // TODO Begiratu hau. Checkoutakin aldatu egin da
                // if (areRelationsEditable(currentDoc, isAnarchic)) {
                //
                List<TreeNode> firstLevelNodes = getRoot().getChildren();
                if (firstLevelNodes != null && !firstLevelNodes.isEmpty()) {
                    for (TreeNode childNode : firstLevelNodes) {

                        T nodeData = getNodeData(childNode);

                        if (nodeData.getIsNew()) {
                            doCreate(nodeData, subjectDoc);
                        } else if (nodeData.getIsRemoved()) {
                            doRemove(nodeData, subjectDoc);
                        } else if (nodeData.getIsModified()) {
                            doUpdate(nodeData, subjectDoc);
                        }
                    }
                }

                // executePostSaveActions()

                createRoot();

                // } else if (!currentDoc.isLocked()) {
                // facesMessages.add(StatusMessage.Severity.ERROR,
                // messages.get("label.relation.documentNotLocked"));
                // } else {
                // facesMessages.add(StatusMessage.Severity.ERROR,
                // messages.get(
                // "label.relation.documentLockedByOther"));
                // }

                log.trace(logInitMsg + "Relation changes saved.");

                facesMessages.add(StatusMessage.Severity.INFO, messages.get(
                        "eloraplm.message.success.relations.saved"));

            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.relations.saved"));
                TransactionHelper.setTransactionRollbackOnly();
            } catch (Exception e) {
                log.error(logInitMsg + "Uncontrolled exception: "
                        + e.getClass().getName() + ". " + e.getMessage(), e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("eloraplm.message.error.relations.saved"));
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

    /*
     * ^
     * |
     * |
     *
     */

    protected abstract T getNodeData(TreeNode node);

    @Override
    protected void doCreate(T nodeData, DocumentModel subjectDoc)
            throws EloraException {
        String logInitMsg = "[doCreate] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            // TODO
            boolean isAnarchic = false;
            boolean isInverse = false;
            // TODO
            createRelation(nodeData, subjectDoc, isAnarchic, isInverse);
            log.trace(logInitMsg + "Added related child |" + subjectDoc.getId()
                    + "|.");
        } catch (RelationNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.label.error.relation.notAllowed"));
            throw new EloraException(
                    "Error creating relation: relation not allowed.", e);
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
            throw new EloraException(
                    "Error creating relation: relation already exists.", e);
        }
    }

    @Override
    protected void doRemove(T nodeData, DocumentModel subjectDoc)
            throws EloraException {
        String logInitMsg = "[doRemove] ["
                + documentManager.getPrincipal().getName() + "] ";
        // TODO
        boolean isAnarchic = false;
        boolean isInverse = false;
        // TODO
        removeRelation(nodeData, subjectDoc, isAnarchic, isInverse);
        log.trace(logInitMsg + "Removed related child |" + subjectDoc.getId()
                + "|.");
    }

    @Override
    protected void doUpdate(T nodeData, DocumentModel subjectDoc)
            throws EloraException {
        String logInitMsg = "[doUpdate] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            // TODO
            boolean isAnarchic = false;
            boolean isInverse = false;
            // TODO
            updateRelation(nodeData, subjectDoc, isAnarchic, isInverse);
            log.trace(logInitMsg + "Updated related child |"
                    + subjectDoc.getId() + "|.");
        } catch (RelationNotAllowedException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.label.error.relation.notAllowed"));
            throw new EloraException(
                    "Error creating relation: relation not allowed.", e);
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
            throw new EloraException(
                    "Error creating relation: relation already exists.", e);
        }
    }

    @Override
    protected void executePostSaveActions() {
        // TODO
        boolean isAnarchic = false;
        // TODO current???
        // if (!isAnarchic) {
        // EloraDocumentHelper.updateContributorAndModified(
        // currentDoc, true);
        // }
    }

    private void createRelation(T nodeData, DocumentModel subjectDoc,
            boolean isAnarchic, boolean isInverse)
            throws EloraException, RelationAlreadyExistsException {

        // TODO If it is Anarchic, it will always be the WC. Check?

        DocumentModel objectDoc = nodeData.getData();
        if (isInverse
                && !EloraDocumentHelper.isNotLockedOrLockedByMe(objectDoc)) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.relations.edition.locked.by.another"),
                    objectDoc.getPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE));
        }

        addRelation(subjectDoc, nodeData.getPredicateUri(), objectDoc, nodeData,
                isInverse);

        if (isAnarchic) {
            // Create relation among base documents
            DocumentModel baseSubjectDoc = EloraDocumentHelper.getLatestVersion(
                    subjectDoc);
            DocumentModel baseObjectDoc = EloraDocumentHelper.getLatestVersion(
                    objectDoc);

            if (baseSubjectDoc != null && baseObjectDoc != null) {

                addRelation(baseObjectDoc, nodeData.getPredicateUri(),
                        baseSubjectDoc, nodeData, isInverse);

            } else {
                throw new EloraException(
                        messages.get("label.archived.relations.not.created"));
            }
        }

    }

    protected abstract void addRelation(DocumentModel from, String predicate,
            DocumentModel to, T nodeData, boolean isInverse)
            throws RelationAlreadyExistsException, RelationNotAllowedException;

    private void removeRelation(T nodeData, DocumentModel subjectDoc,
            boolean isAnarchic, boolean isInverse) throws EloraException {

        if (isAnarchic) {
            removeAnarchicRelation(nodeData, subjectDoc, isInverse);
        } else {
            removeNormalRelation(nodeData, subjectDoc, isInverse);
        }
    }

    private void removeNormalRelation(T nodeData, DocumentModel subjectDoc,
            boolean isInverse) throws EloraException {
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

        removeRelation(subjectDoc, nodeData.getPredicateUri(), objectDoc);

        log.info(logInitMsg + "Removed relation between subject |"
                + subjectDoc.getId() + "| and object |" + objectDoc.getId()
                + "| with predicate URI |" + nodeData.getPredicateUri() + "|.");
    }

    protected abstract void removeRelation(DocumentModel subjectDoc,
            String predicateUri, DocumentModel objectDoc);

    private void removeAnarchicRelation(T nodeData, DocumentModel subjectDoc,
            boolean isInverse) throws EloraException {

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

            // TODO HAU ESTANDARTZEKO
            // eloraDocumentRelationManager.softDeleteRelation(documentManager,
            // subjectDoc, nodeData.getPredicateUri(), objectDoc);

            log.info(logInitMsg + "Removed anarchic relation between subject |"
                    + subjectDoc.getId() + "| and object |" + objectDoc.getId()
                    + "| with predicate URI |" + nodeData.getPredicateUri()
                    + "|.");

        } else {
            // Check if parent is locked by someone else
            if (EloraDocumentHelper.isNotLockedOrLockedByMe(objectDoc)) {
                // TODO HAU ESTANDARTZEKO
                // eloraDocumentRelationManager.softDeleteRelation(documentManager,
                // objectDoc, nodeData.getPredicateUri(), subjectDoc);
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
                // TODO HAU ESTANDARTZEKO
                // eloraDocumentRelationManager.softDeleteRelation(documentManager,
                // baseSubjectDoc, nodeData.getPredicateUri(),
                // baseObjectDoc);
                log.info(logInitMsg
                        + "Removed anarchic relation between subject |"
                        + baseSubjectDoc.getId() + "| and object |"
                        + baseObjectDoc.getId() + "| with predicate URI |"
                        + nodeData.getPredicateUri() + "|.");

            } else {
                // TODO HAU ESTANDARTZEKO
                // eloraDocumentRelationManager.softDeleteRelation(documentManager,
                // baseObjectDoc, nodeData.getPredicateUri(),
                // baseSubjectDoc);

                log.info(logInitMsg
                        + "Removed anarchic relation between subject |"
                        + baseSubjectDoc.getId() + "| and object |"
                        + baseObjectDoc.getId() + "| with predicate URI |"
                        + nodeData.getPredicateUri() + "| (inverse).");
            }
        }

    }

    private void updateRelation(T nodeData, DocumentModel doc,
            boolean isAnarchic, boolean isInverse)
            throws RelationAlreadyExistsException, EloraException {
        if (isAnarchic) {
            updateAnarchicRelation(nodeData, doc, isInverse);
        } else {
            updateNormalRelation(nodeData, doc);
        }

    }

    private void updateNormalRelation(T nodeData, DocumentModel subjectDoc)
            throws EloraException, RelationAlreadyExistsException {

        // TODO Ezin da hau beste moduren baten atara?
        DocumentModelList relatedObjectVersions = CommonRelationHelper.getAllRelatedVersionsOfObject(
                subjectDoc, nodeData.getData(), nodeData.getPredicateUri(),
                documentManager);

        if (relatedObjectVersions.isEmpty()) {

            // TODO Hau aldatu daiteke?
            boolean isInverse = false;

            addRelation(subjectDoc, nodeData.getPredicateUri(),
                    nodeData.getData(), nodeData, isInverse);

        } else {

            if (relatedObjectVersions.size() > 1) {
                throw new EloraException(
                        "More than one version of the object is related to the subject with the same predicate.");
            }

            DocumentModel objectDoc = relatedObjectVersions.get(0);

            updateRelation(subjectDoc, nodeData.getPredicateUri(), objectDoc,
                    nodeData);
        }
    }

    private void updateAnarchicRelation(T nodeData, DocumentModel currentWcDoc,
            boolean isInverse) throws EloraException {

        // For the moment, we consider that we only change simple relation
        // properties (ordering, ...) in anarchic relations.

        DocumentModel currentBaseDoc = EloraDocumentHelper.getBaseVersion(
                currentWcDoc);
        DocumentModel relatedDoc = nodeData.getData();

        DocumentModel relatedWcDoc = documentManager.getWorkingCopy(
                nodeData.getData().getRef());

        // Current base - AV relations
        if (!isInverse) {

            updateRelation(currentBaseDoc, nodeData.getPredicateUri(),
                    relatedDoc, nodeData);

            // TODO HAU ESTANDARTZEKO
            // eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
            // documentManager, currentBaseDoc, nodeData.getPredicateUri(),
            // relatedDoc, relatedDoc, nodeData.getQuantity(),
            // nodeData.getOrdering(), nodeData.getDirectorOrdering(),
            // nodeData.getViewerOrdering(),
            // nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
            // nodeData.getDesignators());
        } else {

            updateRelation(relatedDoc, nodeData.getPredicateUri(),
                    currentBaseDoc, nodeData);

            // TODO HAU ESTANDARTZEKO
            // eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
            // documentManager, relatedDoc, nodeData.getPredicateUri(),
            // currentBaseDoc, currentBaseDoc, nodeData.getQuantity(),
            // nodeData.getOrdering(), nodeData.getDirectorOrdering(),
            // nodeData.getViewerOrdering(),
            // nodeData.getInverseViewerOrdering(), nodeData.getIsManual(),
            // nodeData.getDesignators());
        }

        // Current WC - Related WC
        DocumentModel relatedBaseDoc = EloraDocumentHelper.getBaseVersion(
                relatedWcDoc);
        if (relatedBaseDoc != null) {
            if (relatedBaseDoc.getId().equals(relatedDoc.getId())) {

                if (!isInverse) {

                    updateRelation(currentWcDoc, nodeData.getPredicateUri(),
                            relatedWcDoc, nodeData);

                    // TODO HAU ESTANDARTZEKO
                    // eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    // documentManager, currentWcDoc,
                    // nodeData.getPredicateUri(), relatedWcDoc,
                    // relatedWcDoc, nodeData.getQuantity(),
                    // nodeData.getOrdering(),
                    // nodeData.getDirectorOrdering(),
                    // nodeData.getViewerOrdering(),
                    // nodeData.getInverseViewerOrdering(),
                    // nodeData.getIsManual(), nodeData.getDesignators());
                } else {

                    updateRelation(relatedWcDoc, nodeData.getPredicateUri(),
                            currentWcDoc, nodeData);

                    // TODO HAU ESTANDARTZEKO
                    // eloraDocumentRelationManager.updateObjectAndAllPropsInRelation(
                    // documentManager, relatedWcDoc,
                    // nodeData.getPredicateUri(), currentWcDoc,
                    // currentWcDoc, nodeData.getQuantity(),
                    // nodeData.getOrdering(),
                    // nodeData.getDirectorOrdering(),
                    // nodeData.getViewerOrdering(),
                    // nodeData.getInverseViewerOrdering(),
                    // nodeData.getIsManual(), nodeData.getDesignators());
                }

            }
        }

    }

    protected abstract void updateRelation(DocumentModel from, String predicate,
            DocumentModel to, T nodeData) throws EloraException;

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

                    CoreRelationNodeData newNodeData = new CoreRelationNodeDataImpl(
                            nextNodeId, 1, relatedObjectDoc.getId(),
                            relatedObjectDoc, relatedObjectWcDoc, null,
                            predicateUri, true, false);

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

    public void refreshVersionList(T nodeData) {
        Map<String, String> versionList = new LinkedHashMap<>();
        Map<String, String> reversedVersionList = new LinkedHashMap<>();

        DocumentModel doc = nodeData.getData();
        DocumentModel wcDoc = nodeData.getWcDoc();

        for (DocumentModel version : documentManager.getVersions(
                doc.getRef())) {
            String versionLabel = EloraDocumentHelper.getVersionWithExternalIfExists(version);
            versionList.put(version.getId(), versionLabel);
        }

        // Add WC
        String wcVersionLabel = EloraDocumentHelper.getVersionWithExternalIfExists(wcDoc);
        versionList.put(wcDoc.getId(), wcVersionLabel);

        // Reverse order
        List<String> keys = new ArrayList<String>(versionList.keySet());
        Collections.reverse(keys);
        for (String key : keys) {
            reversedVersionList.put(key, versionList.get(key));
        }

        nodeData.setVersionList(reversedVersionList);
    }

    @Override
    public void refreshNode(TreeTable table, TreeNode node) {
        refreshNode(node);
        EloraAjax.updateTreeTableRow(table, node.getRowKey());
    }

    @Override
    protected void refreshNode(TreeNode node) {
        String logInitMsg = "[refreshNode] ["
                + documentManager.getPrincipal().getName() + "] ";

        @SuppressWarnings("unchecked")
        T nodeData = (T) node.getData();
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

    @Override
    protected void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentWcUid = null;
        objectDocumentTitle = null;
        comment = null;
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
}
