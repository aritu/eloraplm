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
package com.aritu.eloraplm.webapp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

/**
 *
 * @author aritu
 *
 */
public class ListChoiceHelper {

    private static final String CHOICE_MAP_KEY_ID = "choiceId";

    private static final String CHOICE_MAP_KEY_ORDER = "order";

    private static final String CHOICE_MAP_KEY_LABELS = "labels";

    public static List<ListChoice> addChoice(List<ListChoice> listChoices) {

        List<LocalizedLabel> labels = new ArrayList<LocalizedLabel>();
        labels = LocalizedLabelHelper.initializeLabelList(labels);

        ListChoice listChoice = new ListChoice("", listChoices.size() + 1,
                labels);

        listChoices.add(listChoice);
        return listChoices;
    }

    public static List<ListChoice> convertMapListToObjectList(
            List<Map<String, Object>> listChoiceMaps) {
        ListMultimap<Integer, ListChoice> listChoices = MultimapBuilder.treeKeys().arrayListValues().build();

        for (Map<String, Object> listChoiceMap : listChoiceMaps) {
            if (listChoiceMap.containsKey(CHOICE_MAP_KEY_ID)
                    && listChoiceMap.containsKey(CHOICE_MAP_KEY_ORDER)
                    && listChoiceMap.containsKey(CHOICE_MAP_KEY_LABELS)) {
                @SuppressWarnings("unchecked")
                List<LocalizedLabel> labels = LocalizedLabelHelper.convertMapListToObjectList(
                        (List<Map<String, String>>) listChoiceMap.get(
                                CHOICE_MAP_KEY_LABELS));
                int order = (int) (long) listChoiceMap.get(
                        CHOICE_MAP_KEY_ORDER);

                listChoices.put(order,
                        new ListChoice(
                                (String) listChoiceMap.get(CHOICE_MAP_KEY_ID),
                                order, labels));
            }
        }
        return new ArrayList<ListChoice>(listChoices.values());
    }

    public static List<Map<String, Object>> convertObjectListToMapList(
            List<ListChoice> listChoices) {
        ListMultimap<Integer, Map<String, Object>> listChoiceMaps = MultimapBuilder.treeKeys().arrayListValues().build();

        for (ListChoice listChoice : listChoices) {
            Map<String, Object> listChoiceMap = new HashMap<String, Object>();
            listChoiceMap.put(CHOICE_MAP_KEY_ID, listChoice.getChoiceId());
            listChoiceMap.put(CHOICE_MAP_KEY_ORDER, listChoice.getOrder());
            listChoiceMap.put(CHOICE_MAP_KEY_LABELS,
                    LocalizedLabelHelper.convertObjectListToMapList(
                            listChoice.getLabels()));

            listChoiceMaps.put(listChoice.getOrder(), listChoiceMap);
        }
        return new ArrayList<Map<String, Object>>(listChoiceMaps.values());
    }

    public static String getChoiceLabel(List<ListChoice> listChoices,
            String choiceId, String userLocale) {
        String choiceLabel = choiceId;

        for (ListChoice choice : listChoices) {
            if (choice.getChoiceId().equals(choiceId)) {
                choiceLabel = LocalizedLabelHelper.getLocalizedLabel(
                        choice.getLabels(), userLocale, choice.getChoiceId());
                break;
            }
        }

        return choiceLabel;
    }

}
