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
package com.aritu.eloraplm.bom.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 *
 * @author aritu
 *
 */
public class SupplierHelper {

    public static List<DocumentModel> getLatestApprovedDesignItemsBySupplier(
            CoreSession session, String supplierId) {

        String query = EloraQueryFactory.getApprovedDesignItemsBySupplier(
                supplierId);
        DocumentModelList designItems = session.query(query);

        Map<String, DocumentModel> latestDesignItemsByWcUid = new HashMap<String, DocumentModel>();

        for (DocumentModel designItem : designItems) {
            String wcUid = designItem.getVersionSeriesId();
            if (latestDesignItemsByWcUid.containsKey(wcUid)) {
                DocumentModel existingDoc = latestDesignItemsByWcUid.get(wcUid);
                long existingMajor = (long) existingDoc.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
                long newMajor = (long) designItem.getPropertyValue(
                        NuxeoMetadataConstants.NX_UID_MAJOR_VERSION);
                if (newMajor > existingMajor) {
                    latestDesignItemsByWcUid.put(wcUid, designItem);
                }
            } else {
                latestDesignItemsByWcUid.put(wcUid, designItem);
            }
        }

        return new ArrayList<DocumentModel>(latestDesignItemsByWcUid.values());
    }

}
