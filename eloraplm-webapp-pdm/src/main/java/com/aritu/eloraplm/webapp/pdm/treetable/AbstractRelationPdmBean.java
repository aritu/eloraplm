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
package com.aritu.eloraplm.webapp.pdm.treetable;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.relations.treetable.RelationNodeData;

/**
 * @author aritu
 *
 */
public abstract class AbstractRelationPdmBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            AbstractRelationPdmBean.class);

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    protected static final String CHECKOUT_SWITCH_CHILDREN_OPTION_AS_STORED = "AsStored";

    protected static final String CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_VERSIONS = "LatestVersions";

    protected static final String CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_RELEASED = "LatestReleased";

    protected static final String CHECKOUT_SWITCH_CHILDREN_OPTION_WORKING_COPIES = "WorkingCopies";

    protected String switchChildrenOption;

    public AbstractRelationPdmBean() {
    }

    public String getSwitchChildrenOption() {
        return switchChildrenOption;
    }

    public void setSwitchChildrenOption(String switchChildrenOption) {
        this.switchChildrenOption = switchChildrenOption;
    }

    public void switchChildren() {
        String logInitMsg = "[switchChildren] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        try {

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            TreeNode root = getTreeBeanRoot();

            DocumentModel subjectWcDoc = navigationContext.getCurrentDocument();
            if (subjectWcDoc.isProxy()) {
                subjectWcDoc = documentManager.getSourceDocument(
                        subjectWcDoc.getRef());
            }

            for (TreeNode treeNode : root.getChildren()) {
                RelationNodeData nodeData = (RelationNodeData) treeNode.getData();
                DocumentModel currentObjectDoc = nodeData.getData();
                DocumentModel currentObjectWcDoc = null;
                if (currentObjectDoc.isImmutable()) {
                    currentObjectWcDoc = documentManager.getWorkingCopy(
                            currentObjectDoc.getRef());
                } else {
                    currentObjectWcDoc = currentObjectDoc;
                }

                DocumentModel switchedObjectDoc = getSwitchedObjectDoc(
                        subjectWcDoc, currentObjectDoc, currentObjectWcDoc,
                        nodeData.getPredicateUri(), switchChildrenOption);

                switchRelation(subjectWcDoc, nodeData.getPredicateUri(),
                        currentObjectDoc, switchedObjectDoc, nodeData);

            }

            EloraDocumentHelper.updateContributorAndModified(subjectWcDoc,
                    true);

            setSwitchChildrenOption(null);
            createTreeBeanRoot();

        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } catch (Exception e) {
            log.error(
                    logInitMsg + "Uncontrolled exception: "
                            + e.getClass().getName() + ". " + e.getMessage(),
                    e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get(e.getMessage()));
            TransactionHelper.setTransactionRollbackOnly();
            navigationContext.invalidateCurrentDocument();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected abstract TreeNode getTreeBeanRoot();

    protected abstract void createTreeBeanRoot();

    protected DocumentModel getSwitchedObjectDoc(DocumentModel subjectWcDoc,
            DocumentModel currentObjectDoc, DocumentModel currentObjectWcDoc,
            String predicateUri, String switchChildrenOption)
            throws EloraException {

        switch (switchChildrenOption) {
        case CHECKOUT_SWITCH_CHILDREN_OPTION_WORKING_COPIES:
            return currentObjectWcDoc;
        case CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_RELEASED:
            return EloraDocumentHelper.getLatestReleasedVersionOrLatestVersion(
                    currentObjectDoc);
        case CHECKOUT_SWITCH_CHILDREN_OPTION_LATEST_VERSIONS:
            // To avoid crashing when the document has no AVs
            if (documentManager.getVersions(
                    currentObjectWcDoc.getRef()).isEmpty()) {
                return currentObjectWcDoc;
            } else {
                return documentManager.getDocument(
                        EloraDocumentHelper.getBaseVersion(
                                currentObjectWcDoc).getRef());
            }

        case CHECKOUT_SWITCH_CHILDREN_OPTION_AS_STORED:
            return getObjectOfBaseSubject(subjectWcDoc, currentObjectDoc,
                    predicateUri);

        default:
            throw new EloraException("Incorrect switch children option.");
        }
    }

    protected DocumentModel getObjectOfBaseSubject(DocumentModel subjectWcDoc,
            DocumentModel currentObjectDoc, String predicateUri)
            throws EloraException {

        DocumentModel subjectBaseDoc = EloraDocumentHelper.getBaseVersion(
                subjectWcDoc);

        DocumentModelList relatedObjects = EloraRelationHelper.getAllVersionsOfRelatedObject(
                subjectBaseDoc, currentObjectDoc, predicateUri,
                documentManager);

        if (relatedObjects.size() > 1) {
            throw new EloraException(
                    "Subject with the same relation to different object versions");
        }

        if (relatedObjects.size() > 0) {
            return relatedObjects.get(0);
        }

        return null;
    }

    protected void switchRelation(DocumentModel subjectWcDoc,
            String predicateUri, DocumentModel currentObjectDoc,
            DocumentModel switchedObjectDoc, RelationNodeData nodeData)
            throws EloraException {

        String logInitMsg = "[switchRelation] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "Switching relation: subject uid |"
                + subjectWcDoc.getId() + "| predicate |" + predicateUri
                + "| object uid |" + currentObjectDoc.getId() + "|.");

        if (EloraRelationHelper.existsRelation(subjectWcDoc, currentObjectDoc,
                predicateUri, documentManager)) {

            log.trace(logInitMsg + "Relation exists.");

            if (nodeData.getIsRemoved()) {
                log.trace(logInitMsg + "Relation is marked to remove.");
                // RelationHelper.removeRelation(subjectWcDoc,
                // new ResourceImpl(predicateUri), currentObjectDoc);
                eloraDocumentRelationManager.softDeleteRelation(documentManager,
                        subjectWcDoc, predicateUri, currentObjectDoc);
            } else if (switchedObjectDoc != null
                    && currentObjectDoc.getId() != switchedObjectDoc.getId()) {
                RelationHelper.removeRelation(subjectWcDoc,
                        new ResourceImpl(predicateUri), currentObjectDoc);
                log.trace(logInitMsg + "Relation removed.");
                createRelation(subjectWcDoc, predicateUri, switchedObjectDoc,
                        nodeData);
                log.trace(
                        logInitMsg + "New relation created to switched object |"
                                + switchedObjectDoc.getId() + "|.");
            }
        } else {

            log.trace(logInitMsg + "Relation does not exist.");

            if (switchedObjectDoc != null) {
                createRelation(subjectWcDoc, predicateUri, switchedObjectDoc,
                        nodeData);
                log.trace(logInitMsg + "Relation created to switched object |"
                        + switchedObjectDoc.getId() + "|.");
            } else {
                createRelation(subjectWcDoc, predicateUri, currentObjectDoc,
                        nodeData);
                log.trace(logInitMsg + "Relation created to object |"
                        + currentObjectDoc.getId() + "|.");
            }
        }
    }

    protected void createRelation(DocumentModel subjectDoc, String predicateUri,
            DocumentModel objectDoc, RelationNodeData nodeData) {

        eloraDocumentRelationManager.addRelation(documentManager, subjectDoc,
                objectDoc, predicateUri, nodeData.getComment(),
                nodeData.getQuantity(), nodeData.getOrdering(),
                nodeData.getDirectorOrdering(), nodeData.getViewerOrdering());
    }

}