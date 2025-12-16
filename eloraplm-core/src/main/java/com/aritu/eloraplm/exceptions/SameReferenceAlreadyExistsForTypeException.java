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
package com.aritu.eloraplm.exceptions;

/**
 * Exception thrown when the reference we are trying to give to a document
 * already exists for another document of the same type.
 *
 * It is an EloraException that can be used when we want to differentiate it
 * from other exceptions to show a more concrete message to the user.
 *
 * @author aritu
 *
 */
public class SameReferenceAlreadyExistsForTypeException extends EloraException {

    private static final long serialVersionUID = 1L;

    private String reference;

    private String type;

    public SameReferenceAlreadyExistsForTypeException(String reference,
            String type) {
        super("The reference '" + reference
                + "' has already been used by another document of the type '"
                + type + "'.");
        this.reference = reference;
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public String getType() {
        return type;
    }
}
