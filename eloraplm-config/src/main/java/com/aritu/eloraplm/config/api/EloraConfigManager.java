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

package com.aritu.eloraplm.config.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.exceptions.EloraException;

public interface EloraConfigManager {

    String getConfig(String directory, String key) throws EloraException;

    String getConfig(String directory, String key, boolean canBeEmpty)
            throws EloraException;

    String getGeneralConfig(String key) throws EloraException;

    String getGeneralConfig(String key, boolean canBeEmpty)
            throws EloraException;

    Object getConfig(String directory, String key, String property)
            throws EloraException;

    Object getConfig(String directory, String key, String property,
            boolean canBeEmpty) throws EloraException;

    Object getConfig(String directory, Map<String, Serializable> filter,
            String property) throws EloraException;

    Object getConfig(String directory, Map<String, Serializable> filter,
            String property, boolean canBeEmpty) throws EloraException;

    EloraConfigRow getConfigProperties(String directory, String key,
            List<String> properties) throws EloraException;

    EloraConfigRow getConfigProperties(String directory, String key,
            List<String> properties, boolean CanBeEmpty) throws EloraException;

    EloraConfigRow getConfigProperties(String directory,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException;

    EloraConfigRow getConfigProperties(String directory,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException;

    EloraConfigTable getConfigTable(String directory,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException;

    EloraConfigTable getConfigTable(String directory,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException;

    EloraConfigTable getConfigTable(String directory, String keyColumn,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException;

    EloraConfigTable getConfigTable(String directory, String keyColumn,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException;

    void updateConfigProperty(String vocabulary, String key, String property,
            String value) throws EloraException;

    void updateConfigProperty(String vocabulary,
            Map<String, Serializable> filter, String property, String value)
            throws EloraException;

}