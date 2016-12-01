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
public class EloraStatementInfoImpl extends StatementInfoImpl implements
        EloraStatementInfo {

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
    public int getQuantity() {
        String quantity = null;
        Node node = statement.getProperty(EloraRelationConstants.QUANTITY);
        if (node != null && node.isLiteral()) {
            quantity = ((Literal) node).getValue();
            if (quantity != null) {
                try {
                    return Integer.parseInt(quantity);
                } catch (NumberFormatException e) {
                    // TODO Logak jarri
                }
            }
        }
        return 1;
    }

    @Override
    public boolean getIsObjectWc() {
        String isObjectWc = "true";
        Node node = statement.getProperty(EloraRelationConstants.IS_OBJECT_WC);
        if (node != null && node.isLiteral()) {
            if (isObjectWc != null) {
                isObjectWc = ((Literal) node).getValue();
            }
        }
        return Boolean.parseBoolean(isObjectWc);
    }

    @Override
    public int getOrdering() {
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
        return 0;
    }

    @Override
    public int getDirectorOrdering() {
        String directorOrdering = null;
        Node node = statement.getProperty(EloraRelationConstants.DIRECTOR_ORDERING);
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
        return 0;
    }
}
