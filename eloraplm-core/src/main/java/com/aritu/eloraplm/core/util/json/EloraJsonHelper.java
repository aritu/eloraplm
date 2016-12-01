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
package com.aritu.eloraplm.core.util.json;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Elora Json helper class. This class provides methods for handling JSON stuff.
 *
 * @author aritu
 *
 */
public class EloraJsonHelper {

    private static final Log log = LogFactory.getLog(EloraJsonHelper.class);

    private EloraJsonHelper() {
    }

    /**
     * @param item
     * @param fieldName
     * @param isMandatory
     * @return
     * @throws EloraException
     */
    public static String getJsonFieldAsString(JsonNode item, String fieldName,
            boolean isMandatory) throws EloraException {

        if (isMandatory) {
            return getJsonFieldAsString(item, fieldName, isMandatory, false,
                    false);
        } else {
            return getJsonFieldAsString(item, fieldName, isMandatory, true,
                    true);
        }
    }

    /**
     * Returns the string value stored in the specified JsonNode field name.
     *
     * @param item
     * @param fieldName
     * @param isMandatory
     * @param canBeEmpty
     * @return
     * @throws EloraException
     */
    public static String getJsonFieldAsString(JsonNode item, String fieldName,
            boolean isMandatory, boolean canBeNull, boolean canBeEmpty)
                    throws EloraException {
        String value = null;
        String errorMsg = null;

        if (isItemValid(item) && isFieldNameValid(item, fieldName)) {
            if (isMandatory) {
                if (!item.has(fieldName)) {
                    errorMsg = fieldName + " is missing.";
                    throw new EloraException(errorMsg);
                }

                if (item.get(fieldName).isNull()) {
                    if (!canBeNull) {
                        errorMsg = fieldName + " is null.";
                        throw new EloraException(errorMsg);
                    }
                } else {
                    if (!canBeEmpty) {
                        if (!item.get(fieldName).isTextual()) {
                            errorMsg = fieldName + " is not Textual.";
                            throw new EloraException(errorMsg);
                        }
                        if (item.get(fieldName).getTextValue().isEmpty()) {
                            errorMsg = fieldName + " is empty.";
                            throw new EloraException(errorMsg);
                        }
                    }

                    if (!item.get(fieldName).isTextual()) {
                        errorMsg = fieldName + " is not Textual.";
                        throw new EloraException(errorMsg);
                    }
                    value = item.get(fieldName).getTextValue();
                }
            } else {

                if (item.has(fieldName)) {
                    if (item.get(fieldName).isNull()) {
                        if (!canBeNull) {
                            errorMsg = fieldName + " is null.";
                            throw new EloraException(errorMsg);
                        }
                    } else {
                        if (!canBeEmpty) {
                            if (!item.get(fieldName).isTextual()) {
                                errorMsg = fieldName + " is not Textual.";
                                throw new EloraException(errorMsg);
                            }
                            if (item.get(fieldName).getTextValue().isEmpty()) {
                                errorMsg = fieldName + " is empty.";
                                throw new EloraException(errorMsg);
                            }
                        }
                        if (!item.get(fieldName).isTextual()) {
                            errorMsg = fieldName + " is not Textual.";
                            throw new EloraException(errorMsg);
                        }
                        value = item.get(fieldName).getTextValue();
                    }
                }
            }
        }

        return value;
    }

    /**
     * Returns the boolean value stored in the specified JsonNode field name.
     *
     * @param item
     * @param fieldName
     * @param isMandatory
     * @return boolean value. If requested field is not mandatory and it is not
     *         present, the method returns false.
     * @throws EloraException if stored value cannot be converted to a boolean
     *             value.
     */
    public static boolean getJsonFieldAsBoolean(JsonNode item, String fieldName,
            boolean isMandatory) throws EloraException {

        boolean value = false;
        String errorMsg = null;

        if (isItemValid(item) && isFieldNameValid(item, fieldName)) {
            if (isMandatory) {
                if (!item.has(fieldName)) {
                    errorMsg = fieldName + " is missing.";
                    throw new EloraException(errorMsg);
                }
                if (!item.get(fieldName).isBoolean()) {
                    errorMsg = fieldName + " is not Boolean.";
                    throw new EloraException(errorMsg);
                }
                value = item.get(fieldName).getValueAsBoolean();

            } else {
                if (item.has(fieldName)) {
                    if (!item.get(fieldName).isBoolean()) {
                        errorMsg = fieldName + " is not Boolean.";
                        throw new EloraException(errorMsg);
                    }
                    value = item.get(fieldName).getValueAsBoolean();
                }
            }
        }

        return value;
    }

    /**
     * Returns a DocumentRef object pointing to the identifier stored in the
     * specified JsonNode field name.
     *
     * @param item
     * @param fieldName
     * @param isMandatory
     * @return
     * @throws EloraException
     */
    public static DocumentRef getJsonFieldAsDocumentRef(JsonNode item,
            String fieldName, boolean isMandatory) throws EloraException {

        DocumentRef valueDocRef = null;

        String valueStr = EloraJsonHelper.getJsonFieldAsString(item, fieldName,
                isMandatory);

        if (valueStr != null && !valueStr.isEmpty()) {
            valueDocRef = new IdRef(valueStr);
        }

        return valueDocRef;
    }

    /**
     * Returns the int value stored in the specified JsonNode field name.
     *
     * @param item
     * @param fieldName
     * @param isMandatory
     * @return
     * @throws EloraException
     */
    public static int getJsonFieldAsInt(JsonNode item, String fieldName,
            boolean isMandatory) throws EloraException {

        // TODO::: A qué inicializamos este valor??? En el caso de que sea un
        // campo NO MANDATORY (isMandatory=false) y el campo no esté presente,
        // la función devuelve este valor por defecto.
        // Alternativas:::
        // 1.- llamar antes a la función item.has(fieldName)
        // 2.- pasar un valor por defecto cada vez
        // 3.- utilizar un valor por defecto especial, tipo: 999999999999
        int value = -1;

        String errorMsg = null;

        if (isItemValid(item) && isFieldNameValid(item, fieldName)) {
            if (isMandatory) {
                if (!item.has(fieldName)) {
                    errorMsg = fieldName + " is missing.";
                    throw new EloraException(errorMsg);
                }
                if (!item.get(fieldName).isInt()) {
                    errorMsg = fieldName + " is not Int.";
                    throw new EloraException(errorMsg);
                }
                value = item.get(fieldName).getValueAsInt();

            } else {
                if (item.has(fieldName)) {
                    if (!item.get(fieldName).isInt()) {
                        errorMsg = fieldName + " is not Int.";
                        throw new EloraException(errorMsg);
                    }
                    value = item.get(fieldName).getValueAsInt();
                }
            }
        }
        return value;
    }

    public static long getJsonFieldAsLong(JsonNode item, String fieldName,
            boolean isMandatory) throws EloraException {

        // TODO::: A qué inicializamos este valor??? En el caso de que sea un
        // campo NO MANDATORY (isMandatory=false) y el campo no esté presente,
        // la función devuelve este valor por defecto.
        // Alternativas:::
        // 1.- llamar antes a la función item.has(fieldName)
        // 2.- pasar un valor por defecto cada vez
        // 3.- utilizar un valor por defecto especial, tipo: 999999999999
        long value = -1;

        String errorMsg = null;

        if (isItemValid(item) && isFieldNameValid(item, fieldName)) {
            if (isMandatory) {
                if (!item.has(fieldName)) {
                    errorMsg = fieldName + " is missing.";
                    throw new EloraException(errorMsg);
                }
                // We do a double check since isLong() method returns false if
                // the field value can be an int (isInt() == true)
                if (!item.get(fieldName).isInt()
                        && !item.get(fieldName).isLong()) {
                    errorMsg = fieldName + " is not Long.";
                    throw new EloraException(errorMsg);
                }
                value = item.get(fieldName).getValueAsLong();

            } else {
                if (item.has(fieldName)) {
                    // We do a double check since isLong() method returns false
                    // if the field value can be an int (isInt() == true)
                    if (!item.get(fieldName).isInt()
                            && !item.get(fieldName).isLong()) {
                        errorMsg = fieldName + " is not Long.";
                        throw new EloraException(errorMsg);
                    }
                    value = item.get(fieldName).getValueAsLong();
                }
            }
        }
        return value;
    }

    /**
     * Returns the JsonNode corresponding to the node specified by nodeName.
     *
     * @param item
     * @param nodeName
     * @param isMandatory
     * @return the JsonNode corresponding to the specified node name.
     * @throws EloraException
     */
    public static JsonNode getJsonNode(JsonNode item, String fieldName,
            boolean isMandatory) throws EloraException {
        JsonNode jsonNode = null;
        String errorMsg = "";

        if (isItemValid(item) && isFieldNameValid(item, fieldName)) {

            if (isMandatory) {
                if (!item.has(fieldName) || item.get(fieldName) == null
                        || !item.get(fieldName).isContainerNode()) {
                    errorMsg = fieldName + " is missing.";
                    throw new EloraException(errorMsg);
                }
                jsonNode = item.get(fieldName);
            } else {
                if (item.has(fieldName) && item.get(fieldName) != null) {
                    jsonNode = item.get(fieldName);
                }
            }
        }

        return jsonNode;
    }

    private static boolean isItemValid(JsonNode item) throws EloraException {
        boolean isItemValid = false;

        String errorMsg = "";
        if (item == null) {
            errorMsg = "Entry item is missing.";
            log.error(errorMsg);
            throw new EloraException(errorMsg);
        }

        isItemValid = true;

        return isItemValid;
    }

    private static boolean isFieldNameValid(JsonNode item, String fieldName)
            throws EloraException {
        boolean isFieldNameValid = false;

        String errorMsg = "";
        if (fieldName == null || fieldName.isEmpty()) {
            errorMsg = "Entry fieldName is missing.";
            log.error(errorMsg);
            throw new EloraException(errorMsg);
        }
        isFieldNameValid = true;

        return isFieldNameValid;
    }

    /**
     * This method serializes specified class into a Java String, following JSON
     * structure.
     *
     * @param classToConvert
     * @return
     * @throws EloraException
     */
    public static String convertToJson(Object classToConvert)
            throws EloraException {
        String methodName = "[convertToJson] ";
        log.trace(methodName + "--- ENTER ---");

        if (classToConvert == null) {
            log.error(
                    "Error converting Java class to json. Specified class is null.");
            throw new EloraException(
                    "Error converting Java class to json. Specified class is null.");
        }

        log.trace(methodName + "classToConvert = |"
                + classToConvert.getClass().getName() + "|");

        String json = "";
        ObjectMapper mapper = new ObjectMapper();

        try {

            json = mapper.writeValueAsString(classToConvert);

        } catch (IOException e) {
            log.error("Error converting Java class to json.", e);
            throw new EloraException("Error converting Java class to json.");
        }

        // log.trace(methodName + "converted json = |" + json + "|");

        log.trace(methodName + "--- EXIT ---");

        return json;
    }
}
