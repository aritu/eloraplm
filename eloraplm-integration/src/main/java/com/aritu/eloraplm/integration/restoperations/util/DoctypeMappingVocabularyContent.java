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

import org.codehaus.jackson.annotate.JsonProperty;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.constants.EloraConfigConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class DoctypeMappingVocabularyContent implements VocabularyContent {

    private String id;

    private String authoringTool;

    private String cadDoctype;

    private String plmDoctype;

    private int obsolete;

    private int ordering;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;

    }

    @JsonProperty("authoring_tool")
    public String getAuthoringTool() {
        return authoringTool;
    }

    public void setAuthoringTool(String authoringTool) {
        this.authoringTool = authoringTool;
    }

    @JsonProperty("cad_doctype")
    public String getCadDoctype() {
        return cadDoctype;
    }

    public void setCadDoctype(String cadDoctype) {
        this.cadDoctype = cadDoctype;
    }

    @JsonProperty("plm_doctype")
    public String getPlmDoctype() {
        return plmDoctype;
    }

    public void setPlmDoctype(String plmDoctype) {
        this.plmDoctype = plmDoctype;
    }

    @Override
    public int getObsolete() {
        return obsolete;
    }

    @Override
    public void setObsolete(int obsolete) {
        this.obsolete = obsolete;
    }

    @Override
    public int getOrdering() {
        return ordering;
    }

    @Override
    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    @Override
    public VocabularyContent convertFromConfigRow(EloraConfigRow row) {

        DoctypeMappingVocabularyContent convertedContent = new DoctypeMappingVocabularyContent();
        convertedContent.setId(
                row.getProperty(EloraConfigConstants.PROP_ID).toString());
        convertedContent.setAuthoringTool(row.getProperty(
                EloraConfigConstants.PROP_DOCTYPE_MAPPING_AUTHORING_TOOL).toString());
        convertedContent.setCadDoctype(row.getProperty(
                EloraConfigConstants.PROP_DOCTYPE_MAPPING_CAD_DOCTYPE).toString());
        convertedContent.setPlmDoctype(row.getProperty(
                EloraConfigConstants.PROP_DOCTYPE_MAPPING_PLM_DOCTYPE).toString());
        convertedContent.setObsolete((int) (long) row.getProperty(
                EloraConfigConstants.PROP_OBSOLETE));
        convertedContent.setOrdering((int) (long) row.getProperty(
                EloraConfigConstants.PROP_ORDERING));

        return convertedContent;
    }

}
