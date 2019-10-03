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
package com.aritu.eloraplm.bom.characteristics.util;

import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;

import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomCharacteristicMastersQueryFactory {

    public static String getBomCharacteristicMastersFolderQuery() {

        String query = String.format("SELECT * from %s",
                BomCharacteristicsConstants.BOM_CHARAC_MASTER_FOLDER_DOCUMENT_TYPE);

        return query;
    }

    public static String getLoadBomCharacteristicMastersQuery(
            String bomCharacteristicMasterDocType, String classification)
            throws EloraException {

        String classificationCondition = " AND ("
                + BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_CLASSIFICATION
                + " is null";
        if (classification != null && !classification.isEmpty()) {
            String classificationItem = "";
            // First split the classification, since it can be hierarchical
            String[] classificationLevels = classification.split(
                    ChainSelect.DEFAULT_KEY_SEPARATOR);
            for (int i = 0; i < classificationLevels.length; i++) {
                if (i == 0) {
                    classificationItem = classificationLevels[i];
                } else {
                    classificationItem += ChainSelect.DEFAULT_KEY_SEPARATOR
                            + classificationLevels[i];
                }
                classificationCondition += " OR "
                        + BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_CLASSIFICATION
                        + " = '" + classificationItem + "' ";
            }
        }
        classificationCondition += ")";

        String query = "SELECT * FROM " + bomCharacteristicMasterDocType + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + bomCharacteristicMasterDocType
                + "' AND "
                + BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_OBSOLETE
                + " = 0 " + classificationCondition + " ORDER BY "
                + BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_CLASSIFICATION
                + ", "
                + BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_ORDER;
        return query;
    }

}
