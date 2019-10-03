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
package com.aritu.eloraplm.config.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author aritu
 *
 */
public class EloraConfigTable {

    private Map<String, EloraConfigRow> rows;

    public EloraConfigTable() {
        rows = new HashMap<String, EloraConfigRow>();
    }

    public Map<String, EloraConfigRow> getRows() {
        return rows;
    }

    public void setRows(Map<String, EloraConfigRow> rows) {
        this.rows = rows;
    }

    public boolean containsKey(String key) {
        return rows.containsKey(key);
    }

    public EloraConfigRow getRow(String key) {
        return rows.get(key);
    }

    public void mergeWithTable(EloraConfigTable table) {
        rows.putAll(table.getRows());
    }

    public void setRow(String key, EloraConfigRow value) {
        rows.put(key, value);
    }

    public EloraConfigRow getFirst() {
        return rows.entrySet().iterator().next().getValue();
    }

    public int size() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public Set<String> getKeys() {
        return rows.keySet();
    }

    public Collection<EloraConfigRow> getValues() {
        return rows.values();
    }

    /**
     * This method returns a list with the distinct values that the specified
     * property has in this configuration table. propertyName has in this
     * configuration.
     *
     * @param propertyName
     * @return
     */
    public ArrayList<String> extractConfigTablePropertyValuesAsList(
            String propertyName) {

        ArrayList<String> list = new ArrayList<String>();

        for (EloraConfigRow configRow : getValues()) {
            if (configRow.getProperty(propertyName) != null) {
                String rowPropertyValue = configRow.getProperty(
                        propertyName).toString();
                if (!list.contains(rowPropertyValue)) {
                    list.add(rowPropertyValue);
                }
            }
        }
        return list;
    }

    public HashMap<String, String> extractConfigTablePropertyValuesAsMap(
            String keyPropertyName, String valuePropertyName) {

        HashMap<String, String> map = new HashMap<String, String>();

        for (EloraConfigRow configRow : getValues()) {
            if (configRow.getProperty(keyPropertyName) != null) {
                String keyPropertyValue = configRow.getProperty(
                        keyPropertyName).toString();

                if (configRow.getProperty(valuePropertyName) != null) {
                    String valuePropertyValue = configRow.getProperty(
                            valuePropertyName).toString();

                    if (!map.containsKey(keyPropertyValue)) {
                        map.put(keyPropertyValue, valuePropertyValue);
                    }
                }
            }
        }
        return map;
    }

}
