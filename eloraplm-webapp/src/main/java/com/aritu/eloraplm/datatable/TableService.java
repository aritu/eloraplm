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

import java.util.List;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface TableService {

    /**
     * Receives the parent object, and returns all the rows of the table
     *
     * @param parentObject
     * @return
     */
    List<RowData> getData(Object parentObject) throws EloraException;

    /**
     * Creates row data
     *
     * @param rowId
     * @return
     * @throws EloraException
     */
    RowData createRowData(String rowId);

}
