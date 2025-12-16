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
package com.aritu.eloraplm.bom.lists.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;

import com.aritu.eloraplm.bom.lists.util.BomListEntry;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public class BomListServiceImpl implements BomListService {

    @Override
    public Map<String, List<BomListEntry>> getBomListEntries(
            CoreSession session, String docRef, String bomListId,
            String treeDirection) throws EloraException {

        Map<String, List<BomListEntry>> bomListEntries = new HashMap<String, List<BomListEntry>>();

        // TODO: THIS METHOD MUST BE IMPLEMENTED WHEN ADDING MRC MODULE TO
        // STANDARD IMPLEMENTATION

        return bomListEntries;
    }

    @Override
    public List<BomListEntry> getDistinctBomListEntries(CoreSession session,
            String docRef, List<String> bomListIds) throws EloraException {

        List<BomListEntry> distinctBomListEntries = new ArrayList<BomListEntry>();

        // TODO: THIS METHOD MUST BE IMPLEMENTED WHEN ADDING MRC MODULE TO
        // STANDARD IMPLEMENTATION

        return distinctBomListEntries;
    }

    @Override
    public LinkedHashMap<String, List<BomListEntry>> getBomListImpact(
            CoreSession session, String docRef) throws EloraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean show(String docRef, String bomListId, String treeDirection) {
        if (treeDirection.equals(
                EloraRelationConstants.DIRECTION_COMPOSITION)) {
            return true;
        } else {
            return hasBomList(docRef, bomListId, treeDirection);
        }
    }

    @Override
    public boolean hasBomList(String docRef, String bomListId,
            String treeDirection) {
        return count(docRef, bomListId, treeDirection) > 0 ? true : false;
    }

    @Override
    public int count(String docRef, String bomListId, String treeDirection) {

        // TODO: Calcular la cantidad del bomlist. Mirar qué llega aquí como
        // docRef, si llega el currentdocument o llega el documento que está
        // entre el current y la lista
        return 1;

    }
}
