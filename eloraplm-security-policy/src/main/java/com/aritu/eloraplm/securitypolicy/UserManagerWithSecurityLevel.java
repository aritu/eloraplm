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
package com.aritu.eloraplm.securitypolicy;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.computedgroups.UserManagerWithComputedGroups;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;

import com.aritu.eloraplm.securitypolicy.api.EloraPrincipal;
import com.aritu.eloraplm.securitypolicy.api.EloraPrincipalImpl;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;

public class UserManagerWithSecurityLevel
        extends UserManagerWithComputedGroups {

    private static final long serialVersionUID = 1L;

    @Override
    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry,
            boolean anonymous, List<String> groups) {

        NuxeoPrincipal principal = super.makePrincipal(userEntry, anonymous,
                groups);

        if (principal instanceof NuxeoPrincipalImpl) {
            NuxeoPrincipalImpl nuxPrincipal = (NuxeoPrincipalImpl) principal;

            EloraPrincipal eloPrincipal = new EloraPrincipalImpl(nuxPrincipal,
                    userEntry);

            Integer securityLevel = 1;
            Integer maxSecurityLevel = 1;
            List<String> prinGroups = eloPrincipal.getAllGroups();
            for (String group : prinGroups) {
                DocumentModel groupDoc = this.getGroupModel(group);
                if (groupDoc == null) {
                    break;
                }
                Long lsecurityLevel = (Long) groupDoc.getPropertyValue(
                        SecurityPolicyMetadataConstants.GROUP_SECURITY_LEVEL);
                if (lsecurityLevel != null) {
                    securityLevel = lsecurityLevel.intValue();
                    if (securityLevel > maxSecurityLevel) {
                        maxSecurityLevel = securityLevel;
                    }
                }
            }

            eloPrincipal.setSecurityLevel(maxSecurityLevel);

            return eloPrincipal;
        }

        return principal;
    }

    @Override
    protected NuxeoPrincipal getPrincipalUsingCache(String username) {
        NuxeoPrincipal ret = (NuxeoPrincipal) principalCache.get(username);
        if (ret == null) {
            ret = getPrincipal(username, null);
            if (ret == null) {
                return ret;
            }
            principalCache.put(username, ret);
        }
        return ((EloraPrincipalImpl) ret).cloneTransferable(); // should not
                                                               // return cached
                                                               // principal
    }

    @Override
    public DocumentModel createGroup(DocumentModel groupModel) {
        return createGroup(groupModel, null);
    }

    @Override
    public DocumentModel createGroup(DocumentModel groupModel,
            DocumentModel context) throws GroupAlreadyExistsException {
        groupModel = multiTenantManagement.groupTransformer(this, groupModel,
                context);

        // be sure the name does not contains trailing spaces
        checkGrouId(groupModel);

        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            String groupId = getGroupId(groupModel);

            // check the group does not exist
            if (groupDir.hasEntry(groupId)) {
                throw new GroupAlreadyExistsException();
            }
            groupModel = groupDir.createEntry(groupModel);
            notifyGroupChanged(groupId);
            notify(groupId, GROUPCREATED_EVENT_ID);
            return groupModel;

        }
    }

    @Override
    public void createGroup(NuxeoGroup group) {
        DocumentModel newGroupModel = getBareGroupModel();
        newGroupModel.setProperty(groupSchemaName, groupIdField,
                group.getName());
        newGroupModel.setProperty(groupSchemaName, groupLabelField,
                group.getLabel());
        newGroupModel.setProperty(groupSchemaName, groupMembersField,
                group.getMemberUsers());
        newGroupModel.setProperty(groupSchemaName, groupSubGroupsField,
                group.getMemberGroups());
        createGroup(newGroupModel);
    }

}
