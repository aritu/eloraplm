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
package com.aritu.eloraplm.core.relations.web;

import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoImpl;

import com.aritu.eloraplm.constants.EloraRelationConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraStatementInfoImpl extends StatementInfoImpl
        implements EloraStatementInfo {

    private static final long serialVersionUID = 1L;

    private final Statement statement;

    public EloraStatementInfoImpl(Statement statement) {
        super(statement);
        this.statement = statement;
    }

    public EloraStatementInfoImpl(Statement statement,
            NodeInfo subjectRepresentation, NodeInfo predicateRepresentation,
            NodeInfo objectRepresentation) {
        super(statement, subjectRepresentation, predicateRepresentation,
                objectRepresentation);
        this.statement = statement;
    }

    @Override
    public String getQuantity() {
        String quantity = null;
        Node node = statement.getProperty(EloraRelationConstants.QUANTITY);
        if (node != null && node.isLiteral()) {
            quantity = ((Literal) node).getValue();
        }
        return quantity;
    }

    @Override
    public Integer getOrdering() {
        String ordering = null;
        Node node = statement.getProperty(EloraRelationConstants.ORDERING);
        if (node != null && node.isLiteral()) {
            ordering = ((Literal) node).getValue();
            if (ordering != null) {
                try {
                    return Integer.parseInt(ordering);
                } catch (NumberFormatException e) {
                    // TODO Logak jarri
                }
            }
        }
        return null;
    }

    @Override
    public Integer getDirectorOrdering() {
        String directorOrdering = null;
        Node node = statement.getProperty(
                EloraRelationConstants.DIRECTOR_ORDERING);
        if (node != null && node.isLiteral()) {
            directorOrdering = ((Literal) node).getValue();
            if (directorOrdering != null) {
                try {
                    return Integer.parseInt(directorOrdering);
                } catch (NumberFormatException e) {
                    // TODO Logak jarri
                }
            }
        }
        return null;
    }

    @Override
    public Integer getViewerOrdering() {
        String viewerOrdering = null;
        Node node = statement.getProperty(
                EloraRelationConstants.VIEWER_ORDERING);
        if (node != null && node.isLiteral()) {
            viewerOrdering = ((Literal) node).getValue();
            if (viewerOrdering != null) {
                try {
                    return Integer.parseInt(viewerOrdering);
                } catch (NumberFormatException e) {
                    // TODO Logak jarri
                }
            }
        }
        return null;
    }
}
