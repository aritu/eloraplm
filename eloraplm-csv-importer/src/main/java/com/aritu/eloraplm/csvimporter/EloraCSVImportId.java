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

package com.aritu.eloraplm.csvimporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to compute a unique id for an import task.
 *
 * @since 5.7.3
 */
public class EloraCSVImportId {

    private static final Log log = LogFactory.getLog(EloraCSVImportId.class);

    private EloraCSVImportId() {
        // utility class
    }

    public static String create(String repositoryName, String path,
            File csvFile, File relationsCsvFile, File proxiesCsvFile) {

        File computeDigestFile = (csvFile != null) ? csvFile
                : ((relationsCsvFile != null) ? relationsCsvFile
                        : proxiesCsvFile);

        return create(repositoryName, path, computeDigest(computeDigestFile));
    }

    public static String create(String repositoryName, String path,
            String csvBlobDigest) {
        return repositoryName + ':' + path + ":csvImport:" + csvBlobDigest;
    }

    protected static String computeDigest(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            log.error(e, e);
            return "";
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
