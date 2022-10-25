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

import java.io.Serializable;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class EloraCSVImporterOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final EloraCSVImporterOptions DEFAULT_OPTIONS = new Builder().build();

    public static class Builder {

        private EloraCSVImporterDocumentFactory CSVImporterDocumentFactory = new EloraDefaultCSVImporterDocumentFactory();

        private String dateFormat = "MM/dd/yyyy";

        private String listSeparatorRegex = "\\|";

        private Character escapeCharacter = '\\';

        private boolean updateExisting = true;

        private boolean checkAllowedSubTypes = true;

        private boolean sendEmail = false;

        private int batchSize = 50;

        public Builder documentModelFactory(
                EloraCSVImporterDocumentFactory factory) {
            CSVImporterDocumentFactory = factory;
            return this;
        }

        public Builder dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public Builder listSeparatorRegex(String listSeparatorRegex) {
            this.listSeparatorRegex = listSeparatorRegex;
            return this;
        }

        public Builder escapeCharacter(Character escapeCharacter) {
            this.escapeCharacter = escapeCharacter;
            return this;
        }

        public Builder updateExisting(boolean updateExisting) {
            this.updateExisting = updateExisting;
            return this;
        }

        public Builder checkAllowedSubTypes(boolean checkAllowedSubTypes) {
            this.checkAllowedSubTypes = checkAllowedSubTypes;
            return this;
        }

        public Builder sendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public EloraCSVImporterOptions build() {
            return new EloraCSVImporterOptions(CSVImporterDocumentFactory,
                    dateFormat, listSeparatorRegex, escapeCharacter,
                    updateExisting, checkAllowedSubTypes, sendEmail, batchSize);
        }
    }

    protected final EloraCSVImporterDocumentFactory CSVImporterDocumentFactory;

    protected final String dateFormat;

    protected final String listSeparatorRegex;

    protected final Character escapeCharacter;

    protected final boolean updateExisting;

    protected final boolean checkAllowedSubTypes;

    protected final boolean sendEmail;

    protected final int batchSize;

    protected EloraCSVImporterOptions(
            EloraCSVImporterDocumentFactory CSVImporterDocumentFactory,
            String dateFormat, String listSeparatorRegex,
            boolean updateExisting, boolean checkAllowedSubTypes,
            boolean sendEmail, int batchSize) {
        this(CSVImporterDocumentFactory, dateFormat, listSeparatorRegex, '\\',
                updateExisting, checkAllowedSubTypes, sendEmail, batchSize);
    }

    /**
     * @since 7.2
     */
    protected EloraCSVImporterOptions(
            EloraCSVImporterDocumentFactory CSVImporterDocumentFactory,
            String dateFormat, String listSeparatorRegex,
            Character escapeCharacter, boolean updateExisting,
            boolean checkAllowedSubTypes, boolean sendEmail, int batchSize) {
        this.CSVImporterDocumentFactory = CSVImporterDocumentFactory;
        this.dateFormat = dateFormat;
        this.listSeparatorRegex = listSeparatorRegex;
        this.escapeCharacter = escapeCharacter;
        this.updateExisting = updateExisting;
        this.checkAllowedSubTypes = checkAllowedSubTypes;
        this.sendEmail = sendEmail;
        this.batchSize = batchSize;
    }

    public EloraCSVImporterDocumentFactory getCSVImporterDocumentFactory() {
        return CSVImporterDocumentFactory;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getListSeparatorRegex() {
        return listSeparatorRegex;
    }

    public Character getEscapeCharacter() {
        return escapeCharacter;
    }

    public boolean updateExisting() {
        return updateExisting;
    }

    public boolean checkAllowedSubTypes() {
        return checkAllowedSubTypes;
    }

    public boolean sendEmail() {
        return sendEmail;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
