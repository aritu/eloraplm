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

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.integration.cm.CmProcessInfo;
import com.aritu.eloraplm.integration.cm.util.IntegrationCmHelper;
import com.aritu.eloraplm.integration.get.factories.impl.AbstractWorkspaceDataLoader;

/**
 * @author aritu
 *
 */
public class CmEcoDataLoader extends AbstractWorkspaceDataLoader {

    public CmEcoDataLoader(DocumentModel workspaceDoc) throws EloraException {
        super(workspaceDoc);
    }

    @Override
    protected void processCmProcessStructure() throws EloraException {

        CmProcessInfo structInfo = IntegrationCmHelper.getCmEcoProcessInfo(
                workspaceDoc, true);

        for (DocumentModel doc : structInfo.getRootItemDocuments()) {
            try {
                processDocument(0, SOURCE_CM_PROCESS_ROOT_ITEM, doc, true,
                        workspaceDoc.getId(),
                        cmProcessRootItemChildrenVersions);
            } catch (DocumentWithoutArchivedVersionsException e) {
                continue;
            }
        }

        for (DocumentModel doc : structInfo.getSubitemDocuments()) {
            try {
                processDocument(0, SOURCE_CM_PROCESS_SUBITEM, doc, true,
                        workspaceDoc.getId(), cmProcessSubitemChildrenVersions);
            } catch (DocumentWithoutArchivedVersionsException e) {
                continue;
            }
        }

        response.setCmProcessStructure(structInfo.getStructure());

        return;

    }

}
