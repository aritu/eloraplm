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
package com.aritu.eloraplm.bom.characteristics.bean;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicMastersHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * BOM Characteristics Helper Bean.
 *
 * @author aritu
 *
 */
@Name("bomCharacteristicsHelper")
@Scope(EVENT)
public class BomCharacteristicsHelperBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsHelperBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public int countBomCharacteristicMasters() {
        String logInitMsg = "[countBomCharacteristicMasters] ["
                + documentManager.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        int countBomCharacteristicsMasters = 0;

        try {
            countBomCharacteristicsMasters = BomCharacteristicMastersHelper.countBomCharacteristicMasters(
                    documentManager);
        } catch (EloraException e) {
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
        return countBomCharacteristicsMasters;
    }

    public List<SortInfo> getSortInfos() {

        List<SortInfo> sortInfos = new ArrayList<SortInfo>();

        if (countBomCharacteristicMasters() > 0) {
            sortInfos.add(new SortInfo(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_CLASSIFICATION,
                    true));
            sortInfos.add(new SortInfo(
                    BomCharacteristicsMetadataConstants.BOM_CHARAC_MASTER_ORDER,
                    true));
            sortInfos.add(
                    new SortInfo(NuxeoMetadataConstants.NX_DC_TITLE, true));
        }

        return sortInfos;
    }

}
