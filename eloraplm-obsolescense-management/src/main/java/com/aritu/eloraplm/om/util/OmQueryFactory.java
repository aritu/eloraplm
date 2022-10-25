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
package com.aritu.eloraplm.om.util;

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.OmDoctypeConstants;
import com.aritu.eloraplm.constants.OmMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 * @author aritu
 *
 */
public class OmQueryFactory {

    public static String getAnarchicImpactedDocUidsQuery(String omProcessUid)
            throws EloraException {

        String pfx = OmMetadataConstants.OM_IMPACTED_DOC_LIST;

        String query = String.format(
                "SELECT " + pfx + "/*1/" + OmMetadataConstants.OM_PDL_UID
                        + " FROM " + OmDoctypeConstants.OM_PROCESS + " WHERE "
                        + NXQL.ECM_PRIMARYTYPE + " = '"
                        + OmDoctypeConstants.OM_PROCESS + "' AND "
                        + NXQL.ECM_UUID + " = '%s' AND " + pfx + "/*1/"
                        + OmMetadataConstants.OM_PDL_IS_ANARCHIC + " = 1",
                omProcessUid);

        return query;
    }

    public static String getImpactedDocUidsQuery(String omProcessUid)
            throws EloraException {

        String pfx = OmMetadataConstants.OM_IMPACTED_DOC_LIST;

        String query = String.format("SELECT " + pfx + "/*1/"
                + OmMetadataConstants.OM_PDL_UID + " FROM "
                + OmDoctypeConstants.OM_PROCESS + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + OmDoctypeConstants.OM_PROCESS
                + "' AND " + NXQL.ECM_UUID + " = '%s'", omProcessUid);

        return query;
    }

    public static String getProcessedDocUidsQuery(String omProcessUid)
            throws EloraException {

        String pfx = OmMetadataConstants.OM_PROCESSED_DOC_LIST;

        String query = String.format("SELECT " + pfx + "/*1/"
                + OmMetadataConstants.OM_PDL_UID + " FROM "
                + OmDoctypeConstants.OM_PROCESS + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + OmDoctypeConstants.OM_PROCESS
                + "' AND " + NXQL.ECM_UUID + " = '%s'", omProcessUid);

        return query;
    }

    public static String getOmProcessesBySourceDocQuery(String docId)
            throws EloraException {

        String query = String.format("SELECT DISTINCT " + NXQL.ECM_UUID
                + " FROM " + OmDoctypeConstants.OM_PROCESS + " WHERE "
                + NXQL.ECM_PRIMARYTYPE + " = '" + OmDoctypeConstants.OM_PROCESS
                + "' AND " + OmMetadataConstants.OM_SOURCE_DOC_REAL_UID
                + " = '%s'", docId);

        return query;
    }

    public static String getOmProcessesByProcessedDocQuery(String docId)
            throws EloraException {

        String pfx = OmMetadataConstants.OM_PROCESSED_DOC_LIST;

        String query = String.format(
                "SELECT DISTINCT " + NXQL.ECM_UUID + " FROM "
                        + OmDoctypeConstants.OM_PROCESS + " WHERE "
                        + NXQL.ECM_PRIMARYTYPE + " = '"
                        + OmDoctypeConstants.OM_PROCESS + "' AND " + pfx
                        + "/*1/" + OmMetadataConstants.OM_PDL_UID + " = '%s'",
                docId);

        return query;
    }

}
