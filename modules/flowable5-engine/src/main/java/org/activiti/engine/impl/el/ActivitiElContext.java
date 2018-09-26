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
package org.activiti.engine.impl.el;

import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.FunctionMapper;
import org.flowable.common.engine.impl.javax.el.VariableMapper;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ActivitiElContext extends ELContext {

    protected ELResolver elResolver;

    public ActivitiElContext(ELResolver elResolver) {
        this.elResolver = elResolver;
    }

    @Override
    public ELResolver getELResolver() {
        return elResolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return new ActivitiFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper() {
        return null;
    }
}
