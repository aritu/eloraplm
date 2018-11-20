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
package com.aritu.eloraplm.integration.get.factories.impl;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.constants.NuxeoDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoader;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoaderFactory;

/**
 * @author aritu
 *
 */
public class WorkspaceDataLoaderFactoryImpl
        implements WorkspaceDataLoaderFactory {

    @Override
    public WorkspaceDataLoader getDataLoader(DocumentModel doc)
            throws EloraException {
        switch (doc.getType()) {
        case NuxeoDoctypeConstants.WORKSPACE:
            return new GeneralWorkspaceDataLoader(doc);
        case EloraDoctypeConstants.PROJECT:
            return new ProjectDataLoader(doc);
        default:
            return null;
        }
    }

}
