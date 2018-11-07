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
package org.flowable.dmn.engine.impl.el;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class ExecutionVariableFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionVariableFactory.class);

    public static Object getExecutionVariable(String type, Object expressionResult) {

        if (type == null || expressionResult == null) {
            LOGGER.error("could not create result variable: type {} expression result {}", type, expressionResult);
            throw new FlowableException("could not create result variable");
        }

        Object executionVariable;

        try {
            if (StringUtils.equals("boolean", type)) {
                if (expressionResult instanceof Boolean) {
                    executionVariable = expressionResult;
                } else {
                    executionVariable = new Boolean(expressionResult.toString());
                }
            } else if (StringUtils.equals("string", type)) {
                if (expressionResult instanceof String) {
                    executionVariable = expressionResult;
                } else {
                    executionVariable = expressionResult.toString();
                }
            } else if (StringUtils.equals("number", type)) {
                if (expressionResult instanceof Double) {
                    executionVariable = expressionResult;
                } else if (expressionResult instanceof BigDecimal) {
                    executionVariable = ((BigDecimal) expressionResult).doubleValue();
                } else if (expressionResult instanceof BigInteger) {
                    executionVariable = ((BigInteger) expressionResult).longValue();
                } else {
                    executionVariable = Double.valueOf(expressionResult.toString());
                }
            } else if (StringUtils.equals("date", type)) {
                if (expressionResult instanceof Date) {
                    executionVariable = expressionResult;
                } else {
                    executionVariable = new DateTime(expressionResult.toString()).toDate();
                }
            } else {
                LOGGER.error("could not create result variable: unrecognized mapping type");
                throw new FlowableException("could not create result variable: unrecognized mapping type");
            }
        } catch (Exception e) {
            LOGGER.error("could not create result variable", e);
            throw new FlowableException("Could not create execution variable", e);
        }

        return executionVariable;
    }

    public static List<Object> getExecutionVariables(String type, List<Object> expressionResults) {
        if (type == null || expressionResults == null) {
            return null;
        }

        List<Object> executionVariables = new ArrayList<>();
        for (Object expressionResult : expressionResults) {
            executionVariables.add(getExecutionVariable(type, expressionResult));
        }

        return executionVariables;
    }
}
