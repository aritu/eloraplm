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
package com.aritu.eloraplm.lifecycles.util;

/**
 *
 * @author aritu
 *
 */
public class ErrorInfo {

    private String message;

    private String documentUid;

    public ErrorInfo(String message) {
        this.message = message;
    }

    public ErrorInfo(String message, String documentUid) {
        this.message = message;
        this.documentUid = documentUid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentUid() {
        return documentUid;
    }

    public void setDocumentUid(String documentUid) {
        this.documentUid = documentUid;
    }

}
