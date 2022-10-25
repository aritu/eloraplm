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

/**
 * @author aritu
 *
 */
public class CadAttachmentInfo extends EloraFileInfo {

    private String type;

    public CadAttachmentInfo(int fileId, String batch, String hash,
            String type) {
        this(fileId, null, batch, hash, type);
    }

    public CadAttachmentInfo(int fileId, String fileName, String batch,
            String hash, String type) {
        super(fileId, fileName, batch, hash);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
