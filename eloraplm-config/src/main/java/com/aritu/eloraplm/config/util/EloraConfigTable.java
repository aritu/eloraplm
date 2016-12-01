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
}
