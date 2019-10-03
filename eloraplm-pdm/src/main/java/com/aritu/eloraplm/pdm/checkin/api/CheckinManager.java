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
package com.aritu.eloraplm.pdm.checkin.api;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.BomCharacteristicsValidatorException;
import com.aritu.eloraplm.exceptions.CheckinNotAllowedException;
import com.aritu.eloraplm.exceptions.DocumentNotCheckedOutException;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */

public interface CheckinManager {

    DocumentModel checkinDocument(DocumentModel doc, String checkinComment,
            boolean unlock) throws EloraException, CheckinNotAllowedException,
            DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException;

    DocumentModel checkinDocument(DocumentModel doc, String checkinComment,
            String clientName, String processReference, boolean unlock)
            throws EloraException, CheckinNotAllowedException,
            DocumentNotCheckedOutException,
            BomCharacteristicsValidatorException;

    void checkThatRelationIsAllowed(DocumentModel subjectDoc,
            String predicateUri, DocumentModel objectDoc, String quantity)
            throws EloraException;

}
