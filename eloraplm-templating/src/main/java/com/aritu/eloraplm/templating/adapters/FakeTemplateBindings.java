package com.aritu.eloraplm.templating.adapters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.nuxeo.template.adapters.doc.TemplateBinding;

public class FakeTemplateBindings extends ArrayList<TemplateBinding> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_BINDING = "default";

    public FakeTemplateBindings() {
    }

    public String useMainContentAsTemplate() {
        for (TemplateBinding tb : this) {
            if (tb.isUseMainContentAsTemplate()) {
                return tb.getName();
            }
        }
        return null;
    }

    public TemplateBinding get() {
        return get(DEFAULT_BINDING);
    }

    public void removeByName(String templateName) {
        Iterator<TemplateBinding> it = iterator();
        while (it.hasNext()) {
            TemplateBinding binding = it.next();
            if (binding.getName().equals(templateName)) {
                it.remove();
                return;
            }
        }
    }

    public boolean containsTemplateName(String templateName) {
        for (TemplateBinding tb : this) {
            if (templateName.equals(tb.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsTemplateId(String templateId) {
        for (TemplateBinding tb : this) {
            if (templateId.equals(tb.getTemplateId())) {
                return true;
            }
        }
        return false;
    }

    public TemplateBinding get(String name) {
        for (TemplateBinding tb : this) {
            if (name.equals(tb.getName())) {
                return tb;
            }
        }
        return null;
    }

    public void addOrUpdate(TemplateBinding tb) {
        TemplateBinding existing = get(tb.getName());
        if (existing == null) {
            add(tb);
        } else {
            existing.update(tb);
        }
    }

    public List<String> getNames() {

        List<String> names = new ArrayList<String>();
        for (TemplateBinding tb : this) {
            names.add(tb.getName());
        }
        return names;
    }
}
