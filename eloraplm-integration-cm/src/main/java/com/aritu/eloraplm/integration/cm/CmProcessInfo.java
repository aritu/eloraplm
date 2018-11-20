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
package com.aritu.eloraplm.integration.cm;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.integration.restoperations.util.CmProcessNode;

/**
 * @author aritu
 *
 */
public class CmProcessInfo {

    private List<CmProcessNode> structure;

    private List<DocumentModel> rootItemDocuments;

    private List<DocumentModel> subitemDocuments;

    public List<CmProcessNode> getStructure() {
        return structure;
    }

    public void setStructure(List<CmProcessNode> structure) {
        this.structure = structure;
    }

    public List<DocumentModel> getRootItemDocuments() {
        return rootItemDocuments;
    }

    public void setRootItemDocuments(List<DocumentModel> rootItemDocuments) {
        this.rootItemDocuments = rootItemDocuments;
    }

    public List<DocumentModel> getSubitemDocuments() {
        return subitemDocuments;
    }

    public void setSubitemDocuments(List<DocumentModel> subitemDocuments) {
        this.subitemDocuments = subitemDocuments;
    }

}
