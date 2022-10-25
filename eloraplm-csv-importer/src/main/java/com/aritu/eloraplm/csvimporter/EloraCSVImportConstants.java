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
public class EloraCSVImportConstants {

    // Import Documents CSV column names
    // TODO LEIRE: konstante hauei izena aldatu: CSV_IMPORT_DOC_XXXX
    public static String CSV_NAME_COL = "name";

    public static String CSV_TYPE_COL = "type";

    public static String CSV_PARENT_PATH_COL = "parentPath";

    public static String CSV_DO_CHECKIN_COL = "doCheckin";

    public static String CSV_CHECHIN_COMMENT_COL = "checkinComment";

    public static String CHECKIN_DEFAULT_COMMENT = "Imported document";

    // Import Relations CSV column names
    // TODO LEIRE: konstante hauei izena aldatu: CSV_IMPORT_RELATION_XXXX
    public static String CSV_SOURCE_NAME_COL = "sourceName";

    public static String CSV_SOURCE_PARENT_PATH_COL = "sourceParentPath";

    public static String CSV_SOURCE_DOC_UID_COL = "sourceDocUid";

    public static String CSV_RELATION_PREDICATE_COL = "relation:predicate";

    public static String CSV_TARGET_NAME_COL = "targetName";

    public static String CSV_TARGET_PARENT_PATH_COL = "targetParentPath";

    public static String CSV_TARGET_DOC_UID_COL = "targetDocUid";

    public static String CSV_QUANTITY_COL = "quantity";

    public static String CSV_ORDERING_COL = "ordering";

    public static String CSV_DIRECTOR_ORDERING_COL = "directorOrdering";

    public static String CSV_VIEWER_ORDERING_COL = "viewerOrdering";

    public static String CSV_INVERSE_VIEWER_ORDERING_COL = "inverseViewerOrdering";

    public static String CSV_IS_MANUAL_COL = "isManual";

    // Import Proxies CSV column names
    public static String CSV_IMPORT_PROXY_DOC_NAME_COL = "docName";

    public static String CSV_IMPORT_PROXY_DOC_PARENT_PATH_COL = "docParentPath";

    public static String CSV_IMPORT_PROXY_DOC_UID_COL = "docUid";

    public static String CSV_IMPORT_PROXY_FOLDER_NAME_COL = "folderName";

    public static String CSV_IMPORT_PROXY_FOLDER_PARENT_PATH_COL = "folderParentPath";

    public static String CSV_IMPORT_PROXY_FOLDER_UID_COL = "folderUid";

    // Result additional constants
    public static String CSV_RESULT_COL = "result";

    public static String CSV_RESULT_OK_VAL = "OK";

    public static String CSV_RESULT_ERROR_VAL = "ERROR";

    public static String CSV_RESULT_MSG_COL = "resultMessage";

    // Folder for storing Import Results
    public static final String IMPORT_RESULTS_FOLDER_DOCUMENT_TYPE = "ImportResultsFolder";

}
