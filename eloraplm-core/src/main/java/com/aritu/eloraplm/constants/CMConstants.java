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
package com.aritu.eloraplm.constants;

/**
 * General constants related to the Change Management
 *
 * @author aritu
 *
 */
public class CMConstants {

    // ECO TYPE VALUES
    public static final String FAST_TRACK_VALUE = "id_fastTrack";

    public static final String MEDIUM_IMPACT_VALUE = "id_mediumImpact";

    public static final String HIGH_IMPACT_VALUE = "id_highImpact";

    // ITEM TYPES
    public static final String ITEM_TYPE_DOC = "DOC";

    public static final String ITEM_TYPE_BOM = "BOM";

    // ACTION VALUES
    public static final String ACTION_CHANGE = "id_change";

    public static final String ACTION_REPLACE = "id_replace";

    public static final String ACTION_REMOVE = "id_remove";

    public static final String ACTION_IGNORE = "id_ignore";

    // FIELDS and ACTIONS TRIGERRING REFRESH NODE ACTION
    public static final String TRIGGER_FIELD_ACTION = "action";

    public static final String TRIGGER_FIELD_IS_MANAGED = "isManaged";

    public static final String TRIGGER_FIELD_DESTINATION_ITEM_UID = "destinationItemUid";

    public static final String TRIGGER_ACTION_LOAD_DESTINATION_VERSIONS = "loadDestinationVersions";

    // MODIFIABLE ATTRIBUTES
    public static final String MODIFIABLE_ATTRIBUTE_ACTION = "action";

    public static final String MODIFIABLE_ATTRIBUTE_IS_MANAGED = "isManaged";

    public static final String MODIFIABLE_ATTRIBUTE_COMMENT = "comment";

    // COMMENTS
    public static final String COMMENT_IGNORE_SINCE_NO_CHANGES_IN_DESTINATION = "Ignore: since there is nothing to change in the destination";

    public static final String COMMENT_IGNORE_SINCE_ANCESTOR_IS_IGNORE = "Ignore: since it's ancestor action is Ignore";

    public static final String COMMENT_IGNORE_SINCE_ORIGIN_SAME_AS_REPLACE_DESTINATION = "Ignore: origin item is the same as the modification destination, so there is nothing to change";

    public static final String COMMENT_IGNORE_SINCE_ITEM_IGNORED = "Ignore: since ITEM ignored";

}
