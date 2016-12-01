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
package com.aritu.eloraplm.treetable;

import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface NodeManager {
    /**
     * Receives the parent document, and returns all the TreeNode structure
     *
     * @param parentDoc
     * @return
     */
    TreeNode getRoot(Object parentObject) throws EloraException;

}
