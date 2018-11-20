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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.EloraDocContextBoundActionBean;
import com.aritu.eloraplm.core.relations.api.EloraDocumentRelationManager;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfo;
import com.aritu.eloraplm.core.relations.web.EloraStatementInfoImpl;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.relations.web.StatementInfoImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

@Name("eloraRelationActions")
@Scope(ScopeType.EVENT)
@AutomaticDocumentBasedInvalidation
public class EloraRelationActionsBean extends EloraDocContextBoundActionBean
        implements Serializable {

    private static final Log log = LogFactory.getLog(
            EloraRelationActionsBean.class);

    private static final long serialVersionUID = 1L;

    protected static boolean includeStatementsInEvents = false;

    // statements lists
    protected List<Statement> outgoingBomDocumentStatements;

    protected List<StatementInfo> outgoingBomDocumentStatementsInfo;

    protected List<Statement> outgoingBomCadDocumentStatements;

    protected List<StatementInfo> outgoingBomCadDocumentStatementsInfo;

    protected List<Statement> outgoingCadSpecialStatements;

    protected List<StatementInfo> outgoingCadSpecialStatementsInfo;

    protected List<Statement> incomingBomDocumentStatements;

    protected List<StatementInfo> incomingBomDocumentStatementsInfo;

    protected List<Statement> incomingCadSpecialStatements;

    protected List<StatementInfo> incomingCadSpecialStatementsInfo;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected EloraDocumentRelationManager eloraDocumentRelationManager;

    @In(create = true)
    protected RelationManager relationManager;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected String predicateUri;

    protected String objectDocumentUid;

    protected String objectDocumentTitle;

    protected String comment;

    protected String quantity = "1";

    protected Integer ordering;

    protected Integer directorOrdering;

    protected Integer viewerOrdering;

    protected Boolean showCreateForm = false;

    // popupDisplayed flag for preventing relation_search content view execution
    // until search button clicked
    protected Boolean popupDisplayed = false;

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

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    public Integer getDirectorOrdering() {
        return directorOrdering;
    }

    public void setDirectorOrdering(Integer directorOrdering) {
        this.directorOrdering = directorOrdering;
    }

    public Integer getViewerOrdering() {
        return viewerOrdering;
    }

    public void setViewerOrdering(Integer viewerOrdering) {
        this.viewerOrdering = viewerOrdering;
    }

    public Boolean getShowCreateForm() {
        return showCreateForm;
    }

    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    public Boolean getPopupDisplayed() {
        return popupDisplayed;
    }

    public void setPopupDisplayed(Boolean popupDisplayed) {
        this.popupDisplayed = popupDisplayed;
    }

    @Factory(value = "outgoingBomDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingBomDocStatementsInfo() {
        // if (outgoingBomDocumentStatementsInfo != null) {
        // return outgoingBomDocumentStatementsInfo;
        // }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<Resource> predicates = new ArrayList<>();
        predicates.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));

        outgoingBomDocumentStatements = new ArrayList<>();
        for (Resource predicate : predicates) {
            // We don't need EloraCoreGraph if we don't use quantity
            List<Statement> stmts = RelationHelper.getStatements(currentDoc,
                    predicate);
            outgoingBomDocumentStatements.addAll(stmts);
        }

        if (outgoingBomDocumentStatements.isEmpty()) {
            outgoingBomDocumentStatements = Collections.emptyList();
            outgoingBomDocumentStatementsInfo = Collections.emptyList();
        } else {
            outgoingBomDocumentStatementsInfo = getStatementsInfo(
                    outgoingBomDocumentStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingBomDocumentStatementsInfo, comp);
        }
        return outgoingBomDocumentStatementsInfo;
    }

    @Factory(value = "outgoingBomCadDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingBomCadDocStatementsInfo() {
        if (outgoingBomCadDocumentStatementsInfo != null) {
            return outgoingBomCadDocumentStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<Resource> predicates = new ArrayList<>();
        predicates.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));

        outgoingBomCadDocumentStatements = new ArrayList<>();
        for (Resource predicate : predicates) {
            // We don't need EloraCoreGraph if we don't use quantity
            List<Statement> stmts = RelationHelper.getStatements(currentDoc,
                    predicate);
            outgoingBomCadDocumentStatements.addAll(stmts);
        }

        if (outgoingBomCadDocumentStatements.isEmpty()) {
            outgoingBomCadDocumentStatements = Collections.emptyList();
            outgoingBomCadDocumentStatementsInfo = Collections.emptyList();
        } else {
            outgoingBomCadDocumentStatementsInfo = getStatementsInfo(
                    outgoingBomCadDocumentStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingBomCadDocumentStatementsInfo, comp);
        }
        return outgoingBomCadDocumentStatementsInfo;
    }

    @Factory(value = "outgoingCadSpecialRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingCadSpecialStatementsInfo() {
        if (outgoingCadSpecialStatementsInfo != null) {
            return outgoingCadSpecialStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<Resource> predicates = new ArrayList<>();
        predicates.add(new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));

        outgoingCadSpecialStatements = new ArrayList<>();
        for (Resource predicate : predicates) {
            // We don't need EloraCoreGraph if we don't use quantity
            List<Statement> stmts = RelationHelper.getStatements(currentDoc,
                    predicate);
            outgoingCadSpecialStatements.addAll(stmts);
        }

        if (outgoingCadSpecialStatements.isEmpty()) {
            outgoingCadSpecialStatements = Collections.emptyList();
            outgoingCadSpecialStatementsInfo = Collections.emptyList();
        } else {
            outgoingCadSpecialStatementsInfo = getStatementsInfo(
                    outgoingCadSpecialStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(outgoingCadSpecialStatementsInfo, comp);
        }
        return outgoingCadSpecialStatementsInfo;
    }

    @Factory(value = "incomingCadSpecialRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingCadSpecialStatementsInfo() {
        if (incomingCadSpecialStatementsInfo != null) {
            return incomingCadSpecialStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<Resource> predicates = new ArrayList<>();
        predicates.add(new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));

        getLatestStatements(currentDoc, predicates);

        if (incomingCadSpecialStatements.isEmpty()) {
            incomingCadSpecialStatements = Collections.emptyList();
            incomingCadSpecialStatementsInfo = Collections.emptyList();
        } else {
            incomingCadSpecialStatementsInfo = getStatementsInfo(
                    incomingCadSpecialStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(incomingCadSpecialStatementsInfo, comp);
        }
        return incomingCadSpecialStatementsInfo;
    }

    // TODO: Txapuza para sacar los ultimos statements relacionados. En un
    // futuro puede que hagamos estas cosas de otra manera
    private void getLatestStatements(DocumentModel currentDoc,
            List<Resource> predicates) {
        incomingCadSpecialStatements = new ArrayList<>();
        for (Resource predicate : predicates) {
            // We don't need EloraCoreGraph if we don't use quantity
            List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                    currentDoc, predicate);

            Map<String, List<String>> idMap = new HashMap<>();
            Map<String, List<DocumentModel>> docMap = new HashMap<>();
            Map<String, Statement> stmtMap = new HashMap<>();

            for (Statement stmt : stmts) {
                DocumentModel subjectDoc = RelationHelper.getDocumentModel(
                        stmt.getSubject(), documentManager);

                String versionSeriesId = subjectDoc.getVersionSeriesId();
                String docId = subjectDoc.getId();

                if (idMap.containsKey(versionSeriesId)) {
                    idMap.get(versionSeriesId).add(docId);
                    docMap.get(versionSeriesId).add(subjectDoc);
                } else {
                    List<String> idList = new ArrayList<String>();
                    idList.add(docId);
                    idMap.put(versionSeriesId, idList);

                    List<DocumentModel> docList = new ArrayList<DocumentModel>();
                    docList.add(subjectDoc);
                    docMap.put(versionSeriesId, docList);
                }

                stmtMap.put(docId, stmt);
            }

            for (Map.Entry<String, List<String>> entry : idMap.entrySet()) {
                List<String> relatedUids = entry.getValue();
                if (relatedUids.size() > 1) {
                    String majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                            docMap.get(entry.getKey())).toString();
                    DocumentModel latestDoc = EloraRelationHelper.getLatestRelatedVersion(
                            majorVersion, relatedUids, documentManager);
                    incomingCadSpecialStatements.add(
                            stmtMap.get(latestDoc.getId()));
                } else {
                    incomingCadSpecialStatements.add(
                            stmtMap.get(relatedUids.get(0)));
                }
            }
        }
    }

    @Factory(value = "incomingBomDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingBomDocStatementsInfo() {
        if (incomingBomDocumentStatementsInfo != null) {
            return incomingBomDocumentStatementsInfo;
        }

        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<Resource> predicates = new ArrayList<>();
        predicates.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));
        predicates.add(
                new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));

        incomingBomDocumentStatements = new ArrayList<>();
        for (Resource predicate : predicates) {
            // We don't need EloraCoreGraph if we don't use quantity
            List<Statement> stmts = EloraRelationHelper.getSubjectStatements(
                    currentDoc, predicate);
            incomingBomDocumentStatements.addAll(stmts);
        }

        if (incomingBomDocumentStatements.isEmpty()) {
            incomingBomDocumentStatements = Collections.emptyList();
            incomingBomDocumentStatementsInfo = Collections.emptyList();
        } else {
            incomingBomDocumentStatementsInfo = getStatementsInfo(
                    incomingBomDocumentStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(incomingBomDocumentStatementsInfo, comp);
        }
        return incomingBomDocumentStatementsInfo;
    }

    public List<DocumentModel> getCurrentDocumentRelatedBoms()
            throws EloraException {

        String logInitMsg = "[getCurrentDocumentRelatedBoms] ["
                + documentManager.getPrincipal().getName() + "] ";

        List<DocumentModel> relatedBoms = new ArrayList<>();

        try {
            DocumentModel currentDoc = getCurrentDocument();
            if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
                // Get last version to show its relations
                currentDoc = EloraDocumentHelper.getLatestVersion(currentDoc);
                // currentDoc =
                // documentManager.getLastDocumentVersion(currentDoc.getRef());
            }

            String predicateUri;

            if (currentDoc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                return relatedBoms;
            } else if (currentDoc.hasFacet(
                    EloraFacetConstants.FACET_CAD_DOCUMENT)) {
                predicateUri = EloraRelationConstants.BOM_HAS_CAD_DOCUMENT;
            } else {
                predicateUri = EloraRelationConstants.BOM_HAS_DOCUMENT;
            }

            Resource predicateResource = new ResourceImpl(predicateUri);

            relatedBoms = RelationHelper.getSubjectDocuments(predicateResource,
                    currentDoc);

            if (currentDoc.isVersion() && relatedBoms.size() > 1) {
                // It is possible to have different versions of the same bom
                // item pointing to current document version. Take latest.
                // related
                Map<String, List<DocumentModel>> docListByVersionSerieId = new HashMap<>();
                for (DocumentModel relatedBom : relatedBoms) {
                    // TODO: Poner control de permisos.
                    // We consider that when relatedDoc is null user doesn't
                    // have any permission on the document
                    String versionSeriesId = documentManager.getVersionSeriesId(
                            relatedBom.getRef());
                    if (docListByVersionSerieId.containsKey(versionSeriesId)) {
                        docListByVersionSerieId.get(versionSeriesId).add(
                                relatedBom);
                    } else {
                        List<DocumentModel> docList = new ArrayList<>();
                        docList.add(relatedBom);
                        docListByVersionSerieId.put(versionSeriesId, docList);
                    }
                }

                if (docListByVersionSerieId.size() == 1) {
                    DocumentModel doc = null;
                    // There are different versions of the same bom item
                    // related. Take the latest version.
                    for (Map.Entry<String, List<DocumentModel>> entry : docListByVersionSerieId.entrySet()) {
                        String versionSeriesId = entry.getKey();
                        List<DocumentModel> docList = docListByVersionSerieId.get(
                                versionSeriesId);
                        List<String> uidList = EloraDocumentHelper.getUidListFromDocList(
                                docList);
                        Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                                docList);

                        doc = EloraRelationHelper.getLatestRelatedVersion(
                                String.valueOf(majorVersion), uidList,
                                documentManager);
                    }
                    relatedBoms.removeAll(relatedBoms);
                    relatedBoms.add(doc);
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + "getCurrentDocumentRelatedBoms failed"
                    + e.getMessage(), e);
        }

        return relatedBoms;
    }

    // TODO: Mira si esto todavia se utiliza. Es la forma que tenia Nuxeo de
    // crear relaciones
    public String addStatement() {
        resetEventContext();

        // Check that the subject is locked
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isProxy()) {
            currentDoc = documentManager.getWorkingCopy(currentDoc.getRef());
        }
        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {

                Node object = null;

                objectDocumentUid = objectDocumentUid.trim();
                String repositoryName = navigationContext.getCurrentServerLocation().getName();
                String localName = repositoryName + "/" + objectDocumentUid;
                object = new QNameResourceImpl(
                        RelationConstants.DOCUMENT_NAMESPACE, localName);
                try {
                    eloraDocumentRelationManager.addRelation(documentManager,
                            currentDoc, object, predicateUri, false,
                            includeStatementsInEvents,
                            StringUtils.trim(comment), quantity, ordering,
                            directorOrdering, viewerOrdering);
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

        resetStatements();
        return null;
    }

    public String deleteStatement(StatementInfo stmtInfo) {
        resetEventContext();

        // Check that the subject is locked
        DocumentModel currentDoc = getCurrentDocument();
        if (currentDoc.isLocked()) {
            if (currentDoc.getLockInfo().getOwner().equals(
                    documentManager.getPrincipal().getName())) {
                DocumentModel subject = RelationHelper.getDocumentModel(
                        stmtInfo.getSubject(), documentManager);
                DocumentModel object = RelationHelper.getDocumentModel(
                        stmtInfo.getObject(), documentManager);

                if (currentDoc.isCheckedOut()) {
                    // RelationHelper.removeRelation(subject,
                    // stmtInfo.getPredicate(), object);
                    eloraDocumentRelationManager.softDeleteRelation(
                            documentManager, subject,
                            stmtInfo.getPredicate().getUri(), object);
                } else {
                    // Get working copy stmt data and delete
                    DocumentModel subjectWc = documentManager.getWorkingCopy(
                            subject.getRef());
                    DocumentModel objectWc;
                    if (object.isImmutable()) {
                        objectWc = documentManager.getWorkingCopy(
                                object.getRef());
                    } else {
                        objectWc = object;
                    }
                    // RelationHelper.removeRelation(subjectWc,
                    // stmtInfo.getPredicate(), objectWc);
                    eloraDocumentRelationManager.softDeleteRelation(
                            documentManager, subjectWc,
                            stmtInfo.getPredicate().getUri(), objectWc);
                }
                facesMessages.add(StatusMessage.Severity.INFO,
                        messages.get("label.relation.deleted"));

                // Set subject document as checked out; relations have changed.
                EloraDocumentHelper.checkOutDocument(currentDoc);

            } else {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        messages.get("label.relation.documentLockedByOther"));
            }
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.relation.documentNotLocked"));
        }
        resetStatements();
        return null;
    }

    protected void resetEventContext() {
        Context evtCtx = Contexts.getEventContext();
        if (evtCtx != null) {
            evtCtx.remove("outgoingBomDocumentRelations");
            evtCtx.remove("outgoingBomCadDocumentRelations");
            evtCtx.remove("incomingBomDocumentRelations");
        }
    }

    // TODO: Tener en cuenta que utilizamos este Bean para todas las pesatañas
    // donde se crean relaciones. Al resetear los factory estamos reseteando los
    // valores de todas las pestañas y solo seria necesario resetear los de la
    // pestaña actual. En un futuro habria que crear diferentes bean y que cada
    // uno gestione sus datos
    public void resetStatements() {
        outgoingBomDocumentStatements = null;
        outgoingBomDocumentStatementsInfo = null;
        outgoingBomCadDocumentStatements = null;
        outgoingBomCadDocumentStatementsInfo = null;
        incomingBomDocumentStatements = null;
        incomingBomDocumentStatementsInfo = null;
    }

    private void resetCreateFormValues() {
        predicateUri = "";
        objectDocumentUid = "";
        objectDocumentTitle = "";
        comment = "";
        quantity = "1";
        ordering = null;
        directorOrdering = null;
        viewerOrdering = null;
        showCreateForm = false;
        popupDisplayed = false;
    }

    public List<StatementInfo> getStatementsInfo(List<Statement> statements) {
        if (statements == null) {
            return null;
        }
        List<StatementInfo> infoList = new ArrayList<>();

        for (Statement statement : statements) {
            Subject subject = statement.getSubject();

            // TODO: filter on doc visibility (?)
            NodeInfo subjectInfo = new NodeInfoImpl(subject,
                    RelationHelper.getDocumentModel(subject, documentManager),
                    true);
            Resource predicate = statement.getPredicate();
            Node object = statement.getObject();
            NodeInfo objectInfo = new NodeInfoImpl(object,
                    RelationHelper.getDocumentModel(object, documentManager),
                    true);
            StatementInfo info = new StatementInfoImpl(statement, subjectInfo,
                    new NodeInfoImpl(predicate), objectInfo);
            infoList.add(info);
        }
        return infoList;
    }

    // public List<StatementInfo> getLatestVersionStatementsInfo(
    // List<Statement> statements) {
    // if (statements == null) {
    // return null;
    // }
    // List<StatementInfo> infoList = new ArrayList<StatementInfo>();
    //
    // for (Statement statement : statements) {
    //
    // // SACAR EL OBJECT DOC, SACAR SU ULTIMA VERSION, GUARDAR EL UID PARA
    // // IGNORAR LOS SIGUENTES IGUALES, UTILIZAR ESE OBJECT
    // //
    // //
    // // Node object = statement.getObject();
    // // NodeInfo objectInfo = new NodeInfoImpl(object,
    // // RelationHelper.getDocumentModel(object, documentManager),
    // // true);
    // //
    // // objectInfo.getDocumentModel()
    //
    // Subject subject = statement.getSubject();
    // // TODO: filter on doc visibility (?)
    // NodeInfo subjectInfo = new NodeInfoImpl(subject,
    // RelationHelper.getDocumentModel(subject, documentManager),
    // true);
    // Resource predicate = statement.getPredicate();
    //
    // StatementInfo info = new StatementInfoImpl(statement, subjectInfo,
    // new NodeInfoImpl(predicate), objectInfo);
    // infoList.add(info);
    // }
    // return infoList;
    // }

    protected List<EloraStatementInfo> getEloraStatementsInfo(
            List<Statement> statements) {
        if (statements == null) {
            return null;
        }
        List<EloraStatementInfo> infoList = new ArrayList<>();

        for (Statement statement : statements) {
            Subject subject = statement.getSubject();

            // TODO: filter on doc visibility (?)
            NodeInfo subjectInfo = new NodeInfoImpl(subject,
                    RelationHelper.getDocumentModel(subject, documentManager),
                    true);
            Resource predicate = statement.getPredicate();
            Node object = statement.getObject();
            NodeInfo objectInfo = new NodeInfoImpl(object,
                    RelationHelper.getDocumentModel(object, documentManager),
                    true);

            EloraStatementInfo info = new EloraStatementInfoImpl(statement,
                    subjectInfo, new NodeInfoImpl(predicate), objectInfo);
            infoList.add(info);
        }
        return infoList;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }

    // public void restoreRelations() throws EloraException {
    // resetEventContext();
    //
    // DocumentModel doc = getCurrentDocument();
    // VersionModel version = new VersionModelImpl();
    // version.setId(documentManager.getLastDocumentVersion(doc.getRef()).getId());
    //
    // EloraRelationHelper.restoreRelations(doc, version,
    // eloraDocumentRelationManager, documentManager);
    //
    // resetStatements();
    // }

}
