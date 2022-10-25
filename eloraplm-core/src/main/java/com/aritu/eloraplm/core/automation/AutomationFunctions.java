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
package com.aritu.eloraplm.core.automation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;

/**
 *
 * @author aritu
 *
 */
public class AutomationFunctions extends CoreFunctions {

    public <T> List<T> removeDuplicatedElementsFromList(List<T> list) {

        if (list == null) {
            throw new IllegalArgumentException(
                    "First parameter must not be null");
        }

        // With this change we don't preserve insertion ordering
        Set<T> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);

        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> removeValuesFromList(List<T> list, Object... values) {

        if (list == null) {
            throw new IllegalArgumentException(
                    "First parameter must not be null");
        }

        for (Object value : values) {
            if (value == null) {
                continue;
            }

            if (value instanceof Object[]) {
                for (Object subValue : (Object[]) value) {
                    if (subValue != null) {
                        list.remove(subValue);
                    }
                }
                continue;
            }

            if (value instanceof Collection) {
                for (Object subValue : (Collection<Object>) value) {
                    if (subValue != null) {
                        list.remove(subValue);
                    }
                }
                continue;
            }

            list.remove(value);

        }

        return list;
    }

    public Object[] listToArray(List<Object> list) {
        return list.toArray(new Object[list.size()]);
    }

}
