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

import java.util.Locale;

import javax.faces.context.FacesContext;

import org.nuxeo.common.utils.i18n.I18NUtils;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * Helper class for Elora messages.
 *
 * @author aritu
 *
 */
public class EloraMessageHelper {

    /**
     *
     */
    public EloraMessageHelper() {
    }

    /**
     * This method first retrieves the current context, then retrieves the
     * current language from the context and finally translates the received key
     * in that language.
     *
     * @param Key of the message to be translated.
     * @return Translated message corresponding to the specified key.
     * @throws EloraException
     */
    public static String getTranslatedMessage(String key) {

        FacesContext ctx = FacesContext.getCurrentInstance();

        return getTranslatedMessage(ctx, key);
    }

    /**
     * This method retrieves the current language from the context and
     * translates the received key in that language.
     *
     * @param ctx Current context.
     * @param key Translated message corresponding to the specified key.
     * @return
     * @throws EloraException
     */
    public static String getTranslatedMessage(FacesContext ctx, String key) {
        String msg = "";

        if (ctx != null) {
            String bundleName = ctx.getApplication().getMessageBundle();

            Locale locale = ctx.getViewRoot().getLocale();

            msg = I18NUtils.getMessageString(bundleName, key, null, locale);
        }

        return msg;
    }

}
