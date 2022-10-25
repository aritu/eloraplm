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
package com.aritu.eloraplm.core.persistence;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.resolver.BasicDialectResolver;
import org.hibernate.dialect.resolver.DialectFactory;
import org.hibernate.dialect.resolver.DialectResolverSet;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * @author aritu
 *
 */
public class EloraPersistenceServiceImpl extends DefaultComponent
        implements EloraPersistenceService {

    private static final Log log = LogFactory
            .getLog(EloraPersistenceServiceImpl.class);

    {
        // do this statically once, as we're patching a static variable
        registerOracle19DialectResolver();
    }

    protected static void registerOracle19DialectResolver() {
        try {
            log.info("Patching Hibernate to support Oracle 19...");
            Field f = DialectFactory.class
                    .getDeclaredField("DIALECT_RESOLVERS");
            f.setAccessible(true);
            DialectResolverSet resolvers = (DialectResolverSet) f.get(null);
            resolvers.addResolverAtFirst(new BasicDialectResolver("Oracle", 19,
                    Oracle10gDialect.class));
            log.info("Patched.");

        } catch (ReflectiveOperationException | SecurityException e) {
            log.error("Cannot patch Hibernate to support Oracle 19", e);
        }
    }
}
