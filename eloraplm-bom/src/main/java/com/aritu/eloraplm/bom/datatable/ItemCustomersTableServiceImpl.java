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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.EloraRelationConstants;
import com.aritu.eloraplm.datatable.EditableTableService;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class ItemCustomersTableServiceImpl implements EditableTableService {

    @Override
    public List<RowData> getData(Object parentObject) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;
        CoreSession session = doc.getCoreSession();

        String[] dataArray = (String[]) doc.getPropertyValue(
                EloraMetadataConstants.ELORA_CUST_CUSTOMERS);

        Map<String, String> cpMap = getRelatedCustomerProducts(doc);

        if (dataArray.length > 0) {
            for (String d : dataArray) {
                String customerProductDocId = cpMap.containsKey(d)
                        ? cpMap.get(d)
                        : null;
                DocumentModel customerProductDoc = null;
                if (customerProductDocId != null) {
                    customerProductDoc = session.getDocument(
                            new IdRef(customerProductDocId));
                }
                data.add(createRowData(d, customerProductDoc, false, false,
                        false));
            }
        }

        return data;
    }

    @Override
    public RowData createRowData(String rowId) {
        return createRowData(rowId, null, false, false, false);
    }

    @Override
    public RowData createRowData(String rowId, boolean isNew,
            boolean isModified, boolean isRemoved) {
        return createRowData(rowId, null, isNew, isModified, isRemoved);
    }

    public RowData createRowData(String rowId, DocumentModel customerProductDoc,
            boolean isNew, boolean isModified, boolean isRemoved) {
        RowData row = new ItemCustomerRowData(rowId, customerProductDoc, isNew,
                isModified, isRemoved);

        return row;
    }

    private Map<String, String> getRelatedCustomerProducts(DocumentModel doc) {
        Map<String, String> cpMap = new HashMap<String, String>();

        DocumentModelList cpList = RelationHelper.getSubjectDocuments(
                new ResourceImpl(
                        EloraRelationConstants.BOM_CUSTOMER_HAS_PRODUCT),
                doc);
        for (DocumentModel cp : cpList) {
            Serializable customer = cp.getPropertyValue(
                    EloraMetadataConstants.ELORA_BOMCUSTPROD_CUSTOMER);
            if (customer != null) {
                cpMap.put((String) customer, cp.getId());
            }
        }
        return cpMap;

    }

}
