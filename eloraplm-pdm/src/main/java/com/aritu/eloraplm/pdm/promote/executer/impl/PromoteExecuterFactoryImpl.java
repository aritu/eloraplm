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
package com.aritu.eloraplm.pdm.promote.executer.impl;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.pdm.promote.executer.PromoteExecuterFactory;
import com.aritu.eloraplm.pdm.promote.executer.PromoteExecuterManager;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class PromoteExecuterFactoryImpl implements PromoteExecuterFactory {

    @Override
    public PromoteExecuterManager getExecuter(String action) {
        switch (action) {
        case EloraLifeCycleConstants.TRANS_APPROVE:
            return new ApprovePromoteExecuterService();
        case EloraLifeCycleConstants.TRANS_OBSOLETE:
            return new ObsoletePromoteExecuterService();
        default:
            return null;
        }
    }

}
