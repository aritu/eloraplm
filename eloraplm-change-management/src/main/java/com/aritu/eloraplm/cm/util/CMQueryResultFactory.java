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
package com.aritu.eloraplm.cm.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * CM Query Results Factory class.
 *
 * @author aritu
 *
 */
public class CMQueryResultFactory {

    private static final Log log = LogFactory.getLog(
            CMQueryResultFactory.class);

    public static List<String> getDistinctDerivedModifiedItemsByOriginList(
            CoreSession session, String cmProcessUid, String itemType,
            List<String> originItemUids) throws EloraException {

        String logInitMsg = "[getDistinctDerivedModifiedItemsByOriginList] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- itemType = |" + itemType + "|");

        List<String> derivedModifiedItems = new ArrayList<String>();

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getDistinctDerivedModifiedItemsByOriginListQuery(
                    cmProcessUid, itemType, originItemUids);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = CMHelper.getModifiedItemListMetadaName(itemType);

                for (Map<String, Serializable> map : it) {
                    String originItemUid = (String) map.get(
                            pfx + "/*1/originItem");

                    derivedModifiedItems.add(originItemUid);

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

        log.trace(
                logInitMsg + "--- EXIT --- with derivedModifiedItems.size() = |"
                        + derivedModifiedItems.size() + "|");

        return derivedModifiedItems;
    }

    public static List<String> getDistinctImpacteItemsActionsByOriginList(
            CoreSession session, String cmProcessUid, String itemType,
            List<String> originItemUids) throws EloraException {

        String logInitMsg = "[getDistinctImpacteItemsActionsByOriginList] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- itemType = |" + itemType + "|");

        List<String> distinctActions = new ArrayList<String>();

        IterableQueryResult it = null;
        try {
            String query = CMQueryFactory.getDistinctImpacteItemsActionsByOriginListQuery(
                    cmProcessUid, itemType, originItemUids);
            it = session.queryAndFetch(query, NXQL.NXQL);

            if (it.size() > 0) {

                String pfx = CMHelper.getImpactedItemListMetadaName(itemType);

                for (Map<String, Serializable> map : it) {
                    String action = (String) map.get(pfx + "/*1/action");

                    distinctActions.add(action);

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

        log.trace(logInitMsg + "--- EXIT --- with distinctActions.size() = |"
                + distinctActions.size() + "|");

        return distinctActions;
    }

}
