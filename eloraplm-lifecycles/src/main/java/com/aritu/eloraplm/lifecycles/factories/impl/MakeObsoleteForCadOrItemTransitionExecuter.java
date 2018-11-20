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

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;

/**
 * @author aritu
 *
 */
public class MakeObsoleteForCadOrItemTransitionExecuter
        implements TransitionExecuter {

    @Override
    public String getPreviousScreen() {
        return "/incl/action/tree_promote_wrapper.xhtml";
    }

    @Override
    public void init(DocumentModel doc) {

        List<Object> params = new ArrayList<Object>();
        params.add(EloraLifeCycleConstants.TRANS_OBSOLETE);
        params.add(doc);
        SeamComponentCallHelper.callSeamComponentByName("promoteTreeBean",
                "setTransitionAndCreateRoot", params.toArray());
    }

    @Override
    public boolean canBeExecuted() {
        Object reply = SeamComponentCallHelper.callSeamComponentByName(
                "promoteTreeBean", "getAllOK",
                new ArrayList<Object>().toArray());
        return (boolean) reply;
    }

    @Override
    public void execute(DocumentModel doc) {
        SeamComponentCallHelper.callSeamComponentByName("promoteTreeBean",
                "runPromote", new ArrayList<Object>().toArray());
    }

    @Override
    public boolean hasToFireDefaultEvent() {
        return false;
    }

}
