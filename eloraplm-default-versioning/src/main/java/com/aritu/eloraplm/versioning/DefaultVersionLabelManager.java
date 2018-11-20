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
import org.nuxeo.ecm.core.versioning.VersioningService;

import com.aritu.eloraplm.versioning.EloraVersionLabelService;

public class DefaultVersionLabelManager implements EloraVersionLabelService {

    @Override
    public String translateVersionLabel(String versionLabel) {
        return versionLabel;
    }

    @Override
    public Object translateMinor(Long minor) {
        return minor;
    }

    @Override
    public Object translateMajor(Long major) {
        return major;
    }

    @Override
    public String getMajor(DocumentModel doc) {
        Object major = doc.getPropertyValue(
                VersioningService.MAJOR_VERSION_PROP);
        if (major == null || !(major instanceof Long)) {
            return "-";
        } else {
            return major.toString();
        }
    }

    @Override
    public String getMinor(DocumentModel doc) {
        Object minor = doc.getPropertyValue(
                VersioningService.MINOR_VERSION_PROP);
        if (minor == null || !(minor instanceof Long)) {
            return "-";
        } else {
            return minor.toString();
        }
    }

    @Override
    public void setMajor(DocumentModel doc, String major) {
        doc.setPropertyValue(VersioningService.MAJOR_VERSION_PROP, major);
    }

    @Override
    public void setMinor(DocumentModel doc, String minor) {
        doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP, minor);
    }

    @Override
    public int compare(String vl1, String vl2) {
        int result = vl1.compareTo(vl2);
        if (result == 0) {
            return 0;
        } else if (result > 0) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    public String getZeroVersion() {
        return "0.0+";
    }

    @Override
    public String getZeroNextVersion() {
        return "0.1";
    }

}