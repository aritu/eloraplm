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

import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.constants.EloraPropertiesConstants;

/**
 * @author aritu
 *
 */
public class EloraPropertiesHelper {

    public static boolean areBomListsExternal() {

        return Boolean.valueOf(Framework.getProperty(
                EloraPropertiesConstants.PROP_EXTERNAL_BOM_LISTS,
                Boolean.toString(false)));

    }

}