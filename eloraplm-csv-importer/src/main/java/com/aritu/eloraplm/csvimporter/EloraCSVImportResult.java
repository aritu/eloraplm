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

import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class EloraCSVImportResult {

    protected final long totalLineCount;

    protected final long successLineCount;

    protected final long skippedLineCount;

    protected final long errorLineCount;

    public static final EloraCSVImportResult fromImportLogs(
            List<EloraCSVImportLog> importLogs) {
        long totalLineCount = importLogs.size();
        long successLineCount = 0;
        long skippedLineCount = 0;
        long errorLineCount = 0;
        for (EloraCSVImportLog importLog : importLogs) {
            if (importLog.isSuccess()) {
                successLineCount++;
            } else if (importLog.isSkipped()) {
                skippedLineCount++;
            } else if (importLog.isError()) {
                errorLineCount++;
            }
        }
        return new EloraCSVImportResult(totalLineCount, successLineCount,
                skippedLineCount, errorLineCount);
    }

    public EloraCSVImportResult(long totalLineCount, long successLineCount,
            long skippedLineCount, long errorLineCount) {
        this.totalLineCount = totalLineCount;
        this.successLineCount = successLineCount;
        this.skippedLineCount = skippedLineCount;
        this.errorLineCount = errorLineCount;
    }

    public long getTotalLineCount() {
        return totalLineCount;
    }

    public long getSuccessLineCount() {
        return successLineCount;
    }

    public long getSkippedLineCount() {
        return skippedLineCount;
    }

    public long getErrorLineCount() {
        return errorLineCount;
    }
}
