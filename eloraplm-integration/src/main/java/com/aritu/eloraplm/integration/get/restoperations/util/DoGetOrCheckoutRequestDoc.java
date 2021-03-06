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
package com.aritu.eloraplm.integration.get.restoperations.util;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author aritu
 *
 */
@JsonPropertyOrder({ "checkout", "get", "proxyRef", "realRef" })
public class DoGetOrCheckoutRequestDoc {

    private DocumentRef proxyRef;

    private DocumentRef realRef;

    private boolean selected;

    private boolean lock;

    private boolean isRootElement;

    public DoGetOrCheckoutRequestDoc(DocumentRef proxyRef, DocumentRef realRef,
            boolean selected, boolean lock, boolean isRootElement) {
        this.proxyRef = proxyRef;
        this.realRef = realRef;
        this.selected = selected;
        this.lock = lock;
        this.isRootElement = isRootElement;
    }

    public DocumentRef getProxyRef() {
        return proxyRef;
    }

    public void setProxyRef(DocumentRef proxyRef) {
        this.proxyRef = proxyRef;
    }

    public DocumentRef getRealRef() {
        return realRef;
    }

    public void setRealRef(DocumentRef realRef) {
        this.realRef = realRef;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public boolean getIsRootElement() {
        return isRootElement;
    }

    public void setIsRootElement(boolean isRootElement) {
        this.isRootElement = isRootElement;
    }
}
