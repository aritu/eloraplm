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
package com.aritu.eloraplm.integration.restoperations.util;

import com.aritu.eloraplm.config.util.EloraConfigRow;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public interface VocabularyContent {

    public String getId();

    public void setId(String id);

    public int getObsolete();

    public void setObsolete(int obsolete);

    public int getOrdering();

    public void setOrdering(int ordering);

    public VocabularyContent convertFromConfigRow(EloraConfigRow row);

}
