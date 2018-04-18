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

package org.flowable.cmmn.engine.impl.task;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnTaskVariableScopeResolver implements InternalTaskVariableScopeResolver {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultCmmnTaskVariableScopeResolver(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public VariableScopeImpl resolveParentVariableScope(TaskEntity taskEntity) {
        if (ScopeTypes.CMMN.equals(taskEntity.getScopeType())) {
            if (taskEntity.getSubScopeId() != null) {
                return (VariableScopeImpl) cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(taskEntity.getSubScopeId());
            } else if (taskEntity.getScopeId() != null) {
                return  (VariableScopeImpl) cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(taskEntity.getScopeId());
            }
        }
        return null;
    }

}
