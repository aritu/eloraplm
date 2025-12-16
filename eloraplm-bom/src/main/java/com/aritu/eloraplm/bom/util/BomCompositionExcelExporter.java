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
package com.aritu.eloraplm.bom.util;

import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomCompositionExcelExporter
        extends BomCompositionAbstractExcelExporter {

    /**
     * @param compositionTree
     * @param messages
     */
    public BomCompositionExcelExporter(TreeNode compositionTree,
            Map<String, String> messages) {
        super(compositionTree, messages);
    }

    @Override
    protected int writeAdditionalExcelColumnNames(Workbook workbook, int rowNb,
            int lastColumnNamesColumNb) {
        // nothing to do
        return 0;
    }

    @Override
    protected int writeAdditionalExcelColumValues(Workbook workbook, int rowNb,
            int lastColumnNamesColumNb, DocumentModel currentDocworkbook) {
        // nothing to do
        return 0;
    }

}
