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

import java.util.Map;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.variable.service.impl.el.VariableScopeExpressionManager;

/**
 * @author Joram Barrez
 */
public class CmmnExpressionManager extends VariableScopeExpressionManager {

    public CmmnExpressionManager() {
    }

    public CmmnExpressionManager(Map<Object, Object> beans) {
       super(beans);
    }

    @Override
    protected ELResolver createVariableElResolver(VariableContainer variableContainer) {
        return new CmmnVariableScopeELResolver(variableContainer);
    }
    
}
