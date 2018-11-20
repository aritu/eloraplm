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

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.constants.EloraConfigConstants;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class RelationPropagationVocabularyContent implements VocabularyContent {

    private String id;

    private String action;

    private String relation;

    private int direction;

    private int propagate;

    private int enforce;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getPropagate() {
        return propagate;
    }

    public void setPropagate(int propagate) {
        this.propagate = propagate;
    }

    public int getEnforce() {
        return enforce;
    }

    public void setEnforce(int enforce) {
        this.enforce = enforce;
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

        RelationPropagationVocabularyContent convertedContent = new RelationPropagationVocabularyContent();
        convertedContent.setId(
                row.getProperty(EloraConfigConstants.PROP_ID).toString());
        convertedContent.setAction(row.getProperty(
                EloraConfigConstants.PROP_RELATION_PROPAGATION_ACTION).toString());
        convertedContent.setRelation(row.getProperty(
                EloraConfigConstants.PROP_RELATION_PROPAGATION_RELATION).toString());
        convertedContent.setDirection((int) (long) row.getProperty(
                EloraConfigConstants.PROP_RELATION_PROPAGATION_DIRECTION));
        convertedContent.setPropagate((int) (long) row.getProperty(
                EloraConfigConstants.PROP_RELATION_PROPAGATION_PROPAGATE));
        convertedContent.setEnforce((int) (long) row.getProperty(
                EloraConfigConstants.PROP_RELATION_PROPAGATION_ENFORCE));
        convertedContent.setObsolete((int) (long) row.getProperty(
                EloraConfigConstants.PROP_OBSOLETE));
        convertedContent.setOrdering((int) (long) row.getProperty(
                EloraConfigConstants.PROP_ORDERING));

        return convertedContent;
    }

}
