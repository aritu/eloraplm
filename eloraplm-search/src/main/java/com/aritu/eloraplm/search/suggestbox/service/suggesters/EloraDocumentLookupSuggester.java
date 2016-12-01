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
package com.aritu.eloraplm.search.suggestbox.service.suggesters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.suggestbox.service.DocumentSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.suggesters.DocumentLookupSuggester;
import org.nuxeo.runtime.api.Framework;

/**
 * This class overrides Nuxeo's DocumentLookupSuggester class. The difference
 * between both classes is that EloraDocumentSuggester performs the search
 * operation regarding two fields: elo:reference and dc:title. For that reason,
 * suggest method has been overridden.
 *
 * @author aritu
 *
 */
public class EloraDocumentLookupSuggester extends DocumentLookupSuggester {

    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        PageProviderService ppService = Framework.getLocalService(
                PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) context.session);
        userInput = NXQLQueryBuilder.sanitizeFulltextInput(userInput);
        if (userInput.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!userInput.endsWith(" ")) {
            // perform a prefix search on the last typed word
            userInput += "*";
        }
        try {
            List<Suggestion> suggestions = new ArrayList<Suggestion>();
            /*
             * Elora modification: we should pass two parameters (twice userInput param) to perform
             * the search since the SQL behind should perform the search regarding two fields:
             * elo:reference and dc:title.
             */
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    providerName, null, null, null, props,
                    new Object[] { userInput, userInput });
            for (DocumentModel doc : pp.getCurrentPage()) {
                suggestions.add(DocumentSuggestion.fromDocumentModel(doc));
            }
            return suggestions;
        } catch (QueryParseException e) {
            throw new SuggestionException(String.format(
                    "Suggester '%s' failed to perform query with input '%s'",
                    descriptor.getName(), userInput), e);
        }
    }
}
