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
package com.aritu.eloraplm.cm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.cm.ImpactedItem;
import com.aritu.eloraplm.cm.ManualItem;
import com.aritu.eloraplm.cm.ModifiedItem;
import com.aritu.eloraplm.cm.util.IMOptions;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.exceptions.EloraExceptionWithStoredMessage;

/**
 * @author aritu
 *
 */
public interface ChangeManagementService {

    void calculateImpactMatrix(CoreSession session, DocumentModel cmProcess,
            IMOptions IMOptions, FacesMessages facesMessages,
            Map<String, String> messages)
            throws EloraException, EloraExceptionWithStoredMessage;

    void saveImpactedItemListInCMProcess(CoreSession session,
            DocumentModel cmProcess, String itemType,
            List<ImpactedItem> impactedItems,
            List<TreeNode> actionChangeTreeNodeList)
            throws EloraException, DocumentUnreadableException;

    void saveModifiedItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<String> originItemUidsToBeRemoved,
            List<ModifiedItem> modifiedItemsToBeAdded,
            HashMap<String, ModifiedItem> changedModifiedItems)
            throws EloraException;

    void saveManualItemChangesInCMProcess(CoreSession session,
            DocumentModel cmProcess, List<String> originItemUidsToBeRemoved,
            List<ManualItem> modifiedItemsToBeAdded,
            HashMap<String, ManualItem> changedModifiedItems)
            throws EloraException;

}
