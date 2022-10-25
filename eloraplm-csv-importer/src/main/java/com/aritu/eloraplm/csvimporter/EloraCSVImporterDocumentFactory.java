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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public interface EloraCSVImporterDocumentFactory extends Serializable {

    public void createDocument(CoreSession session, String parentPath,
            String name, String type, boolean doCheckin, String checkinComment,
            Map<String, Serializable> values);

    public void updateDocument(CoreSession session, DocumentRef docRef,
            Map<String, Serializable> values);

    public boolean exists(CoreSession session, String parentPath, String name,
            String type, Map<String, Serializable> values);

}
