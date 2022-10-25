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
package com.aritu.eloraplm.codecreation.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import com.aritu.eloraplm.codecreation.util.CodeCreationHelper;

/**
 * @author aritu
 *
 */
@Name("codeCreationActions")
@Scope(CONVERSATION)
public class CodeCreationActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getModeForType(String type) {
        return CodeCreationHelper.getModeForType(type);
    }

}
