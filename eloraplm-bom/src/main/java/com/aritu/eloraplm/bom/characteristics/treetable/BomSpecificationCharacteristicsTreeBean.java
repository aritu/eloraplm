package com.aritu.eloraplm.bom.characteristics.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;

@Name(BomSpecificationCharacteristicsTreeBean.NAME)
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomSpecificationCharacteristicsTreeBean
        extends BomCharacteristicsTree {

    public static final String NAME = "bomSpecificationCharacteristicsTreeBean";

    private static final long serialVersionUID = 1L;

    public BomSpecificationCharacteristicsTreeBean() {
        super(EloraDoctypeConstants.BOM_SPECIFICATION);
    }

}