/**
 *
 */
package com.aritu.eloraplm.templating.api;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * @author aritu
 *
 */
public interface TemplatingService {

    public boolean existsTemplate(String id);

    public TemplateDescriptor getTemplate(String id);

    public Blob processTemplate(String id, DocumentModel doc)
            throws EloraException;

}
