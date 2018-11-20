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

import org.apache.commons.lang.LocaleUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.user.center.profile.UserProfileConstants;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

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
     * in that language, without parameters.
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
     * This method first retrieves the current context, then retrieves the
     * current language from the context and finally translates the received key
     * in that language, with the given parameters.
     *
     * @param Key of the message to be translated.
     * @return Translated message corresponding to the specified key.
     * @throws EloraException
     */
    public static String getTranslatedMessage(String key, Object[] params) {

        FacesContext ctx = FacesContext.getCurrentInstance();

        return getTranslatedMessage(ctx, key, params);
    }

    /**
     * This method retrieves the current language from the context and
     * translates the received key in that language, without parameters.
     *
     * @param ctx Current context.
     * @param key Translated message corresponding to the specified key.
     * @return
     * @throws EloraException
     */
    public static String getTranslatedMessage(FacesContext ctx, String key) {

        return getTranslatedMessage(ctx, key, new Object[0]);
    }

    /**
     * This method retrieves the current language from the context and
     * translates the received key in that language, with the given parameters.
     *
     * @param ctx Current context.
     * @param key Translated message corresponding to the specified key.
     * @param params An array of Objects with the parameters to pass to the
     *            message
     * @return
     * @throws EloraException
     */
    public static String getTranslatedMessage(FacesContext ctx, String key,
            Object[] params) {
        String msg = "";

        if (ctx != null) {
            String bundleName = ctx.getApplication().getMessageBundle();

            Locale locale = ctx.getViewRoot().getLocale();

            msg = I18NUtils.getMessageString(bundleName, key, params, locale);
        }

        return msg;
    }

    /**
     * @param ctx
     * @param key
     * @return
     */
    public static String getTranslatedMessageFromOperation(CoreSession session,
            String key) {

        return getTranslatedMessageFromOperation(session, key, new Object[0]);
    }

    /**
     * @param ctx
     * @param key
     * @param params
     * @return
     */
    public static String getTranslatedMessageFromOperation(CoreSession session,
            String key, Object[] params) {

        if (key == null) {
            return "";
        }

        return I18NUtils.getMessageString("messages", key, params,
                getLocale(session));

    }

    /**
     * Returns the locale based on the logged user profile
     *
     * @param ctx
     * @return
     */
    public static Locale getLocale(CoreSession session) {

        String lang = null;

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        UserProfileService userProfileService = Framework.getLocalService(
                UserProfileService.class);
        DocumentModel userProfileDoc = userProfileService.getUserProfileDocument(
                principal.getName(), session);

        if (userProfileDoc.getPropertyValue(
                UserProfileConstants.USER_PROFILE_LOCALE) != null) {
            lang = (String) userProfileDoc.getPropertyValue(
                    UserProfileConstants.USER_PROFILE_LOCALE);
        }

        if (lang == null) {
            lang = "en";
        }
        return LocaleUtils.toLocale(lang);
    }

    // returns OS's default locale language code
    /* public static Locale getLocale(final CoreSession session) {
        Locale locale = null;
        locale = Framework.getLocalService(LocaleProvider.class).getLocale(
                session);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return new Locale(Locale.getDefault().getLanguage());
    }*/

}
