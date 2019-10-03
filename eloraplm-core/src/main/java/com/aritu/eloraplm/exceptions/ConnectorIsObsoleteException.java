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
package com.aritu.eloraplm.exceptions;

/**
 * Exception thrown when an operation is executed using a PlmConnector that has
 * an older version than the allowed minimum specified in the configuration
 *
 * @author aritu
 *
 */
public class ConnectorIsObsoleteException extends Exception {

    private static final long serialVersionUID = 1L;

    private String plmConnectorClient;

    private Integer plmConnectorVersion;

    private Integer minAllowedVersion;

    /**
     * Constructs a ConnectorIsObsoleteException with the exception message.
     *
     * @param message exception message
     */
    public ConnectorIsObsoleteException(String plmConnectorClient,
            Integer plmConnectorVersion, Integer minAllowedVersion) {
        super("Execution of operation was not allowed because the version of the connector is older that the allowed minimum version. Connector: '"
                + plmConnectorClient + "' | Connector version: '"
                + plmConnectorVersion.toString() + "' | Min allowed version: '"
                + minAllowedVersion.toString()
                + "'. Please update the connector.");
        this.plmConnectorClient = plmConnectorClient;
        this.plmConnectorVersion = plmConnectorVersion;
        this.minAllowedVersion = minAllowedVersion;
    }

    public String getPlmConnectorClient() {
        return plmConnectorClient;
    }

    public Integer getPlmConnectorVersion() {
        return plmConnectorVersion;
    }

    public Integer getMinAllowedVersion() {
        return minAllowedVersion;
    }

}
