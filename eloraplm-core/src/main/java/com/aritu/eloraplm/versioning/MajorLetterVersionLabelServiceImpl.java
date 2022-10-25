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
import java.util.Map;

public class MajorLetterVersionLabelServiceImpl
        extends AbstractVersionLabelServiceImpl {

    public MajorLetterVersionLabelServiceImpl() {
        initConfig();
    }

    private Map<String, String> initConfig() {

        config.put(CFG_ZERO_MAJOR_CHAR, "_");
        config.put(CFG_ZERO_MINOR_CHAR, "0");
        config.put(CFG_ZERO_NEXT_MINOR_CHAR, "1");
        config.put(CFG_CHECKED_OUT_SYMBOL, "+");
        config.put(CFG_VERSION_SEPARATOR, ".");

        return config;
    }

    @Override
    public String translateMajor(Long major) {
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
                letteredMajor += getConfig(CFG_ZERO_MAJOR_CHAR);
            }
        }

        return letteredMajor;
    }

    @Override
    public String translateMinor(Long minor) {
        if (minor == null) {
            // This can really happen when we copy & paste a document
            return "0";

        } else {
            return String.valueOf(minor);
        }
    }

}