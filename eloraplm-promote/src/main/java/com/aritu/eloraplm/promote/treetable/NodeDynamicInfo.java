/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.promote.treetable;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class NodeDynamicInfo {

    protected boolean isPropagated;

    protected boolean isEnforced;

    protected boolean editableVersion;

    protected boolean switchableVersion;

    protected boolean alreadyPromoted;

    protected String finalState;

    protected String resultMsg;

    protected String result;

    protected String hiddenResultMsg;

    protected String hiddenResult;

    public String getFinalState() {
        return finalState;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
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

    public String getHiddenResultMsg() {
        return hiddenResultMsg;
    }

    public void setHiddenResultMsg(String hiddenResultMsg) {
        this.hiddenResultMsg = hiddenResultMsg;
    }

    public String getHiddenResult() {
        return hiddenResult;
    }

    public void setHiddenResult(String hiddenResult) {
        this.hiddenResult = hiddenResult;
    }

    public boolean getSwitchableVersion() {
        return switchableVersion;
    }

    public void setSwitchableVersion(boolean switchableVersion) {
        this.switchableVersion = switchableVersion;
    }

    public boolean getIsPropagated() {
        return isPropagated;
    }

    public void setIsPropagated(boolean isPropagated) {
        this.isPropagated = isPropagated;
    }

    public boolean getIsEnforced() {
        return isEnforced;
    }

    public void setIsEnforced(boolean isEnforced) {
        this.isEnforced = isEnforced;
    }

    public boolean getEditableVersion() {
        return editableVersion;
    }

    public void setEditableVersion(boolean editableVersion) {
        this.editableVersion = editableVersion;
    }

    public boolean getAlreadyPromoted() {
        return alreadyPromoted;
    }

    public void setAlreadyPromoted(boolean alreadyPromoted) {
        this.alreadyPromoted = alreadyPromoted;
    }

}
