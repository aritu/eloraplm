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
package com.aritu.eloraplm.treetable;

/**
 * @author aritu
 *
 */
public class BaseNodeData implements NodeData {
    private static final long serialVersionUID = 1L;

    private String id;

    private int level;

    private boolean isNew;

    private boolean isRemoved;

    private boolean isModified;

    public BaseNodeData(String id, int level) {
        this(id, level, false, false, false);
    }

    public BaseNodeData(String id, int level, boolean isNew,
            boolean isRemoved) {
        this(id, level, isNew, isRemoved, false);
    }

    public BaseNodeData(String id, int level, boolean isNew, boolean isRemoved,
            boolean isModified) {
        this.id = id;
        this.level = level;
        this.isNew = isNew;
        this.isRemoved = isRemoved;
        this.isModified = isModified;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean getIsNew() {
        return isNew;
    }

    @Override
    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public boolean getIsRemoved() {
        return isRemoved;
    }

    @Override
    public void setIsRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    @Override
    public boolean getIsModified() {
        return isModified;
    }

    @Override
    public void setIsModified(boolean isModified) {
        this.isModified = isModified;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (isModified ? 1231 : 1237);
        result = prime * result + (isNew ? 1231 : 1237);
        result = prime * result + (isRemoved ? 1231 : 1237);
        result = prime * result + level;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BaseNodeData other = (BaseNodeData) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (isModified != other.isModified) {
            return false;
        }
        if (isNew != other.isNew) {
            return false;
        }
        if (isRemoved != other.isRemoved) {
            return false;
        }
        if (level != other.level) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(Object obj) {
        BaseNodeData objNode = (BaseNodeData) obj;
        return getId().compareTo(objNode.getId());
    }
}