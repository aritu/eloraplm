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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;

/**
 * Factory to control the creation of relation graphs for EloraPLM
 *
 * @author aritu
 *
 */
public class RelationGraphFactory implements GraphFactory {

    @Override
    public Graph createGraph(GraphDescription graphDescription,
            CoreSession session) {
        Graph graph;
        String name = graphDescription.getName();
        switch (name) {
        // case EloraRelationConstants.XXX:
        // graph = new XXXRelationGraph(session);
        // break;
        default:
            graph = new CoreRelationGraph(session);
        }

        graph.setDescription(graphDescription);
        return graph;
    }

}
