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

package com.aritu.eloraplm.versioning;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface VersionLabelService {

    public static final String VERSION_LABEL_MAJOR_PROP = "version_label:major";

    public static final String VERSION_LABEL_MINOR_PROP = "version_label:minor";

    public static final String OPT_DISABLE_VERSION_LABEL_TRANSLATION = "disableVersionLabelTranslation";

    public static final String OPT_UPDATE_ELORA_VERSION_LABEL = "updateEloraVersionLabel";

    public static String CFG_ZERO_MAJOR_CHAR = "ZERO_MAJOR_CHAR";

    public static String CFG_ZERO_MINOR_CHAR = "ZERO_MINOR_CHAR";

    public static String CFG_ZERO_NEXT_MINOR_CHAR = "ZERO_NEXT_MINOR_CHAR";

    public static String CFG_CHECKED_OUT_SYMBOL = "CHECKED_OUT_SYMBOL";

    public static String CFG_VERSION_SEPARATOR = "VERSION_SEPARATOR";

    Map<String, String> config = new HashMap<String, String>();

    /*
     * Get configuration value identified by the key.
     */
    String getConfig(String key);

    /*
     * Used to translate the version label from the VersionModel objects.
     */
    String translateVersionLabel(String versionLabel);

    /*
     * Used from the listener to translate the major value.
     */
    String translateMajor(Long major);

    /*
     * Used from the listener to translate the minor value.
     */
    String translateMinor(Long minor);

    /*
     * Used to get the major value from the version_label schema, or translate it in the moment.
     */
    String getMajor(DocumentModel doc);

    /*
     * Used to get the minor value from the version_label schema, or translate it in the moment.
     */
    String getMinor(DocumentModel doc);

    /*
     * Used to get the full version label (with checked out symbol).
     */
    String getVersionLabel(DocumentModel doc);

    /*
     * Used to set the major value in the version_label schema.
     */
    void setMajor(DocumentModel doc, String major);

    /*
     * Used to set the minor value in the version_label schema.
     */
    void setMinor(DocumentModel doc, String minor);

    /*
     * If vl1 is greater than vl2, returns 1; if vl2 is greater, returns 2; if equal, returns 0.
     */
    int compare(String vl1, String vl2);

    /*
     * Used to get the zero version (0.0)
     */
    String getZeroVersion();

    /*
     * Used to get the next to zero version (0.1)
     */
    String getZeroNextVersion();

    /*
     * Return the symbol that identifies checked out documents (by default '+')
     */
    String getCheckedOutSymbol();

    /*
     * Return the separator of major and minor versions (by default '.')
     */
    String getVersionSeparator();
}
