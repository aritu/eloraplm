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

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;

import com.aritu.eloraplm.securitypolicy.api.EloraPrincipal;
import com.aritu.eloraplm.securitypolicy.api.EloraPrincipalImpl;
import com.aritu.eloraplm.securitypolicy.constants.SecurityFacetConstants;
import com.aritu.eloraplm.securitypolicy.constants.SecurityPolicyMetadataConstants;

public class EloraSecurityPolicy extends AbstractSecurityPolicy {

    private static final Log log = LogFactory.getLog(EloraSecurityPolicy.class);

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {

        if (doc.hasFacet(SecurityFacetConstants.FACET_HAS_SECURITY_INFO)) {
            Long secLevelDoc = (Long) doc.getPropertyValue(
                    SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL);

            if (secLevelDoc == null) {
                secLevelDoc = (long) 1;
            }

            if (principal instanceof EloraPrincipalImpl) {
                EloraPrincipal eloPrincipal = (EloraPrincipal) principal;

                Integer secLevelPrincipal = eloPrincipal.getSecurityLevel();

                if (secLevelDoc > secLevelPrincipal) {
                    return Access.DENY;
                }
            }
        }

        return Access.UNKNOWN;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        // could only restrict Browse permission, or others
        return true;
    }

    @Override
    public boolean isExpressibleInQuery() {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer() {
        return ELORA_SECURITY_POLICY_TRANSFORMER;
    }

    public static final Transformer ELORA_SECURITY_POLICY_TRANSFORMER = new securityLevelTransformer();

    public static class securityLevelTransformer implements Transformer {

        private static final long serialVersionUID = 1L;

        public static final Predicate SECURITY_LEVEL_NULL = new Predicate(
                new Reference(
                        SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL),
                Operator.ISNULL, null);

        @Override
        public SQLQuery transform(Principal principal, SQLQuery query) {

            if (((NuxeoPrincipal) principal).isAdministrator()
                    || principal.getName() == "system") {
                return query;
            }

            if (!(principal instanceof EloraPrincipalImpl)) {
                return query;
            }

            EloraPrincipal eloPrincipal = (EloraPrincipal) principal;

            Integer secLevelPrincipal = eloPrincipal.getSecurityLevel();
            if (secLevelPrincipal == null) {
                secLevelPrincipal = 1;
            }

            Predicate SECURITY_LEVEL_CHECK = new Predicate(
                    new Reference(
                            SecurityPolicyMetadataConstants.SEC_SECURITY_LEVEL),
                    Operator.LTEQ, new IntegerLiteral(secLevelPrincipal));

            Predicate SECURITY_LEVEL_CHECK_GRANTED = new Predicate(
                    SECURITY_LEVEL_CHECK, Operator.OR, SECURITY_LEVEL_NULL);

            WhereClause where = query.where;

            Predicate predicate;

            if (where == null || where.predicate == null) {
                predicate = SECURITY_LEVEL_CHECK_GRANTED;
            } else {
                predicate = new Predicate(SECURITY_LEVEL_CHECK_GRANTED,
                        Operator.AND, where.predicate);
            }
            return new SQLQuery(query.select, query.from,
                    new WhereClause(predicate), query.groupBy, query.having,
                    query.orderBy, query.limit, query.offset);
        }
    }

}
