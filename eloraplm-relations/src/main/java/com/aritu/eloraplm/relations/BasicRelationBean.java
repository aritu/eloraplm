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
import java.util.List;
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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;

@Name("basicRelationBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BasicRelationBean extends EloraDocContextBoundActionBean implements
        Serializable {

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

    private int directorOrdering = 0;

    private boolean addDirectRelation = false;

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

    public int getDirectorOrdering() {
        return directorOrdering;
    }

    public void setDirectorOrdering(int directorOrdering) {
        this.directorOrdering = directorOrdering;
    }

    public boolean getAddDirectRelation() {
        return addDirectRelation;
    }

    public void setAddDirectRelation(boolean addDirectRelation) {
        this.addDirectRelation = addDirectRelation;
    }

    public BasicRelationBean() {
    }

    private String calculatePredicateUri(String predicate) {
        if (predicate == null) {
            return predicateUri;
        } else {
            switch (predicate) {
            case "BOM_HAS_CAD_DOCUMENT":
                predicateUri = EloraRelationConstants.BOM_HAS_CAD_DOCUMENT;
                break;
            case "BOM_HAS_DOCUMENT":
                predicateUri = EloraRelationConstants.BOM_HAS_DOCUMENT;
                break;
            default:
                break;
            }
            return predicateUri;
        }
    }

    public String addRelation(String predicate) {
        predicateUri = calculatePredicateUri(predicate);

        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        // Check that the subject is locked
        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {

                objectDocumentUid = objectDocumentUid.trim();
                DocumentModel object = documentManager.getDocument(new IdRef(
                        objectDocumentUid));
                try {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            currentDoc, object, predicateUri, false,
                            includeStatementsInEvents,
                            StringUtils.trim(comment), quantity, true, 0,
                            directorOrdering);

                    // Checkbox is selected
                    if (addDirectRelation) {
                        addDirectRelations(currentDoc, object);
                    }

                    facesMessages.add(StatusMessage.Severity.INFO,
                            messages.get("label.relation.created"));
                    resetCreateFormValues();

                    // Set subject document as checked out; relations have
                    // changed.
                    EloraDocumentHelper.checkOutDocument(currentDoc);
                } catch (RelationAlreadyExistsException e) {
                    facesMessages.add(StatusMessage.Severity.WARN,
                            messages.get("label.relation.already.exists"));

                }
            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        }
        eloraRelationActions.resetStatements();
        return null;
    }

    private void addDirectRelations(DocumentModel currentDoc,
            DocumentModel object) {
        if (object.getType().equals(EloraDoctypeConstants.CAD_DRAWING)) {
            List<Statement> stmts = RelationHelper.getStatements(object,
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));
            for (Statement stmt : stmts) {
                NodeInfo objectInfo = new NodeInfoImpl(stmt.getObject(),
                        RelationHelper.getDocumentModel(stmt.getObject(),
                                documentManager), true);
                // If document is not visible we don't relate it
                if (objectInfo.isDocumentVisible()) {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            currentDoc, stmt.getObject(), predicateUri, false,
                            includeStatementsInEvents,
                            StringUtils.trim(comment), quantity, true, 0,
                            directorOrdering);
                }
            }
        } else {
            List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                    object, new ResourceImpl(
                            EloraRelationConstants.CAD_DRAWING_OF));
            for (Statement stmt : stmts) {
                NodeInfo subjectInfo = new NodeInfoImpl(stmt.getSubject(),
                        RelationHelper.getDocumentModel(stmt.getSubject(),
                                documentManager), true);
                // If document is not visible we don't relate it
                if (subjectInfo.isDocumentVisible()) {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            currentDoc, stmt.getSubject(), predicateUri, false,
                            includeStatementsInEvents,
                            StringUtils.trim(comment), quantity, true, 0,
                            directorOrdering);
                }
            }
        }
    }

    private void resetCreateFormValues() {
        predicateUri = null;
        objectDocumentUid = null;
        objectDocumentTitle = null;
        comment = null;
        quantity = 1;
        ordering = 0;
        addDirectRelation = false;
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