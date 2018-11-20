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
package com.aritu.eloraplm.integration.restoperations.util;

import java.util.ArrayList;
import java.util.List;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraConfigConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class VocabularyContentFactory {

    public static List<VocabularyContent> convertConfigTable(String vocId,
            EloraConfigTable configTable) {
        List<VocabularyContent> content = new ArrayList<VocabularyContent>();
        for (EloraConfigRow configRow : configTable.getValues()) {
            content.add(convertConfigRow(vocId, configRow));
        }
        return content;
    }

    public static VocabularyContent convertConfigRow(String vocId,
            EloraConfigRow configRow) {
        VocabularyContent contentItem = null;
        switch (vocId) {
        case EloraConfigConstants.VOC_CAD_METADATA_MAPPING:
            contentItem = new CadMetadataMappingVocabularyContent();
            break;
        case EloraConfigConstants.VOC_DOCTYPE_MAPPING:
            contentItem = new DoctypeMappingVocabularyContent();
            break;
        case EloraConfigConstants.VOC_GENERAL_CONFIG:
            contentItem = new GeneralConfigVocabularyContent();
            break;
        case EloraConfigConstants.VOC_METADATA_MAPPING:
            contentItem = new MetadataMappingVocabularyContent();
            break;
        case EloraConfigConstants.VOC_RELATION_PROPAGATION:
            contentItem = new RelationPropagationVocabularyContent();
            break;
        }

        if (contentItem != null) {
            return contentItem.convertFromConfigRow(configRow);
        }
        return contentItem;
    }

}
