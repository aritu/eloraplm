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
package com.aritu.eloraplm.core.relations.api;

import java.util.List;

import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractNode;

/**
 * Fake Node type used to pass down multiple nodes into whereBuilder.
 *
 * @author aritu
 *
 */
public class SubjectList extends AbstractNode implements ListNode {

    private static final long serialVersionUID = 1L;

    protected List<? extends Node> nodes;

    public SubjectList(List<? extends Node> nodes) {
        this.nodes = nodes;
    }

    public List<? extends Node> getNodes() {
        return nodes;
    }

    @Override
    public NodeType getNodeType() {
        return null;
    }

}
