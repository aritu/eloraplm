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
package com.aritu.eloraplm.integration.restoperations.util;

import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author aritu
 *
 */
public class UpdateCadDocumentsRequestDoc extends CadFileInfoRequestDoc {

    private DocumentRef realRef;

    private Properties properties;

    public UpdateCadDocumentsRequestDoc(DocumentRef realRef) {
        super();
        this.realRef = realRef;
        properties = new Properties();
    }

    public DocumentRef getRealRef() {
        return realRef;
    }

    public void setRealRef(DocumentRef realRef) {
        this.realRef = realRef;
    }

    public Properties getProperties() {
        return properties;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

}
