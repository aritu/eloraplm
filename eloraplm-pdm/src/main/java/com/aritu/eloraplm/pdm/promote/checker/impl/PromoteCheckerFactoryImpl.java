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
package com.aritu.eloraplm.pdm.promote.checker.impl;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.promote.checker.PromoteCheckerFactory;
import com.aritu.eloraplm.pdm.promote.checker.PromoteCheckerManager;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class PromoteCheckerFactoryImpl implements PromoteCheckerFactory {

    @Override
    public PromoteCheckerManager getChecker(DocumentModel doc)
            throws EloraException {
        if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            return new CadPromoteCheckerService();
        } else if (doc.hasFacet(EloraFacetConstants.FACET_BOM_DOCUMENT)) {
            return new BomPromoteCheckerService();
        } else {
            return null;
        }
    }

}
