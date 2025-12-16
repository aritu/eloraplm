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
package com.aritu.eloraplm.bom.datatable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.nuxeo.ecm.core.api.DocumentModel;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraSchemaConstants;
import com.aritu.eloraplm.constants.OEMConstants;
import com.aritu.eloraplm.datatable.EditableTableService;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class ItemOemsTableServiceImpl implements EditableTableService {

    @SuppressWarnings("unchecked")
    @Override
    public List<RowData> getData(Object parentObject) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;

        ArrayList<HashMap<String, Object>> oemsListData = new ArrayList<HashMap<String, Object>>();

        if (doc.hasSchema(EloraSchemaConstants.RELATED_OEM)) {
            oemsListData = (ArrayList<HashMap<String, Object>>) doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_OEM_OEMS);
        }

        if (oemsListData.size() > 0) {

            for (int i = 0; i < oemsListData.size(); ++i) {

                HashMap<String, Object> oemData = oemsListData.get(i);

                String manufacturer = (String) oemData.get(
                        OEMConstants.MANUFACTURER);

                int preference = 0;

                if (oemData.get(OEMConstants.PREFERENCE) != null) {
                    String preferenceStr = String.valueOf(
                            oemData.get(OEMConstants.PREFERENCE));
                    if (preferenceStr != null && preferenceStr.length() > 0) {
                        preference = Integer.parseInt(
                                String.valueOf(preferenceStr));
                    }
                }

                String partNumber = (String) oemData.get(
                        OEMConstants.PART_NUMBER);

                data.add(createRowData(String.valueOf(i), manufacturer,
                        preference, partNumber, false, false, false));
            }
        }

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, false, false, false);
    }

    @Override
    public RowData createRowData(String rowId, boolean isNew,
            boolean isModified, boolean isRemoved) {
        return createRowData(rowId, null, 0, null, isNew, isModified,
                isRemoved);
    }

    public RowData createRowData(String rowId, String manufacturer,
            int preference, String partNumber, boolean isNew,
            boolean isModified, boolean isRemoved) {
        RowData row = new ItemOemRowData(rowId, manufacturer, preference,
                partNumber, isNew, isModified, isRemoved);

        return row;
    }

}
