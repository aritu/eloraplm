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

public class EloraCSVImportConstants {

    // Import Documents CSV column names
    public static String CSV_IMPORT_DOC_NAME_COL = "name";

    public static String CSV_IMPORT_DOC_TYPE_COL = "type";

    public static String CSV_IMPORT_DOC_PARENT_PATH_COL = "parentPath";

    public static String CSV_IMPORT_DOC_DO_CHECKIN_COL = "doCheckin";

    public static String CSV_IMPORT_DOC_CHECHIN_COMMENT_COL = "checkinComment";

    public static String CSV_IMPORT_DOC_CHECKIN_DEFAULT_COMMENT = "Imported document";

    // Import Relations CSV column names
    public static String CSV_IMPORT_RELATION_SOURCE_NAME_COL = "sourceName";

    public static String CSV_IMPORT_RELATION_SOURCE_PARENT_PATH_COL = "sourceParentPath";

    public static String CSV_IMPORT_RELATION_SOURCE_DOC_UID_COL = "sourceDocUid";

    public static String CSV_IMPORT_RELATION_RELATION_PREDICATE_COL = "predicate";

    public static String CSV_IMPORT_RELATION_TARGET_NAME_COL = "targetName";

    public static String CSV_IMPORT_RELATION_TARGET_PARENT_PATH_COL = "targetParentPath";

    public static String CSV_IMPORT_RELATION_TARGET_DOC_UID_COL = "targetDocUid";

    public static String CSV_IMPORT_RELATION_GRAPH_NAME_COL = "graphName";

    public static String CSV_IMPORT_RELATION_TARGET_ALWAYS_WC_COL = "targetAlwaysWc";

    public static String CSV_IMPORT_RELATION_OBJECT = "object";

    public static String CSV_IMPORT_RELATION_SUBJECT = "subject";

    // Import Proxies CSV column names
    public static String CSV_IMPORT_PROXY_DOC_NAME_COL = "docName";

    public static String CSV_IMPORT_PROXY_DOC_PARENT_PATH_COL = "docParentPath";

    public static String CSV_IMPORT_PROXY_DOC_UID_COL = "docUid";

    public static String CSV_IMPORT_PROXY_FOLDER_NAME_COL = "folderName";

    public static String CSV_IMPORT_PROXY_FOLDER_PARENT_PATH_COL = "folderParentPath";

    public static String CSV_IMPORT_PROXY_FOLDER_UID_COL = "folderUid";

    // Import Comments CSV column names

    public static String CSV_IMPORT_COMMENT_DOC_NAME_COL = "docName";

    public static String CSV_IMPORT_COMMENT_DOC_PARENT_PATH_COL = "docParentPath";

    public static String CSV_IMPORT_COMMENT_DOC_UID_COL = "docUid";

    public static String CSV_IMPORT_COMMENT_AUTHOR_COL = "commentAuthor";

    public static String CSV_IMPORT_COMMENT_TEXT_COL = "commentText";

    public static String CSV_IMPORT_COMMENT_CREATION_DATE_COL = "commentCreationDate";

    // Result additional constants
    public static String CSV_RESULT_COL = "result";

    public static String CSV_RESULT_OK_VAL = "OK";

    public static String CSV_RESULT_ERROR_VAL = "ERROR";

    public static String CSV_RESULT_MSG_COL = "resultMessage";

    // Folder for storing Import Results
    public static final String IMPORT_RESULTS_FOLDER_DOCUMENT_TYPE = "ImportResultsFolder";

}
