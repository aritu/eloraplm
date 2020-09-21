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
package com.aritu.eloraplm.integration.config;

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
import com.aritu.eloraplm.config.util.MetadataConfig;
import com.aritu.eloraplm.constants.EloraConfigConstants;
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
        case EloraConfigConstants.VOC_CAD_METADATA_MAPPING:
        case EloraConfigConstants.VOC_METADATA_MAPPING:
            MetadataConfig.reload();
            break;

        // TODO Add remaining vocabularies

        }
    }
}
