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
package com.aritu.eloraplm.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.aritu.eloraplm.constants.EloraGroupConstants;
import com.aritu.eloraplm.constants.NuxeoMetadataConstants;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper for permission management.
 *
 * @author aritu
 *
 */
public class EloraPermissionHelper {

    private static final Log log = LogFactory.getLog(
            EloraPermissionHelper.class);

    @SuppressWarnings("serial")
    protected static Map<String, List<String>> vaultFolderGroupsAndPermissions = new HashMap<String, List<String>>() {
        {
            put(SecurityConstants.READ, new ArrayList<String>() {
                {
                    add(SecurityConstants.READ);
                }
            });
            put(SecurityConstants.WRITE, new ArrayList<String>() {
                {
                    add(SecurityConstants.READ_WRITE);
                }
            });
            put(SecurityConstants.REMOVE, new ArrayList<String>() {
                {
                    add(SecurityConstants.REMOVE);
                }
            });
        }
    };

    ////////////////////////////////////////////////////////////////
    // Permissions helper general methods
    ////////////////////////////////////////////////////////////////
    public static void setPermission(DocumentModel doc, String user,
            String permission) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        ACE ace = new ACE(user, permission);
        localACL.add(ace);
        doc.setACP(acp, true);
    }

    public static void removePermission(DocumentModel doc, String user) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        boolean permissionChanged = false;
        if (user != null) {
            permissionChanged = acp.removeACEsByUsername(ACL.LOCAL_ACL, user);
        }
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

    public static void blockPermissionInheritance(DocumentModel doc) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        String username = doc.getCoreSession().getPrincipal().getName();
        boolean permissionChanged = acp.blockInheritance(ACL.LOCAL_ACL,
                username);
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

    public static void unblockPermissionInheritance(DocumentModel doc) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        boolean permissionChanged = acp.unblockInheritance(ACL.LOCAL_ACL);
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Vault Folders permissions related methods
    // TODO LEIRE: agian hauek beste klase batera eraman???
    // EloraVaultFolderPermissionHelper (????)
    ////////////////////////////////////////////////////////////////
    public static List<String> createVaultFolderGroupsAndSetPermission(
            UserManager userManager, DocumentModel vaultFolder) {

        String logInitMsg = "[createVaultFolderGroups] ["
                + vaultFolder.getCoreSession().getPrincipal().getName() + "] ";

        List<String> createdGroupLabels = new ArrayList<String>();

        String eloraRootFolderTitle = "";
        try {
            DocumentModel eloraRootFolder = EloraStructureHelper.getWorkableDomainChildDocModel(
                    vaultFolder, vaultFolder.getCoreSession());
            eloraRootFolderTitle = eloraRootFolder.getTitle();
        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Exception getting eloraRootFolder document for vaultFolder |"
                    + vaultFolder.getId() + "|" + e.getMessage(), e);
        }

        // - Block Permission Inheritance
        blockPermissionInheritance(vaultFolder);

        for (Map.Entry<String, List<String>> entry : vaultFolderGroupsAndPermissions.entrySet()) {
            String group = entry.getKey();
            List<String> permissions = entry.getValue();

            String groupName = getVaultFolderGroupName(vaultFolder.getId(),
                    group);
            String groupLabel = getVaultFolderGroupLabel(vaultFolder,
                    eloraRootFolderTitle, group);

            // Create group
            createGroup(userManager, groupName, groupLabel);
            createdGroupLabels.add(groupLabel);

            // Set group permission
            for (String permission : permissions) {
                setPermission(vaultFolder, groupName, permission);
            }
        }

        // - Everything permission to vaultFolderManagers
        setPermission(vaultFolder,
                EloraGroupConstants.VAULT_FOLDER_MANAGERS_GROUP,
                SecurityConstants.EVERYTHING);

        return createdGroupLabels;
    }

    public static void deleteVaultFolderGroups(UserManager userManager,
            String vaultFolderId) {

        for (String group : vaultFolderGroupsAndPermissions.keySet()) {
            String groupName = getVaultFolderGroupName(vaultFolderId, group);
            if (userManager.getGroup(groupName) != null) {
                userManager.deleteGroup(groupName);
            }
        }
    }

    public static void updateVaultFolderGroups(UserManager userManager,
            DocumentModel vaultFolder) {

        String logInitMsg = "[updateVaultFolderGroups] ["
                + vaultFolder.getCoreSession().getPrincipal().getName() + "] ";

        String eloraRootFolderTitle = "";
        try {
            DocumentModel eloraRootFolder = EloraStructureHelper.getWorkableDomainChildDocModel(
                    vaultFolder, vaultFolder.getCoreSession());
            eloraRootFolderTitle = eloraRootFolder.getTitle();
        } catch (EloraException e) {
            log.error(logInitMsg
                    + "Exception getting eloraRootFolder document for vaultFolder |"
                    + vaultFolder.getId() + "|" + e.getMessage(), e);
        }

        updateVaultFolderGroups(userManager, vaultFolder, eloraRootFolderTitle);
    }

    public static void updateVaultFolderGroups(UserManager userManager,
            DocumentModel vaultFolder, String eloraRootFolderTitle) {

        for (String group : vaultFolderGroupsAndPermissions.keySet()) {
            String groupName = getVaultFolderGroupName(vaultFolder.getId(),
                    group);
            String groupLabel = getVaultFolderGroupLabel(vaultFolder,
                    eloraRootFolderTitle, group);

            updateGroup(userManager, groupName, groupLabel);
        }
    }

    protected static String getVaultFolderGroupName(String vaultFolderDocId,
            String permission) {
        String groupName = EloraGroupConstants.VAULT_FOLDER_GROUP_PREFIX
                + vaultFolderDocId + "_" + permission;
        return groupName;
    }

    protected static String getVaultFolderGroupLabel(DocumentModel vaultFolder,
            String eloraRootFolderTitle, String permission) {
        String groupLabel = eloraRootFolderTitle + " - "
                + vaultFolder.getTitle() + " - " + permission;
        return groupLabel;
    }

    protected static DocumentModel createGroup(UserManager userManager,
            String groupName, String groupLabel) {

        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setPropertyValue(NuxeoMetadataConstants.NX_GROUP_GROUP_NAME,
                groupName);
        groupModel.setPropertyValue(NuxeoMetadataConstants.NX_GROUP_GROUP_LABEL,
                groupLabel);
        groupModel = userManager.createGroup(groupModel);

        return groupModel;
    }

    protected static void updateGroup(UserManager userManager, String groupName,
            String groupLabel) {

        DocumentModel groupModel = userManager.getGroupModel(groupName);
        if (groupModel != null) {
            groupModel.setPropertyValue(
                    NuxeoMetadataConstants.NX_GROUP_GROUP_LABEL, groupLabel);
            userManager.updateGroup(groupModel);
        }
    }

}
