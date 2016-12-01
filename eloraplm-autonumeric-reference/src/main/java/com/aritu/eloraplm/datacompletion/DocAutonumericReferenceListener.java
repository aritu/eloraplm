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

package com.aritu.eloraplm.datacompletion;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.core.util.EloraEventTypes;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * @author aritu
 *
 */
public class DocAutonumericReferenceListener implements EventListener {

    protected static Log log = LogFactory.getLog(DocAutonumericReferenceListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();

                // TODO: Hau hobeto ulertu.
                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                CoreSession session = docEventContext.getCoreSession();

                // Check if reference is null or empty
                Serializable currentReference = doc.getPropertyValue(EloraMetadataConstants.ELORA_ELO_REFERENCE);
                if (currentReference == null
                        || currentReference.toString().isEmpty()) {
                    String maxReference = null;

                    String query = EloraQueryFactory.getMaxReferenceByType(doc.getType());
                    IterableQueryResult queryResult = session.queryAndFetch(
                            query, NXQL.NXQL);

                    try {
                        if (queryResult.iterator().hasNext()) {
                            Map<String, Serializable> map = queryResult.iterator().next();
                            maxReference = map.get("MAX(elo:reference)").toString();
                        }
                    } finally {
                        queryResult.close();
                    }

                    int newReferenceNum = 0;
                    String newReference = null;
                    if (maxReference != null) {
                        try {
                            newReferenceNum = Integer.parseInt(maxReference) + 1;
                        } catch (NumberFormatException e) {
                            log.error("Max reference is not numeric, cannot use autonumeric.");
                            return;
                        }
                    }
                    newReference = String.valueOf(newReferenceNum);

                    doc.setPropertyValue(
                            EloraMetadataConstants.ELORA_ELO_REFERENCE,
                            newReference);
                }

            }
        }
    }

    protected boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getHandledEventsName() {
        return Arrays.asList(EloraEventTypes.BEFORE_TCI_DOC_VALIDATION);
    }
}