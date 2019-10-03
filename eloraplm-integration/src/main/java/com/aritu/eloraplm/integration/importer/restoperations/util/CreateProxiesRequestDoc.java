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
package com.aritu.eloraplm.integration.importer.restoperations.util;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "docRef", "destinationRef" })
public class CreateProxiesRequestDoc {

    private DocumentRef docRef;

    private DocumentRef destinationRef;

    public CreateProxiesRequestDoc(DocumentRef docRef,
            DocumentRef destinationRef) {
        this.docRef = docRef;
        this.destinationRef = destinationRef;
    }

    public DocumentRef getDocRef() {
        return docRef;
    }

    public void setDocRef(DocumentRef docRef) {
        this.docRef = docRef;
    }

    public DocumentRef getDestinationRef() {
        return destinationRef;
    }

    public void setDestinationRef(DocumentRef destinationRef) {
        this.destinationRef = destinationRef;
    }
}
