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

package org.flowable.common.engine.impl.el;

import java.lang.reflect.Method;
import java.util.List;

import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.javax.el.FunctionMapper;

/**
 * A date function mapper that can be used in EL expressions
 * 
 * @author Tijs Rademakers
 */
public class FlowableFunctionMapper extends FunctionMapper {

    protected List<FlowableFunctionDelegate> functionDelegates;

    public FlowableFunctionMapper(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;
    }

    public void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;
    }

    @Override
    public Method resolveFunction(String prefix, String localName) {
        if (functionDelegates != null) {
            for (FlowableFunctionDelegate functionDelegate : functionDelegates) {
                if (functionDelegate.prefix().equals(prefix) && functionDelegate.localName().equals(localName)) {
                    return functionDelegate.functionMethod();
                }
            }
        }

        return null;
    }

}
