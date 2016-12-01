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
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class EloraFileInfo {
    private int fileId;

    private String fileName;

    private String batch;

    private String hash;

    public EloraFileInfo(int fileId, String batch, String hash) {
        this(fileId, null, batch, hash);
    }

    public EloraFileInfo(int fileId, String fileName, String batch, String hash) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.batch = batch;
        this.hash = hash;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
