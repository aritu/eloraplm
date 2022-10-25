/**
 *
 */
package com.aritu.eloraplm.viewer.filename.api;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface FilenameService {

    public FilenameDescriptor getFilenameDescriptor(String id)
            throws EloraException;

    public String generateFilename(DocumentModel doc,
            String filenameDescriptorId, String action);

}
