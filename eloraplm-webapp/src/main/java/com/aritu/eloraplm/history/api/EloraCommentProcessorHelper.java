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
package com.aritu.eloraplm.history.api;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;

import com.aritu.eloraplm.constants.EloraDocumentEventNames;

/**
 *
 * Fixes for CommentProcessorHelper because the original class does not check if
 * the part after ':' in the comment is a real document reference or just a
 * comment. It fails with PostgreSQL and UUID type id columns.
 *
 * @author aritu
 *
 */
public class EloraCommentProcessorHelper extends CommentProcessorHelper {

    public EloraCommentProcessorHelper(CoreSession documentManager) {
        super(documentManager);
    }

    @Override
    public String getLogComment(LogEntry entry) {
        String oldComment = entry.getComment();
        if (oldComment == null) {
            return null;
        }

        String newComment = oldComment;
        boolean targetDocExists = false;
        String[] split = oldComment.split(":");
        if (split.length >= 2) {
            String strDocRef = split[1];
            // Check that it is a valid UUID
            if (IdUtils.isValidUUID(strDocRef)) {
                DocumentRef docRef = new IdRef(strDocRef);
                targetDocExists = documentManager.exists(docRef);
            }
        }

        if (targetDocExists) {
            String eventId = entry.getEventId();
            // update comment
            if (DocumentEventTypes.DOCUMENT_DUPLICATED.equals(eventId)) {
                newComment = "audit.duplicated_to";
            } else if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(
                    eventId)) {
                newComment = "audit.copied_from";
            } else if (DocumentEventTypes.DOCUMENT_MOVED.equals(eventId)) {
                newComment = "audit.moved_from";
            } else if (EloraDocumentEventNames.DOCUMENT_ELORA_ROOT_FOLDER_SWITCHED.equals(
                    eventId)) {
                newComment = "audit.switch_elora_root_folder.origin_doc";
            }
        }

        return newComment;
    }
}
