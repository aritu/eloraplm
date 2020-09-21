package com.aritu.eloraplm.lifecycles;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.core.lifecycles.api.LifecycleConfigService;
import com.aritu.eloraplm.core.lifecycles.util.LifecyclesConfig;

@Name("lifecycleActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class LifecycleActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(LifecycleActionsBean.class);

    private LifecycleConfigService lcs;

    private LifecycleConfigService getLifeCycleConfigService() {
        if (lcs == null) {
            lcs = Framework.getService(LifecycleConfigService.class);
        }
        return lcs;
    }

    public String getLifeCycleStateColor(String lifeCycleState) {
        return getLifeCycleConfigService().getStateColor(lifeCycleState);
    }

    public boolean isReleasedState(String lifeCycleState) {
        if (LifecyclesConfig.releasedStatesList.contains(lifeCycleState)) {
            return true;
        }
        return false;
    }

}
