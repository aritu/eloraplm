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
package com.aritu.eloraplm.core.relations.web;

import org.nuxeo.ecm.platform.relations.web.StatementInfo;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public interface EloraStatementInfo extends StatementInfo {

    String getQuantity();

    Integer getOrdering();

    Integer getDirectorOrdering();

    Integer getViewerOrdering();

    Integer getInverseViewerOrdering();

    Boolean getIsManual();

}
