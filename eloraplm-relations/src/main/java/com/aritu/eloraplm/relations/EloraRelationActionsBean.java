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
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
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

    // Outgoing

    protected List<Statement> outgoingBomDocumentStatements;

    protected List<StatementInfo> outgoingBomDocumentStatementsInfo;

    protected List<Statement> outgoingBomCadDocumentStatements;

    protected List<StatementInfo> outgoingBomCadDocumentStatementsInfo;

    protected List<Statement> outgoingCadSpecialStatements;

    protected List<StatementInfo> outgoingCadSpecialStatementsInfo;

    protected List<Statement> outgoingBomHasBomStatements;

    protected List<StatementInfo> outgoingBomHasBomStatementsInfo;

    protected List<Statement> outgoingCustomerProductStatements;

    protected List<StatementInfo> outgoingCustomerProductStatementsInfo;

    // Incoming

    protected List<Statement> incomingBomDocumentStatements;

    protected List<StatementInfo> incomingBomDocumentStatementsInfo;

    protected List<Statement> incomingCadSpecialStatements;

    protected List<StatementInfo> incomingCadSpecialStatementsInfo;

    protected List<Statement> incomingBomHasBomStatements;

    protected List<StatementInfo> incomingBomHasBomStatementsInfo;

    protected List<Statement> incomingCustomerProductStatements;

    protected List<StatementInfo> incomingCustomerProductStatementsInfo;

    // Related Boms
    protected List<Statement> relatedBomsStatements;

    protected List<StatementInfo> relatedBomsStatementsInfo;

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

    // TODO HONEIK ERABILTZEN DIE??

    protected String predicateUri;

    protected String objectDocumentUid;

    protected String objectDocumentTitle;

    protected String comment;

    protected String quantity = "1";

    protected Integer ordering;

    protected Integer directorOrdering;

    protected Integer viewerOrdering;

    protected Integer inverseViewerOrdering;

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

    public Integer getInverseViewerOrdering() {
        return inverseViewerOrdering;
    }

    public void setInverseViewerOrdering(Integer inverseViewerOrdering) {
        this.inverseViewerOrdering = inverseViewerOrdering;
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

    // ------------
    // Outgoing
    // ------------

    @Factory(value = "outgoingBomDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingBomDocStatementsInfo() {
        if (outgoingBomDocumentStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));

            outgoingBomDocumentStatementsInfo = getOutgoingStatementsInfo(
                    predicates, outgoingBomDocumentStatements);
        }

        return outgoingBomDocumentStatementsInfo;
    }

    @Factory(value = "outgoingBomCadDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingBomCadDocStatementsInfo() {
        if (outgoingBomCadDocumentStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));

            outgoingBomCadDocumentStatementsInfo = getOutgoingStatementsInfo(
                    predicates, outgoingBomCadDocumentStatements);
        }

        return outgoingBomCadDocumentStatementsInfo;
    }

    @Factory(value = "outgoingCadSpecialRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingCadSpecialStatementsInfo() {
        if (outgoingCadSpecialStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.CAD_HAS_DESIGN_TABLE));

            outgoingCadSpecialStatementsInfo = getOutgoingStatementsInfo(
                    predicates, outgoingCadSpecialStatements);
        }

        return outgoingCadSpecialStatementsInfo;
    }

    @Factory(value = "outgoingBomHasBomRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingBomHasBomStatementsInfo() {
        if (outgoingBomHasBomStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.BOM_HAS_BOM));

            outgoingBomHasBomStatementsInfo = getOutgoingStatementsInfo(
                    predicates, outgoingBomHasBomStatements);
        }

        return outgoingBomHasBomStatementsInfo;
    }

    @Factory(value = "outgoingCustomerProductRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingCustomerProductStatementsInfo() {
        if (outgoingCustomerProductStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT));

            outgoingCustomerProductStatementsInfo = getOutgoingStatementsInfo(
                    predicates, outgoingCustomerProductStatements);
        }

        return outgoingCustomerProductStatementsInfo;
    }

    private List<StatementInfo> getOutgoingStatementsInfo(
            List<Resource> predicates, List<Statement> stmts) {
        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<StatementInfo> stmtsInfo = new ArrayList<StatementInfo>();
        stmts = new ArrayList<Statement>();
        for (Resource predicate : predicates) {
            stmts.addAll(RelationHelper.getStatements(currentDoc, predicate));
        }

        if (!stmts.isEmpty()) {
            stmtsInfo = getStatementsInfo(stmts);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(stmtsInfo, comp);
        }
        return stmtsInfo;
    }

    // ------------
    // Incoming
    // ------------

    @Factory(value = "incomingBomDocumentRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingBomDocStatementsInfo() {
        if (incomingBomDocumentStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.BOM_HAS_DOCUMENT));

            // onlyLatest??
            incomingBomDocumentStatementsInfo = getIncomingStatementsInfo(
                    predicates, incomingBomDocumentStatements, false);
        }

        return incomingBomDocumentStatementsInfo;
    }

    @Factory(value = "incomingCadSpecialRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingCadSpecialStatementsInfo() {
        if (incomingCadSpecialStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.CAD_DRAWING_OF));
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.CAD_HAS_DESIGN_TABLE));

            incomingCadSpecialStatementsInfo = getIncomingStatementsInfo(
                    predicates, incomingCadSpecialStatements, true);
        }

        return incomingCadSpecialStatementsInfo;
    }

    @Factory(value = "incomingBomHasBomRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingBomHasBomStatementsInfo() {
        if (incomingBomHasBomStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(
                    new ResourceImpl(EloraRelationConstants.BOM_HAS_BOM));

            // onlyLatest??
            incomingBomHasBomStatementsInfo = getIncomingStatementsInfo(
                    predicates, incomingBomHasBomStatements, false);
        }

        return incomingBomHasBomStatementsInfo;
    }

    @Factory(value = "incomingCustomerProductRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingCustomerProductStatementsInfo() {
        if (incomingCustomerProductStatementsInfo == null) {
            List<Resource> predicates = new ArrayList<>();
            predicates.add(new ResourceImpl(
                    EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT));

            // onlyLatest??
            incomingCustomerProductStatementsInfo = getIncomingStatementsInfo(
                    predicates, incomingCustomerProductStatements, false);
        }

        return incomingCustomerProductStatementsInfo;
    }

    private List<StatementInfo> getIncomingStatementsInfo(
            List<Resource> predicates, List<Statement> stmts,
            boolean onlyLatest) {
        DocumentModel currentDoc = getCurrentDocument();
        if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
            // Get last version to show its relations
            currentDoc = documentManager.getLastDocumentVersion(
                    currentDoc.getRef());
        }
        List<StatementInfo> stmtsInfo = new ArrayList<StatementInfo>();
        stmts = new ArrayList<Statement>();

        if (onlyLatest) {
            getLatestStatements(currentDoc, predicates, stmts);
        } else {
            for (Resource predicate : predicates) {
                stmts.addAll(EloraRelationHelper.getSubjectStatements(
                        currentDoc, predicate));
            }
        }

        if (!stmts.isEmpty()) {
            stmtsInfo = getStatementsInfo(stmts);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(
                    new StatementInfoComparator());
            Collections.sort(stmtsInfo, comp);
        }
        return stmtsInfo;
    }

    // TODO: Txapuza para sacar los ultimos statements relacionados. En un
    // futuro puede que hagamos estas cosas de otra manera
    private void getLatestStatements(DocumentModel currentDoc,
            List<Resource> predicates, List<Statement> incomingStmts) {
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

                if (subjectDoc != null) {

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
                } else {
                    // Add not visible statements
                    incomingStmts.add(stmt);
                }
            }

            for (Map.Entry<String, List<String>> entry : idMap.entrySet()) {
                List<String> relatedUids = entry.getValue();
                if (relatedUids.size() > 1) {
                    Long majorVersion = EloraDocumentHelper.getLatestMajorFromDocList(
                            docMap.get(entry.getKey()));
                    String type = docMap.get(entry.getKey()).get(0).getType();

                    DocumentModel latestDoc = EloraRelationHelper.getLatestRelatedVersion(
                            documentManager, majorVersion, relatedUids, type);
                    incomingStmts.add(stmtMap.get(latestDoc.getId()));
                } else {
                    incomingStmts.add(stmtMap.get(relatedUids.get(0)));
                }
            }
        }
    }

    @Factory(value = "relatedBomStatements", scope = ScopeType.EVENT)
    public List<StatementInfo> getRelatedBomsStatementsInfo()
            throws EloraException {
        String logInitMsg = "[getRelatedBomsStatementsInfo] ["
                + documentManager.getPrincipal().getName() + "] ";
        try {
            if (relatedBomsStatementsInfo == null) {
                DocumentModel currentDoc = getCurrentDocument();
                if (!currentDoc.isCheckedOut() && !currentDoc.isVersion()) {
                    // Get last version to show its relations
                    currentDoc = EloraDocumentHelper.getLatestVersion(
                            currentDoc);
                    if (currentDoc == null) {
                        throw new EloraException("Document |"
                                + getCurrentDocument().getId()
                                + "| has no latest version or it is unreadable.");
                    }
                }

                List<Resource> predicates = new ArrayList<>();
                if (currentDoc.hasFacet(
                        EloraFacetConstants.FACET_BOM_DOCUMENT)) {
                    return relatedBomsStatementsInfo;
                } else if (currentDoc.hasFacet(
                        EloraFacetConstants.FACET_CAD_DOCUMENT)) {
                    predicates.add(new ResourceImpl(
                            EloraRelationConstants.BOM_HAS_CAD_DOCUMENT));
                } else {
                    predicates.add(new ResourceImpl(
                            EloraRelationConstants.BOM_HAS_DOCUMENT));
                }
                relatedBomsStatementsInfo = getIncomingStatementsInfo(
                        predicates, relatedBomsStatements, true);
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + "getRelatedBomsStatementsInfo failed"
                    + e.getMessage(), e);
        }

        return relatedBomsStatementsInfo;
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
        outgoingCadSpecialStatements = null;
        outgoingCadSpecialStatementsInfo = null;
        outgoingBomHasBomStatements = null;
        outgoingBomHasBomStatementsInfo = null;
        outgoingCustomerProductStatements = null;
        outgoingCustomerProductStatementsInfo = null;
        incomingBomDocumentStatements = null;
        incomingBomDocumentStatementsInfo = null;
        incomingCadSpecialStatements = null;
        incomingCadSpecialStatementsInfo = null;
        incomingBomHasBomStatements = null;
        incomingBomHasBomStatementsInfo = null;
        incomingCustomerProductStatements = null;
        incomingCustomerProductStatementsInfo = null;
        relatedBomsStatements = null;
        relatedBomsStatementsInfo = null;
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

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }
}
