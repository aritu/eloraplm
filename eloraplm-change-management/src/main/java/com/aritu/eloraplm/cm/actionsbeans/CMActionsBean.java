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
package com.aritu.eloraplm.cm.actionsbeans;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.cm.util.CMHelper;
import com.aritu.eloraplm.constants.CMConstants;
import com.aritu.eloraplm.constants.CMMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * CM actions bean.
 *
 * @author aritu
 *
 */
@Name("cmActionsBean")
@Scope(ScopeType.EVENT)
@Install(precedence = APPLICATION)
public class CMActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CMActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    public ArrayList<HashMap<String, Object>> getManualModifiedItemsList()
            throws EloraException {

        String logInitMsg = "[getManualModifiedItemsList] ";

        ArrayList<HashMap<String, Object>> manualModifiedItemsList = new ArrayList<HashMap<String, Object>>();

        try {
            ArrayList<HashMap<String, Object>> modifiedItemsList = new ArrayList<HashMap<String, Object>>();

            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            if (currentDocument.getPropertyValue(
                    CMMetadataConstants.BOM_MODIFIED_ITEM_LIST) != null) {
                modifiedItemsList.addAll(
                        (ArrayList<HashMap<String, Object>>) currentDocument.getPropertyValue(
                                CMMetadataConstants.BOM_MODIFIED_ITEM_LIST));
            }

            if (currentDocument.getPropertyValue(
                    CMMetadataConstants.DOC_MODIFIED_ITEM_LIST) != null) {
                modifiedItemsList.addAll(
                        (ArrayList<HashMap<String, Object>>) currentDocument.getPropertyValue(
                                CMMetadataConstants.DOC_MODIFIED_ITEM_LIST));
            }

            for (HashMap<String, Object> modifiedItem : modifiedItemsList) {
                boolean isManual = (boolean) modifiedItem.get("isManual");
                if (isManual) {
                    manualModifiedItemsList.add(modifiedItem);
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        return manualModifiedItemsList;
    }

    public boolean existImpactedItemInCMProcess(String cmProcessUid,
            String originItemUid) {

        String logInitMsg = "[existImpactedItemInCMProcess] ";

        boolean isContained = false;

        try {
            isContained = CMHelper.existImpactedItemInCMProcess(documentManager,
                    cmProcessUid, originItemUid, CMConstants.ITEM_TYPE_DOC);

            if (!isContained) {
                isContained = CMHelper.existImpactedItemInCMProcess(
                        documentManager, cmProcessUid, originItemUid,
                        CMConstants.ITEM_TYPE_BOM);
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        return isContained;
    }

    public boolean existModifiedItemInCMProcess(String cmProcessUid,
            String originItemUid) {

        String logInitMsg = "[existModifiedItemInCMProcess] ";

        boolean isContained = false;

        try {
            isContained = CMHelper.existModifiedItemInCMProcess(documentManager,
                    cmProcessUid, originItemUid, CMConstants.ITEM_TYPE_DOC);

            if (!isContained) {
                isContained = CMHelper.existModifiedItemInCMProcess(
                        documentManager, cmProcessUid, originItemUid,
                        CMConstants.ITEM_TYPE_BOM);
            }
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        return isContained;
    }

}
