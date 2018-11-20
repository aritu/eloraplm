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
package com.aritu.eloraplm.bom.characteristics.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import com.aritu.eloraplm.bom.characteristics.util.BomCharacteristicsHelper;
import com.aritu.eloraplm.bom.util.BomHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class LoadBomCharacteristicsListener implements EventListener {

    private static final Log log = LogFactory.getLog(
            LoadBomCharacteristicsListener.class);

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.core.event.EventListener#handleEvent(org.nuxeo.ecm.core.event.Event)
     */
    @Override
    public void handleEvent(Event event) {

        String logInitMsg = "[handleEvent] ["
                + event.getContext().getPrincipal().getName() + "] ";
        // log.trace(logInitMsg + "--- ENTER --- ");

        // Check that we are handling the right event
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            // event is not tied to a document, we should not be here
            // log.trace(logInitMsg + "--- return [invalid event context]---");
            return;
        }

        String eventId = event.getName();
        // log.trace(logInitMsg + "eventId = |" + eventId + "|");

        if (!eventId.equals(DOCUMENT_CREATED)
                && !eventId.equals(BEFORE_DOC_UPDATE)) {
            // log.trace(logInitMsg + "--- return [invalid event name]---");
            return;
        }

        // get the document model from the event context
        DocumentModel doc = docCtx.getSourceDocument();

        // check that we are handling a document with characteristics
        if (!EloraDocumentHelper.checkFilter(doc,
                BomCharacteristicsConstants.IS_DOC_WITH_CHARAC_FILTER_ID)) {
            // log.trace(logInitMsg + "--- return [invalid doc type]---");
            return;
        }

        // check if bom characteristics should be loaded
        String classificationMetadata = BomHelper.getBomClassificationMetadataForBomType(
                doc.getType());

        String classification = BomHelper.getBomClassificationValue(doc,
                classificationMetadata);

        if (needToLoadCharacteristic(doc, eventId, classificationMetadata,
                classification)) {
            try {
                BomCharacteristicsHelper.loadCharacteristicMastersFromListener(
                        doc, classification);
            } catch (EloraException e) {
                log.error(logInitMsg + e.getMessage(), e);
                event.markRollBack();
                throw new NuxeoException(e.getMessage());
            }
        }

        // log.trace(logInitMsg + "--- EXIT ---");
    }

    private boolean needToLoadCharacteristic(DocumentModel doc, String eventId,
            String classificationMetadata, String classification) {
        String logInitMsg = "[needToLoadCharacteristic]";
        log.trace(logInitMsg + "--- ENTER --- docId = |" + doc.getId() + "|");

        boolean result = false;

        if (eventId.equals(DOCUMENT_CREATED) && classification != null
                && !classification.isEmpty()) {
            result = true;
        } else if (eventId.equals(BEFORE_DOC_UPDATE)
                && doc.getProperty(classificationMetadata).isDirty()) {
            result = true;
        }

        log.trace(logInitMsg + "--- EXIT with result = |" + result + "|---");

        return result;
    }

}
