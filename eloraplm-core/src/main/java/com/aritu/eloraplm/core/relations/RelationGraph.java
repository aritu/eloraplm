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

import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;

/**
 * Relation graph interface for EloraPLM
 *
 * @author aritu
 *
 */
public interface RelationGraph extends Graph {

    public void softDelete(Statement statement);

    public void update(Statement oldStmt, Statement newStmt);

    public void update(Map<Statement, Statement> statements);

    public Map<String, Resource> getMetadataResourceMap();

    public String getDocType();

}
