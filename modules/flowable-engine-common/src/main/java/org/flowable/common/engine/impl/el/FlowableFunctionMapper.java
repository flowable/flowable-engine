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

import org.flowable.common.engine.impl.javax.el.FunctionMapper;

/**
 * A date function mapper that can be used in EL expressions
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FlowableFunctionMapper extends FunctionMapper {

    protected FlowableFunctionResolver functionResolver;

    public FlowableFunctionMapper(FlowableFunctionResolver functionResolver) {
        setFunctionResolver(functionResolver);

    }

    public void setFunctionResolver(FlowableFunctionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }

    @Override
    public Method resolveFunction(String prefix, String localName) {
        if (functionResolver != null) {
            return functionResolver.resolveFunction(prefix, localName);
        }
        return null;
    }

}
