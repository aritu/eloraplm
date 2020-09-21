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
package com.aritu.eloraplm.datatable;

import java.io.Serializable;

/**
 * @author aritu
 *
 */
public interface RowData extends Serializable, Comparable<Object> {

    String getId();

    void setId(String id);

    boolean getIsNew();

    void setIsNew(boolean isNew);

    boolean getIsRemoved();

    void setIsRemoved(boolean isRemoved);

    boolean getIsModified();

    void setIsModified(boolean isModified);

    boolean getIsDirty();

}