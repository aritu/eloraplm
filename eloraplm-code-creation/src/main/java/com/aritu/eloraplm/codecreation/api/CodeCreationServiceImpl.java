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

import java.util.HashMap;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author aritu
 *
 */
public class CodeCreationServiceImpl extends DefaultComponent
        implements CodeCreationService {

    private HashMap<String, TypeDescriptor> types;

    private static final String XP_TYPES = "types";

    @Override
    public void activate(ComponentContext context) {
        types = new HashMap<String, TypeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        types = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        if (extensionPoint.equals(XP_TYPES)) {
            TypeDescriptor type = (TypeDescriptor) contribution;
            if (type.id != null) {
                types.put(type.id, type);
            } else {
                throw new NuxeoException("Type sent without an id");
            }
        }
    }

    @Override
    public String getModeForType(String type) {
        if (types.containsKey(type)) {
            return types.get(type).mode;
        } else {
            return CODE_CREATION_TYPE_MODE_MANUAL;
        }
    }

}
