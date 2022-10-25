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

package com.aritu.eloraplm.csvimporter;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class EloraCSVImportStatus {

    private final State state;

    private final int positionInQueue;

    private final int queueSize;

    public enum State {
        SCHEDULED, RUNNING, COMPLETED
    }

    public EloraCSVImportStatus(State state) {
        this(state, 0, 0);
    }

    public EloraCSVImportStatus(State state, int positionInQueue,
            int queueSize) {
        this.state = state;
        this.positionInQueue = positionInQueue;
        this.queueSize = queueSize;
    }

    public State getState() {
        return state;
    }

    public int getPositionInQueue() {
        return positionInQueue;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public boolean isScheduled() {
        return state == State.SCHEDULED;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isComplete() {
        return state == State.COMPLETED;
    }
}
