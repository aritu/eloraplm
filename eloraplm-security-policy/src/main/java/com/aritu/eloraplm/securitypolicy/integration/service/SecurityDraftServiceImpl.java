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
package com.aritu.eloraplm.securitypolicy.integration.service;

import java.util.ArrayList;
import java.util.List;
import com.aritu.eloraplm.integration.service.DraftService;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecuritySchemaConstants;

/**
 * @author aritu
 *
 */
public class SecurityDraftServiceImpl extends DraftService {

    public SecurityDraftServiceImpl() {
        super();

        List<String> secList = new ArrayList<String>();
        secList.add(SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL);
        secList.add(SecurityPolicyMetadataConstants.SEC_SECURITY_BY);
        secList.add(SecurityPolicyMetadataConstants.SEC_SECURITY_DATE);
        secList.add(SecurityPolicyMetadataConstants.SEC_SECURITY_VIEWER_FILE);

        excludedPropertiesFromCopy.put(SecuritySchemaConstants.SECURITY_INFO,
                secList);
    }

}