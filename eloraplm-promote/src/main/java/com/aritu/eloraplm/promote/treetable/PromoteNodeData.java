package com.aritu.eloraplm.promote.treetable;

import java.util.Map;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioning;

import com.aritu.eloraplm.relations.treetable.BaseRelationNodeData;

public class PromoteNodeData extends BaseRelationNodeData {
    private static final long serialVersionUID = 1L;

    private String wcVersion;

    private Map<String, String> versionMap;

    private String finalState;

    private boolean isPropagated;

    private boolean alreadyPromoted;

    private boolean editableVersion;

    private boolean isEnforced;

    private boolean isDirect;

    private boolean switchableVersion;

    private String resultMsg;

    private String result;

    private NodeDynamicInfo nodeInfo;

    @In(create = true, required = false)
    private DocumentVersioning documentVersioning;

    @In(create = true)
    protected transient CoreSession documentManager;

    public PromoteNodeData(String id, DocumentModel data,
            NodeDynamicInfo nodeInfo, int level, String docId, Statement stmt,
            int quantity, boolean isObjectWc, Map<String, String> versionMap,
            String wcVersion, boolean isDirect, boolean isSpecial,
            boolean alreadyPromoted) {

        // TODO wcDoc NULL pasatzen da oingoz
        super(id, level, docId, data, null, stmt, null, quantity, null,
                isObjectWc, 0, isSpecial);

        // TODO: deberia limpiar esta clase quitando las propiedades que ya
        // estan en nodeInfo y creando una propiedad de tipo NodeChangeableInfo
        // Por ahora no lo hago porque en todos los xml y templates se le llama
        // a las propiedades directamente desde PromoteNodeData
        this.nodeInfo = nodeInfo;
        this.wcVersion = wcVersion;
        switchableVersion = nodeInfo.getSwitchableVersion();
        this.isDirect = isDirect;
        this.versionMap = versionMap;
        finalState = nodeInfo.getFinalState();
        isPropagated = nodeInfo.getIsPropagated();
        this.alreadyPromoted = alreadyPromoted;
        editableVersion = nodeInfo.getEditableVersion();
        isEnforced = nodeInfo.getIsEnforced();
        result = nodeInfo.getResult();
        resultMsg = nodeInfo.getResultMsg();
    }

    public NodeDynamicInfo getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(NodeDynamicInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public String getWcVersion() {
        return wcVersion;
    }

    public void setWcVersion(String wcVersion) {
        this.wcVersion = wcVersion;
    }

    public Map<String, String> getVersionMap() {
        return versionMap;
    }

    public void setVersionMap(Map<String, String> versionMap) {
        this.versionMap = versionMap;
    }

    public String getFinalState() {
        return finalState;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public boolean getIsPropagated() {
        return isPropagated;
    }

    public void setIsPropagated(boolean isPropagated) {
        this.isPropagated = isPropagated;
    }

    public boolean getAlreadyPromoted() {
        return alreadyPromoted;
    }

    public void setAlreadyPromoted(boolean alreadyPromoted) {
        this.alreadyPromoted = alreadyPromoted;
    }

    public boolean getEditableVersion() {
        return editableVersion;
    }

    public void setEditableVersion(boolean editableVersion) {
        this.editableVersion = editableVersion;
    }

    public boolean getIsEnforced() {
        return isEnforced;
    }

    public void setIsEnforced(boolean isEnforced) {
        this.isEnforced = isEnforced;
    }

    public boolean getSwitchableVersion() {
        return switchableVersion;
    }

    public void setSwitchableVersion(boolean switchableVersion) {
        this.switchableVersion = switchableVersion;
    }

    public boolean getIsDirect() {
        return isDirect;
    }

    public void setIsDirect(boolean isDirect) {
        this.isDirect = isDirect;
    }

    public String getResultMsg() {
        return resultMsg;
    }

    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (alreadyPromoted ? 1231 : 1237);
        result = prime * result
                + ((finalState == null) ? 0 : finalState.hashCode());
        result = prime * result + (isDirect ? 1231 : 1237);
        result = prime * result + (isEnforced ? 1231 : 1237);
        result = prime * result + (isPropagated ? 1231 : 1237);
        result = prime * result
                + ((this.result == null) ? 0 : this.result.hashCode());
        result = prime * result
                + ((resultMsg == null) ? 0 : resultMsg.hashCode());
        result = prime * result + (switchableVersion ? 1231 : 1237);
        result = prime * result
                + ((versionMap == null) ? 0 : versionMap.hashCode());
        result = prime * result
                + ((wcVersion == null) ? 0 : wcVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PromoteNodeData other = (PromoteNodeData) obj;
        if (alreadyPromoted != other.alreadyPromoted) {
            return false;
        }
        if (finalState == null) {
            if (other.finalState != null) {
                return false;
            }
        } else if (!finalState.equals(other.finalState)) {
            return false;
        }
        if (isDirect != other.isDirect) {
            return false;
        }
        if (isEnforced != other.isEnforced) {
            return false;
        }
        if (isPropagated != other.isPropagated) {
            return false;
        }
        if (result == null) {
            if (other.result != null) {
                return false;
            }
        } else if (!result.equals(other.result)) {
            return false;
        }
        if (resultMsg == null) {
            if (other.resultMsg != null) {
                return false;
            }
        } else if (!resultMsg.equals(other.resultMsg)) {
            return false;
        }
        if (switchableVersion != other.switchableVersion) {
            return false;
        }
        if (versionMap == null) {
            if (other.versionMap != null) {
                return false;
            }
        } else if (!versionMap.equals(other.versionMap)) {
            return false;
        }
        if (wcVersion == null) {
            if (other.wcVersion != null) {
                return false;
            }
        } else if (!wcVersion.equals(other.wcVersion)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object obj) {
        // TODO Auto-generated method stub
        return 0;
    }

}