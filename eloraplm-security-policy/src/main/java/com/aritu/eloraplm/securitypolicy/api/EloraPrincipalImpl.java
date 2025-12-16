/**
 *
 */
package com.aritu.eloraplm.securitypolicy.api;

import java.io.ObjectStreamException;
import java.io.Serializable;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author aritu
 *
 */
public class EloraPrincipalImpl extends NuxeoPrincipalImpl
        implements EloraPrincipal {

    private static final long serialVersionUID = 1L;

    public Integer securityLevel;

    protected EloraPrincipalImpl(EloraPrincipalImpl other) {
        super(other);

        securityLevel = other.securityLevel;
    }

    public EloraPrincipalImpl(NuxeoPrincipalImpl nuxeoPrincipalImpl,
            DocumentModel userEntry) {
        super(nuxeoPrincipalImpl.getName(), nuxeoPrincipalImpl.isAnonymous(),
                nuxeoPrincipalImpl.isAdministrator(), true);

        setConfig(nuxeoPrincipalImpl.getConfig());
        this.setModel(userEntry, false);
        this.setVirtualGroups(nuxeoPrincipalImpl.getVirtualGroups());
        setRoles(nuxeoPrincipalImpl.getRoles());

        if (!Framework.isTestModeSet()) {
            updateAllGroups();
        }
    }

    @Override
    public Integer getSecurityLevel() {
        return securityLevel;
    }

    @Override
    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    @Override
    public NuxeoPrincipal cloneTransferable() {
        return new TransferableClone(this);
    }

    /**
     * Provides another implementation which marshall the user id instead of
     * transferring the whole content and resolve it when unmarshalled.
     */
    static protected class TransferableClone extends EloraPrincipalImpl {

        protected TransferableClone(EloraPrincipalImpl other) {
            super(other);
        }

        static class DataTransferObject implements Serializable {

            private static final long serialVersionUID = 1L;

            final String username;

            final String originatingUser;

            DataTransferObject(NuxeoPrincipal principal) {
                username = principal.getName();
                originatingUser = principal.getOriginatingUser();
            }

            private Object readResolve() throws ObjectStreamException {
                NuxeoPrincipal principal = Framework.getService(
                        UserManager.class).getPrincipal(username);
                principal.setOriginatingUser(originatingUser);
                return principal;
            }

        }

        private Object writeReplace() throws ObjectStreamException {
            return new DataTransferObject(this);
        }
    }

}
