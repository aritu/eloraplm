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
package com.aritu.eloraplm.viewer.methodexecuter;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Viewer Method Executer general adapter.
 *
 * @author aritu
 *
 */
public class MethodExecuterAdapter implements MethodExecuter {

    public final DocumentModel doc;

    public MethodExecuterAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public String getCurrentLifeCycleState() {

        return doc.getCurrentLifeCycleState();

    }

    @Override
    public String getVersionLabel() {
        return doc.getVersionLabel();
    }

    @Override
    public Serializable getContextData(String key) {
        return doc.getContextData(key);
    }

}
