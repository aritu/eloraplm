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
package com.aritu.eloraplm.codecreation.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.aritu.eloraplm.codecreation.service.CodeCreationService;

/**
 * @author aritu
 *
 */

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("com.aritu.eloraplm.codecreation")
public class TestCodeCreationService {

    @Test
    public void isNuxeoStarted() {
        Assert.assertNotNull("Runtime is not available",
                Framework.getRuntime());
    }

    @Test
    public void isComponentLoaded() {
        RuntimeService runtime = Framework.getRuntime();
        Assert.assertNotNull(runtime.getComponent(
                "com.aritu.eloraplm.codecreation.service.CodeCreationService"));
    }

    @Test
    public void isCodeCreationServiceAvailable() {
        CodeCreationService ccs = Framework.getService(
                CodeCreationService.class);
        Assert.assertNotNull(ccs);
    }

}
