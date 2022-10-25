/**
 *
 */
package com.aritu.eloraplm.viewer.api;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface ViewerFileService {

    public TypeDescriptor getType(DocumentModel doc, String action)
            throws EloraException;

    public ViewerFileDescriptor getViewerFile(String id) throws EloraException;

    public ModifierDescriptor getModifier(String id) throws EloraException;

    public void createViewer(DocumentModel doc, String action) throws Exception;

}
