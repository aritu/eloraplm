/**
 *
 */
package com.aritu.eloraplm.templating.api;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateInput;

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

    public Blob processTemplate(String id, DocumentModel doc,
            List<TemplateInput> params) throws EloraException;

}
