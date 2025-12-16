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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;

import com.aritu.eloraplm.bom.lists.util.BomListEntry;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface BomListService {

    Map<String, List<BomListEntry>> getBomListEntries(CoreSession session,
            String docRef, String bomListId, String treeDirection)
            throws EloraException;

    List<BomListEntry> getDistinctBomListEntries(CoreSession session,
            String docRef, List<String> bomListIds) throws EloraException;

    LinkedHashMap<String, List<BomListEntry>> getBomListImpact(
            CoreSession session, String docRef) throws EloraException;

    boolean show(String docRef, String bomListId, String treeDirection);

    boolean hasBomList(String docRef, String bomListId, String treeDirection);

    int count(String docRef, String bomListId, String treeDirection);
}
