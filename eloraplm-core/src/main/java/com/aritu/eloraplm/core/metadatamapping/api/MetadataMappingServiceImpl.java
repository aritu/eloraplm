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
package com.aritu.eloraplm.core.metadatamapping.api;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import com.aritu.eloraplm.config.util.DocumentMetadataMappingConfig;
import com.aritu.eloraplm.config.util.DocumentMetadataMappingConfigHelper;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.constants.MetadataMappingEventNames;
import com.aritu.eloraplm.core.metadatamapping.methodexecuter.MethodExecuter;
import com.aritu.eloraplm.core.relations.util.EloraRelationHelper;
import com.aritu.eloraplm.core.util.EloraDecimalHelper;
import com.aritu.eloraplm.core.util.EloraDocumentHelper;
import com.aritu.eloraplm.core.util.EloraEventHelper;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class MetadataMappingServiceImpl implements MetadataMappingService {

    private static Log log = LogFactory.getLog(
            MetadataMappingServiceImpl.class);

    @Override
    public boolean isOriginDocument(DocumentModel doc) {
        String logInitMsg = "[isOriginDocument] ";
        boolean isOriginDocument = false;

        if (doc != null) {
            logInitMsg = logInitMsg + "["
                    + doc.getCoreSession().getPrincipal().getName() + "] ";
            log.trace(logInitMsg + "--- ENTER --- with doc type = |"
                    + doc.getType() + "|");

            if (DocumentMetadataMappingConfig.originDocumentTypes.contains(
                    doc.getType())) {
                isOriginDocument = true;
            }

            log.trace(logInitMsg + "--- EXIT --- with isOriginDocument = |"
                    + isOriginDocument + "|");
        } else {
            log.error(logInitMsg + "Received doc parameter is null");
        }

        return isOriginDocument;
    }

    @Override
    public boolean isDestinationDocument(DocumentModel doc) {
        String logInitMsg = "[isDestinationDocument] ";
        boolean isDestinationDocument = false;

        if (doc != null) {
            logInitMsg = logInitMsg + "["
                    + doc.getCoreSession().getPrincipal().getName() + "] ";
            log.trace(logInitMsg + "--- ENTER --- with doc type = |"
                    + doc.getType() + "|");

            if (DocumentMetadataMappingConfig.destinationDocumentTypes.contains(
                    doc.getType())) {
                isDestinationDocument = true;
            }

            log.trace(logInitMsg + "--- EXIT --- with isDestinationDocument = |"
                    + isDestinationDocument + "|");
        } else {
            log.error(logInitMsg + "Received doc parameter is null");
        }
        return isDestinationDocument;
    }

    @Override
    public void applyMappingFromOriginDocument(DocumentModel originDoc) {
        String logInitMsg = "[applyMappingFromOriginDocument] ";

        if (originDoc != null) {
            logInitMsg = logInitMsg + "["
                    + originDoc.getCoreSession().getPrincipal().getName()
                    + "] ";
            log.trace(logInitMsg + "--- ENTER --- with docId = |"
                    + originDoc.getId() + "|");

            // key: originDocId_relPredicate_relOrgRole
            Map<String, DocumentModelList> retrievedDestinationDocumentsList = new HashMap<String, DocumentModelList>();
            Map<String, DocumentModelList> retrievedOriginDocumentsList = new HashMap<String, DocumentModelList>();

            // handle setMetadataValue processes
            handleSetMetadataValueProcessesFromOriginDocument(originDoc,
                    retrievedDestinationDocumentsList,
                    retrievedOriginDocumentsList);

            // handle execMethod processes
            handleExecMethodProcessesFromOriginDocument(originDoc,
                    retrievedDestinationDocumentsList,
                    retrievedOriginDocumentsList);

            log.trace(logInitMsg + "--- EXIT ---");
        } else {
            log.error(logInitMsg + "Received originDoc parameter is null");
        }
    }

    @Override
    public void applyMappingToDestinationDocument(
            DocumentModel destinationDoc) {
        String logInitMsg = "[applyMappingToDestinationDocument] ";

        if (destinationDoc != null) {
            logInitMsg = logInitMsg + "["
                    + destinationDoc.getCoreSession().getPrincipal().getName()
                    + "] ";

            log.trace(logInitMsg + "--- ENTER --- with docId = |"
                    + destinationDoc.getId() + "|");

            // key: destinationDocId_relPredicate_relOrgRole
            Map<String, DocumentModelList> retrievedOriginDocumentsList = new HashMap<String, DocumentModelList>();

            // handle setMetadataValue processes
            handleSetMetadataValueProcessesToDestinationDocument(destinationDoc,
                    retrievedOriginDocumentsList);

            // handle execMethod processes
            handleExecMethodProcessesToDestinationDocument(destinationDoc,
                    retrievedOriginDocumentsList);

            log.trace(logInitMsg + "--- EXIT ---");
        } else {
            log.error(logInitMsg + "Received destinationDoc parameter is null");
        }
    }

    protected void handleSetMetadataValueProcessesFromOriginDocument(
            DocumentModel originDoc,
            Map<String, DocumentModelList> retrievedDestinationDocumentsList,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleSetMetadataValueProcessesFromOriginDocument] ["
                + originDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");
        try {

            // Get configuration for setMetadataValue process
            EloraConfigTable setMetadataValueConfig = DocumentMetadataMappingConfigHelper.getDocumentMetadataMappingConfigForOrgDocType(
                    originDoc.getType(),
                    EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_SET_METADATA_VALUE,
                    false);

            if (setMetadataValueConfig.size() == 0) {
                log.trace(logInitMsg + "Nothing to be handled.");
            } else {
                log.trace(logInitMsg + "|" + setMetadataValueConfig.size()
                        + "| configurations found.");

                // Iterate over configuration rows
                Map<String, EloraConfigRow> setMetadataValueConfigRows = setMetadataValueConfig.getRows();
                for (Map.Entry<String, EloraConfigRow> entry : setMetadataValueConfigRows.entrySet()) {
                    try {
                        String key = entry.getKey();
                        EloraConfigRow configRow = entry.getValue();
                        log.trace(logInitMsg + "Processing entry |" + key
                                + "| for document id =|" + originDoc.getId()
                                + "|");

                        // Retrieve destination documents
                        String relPredicate = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE).toString();
                        if (relPredicate.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String relOrgRole = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String dstDocType = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        DocumentModelList destinationDocuments = getDestinationDocuments(
                                originDoc, relPredicate, relOrgRole, dstDocType,
                                retrievedDestinationDocumentsList);
                        log.trace(logInitMsg + "|" + destinationDocuments.size()
                                + "| destination documents found.");

                        if (destinationDocuments.size() > 0) {
                            // Handle each destination document
                            for (DocumentModel destinationDocument : destinationDocuments) {
                                handleSetMetadataValueProcessToDestinationDocument(
                                        destinationDocument, relPredicate,
                                        relOrgRole, configRow,
                                        retrievedOriginDocumentsList);
                            }
                        }
                    } catch (Exception e) {
                        log.error(logInitMsg + e.getMessage(), e);
                    }
                }
            }

        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling setMetadataValue process to destination document. Destination document id = |"
                    + originDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void handleSetMetadataValueProcessToDestinationDocument(
            DocumentModel destinationDoc, String relPredicate,
            String relOrgRole, EloraConfigRow configRow,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleSetMetadataValueProcessToDestinationDocument] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {

            CoreSession session = destinationDoc.getCoreSession();
            boolean destinationDocModified = false;

            String orgDocType = getPropertyValueFromCongifRow(configRow,
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE).toString();
            if (relOrgRole.isEmpty()) {
                log.error(logInitMsg + "Property |"
                        + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                        + "| is empty in the configuration.");
            }

            String dstMetadata = getPropertyValueFromCongifRow(configRow,
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA).toString();
            if (dstMetadata.isEmpty()) {
                log.error(logInitMsg + "Property |"
                        + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA
                        + "| is empty in the configuration.");

            }

            // Get origin documents
            DocumentModelList originDocuments = getOriginDocuments(
                    destinationDoc, relPredicate, relOrgRole, orgDocType,
                    retrievedOriginDocumentsList);

            // Process origin documents
            if (originDocuments != null && originDocuments.size() > 0) {
                Serializable metadataValue = getMetadataValueFromOriginDocuments(
                        originDocuments, configRow, session);

                boolean docModified = setMetadataValueToDestinationDocument(
                        destinationDoc, dstMetadata, metadataValue, configRow);
                if (docModified) {
                    destinationDocModified = true;
                }
            }

            // if something updated, save destination document
            if (destinationDocModified) {
                String appliedMetadataMappingId = getPropertyValueFromCongifRow(
                        configRow, EloraConfigConstants.PROP_ID).toString();
                fireMetadataMappingAppliedEvent(destinationDoc,
                        appliedMetadataMappingId);
                EloraDocumentHelper.disableVersioningDocument(destinationDoc);
                session.saveDocument(destinationDoc);
                session.save();
            }

        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling setMetadataValue process to destination document. Destination document id = |"
                    + destinationDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void handleSetMetadataValueProcessesToDestinationDocument(
            DocumentModel destinationDoc,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleSetMetadataValueProcessesToDestinationDocument] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {
            // Get configuration for setMetadataValue process
            EloraConfigTable setMetadataValueConfig = DocumentMetadataMappingConfigHelper.getDocumentMetadataMappingConfigForDestDocType(
                    destinationDoc.getType(),
                    EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_SET_METADATA_VALUE,
                    false);

            if (setMetadataValueConfig.size() == 0) {
                log.trace(logInitMsg + "Nothing to be handled.");
            } else {
                log.trace(logInitMsg + "|" + setMetadataValueConfig.size()
                        + "| configurations found.");
                CoreSession session = destinationDoc.getCoreSession();
                boolean destinationDocModified = false;

                // Iterate over configuration rows
                Map<String, EloraConfigRow> setMetadataValueConfigRows = setMetadataValueConfig.getRows();

                String appliedMappingIds = "";
                for (Map.Entry<String, EloraConfigRow> entry : setMetadataValueConfigRows.entrySet()) {
                    try {
                        String key = entry.getKey();
                        EloraConfigRow configRow = entry.getValue();
                        log.trace(logInitMsg + "Processing | " + key
                                + "| configuration entry.");

                        // Retrieve origin documents
                        String relPredicate = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE).toString();
                        if (relPredicate.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String relOrgRole = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String orgDocType = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        // Get origin documents
                        DocumentModelList originDocuments = getOriginDocuments(
                                destinationDoc, relPredicate, relOrgRole,
                                orgDocType, retrievedOriginDocumentsList);

                        // Process origin documents
                        if (originDocuments != null
                                && originDocuments.size() > 0) {
                            Serializable metadataValue = getMetadataValueFromOriginDocuments(
                                    originDocuments, configRow, session);

                            String dstMetadata = getPropertyValueFromCongifRow(
                                    configRow,
                                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA).toString();
                            if (dstMetadata.isEmpty()) {
                                log.error(logInitMsg + "Property |"
                                        + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA
                                        + "| is empty in the configuration.");
                                break;
                            }

                            boolean docModified = setMetadataValueToDestinationDocument(
                                    destinationDoc, dstMetadata, metadataValue,
                                    configRow);
                            if (docModified) {
                                destinationDocModified = true;
                                String appliedMetadataMappingId = getPropertyValueFromCongifRow(
                                        configRow,
                                        EloraConfigConstants.PROP_ID).toString();
                                if (!appliedMappingIds.isEmpty()) {
                                    appliedMappingIds += ", ";
                                }
                                appliedMappingIds += appliedMetadataMappingId;
                            }
                        }

                    } catch (EloraException e) {
                        log.error(logInitMsg + e.getMessage());
                    } catch (Exception e) {
                        log.error(logInitMsg + e.getMessage(), e);
                    }
                }

                // if something updated, save destination document
                if (destinationDocModified) {
                    fireMetadataMappingAppliedEvent(destinationDoc,
                            appliedMappingIds);
                    EloraDocumentHelper.disableVersioningDocument(
                            destinationDoc);
                    session.saveDocument(destinationDoc);
                    session.save();
                }
            }
        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling setMetadataValue process to destination document. Destination document id = |"
                    + destinationDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private Object getPropertyValueFromCongifRow(EloraConfigRow configRow,
            String propertyName) {

        Object propertyValue = new Object();

        if (configRow.getProperty(propertyName) != null) {
            propertyValue = configRow.getProperty(propertyName).toString();
        }

        return propertyValue;
    }

    private DocumentModelList getOriginDocuments(DocumentModel destinationDoc,
            String relPredicate, String relOrgRole, String orgDocType,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[getOriginDocuments] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";

        DocumentModelList originDocuments = new DocumentModelListImpl();

        if (retrievedOriginDocumentsList.containsKey(destinationDoc.getId()
                + "_" + relPredicate + "_" + relOrgRole)) {
            originDocuments = retrievedOriginDocumentsList.get(
                    destinationDoc.getId() + "_" + relPredicate + "_"
                            + relOrgRole);
        } else {

            switch (relOrgRole) {
            case EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_OBJECT:
                originDocuments = EloraRelationHelper.getObjectDocumentsNotExcludedFromMapping(
                        destinationDoc, new ResourceImpl(relPredicate));
                break;

            case EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_SUBJECT:
                originDocuments = EloraRelationHelper.getSubjectDocumentsNotExcludedFromMapping(
                        new ResourceImpl(relPredicate), destinationDoc);
                break;

            default:
                break;
            }

            retrievedOriginDocumentsList.put(destinationDoc.getId() + "_"
                    + relPredicate + "_" + relOrgRole, originDocuments);
        }

        DocumentModelList originDocumentsByDocType = new DocumentModelListImpl();

        if (originDocuments != null && originDocuments.size() > 0) {
            for (DocumentModel originDocument : originDocuments) {
                if (originDocument.getType().equals(orgDocType)) {
                    originDocumentsByDocType.add(originDocument);
                }
            }
        }

        log.trace(logInitMsg + "|" + originDocumentsByDocType.size()
                + "| origin documents retrieved for relPredicate |"
                + relPredicate + "| and relOrgRole |" + relOrgRole
                + "| and orgDocType = |" + orgDocType + "|");

        return originDocumentsByDocType;
    }

    private DocumentModelList getDestinationDocuments(DocumentModel originDoc,
            String relPredicate, String relOrgRole, String dstDocType,
            Map<String, DocumentModelList> retrievedDestinationDocumentsList) {
        String logInitMsg = "[getDestinationDocuments] ["
                + originDoc.getCoreSession().getPrincipal().getName() + "] ";

        DocumentModelList destinationDocuments = new DocumentModelListImpl();

        if (retrievedDestinationDocumentsList.containsKey(
                originDoc.getId() + "_" + relPredicate + "_" + relOrgRole)) {
            destinationDocuments = retrievedDestinationDocumentsList.get(
                    originDoc.getId() + "_" + relPredicate + "_" + relOrgRole);
        } else {

            switch (relOrgRole) {
            case EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_OBJECT:
                destinationDocuments = EloraRelationHelper.getSubjectDocumentsNotExcludedFromMapping(
                        new ResourceImpl(relPredicate), originDoc);

                break;

            case EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE_SUBJECT:
                destinationDocuments = EloraRelationHelper.getObjectDocumentsNotExcludedFromMapping(
                        originDoc, new ResourceImpl(relPredicate));
                break;

            default:
                break;
            }

            retrievedDestinationDocumentsList.put(
                    originDoc.getId() + "_" + relPredicate + "_" + relOrgRole,
                    destinationDocuments);
        }

        DocumentModelList destinationDocumentsByDocType = new DocumentModelListImpl();

        if (destinationDocuments != null && destinationDocuments.size() > 0) {
            for (DocumentModel destinationDocument : destinationDocuments) {
                if (destinationDocument.getType().equals(dstDocType)) {
                    destinationDocumentsByDocType.add(destinationDocument);
                }
            }
        }

        log.trace(logInitMsg + "|" + destinationDocumentsByDocType.size()
                + "| destination documents retrieved for relPredicate |"
                + relPredicate + "| and relOrgRole |" + relOrgRole
                + "| and dstDocType = |" + dstDocType + "|");

        return destinationDocumentsByDocType;
    }

    private Serializable getMetadataValueFromOriginDocuments(
            DocumentModelList originDocuments, EloraConfigRow configRow,
            CoreSession session) throws EloraException {
        String logInitMsg = "[getMetadataValueFromOriginDocuments] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        Serializable metadataValue = null;

        if (originDocuments.size() == 1) {
            DocumentModel originDocument = originDocuments.get(0);
            metadataValue = getMetadataValueFromSingleOriginDocument(
                    originDocument, configRow, session);
        } else {
            metadataValue = getMetadataValueFromOriginDocumentsList(
                    originDocuments, configRow, session);
        }

        log.trace(logInitMsg + "--- EXIT --- with metadataValue = |"
                + metadataValue + "|");

        return metadataValue;
    }

    private Serializable getMetadataValueFromSingleOriginDocument(
            DocumentModel originDocument, EloraConfigRow configRow,
            CoreSession session) throws EloraException {
        String logInitMsg = "[getMetadataValueFromSingleOriginDocument] ["
                + session.getPrincipal().getName() + "] ";

        log.trace(logInitMsg + "Processing origin document |"
                + originDocument.getId() + "|");

        String orgDocType = getPropertyValueFromCongifRow(configRow,
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE).toString();
        if (orgDocType.isEmpty()) {
            String errorMsg = "Property |"
                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                    + "| is empty in the configuration.";
            throw new EloraException(errorMsg);
        }
        if (!originDocument.getType().equals(orgDocType)) {
            String errorMsg = "Origin document type |"
                    + originDocument.getType()
                    + "| is different from configured "
                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                    + " property value |" + orgDocType + "|.";
            throw new EloraException(errorMsg);
        }

        String orgMetadata = getPropertyValueFromCongifRow(configRow,
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_METADATA).toString();
        if (orgMetadata.isEmpty()) {
            String errorMsg = "Property |"
                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_METADATA
                    + "| is empty in the configuration.";
            throw new EloraException(errorMsg);
        }

        Serializable metadataValue = null;
        try {
            metadataValue = originDocument.getPropertyValue(orgMetadata);
            log.trace(logInitMsg + "|" + orgMetadata + "| metadata value is |"
                    + metadataValue + "| for origin document id |"
                    + originDocument.getId() + "|");

        } catch (PropertyNotFoundException e) {
            String errorMsg = "Exception retrieving |" + orgMetadata
                    + "| metadata from origin document id = |"
                    + originDocument.getId() + "|";
            throw new EloraException(errorMsg);
        }

        return metadataValue;
    }

    private Serializable getMetadataValueFromOriginDocumentsList(
            DocumentModelList originDocuments, EloraConfigRow configRow,
            CoreSession session) throws EloraException {
        String logInitMsg = "[getMetadataValueFromOriginDocumentsList] ["
                + session.getPrincipal().getName() + "] ";

        String dstConsolidationMode = getPropertyValueFromCongifRow(configRow,
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE).toString();

        if (!dstConsolidationMode.isEmpty() && !dstConsolidationMode.equals(
                EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE_SUM)) {
            String errorMsg = "|"
                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE
                    + "| property value |" + dstConsolidationMode
                    + "| is not valid.";
            throw new EloraException(errorMsg);
        }
        log.trace(logInitMsg + "dstConsolidationMode = |" + dstConsolidationMode
                + "|");

        Serializable consolidatedMetadataValue = null;
        BigDecimal consolidatedMetadataValueBD = new BigDecimal(0);
        Integer consolidatedMetadataValueI = new Integer(0);
        Long consolidatedMetadataValueL = new Long(0);

        for (DocumentModel originDocument : originDocuments) {

            Serializable metadataValue = getMetadataValueFromSingleOriginDocument(
                    originDocument, configRow, session);

            if (dstConsolidationMode.equals(
                    EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_CONSOLIDATION_MODE_SUM)) {
                if (metadataValue instanceof String) {
                    BigDecimal metadataValueBD = new BigDecimal(
                            (String) metadataValue);
                    consolidatedMetadataValueBD = consolidatedMetadataValueBD.add(
                            metadataValueBD);
                    consolidatedMetadataValue = EloraDecimalHelper.fromDecimalToStandard(
                            consolidatedMetadataValueBD);
                } else if (metadataValue instanceof Integer) {
                    consolidatedMetadataValueI = consolidatedMetadataValueI
                            + (Integer) metadataValue;
                    consolidatedMetadataValue = consolidatedMetadataValueI.toString();
                } else if (metadataValue instanceof Long) {
                    consolidatedMetadataValueL = consolidatedMetadataValueL
                            + (Long) metadataValue;
                    consolidatedMetadataValue = consolidatedMetadataValueL.toString();
                } else {
                    String errorMsg = "Metadata value class |"
                            + metadataValue.getClass()
                            + "| is not handled for |" + dstConsolidationMode
                            + "| consolidation mode.";
                    throw new EloraException(errorMsg);
                }
            } else {
                consolidatedMetadataValue = metadataValue;
            }
        }
        return consolidatedMetadataValue;
    }

    private boolean setMetadataValueToDestinationDocument(
            DocumentModel destinationDoc, String dstMetadata,
            Serializable metadataValue, EloraConfigRow configRow)
            throws EloraException {

        String logInitMsg = "[setMetadataValueToDestinationDocument] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";

        boolean destinationDocModified = false;

        // Check dstMetadataOverwrite property value before updating destination
        // document
        String dstMetadataOverwrite = getPropertyValueFromCongifRow(configRow,
                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE).toString();
        log.trace(logInitMsg + "|"
                + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE
                + "| property value = |" + dstMetadataOverwrite + "|");
        // Overwrite always
        if (dstMetadataOverwrite.equals(
                EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_Y)) {
            destinationDoc.setPropertyValue(dstMetadata, metadataValue);
            destinationDocModified = true;
            log.trace(logInitMsg + "|" + dstMetadata
                    + "| metadata set with value |" + metadataValue
                    + "| in destination document id = |"
                    + destinationDoc.getId() + "|");
        }
        // Overwrite only if Origin Not Empty
        else if (dstMetadataOverwrite.equals(
                EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_Y_ONE)) {
            if (metadataValue != null) {
                destinationDoc.setPropertyValue(dstMetadata, metadataValue);
                destinationDocModified = true;
                log.trace(logInitMsg + "|" + dstMetadata
                        + "| metadata set with value |" + metadataValue
                        + "| in destination document id = |"
                        + destinationDoc.getId() + "|");
            }
        } else if (dstMetadataOverwrite.equals(
                EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE_N)) {
            try {
                Serializable destinationDocMetadataValue = destinationDoc.getPropertyValue(
                        dstMetadata);
                if (destinationDocMetadataValue == null
                        || destinationDocMetadataValue.toString().isEmpty()) {
                    destinationDoc.setPropertyValue(dstMetadata, metadataValue);
                    destinationDocModified = true;
                    log.trace(logInitMsg + "|" + dstMetadata
                            + "| metadata set with value |" + metadataValue
                            + "| in destination document id = |"
                            + destinationDoc.getId() + "|");
                }
            } catch (PropertyNotFoundException e) {
                String errorMsg = "Exception retrieving |" + dstMetadata
                        + "| metadata from destination document id = |"
                        + destinationDoc.getId() + "|";
                throw new EloraException(errorMsg);
            }
        } else {
            String errorMsg = "|"
                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_METADATA_OVERWRITE
                    + "| property value |" + dstMetadataOverwrite
                    + "| is not valid.";
            throw new EloraException(errorMsg);
        }

        return destinationDocModified;
    }

    protected void handleExecMethodProcessesFromOriginDocument(
            DocumentModel originDoc,
            Map<String, DocumentModelList> retrievedDestinationDocumentsList,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleExecMethodProcessesFromOriginDocument] ["
                + originDoc.getCoreSession().getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {

            // Get configuration for execMethod process
            EloraConfigTable execMethodConfig = DocumentMetadataMappingConfigHelper.getDocumentMetadataMappingConfigForOrgDocType(
                    originDoc.getType(),
                    EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_EXEC_METHOD,
                    false);

            if (execMethodConfig.size() == 0) {
                log.trace(logInitMsg + "Nothing to be handled.");
            } else {
                log.trace(logInitMsg + "|" + execMethodConfig.size()
                        + "| configurations found.");

                // Iterate over configuration rows
                Map<String, EloraConfigRow> execMethodConfigRows = execMethodConfig.getRows();
                for (Map.Entry<String, EloraConfigRow> entry : execMethodConfigRows.entrySet()) {
                    try {
                        String key = entry.getKey();
                        EloraConfigRow configRow = entry.getValue();
                        log.trace(logInitMsg + "Processing entry |" + key
                                + "| for document id =|" + originDoc.getId()
                                + "|");

                        // Retrieve destination documents
                        String relPredicate = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE).toString();
                        if (relPredicate.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String relOrgRole = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String dstDocType = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_DOC_TYPE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        DocumentModelList destinationDocuments = getDestinationDocuments(
                                originDoc, relPredicate, relOrgRole, dstDocType,
                                retrievedDestinationDocumentsList);
                        log.trace(logInitMsg + "|" + destinationDocuments.size()
                                + "| destination documents found.");

                        if (destinationDocuments.size() > 0) {
                            // Handle each destination document
                            for (DocumentModel destinationDocument : destinationDocuments) {
                                handleExecMethodProcessToDestinationDocument(
                                        destinationDocument, relPredicate,
                                        relOrgRole, configRow,
                                        retrievedOriginDocumentsList);
                            }
                        }
                    } catch (Exception e) {
                        log.error(logInitMsg + e.getMessage(), e);
                    }
                }
            }

        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling execMethod process from origin document. Origin document id = |"
                    + originDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void handleExecMethodProcessToDestinationDocument(
            DocumentModel destinationDoc, String relPredicate,
            String relOrgRole, EloraConfigRow configRow,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleExecMethodProcessToDestinationDocument] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {

            CoreSession session = destinationDoc.getCoreSession();

            String orgDocType = getPropertyValueFromCongifRow(configRow,
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE).toString();
            if (relOrgRole.isEmpty()) {
                log.error(logInitMsg + "Property |"
                        + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                        + "| is empty in the configuration.");
            }

            // Get origin documents
            DocumentModelList originDocuments = getOriginDocuments(
                    destinationDoc, relPredicate, relOrgRole, orgDocType,
                    retrievedOriginDocumentsList);

            // Process origin documents
            String dstExecMethodName = getPropertyValueFromCongifRow(configRow,
                    EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_EXEC_METHOD_NAME).toString();
            if (dstExecMethodName.isEmpty()) {
                log.error(logInitMsg + "Property |"
                        + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_EXEC_METHOD_NAME
                        + "| is empty in the configuration.");
            }

            applyExecMethod(destinationDoc, originDocuments, dstExecMethodName,
                    true, session);

        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling execMethod process to destination document. Destination document id = |"
                    + destinationDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    protected void handleExecMethodProcessesToDestinationDocument(
            DocumentModel destinationDoc,
            Map<String, DocumentModelList> retrievedOriginDocumentsList) {
        String logInitMsg = "[handleExecMethodProcessesToDestinationDocument] ["
                + destinationDoc.getCoreSession().getPrincipal().getName()
                + "] ";
        log.trace(logInitMsg + "--- ENTER ---");

        try {
            // Get configuration for execMethod process
            EloraConfigTable execMethodConfig = DocumentMetadataMappingConfigHelper.getDocumentMetadataMappingConfigForDestDocType(
                    destinationDoc.getType(),
                    EloraConfigConstants.VAL_DOCUMENT_METADATA_MAPPING_DST_PROCESS_EXEC_METHOD,
                    false);

            if (execMethodConfig.size() == 0) {
                log.trace(logInitMsg + "Nothing to be handled.");
            } else {
                log.trace(logInitMsg + "|" + execMethodConfig.size()
                        + "| configurations found.");
                CoreSession session = destinationDoc.getCoreSession();

                // Iterate over configuration rows
                Map<String, EloraConfigRow> execMethodConfigRows = execMethodConfig.getRows();
                for (Map.Entry<String, EloraConfigRow> entry : execMethodConfigRows.entrySet()) {
                    try {
                        String key = entry.getKey();
                        EloraConfigRow configRow = entry.getValue();
                        log.trace(logInitMsg + "Processing | " + key
                                + "| configuration entry.");

                        // Retrieve origin documents
                        String relPredicate = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE).toString();
                        if (relPredicate.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_PREDICATE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String relOrgRole = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_REL_ORG_ROLE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        String orgDocType = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE).toString();
                        if (relOrgRole.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_ORG_DOC_TYPE
                                    + "| is empty in the configuration.");
                            break;
                        }

                        // Get origin documents
                        DocumentModelList originDocuments = getOriginDocuments(
                                destinationDoc, relPredicate, relOrgRole,
                                orgDocType, retrievedOriginDocumentsList);

                        // Process origin documents
                        String dstExecMethodName = getPropertyValueFromCongifRow(
                                configRow,
                                EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_EXEC_METHOD_NAME).toString();
                        if (dstExecMethodName.isEmpty()) {
                            log.error(logInitMsg + "Property |"
                                    + EloraConfigConstants.PROD_DOCUMENT_METADATA_MAPPING_DST_EXEC_METHOD_NAME
                                    + "| is empty in the configuration.");
                            break;
                        }

                        applyExecMethod(destinationDoc, originDocuments,
                                dstExecMethodName, false, session);

                    } catch (EloraException e) {
                        log.error(logInitMsg + e.getMessage());
                    } catch (Exception e) {
                        log.error(logInitMsg + e.getMessage(), e);
                    }
                }

            }
        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Error handling execMethod process to destination document. Destination document id = |"
                    + destinationDoc.getId());
            log.error(logInitMsg + e.getMessage(), e);
        }

        log.trace(logInitMsg + "--- EXIT ---");
    }

    private void applyExecMethod(DocumentModel destinationDoc,
            DocumentModelList originDocuments, String dstExecMethodName,
            Boolean isFiredFromOrigin, CoreSession session)
            throws EloraException {

        String logInitMsg = "[applyExecMethodToOriginDocuments] ["
                + session.getPrincipal().getName() + "] ";

        try {

            Method method;
            MethodExecuter methodExec = destinationDoc.getAdapter(
                    MethodExecuter.class);
            if (methodExec != null) {
                method = methodExec.getClass().getMethod(dstExecMethodName,
                        new Class[] { DocumentModelList.class, Boolean.class });
                method.invoke(methodExec, originDocuments, isFiredFromOrigin);
                fireMetadataMappingAppliedEvent(destinationDoc,
                        dstExecMethodName);
            } else {
                throw new EloraException(
                        "Error calling to method |" + dstExecMethodName
                                + "|. No MethodExecuterAdapter found.");
            }
        } catch (SecurityException | NoSuchMethodException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            log.error(logInitMsg + e.getMessage(), e);
            throw new EloraException(
                    "Error calling to method |" + dstExecMethodName + "|.", e);
        }
    }

    private void fireMetadataMappingAppliedEvent(DocumentModel doc,
            String comment) {
        EloraEventHelper.fireEvent(
                MetadataMappingEventNames.METADATA_MAPPING_APPLIED, doc,
                comment);
    }

}
