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
package com.aritu.eloraplm.bom.characteristics.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class BomCharacteristicsQueryResultFactory {

    private static final Log log = LogFactory.getLog(
            BomCharacteristicsQueryResultFactory.class);

    public static List<Map<String, String>> getBomCharacteristicListContent(
            CoreSession session, String documentId, String bomCharacteristicId)
            throws EloraException {
        String logInitMsg = "[getBomCharacteristicListContent] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- bomCharacteristicId = |"
                + bomCharacteristicId + "|");

        List<Map<String, String>> listContent = new ArrayList<Map<String, String>>();

        IterableQueryResult it = null;
        try {
            // First retrieve id of the list element

            String query = BomCharacteristicsQueryFactory.getBomCharacteristicListContentQuery(
                    documentId, bomCharacteristicId);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

                for (Map<String, Serializable> map : it) {
                    String listValue = (String) map.get(
                            pfx + "/*1/listContent/*2/listValue");
                    Long listOrder = (Long) map.get(
                            pfx + "/*1/listContent/*2/listOrder");

                    Map<String, String> listContentEntry = new LinkedHashMap<String, String>();
                    listContentEntry.put("listOrder", listOrder.toString());
                    listContentEntry.put("listValue", listValue);
                    listContent.add(listContentEntry);
                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            it.close();
        }

        log.trace(logInitMsg + "--- EXIT --- with listContent.size() = |"
                + listContent.size() + "|");

        return listContent;

    }

    // TODO:: TO BE REMOVED
    /*public static List<String> getDistinctBomCharacteristicMastersByDocument(
            CoreSession session, String documentId) throws EloraException {

        String logInitMsg = "[getDistinctBomCharacteristicMastersByDocument] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(
                logInitMsg + "--- ENTER --- documentId = |" + documentId + "|");

        List<String> bomCharacteristicMastersIdList = new ArrayList<String>();

        IterableQueryResult it = null;
        try {
            String query = BomCharacteristicsQueryFactory.getDistinctBomCharacteristicMastersByDocumentQuery(
                    documentId);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = BomCharacteristicsMetadataConstants.BOM_CHARAC_LIST;

                for (Map<String, Serializable> map : it) {
                    String bomCharacMasterUid = (String) map.get(
                            pfx + "/*1/bomCharacMaster");

                    bomCharacteristicMastersIdList.add(bomCharacMasterUid);

                }
            }
        } catch (NuxeoException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Nuxeo exception thrown: |" + e.getMessage() + "|");
        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Exception thrown: |" + e.getMessage() + "|");
        } finally {
            it.close();
        }

        log.trace(logInitMsg
                + "--- EXIT --- with bomCharacteristicMastersIdList.size() = |"
                + bomCharacteristicMastersIdList.size() + "|");

        return bomCharacteristicMastersIdList;
    }*/

}
