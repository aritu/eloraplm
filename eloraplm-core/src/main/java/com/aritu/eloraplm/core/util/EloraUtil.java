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

import java.util.Map;
import java.util.Set;

/**
 *
 * @author aritu
 *
 */
public class EloraUtil {

    public static String getLastKeyFromMap(Map<String, String> mapList) {

        String lastKey = null;
        if (mapList != null && !mapList.isEmpty()) {
            Set<String> mapListKeys = mapList.keySet();
            Object[] mapListKeysArray = mapListKeys.toArray();

            lastKey = (String) mapListKeysArray[mapList.size() - 1];
        }

        return lastKey;
    }

}
