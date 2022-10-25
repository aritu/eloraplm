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
import static org.nuxeo.ecm.core.api.VersioningOption.NONE;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraGeneralConstants;

public class VersionLabelListener implements EventListener {

    private static Log log = LogFactory.getLog(VersionLabelListener.class);

    private VersionLabelService vls;

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

                if (docEventContext.getPrincipal() instanceof SystemPrincipal) {
                    return;
                }

                if (doc.getContextData(
                        VersionLabelService.OPT_DISABLE_VERSION_LABEL_TRANSLATION) != null
                        && (Boolean) doc.getContextData(
                                VersionLabelService.OPT_DISABLE_VERSION_LABEL_TRANSLATION)) {
                    return;
                }

                // Get the selected versioning option (NONE/MINOR/MAJOR)
                VersioningOption versioningOption = (VersioningOption) doc.getContextData(
                        VersioningService.VERSIONING_OPTION);
                boolean isDirty = doc.isDirty();
                boolean updateEloraVersionLabel = false;
                // Check if update version label requirement is specified in the
                // document context
                if (doc.getContextData(
                        VersionLabelService.OPT_UPDATE_ELORA_VERSION_LABEL) != null) {
                    updateEloraVersionLabel = (boolean) doc.getContextData(
                            VersionLabelService.OPT_UPDATE_ELORA_VERSION_LABEL);
                }

                // If versioning option is NONE or document is not dirty AND
                // there is not requirement to update the version label, there
                // is not need to update the version label.
                if ((versioningOption == null || versioningOption == NONE
                        || !isDirty) && !updateEloraVersionLabel) {
                    return;
                }

                CoreSession session = docEventContext.getCoreSession();

                try {
                    vls = Framework.getService(VersionLabelService.class);

                    String newMajor = null;
                    String newMinor = null;

                    // If the version label is in the context data, use it
                    if (doc.getContextData(
                            EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CHECKIN) != null
                            || doc.getContextData(
                                    EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CREATE) != null) {
                        if (doc.getPropertyValue(
                                VersioningService.MAJOR_VERSION_PROP) != null
                                && doc.getPropertyValue(
                                        VersioningService.MINOR_VERSION_PROP) != null) {
                            newMajor = vls.translateMajor(
                                    (long) doc.getPropertyValue(
                                            VersioningService.MAJOR_VERSION_PROP));
                            newMinor = vls.translateMinor(
                                    (long) doc.getPropertyValue(
                                            VersioningService.MINOR_VERSION_PROP));
                        }
                    }
                    // if newMajor and newMinor are still null, calculate them
                    // in function of the versioningOption
                    if (newMajor == null || newMinor == null) {
                        newMajor = getTranslatedMajor(session, doc,
                                versioningOption);
                        newMinor = getTranslatedMinor(session, doc,
                                versioningOption);
                    }

                    vls.setMajor(doc, newMajor);
                    vls.setMinor(doc, newMinor);

                } catch (Exception e) {
                    log.error("Unknown exception", e);
                    e.printStackTrace();
                }
            }
        }
    }

    private String getTranslatedMajor(CoreSession session, DocumentModel doc,
            VersioningOption versioningOption) {

        return vls.translateMajor(getMajor(session, doc, versioningOption));
    }

    private Long getMajor(CoreSession session, DocumentModel doc,
            VersioningOption versioningOption) {
        Object currentMajorObject = doc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP);
        Object lastMajorObject = null;
        DocumentModel lastDocumentVersion = session.getLastDocumentVersion(
                doc.getRef());
        if (lastDocumentVersion != null) {
            lastMajorObject = lastDocumentVersion.getPropertyValue(
                    VersioningService.MAJOR_VERSION_PROP);
        } else {
            lastMajorObject = currentMajorObject;
        }

        // Current major can really be null when we copy and paste a
        // document
        Long lastMajor = lastMajorObject == null ? 0
                : Long.valueOf(lastMajorObject.toString());
        Long currentMajor = currentMajorObject == null ? 0
                : Long.valueOf(currentMajorObject.toString());

        // Translate major
        if (versioningOption == MAJOR) {
            return lastMajor + 1;
        } else {
            if (lastMajor > currentMajor) {
                return lastMajor;
            } else {
                return currentMajor;
            }
        }
    }

    private String getTranslatedMinor(CoreSession session, DocumentModel doc,
            VersioningOption versioningOption) {

        return vls.translateMinor(getMinor(session, doc, versioningOption));
    }

    private Long getMinor(CoreSession session, DocumentModel doc,
            VersioningOption versioningOption) {

        if (versioningOption == MAJOR) {
            return 0L;
        } else {
            Object currentMinorObject = doc.getPropertyValue(
                    VersioningService.MINOR_VERSION_PROP);

            Object lastMinorObject = null;
            DocumentModel lastDocumentVersion = session.getLastDocumentVersion(
                    doc.getRef());
            if (lastDocumentVersion != null) {
                lastMinorObject = lastDocumentVersion.getPropertyValue(
                        VersioningService.MINOR_VERSION_PROP);
            } else {
                lastMinorObject = currentMinorObject;
            }

            // Current minor can really be null when we copy and paste a
            // document
            Long lastMinor = lastMinorObject == null ? 0
                    : Long.valueOf(lastMinorObject.toString());

            return lastMinor + 1;
        }
    }

    private boolean isSkippedDocument(DocumentModel doc) {
        return doc instanceof ShallowDocumentModel
                || doc.hasFacet(HIDDEN_IN_NAVIGATION) || !doc.isVersionable();
    }

    private boolean isEventHandled(Event event) {
        for (String eventName : getHandledEventsName()) {
            if (eventName.equals(event.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getHandledEventsName() {
        return Arrays.asList(DocumentEventTypes.BEFORE_DOC_UPDATE);
    }
}
