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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;
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
        StatementAdder statementAdder = session == null ? new StatementAdder(
                statements) : new StatementAdder(statements, session);
        statementAdder.runUnrestricted();
    }

    @Override
    public List<Statement> getStatements(Node subject, Node predicate,
            Node object) {
        return getStatements(new StatementImpl(subject, predicate, object));
    }

    @Override
    public List<Statement> getStatements(Statement statement) {
        StatementFinder statementFinder = session == null ? new StatementFinder(
                statement) : new StatementFinder(statement, session);
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

        protected StatementAdder(List<Statement> statements, CoreSession session) {
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
            // TODO: Estos tipos se pasan como string porque nuxeo utiliza la
            // clase Literal para gestionarlos. Mirar si se puede hacer algo
            // para que sean integer, boolean, etc. ya que los tenemos que
            // convertir al guardar en la bbdd
            String quantity = getQuantity(statement);
            if (quantity == null) {
                quantity = "1";
            }

            String isObjectWc = getIsObjectWc(statement);
            if (isObjectWc == null) {
                isObjectWc = "true";
            }

            String ordering = getOrdering(statement);
            if (ordering == null) {
                ordering = "1";
            }
            // end of custom metadata

            String comment = getComment(statement);

            String title = (source.id != null ? source.id : source.uri)
                    + " "
                    + predicate.uri.substring(predicate.uri.lastIndexOf('/') + 1)
                    + " "
                    + (target.id != null ? target.id
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
                        EloraMetadataConstants.ELORA_RELEXT_QUANTITY,
                        Integer.parseInt(quantity));
            }

            if (isObjectWc != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ISOBJECTWC,
                        Boolean.parseBoolean(isObjectWc));
            }

            if (ordering != null) {
                rel.setPropertyValue(
                        EloraMetadataConstants.ELORA_RELEXT_ORDERING,
                        Integer.parseInt(ordering));
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
                    + DC_CREATED + ", " + DC_CREATOR + ", " + DC_MODIFIED
                    + ", " + DC_DESCRIPTION + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_QUANTITY + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_ISOBJECTWC + ", "
                    + EloraMetadataConstants.ELORA_RELEXT_ORDERING + " FROM "
                    + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                statements = EMPTY_STATEMENTS;
                return;
            }
            statements = new ArrayList<Statement>();
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
                    String quantity = String.valueOf(map.get(EloraMetadataConstants.ELORA_RELEXT_QUANTITY));
                    String isObjectWc = String.valueOf(map.get(EloraMetadataConstants.ELORA_RELEXT_ISOBJECTWC));
                    String ordering = String.valueOf(map.get(EloraMetadataConstants.ELORA_RELEXT_ORDERING));
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
                    setIsObjectWc(statement, isObjectWc);
                    setOrdering(statement, ordering);
                    // end of custom metadata
                    statements.add(statement);
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

    protected static String getIsObjectWc(Statement statement) {
        return getStringProperty(statement, EloraRelationConstants.IS_OBJECT_WC);
    }

    protected static String getOrdering(Statement statement) {
        return getStringProperty(statement, EloraRelationConstants.ORDERING);
    }

    protected static void setQuantity(Statement statement, String quantity) {
        setStringProperty(statement, EloraRelationConstants.QUANTITY, quantity);
    }

    protected static void setIsObjectWc(Statement statement, String isObjectWc) {
        setStringProperty(statement, EloraRelationConstants.IS_OBJECT_WC,
                isObjectWc);
    }

    protected static void setOrdering(Statement statement, String ordering) {
        setStringProperty(statement, EloraRelationConstants.ORDERING, ordering);
    }
}
