package com.aritu.eloraplm.relations.treetable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.config.util.EloraConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.treetable.CoreTreeBean;

public class EditableRelationTreeBean extends CoreTreeBean implements
        Serializable {
    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected transient WebActions webActions;

    // Add Relation form properties

    private String predicateUri;

    private String objectDocumentUid;

    private String objectDocumentTitle;

    private String comment;

    private int quantity = 1;

    private int ordering = 0;

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    public String getObjectDocumentUid() {
        return objectDocumentUid;
    }

    public void setObjectDocumentUid(String objectDocumentUid) {
        this.objectDocumentUid = objectDocumentUid;
    }

    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    public EditableRelationTreeBean() {
    }

    public Map<String, String> getPredicateList() {

        // TODO Hau txukundu: etiketena modu hobe baten? Switcha konfigurazioren
        // baten egon behar da??

        Map<String, String> predicateList = new HashMap<String, String>();

        DocumentModel currentDoc = getCurrentDocument();

        // Get relations configuration
        try {
            DirectoryService dirService = Framework.getService(DirectoryService.class);
            Map<String, String> relationsMap = new HashMap<String, String>();

            EloraConfigTable cadRelationsConfig = EloraConfigHelper.getCadRelationsConfig(
                    false, false);
            try (Session session = dirService.open("elora_cad_predicates")) {

                for (String key : cadRelationsConfig.getRows().keySet()) {
                    DocumentModel entry = session.getEntry(key);
                    relationsMap.put(key,
                            entry.getPropertyValue("label").toString());
                }
            }

            EloraConfigTable bomRelationsConfig = EloraConfigHelper.getBomRelationsConfig();
            try (Session session = dirService.open("elora_bom_predicates")) {

                for (String key : bomRelationsConfig.getRows().keySet()) {
                    DocumentModel entry = session.getEntry(key);
                    relationsMap.put(key,
                            entry.getPropertyValue("label").toString());
                }
            }

            switch (currentDoc.getType()) {
            case EloraDoctypeConstants.CAD_ASSEMBLY:
                predicateList.put(
                        EloraRelationConstants.CAD_COMPOSED_OF,
                        relationsMap.get(EloraRelationConstants.CAD_COMPOSED_OF));
                predicateList.put(
                        EloraRelationConstants.CAD_HAS_SUPPRESSED,
                        relationsMap.get(EloraRelationConstants.CAD_HAS_SUPPRESSED));
                predicateList.put(
                        EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                        relationsMap.get(EloraRelationConstants.CAD_IN_CONTEXT_WITH));
                predicateList.put(
                        EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                        relationsMap.get(EloraRelationConstants.CAD_HAS_DESIGN_TABLE));

                break;

            case EloraDoctypeConstants.CAD_PART:
                predicateList.put(EloraRelationConstants.CAD_BASED_ON,
                        relationsMap.get(EloraRelationConstants.CAD_BASED_ON));
                predicateList.put(
                        EloraRelationConstants.CAD_IN_CONTEXT_WITH,
                        relationsMap.get(EloraRelationConstants.CAD_IN_CONTEXT_WITH));
                predicateList.put(
                        EloraRelationConstants.CAD_HAS_DESIGN_TABLE,
                        relationsMap.get(EloraRelationConstants.CAD_HAS_DESIGN_TABLE));

                break;

            case EloraDoctypeConstants.CAD_DRAWING:
                predicateList.put(EloraRelationConstants.CAD_DRAWING_OF,
                        relationsMap.get(EloraRelationConstants.CAD_DRAWING_OF));

                break;

            case EloraDoctypeConstants.BOM_PART:
                predicateList.put(
                        EloraRelationConstants.BOM_COMPOSED_OF,
                        relationsMap.get(EloraRelationConstants.BOM_COMPOSED_OF));
                predicateList.put(
                        EloraRelationConstants.BOM_HAS_CAD_DOCUMENT,
                        relationsMap.get(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));
                predicateList.put(
                        EloraRelationConstants.BOM_HAS_DOCUMENT,
                        relationsMap.get(EloraRelationConstants.BOM_HAS_DOCUMENT));

                break;

            // TODO Hau aldatu egin beharko da??? BOM_HAS_BOM, BOM_HAS_LIST,...
            // case EloraDoctypeConstants.BOM_CUSTOMER_PRODUCT:
            // predicateList.put(
            // EloraRelationConstants.BOM_CUSTOMER_PRODUCT_OF,
            // relationsMap.get(EloraRelationConstants.BOM_CUSTOMER_PRODUCT_OF));
            //
            // break;
            //
            // case EloraDoctypeConstants.BOM_MANUFACTURER_PART:
            // predicateList.put(
            // EloraRelationConstants.BOM_MANUFACTURER_PART_OF,
            // relationsMap.get(EloraRelationConstants.BOM_MANUFACTURER_PART_OF));
            //
            // break;
            }

        } catch (EloraException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return predicateList;
    }

    public void addRelationNode(DocumentModel currentDoc) {
        // TODO Txekeo honeik ondo dauz???
        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {

                // TODO txekeo gehixau??
                DocumentModel objectDoc = documentManager.getDocument(new IdRef(
                        objectDocumentUid));

                String nextNodeId = Integer.toString(getRoot().getChildCount() + 1);

                // TODO Aldatu
                boolean isSpecial = predicateUri.equals(EloraRelationConstants.CAD_DRAWING_OF) ? true
                        : false;

                DocumentModel wcDoc = null;
                if (objectDoc.isImmutable()) {
                    wcDoc = documentManager.getWorkingCopy(objectDoc.getRef());
                } else {
                    wcDoc = objectDoc;
                }

                // Add a new EditableRelationNode
                BaseRelationNodeData newNodeData = new BaseRelationNodeData(
                        nextNodeId, 1, objectDocumentUid, objectDoc, wcDoc,
                        null, predicateUri, quantity, comment, true, ordering,
                        isSpecial, true, false);

                TreeNode node = new DefaultTreeNode(newNodeData, getRoot());
                // Set nodes initial expanded state
                node.setExpanded(false);

                resetCreateFormValues();

                // When we reset tabs, the current tab/subtab is lost, so we
                // have to get it before, and reset it after
                String currentTabId = webActions.getCurrentTabId();
                String currentSubTabId = webActions.getCurrentSubTabId();
                webActions.resetTabList();
                webActions.setCurrentTabId(currentTabId);
                webActions.setCurrentSubTabId(currentSubTabId);

                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get("eloraplm.message.success.relation.added"));

            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        }

    }

    public void markRelationNodeAsRemoved(TreeNode node,
            DocumentModel currentDoc) {
        toggleRelationNodeAsRemoved(true, node, currentDoc);
    }

    public void unmarkRelationNodeAsRemoved(TreeNode node,
            DocumentModel currentDoc) {
        toggleRelationNodeAsRemoved(false, node, currentDoc);
    }

    private void toggleRelationNodeAsRemoved(boolean isRemoved, TreeNode node,
            DocumentModel currentDoc) {
        String message;

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }

        // TODO Txekeo honeik ondo dauz???
        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {

                BaseRelationNodeData nodeData = (BaseRelationNodeData) node.getData();
                // If it is a new node, remove it completely
                if (nodeData.getIsNew()) {
                    message = "eloraplm.message.success.relation.removed";
                    getRoot().getChildren().remove(node);
                } else {
                    message = isRemoved ? "eloraplm.message.success.relation.markedToRemove"
                            : "eloraplm.message.success.relation.unmarkedToRemove";
                    nodeData.setIsRemoved(isRemoved);
                    node.getChildren().clear();

                    // TODO Bere semeak ere markatu, edo desaktibatu expand
                    // aukera???
                    node.setExpanded(false);

                    // TODO unmark egiterakoan semeak berkalkulatu???? Edo
                    // expand lekuan ikono berezi bat, adierazteko edizioa egin
                    // denez semeak ez direla ondo ikusten??
                }

                // When we reset tabs, the current tab/subtab is lost, so we
                // have to get it before, and reset it after
                String currentTabId = webActions.getCurrentTabId();
                String currentSubTabId = webActions.getCurrentSubTabId();
                webActions.resetTabList();
                webActions.setCurrentTabId(currentTabId);
                webActions.setCurrentSubTabId(currentSubTabId);

                // TODO aldatu
                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get(message));

            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        }

    }

    public String save(DocumentModel currentDoc, DocumentModel subjectDoc) {
        // TODO Txekeo honeik ondo dauz???

        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        if (subjectDoc.isProxy()) {
            subjectDoc = documentManager.getWorkingCopy(subjectDoc.getRef());
        }

        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {

                List<TreeNode> firstLevelNodes = getRoot().getChildren();
                if (firstLevelNodes != null && !firstLevelNodes.isEmpty()) {
                    for (TreeNode childNode : firstLevelNodes) {

                        BaseRelationNodeData nodeData = (BaseRelationNodeData) childNode.getData();

                        // Update ordering
                        Statement nodeStmt = nodeData.getStmt();
                        if (nodeStmt != null) {
                            EloraStatementInfo stmtInfo = new EloraStatementInfoImpl(
                                    nodeStmt);
                            int stmtOrdering = stmtInfo.getOrdering();

                            if (stmtOrdering != nodeData.getOrdering()) {
                                // Remove the relation and create it again
                                removeRelation(nodeData, subjectDoc);
                                createRelation(nodeData, subjectDoc);
                            }
                        }

                        if (nodeData.getIsNew() == true) {
                            createRelation(nodeData, subjectDoc);

                        } else if (nodeData.getIsRemoved() == true) {
                            removeRelation(nodeData, subjectDoc);
                        }
                    }
                }

                // Set subject document as checked out; relations have
                // changed.
                EloraDocumentHelper.checkOutDocument(subjectDoc);

                createRoot();

                navigationContext.invalidateCurrentDocument();

                facesMessages.add(
                        StatusMessage.Severity.INFO,
                        messages.get("eloraplm.message.success.relations.saved"));

            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        }

        return null;

    }

    private void createRelation(BaseRelationNodeData nodeData,
            DocumentModel subjectDoc) {
        // Process new relation
        Node object = null;

        // TODO Hau ez da ondo egongo WC batetik AV batera
        // erlazioak sortu ahal badie... oingoz aukera hori
        // ez egon arren
        // If object isn't a WC, get the WC
        DocumentModel objectDoc = nodeData.getData();
        if (objectDoc.isImmutable()) {
            objectDoc = documentManager.getWorkingCopy(objectDoc.getRef());
        }
        String objectDocUid = objectDoc.getId();
        String repositoryName = navigationContext.getCurrentServerLocation().getName();
        String localName = repositoryName + "/" + objectDocUid;
        object = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                localName);
        try {
            eloraDocumentRelationManager.addRelation(documentManager,
                    subjectDoc, object, nodeData.getPredicateUri(), false,
                    false, nodeData.getComment(), nodeData.getQuantity(),
                    nodeData.getIsObjectWc(), nodeData.getOrdering());

            // TODO logak

        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }
    }

    private void removeRelation(BaseRelationNodeData nodeData,
            DocumentModel subjectDoc) {
        // TODO Hau ez da ondo egongo WC batetik AV batera
        // erlazioak sortu ahal badie... oingoz aukera hori
        // ez egon arren
        // If object isn't a WC, get the WC
        DocumentModel objectDoc = nodeData.getData();
        if (objectDoc.isImmutable()) {
            objectDoc = documentManager.getWorkingCopy(objectDoc.getRef());
        }

        // Process removed relation
        eloraDocumentRelationManager.deleteRelation(documentManager,
                subjectDoc, objectDoc, nodeData.getPredicateUri());
        // TODO logak
    }

    public void restoreRelations(DocumentModel currentDoc,
            DocumentModel subjectDoc) {
        try {
            if (currentDoc.isProxy()) {
                currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
            }
            if (subjectDoc.isProxy()) {
                subjectDoc = documentManager.getWorkingCopy(subjectDoc.getRef());
            }
            if (currentDoc.isLocked()) {
                if (currentDoc.getLockInfo().getOwner().equals(
                        documentManager.getPrincipal().getName())) {

                    VersionModel version = new VersionModelImpl();
                    version.setId(EloraDocumentHelper.getLatestVersion(
                            subjectDoc, documentManager).getId());

                    EloraRelationHelper.restoreRelations(subjectDoc, version,
                            eloraDocumentRelationManager, documentManager);

                    facesMessages.add(
                            StatusMessage.Severity.INFO,
                            messages.get("eloraplm.message.success.relations.restore"));

                    createRoot();
                } else {
                    facesMessages.add(
                            StatusMessage.Severity.ERROR,
                            messages.get("label.relation.documentLockedByOther"));
                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentNotLocked"));
            }

        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("eloraplm.message.error.relations.restore"));
        }
    }

    private void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentUid = null;
        objectDocumentTitle = null;
        comment = null;
        quantity = 1;
        ordering = 0;
    }
}