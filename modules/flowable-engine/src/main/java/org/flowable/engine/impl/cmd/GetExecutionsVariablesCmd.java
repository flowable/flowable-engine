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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.VariableInstance;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Daisuke Yoshimoto
 */
public class GetExecutionsVariablesCmd implements Command<List<VariableInstance>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected Set<String> executionIds;

    public GetExecutionsVariablesCmd(Set<String> executionIds) {
        this.executionIds = executionIds;
    }

    @Override
    public List<VariableInstance> execute(CommandContext commandContext) {
        // Verify existence of executions
        if (executionIds == null) {
            throw new FlowableIllegalArgumentException("executionIds is null");
        }
        if (executionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of executionIds is empty");
        }

        List<VariableInstance> instances = new ArrayList<>();
        List<VariableInstanceEntity> entities = CommandContextUtil.getVariableInstanceEntityManager(commandContext).findVariableInstancesByExecutionIds(executionIds);
        for (VariableInstanceEntity entity : entities) {
            entity.getValue();
            instances.add(entity);
        }
        return instances;
    }

}
