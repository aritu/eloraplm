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

package com.aritu.eloraplm.config.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.constants.EloraConfigConstants;
import com.aritu.eloraplm.exceptions.EloraException;

public class EloraConfigService extends DefaultComponent
        implements EloraConfigManager {

    /*
     * Returns single value with a key (simple key-value schema), can be empty
     */
    @Override
    public String getConfig(String vocabulary, String key)
            throws EloraException {
        return getConfig(vocabulary, key, true);
    }

    /*
     * Returns single value with a key (simple key-value schema)
     */
    @Override
    public String getConfig(String vocabulary, String key, boolean canBeEmpty)
            throws EloraException {
        Object value = getConfig(vocabulary, key, "label", canBeEmpty);
        if (value == null) {
            if (!canBeEmpty) {
                throw new EloraException("Configuration value is empty.");
            }
            return null;
        }
        return value.toString();
    }

    /*
     * Returns single value with a key from the general_config vocabulary, can be empty
     */
    @Override
    public String getGeneralConfig(String key) throws EloraException {
        return getGeneralConfig(key, true);
    }

    /*
     * Returns single value with a key from the general_config vocabulary
     */
    @Override
    public String getGeneralConfig(String key, boolean canBeEmpty)
            throws EloraException {
        return getConfig(EloraConfigConstants.VOC_GENERAL_CONFIG, key,
                canBeEmpty);
    }

    /*
     * Returns single property with a key (key + multicolumn schema), can be empty
     */
    @Override
    public Object getConfig(String vocabulary, String key, String property)
            throws EloraException {
        return getConfig(vocabulary, key, property, true);
    }

    /*
     * Returns single property with a key (key + multicolumn schema)
     */
    @Override
    public Object getConfig(String vocabulary, String key, String property,
            boolean canBeEmpty) throws EloraException {
        Object value = null;
        List<String> properties = new ArrayList<String>();
        properties.add(property);
        EloraConfigRow configRow = getConfigProperties(vocabulary, key,
                properties, canBeEmpty);
        if (!configRow.isEmpty()) {
            value = configRow.getProperty(property);
        } else {
            if (!canBeEmpty) {
                throw new EloraException("Configuration value is empty.");
            }
        }
        return value;
    }

    /*
     * Returns single property, selecting a single row with a filter (multicolumn schema), can be empty
     */
    @Override
    public Object getConfig(String vocabulary, Map<String, Serializable> filter,
            String property) throws EloraException {
        return getConfig(vocabulary, filter, property, true);
    }

    /*
     * Returns single property, selecting a single row with a filter (multicolumn schema)
     */
    @Override
    public Object getConfig(String vocabulary, Map<String, Serializable> filter,
            String property, boolean canBeEmpty) throws EloraException {
        Object value = null;
        List<String> properties = new ArrayList<String>();
        properties.add(property);
        EloraConfigRow configRow = getConfigProperties(vocabulary, filter,
                properties, canBeEmpty);
        if (!configRow.isEmpty()) {
            value = configRow.getProperty(property);
        } else {
            if (!canBeEmpty) {
                throw new EloraException("Configuration value is empty.");
            }
        }
        return value;
    }

    /*
     * Returns multiple properties with a key (key + multicolumn schema), can be empty
     */
    @Override
    public EloraConfigRow getConfigProperties(String vocabulary, String key,
            List<String> properties) throws EloraException {

        return getConfigProperties(vocabulary, key, properties, true);
    }

    /*
     * Returns multiple properties with a key (key + multicolumn schema)
     */
    @Override
    public EloraConfigRow getConfigProperties(String vocabulary, String key,
            List<String> properties, boolean canBeEmpty) throws EloraException {

        EloraConfigRow configRow = new EloraConfigRow();
        DirectoryService directoryService = Framework.getLocalService(
                DirectoryService.class);
        Session dirSession = null;
        try {
            dirSession = directoryService.open(vocabulary);

            DocumentModel vocabDoc = dirSession.getEntry(key);
            if (vocabDoc == null) {
                if (!canBeEmpty) {
                    throw new EloraException("Configuration value is empty.");
                }
            } else {
                DataModel vocabDataModel = vocabDoc.getDataModels().values().iterator().next();
                for (String property : properties) {
                    configRow.setProperty(property,
                            vocabDataModel.getData(property));
                }
            }
        } catch (Exception e) {
            throw new EloraException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
        return configRow;
    }

    /*
     * Returns multiple properties, selecting a single row with a filter (multicolumn schema), can be empty
     */
    @Override
    public EloraConfigRow getConfigProperties(String vocabulary,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException {

        return getConfigProperties(vocabulary, filter, properties, true);
    }

    /*
     * Returns multiple properties, selecting a single row with a filter (multicolumn schema)
     */
    @Override
    public EloraConfigRow getConfigProperties(String vocabulary,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException {

        EloraConfigTable configTable = getConfigTable(vocabulary, filter,
                properties, canBeEmpty);
        if (configTable.size() != 1) {
            throw new EloraException(
                    "Filter query returns more than one vocabulary item (or nothing).");
        }

        return configTable.getFirst();
    }

    /*
     * Returns multiple properties with ID as key column, selecting multiple rows with a filter,
     * or all rows if filter is null (multicolumn schema), can be empty
     */
    @Override
    public EloraConfigTable getConfigTable(String vocabulary,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException {
        return getConfigTable(vocabulary, filter, properties, true);
    }

    /*
     * Returns multiple properties with ID as key column, selecting multiple rows with a filter,
     * or all rows if filter is null (multicolumn schema)
     */
    @Override
    public EloraConfigTable getConfigTable(String vocabulary,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException {
        return getConfigTable(vocabulary, "id", filter, properties, canBeEmpty);
    }

    /*
     * Returns multiple properties with custom key column, selecting multiple rows with a filter,
     * or all rows if filter is null (multicolumn schema), can be empty
     */
    @Override
    public EloraConfigTable getConfigTable(String vocabulary, String keyColumn,
            Map<String, Serializable> filter, List<String> properties)
            throws EloraException {

        return getConfigTable(vocabulary, keyColumn, filter, properties, true);
    }

    /*
     * Returns multiple properties with custom key column, selecting multiple rows with a filter,
     * or all rows if filter is null (multicolumn schema)
     */
    @Override
    public EloraConfigTable getConfigTable(String vocabulary, String keyColumn,
            Map<String, Serializable> filter, List<String> properties,
            boolean canBeEmpty) throws EloraException {
        EloraConfigTable configTable = new EloraConfigTable();
        DirectoryService directoryService = Framework.getLocalService(
                DirectoryService.class);
        Session dirSession = null;
        try {
            dirSession = directoryService.open(vocabulary);

            filter = filter == null ? new HashMap<String, Serializable>()
                    : filter;
            DocumentModelList vocabDocs = dirSession.query(filter);

            if (vocabDocs.isEmpty()) {
                if (!canBeEmpty) {
                    throw new EloraException(
                            "Filter query does not return any result.");
                }
                return configTable;
            }

            for (DocumentModel vocabDoc : vocabDocs) {
                EloraConfigRow configRow = new EloraConfigRow();
                if (vocabDoc != null) {
                    DataModel vocabDataModel = vocabDoc.getDataModels().values().iterator().next();

                    if (properties == null) {
                        configRow.importMap(vocabDataModel.getMap());
                    } else {
                        for (String property : properties) {
                            configRow.setProperty(property,
                                    vocabDataModel.getData(property));
                        }
                    }
                    configTable.setRow(
                            vocabDataModel.getData(keyColumn).toString(),
                            configRow);
                }
            }
        } catch (Exception e) {
            throw new EloraException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
        return configTable;
    }

    @Override
    public void updateConfigProperty(String vocabulary, String key,
            String property, String value, boolean mustExistKey)
            throws EloraException {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        // TODO Hau konstanteekin
        filter.put("id", key);

        updateConfigProperty(vocabulary, filter, property, value, mustExistKey);
    }

    @Override
    public void updateConfigProperty(String vocabulary,
            Map<String, Serializable> filter, String property, String value,
            boolean mustExistKey) throws EloraException {
        DirectoryService directoryService = Framework.getLocalService(
                DirectoryService.class);
        Session dirSession = null;
        try {
            dirSession = directoryService.open(vocabulary);

            filter = filter == null ? new HashMap<String, Serializable>()
                    : filter;
            DocumentModelList vocabDocs = dirSession.query(filter);

            if (vocabDocs.isEmpty()) {
                if (mustExistKey) {
                    throw new EloraException(
                            "Filter has returned empty response. No vocabularies updated.");
                } else {
                    return;
                }
            }

            if (vocabDocs.size() > 1) {
                throw new EloraException(
                        "Filter query must return just one result.");
            }

            // We get the first and unique entry
            DocumentModel vocabDoc = vocabDocs.get(0);
            if (vocabDoc != null) {
                vocabDoc.setPropertyValue(property, value);
                dirSession.updateEntry(vocabDoc);
            }
        } catch (Exception e) {
            throw new EloraException(e);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

}