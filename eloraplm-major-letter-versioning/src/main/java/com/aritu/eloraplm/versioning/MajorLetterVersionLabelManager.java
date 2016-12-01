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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.versioning.VersioningService;

public class MajorLetterVersionLabelManager implements EloraVersionLabelService {

    protected static final String MAJOR_VERSION_PROP = "major_letter_versioning:major";

    protected static final String ZERO_MAJOR_CHAR = "_";

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
        String translatedMajor = (String) translateMajor(major);
        String minor = splittedVersionLabel[1];
        return translatedMajor + "." + minor;
    }

    @Override
    public Object translateMajor(Long major) {
        // Major can really be null when we copy & paste a document
        Integer i = major != null ? major.intValue() : 0;
        List<Integer> letterInts = new ArrayList<Integer>();
        String letteredMajor = "";
        int maxLetters = 27;
        Integer l = 0;
        Integer r = i;

        // Add letters until i is < 27
        r = i;
        while (r >= maxLetters) {
            l = (r / (maxLetters - 1));
            r = (r % (maxLetters - 1));
            letterInts.add(l);
        }

        // Add last letter
        letterInts.add(r);
        for (Integer letterInt : letterInts) {
            if (letterInt > 0 && letterInt < maxLetters) {
                letteredMajor += String.valueOf((char) (letterInt + 64));
            } else {
                letteredMajor += ZERO_MAJOR_CHAR;
            }
        }

        return letteredMajor;
    }

    @Override
    public Object translateMinor(Long minor) {
        if (minor == null) {
            // This can really happen when we copy & paste a document
            return "0";

        } else {
            return minor;
        }
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
            Object major = doc.getPropertyValue(MAJOR_VERSION_PROP);
            if (major == null || !(major instanceof String)) {
                Object defaultMajor = doc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP);
                if (defaultMajor == null || !(defaultMajor instanceof Long)) {
                    // This can really happen when we copy & paste a document
                    return ZERO_MAJOR_CHAR;
                }
                Long defaultMajorLong = (Long) defaultMajor;
                return translateMajor(defaultMajorLong).toString();
            } else {
                return major.toString();
            }
        } catch (PropertyNotFoundException e) {
            return doc.getPropertyValue(VersioningService.MAJOR_VERSION_PROP).toString();
        }

    }

    @Override
    public String getMinor(DocumentModel doc) {
        Object minor = doc.getPropertyValue(VersioningService.MINOR_VERSION_PROP);
        if (minor == null || !(minor instanceof Long)) {
            // This can really happen when we copy & paste a document
            return "0";
        } else {
            return minor.toString();
        }
    }

    @Override
    public void setMajor(DocumentModel doc, String major) {
        doc.setPropertyValue(MAJOR_VERSION_PROP, major);
    }

    @Override
    public void setMinor(DocumentModel doc, String minor) {
        doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP, minor);
    }

    @Override
    public String getZeroVersion() {
        return ZERO_MAJOR_CHAR + ".0";
    }

    @Override
    public String getZeroNextVersion() {
        return ZERO_MAJOR_CHAR + ".1";
    }
}