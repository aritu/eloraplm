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
package com.aritu.eloraplm.codecreation.api;

/**
 * @author aritu
 *
 */
public interface CodeCreationService {

    public static final String CODE_CREATION_TYPE_MODE_MANUAL = "manual";

    public static final String CODE_CREATION_TYPE_MODE_MANUAL_REQUIRED = "manualRequired";

    public static final String CODE_CREATION_TYPE_MODE_AUTO = "auto";

    public static final String CODE_CREATION_TYPE_MODE_AUTO_IF_EMPTY = "autoIfEmpty";

    public static final String[] CODE_CREATION_TYPE_MODES = {
            CODE_CREATION_TYPE_MODE_MANUAL,
            CODE_CREATION_TYPE_MODE_MANUAL_REQUIRED,
            CODE_CREATION_TYPE_MODE_AUTO,
            CODE_CREATION_TYPE_MODE_AUTO_IF_EMPTY };

    public String getModeForType(String type);

}
