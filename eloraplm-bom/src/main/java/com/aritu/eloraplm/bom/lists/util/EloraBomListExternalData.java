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
package com.aritu.eloraplm.bom.lists.util;

import java.util.List;

import com.aritu.eloraplm.relations.treetable.BomListExternalData;

/**
 *
 * @author aritu
 *
 */
public class EloraBomListExternalData implements BomListExternalData {

    private String reference;

    private String name;

    private List<String> suppliers;

    private boolean existsInElora;

    public EloraBomListExternalData(String reference, String name,
            List<String> suppliers, boolean existsInElora) {
        this.reference = reference;
        this.name = name;
        this.suppliers = suppliers;
        this.existsInElora = existsInElora;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getSuppliers() {
        return suppliers;
    }

    @Override
    public void setSuppliers(List<String> suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    public boolean getExistsInElora() {
        return existsInElora;
    }

    @Override
    public void setExistsInElora(boolean existsInElora) {
        this.existsInElora = existsInElora;
    }

}
