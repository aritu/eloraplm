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
 * General Elora Exception //TODO:: begiratu hau beharrezkoa den??
 *
 * @author aritu
 *
 */
public class EloraExceptionWithStoredMessage extends Exception {

    private static final long serialVersionUID = 1L;

    private String storedMessage;

    /**
     * Constructs a EloraException with the exception message.
     *
     * @param message exception message
     */
    public EloraExceptionWithStoredMessage(String message,
            String storedMessage) {
        super(message);
        this.storedMessage = storedMessage;
    }

    /**
     * Constructs a EloraException with the exception cause.
     *
     * @param cause exception cause
     */
    public EloraExceptionWithStoredMessage(Throwable cause,
            String storedMessage) {
        super(cause);
        this.storedMessage = storedMessage;
    }

    /**
     * Constructs a EloraException with the exception message and cause.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public EloraExceptionWithStoredMessage(String message, Throwable cause,
            String storedMessage) {
        super(message, cause);
        this.storedMessage = storedMessage;
    }

    /**
     * Return the message stored in the exception
     *
     * @return
     */
    public String getStoredMessage() {
        return storedMessage;
    }

}
