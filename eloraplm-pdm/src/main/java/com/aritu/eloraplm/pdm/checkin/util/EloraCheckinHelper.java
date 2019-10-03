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
package com.aritu.eloraplm.pdm.checkin.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ObjectResolverConstraint;
import com.aritu.eloraplm.constants.EloraFacetConstants;
import com.aritu.eloraplm.constants.EloraMetadataConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.core.util.restoperations.ValidationErrorItem;
import com.aritu.eloraplm.queries.EloraQueryFactory;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraCheckinHelper {

    private static final Log log = LogFactory.getLog(EloraCheckinHelper.class);

    public static List<ValidationErrorItem> validateCadDocument(
            DocumentModel doc, CoreSession session) {
        String logInitMsg = "[validateCadDocument] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();
        // Check title validation
        Serializable title = doc.getPropertyValue(
                NuxeoMetadataConstants.NX_DC_TITLE);
        if (title == null || title.toString().isEmpty()) {
            ValidationErrorItem errorItem = new ValidationErrorItem(
                    NuxeoMetadataConstants.NX_DC_TITLE, "NotNullConstraint");
            errorList.add(errorItem);

            log.trace(logInitMsg + "Validation error for document |"
                    + doc.getId() + "| in field |"
                    + NuxeoMetadataConstants.NX_DC_TITLE
                    + "|. It is a required field.");
        }

        // Check reference validation
        if (doc.hasFacet(EloraFacetConstants.FACET_CAD_DOCUMENT)) {
            Serializable reference = doc.getPropertyValue(
                    EloraMetadataConstants.ELORA_ELO_REFERENCE);
            if (reference == null || reference.toString().isEmpty()) {
                ValidationErrorItem errorItem = new ValidationErrorItem(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE,
                        "NotNullConstraint");
                errorList.add(errorItem);

                log.trace(logInitMsg + "Validation error for document |"
                        + doc.getId() + "| in field |"
                        + EloraMetadataConstants.ELORA_ELO_REFERENCE
                        + "|. It is a required field.");
            }
        }

        log.trace(logInitMsg + "--- EXIT --- ");

        return errorList;
    }

    /**
     * Validates the document according to the schema and populates the error
     * list
     *
     * @param doc
     * @return
     */
    public static List<ValidationErrorItem> checkForErrors(DocumentModel doc,
            DocumentValidationService validator, CoreSession session) {

        String logInitMsg = "[checkForErrors] ["
                + session.getPrincipal().getName() + "] ";
        log.trace(logInitMsg + "--- ENTER --- ");

        DocumentValidationReport report = validator.validate(doc);
        List<ValidationErrorItem> errorList = new ArrayList<ValidationErrorItem>();
        if (report.hasError()) {
            for (ConstraintViolation violation : report.asList()) {
                Constraint constraint = violation.getConstraint();
                String constraintName = constraint.getDescription().getName();
                for (ConstraintViolation.PathNode pathNode : violation.getPath()) {
                    // We don't want to block the operation
                    // when a resolver fails
                    if (!(constraint instanceof ObjectResolverConstraint)) {
                        String fieldName = pathNode.getField().getName().toString();
                        // TODO Se ha cambiado arriba ObjectResolver por
                        // ObjectResolverConstraint y creemos que las
                        // comprobaciones de dc que se hacen abajo ya no hacen
                        // falta. JIRA ELORAINT-60
                        if (fieldName != "dc:creator"
                                && fieldName != "dc:contributors"
                                && fieldName != "cad:authoringTool"
                                && fieldName != "cad:authoringToolVersion") {

                            errorList.add(new ValidationErrorItem(fieldName,
                                    constraintName));
                        }
                        log.trace(logInitMsg + "Validation error for document |"
                                + doc.getId() + "| in field |" + fieldName
                                + "| with constraint |" + constraintName
                                + "|.");
                    }
                }
            }
        }
        log.trace(logInitMsg + "--- EXIT --- ");

        return errorList;
    }

    public static List<ValidationErrorItem> checkUniqueReferenceByType(
            String wcUid, String reference, String type,
            List<ValidationErrorItem> errorList, CoreSession session) {

        if (reference != null && !reference.isEmpty()) {
            String query;
            if (wcUid == null) {
                query = EloraQueryFactory.getWcDocsByTypeAndReferenceQuery(type,
                        reference);
            } else {
                query = EloraQueryFactory.getWcDocsByTypeAndReferenceExcludingUidQuery(
                        type, reference, wcUid);
            }
            DocumentModelList uniqueErrorDocs = session.query(query);
            if (uniqueErrorDocs != null && !uniqueErrorDocs.isEmpty()) {

                errorList.add(new ValidationErrorItem(
                        EloraMetadataConstants.ELORA_ELO_REFERENCE,
                        "Same reference exist for a document of the same type."));
            }
        }
        return errorList;
    }

}
