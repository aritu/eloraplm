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

import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractNode;

/**
 * Fake Resource type used to pass down multiple resources into whereBuilder.
 *
 * @author aritu
 *
 */
public class PredicateList extends AbstractNode implements ListResource {

    private static final long serialVersionUID = 1L;

    protected List<Resource> resources;

    public PredicateList(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public NodeType getNodeType() {
        return null;
    }

    @Override
    public boolean isResource() {
        return true;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public void setUri(String uri) {
    }

}
