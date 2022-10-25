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
package com.aritu.eloraplm.lifecycles.factories.impl;

import com.aritu.eloraplm.constants.EloraLifeCycleConstants;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuter;
import com.aritu.eloraplm.lifecycles.factories.TransitionExecuterFactory;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class TransitionExecuterFactoryImpl
        implements TransitionExecuterFactory {

    @Override
    public TransitionExecuter getTransitionExecuter(String transition,
            String lifecycle) {
        switch (transition) {
        case EloraLifeCycleConstants.TRANS_APPROVE:
            switch (lifecycle) {
            case EloraLifeCycleConstants.CAD_LIFE_CYCLE:
            case EloraLifeCycleConstants.BOM_LIFE_CYCLE:
                return new ApproveForCadOrItemTransitionExecuter();
            case EloraLifeCycleConstants.DEFAULT_LIFE_CYCLE:
                return new DefaultForVersionableTransitionExecuter(transition);
            default:
                return new DefaultTransitionExecuter(transition);
            }
        case EloraLifeCycleConstants.TRANS_OBSOLETE:
            switch (lifecycle) {
            case EloraLifeCycleConstants.CAD_LIFE_CYCLE:
            case EloraLifeCycleConstants.BOM_LIFE_CYCLE:
                return new MakeObsoleteForCadOrItemTransitionExecuter();
            case EloraLifeCycleConstants.DEFAULT_LIFE_CYCLE:
                return new MakeObsoleteForBasicDocTransitionExecuter();
            default:
                return new DefaultTransitionExecuter(transition);
            }
        case EloraLifeCycleConstants.TRANS_BACK_TO_PRELIMINARY:
            switch (lifecycle) {
            case EloraLifeCycleConstants.CAD_LIFE_CYCLE:
            case EloraLifeCycleConstants.BOM_LIFE_CYCLE:
            case EloraLifeCycleConstants.DEFAULT_LIFE_CYCLE:
                return new DefaultForVersionableTransitionExecuter(transition);
            default:
                return new DefaultTransitionExecuter(transition);
            }

        default:
            return new DefaultTransitionExecuter(transition);
        }
    }

}
