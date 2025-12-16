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
package com.aritu.eloraplm.webapp.base.beans;

import static org.jboss.seam.ScopeType.CONVERSATION;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.core.DocumentContextBoundActions;

/**
 * @author aritu
 *
 */
@Name("pfDialogActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class PrimeFacesDialogActionsBean extends DocumentContextBoundActions {

    private static final long serialVersionUID = 1L;

    private static final boolean DEFAULT_MODAL = true;

    private static final boolean DEFAULT_RESIZABLE = true;

    private static final boolean DEFAULT_DRAGGABLE = true;

    private Object actionValue;

    private DocumentModel contextDoc;

    private String title;

    private String include;

    private boolean modal;

    private boolean resizable;

    private boolean draggable;

    /* Using Integer property does not work */

    private String width;

    private String height;

    private String minWidth;

    private String minHeight;

    public Object getActionValue() {
        return actionValue;
    }

    public void setActionValue(Object actionValue) {
        this.actionValue = actionValue;
    }

    public DocumentModel getContextDoc() {
        return contextDoc;
    }

    public void setContextDoc(DocumentModel contextDoc) {
        this.contextDoc = contextDoc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public boolean isModal() {
        return modal;
    }

    public void setModal(boolean modal) {
        this.modal = modal;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(String minWidth) {
        this.minWidth = minWidth;
    }

    public String getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(String minHeight) {
        this.minHeight = minHeight;
    }

    public PrimeFacesDialogActionsBean() {
        emptyDialog();
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        emptyDialog();
    }

    public void emptyDialog() {
        initDialog(null, null, null, null, null, null, null, null, null, null,
                null);
    }

    /* For widgets, without action params */
    public void initDialog(String title, String include, String modal,
            String resizable, String draggable, String width, String height,
            String minWidth, String minHeight) {
        initDialog(null, null, title, include, modal, resizable, draggable,
                width, height, minWidth, minHeight);
    }

    /*
     * We have to parse from String to Integer, because using Integer directly does
     * not work
     */
    public void initDialog(Object actionValue, DocumentModel contextDoc,
            String title, String include, String modal, String resizable,
            String draggable, String width, String height, String minWidth,
            String minHeight) {
        this.actionValue = actionValue;
        this.contextDoc = contextDoc;
        this.title = title;
        this.include = include;
        this.modal = (modal != null && !modal.isEmpty() ? Boolean.valueOf(modal)
                : DEFAULT_MODAL);
        this.resizable = (resizable != null && !resizable.isEmpty()
                ? Boolean.valueOf(resizable)
                : DEFAULT_RESIZABLE);
        this.draggable = (draggable != null && !draggable.isEmpty()
                ? Boolean.valueOf(draggable)
                : DEFAULT_DRAGGABLE);
        this.width = width;
        this.height = height;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

}
