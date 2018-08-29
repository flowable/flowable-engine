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
package org.flowable.cmmn.engine.impl.el;

import java.util.Arrays;
import java.util.List;

/**
 * @author Joram Barrez
 */
public class AbstractVariableExpressionEnhancer extends AbstractFlowableFunctionExpressionEnhancer {
    
    private static final List<String> FUNCTION_PREFIXES = Arrays.asList("variables", "vars", "var");
    
    private static final String FINAL_FUNCTION_PREFIX = "variables";
    
    public AbstractVariableExpressionEnhancer(List<String> functionNameOptions, String functionName) {
        super(FUNCTION_PREFIXES, functionNameOptions, FINAL_FUNCTION_PREFIX, functionName);
    }
    
}
