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
package com.aritu.eloraplm.workflows.util;

import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author aritu
 *
 */
public class TaskInfo {

    private String id;

    private String title;

    private GregorianCalendar executed;

    private List<String> actors;

    private List<String> delegatedActors;

    public TaskInfo(String id, String title, GregorianCalendar executed,
            List<String> actors, List<String> delegatedActors) {
        this.id = id;
        this.title = title;
        this.executed = executed;
        this.actors = actors;
        this.delegatedActors = delegatedActors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public GregorianCalendar getExecuted() {
        return executed;
    }

    public void setExecuted(GregorianCalendar executed) {
        this.executed = executed;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public List<String> getDelegatedActors() {
        return delegatedActors;
    }

    public void setDelegatedActors(List<String> delegatedActors) {
        this.delegatedActors = delegatedActors;
    }

    public boolean isDelegated() {
        return !delegatedActors.isEmpty();
    }

}
