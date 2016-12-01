/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Razvan Caraghin
 *     Florent Guillaume
 *     Thierry Martins
 *     Antoine Taillefer
 */

package com.aritu.eloraplm.history;

import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.jboss.seam.ScopeType.CONVERSATION;
import java.io.Serializable;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Extra actions for History tab
 *
 * @author Aritu
 */
@Name("eloraPlmVersionHistoryActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class EloraPlmVersionHistoryActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public DocumentModel getArchivedVersionDocument(String versionId) {
        DocumentModel versionDoc = null;

        DocumentRef docRef = new IdRef(versionId);
        if (docRef != null) {
            versionDoc = documentManager.getDocument(docRef);
        }

        return versionDoc;
    }

    public boolean isCurrent(DocumentModel versionDoc,
            DocumentModel currentDoc) {

        String sourceDocId = "";

        if (currentDoc != null && versionDoc != null) {
            if (currentDoc.isProxy()) {
                sourceDocId = currentDoc.getSourceId();
            } else {
                sourceDocId = currentDoc.getId();
            }
            if (versionDoc.getId().equals(sourceDocId)) {
                return true;
            }
        }

        return false;
    }

    /*public String getLastComment(DocumentModel versionDoc,
            DocumentModel currentDoc) {
        String lastComment = "";
    
        //
    
        AuditReader reader = Framework.getService(AuditReader.class);
    
        // List<LogEntry> logEntries =
        // reader.getLogEntriesFor(versionDoc.getId());
    
        // Same method but with a filter
        FilterMapEntry eventIdFilter = new FilterMapEntry();
        eventIdFilter.setColumnName("eventId");
        eventIdFilter.setOperator("=");
        eventIdFilter.setQueryParameterName("eventId");
        eventIdFilter.setObject(DocumentEventTypes.DOCUMENT_CHECKEDIN);
    
         FilterMapEntry eventStartDateFilter = new FilterMapEntry();
        eventStartDateFilter.setColumnName("eventDate");
        eventStartDateFilter.setOperator(">=");
        eventStartDateFilter.setQueryParameterName("startDate");
        eventStartDateFilter.setObject(versionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_CREATED));
    
         FilterMapEntry eventEndDateFilter = new FilterMapEntry();
        eventEndDateFilter.setColumnName("eventDate");
        eventEndDateFilter.setOperator("<=");
        eventEndDateFilter.setQueryParameterName("endDate");
        eventEndDateFilter.setObject(versionDoc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_MODIFIED));
    
        Map<String, FilterMapEntry> filterMap = new HashMap<String, FilterMapEntry>();
        filterMap.put("eventId", eventIdFilter);
        // filterMap.put("eventStartDate", eventStartDateFilter);
        // filterMap.put("eventEndDate", eventEndDateFilter);
    
        List<LogEntry> logEntriesFiltered = reader.getLogEntriesFor(
                currentDoc.getId(), filterMap, true);
    
        lastComment = "size=" + logEntriesFiltered.size();
    
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(" FROM LogEntry log WHERE log.docUUID=:uuid ");
        queryStr.append(" AND log.eventId =:eventId ");
        queryStr.append(" AND log.eventDate >=:startDate ");
        queryStr.append(" AND log.eventDate <=:endDate ");
    
        return lastComment;
    }*/

}
