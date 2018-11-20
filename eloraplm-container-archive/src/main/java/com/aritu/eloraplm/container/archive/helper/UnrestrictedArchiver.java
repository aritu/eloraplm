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
package com.aritu.eloraplm.container.archive.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class UnrestrictedArchiver extends UnrestrictedSessionRunner {

    private DocumentRef docRef;

    private DocumentRef destFolderRef;

    public UnrestrictedArchiver(DocumentRef docRef, DocumentRef destFolderRef,
            CoreSession session) {
        super(session);
        this.docRef = docRef;
        this.destFolderRef = destFolderRef;
    }

    @Override
    public void run() {
        session.move(docRef, destFolderRef, null);
        // session.save();
    }

}
