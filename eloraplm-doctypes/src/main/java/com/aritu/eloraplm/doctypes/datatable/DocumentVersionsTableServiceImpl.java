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
package com.aritu.eloraplm.doctypes.datatable;

import java.util.ArrayList;
import java.util.List;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.datatable.EditableTableService;
import com.aritu.eloraplm.datatable.RowData;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class DocumentVersionsTableServiceImpl implements EditableTableService {

    @Override
    public List<RowData> getData(Object parentObject) throws EloraException {
        List<RowData> data = new ArrayList<RowData>();
        DocumentModel doc = (DocumentModel) parentObject;

        CoreSession session = doc.getCoreSession();

        List<DocumentModel> versions = new ArrayList<DocumentModel>();
        versions = session.getVersions(doc.getRef());

        for (DocumentModel versionDoc : versions) {
            String rowId = versionDoc.getId();
            String type = versionDoc.getType();
            String reference = (String) versionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            String version = versionDoc.getVersionLabel();
            String sourceVersion = "";
            String sourceVersionDocId = "";
            if (versionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_VINF_SOURCE_VERSION) != null) {
                sourceVersionDocId = (String) versionDoc.getPropertyValue(
                        EloraMetadataConstants.ELORA_VINF_SOURCE_VERSION);
                if (sourceVersionDocId.length() > 0) {
                    DocumentModel sourceVersionDoc = session.getDocument(
                            new IdRef(sourceVersionDocId));
                    sourceVersion = sourceVersionDoc.getVersionLabel();
                }
            }
            boolean nonCorrelativeVersion = (boolean) versionDoc.getPropertyValue(
                    EloraMetadataConstants.ELORA_VINF_NON_CORRELATIVE_VERSION);
            String comment = versionDoc.getCheckinComment();
            String lifecycleState = versionDoc.getCurrentLifeCycleState();
            String lastContributor = (String) versionDoc.getPropertyValue(
                    NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);

            data.add(createRowData(rowId, versionDoc, type, reference, version,
                    sourceVersion, nonCorrelativeVersion, comment,
                    lifecycleState, lastContributor, false, false, false));
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
        return createRowData(rowId, null, null, null, null, null, false, null,
                null, null, isNew, isModified, isRemoved);
    }

    public RowData createRowData(String rowId, DocumentModel versionDoc,
            String type, String reference, String version, String sourceVersion,
            boolean nonCorrelativeVersion, String comment,
            String lifecycleState, String lastContributor, boolean isNew,
            boolean isModified, boolean isRemoved) {

        RowData row = new DocumentVersionRowData(rowId, versionDoc, type,
                reference, version, sourceVersion, nonCorrelativeVersion,
                comment, lifecycleState, lastContributor, isNew, isModified,
                isRemoved);

        return row;
    }

}
