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
package com.aritu.eloraplm.config.bean;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.config.util.BomConfig;
import com.aritu.eloraplm.config.util.DocumentMetadataMappingConfig;
import com.aritu.eloraplm.config.util.EloraConfig;
import com.aritu.eloraplm.config.util.MetadataConfig;
import com.aritu.eloraplm.constants.BomConfigConstants;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.EloraDoctypeConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
@Name("vocabularyUpdateManager")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class VocabularyUpdateManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private static final String timestampsVoc = "elora_vocabularies_timestamps";

    private EloraConfigManager configService;

    @Observer(EventNames.DIRECTORY_CHANGED)
    public void manageUpdate(String vocabularyName) {

        if (configService == null) {
            configService = Framework.getService(EloraConfigManager.class);
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try {
            configService.updateConfigProperty(timestampsVoc, vocabularyName,
                    "label", String.valueOf(timestamp.getTime()), false);

            reloadStaticConfigs(vocabularyName);
        } catch (EloraException e) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(
                    "eloraplm.message.error.updateVocabularyTimestamp"));
        }

    }

    /**
     * When we change vocabularies that are loaded initially in static classes,
     * we need to reload them.
     *
     * @param vocabulary
     */
    private void reloadStaticConfigs(String vocabulary) {
        switch (vocabulary) {

        // EloraConfig
        case EloraConfigConstants.VOC_GENERAL_CONFIG:
            EloraConfig.reloadGeneralConfig();
            break;
        case EloraConfigConstants.VOC_INTEGRATION_VERSION_CONTROL:
            EloraConfig.reloadIntegrationConfig();
            break;

        // MetadataConfig
        case EloraConfigConstants.VOC_CAD_METADATA_MAPPING:
        case EloraConfigConstants.VOC_METADATA_MAPPING:
            MetadataConfig.reload();
            break;

        // DocumentMetadataMappingConfig
        case EloraConfigConstants.VOC_DOCUMENT_METADATA_MAPPING:
            DocumentMetadataMappingConfig.reloadConfig();
            break;

        // Bom Part Classification
        case BomConfigConstants.VOC_BOM_PART_CLASS_HIERARCH:
            BomConfig.reloadBomClassificationConfig(
                    EloraDoctypeConstants.BOM_PART);
            break;

        // Bom Product Classification
        case BomConfigConstants.VOC_BOM_PRODUCT_CLASS_HIERARCH:
            BomConfig.reloadBomClassificationConfig(
                    EloraDoctypeConstants.BOM_PRODUCT);
            break;

        // Bom Tool Classification
        case BomConfigConstants.VOC_BOM_TOOL_CLASS_HIERARCH:
            BomConfig.reloadBomClassificationConfig(
                    EloraDoctypeConstants.BOM_TOOL);
            break;

        // Bom Packaging Classification
        case BomConfigConstants.VOC_BOM_PACKAGING_CLASS_HIERARCH:
            BomConfig.reloadBomClassificationConfig(
                    EloraDoctypeConstants.BOM_PACKAGING);
            break;

        // Bom Specification Classification
        case BomConfigConstants.VOC_BOM_SPECIFICATION_CLASS_HIERARCH:
            BomConfig.reloadBomClassificationConfig(
                    EloraDoctypeConstants.BOM_SPECIFICATION);
            break;
        }
    }
}
