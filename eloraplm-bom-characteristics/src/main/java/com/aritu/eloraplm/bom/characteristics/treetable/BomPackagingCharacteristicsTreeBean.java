package com.aritu.eloraplm.bom.characteristics.treetable;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.primefaces.model.TreeNode;

import com.aritu.eloraplm.constants.EloraDoctypeConstants;

@Name("bomPackagingCharacteristicsTreeBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = APPLICATION)
@AutomaticDocumentBasedInvalidation
public class BomPackagingCharacteristicsTreeBean
        extends BomCharacteristicsTreeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public BomPackagingCharacteristicsTreeBean() {
        super(EloraDoctypeConstants.BOM_PACKAGING);
    }

    @Override
    @Factory(value = "bomPackagingCharacteristicsRoot", scope = ScopeType.EVENT)
    public TreeNode getRootFromFactory() {
        return getRoot();
    }

}