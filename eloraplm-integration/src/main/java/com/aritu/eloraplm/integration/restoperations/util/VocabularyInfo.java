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
package com.aritu.eloraplm.integration.restoperations.util;

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class VocabularyInfo {
    private String id;

    private String timestamp;

    boolean update;

    List<VocabularyContent> content;

    public VocabularyInfo() {
        super();
        content = new ArrayList<VocabularyContent>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public List<VocabularyContent> getContent() {
        return content;
    }

    public void setContent(List<VocabularyContent> content) {
        this.content = content;
    }

    public void addContent(VocabularyContent contentItem) {
        content.add(contentItem);
    }

    public void emptyContent() {
        content.clear();
    }
}
