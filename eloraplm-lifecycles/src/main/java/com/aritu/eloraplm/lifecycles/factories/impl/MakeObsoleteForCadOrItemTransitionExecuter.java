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
package com.aritu.eloraplm.lifecycles.factories.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;

import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;

/**
 * @author aritu
 *
 */

public class MakeObsoleteForCadOrItemTransitionExecuter
        implements TransitionExecuter {

    @Override
    public String getPreviousScreen() {
        return "/incl/action/make_obsolete_tree.xhtml";
    }

    @Override
    public void init(DocumentModel doc) {
    }

    @Override
    public boolean canBeExecuted() {
        Object reply = SeamComponentCallHelper.callSeamComponentByName(
                "makeObsoleteTreeBean", "canBeExecuted",
                new ArrayList<Object>().toArray());
        return (boolean) reply;
    }

    @Override
    public List<String> getErrorList() {
        // For now we don't use this method
        return new ArrayList<String>();
    }

    @Override
    public void execute(DocumentModel doc) {
        try {

            SeamComponentCallHelper.callSeamComponentByName(
                    "makeObsoleteTreeBean", "makeObsolete",
                    new ArrayList<Object>().toArray());

        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return false;
    }

}
