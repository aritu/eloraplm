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
package com.aritu.eloraplm.promote.checker.impl;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.promote.checker.PromoteCheckerManager;
import com.aritu.eloraplm.promote.constants.PromoteConstants;
import com.aritu.eloraplm.promote.treetable.NodeDynamicInfo;
import com.aritu.eloraplm.promote.treetable.PromoteNodeData;

/**
 *
 * @author aritu
 *
 */
public abstract class PromoteCheckerService implements PromoteCheckerManager {

    protected boolean topLevelOK;

    @Override
    public void resetValues() {
        topLevelOK = true;
    }

    @Override
    public boolean isTopLevelOK() {
        return topLevelOK;
    }

    @Override
    public void processTreeResult(TreeNode node, int level,
            String promoteTransition, String finalState,
            Map<String, String> messages) throws EloraException {

        PromoteNodeData nodeData = (PromoteNodeData) node.getData();
        String parentState = null;
        PromoteNodeData parentNodeData = (PromoteNodeData) node.getParent().getData();

        // if (level != 1) {
        // parentState = parentNodeData.getFinalState();
        // } else {
        // parentState = finalState;
        // }

        if (parentNodeData.getResult().equals(PromoteConstants.RESULT_KO)) {
            parentState = parentNodeData.getData().getCurrentLifeCycleState();
        } else {
            parentState = parentNodeData.getFinalState();
        }

        boolean isSpecial = nodeData.getIsSpecial();
        Statement stmt = nodeData.getStmt();

        processNodeInfo(nodeData.getData(), parentNodeData,
                nodeData.getNodeInfo(), level, isSpecial, parentState, stmt,
                promoteTransition, finalState, messages);

        // TODO: Todos estos set no son necesarios si de los templates accedo a
        // nodeInfo!!
        if (level != 1) {
            nodeData.setFinalState(nodeData.getNodeInfo().getFinalState());
            nodeData.setIsPropagated(nodeData.getNodeInfo().getIsPropagated());
            nodeData.setIsEnforced(nodeData.getNodeInfo().getIsEnforced());
        }
        nodeData.setResult(nodeData.getNodeInfo().getResult());
        nodeData.setResultMsg(nodeData.getNodeInfo().getResultMsg());

        if (topLevelOK
                && nodeData.getResult().equals(PromoteConstants.RESULT_KO)) {
            topLevelOK = false;
        }
        for (TreeNode child : node.getChildren()) {
            processTreeResult(child, -1, promoteTransition, finalState,
                    messages);
        }
    }

    @Override
    public abstract void processNodeInfo(DocumentModel doc,
            PromoteNodeData parentNodeData, NodeDynamicInfo nodeInfo,
            int level, boolean isSpecial, String parentState, Statement stmt,
            String promoteTransition, String finalState,
            Map<String, String> messages) throws EloraException;

}
