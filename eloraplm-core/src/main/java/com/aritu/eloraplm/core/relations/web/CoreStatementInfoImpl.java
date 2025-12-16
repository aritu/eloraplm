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
 *
 * @author aritu
 *
 */
public class CoreStatementInfoImpl extends StatementInfoImpl
        implements CoreStatementInfo {

    private static final long serialVersionUID = 1L;

    public CoreStatementInfoImpl(Statement statement) {
        super(statement);
    }

    public CoreStatementInfoImpl(Statement statement,
            NodeInfo subjectRepresentation, NodeInfo predicateRepresentation,
            NodeInfo objectRepresentation) {
        super(statement, subjectRepresentation, predicateRepresentation,
                objectRepresentation);
    }

    @Override
    public Boolean getIsTemplate() {
        String isTemplate = null;
        Node node = getStatement().getProperty(
                EloraRelationConstants.IS_TEMPLATE);
        if (node != null && node.isLiteral()) {
            isTemplate = ((Literal) node).getValue();
            if (isTemplate != null) {
                return Boolean.parseBoolean(isTemplate);
            }
        }
        return null;
    }
}
