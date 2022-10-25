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
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.versioning.VersioningService;

public abstract class AbstractVersionLabelServiceImpl
        implements VersionLabelService {

    @Override
    public String translateVersionLabel(String versionLabel) {
        if (versionLabel == null || versionLabel.isEmpty()) {
            return versionLabel;
        }
        String[] splittedVersionLabel = versionLabel.split("\\.");
        if (splittedVersionLabel.length != 2) {
            return versionLabel;
        }
        Long major = Long.valueOf(splittedVersionLabel[0]);
        String translatedMajor = translateMajor(major);
        Long minor = Long.valueOf(splittedVersionLabel[1]);
        String translatedMinor = translateMinor(minor);

        return translatedMajor + getConfig(CFG_VERSION_SEPARATOR)
                + translatedMinor;
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
    public String getMajor(DocumentModel doc) {
        try {
            Object major = doc.getPropertyValue(VERSION_LABEL_MAJOR_PROP);
            if (major == null || !(major instanceof String)) {
                Object defaultMajor = doc.getPropertyValue(
                        VersioningService.MAJOR_VERSION_PROP);
                if (defaultMajor == null || !(defaultMajor instanceof Long)) {
                    // This can really happen when we copy & paste a document
                    return getConfig(CFG_ZERO_MAJOR_CHAR);
                }
                Long defaultMajorLong = (Long) defaultMajor;
                return translateMajor(defaultMajorLong).toString();
            } else {
                return major.toString();
            }
        } catch (PropertyNotFoundException e) {
            return doc.getPropertyValue(
                    VersioningService.MAJOR_VERSION_PROP).toString();
        }

    }

    @Override
    public String getMinor(DocumentModel doc) {
        try {
            Object minor = doc.getPropertyValue(VERSION_LABEL_MINOR_PROP);
            if (minor == null || !(minor instanceof String)) {
                Object defaultMinor = doc.getPropertyValue(
                        VersioningService.MINOR_VERSION_PROP);
                if (defaultMinor == null || !(defaultMinor instanceof Long)) {
                    // This can really happen when we copy & paste a document
                    return getConfig(CFG_ZERO_MINOR_CHAR);
                }
                Long defaultMinorLong = (Long) defaultMinor;
                return translateMinor(defaultMinorLong).toString();
            } else {
                return minor.toString();
            }
        } catch (PropertyNotFoundException e) {
            return doc.getPropertyValue(
                    VersioningService.MINOR_VERSION_PROP).toString();
        }

    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        String label = getMajor(doc) + getConfig(CFG_VERSION_SEPARATOR)
                + getMinor(doc);
        if (doc.isCheckedOut()) {
            label += getConfig(CFG_CHECKED_OUT_SYMBOL);
        }
        return label;
    }

    @Override
    public void setMajor(DocumentModel doc, String major) {
        doc.setPropertyValue(VERSION_LABEL_MAJOR_PROP, major);
    }

    @Override
    public void setMinor(DocumentModel doc, String minor) {
        doc.setPropertyValue(VERSION_LABEL_MINOR_PROP, minor);
    }

    @Override
    public String getZeroVersion() {
        return getConfig(CFG_ZERO_MAJOR_CHAR) + getConfig(CFG_VERSION_SEPARATOR)
                + getConfig(CFG_ZERO_MINOR_CHAR)
                + getConfig(CFG_CHECKED_OUT_SYMBOL);
    }

    @Override
    public String getZeroNextVersion() {
        return getConfig(CFG_ZERO_MAJOR_CHAR) + getConfig(CFG_VERSION_SEPARATOR)
                + getConfig(CFG_ZERO_NEXT_MINOR_CHAR);
    }

    @Override
    public String getCheckedOutSymbol() {
        return getConfig(CFG_CHECKED_OUT_SYMBOL);
    }

    @Override
    public String getVersionSeparator() {
        return getConfig(CFG_VERSION_SEPARATOR);
    }

    @Override
    public String getConfig(String key) {

        if (config.containsKey(key)) {
            return config.get(key);
        }
        return "";
    }
}