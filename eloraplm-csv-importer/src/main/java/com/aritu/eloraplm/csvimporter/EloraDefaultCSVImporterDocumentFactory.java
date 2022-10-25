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

package com.aritu.eloraplm.csvimporter;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraGeneralConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoSchemaConstants;
import com.aritu.eloraplm.core.util.CheckInInfoHelper;
import com.aritu.eloraplm.core.util.EloraDocumentTypesHelper;
import com.aritu.eloraplm.core.util.EloraUnitConversionHelper;
import com.aritu.eloraplm.core.util.ReviewInfoHelper;
import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.pdm.checkin.api.CheckinManager;
import com.aritu.eloraplm.versioning.VersionLabelService;

public class EloraDefaultCSVImporterDocumentFactory
        implements EloraCSVImporterDocumentFactory {

    private static final Log log = LogFactory.getLog(
            EloraDefaultCSVImporterDocumentFactory.class);

    private static final long serialVersionUID = 1L;

    public static final List<String> IGNORE_FIELDS_ON_UPDATE = Arrays.asList(
            NXQL.ECM_LIFECYCLESTATE);

    @Override
    public void createDocument(CoreSession session, String parentPath,
            String name, String type, boolean doCheckin, String checkinComment,
            Map<String, Serializable> values) {

        String logInitMsg = "[createDocument] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "--- ENTER --- ");

        // Start a transation. In this transaction there are two actions: create
        // the document and do checkin of the document. If it is not possible to
        // do checkin of the document, the creation should also be rollbacked.
        try {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            values = prepareValues(session, values, type);

            DocumentModel doc = session.createDocumentModel(parentPath, name,
                    type);
            for (Map.Entry<String, Serializable> entry : values.entrySet()) {

                if (NXQL.ECM_LIFECYCLESTATE.equals(entry.getKey())) {
                    doc.putContextData(INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                            entry.getValue());
                } else {
                    if (EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY.equals(
                            entry.getKey())
                            || EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE.equals(
                                    entry.getKey())
                            || EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER.equals(
                                    entry.getKey())
                            || EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED.equals(
                                    entry.getKey())) {
                        // don't set these values, since they should only be
                        // applied if doCheckin = true
                    } else {
                        doc.setPropertyValue(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (doc.hasSchema(NuxeoSchemaConstants.DUBLINCORE)) {
                // Disable dublincore listener
                doc.putContextData(
                        DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                        Boolean.TRUE);

                // Store in context creator and created properties. They are
                // used by
                // StateLogListener.
                String creator = (String) doc.getPropertyValue(
                        NuxeoMetadataConstants.NX_DC_CREATOR);
                doc.putContextData(
                        EloraGeneralConstants.CONTEXT_KEY_CREATOR_USER,
                        creator);
                Date created = (Date) values.get("dc:created");
                doc.putContextData(
                        EloraGeneralConstants.CONTEXT_KEY_CREATED_DATE,
                        created);
            }

            String majorVersion = null;
            String minorVersion = null;
            if (values.containsKey(VersioningService.MAJOR_VERSION_PROP)) {
                majorVersion = String.valueOf(
                        values.get(VersioningService.MAJOR_VERSION_PROP));
            }
            if (values.containsKey(VersioningService.MINOR_VERSION_PROP)) {
                minorVersion = String.valueOf(
                        values.get(VersioningService.MINOR_VERSION_PROP));
            }
            if (majorVersion != null && minorVersion != null) {
                doc.putContextData(
                        VersionLabelService.OPT_UPDATE_ELORA_VERSION_LABEL,
                        Boolean.TRUE);
                doc.putContextData(
                        EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CREATE,
                        majorVersion + "." + minorVersion);
            }

            if (!doCheckin) {
                doc.putContextData(EloraGeneralConstants.CONTEXT_SKIP_LOCK,
                        true);
            }

            doc.putContextData(ScopeType.REQUEST,
                    VersioningService.SKIP_VERSIONING, true);

            // Create the document
            doc = session.createDocument(doc);

            if (!doCheckin) {
                // if not checkin is required, save the document
                doc = session.saveDocument(doc);
            }

            // if checkin is required, do the checkin action
            if (doCheckin) {
                try {
                    // Complete CHECKIN INFO metadata
                    if (doc.hasFacet(
                            EloraFacetConstants.FACET_STORE_CHECKIN_INFO)) {
                        String lastCheckedInBy = null;
                        if (values.containsKey(
                                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY)) {
                            lastCheckedInBy = (String) values.get(
                                    EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_BY);
                        }
                        if (StringUtils.isBlank(lastCheckedInBy)) {
                            lastCheckedInBy = session.getPrincipal().toString();
                        }
                        Date lastCheckedInDate = null;
                        if (values.containsKey(
                                EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE)) {
                            lastCheckedInDate = (Date) values.get(
                                    EloraMetadataConstants.ELORA_CHECKIN_LAST_CHECKED_IN_DATE);
                        }
                        if (lastCheckedInDate == null) {
                            lastCheckedInDate = new Date();
                        }
                        CheckInInfoHelper.setLastCheckInInfoProperties(doc,
                                lastCheckedInBy, lastCheckedInDate);

                        doc.putContextData(
                                EloraGeneralConstants.CONTEXT_SKIP_CHECKIN_INFO,
                                Boolean.TRUE);
                    }

                    // Complete REVIEW INFO metadata
                    if (doc.hasFacet(
                            EloraFacetConstants.FACET_STORE_REVIEW_INFO)) {
                        String lastReviewer = null;
                        if (values.containsKey(
                                EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER)) {
                            lastReviewer = (String) values.get(
                                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWER);
                        }
                        if (StringUtils.isBlank(lastReviewer)) {
                            lastReviewer = session.getPrincipal().toString();
                        }
                        Date lastReviewed = null;
                        if (values.containsKey(
                                EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED)) {
                            lastReviewed = (Date) values.get(
                                    EloraMetadataConstants.ELORA_REVIEW_LAST_REVIEWED);
                        }
                        if (lastReviewed == null) {
                            lastReviewed = new Date();
                        }
                        ReviewInfoHelper.setLastReviewInfoPropertiesByState(doc,
                                lastReviewer, lastReviewed,
                                doc.getCurrentLifeCycleState(), session);
                        doc.putContextData(
                                EloraGeneralConstants.CONTEXT_SKIP_REVIEW_INFO,
                                Boolean.TRUE);
                    }

                    // Disable Viewer File creation since viewer files should
                    // not be created during importation.
                    doc.putContextData(
                            EloraGeneralConstants.CONTEXT_SKIP_VIEWER_FILE_CREATION,
                            Boolean.TRUE);

                    // If major and minor versions are set, force the version
                    // label value on checkin
                    if (majorVersion != null && minorVersion != null) {
                        doc.putContextData(
                                EloraGeneralConstants.CONTEXT_KEY_DOC_VERSION_LABEL_ON_CHECKIN,
                                majorVersion + "." + minorVersion);
                    }

                    // Do CHECKIN of the document
                    CheckinManager checkinManager = Framework.getService(
                            CheckinManager.class);
                    checkinManager.checkinDocument(doc, checkinComment, null,
                            null, true);

                } catch (EloraException | CheckinNotAllowedException
                        | DocumentNotCheckedOutException
                        | BomCharacteristicsValidatorException e) {
                    throw new NuxeoException(
                            "Error doing checkin of the document |"
                                    + doc.getId() + "|.",
                            e);
                }
            }

        } catch (Exception e) {
            log.error(logInitMsg + e.getMessage(), e);
            TransactionHelper.setTransactionRollbackOnly();
            throw new NuxeoException("Error creating the document.", e);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            log.trace(logInitMsg + "--- EXIT --- ");
        }
    }

    protected Map<String, Serializable> prepareValues(CoreSession session,
            Map<String, Serializable> values, String docType) {

        // Initialize Dublin Core metadata values: creator, created date, last
        // contributor, last modification date, contributors

        if (EloraDocumentTypesHelper.getSchemasByDocumentType(docType).contains(
                NuxeoSchemaConstants.DUBLINCORE)) {
            String creator = null;

            // dc:creator
            if (values.containsKey(NuxeoMetadataConstants.NX_DC_CREATOR)) {
                creator = (String) values.get(
                        NuxeoMetadataConstants.NX_DC_CREATOR);
            }
            if (StringUtils.isBlank(creator)) {
                creator = session.getPrincipal().toString();
                values.put(NuxeoMetadataConstants.NX_DC_CREATOR, creator);
            }

            // dc:created
            Date created = null;
            if (values.containsKey(NuxeoMetadataConstants.NX_DC_CREATED)) {
                created = (Date) values.get(
                        NuxeoMetadataConstants.NX_DC_CREATED);
            }
            if (created == null) {
                created = new Date();
                values.put(NuxeoMetadataConstants.NX_DC_CREATED, created);
            }

            // dc:lastContributor
            String lastContributor = null;
            if (values.containsKey(
                    NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR)) {
                lastContributor = (String) values.get(
                        NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR);
            }
            if (StringUtils.isBlank(lastContributor)) {
                if (!StringUtils.isBlank(creator)) {
                    lastContributor = creator;
                } else {
                    lastContributor = session.getPrincipal().toString();
                }
                values.put(NuxeoMetadataConstants.NX_DC_LAST_CONTRIBUTOR,
                        lastContributor);
            }

            // dc:modified
            Date modified = null;
            if (values.containsKey(NuxeoMetadataConstants.NX_DC_MODIFIED)) {
                modified = (Date) values.get(
                        NuxeoMetadataConstants.NX_DC_MODIFIED);
            }
            if (modified == null) {
                modified = created;
                values.put(NuxeoMetadataConstants.NX_DC_MODIFIED, modified);
            }

            // dc:contributors
            if (creator != null || lastContributor != null) {
                String[] contributorsArray = (String[]) values.get(
                        NuxeoMetadataConstants.NX_DC_CONTRIBUTORS);
                List<String> contributors = contributorsArray == null
                        ? new ArrayList<>()
                        : new ArrayList<>(Arrays.asList(contributorsArray));
                if (StringUtils.isNotBlank(creator)
                        && !contributors.contains(creator)) {
                    contributors.add(creator);
                }
                if (StringUtils.isNotBlank(lastContributor)
                        && !contributors.contains(lastContributor)) {
                    contributors.add(lastContributor);
                }
                values.put(NuxeoMetadataConstants.NX_DC_CONTRIBUTORS,
                        contributors.toArray(new String[contributors.size()]));
            }
        }

        // Round decimal values
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            String property = entry.getKey();
            if (EloraUnitConversionHelper.isDecimalProperty(property)) {
                String value = (String) entry.getValue();
                if (value != null && !value.isEmpty() && !value.equals("0")) {
                    value = EloraUnitConversionHelper.roundDecimalValue(
                            property, value);
                    values.put(property, value);
                }
            }
        }

        return values;
    }

    @Override
    public void updateDocument(CoreSession session, DocumentRef docRef,
            Map<String, Serializable> values) {
        DocumentModel doc = session.getDocument(docRef);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            if (!IGNORE_FIELDS_ON_UPDATE.contains(entry.getKey())) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        }
        session.saveDocument(doc);
    }

    @Override
    public boolean exists(CoreSession session, String parentPath, String name,
            String type, Map<String, Serializable> values) {
        String targetPath = new Path(parentPath).append(name).toString();
        DocumentRef docRef = new PathRef(targetPath);
        return session.exists(docRef);
    }
}
