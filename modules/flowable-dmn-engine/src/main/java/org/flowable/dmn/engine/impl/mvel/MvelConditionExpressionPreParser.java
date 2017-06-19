/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.dmn.engine.impl.mvel;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Yvo Swillens
 */
public class MvelConditionExpressionPreParser implements MvelExpressionPreParser {

    protected static String[] OPERATORS = new String[]{"==", "!=", "<", ">", ">=", "<="};

    public static String parse(String expression, String inputVariable, String inputVariableType) {

        String parsedExpression = inputVariable;
        if ("date".equals(inputVariableType) || "number".equals(inputVariableType)) {
            parsedExpression += parseSegmentWithOperator(expression);
        } else {
            if (expression.startsWith(".")) {
                parsedExpression += expression;
            } else {
                parsedExpression += parseSegmentWithOperator(expression);
            }
        }
        return parsedExpression;
    }

    protected static String parseSegmentWithOperator(String expression) {
        String parsedExpressionSegment;
        if (expression.length() < 2 || !StringUtils.startsWithAny(expression, OPERATORS)) {
            parsedExpressionSegment = " == " + expression;
        } else {
            parsedExpressionSegment = " " + expression;
        }

        return parsedExpressionSegment;
    }
}
