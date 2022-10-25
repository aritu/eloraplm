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
package com.aritu.eloraplm.codecreation.util;

/**
 * @author aritu
 *
 */
public class CodeCreationConditionEvaluator {

    private String className;

    private String methodName;

    private String methodParams;

    private String operator;

    private String value;

    /**
     * @param className
     * @param methodName
     * @param methodParams
     * @param operator
     * @param value
     */
    public CodeCreationConditionEvaluator(String className, String methodName,
            String methodParams, String operator, String value) {
        super();
        this.className = className;
        this.methodName = methodName;
        this.methodParams = methodParams;
        this.operator = operator;
        this.value = value;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName the methodName to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return the methodParams
     */
    public String getMethodParams() {
        return methodParams;
    }

    /**
     * @param methodParams the methodParams to set
     */
    public void setMethodParams(String methodParams) {
        this.methodParams = methodParams;
    }

    /**
     * @return the operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * @param operator the operator to set
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

}
