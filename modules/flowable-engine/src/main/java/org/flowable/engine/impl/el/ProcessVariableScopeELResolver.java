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
package org.flowable.engine.impl.el;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerELResolver;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class ProcessVariableScopeELResolver extends VariableContainerELResolver  {
    
    public ProcessVariableScopeELResolver(VariableContainer variableContainer) {
        super(variableContainer);
    }

    public static final String EXECUTION_KEY = "execution";
    public static final String TASK_KEY = "task";
    public static final String LOGGED_IN_USER_KEY = "authenticatedUserId";
    
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            if ((EXECUTION_KEY.equals(property) && variableContainer instanceof ExecutionEntity) || (TASK_KEY.equals(property) && variableContainer instanceof TaskEntity)) {
                context.setPropertyResolved(true);
                return variableContainer;
                
            } else if (EXECUTION_KEY.equals(property) && variableContainer instanceof TaskEntity) {
                context.setPropertyResolved(true);
                String executionId = ((TaskEntity) variableContainer).getExecutionId();
                ExecutionEntity executionEntity = null;
                if (executionId != null) {
                    executionEntity = CommandContextUtil.getExecutionEntityManager().findById(executionId);
                }
                return executionEntity;
                
            } else if (LOGGED_IN_USER_KEY.equals(property)) {
                context.setPropertyResolved(true);
                return Authentication.getAuthenticatedUserId();
                
            } else {
                return super.getValue(context, base, property);
            }
        }
        return null;
    }

}
