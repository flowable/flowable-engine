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
package org.flowable.app.engine.impl.el;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;

/**
 * @author Tijs Rademakers
 */
public class AppVariableScopeELResolver extends VariableContainerELResolver {

    public AppVariableScopeELResolver(VariableContainer variableContainer) {
        super(variableContainer);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            return super.getValue(context, base, property);
        }
        return null;
    }

}
