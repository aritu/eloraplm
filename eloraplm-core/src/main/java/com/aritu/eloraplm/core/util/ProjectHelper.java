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
package com.aritu.eloraplm.core.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.LifeCycleConstants;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Elora Project.
 *
 * @author aritu
 *
 */
public class ProjectHelper {

    private static final Log log = LogFactory.getLog(ProjectHelper.class);

    /**
     * Retrieves the identifiers of the products located in the content of the
     * specified Project. If no product is contained in the content of the
     * Project, an empty list is returned.
     *
     * @param project
     * @return
     * @throws EloraException
     */
    public static List<String> getProductIdsFromProject(DocumentModel project)
            throws EloraException {
        String logInitMsg = "[getProductIdsFromProject] ["
                + project.getCoreSession().getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- projectId = |" + project.getId()
                + "|");

        List<String> productIds = new ArrayList<String>();

        try {
            DocumentModelList children = project.getCoreSession().getChildren(
                    project.getRef());
            if (children != null && !children.isEmpty()) {
                for (DocumentModel child : children) {
                    if (child.getType().equals(
                            EloraDoctypeConstants.BOM_PRODUCT)
                            && !(child.getCurrentLifeCycleState().equals(
                                    LifeCycleConstants.DELETED_STATE))) {
                        String childId = child.getId();
                        if (child.isProxy()) {
                            childId = child.getSourceId();
                        }
                        productIds.add(childId);
                    }
                }
            }
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        }

        log.trace(logInitMsg + "--- EXIT --- with productIds.size() = |"
                + productIds.size() + "|");

        return productIds;
    }

}
