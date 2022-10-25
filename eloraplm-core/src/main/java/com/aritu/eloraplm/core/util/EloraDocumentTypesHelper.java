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

package com.aritu.eloraplm.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

public class EloraDocumentTypesHelper {

    public static Set<String> getFacetsByDocumentType(String docTypeName) {
        Set<String> facets = new HashSet<String>();
        DocumentType docType = getDocumentType(docTypeName);
        if (docType != null) {
            facets.addAll(docType.getFacets());
        }
        return facets;
    }

    // Returns the direct schemas associated with the specified docType. It
    // doesn't return the schemas associated with the facets associated to the
    // docType.
    public static Set<String> getSchemasByDocumentType(String docTypeName) {
        Set<String> schemas = new HashSet<String>();
        DocumentType docType = getDocumentType(docTypeName);
        if (docType != null) {
            schemas.addAll(Arrays.asList(docType.getSchemaNames()));
        }
        return schemas;
    }

    public static DocumentType getDocumentType(String docTypeName) {
        SchemaManager schemaManager = Framework.getLocalService(
                SchemaManager.class);
        if (schemaManager == null) {
            throw new NullPointerException("No registered SchemaManager");
        }
        DocumentType docType = schemaManager.getDocumentType(docTypeName);
        return docType;
    }

    public static List<String> getExtendedDocumentTypeNames(
            String docTypeName) {
        SchemaManager schemaManager = Framework.getLocalService(
                SchemaManager.class);
        if (schemaManager == null) {
            throw new NullPointerException("No registered SchemaManager");
        }
        Set<String> typeNames = schemaManager.getDocumentTypeNamesExtending(
                docTypeName);
        typeNames.add(docTypeName);
        return new ArrayList<>(typeNames);
    }

    public static String test(String superTypeName, String typeName) {
        return getDocumentType(superTypeName).isSuperTypeOf(
                getDocumentType(typeName)) ? superTypeName : null;
    }

}
