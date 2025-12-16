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
package com.aritu.eloraplm.cm.batchProcessing.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import com.aritu.eloraplm.cm.treetable.ImpactedItemsNodeData;

public class DAGManager {
    private DAG dag;

    private Map<String, ImpactedItemsNodeData> nodeMap;

    public DAGManager() {
        dag = new DAG();
        nodeMap = new HashMap<>();
    }

    public void addVertex(String id, ImpactedItemsNodeData nodeData) {
        dag.addVertex(id);
        nodeMap.put(id, nodeData);
    }

    public void addEdge(String source, String destination)
            throws CycleDetectedException {
        dag.addEdge(source, destination);
    }

    public List<String> topologicalSort() {
        List<String> sortedLabels = TopologicalSorter.sort(dag);
        return sortedLabels;
    }

    public Map<String, ImpactedItemsNodeData> getNodeMap() {
        return nodeMap;
    }

    public DAG getDag() {
        return dag;
    }
}