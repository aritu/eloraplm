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
package com.aritu.eloraplm.core.relations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.relations.CoreGraph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.core.relations.api.ObjectList;
import com.aritu.eloraplm.core.relations.api.PredicateList;
import com.aritu.eloraplm.core.relations.api.SubjectList;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraCoreGraph extends CoreGraph {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param session
     */
    public EloraCoreGraph(CoreSession session) {
        super(session);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void add(Statement statement) {
        add(Collections.singletonList(statement));
    }

    @Override
    public void add(List<Statement> statements) {
        StatementAdder statementAdder = session == null
                ? new StatementAdder(statements)
                : new StatementAdder(statements, session);
        statementAdder.runUnrestricted();
    }

    @Override
    public List<Statement> getStatements(Node subject, Node predicate,
            Node object) {
        return getStatements(new StatementImpl(subject, predicate, object));
    }

    @Override
    public List<Statement> getStatements(Statement statement) {
        StatementFinder statementFinder = session == null
                ? new StatementFinder(statement)
                : new StatementFinder(statement, session);
        statementFinder.runUnrestricted();
        return statementFinder.statements;
    }

    protected class StatementAdder extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Date now;

        protected StatementAdder(List<Statement> statements) {
            super(getDefaultRepositoryName(), "system");
            this.statements = statements;
        }

        protected StatementAdder(List<Statement> statements,
                CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() {
            now = new Date();
            for (Statement statement : statements) {
                add(statement);
            }
            session.save();
        }

        protected void add(Statement statement) {
            DocumentModel rel = session.createDocumentModel(null, "relation",
                    docType);
            rel = setRelationProperties(rel, statement);
            session.createDocument(rel);
        }

        protected DocumentModel setRelationProperties(DocumentModel rel,
                Statement statement) {
            Resource pred = statement.getPredicate();
            NodeAsString predicate = getNodeAsString(pred);
            if (predicate.uri == null) {
                throw new IllegalArgumentException(
                        "Invalid predicate in statement: " + statement);
            }

            Subject subject = statement.getSubject();
            if (subject.isLiteral()) {
                throw new IllegalArgumentException(
                        "Invalid literal subject in statement: " + statement);
            }
            NodeAsString source = getNodeAsString(subject);

            Node object = statement.getObject();
            NodeAsString target = getNodeAsString(object);

            String author = getAuthor(statement);
            if (author == null) {
                author = getOriginatingUsername();
            }

            Date created = getCreationDate(statement);
            if (created == null) {
                created = now;
            }

            Date modified = getModificationDate(statement);
            if (modified == null) {
                modified = now;
            }

            // Custom metadata for relation document model
            String quantity = getQuantity(statement);
            if (quantity == null) {
                quantity = "1";
            }

            // TODO: Estos tipos se pasan como string porque nuxeo utiliza la
            // clase Literal para gestionarlos. Mirar si se puede hacer algo
            // para que sean integer, boolean, etc. ya que los tenemos que
            // convertir al guardar en la bbdd

            String ordering = getOrdering(statement);

            String directorOrdering = getDirectorOrdering(statement);

            String viewerOrdering = getViewerOrdering(statement);

            String inverseViewerOrdering = getInverseViewerOrdering(statement);

            String isManual = getIsManual(statement);

            // end of custom metadata

            String comment = getComment(statement);

            String title = (source.id != null ? source.id : source.uri) + " "
                    + predicate.uri.substring(
                            predicate.uri.lastIndexOf('/') + 1)
                    + " " + (target.id != null ? target.id
                            : target.uri != null ? target.uri : target.string);
            int MAX_TITLE = 200;
            if (title.length() > MAX_TITLE) {
                title = title.substring(0, MAX_TITLE);
            }

            rel.setPropertyValue(REL_PREDICATE, predicate.uri);
            if (source.id != null) {
                rel.setPropertyValue(REL_SOURCE_ID, source.id);
            } else {
                rel.setPropertyValue(REL_SOURCE_URI, source.uri);
            }
            if (target.id != null) {
                rel.setPropertyValue(REL_TARGET_ID, target.id);
            } else if (target.uri != null) {
                rel.setPropertyValue(REL_TARGET_URI, target.uri);
            } else {
                rel.setPropertyValue(REL_TARGET_STRING, target.string);
            }
            if (author != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATOR, author);
            }
            if (created != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATED, created);
            }
            if (modified != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_MODIFIED, modified);
            }
            rel.setPropertyValue(DC_TITLE, title); // for debug
            if (comment != null) {
                rel.setPropertyValue(DC_DESCRIPTION, comment);
            }

            // Custom metadata for relation document model
            if (quantity != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_QUANTITY, quantity);
            }

            if (ordering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ORDERING,
                        Integer.parseInt(ordering));
            }

            if (directorOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING,
                        Integer.parseInt(directorOrdering));
            }

            if (viewerOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING,
                        Integer.parseInt(viewerOrdering));
            }
            if (inverseViewerOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING,
                        Integer.parseInt(inverseViewerOrdering));
            }
            if (isManual != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ISMANUAL,
                        Boolean.parseBoolean(isManual));
            }
            // end of custom metadata

            return rel;
        }
    }

    /**
     * Nuxeo has its own soft-delete functionality which is activated via config
     * templates. Documents are really deleted with a scheduler each 15 minutes.
     * But all queries must have an extra where to avoid soft-deleted docs to
     * show, and we only need this functionality with relations. So, for now, we
     * have created our version that deletes files daily with another scheduler.
     *
     * @param statement
     */
    public void softDelete(Statement statement) {
        StatementSoftDeleter statementSoftDeleter = session == null
                ? new StatementSoftDeleter(statement)
                : new StatementSoftDeleter(statement, session);
        statementSoftDeleter.runUnrestricted();
    }

    protected class StatementSoftDeleter extends UnrestrictedSessionRunner {

        protected Statement statement;

        protected Date now;

        protected StatementSoftDeleter(Statement statement) {
            super(getDefaultRepositoryName(), "system");
            this.statement = statement;
        }

        protected StatementSoftDeleter(Statement statement,
                CoreSession session) {
            super(session);
            this.statement = statement;
        }

        @Override
        public void run() {
            now = new Date();
            softDelete(statement);
            session.save();
        }

        protected void softDelete(Statement statement) {

            String query = "SELECT * FROM " + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                return;
            }

            DocumentModelList relations = session.query(query);
            if (relations.isEmpty()) {
                return;
            }

            for (DocumentModel rel : relations) {
                rel = setRelationProperties(rel);
                session.saveDocument(rel);
            }
        }

        protected DocumentModel setRelationProperties(DocumentModel rel) {
            rel.setPropertyValue(REL_SOURCE_ID,
                    EloraRelationConstants.SOFT_DELETED_RELATION_SOURCE);
            rel.setPropertyValue(REL_PREDICATE,
                    EloraRelationConstants.SOFT_DELETED_RELATION_PREDICATE);
            rel.setPropertyValue(REL_TARGET_ID,
                    EloraRelationConstants.SOFT_DELETED_RELATION_TARGET);
            rel.setPropertyValue(DC_MODIFIED, now);

            return rel;
        }

    }

    public void update(Statement oldStmt, Statement newStmt) {
        update(Collections.singletonMap(oldStmt, newStmt));
    }

    public void update(Map<Statement, Statement> statements) {
        StatementUpdater statementUpdater = session == null
                ? new StatementUpdater(statements)
                : new StatementUpdater(statements, session);
        statementUpdater.runUnrestricted();
    }

    protected class StatementUpdater extends UnrestrictedSessionRunner {

        protected Map<Statement, Statement> statements;

        protected Date now;

        protected StatementUpdater(Map<Statement, Statement> statements) {
            super(getDefaultRepositoryName(), "system");
            this.statements = statements;
        }

        protected StatementUpdater(Map<Statement, Statement> statements,
                CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() {
            now = new Date();
            for (Map.Entry<Statement, Statement> stmtCouple : statements.entrySet()) {
                Statement oldStmt = stmtCouple.getKey();
                Statement newStmt = stmtCouple.getValue();
                update(oldStmt, newStmt);
            }
            session.save();
        }

        protected void update(Statement oldStmt, Statement newStmt) {

            String query = "SELECT * FROM " + docType;
            query = whereBuilder(query, oldStmt);
            if (query == null) {
                return;
            }

            DocumentModelList relations = session.query(query);
            if (relations.isEmpty()) {
                return;
            }
            if (relations.size() > 1) {
                // TODO ZER EIN??
                return;
            }

            DocumentModel rel = relations.get(0);
            rel = setRelationProperties(rel, newStmt);

            session.saveDocument(rel);
        }

        protected DocumentModel setRelationProperties(DocumentModel rel,
                Statement statement) {
            Resource pred = statement.getPredicate();
            NodeAsString predicate = getNodeAsString(pred);
            if (predicate.uri == null) {
                throw new IllegalArgumentException(
                        "Invalid predicate in statement: " + statement);
            }

            Subject subject = statement.getSubject();
            if (subject.isLiteral()) {
                throw new IllegalArgumentException(
                        "Invalid literal subject in statement: " + statement);
            }
            NodeAsString source = getNodeAsString(subject);

            Node object = statement.getObject();
            NodeAsString target = getNodeAsString(object);

            String author = getAuthor(statement);
            if (author == null) {
                author = getOriginatingUsername();
            }

            Date created = getCreationDate(statement);
            if (created == null) {
                created = now;
            }

            Date modified = getModificationDate(statement);
            if (modified == null) {
                modified = now;
            }

            // Custom metadata for relation document model
            String quantity = getQuantity(statement);
            if (quantity == null) {
                quantity = "1";
            }

            // TODO: Estos tipos se pasan como string porque nuxeo utiliza la
            // clase Literal para gestionarlos. Mirar si se puede hacer algo
            // para que sean integer, boolean, etc. ya que los tenemos que
            // convertir al guardar en la bbdd

            String ordering = getOrdering(statement);

            String directorOrdering = getDirectorOrdering(statement);

            String viewerOrdering = getViewerOrdering(statement);

            String inverseViewerOrdering = getInverseViewerOrdering(statement);

            String isManual = getIsManual(statement);

            // end of custom metadata

            String comment = getComment(statement);

            String title = (source.id != null ? source.id : source.uri) + " "
                    + predicate.uri.substring(
                            predicate.uri.lastIndexOf('/') + 1)
                    + " " + (target.id != null ? target.id
                            : target.uri != null ? target.uri : target.string);
            int MAX_TITLE = 200;
            if (title.length() > MAX_TITLE) {
                title = title.substring(0, MAX_TITLE);
            }

            rel.setPropertyValue(REL_PREDICATE, predicate.uri);
            if (source.id != null) {
                rel.setPropertyValue(REL_SOURCE_ID, source.id);
            } else {
                rel.setPropertyValue(REL_SOURCE_URI, source.uri);
            }
            if (target.id != null) {
                rel.setPropertyValue(REL_TARGET_ID, target.id);
            } else if (target.uri != null) {
                rel.setPropertyValue(REL_TARGET_URI, target.uri);
            } else {
                rel.setPropertyValue(REL_TARGET_STRING, target.string);
            }
            if (author != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATOR, author);
            }
            if (created != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATED, created);
            }
            if (modified != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_MODIFIED, modified);
            }
            rel.setPropertyValue(DC_TITLE, title); // for debug
            if (comment != null) {
                rel.setPropertyValue(DC_DESCRIPTION, comment);
            }

            // Custom metadata for relation document model
            if (quantity != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_QUANTITY, quantity);
            }

            if (ordering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ORDERING,
                        Integer.parseInt(ordering));
            }

            if (directorOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING,
                        Integer.parseInt(directorOrdering));
            }

            if (viewerOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING,
                        Integer.parseInt(viewerOrdering));
            }

            if (inverseViewerOrdering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING,
                        Integer.parseInt(inverseViewerOrdering));
            }

            if (isManual != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ISMANUAL,
                        Boolean.parseBoolean(isManual));
            }
            // end of custom metadata

            return rel;
        }
    }

    protected class StatementFinder extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Statement statement;

        protected StatementFinder(Statement statement) {
            super(getDefaultRepositoryName());
            this.statement = statement;
        }

        protected StatementFinder(Statement statement, CoreSession session) {
            super(session);
            this.statement = statement;
        }

        @Override
        public void run() {
            String query = "SELECT " + REL_PREDICATE + ", " + REL_SOURCE_ID
                    + ", " + REL_SOURCE_URI + ", " + REL_TARGET_ID + ", "
                    + REL_TARGET_URI + ", " + REL_TARGET_STRING + ", "
                    + DC_CREATED + ", " + DC_CREATOR + ", " + DC_MODIFIED + ", "
                    + DC_DESCRIPTION + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_QUANTITY + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_ORDERING + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING
                    + ", " + EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING
                    + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING
                    + ", " + EloraMetadataConstants.ELORA_RELEXT_ISMANUAL
                    + " FROM " + docType;
            query = whereBuilder(query, statement);

            if (query == null) {
                statements = EMPTY_STATEMENTS;
                return;
            }

            // query += " ORDER BY " + REL_PREDICATE + " DESC, " + DC_MODIFIED
            // + " DESC";

            query += " ORDER BY " + EloraMetadataConstants.ELORA_RELEXT_ORDERING
                    + " ASC, " + REL_PREDICATE + " DESC ";

            statements = new ArrayList<>();
            IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);
            try {
                for (Map<String, Serializable> map : it) {
                    String pred = (String) map.get(REL_PREDICATE);
                    String source = (String) map.get(REL_SOURCE_ID);
                    String sourceUri = (String) map.get(REL_SOURCE_URI);
                    String target = (String) map.get(REL_TARGET_ID);
                    String targetUri = (String) map.get(REL_TARGET_URI);
                    String targetString = (String) map.get(REL_TARGET_STRING);
                    Calendar created = (Calendar) map.get(DC_CREATED);
                    String creator = (String) map.get(DC_CREATOR);
                    Calendar modified = (Calendar) map.get(DC_MODIFIED);
                    String comment = (String) map.get(DC_DESCRIPTION);
                    // Custom metadata
                    String quantity = String.valueOf(map.get(
                            EloraMetadataConstants.ELORA_RELEXT_QUANTITY));
                    String ordering = String.valueOf(map.get(
                            EloraMetadataConstants.ELORA_RELEXT_ORDERING));
                    String directorOrdering = String.valueOf(map.get(
                            EloraMetadataConstants.ELORA_RELEXT_DIRECTORORDERING));
                    String viewerOrdering = String.valueOf(map.get(
                            EloraMetadataConstants.ELORA_RELEXT_VIEWERORDERING));
                    String inverseViewerOrdering = String.valueOf(map.get(
                            EloraMetadataConstants.ELORA_RELEXT_INVERSEVIEWERORDERING));
                    Boolean isManual = Boolean.parseBoolean(String.valueOf(
                            map.get(EloraMetadataConstants.ELORA_RELEXT_ISMANUAL)));
                    // end of custom metadata

                    Resource predicate = NodeFactory.createResource(pred);
                    Node subject;
                    if (source != null) {
                        subject = createId(source);
                    } else {
                        subject = createUri(sourceUri);
                    }
                    Node object;
                    if (target != null) {
                        object = createId(target);
                    } else if (targetUri != null) {
                        object = createUri(targetUri);
                    } else {
                        object = NodeFactory.createLiteral(targetString);
                    }
                    Statement statement = new StatementImpl(subject, predicate,
                            object);
                    setCreationDate(statement, created);
                    setAuthor(statement, creator);
                    setModificationDate(statement, modified);
                    setComment(statement, comment);
                    // Custom metadata
                    setQuantity(statement, quantity);
                    setOrdering(statement, ordering);
                    setDirectorOrdering(statement, directorOrdering);
                    setViewerOrdering(statement, viewerOrdering);
                    setInverseViewerOrdering(statement, inverseViewerOrdering);
                    setIsManual(statement, isManual);
                    // end of custom metadata

                    statements.add(statement);

                    // TODO: A futuro podria ser la forma de ordenar las
                    // relaciones. Se podria leer el orden desde una
                    // configuracion
                    // if (pred.equals(EloraRelationConstants.CAD_DRAWING_OF)) {
                    // statements.add(0, statement);
                    // } else {
                    // statements.add(statement);
                    // }
                }
            } finally {
                it.close();
            }
        }

        protected QNameResource createId(String id) {
            return NodeFactory.createQNameResource(DOCUMENT_NAMESPACE,
                    session.getRepositoryName() + '/' + id);
        }

        protected Node createUri(String uri) {
            if (uri.startsWith(BLANK_NS)) {
                // skolemization
                String id = uri.substring(BLANK_NS.length());
                return NodeFactory.createBlank(id.isEmpty() ? null : id);
            } else {
                for (String ns : namespaceList) {
                    if (uri.startsWith(ns)) {
                        String localName = uri.substring(ns.length());
                        return NodeFactory.createQNameResource(ns, localName);
                    }
                }
                return NodeFactory.createResource(uri);
            }
        }

    }

    protected static String getQuantity(Statement statement) {
        return getStringProperty(statement, EloraRelationConstants.QUANTITY);
    }

    protected static String getOrdering(Statement statement) {
        return getStringProperty(statement, EloraRelationConstants.ORDERING);
    }

    protected static String getDirectorOrdering(Statement statement) {
        return getStringProperty(statement,
                EloraRelationConstants.DIRECTOR_ORDERING);
    }

    protected static String getViewerOrdering(Statement statement) {
        return getStringProperty(statement,
                EloraRelationConstants.VIEWER_ORDERING);
    }

    protected static String getInverseViewerOrdering(Statement statement) {
        return getStringProperty(statement,
                EloraRelationConstants.INVERSE_VIEWER_ORDERING);
    }

    protected static String getIsManual(Statement statement) {
        return getStringProperty(statement, EloraRelationConstants.IS_MANUAL);
    }

    protected static void setQuantity(Statement statement, String quantity) {
        setStringProperty(statement, EloraRelationConstants.QUANTITY, quantity);
    }

    protected static void setOrdering(Statement statement, String ordering) {
        setStringProperty(statement, EloraRelationConstants.ORDERING, ordering);
    }

    protected static void setDirectorOrdering(Statement statement,
            String directorOrdering) {
        setStringProperty(statement, EloraRelationConstants.DIRECTOR_ORDERING,
                directorOrdering);
    }

    protected static void setViewerOrdering(Statement statement,
            String viewerOrdering) {
        setStringProperty(statement, EloraRelationConstants.VIEWER_ORDERING,
                viewerOrdering);
    }

    protected static void setInverseViewerOrdering(Statement statement,
            String inverseViewerOrdering) {
        setStringProperty(statement,
                EloraRelationConstants.INVERSE_VIEWER_ORDERING,
                inverseViewerOrdering);
    }

    protected static void setIsManual(Statement statement, Boolean isManual) {
        setStringProperty(statement, EloraRelationConstants.IS_MANUAL,
                isManual.toString());
    }

    @Override
    protected String whereBuilder(String query, Statement statement) {
        List<Object> params = new ArrayList<>(3);
        List<String> clauses = new ArrayList<>(3);

        // Predicates
        Node p = statement.getPredicate();
        if (p != null) {
            if (p instanceof PredicateList) {
                List<? extends Node> predicates = ((PredicateList) p).getResources();
                if (predicates.isEmpty()) {
                    throw new UnsupportedOperationException("empty predicates");
                }
                StringBuilder buf = new StringBuilder(REL_PREDICATE);
                buf.append(" IN (");
                for (Node pre : predicates) {
                    NodeAsString pn = getNodeAsString(pre);
                    if (pn.uri != null) {
                        buf.append("?, ");
                        params.add(pn.uri);
                    }
                }
                buf.setLength(buf.length() - 2); // remove last comma/space
                buf.append(")");
                clauses.add(buf.toString());
            } else {
                NodeAsString pn = getNodeAsString(p);
                if (pn.uri != null) {
                    clauses.add(REL_PREDICATE + " = ?");
                    params.add(pn.uri);
                }
            }
        }

        // Subjects
        Node s = statement.getSubject();
        if (s != null) {
            if (s instanceof Subjects) {
                List<Node> subjects = ((Subjects) s).getNodes();
                if (subjects.isEmpty()) {
                    throw new UnsupportedOperationException("empty subjects");
                }
                StringBuilder buf = new StringBuilder(REL_SOURCE_URI);
                buf.append(" IN (");
                for (Node sub : subjects) {
                    NodeAsString sn = getNodeAsString(sub);
                    if (sn.id != null) {
                        throw new UnsupportedOperationException(
                                "subjects ListNode with id instead of uri"
                                        + subjects);
                    }
                    buf.append("?, ");
                    params.add(sn.uri);
                }
                buf.setLength(buf.length() - 2); // remove last comma/space
                buf.append(")");
                clauses.add(buf.toString());
            } else if (s instanceof SubjectList) {
                List<? extends Node> subjects = ((SubjectList) s).getNodes();
                if (subjects.isEmpty()) {
                    throw new UnsupportedOperationException("empty subjects");
                }
                // We only use id with ListNodes
                StringBuilder buf = new StringBuilder(REL_SOURCE_ID);
                buf.append(" IN (");
                for (Node sub : subjects) {
                    NodeAsString sn = getNodeAsString(sub);
                    if (sn.id == null) {
                        throw new UnsupportedOperationException(
                                "subjects ListNode with no id" + subjects);
                    }
                    buf.append("?, ");
                    params.add(sn.id);
                }
                buf.setLength(buf.length() - 2); // remove last comma/space
                buf.append(")");
                clauses.add(buf.toString());
            } else {
                NodeAsString sn = getNodeAsString(s);
                if (sn.id != null) {
                    clauses.add(REL_SOURCE_ID + " = ?");
                    params.add(sn.id);
                } else {
                    clauses.add(REL_SOURCE_URI + " = ?");
                    params.add(sn.uri);
                }

            }
        }

        // Objects
        Node o = statement.getObject();
        if (o != null) {
            if (o instanceof ObjectList) {
                List<? extends Node> objects = ((ObjectList) o).getNodes();
                if (objects.isEmpty()) {
                    throw new UnsupportedOperationException("empty objects");
                }
                // We only use id with ListNodes
                StringBuilder buf = new StringBuilder(REL_TARGET_ID);
                buf.append(" IN (");
                for (Node ob : objects) {
                    NodeAsString on = getNodeAsString(ob);
                    if (on.id == null) {
                        throw new UnsupportedOperationException(
                                "objects ListNode with no id" + objects);
                    }
                    buf.append("?, ");
                    params.add(on.id);
                }
                buf.setLength(buf.length() - 2); // remove last comma/space
                buf.append(")");
                clauses.add(buf.toString());
            } else {
                NodeAsString on = getNodeAsString(o);
                if (on.id != null) {
                    clauses.add(REL_TARGET_ID + " = ?");
                    params.add(on.id);
                } else if (on.uri != null) {
                    clauses.add(REL_TARGET_URI + " = ?");
                    params.add(on.uri);
                } else {
                    clauses.add(REL_TARGET_STRING + " = ?");
                    params.add(on.string);
                }

            }
        }

        // Build WHERE clause
        if (!clauses.isEmpty()) {
            query += " WHERE " + StringUtils.join(clauses, " AND ");
            query = NXQLQueryBuilder.getQuery(query, params.toArray(), true,
                    true, null);
        }
        return query;
    }
}
