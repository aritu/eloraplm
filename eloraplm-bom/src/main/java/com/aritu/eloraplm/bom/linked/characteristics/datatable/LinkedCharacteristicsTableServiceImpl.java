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
package com.aritu.eloraplm.bom.linked.characteristics.datatable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristic;
import com.aritu.eloraplm.bom.linked.characteristics.LinkedCharacteristicInfo;
import com.aritu.eloraplm.bom.linked.characteristics.util.LinkedCharacteristicsHelper;
import com.aritu.eloraplm.constants.BomCharacteristicsConstants;
import com.aritu.eloraplm.constants.BomCharacteristicsMetadataConstants;
import com.aritu.eloraplm.datatable.EditableTableService;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.DocumentUnreadableException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */

public class LinkedCharacteristicsTableServiceImpl
        implements EditableTableService {

    private static final Log log = LogFactory.getLog(
            LinkedCharacteristicsTableServiceImpl.class);

    @SuppressWarnings("unchecked")
    @Override
    public List<RowData> getData(Object parentObject)
            throws EloraException, DocumentUnreadableException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;
        CoreSession session = doc.getCoreSession();

        String logInitMsg = "[getData] [" + session.getPrincipal().getName()
                + "] ";

        ArrayList<HashMap<String, Object>> linkedCharacteristics = new ArrayList<HashMap<String, Object>>();

        if (doc.getPropertyValue(
                BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST) != null) {
            linkedCharacteristics = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                    BomCharacteristicsMetadataConstants.LINKED_CHARAC_LIST);
        }

        if (linkedCharacteristics.size() > 0) {

            for (int i = 0; i < linkedCharacteristics.size(); i++) {

                HashMap<String, Object> linkedCharac = linkedCharacteristics.get(
                        i);

                String linkedDocumentUid = (String) linkedCharac.get(
                        BomCharacteristicsConstants.LINKED_DOCUMENT);
                DocumentModel linkedDocument = null;
                if (linkedDocumentUid != null) {
                    try {
                        linkedDocument = session.getDocument(
                                new IdRef(linkedDocumentUid));
                    } catch (DocumentNotFoundException e) {
                        log.error(logInitMsg
                                + "Error getting linked document with uid = |"
                                + linkedDocumentUid + "|: " + e.getMessage());
                    }
                }

                String linkedDocumentCharacteristicId = (String) linkedCharac.get(
                        BomCharacteristicsConstants.LINKED_DOCUMENT_CHARAC_ID);
                LinkedCharacteristicInfo linkedCharacteristicInfo = LinkedCharacteristicsHelper.retrieveLinkedCharacteristicInfo(
                        session, linkedDocumentUid,
                        linkedDocumentCharacteristicId);

                Boolean showInReport = (Boolean) linkedCharac.get(
                        BomCharacteristicsConstants.SHOW_IN_REPORT);
                Long orderInReport = (Long) linkedCharac.get(
                        BomCharacteristicsConstants.ORDER_IN_REPORT);

                LinkedCharacteristic linkedCharacteristic = new LinkedCharacteristic(
                        linkedDocument, linkedCharacteristicInfo, showInReport,
                        orderInReport);

                data.add(createRowData(String.valueOf(i), linkedCharacteristic,
                        false, false, false));
            }
        }

        // Sort retrieved data
        sortData(data);

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, false, false, false);
    }

    @Override
    public RowData createRowData(String rowId, boolean isNew,
            boolean isModified, boolean isRemoved) {
        return createRowData(rowId, null, isNew, isModified, isRemoved);
    }

    public RowData createRowData(String id,
            LinkedCharacteristic linkedCharacteristic, boolean isNew,
            boolean isRemoved, boolean isModified) {
        RowData row = new LinkedCharacteristicRowData(id, linkedCharacteristic,
                isNew, isRemoved, isModified);

        return row;
    }

    private static void sortData(List<RowData> data) {

        Comparator<RowData> rowDataComparator = new Comparator<RowData>() {

            @Override
            public int compare(RowData o1, RowData o2) {
                // First sort by orderInReport
                Long order1 = ((LinkedCharacteristicRowData) o1).getLinkedCharacteristic().getOrderInReport();
                Long order2 = ((LinkedCharacteristicRowData) o2).getLinkedCharacteristic().getOrderInReport();
                if (order1 == null) {
                    order1 = 0L;
                }
                if (order2 == null) {
                    order2 = 0L;
                }
                int comp = order1.compareTo(order2);
                if (comp != 0) {
                    return comp;
                }

                // Then, by linked document title
                String docTitle1 = ((LinkedCharacteristicRowData) o1).getLinkedCharacteristic().getLinkedDocument().getTitle();
                String docTitle2 = ((LinkedCharacteristicRowData) o2).getLinkedCharacteristic().getLinkedDocument().getTitle();
                comp = docTitle1.compareToIgnoreCase(docTitle2);
                if (comp != 0) {
                    return comp;
                }

                // Then, by linked characteristic title
                String characTitle1 = ((LinkedCharacteristicRowData) o1).getLinkedCharacteristic().getLinkedCharacteristicInfo().getTitle();
                String characTitle2 = ((LinkedCharacteristicRowData) o2).getLinkedCharacteristic().getLinkedCharacteristicInfo().getTitle();
                return characTitle1.compareToIgnoreCase(characTitle2);
            }
        };

        Collections.sort(data, rowDataComparator);
    }

}
