/**
 *
 */
package com.aritu.eloraplm.viewer.dataevaluator.util;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.aritu.eloraplm.core.util.ExpressionEvaluator;
import com.aritu.eloraplm.exceptions.EloraException;
import com.aritu.eloraplm.viewer.dataevaluator.api.ConditionDescriptor;
import com.aritu.eloraplm.viewer.dataevaluator.api.DataObtainerAdapter;

/**
 * @author aritu
 *
 */
public class ConditionEvaluatorHelper {

    public static boolean fulfillsConditions(DocumentModel doc, String action,
            ConditionDescriptor[] conditions, boolean allRequired)
            throws EloraException {
        return fulfillsConditions(doc, null, action, conditions, allRequired);
    }

    public static boolean fulfillsConditions(DocumentModel doc,
            DocumentModel relatedDoc, String action,
            ConditionDescriptor[] conditions, boolean allRequired)
            throws EloraException {

        if (conditions == null || conditions.length == 0) {
            return true;
        }

        boolean allOk = false;
        for (ConditionDescriptor condition : conditions) {
            DataObtainerAdapter doa = new DataObtainerAdapter(condition);

            String left = ValueObtainerHelper.getValue(doc, relatedDoc, action,
                    doa);
            String right = condition.value;

            boolean ok = ExpressionEvaluator.evaluateExpression(left, right,
                    condition.operator);

            if (allRequired && !ok) {
                return false;
            }
            allOk = allOk || ok;
        }

        return allOk;
    }

}
