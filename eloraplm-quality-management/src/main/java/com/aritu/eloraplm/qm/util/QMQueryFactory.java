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
package com.aritu.eloraplm.qm.util;

import org.nuxeo.ecm.core.query.sql.NXQL;

import com.aritu.eloraplm.constants.QMDoctypeConstants;
import com.aritu.eloraplm.constants.QMMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Quality Management Query Factory class.
 *
 * @author aritu
 *
 */
public class QMQueryFactory {

    public static String getProcessesBySubjectQuery(String docUid)
            throws EloraException {

        String query = "SELECT " + NXQL.ECM_UUID + " FROM "
                + QMDoctypeConstants.VAL + ", " + QMDoctypeConstants.VER + ", "
                + QMDoctypeConstants.PPV + ", " + QMDoctypeConstants.PAP
                + " WHERE " + NXQL.ECM_PRIMARYTYPE + " IN ('"
                + QMDoctypeConstants.VAL + "', '" + QMDoctypeConstants.VER
                + "', '" + QMDoctypeConstants.PPV + "', '"
                + QMDoctypeConstants.PAP + "') AND "
                + QMMetadataConstants.QM_SUBJECT + " = '" + docUid + "'";

        return query;
    }

}
