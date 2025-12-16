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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author aritu
 *
 */
public interface MetadataMappingService {

    /**
     * Checks if specified document is an origin document in metadata mapping
     * configuration.
     *
     * @param doc
     * @return
     */
    public boolean isOriginDocument(DocumentModel doc);

    /**
     * Checks if specified document is a destination document in metadata
     * mapping configuration.
     *
     * @param doc
     * @return
     */
    public boolean isDestinationDocument(DocumentModel doc);

    /**
     * Applies metadata mapping to the specified origin document.
     *
     * @param originDoc
     */
    public void applyMappingFromOriginDocument(DocumentModel originDoc);

    /**
     * Applies metadata mapping to the specified destination document.
     *
     * @param destinationDoc
     */
    public void applyMappingToDestinationDocument(DocumentModel destinationDoc);

}
