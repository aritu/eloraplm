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

    public ViewerFileDescriptor getViewerFileForType(String type)
            throws EloraException;

    public void createViewer(DocumentModel doc, String action) throws Exception;

}
