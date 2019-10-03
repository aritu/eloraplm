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
package com.aritu.eloraplm.pdm.webapp.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.bom.treetable.BomCadDocumentsTreeBean;

/**
 * @author aritu
 *
 */
@Name("bomCadDocumentsPdm")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
public class BomCadDocumentsPdmBean extends AbstractRelationPdmBean
        implements Serializable {
    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient BomCadDocumentsTreeBean bomCadDocumentsTreeBean;

    public BomCadDocumentsPdmBean() {
    }

    @Override
    protected TreeNode getTreeBeanRoot() {
        return bomCadDocumentsTreeBean.getRoot();
    }

    @Override
    protected void createTreeBeanRoot() {
        bomCadDocumentsTreeBean.createRoot();
    }

}