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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import com.aritu.eloraplm.webapp.util.ListChoice;
import com.aritu.eloraplm.webapp.util.ListChoiceHelper;

/**
 * @author aritu
 *
 */
@Name("listChoiceActions")
@Scope(SESSION)
public class ListChoiceActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(
            ListChoiceActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public List<ListChoice> addChoice(List<ListChoice> listChoices) {

        return ListChoiceHelper.addChoice(listChoices);

    }

    public List<ListChoice> convertMapListToObjectList(
            List<Map<String, Object>> listChoiceMaps) {
        return ListChoiceHelper.convertMapListToObjectList(listChoiceMaps);
    }

    public List<Map<String, Object>> convertObjectListToMapList(
            List<ListChoice> listChoices) {
        return ListChoiceHelper.convertObjectListToMapList(listChoices);
    }

    public String getChoiceLabel(List<ListChoice> listChoices, String choiceId,
            String userLocale) {
        return ListChoiceHelper.getChoiceLabel(listChoices, choiceId,
                userLocale);
    }

    public void removeChoice(List<ListChoice> listChoices, ListChoice choice) {
        listChoices.remove(choice);
    }

}
