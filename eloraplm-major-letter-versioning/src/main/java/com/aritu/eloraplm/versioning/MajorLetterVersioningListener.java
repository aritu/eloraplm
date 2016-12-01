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

package com.aritu.eloraplm.versioning;

import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

public class MajorLetterVersioningListener implements EventListener {

    protected static Log log = LogFactory.getLog(MajorLetterVersioningListener.class);

    protected EloraVersionLabelService versionLabelService;

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            if (isEventHandled(event)) {
                DocumentEventContext docEventContext = (DocumentEventContext) eventContext;
                DocumentModel doc = docEventContext.getSourceDocument();
                if (isSkippedDocument(doc)) {
                    return;
                }

                // TODO: Hau hobeto ulertu.
                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                CoreSession session = docEventContext.getCoreSession();

                try {
                    versionLabelService = Framework.getService(EloraVersionLabelService.class);

                    String majorLetterVersionLabel = getTranslatedMajor(
                            session, doc);
                    if (majorLetterVersionLabel != null) {
                        versionLabelService.setMajor(doc,
                                majorLetterVersionLabel);
                    }

                } catch (Exception e) {
                    log.error("Unknown exception", e);
                    e.printStackTrace();
                }
            }
        }
    }

    protected String getTranslatedMajor(CoreSession session, DocumentModel doc) {
        // Get the selected versioning option (NONE/MINOR/MAJOR)
        VersioningOption versioningOption = (VersioningOption) doc.getContextData(VersioningService.VERSIONING_OPTION);

        Object currentMajorObject = doc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP);
        Object lastMajorObject = null;
        DocumentModel lastDocumentVersion = session.getLastDocumentVersion(doc.getRef());
        if (lastDocumentVersion != null) {
            lastMajorObject = lastDocumentVersion.getPropertyValue(VersioningService.MAJOR_VERSION_PROP);
        } else {
            lastMajorObject = currentMajorObject;
        }

        // Current major can really be null when we copy and paste a document
        Long lastMajor = lastMajorObject == null ? 0
                : Long.valueOf(lastMajorObject.toString());
        Long currentMajor = lastMajorObject == null ? 0
                : Long.valueOf(currentMajorObject.toString());

        // Change major version to letter
        boolean dirty = doc.isDirty();
        if (dirty) {
            if (versioningOption == MAJOR) {
                return (String) versionLabelService.translateMajor(lastMajor + 1);
            } else if (lastMajor > currentMajor) {
                return (String) versionLabelService.translateMajor(lastMajor);
            }
        }
        return null;
    }

    protected boolean isSkippedDocument(DocumentModel doc) {
        // Not really interested if document
        // cannot be reconnected
        // or if not visible
        return doc instanceof ShallowDocumentModel
                || doc.hasFacet(HIDDEN_IN_NAVIGATION) || !doc.isVersionable();
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
        return Arrays.asList(BEFORE_DOC_UPDATE);
    }
}
