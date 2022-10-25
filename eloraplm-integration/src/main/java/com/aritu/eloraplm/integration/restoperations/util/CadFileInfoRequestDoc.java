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

import java.util.ArrayList;
import java.util.List;

import com.aritu.eloraplm.core.util.CadAttachmentInfo;
import com.aritu.eloraplm.core.util.EloraFileInfo;

/**
 * @author aritu
 *
 */
public class CadFileInfoRequestDoc {

    private EloraFileInfo contentFile;

    private EloraFileInfo eloraViewerFile;

    private List<CadAttachmentInfo> cadAttachments;

    public CadFileInfoRequestDoc() {
        cadAttachments = new ArrayList<CadAttachmentInfo>();
    }

    public EloraFileInfo getContentFile() {
        return contentFile;
    }

    public void setContentFile(int fileId, String fileName, String batch,
            String hash) {
        contentFile = new EloraFileInfo(fileId, fileName, batch, hash);
    }

    public EloraFileInfo getViewerFile() {
        return eloraViewerFile;
    }

    public void setViewerFile(int fileId, String fileName, String batch,
            String hash) {
        eloraViewerFile = new EloraFileInfo(fileId, fileName, batch, hash);
    }

    public List<CadAttachmentInfo> getCadAttachments() {
        return cadAttachments;
    }

    public void addCadAttachmentFile(int fileId, String fileName, String batch,
            String hash, String type) {
        cadAttachments.add(
                new CadAttachmentInfo(fileId, fileName, batch, hash, type));
    }
}
