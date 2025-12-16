/**
 *
 */
package com.aritu.eloraplm.securitypolicy.api;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author aritu
 *
 */
public interface EloraPrincipal extends NuxeoPrincipal {

    Integer getSecurityLevel();

    void setSecurityLevel(Integer securityLevel);

}
