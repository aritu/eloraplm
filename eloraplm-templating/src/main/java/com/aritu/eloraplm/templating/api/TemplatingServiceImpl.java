/**
 *
 */
package com.aritu.eloraplm.templating.api;

import java.util.HashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.templating.adapters.FakeTemplateBasedDocumentAdapter;

/**
 * @author aritu
 *
 */
public class TemplatingServiceImpl extends DefaultComponent
        implements TemplatingService {

    private HashMap<String, TemplateDescriptor> templates;

    private static final String XP_TEMPLATES = "templates";

    @Override
    public void activate(ComponentContext context) {
        templates = new HashMap<String, TemplateDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        templates = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        if (extensionPoint.equals(XP_TEMPLATES)) {
            TemplateDescriptor template = (TemplateDescriptor) contribution;
            if (template.id != null) {
                templates.put(template.id, template);
            } else {
                throw new NuxeoException("Template sent without an id");
            }
        }
    }

    @Override
    public boolean existsTemplate(String id) {
        return templates.containsKey(id);
    }

    @Override
    public TemplateDescriptor getTemplate(String id) {

        if (templates != null && templates.containsKey(id)) {
            return templates.get(id);
        } else {
            return null;
        }
    }

    @Override
    public Blob processTemplate(String id, DocumentModel doc)
            throws EloraException {
        TemplateDescriptor template = getTemplate(id);
        if (template == null) {
            throw new EloraException("Provided template id |" + id
                    + "| is not registered. Could not get template info.");
        }
        TemplateBasedDocument tmplBasedDoc = new FakeTemplateBasedDocumentAdapter(
                doc, template.name);
        Blob blob = tmplBasedDoc.renderWithTemplate(template.name);
        blob.setMimeType(template.mimetype);

        return blob;
    }
}
