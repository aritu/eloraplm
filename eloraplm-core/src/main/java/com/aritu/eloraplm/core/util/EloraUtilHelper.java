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

import java.text.Collator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for general utilities
 *
 * @author aritu
 *
 */
public class EloraUtilHelper {

    /**
     *
     */
    public EloraUtilHelper() {
    }

    /**
     * This method returns the specified Map sorted according to its value
     * elements in ascending order.
     *
     * @param map
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueAsc(
            Map<K, V> map) {
        return map.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Collator.getInstance())).collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * This method returns the specified Map sorted according to its value
     * elements in descending order.
     *
     * @param map
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDesc(
            Map<K, V> map) {

        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(
                Collator.getInstance().reversed())).collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
    }

}
