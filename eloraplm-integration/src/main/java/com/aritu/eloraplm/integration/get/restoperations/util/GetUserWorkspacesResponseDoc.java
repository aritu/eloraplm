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
package com.aritu.eloraplm.integration.get.restoperations.util;

import com.aritu.eloraplm.integration.restoperations.util.EloraStructureRootInfo;
import com.aritu.eloraplm.integration.restoperations.util.EloraTypeInfo;

/**
 * @author aritu
 *
 */
public class GetUserWorkspacesResponseDoc {

    private String domainRealUid;

    private String realUid;

    private String reference;

    private EloraTypeInfo typeInfo;

    private EloraStructureRootInfo structureRootInfo;

    private String title;

    private boolean isFavorite;

    private String path;

    public String getRealUid() {
        return realUid;
    }

    public void setRealUid(String realUid) {
        this.realUid = realUid;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public EloraTypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(EloraTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public EloraStructureRootInfo getStructureRootInfo() {
        return structureRootInfo;
    }

    public void setStructureRootInfo(EloraStructureRootInfo structureRootInfo) {
        this.structureRootInfo = structureRootInfo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomainRealUid() {
        return domainRealUid;
    }

    public void setDomainRealUid(String domainRealUid) {
        this.domainRealUid = domainRealUid;
    }
}
