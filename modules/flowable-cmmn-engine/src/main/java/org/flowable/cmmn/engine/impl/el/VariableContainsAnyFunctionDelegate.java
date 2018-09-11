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

import java.lang.reflect.Method;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class VariableContainsAnyFunctionDelegate implements FlowableFunctionDelegate {
    
    public static final String FUNCTION_NAME = "containsAny";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VariableContainsAnyFunctionDelegate.class);
    
    private static Method METHOD;
    
    static {
        try {
            METHOD = VariableExpressionFunctionsUtil.class.getDeclaredMethod("containsAny", PlanItemInstance.class, String.class, Object[].class);
        } catch (Exception e) {
            LOGGER.error("Cannot find correct contains method on " + VariableExpressionFunctionsUtil.class, e);
        }
    }

    @Override
    public String prefix() {
        return "variables";
    }

    @Override
    public String localName() {
        return FUNCTION_NAME;
    }
    
    @Override
    public Method functionMethod() {
        return METHOD;
    }

}
