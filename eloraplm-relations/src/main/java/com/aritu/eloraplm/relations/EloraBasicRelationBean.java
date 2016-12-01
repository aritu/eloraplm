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
package com.aritu.eloraplm.relations;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

@Name("eloraBasicRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class EloraBasicRelationBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static boolean includeStatementsInEvents = false;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient EloraRelationActionsBean eloraRelationActions;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected transient WebActions webActions;

    // Add Relation form properties

    private String predicateUri;

    // This value is set from a template
    private String paramPredicateUri;

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

    public String getParamPredicateUri() {
        return paramPredicateUri;
    }

    public void setParamPredicateUri(String paramPredicateUri) {
        this.paramPredicateUri = paramPredicateUri;
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

    public EloraBasicRelationBean() {
    }

    public String addRelation() {
        return addRelation(false);
    }

    public String addRelation(boolean inverse) {
        // Check that the subject is locked
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }

        // TODO: ¿No hay otra forma de sacar el node del object?
        Node object = null;
        objectDocumentUid = objectDocumentUid.trim();
        String repositoryName = navigationContext.getCurrentServerLocation().getName();
        String localName = repositoryName + "/" + objectDocumentUid;
        object = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                localName);
        try {
            eloraDocumentRelationManager.addRelation(documentManager,
                    currentDoc, object, predicateUri, inverse,
                    includeStatementsInEvents, StringUtils.trim(comment),
                    quantity, true, 0);
            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("label.relation.created"));
            resetCreateFormValues();
            EloraDocumentHelper.checkOutDocument(currentDoc);
            navigationContext.invalidateCurrentDocument();
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    messages.get("label.relation.already.exists"));
        }
        return null;
    }

    public String deleteStatement(StatementInfo stmtInfo) {
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isCheckedOut()) {
            eloraDocumentRelationManager.deleteRelation(documentManager,
                    stmtInfo.getStatement());
        } else {
            // Get working copy stmt data and delete
            DocumentModel subject = RelationHelper.getDocumentModel(
                    stmtInfo.getSubject(), documentManager);
            DocumentModel subjectWc = documentManager.getWorkingCopy(subject.getRef());

            DocumentModel object = RelationHelper.getDocumentModel(
                    stmtInfo.getObject(), documentManager);
            EloraStatementInfo eloraStmtInfo = new EloraStatementInfoImpl(
                    stmtInfo.getStatement());
            DocumentModel objectWc;
            if (eloraStmtInfo.getIsObjectWc()) {
                objectWc = documentManager.getWorkingCopy(object.getRef());
            } else {
                objectWc = object;
            }

            eloraDocumentRelationManager.deleteRelation(documentManager,
                    subjectWc, objectWc, stmtInfo.getPredicate().getUri());

            // Set subject document as checked out; relations have changed.
            EloraDocumentHelper.checkOutDocument(currentDoc);
        }

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("label.relation.deleted"));
        return null;
    }

    private void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentUid = null;
        objectDocumentTitle = null;
        comment = null;
        quantity = 1;
        ordering = 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.aritu.eloraplm.core.EloraDocContextBoundActionBean#resetBeanCache
     * (org.nuxeo.ecm.core.api.DocumentModel)
     */
    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        // TODO Auto-generated method stub
    }

    public void cambiaPredicate(String predicate) {
        predicateUri = predicate;
    }
}