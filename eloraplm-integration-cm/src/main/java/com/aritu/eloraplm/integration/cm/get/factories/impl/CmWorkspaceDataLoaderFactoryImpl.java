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
package com.aritu.eloraplm.integration.cm.get.factories.impl;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.CMDocTypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.get.factories.WorkspaceDataLoader;
import com.aritu.eloraplm.integration.get.factories.impl.WorkspaceDataLoaderFactoryImpl;

/**
 * @author aritu
 *
 */
public class CmWorkspaceDataLoaderFactoryImpl
        extends WorkspaceDataLoaderFactoryImpl {

    @Override
    public WorkspaceDataLoader getDataLoader(DocumentModel doc)
            throws EloraException {

        WorkspaceDataLoader dataLoader = super.getDataLoader(doc);
        if (dataLoader == null) {

            switch (doc.getType()) {
            case CMDocTypeConstants.CM_ECO:
                dataLoader = new CmEcoDataLoader(doc);
                break;
            /*case CMDocTypeConstants.CM_ECR:
            dataLoader = new CmEcrDataLoader(doc);
            break;
            case CMDocTypeConstants.CM_PR:
            dataLoader = new CmPrDataLoader(doc);
            break;*/
            default:
                dataLoader = null;
                break;
            }

        }

        return dataLoader;
    }

}
