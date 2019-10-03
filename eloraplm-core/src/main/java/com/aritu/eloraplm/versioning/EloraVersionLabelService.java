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

import org.nuxeo.ecm.core.api.DocumentModel;

public interface EloraVersionLabelService {

    /*
     * Used to translate the version label from the VersionModel objects.
     */
    String translateVersionLabel(String versionLabel);

    /*
     * Used from the listener to translate the major value.
     */
    Object translateMajor(Long major);

    /*
     * Used from the listener to translate the minor value.
     */
    Object translateMinor(Long minor);

    /*
     * Used to get the major value from the right schema.
     */
    String getMajor(DocumentModel doc);

    /*
     * Used to get the minor value from the right schema.
     */
    String getMinor(DocumentModel doc);

    /*
     * Used to set the major value in the right schema.
     */
    void setMajor(DocumentModel doc, String major);

    /*
     * Used to set the minor value in the right schema.
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
}