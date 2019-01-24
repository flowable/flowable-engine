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
package org.flowable.cmmn.engine.impl.function;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.el.function.AbstractFlowableShortHandExpressionFunction;

/**
 * @author Joram Barrez
 */
public abstract class AbstractCmmnExpressionFunction extends AbstractFlowableShortHandExpressionFunction {

    public static final String VARIABLE_SCOPE_NAME = "planItemInstance";

    public static final String FINAL_FUNCTION_PREFIX = "cmmn";

    public static final List<String> FUNCTION_PREFIXES = Collections.singletonList(FINAL_FUNCTION_PREFIX);

    public AbstractCmmnExpressionFunction(String functionName) {
        super(VARIABLE_SCOPE_NAME, Collections.singletonList(functionName), functionName);
    }

    @Override
    protected List<String> getFunctionPrefixOptions() {
        return FUNCTION_PREFIXES;
    }

    @Override
    protected String getFinalFunctionPrefix() {
        return FINAL_FUNCTION_PREFIX;
    }

}
